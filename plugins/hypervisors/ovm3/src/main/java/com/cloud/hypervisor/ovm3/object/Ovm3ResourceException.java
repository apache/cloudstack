/**
 *
 */
package com.cloud.hypervisor.ovm3.object;

public class Ovm3ResourceException extends Exception {
    private static final long serialVersionUID = 1L;
    private static final Throwable cause = null;
    public Ovm3ResourceException() {
        super();
    }

    public Ovm3ResourceException(String message) {
        super(message);
    }

    public Ovm3ResourceException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public Throwable getCause() {
        return cause;
    }
}
