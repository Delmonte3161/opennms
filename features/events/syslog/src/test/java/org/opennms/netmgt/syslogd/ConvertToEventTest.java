package org.opennms.netmgt.syslogd;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opennms.core.logging.Logging;
import org.opennms.core.test.ConfigurationTestUtils;
import org.opennms.core.test.db.annotations.JUnitTemporaryDatabase;
import org.opennms.netmgt.config.SyslogdConfig;
import org.opennms.netmgt.config.SyslogdConfigFactory;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

/**
  * Convert to event junit test file to test the performance of Syslogd ConvertToEvent processor
 * @author ms043660
 */
@JUnitConfigurationEnvironment
@JUnitTemporaryDatabase
public class ConvertToEventTest {
    private static final Logger LOG = LoggerFactory.getLogger(ConvertToEvent.class);
    private static SyslogdConfig config;
    private static final MetricRegistry METRICS = new MetricRegistry();
    Meter packetMeter=null;
    Meter connectionMeter=null;
    Histogram packetSizeHistogram=null;
    @BeforeClass
    public static void onceExecutedBeforeAll() throws MarshalException, ValidationException, IOException {
    	// Get a log instance
    	Logging.putPrefix(Syslogd.LOG4J_CATEGORY);

    	ConsoleReporter reporter = ConsoleReporter.forRegistry(METRICS)
    			.convertRatesTo(TimeUnit.SECONDS)
    			.convertDurationsTo(TimeUnit.MILLISECONDS)
    			.build();
    	reporter.start(1, TimeUnit.SECONDS);
    	
    		InputStream stream = new FileInputStream(
    				"src/test/resources/etc/syslogd-loadtest-configuration.xml");
    		config = new SyslogdConfigFactory(stream);
    }
    
    /**
     * Test method which calls the ConvertToEvent constructor.
     * 
     * @throws MarshalException
     * @throws ValidationException
     * @throws IOException
     * @throws InterruptedException 
     * @throws MessageDiscardedException 
     */
    @Test
    public void testConvertToEventOnSingleMessage() throws MarshalException,
            ValidationException, IOException, InterruptedException, MessageDiscardedException {

    	// Create some metrics
    	packetMeter = METRICS.meter(MetricRegistry.name(getClass(), "packets"));
    	connectionMeter = METRICS.meter(MetricRegistry.name(getClass(), "connections"));
    	packetSizeHistogram = METRICS.histogram(MetricRegistry.name(getClass(), "packetSize"));

        // Sample message which is embedded in packet and passed as parameter
        // to
        // ConvertToEvent constructor
        byte[] bytes = "<34> 2010-08-19 localhost foo10000: load test 10000 on tty1".getBytes();
        packetMeter.mark();
        // Datagram packet which is passed as parameter for ConvertToEvent
        // constructor
        DatagramPacket pkt = new DatagramPacket(bytes, bytes.length,
                                                InetAddress.getLocalHost(),
                                                SyslogClient.PORT);
        String data = StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(pkt.getData())).toString();
        packetSizeHistogram.update(data.length());
        // ConvertToEvent takes 4 parameter
        // @param addr The remote agent's address.
        // @param port The remote agent's port
        // @param data The XML data in US-ASCII encoding.
        // @param len The length of the XML data in the buffer
        
        	Instant startTime = Instant.now();
			ConvertToEvent convertToEvent = new ConvertToEvent(
					pkt.getAddress(),
					pkt.getPort(),
					data, config);
			connectionMeter.mark();
			Instant endTime = Instant.now();
			System.out.println((Duration.between(startTime, endTime).toNanos())/1e6); 
			Thread.sleep(1000);
        
    }
    
    
    /**
     * Test method which calls the ConvertToEvent constructor.
     * 
     * @throws MarshalException
     * @throws ValidationException
     * @throws IOException
     * @throws MessageDiscardedException 
     * @throws InterruptedException 
     */
    @Test
    public void testConvertToEventOnMultipleMessage() throws MarshalException,
    ValidationException, IOException, MessageDiscardedException, InterruptedException {
    	// Create some metrics
    	packetMeter = METRICS.meter(MetricRegistry.name(getClass(), "packets"));
    	connectionMeter = METRICS.meter(MetricRegistry.name(getClass(), "connections"));
    	packetSizeHistogram = METRICS.histogram(MetricRegistry.name(getClass(), "packetSize"));

    	double meanTime=0;
    	int numberOfIteration=1000;
    	
	 	for(int i=1;i<=numberOfIteration;i++)
    	{

    		// Sample message which is embedded in packet and passed as parameter
    		// to
    		// ConvertToEvent constructor
    		String testMessage="<34> 2010-08-19 localhost foo"+i+": load test "+i+" on tty1";
    		byte[] bytes = testMessage.getBytes();
    		packetMeter.mark();
    		// Datagram packet which is passed as parameter for ConvertToEvent
    		// constructor
    		DatagramPacket pkt = new DatagramPacket(bytes, bytes.length,
    				InetAddress.getLocalHost(),
    				SyslogClient.PORT);
    		String data = StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(pkt.getData())).toString();
            packetSizeHistogram.update(data.length());
    		// ConvertToEvent takes 4 parameter
    		// @param addr The remote agent's address.
    		// @param port The remote agent's port
    		// @param data The XML data in US-ASCII encoding.
    		// @param len The length of the XML data in the buffer
    		
    			Instant startTime = Instant.now();
    			ConvertToEvent convertToEvent = new ConvertToEvent(
    					pkt.getAddress(),
    					pkt.getPort(),
    					data, config);
    			connectionMeter.mark();
    			Instant endTime = Instant.now();
    			meanTime+= (Duration.between(startTime, endTime).toNanos())/1e6; 
    	}
    	System.out.println("Mean Time for converToEvent : "+(meanTime/numberOfIteration));
    }

    
}
