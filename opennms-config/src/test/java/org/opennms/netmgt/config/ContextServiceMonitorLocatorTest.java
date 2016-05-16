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

package org.opennms.netmgt.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.soa.ServiceRegistry;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.db.annotations.JUnitTemporaryDatabase;
import org.opennms.netmgt.poller.Distributable;
import org.opennms.netmgt.poller.DistributionContext;
import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.ServiceMonitor;
import org.opennms.netmgt.poller.ServiceMonitorLocator;
import org.opennms.netmgt.poller.MonitoredServiceTask;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Malatesh.Sudarshan@cerner.com
 *
 */
@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations={
		"classpath:/META-INF/opennms/applicationContext-commonConfigs.xml",
		"classpath:/META-INF/opennms/applicationContext-soa.xml",
})
@JUnitConfigurationEnvironment
@JUnitTemporaryDatabase
//@JUnitTemporaryDatabase(dirtiesContext=false,tempDbClass=MockDatabase.class,reuseDatabase=false)
//@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class ContextServiceMonitorLocatorTest {

	public static final String POLLER_CONFIG = "\n" +
			"<poller-configuration\n" +
			"   threads=\"10\"\n" +
			"   nextOutageId=\"SELECT nextval(\'outageNxtId\')\"\n" +
			"   serviceUnresponsiveEnabled=\"false\">\n" +
			"   <node-outage status=\"on\" pollAllIfNoCriticalServiceDefined=\"true\"></node-outage>\n" +
			"   <package name=\"default\">\n" +
			"       <filter>IPADDR IPLIKE *.*.*.*</filter>\n" +
			"       <rrd step = \"300\">\n" + 
			"           <rra>RRA:AVERAGE:0.5:1:2016</rra>\n" + 
			"           <rra>RRA:AVERAGE:0.5:12:4464</rra>\n" + 
			"           <rra>RRA:MIN:0.5:12:4464</rra>\n" + 
			"           <rra>RRA:MAX:0.5:12:4464</rra>\n" + 
			"       </rrd>\n" +
			"       <service name=\"mockServiceMonitorA\" interval=\"3000\">\n" +
			"         <parameter key=\"retry\" value=\"1\"/>\n" +
			"       </service>\n" +
			"       <service name=\"mockServiceMonitorB\" interval=\"3000\">\n" +
			"         <parameter key=\"retry\" value=\"1\"/>\n" +
			"       </service>\n" +
			"       <service name=\"mockServiceMonitorC\" interval=\"3000\">\n" +
			"         <parameter key=\"retry\" value=\"1\"/>\n" +
			"       </service>\n" +

			"       <downtime begin=\"0\" end=\"900000\"/>\n" + 
			"   </package>\n" +
			"   <monitor service=\"mockServiceMonitorA\" class-name=\"" + MockServiceMonitor_A.class.getName() + "\"/>\n"+
			"   <monitor service=\"mockServiceMonitorB\" class-name=\"" + MockServiceMonitor_B.class.getName() + "\"/>\n"+
			"   <monitor service=\"mockServiceMonitorC\" class-name=\"" + MockServiceMonitor_C.class.getName() + "\"/>\n"+
			"</poller-configuration>\n";

	@Autowired
	ServiceRegistry m_serviceregistry;

	@Autowired
	PollerConfigManager m_pollerConfig;

	MockServiceMonitor m_mockServiceMonitor = null;

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testToCheckForContextServiceMonitorLocator() {

		Map<String, String> properties = new ConcurrentSkipListMap<String, String>();
		properties.put("implementation", "org.opennms.netmgt.poller.monitors.VmwareMonitor");

		m_mockServiceMonitor=new MockServiceMonitor();
		m_serviceregistry.register(m_mockServiceMonitor, properties,ServiceMonitor.class);

		assertTrue(ContextServiceMonitorLocator.class.isInstance(m_pollerConfig.getServiceMonitorLocators(DistributionContext.REMOTE_MONITOR).iterator().next()));
	}

	@Test
	public void testToCheckDaemonForContextServiceMonitorLocator() throws IOException {

		TestPollerConfigManager factory = new TestPollerConfigManager(POLLER_CONFIG, "localhost", false);

		Map<String, String> properties = new ConcurrentSkipListMap<String, String>();
		properties.put("implementation",  MockServiceMonitor_A.class.getName());

		MockServiceMonitor_A m_mockServiceMonitor=new MockServiceMonitor_A();

		factory.onServiceMonitorBind(m_mockServiceMonitor, properties);

		List<ServiceMonitorLocator> service=new ArrayList<ServiceMonitorLocator>(factory.getServiceMonitorLocators(DistributionContext.DAEMON));

		assertTrue(ContextServiceMonitorLocator.class.isInstance(service.get(0)));
		assertEquals("mockServiceMonitorA" ,service.get(0).getServiceName());

		assertTrue(DefaultServiceMonitorLocator.class.isInstance(service.get(1)));
		assertEquals("mockServiceMonitorB" ,service.get(1).getServiceName());

		assertTrue(DefaultServiceMonitorLocator.class.isInstance(service.get(2)));
		assertEquals("mockServiceMonitorC" ,service.get(2).getServiceName());

	}

	public class MockServiceMonitor implements ServiceMonitor
	{
		@Override
		public void close() {
		}

		@Override
		public PollStatus poll(MonitoredServiceTask monSvct) {
			return null;
		}
	}

	@Distributable
	public static class MockServiceMonitor_A implements ServiceMonitor
	{
		@Override
		public void close() {
		}

		@Override
		public PollStatus poll(MonitoredServiceTask monSvct) {
			return null;
		}
	}

	@Distributable
	public static class MockServiceMonitor_B implements ServiceMonitor
	{
		@Override
		public void close() {
		}

		@Override
		public PollStatus poll(MonitoredServiceTask monSvct) {
			return null;
		}
	}


	@Distributable
	public static class MockServiceMonitor_C implements ServiceMonitor
	{
		@Override
		public void close() {
		}

		@Override
		public PollStatus poll(MonitoredServiceTask monSvct) {
			return null;
		}
	}

	private static class TestPollerConfigManager extends PollerConfigManager {

		public TestPollerConfigManager(String xml, String localServer, boolean verifyServer) throws IOException {
			super(new ByteArrayInputStream(xml.getBytes("UTF-8")), localServer, verifyServer);
		}

		@Override
		public void update() throws IOException {}

		@Override
		protected void saveXml(String xml) throws IOException {}
	}
}
