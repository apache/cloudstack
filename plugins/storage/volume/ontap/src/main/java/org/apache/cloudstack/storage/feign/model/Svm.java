package org.apache.cloudstack.storage.feign.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Svm {
    @JsonProperty("uuid")
    @SerializedName("uuid")
    private String uuid = null;

    @JsonProperty("name")
    @SerializedName("name")
    private String name = null;

    @JsonProperty("iscsi.enabled")
    @SerializedName("iscsi.enabled")
    private Boolean iscsiEnabled = null;

    @JsonProperty("fcp.enabled")
    @SerializedName("fcp.enabled")
    private Boolean fcpEnabled = null;

    @JsonProperty("nfs.enabled")
    @SerializedName("nfs.enabled")
    private Boolean nfsEnabled = null;

    @JsonProperty("aggregates")
    @SerializedName("aggregates")
    private List<Aggregate> aggregates = null;

    @JsonProperty("aggregates_delegated")
    @SerializedName("aggregates_delegated")
    private Boolean aggregatesDelegated = null;

    @JsonProperty("state.value")
    @SerializedName("state.value")
    private String state = null;
}
