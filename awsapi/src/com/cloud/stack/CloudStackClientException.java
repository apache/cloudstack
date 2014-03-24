package com.cloud.stack;

/**
 * @author Dmitry Batkovich
 */
public class CloudStackClientException extends Exception {
    public CloudStackClientException() {
    }

    public CloudStackClientException(final String s) {
        super(s);
    }

    public CloudStackClientException(final String s, final Throwable throwable) {
        super(s, throwable);
    }

    public CloudStackClientException(final Throwable throwable) {
        super(throwable);
    }
}
