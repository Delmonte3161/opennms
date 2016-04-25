/**
 * 
 */
package org.opennms.netmgt.poller.remote;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.soa.ServiceRegistry;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.db.MockDatabase;
import org.opennms.core.test.db.annotations.JUnitTemporaryDatabase;
import org.opennms.core.xml.JaxbUtils;
import org.opennms.netmgt.config.ContextServiceMonitorLocator;
import org.opennms.netmgt.config.DefaultServiceMonitorLocator;
import org.opennms.netmgt.config.PollerConfigManager;
import org.opennms.netmgt.config.poller.PollerConfiguration;
import org.opennms.netmgt.poller.Distributable;
import org.opennms.netmgt.poller.DistributionContext;
import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.ServiceMonitor;
import org.opennms.netmgt.poller.ServiceMonitorLocator;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
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
@JUnitTemporaryDatabase(dirtiesContext=false,tempDbClass=MockDatabase.class,reuseDatabase=false)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class ContextServiceMonitorLocatorTest extends CamelTestSupport {
	
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
	            "   <monitor service=\"mockServiceMonitorA\" class-name=\"org.opennms.netmgt.poller.remote.ContextServiceMonitorLocatorTest$MockServiceMonitor_A\"/>\n"+
	            "   <monitor service=\"mockServiceMonitorB\" class-name=\"org.opennms.netmgt.poller.remote.ContextServiceMonitorLocatorTest$MockServiceMonitor_B\"/>\n"+
	            "   <monitor service=\"mockServiceMonitorC\" class-name=\"org.opennms.netmgt.poller.remote.ContextServiceMonitorLocatorTest$MockServiceMonitor_C\"/>\n"+
	            "</poller-configuration>\n";
	

	@Autowired
	ServiceRegistry m_serviceregistry;
	
	@Autowired
	PollerConfigManager m_pollerConfig;
	
	MockServiceMonitor m_mockServiceMonitor=null;
	
	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void testToCheckForContextServiceMonitorLocator() {
		
		Map<String, String> properties =new ConcurrentSkipListMap<String, String>();
		properties.put("implementation", "org.opennms.netmgt.poller.monitors.VmwareMonitor");
		
		m_mockServiceMonitor=new MockServiceMonitor();
		m_serviceregistry.register(m_mockServiceMonitor, properties,ServiceMonitor.class);

		assertIsInstanceOf(ContextServiceMonitorLocator.class ,m_pollerConfig.getServiceMonitorLocators(DistributionContext.REMOTE_MONITOR).iterator().next());
	}
	
	@Test
	public void testToCheckDaemonForContextServiceMonitorLocator() throws IOException {

		TestPollerConfigManager factory = new TestPollerConfigManager(POLLER_CONFIG, "localhost", false);
		
		Map<String, String> properties =new ConcurrentSkipListMap<String, String>();
		properties.put("implementation", "org.opennms.netmgt.poller.remote.ContextServiceMonitorLocatorTest$MockServiceMonitor_A");

		MockServiceMonitor_A m_mockServiceMonitor=new MockServiceMonitor_A();
		
		factory.onServiceMonitorBind(m_mockServiceMonitor, properties);

		List<ServiceMonitorLocator> service=new ArrayList<ServiceMonitorLocator>(factory.getServiceMonitorLocators(DistributionContext.DAEMON));
		
		assertIsInstanceOf(ContextServiceMonitorLocator.class ,service.get(0));
		assertEquals("mockServiceMonitorA" ,service.get(0).getServiceName());
		
		assertIsInstanceOf(DefaultServiceMonitorLocator.class ,service.get(1));
		assertEquals("mockServiceMonitorB" ,service.get(1).getServiceName());
		
		assertIsInstanceOf(DefaultServiceMonitorLocator.class ,service.get(2));
		assertEquals("mockServiceMonitorC" ,service.get(2).getServiceName());

	}
	
	public class MockServiceMonitor implements ServiceMonitor
	{
		@Override
		public void close() {
		}

		@Override
		public PollStatus poll(MonitoredService svc,
				Map<String, Object> parameters) {
					return null;
		}
	}
	
	@Distributable
	public class MockServiceMonitor_A implements ServiceMonitor
	{
		@Override
		public void close() {
		}

		@Override
		public PollStatus poll(MonitoredService svc,
				Map<String, Object> parameters) {
					return null;
		}
	}
	
	@Distributable
	public class MockServiceMonitor_B implements ServiceMonitor
	{
		@Override
		public void close() {
		}

		@Override
		public PollStatus poll(MonitoredService svc,
				Map<String, Object> parameters) {
					return null;
		}
	}


	@Distributable
	public class MockServiceMonitor_C implements ServiceMonitor
	{
		@Override
		public void close() {
		}

		@Override
		public PollStatus poll(MonitoredService svc,
				Map<String, Object> parameters) {
			return null;
		}
	}
	
	static class TestPollerConfigManager extends PollerConfigManager {
	        private String m_xml;
	        
	        public TestPollerConfigManager(String xml, String localServer, boolean verifyServer) throws IOException {
	            super(new ByteArrayInputStream(xml.getBytes("UTF-8")), localServer, verifyServer);
	            save();
	        }

	        @Override
	        public void update() throws IOException {
	            m_config = JaxbUtils.unmarshal(PollerConfiguration.class, m_xml);
	            setUpInternalData();
	        }

	        @Override
	        protected void saveXml(String xml) throws IOException {
	            m_xml = xml;
	        }

	        public String getXml() {
	            return m_xml;
	        }
	    }
}




