/**
 * 
 */
package org.opennms.aci.rpc.rest.client;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tf016851
 *
 */
public class Credentials
{

    private List<ACIRestConfig> credentials = new ArrayList<ACIRestConfig>();

    /**
     * @return the credentials
     */
    public List<ACIRestConfig> getCredentials()
    {
        return credentials;
    }

    /**
     * @param credentials the credentials to set
     */
    public void setCredentials( List<ACIRestConfig> credentials )
    {
        this.credentials = credentials;
    }

    /**
     * @param cluster
     * @param username
     * @return ACIRestConfig
     */
    public ACIRestConfig findCrednetial( String cluster, String username )
    {
        ACIRestConfig config = null;
        for ( ACIRestConfig c : this.credentials )
        {
            if ( cluster.equals( c.getClusterName() ) && username.equals( c.getUsername() ) )
            {
                config = c;
            }
        }

        return config;
    }

    /**
     * @param config
     */
    public void addCredential( ACIRestConfig config )
    {
        List<ACIRestConfig> newCreds = new ArrayList<ACIRestConfig>();
        boolean found = false;
        for ( ACIRestConfig c : this.credentials )
        {
            if ( config.getClusterName().equals( c.getClusterName() )
                            && config.getUsername().equals( c.getUsername() ) )
            {
                if (!c.equals( config ))
                    newCreds.add( config );
                else
                    newCreds.add( c );
                found = true;
            }
            else
            {
                newCreds.add( c );
            }
        }

        if ( !found )
            newCreds.add( config );
        this.credentials = newCreds;
    }
}
