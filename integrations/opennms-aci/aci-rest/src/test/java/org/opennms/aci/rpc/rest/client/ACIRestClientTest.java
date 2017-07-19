/**
 * 
 */
package org.opennms.aci.rpc.rest.client;

import org.opennms.aci.rpc.rest.client.ACIRestClient;

/**
 * @author tf016851
 *
 */
public class ACIRestClientTest
{
    /**
     * @param args
     */
    public static void main( String[] args )
    {
        final String url = "https://7.192.80.10,https://7.192.80.11,https://7.192.80.12";
        final String userName = "svcOssAci";
        final String pw = "kf3as=Nx";

        try
        {
            ACIRestClient client = ACIRestClient.newAciRest( "LS", url, userName, pw );
            
            client.getClassInfo(  "faultRecord" );
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
    }

}
