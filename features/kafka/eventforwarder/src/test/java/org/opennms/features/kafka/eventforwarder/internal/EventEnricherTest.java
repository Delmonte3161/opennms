package org.opennms.features.kafka.eventforwarder.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.bean.BeanInvocation;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.netmgt.model.OnmsAssetRecord;
import org.opennms.netmgt.model.OnmsDistPoller;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsNode.NodeLabelSource;
import org.opennms.netmgt.model.OnmsNode.NodeType;
import org.opennms.netmgt.model.monitoringLocations.OnmsMonitoringLocation;
import org.opennms.netmgt.xml.event.AlarmData;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Logmsg;
import org.opennms.netmgt.xml.event.Snmp;

import com.fasterxml.jackson.databind.ObjectMapper;

public class EventEnricherTest
{

    private static EventEnricher  eventEnricher  = null;
    private static Event          event          = null;
    private static OnmsNode       onmsNode       = null;
    private static OnmsAlarm      onmsAlarm      = null;
    private static AlarmData      alarmData      = null;

    private static ObjectMapper   objectMapper   = null;

    private static DateFormat     dateFormat     = new SimpleDateFormat( "EEE MMM d HH:mm:ss z yyyy" );
    // private List<Parm> parms = new ArrayList<>();

    private static BeanInvocation beanInvocation = null;

    @BeforeClass
    public static void setupBefore() throws ParseException
    {
        eventEnricher = new EventEnricher();
        objectMapper = new ObjectMapper();

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

        alarmData = new AlarmData();

        alarmData.setReductionKey( "uei.opennms.org/KafkaTestEvent/OK::1:127.0.0.1" );
        alarmData.setAlarmType( 3 );
        alarmData.setClearKey( null );
        alarmData.setAutoClean( false );
        alarmData.setX733AlarmType( null );
        alarmData.setX733ProbableCause( null );

        event.setAlarmData( alarmData );

        // setting test onmsNode properties
        onmsNode = new OnmsNode();
        onmsNode.setCreateTime( dateFormat.parse( "Tue Mar 21 19:39:53 UTC 2017" ) );

        OnmsAssetRecord onmsAssetRecord = new OnmsAssetRecord();
        onmsAssetRecord.setLastModifiedDate( dateFormat.parse( "Tue Mar 21 19:39:53 UTC 2017" ) );
        onmsNode.setAssetRecord( onmsAssetRecord );

        onmsNode.setId( 1 );
        onmsNode.setLocation( new OnmsMonitoringLocation( "dockeronmsdev", null ) );
        onmsNode.setForeignSource( "Minions" );
        onmsNode.setForeignId( "dockeronmsdev" );
        onmsNode.setLabelSource( NodeLabelSource.USER );
        onmsNode.setLabel( "dockeronmsdev" );
        onmsNode.setParent( null );
        onmsNode.setCreateTime( dateFormat.parse( "Tue Mar 21 19:39:53 UTC 2017" ) );
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

        beanInvocation = new BeanInvocation();

        Object[] args = new Object[1];
        args[0] = event;
        beanInvocation.setArgs( args );
    }

    @AfterClass
    public static void setupAfter()
    {
        eventEnricher = null;
        objectMapper = null;
        event = null;
        onmsNode = null;
        onmsAlarm = null;
        beanInvocation = null;
    }

    // Create a test CamelExchange based on default CamelContext
    private Exchange createExchange()
    {
        CamelContext context = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange( context );
        populateExchange( exchange );
        return exchange;
    }

    // Populate Camel exchange with test body
    private void populateExchange( Exchange exchange )
    {
        assertNotNull( "Exchange cannot be null. No exchange created", exchange );
        Message in = exchange.getIn();
        in.setBody( beanInvocation );
    }

    @Test
    public void testNodeMarshall() throws JAXBException, IOException
    {
        FileReader fr = new FileReader( "src/test/resources/nodejsonstring.txt" );
        BufferedReader br = new BufferedReader( fr );
        StringBuilder sb = new StringBuilder();
        String currLine = null;
        while ( (currLine = br.readLine()) != null )
        {
            sb.append( currLine );
        }

        br.close();

        String nodeJsonString = eventEnricher.marshall( onmsNode );

        Map<String, Object> m1 = (Map<String, Object>) (objectMapper.readValue( sb.toString(), Map.class ));
        Map<String, Object> m2 = (Map<String, Object>) (objectMapper.readValue( nodeJsonString, Map.class ));
        assertTrue( m1.equals( m2 ) );
    }

    @Test
    public void testAlarmMarshall() throws IOException, JAXBException
    {
        FileReader fr = new FileReader( "src/test/resources/alarmjsonstring.txt" );
        BufferedReader br = new BufferedReader( fr );
        StringBuilder sb = new StringBuilder();
        String currLine = null;
        while ( (currLine = br.readLine()) != null )
        {
            sb.append( currLine );
        }

        br.close();

        String alarmJsonString = eventEnricher.marshall( alarmData );
        Map<String, Object> m1 = (Map<String, Object>) (objectMapper.readValue( sb.toString(), Map.class ));
        Map<String, Object> m2 = (Map<String, Object>) (objectMapper.readValue( alarmJsonString, Map.class ));
        assertTrue( m1.equals( m2 ) );
    }

    @Test
    public void testProcess() throws NodeNotFoundException, IOException
    {
        NodeCache cache = Mockito.mock( NodeCache.class );
        eventEnricher.setCache( cache );
        when( cache.getEntry( anyLong() ) ).thenReturn( onmsNode );

        Exchange exchange = createExchange();
        eventEnricher.process( exchange );

        String jsonString = (String) exchange.getOut().getBody();

        FileReader fr = new FileReader( "src/test/resources/json.txt" );
        BufferedReader br = new BufferedReader( fr );
        StringBuilder sb = new StringBuilder();
        String currLine = null;
        while ( (currLine = br.readLine()) != null )
        {
            sb.append( currLine );
        }

        br.close();

        Map<String, Object> m1 = (Map<String, Object>) (objectMapper.readValue( sb.toString(), Map.class ));
        Map<String, Object> m2 = (Map<String, Object>) (objectMapper.readValue( jsonString, Map.class ));
        assertTrue( m1.equals( m2 ) );
    }
}
