// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.network.vpn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.command.user.vpn.ListRemoteAccessVpnsCmd;
import org.apache.cloudstack.api.command.user.vpn.ListVpnUsersCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;

import com.cloud.configuration.Config;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.event.dao.UsageEventDao;
import com.cloud.exception.AccountLimitException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VpnUser;
import com.cloud.network.VpnUser.State;
import com.cloud.network.VpnUserVO;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.RemoteAccessVpnDao;
import com.cloud.network.dao.RemoteAccessVpnVO;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.element.RemoteAccessVPNServiceProvider;
import com.cloud.network.rules.FirewallManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.server.ConfigurationServer;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.net.NetUtils;

public class RemoteAccessVpnManagerImpl extends ManagerBase implements RemoteAccessVpnService, Configurable {
    private final static Logger s_logger = Logger.getLogger(RemoteAccessVpnManagerImpl.class);

    static final ConfigKey<String> RemoteAccessVpnClientIpRange = new ConfigKey<String>("Network", String.class, RemoteAccessVpnClientIpRangeCK, "10.1.2.1-10.1.2.8",
        "The range of ips to be allocated to remote access vpn clients. The first ip in the range is used by the VPN server", false, ConfigKey.Scope.Account);

    @Inject
    AccountDao _accountDao;
    @Inject
    VpnUserDao _vpnUsersDao;
    @Inject
    RemoteAccessVpnDao _remoteAccessVpnDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    DomainManager _domainMgr;
    @Inject
    NetworkModel _networkMgr;
    @Inject
    RulesManager _rulesMgr;
    @Inject
    DomainDao _domainDao;
    @Inject
    FirewallRulesDao _rulesDao;
    @Inject
    FirewallManager _firewallMgr;
    @Inject
    UsageEventDao _usageEventDao;
    @Inject
    ConfigurationDao _configDao;
    List<RemoteAccessVPNServiceProvider> _vpnServiceProviders;

    @Inject
    ConfigurationServer _configServer;
    @Inject
    VpcDao _vpcDao;

    int _userLimit;
    int _pskLength;
    SearchBuilder<RemoteAccessVpnVO> VpnSearch;

