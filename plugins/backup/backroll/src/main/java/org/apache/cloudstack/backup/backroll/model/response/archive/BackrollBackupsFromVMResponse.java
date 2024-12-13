package org.apache.cloudstack.backup.backroll.model.response.archive;

import org.apache.cloudstack.backup.backroll.model.response.BackrollAsyncResponse;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackrollBackupsFromVMResponse extends BackrollAsyncResponse {
    @JsonProperty("info")
    public BackrollArchivesResponse archives;
}