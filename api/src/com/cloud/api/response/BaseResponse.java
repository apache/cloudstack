package com.cloud.api.response;

import com.cloud.api.ResponseObject;

public class BaseResponse implements ResponseObject {
    private transient String responseName;
    private transient String objectName;

    @Override
    public String getResponseName() {
        return responseName;
    }

    @Override
    public void setResponseName(String responseName) {
        this.responseName = responseName;
    }
    
    @Override
    public String getObjectName() {
        return objectName;
    }

    @Override
    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

}
