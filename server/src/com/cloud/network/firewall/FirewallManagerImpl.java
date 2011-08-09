package com.cloud.network.firewall;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.api.commands.ListFirewallRulesCmd;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventVO;
import com.cloud.event.dao.EventDao;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IPAddressVO;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRule.State;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.UserContext;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;

@Local(value = { FirewallService.class, FirewallManager.class })
public class FirewallManagerImpl implements FirewallService, FirewallManager, Manager{
    private static final Logger s_logger = Logger.getLogger(FirewallManagerImpl.class);
    String _name;

    @Inject
    FirewallRulesDao _firewallDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    EventDao _eventDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    FirewallRulesCidrsDao _firewallCidrsDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    NetworkManager _networkMgr;
    @Inject 
    UsageEventDao _usageEventDao;
    
    
    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        return true;
    }
    
    @Override
    public FirewallRule createFirewallRule(FirewallRule rule) throws NetworkRuleConflictException {
        Account caller = UserContext.current().getCaller();

        return createFirewallRule(rule.getSourceIpAddressId(), caller, rule.getXid(), rule.getSourcePortStart() ,rule.getSourcePortEnd(), rule.getProtocol(), rule.getIcmpCode(), rule.getIcmpType());    
    }
    
    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_OPEN, eventDescription = "creating firewll rule", create = true)
    public FirewallRule createFirewallRule(long ipAddrId, Account caller, String xId, Integer portStart,Integer portEnd, String protocol, Integer icmpCode, Integer icmpType) throws NetworkRuleConflictException{
        IPAddressVO ipAddress = _ipAddressDao.findById(ipAddrId);
        
        // Validate ip address
        if (ipAddress == null) {
            throw new InvalidParameterValueException("Unable to create port forwarding rule; ip id=" + ipAddrId + " doesn't exist in the system");
        } else if (ipAddress.isOneToOneNat()) {
            throw new InvalidParameterValueException("Unable to create port forwarding rule; ip id=" + ipAddrId + " has static nat enabled");
        } 
        
        validateFirewallRule(caller, ipAddress, portStart, portEnd, protocol);

        if (!protocol.equalsIgnoreCase(NetUtils.ICMP_PROTO) && (icmpCode != null || icmpType != null)) {
            throw new InvalidParameterValueException("Can specify icmpCode and icmpType for ICMP protocol only");
        }
        
        Long networkId = ipAddress.getAssociatedWithNetworkId();
        Long accountId = ipAddress.getAccountId();
        Long domainId = ipAddress.getDomainId();
        
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        FirewallRuleVO newRule = new FirewallRuleVO (xId, ipAddrId, portStart, portEnd, protocol.toLowerCase(), networkId, accountId, domainId, Purpose.Firewall, icmpCode, icmpType);
        newRule = _firewallDao.persist(newRule);

        detectRulesConflict(newRule, ipAddress);
        if (!_firewallDao.setStateToAdd(newRule)) {
            throw new CloudRuntimeException("Unable to update the state to add for " + newRule);
        }
        UserContext.current().setEventDetails("Rule Id: " + newRule.getId());
        
        txn.commit();

        return newRule;
    }
    
    @Override
    public List<? extends FirewallRule> listFirewallRules(ListFirewallRulesCmd cmd) {
        Account caller = UserContext.current().getCaller();
        Long ipId = cmd.getIpAddressId();
        Long id = cmd.getId();
        String path = null;

        Pair<String, Long> accountDomainPair = _accountMgr.finalizeAccountDomainForList(caller, cmd.getAccountName(), cmd.getDomainId());
        String accountName = accountDomainPair.first();
        Long domainId = accountDomainPair.second();

        if (ipId != null) {
            IPAddressVO ipAddressVO = _ipAddressDao.findById(ipId);
            if (ipAddressVO == null || !ipAddressVO.readyToUse()) {
                throw new InvalidParameterValueException("Ip address id=" + ipId + " not ready for firewall rules yet");
            }
            _accountMgr.checkAccess(caller, ipAddressVO);
        }

        if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
            Domain domain = _accountMgr.getDomain(caller.getDomainId());
            path = domain.getPath();
        }

        Filter filter = new Filter(FirewallRuleVO.class, "id", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<FirewallRuleVO> sb = _firewallDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), Op.EQ);
        sb.and("ip", sb.entity().getSourceIpAddressId(), Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), Op.EQ);
        sb.and("domainId", sb.entity().getDomainId(), Op.EQ);
        sb.and("purpose", sb.entity().getPurpose(), Op.EQ);

        if (path != null) {
            // for domain admin we should show only subdomains information
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<FirewallRuleVO> sc = sb.create();

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (ipId != null) {
            sc.setParameters("ip", ipId);
        }

        if (domainId != null) {
            sc.setParameters("domainId", domainId);
            if (accountName != null) {
                Account account = _accountMgr.getActiveAccount(accountName, domainId);
                sc.setParameters("accountId", account.getId());
            }
        }

        sc.setParameters("purpose", Purpose.Firewall);

        if (path != null) {
            sc.setJoinParameters("domainSearch", "path", path + "%");
        }

        return _firewallDao.search(sc, filter);
    }
    
    @Override
    public void detectRulesConflict(FirewallRule newRule, IpAddress ipAddress) throws NetworkRuleConflictException {
        assert newRule.getSourceIpAddressId() == ipAddress.getId() : "You passed in an ip address that doesn't match the address in the new rule";

        List<FirewallRuleVO> rules = _firewallDao.listByIpAndPurposeAndNotRevoked(newRule.getSourceIpAddressId(), null);
        assert (rules.size() >= 1) : "For network rules, we now always first persist the rule and then check for network conflicts so we should at least have one rule at this point.";

        
        
        for (FirewallRuleVO rule : rules) {
            if (rule.getId() == newRule.getId()) {
                continue; // Skips my own rule.
            }

            if (rule.getPurpose() == Purpose.StaticNat && newRule.getPurpose() != Purpose.StaticNat) {
                throw new NetworkRuleConflictException("There is 1 to 1 Nat rule specified for the ip address id=" + newRule.getSourceIpAddressId());
            } else if (rule.getPurpose() != Purpose.StaticNat && newRule.getPurpose() == Purpose.StaticNat) {
                throw new NetworkRuleConflictException("There is already firewall rule specified for the ip address id=" + newRule.getSourceIpAddressId());
            }

            if (rule.getNetworkId() != newRule.getNetworkId() && rule.getState() != State.Revoke) {
                throw new NetworkRuleConflictException("New rule is for a different network than what's specified in rule " + rule.getXid());
            }
            
            boolean allowFirewall = ((rule.getPurpose() == Purpose.Firewall || newRule.getPurpose() == Purpose.Firewall) && newRule.getPurpose() != rule.getPurpose());

            boolean notNullPorts = (newRule.getSourcePortStart() != null && newRule.getSourcePortEnd() != null && rule.getSourcePortStart() != null && rule.getSourcePortEnd() != null);
            if (!allowFirewall && notNullPorts && ((rule.getSourcePortStart() <= newRule.getSourcePortStart() && rule.getSourcePortEnd() >= newRule.getSourcePortStart())
                    || (rule.getSourcePortStart() <= newRule.getSourcePortEnd() && rule.getSourcePortEnd() >= newRule.getSourcePortEnd())
                    || (newRule.getSourcePortStart() <= rule.getSourcePortStart() && newRule.getSourcePortEnd() >= rule.getSourcePortStart())
                    || (newRule.getSourcePortStart() <= rule.getSourcePortEnd() && newRule.getSourcePortEnd() >= rule.getSourcePortEnd()))) {

                // we allow port forwarding rules with the same parameters but different protocols
                boolean allowPf = (rule.getPurpose() == Purpose.PortForwarding && newRule.getPurpose() == Purpose.PortForwarding && !newRule.getProtocol().equalsIgnoreCase(rule.getProtocol()));
                boolean allowStaticNat = (rule.getPurpose() == Purpose.StaticNat && newRule.getPurpose() == Purpose.StaticNat && !newRule.getProtocol().equalsIgnoreCase(rule.getProtocol()));
                
                if (!(allowPf || allowStaticNat || allowFirewall)) {
                    throw new NetworkRuleConflictException("The range specified, " + newRule.getSourcePortStart() + "-" + newRule.getSourcePortEnd() + ", conflicts with rule " + rule.getId()
                            + " which has " + rule.getSourcePortStart() + "-" + rule.getSourcePortEnd());
                }
            }
            
            if (newRule.getProtocol().equalsIgnoreCase(NetUtils.ICMP_PROTO) && newRule.getProtocol().equalsIgnoreCase(rule.getProtocol())) {
                if (newRule.getIcmpCode().longValue() == rule.getIcmpCode().longValue() || newRule.getIcmpType().longValue() == rule.getIcmpType().longValue() || newRule.getProtocol().equalsIgnoreCase(rule.getProtocol())) {
                    throw new InvalidParameterValueException("New rule conflicts with existing rule id=" + rule.getId());
                }
                
            }
        }
        
        

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("No network rule conflicts detected for " + newRule + " against " + (rules.size() - 1) + " existing rules");
        }
    }

    
    @Override
    public void validateFirewallRule(Account caller, IPAddressVO ipAddress, Integer portStart, Integer portEnd, String proto) {
        // Validate ip address
        _accountMgr.checkAccess(caller, ipAddress);

        Long networkId = ipAddress.getAssociatedWithNetworkId();
        if (networkId == null) {
            throw new InvalidParameterValueException("Unable to create port forwarding rule ; ip id=" + ipAddress.getId() + " is not associated with any network");

        }
        
        Network network = _networkMgr.getNetwork(networkId);
        assert network != null : "Can't create port forwarding rule as network associated with public ip address is null...how is it possible?";
        
        if (portStart != null && !NetUtils.isValidPort(portStart)) {
            throw new InvalidParameterValueException("publicPort is an invalid value: " + portStart);
        }
        if (portEnd != null && !NetUtils.isValidPort(portEnd)) {
            throw new InvalidParameterValueException("Public port range is an invalid value: " + portEnd);
        }
        
        // start port can't be bigger than end port
        if (portStart != null && portEnd != null && portStart > portEnd) {
            throw new InvalidParameterValueException("Start port can't be bigger than end port");
        }
        
        // Verify that the network guru supports the protocol specified
        Map<Network.Capability, String> firewallCapabilities = _networkMgr.getServiceCapabilities(network.getDataCenterId(), network.getNetworkOfferingId(), Service.Firewall);
        String supportedProtocols = firewallCapabilities.get(Capability.SupportedProtocols).toLowerCase();
        if (!supportedProtocols.contains(proto.toLowerCase())) {
            throw new InvalidParameterValueException("Protocol " + proto + " is not supported in zone " + network.getDataCenterId());
        }
    }
    
    @Override
    public boolean applyRules(List<? extends FirewallRule> rules, boolean continueOnError) throws ResourceUnavailableException {
        if (!_networkMgr.applyRules(rules, continueOnError)) {
            s_logger.warn("Rules are not completely applied");
            return false;
        } else {
            for (FirewallRule rule : rules) {
                if (rule.getState() == FirewallRule.State.Revoke) {
                    _firewallDao.remove(rule.getId());
                } else if (rule.getState() == FirewallRule.State.Add) {
                    FirewallRuleVO ruleVO = _firewallDao.findById(rule.getId());
                    ruleVO.setState(FirewallRule.State.Active);
                    _firewallDao.update(ruleVO.getId(), ruleVO);
                }
            }
            return true;
        }
    }
    
    @Override
    public boolean applyFirewallRules(long ipId, Account caller) throws ResourceUnavailableException {
        List<FirewallRuleVO> rules = _firewallDao.listByIpAndPurpose(ipId, Purpose.Firewall);
        return applyFirewallRules(rules, false, caller);
    }
    
    
    @Override
    public boolean applyFirewallRules(List<FirewallRuleVO> rules, boolean continueOnError, Account caller) {
        
        if (rules.size() == 0) {
            s_logger.debug("There are no firewall rules to apply for ip id=" + rules);
            return true;
        }
        
        if (caller != null) {
            _accountMgr.checkAccess(caller, rules.toArray(new FirewallRuleVO[rules.size()]));
        }

        try {
            if (!applyRules(rules, continueOnError)) {
                return false;
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Failed to apply firewall rules due to ", ex);
            return false;
        }

        return true;
    }
    
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_FIREWALL_CLOSE, eventDescription = "revoking firewall rule", async = true)
    public boolean revokeFirewallRule(long ruleId, boolean apply, Account caller, long userId) {

        FirewallRuleVO rule = _firewallDao.findById(ruleId);
        if (rule == null || rule.getPurpose() != Purpose.Firewall) {
            throw new InvalidParameterValueException("Unable to find " + ruleId + " having purpose " + Purpose.Firewall);
        }

        _accountMgr.checkAccess(caller, rule);
        
        
        revokeRule(rule, caller, userId, false);

        boolean success = false;

        if (apply) {
            List<FirewallRuleVO> rules = _firewallDao.listByIpAndPurpose(rule.getSourceIpAddressId(), Purpose.Firewall);
            return applyFirewallRules(rules, false, caller);
        } else {
            success = true;
        }

        return success;

    }
    
    @Override
    public boolean revokeFirewallRule(long ruleId, boolean apply) {
        Account caller = UserContext.current().getCaller();
        long userId = UserContext.current().getCallerUserId();
        return revokeFirewallRule(ruleId, apply, caller, userId);
    }
    
    @Override 
    @DB
    public void revokeRule(FirewallRuleVO rule, Account caller, long userId, boolean needUsageEvent) {
        if (caller != null) {
            _accountMgr.checkAccess(caller, rule);
        }

        Transaction txn = Transaction.currentTxn();
        boolean generateUsageEvent = false;

        txn.start();
        if (rule.getState() == State.Staged) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Found a rule that is still in stage state so just removing it: " + rule);
            }
            _firewallDao.remove(rule.getId());
            generateUsageEvent = true;
        } else if (rule.getState() == State.Add || rule.getState() == State.Active) {
            rule.setState(State.Revoke);
            _firewallDao.update(rule.getId(), rule);
            generateUsageEvent = true;
        }

        if (generateUsageEvent && needUsageEvent) {
            UsageEventVO usageEvent = new UsageEventVO(EventTypes.EVENT_NET_RULE_DELETE, rule.getAccountId(), 0, rule.getId(), null);
            _usageEventDao.persist(usageEvent);
        }
        
        txn.commit();
    }

    
    @Override
    public FirewallRule getFirewallRule(long ruleId) {
        return _firewallDao.findById(ruleId);
    }
    
    @Override
    public boolean revokeFirewallRulesForIp(long ipId, long userId, Account caller) throws ResourceUnavailableException {
        List<FirewallRule> rules = new ArrayList<FirewallRule>();

        List<FirewallRuleVO> fwRules = _firewallDao.listByIpAndPurposeAndNotRevoked(ipId, Purpose.Firewall);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + fwRules.size() + " firewall rules for ip id=" + ipId);
        }

        for (FirewallRuleVO rule : fwRules) {
            // Mark all Firewall rules as Revoke, but don't revoke them yet - we have to revoke all rules for ip, no need to send them one by one
            revokeFirewallRule(rule.getId(), false, caller, Account.ACCOUNT_ID_SYSTEM);
        }

        // now send everything to the backend
        List<FirewallRuleVO> rulesToApply = _firewallDao.listByIpAndPurpose(ipId, Purpose.Firewall);
        applyFirewallRules(rulesToApply, true, caller);

        // Now we check again in case more rules have been inserted.
        rules.addAll(_firewallDao.listByIpAndPurposeAndNotRevoked(ipId, Purpose.Firewall));

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Successfully released firewall rules for ip id=" + ipId + " and # of rules now = " + rules.size());
        }

        return rules.size() == 0;
    }
    
    @Override
    public FirewallRule createRuleForAllCidrs(long ipAddrId, Account caller, Integer startPort, Integer endPort, String protocol, Integer icmpCode, Integer icmpType) throws NetworkRuleConflictException{
        
        //If firwallRule for this port range already exists, return it
        List<FirewallRuleVO> rules = _firewallDao.listByIpPurposeAndProtocolAndNotRevoked(ipAddrId, startPort, endPort, protocol, Purpose.Firewall);
        if (!rules.isEmpty()) {
            return rules.get(0);
        }
        return createFirewallRule(ipAddrId, caller, null, startPort, endPort, protocol, icmpCode, icmpType);
    }
    
    @Override
    public boolean revokeAllFirewallRulesForNetwork(long networkId, long userId, Account caller) throws ResourceUnavailableException {
        List<FirewallRule> rules = new ArrayList<FirewallRule>();

        List<FirewallRuleVO> fwRules = _firewallDao.listByNetworkAndPurposeAndNotRevoked(networkId, Purpose.Firewall);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Releasing " + fwRules.size() + " firewall rules for network id=" + networkId);
        }

        for (FirewallRuleVO rule : fwRules) {
            // Mark all Firewall rules as Revoke, but don't revoke them yet - we have to revoke all rules for ip, no need to send them one by one
            revokeFirewallRule(rule.getId(), false, caller, Account.ACCOUNT_ID_SYSTEM);
        }

        // now send everything to the backend
        List<FirewallRuleVO> rulesToApply = _firewallDao.listByNetworkAndPurpose(networkId, Purpose.Firewall);
        applyFirewallRules(rulesToApply, true, caller);

        // Now we check again in case more rules have been inserted.
        rules.addAll(_firewallDao.listByNetworkAndPurposeAndNotRevoked(networkId, Purpose.Firewall));

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Successfully released firewall rules for network id=" + networkId + " and # of rules now = " + rules.size());
        }

        return rules.size() == 0;
    }
}
