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

package org.opennms.netmgt.dao.jaxb;

import org.opennms.core.xml.AbstractJaxbConfigDao;
import org.opennms.netmgt.config.southd.SouthCluster;
import org.opennms.netmgt.config.southd.SouthdConfiguration;
import org.opennms.netmgt.dao.api.SouthboundConfigDao;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * DefaultSouthboundConfigDao
 * 
 * @author tf016851
 *
 */
public class DefaultSouthboundConfigDao extends AbstractJaxbConfigDao<SouthdConfiguration, SouthdConfiguration> implements SouthboundConfigDao {

    /**
     * 
     */
    public DefaultSouthboundConfigDao() {
        super(SouthdConfiguration.class, "Southbound Controlller Config");
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.dao.api.SouthboundConfigDao#getSouthboundConfig()
     */
    @Override
    public SouthdConfiguration getSouthboundConfig() {
        return getContainer().getObject();
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.dao.api.SouthboundConfigDao#getSouthboundCluster()
     */
    @Override
    public SouthCluster getSouthboundCluster() {
        return getContainer().getObject().getSouthCluster();
    }

    /* (non-Javadoc)
     * @see org.opennms.netmgt.dao.api.SouthboundConfigDao#reloadConfiguration()
     */
    @Override
    public void reloadConfiguration()
            throws DataAccessResourceFailureException {
        getContainer().reload();
    }

    @Override
    protected SouthdConfiguration translateConfig(
            SouthdConfiguration config) {
        return config;
    }

}
