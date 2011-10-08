/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.network.vpn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.api.commands.ListRemoteAccessVpnsCmd;
import com.cloud.api.commands.ListVpnUsersCmd;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.domain.Domain;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.AccountLimitException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.RemoteAccessVpnVO;
import com.cloud.network.VpnUser;
import com.cloud.network.VpnUser.State;
import com.cloud.network.VpnUserVO;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.RemoteAccessVpnDao;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.element.RemoteAccessVPNServiceProvider;
import com.cloud.network.router.VirtualNetworkApplianceManager;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.RulesManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.net.NetUtils;

@Local(value = RemoteAccessVpnService.class)
public class RemoteAccessVpnManagerImpl implements RemoteAccessVpnService, Manager {
    private final static Logger s_logger = Logger.getLogger(RemoteAccessVpnManagerImpl.class);
    String _name;
    
    @Inject AccountDao _accountDao;
    @Inject VpnUserDao _vpnUsersDao;
    @Inject RemoteAccessVpnDao _remoteAccessVpnDao;
    @Inject IPAddressDao _ipAddressDao;
    @Inject VirtualNetworkApplianceManager _routerMgr;
    @Inject AccountManager _accountMgr;
    @Inject DomainManager _domainMgr;
    @Inject NetworkManager _networkMgr;
    @Inject RulesManager _rulesMgr;
    @Inject DomainDao _domainDao;
    @Inject FirewallRulesDao _rulesDao;
    @Inject FirewallManager _firewallMgr;
    
    int _userLimit;
    int _pskLength;
    String _clientIpRange;
    SearchBuilder<RemoteAccessVpnVO> VpnSearch;

    @Override
    public RemoteAccessVpn createRemoteAccessVpn(long publicIpId, String ipRange, boolean openFirewall) throws NetworkRuleConflictException {
        UserContext ctx = UserContext.current();
        Account caller = ctx.getCaller();

        // make sure ip address exists
        PublicIpAddress ipAddr = _networkMgr.getPublicIpAddress(publicIpId);
        if (ipAddr == null) {
            throw new InvalidParameterValueException("Unable to create remote access vpn, invalid public IP address id" + publicIpId);
        }

        _accountMgr.checkAccess(caller, null, ipAddr);

        if (!ipAddr.readyToUse() || ipAddr.getAssociatedWithNetworkId() == null) {
            throw new InvalidParameterValueException("The Ip address is not ready to be used yet: " + ipAddr.getAddress());
        }

        RemoteAccessVpnVO vpnVO = _remoteAccessVpnDao.findByPublicIpAddress(publicIpId);
       
        if (vpnVO != null) {
            //if vpn is in Added state, return it to the api
            if (vpnVO.getState() == RemoteAccessVpn.State.Added) {
                return vpnVO;
            }
            throw new InvalidParameterValueException("A Remote Access VPN already exists for this public Ip address");
        }

        // TODO: assumes one virtual network / domr per account per zone
        vpnVO = _remoteAccessVpnDao.findByAccountAndNetwork(ipAddr.getAllocatedToAccountId(), ipAddr.getAssociatedWithNetworkId());
        if (vpnVO != null) {
            //if vpn is in Added state, return it to the api
            if (vpnVO.getState() == RemoteAccessVpn.State.Added) {
                return vpnVO;
            }
            throw new InvalidParameterValueException("A Remote Access VPN already exists for this account");
        }
        
        //Verify that vpn service is enabled for the network
        Network network = _networkMgr.getNetwork(ipAddr.getAssociatedWithNetworkId());
        if (!_networkMgr.isServiceSupported(network.getNetworkOfferingId(), Service.Vpn)) {
            throw new InvalidParameterValueException("Vpn service is not supported in network id=" + ipAddr.getAssociatedWithNetworkId());
        }

        if (ipRange == null) {
            ipRange = _clientIpRange;
        }
        String[] range = ipRange.split("-");
        if (range.length != 2) {
            throw new InvalidParameterValueException("Invalid ip range");
        }
        if (!NetUtils.isValidIp(range[0]) || !NetUtils.isValidIp(range[1])) {
            throw new InvalidParameterValueException("Invalid ip in range specification " + ipRange);
        }
        if (!NetUtils.validIpRange(range[0], range[1])) {
            throw new InvalidParameterValueException("Invalid ip range " + ipRange);
        }

        Pair<String, Integer> cidr = NetUtils.getCidr(network.getCidr());

        // FIXME: This check won't work for the case where the guest ip range
        // changes depending on the vlan allocated.
        String[] guestIpRange = NetUtils.getIpRangeFromCidr(cidr.first(), cidr.second());
        if (NetUtils.ipRangesOverlap(range[0], range[1], guestIpRange[0], guestIpRange[1])) {
            throw new InvalidParameterValueException("Invalid ip range: " + ipRange + " overlaps with guest ip range " + guestIpRange[0] + "-"
                    + guestIpRange[1]);
        }
        // TODO: check sufficient range
        // TODO: check overlap with private and public ip ranges in datacenter

        long startIp = NetUtils.ip2Long(range[0]);
        String newIpRange = NetUtils.long2Ip(++startIp) + "-" + range[1];
        String sharedSecret = PasswordGenerator.generatePresharedKey(_pskLength);
        _rulesMgr.reservePorts(ipAddr, NetUtils.UDP_PROTO, Purpose.Vpn, openFirewall, caller, NetUtils.VPN_PORT, NetUtils.VPN_L2TP_PORT, NetUtils.VPN_NATT_PORT);
        vpnVO = new RemoteAccessVpnVO(ipAddr.getAllocatedToAccountId(), ipAddr.getAllocatedInDomainId(), ipAddr.getAssociatedWithNetworkId(),
                publicIpId, range[0], newIpRange, sharedSecret);
        return _remoteAccessVpnDao.persist(vpnVO);
    }

