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

package org.opennms.netmgt.trapd;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.opennms.core.concurrent.LogPreservingThreadFactory;
import org.opennms.core.logging.Logging;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.config.SyslogdConfig;
import org.opennms.netmgt.snmp.SnmpUtils;
import org.opennms.netmgt.snmp.SnmpV3User;
import org.opennms.netmgt.snmp.TrapNotification;
import org.opennms.netmgt.snmp.TrapNotificationListener;
import org.opennms.netmgt.snmp.TrapProcessor;
import org.opennms.netmgt.snmp.TrapProcessorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author <a href="mailto:weave@oculan.com">Brian Weaver</a>
 * @author <a href="http://www.oculan.com">Oculan Corporation</a>
 * @fiddler joed
 */
public class TrapReceiverSnmp4jImpl implements TrapReceiver, TrapNotificationListener,TrapProcessorFactory {
    private static final Logger LOG = LoggerFactory.getLogger(TrapReceiverSnmp4jImpl.class);

    private static final int SOCKET_TIMEOUT = 500;

    /**
     * The thread pool that processes traps
     */
    private ExecutorService m_backlogQ;

    /**
     * The queue processing thread
     */
    @Autowired
    private TrapQueueProcessorFactory m_processorFactory;
    
    @Resource(name="snmpTrapAddress")
    private String m_snmpTrapAddress;

    @Resource(name="snmpTrapPort")
    private Integer m_snmpTrapPort;

    @Resource(name="snmpV3Users")
    private List<SnmpV3User> m_snmpV3Users;
    
    private boolean m_registeredForTraps;
    
    /**
     * The Fiber's status.
     */
    private volatile boolean m_stop;

    /**
     * The UDP socket for receipt and transmission of packets from agents.
     */
    private DatagramSocket m_dgSock;

    /**
     * The context thread
     */
    private Thread m_context;

    private final SyslogdConfig m_config;

    private List<TrapNotificationHandler> m_trapNotificationHandlers = Collections.emptyList();

    private final ExecutorService m_executor;

    /**
     * construct a new receiver
     *
     * @param sock
     * @param matchPattern
     * @param hostGroup
     * @param messageGroup
     */
    public TrapReceiverSnmp4jImpl(final SyslogdConfig config) throws SocketException {
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }

        m_stop = false;
        m_dgSock = null;
        m_config = config;

