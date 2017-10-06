/**
 * 
 */
package org.opennms.aci.module;

/**
 * @author tf016851
 *
 */
public class NodeCacheKeyNotFoundException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public NodeCacheKeyNotFoundException() {
        super();
    }

    public NodeCacheKeyNotFoundException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public NodeCacheKeyNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NodeCacheKeyNotFoundException(String message) {
        super(message);
    }

    public NodeCacheKeyNotFoundException(Throwable cause) {
        super(cause);
    }

}