    private void validateRemoteAccessVpnConfiguration() throws ConfigurationException {
        String ipRange = _clientIpRange;
        if (ipRange == null) {
            s_logger.warn("Remote Access VPN configuration missing client ip range -- ignoring");
            return;
        }
        Integer pskLength = _pskLength;
        if (pskLength != null && (pskLength < 8 || pskLength > 256)) {
            throw new ConfigurationException("Remote Access VPN: IPSec preshared key length should be between 8 and 256");
        } else if (pskLength == null) {
            s_logger.warn("Remote Access VPN configuration missing Preshared Key Length -- ignoring");
            return;
        }

        String[] range = ipRange.split("-");
        if (range.length != 2) {
            throw new ConfigurationException("Remote Access VPN: Invalid ip range " + ipRange);
        }
        if (!NetUtils.isValidIp(range[0]) || !NetUtils.isValidIp(range[1])) {
            throw new ConfigurationException("Remote Access VPN: Invalid ip in range specification " + ipRange);
        }
        if (!NetUtils.validIpRange(range[0], range[1])) {
            throw new ConfigurationException("Remote Access VPN: Invalid ip range " + ipRange);
        }
    }

    @Override @DB
    public void destroyRemoteAccessVpn(long ipId) throws ResourceUnavailableException {
        Account caller = UserContext.current().getCaller();
        
        RemoteAccessVpnVO vpn = _remoteAccessVpnDao.findById(ipId);
        if (vpn == null) {
            s_logger.debug("vpn id=" + ipId + " does not exists ");
            return;
        }
        
        _accountMgr.checkAccess(caller, null, vpn);
        
        Network network = _networkMgr.getNetwork(vpn.getNetworkId());
        
        vpn.setState(RemoteAccessVpn.State.Removed);
        _remoteAccessVpnDao.update(vpn.getServerAddressId(), vpn);
        
        
        List<? extends RemoteAccessVPNServiceProvider> elements = _networkMgr.getRemoteAccessVpnElements();
        boolean success = false;
        try {
            for (RemoteAccessVPNServiceProvider element : elements) {
                if (element.stopVpn(network, vpn)) {
                    success = true;
                    break;
                }
            }
        } finally {        
            if (success) {
                //Cleanup corresponding ports
                List<? extends FirewallRule> vpnFwRules = _rulesDao.listByIpAndPurpose(ipId, Purpose.Vpn);
                Transaction txn = Transaction.currentTxn();
                
                boolean applyFirewall = false;
                List<FirewallRuleVO> fwRules = new ArrayList<FirewallRuleVO>();
                //if related firewall rule is created for the first vpn port, it would be created for the 2 other ports as well, so need to cleanup the backend
                if (_rulesDao.findByRelatedId(vpnFwRules.get(0).getId()) != null) {
                    applyFirewall = true;
                }
                
                if (applyFirewall) {
                    txn.start();
                    
                    for (FirewallRule vpnFwRule : vpnFwRules) {
                        //don't apply on the backend yet; send all 3 rules in a banch 
                        _firewallMgr.revokeRelatedFirewallRule(vpnFwRule.getId(), false);
                        fwRules.add(_rulesDao.findByRelatedId(vpnFwRule.getId()));
                    }
                    
                    s_logger.debug("Marked " + fwRules.size() + " firewall rules as Revoked as a part of disable remote access vpn");
                    
                    txn.commit();
                    
                    //now apply vpn rules on the backend
                    s_logger.debug("Reapplying firewall rules for ip id=" + ipId + " as a part of disable remote access vpn");
                    success = _firewallMgr.applyFirewallRules(ipId, caller);
                }
                
                if (success) {
                    try {
                        txn.start();
                        _remoteAccessVpnDao.remove(ipId);
                        if (vpnFwRules != null) {
                            for (FirewallRule vpnFwRule : vpnFwRules) {
                                _rulesDao.remove(vpnFwRule.getId());
                                s_logger.debug("Successfully removed firewall rule with ip id=" + vpnFwRule.getSourceIpAddressId() + " and port " + vpnFwRule.getSourcePortStart() + " as a part of vpn cleanup");
                            }
                        }
                        txn.commit();   
                    } catch (Exception ex) {
                        txn.rollback();
                        s_logger.warn("Unable to release the three vpn ports from the firewall rules", ex);
                    }
                }
            }
        }
    }

