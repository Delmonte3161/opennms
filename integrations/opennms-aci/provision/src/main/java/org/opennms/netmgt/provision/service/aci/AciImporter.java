/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2017 The OpenNMS Group, Inc.
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
package org.opennms.netmgt.provision.service.aci;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opennms.aci.rpc.rest.client.ACIRestClient;
import org.opennms.netmgt.model.PrimaryType;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.netmgt.provision.persist.requisition.RequisitionAsset;
import org.opennms.netmgt.provision.persist.requisition.RequisitionCategory;
import org.opennms.netmgt.provision.persist.requisition.RequisitionInterface;
import org.opennms.netmgt.provision.persist.requisition.RequisitionMonitoredService;
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * ACI Importer
 * @author mp050407
 */
public class AciImporter {
    
    private static final Logger logger = LoggerFactory.getLogger(AciImporter.class);
    
    private final AciImportRequest request;

    public AciImporter(AciImportRequest request) {
        this.request = request;
    }

    public Requisition getRequisition() {
        logger.debug("Getting existing requisition (if any) for APIC cluster {}",
                     request.getHostname());
        Requisition curReq = request.getExistingRequisition();

        logger.debug("Building new requisition for APIC cluster {}",
                     request.getHostname());
        Requisition newReq = buildAciRequisition();

        logger.debug("Finished building new requisition for APIC cluster {}",
                     request.getHostname());
        if (curReq == null) {
            if (newReq == null) {
                // FIXME Is this correct ? This is the old behavior
                newReq = new Requisition(request.getForeignSource());
            }
        } else {
            if (newReq == null) {
                // If there is a requisition and the vCenter is not responding for some reason, it is better to use the old requisition,
                // instead of returning an empty one, which can cause the lose of all the nodes from the DB.
                newReq = curReq;
            } else {
                // If there is already a requisition, retrieve the custom assets and categories from the old one, and put them on the new one.
                // The ACI related assets and categories will be preserved.
                for (RequisitionNode newNode : newReq.getNodes()) {
                    for (RequisitionNode curNode : curReq.getNodes()) {
                        if (newNode.getForeignId().equals(curNode.getForeignId())) {
                            // Add existing custom assets
                            for (RequisitionAsset asset : curNode.getAssets()) {
//                                if (!asset.getName().startsWith("vmware")) 
                                    newNode.putAsset(asset);
                            }
                            // Add existing custom categories
                            for (RequisitionCategory cat : curNode.getCategories()) {
//                                if (!cat.getName().startsWith("VMWare")) 
                                    newNode.putCategory(cat);
                            }
                            // Add existing custom services
                            /*
                             * For each interface on the new requisition,
                             * - Retrieve the list of custom services from the corresponding interface on the existing requisition,
                             *   matching the interface by the IP address
                             * - If the list of services is not empty, add them to the new interface
                             */
                            for (RequisitionInterface intf : curNode.getInterfaces()) {
                                List<RequisitionMonitoredService> services = getManualyConfiguredServices(intf);
                                if (!services.isEmpty()) {
                                    RequisitionInterface newIntf = getRequisitionInterface(newNode, intf.getIpAddr());
                                    if (newIntf != null) {
                                        newIntf.getMonitoredServices().addAll(services);
                                    } //end if
                                } //end if
                            } //end for
                        } //end if
                    } //end for
                } //end for
            } //end else
        } //end else
        
        return newReq;
    }

    private Requisition buildAciRequisition() {
//        URL apicUrl = null;
//        try {
//                apicUrl = new URL(request.getApicUrl());
//        } catch (MalformedURLException e1) {
//                e1.printStackTrace();
//        }

        Requisition aciRequisiton = new Requisition(request.getForeignSource());
        ACIRestClient client = null;
        try {
                client = ACIRestClient.newAciRest(request.getForeignSource(), request.getApicUrl(), request.getUsername(),
                                request.getPassword());
        } catch (Exception e) {
                e.printStackTrace();
        }
        logger.debug("sending get for top system");
        JSONArray results = null;
        try {
            results = client.getClassInfo( "topSystem" );
        } catch (Exception e) {
                e.printStackTrace();
        }
        for (Object object : results) {
            JSONObject objectData = (JSONObject) object;
            if (objectData == null)
                continue;
            logger.debug("total count " + (String) objectData.get("totalCount"));
            for (Object object2 : objectData.keySet()) {
                String key = (String) object2;
                JSONObject classData = (JSONObject) objectData.get(key);
                JSONObject attributes = (JSONObject) classData.get("attributes");
                if (attributes == null)
                    continue;

                
                RequisitionNode node = createRequisitionNode(request, attributes);
                aciRequisiton.insertNode(node);
            }
        }

        return aciRequisiton;
    }
    
    private RequisitionInterface getRequisitionInterface(RequisitionNode node, String ipAddr) {
        for (RequisitionInterface intf : node.getInterfaces()) {
            if (ipAddr.equals(intf.getIpAddr())) {
                return intf;
            }
        }
        return null;
    }

    private List<RequisitionMonitoredService> getManualyConfiguredServices(RequisitionInterface intf) {
        List<RequisitionMonitoredService> services = new ArrayList<>();
        for (RequisitionMonitoredService svc : intf.getMonitoredServices()) {
            boolean found = false;
            INNER:for (String svcName : request.DEFAULT_SERVICES) {
                if (svcName.trim().equals(svc.getServiceName())) {
                    found = true;
                    break INNER;
                }
            }
            if (!found) {
                services.add(svc);
            }
        }
        return services;
    }

    private RequisitionNode createRequisitionNode(AciImportRequest request,
            JSONObject aciNode) {
        final RequisitionNode node = new RequisitionNode();
        if (request.getLocation() != null) {
            node.setBuilding(request.getLocation());
            node.putCategory(new RequisitionCategory(request.getLocation()));
        } else
            node.setBuilding(request.getForeignSource());
        String role = (String) aciNode.get("role");
        String dn = (String) aciNode.get("dn");
        String[] dnParts = dn.split("/");
//        dn = dn.replace("/", "_");
        dn = dnParts[0] + "_" + dnParts[1] + "_" + dnParts[2];
        node.setForeignId(dn);
        node.setNodeLabel((String) aciNode.get("name"));
        
        node.putCategory(new RequisitionCategory("ACI"));
        node.putCategory(new RequisitionCategory(role));

        final RequisitionInterface iface = new RequisitionInterface();
        iface.setDescr("ACI-" + (String) aciNode.get("dn"));
        iface.setIpAddr((String) aciNode.get("oobMgmtAddr"));
        iface.setSnmpPrimary(PrimaryType.PRIMARY);
        iface.setManaged(Boolean.TRUE);
        iface.setStatus(Integer.valueOf(1));

        for (String service : request.getServices()) {
            service = service.trim();
            iface.insertMonitoredService(new RequisitionMonitoredService(service));
            logger.debug("Adding {} service to the interface", service);
        }

        node.putInterface(iface);
        return node;
    }
}
