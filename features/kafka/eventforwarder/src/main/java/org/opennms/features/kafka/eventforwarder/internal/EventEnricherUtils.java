package org.opennms.features.kafka.eventforwarder.internal;

import org.opennms.netmgt.model.OnmsEvent;
import org.opennms.netmgt.xml.event.Event;

/**
 * Utility class to copy attributes from org.opennms.netmgt.xml.event.Event and OnmsNode into
 * OnmsEvent class
 * 
 * @author sa029738
 *
 */
public class EventEnricherUtils
{
    private EventEnricherUtils ()
    {
    }

    public static OnmsEvent getOnmsEvent( Event event )
    {
        OnmsEvent onmsEvent = new OnmsEvent();
        
        if ( event != null )
        {
            // Setting event attributes
            onmsEvent.setId( event.getDbid() );
            onmsEvent.setEventUei( event.getUei() );
            onmsEvent.setEventTime( event.getTime() );
            onmsEvent.setEventHost( event.getHost() );
            onmsEvent.setEventSource( event.getSource() );
            onmsEvent.setEventSnmpHost( event.getSnmphost() );
            onmsEvent.setEventCreateTime( event.getCreationTime() );
            onmsEvent.setEventDescr( event.getDescr() );
            onmsEvent.setEventLogMsg( event.getLogmsg().getContent() );
            onmsEvent.setEventPathOutage( event.getPathoutage() );
            onmsEvent.setEventOperInstruct( event.getOperinstruct() );
            onmsEvent.setEventMouseOverText( event.getMouseovertext() );
            onmsEvent.setSeverityLabel( event.getSeverity() );
            onmsEvent.setIfIndex( event.getIfIndex() );
        }

        return onmsEvent;
    }
}
