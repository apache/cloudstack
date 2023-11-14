package org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachineBackups;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Archives {
    @JsonProperty("archives")
    public List<BackupInfos> archives;

    @JsonProperty(value = "encryption", required = false)
    public InfosEncryption encryption;

    @JsonProperty(value = "repository", required = false)
    public InfosRepository repository;

    @JsonProperty(value = "state", required = false)
    public String state;
}
