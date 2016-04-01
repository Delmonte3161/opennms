/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2015 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2015 The OpenNMS Group, Inc.
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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.util.Map;

import org.opennms.core.utils.InetAddressUtils;
import org.opennms.core.utils.ParameterMap;
import org.opennms.core.utils.SocketUtils;
import org.opennms.core.utils.TimeoutTracker;
import org.opennms.netmgt.poller.Distributable;
import org.opennms.netmgt.poller.MonitoredService;
import org.opennms.netmgt.poller.NetworkInterface;
import org.opennms.netmgt.poller.NetworkInterfaceNotSupportedException;
import org.opennms.netmgt.poller.PollStatus;
import org.opennms.netmgt.poller.nrpe.CheckNrpe;
import org.opennms.netmgt.poller.nrpe.NrpeException;
import org.opennms.netmgt.poller.nrpe.NrpePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is designed to be used by the service poller framework to test the
 * availability of a generic TCP service on remote interfaces. The class
 * implements the ServiceMonitor interface that allows it to be used along with
 * other plug-ins by the service poller framework.
 *
 * @author <A HREF="mailto:dgregor@interhack.com">DJ Gregor</A>
 * @author <A HREF="mailto:tarus@opennms.org">Tarus Balog </A>
 * @author <A HREF="mike@opennms.org">Mike </A>
 */

@Distributable
public class NrpeMonitor extends AbstractServiceMonitor {
    private static final Logger LOG = LoggerFactory.getLogger(NrpeMonitor.class);

    /**
     * Default port.
     */
    //Commented out because it is not currently used in this monitor
    //private static final int DEFAULT_PORT = -1;

    /**
     * Default retries.
     */
    private static final int DEFAULT_RETRY = 0;

    /**
     * Default timeout. Specifies how long (in milliseconds) to block waiting
     * for data from the monitored interface.
     */
    private static final int DEFAULT_TIMEOUT = 3000; // 3 second timeout on
                                                        // read()
    
    /**
     * Whether to use SSL by default
     */
    private static final boolean DEFAULT_USE_SSL = true;
    
