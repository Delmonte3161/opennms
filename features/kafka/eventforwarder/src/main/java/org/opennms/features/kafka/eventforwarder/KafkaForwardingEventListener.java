package org.opennms.features.kafka.eventforwarder;

import java.util.Arrays;

import org.opennms.features.kafka.eventforwarder.internal.CamelEventForwarder;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.events.api.EventForwarder;
import org.opennms.netmgt.events.api.EventIpcManager;
import org.opennms.netmgt.events.api.EventListener;
import org.opennms.netmgt.xml.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

/**
 * This listener sends incoming events to an {@link EventForwarder} that uses Camel+Kafka to forward
 * events to an external Kafka messaging bus.
 */
public class KafkaForwardingEventListener implements EventListener
{

    private static final Logger          LOG                  = LoggerFactory
                    .getLogger( KafkaForwardingEventListener.class );

    private volatile CamelEventForwarder eventForwarder;
    private volatile EventIpcManager     eventIpcManager;
    private volatile NodeDao             nodeDao;
    private volatile TransactionTemplate transactionTemplate;
    private volatile boolean             logAllEvents         = false;

    String                               kafkaForwarderEvents = "";
    String[]                             kafkaEvents          = null;

    public CamelEventForwarder getEventForwarder()
    {
        return eventForwarder;
    }

    public void setEventForwarder( CamelEventForwarder eventForwarder )
    {
        this.eventForwarder = eventForwarder;
    }

    public EventIpcManager getEventIpcManager()
    {
        return eventIpcManager;
    }

    public void setEventIpcManager( EventIpcManager eventIpcManager )
    {
        this.eventIpcManager = eventIpcManager;
    }

    public NodeDao getNodeDao()
    {
        return nodeDao;
    }

    public void setNodeDao( NodeDao nodeDao )
    {
        this.nodeDao = nodeDao;
    }

    public TransactionTemplate getTransactionTemplate()
    {
        return transactionTemplate;
    }

    public void setTransactionTemplate( TransactionTemplate transactionTemplate )
    {
        this.transactionTemplate = transactionTemplate;
    }

    public boolean isLogAllEvents()
    {
        return logAllEvents;
    }

    public void setLogAllEvents( boolean logAllEvents )
    {
        this.logAllEvents = logAllEvents;
    }

    public String getKafkaForwarderEvents()
    {
        return kafkaForwarderEvents;
    }

    public void setKafkaForwarderEvents( String kafkaForwarderEvents )
    {
        this.kafkaForwarderEvents = kafkaForwarderEvents;
    }

    /**
     * <p>
     * init
     * </p>
     */
    public void init()
    {
        LOG.info( "Init method invoked" );
        Assert.notNull( eventIpcManager, "eventIpcManager must not be null" );
        Assert.notNull( eventForwarder, "eventForwarder must not be null" );

        registerEventListeners();

        LOG.info( "Kafka event forwarder initialized" );

    }

    private void registerEventListeners()
    {
        // Register an event listener against a test event. This is to validate the integration
        // between kafka-eventforwarder and opennms

        kafkaEvents = kafkaForwarderEvents.split( "," );
        getEventIpcManager().addEventListener( this, Arrays.asList( kafkaEvents ) );

        LOG.debug( "KafkaEventForwarder registered for below events: " );
        for ( String event : kafkaEvents )
            LOG.debug( event + "\n" );
    }

    public void destroy()
    {
        Assert.notNull( eventIpcManager, "eventIpcManager must not be null" );
        Assert.notNull( eventForwarder, "eventForwarder must not be null" );

        getEventIpcManager().removeEventListener( this, Arrays.asList( kafkaEvents ) );

        LOG.warn( "Kafka event forwarder uninstalled" );
    }

    /**
     * {@inheritDoc}
     *
     * This method is invoked by the JMS topic session when a new event is available for processing.
     * Currently only text based messages are processed by this callback. Each message is examined
     * for its Universal Event Identifier and the appropriate action is taken based on each UEI.
     */
    @Override
    public void onEvent( final Event event )
    {
        /*
         * Only process action-able events and either those events which are persisted to database
         * or if logAllEvents property is set to true
         */
        if ( (logAllEvents || (event.getDbid() != null && event.getDbid() > 0)) && (event.getAlarmData() != null) )
        {
            LOG.debug( Thread.currentThread().getName() + " Event received in EventListener: " + event.getDbid() );
            // Send the event to the event forwarder
            eventForwarder.sendNow( event );
        }
    }

    @Override
    public String getName()
    {
        return "KafkaForwardingEventListener";
    }
}