    @Override
    @DB
    public RemoteAccessVpn createRemoteAccessVpn(final long publicIpId, String ipRange, boolean openFirewall, final Boolean forDisplay) throws NetworkRuleConflictException {
        CallContext ctx = CallContext.current();
        final Account caller = ctx.getCallingAccount();

        Long networkId = null;

        // make sure ip address exists
        final PublicIpAddress ipAddr = _networkMgr.getPublicIpAddress(publicIpId);
        if (ipAddr == null) {
            throw new InvalidParameterValueException("Unable to create remote access vpn, invalid public IP address id" + publicIpId);
        }

        _accountMgr.checkAccess(caller, null, true, ipAddr);

        if (!ipAddr.readyToUse()) {
            throw new InvalidParameterValueException("The Ip address is not ready to be used yet: " + ipAddr.getAddress());
        }

        IPAddressVO ipAddress = _ipAddressDao.findById(publicIpId);

        networkId = ipAddress.getAssociatedWithNetworkId();
        if (networkId != null) {
            _networkMgr.checkIpForService(ipAddress, Service.Vpn, null);
        }

        final Long vpcId = ipAddress.getVpcId();
        /* IP Address used for VPC must be the source NAT IP of whole VPC */
        if (vpcId != null && ipAddress.isSourceNat()) {
            assert networkId == null;
            // No firewall setting for VPC, it would be open internally
            openFirewall = false;
        }

        final boolean openFirewallFinal = openFirewall;

        if (networkId == null && vpcId == null) {
            throw new InvalidParameterValueException("Unable to create remote access vpn for the ipAddress: " + ipAddr.getAddress().addr() +
                    " as ip is not associated with any network or VPC");
        }

        RemoteAccessVpnVO vpnVO = _remoteAccessVpnDao.findByPublicIpAddress(publicIpId);

        if (vpnVO != null) {
            //if vpn is in Added state, return it to the api
            if (vpnVO.getState() == RemoteAccessVpn.State.Added) {
                return vpnVO;
            }
            throw new InvalidParameterValueException("A Remote Access VPN already exists for this public Ip address");
        }

        if (ipRange == null) {
            ipRange = RemoteAccessVpnClientIpRange.valueIn(ipAddr.getAccountId());
        }
        final String[] range = ipRange.split("-");
        if (range.length != 2) {
            throw new InvalidParameterValueException("Invalid ip range");
        }
        if (!NetUtils.isValidIp(range[0]) || !NetUtils.isValidIp(range[1])) {
            throw new InvalidParameterValueException("Invalid ip in range specification " + ipRange);
        }
        if (!NetUtils.validIpRange(range[0], range[1])) {
            throw new InvalidParameterValueException("Invalid ip range " + ipRange);
        }

        Pair<String, Integer> cidr = null;

        // TODO: assumes one virtual network / domr per account per zone
        if (networkId != null) {
            vpnVO = _remoteAccessVpnDao.findByAccountAndNetwork(ipAddr.getAccountId(), networkId);
            if (vpnVO != null) {
                //if vpn is in Added state, return it to the api
                if (vpnVO.getState() == RemoteAccessVpn.State.Added) {
                    return vpnVO;
                }
                throw new InvalidParameterValueException("A Remote Access VPN already exists for this account");
            }
            //Verify that vpn service is enabled for the network
            Network network = _networkMgr.getNetwork(networkId);
            if (!_networkMgr.areServicesSupportedInNetwork(network.getId(), Service.Vpn)) {
                throw new InvalidParameterValueException("Vpn service is not supported in network id=" + ipAddr.getAssociatedWithNetworkId());
            }
            cidr = NetUtils.getCidr(network.getCidr());
        } else { // Don't need to check VPC because there is only one IP(source NAT IP) available for VPN
            Vpc vpc = _vpcDao.findById(vpcId);
            cidr = NetUtils.getCidr(vpc.getCidr());
        }

        // FIXME: This check won't work for the case where the guest ip range
        // changes depending on the vlan allocated.
        String[] guestIpRange = NetUtils.getIpRangeFromCidr(cidr.first(), cidr.second());
        if (NetUtils.ipRangesOverlap(range[0], range[1], guestIpRange[0], guestIpRange[1])) {
            throw new InvalidParameterValueException("Invalid ip range: " + ipRange + " overlaps with guest ip range " + guestIpRange[0] + "-" + guestIpRange[1]);
        }
        // TODO: check sufficient range
        // TODO: check overlap with private and public ip ranges in datacenter

        long startIp = NetUtils.ip2Long(range[0]);
        final String newIpRange = NetUtils.long2Ip(++startIp) + "-" + range[1];
        final String sharedSecret = PasswordGenerator.generatePresharedKey(_pskLength);

        return Transaction.execute(new TransactionCallbackWithException<RemoteAccessVpn, NetworkRuleConflictException>() {
            @Override
            public RemoteAccessVpn doInTransaction(TransactionStatus status) throws NetworkRuleConflictException {
                if (vpcId == null) {
                    _rulesMgr.reservePorts(ipAddr, NetUtils.UDP_PROTO, Purpose.Vpn, openFirewallFinal, caller, NetUtils.VPN_PORT, NetUtils.VPN_L2TP_PORT,
                        NetUtils.VPN_NATT_PORT);
                }
                RemoteAccessVpnVO vpnVO =
                    new RemoteAccessVpnVO(ipAddr.getAccountId(), ipAddr.getDomainId(), ipAddr.getAssociatedWithNetworkId(), publicIpId, vpcId, range[0], newIpRange,
                        sharedSecret);

                if (forDisplay != null) {
                    vpnVO.setDisplay(forDisplay);
                }
                return _remoteAccessVpnDao.persist(vpnVO);
            }
        });
    }