    @Override
    @DB
    public VpnUser addVpnUser(long vpnOwnerId, String username, String password) {
        Account caller = UserContext.current().getCaller();

        if (!username.matches("^[a-zA-Z0-9][a-zA-Z0-9@._-]{2,63}$")) {
            throw new InvalidParameterValueException(
                    "Username has to be begin with an alphabet have 3-64 characters including alphabets, numbers and the set '@.-_'");
        }
        if (!password.matches("^[a-zA-Z0-9][a-zA-Z0-9@#+=._-]{2,31}$")) {
            throw new InvalidParameterValueException("Password has to be 3-32 characters including alphabets, numbers and the set '@#+=.-_'");
        }
        Transaction txn = Transaction.currentTxn();
        txn.start();
        Account owner = _accountDao.lockRow(vpnOwnerId, true);
        if (owner == null) {
            throw new InvalidParameterValueException("Unable to add vpn user: Another operation active");
        }
        _accountMgr.checkAccess(caller, null, owner);

        long userCount = _vpnUsersDao.getVpnUserCount(owner.getId());
        if (userCount >= _userLimit) {
            throw new AccountLimitException("Cannot add more than " + _userLimit + " remote access vpn users");
        }

        VpnUser user = _vpnUsersDao.persist(new VpnUserVO(vpnOwnerId, owner.getDomainId(), username, password));
        txn.commit();
        return user;
    }

    @Override
    public boolean removeVpnUser(long vpnOwnerId, String username) {
        Account caller = UserContext.current().getCaller();

        VpnUserVO user = _vpnUsersDao.findByAccountAndUsername(vpnOwnerId, username);
        if (user == null) {
            throw new InvalidParameterValueException("Could not find vpn user " + username);
        }
        _accountMgr.checkAccess(caller, null, user);

        user.setState(State.Revoke);
        _vpnUsersDao.update(user.getId(), user);
        return true;
    }

    @Override
    public List<? extends VpnUser> listVpnUsers(long vpnOwnerId, String userName) {
        Account caller = UserContext.current().getCaller();
        Account owner = _accountDao.findById(vpnOwnerId);
        _accountMgr.checkAccess(caller, null, owner);
        return _vpnUsersDao.listByAccount(vpnOwnerId);
    }

    @Override
    public RemoteAccessVpnVO startRemoteAccessVpn(long vpnId, boolean openFirewall) throws ResourceUnavailableException {
        Account caller = UserContext.current().getCaller();

        RemoteAccessVpnVO vpn = _remoteAccessVpnDao.findById(vpnId);
        if (vpn == null) {
            throw new InvalidParameterValueException("Unable to find your vpn: " + vpnId);
        }

        _accountMgr.checkAccess(caller, null, vpn);
        
      

        Network network = _networkMgr.getNetwork(vpn.getNetworkId());

        List<? extends RemoteAccessVPNServiceProvider > elements = _networkMgr.getRemoteAccessVpnElements();
        boolean started = false;
        try {
            boolean firewallOpened = true;
            if (openFirewall) {
                firewallOpened = _firewallMgr.applyFirewallRules(vpn.getServerAddressId(), caller);
            }
            
            if (firewallOpened) {
                for (RemoteAccessVPNServiceProvider element : elements) {
                    if (element.startVpn(network, vpn)) {
                        started = true;
                        break;
                    }
                }
            }
           
            return vpn;
        } finally {
            if (started) {
                vpn.setState(RemoteAccessVpn.State.Running);
                _remoteAccessVpnDao.update(vpn.getServerAddressId(), vpn);
            } 
        }
    }

