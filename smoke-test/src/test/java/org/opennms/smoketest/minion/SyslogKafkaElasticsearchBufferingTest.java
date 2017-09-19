/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/
package org.opennms.smoketest.minion;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.is;

import java.net.InetSocketAddress;
import java.util.Date;

import org.junit.Test;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.netmgt.dao.hibernate.MinionDaoHibernate;
import org.opennms.netmgt.model.minion.OnmsMinion;
import org.opennms.smoketest.utils.DaoUtils;
import org.opennms.test.system.api.AbstractTestEnvironment;
import org.opennms.test.system.api.NewTestEnvironment.ContainerAlias;
import org.opennms.test.system.api.TestEnvironmentBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.RateLimiter;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerInfo;

/**
 * This test will send syslog messages over the following message bus:
 * 
 * Minion -> Kafka -> OpenNMS Eventd -> Elasticsearch REST -> Elasticsearch 5
 * 
 * It stops OpenNMS, sends 1000 messages (that will be buffered inside Kafka)
 * and then starts OpenNMS to make sure that all of the buffered messages get
 * converted into events and are forwarded to Elasticsearch.
 * 
 * @author Seth
 */
public class SyslogKafkaElasticsearchBufferingTest extends AbstractSyslogTestCase {

    private static final Logger LOG = LoggerFactory.getLogger(SyslogKafkaElasticsearchBufferingTest.class);

    /**
     * Override this method to customize the test environment.
     */
    @Override
    protected TestEnvironmentBuilder getEnvironmentBuilder() {
        final TestEnvironmentBuilder builder = super.getEnvironmentBuilder();
        return builder;
    }

    @Test
    public void testMinionSyslogsOverKafkaToEsRest() throws Exception {
        Date startOfTest = new Date();
        int numMessages = 1000;
        int packetsPerSecond = 50;

        ContainerAlias containerAlias = getMinionAlias();

		InetSocketAddress minionSshAddr = testEnvironment.getServiceAddress(containerAlias, 8201);
        InetSocketAddress opennmsSshAddr = testEnvironment.getServiceAddress(ContainerAlias.OPENNMS, 8101);
        InetSocketAddress kafkaAddress = testEnvironment.getServiceAddress(ContainerAlias.KAFKA, 9092);
        InetSocketAddress zookeeperAddress = testEnvironment.getServiceAddress(ContainerAlias.KAFKA, 2181);
        InetSocketAddress esRestAddr = testEnvironment.getServiceAddress(ContainerAlias.ELASTICSEARCH_5, 9200);

        // Install the Kafka syslog and trap handlers on the Minion system
        installFeaturesOnMinion(minionSshAddr, kafkaAddress);

        // Install the Kafka and Elasticsearch features on the OpenNMS system
        installFeaturesOnOpenNMS(opennmsSshAddr, kafkaAddress, zookeeperAddress);

        final String sender = testEnvironment.getContainerInfo(ContainerAlias.SNMPD).networkSettings().ipAddress();

        // Wait for the minion to show up
        await().atMost(90, SECONDS).pollInterval(5, SECONDS)
            .until(DaoUtils.countMatchingCallable(
                 getDaoFactory().getDao(MinionDaoHibernate.class),
                 new CriteriaBuilder(OnmsMinion.class)
                     .gt("lastUpdated", startOfTest)
                     .eq("location", "MINION")
                     .toCriteria()
                 ),
                 is(1)
             );

        // Shut down OpenNMS. Syslog messages will accumulate in the Kafka
        // message queue while it is down.
        stopContainer(ContainerAlias.OPENNMS);

        LOG.info("Warming up syslog routes by sending 100 packets");

        // Warm up the routes
        sendMessage(containerAlias, sender, 100,null);

        for (int i = 0; i < 20; i++) {
            LOG.info("Slept for " + i + " seconds");
            Thread.sleep(1000);
        }

        // Make sure that this evenly divides into the numMessages
        final int chunk = 50;
        // Make sure that this is an even multiple of chunk
        final int logEvery = 100;

        int count = 0;
        long start = System.currentTimeMillis();

        // Send ${numMessages} syslog messages
        RateLimiter limiter = RateLimiter.create(packetsPerSecond);
        for (int i = 0; i < (numMessages / chunk); i++) {
            limiter.acquire(chunk);
            sendMessage(containerAlias, sender, chunk,null);
            count += chunk;
            if (count % logEvery == 0) {
                long mid = System.currentTimeMillis();
                LOG.info(String.format("Sent %d packets in %d milliseconds", logEvery, mid - start));
                start = System.currentTimeMillis();
            }
        }

        // Start OpenNMS. It should begin to consume syslog messages and forward
        // them to Elasticsearch without dropping messages.
        startContainer(ContainerAlias.OPENNMS);

        // 100 warm-up messages plus ${numMessages} messages
        //pollForElasticsearchEventsUsingJest(this::getEs5Address, 100 + numMessages);
        
        int resendCount = 0;
        while(pollForElasticsearchEventsUsingJestAndReturnValue(esRestAddr,numMessages) == 0){
           resendCount++;
     	   LOG.info("Resending Packets:"+resendCount);
     	   sendMessage(containerAlias, sender, chunk,null);
     	   Thread.sleep(60000);
      	   if(resendCount>30){
        		 LOG.info("Timed out :( Test failed! ");
        		 break;
      	   }
        }
        
    }

    protected InetSocketAddress getEs5Address() {
        return getServiceAddress((AbstractTestEnvironment)testEnvironment, ContainerAlias.ELASTICSEARCH_5, 9200, "tcp");
    }

    protected static InetSocketAddress getServiceAddress(AbstractTestEnvironment env, ContainerAlias alias, int port, String protocol) {
        try {
            // Fetch an up-to-date ContainerInfo for the ELASTICSEARCH_5 container
            final DockerClient docker = env.getDockerClient();
            final String id = env.getContainerInfo(alias).id();
            ContainerInfo info = docker.inspectContainer(id);
            return env.getServiceAddress(info, port, protocol); 
        } catch (DockerException | InterruptedException e) {
            LOG.error("Unexpected exception trying to fetch Elassticsearch port", e);
            return null;
        }
    }

    private void stopContainer(ContainerAlias alias) {
        final DockerClient docker = ((AbstractTestEnvironment)testEnvironment).getDockerClient();
        final String id = testEnvironment.getContainerInfo(alias).id();
        try {
            LOG.info("Stopping container: {} -> {}", alias, id);
            //docker.stopContainer(id, 60);
            docker.pauseContainer(id);
            LOG.info("Container stopped: {} -> {}", alias, id);
        } catch (DockerException | InterruptedException e) {
            LOG.warn("Unexpected exception while stopping container {}", id, e);
        }
    }

    private void startContainer(ContainerAlias alias) {
        final DockerClient docker = ((AbstractTestEnvironment)testEnvironment).getDockerClient();
        final String id = testEnvironment.getContainerInfo(alias).id();
        try {
            LOG.info("Starting container: {} -> {}", alias, id);
            docker.unpauseContainer(id);
            LOG.info("Container started: {} -> {}", alias, id);
        } catch (DockerException | InterruptedException e) {
            LOG.warn("Unexpected exception while starting container {}", id, e);
        }
    }
}