    private void validateRemoteAccessVpnConfiguration() throws ConfigurationException {
        String ipRange = RemoteAccessVpnClientIpRange.value();
        if (ipRange == null) {
            s_logger.warn("Remote Access VPN global configuration missing client ip range -- ignoring");
            return;
        }
        Integer pskLength = _pskLength;
        if (pskLength != null && (pskLength < 8 || pskLength > 256)) {
            throw new ConfigurationException("Remote Access VPN: IPSec preshared key length should be between 8 and 256");
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

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_REMOTE_ACCESS_VPN_DESTROY, eventDescription = "removing remote access vpn", async = true)
    public boolean destroyRemoteAccessVpnForIp(long ipId, Account caller) throws ResourceUnavailableException {
        final RemoteAccessVpnVO vpn = _remoteAccessVpnDao.findByPublicIpAddress(ipId);
        if (vpn == null) {
            s_logger.debug("there are no Remote access vpns for public ip address id=" + ipId);
            return true;
        }

        _accountMgr.checkAccess(caller, AccessType.OperateEntry, true, vpn);

        RemoteAccessVpn.State prevState = vpn.getState();
        vpn.setState(RemoteAccessVpn.State.Removed);
        _remoteAccessVpnDao.update(vpn.getId(), vpn);

        boolean success = false;
        try {
            for (RemoteAccessVPNServiceProvider element : _vpnServiceProviders) {
                if (element.stopVpn(vpn)) {
                    success = true;
                    break;
                }
            }
        }catch (ResourceUnavailableException ex) {
            vpn.setState(prevState);
            _remoteAccessVpnDao.update(vpn.getId(), vpn);
            s_logger.debug("Failed to stop the vpn " + vpn.getId() + " , so reverted state to "+
                    RemoteAccessVpn.State.Running);
            success = false;
        } finally {
            if (success) {
                //Cleanup corresponding ports
                final List<? extends FirewallRule> vpnFwRules = _rulesDao.listByIpAndPurpose(ipId, Purpose.Vpn);

                boolean applyFirewall = false;
                final List<FirewallRuleVO> fwRules = new ArrayList<FirewallRuleVO>();
                //if related firewall rule is created for the first vpn port, it would be created for the 2 other ports as well, so need to cleanup the backend
                if (vpnFwRules.size() != 0 && _rulesDao.findByRelatedId(vpnFwRules.get(0).getId()) != null) {
                    applyFirewall = true;
                }

                if (applyFirewall) {
                    Transaction.execute(new TransactionCallbackNoReturn() {
                        @Override
                        public void doInTransactionWithoutResult(TransactionStatus status) {
                            for (FirewallRule vpnFwRule : vpnFwRules) {
                                //don't apply on the backend yet; send all 3 rules in a banch
                                _firewallMgr.revokeRelatedFirewallRule(vpnFwRule.getId(), false);
                                fwRules.add(_rulesDao.findByRelatedId(vpnFwRule.getId()));
                            }

                            s_logger.debug("Marked " + fwRules.size() + " firewall rules as Revoked as a part of disable remote access vpn");
                        }
                    });

                    //now apply vpn rules on the backend
                    s_logger.debug("Reapplying firewall rules for ip id=" + ipId + " as a part of disable remote access vpn");
                    success = _firewallMgr.applyIngressFirewallRules(ipId, caller);
                }

                if (success) {
                    try {
                        Transaction.execute(new TransactionCallbackNoReturn() {
                            @Override
                            public void doInTransactionWithoutResult(TransactionStatus status) {
                                _remoteAccessVpnDao.remove(vpn.getId());
                                // Stop billing of VPN users when VPN is removed. VPN_User_ADD events will be generated when VPN is created again
                                List<VpnUserVO> vpnUsers = _vpnUsersDao.listByAccount(vpn.getAccountId());
                                for (VpnUserVO user : vpnUsers) {
                                    // VPN_USER_REMOVE event is already generated for users in Revoke state
                                    if (user.getState() != VpnUser.State.Revoke) {
                                        UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VPN_USER_REMOVE, user.getAccountId(), 0, user.getId(), user.getUsername(),
                                            user.getClass().getName(), user.getUuid());
                                    }
                                }
                                if (vpnFwRules != null) {
                                    for (FirewallRule vpnFwRule : vpnFwRules) {
                                        _rulesDao.remove(vpnFwRule.getId());
                                        s_logger.debug("Successfully removed firewall rule with ip id=" + vpnFwRule.getSourceIpAddressId() + " and port " +
                                            vpnFwRule.getSourcePortStart().intValue() + " as a part of vpn cleanup");
                                    }
                                }
                            }
                        });
                    } catch (Exception ex) {
                        s_logger.warn("Unable to release the three vpn ports from the firewall rules", ex);
                    }
                }
            }
        }
        return success;
    }