    @DB
    @Override
    public boolean applyVpnUsers(long vpnOwnerId) {
        Account caller = UserContext.current().getCaller();
        Account owner = _accountDao.findById(vpnOwnerId);
        _accountMgr.checkAccess(caller, null, owner);

        s_logger.debug("Applying vpn users for " + owner);
        List<RemoteAccessVpnVO> vpns = _remoteAccessVpnDao.findByAccount(vpnOwnerId);

        List<VpnUserVO> users = _vpnUsersDao.listByAccount(vpnOwnerId);
        
        //If user is in Active state, we still have to resend them therefore their status has to be Add
        for (VpnUserVO user : users) {
            if (user.getState() == State.Active) {
                user.setState(State.Add);
                _vpnUsersDao.update(user.getId(), user);
            }
        }
        
        List<? extends RemoteAccessVPNServiceProvider> elements = _networkMgr.getRemoteAccessVpnElements();

        boolean success = true;

        boolean[] finals = new boolean[users.size()];
        for (RemoteAccessVPNServiceProvider element : elements) {
            s_logger.debug("Applying vpn access to " + element.getName());
            for (RemoteAccessVpnVO vpn : vpns) {
                try {
                    String[] results = element.applyVpnUsers(vpn, users);
                    if (results != null) {
                        for (int i = 0; i < results.length; i++) {
                            s_logger.debug("VPN User " + users.get(i)
                                    + (results[i] == null ? " is set on " : (" couldn't be set due to " + results[i]) + " on ") + vpn);
                            if (results[i] == null) {
                                if (!finals[i]) {
                                    finals[i] = true;
                                }
                            } else {
                                finals[i] = false;
                                success = false;
                            }
                        }
                    }
                } catch (ResourceUnavailableException e) {
                    s_logger.warn("Unable to apply vpn users ", e);
                    success= false;
                    
                    for (int i = 0; i < finals.length; i++) {
                        finals[i] = false;
                    }
                }
            }
        }

        for (int i = 0; i < finals.length; i++) {
            VpnUserVO user = users.get(i);
            if (finals[i]) {     
                if (user.getState() == State.Add) {
                    user.setState(State.Active);
                    _vpnUsersDao.update(user.getId(), user);
                } else if (user.getState() == State.Revoke) {
                    _vpnUsersDao.remove(user.getId());
                }
            } else {
                s_logger.warn("Failed to apply vpn for user " + user.getUsername() + ", accountId=" + user.getAccountId());
            }
        }

        return success;
    }

    @Override
    public List<VpnUserVO> searchForVpnUsers(ListVpnUsersCmd cmd) {
        Account caller = UserContext.current().getCaller();
        String username = cmd.getUsername();
        String path = null;
        
        //Verify account information
        Pair<List<Long>, Long> accountDomainPair = _accountMgr.finalizeAccountDomainForList(caller, cmd.getAccountName(), cmd.getDomainId(), null);
        List<Long> permittedAccounts = accountDomainPair.first();
        Long domainId = accountDomainPair.second();
        
        
        if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
            Domain domain = _domainMgr.getDomain(caller.getDomainId());
            path = domain.getPath();
        }

        Filter searchFilter = new Filter(VpnUserVO.class, "username", true, cmd.getStartIndex(), cmd.getPageSizeVal());

        Object id = cmd.getId();

        SearchBuilder<VpnUserVO> sb = _vpnUsersDao.createSearchBuilder();
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("username", sb.entity().getUsername(), SearchCriteria.Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.IN);
        sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        sb.and("state", sb.entity().getState(), SearchCriteria.Op.EQ);

        if (path != null) {
            //for domain admin we should show only subdomains information
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }

        SearchCriteria<VpnUserVO> sc = sb.create();
        
        //list only active users
        sc.setParameters("state", State.Active);

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (username != null) {
            sc.setParameters("username", username);
        }

        if (domainId != null) {
            sc.setParameters("domainId", domainId);
        }
        
        if (!permittedAccounts.isEmpty()) {
            sc.setParameters("accountId", permittedAccounts.toArray());
        }
        
