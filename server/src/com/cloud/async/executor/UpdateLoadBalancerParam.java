package com.cloud.async.executor;

public class UpdateLoadBalancerParam {
    private long userId;
    private long loadBalancerId;
    private String name;
    private String description;
    private String privatePort;
    private String algorithm;

    public UpdateLoadBalancerParam() { }

    public UpdateLoadBalancerParam(long userId, long loadBalancerId, String name, String description, String privatePort, String algorithm) {
        this.userId = userId;
        this.loadBalancerId = loadBalancerId;
        this.name = name;
        this.description = description;
        this.privatePort = privatePort;
        this.algorithm = algorithm;
    }

    public long getUserId() {
        return userId;
    }

    public long getLoadBalancerId() {
        return loadBalancerId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPrivatePort() {
        return privatePort;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}
