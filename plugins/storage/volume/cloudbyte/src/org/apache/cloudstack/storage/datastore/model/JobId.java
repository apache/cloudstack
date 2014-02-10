package org.apache.cloudstack.storage.datastore.model;

import com.google.gson.annotations.SerializedName;

public class JobId {

    @SerializedName("jobid")
    private String jobid;

    @SerializedName("success")
    private String jobStatus;

    public String getJobid() {
        return jobid;
    }

    public String getJobStatus() {
        return jobStatus;
    }

}
