package com.cloud.agent.resource.virtualnetwork.model;

public abstract class ConfigBase {
    private String type = "unknown";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

}
