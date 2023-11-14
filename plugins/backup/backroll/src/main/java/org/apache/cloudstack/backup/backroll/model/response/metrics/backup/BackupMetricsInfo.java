package org.apache.cloudstack.backup.backroll.model.response.metrics.backup;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackupMetricsInfo {
    @JsonProperty("compressed_size")
    public String compressedSize;

    @JsonProperty("deduplicated_size")
    public String deduplicatedSize;

    @JsonProperty("nfiles")
    public String nFiles;

    @JsonProperty("original_size")
    public String originalSize;
}
