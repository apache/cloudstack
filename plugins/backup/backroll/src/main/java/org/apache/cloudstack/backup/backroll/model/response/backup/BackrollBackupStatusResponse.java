package org.apache.cloudstack.backup.backroll.model.response.backup;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackrollBackupStatusResponse {
    @JsonProperty("state")
    public String state;

    @JsonProperty("current")
    public String current;

    @JsonProperty("total")
    public String total;

    @JsonProperty("status")
    public String status;
}