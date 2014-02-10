package org.apache.cloudstack.storage.datastore.model;

import java.util.HashMap;

import com.google.gson.annotations.SerializedName;

public class QoSGroup {

    @SerializedName("id")
    private String uuid;

    @SerializedName("name")
    private String name;

    @SerializedName("qosgroupproperties")
    private HashMap<String, String> qosGroupProperties;

    public String getName() {
        return name;
    }

    public String getUuid() {
        return uuid;
    }

    public String getIops() {
        return qosGroupProperties.get("iops");
    }

    public String getThroughput() {
        return qosGroupProperties.get("throughput");
    }

    public String getLatency() {
        return qosGroupProperties.get("latency");
    }
}
