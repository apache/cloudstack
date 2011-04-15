package com.cloud.exception;

import com.cloud.utils.SerialVersionUID;

public class DiscoveredWithErrorException extends DiscoveryException {
    
    private static final long serialVersionUID = SerialVersionUID.DiscoveredWithErrorException;

    public DiscoveredWithErrorException(String msg) {
        this(msg, null);
    }
    
    public DiscoveredWithErrorException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
