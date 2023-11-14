package org.apache.cloudstack.backup.backroll.model.response.archive;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BackrollArchivesResponse {
    @JsonProperty("archives")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    public List<BackrollArchiveResponse> archives;
}
