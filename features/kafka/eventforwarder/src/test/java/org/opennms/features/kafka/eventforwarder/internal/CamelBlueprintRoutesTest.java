package org.opennms.features.kafka.eventforwarder.internal;

import java.util.Dictionary;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.netmgt.dao.mock.MockEventIpcManager;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

@RunWith( OpenNMSJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath:/META-INF/opennms/applicationContext-soa.xml",
        "classpath:/META-INF/opennms/applicationContext-mockDao.xml",
        "classpath:/META-INF/opennms/mockEventIpcManager.xml" } )
@JUnitConfigurationEnvironment
public class CamelBlueprintRoutesTest extends CamelBlueprintTestSupport
{

    private static final Logger LOG               = LoggerFactory.getLogger( CamelBlueprintRoutesTest.class );

    @Autowired
    private MockEventIpcManager m_eventIpcManager = null;

    /**
     */
    @SuppressWarnings( "rawtypes" )
    @Override
    protected void addServicesOnStartup( Map<String, KeyValueHolder<Object, Dictionary>> services )
    {
    }

    // Location of our Blueprint XML file to be used for testing
    @Override
    protected String getBlueprintDescriptor()
    {
        return "blueprint-kafka-event-forwarder-test.xml";
    }

    /*@Test
    public void testCamelContext()
    {
        ApplicationContext context = new ClassPathXmlApplicationContext( "blueprint-kafka-event-forwarder-test.xml" );
        assertNotNull(context);
    }*/

    @Test
    public void testBlueprintRoutes() throws InvalidSyntaxException
    {
        assertNotNull( m_eventIpcManager );
        try
        {
            ServiceReference<?>[] references = getBundleContext().getAllServiceReferences( CamelContext.class.getName(),
                                                                                           null );
            for ( ServiceReference<?> reference : references )
            {
                CamelContext context = (CamelContext) getBundleContext().getService( reference );

                // If the context has started and contains the endpoints from
                // blueprint-event-forwarder.xml, then we've found the correct
                // context so return true.
                if ( context.getStatus().isStarted()
                                && context.hasEndpoint( "seda:kafkaForwardEvent?concurrentConsumers=4&size=1000" ) != null
                                && context.hasEndpoint( "seda:KAFKA_PRE?size=1000&timeout=0&blockWhenFull=true" ) != null
                                && context.hasEndpoint( "seda:KAFKA?size=1001&timeout=0&blockWhenFull=true" ) != null )
                {
                    System.out.println( "Context started**************************************************************" );
                }
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Camel never started up. Test cannot continue." );
            throw e;
        }
    }
}
