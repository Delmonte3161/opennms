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

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.model.OnmsNode;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.opennms.netmgt.model.OnmsSnmpInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * @author tf016851
 *
 */
public class NodeCache {
    private static final Logger LOG = LoggerFactory.getLogger(NodeCache.class);
    
    private long MAX_SIZE = 10000;
    private long MAX_TTL  = 3; // Minutes

    private volatile NodeDao nodeDao;

    private LoadingCache<String, String> cache = null;
    
    public NodeCache() {}
    
    @SuppressWarnings("unchecked")
    public void init() {
        LOG.info("initializing node data cache (TTL="+MAX_TTL+"m, MAX_SIZE="+MAX_SIZE+")");
         @SuppressWarnings("rawtypes")
        CacheBuilder cacheBuilder =  CacheBuilder.newBuilder();
         if(MAX_TTL>0) {
             cacheBuilder.expireAfterWrite(MAX_TTL, TimeUnit.MINUTES);
         }
         if(MAX_SIZE>0) {
             cacheBuilder.maximumSize(MAX_SIZE);
         }

         cache=cacheBuilder.build(new CacheLoader<String, String>() {
             @Override
             public String load(String key) throws Exception {
                 return lookupNodeKey(key);
             }
         }
);
    }
    
    private String lookupNodeKey(String key) throws Exception {
        if (key == null)
            return null;
        
        String[] keyParts = key.split(ApicService.FS_SEP);
        if (keyParts.length != 2) {
            throw new NodeCacheInvalidKeyException("Incorrect key format key=" + key);
        }

        String foreignSource = keyParts[0];
        String[] dnParts = keyParts[1].split(ApicService.DN_SEP);
        
        if (dnParts.length >= 4) {
            //We have a Node DN, construct foreignId and lookup
            String foreignId = dnParts[0] + ApicService.DN_SEP + dnParts[1] + ApicService.DN_SEP + dnParts[2] + ApicService.DN_SEP + dnParts[3];
            OnmsNode node = nodeDao.findByForeignId(foreignSource, foreignId);
            
            if (node == null)
                throw new NodeCacheKeyNotFoundException("ACI Key not found, key=" + key);
            
            String onmsKey = node.getNodeId() + ApicService.FS_SEP;
//            if (dnParts.length > 4) {
//                //We have an interface DN, lookup interface and append to key
//                //TODO - Implement interface lookup
//                Set<OnmsSnmpInterface> interfaces = node.getSnmpInterfaces();
//                for (OnmsSnmpInterface onmsSnmpInterface : interfaces) {
//                    if (onmsSnmpInterface.getIfName().equals(dnParts[4])) {
//                       break; 
//                    }
//                    
//                }
//            }
            
            return node.getNodeId();
        }
        
        throw new NodeCacheInvalidKeyException("The ACI DN key is not valid, key=" + key);
    }
    
    public Long getNodeId(String key) {
        try {
            String nodeKey = this.cache.get(key);
            String[] nodeKeyParts = nodeKey.split(ApicService.FS_SEP);
            return Long.parseLong(nodeKeyParts[0]);
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }
    
    public String getInterfaceId(String key) {
        try {
            String nodeKey = this.cache.get(key);
            String[] nodeKeyParts = nodeKey.split(ApicService.FS_SEP);
            if (nodeKeyParts.length == 2)
                return nodeKeyParts[1];
        } catch (ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }

    public NodeDao getNodeDao() {
        return nodeDao;
    }

    public void setNodeDao(NodeDao nodeDao) {
        this.nodeDao = nodeDao;
    }
    
}
