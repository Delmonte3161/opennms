/**
 * 
 */
package org.opennms.netmgt.poller.monitors;

import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.broker.BrokerService;
import org.apache.camel.BeanInject;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.mock.MockMonitoredService;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.springframework.test.context.ContextConfiguration;

/**
 * This is a simple blueprint test case for the service monitor.
 * 
 * @author pk015603
 *
 */
@RunWith( OpenNMSJUnit4ClassRunner.class )
@ContextConfiguration( locations = {"classpath:/META-INF/opennms/emptyContext.xml",
		"classpath:/META-INF/opennms/applicationContext-soa.xml" } )
@JUnitConfigurationEnvironment
public class PollerBluprintIT extends CamelBlueprintTestSupport{
	
	private static BrokerService m_broker = null;
	
	@BeanInject
	private IcmpMonitor icmpMonitor;

    /**
     * Use Aries Blueprint synchronous mode to avoid a blueprint deadlock bug.
     * 
     * @see https://issues.apache.org/jira/browse/ARIES-1051
     * @see https://access.redhat.com/site/solutions/640943
     */
    @Override
    public void doPreSetup() throws Exception
    {
        System.setProperty( "org.apache.aries.blueprint.synchronous", Boolean.TRUE.toString() );
        System.setProperty( "de.kalpatec.pojosr.framework.events.sync", Boolean.TRUE.toString() );
    }

    @Override
    public boolean isUseAdviceWith()
    {
        return true;
    }

    @Override
    public boolean isUseDebugger()
    {
        // must enable debugger
        return true;
    }

    @Override
    public String isMockEndpoints()
    {
        return "*";
    }

    /**
     * Register a mock OSGi {@link SchedulerService} so that we can make sure that the scheduler
     * whiteboard is working properly.
     */
    @SuppressWarnings( "rawtypes" )
    @Override
    protected void addServicesOnStartup( Map<String, KeyValueHolder<Object, Dictionary>> services )
    {
    }

    // The location of our Blueprint XML file to be used for testing
    @Override
    protected String getBlueprintDescriptor()
    {
        return "file:src/main/resources/OSGI-INF/blueprint/blueprint.xml,file:src/test/resources/blueprint-empty-camel-context.xml";
    }

    @BeforeClass
    public static void startActiveMQ() throws Exception {
        m_broker = new BrokerService();
        m_broker.addConnector("tcp://127.0.0.1:61716");
        m_broker.start();
    }

    @AfterClass
    public static void stopActiveMQ() throws Exception {
        if (m_broker != null) {
            m_broker.stop();
        }
    }

    @Test
    public void testPoller() throws UnknownHostException
    {
    	MonitoredService svc = new MockMonitoredService(1, "Node One", InetAddressUtils.addr("127.0.0.1"), "SMTP");
        Map<String, Object> parms = new HashMap<String, Object>();
        parms.put("port",61716);

        PollStatus ps = icmpMonitor.poll(svc, parms);
        assertTrue(ps.isUp());
        assertFalse(ps.isDown());
    }

}
