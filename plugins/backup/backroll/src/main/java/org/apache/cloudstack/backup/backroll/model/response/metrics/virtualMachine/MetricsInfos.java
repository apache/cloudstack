package org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachine;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetricsInfos {
    @JsonProperty("cache")
    public InfosCache cache;

    @JsonProperty("encryption")
    public InfosEncryption encryption;

    @JsonProperty("repository")
    public InfosRepository repository;

    @JsonProperty("security_dir")
    public String securityDir;
}
