package org.apache.cloudstack.agent.api;

import org.apache.cloudstack.resource.NsxNetworkRule;

import java.util.List;

public class DeletedNsxDistributedFirewallRulesCommand extends CreateNsxDistributedFirewallRulesCommand {
    public DeletedNsxDistributedFirewallRulesCommand(long domainId, long accountId, long zoneId, Long vpcId, long networkId, List<NsxNetworkRule> rules) {
        super(domainId, accountId, zoneId, vpcId, networkId, rules);
    }
}
