package org.apache.cloudstack.backup.backroll.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackrollTaskRequestResponse {
    @JsonProperty("Location")
    public String location;
}
