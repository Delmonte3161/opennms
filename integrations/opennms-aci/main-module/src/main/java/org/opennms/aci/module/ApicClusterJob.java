/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2014 The OpenNMS Group, Inc.
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

package org.opennms.aci.module;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opennms.aci.rpc.rest.client.ACIRestClient;
import org.opennms.core.criteria.Alias.JoinType;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.netmgt.dao.api.EventDao;
import org.opennms.netmgt.events.api.EventForwarder;
import org.opennms.netmgt.model.OnmsEvent;
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
 */
public class ApicClusterJob implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(ApicClusterJob.class);

    private ACIRestClient client;

    private String lastProcessTime = null;

    public ApicClusterJob() {
        LOG.debug("Initializing ApicClusterJob ...");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {

        LOG.debug("Running execution for Job: "
                + context.getJobDetail().getKey());

        String location = (String) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_LOCATION_KEY);
        String apicUrl = (String) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_URL_KEY);
        String username = (String) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_USERNAME_KEY);
        String password = (String) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_PASSWORD_KEY);

        if (client == null) {
            try {
                LOG.info("Initializing ACIRestClient with: \n"
                        + ApicService.APIC_CONFIG_LOCATION_KEY + ": "
                        + location + "\n" + ApicService.APIC_CONFIG_URL_KEY
                        + ": " + apicUrl + "\n"
                        + ApicService.APIC_CONFIG_USERNAME_KEY + ": "
                        + username);
                client = ACIRestClient.newAciRest(location, apicUrl, username, password);
            } catch (Exception e) {
                // e.printStackTrace();
                String msg = "Failed to initialize ACIRestClient";
                LOG.debug(msg, e);
                throw new JobExecutionException(msg, e, false);
            }

        }

        if (client == null)
            throw new JobExecutionException(context.getJobDetail().getKey()
                    + ": Failed to initialize client.", false);

        SchedulerContext schedulerContext = null;
        try {
            schedulerContext = context.getScheduler().getContext();
        } catch (SchedulerException e1) {
//            e1.printStackTrace();
            throw new JobExecutionException(context.getJobDetail().getKey()
                    + ": Failed to initialize client.", e1, false);
        }

        EventForwarder eventForwarder = (EventForwarder) schedulerContext.get(ApicService.APIC_CONFIG_EVENT_FORWARDER);
        EventDao eventDao = (EventDao) schedulerContext.get(ApicService.APIC_CONFIG_EVENT_DAO);

        int pollDuration = (int) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_POLL_DURATION_KEY);

        if (schedulerContext.get(ApicService.APIC_CONFIG_CLUSTER_MAP) == null
                || ((Map<String, Map<String, Object>>) schedulerContext.get(ApicService.APIC_CONFIG_CLUSTER_MAP)).get(context.getJobDetail().getKey().toString()) == null) {

            String msg = context.getJobDetail().getKey()
                    + ": Failed to initialize "
                    + ApicService.APIC_CONFIG_CLUSTER_MAP;

            LOG.error(msg);
            throw new JobExecutionException(msg, false);
        }

        Map<String, Object> clusterJobMap = (Map<String, Object>) ((Map<String, Map<String, Object>>) schedulerContext.get(ApicService.APIC_CONFIG_CLUSTER_MAP)).get(context.getJobDetail().getKey().toString());
        try {
            this.setLastProcessTime(clusterJobMap, eventDao, location, pollDuration);

            LOG.debug("Querying for faults after: " + lastProcessTime);
            JSONArray results = client.getCurrentFaults(lastProcessTime);

            if (results == null || results.isEmpty())
                return;

            this.sendEvents(clusterJobMap, eventForwarder, results, location);
        } catch (Exception e) {
//            e.printStackTrace();
            String msg = "ApicClusterJob failed for cluster: LS6 at "
                    + lastProcessTime;
            LOG.error(msg, e);
            throw new JobExecutionException(msg, e, false);
        }

    }

    private void setLastProcessTime(Map<String, Object> clusterJobMap,
            EventDao eventDao, String location, int pollDuration)
            throws Exception {

        // If last execution time is null (first time after restart),
        // then set from last event
        if (clusterJobMap.get(ApicService.APIC_CLUSTER_MAP_LAST_PROCESS_TIME) == null) {
            LOG.debug("Querying for last processed event for location: "
                    + location);
            CriteriaBuilder builder = new CriteriaBuilder(OnmsEvent.class);
            builder.alias("node", "node", JoinType.LEFT_JOIN);
            builder.alias("node.snmpInterfaces", "snmpInterface", JoinType.LEFT_JOIN);
            builder.alias("node.ipInterfaces", "ipInterface", JoinType.LEFT_JOIN);
            builder.alias("node.location", "location", JoinType.LEFT_JOIN);
            builder.alias("serviceType", "serviceType", JoinType.LEFT_JOIN);
            builder.eq("location.id", location);
            builder.orderBy("eventTime").desc();
            builder.limit(10);
            List<OnmsEvent> events = eventDao.findMatching(builder.toCriteria());
            if (events == null || events.size() < 1) {
                LOG.debug("No events found for location: " + location);
                lastProcessTime = client.getTimeStamp(pollDuration * 60);
                clusterJobMap.put(ApicService.APIC_CLUSTER_MAP_LAST_PROCESS_TIME, lastProcessTime);
            } else {
                LOG.debug("Found events, setting last processed time.");
                OnmsEvent event = events.get(1);
                Date eventTime = event.getEventTime();
                lastProcessTime = ApicService.format.format(eventTime);
                clusterJobMap.put(ApicService.APIC_CLUSTER_MAP_LAST_PROCESS_TIME, lastProcessTime);
            }
        } else {
            lastProcessTime = (String) clusterJobMap.get(ApicService.APIC_CLUSTER_MAP_LAST_PROCESS_TIME);
            LOG.trace("Setting lastProcessTime = " + lastProcessTime);
        }

    }

    private void sendEvents(Map<String, Object> clusterJobMap,
            EventForwarder eventForwarder, JSONArray results, String location)
            throws ParseException {

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
                    String created = (String) attributes.get("created");
                    String[] startTimeparts = created.split("T");
                    String onlydate = startTimeparts[0];
                    String onlytimewtz = startTimeparts[1];
                    String onlytime = onlytimewtz.substring(0, onlytimewtz.length()
                            - 6);
                    String onlytz = onlytimewtz.substring(onlytimewtz.length()
                            - 6);
                    String tz = onlytz.replace(":", "");

                    Date createDate = ApicService.format.parse(onlydate + "T"
                            + onlytime + tz);
                    if (lastProcessDate == null
                            || createDate.after(lastProcessDate)) {
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
    }
}
