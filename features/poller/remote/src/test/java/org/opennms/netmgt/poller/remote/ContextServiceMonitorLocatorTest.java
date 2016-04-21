/**
 * 
 */
package org.opennms.netmgt.poller.remote;

import static org.easymock.EasyMock.expect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.lang.builder.EqualsBuilder;
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
import org.opennms.netmgt.poller.ServiceMonitorLocator;
import org.opennms.netmgt.poller.remote.support.DefaultPollerBackEnd;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.opennms.test.mock.EasyMockUtils;
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
@JUnitTemporaryDatabase(dirtiesContext=false,tempDbClass=MockDatabase.class,reuseDatabase=false)
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
