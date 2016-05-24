/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2015-2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.poller;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.opennms.core.concurrent.TimeoutTracker;
import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.poller.support.MockMonitoredService;

public class MonitoredServiceTaskTest {

    private static final MonitoredService MOCK_SERVICE = new MockMonitoredService(1, "localhost", InetAddressUtils.ONE_TWENTY_SEVEN, "ICMP");

    @Test
    public void testCalculateTaskTimeout() throws Exception {

        // Calculate the timeout with default retries and timeouts
        MonitoredServiceTask task = new MonitoredServiceTask(MOCK_SERVICE, Collections.emptyMap());
        assertEquals(Math.round(TimeoutTracker.DEFAULT_TIMEOUT * (TimeoutTracker.DEFAULT_RETRY + 1) * 1.5), task.calculateTaskTimeout());


        // Manipulate the retries
        task = new MonitoredServiceTask(MOCK_SERVICE, Collections.singletonMap(TimeoutTracker.PARM_RETRY, 0));
        assertEquals(Math.round(TimeoutTracker.DEFAULT_TIMEOUT * 1.5), task.calculateTaskTimeout());

        task = new MonitoredServiceTask(MOCK_SERVICE, Collections.singletonMap(TimeoutTracker.PARM_RETRY, 1));
        assertEquals(Math.round(TimeoutTracker.DEFAULT_TIMEOUT * 2 * 1.5), task.calculateTaskTimeout());

        task = new MonitoredServiceTask(MOCK_SERVICE, Collections.singletonMap(TimeoutTracker.PARM_RETRY, Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, task.calculateTaskTimeout());


        // Manipulate the timeouts
        task = new MonitoredServiceTask(MOCK_SERVICE, Collections.singletonMap(TimeoutTracker.PARM_TIMEOUT, 100));
        assertEquals(Math.round(100 * (TimeoutTracker.DEFAULT_RETRY + 1) * 1.5), task.calculateTaskTimeout());

        task = new MonitoredServiceTask(MOCK_SERVICE, Collections.singletonMap(TimeoutTracker.PARM_TIMEOUT, 33));
        assertEquals(Math.round(33 * (TimeoutTracker.DEFAULT_RETRY + 1) * 1.5), task.calculateTaskTimeout());

        task = new MonitoredServiceTask(MOCK_SERVICE, Collections.singletonMap(TimeoutTracker.PARM_TIMEOUT, Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, task.calculateTaskTimeout());


        // Manipulate both of the parameters
        Map<String,Object> params = new HashMap<>();

        params.put(TimeoutTracker.PARM_TIMEOUT, 100);
        params.put(TimeoutTracker.PARM_RETRY, 5);
        task = new MonitoredServiceTask(MOCK_SERVICE, params);
        assertEquals(Math.round(100 * (5 + 1) * 1.5), task.calculateTaskTimeout());


        // Use String values to test ParameterMap.getKeyedInteger()
        params.put(TimeoutTracker.PARM_TIMEOUT, "100");
        params.put(TimeoutTracker.PARM_RETRY, 5);
        task = new MonitoredServiceTask(MOCK_SERVICE, params);
        assertEquals(Math.round(100 * (5 + 1) * 1.5), task.calculateTaskTimeout());

        params.put(TimeoutTracker.PARM_TIMEOUT, 200);
        params.put(TimeoutTracker.PARM_RETRY, "5");
        task = new MonitoredServiceTask(MOCK_SERVICE, params);
        assertEquals(Math.round(200 * (5 + 1) * 1.5), task.calculateTaskTimeout());

        params.put(TimeoutTracker.PARM_TIMEOUT, "100");
        params.put(TimeoutTracker.PARM_RETRY, "3");
        task = new MonitoredServiceTask(MOCK_SERVICE, params);
        assertEquals(Math.round(100 * (3 + 1) * 1.5), task.calculateTaskTimeout());
    }
}
