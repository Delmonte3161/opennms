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
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opennms.aci.rpc.rest.client.ACIRestClient;
import org.opennms.netmgt.events.api.EventForwarder;
import org.opennms.netmgt.model.events.EventBuilder;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Events;
import org.opennms.netmgt.xml.event.Log;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tf016851
 *
 */
public class ApicClusterJob implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(ApicClusterJob.class);
    
    private ACIRestClient client;
    
    private String lastProcessTime = null;

    public ApicClusterJob() {
        LOG.debug("Initializing ApicClusterJob ...");
    }

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        
        LOG.debug("Running execution for Job: " + context.getJobDetail().getKey());

        String location = (String) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_LOCATION_KEY);
        String apicUrl = (String) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_URL_KEY);
        String username = (String) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_USERNAME_KEY);
        String password = (String) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_PASSWORD_KEY);
        
        if (client == null) {
            try {
                LOG.info("Initializing ACIRestClient with: \n" +
                         ApicService.APIC_CONFIG_LOCATION_KEY + ": " + location + "\n" +
                         ApicService.APIC_CONFIG_URL_KEY + ": " + apicUrl + "\n" +
                         ApicService.APIC_CONFIG_USERNAME_KEY + ": " + username 
                        );
                client = ACIRestClient.newAciRest(location, apicUrl, username, password);
            } catch (Exception e) {
//                e.printStackTrace();
                LOG.debug("Failed to initialize ACIRestClient", e);
            }

        }

        if (client == null)
            throw new JobExecutionException(context.getJobDetail().getKey()
                    + ": Failed to initialize client.");
        
        SchedulerContext schedulerContext = null;
        try {
            schedulerContext = context.getScheduler().getContext();
        } catch (SchedulerException e1) {
            e1.printStackTrace();
            throw new JobExecutionException(context.getJobDetail().getKey()
                    + ": Failed to initialize client.", e1);
        }

        EventForwarder eventForwarder = (EventForwarder) schedulerContext.get(ApicService.APIC_CONFIG_EVENT_FORWARDER);

        // TODO - Need to add logic for determining the last fault process
        // time.
        // For now, just query for the same time interval for testing
        int pollDuration = (int)context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_POLL_DURATION_KEY);
//        final java.util.Calendar startCal = GregorianCalendar.getInstance();
        
        if (schedulerContext.get(ApicService.APIC_CONFIG_CLUSTER_MAP) == null || 
                ((Map<String,Map<String, Object>>)schedulerContext.get(ApicService.APIC_CONFIG_CLUSTER_MAP)).get(context.getJobDetail().getKey().toString()) == null) {
            
            LOG.error(context.getJobDetail().getKey()
                    + ": Failed to initialize " + ApicService.APIC_CONFIG_CLUSTER_MAP);
            throw new JobExecutionException(context.getJobDetail().getKey()
                    + ": Failed to initialize " + ApicService.APIC_CONFIG_CLUSTER_MAP);
        }
        
        Map<String,Object> clusterJobMap = (Map<String, Object>) ((Map<String,Map<String, Object>>)schedulerContext.get(ApicService.APIC_CONFIG_CLUSTER_MAP)).get(context.getJobDetail().getKey().toString());

        try {
            // If last execution time is null (first time after restart),
            // then set from last event
            if (clusterJobMap.get(ApicService.APIC_CLUSTER_MAP_LAST_PROCESS_TIME) == null) {
                // TODO - For now just use current system time from APIC,
                // replace with logic to query last event time.
                lastProcessTime = client.getTimeStamp(pollDuration * 60);
                clusterJobMap.put(ApicService.APIC_CLUSTER_MAP_LAST_PROCESS_TIME, lastProcessTime);
            } else {
                lastProcessTime = (String) clusterJobMap.get(ApicService.APIC_CLUSTER_MAP_LAST_PROCESS_TIME);
            }

            LOG.debug("Querying for faults after: " + lastProcessTime);
            JSONArray results = client.getCurrentFaults(lastProcessTime);
            
            if (results == null || results.isEmpty())
                return;
            
            Date lastProcessDate = null;
            final Log elog = new Log();
            final Events events = new Events();
            elog.setEvents(events);
                        
            for (Object object : results) {
                JSONObject objectData = (JSONObject) object;
                if (objectData == null)
                    continue;
                for (Object object2 : objectData.keySet()) {
                    String key = (String) object2;
                    JSONObject classData = (JSONObject) objectData.get(key);
                    JSONObject attributes = (JSONObject) classData.get("attributes");
                    if (attributes == null)
                        continue;
                    
                    if (attributes.get("created") != null) {
                        String created = (String)attributes.get("created");
                        String[] startTimeparts = created.split("T");
                        String onlydate = startTimeparts[0];
                        String onlytimewtz = startTimeparts[1];
                        String onlytime = onlytimewtz.substring(0, onlytimewtz.length() - 6);
                        String onlytz = onlytimewtz.substring(onlytimewtz.length() - 6);
                        String tz = onlytz.replace(":", "");

                        Date createDate = ApicService.format.parse(onlydate + "T" + onlytime
                                + tz);
                        if (lastProcessDate == null || createDate.after(lastProcessDate)) {
                            lastProcessDate = createDate;
                            lastProcessTime = ApicService.format.format(lastProcessDate);
                        }

                        LOG.debug(created + " --- " + attributes.toJSONString());
                        EventBuilder bldr = ConvertToEvent.toEventBuilder(location, createDate, attributes);
                        
                        Event event = bldr.getEvent();
                        
                        events.addEvent(event);
                    }
                }
            }
            if (events.getEventCount() > 0) {
                LOG.debug("Sending " + events.getEventCount() + " event(s)");
                eventForwarder.sendNowSync(elog);
            }
            clusterJobMap.put(ApicService.APIC_CLUSTER_MAP_LAST_PROCESS_TIME, lastProcessTime);
            LOG.debug("Last Process Date: " + lastProcessTime);
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("ApicClusterJob failed for cluster: LS6 at " + lastProcessTime, e);
            throw new JobExecutionException(e, false);
        }

    }


}
