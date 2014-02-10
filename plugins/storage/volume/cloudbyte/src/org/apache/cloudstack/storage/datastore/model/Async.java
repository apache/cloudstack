package org.apache.cloudstack.storage.datastore.model;

import com.google.gson.annotations.SerializedName;

public class Async {

    @SerializedName("jobstatus")
    private int jobstatus;

    @SerializedName("cmd")
    private String cmd;

    public int getJobStatus() {
        return jobstatus;
    }

    public String getCmd() {
        return cmd;
    }

}
