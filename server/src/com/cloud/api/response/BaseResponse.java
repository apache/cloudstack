package com.cloud.api.response;

import com.cloud.api.ResponseObject;

public class BaseResponse implements ResponseObject {
    private String responseName;

    @Override
    public String getResponseName() {
        return responseName;
    }

    @Override
    public void setResponseName(String responseName) {
        this.responseName = responseName;
    }

}
