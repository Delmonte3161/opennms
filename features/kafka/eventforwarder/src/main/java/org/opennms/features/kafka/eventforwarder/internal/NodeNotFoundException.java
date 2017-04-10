package org.opennms.features.kafka.eventforwarder.internal;

public class NodeNotFoundException extends Exception
{

    /**
     * 
     */
    private static final long serialVersionUID = 8806385703174708104L;

    public NodeNotFoundException ()
    {

    }

    public NodeNotFoundException ( String message )
    {
        super( message );
    }

    public NodeNotFoundException ( String message, Throwable cause )
    {
        super( message, cause );
    }

    public NodeNotFoundException ( Throwable cause )
    {
        super( cause );
    }

    public NodeNotFoundException ( String message, Throwable cause, boolean enableSuppression,
                    boolean writeableStackTrace )
    {
        super( message, cause, enableSuppression, writeableStackTrace );
    }
}
