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

package org.opennms.netmgt.poller.monitors;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;

import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.ServiceMonitor;
import org.opennms.netmgt.poller.mock.MockMonitoredService;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.springframework.test.context.ContextConfiguration;

/**
 * This is a simple blueprint test case for the service monitor.
 * 
 * @author pk015603
 *
 */
@RunWith(OpenNMSJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"classpath:/META-INF/opennms/emptyContext.xml"
})
@JUnitConfigurationEnvironment
public class PollerBlueprintIT extends CamelBlueprintTestSupport {

    /**
     * Use Aries Blueprint synchronous mode to avoid a blueprint deadlock bug.
     * 
     * @see https://issues.apache.org/jira/browse/ARIES-1051
     * @see https://access.redhat.com/site/solutions/640943
     */
    @Override
    public void doPreSetup() throws Exception {
        System.setProperty( "org.apache.aries.blueprint.synchronous", Boolean.TRUE.toString() );
        System.setProperty( "de.kalpatec.pojosr.framework.events.sync", Boolean.TRUE.toString() );
    }

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    public boolean isUseDebugger() {
        // must enable debugger
        return true;
    }

    @Override
    public String isMockEndpoints() {
        return "*";
    }

    /**
     * Register a mock OSGi {@link SchedulerService} so that we can make sure that the scheduler
     * whiteboard is working properly.
     */
    @SuppressWarnings( "rawtypes" )
    @Override
    protected void addServicesOnStartup( Map<String, KeyValueHolder<Object, Dictionary>> services ) {
    }

    // The location of our Blueprint XML file to be used for testing
    @Override
    protected String getBlueprintDescriptor() {
        return "file:src/main/resources/OSGI-INF/blueprint/blueprint.xml,file:src/test/resources/blueprint-empty-camel-context.xml";
    }

    @Test
    public void testIcmpPoller() throws UnknownHostException {
        // Fetch the ICMP monitor from the OSGi registry
        ServiceMonitor icmpMonitor = getOsgiService(ServiceMonitor.class, String.format("(implementation=%s)", IcmpMonitor.class.getName()));
        assertNotNull(icmpMonitor);

        MonitoredService svc = new MockMonitoredService(1, "Node One", InetAddressUtils.addr("127.0.0.1"), "ICMP");

        // Ping localhost
        PollStatus ps = icmpMonitor.poll(svc, Collections.emptyMap());
        assertTrue(ps.isUp());
        assertFalse(ps.isDown());
    }

    @Test
    public void testPollerRegistrations() throws UnknownHostException {
        // Fetch the ICMP monitor from the OSGi registry
        for (Class<?> clazz : new Class<?>[] {
            AvailabilityMonitor.class,
            CitrixMonitor.class,
            DnsMonitor.class,
            DNSResolutionMonitor.class,
            DominoIIOPMonitor.class,
            FtpMonitor.class,
            GpMonitor.class,
            HttpMonitor.class,
            HttpsMonitor.class,
            IcmpMonitor.class,
            ImapMonitor.class,
            JDBCMonitor.class,
            JDBCQueryMonitor.class,
            JDBCStoredProcedureMonitor.class,
            JolokiaBeanMonitor.class,
            JschSshMonitor.class,
            LdapMonitor.class,
            LdapsMonitor.class,
            LoopMonitor.class,
            MemcachedMonitor.class,
            NrpeMonitor.class,
            NtpMonitor.class,
            Pop3Monitor.class,
            SmtpMonitor.class,
            SshMonitor.class,
            SSLCertMonitor.class,
            StrafePingMonitor.class,
            SystemExecuteMonitor.class,
            TcpMonitor.class,
            TrivialTimeMonitor.class,
            WebMonitor.class
        }) {
            ServiceMonitor monitor = getOsgiService(ServiceMonitor.class, String.format("(implementation=%s)", clazz.getName()));
            assertNotNull(monitor);
        }
    }

    @Test
    public void testMissingRegistration() throws UnknownHostException {
        // Fetch the ICMP monitor from the OSGi registry
        try {
            getOsgiService(ServiceMonitor.class, "(implementation=org.opennms.doesnt.Exist)", 5000);
        } catch (RuntimeException e) {
            assertEquals("Gave up waiting for service (&(objectClass=org.opennms.netmgt.poller.ServiceMonitor)(implementation=org.opennms.doesnt.Exist))", e.getMessage());
            return;
        }
        fail("Didn't catch expected exception");
    }
}
