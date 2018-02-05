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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.opennms.netmgt.model.OnmsSeverity;
import org.opennms.netmgt.model.events.EventBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tf016851
 *
 */
public class ConvertToEvent {
    
    private static final Logger LOG = LoggerFactory.getLogger(ConvertToEvent.class);
    
    private static final String ACI_UEI_PART = "uei.opennms.org/cisco/aci/";
    
    private static final Map<String, OnmsSeverity> SEVERITY_MAP;
    
    static
    {
        SEVERITY_MAP = new HashMap<String, OnmsSeverity>();
        SEVERITY_MAP.put("critical", OnmsSeverity.CRITICAL);
        SEVERITY_MAP.put("major", OnmsSeverity.MAJOR);
        SEVERITY_MAP.put("minor", OnmsSeverity.MINOR);
        SEVERITY_MAP.put("warning", OnmsSeverity.WARNING);
        SEVERITY_MAP.put("info", OnmsSeverity.NORMAL);
        SEVERITY_MAP.put("cleared", OnmsSeverity.CLEARED);
    }

    
    public static final EventBuilder toEventBuilder(NodeCache nodeCache, String location, Date createDate, JSONObject attributes, String apicHost) {
        
        if (attributes == null || attributes.size() == 0)
            return null;
        
        EventBuilder bldr = new EventBuilder();
        
        Long nodeId = (long) 0;
        LOG.trace("Building Event for " + location + " message: " + attributes.toJSONString());
        //First, let's add all Fault attributes as parameters.
        for (Object obj : attributes.keySet()) { 
            String key = (String)obj;
            String value = (String)attributes.get(obj);
            bldr.addParam(key, value);
        }
        String dn = (String) attributes.get("affected");
        if (dn == null)
            dn = (String) attributes.get("dn");
        String[] dnParts = dn.split(ApicService.DN_SEP);
        
        if (dnParts[0].equals("topology") && dnParts.length >= 3) {
            //Device Fault
            String key = location + ApicService.FS_SEP + dnParts[0] + "_" + dnParts[1] 
                    + "_" + dnParts[2];
//                    + "_" + dnParts[2] + "_" + dnParts[3];
            nodeId = nodeCache.getNodeId(key);
            if (nodeId != null)
                bldr.setNodeid(nodeId);
//        } else if (dnParts[0].equals("dbgs")) {
//            // for path violations, just associate with apic
//            nodeId = nodeCache.getNodeId(apicHost);
//            if (nodeId != null)
//                bldr.setNodeid(nodeId);
//        } else {
//            // for all others, just associate with apic
//            nodeId = nodeCache.getNodeId(apicHost);
//            if (nodeId != null)
//                bldr.setNodeid(nodeId);
        }
        
        //If we still do not have a nodeId, default to apichost
        if (nodeId == null || nodeId.longValue() == 0) {
            nodeId = nodeCache.getNodeId(apicHost);
            if (nodeId != null)
                bldr.setNodeid(nodeId);
        }
        
        bldr.setTime(createDate);
//        bldr.setDescription((String) attributes.get("descr"));
//        bldr.setLogMessage((String) attributes.get("rule"));
        if ("cleared".equals(attributes.get("severity")))
            bldr.setUei(ACI_UEI_PART + attributes.get("severity"));
        else
            bldr.setUei(ACI_UEI_PART + attributes.get("code"));
        bldr.setSeverity(SEVERITY_MAP.get(attributes.get("severity")).getLabel());
        bldr.setSource(ApicService.class.getSimpleName());
        
        bldr.addParam("apicHost", apicHost);
        return bldr;
    }
            
}
