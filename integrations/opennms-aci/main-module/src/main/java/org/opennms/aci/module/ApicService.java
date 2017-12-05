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

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.opennms.core.logging.Logging;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.config.southbound.SouthCluster;
import org.opennms.netmgt.config.southbound.SouthElement;
import org.opennms.netmgt.dao.api.EventDao;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.dao.southbound.SouthboundConfigDao;
import org.opennms.netmgt.events.api.EventForwarder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import antlr.StringUtils;

/**
 * @author tf016851
 */
public class ApicService {
    
    private static final Logger LOG = LoggerFactory.getLogger(ApicService.class);

    static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    
    public static final String DN_SEP = "/";
    
    public static final String FS_SEP = "~";
    
    final int pollDurationMinutes = 5;

    public final static String APIC_CONFIG_LOCATION_KEY = "Location";
    public final static String APIC_CONFIG_URL_KEY = "URL";
    public final static String APIC_CONFIG_USERNAME_KEY = "Username";
    public final static String APIC_CONFIG_PASSWORD_KEY = "Password";
    public final static String APIC_CONFIG_POLL_DURATION_KEY = "pollDuration";
    public final static String APIC_CONFIG_EVENT_FORWARDER = "EventForwarder";
    public final static String APIC_CONFIG_EVENT_DAO = "EventDao";
    public final static String APIC_CONFIG_NODE_CACHE = "NodeCache";
    public final static String APIC_CONFIG_CLUSTER_MAP = "ClusterMap";
    public final static String APIC_CONFIG_LOCAL_ADDR = "localAddr";
    
    public final static String APIC_CLUSTER_MAP_LAST_PROCESS_TIME = "lastProcessTime";
    
    @Autowired
    private EventForwarder eventForwarder;
    
    private SouthboundConfigDao southboundConfigDao;
    
    private EventDao eventDao;
    
    private NodeDao nodeDao;
    
    private Scheduler scheduler = null;
    
    private Map<String, Map<String, Object>> clusterMap;
    
    private String localAddr;

    public void init() {

        Logging.putPrefix("aci");
        LOG.info("Initializaing ApicService ...");
        localAddr = InetAddressUtils.getLocalHostName();
        clusterMap = new HashMap<String, Map<String, Object>>();
        
        if (scheduler != null) {
            try {
                scheduler.shutdown(true);
            } catch (SchedulerException e) {
                LOG.debug("Error stopping scheduler.", e);
            }
            scheduler = null;
        }
        
        List<SouthCluster> clusters = this.southboundConfigDao.getSouthboundClusters();
        for (SouthCluster southCluster : clusters) {
            if (southCluster.getClusterType().equals("CISCO-ACI")) {
                String location = southCluster.getClusterName();
                //Build URL
                String url = "";
                String username = "";
                String password = "";
                List<SouthElement> elements = southCluster.getElements();
                for (SouthElement element : elements ){
                    url += "https://" + element.getHost() + ":"  + element.getPort() + ",";
                    username = element.getUserid();
                    password = element.getPassword();
                }
                this.createAndScheduleJob(location, org.apache.commons.lang.StringUtils.chomp(url, ","), 
                        username, password, southCluster.getPollDurationMinutes());
            }
        }
    }

    public void destroy() {

        LOG.info("Destorying ApicService ...");
        try {
            if (scheduler != null)
                scheduler.shutdown(true);
        } catch (SchedulerException e) {
            LOG.debug("Error shutting down scheduler", e);
        }

    }

    public void createAndScheduleJob(String location, String apicUrl, String username, String password, int pollDuration) {
        String jobIdentity = ApicClusterJob.class.getSimpleName() + "-" + location;
        LOG.info("Creating job: " + jobIdentity);
        
        JobDetail job = JobBuilder.newJob(ApicClusterJob.class).withIdentity(jobIdentity, ApicClusterJob.class.getSimpleName())
                    .usingJobData(APIC_CONFIG_LOCATION_KEY, location)
                    .usingJobData(APIC_CONFIG_URL_KEY, apicUrl)
                    .usingJobData(APIC_CONFIG_USERNAME_KEY, username)
                    .usingJobData(APIC_CONFIG_PASSWORD_KEY, password)
                    .usingJobData(APIC_CONFIG_POLL_DURATION_KEY, pollDuration)
                    .usingJobData(APIC_CONFIG_LOCAL_ADDR, localAddr)
                    .storeDurably()
                    .build();
        
        NodeCache nodeCache = new NodeCache();
        nodeCache.setNodeDao(nodeDao);
        nodeCache.init();
        
        Map<String, Object> clusterJobMap = new HashMap<String, Object>();
        
        clusterMap.put(job.getKey().toString(), clusterJobMap);

        // Trigger the job to run on the next round minute
        String triggerIdentity = ApicService.class.getSimpleName() + "-Trigger-" + location;
        Trigger trigger = TriggerBuilder.newTrigger()
                            .withIdentity(triggerIdentity, "org.opennms.aci")
                            .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMinutes(pollDuration)
                            .withMisfireHandlingInstructionFireNow()
                            .repeatForever())
                            .build();

        try {
            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.getContext().put(APIC_CONFIG_EVENT_FORWARDER, eventForwarder);
            scheduler.getContext().put(APIC_CONFIG_EVENT_DAO, eventDao);
            scheduler.getContext().put(APIC_CONFIG_NODE_CACHE, nodeCache);
            scheduler.getContext().put(APIC_CONFIG_CLUSTER_MAP, clusterMap);
            scheduler.start();
            
            if (!scheduler.checkExists(job.getKey()))
                scheduler.scheduleJob(job, trigger);
            
        } catch (SchedulerException e) {
            LOG.error("Error executing job.", e);
        }

    }
    
    /**
     * @return the eventForwarder
     */
    public EventForwarder getEventForwarder() {
        return eventForwarder;
    }

    /**
     * @param eventForwarder the eventForwarder to set
     */
    public void setEventForwarder(EventForwarder eventForwarder) {
        this.eventForwarder = eventForwarder;
    }

    public void setSouthboundConfigDao(SouthboundConfigDao southboundConfigDao) {
        this.southboundConfigDao = southboundConfigDao;
    }

    public EventDao getEventDao() {
        return eventDao;
    }

    public void setEventDao(EventDao eventDao) {
        this.eventDao = eventDao;
    }

    public NodeDao getNodeDao() {
        return nodeDao;
    }

    public void setNodeDao(NodeDao nodeDao) {
        this.nodeDao = nodeDao;
    }

}
