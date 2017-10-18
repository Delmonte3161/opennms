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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.opennms.netmgt.config.southbound.SouthCluster;
import org.opennms.netmgt.config.southbound.SouthElement;
import org.opennms.netmgt.dao.southbound.SouthboundConfigDao;
import org.opennms.netmgt.provision.persist.AbstractRequisitionProvider;
import org.opennms.netmgt.provision.persist.ForeignSourceRepository;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/*
 * Cisco ACI Provisioning
 * 
 * author mp050407
 */
public class AciRequisitionProvider extends AbstractRequisitionProvider<AciImportRequest> {

	private static final Logger logger = LoggerFactory.getLogger(AciRequisitionProvider.class);

	public static final String TYPE_NAME = "aci";
	
	private SouthboundConfigDao southboundConfigDao;

	@Autowired
	@Qualifier("fileDeployed")
	private ForeignSourceRepository foreignSourceRepository;

	public AciRequisitionProvider(Class<AciImportRequest> clazz) {
		super(clazz);
		logger.debug("AciRequisitionProvider::AciRequisitionProvider");
	}

	public AciRequisitionProvider() {
		super(AciImportRequest.class);
	}

	@Override
	public String getType() {
		return TYPE_NAME;
	}

	@Override
	public AciImportRequest getRequest(Map<String, String> parameters) {
		logger.debug("AciRequisitionProvider::getRequest");
	
		final AciImportRequest request = new AciImportRequest(parameters);
		if (StringUtils.isBlank(request.getUsername()) ||
	                StringUtils.isBlank(request.getPassword()) ||
	                StringUtils.isBlank(request.getHostname())) {

	            // No host or credentials were specified in the parameter map, attempt to look these up
		    List<SouthCluster> clusters = this.southboundConfigDao.getSouthboundClusters();
		        for (SouthCluster southCluster : clusters) {
		            if (southCluster.getClusterType().equals("CISCO-ACI") &&
		                    request.getForeignSource().equals(southCluster.getClusterName())) {
		                //TODO - Need to determine which foreign-source this request is for.
		                //Build URL
		                String url = "";
		                String username = "";
		                String password = "";
		                String location = southCluster.getLocation();
		                
		                if (location != null)
		                    request.setLocation(location);
		                
		                List<SouthElement> elements = southCluster.getElements();
		                for (SouthElement element : elements ){
		                    url += "https://" + element.getHost() + ":"  + element.getPort() + ",";
		                    username = element.getUserid();
		                    password = element.getPassword();
		                }
		                // We found a corresponding entry - copy the credentials to the request
		                request.setApicUrl(url);
		                request.setUsername(username);
		                request.setPassword(password);
		            }
		        }
		}
	        // Lookup the existing requisition, and store it in the request
	        final Requisition existingRequisition = getExistingRequisition(request.getForeignSource());
	        request.setExistingRequisition(existingRequisition);
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
	public Requisition getRequisitionFor(AciImportRequest request) {
		logger.debug("AciRequisitionProvider::getRequisitionFor");
		logger.debug("reuest.getForeignSource(): " + request.getForeignSource());
		final AciImporter importer = new AciImporter(request);
		return importer.getRequisition();
	}

    public SouthboundConfigDao getSouthboundConfigDao() {
        return southboundConfigDao;
    }

    public void setSouthboundConfigDao(SouthboundConfigDao southboundConfigDao) {
        this.southboundConfigDao = southboundConfigDao;
    }
	

}