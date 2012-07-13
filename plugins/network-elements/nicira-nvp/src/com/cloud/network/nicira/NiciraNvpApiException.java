package com.cloud.network.nicira;

public class NiciraNvpApiException extends Exception {

    public NiciraNvpApiException() {
    }

    public NiciraNvpApiException(String message) {
        super(message);
    }

    public NiciraNvpApiException(Throwable cause) {
        super(cause);
    }

    public NiciraNvpApiException(String message, Throwable cause) {
        super(message, cause);
    }

}
