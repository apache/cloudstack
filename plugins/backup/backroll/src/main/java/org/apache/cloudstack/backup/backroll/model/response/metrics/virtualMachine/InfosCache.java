package org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachine;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InfosCache {
    @JsonProperty("path")
    public String path;

    @JsonProperty("stats")
    public CacheStats stats;
}
