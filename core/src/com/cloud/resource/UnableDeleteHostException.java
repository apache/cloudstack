package com.cloud.resource;

import com.cloud.utils.SerialVersionUID;

public class UnableDeleteHostException extends Exception {
    private static final long serialVersionUID = SerialVersionUID.UnableDeleteHostException;
    
    public UnableDeleteHostException(String msg) {
        super(msg);
    }
}
