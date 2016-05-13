/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.util.Map;
import java.util.StringTokenizer;

import org.opennms.core.concurrent.TimeoutTracker;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.core.utils.ParameterMap;
import org.opennms.netmgt.poller.Distributable;
import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.netmgt.poller.MonitoredServiceTask;
import org.opennms.netmgt.poller.NetworkInterface;
import org.opennms.netmgt.poller.NetworkInterfaceNotSupportedException;
import org.opennms.netmgt.poller.PollStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <P>
 * This class is designed to be used by the service poller framework to test the
 * availability of the POP3 service on remote interfaces. The class implements
 * the ServiceMonitor interface that allows it to be used along with other
 * plug-ins by the service poller framework.
 * </P>
 *
 * @author <A HREF="mailto:tarus@opennms.org">Tarus Balog </A>
 * @author <A HREF="mailto:mike@opennms.org">Mike </A>
 */

@Distributable
public class Pop3Monitor extends AbstractServiceMonitor {
    
    public static final Logger LOG = LoggerFactory.getLogger(Pop3Monitor.class);

    /**
     * Default POP3 port.
     */
    private static final int DEFAULT_PORT = 110;

    /**
     * {@inheritDoc}
     *
     * <P>
     * Poll the specified address for POP3 service availability.
     * </P>
     *
     * <P>
     * During the poll an attempt is made to connect on the specified port (by
     * default TCP port 110). If the connection request is successful, the
     * banner line generated by the interface is parsed and if the response
     * indicates that we are talking to an POP3 server we continue. Next, a POP3
     * 'QUIT' command is sent to the interface. Again the response is parsed and
     * verified. Provided that the interface's response is valid we set the
     * service status to SERVICE_AVAILABLE and return.
     * </P>
     */
    @Override
    public PollStatus poll(MonitoredServiceTask monSvct) {
    	MonitoredService svc = monSvct.getMonitoredService();
    	Map<String, Object> parameters = monSvct.getParameters();
        NetworkInterface<InetAddress> iface = svc.getNetInterface();

        // Get interface address from NetworkInterface
        //
        if (iface.getType() != NetworkInterface.TYPE_INET)
            throw new NetworkInterfaceNotSupportedException("Unsupported interface type, only TYPE_INET currently supported");

        // Process parameters
        //
        TimeoutTracker tracker = new TimeoutTracker(parameters, TimeoutTracker.ZERO_RETRIES, TimeoutTracker.DEFAULT_TIMEOUT);

        int port = ParameterMap.getKeyedInteger(parameters, "port", DEFAULT_PORT);

        InetAddress ipv4Addr = (InetAddress) iface.getAddress();

        LOG.debug("poll: address = {}, port = {}, {}", ipv4Addr, port, tracker);

        PollStatus serviceStatus = PollStatus.unavailable();

        for (tracker.reset(); tracker.shouldRetry() && !serviceStatus.isAvailable(); tracker.nextAttempt()) {
            Socket socket = null;
            final String hostAddress = InetAddressUtils.str(ipv4Addr);
			try {
                //
                // create a connected socket
                //
                tracker.startAttempt();

                socket = new Socket();
                socket.connect(new InetSocketAddress(ipv4Addr, port), tracker.getConnectionTimeout());
                socket.setSoTimeout(tracker.getSoTimeout());
                LOG.debug("Pop3Monitor: connected to host: {} on port: {}", ipv4Addr, port);

                // We're connected, so upgrade status to unresponsive
                serviceStatus = PollStatus.unresponsive();
                BufferedReader rdr = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                //
                // Tokenize the Banner Line, and check the first
                // line for a valid return.
                //
                // Server response should start with: "+OK"
                //
                String banner = rdr.readLine();
                double responseTime = tracker.elapsedTimeInMillis();

                if (banner == null)
                    continue;
                StringTokenizer t = new StringTokenizer(banner);

                if (t.nextToken().equals("+OK")) {
                    //
                    // POP3 server should recoginize the QUIT command
                    //
                    String cmd = "QUIT\r\n";
                    socket.getOutputStream().write(cmd.getBytes());

                    //
                    // Parse the response to the QUIT command
                    //
                    // Server response should start with: "+OK"
                    //
                    t = new StringTokenizer(rdr.readLine());
                    if (t.nextToken().equals("+OK")) {
                        serviceStatus = PollStatus.available(responseTime);
                    }
                }

                // If we get this far and the status has not been set
                // to available, then something didn't verify during
                // the banner checking or QUIT command process.
                if (!serviceStatus.isAvailable()) {
                    serviceStatus = PollStatus.unavailable();
                }
            } catch (NoRouteToHostException e) {
            	
            	String reason = "No route to host exception for address " + hostAddress;
                LOG.debug(reason, e);
                serviceStatus = PollStatus.unavailable(reason);
                
            } catch (InterruptedIOException e) {
            	
            	String reason = "did not connect to host with " + tracker;
                LOG.debug(reason);
                serviceStatus = PollStatus.unavailable(reason);
            	
            } catch (ConnectException e) {
            	
            	String reason = "Connection exception for address " + hostAddress;
                LOG.debug(reason, e);
                serviceStatus = PollStatus.unavailable(reason);
            } catch (IOException e) {
            	
            	String reason = "IOException while polling address " + hostAddress;
                LOG.debug(reason, e);
                serviceStatus = PollStatus.unavailable(reason);
            } finally {
                try {
                    // Close the socket
                    if (socket != null)
                        socket.close();

                } catch (IOException e) {
                    LOG.debug("poll: Error closing socket.", e);
                }
            }
        }

        //
        // return the status of the service
        //
        return serviceStatus;
    }

}
