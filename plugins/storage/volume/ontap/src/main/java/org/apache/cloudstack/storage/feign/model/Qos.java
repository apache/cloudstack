// Qos.java
package org.apache.cloudstack.storage.feign.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Qos {
    @JsonProperty("policy")
    private Policy policy;

    // Getters and setters
}
