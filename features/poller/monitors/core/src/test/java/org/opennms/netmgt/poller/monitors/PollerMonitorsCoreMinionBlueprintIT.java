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

package org.opennms.netmgt.poller.monitors;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;
import java.util.Properties;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.Component;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.dao.api.DistPollerDao;
import org.opennms.netmgt.model.OnmsDistPoller;
import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.netmgt.poller.MonitoredServiceTask;
import org.opennms.netmgt.poller.mock.MockMonitoredService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;

/**
 * This is a simple blueprint test case for the service monitor.
 * 
 * @author pk015603
 *
 */
@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations={
    "classpath:/META-INF/opennms/emptyContext.xml"
})
public class PollerMonitorsCoreMinionBlueprintIT extends CamelBlueprintTestSupport {

    private static final Logger LOG = LoggerFactory.getLogger(PollerMonitorsCoreMinionBlueprintIT.class);

    private static BrokerService m_broker = null;

    private static final String LOCATION = "RDU";

    /**
     * Use Aries Blueprint synchronous mode to avoid a blueprint deadlock bug.
     * 
     * @see https://issues.apache.org/jira/browse/ARIES-1051
     * @see https://access.redhat.com/site/solutions/640943
     */
    @Override
    public void doPreSetup() throws Exception {
        System.setProperty( "org.apache.aries.blueprint.synchronous", Boolean.TRUE.toString() );
        System.setProperty( "de.kalpatec.pojosr.framework.events.sync", Boolean.TRUE.toString() );
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    public boolean isUseDebugger() {
        // must enable debugger
        return true;
    }

    @Override
    public String isMockEndpoints() {
        return "*";
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected void addServicesOnStartup(Map<String, KeyValueHolder<Object, Dictionary>> services) {
        Properties props = new Properties();

        //creating the Active MQ component and service
        props.setProperty("alias", "opennms.broker");
        ActiveMQComponent activeMQ = new ActiveMQComponent();
        activeMQ.setBrokerURL("tcp://127.0.0.1:61616");
        services.put(Component.class.getName(), new KeyValueHolder<Object, Dictionary>( activeMQ, props ));

        OnmsDistPoller distPoller = new OnmsDistPoller();
        distPoller.setId(DistPollerDao.DEFAULT_DIST_POLLER_ID);
        distPoller.setLabel(DistPollerDao.DEFAULT_DIST_POLLER_ID);
        distPoller.setLocation(LOCATION);
        DistPollerDao distPollerDao = new DistPollerDaoMinion(distPoller);

        services.put(DistPollerDao.class.getName(), new KeyValueHolder<Object, Dictionary>(distPollerDao, new Properties()));
    }

    // The location of our Blueprint XML file to be used for testing
    @Override
    protected String getBlueprintDescriptor() {
        // We don't need the OSGI-INF/blueprint files here, they are loaded when the bundle starts
        return "file:blueprint-poller-monitors-core-minion.xml";
    }

    @BeforeClass
    public static void startActiveMQ() throws Exception {
        m_broker = new BrokerService();
        m_broker.addConnector("tcp://127.0.0.1:61616");
        m_broker.start();
    }

    @AfterClass
    public static void stopActiveMQ() throws Exception {
        if (m_broker != null) {
            m_broker.stop();
        }
    }
    
    @Test
    public void testPollerMonitorCoreMinion() throws Exception {
        // TODO: Add mock endpoint assertions

        MonitoredService svc = new MockMonitoredService(1, "Node One", InetAddressUtils.UNPINGABLE_ADDRESS, "ICMP");
        template.sendBody("queuingservice:Location-" + LOCATION + ".Poller." + IcmpMonitor.class.getSimpleName(), new MonitoredServiceTask(svc, Collections.emptyMap()));

        // TODO: Send messages to every service monitor endpoint

        // TODO: Assert that status messages that are returned are all unavailable

        assertMockEndpointsSatisfied();
    }
}
