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
package com.cloud.network;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.ExternalNetworkResourceUsageAnswer;
import com.cloud.agent.api.ExternalNetworkResourceUsageCommand;
import com.cloud.configuration.Config;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.dao.ExternalFirewallDeviceDao;
import com.cloud.network.dao.ExternalFirewallDeviceVO;
import com.cloud.network.dao.ExternalLoadBalancerDeviceDao;
import com.cloud.network.dao.ExternalLoadBalancerDeviceVO;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.InlineLoadBalancerNicMapDao;
import com.cloud.network.dao.InlineLoadBalancerNicMapVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkExternalFirewallDao;
import com.cloud.network.dao.NetworkExternalFirewallVO;
import com.cloud.network.dao.NetworkExternalLoadBalancerDao;
import com.cloud.network.dao.NetworkExternalLoadBalancerVO;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.ResourceManager;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicVO;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;

@Component
@Local(value = {ExternalDeviceUsageManager.class})
public class ExternalDeviceUsageManagerImpl extends ManagerBase implements ExternalDeviceUsageManager {

    String _name;
    @Inject
    NetworkExternalLoadBalancerDao _networkExternalLBDao;
    @Inject
    ExternalLoadBalancerDeviceDao _externalLoadBalancerDeviceDao;
    @Inject
    HostDao _hostDao;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    InlineLoadBalancerNicMapDao _inlineLoadBalancerNicMapDao;
    @Inject
    NicDao _nicDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    UserStatisticsDao _userStatsDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    DomainRouterDao _routerDao;
    @Inject
    LoadBalancerDao _loadBalancerDao;
    @Inject
    PortForwardingRulesDao _portForwardingRulesDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    HostDetailsDao _hostDetailDao;
    @Inject
    NetworkExternalLoadBalancerDao _networkLBDao;
    @Inject
    NetworkServiceMapDao _ntwkSrvcProviderDao;
    @Inject
    NetworkExternalFirewallDao _networkExternalFirewallDao;
    @Inject
    ExternalFirewallDeviceDao _externalFirewallDeviceDao;
    @Inject
    protected HostPodDao _podDao = null;
    @Inject
    NetworkModel _networkModel;

