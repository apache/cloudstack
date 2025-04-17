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

import com.cloud.configuration.Config;
import com.cloud.domain.DomainVO;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.*;
import com.cloud.network.dao.*;
import com.cloud.network.element.RemoteAccessVPNServiceProvider;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.vpc.Vpc;
import com.cloud.user.Account;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.db.*;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import org.apache.cloudstack.api.command.user.vpn.ListRemoteAccessVpnsCmd;
import org.apache.cloudstack.api.command.user.vpn.ListVpnUsersCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import java.util.List;
import java.util.Map;

public class RemoteAccessVpnL2TPProtocol extends RemoteAccessVpnProtocolBase {

    private final static Logger s_logger = Logger.getLogger(RemoteAccessVpnL2TPProtocol.class);

    @Inject
    VpnUserDao _vpnUsersDao;

    public static final String PROTOCOL_NAME = "l2tp";
    public static final String PSK_KEY = "preshared_key";


    SearchBuilder<RemoteAccessVpnL2TPVO> VpnSearch;
    RemoteAccessVpnL2TPDao _remoteAccessVpnDao = new RemoteAccessVpnL2TPDao();

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

    public String GetName() {
        return PROTOCOL_NAME;
    }

    @DB
    public RemoteAccessVpn createRemoteAccessVpn(long publicIpId, String ipRange, boolean openFirewall, Boolean forDisplay, Integer port, Map<String, String> implementationData) throws NetworkRuleConflictException {

        CallContext ctx = CallContext.current();
        final Account caller = ctx.getCallingAccount();

        final PublicIpAddress ipAddr = _networkMgr.getPublicIpAddress(publicIpId);
        if (ipAddr == null) {
            throw new InvalidParameterValueException(String.format("Unable to create remote access VPN, invalid public IP address {\"id\": %s}.", publicIpId));
        }

        _accountMgr.checkAccess(caller, null, true, ipAddr);

        if (!ipAddr.readyToUse()) {
            throw new InvalidParameterValueException("The Ip address is not ready to be used yet: " + ipAddr.getAddress());
        }

        try {
            IPAddressVO ipAddress = _ipAddressDao.acquireInLockTable(publicIpId);

            //
            // check if ip and network exists
            //

            if (ipAddress == null) {
                s_logger.error(String.format("Unable to acquire lock on public IP %s.", publicIpId));
                throw new CloudRuntimeException("Unable to acquire lock on public IP.");
            }

            Long networkId = ipAddress.getAssociatedWithNetworkId();
            if (networkId != null) {
                _networkMgr.checkIpForService(ipAddress, Network.Service.Vpn, null);
            }

            final Long vpcId = ipAddress.getVpcId();
            if (vpcId != null && ipAddress.isSourceNat()) {
                assert networkId == null;
                openFirewall = false;
            }

            final boolean openFirewallFinal = openFirewall;

            if (networkId == null && vpcId == null) {
                throw new InvalidParameterValueException("Unable to create remote access vpn for the ipAddress: " + ipAddr.getAddress().addr() +
                        " as ip is not associated with any network or VPC");
            }

            //
            // check if port is already used on given ip
            //

            RemoteAccessVpn vpnVO = _remoteAccessVpnDao.findByPublicIpAddressAndPort(publicIpId, port);
            if (vpnVO != null) {
                if (vpnVO.getState() == RemoteAccessVpn.State.Added) {
                    return vpnVO;
                }

                throw new InvalidParameterValueException(String.format("A remote Access VPN already exists for the public IP address [%s] on port %d.", ipAddr.getAddress().toString(), port));
            }

            //
            // validate ip range
            //

            if (ipRange == null) {
                ipRange = RemoteAccessVpnClientIpRange.valueIn(ipAddr.getAccountId());
            }

            validateIpRange(ipRange, InvalidParameterValueException.class);
            String[] range = ipRange.split("-");
            Pair<String, Integer> cidr = null;

            if (networkId != null) {

                long ipAddressOwner = ipAddr.getAccountId();

                //
                // ????
                //

                vpnVO = _remoteAccessVpnDao.findByAccountNetworkAndPort(ipAddressOwner, networkId, port);
                if (vpnVO != null) {
                    if (vpnVO.getState() == RemoteAccessVpn.State.Added) {
                        return vpnVO;
                    }

                    throw new InvalidParameterValueException(String.format("A remote access VPN already exists for the account [%s].", ipAddressOwner));
                }

                //
                // Check if network supports vpn
                //

                Network network = _networkMgr.getNetwork(networkId);
                if (!_networkMgr.areServicesSupportedInNetwork(network.getId(), Network.Service.Vpn)) {
                    throw new InvalidParameterValueException("Vpn service is not supported in network id=" + ipAddr.getAssociatedWithNetworkId());
                }

                //
                // Get CIDR from net
                //
                cidr = NetUtils.getCidr(network.getCidr());

            } else {

                //
                // Get CIDR from vpc
                //

                Vpc vpc = _vpcDao.findById(vpcId);
                cidr = NetUtils.getCidr(vpc.getCidr());
            }

            String[] guestIpRange = NetUtils.getIpRangeFromCidr(cidr.first(), cidr.second());
            if (NetUtils.ipRangesOverlap(range[0], range[1], guestIpRange[0], guestIpRange[1])) {
                throw new InvalidParameterValueException("Invalid ip range: " + ipRange + " overlaps with guest ip range " + guestIpRange[0] + "-" + guestIpRange[1]);
            }

            long startIp = NetUtils.ip2Long(range[0]);
            final String newIpRange = NetUtils.long2Ip(++startIp) + "-" + range[1];

            final String rawPsk = implementationData.get(PSK_KEY);

            final String sharedSecret;

            if (rawPsk != null && !rawPsk.trim().isEmpty() && rawPsk.trim().length() == _pskLength) {
                sharedSecret = rawPsk.trim();
            } else {
                sharedSecret = PasswordGenerator.generatePresharedKey(_pskLength);
            }

            return Transaction.execute((TransactionCallbackWithException<RemoteAccessVpn, NetworkRuleConflictException>) status -> {

                if (vpcId == null) {
                    _rulesMgr.reservePorts(
                        ipAddr,
                        NetUtils.UDP_PROTO,
                        FirewallRule.Purpose.Vpn,
                        openFirewallFinal,
                        caller,
                        NetUtils.VPN_PORT,
                        NetUtils.VPN_L2TP_PORT,
                        NetUtils.VPN_NATT_PORT
                    );
                }

                RemoteAccessVpnL2TPVO remoteAccessVpnVO = new RemoteAccessVpnL2TPVO(
                    ipAddr.getAccountId(),
                    ipAddr.getDomainId(),
                    ipAddr.getAssociatedWithNetworkId(),
                    publicIpId,
                    vpcId,
                    range[0],
                    newIpRange,
                    sharedSecret
                );

                if (forDisplay != null) {
                    remoteAccessVpnVO.setDisplay(forDisplay);
                }

                return _remoteAccessVpnDao.persist(remoteAccessVpnVO);
            });

        } finally {
            _ipAddressDao.releaseFromLockTable(publicIpId);
        }
    }

