package org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachineBackups;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackupInfos {
    @JsonProperty("archive")
    public String archive;

    @JsonProperty("barchive")
    public String barchive;

    @JsonProperty("id")
    public String id;

    @JsonProperty("name")
    public String name;

    @JsonProperty("start")
    public String start;

    @JsonProperty("time")
    public String time;
}
