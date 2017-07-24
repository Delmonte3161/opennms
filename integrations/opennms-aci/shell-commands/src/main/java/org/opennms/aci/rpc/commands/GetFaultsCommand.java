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

package org.opennms.aci.rpc.commands;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opennms.aci.rpc.rest.client.ACIRestClient;


/**
 * @author tf016851
 */
@Command(scope = "aci", name = "get-faults", description="Gets faults from ACI")
public class GetFaultsCommand extends OsgiCommandSupport {
    
    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    
    @Option(name = "-l", aliases = "--location", description = "Location", required=true, multiValued=false)
    private String location;

    @Option(name = "-a", aliases = "--aci-url", description = "ACI URL", required=true, multiValued=false)
    private String aciUrl;

    @Option(name = "-u", aliases = "--username", description = "Username", required=true, multiValued=false)
    private String username;

    @Option(name = "-p", aliases = "--password", description = "Password", required=true, multiValued=false)
    public String password;
    
    @Option(name = "-d", aliases = "--duration", description = "Fault Polling Duration in minutes.", required=false, multiValued=false)
    public int pollDuration = 5;


    /*
     * (non-Javadoc)
     * 
     * @see org.apache.karaf.shell.console.AbstractAction#doExecute()
     */
    @Override
    protected Object doExecute() throws Exception {
        final java.util.Calendar startCal = GregorianCalendar.getInstance();
        startCal.add(GregorianCalendar.MINUTE, this.pollDuration * -1);
        try
        {
            ACIRestClient client = ACIRestClient.newAciRest( location, aciUrl, username, password );
            
            client.getCurrentFaults(format.format(startCal.getTime()));
//            client.getClassInfo(  "faultRecord" );
//            client.getClassInfo(  "faultRecord", "eventRecord" );
//            client.getClassInfo( "topSystem" );
            
//            client.getHealth( "fvAp", "fvTenant", "fabricNode" );
//            client.getStats( "fvAp" );
        }
        catch ( Exception e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

}