        if (path != null) {
            sc.setJoinParameters("domainSearch", "path", path + "%");
        }

        return _vpnUsersDao.search(sc, searchFilter);
    }

    @Override
    public List<RemoteAccessVpnVO> searchForRemoteAccessVpns(ListRemoteAccessVpnsCmd cmd) {
        // do some parameter validation
        Account caller = UserContext.current().getCaller();
        String path = null;
        
        Pair<List<Long>, Long> accountDomainPair = _accountMgr.finalizeAccountDomainForList(caller, cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId());
        List<Long> permittedAccounts = accountDomainPair.first();
        Long domainId = accountDomainPair.second();
        
        if (caller.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN || caller.getType() == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN) {
            Domain domain = _domainMgr.getDomain(caller.getDomainId());
            path = domain.getPath();
        }

        Long ipAddressId = cmd.getPublicIpId();
        if (ipAddressId != null) {
            PublicIpAddress publicIp = _networkMgr.getPublicIpAddress(ipAddressId);
            if (publicIp == null) {
                throw new InvalidParameterValueException("Unable to list remote access vpns, IP address " + ipAddressId + " not found.");
            } else {
                Long ipAddrAcctId = publicIp.getAllocatedToAccountId();
                if (ipAddrAcctId == null) {
                    throw new InvalidParameterValueException("Unable to list remote access vpns, IP address " + ipAddressId
                            + " is not associated with an account.");
                }
            }
            _accountMgr.checkAccess(caller, null, publicIp);
        }

        
        Filter filter = new Filter(RemoteAccessVpnVO.class, "serverAddressId", false, cmd.getStartIndex(), cmd.getPageSizeVal()); 
        SearchBuilder<RemoteAccessVpnVO> sb = _remoteAccessVpnDao.createSearchBuilder();
        sb.and("serverAddressId", sb.entity().getServerAddressId(), Op.EQ);
        sb.and("accountId", sb.entity().getAccountId(), Op.IN);
        sb.and("domainId", sb.entity().getDomainId(), Op.EQ);
        sb.and("state", sb.entity().getState(), Op.EQ);
        
        if (path != null) {
            //for domain admin we should show only subdomains information
            SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
            domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
            sb.join("domainSearch", domainSearch, sb.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        }
        
        SearchCriteria<RemoteAccessVpnVO> sc = sb.create();

        sc.setParameters("state", RemoteAccessVpn.State.Running);
        
        if (ipAddressId != null) {
            sc.setParameters("serverAddressId", ipAddressId);
        }
        
        if (domainId != null) {
            sc.setParameters("domainId", domainId);
        }
        
        if (!permittedAccounts.isEmpty()) {
            sc.setParameters("accountId", permittedAccounts.toArray());
        }
        
        if (path != null) {
            sc.setJoinParameters("domainSearch", "path", path + "%");
        }

        return _remoteAccessVpnDao.search(sc, filter);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;

        ComponentLocator locator = ComponentLocator.getCurrentLocator();
        ConfigurationDao configDao = locator.getDao(ConfigurationDao.class);
        Map<String, String> configs = configDao.getConfiguration(params);

        _userLimit = NumbersUtil.parseInt(configs.get(Config.RemoteAccessVpnUserLimit.key()), 8);

        _clientIpRange = configs.get(Config.RemoteAccessVpnClientIpRange.key());

        _pskLength = NumbersUtil.parseInt(configs.get(Config.RemoteAccessVpnPskLength.key()), 24);

        validateRemoteAccessVpnConfiguration();

        VpnSearch = _remoteAccessVpnDao.createSearchBuilder();
        VpnSearch.and("accountId", VpnSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        SearchBuilder<DomainVO> domainSearch = _domainDao.createSearchBuilder();
        domainSearch.and("path", domainSearch.entity().getPath(), SearchCriteria.Op.LIKE);
        VpnSearch.join("domainSearch", domainSearch, VpnSearch.entity().getDomainId(), domainSearch.entity().getId(), JoinBuilder.JoinType.INNER);
        VpnSearch.done();

        return true;
    }

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
    public List<? extends RemoteAccessVpn> listRemoteAccessVpns(long networkId) {
        return _remoteAccessVpnDao.listByNetworkId(networkId);
    }
    
    @Override
    public RemoteAccessVpn getRemoteAccessVpn(long vpnId) {
        return _remoteAccessVpnDao.findById(vpnId);
    }

}
