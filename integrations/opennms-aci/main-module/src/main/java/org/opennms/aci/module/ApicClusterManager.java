/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
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

package org.opennms.aci.module;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslContextConfigurator;
import org.glassfish.tyrus.client.SslEngineConfigurator;
import org.json.simple.JSONObject;
import org.opennms.aci.rpc.rest.client.ACIRestClient;
import org.opennms.core.logging.Logging;
import org.opennms.netmgt.config.southbound.SouthCluster;
import org.opennms.netmgt.config.southbound.SouthElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tf016851
 *
 */
public class ApicClusterManager implements Runnable {
    
    private static final Logger LOG = LoggerFactory.getLogger(ApicClusterManager.class);
    
    private final ApicEventForwader apicEventForwader;
    private final SouthCluster southCluster;
    public final String clusterUrl;
    private final ACIRestClient aciClient;
    private final ClientManager client;
    public final boolean hostVerficationEnabled;
    public final String clusterName;
    
    private NodeCache nodeCache;
    
    private boolean shutdown = false;
    
    private boolean connectionOpen = false;
    private String subscriptionId =  null;
    
    private Session session = null;
    

    /**
     * Default Constructor
     * @param cluster
     * @throws Exception
     */
    public ApicClusterManager(ApicEventForwader apicEventForwader, SouthCluster cluster) throws Exception {
        this(apicEventForwader, cluster, false);
    }

    /**
     * Constructor with SSL HostVerification flag.
     * @param cluster
     * @param hostVerificationEnabled
     * @throws Exception
     */
    public ApicClusterManager(ApicEventForwader apicEventForwader, SouthCluster cluster, boolean hostVerificationEnabled) throws Exception {
        this.southCluster = cluster;
        this.apicEventForwader = apicEventForwader;
        this.hostVerficationEnabled = hostVerificationEnabled;
        this.clusterName = cluster.getClusterName();
        this.client = ClientManager.createClient();

        if (!this.hostVerficationEnabled) {
            SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(new SslContextConfigurator());
            sslEngineConfigurator.setHostVerificationEnabled(false);
            client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR,
                                       sslEngineConfigurator);
        }
        
//        Logging.putPrefix("aci");
        
        List<SouthElement> elements = southCluster.getElements();
        String url = "";
        String username = "";
        String password = "";
        for (SouthElement element : elements ){
            url += "https://" + element.getHost() + ":"  + element.getPort() + ",";
            username = element.getUserid();
            password = element.getPassword();
        }
        this.clusterUrl = url;
        
        nodeCache = new NodeCache();
        
        this.aciClient = ACIRestClient.newAciRest( cluster.getClusterName(), clusterUrl, username, password );

    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        LOG.debug("Starting ApicClusterManager for: " + clusterName);
        System.out.println("ACI: Starting ApicClusterManager for: " + clusterName);

        try {
            long now = System.currentTimeMillis();
            
            while (!shutdown) {
                //Check if connection is open, if not reconnect
                if (session == null || !session.isOpen() || !this.connectionOpen) 
                    subscriptionId = this.connectAndSubscribeToFaults();

                //Currently both Subscription and Token expire every 60 seconds
                if ((System.currentTimeMillis() - now) > 30000 && this.subscriptionId != null) {
                    //Do refresh on client session
                    aciClient.runQueryNoAuth("/api/aaaRefresh.json");
                    //Do refresh on subscription
                    aciClient.runQueryNoAuth("/api/subscriptionRefresh.json?id=" + subscriptionId);
                    now = System.currentTimeMillis();
                }
                Thread.sleep(1000);
            }
            
            LOG.debug("Stopping websocket client for " + this.clusterName);
            System.out.println("ACI: Stopping websocket client for " + this.clusterName);
            client.shutdown();
        } catch (Throwable e) {
            LOG.error("APIC websocket exception", e);
//            e.printStackTrace();
            this.connectionOpen = false;
        }
    }
    
    public void stop() {
        LOG.debug("Shutting down " + this.clusterName);
        System.out.println("ACI: Shutting down " + this.clusterName);
        this.shutdown = true;
    }

    private String connectAndSubscribeToFaults() throws Exception {
        LOG.debug("Starting websocket client for: " + clusterName);
        System.out.println("ACI: Starting websocket client for: " + clusterName);
        
        final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

        session = client.connectToServer(new Endpoint() {

            @Override
            public void onOpen(Session session, EndpointConfig config) {
                try {
                    session.addMessageHandler(new MessageHandler.Whole<String>() {

                        ExecutorService execService = Executors.newFixedThreadPool(10);

                        @Override
                        public void onMessage(String message) {
                            if (message == null)
                                return;
//                            System.out.println("Received message: "+message);
                            Runnable runnableTask = () -> {
                                apicEventForwader.sendEvent(clusterName, aciClient.getHost(), message);
                            };
                            
                            execService.execute(runnableTask);
                        }

                    });
                    connectionOpen = true;
                } catch (Exception e) {
                    LOG.error("Failed to connect to server.", e);
//                    e.printStackTrace();
                }
            }
            
            @Override
            public void onClose(Session session, CloseReason closeReason) {
                LOG.info("ACI: Websocket connection closed: " + closeReason.getReasonPhrase());
                System.out.println("ACI: Websocket connection closed: " + closeReason.getReasonPhrase());
                connectionOpen = false;
            }
            
            @Override
            public void onError(Session session, Throwable thr) {
                LOG.error("ACI: Websocket connection error: ", thr);
                System.out.println("ACI: Websocket connection error: ");
                thr.printStackTrace();
                connectionOpen = false;
            }

        }, cec, new URI("wss://"+ this.aciClient.getHost() + "/socket" + this.aciClient.getToken()));
        
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        final java.util.Calendar startCal = GregorianCalendar.getInstance();
        String formattedTime = format.format(startCal.getTime());

//        String query = "/api/node/class/faultInfo.json?subscription=yes";
        String query = "/api/node/class/faultRecord.json?query-target-filter=gt(faultRecord.created, \"" + formattedTime + "\")&subscription=yes";
        LOG.debug("Subscribing to query: " + query);
//        System.out.println("ACI: Subscribing to query: " + query);
        long now = System.currentTimeMillis();
        JSONObject result = (JSONObject) aciClient.runQueryNoAuth(query);
        return (String)result.get("subscriptionId");
    }
}
