package org.opennms.features.kafka.eventforwarder.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opennms.netmgt.model.OnmsEvent;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Logmsg;
import org.opennms.netmgt.xml.event.Snmp;

public class EventEnricherUtilsTest
{

    private static Event      event      = null;
    private static DateFormat dateFormat = new SimpleDateFormat( "EEE MMM d HH:mm:ss z yyyy" );

    @BeforeClass
    public static void setupBeforeClass() throws ParseException
    {
        event = new Event();
        event.setUuid( null );
        event.setDbid( 751 );
        event.setDistPoller( "dist-poller" );
        event.setCreationTime( dateFormat.parse( "Tue Mar 21 19:39:53 UTC 2017" ) );
        event.setMasterStation( "master-station" );
        event.setMask( null );
        event.setUei( "uei.opennms.org/KafkaTestEvent/OK" );
        event.setSource( "perl_send_event" );
        event.setNodeid( 1L );
        event.setTime( dateFormat.parse( "Tue Mar 21 19:39:53 UTC 2017" ) );
        event.setHost( "dockeronmsdev5.novalocal" );
        event.setInterface( "127.0.0.1" );
        event.setSnmphost( "snmp-host" );
        event.setService( "ICMP" );
        event.setSnmp( new Snmp() );
        event.setDescr( "Test event to verfiy OpenNMS-Kafka integration" );

        Logmsg logMsg = new Logmsg();
        logMsg.setContent( "Test default event to verify eventlistener to eventforwarder integration." );
        logMsg.setNotify( true );
        logMsg.setDest( "logndisplay" );

        event.setLogmsg( logMsg );
        event.setSeverity( "Normal" );
        event.setPathoutage( "path-outage" );
        event.setCorrelation( null );
        event.setOperinstruct( "operator-instruction" );
        event.setAutoacknowledge( null );
        event.setTticket( null );
        event.setIfIndex( 1 );
        event.setIfAlias( null );
        event.setMouseovertext( "mouse-over-text" );
    }

    @Test
    public void testNonNullEvent()
    {
        OnmsEvent onmsEvent = EventEnricherUtils.getOnmsEvent( event );
        assertNotNull( onmsEvent );

        assertEquals( 751, onmsEvent.getId().intValue() );
        assertEquals( "uei.opennms.org/KafkaTestEvent/OK", onmsEvent.getEventUei() );
        assertEquals( event.getTime(), onmsEvent.getEventTime() );
        assertEquals( "dockeronmsdev5.novalocal", onmsEvent.getEventHost() );
        assertEquals( "perl_send_event", onmsEvent.getEventSource() );
        assertEquals( "snmp-host", onmsEvent.getEventSnmpHost() );
        assertEquals( event.getCreationTime(), onmsEvent.getEventCreateTime() );
        assertEquals( "Test event to verfiy OpenNMS-Kafka integration", onmsEvent.getEventDescr() );
        assertEquals( "Test default event to verify eventlistener to eventforwarder integration.",
                      onmsEvent.getEventLogMsg() );
        assertEquals( "path-outage", onmsEvent.getEventPathOutage() );
        assertEquals( "operator-instruction", onmsEvent.getEventOperInstruct() );
        assertEquals( "mouse-over-text", onmsEvent.getEventMouseOverText() );
        assertEquals( "NORMAL", onmsEvent.getSeverityLabel() );
        assertEquals( 1, onmsEvent.getIfIndex().intValue() );
    }

    @Test
    public void testNullEvent()
    {
        OnmsEvent onmsEvent = EventEnricherUtils.getOnmsEvent( null );
        assertNotNull(onmsEvent);
        assertNull(onmsEvent.getId());
    }
}
