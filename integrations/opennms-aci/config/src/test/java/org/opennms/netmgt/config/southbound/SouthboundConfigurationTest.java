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

package org.opennms.netmgt.config.southbound;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;
import org.opennms.core.test.xml.XmlTestNoCastor;
import org.opennms.netmgt.config.southbound.SouthCluster;
import org.opennms.netmgt.config.southbound.SouthElement;
import org.opennms.netmgt.config.southbound.SouthboundConfiguration;

public class SouthboundConfigurationTest extends XmlTestNoCastor<SouthboundConfiguration> {

    public SouthboundConfigurationTest(SouthboundConfiguration sampleObject,
            Object sampleXml) {
        super(sampleObject, sampleXml, "src/main/resources/xsds/southbound-configuration.xsd");
    }
    
    @Parameters
    public static Collection<Object[]> data() throws ParseException {
        return Arrays.asList(new Object[][] {
            {
                getConfig(),
                "<southbound-configuration xmlns=\"http://xmlns.opennms.org/xsd/config/southbound-configuration\">\n" +
                "  <south-cluster>\n" +
                "    <cluster-name>Test-Cluster</cluster-name>\n" +
                "    <cluster-type>CISCO-ACI</cluster-type>\n" +
                "    <cron-schedule>0 0 0 * * ? *</cron-schedule>\n" +
                "    <location>Test</location>\n" +
                "    <poll-duration-minutes>5</poll-duration-minutes>\n" +
                "    <south-element host=\"localhost\" \n" +
                "               port=\"443\" \n" +
                "               password=\"opennms-test\" \n" +
                "               reconnect-delay=\"1000\" \n" +
                "               southbound-api=\"" + SouthElement.DEFAULT_SOUTH_CLIENT_API + "\"\n" +
                "               southbound-message-parser=\"" + SouthElement.DEFAULT_SOUTH_MESSAGE_PARSER + "\" \n" +
                "               userid=\"opennms-test\"/>\n" +
                "  </south-cluster>\n" +
                "  <south-cluster>\n" +
                "    <cluster-name>Test-Cluster2</cluster-name>\n" +
                "    <cluster-type>CISCO-ACI</cluster-type>\n" +
                "    <cron-schedule>0 0 0 * * ? *</cron-schedule>\n" +
                "    <location>Test</location>\n" +
                "    <poll-duration-minutes>3</poll-duration-minutes>\n" +
                "    <south-element host=\"localhost\" \n" +
                "               port=\"443\" \n" +
                "               password=\"opennms-test2\" \n" +
                "               reconnect-delay=\"2000\" \n" +
                "               southbound-api=\"" + SouthElement.DEFAULT_SOUTH_CLIENT_API + "\"\n" +
                "               southbound-message-parser=\"" + SouthElement.DEFAULT_SOUTH_MESSAGE_PARSER + "\" \n" +
                "               userid=\"opennms-test2\"/>\n" +
                "    <south-element host=\"localhost2\" \n" +
                "               port=\"443\" \n" +
                "               password=\"opennms-test2\" \n" +
                "               reconnect-delay=\"2000\" \n" +
                "               southbound-api=\"" + SouthElement.DEFAULT_SOUTH_CLIENT_API + "\"\n" +
                "               southbound-message-parser=\"" + SouthElement.DEFAULT_SOUTH_MESSAGE_PARSER + "\" \n" +
                "               userid=\"opennms-test2\"/>\n" +
                "  </south-cluster>\n" +
                "</southbound-configuration>"
            },
            {
                new SouthboundConfiguration(),
                "<southbound-configuration xmlns=\"http://xmlns.opennms.org/xsd/config/southbound-configuration\" />"
            }
        });
    }



    private static SouthboundConfiguration getConfig() {
        
        SouthboundConfiguration config = new SouthboundConfiguration();
        
        SouthCluster southCluster = new SouthCluster();
        southCluster.setClusterName("Test-Cluster");
        southCluster.setClusterType("CISCO-ACI");
        southCluster.setLocation("Test");
        southCluster.setCronSchedule("0 0 0 * * ? *");
        southCluster.setPollDurationMinutes(5);
        
        SouthElement southElement = new SouthElement();
        southElement.setHost("localhost");
        southElement.setPort(443);
        southElement.setUserid("opennms-test");
        southElement.setPassword("opennms-test");
        southElement.setReconnectDelay((long) 1000);
        southElement.setSouthboundApi(SouthElement.DEFAULT_SOUTH_CLIENT_API);
        southElement.setSouthboundMessageParser(SouthElement.DEFAULT_SOUTH_MESSAGE_PARSER);

        southCluster.addElement(southElement);
        
        config.addSouthCluster(southCluster);
        
        SouthCluster southCluster2 = new SouthCluster();
        southCluster2.setClusterName("Test-Cluster2");
        southCluster2.setClusterType("CISCO-ACI");
        southCluster2.setCronSchedule("0 0 0 * * ? *");
        southCluster2.setLocation("Test");
        southCluster2.setPollDurationMinutes(3);
        
        SouthElement southElement2 = new SouthElement();
        southElement2.setHost("localhost");
        southElement2.setPort(443);
        southElement2.setUserid("opennms-test2");
        southElement2.setPassword("opennms-test2");
        southElement2.setReconnectDelay((long) 2000);
        southElement2.setSouthboundApi(SouthElement.DEFAULT_SOUTH_CLIENT_API);
        southElement2.setSouthboundMessageParser(SouthElement.DEFAULT_SOUTH_MESSAGE_PARSER);

        southCluster2.addElement(southElement2);

        SouthElement southElement3 = new SouthElement();
        southElement3.setHost("localhost2");
        southElement3.setPort(443);
        southElement3.setUserid("opennms-test2");
        southElement3.setPassword("opennms-test2");
        southElement3.setReconnectDelay((long) 2000);
        southElement3.setSouthboundApi(SouthElement.DEFAULT_SOUTH_CLIENT_API);
        southElement3.setSouthboundMessageParser(SouthElement.DEFAULT_SOUTH_MESSAGE_PARSER);

        southCluster2.addElement(southElement3);

        
        config.addSouthCluster(southCluster2);

        return config;
    }
}
