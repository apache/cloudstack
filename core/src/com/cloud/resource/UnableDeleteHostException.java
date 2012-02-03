package com.cloud.resource;

import com.cloud.utils.SerialVersionUID;
import com.cloud.exception.CloudException;

public class UnableDeleteHostException extends CloudException {
    private static final long serialVersionUID = SerialVersionUID.UnableDeleteHostException;
    
    public UnableDeleteHostException(String msg) {
        super(msg);
    }
}
