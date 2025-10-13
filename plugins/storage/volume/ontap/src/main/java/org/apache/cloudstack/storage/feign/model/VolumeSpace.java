
package org.apache.cloudstack.storage.feign.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class VolumeSpace {
    @JsonProperty("size")
    private long size;

    @JsonProperty("available")
    private long available;

    @JsonProperty("used")
    private long used;

    // Getters and setters
}
