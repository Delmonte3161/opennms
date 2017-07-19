package org.opennms.aci.rpc.rest.client;

/**
 * Exception in oci rest sample.
 */
public class ACIRestException extends Exception
{
    ACIRestException ( String message )
    {
        super( message );
    }
}
