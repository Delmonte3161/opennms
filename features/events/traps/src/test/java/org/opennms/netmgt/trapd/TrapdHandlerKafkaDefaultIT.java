/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016-2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.trapd;

import static org.junit.Assert.*;

import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;

import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.util.KeyValueHolder;
import org.apache.curator.test.TestingServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.camel.CamelBlueprintTest;
import org.opennms.netmgt.config.TrapdConfig;
import org.opennms.netmgt.config.api.EventConfDao;
import org.opennms.netmgt.dao.mock.MockEventIpcManager.EmptyEventConfDao;
import org.opennms.netmgt.events.api.EventForwarder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Log;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:/META-INF/opennms/emptyContext.xml" })
public class TrapdHandlerKafkaDefaultIT extends CamelBlueprintTest {
	
	private static final Logger LOG = LoggerFactory.getLogger(TrapdHandlerKafkaDefaultIT.class);
	
	private static KafkaConfig kafkaConfig;

	private KafkaServer kafkaServer;

	private TestingServer zkTestServer;

	/**
	 * Use Aries Blueprint synchronous mode to avoid a blueprint deadlock bug.
	 * 
	 * @see https://issues.apache.org/jira/browse/ARIES-1051
	 * @see https://access.redhat.com/site/solutions/640943
	 */
	@Override
	public void doPreSetup() throws Exception {
		System.setProperty("org.apache.aries.blueprint.synchronous", Boolean.TRUE.toString());
		System.setProperty("de.kalpatec.pojosr.framework.events.sync", Boolean.TRUE.toString());

		zkTestServer = new TestingServer(2181);
		Properties properties = new Properties();
		properties.put("port", "9092");
		properties.put("host.name", "localhost");
		properties.put("broker.id", "5001");
		properties.put("enable.zookeeper", "false");
		properties.put("zookeeper.connect",zkTestServer.getConnectString());
		try{
			kafkaConfig = new KafkaConfig(properties);
			kafkaServer = new KafkaServer(kafkaConfig, null);
			kafkaServer.startup();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected void addServicesOnStartup(Map<String, KeyValueHolder<Object, Dictionary>> services) {
		// Create a mock TrapdConfigBean
		TrapdConfigBean config = new TrapdConfigBean();
		config.setSnmpTrapPort(10514);
		config.setSnmpTrapAddress("127.0.0.1");
		config.setNewSuspectOnTrap(false);

		services.put(
			TrapdConfig.class.getName(),
			new KeyValueHolder<Object, Dictionary>(config, new Properties())
		);

		services.put(
			EventForwarder.class.getName(),
			new KeyValueHolder<Object, Dictionary>(new EventForwarder() {
				@Override
				public void sendNow(Log eventLog) {
					// Do nothing
					LOG.info("Got an event log: " + eventLog.toString());
				}

				@Override
				public void sendNow(Event event) {
					// Do nothing
					LOG.info("Got an event: " + event.toString());
				}
			}, new Properties())
		);

		services.put(EventConfDao.class.getName(), new KeyValueHolder<Object, Dictionary>(new EmptyEventConfDao(), new Properties()));
        Properties props = new Properties();
        props.setProperty("alias", "onms.broker");
        services.put(Component.class.getName(),
                     new KeyValueHolder<Object, Dictionary>(ActiveMQComponent.activeMQComponent("vm://localhost?create=false"),
                                                            props));


	}

	// The location of our Blueprint XML files to be used for testing
	@Override
	protected String getBlueprintDescriptor() {
		return "file:blueprint-trapd-handler-kafka-default.xml,blueprint-empty-camel-context.xml";
	}

	@Test
	public void testTrapd() throws Exception {
		
		SimpleRegistry registry = new SimpleRegistry();
		CamelContext syslogd = new DefaultCamelContext(registry);
		
		syslogd.addRoutes(new RouteBuilder(){
		  @Override
		  public void configure() throws Exception {
			  from("direct:start").process(new Processor() {
                  @Override
                  public void process(Exchange exchange) throws Exception {
                      exchange.getIn().setBody("Test Message from Camel Kafka Component Final",String.class);
                      exchange.getIn().setHeader(KafkaConstants.PARTITION_KEY, 1);
                      exchange.getIn().setHeader(KafkaConstants.KEY, "1");
                  }
              }).to("kafka:localhost:9092?topic=trapd&serializerClass=kafka.serializer.StringEncoder");
		  }
		  
				
		});
		
		
		
		syslogd.addRoutes(new RouteBuilder() {
			@Override
			public void configure() throws Exception {

				from("kafka:localhost:9092?topic=trapd&zookeeperHost=localhost&zookeeperPort=2181&groupId=testing")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange)
                            throws Exception {
                        String messageKey = "";
                        if (exchange.getIn() != null) {
                            Message message = exchange.getIn();
                            Integer partitionId = (Integer) message
                                    .getHeader(KafkaConstants.PARTITION);
                            String topicName = (String) message
                                    .getHeader(KafkaConstants.TOPIC);
                            if (message.getHeader(KafkaConstants.KEY) != null)
                                messageKey = (String) message
                                        .getHeader(KafkaConstants.KEY);
                            Object data = message.getBody();

                            System.out.println("topicName :: "
                                    + topicName + " partitionId :: "
                                    + partitionId + " messageKey :: "
                                    + messageKey + " message :: "
                                    + data + "\n");
                        }
                    }
                }).to("log:input");

			}
		});
		

		syslogd.start();
	}
}
