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
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opennms.aci.rpc.rest.client.ACIRestClient;
import org.opennms.netmgt.model.PrimaryType;
import org.opennms.netmgt.provision.persist.AbstractRequisitionProvider;
import org.opennms.netmgt.provision.persist.ForeignSourceRepository;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.netmgt.provision.persist.requisition.RequisitionInterface;
import org.opennms.netmgt.provision.persist.requisition.RequisitionMonitoredService;
import org.opennms.netmgt.provision.persist.requisition.RequisitionNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/*
 * Cisco ACI Provisioning
 * 
 * author mp050407
 */
public class AciRequisitionProvider extends AbstractRequisitionProvider<AciRequisitionRequest> {

	private static final Logger logger = LoggerFactory.getLogger(AciRequisitionProvider.class);

	public static final String TYPE_NAME = "aci";

	@Autowired
	@Qualifier("fileDeployed")
	private ForeignSourceRepository foreignSourceRepository;

	public AciRequisitionProvider(Class<AciRequisitionRequest> clazz) {
		super(clazz);
		logger.debug("AciRequisitionProvider::AciRequisitionProvider");
	}

	public AciRequisitionProvider() {
		super(AciRequisitionRequest.class);
	}

	@Override
	public String getType() {
		return TYPE_NAME;
	}

	@Override
	public AciRequisitionRequest getRequest(Map<String, String> parameters) {
		logger.debug("AciRequisitionProvider::getRequest");
		parameters.put("location", "LS6");
		parameters.put("username", "svcOssAci");
		parameters.put("password", "kf3as=Nx");
		
		final AciRequisitionRequest request = new AciRequisitionRequest(parameters);
		request.setForeignSource("LS6-APIC");
		
//		final Requisition existingRequisition  = getExistingRequisition(request.getForeignSource());
//		if (existingRequisition != null) {
//			request.setExistingRequisition(existingRequisition);
//		}
		return request;
	}

	protected Requisition getExistingRequisition(String foreignSource) {
		try {
			return foreignSourceRepository.getRequisition(foreignSource);
		} catch (Exception e) {
			logger.warn("Can't retrieve requisition {}", foreignSource, e);
			return null;
		}
	}

	@Override
	public Requisition getRequisitionFor(AciRequisitionRequest request) {
		logger.debug("AciRequisitionProvider::getRequisitionFor");
		logger.debug("reuest.getForeignSource(): " + request.getForeignSource());
		final AciImporter importer = new AciImporter(request);
		URL apicUrl = null;
		try {
			apicUrl = new URL(request.getApicUrl());
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}

		Requisition aciRequisiton = new Requisition(request.getForeignSource());
		ACIRestClient client = null;
		try {
			client = ACIRestClient.newAciRest(request.getForeignSource(), request.getApicUrl(), request.getUsername(),
					request.getPassword());
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.debug("sending get for top system");
		JSONObject obj = null;
		try {
			obj = (JSONObject) client.get(apicUrl.getPath());
		} catch (Exception e) {
			e.printStackTrace();
		}
		JSONArray msg = (JSONArray) obj.get("imdata");
		@SuppressWarnings("unchecked")
		Iterator<JSONObject> iterator = msg.iterator();
		while (iterator.hasNext()) {
			// JSONObject attr = (JSONObject) ((JSONObject)
			// iterator.next().get("topSystem")).get("attributes");
			JSONObject topSystem = (JSONObject) iterator.next().get("topSystem");
			JSONObject attr = (JSONObject) topSystem.get("attributes");
			aciRequisiton.insertNode(createRequisitionNode(request, attr));
			logger.debug("oobMgmtAddr " + attr.get("oobMgmtAddr"));
		}
		logger.debug("total count " + (String) obj.get("totalCount"));
		
		//return importer.getRequisition();
		return aciRequisiton;
	}
	
	private RequisitionNode createRequisitionNode(AciRequisitionRequest request, JSONObject aciNode) { 
		final RequisitionNode node = new RequisitionNode();
		node.setBuilding(request.getForeignSource());
		String dn = ((String) aciNode.get("dn")).replace('/', '-');
		node.setForeignId(dn);
		node.setNodeLabel((String)aciNode.get("name"));
		
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