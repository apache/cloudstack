package org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachineBackups;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VirtualMachineBackupsResponse {
    @JsonProperty("state")
    public String state;

    @JsonProperty("info")
    public Archives info;
}
