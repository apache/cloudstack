package com.cloud.async.executor;

import com.cloud.serializer.Param;

public class IngressRuleResultObject {
    @Param(name="id")
    private Long id;

    @Param(name="startport")
    private int startPort;

    @Param(name="endport")
    private int endPort;

    @Param(name="protocol")
    private String protocol;

    @Param(name="networkgroup")
    private String allowedNetworkGroup = null;

    @Param(name="account")
    private String allowedNetGroupAcct = null;

    @Param(name="cidr")
    private String allowedSourceIpCidr = null;

    public IngressRuleResultObject() { }

    public IngressRuleResultObject(Long id, int startPort, int endPort, String protocol, String allowedNetworkGroup, String allowedNetGroupAcct, String allowedSourceIpCidr) {
        this.id = id;
        this.startPort = startPort;
        this.endPort = endPort;
        this.protocol = protocol;
        this.allowedNetworkGroup = allowedNetworkGroup;
        this.allowedNetGroupAcct = allowedNetGroupAcct;
        this.allowedSourceIpCidr = allowedSourceIpCidr;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getStartPort() {
        return startPort;
    }

    public void setStartPort(int startPort) {
        this.startPort = startPort;
    }

    public int getEndPort() {
        return endPort;
    }

    public void setEndPort(int endPort) {
        this.endPort = endPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAllowedNetworkGroup() {
        return allowedNetworkGroup;
    }

    public void setAllowedNetworkGroup(String allowedNetworkGroup) {
        this.allowedNetworkGroup = allowedNetworkGroup;
    }

    public String getAllowedNetGroupAcct() {
        return allowedNetGroupAcct;
    }

    public void setAllowedNetGroupAcct(String allowedNetGroupAcct) {
        this.allowedNetGroupAcct = allowedNetGroupAcct;
    }

    public String getAllowedSourceIpCidr() {
        return allowedSourceIpCidr;
    }

    public void setAllowedSourceIpCidr(String allowedSourceIpCidr) {
        this.allowedSourceIpCidr = allowedSourceIpCidr;
    }
}