        m_executor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 2,
            Runtime.getRuntime().availableProcessors() * 2,
            1000L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(),
            new LogPreservingThreadFactory(getClass().getSimpleName(), Integer.MAX_VALUE)
        );
    }

    public TrapNotificationHandler getSyslogConnectionHandlers() {
        return m_trapNotificationHandlers.get(0);
    }

    public void setSyslogConnectionHandlers(TrapNotificationHandler handler) {
        m_trapNotificationHandlers = Collections.singletonList(handler);
    }

    @Override
    public String getName() {
        String listenAddress = (m_config.getListenAddress() != null && m_config.getListenAddress().length() > 0) ? m_config.getListenAddress() : "0.0.0.0";
        return getClass().getSimpleName() + " [" + listenAddress + ":" + m_config.getSyslogPort() + "]";
    }

    /*
     * stop the current receiver
     * @throws InterruptedException
     * 
     */
    @Override
    public void stop() throws InterruptedException {
        m_stop = true;

        // Close the datagram socket
        if (m_dgSock != null) {
            m_dgSock.close();
        }

        // Shut down the thread pools that are executing SyslogConnection and SyslogProcessor tasks
        m_executor.shutdown();

        if (m_context != null) {
            LOG.debug("Stopping and joining thread context {}", m_context.getName());
            m_context.interrupt();
            m_context.join();
            LOG.debug("Thread context stopped and joined");
        }
    }

    /**
     * The execution context.
     */
    @Override
    public void run() {
        // get the context
        m_context = Thread.currentThread();

        // Get a log instance
        Logging.putPrefix(Syslogd.LOG4J_CATEGORY);

        if (m_stop) {
            LOG.debug("Stop flag set before thread started, exiting");
            return;
        } else
            LOG.debug("Thread context started");

        // allocate a buffer
        final int length = 0xffff;
        final byte[] buffer = new byte[length];

        try {
            LOG.debug("Creating syslog socket");
            m_dgSock = new DatagramSocket(null);
        } catch (SocketException e) {
            LOG.warn("Could not create syslog socket: " + e.getMessage(), e);
            return;
        }

        // set an SO timeout to make sure we don't block forever
        // if a socket is closed.
        try {
            LOG.debug("Setting socket timeout to {}ms", SOCKET_TIMEOUT);
            m_dgSock.setSoTimeout(SOCKET_TIMEOUT);
        } catch (SocketException e) {
            LOG.warn("An I/O error occured while trying to set the socket timeout", e);
        }

        // Set SO_REUSEADDR so that we don't run into problems in
        // unit tests trying to rebind to an address where other tests
        // also bound. This shouldn't have any effect at runtime.
        try {
            LOG.debug("Setting socket SO_REUSEADDR to true");
            m_dgSock.setReuseAddress(true);
        } catch (SocketException e) {
            LOG.warn("An I/O error occured while trying to set SO_REUSEADDR", e);
        }

        // Increase the receive buffer for the socket
        try {
            LOG.debug("Attempting to set receive buffer size to {}", Integer.MAX_VALUE);
            m_dgSock.setReceiveBufferSize(Integer.MAX_VALUE);
            LOG.debug("Actual receive buffer size is {}", m_dgSock.getReceiveBufferSize());
        } catch (SocketException e) {
            LOG.info("Failed to set the receive buffer to {}", Integer.MAX_VALUE, e);
        }

        try {
            LOG.debug("Opening datagram socket");
            if (m_config.getListenAddress() != null && m_config.getListenAddress().length() != 0) {
                m_dgSock.bind(new InetSocketAddress(InetAddressUtils.addr(m_config.getListenAddress()), m_config.getSyslogPort()));
            } else {
                m_dgSock.bind(new InetSocketAddress(m_config.getSyslogPort()));
            }
        } catch (SocketException e) {
            LOG.info("Failed to open datagram socket", e);
        }

        // set to avoid numerous tracing message
        boolean ioInterrupted = false;

        // Construct one mutable {@link DatagramPacket} that will be used for receiving syslog messages 
        DatagramPacket pkt = new DatagramPacket(buffer, length);

        // now start processing incoming requests
        while (!m_stop) {
            if (m_context.isInterrupted()) {
                LOG.debug("Thread context interrupted");
                break;
            }

            try {
                if (!ioInterrupted) {
                    LOG.debug("Waiting on a datagram to arrive");
                }

                m_dgSock.receive(pkt);

                SyslogConnection connection = new SyslogConnection(pkt, m_config);

                try {
                    for (TrapNotificationHandler handler : m_trapNotificationHandlers) {
                      //  handler.handleTrapNotification(connection);
                    }
                } catch (Throwable e) {
                    LOG.error("Handler execution failed in {}", this.getClass().getSimpleName(), e);
                }

                ioInterrupted = false; // reset the flag
            } catch (SocketTimeoutException e) {
                ioInterrupted = true;
                continue;
            } catch (InterruptedIOException e) {
                ioInterrupted = true;
                continue;
            } catch (IOException e) {
                if (m_stop) {
                    // A SocketException can be thrown during normal shutdown so log as debug
                    LOG.debug("Shutting down the datagram receipt port: " + e.getMessage());
                } else {
                    LOG.error("An I/O exception occured on the datagram receipt port, exiting", e);
                }
                break;
            }

        } // end while status OK

        LOG.debug("Thread context exiting");

    }

	@Override
	public void trapReceived(TrapNotification trapNotification) {
		m_backlogQ.submit(m_processorFactory.getInstance(trapNotification));
		
	}

	@Override
	public void trapError(int error, String msg) {
      LOG.warn("Error Processing Received Trap: error = {} {}", error, (msg != null ? ", ref = " + msg : ""));
	}
	
	public void registeredForTraps(){
        try {
        	InetAddress address = getInetAddress();
        	LOG.info("Listening on {}:{}", address == null ? "[all interfaces]" : InetAddressUtils.str(address), m_snmpTrapPort);
            SnmpUtils.registerForTraps(this, this, address, m_snmpTrapPort, m_snmpV3Users); // Need to clarify 
            m_registeredForTraps = true;
            
            LOG.debug("init: Creating the trap session");
        } catch (final IOException e) {
            if (e instanceof java.net.BindException) {
                Logging.withPrefix("OpenNMS.Manager", new Runnable() {
                    @Override
                    public void run() {
                        LOG.error("init: Failed to listen on SNMP trap port, perhaps something else is already listening?", e);
                    }
                });
                LOG.error("init: Failed to listen on SNMP trap port, perhaps something else is already listening?", e);
            } else {
                LOG.error("init: Failed to initialize SNMP trap socket", e);
            }
            throw new UndeclaredThrowableException(e);
        }
	}
	
	public void unRegisteredForTraps(){
        try {
            if (m_registeredForTraps) {
                LOG.debug("stop: Closing SNMP trap session.");
                SnmpUtils.unregisterForTraps(this, getInetAddress(), m_snmpTrapPort);
                LOG.debug("stop: SNMP trap session closed.");
            } else {
                LOG.debug("stop: not attemping to closing SNMP trap session--it was never opened");
            }

        } catch (final IOException e) {
            LOG.warn("stop: exception occurred closing session", e);
        } catch (final IllegalStateException e) {
            LOG.debug("stop: The SNMP session was already closed", e);
        }
	}
	
    public ExecutorService getM_backlogQ() {
		return m_backlogQ;
	}

	public void setM_backlogQ(ExecutorService m_backlogQ) {
		this.m_backlogQ = m_backlogQ;
	}

	public TrapQueueProcessorFactory getM_processorFactory() {
		return m_processorFactory;
	}

	public void setM_processorFactory(TrapQueueProcessorFactory m_processorFactory) {
		this.m_processorFactory = m_processorFactory;
	}

	private InetAddress getInetAddress() {
    	if (m_snmpTrapAddress.equals("*")) {
    		return null;
    	}
		return InetAddressUtils.addr(m_snmpTrapAddress);
    }

	@Override
	public TrapProcessor createTrapProcessor() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
