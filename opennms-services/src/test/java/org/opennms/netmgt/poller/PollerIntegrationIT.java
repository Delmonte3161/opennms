package org.opennms.netmgt.poller;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.spring.BeanUtils;
import org.opennms.core.test.MockLogAppender;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.db.annotations.JUnitTemporaryDatabase;
import org.opennms.netmgt.config.PollerConfigFactory;
import org.opennms.netmgt.config.poller.PollerConfiguration;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * A simple Spring context unit test for Discovery.
 * 
 * @author Seth
 */
@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations={
        "classpath:/META-INF/opennms/applicationContext-soa.xml",
        "classpath:/META-INF/opennms/applicationContext-commonConfigs.xml",
        "classpath:/META-INF/opennms/applicationContext-minimal-conf.xml",
        "classpath:/META-INF/opennms/applicationContext-dao.xml",
        "classpath*:/META-INF/opennms/component-dao.xml",
        "classpath:/META-INF/opennms/applicationContext-pollerd.xml"
})
@JUnitConfigurationEnvironment
@JUnitTemporaryDatabase
public class PollerIntegrationIT implements InitializingBean {

    @Autowired
    private Poller m_poller;

    @Autowired
    private PollerConfigFactory m_pollerConfig;

    @Override
    public void afterPropertiesSet() throws Exception {
        BeanUtils.assertAutowiring(this);
    }

    @Before
    public void setUp() throws Exception {
        MockLogAppender.setupLogging(true, "INFO");
    }

    @Test
    public void testPoller() throws Exception {

        PollerConfiguration config = m_pollerConfig.getConfiguration();


        // Don't re-init Discovery or it will reload the 
        // DiscoveryConfigFactory and erase our changes to 
        // the config
        //m_discovery.init();

        m_poller.start();

        m_poller.stop();
    }
}

