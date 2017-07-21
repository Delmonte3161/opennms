/*******************************************************************************
 * This file is part of OpenNMS(R). Copyright (C) 2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc. OpenNMS(R) is
 * a registered trademark of The OpenNMS Group, Inc. OpenNMS(R) is free
 * software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. OpenNMS(R) is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details. You should have received a copy of the GNU Affero
 * General Public License along with OpenNMS(R). If not, see:
 * http://www.gnu.org/licenses/ For more information contact: OpenNMS(R)
 * Licensing <license@opennms.org> http://www.opennms.org/
 * http://www.opennms.com/
 *******************************************************************************/

package org.opennms.aci.module;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import org.opennms.aci.rpc.rest.client.ACIRestClient;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

/**
 * @author tf016851
 */
public class ApicService {

    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    final int pollDuration = 5;

    final String location = "LS6";
    final String url = "https://7.192.80.10,https://7.192.80.11,https://7.192.80.12";
    final String userName = "svcOssAci";
    final String pw = "kf3as=Nx";

    final static String APIC_CONFIG_LOCATION_KEY = "Location";
    final static String APIC_CONFIG_URL_KEY = "URL";
    final static String APIC_CONFIG_USERNAME_KEY = "Username";
    final static String APIC_CONFIG_PASSWORD_KEY = "Password";

    Trigger trigger;
    Scheduler scheduler;

    public void init() {

        // JobBuilder.newJob().

        // TODO - this needs to be replaced with logic to build job for each
        // configured cluster
        JobDetail job = JobBuilder.newJob(ApicClusterJob.class).withIdentity(ApicClusterJob.class.getSimpleName()
                + location, ApicClusterJob.class.getSimpleName()).usingJobData(APIC_CONFIG_LOCATION_KEY, location).usingJobData(APIC_CONFIG_URL_KEY, url).usingJobData(APIC_CONFIG_USERNAME_KEY, userName).usingJobData(APIC_CONFIG_PASSWORD_KEY, pw).build();

        // Trigger the job to run on the next round minute
        trigger = TriggerBuilder.newTrigger().withIdentity(ApicService.class.getSimpleName(), "org.opennms.aci").withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInMinutes(pollDuration).repeatForever()).build();

        try {
            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.start();
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void destroy() {

        try {
            scheduler.shutdown(true);
        } catch (SchedulerException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    class ApicClusterJob implements Job {

        ACIRestClient client;

        @Override
        public void execute(JobExecutionContext context)
                throws JobExecutionException {
            if (client == null) {
                try {
                    client = ACIRestClient.newAciRest((String) context.get(APIC_CONFIG_LOCATION_KEY), (String) context.get(APIC_CONFIG_URL_KEY), (String) context.get(APIC_CONFIG_USERNAME_KEY), (String) context.get(APIC_CONFIG_PASSWORD_KEY));
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

            if (client == null)
                throw new JobExecutionException(context.getJobDetail().getDescription()
                        + ": Failed to initialize client.");

            // TODO - Need to add logic for determining the last fault process
            // time.
            // For now, just query for the same time interval for testing
            final java.util.Calendar startCal = GregorianCalendar.getInstance();
            startCal.add(GregorianCalendar.MINUTE, pollDuration * -1);

            // TODO - Still need to add logic for un-marshalling returned JSON
            // fault records.
            try {
                client.getCurrentFaults(format.format(startCal.getTime()));
            } catch (Exception e) {
                throw new JobExecutionException(e, false);
            }

        }

    }

}
