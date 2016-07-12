/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2003-2014 The OpenNMS Group, Inc.
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

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.dao.api.IpInterfaceDao;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class represents a singular instance that is used to map trap IP
 * addresses to known nodes.
 * 
 * TODO: Replace method-level synchronization with collection-level
 * synchronization.
 *
 * @author <a href="mailto:joed@opennms.org">Johan Edstrom</a>
 * @author <a href="mailto:weave@oculan.com">Brian Weaver </a>
 * @author <a href="mailto:tarus@opennms.org">Tarus Balog </a>
 * @author <a href="http://www.opennms.org/">OpenNMS </a>
 */
public class TrapdIpManagerDaoImpl implements TrapdIpMgr {

    private static final Logger LOG = LoggerFactory.getLogger(TrapdIpManagerDaoImpl.class);

    @Autowired
    private IpInterfaceDao m_ipInterfaceDao;

    public IpInterfaceDao getIpInterfaceDao() {
		return m_ipInterfaceDao;
	}

	public void setIpInterfaceDao(IpInterfaceDao m_ipInterfaceDao) {
		this.m_ipInterfaceDao = m_ipInterfaceDao;
	}

	/**
     * A Map of IP addresses and node IDs
     */
    protected Map<InetAddress, Integer> m_knownips = new ConcurrentHashMap<InetAddress, Integer>();

    /**
     * Clears and synchronizes the internal known IP address cache with the
     * current information contained in the database. To synchronize the cache
     * the method opens a new connection to the database, loads the address,
     * and then closes it's connection.
     *
     * @throws java.sql.SQLException
     *             Thrown if the connection cannot be created or a database
     *             error occurs.
     */
    @Override
    public synchronized void dataSourceSync() {
        m_knownips = m_ipInterfaceDao.getInterfacesForNodes();
    }

    /**
     * Returns the nodeid for the IP Address
     *
     * @param addr The IP Address to query.
     * @return The node ID of the IP Address if known.
     */
    @Override
    public synchronized int getNodeId(String addr) {
        if (addr == null) {
            return -1;
        }
        return intValue(m_knownips.get(InetAddressUtils.getInetAddress(addr)));
    }

    /**
     * Sets the IP Address and Node ID in the Map.
     *
     * @param addr   The IP Address to add.
     * @param nodeid The Node ID to add.
     * @return The nodeid if it existed in the map.
     */
    @Override
    public synchronized int setNodeId(String addr, int nodeid) {
        if (addr == null || nodeid == -1) {
            return -1;
        }
        // Only add the address if it doesn't exist on the map. If it exists, only replace the current one if the new address is primary.
        if (m_knownips.containsKey(InetAddressUtils.getInetAddress(addr))) {
            OnmsIpInterface intf = m_ipInterfaceDao.findByNodeIdAndIpAddress(nodeid, addr);
            if (intf != null && intf.isPrimary()) {
                LOG.info("setNodeId: adding SNMP primary IP address {} to known IP list", intf);
                return intValue(m_knownips.put(InetAddressUtils.getInetAddress(addr), nodeid));
            } else {
                return -1;
            }
        } else {
            return intValue(m_knownips.put(InetAddressUtils.getInetAddress(addr), nodeid));
        }
    }

    /**
     * Removes an address from the node ID map.
     *
     * @param addr The address to remove from the node ID map.
     * @return The nodeid that was in the map.
     */
    @Override
    public synchronized int removeNodeId(String addr) {
        if (addr == null) {
            return -1;
        }
        return intValue(m_knownips.remove(InetAddressUtils.getInetAddress(addr)));
    }

    @Override
    public int intValue(final Integer result) {
        return (result == null ? -1 : result);
    }
}
