/**
 * 
 */
package org.opennms.netmgt.poller.remote;

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
import org.opennms.netmgt.config.ContextServiceMonitorLocator;
import org.opennms.netmgt.config.PollerConfigManager;
import org.opennms.netmgt.poller.DistributionContext;
import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.ServiceMonitor;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.opennms.test.mock.EasyMockUtils;
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
	private EasyMockUtils m_mock = new EasyMockUtils(); 

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
	public void testToCheckDaemonForContextServiceMonitorLocator() {
		
		Map<String, String> properties =new ConcurrentSkipListMap<String, String>();
		properties.put("implementation", "org.opennms.netmgt.poller.monitors.VmwareMonitor");
		
		m_mockServiceMonitor=new MockServiceMonitor();
		m_serviceregistry.register(m_mockServiceMonitor, properties,ServiceMonitor.class);
		
		assertIsInstanceOf(ContextServiceMonitorLocator.class ,m_pollerConfig.getServiceMonitorLocators(DistributionContext.DAEMON).iterator().next());
	
		properties.put("implementation", "org.opennms.netmgt.poller.monitors.ImapMonitor");
		m_serviceregistry.register(m_mockServiceMonitor, properties,ServiceMonitor.class);
		
		assertEquals(2, m_pollerConfig.getServiceMonitorLocators(DistributionContext.DAEMON).size());
		
		
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
}
