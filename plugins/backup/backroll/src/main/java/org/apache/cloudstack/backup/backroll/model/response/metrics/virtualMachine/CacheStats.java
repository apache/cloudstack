package org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachine;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CacheStats {
    @JsonProperty("total_chunks")
    public String totalChunks;

    @JsonProperty("total_csize")
    public String totalCsize;

    @JsonProperty("total_size")
    public String totalSize;

    @JsonProperty("total_unique_chunks")
    public String totalUniqueChunks;

    @JsonProperty("unique_csize")
    public String uniqueCsize;

    @JsonProperty("unique_size")
    public String uniqueSize;
}
