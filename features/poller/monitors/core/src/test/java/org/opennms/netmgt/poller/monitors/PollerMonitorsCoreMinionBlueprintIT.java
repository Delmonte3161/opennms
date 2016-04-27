/**
 * 
 */
package org.opennms.netmgt.poller.monitors;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.camel.component.ActiveMQComponent;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
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
import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.ServiceMonitor;
import org.opennms.netmgt.poller.mock.MockMonitoredService;
import org.opennms.netmgt.poller.monitors.support.DistPollerMonitorDao;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author pk015603
 *
 */
@RunWith( OpenNMSJUnit4ClassRunner.class )
@ContextConfiguration( locations = {"classpath:/META-INF/opennms/emptyContext.xml",
		"classpath:/META-INF/opennms/applicationContext-soa.xml" } )
public class PollerMonitorsCoreMinionBlueprintIT extends CamelBlueprintTestSupport{
	
	private static BrokerService m_broker = null;
	
	MockServiceMonitor m_mockServiceMonitor=null;
	
	private static final String LOCATION = "RDU";

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
    	Properties props = new Properties();
        props.setProperty("alias", "opennms.broker");
       
        //creating the Active MQ component and service
        ActiveMQComponent activeMQ = new ActiveMQComponent();
        activeMQ.setBrokerURL("tcp://127.0.0.1:61616");
        services.put( Component.class.getName(),
               new KeyValueHolder<Object, Dictionary>( activeMQ, props ) );
        
        OnmsDistPoller distPoller = new OnmsDistPoller();
        distPoller.setId(DistPollerDao.DEFAULT_DIST_POLLER_ID);
        distPoller.setLabel(DistPollerDao.DEFAULT_DIST_POLLER_ID);
        distPoller.setLocation(LOCATION);
        DistPollerDao distPollerDao = new DistPollerMonitorDao(distPoller);

        services.put( DistPollerDao.class.getName(),
                new KeyValueHolder<Object, Dictionary>(distPollerDao, new Properties() ) );
    	
    	Properties prop = new Properties();
    	prop.setProperty("implementation", "org.opennms.netmgt.poller.monitors.AvailabilityMonitor");
    	m_mockServiceMonitor = new MockServiceMonitor();
    	services.put(ServiceMonitor.class.getName(),new KeyValueHolder<Object, Dictionary>( m_mockServiceMonitor, prop));
    }

    // The location of our Blueprint XML file to be used for testing
    @Override
    protected String getBlueprintDescriptor()
    {
        return "file:blueprint-poller-monitors-core-minion.xml,file:src/test/resources/blueprint-empty-camel-context.xml";
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
    public void testPollerMonitorCoreMinion() throws Exception
    {
		 /*
         * Create a Camel listener for the location queue that will respond with
         */
        SimpleRegistry registry = new SimpleRegistry();
        CamelContext mockPoller = new DefaultCamelContext(registry);
        mockPoller.addComponent("queuingservice", ActiveMQComponent.activeMQComponent("tcp://127.0.0.1:61616"));
        mockPoller.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                String from = String.format("activemq:Location-%s.Poller.AvailabilityMonitor", LOCATION);

                from(from)
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                    	MonitoredService svc = new MockMonitoredService(1, "Node One", InetAddressUtils.addr("127.0.0.1"), "SMTP");
                        Map<String, Object> parms = new HashMap<String, Object>();
                        parms.put("port",61616);	
                	    AvailabilityMonitor am = exchange.getIn().getBody(AvailabilityMonitor.class);

                        Message out = exchange.getOut();
                        PollStatus ps = am.poll(svc, parms);
                        System.out.println("************************"+ps+"**************************");
                        out.setBody(ps);
                    }
                });
            }
        });
        
        mockPoller.start();
        
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
