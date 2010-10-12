package com.cloud.api.response;

import com.cloud.api.ResponseObject;

public class BaseResponse implements ResponseObject {
    private transient String responseName;

    @Override
    public String getResponseName() {
        return responseName;
    }

    @Override
    public void setResponseName(String responseName) {
        this.responseName = responseName;
    }

}
