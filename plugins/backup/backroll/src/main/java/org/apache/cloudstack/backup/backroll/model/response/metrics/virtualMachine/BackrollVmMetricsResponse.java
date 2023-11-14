package org.apache.cloudstack.backup.backroll.model.response.metrics.virtualMachine;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BackrollVmMetricsResponse {
    @JsonProperty("state")
    public String state;

    @JsonProperty("info")
    public MetricsInfos infos;
}
