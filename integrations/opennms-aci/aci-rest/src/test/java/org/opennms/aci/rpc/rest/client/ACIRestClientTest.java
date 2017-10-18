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

package org.opennms.aci.rpc.rest.client;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import org.opennms.aci.rpc.rest.client.ACIRestClient;

/**
 * @author tf016851
 *
 */
public class ACIRestClientTest
{
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    
    /**
     * @param args
     */
    public static void main( String[] args )
    {
        //LS6
//        final String cluster = "ls6apic";
//        final String url = "https://7.192.80.10,https://7.192.80.11,https://7.192.80.12";
        //KC8
        final String cluster = "kc8apic";
        final String url = "https://7.192.240.10,https://7.192.240.11,https://7.192.240.12";
//        final String url = "https://bogus,https://bogus,https://7.192.80.12";
        final String userName = "svcOssAci";
        final String pw = "kf3as=Nx";

        try
        {
            final java.util.Calendar startCal = GregorianCalendar.getInstance();
            startCal.add(GregorianCalendar.MINUTE, -100);
            
            ACIRestClient client = ACIRestClient.newAciRest( cluster, url, userName, pw );
            
//            client.getCurrentFaults(format.format(startCal.getTime()));
//            client.getClassInfo(  "faultRecord" );
//            client.getClassInfo(  "faultRecord", "eventRecord" );
            client.getClassInfo( "topSystem" );
//            client.getClassInfo( "ethpmPhysIf" );
//            client.getManagedObject( "topology/pod-1/node-155/sys/phys-[eth1/18]/phys" );
//            client.getManagedObject( "topology/pod-1/node-155/sys" );
//            client.getManagedObjectSubtree( "topology/pod-1/node-155/sys", "ethpmPhysIf" );
//            client.getManagedObject( "dbgs/ac/path-121-to-122" );
//            client.getManagedObjectSubtree( "dbgs/ac/path-121-to-122", "topSystem" );
            
//            client.getHealth( "fvAp", "fvTenant", "fabricNode" );
//            client.getStats( "fvAp" );
        }
        catch ( Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
