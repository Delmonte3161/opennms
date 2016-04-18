package org.opennms.netmgt.poller;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.db.DataSourceFactory;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.db.MockDatabase;
import org.opennms.netmgt.config.poller.NodeOutage;
import org.opennms.netmgt.config.poller.Package;
import org.opennms.netmgt.config.poller.PollerConfiguration;
import org.opennms.netmgt.mock.MockNetwork;
import org.opennms.netmgt.mock.MockPollerConfig;
import org.opennms.netmgt.mock.MockService;
import org.opennms.netmgt.poller.monitors.AvailabilityMonitor;
import org.springframework.test.context.ContextConfiguration;

@RunWith( OpenNMSJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath:/META-INF/opennms/emptyContext.xml" } )
public class PollerRoutingTest extends CamelTestSupport {
	
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

                from( "direct:pollAvailabilityMonitor" ).to( "bean:availabilityMonitorCamel" ).split( body() ).recipientList(
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

        MockNetwork m_network = new MockNetwork();
        m_network.setCriticalService("ICMP");
        m_network.addNode(1, "Router");
        m_network.addInterface("192.168.1.1");
        m_network.addService("ICMP");
        m_network.addService("SMTP");
        m_network.addService("SNMP");
        m_network.addInterface("192.168.1.2");
        m_network.addService("ICMP");
        m_network.addService("SMTP");
        m_network.addNode(2, "Server");
        m_network.addInterface("192.168.1.3");
        m_network.addService("ICMP");
        m_network.addService("HTTP");
        m_network.addService("SMTP");
        m_network.addService("SNMP");
        m_network.addNode(3, "Firewall");
        m_network.addInterface("192.168.1.4");
        m_network.addService("SMTP");
        m_network.addService("HTTP");
        m_network.addInterface("192.168.1.5");
        m_network.addService("SMTP");
        m_network.addService("HTTP");
        m_network.addNode(4, "DownNode");
        m_network.addInterface("192.168.1.6");
        m_network.addService("SNMP");
        m_network.addNode(5, "Loner");
        m_network.addInterface("192.168.1.7");
        m_network.addService("ICMP");
        m_network.addService("SNMP");
    
        
        MockDatabase m_db = new MockDatabase();
        m_db.populate(m_network);
        DataSourceFactory.setInstance(m_db);
        
        PollerConfiguration m_pollerConfig = new PollerConfiguration();
        
        m_pollerConfig.setNextOutageId(m_db.getNextOutageIdStatement());
        m_pollerConfig.setNodeOutage(new NodeOutage());
        m_pollerConfig.addPackage(new Package());
     

        // Execute the job
        template.requestBody( "direct:pollAvailabilityMonitor", m_pollerConfig );

        assertMockEndpointsSatisfied();
    }

}
