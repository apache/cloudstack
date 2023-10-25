package org.apache.cloudstack.agent.api;

public class CreateNsxStaticNatCommand extends NsxNetworkCommand {

    public CreateNsxStaticNatCommand(long domainId, long accountId, long zoneId, Long networkResourceId, String networkResourceName,
                                     boolean isResourceVpc, Long vmId, String publicIp, String vmIp) {
        super(domainId, accountId, zoneId, networkResourceId, networkResourceName, isResourceVpc, vmId, publicIp, vmIp);
    }
}
