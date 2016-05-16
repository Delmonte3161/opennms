
package org.opennms.netmgt.poller;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.SpringCamelContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.db.MockDatabase;
import org.opennms.core.test.db.annotations.JUnitTemporaryDatabase;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"classpath:/META-INF/opennms/applicationContext-soa.xml",
		"classpath:/META-INF/opennms/applicationContext-pollerd.xml",
		"classpath:/META-INF/opennms/applicationContext-dao.xml",
		"classpath:/META-INF/opennms/applicationContext-minimal-conf.xml",
		"classpath*:/META-INF/opennms/component-dao.xml",
		"classpath*:/META-INF/opennms/component-service.xml",
		"classpath:/META-INF/opennms/applicationContext-daemon.xml",
		"classpath:/META-INF/opennms/applicationContext-eventUtil.xml",
		"classpath:/META-INF/opennms/mockEventIpcManager.xml",
		"classpath:/META-INF/opennms/applicationContext-commonConfigs.xml",
		"classpath:/META-INF/opennms/applicationContext-testMockEventIpcManager.xml" })
@JUnitConfigurationEnvironment
@JUnitTemporaryDatabase(tempDbClass = MockDatabase.class, reuseDatabase = false)
public class PollerMonitorsIntegrationIT {

	@Autowired
	ApplicationContext context;

	@Test
	public void testPoller() throws Exception {
		CamelContext camelContext = null;
		try {
			camelContext = SpringCamelContext.springCamelContext(context, false);

			camelContext.start();
			Thread.sleep(4000);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			camelContext.stop();

		}

	}

}