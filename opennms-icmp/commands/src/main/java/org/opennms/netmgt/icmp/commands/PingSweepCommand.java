/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014-2016 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.icmp.commands;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opennms.netmgt.icmp.proxy.LocationAwarePingSweepClient;
import org.opennms.netmgt.icmp.proxy.PingSweepSummary;

@Command(scope = "ping", name = "sweep", description = "Ping-Sweep")
public class PingSweepCommand extends OsgiCommandSupport {

    private LocationAwarePingSweepClient client;

    @Option(name = "-l", aliases = "--location", description = "Location", required = false, multiValued = false)
    String m_location = "Default";

    @Option(name = "-r", aliases = "--retries", description = "number of retries")
    int m_retries;

    @Option(name = "-t", aliases = "--timeout", description = "timeout in msec")
    int m_timeout;

    @Option(name = "-p", aliases = "--packetsize", description = "packet size")
    int m_packetsize;

    @Argument(index = 0, name = "begin", description = "begin address of the IP range to be pinged", required = true, multiValued = false)
    String m_begin;

    @Argument(index = 1, name = "end", description = "end address of the IP range to be pinged", required = true, multiValued = false)
    String m_end;

    @Override
    protected Object doExecute() throws Exception {

        System.out.printf("ping:sweep %s  begin=%s   end=%s \n", m_location != null ? "-l " + m_location : "", m_begin,
                m_end);

        final CompletableFuture<PingSweepSummary> future = client.ping()
                .withRange(InetAddress.getByName(m_begin), InetAddress.getByName(m_end)).withLocation(m_location)
                .withRetries(m_retries).withTimeout(m_timeout, TimeUnit.MILLISECONDS).withPacketSize(m_packetsize)
                .execute();

        while (true) {
            try {
                try {
                    PingSweepSummary summary = future.get(1, TimeUnit.SECONDS);
                    if (summary.getResponses().isEmpty()) {
                        System.out.printf("Not able to ping any IPs in given range %s-%s", m_begin, m_end);
                    }
                    System.out.printf("IpAddress : ElapsedTime \n");
                    summary.getResponses().forEach((address, rtt) -> {
                        System.out.printf(" %s : %.3f ms  \n", address, rtt);
                    });
                } catch (InterruptedException e) {
                    System.out.println("\nInterrupted.");
                } catch (ExecutionException e) {
                    System.out.printf("\n Ping Sweep failed with: %s\n", e);
                }
                break;
            } catch (TimeoutException e) {
                // pass
            }
            System.out.print(".");
        }
        return null;
    }

    public void setClient(LocationAwarePingSweepClient client) {
        this.client = client;
    }

}
