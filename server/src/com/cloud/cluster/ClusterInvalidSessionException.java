package com.cloud.cluster;

public class ClusterInvalidSessionException extends Exception {

	private static final long serialVersionUID = -6636524194520997512L;

    public ClusterInvalidSessionException(String message) {
        super(message);
    }

    public ClusterInvalidSessionException(String message, Throwable th) {
        super(message, th);
    }
}