    @DB
    @ActionEvent(eventType = EventTypes.EVENT_REMOTE_ACCESS_VPN_CREATE, eventDescription = "creating remote access vpn", async = true)
    public RemoteAccessVpn startRemoteAccessVpn(long vpnServerId, boolean openFirewall) throws ResourceUnavailableException {

        Account caller = CallContext.current().getCallingAccount();

        final RemoteAccessVpnL2TPVO vpn = _remoteAccessVpnDao.findById(vpnServerId);
        if (vpn == null) {
            throw new InvalidParameterValueException("Unable to find your vpn: " + vpnServerId);
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

                        List<VpnUserVO> vpnUsers = _vpnUsersDao.listByAccount(vpn.getAccountId());
                        for (VpnUserVO user : vpnUsers) {
                            if (user.getState() != VpnUser.State.Revoke) {
                                UsageEventUtils.publishUsageEvent(
                                        EventTypes.EVENT_VPN_USER_ADD,
                                        user.getAccountId(),
                                        0,
                                        user.getId(),
                                        user.getUsername(),
                                        user.getClass().getName(),
                                        user.getUuid()
                                );
                            }
                        }
                    }
                });
            }
        }
    }

    @Override
    public boolean destroyRemoteAccessVpnForIp(long ipId, Account caller, boolean forceCleanup) throws ResourceUnavailableException {
        return false;
    }

    @Override
    public VpnUser addVpnUser(long vpnOwnerId, String userName, String password) {
        return null;
    }
    @Override
    public boolean removeVpnUser(long vpnOwnerId, String userName, Account caller) {
        return false;
    }
    @Override
    public List<? extends VpnUser> listVpnUsers(long vpnOwnerId, String userName) {
        return List.of();
    }
    @Override
    public boolean applyVpnUsers(long vpnOwnerId, String userName, boolean forRemove) throws ResourceUnavailableException {
        return false;
    }
    @Override
    public boolean applyVpnUsers(long vpnOwnerId, String userName) throws ResourceUnavailableException {
        return false;
    }
    @Override
    public Pair<List<? extends RemoteAccessVpn>, Integer> searchForRemoteAccessVpns(ListRemoteAccessVpnsCmd cmd) {
        return null;
    }
    @Override
    public Pair<List<? extends VpnUser>, Integer> searchForVpnUsers(ListVpnUsersCmd cmd) {
        return null;
    }
    @Override
    public List<? extends RemoteAccessVpn> listRemoteAccessVpns(long networkId) {
        return List.of();
    }
    @Override
    public RemoteAccessVpn getRemoteAccessVpn(long vpnAddrId) {
        return null;
    }
    @Override
    public RemoteAccessVpn getRemoteAccessVpnById(long vpnId) {
        return null;
    }
    @Override
    public RemoteAccessVpn updateRemoteAccessVpn(long id, String customId, Boolean forDisplay) {
        return null;
    }
    public String getConfigComponentName() {
        return RemoteAccessVpnProtocolBase.class.getSimpleName();
    }
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {RemoteAccessVpnClientIpRange};
    }

}
