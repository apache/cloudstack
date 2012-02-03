package com.cloud.network.firewall;

import java.util.List;

import com.cloud.api.commands.ListFirewallRulesCmd;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.rules.FirewallRule;
import com.cloud.user.Account;

public interface FirewallService {
    FirewallRule createFirewallRule(FirewallRule rule) throws NetworkRuleConflictException;

    List<? extends FirewallRule> listFirewallRules(ListFirewallRulesCmd cmd);

    /**
     * Revokes a firewall rule
     * 
     * @param ruleId
     *            the id of the rule to revoke.
     * @return
     */
    boolean revokeFirewallRule(long ruleId, boolean apply);

    boolean applyFirewallRules(long ipId, Account caller) throws ResourceUnavailableException;

    FirewallRule getFirewallRule(long ruleId);

    boolean revokeRelatedFirewallRule(long ruleId, boolean apply);

}
