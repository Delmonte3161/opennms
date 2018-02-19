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

import static org.opennms.core.utils.InetAddressUtils.addr;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.opennms.core.logging.Logging;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.dao.api.EventDao;
import org.opennms.netmgt.events.api.EventConstants;
import org.opennms.netmgt.events.api.EventForwarder;
import org.opennms.netmgt.model.events.EventBuilder;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Events;
import org.opennms.netmgt.xml.event.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tf016851
 *
 */
public class ApicEventForwader {
    
    private static final Logger LOG = LoggerFactory.getLogger(ApicEventForwader.class);

    private final EventForwarder eventForwarder;
    private final EventDao eventDao;
    private final NodeCache nodeCache;
    
    private final String localAddr;
    
    public ApicEventForwader (EventForwarder eventForwarder, EventDao eventDao, NodeCache nodeCache) {
        this.eventForwarder = eventForwarder;
        this.eventDao = eventDao;
        this.nodeCache = nodeCache;
        
//        Logging.putPrefix("aci");
        
        localAddr = InetAddressUtils.getLocalHostName();
    }
    
    public void sendEvent(String clusterName, String apicHost, String jsonMessage) {
        
        try {
            final JSONParser parser = new JSONParser();
            //TODO - Implement processing event
            JSONObject result = (JSONObject) parser.parse(jsonMessage);
            JSONArray imdata = (JSONArray) result.get("imdata");
            final Log elog = new Log();
            final Events events = new Events();
            elog.setEvents(events);

            Map<String, Event> suspects = new HashMap<String, Event>();
            for (Object object : imdata) {
                JSONObject objectData = (JSONObject) object;
                if (objectData == null)
                    continue;
                
                for (Object object2 : objectData.keySet()) {
                    String key = (String) object2;
                    JSONObject classData = (JSONObject) objectData.get(key);
                    JSONObject attributes = (JSONObject) classData.get("attributes");
                    if (attributes == null)
                        continue;
                    
                    Date createDate = null;
                    String created = (String) attributes.get("created");
                    if (created != null) {
                        try {
                            String[] startTimeparts = created.split("T");
                            String onlydate = startTimeparts[0];
                            String onlytimewtz = startTimeparts[1];
                            String onlytime = onlytimewtz.substring(0,
                                                                    onlytimewtz.length()
                                                                            - 6);
                            String onlytz = onlytimewtz.substring(onlytimewtz.length()
                                    - 6);
                            String tz = onlytz.replace(":", "");

                            createDate = ApicService.format.parse(onlydate + "T" + onlytime
                                    + tz);
                        } catch (Throwable e) {
                            e.printStackTrace();
                            createDate = null;
                        }
                    }
                    
                    Date today = new Date();
                    if (createDate == null || createDate.after(today))
                        createDate = today;
                    
                    EventBuilder bldr = ConvertToEvent.toEventBuilder(nodeCache, clusterName, createDate, attributes, apicHost);

                    Event event = bldr.getEvent();

                    events.addEvent(event);
                }
                
                if (events.getEventCount() > 0) {
                    eventForwarder.sendNowSync(elog);
                    
                    LOG.debug("Sent " + events.getEventCount() + " event(s)");
                    //Loop on suspects and send newSuspectEvents
                    suspects.values().stream()
                    .filter(e -> !e.hasNodeid() && e.getInterface() != null)
                    .forEach(e -> {
                        LOG.trace("ApicService: Found a new suspect {}", e.getInterface());
                        sendNewSuspectEvent(e.getInterface(), e.getDistPoller());
                    });
                }
            }
        } catch (Throwable e) {
            LOG.error("Failure sending message:\n" + jsonMessage + "\n", e);
            if (LOG.isDebugEnabled()) {
                System.out.println(jsonMessage);
                e.printStackTrace();
            }
        }
    }
    
    private void sendNewSuspectEvent(String eventInterface, String distPoller) {
        EventBuilder bldr = new EventBuilder(EventConstants.NEW_SUSPECT_INTERFACE_EVENT_UEI, "syslogd");
        bldr.setInterface(addr(eventInterface));
        bldr.setHost(localAddr);
//        bldr.setDistPoller(distPoller);
        eventForwarder.sendNow(bldr.getEvent());
    }
}