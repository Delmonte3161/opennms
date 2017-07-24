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
import java.util.GregorianCalendar;

import org.opennms.aci.rpc.rest.client.ACIRestClient;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author tf016851
 *
 */
public class ApicClusterJob implements Job {

    private static final Logger LOG = LoggerFactory.getLogger(ApicClusterJob.class);
    
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    ACIRestClient client;

    public ApicClusterJob() {
        LOG.debug("Initializing ApicClusterJob ...");
    }

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        if (client == null) {
            try {
                LOG.info("Initializing ACIRestClient with: \n" +
                         ApicService.APIC_CONFIG_LOCATION_KEY + ": " + (String) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_LOCATION_KEY) + "\n" +
                         ApicService.APIC_CONFIG_URL_KEY + ": " + (String) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_URL_KEY) + "\n" +
                         ApicService.APIC_CONFIG_USERNAME_KEY + ": " + (String) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_USERNAME_KEY) 
                        );
                client = ACIRestClient.newAciRest((String) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_LOCATION_KEY), 
                                                (String) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_URL_KEY), 
                                                (String) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_USERNAME_KEY), 
                                                (String) context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_PASSWORD_KEY));
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
        int pollDuration = (int)context.getMergedJobDataMap().get(ApicService.APIC_CONFIG_POLL_DURATION_KEY);
        final java.util.Calendar startCal = GregorianCalendar.getInstance();
        startCal.add(GregorianCalendar.MINUTE, pollDuration * -1);

        // TODO - Still need to add logic for un-marshalling returned JSON
        // fault records.
        try {
            client.getCurrentFaults(client.getTimeStamp(pollDuration * 60));
        } catch (Exception e) {
            throw new JobExecutionException(e, false);
        }

    }

}
