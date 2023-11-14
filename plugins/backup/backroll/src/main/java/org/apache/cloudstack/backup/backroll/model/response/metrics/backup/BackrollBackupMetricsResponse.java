package org.apache.cloudstack.backup.backroll.model.response.metrics.backup;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackrollBackupMetricsResponse {
    @JsonProperty("state")
    public String state;

    @JsonProperty("info")
    public BackupMetricsInfo info;
}
