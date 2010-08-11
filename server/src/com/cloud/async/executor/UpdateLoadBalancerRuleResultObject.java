package com.cloud.async.executor;

import com.cloud.serializer.Param;

public class UpdateLoadBalancerRuleResultObject {
    @Param(name="id")
    private long id;

    @Param(name="name")
    private String name;

    @Param(name="description")
    private String description;

    @Param(name="publicip")
    private String publicIp;

    @Param(name="publicport")
    private String publicPort;

    @Param(name="privateport")
    private String privatePort;

    @Param(name="algorithm")
    private String algorithm;

    @Param(name="account")
    private String accountName;

    @Param(name="domainid")
    private long domainId;

    @Param(name="domain")
    private String domainName;

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String getPublicIp() {
        return publicIp;
    }
    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }

    public String getPublicPort() {
        return publicPort;
    }
    public void setPublicPort(String publicPort) {
        this.publicPort = publicPort;
    }

    public String getPrivatePort() {
        return privatePort;
    }
    public void setPrivatePort(String privatePort) {
        this.privatePort = privatePort;
    }

    public String getAlgorithm() {
        return algorithm;
    }
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getAccountName() {
        return accountName;
    }
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public long getDomainId() {
        return domainId;
    }
    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }

    public String getDomainName() {
        return domainName;
    }
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }
}
