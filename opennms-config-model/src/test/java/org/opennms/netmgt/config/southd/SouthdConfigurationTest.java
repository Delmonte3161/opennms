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

package org.opennms.netmgt.config.southd;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;
import org.opennms.core.test.xml.XmlTestNoCastor;

public class SouthdConfigurationTest extends XmlTestNoCastor<SouthdConfiguration> {

    public SouthdConfigurationTest(SouthdConfiguration sampleObject,
            Object sampleXml) {
        super(sampleObject, sampleXml, "src/main/resources/xsds/southd-configuration.xsd");
    }
    
    @Parameters
    public static Collection<Object[]> data() throws ParseException {
        return Arrays.asList(new Object[][] {
            {
                getConfig(),
                "<southd-configuration xmlns=\"http://xmlns.opennms.org/xsd/config/southd-configuration\">\n" +
                "  <south-cluster>\n" +
                "    <cluster-name>Test-Cluster</cluster-name>\n" +
                "    <cron-schedule>0 0 0 * * ? *</cron-schedule>\n" +
                "    <south-element host=\"localhost\" \n" +
                "               port=\"443\" \n" +
                "               password=\"opennms-test\" \n" +
                "               reconnect-delay=\"1000\" \n" +
                "               southbound-api=\"" + SouthElement.DEFAULT_SOUTH_CLIENT_API + "\"\n" +
                "               southbound-message-parser=\"" + SouthElement.DEFAULT_SOUTH_MESSAGE_PARSER + "\" \n" +
                "               userid=\"opennms-test\"/>\n" +
                "  </south-cluster>\n" +
                "</southd-configuration>"
            },
            {
                new SouthdConfiguration(),
                "<southd-configuration xmlns=\"http://xmlns.opennms.org/xsd/config/southd-configuration\" />"
            }
        });
    }



    private static SouthdConfiguration getConfig() {
        
        SouthdConfiguration config = new SouthdConfiguration();
        
        SouthCluster southCluster = new SouthCluster();
        southCluster.setClusterName("Test-Cluster");
        southCluster.setCronSchedule("0 0 0 * * ? *");
        
        SouthElement southElement = new SouthElement();
        southElement.setHost("localhost");
        southElement.setPort(443);
        southElement.setUserid("opennms-test");
        southElement.setPassword("opennms-test");
        southElement.setReconnectDelay((long) 1000);
        southElement.setSouthboundApi(SouthElement.DEFAULT_SOUTH_CLIENT_API);
        southElement.setSouthboundMessageParser(SouthElement.DEFAULT_SOUTH_MESSAGE_PARSER);

        southCluster.addElement(southElement);
        
        config.setSouthCluster(southCluster);
        
        return config;
    }
}
