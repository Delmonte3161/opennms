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

package org.opennms.netmgt.discovery.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.opennms.netmgt.model.discovery.IPPollRange;

public class DiscoveryJobTest {

    @Test
    public void testCalculateTaskTimeout() throws Exception {
        final List<IPPollRange> m_ranges = new ArrayList<IPPollRange>();
        for (int i = 1 ; i < 12; i += 5) {
            IPPollRange ipPollRange = new IPPollRange("127.0.1." + i, "127.0.1." + (i + 4), 50, 2);
            m_ranges.add(ipPollRange);
        }
        DiscoveryJob discoveryJob = new DiscoveryJob(m_ranges, "Bogus FS", "Bogus Location");
        // Each task is taking 750 ms so totalTaskTimeout = 750 ms * 3 (number of tasks) = 2250 ms
        assertEquals(Math.round(2250 * 1.5), discoveryJob.calculateTaskTimeout()); 
        assertFalse(discoveryJob.calculateTaskTimeout() > Integer.MAX_VALUE);
    }

    @Test
    public void testTimeoutLargerThanIntegerMaxValue() throws Exception {
        // TODO: Write this test
    }
}
