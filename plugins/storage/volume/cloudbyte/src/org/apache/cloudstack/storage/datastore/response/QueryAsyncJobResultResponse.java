package org.apache.cloudstack.storage.datastore.response;

import org.apache.cloudstack.storage.datastore.model.Async;

import com.google.gson.annotations.SerializedName;

public class QueryAsyncJobResultResponse {

    @SerializedName("queryasyncjobresultresponse")
    private Async async;

    public Async getAsync() {
        return async;
    }
}
