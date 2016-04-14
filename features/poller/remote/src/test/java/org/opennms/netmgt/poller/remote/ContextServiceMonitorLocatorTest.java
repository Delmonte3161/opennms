/**
 * 
 */
package org.opennms.netmgt.poller.remote;

import static org.easymock.EasyMock.expect;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.db.MockDatabase;
import org.opennms.core.test.db.annotations.JUnitTemporaryDatabase;
import org.opennms.netmgt.poller.DistributionContext;
import org.opennms.netmgt.poller.ServiceMonitorLocator;
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
public class ContextServiceMonitorLocatorTest {
	private EasyMockUtils m_mock = new EasyMockUtils(); 

	@Autowired
	ServiceMonitorLocator locator;
	
	private PollerBackEnd m_backEnd;
	private PollService m_pollService;

	@Before
	public void setUp() throws Exception {
		m_backEnd = m_mock.createMock(PollerBackEnd.class);
		m_pollService = m_mock.createMock(PollService.class);
	}

	@Test
	public void testToRegisterServiceMonitor() {
		
		Set<ServiceMonitorLocator> locators = Collections.singleton(locator);
		expect(
				m_backEnd
				.getServiceMonitorLocators(DistributionContext.REMOTE_MONITOR))
				.andReturn(locators);
		m_pollService.setServiceMonitorLocators(locators);
	}

}
