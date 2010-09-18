package com.cloud.api.response;

import java.util.List;

import com.cloud.api.ResponseObject;

public class ListResponse extends BaseResponse {
    List<? extends ResponseObject> responses;

    public List<? extends ResponseObject> getResponses() {
        return responses;
    }

    public void setResponses(List<? extends ResponseObject> responses) {
        this.responses = responses;
    }
}
