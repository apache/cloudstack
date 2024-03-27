package org.apache.cloudstack.backup.backroll.model.response.backup;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackrollBackupStatusSuccessResponse {
    @JsonProperty("state")
    public String state;

    @JsonProperty("info")
    public String info;
}
