package com.cloud.api.response;

import com.cloud.api.ResponseObject;
import com.cloud.serializer.Param;

public class DeleteDomainResponse implements ResponseObject {
    @Param(name="result")
    private String result;

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}
