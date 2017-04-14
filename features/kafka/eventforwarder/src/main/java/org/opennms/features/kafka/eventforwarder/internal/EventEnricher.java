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
            OnmsAlarm onmsAlarm = null;

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
                    /*
                     * LOG.info( "******EVENT******" ); LOG.info( event.toString() ); LOG.info(
                     * "Fetching node from cache" );
                     */
                    onmsNode = cache.getEntry( event.getNodeid() );

                    // No null checks performed on the node fetched. No null values are cached

                    LOG.info( Thread.currentThread().getName() + " Node fetched from the cache: "
                                    + onmsNode.getNodeId() );

                    /*
                     * LOG.info( "******NODE******" ); LOG.info( onmsNode.toString() ); LOG.info(
                     * "Categories size: " + (onmsNode.getCategories() == null ? null :
                     * onmsNode.getCategories().size()) ); if ( onmsNode.getCategories() != null ) {
                     * LOG.info( "*************Categories info*************" ); for ( OnmsCategory
                     * category : onmsNode.getCategories() ) LOG.info( "Category : " +
                     * category.getId() + " : " + category.getName() + " : " +
                     * category.getDescription() ); }
                     */

                    AlarmData alarmData = event.getAlarmData();
                    LOG.info( Thread.currentThread().getName() + " Fetched alarm data " );

                    String onmsAlarmJsonString = "";

                    String onmsNodeJsonString = marshall( onmsNode );
                    StringBuilder sb = new StringBuilder();

                    if ( alarmData != null )
                    {
                        LOG.debug( Thread.currentThread().getName() + " Clearkey: " + alarmData.getClearKey() );
                        LOG.debug( Thread.currentThread().getName() + " Reductionkey: " + alarmData.getReductionKey() );

                        onmsAlarmJsonString = getOnmsAlarmJsonString( alarmData.getReductionKey() );
                        sb.append( onmsAlarmJsonString ).append( "\n" ).append( onmsNodeJsonString );
                    }
                    else
                    {
                        OnmsEvent onmsEvent = EventEnricherUtils.getOnmsEvent( event );

                        LOG.debug( Thread.currentThread().getName()
                                        + " *************Details of enriched event*************" );

                        String onmsEventJsonString = marshall( onmsEvent );

                        sb.append( onmsEventJsonString ).append( "\n" ).append( onmsNodeJsonString );

                        LOG.debug( Thread.currentThread().getName() + " Event id: " + onmsEvent.getId()
                                        + " Create time: " + onmsEvent.getEventCreateTime() );
                    }

                    /*
                     * LOG.debug( "Serialized OnmsEvent: " ); LOG.debug( onmsEventJsonString );
                     */

                    // LOG.debug( "Serialized OnmsNode: " );
                    /*
                     * LOG.debug( onmsNodeJsonString );
                     * 
                     * LOG.debug( "Serialized OnmsAlarm: " ); LOG.debug( onmsAlarmJsonString );
                     * 
                     * LOG.info( "***************OnmsEvent converted to JSON string***************"
                     * );
                     * 
                     * LOG.info( sb.toString() );
                     */

                    exchange.getOut().setBody( sb.toString() );
                }
            }
        }
        catch ( NodeNotFoundException e )
        {
            LOG.error( "No node in database: ", e );
        }
        catch ( JAXBException e )
        {
            LOG.error( "Marshall exception: ", e );
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
                onmsAlarm = alarmDao.findByReductionKey( reductionKey );
                if ( onmsAlarm != null )
                {
                    LOG.debug( "********OnmsAlarm fetched from database: Alarm details********" );
                    LOG.debug( "AlarmId: " + onmsAlarm.getId() );
                    LOG.debug( "Reductionkey: " + onmsAlarm.getReductionKey() );
                    LOG.debug( "Event uei: " + onmsAlarm.getUei() );

                    try
                    {
                        onmsAlarmJsonString = marshall( onmsAlarm );
                    }
                    catch ( JAXBException e )
                    {
                        LOG.error( "OnmsAlarm marshall exception: ", e );
                        onmsAlarmJsonString = "";
                    }
                }
                else
                {
                    LOG.info( "No alarm exists for reductionkey: " + reductionKey );
                }
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
    protected String marshall( Serializable onmsEntity ) throws JAXBException
    {

        Map<String, Object> properties = new HashMap<>();
        properties.put( MarshallerProperties.MEDIA_TYPE, "application/json" );
        properties.put( MarshallerProperties.JSON_MARSHAL_EMPTY_COLLECTIONS, false );

        // Create jaxb context
        JAXBContext jc = JAXBContext.newInstance( new Class[] { OnmsNode.class, OnmsAlarm.class }, properties );

        Marshaller marshaller = jc.createMarshaller();
        marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, true );

        Writer stringWriter = new StringWriter();
        marshaller.marshal( onmsEntity, stringWriter );
        return stringWriter.toString();
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
