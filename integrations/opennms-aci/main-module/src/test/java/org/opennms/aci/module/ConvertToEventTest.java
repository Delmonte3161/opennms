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

import static org.junit.Assert.assertNotNull;

import java.util.Date;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opennms.netmgt.model.events.EventBuilder;
import org.opennms.netmgt.xml.event.Event;

/**
 * @author tf016851
 *
 */
public class ConvertToEventTest {
    
    private static String message = "{\"code\":\"F120368\",\"subject\":\"counter\",\"ack\":\"no\",\"cause\":\"threshold-crossed\",\"rule\":\"tca-qosm-ingr-pkts5min-drop-rate\",\"dn\":\"subj-[topology\\/pod-1\\/node-125\\/sys\\/qosm\\/if-[eth1\\/52]\\/class-level3]\\/fr-12888860782\",\"type\":\"operational\",\"modTs\":\"never\",\"delegated\":\"no\",\"id\":\"12888860782\",\"prevSeverity\":\"warning\",\"ind\":\"deletion\",\"severity\":\"cleared\",\"origSeverity\":\"warning\",\"created\":\"2017-07-27T13:14:06.807-05:00\",\"occur\":\"2\",\"childAction\":\"\",\"delegatedFrom\":\"\",\"affected\":\"topology\\/pod-1\\/node-125\\/sys\\/qosm\\/if-[eth1\\/52]\\/class-level3\",\"highestSeverity\":\"warning\",\"changeSet\":\"dropRate:8\",\"descr\":\"TCA: ingress drop packets rate(qosmIngrPkts5min:dropRate) value 9 raised above threshold 9 and value is recovering \",\"domain\":\"infra\",\"lc\":\"\",\"status\":\"\"}";

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
    public void test() throws ParseException, java.text.ParseException {
        JSONParser parser = new JSONParser();

        JSONObject attributes = (JSONObject) parser.parse(message);
        
        String created = (String)attributes.get("created");
        String[] startTimeparts = created.split("T");
        String onlydate = startTimeparts[0];
        String onlytimewtz = startTimeparts[1];
        String onlytime = onlytimewtz.substring(0, onlytimewtz.length() - 6);
        String onlytz = onlytimewtz.substring(onlytimewtz.length() - 6);
        String tz = onlytz.replace(":", "");

        Date createDate = ApicService.format.parse(onlydate + "T" + onlytime
                + tz);

        NodeCache nodeCache = new NodeCache();
        nodeCache.init();
        
        EventBuilder bldr = ConvertToEvent.toEventBuilder(nodeCache, "test", createDate, attributes);
        
        Event event = bldr.getEvent();
        
        System.out.println(event.toString());
        
        assertNotNull(bldr);
    }

}
