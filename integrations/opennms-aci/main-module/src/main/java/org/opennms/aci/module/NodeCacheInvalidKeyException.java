/**
 * 
 */
package org.opennms.aci.module;

/**
 * @author tf016851
 *
 */
public class NodeCacheInvalidKeyException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;


    public NodeCacheInvalidKeyException() {
        super();
    }

    public NodeCacheInvalidKeyException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public NodeCacheInvalidKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public NodeCacheInvalidKeyException(String message) {
        super(message);
    }

    public NodeCacheInvalidKeyException(Throwable cause) {
        super(cause);
    }

}
