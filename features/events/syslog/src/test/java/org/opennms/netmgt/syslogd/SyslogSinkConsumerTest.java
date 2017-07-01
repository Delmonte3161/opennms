/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016-2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.syslogd;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.ipc.sink.mock.MockMessageDispatcherFactory;
import org.opennms.core.spring.BeanUtils;
import org.opennms.core.test.ConfigurationTestUtils;
import org.opennms.core.test.MockLogAppender;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.test.db.annotations.JUnitTemporaryDatabase;
import org.opennms.netmgt.config.SyslogdConfigFactory;
import org.opennms.netmgt.dao.api.DistPollerDao;
import org.opennms.netmgt.dao.mock.MockEventIpcManager;
import org.opennms.netmgt.eventd.Eventd;
import org.opennms.netmgt.events.api.EventListener;
import org.opennms.netmgt.syslogd.api.SyslogConnection;
import org.opennms.netmgt.syslogd.api.SyslogMessageLogDTO;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import com.codahale.metrics.MetricRegistry;

@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/META-INF/opennms/applicationContext-commonConfigs.xml",
        "classpath:/META-INF/opennms/applicationContext-minimal-conf.xml",
        "classpath:/META-INF/opennms/applicationContext-soa.xml",
        "classpath:/META-INF/opennms/applicationContext-dao.xml",
        "classpath:/META-INF/opennms/applicationContext-daemon.xml",
        "classpath:/META-INF/opennms/applicationContext-eventDaemon.xml",
        "classpath:/META-INF/opennms/applicationContext-eventUtil.xml",
        "classpath:/META-INF/opennms/mockEventIpcManager.xml",
        "classpath:/META-INF/opennms/mockMessageDispatcherFactory.xml",
        "classpath:/syslogdTest.xml" })
@JUnitConfigurationEnvironment
@JUnitTemporaryDatabase
public class SyslogSinkConsumerTest implements InitializingBean {

    private EventCounter m_eventCounter;

    @Autowired
    private MockEventIpcManager m_eventIpcManager;

    @Autowired
    private DistPollerDao m_distPollerDao;

    @Autowired
    private Eventd m_eventd;

    @Autowired
    private SyslogdConfigFactory m_config;

    @Autowired
    private MockMessageDispatcherFactory<SyslogConnection, SyslogMessageLogDTO> m_messageDispatcherFactory;

    private Syslogd m_syslogd;

    private SyslogSinkConsumer m_syslogSinkConsumer;

    private SyslogSinkModule m_syslogSinkModule;

    public static List<String> grookPatternList=new ArrayList<String>();

    @Override
    public void afterPropertiesSet() throws Exception {
        BeanUtils.assertAutowiring(this);
    }

    @Before
    public void setUp() throws Exception {
        MockLogAppender.setupLogging(true, "WARN");

        loadSyslogConfiguration("/etc/syslogd-loadtest-configuration.xml");

        m_eventCounter = new EventCounter();
        m_eventIpcManager.addEventListener(m_eventCounter);

        m_syslogSinkConsumer = new SyslogSinkConsumer(new MetricRegistry());
        m_syslogSinkConsumer.setDistPollerDao(m_distPollerDao);
        m_syslogSinkConsumer.setSyslogdConfig(m_config);
        m_syslogSinkConsumer.setEventForwarder(m_eventIpcManager);
        m_syslogSinkModule = m_syslogSinkConsumer.getModule();
        m_messageDispatcherFactory.setConsumer(m_syslogSinkConsumer);
        grookPatternList = setGrookPatternList(new File(
                                                        this.getClass().getResource("/etc/syslogd-configuration.properties").getPath()));
        m_syslogSinkConsumer.setGrokPatternsList(grookPatternList);
    }

    @After
    public void tearDown() throws Exception {
        if (m_syslogd != null) {
            m_syslogd.stop();
        }
    }

    private void loadSyslogConfiguration(final String configuration)
            throws IOException {
        InputStream stream = null;
        try {
            stream = ConfigurationTestUtils.getInputStreamForResource(this,
                                                                      configuration);
            m_config = new SyslogdConfigFactory(stream);
        } finally {
            if (stream != null) {
                IOUtils.closeQuietly(stream);
            }
        }
        if (m_syslogSinkConsumer != null) {
            m_syslogSinkConsumer.setSyslogdConfig(m_config);
            m_syslogSinkModule = m_syslogSinkConsumer.getModule();
        }
    }

