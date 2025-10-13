package org.apache.cloudstack.storage.feign.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VolumeSpaceLogicalSpace {

    @JsonProperty("available")
    @SerializedName("available")
    private Long available = null;

    @JsonProperty("used")
    @SerializedName("used")
    private Double used = null;
}
