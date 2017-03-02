package org.opennms.netmgt.syslogd;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.junit.Test;
import org.opennms.core.test.ConfigurationTestUtils;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.config.SyslogdConfig;
import org.opennms.netmgt.config.SyslogdConfigFactory;
import org.opennms.netmgt.dao.api.DistPollerDao;
import org.opennms.netmgt.dao.api.MonitoringLocationDao;
import org.opennms.netmgt.dao.hibernate.InterfaceToNodeCacheDaoImpl;
import org.opennms.netmgt.dao.mock.MockInterfaceToNodeCache;
import org.opennms.netmgt.syslogd.BufferParser.BufferParserFactory;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Parm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
  * Convert to event junit test file to test the performance of Syslogd ConvertToEvent processor
 * @author ms043660
 */
public class ConvertToEventTest {

    private static final Logger LOG = LoggerFactory.getLogger(ConvertToEventTest.class);
    
    private final ExecutorService m_executor = Executors.newSingleThreadExecutor();

    /**
     * Test method which calls the ConvertToEvent constructor.
     * 
     * @throws MarshalException
     * @throws ValidationException
     * @throws IOException
     */
    @Test
    public void testConvertToEvent() throws MarshalException, ValidationException, IOException {

        InterfaceToNodeCacheDaoImpl.setInstance(new MockInterfaceToNodeCache());

        // 10000 sample syslogmessages from xml file are taken and passed as
        // Inputstream to create syslogdconfiguration
        InputStream stream = ConfigurationTestUtils.getInputStreamForResource(this,
                                                                              "/etc/syslogd-loadtest-configuration.xml");
        SyslogdConfig config = new SyslogdConfigFactory(stream);

        // Sample message which is embedded in packet and passed as parameter
        // to
        // ConvertToEvent constructor
        byte[] bytes = "<34> 2010-08-19 localhost foo10000: load test 10000 on tty1".getBytes();

        // Datagram packet which is passed as parameter for ConvertToEvent
        // constructor
        DatagramPacket pkt = new DatagramPacket(bytes, bytes.length,
                                                InetAddress.getLocalHost(),
                                                SyslogClient.PORT);
        String data = StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(pkt.getData())).toString();

        // ConvertToEvent takes 4 parameter
        // @param addr The remote agent's address.
        // @param port The remote agent's port
        // @param data The XML data in US-ASCII encoding.
        // @param len The length of the XML data in the buffer
        try {
            String pattern="<%{INTEGER:facilityCode}> %{INTEGER:year}-%{INTEGER:month}-%{INTEGER:day} %{STRING:hostname} %{STRING:processName}: %{STRING:message}";
            ByteBuffer message = ByteBuffer.wrap(bytes);
            ConvertToEvent convertToEvent = new ConvertToEvent(
                DistPollerDao.DEFAULT_DIST_POLLER_ID,
                MonitoringLocationDao.DEFAULT_MONITORING_LOCATION_ID,
                pkt.getAddress(),
                pkt.getPort(),
                data,
                config,
                SyslogSinkConsumer.loadParamsMap(getParamsList(message, pattern))
            );
            LOG.info("Generated event: {}", convertToEvent.getEvent().toString());
        } catch (MessageDiscardedException | ParseException | InterruptedException | ExecutionException e) {
            LOG.error("Message Parsing failed", e);
            fail("Message Parsing failed: " + e.getMessage());
        }
    }

    @Test
    public void testCiscoEventConversion() throws MarshalException, ValidationException, IOException {

        InputStream stream = ConfigurationTestUtils.getInputStreamForResource(this, "/etc/syslogd-cisco-configuration.xml");
        SyslogdConfig config = new SyslogdConfigFactory(stream);

        try {
            String pattern="<%{INTEGER:facilityCode}> %{MONTH:month} %{INTEGER:day} %{INTEGER:hour}:%{INTEGER:minute}:%{INTEGER:second} %{STRING:hostname} %{STRING:processName}[%{INTEGER:processId}]: %{STRING:month} %{INTEGER:day} %{STRING:timestamp} %{STRING:timeZone}: \\%%{STRING:facilityField}-%{INTEGER:priority}-%{STRING:mnemonic}: %{STRING:message}";
            String syslogMessage= "<190>Mar 11 08:35:17 127.0.0.1 30128311[4]: Mar 11 08:35:16.844 CST: %SEC-6-IPACCESSLOGP: list in110 denied tcp 192.168.10.100(63923) -> 192.168.11.128(1521), 1 packet";
            ByteBuffer message = ByteBuffer.wrap(syslogMessage.getBytes());
            ConvertToEvent convertToEvent = new ConvertToEvent(
                DistPollerDao.DEFAULT_DIST_POLLER_ID,
                MonitoringLocationDao.DEFAULT_MONITORING_LOCATION_ID,
                InetAddressUtils.ONE_TWENTY_SEVEN,
                9999,
                syslogMessage,
                config,
                SyslogSinkConsumer.loadParamsMap(getParamsList(message, pattern))
            );
            LOG.info("Generated event: {}", convertToEvent.getEvent().toString());
        } catch (MessageDiscardedException | ParseException | InterruptedException | ExecutionException e) {
            LOG.error("Message Parsing failed", e);
            fail("Message Parsing failed: " + e.getMessage());
        }
    }
<<<<<<< HEAD
    
    /**
     Method to  generate Params List matching with grok pattern 
    **/
    private List<Parm> getParamsList(ByteBuffer message,String pattern) throws InterruptedException, ExecutionException
    {
        BufferParserFactory grokFactory = GrokParserFactory.parseGrok(pattern);

        CompletableFuture<Event>  event = null;

        event = grokFactory.parse(message.asReadOnlyBuffer(), m_executor);
        event.whenComplete((e, ex) -> {
            if (ex == null) {
                //System.out.println(e.toString());
            } else {
                ex.printStackTrace();
            }
        });

        return event.get().getParmCollection();

=======

    @Test
    public void testNms5984() throws MarshalException, ValidationException, IOException {

        InputStream stream = ConfigurationTestUtils.getInputStreamForResource(this, "/etc/syslogd-nms5984-configuration.xml");
        SyslogdConfig config = new SyslogdConfigFactory(stream);

        try {
            ConvertToEvent convertToEvent = new ConvertToEvent(
                DistPollerDao.DEFAULT_DIST_POLLER_ID,
                MonitoringLocationDao.DEFAULT_MONITORING_LOCATION_ID,
                InetAddressUtils.ONE_TWENTY_SEVEN,
                9999,
                "<11>Jul 19 15:55:21 otrs-test OTRS-CGI-76[14364]: [Error][Kernel::System::ImportExport::ObjectBackend::CI2CILink::ImportDataSave][Line:468]: CILink: Could not create link between CIs!", 
                config
            );
            LOG.info("Generated event: {}", convertToEvent.getEvent().toString());
        } catch (MessageDiscardedException e) {
            LOG.error("Message Parsing failed", e);
            fail("Message Parsing failed: " + e.getMessage());
        }
>>>>>>> opennmsdevelop/foundation-2017
    }
}
