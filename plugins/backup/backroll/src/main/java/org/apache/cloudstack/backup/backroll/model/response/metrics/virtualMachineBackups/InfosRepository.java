package org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachineBackups;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InfosRepository {
    @JsonProperty("id")
    public String id;

    @JsonProperty("last_modified")
    public String lastModified;

    @JsonProperty("location")
    public String location;
}
