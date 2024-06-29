package org.apache.cloudstack.backup.backroll.model.response.archive;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackrollBackupsFromVMResponse {
    @JsonProperty("state")
    public String state;

    @JsonProperty("info")
    public BackrollArchivesResponse archives;
}
