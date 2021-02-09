package com.cloud.agent.api;

import java.util.HashMap;
import java.util.Map;

public class GetStoragePoolCapabilitiesAnswer extends Answer {

    private Map<String, String> poolDetails;

    public GetStoragePoolCapabilitiesAnswer(GetStoragePoolCapabilitiesCommand cmd) {
        super(cmd);
        poolDetails = new HashMap<>();
    }

    public Map<String, String> getPoolDetails() {
        return poolDetails;
    }

    public void setPoolDetails(Map<String, String> poolDetails) {
        this.poolDetails = poolDetails;
    }

}
