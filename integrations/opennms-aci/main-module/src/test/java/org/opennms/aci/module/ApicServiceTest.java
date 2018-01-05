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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.criteria.Criteria;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.core.test.OpenNMSJUnit4ClassRunner;
import org.opennms.core.xml.AbstractJaxbConfigDao;
import org.opennms.netmgt.config.southbound.SouthElement;
import org.opennms.netmgt.config.southbound.SouthboundConfiguration;
import org.opennms.netmgt.dao.api.EventDao;
import org.opennms.netmgt.dao.api.IpInterfaceDao;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.dao.jaxb.southbound.DefaultSouthboundConfigDao;
import org.opennms.netmgt.dao.mock.MockNodeDao;
import org.opennms.netmgt.dao.southbound.SouthboundConfigDao;
import org.opennms.netmgt.events.api.EventForwarder;
import org.opennms.netmgt.model.NetworkBuilder;
import org.opennms.netmgt.model.OnmsEvent;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.opennms.netmgt.model.NetworkBuilder.InterfaceBuilder;
import org.opennms.netmgt.model.NetworkBuilder.NodeBuilder;
import org.opennms.test.JUnitConfigurationEnvironment;
import org.opennms.test.mock.EasyMockUtils;
import org.opennms.netmgt.xml.event.Log;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;

import java.util.Collections;
import java.util.List;

import org.easymock.EasyMock;

/**
 * @author tf016851
 *
 */
public class ApicServiceTest {
    
    private EasyMockUtils m_mockUtils;
    
    private EventForwarder eventForwarder;
    
    private IpInterfaceDao m_ifaceDao;

    private NodeDao m_nodeDao;
    
    private EventDao m_eventDao;
    
    private SouthboundConfigDao m_configDao;
    
    @Before
    public void setUp() throws Exception {
        
        String xml = this.generateConfigXml();
        m_configDao = new DefaultSouthboundConfigDao();
        
        Resource resource = new ByteArrayResource(xml.getBytes());
        ((DefaultSouthboundConfigDao) m_configDao).setConfigResource(resource);
        ((DefaultSouthboundConfigDao) m_configDao).afterPropertiesSet();
        
        m_mockUtils = new EasyMockUtils();
        
        m_ifaceDao = m_mockUtils.createMock(IpInterfaceDao.class);
        m_nodeDao = m_mockUtils.createMock(NodeDao.class);
        m_eventDao = m_mockUtils.createMock(EventDao.class);
        eventForwarder = m_mockUtils.createMock(EventForwarder.class);
        
        NetworkBuilder netBuilder = new NetworkBuilder();
        NodeBuilder nodeBuilder = netBuilder.addNode("node1").setId(1);
        InterfaceBuilder ifaceBlder = 
            netBuilder.addInterface("192.168.1.1")
            .setId(2)
            .setIsSnmpPrimary("P");
        ifaceBlder.addSnmpInterface(1);
        
        List<OnmsIpInterface> initialIfs = Collections.emptyList();
        //EasyMock.expect(m_ifaceDao.findByServiceType(snmp.getName())).andReturn(initialIfs).anyTimes();
        
        CriteriaBuilder builder = new CriteriaBuilder(OnmsEvent.class);
        List<OnmsEvent> events = Collections.emptyList();
        EasyMock.expect(m_eventDao.findMatching(EasyMock.anyObject(Criteria.class))).andReturn(events).anyTimes();
        
        EasyMock.expect(m_nodeDao.findByForeignId(EasyMock.anyObject(String.class), EasyMock.anyObject(String.class))).andReturn(nodeBuilder.getNode()).anyTimes();
        
        EasyMock.expect(m_ifaceDao.load(2)).andReturn(ifaceBlder.getInterface()).anyTimes();
        
        m_mockUtils.replayAll();
    }

//    @Test
    public void testService() {

        ApicService service = new ApicService();
        service.setSouthboundConfigDao(m_configDao);
        service.setNodeDao(m_nodeDao);
        service.setEventDao(m_eventDao);
        service.setEventForwarder(eventForwarder);
        
        System.out.println("Initializing ApicService ...");
        service.init();
        
        try {
            Thread.sleep(10000); //Sleep long enough for job to start
        } catch (InterruptedException e) {
            e.printStackTrace();
        } 

        System.out.println("Destroying ApicService ...");
        service.destroy();
        
    }

    private String generateConfigXml() {
        return "<southbound-configuration xmlns=\"http://xmlns.opennms.org/xsd/config/southbound-configuration\">\n" +
                "  <south-cluster>\n" +
                "    <cluster-name>CTC-KC-VIII</cluster-name>\n" +
                "    <cluster-type>CISCO-ACI</cluster-type>\n" +
                "    <poll-duration-minutes>3</poll-duration-minutes>\n" +
                "    <south-element host=\"7.192.240.10\" \n" +
                "               port=\"443\" \n" +
                "               reconnect-delay=\"2000\" \n" +
                "               southbound-api=\"" + SouthElement.DEFAULT_SOUTH_CLIENT_API + "\"\n" +
                "               southbound-message-parser=\"" + SouthElement.DEFAULT_SOUTH_MESSAGE_PARSER + "\" \n" +
                "               userid=\"svcOssAci\"\n" +
                "               password=\"kf3as=Nx\"/>\n" +
                "    <south-element host=\"7.192.240.11\" \n" +
                "               port=\"443\" \n" +
                "               reconnect-delay=\"2000\" \n" +
                "               southbound-api=\"" + SouthElement.DEFAULT_SOUTH_CLIENT_API + "\"\n" +
                "               southbound-message-parser=\"" + SouthElement.DEFAULT_SOUTH_MESSAGE_PARSER + "\" \n" +
                "               userid=\"svcOssAci\"\n" +
                "               password=\"kf3as=Nx\"/>\n" +
                "    <south-element host=\"7.192.240.12\" \n" +
                "               port=\"443\" \n" +
                "               reconnect-delay=\"2000\" \n" +
                "               southbound-api=\"" + SouthElement.DEFAULT_SOUTH_CLIENT_API + "\"\n" +
                "               southbound-message-parser=\"" + SouthElement.DEFAULT_SOUTH_MESSAGE_PARSER + "\" \n" +
                "               userid=\"svcOssAci\"\n" +
                "               password=\"kf3as=Nx\"/>\n" +
                "  </south-cluster>\n" +
                "</southbound-configuration>";
    }

    /**
     * @param args
     */
//    public static void main(String[] args) {
//        
//
//    }

}