    /**
     * List of cipher suites to use when talking SSL to NRPE, which uses anonymous DH
     */
    private static final String[] ADH_CIPHER_SUITES = new String[] {"TLS_DH_anon_WITH_AES_128_CBC_SHA"};

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
    public PollStatus poll(MonitoredService svc, Map<String, Object> parameters) {
        NetworkInterface<InetAddress> iface = svc.getNetInterface();
		
		String reason = null;

        //
        // Get interface address from NetworkInterface
        //
        if (iface.getType() != NetworkInterface.TYPE_INET) {
            throw new NetworkInterfaceNotSupportedException("Unsupported interface type, only TYPE_INET currently supported");
        }

        TimeoutTracker tracker = new TimeoutTracker(parameters, DEFAULT_RETRY, DEFAULT_TIMEOUT);
        String command = ParameterMap.getKeyedString(parameters, "command", NrpePacket.HELLO_COMMAND);
        int port = ParameterMap.getKeyedInteger(parameters, "port", CheckNrpe.DEFAULT_PORT);
        int padding = ParameterMap.getKeyedInteger(parameters, "padding", NrpePacket.DEFAULT_PADDING);
        boolean useSsl = ParameterMap.getKeyedBoolean(parameters, "usessl", DEFAULT_USE_SSL);

		/*
        // Port
        //
        int port = ParameterMap.getKeyedInteger(parameters, "port", DEFAULT_PORT);
        if (port == DEFAULT_PORT) {
            throw new RuntimeException("NrpeMonitor: required parameter 'port' is not present in supplied properties.");
        }
        */

        // BannerMatch
        //
        //Commented out because it is not currently referenced in this monitor
        //String strBannerMatch = (String) parameters.get("banner");

        // Get the address instance.
        //
        InetAddress ipv4Addr = (InetAddress) iface.getAddress();

        final String hostAddress = InetAddressUtils.str(ipv4Addr);
        LOG.debug("poll: address = {}, port = {}, {}", hostAddress, port, tracker);

        // Give it a whirl
        //
        int serviceStatus = PollStatus.SERVICE_UNAVAILABLE;
        Double responseTime = null;

        for (tracker.reset(); tracker.shouldRetry() && serviceStatus != PollStatus.SERVICE_AVAILABLE; tracker.nextAttempt()) {
            Socket socket = null;
            try {
                //
                // create a connected socket
                //
                tracker.startAttempt();

                socket = new Socket();
                socket.connect(new InetSocketAddress(ipv4Addr, port), tracker.getConnectionTimeout());
                socket.setSoTimeout(tracker.getSoTimeout());
                LOG.debug("NrpeMonitor: connected to host: {} on port: {}", ipv4Addr, port);
                
            	reason = "Perhaps check the value of 'usessl' for this monitor against the NRPE daemon configuration";
                socket = wrapSocket(socket, useSsl);

                // We're connected, so upgrade status to unresponsive
                serviceStatus = PollStatus.SERVICE_UNRESPONSIVE;
            	reason = "Connected successfully, but no response received";

				NrpePacket p = new NrpePacket(NrpePacket.QUERY_PACKET, (short) 0,
						command);
				byte[] b = p.buildPacket(padding);
				OutputStream o = socket.getOutputStream();
				o.write(b);

				/*
                if (strBannerMatch == null || strBannerMatch.length() == 0 || strBannerMatch.equals("*")) {

				if (true) {
                    serviceStatus = SERVICE_AVAILABLE;
                    // Store response time in RRD
                    if (responseTime >= 0 && rrdPath != null) {
                        try {
                            this.updateRRD(rrdPath, ipv4Addr, dsName, responseTime, pkg);
                        } catch (RuntimeException rex) {
                            LOG.debug("There was a problem writing the RRD: {}", rex);
                        }
                    }
                    break;
                }

                BufferedReader rdr = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                //
                // Tokenize the Banner Line, and check the first
                // line for a valid return.
                //
                String response = rdr.readLine();
                responseTime = System.currentTimeMillis() - sentTime;

                if (response == null)
                    continue;
                LOG.debug("poll: banner = {}", response);
                LOG.debug("poll: responseTime= {}ms", responseTime);

                if (response.indexOf(strBannerMatch) > -1) {
                */

				NrpePacket response = NrpePacket.receivePacket(socket.getInputStream(), padding);
                responseTime = tracker.elapsedTimeInMillis();
				if (response.getResultCode() == 0) {
                    serviceStatus = PollStatus.SERVICE_AVAILABLE;
                    reason = null;
                } else {
                    serviceStatus = PollStatus.SERVICE_UNAVAILABLE;
					reason = "NRPE command returned code " + response.getResultCode() +
						" and message: " + response.getBuffer();
                }
            } catch (NoRouteToHostException e) {
                   reason = "No route to host exception for address " + hostAddress;
                   LOG.warn("poll: {}", reason, e);
            } catch (InterruptedIOException e) {
                reason = "did not connect to host within " + tracker;
                LOG.debug("NrpeMonitor: did not connect to host within {}", tracker);
            } catch (ConnectException e) {
				reason = "Connection exception for address: " + ipv4Addr;
                // Connection refused. Continue to retry.
                //
                if (LOG.isDebugEnabled()) {
	                e.fillInStackTrace();
                    LOG.debug("poll: {}", reason, e);
                }
            } catch (NrpeException e) {
				reason = "NrpeException while polling address: " + ipv4Addr;
                if (LOG.isDebugEnabled()) {
	                e.fillInStackTrace();
                    LOG.debug("poll: {}", reason, e);
                }
            } catch (IOException e) {
                // Ignore
				reason = "IOException while polling address: " + ipv4Addr;
                if (LOG.isDebugEnabled()) {
	                e.fillInStackTrace();
                    LOG.debug("poll: {}", reason, e);
                }
            } finally {
                try {
                    // Close the socket
                    if (socket != null) {
                        socket.close();
                    }
                } catch (IOException e) {
                    if (LOG.isDebugEnabled()) {
	                    e.fillInStackTrace();
                        LOG.debug("poll: Error closing socket.", e);
                    }
                }
            }
        }

        //
        // return the status of the service
        //
        if (reason == null) {
            return PollStatus.get(serviceStatus, responseTime);
        } else {
            return PollStatus.get(serviceStatus, reason);
        }
    }
    
    /**
     * <p>wrapSocket</p>
     *
     * @param socket a {@link java.net.Socket} object.
     * @param useSsl a boolean.
     * @return a {@link java.net.Socket} object.
     * @throws java.io.IOException if any.
     */
    protected Socket wrapSocket(Socket socket, boolean useSsl) throws IOException {
    	if (!useSsl) {
    		return socket;
    	} else {
    	    // Set this socket to use anonymous Diffie-Hellman ciphers. This removes the authentication
    	    // benefits of SSL, but it's how NRPE rolls so we have to play along.
    	    return SocketUtils.wrapSocketInSslContext(socket, ADH_CIPHER_SUITES);
    	}
    	
    }
}
