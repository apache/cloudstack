package com.cloud.api.response;

import java.util.List;

import com.cloud.api.ResponseObject;

public class ListResponse<T extends ResponseObject> extends BaseResponse {
    List<T> responses;

    public List<T> getResponses() {
        return responses;
    }

    public void setResponses(List<T> responses) {
        this.responses = responses;
    }

    public Integer getCount() {
        if (responses != null) {
            return responses.size();
        } else {
            return null;
        }
    }
}