    ScheduledExecutorService _executor;
    private int _externalNetworkStatsInterval;
    private static final org.apache.log4j.Logger s_logger = Logger.getLogger(ExternalDeviceUsageManagerImpl.class);

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _externalNetworkStatsInterval = NumbersUtil.parseInt(_configDao.getValue(Config.ExternalNetworkStatsInterval.key()), 300);
        if (_externalNetworkStatsInterval > 0) {
            _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("ExternalNetworkMonitor"));
        }
        return true;

    }

    @Override
    public boolean start() {
        if (_externalNetworkStatsInterval > 0) {
            _executor.scheduleAtFixedRate(new ExternalDeviceNetworkUsageTask(), _externalNetworkStatsInterval, _externalNetworkStatsInterval, TimeUnit.SECONDS);
        }
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

    private ExternalLoadBalancerDeviceVO getExternalLoadBalancerForNetwork(Network network) {
        NetworkExternalLoadBalancerVO lbDeviceForNetwork = _networkExternalLBDao.findByNetworkId(network.getId());
        if (lbDeviceForNetwork != null) {
            long lbDeviceId = lbDeviceForNetwork.getExternalLBDeviceId();
            ExternalLoadBalancerDeviceVO lbDeviceVo = _externalLoadBalancerDeviceDao.findById(lbDeviceId);
            assert (lbDeviceVo != null);
            return lbDeviceVo;
        }
        return null;
    }

    private ExternalFirewallDeviceVO getExternalFirewallForNetwork(Network network) {
        NetworkExternalFirewallVO fwDeviceForNetwork = _networkExternalFirewallDao.findByNetworkId(network.getId());
        if (fwDeviceForNetwork != null) {
            long fwDeviceId = fwDeviceForNetwork.getExternalFirewallDeviceId();
            ExternalFirewallDeviceVO fwDevice = _externalFirewallDeviceDao.findById(fwDeviceId);
            assert (fwDevice != null);
            return fwDevice;
        }
        return null;
    }

    @Override
    public void updateExternalLoadBalancerNetworkUsageStats(long loadBalancerRuleId) {

        LoadBalancerVO lb = _loadBalancerDao.findById(loadBalancerRuleId);
        if (lb == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Cannot update usage stats, LB rule is not found");
            }
            return;
        }
        long networkId = lb.getNetworkId();
        Network network = _networkDao.findById(networkId);
        if (network == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Cannot update usage stats, Network is not found");
            }
            return;
        }

        ExternalLoadBalancerDeviceVO lbDeviceVO = getExternalLoadBalancerForNetwork(network);
        if (lbDeviceVO == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Cannot update usage stats,  No external LB device found");
            }
            return;
        }

        // Get network stats from the external load balancer
        ExternalNetworkResourceUsageAnswer lbAnswer = null;
        HostVO externalLoadBalancer = _hostDao.findById(lbDeviceVO.getHostId());
        if (externalLoadBalancer != null) {
            ExternalNetworkResourceUsageCommand cmd = new ExternalNetworkResourceUsageCommand();
            lbAnswer = (ExternalNetworkResourceUsageAnswer)_agentMgr.easySend(externalLoadBalancer.getId(), cmd);
            if (lbAnswer == null || !lbAnswer.getResult()) {
                String details = (lbAnswer != null) ? lbAnswer.getDetails() : "details unavailable";
                String msg = "Unable to get external load balancer stats for network" + networkId + " due to: " + details + ".";
                s_logger.error(msg);
                return;
            }
        }

        long accountId = lb.getAccountId();
        AccountVO account = _accountDao.findById(accountId);
        if (account == null) {
            s_logger.debug("Skipping stats update for external LB for account with ID " + accountId);
            return;
        }

        String publicIp = _networkModel.getIp(lb.getSourceIpAddressId()).getAddress().addr();
        DataCenterVO zone = _dcDao.findById(network.getDataCenterId());
        String statsEntryIdentifier =
            "account " + account.getAccountName() + ", zone " + zone.getName() + ", network ID " + networkId + ", host ID " + externalLoadBalancer.getName();

        long newCurrentBytesSent = 0;
        long newCurrentBytesReceived = 0;

        if (publicIp != null) {
            long[] bytesSentAndReceived = null;
            statsEntryIdentifier += ", public IP: " + publicIp;
            boolean inline = _networkModel.isNetworkInlineMode(network);
            if (externalLoadBalancer.getType().equals(Host.Type.ExternalLoadBalancer) && inline) {
                // Look up stats for the guest IP address that's mapped to the public IP address
                InlineLoadBalancerNicMapVO mapping = _inlineLoadBalancerNicMapDao.findByPublicIpAddress(publicIp);

                if (mapping != null) {
                    NicVO nic = _nicDao.findById(mapping.getNicId());
                    String loadBalancingIpAddress = nic.getIPv4Address();
                    bytesSentAndReceived = lbAnswer.ipBytes.get(loadBalancingIpAddress);

                    if (bytesSentAndReceived != null) {
                        bytesSentAndReceived[0] = 0;
                    }
                }
            } else {
                bytesSentAndReceived = lbAnswer.ipBytes.get(publicIp);
            }

            if (bytesSentAndReceived == null) {
                s_logger.debug("Didn't get an external network usage answer for public IP " + publicIp);
            } else {
                newCurrentBytesSent += bytesSentAndReceived[0];
                newCurrentBytesReceived += bytesSentAndReceived[1];
            }

            commitStats(networkId, externalLoadBalancer, accountId, publicIp, zone, statsEntryIdentifier, newCurrentBytesSent, newCurrentBytesReceived);
        }
    }

    private void commitStats(final long networkId, final HostVO externalLoadBalancer, final long accountId, final String publicIp, final DataCenterVO zone,
        final String statsEntryIdentifier, final long newCurrentBytesSent, final long newCurrentBytesReceived) {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                UserStatisticsVO userStats;
                userStats = _userStatsDao.lock(accountId, zone.getId(), networkId, publicIp, externalLoadBalancer.getId(), externalLoadBalancer.getType().toString());

                if (userStats != null) {
                    long oldNetBytesSent = userStats.getNetBytesSent();
                    long oldNetBytesReceived = userStats.getNetBytesReceived();
                    long oldCurrentBytesSent = userStats.getCurrentBytesSent();
                    long oldCurrentBytesReceived = userStats.getCurrentBytesReceived();
                    String warning =
                        "Received an external network stats byte count that was less than the stored value. Zone ID: " + userStats.getDataCenterId() + ", account ID: " +
                            userStats.getAccountId() + ".";

                    userStats.setCurrentBytesSent(newCurrentBytesSent);
                    if (oldCurrentBytesSent > newCurrentBytesSent) {
                        s_logger.warn(warning + "Stored bytes sent: " + oldCurrentBytesSent + ", new bytes sent: " + newCurrentBytesSent + ".");
                        userStats.setNetBytesSent(oldNetBytesSent + oldCurrentBytesSent);
                    }

                    userStats.setCurrentBytesReceived(newCurrentBytesReceived);
                    if (oldCurrentBytesReceived > newCurrentBytesReceived) {
                        s_logger.warn(warning + "Stored bytes received: " + oldCurrentBytesReceived + ", new bytes received: " + newCurrentBytesReceived + ".");
                        userStats.setNetBytesReceived(oldNetBytesReceived + oldCurrentBytesReceived);
                    }

                    if (_userStatsDao.update(userStats.getId(), userStats)) {
                        s_logger.debug("Successfully updated stats for " + statsEntryIdentifier);
                    } else {
                        s_logger.debug("Failed to update stats for " + statsEntryIdentifier);
                    }
                } else {
                    s_logger.warn("Unable to find user stats entry for " + statsEntryIdentifier);
                }
            }
        });
    }

    protected class ExternalDeviceNetworkUsageTask extends ManagedContextRunnable {

        public ExternalDeviceNetworkUsageTask() {

        }

        @Override
        protected void runInContext() {
            GlobalLock scanLock = GlobalLock.getInternLock("ExternalDeviceNetworkUsageManagerImpl");
            try {
                if (scanLock.lock(20)) {
                    try {
                        runExternalDeviceNetworkUsageTask();
                    } finally {
                        scanLock.unlock();
                    }
                }
            } catch (Exception e) {
                s_logger.warn("Problems while getting external device usage", e);
            } finally {
                scanLock.releaseRef();
            }
        }

        private void runExternalDeviceNetworkUsageTask() {
            s_logger.debug("External devices stats collector is running...");

            for (DataCenterVO zone : _dcDao.listAll()) {
                List<DomainRouterVO> domainRoutersInZone = _routerDao.listByDataCenter(zone.getId());
                if (domainRoutersInZone == null) {
                    continue;
                }
                Map<Long, ExternalNetworkResourceUsageAnswer> lbDeviceUsageAnswerMap = new HashMap<Long, ExternalNetworkResourceUsageAnswer>();
                Map<Long, ExternalNetworkResourceUsageAnswer> fwDeviceUsageAnswerMap = new HashMap<Long, ExternalNetworkResourceUsageAnswer>();
                List<Long> accountsProcessed = new ArrayList<Long>();

                for (DomainRouterVO domainRouter : domainRoutersInZone) {
                    long accountId = domainRouter.getAccountId();

                    if (accountsProcessed.contains(new Long(accountId))) {
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("Networks for Account " + accountId + " are already processed for external network usage, so skipping usage check.");
                        }
                        continue;
                    }

                    long zoneId = zone.getId();

                    List<NetworkVO> networksForAccount = _networkDao.listByZoneAndGuestType(accountId, zoneId, Network.GuestType.Isolated, false);
                    if (networksForAccount == null) {
                        continue;
                    }

                    for (NetworkVO network : networksForAccount) {
                        if (!_networkModel.networkIsConfiguredForExternalNetworking(zoneId, network.getId())) {
                            s_logger.debug("Network " + network.getId() + " is not configured for external networking, so skipping usage check.");
                            continue;
                        }

                        ExternalFirewallDeviceVO fwDeviceVO = getExternalFirewallForNetwork(network);
                        ExternalLoadBalancerDeviceVO lbDeviceVO = getExternalLoadBalancerForNetwork(network);
                        if (lbDeviceVO == null && fwDeviceVO == null) {
                            continue;
                        }

                        // Get network stats from the external firewall
                        ExternalNetworkResourceUsageAnswer firewallAnswer = null;
                        HostVO externalFirewall = null;
                        if (fwDeviceVO != null) {
                            externalFirewall = _hostDao.findById(fwDeviceVO.getHostId());
                            if (externalFirewall != null) {
                                Long fwDeviceId = new Long(externalFirewall.getId());
                                if (!fwDeviceUsageAnswerMap.containsKey(fwDeviceId)) {
                                    try {
                                        ExternalNetworkResourceUsageCommand cmd = new ExternalNetworkResourceUsageCommand();
                                        firewallAnswer = (ExternalNetworkResourceUsageAnswer)_agentMgr.easySend(externalFirewall.getId(), cmd);
                                        if (firewallAnswer == null || !firewallAnswer.getResult()) {
                                            String details = (firewallAnswer != null) ? firewallAnswer.getDetails() : "details unavailable";
                                            String msg = "Unable to get external firewall stats for network" + zone.getName() + " due to: " + details + ".";
                                            s_logger.error(msg);
                                        } else {
                                            fwDeviceUsageAnswerMap.put(fwDeviceId, firewallAnswer);
                                        }
                                    } catch (Exception e) {
                                        String msg = "Unable to get external firewall stats for network" + zone.getName();
                                        s_logger.error(msg, e);
                                    }
                                } else {
                                    if (s_logger.isTraceEnabled()) {
                                        s_logger.trace("Reusing usage Answer for device id " + fwDeviceId + "for Network " + network.getId());
                                    }
                                    firewallAnswer = fwDeviceUsageAnswerMap.get(fwDeviceId);
                                }
                            }
                        }

                        // Get network stats from the external load balancer
                        ExternalNetworkResourceUsageAnswer lbAnswer = null;
                        HostVO externalLoadBalancer = null;
                        if (lbDeviceVO != null) {
                            externalLoadBalancer = _hostDao.findById(lbDeviceVO.getHostId());
                            if (externalLoadBalancer != null) {
                                Long lbDeviceId = new Long(externalLoadBalancer.getId());
                                if (!lbDeviceUsageAnswerMap.containsKey(lbDeviceId)) {
                                    try {
                                        ExternalNetworkResourceUsageCommand cmd = new ExternalNetworkResourceUsageCommand();
                                        lbAnswer = (ExternalNetworkResourceUsageAnswer)_agentMgr.easySend(externalLoadBalancer.getId(), cmd);
                                        if (lbAnswer == null || !lbAnswer.getResult()) {
                                            String details = (lbAnswer != null) ? lbAnswer.getDetails() : "details unavailable";
                                            String msg = "Unable to get external load balancer stats for " + zone.getName() + " due to: " + details + ".";
                                            s_logger.error(msg);
                                        } else {
                                            lbDeviceUsageAnswerMap.put(lbDeviceId, lbAnswer);
                                        }
                                    } catch (Exception e) {
                                        String msg = "Unable to get external load balancer stats for " + zone.getName();
                                        s_logger.error(msg, e);
                                    }
                                } else {
                                    if (s_logger.isTraceEnabled()) {
                                        s_logger.trace("Reusing usage Answer for device id " + lbDeviceId + "for Network " + network.getId());
                                    }
                                    lbAnswer = lbDeviceUsageAnswerMap.get(lbDeviceId);
                                }
                            }
                        }

                        if (firewallAnswer == null && lbAnswer == null) {
                            continue;
                        }

                        AccountVO account = _accountDao.findById(accountId);
                        if (account == null) {
                            s_logger.debug("Skipping stats update for account with ID " + accountId);
                            continue;
                        }

                        if (!manageStatsEntries(true, accountId, zoneId, network, externalFirewall, firewallAnswer, externalLoadBalancer, lbAnswer)) {
                            continue;
                        }

                        manageStatsEntries(false, accountId, zoneId, network, externalFirewall, firewallAnswer, externalLoadBalancer, lbAnswer);
                    }

                    accountsProcessed.add(new Long(accountId));
                }
            }
        }

        private boolean updateBytes(UserStatisticsVO userStats, long newCurrentBytesSent, long newCurrentBytesReceived) {
            long oldNetBytesSent = userStats.getNetBytesSent();
            long oldNetBytesReceived = userStats.getNetBytesReceived();
            long oldCurrentBytesSent = userStats.getCurrentBytesSent();
            long oldCurrentBytesReceived = userStats.getCurrentBytesReceived();
            String warning =
                "Received an external network stats byte count that was less than the stored value. Zone ID: " + userStats.getDataCenterId() + ", account ID: " +
                    userStats.getAccountId() + ".";

            userStats.setCurrentBytesSent(newCurrentBytesSent);
            if (oldCurrentBytesSent > newCurrentBytesSent) {
                s_logger.warn(warning + "Stored bytes sent: " + oldCurrentBytesSent + ", new bytes sent: " + newCurrentBytesSent + ".");
                userStats.setNetBytesSent(oldNetBytesSent + oldCurrentBytesSent);
            }

            userStats.setCurrentBytesReceived(newCurrentBytesReceived);
            if (oldCurrentBytesReceived > newCurrentBytesReceived) {
                s_logger.warn(warning + "Stored bytes received: " + oldCurrentBytesReceived + ", new bytes received: " + newCurrentBytesReceived + ".");
                userStats.setNetBytesReceived(oldNetBytesReceived + oldCurrentBytesReceived);
            }

            return _userStatsDao.update(userStats.getId(), userStats);
        }

        // Creates a new stats entry for the specified parameters, if one doesn't already exist.
        private boolean createStatsEntry(long accountId, long zoneId, long networkId, String publicIp, long hostId) {
            HostVO host = _hostDao.findById(hostId);
            UserStatisticsVO userStats = _userStatsDao.findBy(accountId, zoneId, networkId, publicIp, hostId, host.getType().toString());
            if (userStats == null) {
                return (_userStatsDao.persist(new UserStatisticsVO(accountId, zoneId, publicIp, hostId, host.getType().toString(), networkId)) != null);
            } else {
                return true;
            }
        }

        // Updates an existing stats entry with new data from the specified usage answer.
        private boolean updateStatsEntry(long accountId, long zoneId, long networkId, String publicIp, long hostId, ExternalNetworkResourceUsageAnswer answer,
            boolean inline) {
            AccountVO account = _accountDao.findById(accountId);
            DataCenterVO zone = _dcDao.findById(zoneId);
            NetworkVO network = _networkDao.findById(networkId);
            HostVO host = _hostDao.findById(hostId);
            String statsEntryIdentifier =
                "account " + account.getAccountName() + ", zone " + zone.getName() + ", network ID " + networkId + ", host ID " + host.getName();

            long newCurrentBytesSent = 0;
            long newCurrentBytesReceived = 0;

            if (publicIp != null) {
                long[] bytesSentAndReceived = null;
                statsEntryIdentifier += ", public IP: " + publicIp;

                if (host.getType().equals(Host.Type.ExternalLoadBalancer) && inline) {
                    // Look up stats for the guest IP address that's mapped to the public IP address
                    InlineLoadBalancerNicMapVO mapping = _inlineLoadBalancerNicMapDao.findByPublicIpAddress(publicIp);

                    if (mapping != null) {
                        NicVO nic = _nicDao.findById(mapping.getNicId());
                        String loadBalancingIpAddress = nic.getIPv4Address();
                        bytesSentAndReceived = answer.ipBytes.get(loadBalancingIpAddress);

                        if (bytesSentAndReceived != null) {
                            bytesSentAndReceived[0] = 0;
                        }
                    }
                } else {
                    bytesSentAndReceived = answer.ipBytes.get(publicIp);
                }

                if (bytesSentAndReceived == null) {
                    s_logger.debug("Didn't get an external network usage answer for public IP " + publicIp);
                } else {
                    newCurrentBytesSent += bytesSentAndReceived[0];
                    newCurrentBytesReceived += bytesSentAndReceived[1];
                }
            } else {
                URI broadcastURI = network.getBroadcastUri();
                if (broadcastURI == null) {
                    s_logger.debug("Not updating stats for guest network with ID " + network.getId() + " because the network is not implemented.");
                    return true;
                } else {
                    long vlanTag = Integer.parseInt(BroadcastDomainType.getValue(broadcastURI));
                    long[] bytesSentAndReceived = answer.guestVlanBytes.get(String.valueOf(vlanTag));

                    if (bytesSentAndReceived == null) {
                        s_logger.warn("Didn't get an external network usage answer for guest VLAN " + vlanTag);
                    } else {
                        newCurrentBytesSent += bytesSentAndReceived[0];
                        newCurrentBytesReceived += bytesSentAndReceived[1];
                    }
                }
            }

            UserStatisticsVO userStats;
            try {
                userStats = _userStatsDao.lock(accountId, zoneId, networkId, publicIp, hostId, host.getType().toString());
            } catch (Exception e) {
                s_logger.warn("Unable to find user stats entry for " + statsEntryIdentifier);
                return false;
            }

            if (updateBytes(userStats, newCurrentBytesSent, newCurrentBytesReceived)) {
                s_logger.debug("Successfully updated stats for " + statsEntryIdentifier);
                return true;
            } else {
                s_logger.debug("Failed to update stats for " + statsEntryIdentifier);
                return false;
            }
        }

        private boolean createOrUpdateStatsEntry(boolean create, long accountId, long zoneId, long networkId, String publicIp, long hostId,
            ExternalNetworkResourceUsageAnswer answer, boolean inline) {
            if (create) {
                return createStatsEntry(accountId, zoneId, networkId, publicIp, hostId);
            } else {
                return updateStatsEntry(accountId, zoneId, networkId, publicIp, hostId, answer, inline);
            }
        }

        /*
         * Creates/updates all necessary stats entries for an account and zone.
         * Stats entries are created for source NAT IP addresses, static NAT rules, port forwarding rules, and load
         * balancing rules
         */
        private boolean manageStatsEntries(final boolean create, final long accountId, final long zoneId, final Network network, final HostVO externalFirewall,
            final ExternalNetworkResourceUsageAnswer firewallAnswer, final HostVO externalLoadBalancer, final ExternalNetworkResourceUsageAnswer lbAnswer) {
            final String accountErrorMsg = "Failed to update external network stats entry. Details: account ID = " + accountId;
            try {
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(TransactionStatus status) {
                        String networkErrorMsg = accountErrorMsg + ", network ID = " + network.getId();

                        boolean sharedSourceNat = false;
                        Map<Network.Capability, String> sourceNatCapabilities = _networkModel.getNetworkServiceCapabilities(network.getId(), Network.Service.SourceNat);
                        if (sourceNatCapabilities != null) {
                            String supportedSourceNatTypes = sourceNatCapabilities.get(Network.Capability.SupportedSourceNatTypes).toLowerCase();
                            if (supportedSourceNatTypes.contains("zone")) {
                                sharedSourceNat = true;
                            }
                        }

                        if (externalFirewall != null && firewallAnswer != null) {
                            if (!sharedSourceNat) {
                                // Manage the entry for this network's source NAT IP address
                                List<IPAddressVO> sourceNatIps = _ipAddressDao.listByAssociatedNetwork(network.getId(), true);
                                if (sourceNatIps.size() == 1) {
                                    String publicIp = sourceNatIps.get(0).getAddress().addr();
                                    if (!createOrUpdateStatsEntry(create, accountId, zoneId, network.getId(), publicIp, externalFirewall.getId(), firewallAnswer, false)) {
                                        throw new CloudRuntimeException(networkErrorMsg + ", source NAT IP = " + publicIp);
                                    }
                                }

                                // Manage one entry for each static NAT rule in this network
                                List<IPAddressVO> staticNatIps = _ipAddressDao.listStaticNatPublicIps(network.getId());
                                for (IPAddressVO staticNatIp : staticNatIps) {
                                    String publicIp = staticNatIp.getAddress().addr();
                                    if (!createOrUpdateStatsEntry(create, accountId, zoneId, network.getId(), publicIp, externalFirewall.getId(), firewallAnswer, false)) {
                                        throw new CloudRuntimeException(networkErrorMsg + ", static NAT rule public IP = " + publicIp);
                                    }
                                }

                                // Manage one entry for each port forwarding rule in this network
                                List<PortForwardingRuleVO> portForwardingRules = _portForwardingRulesDao.listByNetwork(network.getId());
                                for (PortForwardingRuleVO portForwardingRule : portForwardingRules) {
                                    String publicIp = _networkModel.getIp(portForwardingRule.getSourceIpAddressId()).getAddress().addr();
                                    if (!createOrUpdateStatsEntry(create, accountId, zoneId, network.getId(), publicIp, externalFirewall.getId(), firewallAnswer, false)) {
                                        throw new CloudRuntimeException(networkErrorMsg + ", port forwarding rule public IP = " + publicIp);
                                    }
                                }
                            } else {
                                // Manage the account-wide entry for the external firewall
                                if (!createOrUpdateStatsEntry(create, accountId, zoneId, network.getId(), null, externalFirewall.getId(), firewallAnswer, false)) {
                                    throw new CloudRuntimeException(networkErrorMsg);
                                }
                            }
                        }

                        // If an external load balancer is added, manage one entry for each load balancing rule in this network
                        if (externalLoadBalancer != null && lbAnswer != null) {
                            boolean inline = _networkModel.isNetworkInlineMode(network);
                            List<LoadBalancerVO> loadBalancers = _loadBalancerDao.listByNetworkIdAndScheme(network.getId(), Scheme.Public);
                            for (LoadBalancerVO loadBalancer : loadBalancers) {
                                String publicIp = _networkModel.getIp(loadBalancer.getSourceIpAddressId()).getAddress().addr();
                                if (!createOrUpdateStatsEntry(create, accountId, zoneId, network.getId(), publicIp, externalLoadBalancer.getId(), lbAnswer, inline)) {
                                    throw new CloudRuntimeException(networkErrorMsg + ", load balancing rule public IP = " + publicIp);
                                }
                            }
                        }
                    }
                });
                return true;
            } catch (Exception e) {
                s_logger.warn("Exception: ", e);
                return false;
            }
        }
    }
}
