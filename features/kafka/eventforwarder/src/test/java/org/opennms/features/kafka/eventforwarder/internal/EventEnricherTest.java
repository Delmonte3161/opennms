package org.opennms.features.kafka.eventforwarder.internal;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.JAXBException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.netmgt.model.OnmsDistPoller;
import org.opennms.netmgt.model.OnmsEvent;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsNode.NodeLabelSource;
import org.opennms.netmgt.model.OnmsNode.NodeType;
import org.opennms.netmgt.model.monitoringLocations.OnmsMonitoringLocation;
import org.opennms.netmgt.xml.event.AlarmData;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Logmsg;
import org.opennms.netmgt.xml.event.Snmp;

public class EventEnricherTest
{

    private static EventEnricher eventEnricher = null;
    private static Event         event         = null;
    private static OnmsNode      onmsNode      = null;
    private static OnmsAlarm     onmsAlarm     = null;

    private static DateFormat    dateFormat    = new SimpleDateFormat( "EEE MMM d HH:mm:ss z yyyy" );
    // private List<Parm> parms = new ArrayList<>();

    @BeforeClass
    public static void setupBefore() throws ParseException
    {
        eventEnricher = new EventEnricher();

        // Setting test event parameters
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
        event.setIfIndex( null );
        event.setIfAlias( null );
        event.setMouseovertext( "mouse-over-text" );

        AlarmData alarmData = new AlarmData();
        alarmData.setReductionKey( "uei.opennms.org/KafkaTestEvent/OK::1:127.0.0.1" );
        alarmData.setAlarmType( 3 );
        alarmData.setClearKey( null );
        alarmData.setAutoClean( false );
        alarmData.setX733AlarmType( null );
        alarmData.setX733ProbableCause( null );

        event.setAlarmData( alarmData );

        // setting test onmsNode properties
        onmsNode = new OnmsNode();
        onmsNode.setId( 1 );
        onmsNode.setLocation( new OnmsMonitoringLocation( "dockeronmsdev", null ) );
        onmsNode.setForeignSource( "Minions" );
        onmsNode.setForeignId( "dockeronmsdev" );
        onmsNode.setLabelSource( NodeLabelSource.USER );
        onmsNode.setLabel( "dockeronmsdev" );
        onmsNode.setParent( null );
        onmsNode.setCreateTime( new Date() );
        onmsNode.setSysObjectId( "sys-object-oid" );
        onmsNode.setSysName( null );
        onmsNode.setSysDescription( null );
        onmsNode.setSysLocation( null );
        onmsNode.setSysContact( null );
        onmsNode.setType( NodeType.ACTIVE );
        onmsNode.setOperatingSystem( null );

        // setting test onmsalarm properties
        onmsAlarm = new OnmsAlarm();
        onmsAlarm.setId( 1000 );
        onmsAlarm.setUei( "uei.opennms.org/KafkaTestEvent/OK" );
        onmsAlarm.setReductionKey( "uei.opennms.org/KafkaTestEvent/OK: Reduction Key" );
        onmsAlarm.setClearKey( "uei.opennms.org/KafkaTestEvent/OK: Clear key" );

        OnmsDistPoller distPoller = new OnmsDistPoller();
        distPoller.setId( "00000000-0000-0000-0000-000000000000" );
        distPoller.setLabel( "localhost" );
        distPoller.setLocation( "Default" );
        distPoller.setType( "OpenNMS" );

        onmsAlarm.setDistPoller( distPoller );
        onmsAlarm.setNode( onmsNode );
        onmsAlarm.setLastEvent( EventEnricherUtils.getOnmsEvent( event ) );
    }

    @AfterClass
    public static void setupAfter()
    {
        eventEnricher = null;
        event = null;
        onmsNode = null;
        onmsAlarm = null;
    }

    @Test
    public void testGetOnmsEventAsJson() throws JAXBException
    {

        OnmsEvent onmsEvent = EventEnricherUtils.getOnmsEvent( event );

        String onmsEventJsonString = eventEnricher.marshall( onmsEvent );
        String onmsNodeJsonString = eventEnricher.marshall( onmsNode );
        String onmsAlarmJsonString = eventEnricher.marshall( onmsAlarm );

        /*
         * StringBuilder sb = new StringBuilder(); sb.append( onmsEventJsonString ).append( "\n"
         * ).append( onmsNodeJsonString ).append( "\n" ) .append( onmsAlarmJsonString );
         */
        System.out.println( onmsAlarmJsonString );
    }
}
