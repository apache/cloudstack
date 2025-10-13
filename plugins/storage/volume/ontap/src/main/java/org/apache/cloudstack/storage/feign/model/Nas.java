// Nas.java
package org.apache.cloudstack.storage.feign.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Nas {
    @JsonProperty("path")
    private String path;

    @JsonProperty("export_policy")
    private ExportPolicy exportPolicy;

    // Getters and setters
}
