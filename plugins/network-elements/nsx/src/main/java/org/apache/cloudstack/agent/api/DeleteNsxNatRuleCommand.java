package org.apache.cloudstack.agent.api;

import com.cloud.network.Network;

public class DeleteNsxNatRuleCommand extends NsxNetworkCommand {
    private Long ruleId;
    private Network.Service service;
    public DeleteNsxNatRuleCommand(long domainId, long accountId, long zoneId, Long networkResourceId, String networkResourceName,
                                   boolean isResourceVpc, Long vmId, Long ruleId, String publicIp, String vmIp) {
        super(domainId, accountId, zoneId, networkResourceId, networkResourceName, isResourceVpc, vmId, publicIp, vmIp);
        this.ruleId = ruleId;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public Network.Service getService() {
        return service;
    }

    public void setService(Network.Service service) {
        this.service = service;
    }
}
