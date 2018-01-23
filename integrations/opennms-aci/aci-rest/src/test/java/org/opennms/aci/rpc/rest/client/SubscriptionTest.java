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

package org.opennms.aci.rpc.rest.client;

import java.io.IOException;
import java.net.URI;
import java.security.cert.Certificate;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslContextConfigurator;
import org.glassfish.tyrus.client.SslEngineConfigurator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * @author tf016851
 *
 */
public class SubscriptionTest {

//    static {
//        HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier()
//            {
//                public boolean verify(String hostname, SSLSession session)
//                {
//                    // ip address of the service URL(like.23.28.244.244)
//                    //if (hostname.equals("23.28.244.244"))
//                        return true;
//                    //return false;
//                }
//            });
//    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        final String userName = args[0];
        final String pw = args[1];

        //LS2
      final String cluster = "ls2apic";
      final String url = "https://7.192.17.10,https://7.192.17.11,https://7.192.17.12,https://7.192.17.13,https://7.192.17.14";
        //LS6
//      final String cluster = "ls6apic";
//      final String url = "https://7.192.80.10,https://7.192.80.11,https://7.192.80.12";
//      final String url = "https://ls6apic1.cernerasp.com,https://ls6apic2.cernerasp.com,https://ls6apic3.cernerasp.com";
        //KC8
//        final String cluster = "kc8apic";
//        final String url = "https://7.192.240.10,https://7.192.240.11,https://7.192.240.12";
//        final String url = "https://kc8apic1.cernerasp.com,https://kc8apic2.cernerasp.com,https://kc8apic3.cernerasp.com";
//        final String cluster = "test";

//        final String url = "https://ls6apic1.cernerasp.com";
//        final String url = "https://bogus,https://bogus,https://7.192.80.12";
//        String authHeader;
//        String host;
//        DefaultHttpClient httpClient;
//        HttpContext httpContext;
//        String restUrlPrefix;
//        BasicCookieStore cookieStore;
//        
//        TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
        
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        
        final java.util.Calendar startCal = GregorianCalendar.getInstance();
        
        startCal.add(Calendar.MINUTE, -60);
        
        String formattedTime = format.format(startCal.getTime());
       

        try {
            ACIRestClient aciClient = ACIRestClient.newAciRest( cluster, url, userName, pw );
//            SSLSocketFactory sf = new SSLSocketFactory(acceptingTrustStrategy,
//                                                       SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
//            SchemeRegistry registry = new SchemeRegistry();
//            registry.register(new Scheme("https", 443, sf));
//            ClientConnectionManager ccm = new PoolingClientConnectionManager(registry);
//            httpClient = new DefaultHttpClient(ccm);
//
//            authHeader = "Basic " + Base64.encodeBase64String((username + ':'
//                    + password).getBytes("UTF-8"));
//            httpContext = new BasicHttpContext();
//            cookieStore = new BasicCookieStore();
//            httpContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

            final ClientEndpointConfig cec = ClientEndpointConfig.Builder.create().build();

            ClientManager client = ClientManager.createClient();
            SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(new SslContextConfigurator());
            sslEngineConfigurator.setHostVerificationEnabled(false);
            client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
            
//            SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(new SslContextConfigurator());
//            sslEngineConfigurator.setHostnameVerifier(new HostnameVerifier() {
//                @Override
//                public boolean verify(String host, SSLSession sslSession) {
////                    Certificate certificate = sslSession.getPeerCertificates()[0];
//                    // validate the host in the certificate
//                    return true;
//                }
//            });
            client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
            
            client.connectToServer(new Endpoint() {

                @Override
                public void onOpen(Session session, EndpointConfig config) {
                    try {
                        session.addMessageHandler(new MessageHandler.Whole<String>() {

                            @Override
                            public void onMessage(String message) {
                                System.out.println("Received message: "+message);
                            }
                        });
                        //session.getBasicRemote().sendText(SENT_MESSAGE);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, cec, new URI("wss://"+ aciClient.getHost() + "/socket" + aciClient.getToken()));
            
            //Next, query subscription and store subscriptionId
//            String query = "/api/node/class/faultInfo.json?subscription=yes";
            String query = "/api/node/class/faultRecord.json?query-target-filter=gt(faultRecord.created, \"" + formattedTime + "\")&subscription=yes";
            long now = System.currentTimeMillis();
            JSONObject result = (JSONObject) aciClient.runQueryNoAuth(query);
            String subscriptionId = (String)result.get("subscriptionId");

            int count=0;
            while (subscriptionId != null) {
                //Currently both Subscription and Token expire every 60 seconds
                if ((System.currentTimeMillis() - now) > 30000) {
                    //Do refresh on client session
                    aciClient.runQueryNoAuth("/api/aaaRefresh.json");
                    //Do refresh on subscription
                    aciClient.runQueryNoAuth("/api/subscriptionRefresh.json?id=" + subscriptionId);
                    count = 0;
                    now = System.currentTimeMillis();
                }
                count++;
                System.out.println(count + ": Waiting ...");
                Thread.sleep(1000);
            }
            
            client.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
