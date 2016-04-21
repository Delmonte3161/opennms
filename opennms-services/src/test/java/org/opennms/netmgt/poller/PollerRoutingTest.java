package org.opennms.netmgt.poller;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.db.MockDatabase;
import org.opennms.core.test.db.annotations.JUnitTemporaryDatabase;
import org.opennms.netmgt.poller.mock.MockMonitoredService;
import org.opennms.netmgt.poller.monitors.AvailabilityMonitor;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.springframework.test.context.ContextConfiguration;

@RunWith( OpenNMSJUnit4ClassRunner.class )
@ContextConfiguration( locations = { 
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
        "classpath:/META-INF/opennms/applicationContext-testMockEventIpcManager.xml"
		} )

@JUnitConfigurationEnvironment 
@JUnitTemporaryDatabase(tempDbClass=MockDatabase.class,reuseDatabase=false)
public class PollerRoutingTest extends CamelTestSupport {
	
	protected volatile ModelCamelContext context;
		
	@Override
    protected JndiRegistry createRegistry() throws Exception
    {
        JndiRegistry registry = super.createRegistry();

        registry.bind( "availabilityMonitor", new AvailabilityMonitor() );
        registry.bind( "availabilityMonitorCamel", new ServiceMonitorCamelImpl("direct:pollAvailabilityMonitor") );
       
        return registry;
    }
	
	
	/**
     * Delay calling context.start() so that you can attach an {@link AdviceWithRouteBuilder} to the
     * context before it starts.
     */
    @Override
    public boolean isUseAdviceWith()
    {
        return true;
    }
    
    
    /**
     * Build the route for all of the config parsing messages.
     */
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception
    {
        return new RouteBuilder() {

            @Override
            public void configure() throws Exception
            {
                // Add exception handlers
                onException( IOException.class ).handled( true ).logStackTrace( true ).stop();

                from( "direct:pollAvailabilityMonitor" ).setHeader("CamelJmsRequestTimeout", simple("40000",Long.class)).to( "bean:availabilityMonitorCamel" ).split( body() ).recipientList(
                                simple( "seda:Location-${body.location}.Poller.AvailabilityMonitor" ) );

                from( "seda:Location-localhost.Poller.AvailabilityMonitor" ).to( "bean:availabilityMonitor" );
            }
        };
    }
    

    @Test
    public void testPoller() throws Exception
    {
        for ( RouteDefinition route : new ArrayList<RouteDefinition>( context.getRouteDefinitions() ) )
        {
            route.adviceWith( context, new AdviceWithRouteBuilder() {
                @Override
                public void configure() throws Exception
                {
                    mockEndpoints();
                }
            } );
        }
        context.start();

        MonitoredServiceTask monitoredServiceTask  = new MonitoredServiceTask();
        monitoredServiceTask.setMonitoredService(new MockMonitoredService(1, "wipv6day.opennms.org", InetAddress.getLocalHost(), "RESOLVE"));
        monitoredServiceTask.setParameters(new HashMap<String, Object>());

        // Execute the job
        template.requestBody( "direct:pollAvailabilityMonitor", monitoredServiceTask );

        assertMockEndpointsSatisfied();
    }

}
