package org.apache.cloudstack.agent.api;

import java.util.List;

public class CreateNsxDistributedFirewallRuleCommand extends NsxNetworkCommand {
    private List<String> sourceCidrList;
    private String protocol;
    private String trafficType;
    private String action;
    public CreateNsxDistributedFirewallRuleCommand(long domainId, long accountId, long zoneId, Long networkResourceId,
                                                   String networkResourceName, boolean isResourceVpc,
                                                   List<String> sourceCidrList, String protocol,
                                                   String trafficType, String action) {
        super(domainId, accountId, zoneId, networkResourceId, networkResourceName, isResourceVpc);
        this.sourceCidrList = sourceCidrList;
        this.protocol = protocol;
        this.action = action;
        this.trafficType = trafficType;
    }

    public List<String> getSourceCidrList() {
        return sourceCidrList;
    }

    public void setSourceCidrList(List<String> sourceCidrList) {
        this.sourceCidrList = sourceCidrList;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getTrafficType() {
        return trafficType;
    }

    public void setTrafficType(String trafficType) {
        this.trafficType = trafficType;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