    public static List<String> setGrookPatternList(File syslogConfigFile)
            throws IOException {
        return SyslogSinkConsumer.readPropertiesInOrderFrom(syslogConfigFile);
    }

    private SyslogMessageLogDTO getMessageLog(byte[] messageBytes)
            throws UnknownHostException {

        DatagramPacket pkt = new DatagramPacket(messageBytes,
                                                messageBytes.length,
                                                InetAddress.getLocalHost(),
                                                SyslogClient.PORT);
        return m_syslogSinkModule.toMessageLog(new SyslogConnection(pkt,
                                                                    false));
    }

    @Test
    public void testForRFCParser() throws Exception {
        
        loadSyslogConfiguration("/etc/syslogd-rfc-configuration.xml");
        
        m_eventCounter.anticipate();
        byte[] messageBytes;

        messageBytes = "<34>1 2003-10-11T22:14:15.000Z mymachine.example.com su - ID47 - BOM'su root' failed for lonvick on /dev/pts/8".getBytes("US-ASCII");
        m_syslogSinkConsumer.handleMessage(getMessageLog(messageBytes));

    

        assertEquals(1, m_eventCounter.getCount());
    }
    
    @Test
    public void testForCustomParser() throws Exception {

        loadSyslogConfiguration("/etc/syslogd-loadtest-configuration.xml");

        m_eventCounter.anticipate();
        byte[] messageBytes;

        messageBytes = "<6>test: 2007-01-01 127.0.0.1 OpenNMS[1234]: A SyslogNG style message".getBytes("US-ASCII");
        m_syslogSinkConsumer.handleMessage(getMessageLog(messageBytes));

        messageBytes = "<173>Dec 7 12:02:06 10.13.110.116 mgmtd[8326]: [mgmtd.NOTICE]: Configuration saved to database initial".getBytes("US-ASCII");
        m_syslogSinkConsumer.handleMessage(getMessageLog(messageBytes));

        messageBytes = "<0>Mar 14 17:10:25 petrus sudo:  cyrille : user NOT in sudoers ; TTY=pts/2 ; PWD=/home/cyrille ; USER=root ; COMMAND=/usr/bin/vi /etc/aliases".getBytes("US-ASCII");
        m_syslogSinkConsumer.handleMessage(getMessageLog(messageBytes));

        messageBytes = "<34>Mar 11 08:35:17 127.0.0.1 30128311[4]: Mar 11 08:35:16.844 CST: %SEC-6-IPACCESSLOGP: list in110 denied tcp 192.168.10.100(63923) -> 192.168.11.128(1521), 1 packet".getBytes("US-ASCII");
        m_syslogSinkConsumer.handleMessage(getMessageLog(messageBytes));
        
        assertEquals(4, m_eventCounter.getCount());
    }
    
    @Test
    public void testForSyslogNGParser() throws Exception {

        loadSyslogConfiguration("/etc/syslogd-syslogng-configuration.xml");

        m_eventCounter.anticipate();
        byte[] messageBytes;

        messageBytes = "<34> 2010-08-19 localhost foo10000: load test 10000 on tty1".getBytes("US-ASCII");
        m_syslogSinkConsumer.handleMessage(getMessageLog(messageBytes));

        assertEquals(1, m_eventCounter.getCount());
    }
    
    @Test
    public void testForSyslogNGParserTest() throws Exception {

        loadSyslogConfiguration("/etc/syslogd-syslogng-configuration.xml");

        m_eventCounter.anticipate();
        byte[] messageBytes;

        messageBytes = "<34> 2010-08-19 10.181.230.67 foo10000: load test 10000 on abc".getBytes("US-ASCII");
        m_syslogSinkConsumer.handleMessage(getMessageLog(messageBytes));

        assertEquals(1, m_eventCounter.getCount());
    }

    public static class EventCounter implements EventListener {
        private AtomicInteger m_eventCount = new AtomicInteger(0);

        private int m_expectedCount = 0;

        @Override
        public String getName() {
            return "eventCounter";
        }

        public void setAnticipated(final int eventCount) {
            m_expectedCount = eventCount;
        }

        public int getCount() {
            return m_eventCount.get();
        }

        public void anticipate() {
            m_expectedCount++;
        }

        @Override
        public void onEvent(final Event e) {
            final int current = m_eventCount.incrementAndGet();
            if (current % 100 == 0) {
                System.err.println(current + " out of " + m_expectedCount
                        + " expected events received");
            }
        }

    }
}
