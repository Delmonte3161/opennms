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
 * This class is designed to be used by the service poller framework to test the
 * availability of a generic TCP service on remote interfaces. The class
 * implements the ServiceMonitor interface that allows it to be used along with
 * other plug-ins by the service poller framework.
 *
 * @author Weave
 * @author <A HREF="mailto:tarus@opennms.org">Tarus Balog </A>
 * @author <A HREF="mike@opennms.org">Mike </A>
 * @author <A HREF="http://www.opennms.org/">OpenNMS </A>
 */

@Distributable
public class TcpMonitor extends AbstractServiceMonitor {
    
    
    public static final Logger LOG = LoggerFactory.getLogger(TcpMonitor.class);

    /**
     * Default port.
     */
    private static final int DEFAULT_PORT = -1;

    public static final String PARAMETER_BANNER = "banner";
    public static final String PARAMETER_PORT = "port";

    /**
     * {@inheritDoc}
     *
     * Poll the specified address for service availability.
     *
     * During the poll an attempt is made to connect on the specified port. If
     * the connection request is successful, the banner line generated by the
     * interface is parsed and if the banner text indicates that we are talking
     * to Provided that the interface's response is valid we set the service
     * status to SERVICE_AVAILABLE and return.
     */
    @Override
    public PollStatus poll(MonitoredServiceTask monSvct) {
    	MonitoredService svc = monSvct.getMonitoredService();
    	Map<String, Object> parameters = monSvct.getParameters();
        NetworkInterface<InetAddress> iface = svc.getNetInterface();

        //
        // Process parameters
        //

        //
        // Get interface address from NetworkInterface
        //
        if (iface.getType() != NetworkInterface.TYPE_INET)
            throw new NetworkInterfaceNotSupportedException("Unsupported interface type, only TYPE_INET currently supported");

        TimeoutTracker tracker = new TimeoutTracker(parameters, TimeoutTracker.ZERO_RETRIES, TimeoutTracker.DEFAULT_TIMEOUT);

        // Port
        //
        int port = ParameterMap.getKeyedInteger(parameters, PARAMETER_PORT, DEFAULT_PORT);
        if (port == DEFAULT_PORT) {
            throw new RuntimeException("TcpMonitor: required parameter 'port' is not present in supplied properties.");
        }

        // BannerMatch
        //
        String strBannerMatch = ParameterMap.getKeyedString(parameters, PARAMETER_BANNER, null);

        // Get the address instance.
        //
        InetAddress ipv4Addr = (InetAddress) iface.getAddress();

        final String hostAddress = InetAddressUtils.str(ipv4Addr);
	LOG.debug("poll: address = {}, port = {}, {}", hostAddress, port, tracker);

        // Give it a whirl
        //
        PollStatus serviceStatus = PollStatus.unavailable();

        for (tracker.reset(); tracker.shouldRetry() && !serviceStatus.isAvailable(); tracker.nextAttempt()) {
            Socket socket = null;
            try {
                
                tracker.startAttempt();

                socket = new Socket();
                socket.connect(new InetSocketAddress(ipv4Addr, port), tracker.getConnectionTimeout());
                socket.setSoTimeout(tracker.getSoTimeout());
                LOG.debug("TcpMonitor: connected to host: {} on port: {}", ipv4Addr, port);

                // We're connected, so upgrade status to unresponsive
                serviceStatus = PollStatus.unresponsive();

                if (strBannerMatch == null || strBannerMatch.length() == 0 || strBannerMatch.equals("*")) {
                    serviceStatus = PollStatus.available(tracker.elapsedTimeInMillis());
                    break;
                }

                BufferedReader rdr = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                //
                // Tokenize the Banner Line, and check the first
                // line for a valid return.
                //
                String response = rdr.readLine();
                double responseTime = tracker.elapsedTimeInMillis();

                if (response == null)
                    continue;
                LOG.debug("poll: banner = {}", response);
                LOG.debug("poll: responseTime= {}ms", responseTime);

                //Could it be a regex?
                if (strBannerMatch.charAt(0)=='~'){
                  if (!response.matches(strBannerMatch.substring(1)))
                    serviceStatus = PollStatus.unavailable("Banner does not match Regex '"+strBannerMatch+"'");
                  else
                    serviceStatus = PollStatus.available(responseTime);
                }
                else {
                  if (response.indexOf(strBannerMatch) > -1) {
                    serviceStatus = PollStatus.available(responseTime);
                  }
                  else {
                    serviceStatus = PollStatus.unavailable("Banner: '"+response+"' does not contain match string '"+strBannerMatch+"'");
                  }
                }

            } catch (NoRouteToHostException e) {
            	String reason = "No route to host exception for address " + hostAddress;
                LOG.debug(reason, e);
                serviceStatus = PollStatus.unavailable(reason);
                break; // Break out of for(;;)
            } catch (InterruptedIOException e) {
            	String reason = "did not connect to host with " + tracker;
                LOG.debug(reason);
                serviceStatus = PollStatus.unavailable(reason);
            } catch (ConnectException e) {
            	String reason = "Connection exception for address: " + ipv4Addr;
                LOG.debug(reason, e);
                serviceStatus = PollStatus.unavailable(reason);
            } catch (IOException e) {
            	String reason = "IOException while polling address: " + ipv4Addr;
                LOG.debug(reason, e);
                serviceStatus = PollStatus.unavailable(reason);
            } finally {
                try {
                    // Close the socket
                    if (socket != null)
                        socket.close();
                } catch (IOException e) {
                    e.fillInStackTrace();
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
