/**
 * 
 */
package org.opennms.aci.rpc.rest.client;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opennms.aci.rpc.rest.client.ACIRestConfig;

/**
 * @author tf016851
 *
 */
public class ACIRestConfigTest
{

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception
    {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
    }

    @Test
    public void test()
    {
        ACIRestConfig config = new ACIRestConfig();
        
        config.setAciUrl( "URL" );
        config.setClusterName( "TEST" );
        config.setUsername( "TEST" );
        config.setPasswordEncrypt( "TEST" );
        
        assertEquals( "URL", config.getAciUrl() );
        assertEquals( "TEST", config.getClusterName() );
        assertEquals( "TEST", config.getPasswordClearText() );
        assertNotEquals( "TEST", config.getPassword() );
    }

}