    @Override
    @DB
    public VpnUser addVpnUser(final long vpnOwnerId, final String username, final String password) {
        final Account caller = CallContext.current().getCallingAccount();

        if (!username.matches("^[a-zA-Z0-9][a-zA-Z0-9@._-]{2,63}$")) {
            throw new InvalidParameterValueException("Username has to be begin with an alphabet have 3-64 characters including alphabets, numbers and the set '@.-_'");
        }
        if (!password.matches("^[a-zA-Z0-9][a-zA-Z0-9@#+=._-]{2,31}$")) {
            throw new InvalidParameterValueException("Password has to be 3-32 characters including alphabets, numbers and the set '@#+=.-_'");
        }

        return Transaction.execute(new TransactionCallback<VpnUser>() {
            @Override
            public VpnUser doInTransaction(TransactionStatus status) {
                Account owner = _accountDao.lockRow(vpnOwnerId, true);
                if (owner == null) {
                    throw new InvalidParameterValueException("Unable to add vpn user: Another operation active");
                }
                _accountMgr.checkAccess(caller, null, true, owner);

                //don't allow duplicated user names for the same account
                VpnUserVO vpnUser = _vpnUsersDao.findByAccountAndUsername(owner.getId(), username);
                if (vpnUser != null) {
                    throw new InvalidParameterValueException("VPN User with name " + username + " is already added for account " + owner);
                }

                long userCount = _vpnUsersDao.getVpnUserCount(owner.getId());
                if (userCount >= _userLimit) {
                    throw new AccountLimitException("Cannot add more than " + _userLimit + " remote access vpn users");
                }

                VpnUser user = _vpnUsersDao.persist(new VpnUserVO(vpnOwnerId, owner.getDomainId(), username, password));
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VPN_USER_ADD, user.getAccountId(), 0, user.getId(), user.getUsername(), user.getClass().getName(),
                    user.getUuid());

                return user;
            }
        });
    }

    @DB
    @Override
    public boolean removeVpnUser(long vpnOwnerId, String username, Account caller) {
        final VpnUserVO user = _vpnUsersDao.findByAccountAndUsername(vpnOwnerId, username);
        if (user == null) {
            throw new InvalidParameterValueException("Could not find vpn user " + username);
        }
        _accountMgr.checkAccess(caller, null, true, user);

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                user.setState(State.Revoke);
                _vpnUsersDao.update(user.getId(), user);
                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VPN_USER_REMOVE, user.getAccountId(), 0, user.getId(), user.getUsername(), user.getClass().getName(),
                    user.getUuid());
            }
        });

        return true;
    }

    @Override
    public List<? extends VpnUser> listVpnUsers(long vpnOwnerId, String userName) {
        Account caller = CallContext.current().getCallingAccount();
        Account owner = _accountDao.findById(vpnOwnerId);
        _accountMgr.checkAccess(caller, null, true, owner);
        return _vpnUsersDao.listByAccount(vpnOwnerId);
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_REMOTE_ACCESS_VPN_CREATE, eventDescription = "creating remote access vpn", async = true)
    public RemoteAccessVpnVO startRemoteAccessVpn(long ipAddressId, boolean openFirewall) throws ResourceUnavailableException {
        Account caller = CallContext.current().getCallingAccount();

        final RemoteAccessVpnVO vpn = _remoteAccessVpnDao.findByPublicIpAddress(ipAddressId);
        if (vpn == null) {
            throw new InvalidParameterValueException("Unable to find your vpn: " + ipAddressId);
        }

        if (vpn.getVpcId() != null) {
            openFirewall = false;
        }

        _accountMgr.checkAccess(caller, null, true, vpn);

        boolean started = false;
        try {
            boolean firewallOpened = true;
            if (openFirewall) {
                firewallOpened = _firewallMgr.applyIngressFirewallRules(vpn.getServerAddressId(), caller);
            }

            if (firewallOpened) {
                for (RemoteAccessVPNServiceProvider element : _vpnServiceProviders) {
                    if (element.startVpn(vpn)) {
                        started = true;
                        break;
                    }
                }
            }

            return vpn;
        } finally {
            if (started) {
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        vpn.setState(RemoteAccessVpn.State.Running);
                        _remoteAccessVpnDao.update(vpn.getId(), vpn);

                        // Start billing of existing VPN users in ADD and Active state
                        List<VpnUserVO> vpnUsers = _vpnUsersDao.listByAccount(vpn.getAccountId());
                        for (VpnUserVO user : vpnUsers) {
                            if (user.getState() != VpnUser.State.Revoke) {
                                UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VPN_USER_ADD, user.getAccountId(), 0, user.getId(), user.getUsername(),
                                    user.getClass().getName(), user.getUuid());
                            }
                        }
                    }
                });
            }
        }
    }

    @DB
    @Override
    public boolean applyVpnUsers(long vpnOwnerId, String userName) {
        Account caller = CallContext.current().getCallingAccount();
        Account owner = _accountDao.findById(vpnOwnerId);
        _accountMgr.checkAccess(caller, null, true, owner);

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

        boolean success = true;

        Boolean[] finals = new Boolean[users.size()];
        for (RemoteAccessVPNServiceProvider element : _vpnServiceProviders) {
            s_logger.debug("Applying vpn access to " + element.getName());
            for (RemoteAccessVpnVO vpn : vpns) {
                try {
                    String[] results = element.applyVpnUsers(vpn, users);
                    if (results != null) {
                        int indexUser = -1;
                        for (int i = 0; i < results.length; i++) {
                            indexUser ++;
                            if (indexUser == users.size()) {
                                indexUser = 0; // results on multiple VPC routers are combined in commit 13eb789, reset user index if one VR is done.
                            }
                            s_logger.debug("VPN User " + users.get(indexUser) + (results[i] == null ? " is set on " : (" couldn't be set due to " + results[i]) + " on ") + vpn.getUuid());
                            if (results[i] == null) {
                                if (finals[indexUser] == null) {
                                    finals[indexUser] = true;
                                }
                            } else {
                                finals[indexUser] = false;
                                success = false;
                            }
                        }
                    }
                } catch (Exception e) {
                    s_logger.warn("Unable to apply vpn users ", e);
                    success = false;

                    for (int i = 0; i < finals.length; i++) {
                        finals[i] = false;
                    }
                }
            }
        }

        for (int i = 0; i < finals.length; i++) {
            final VpnUserVO user = users.get(i);
            if (finals[i]) {
                if (user.getState() == State.Add) {
                    user.setState(State.Active);
                    _vpnUsersDao.update(user.getId(), user);
                } else if (user.getState() == State.Revoke) {
                    _vpnUsersDao.remove(user.getId());
                }
            } else {
                if (user.getState() == State.Add && (user.getUsername()).equals(userName)) {
                    Transaction.execute(new TransactionCallbackNoReturn() {
                        @Override
                        public void doInTransactionWithoutResult(TransactionStatus status) {
                            _vpnUsersDao.remove(user.getId());
                            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_VPN_USER_REMOVE, user.getAccountId(), 0, user.getId(), user.getUsername(), user.getClass()
                                .getName(), user.getUuid());
                        }
                    });
                }
                s_logger.warn("Failed to apply vpn for user " + user.getUsername() + ", accountId=" + user.getAccountId());
            }
        }

        return success;
    }

    @Override
    public Pair<List<? extends VpnUser>, Integer> searchForVpnUsers(ListVpnUsersCmd cmd) {
        String username = cmd.getUsername();
        Long id = cmd.getId();
        String keyword = cmd.getKeyword();
        Account caller = CallContext.current().getCallingAccount();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, id, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();
        Filter searchFilter = new Filter(VpnUserVO.class, "username", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<VpnUserVO> sb = _vpnUsersDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);


        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("username", sb.entity().getUsername(), SearchCriteria.Op.EQ);
        sb.and("keyword", sb.entity().getUsername(), SearchCriteria.Op.LIKE);
        sb.and("state", sb.entity().getState(), Op.IN);

        SearchCriteria<VpnUserVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        //list only active users
        sc.setParameters("state", State.Active, State.Add);

        if(keyword != null){
            sc.setParameters("keyword", "%" + keyword + "%");
        }

        if (id != null) {
            sc.setParameters("id", id);
        }

        if (username != null) {
            sc.setParameters("username", username);
        }

        Pair<List<VpnUserVO>, Integer> result = _vpnUsersDao.searchAndCount(sc, searchFilter);
        return new Pair<List<? extends VpnUser>, Integer>(result.first(), result.second());
    }

    @Override
    public Pair<List<? extends RemoteAccessVpn>, Integer> searchForRemoteAccessVpns(ListRemoteAccessVpnsCmd cmd) {
        // do some parameter validation
        Account caller = CallContext.current().getCallingAccount();
        Long ipAddressId = cmd.getPublicIpId();
        List<Long> permittedAccounts = new ArrayList<Long>();

        Long vpnId = cmd.getId();
        Long networkId = cmd.getNetworkId();

        if (ipAddressId != null) {
            PublicIpAddress publicIp = _networkMgr.getPublicIpAddress(ipAddressId);
            if (publicIp == null) {
                throw new InvalidParameterValueException("Unable to list remote access vpns, IP address " + ipAddressId + " not found.");
            } else {
                Long ipAddrAcctId = publicIp.getAccountId();
                if (ipAddrAcctId == null) {
                    throw new InvalidParameterValueException("Unable to list remote access vpns, IP address " + ipAddressId + " is not associated with an account.");
                }
            }
            _accountMgr.checkAccess(caller, null, true, publicIp);
        }

        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<Long, Boolean, ListProjectResourcesCriteria>(cmd.getDomainId(), cmd.isRecursive(), null);
        _accountMgr.buildACLSearchParameters(caller, null, cmd.getAccountName(), cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject, cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        Filter filter = new Filter(RemoteAccessVpnVO.class, "serverAddressId", false, cmd.getStartIndex(), cmd.getPageSizeVal());
        SearchBuilder<RemoteAccessVpnVO> sb = _remoteAccessVpnDao.createSearchBuilder();
        _accountMgr.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);

        sb.and("serverAddressId", sb.entity().getServerAddressId(), Op.EQ);
        sb.and("id", sb.entity().getId(), Op.EQ);
        sb.and("networkId", sb.entity().getNetworkId(), Op.EQ);
        sb.and("state", sb.entity().getState(), Op.EQ);
        sb.and("display", sb.entity().isDisplay(), Op.EQ);

        SearchCriteria<RemoteAccessVpnVO> sc = sb.create();
        _accountMgr.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts, listProjectResourcesCriteria);


        sc.setParameters("state", RemoteAccessVpn.State.Running);

        if (ipAddressId != null) {
            sc.setParameters("serverAddressId", ipAddressId);
        }

        if (vpnId != null) {
            sc.setParameters("id", vpnId);
        }

        if (networkId != null) {
            sc.setParameters("networkId", networkId);
        }

        Pair<List<RemoteAccessVpnVO>, Integer> result = _remoteAccessVpnDao.searchAndCount(sc, filter);
        return new Pair<List<? extends RemoteAccessVpn>, Integer>(result.first(), result.second());
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        Map<String, String> configs = _configDao.getConfiguration(params);

        _userLimit = NumbersUtil.parseInt(configs.get(Config.RemoteAccessVpnUserLimit.key()), 8);

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
    public List<? extends RemoteAccessVpn> listRemoteAccessVpns(long networkId) {
        return _remoteAccessVpnDao.listByNetworkId(networkId);
    }

    @Override
    public RemoteAccessVpn getRemoteAccessVpn(long vpnAddrId) {
        return _remoteAccessVpnDao.findByPublicIpAddress(vpnAddrId);
    }

    @Override
    public RemoteAccessVpn getRemoteAccessVpnById(long vpnId) {
        return _remoteAccessVpnDao.findById(vpnId);
    }

    public List<RemoteAccessVPNServiceProvider> getRemoteAccessVPNServiceProviders() {
        List<RemoteAccessVPNServiceProvider> result = new ArrayList<RemoteAccessVPNServiceProvider>();
        for (Iterator<RemoteAccessVPNServiceProvider> e = _vpnServiceProviders.iterator(); e.hasNext();) {
            result.add(e.next());
        }

        return result;
    }

    @Override
    public String getConfigComponentName() {
        return RemoteAccessVpnService.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {RemoteAccessVpnClientIpRange};
    }

    public List<RemoteAccessVPNServiceProvider> getVpnServiceProviders() {
        return _vpnServiceProviders;
    }

    public void setVpnServiceProviders(List<RemoteAccessVPNServiceProvider> vpnServiceProviders) {
        _vpnServiceProviders = vpnServiceProviders;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_REMOTE_ACCESS_VPN_UPDATE, eventDescription = "updating remote access vpn", async = true)
    public RemoteAccessVpn updateRemoteAccessVpn(long id, String customId, Boolean forDisplay) {
        final RemoteAccessVpnVO vpn = _remoteAccessVpnDao.findById(id);
        if (vpn == null) {
            throw new InvalidParameterValueException("Can't find remote access vpn by id " + id);
        }

        _accountMgr.checkAccess(CallContext.current().getCallingAccount(), null, true, vpn);
        if (customId != null) {
            vpn.setUuid(customId);
        }
        if (forDisplay != null) {
            vpn.setDisplay(forDisplay);
        }

        _remoteAccessVpnDao.update(vpn.getId(), vpn);
        return _remoteAccessVpnDao.findById(id);
    }

}
