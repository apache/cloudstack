package org.apache.cloudstack.storage.datastore.response;

import org.apache.cloudstack.storage.datastore.model.JobId;

import com.google.gson.annotations.SerializedName;

public class DeleteTsmResponse {

    @SerializedName("deleteTsmResponse")
    private JobId jobId;

    public String getJobStatus() {
        return jobId.getJobStatus();
    }

}
