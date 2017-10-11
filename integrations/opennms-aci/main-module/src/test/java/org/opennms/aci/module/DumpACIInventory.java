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

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opennms.aci.rpc.rest.client.ACIRestClient;
import org.opennms.netmgt.config.southbound.SouthCluster;
import org.opennms.netmgt.config.southbound.SouthElement;
import org.opennms.netmgt.dao.jaxb.southbound.DefaultSouthboundConfigDao;
import org.opennms.netmgt.dao.southbound.SouthboundConfigDao;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;

/**
 * @author tf016851
 */
public class DumpACIInventory {

    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private SouthboundConfigDao dao;

    public DumpACIInventory() {
        Resource resource = new PathResource("target/classes/etc/southbound-configuration.xml");

        dao = new DefaultSouthboundConfigDao();
        ((DefaultSouthboundConfigDao) dao).setConfigResource(resource);
        ((DefaultSouthboundConfigDao) dao).afterPropertiesSet();
    }

    public void dumpAll() {
        try {
            Resource resource = new PathResource("target/classes/etc/southbound-configuration.xml");

            dao = new DefaultSouthboundConfigDao();
            ((DefaultSouthboundConfigDao) dao).setConfigResource(resource);
            ((DefaultSouthboundConfigDao) dao).afterPropertiesSet();

            List<SouthCluster> clusters = dao.getSouthboundClusters();

            final java.util.Calendar startCal = GregorianCalendar.getInstance();
            startCal.add(GregorianCalendar.MINUTE, -100);

            for (SouthCluster southCluster : clusters) {

                if (southCluster.getClusterType().equals("CISCO-ACI")) {
                    String location = southCluster.getClusterName();
                    // Build URL
                    String url = "";
                    String username = "";
                    String password = "";
                    List<SouthElement> elements = southCluster.getElements();
                    for (SouthElement element : elements) {
                        url += "https://" + element.getHost() + ":"
                                + element.getPort() + ",";
                        username = element.getUserid();
                        password = element.getPassword();
                    }
                    ACIRestClient client = ACIRestClient.newAciRest(location, url,
                                                                    username,
                                                                    password);
                    System.out.println(
                           "foreignSource, Node_Label, IP_Address, MgmtType_SNMP, ID_Key, InterfaceStatus, cat_1, cat_2, svc_1, svc_2, svc_3"
                                      );

                    JSONArray results = client.getClassInfo(true,
                                                            "topSystem");
                    for (Object object : results) {
                        JSONObject objectData = (JSONObject) object;
                        if (objectData == null)
                            continue;
                        for (Object object2 : objectData.keySet()) {
                            String key = (String) object2;
                            JSONObject classData = (JSONObject) objectData.get(key);
                            JSONObject attributes = (JSONObject) classData.get("attributes");
                            if (attributes == null)
                                continue;

                            String role = (String) attributes.get("role");
                            String row = location + ", " + 
                                      (String) attributes.get("name") + ", " + 
                                      (String) attributes.get("oobMgmtAddr") + ", " + 
                                      "P, " + 
                                      (String) attributes.get("dn") + ", " + 
                                      "1, " + 
                                      role + ", " +
                                      "ACI, "
                                      ;
                            if ("controller".equals(role)){
                                row += "ICMP, SSH, ";
                            } else {
                                
                                row += "SNMP, ICMP, SSH";
                            }
                            System.out.println( row);
                        }
                    }
                }
            }

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        DumpACIInventory aci = new DumpACIInventory();
        
        aci.dumpAll();
    }
}
