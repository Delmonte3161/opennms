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

/**
 * @author tf016851
 */
public class ApicService {
    
    private static final Logger LOG = LoggerFactory.getLogger(ApicService.class);

    
    final int pollDurationMinutes = 5;

    final String location = "LS6";
    final String url = "https://7.192.80.10,https://7.192.80.11,https://7.192.80.12";
    final String userName = "svcOssAci";
    final String pw = "kf3as=Nx";

    public final static String APIC_CONFIG_LOCATION_KEY = "Location";
    public final static String APIC_CONFIG_URL_KEY = "URL";
    public final static String APIC_CONFIG_USERNAME_KEY = "Username";
    public final static String APIC_CONFIG_PASSWORD_KEY = "Password";
    public final static String APIC_CONFIG_POLL_DURATION_KEY = "pollDuration";

    Scheduler scheduler;

    public void init() {

        LOG.info("Initializaing ApicService ...");
        // JobBuilder.newJob().

        // TODO - this needs to be replaced with logic to build job for each
        // configured cluster
        this.createAndScheduleJob(location, url, userName, pw, pollDurationMinutes);
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
        JobDetail job = JobBuilder.newJob(ApicClusterJob.class).withIdentity(ApicClusterJob.class.getSimpleName()
                + location, ApicClusterJob.class.getSimpleName())
                    .usingJobData(APIC_CONFIG_LOCATION_KEY, location)
                    .usingJobData(APIC_CONFIG_URL_KEY, apicUrl)
                    .usingJobData(APIC_CONFIG_USERNAME_KEY, username)
                    .usingJobData(APIC_CONFIG_PASSWORD_KEY, password)
                    .usingJobData(APIC_CONFIG_POLL_DURATION_KEY, pollDuration)
                    .build();

        // Trigger the job to run on the next round minute
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity(ApicService.class.getSimpleName(), "org.opennms.aci").withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(pollDuration).repeatForever()).build();

        try {
            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            LOG.error("Error executing job.", e);
        }


    }
}
