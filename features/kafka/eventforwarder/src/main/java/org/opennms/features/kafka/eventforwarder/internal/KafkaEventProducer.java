package org.opennms.features.kafka.eventforwarder.internal;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.camel.Exchange;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaEventProducer
{
    private static final Logger   LOG         = LoggerFactory.getLogger( KafkaEventProducer.class );

    KafkaProducer<String, String> producer;

    private final Properties      kafkaConfig = new Properties();

    private ConfigurationAdmin    configAdmin;

    private String                topic;

    public void init()
    {
        try
        {
            kafkaConfig.clear();
            kafkaConfig.put( "key.serializer", StringSerializer.class.getCanonicalName() );
            kafkaConfig.put( "value.serializer", StringSerializer.class.getCanonicalName() );

            // Retrieve all of the properties from org.opennms.features.kafka.eventforwarder.cfg
            final Dictionary<String, Object> properties = configAdmin
                            .getConfiguration( KafkaConstants.KAFKA_CONFIG_PID ).getProperties();

            if ( properties != null )
            {
                final Enumeration<String> keys = properties.keys();
                while ( keys.hasMoreElements() )
                {
                    final String key = keys.nextElement();
                    kafkaConfig.put( key, properties.get( key ) );
                }
            }

            LOG.info( "Initializing Kafkaproducer with :{}", properties );
            final ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
            try
            {
                // Class-loader hack for accessing the
                // org.apache.kafka.common.serialization.StringSerializer
                Thread.currentThread().setContextClassLoader( null );
                producer = new KafkaProducer<>( kafkaConfig );
            }
            finally
            {
                Thread.currentThread().setContextClassLoader( currentClassLoader );
            }

            LOG.info( "KafkaEventProducer initialized" );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    public void dispatch( Exchange exchange )
    {
        String message = exchange.getIn().getBody( String.class );

        LOG.info( "OnmsEvent json body received: {}", message );
        final ProducerRecord<String, String> record = new ProducerRecord<>( topic, message );
        try
        {
            final Future<RecordMetadata> future = producer.send( record );

            RecordMetadata recordMetaData = future.get();
            LOG.info( "RecordMetaData: " + recordMetaData.toString() );
        }
        catch ( InterruptedException e )
        {
            LOG.warn( "Interrupted while sending message to topic {}.", e );
        }
        catch ( ExecutionException e )
        {
            LOG.error( "Error occured while sending message to topic {}.", e );
        }
    }

    public void setConfigAdmin( ConfigurationAdmin configAdmin )
    {
        this.configAdmin = configAdmin;
    }

    public String getTopic()
    {
        return topic;
    }

    public void setTopic( String topic )
    {
        this.topic = topic;
    }
}
