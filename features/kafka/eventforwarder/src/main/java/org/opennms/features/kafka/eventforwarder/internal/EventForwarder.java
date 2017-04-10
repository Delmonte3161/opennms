package org.opennms.features.kafka.eventforwarder.internal;

import org.apache.camel.Produce;
import org.apache.commons.lang.NotImplementedException;
import org.opennms.core.camel.DefaultDispatcher;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventForwarder extends DefaultDispatcher implements CamelEventForwarder
{

    private static final Logger LOG = LoggerFactory.getLogger( EventForwarder.class );

    @Produce( property = "endpointUri" )
    CamelEventForwarder         m_proxy;

    public EventForwarder ( String endpointUri )
    {
        super( endpointUri );
        LOG.warn( "Endpoint URI passed: " + endpointUri );
    }

    @Override
    public void sendNowSync( Event event )
    {
        throw new NotImplementedException( this.getClass().getName() + " not yet implemented" );
    }

    @Override
    public void sendNowSync( Log eventLog )
    {
        throw new NotImplementedException( this.getClass().getName() + " not yet implemented" );
    }

    /**
     * Send the incoming {@link Event} message into the Camel route specified by the
     * {@link #m_endpointUri} property.
     */
    @Override
    public void sendNow( Event event )
    {
        LOG.info( "Event " + event.getDbid() + " with uei " + event.getUei() + " received" );
        m_proxy.sendNow( event );
        LOG.info( "Event " + event.getDbid() + " with uei " + event.getUei() + " forwarded via the forwarder" );
    }

    @Override
    public void sendNow( Log eventLog )
    {
        throw new NotImplementedException( this.getClass().getName() + " not yet implemented" );
    }

}
