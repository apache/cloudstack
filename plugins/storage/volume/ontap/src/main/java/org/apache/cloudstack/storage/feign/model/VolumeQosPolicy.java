package org.apache.cloudstack.storage.feign.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VolumeQosPolicy {
    @JsonProperty("max_throughput_iops")
    @SerializedName("max_throughput_iops")
    private Integer maxThroughputIops = null;

    @JsonProperty("max_throughput_mbps")
    @SerializedName("max_throughput_mbps")
    private Integer maxThroughputMbps = null;

    @JsonProperty("min_throughput_iops")
    @SerializedName("min_throughput_iops")
    private Integer minThroughputIops = null;

    @JsonProperty("name")
    @SerializedName("name")
    private String name = null;

    @JsonProperty("uuid")
    @SerializedName("uuid")
    private String uuid = null;
}
