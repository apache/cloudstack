package com.cloud.api.response;

import com.cloud.api.ResponseObject;
import com.cloud.serializer.Param;

public class IngressRuleResponse implements ResponseObject {
    @Param(name="ruleid")
    private Long ruleId;

    @Param(name="protocol")
    private String protocol;

    @Param(name="icmptype")
    private Integer icmpType;

    @Param(name="icmpcode")
    private Integer icmpCode;

    @Param(name="startport")
    private Integer startPort;

    @Param(name="endport")
    private Integer endPort;

    @Param(name="networkgroupname")
    private String networkGroupName;

    @Param(name="account")
    private String accountName;

    @Param(name="cidr")
    private String cidr;

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Integer getIcmpType() {
        return icmpType;
    }

    public void setIcmpType(Integer icmpType) {
        this.icmpType = icmpType;
    }

    public Integer getIcmpCode() {
        return icmpCode;
    }

    public void setIcmpCode(Integer icmpCode) {
        this.icmpCode = icmpCode;
    }

    public Integer getStartPort() {
        return startPort;
    }

    public void setStartPort(Integer startPort) {
        this.startPort = startPort;
    }

    public Integer getEndPort() {
        return endPort;
    }

    public void setEndPort(Integer endPort) {
        this.endPort = endPort;
    }

    public String getNetworkGroupName() {
        return networkGroupName;
    }

    public void setNetworkGroupName(String networkGroupName) {
        this.networkGroupName = networkGroupName;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }
}
