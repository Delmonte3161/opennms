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

package org.opennms.netmgt.dao.jaxb.southbound;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opennms.netmgt.config.southbound.SouthCluster;
import org.opennms.netmgt.dao.southbound.SouthboundConfigDao;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

/**
 * @author tf016851
 *
 */
public class DefaultSouthboundConfigDaoTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testBadConfig() throws MalformedURLException, IOException {
        DefaultSouthboundConfigDao dao = new DefaultSouthboundConfigDao();
        Resource configResource = new UrlResource(Paths.get(new File( "." ).getCanonicalPath(), "src/test/resources/BadSouthboundConfiguration.xml").toUri());
        dao.setConfigResource(configResource);
        dao.afterPropertiesSet();

        try {
            dao.getSouthboundClusters();
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NullPointerException);
        }
    }

    @Test
    public void testConfig() throws MalformedURLException, IOException {

        DefaultSouthboundConfigDao dao = new DefaultSouthboundConfigDao();
        Resource configResource = new UrlResource(Paths.get(new File( "." ).getCanonicalPath(), "src/test/resources/southbound-configuration.xml").toUri());
        dao.setConfigResource(configResource);
        dao.afterPropertiesSet();
        
        List<SouthCluster> clusters = dao.getSouthboundClusters();
        Assert.assertTrue(clusters.size() > 0);
    }
}
