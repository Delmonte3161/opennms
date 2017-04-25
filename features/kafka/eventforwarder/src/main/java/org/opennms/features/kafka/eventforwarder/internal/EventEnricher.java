package org.opennms.features.kafka.eventforwarder.internal;

import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.bean.BeanInvocation;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.opennms.netmgt.dao.api.AlarmDao;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.netmgt.model.OnmsEvent;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.xml.event.AlarmData;
import org.opennms.netmgt.xml.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Fetches the OnmsNode and OnmsCategory from the cache and populates the event body with the same.
 * The enriched event is then forwarded to a JSON serializer
 * 
 * @author sa029738
 *
 */
public class EventEnricher
{
    private static final Logger            LOG                 = LoggerFactory.getLogger( EventEnricher.class );

    private volatile NodeCache             cache;
    private volatile AlarmDao              alarmDao;
    private volatile TransactionOperations transactionOperations;

    private OnmsAlarm                      onmsAlarm           = null;
    private String                         onmsAlarmJsonString = "";

    public void process( Exchange exchange )
    {
        Long startTime = System.currentTimeMillis();

        Message in = exchange.getIn();

        try
        {
            OnmsNode onmsNode = null;

            Object incoming = in.getBody();
            if ( incoming instanceof BeanInvocation )
            {
                Object argument = ((BeanInvocation) incoming).getArgs()[0];

                if ( argument instanceof Event )
                {
                    if ( LOG.isDebugEnabled() )
                    {
                        LOG.debug( Thread.currentThread().getName() + " Processing event" );
                    }

                    Event event = (Event) argument;

                    LOG.info( Thread.currentThread().getName() + " Event received in EventEnricher: "
                                    + event.getDbid() );

                    onmsNode = cache.getEntry( event.getNodeid() );

                    // No null checks performed on the node fetched. No null values are cached
                    LOG.info( Thread.currentThread().getName() + " Node fetched from the cache: "
                                    + onmsNode.getNodeId() );

                    OnmsEvent onmsEvent = EventEnricherUtils.getOnmsEvent( event );
                    LOG.debug( Thread.currentThread().getName()
                                    + " *************Details of enriched event*************" );

                    String onmsEventJsonString = marshall( onmsEvent );

                    String onmsNodeJsonString = marshall( onmsNode );

                    StringBuilder sb = new StringBuilder();
                    sb.append( onmsEventJsonString ).append( "\n" ).append( onmsNodeJsonString ).append( "\n" );

                    AlarmData alarmData = event.getAlarmData();

                    if ( alarmData != null )
                    {
                        LOG.debug( Thread.currentThread().getName() + " Fetched alarm data " );
                        LOG.debug( Thread.currentThread().getName() + " Clearkey: " + alarmData.getClearKey() );
                        LOG.debug( Thread.currentThread().getName() + " Reductionkey: " + alarmData.getReductionKey() );

                        String onmsAlarmJsonString = marshall( alarmData );
                        sb.append( onmsAlarmJsonString );
                    }

                    exchange.getOut().setBody( sb.toString() );
                }
            }
        }
        catch ( NodeNotFoundException e )
        {
            LOG.error( "No node in database: ", e );
        }

        Long endTime = System.currentTimeMillis();
        LOG.debug( Thread.currentThread().getName() + " Enrichment time: " + (endTime - startTime) / 1000.00 );
    }

    /**
     * Retrives OnmsAlarm instance from the database based on the reductionkey
     * 
     * @return
     * @throws JAXBException
     */
    protected String getOnmsAlarmJsonString( String reductionKey )
    {
        LOG.debug( "Fetching alarm data from the database for reductionkey: " + reductionKey );

        transactionOperations.execute( new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult( TransactionStatus transactionStatus )
            {
                onmsAlarmJsonString = marshall( onmsAlarm );
            }
        } );

        return onmsAlarmJsonString;
    }

    /**
     * Serializes incoming OnmsEvent, OnmsNode or OnmsAlarm into Json string
     * 
     * @param onmsEntity
     * @return
     * @throws JAXBException
     */
    protected String marshall( Serializable onmsEntity )
    {
        try
        {
            Map<String, Object> properties = new HashMap<>();
            properties.put( MarshallerProperties.MEDIA_TYPE, "application/json" );
            properties.put( MarshallerProperties.JSON_MARSHAL_EMPTY_COLLECTIONS, false );

            // Create jaxb context
            JAXBContext jc = JAXBContext.newInstance( new Class[] { OnmsEvent.class, OnmsNode.class, AlarmData.class },
                                                      properties );

            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );

            Writer stringWriter = new StringWriter();
            marshaller.marshal( onmsEntity, stringWriter );
            return stringWriter.toString();
        }
        catch ( JAXBException e )
        {
            LOG.error( "Error marshalling " + onmsEntity, e );
            return "";
        }
    }

    /* Getters and setters */
    public NodeCache getCache()
    {
        return cache;
    }

    public void setCache( NodeCache cache )
    {
        this.cache = cache;
    }

    public AlarmDao getAlarmDao()
    {
        return alarmDao;
    }

    public void setAlarmDao( AlarmDao alarmDao )
    {
        this.alarmDao = alarmDao;
    }

    public TransactionOperations getTransactionOperations()
    {
        return transactionOperations;
    }

    public void setTransactionOperations( TransactionOperations transactionOperations )
    {
        this.transactionOperations = transactionOperations;
    }
}
