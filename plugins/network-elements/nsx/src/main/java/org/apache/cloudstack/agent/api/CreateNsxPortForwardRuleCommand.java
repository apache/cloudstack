package org.apache.cloudstack.agent.api;

public class CreateNsxPortForwardRuleCommand extends NsxNetworkCommand {
    private final String publicPort;
    private final String privatePort;
    private final String protocol;
    private final long ruleId;


    public CreateNsxPortForwardRuleCommand(long domainId, long accountId, long zoneId, Long networkResourceId,
                                           String networkResourceName, boolean isResourceVpc, Long vmId,
                                           long ruleId, String publicIp, String vmIp, String publicPort, String privatePort, String protocol) {
        super(domainId, accountId, zoneId, networkResourceId, networkResourceName, isResourceVpc, vmId, publicIp, vmIp);
        this.publicPort = publicPort;
        this.privatePort = privatePort;
        this.ruleId = ruleId;
        this.protocol = protocol;

    }

    public String getPublicPort() {
        return publicPort;
    }

    public String getPrivatePort() {
        return privatePort;
    }

    public long getRuleId() {
        return ruleId;
    }

    public String getProtocol() {
        return protocol;
    }
}
