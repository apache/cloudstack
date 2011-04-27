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
package com.cloud.network.router;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifySshKeysCommand;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.IPAssocCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.RemoteAccessVpnCfgCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.api.routing.VpnUsersCfgCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.api.commands.UpgradeRouterCmd;
import com.cloud.async.AsyncJobManager;
import com.cloud.capacity.dao.CapacityDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.configuration.dao.ResourceLimitDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.AccountVlanMapDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IPAddressVO;
import com.cloud.network.IpAddress;
import com.cloud.network.LoadBalancerVO;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestIpType;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.SshKeysDistriMonitor;
import com.cloud.network.VirtualNetworkApplianceService;
import com.cloud.network.VpnUser;
import com.cloud.network.VpnUserVO;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkRuleConfigDao;
import com.cloud.network.dao.RemoteAccessVpnDao;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.rules.StaticNatRule;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineGuru;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfile.Param;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

/**
 * VirtualNetworkApplianceManagerImpl manages the different types of virtual network appliances available in the Cloud Stack.
 */
@Local(value = { VirtualNetworkApplianceManager.class, VirtualNetworkApplianceService.class })
public class VirtualNetworkApplianceManagerImpl implements VirtualNetworkApplianceManager, VirtualNetworkApplianceService, VirtualMachineGuru<DomainRouterVO> {
    private static final Logger s_logger = Logger.getLogger(VirtualNetworkApplianceManagerImpl.class);

    String _name;
    @Inject
    DataCenterDao _dcDao = null;
    @Inject
    VlanDao _vlanDao = null;
    @Inject
    FirewallRulesDao _rulesDao = null;
    @Inject
    LoadBalancerDao _loadBalancerDao = null;
    @Inject
    LoadBalancerVMMapDao _loadBalancerVMMapDao = null;
    @Inject
    IPAddressDao _ipAddressDao = null;
    @Inject
    VMTemplateDao _templateDao = null;
    @Inject
    DomainRouterDao _routerDao = null;
    @Inject
    UserDao _userDao = null;
    @Inject
    AccountDao _accountDao = null;
    @Inject
    DomainDao _domainDao = null;
    @Inject
    UserStatisticsDao _userStatsDao = null;
    @Inject
    VolumeDao _volsDao = null;
    @Inject
    HostDao _hostDao = null;
    @Inject
    EventDao _eventDao = null;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    HostPodDao _podDao = null;
    @Inject
    VMTemplateHostDao _vmTemplateHostDao = null;
    @Inject
    ResourceLimitDao _limitDao = null;
    @Inject
    CapacityDao _capacityDao = null;
    @Inject
    AgentManager _agentMgr;
    @Inject
    StorageManager _storageMgr;
    @Inject
    AlertManager _alertMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    AccountService _accountService;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    AsyncJobManager _asyncMgr;
    @Inject
    ServiceOfferingDao _serviceOfferingDao = null;
    @Inject
    UserVmDao _userVmDao;
    @Inject
    FirewallRulesDao _firewallRulesDao;
    @Inject
    NetworkRuleConfigDao _networkRuleConfigDao;
    @Inject
    AccountVlanMapDao _accountVlanMapDao;
    @Inject
    UserStatisticsDao _statsDao = null;
    @Inject
    NetworkOfferingDao _networkOfferingDao = null;
    @Inject
    GuestOSDao _guestOSDao = null;
    @Inject
    NetworkManager _networkMgr;
    @Inject
    VirtualMachineManager _itMgr;
    @Inject
    VpnUserDao _vpnUsersDao;
    @Inject
    RemoteAccessVpnDao _remoteAccessVpnDao;
    @Inject
    RulesManager _rulesMgr;
    @Inject
    NetworkDao _networkDao;
    @Inject
    LoadBalancingRulesManager _lbMgr;
    @Inject
    PortForwardingRulesDao _pfRulesDao;
    @Inject
    RemoteAccessVpnDao _vpnDao;
    @Inject
    VMInstanceDao _instanceDao;
    @Inject
    NicDao _nicDao;

    int _routerRamSize;
    int _routerCpuMHz;
    int _retry = 2;
    String _instance;
    String _mgmt_host;
    String _mgmt_cidr;

    int _routerStatsInterval = 300;
    private ServiceOfferingVO _offering;
    private String trafficSentinelHostname;

    ScheduledExecutorService _executor;

    Account _systemAcct;

    @Override
    public DomainRouterVO getRouter(long accountId, long dataCenterId) {
        return _routerDao.findBy(accountId, dataCenterId);
    }

    @Override
    public boolean sendSshKeysToHost(Long hostId, String pubKey, String prvKey) {
        ModifySshKeysCommand cmd = new ModifySshKeysCommand(pubKey, prvKey);
        final Answer answer = _agentMgr.easySend(hostId, cmd);

        if (answer != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean destroyRouter(final long routerId) throws ResourceUnavailableException, ConcurrentOperationException {
        UserContext context = UserContext.current();
        User user = _accountMgr.getActiveUser(context.getCallerUserId());

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Attempting to destroy router " + routerId);
        }

        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            return true;
        }
        boolean result = _itMgr.expunge(router, user, _accountMgr.getAccount(router.getAccountId()));

        return result;
    }

    @Override
    @DB
    public VirtualRouter upgradeRouter(UpgradeRouterCmd cmd) {
        Long routerId = cmd.getId();
        Long serviceOfferingId = cmd.getServiceOfferingId();
        Account account = UserContext.current().getCaller();

        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            throw new InvalidParameterValueException("Unable to find router with id " + routerId);
        }

        if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), router.getDomainId())) {
            throw new PermissionDeniedException("Invalid domain router id (" + routerId + ") given, unable to stop router.");
        }

        if (router.getServiceOfferingId() == serviceOfferingId) {
            s_logger.debug("Router: " + routerId + "already has service offering: " + serviceOfferingId);
            return _routerDao.findById(routerId);
        }

        ServiceOfferingVO newServiceOffering = _serviceOfferingDao.findById(serviceOfferingId);
        if (newServiceOffering == null) {
            throw new InvalidParameterValueException("Unable to find service offering with id " + serviceOfferingId);
        }

        // Check that the router is stopped
        if (!router.getState().equals(State.Stopped)) {
            s_logger.warn("Unable to upgrade router " + router.toString() + " in state " + router.getState());
            throw new InvalidParameterValueException("Unable to upgrade router " + router.toString() + " in state " + router.getState()
                    + "; make sure the router is stopped and not in an error state before upgrading.");
        }

        ServiceOfferingVO currentServiceOffering = _serviceOfferingDao.findById(router.getServiceOfferingId());

        if (currentServiceOffering.getUseLocalStorage() != newServiceOffering.getUseLocalStorage()) {
            throw new InvalidParameterValueException("Can't upgrade, due to new local storage status : " + newServiceOffering.getUseLocalStorage() + " is different from "
                    + "curruent local storage status: " + currentServiceOffering.getUseLocalStorage());
        }

        router.setServiceOfferingId(serviceOfferingId);
        if (_routerDao.update(routerId, router)) {
            return _routerDao.findById(routerId);
        } else {
            throw new CloudRuntimeException("Unable to upgrade router " + routerId);
        }

    }

    private String rot13(final String password) {
        final StringBuffer newPassword = new StringBuffer("");

        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);

            if ((c >= 'a' && c <= 'm') || ((c >= 'A' && c <= 'M'))) {
                c += 13;
            } else if ((c >= 'n' && c <= 'z') || (c >= 'N' && c <= 'Z')) {
                c -= 13;
            }

            newPassword.append(c);
        }

        return newPassword.toString();
    }

    @Override
    public boolean savePasswordToRouter(Network network, NicProfile nic, VirtualMachineProfile<UserVm> profile) throws ResourceUnavailableException {
        DomainRouterVO router = _routerDao.findByNetwork(network.getId());
        if (router == null) {
            s_logger.warn("Unable save password, router doesn't exist in network " + network.getId());
            throw new CloudRuntimeException("Unable to save password to router");
        }

        UserVm userVm = profile.getVirtualMachine();
        String password = (String) profile.getParameter(Param.VmPassword);
        String encodedPassword = rot13(password);

        Commands cmds = new Commands(OnError.Continue);
        SavePasswordCommand cmd = new SavePasswordCommand(encodedPassword, nic.getIp4Address(), userVm.getName());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, router.getPrivateIpAddress());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        cmds.addCommand("password", cmd);

        return sendCommandsToRouter(router, cmds);
    }

    @Override
    public VirtualRouter stopRouter(long routerId, boolean forced) throws ResourceUnavailableException, ConcurrentOperationException {
        UserContext context = UserContext.current();
        Account account = context.getCaller();

        // verify parameters
        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            throw new InvalidParameterValueException("Unable to find router by id " + routerId + ".");
        }

        _accountMgr.checkAccess(account, router);

        UserVO user = _userDao.findById(UserContext.current().getCallerUserId());

        return stop(router, forced, user, account);
    }

    @DB
    public void processStopOrRebootAnswer(final DomainRouterVO router, Answer answer) {
        final Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            final UserStatisticsVO userStats = _userStatsDao.lock(router.getAccountId(), router.getDataCenterId(), null, router.getId(), router.getType().toString());
            if (userStats != null) {
                final RebootAnswer sa = (RebootAnswer) answer;
                final Long received = sa.getBytesReceived();
                long netBytes = 0;
                if (received != null) {
                    if (received.longValue() >= userStats.getCurrentBytesReceived()) {
                        netBytes = received.longValue();
                    } else {
                        netBytes = userStats.getCurrentBytesReceived() + received;
                    }
                } else {
                    netBytes = userStats.getCurrentBytesReceived();
                }
                userStats.setCurrentBytesReceived(0);
                userStats.setNetBytesReceived(userStats.getNetBytesReceived() + netBytes);

                final Long sent = sa.getBytesSent();

                if (sent != null) {
                    if (sent.longValue() >= userStats.getCurrentBytesSent()) {
                        netBytes = sent.longValue();
                    } else {
                        netBytes = userStats.getCurrentBytesSent() + sent;
                    }
                } else {
                    netBytes = userStats.getCurrentBytesSent();
                }
                userStats.setNetBytesSent(userStats.getNetBytesSent() + netBytes);
                userStats.setCurrentBytesSent(0);
                _userStatsDao.update(userStats.getId(), userStats);
                s_logger.debug("Successfully updated user statistics as a part of domR " + router + " reboot/stop");
            } else {
                s_logger.warn("User stats were not created for account " + router.getAccountId() + " and dc " + router.getDataCenterId());
            }
            txn.commit();
        } catch (final Exception e) {
            txn.rollback();
            throw new CloudRuntimeException("Problem getting stats after reboot/stop ", e);
        }
    }

    @Override
    public boolean getRouterStatistics(final long vmId, final Map<String, long[]> netStats, final Map<String, long[]> diskStats) {
        final DomainRouterVO router = _routerDao.findById(vmId);

        if (router == null || router.getState() != State.Running || router.getHostId() == null) {
            return true;
        }

        /*
         * final GetVmStatsCommand cmd = new GetVmStatsCommand(router, router.getInstanceName()); final Answer answer =
         * _agentMgr.easySend(router.getHostId(), cmd); if (answer == null) { return false; }
         * 
         * final GetVmStatsAnswer stats = (GetVmStatsAnswer)answer;
         * 
         * netStats.putAll(stats.getNetworkStats()); diskStats.putAll(stats.getDiskStats());
         */

        return true;
    }

    @Override
    public VirtualRouter rebootRouter(long routerId, boolean restartNetwork) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        Account caller = UserContext.current().getCaller();

        // verify parameters
        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            throw new InvalidParameterValueException("Unable to find domain router with id " + routerId + ".");
        }

        if ((caller != null) && !_domainDao.isChildDomain(caller.getDomainId(), router.getDomainId())) {
            throw new PermissionDeniedException("Unable to reboot domain router with id " + routerId + ". Permission denied");
        }

        // Can reboot domain router only in Running state
        if (router == null || router.getState() != State.Running) {
            s_logger.warn("Unable to reboot, virtual router is not in the right state " + router.getState());
            throw new ResourceUnavailableException("Unable to reboot domR, it is not in right state " + router.getState(), DataCenter.class, router.getDataCenterId());
        }

        UserVO user = _userDao.findById(UserContext.current().getCallerUserId());
        s_logger.debug("Stopping and starting router " + router + " as a part of router reboot");

        if (stop(router, false, user, caller) != null) {
            return startRouter(routerId, restartNetwork);
        } else {
            throw new CloudRuntimeException("Failed to reboot router " + router);
        }
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;

        _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("RouterMonitor"));

        final ComponentLocator locator = ComponentLocator.getCurrentLocator();

        final Map<String, String> configs = _configDao.getConfiguration("AgentManager", params);

        _mgmt_host = configs.get("host");
        _routerRamSize = NumbersUtil.parseInt(configs.get("router.ram.size"), DEFAULT_ROUTER_VM_RAMSIZE);
        _routerCpuMHz = NumbersUtil.parseInt(configs.get("router.cpu.mhz"), DEFAULT_ROUTER_CPU_MHZ);
        String value = configs.get("start.retry");
        _retry = NumbersUtil.parseInt(value, 2);

        value = configs.get("router.stats.interval");
        _routerStatsInterval = NumbersUtil.parseInt(value, 300);

        _instance = configs.get("instance.name");
        if (_instance == null) {
            _instance = "DEFAULT";
        }

        s_logger.info("Router configurations: " + "ramsize=" + _routerRamSize);

        final UserStatisticsDao statsDao = locator.getDao(UserStatisticsDao.class);
        if (statsDao == null) {
            throw new ConfigurationException("Unable to get " + UserStatisticsDao.class.getName());
        }

        _agentMgr.registerForHostEvents(new SshKeysDistriMonitor(this, _hostDao, _configDao), true, false, false);
        _itMgr.registerGuru(VirtualMachine.Type.DomainRouter, this);

        boolean useLocalStorage = Boolean.parseBoolean(configs.get(Config.SystemVMUseLocalStorage.key()));
        _offering = new ServiceOfferingVO("System Offering For Software Router", 1, _routerRamSize, _routerCpuMHz, null, null, true, null, useLocalStorage, true, null, true);
        _offering.setUniqueName("Cloud.Com-SoftwareRouter");
        _offering = _serviceOfferingDao.persistSystemServiceOffering(_offering);

        _systemAcct = _accountService.getSystemAccount();
        
        trafficSentinelHostname = configs.get("traffic.sentinel.hostname");

        s_logger.info("DomainRouterManager is configured.");

        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        _executor.scheduleAtFixedRate(new NetworkUsageTask(), _routerStatsInterval, _routerStatsInterval, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    protected VirtualNetworkApplianceManagerImpl() {
    }

    @Override
    public Long convertToId(final String vmName) {
        if (!VirtualMachineName.isValidRouterName(vmName, _instance)) {
            return null;
        }

        return VirtualMachineName.getRouterId(vmName);
    }

    private VmDataCommand generateVmDataCommand(DomainRouterVO router, String vmPrivateIpAddress, String userData, String serviceOffering, String zoneName, String guestIpAddress, String vmName,
            String vmInstanceName, long vmId, String publicKey) {
        VmDataCommand cmd = new VmDataCommand(vmPrivateIpAddress);

        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, router.getPrivateIpAddress());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());

        cmd.addVmData("userdata", "user-data", userData);
        cmd.addVmData("metadata", "service-offering", serviceOffering);
        cmd.addVmData("metadata", "availability-zone", zoneName);
        cmd.addVmData("metadata", "local-ipv4", guestIpAddress);
        cmd.addVmData("metadata", "local-hostname", vmName);
        cmd.addVmData("metadata", "public-ipv4", router.getPublicIpAddress());
        cmd.addVmData("metadata", "public-hostname", router.getPublicIpAddress());
        cmd.addVmData("metadata", "instance-id", vmInstanceName);
        cmd.addVmData("metadata", "vm-id", String.valueOf(vmId));
        cmd.addVmData("metadata", "public-keys", publicKey);

        String cloudIdentifier = _configDao.getValue("cloud.identifier");
        if (cloudIdentifier == null) {
            cloudIdentifier = "";
        } else {
            cloudIdentifier = "CloudStack-{" + cloudIdentifier + "}";
        }
        cmd.addVmData("metadata", "cloud-identifier", cloudIdentifier);

        return cmd;
    }

    protected class NetworkUsageTask implements Runnable {

        public NetworkUsageTask() {
        }

        @Override
        public void run() {
            //Direct Network Usage
            URL trafficSentinel;
            try {
                //Query traffic Sentinel
                if(trafficSentinelHostname != null){
                    trafficSentinel = new URL(trafficSentinelHostname+"/inmsf/Query?script=var+q+%3D+Query.topN(%22historytrmx%22,%0D%0A+++++++++++++++++%22ipsource,bytes%22,%0D%0A+++++++++++++++++%22sourcezone+!%3D+EXTERNAL" +
                            "+%26+destinationzone+%3D+EXTERNAL%22,%0D%0A+++++++++++++++++%22end+-+5+minutes,+end%22,%0D%0A+++++++++++++++++%22bytes%22,%0D%0A+++++++++++++++++100000);%0D%0A%0D%0Avar+totalsSent+%3D+" +
                            "{};%0D%0A%0D%0Avar+t+%3D+q.run(%0D%0A++function(row,table)+{%0D%0A++++if(row[0])+{++++%0D%0A++++++totalsSent[row[0]]+%3D+row[1];%0D%0A++++}%0D%0A++});%0D%0A%0D%0Avar+totalsRcvd+%3D+{};" +
                            "%0D%0A%0D%0Avar+q+%3D+Query.topN(%22historytrmx%22,%0D%0A+++++++++++++++++%22ipdestination,bytes%22,%0D%0A+++++++++++++++++%22destinationzone+!%3D+EXTERNAL+%26+sourcezone+%3D+EXTERNAL%22," +
                            "%0D%0A+++++++++++++++++%22end+-+5minutes,+end%22,%0D%0A+++++++++++++++++%22bytes%22,%0D%0A+++++++++++++++++100000);%0D%0A%0D%0Avar+t+%3D+q.run(%0D%0A++function(row,table)+{%0D%0A++++" +
                            "if(row[0])+{%0D%0A++++++totalsRcvd[row[0]]+%3D+row[1];%0D%0A++++}%0D%0A++});%0D%0A%0D%0Afor+(var+addr+in+totalsSent)+{%0D%0A++++var+TS+%3D+0;%0D%0A++++var+TR+%3D+0;%0D%0A++++if(totalsSent[addr])" +
                    "+TS+%3D+totalsSent[addr];%0D%0A++++if(totalsRcvd[addr])+TR+%3D+totalsRcvd[addr];%0D%0A++++println(addr+%2B+%22,%22+%2B+TS+%2B+%22,%22+%2B+TR);%0D%0A}&authenticate=basic&resultFormat=txt");

                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(trafficSentinel.openStream()));

                    String inputLine;

                    while ((inputLine = in.readLine()) != null){
                        //Parse the script output
                        StringTokenizer st = new StringTokenizer(inputLine, ",");
                        if(st.countTokens() == 3){
                            String publicIp = st.nextToken();
                            //Find the account owning the IP
                            IPAddressVO ipaddress = _ipAddressDao.findByIpAddress(publicIp);
                            if(ipaddress == null){
                                continue;
                            }
                            Long bytesSent = new Long(st.nextToken());
                            Long bytesRcvd = new Long(st.nextToken());
                            if(bytesSent == null || bytesRcvd == null){
                                s_logger.debug("Incorrect bytes for IP: "+publicIp);
                            }
                            Transaction txn = Transaction.open(Transaction.CLOUD_DB);
                            txn.start();
                            //update user_statistics
                            UserStatisticsVO stats = _statsDao.lock(ipaddress.getAccountId(), ipaddress.getDataCenterId(), null, 0L, "DirectNetwork");
                            if (stats == null) {
                                stats = new UserStatisticsVO(ipaddress.getAccountId(), ipaddress.getDataCenterId(), null, 0L, "DirectNetwork", null);
                                stats.setCurrentBytesSent(bytesSent);
                                stats.setCurrentBytesReceived(bytesRcvd);
                                _statsDao.persist(stats);
                            } else {
                                stats.setCurrentBytesSent(stats.getCurrentBytesSent() + bytesSent);
                                stats.setCurrentBytesReceived(stats.getCurrentBytesReceived() + bytesRcvd);
                                _statsDao.update(stats.getId(), stats);
                            }
                            txn.commit();
                            txn.close();
                        }
                    }

                    in.close();
                }
            } catch (MalformedURLException e1) {
                s_logger.info("Invalid T raffic Sentinel URL",e1);
            } catch (IOException e) {
                s_logger.debug("Error in direct network usage accounting",e);
            } 
            
            
            final List<DomainRouterVO> routers = _routerDao.listUpByHostId(null);
            s_logger.debug("Found " + routers.size() + " running routers. ");

            for (DomainRouterVO router : routers) {
                String privateIP = router.getPrivateIpAddress();
                if (privateIP != null) {
                    final NetworkUsageCommand usageCmd = new NetworkUsageCommand(privateIP, router.getName());
                    final NetworkUsageAnswer answer = (NetworkUsageAnswer) _agentMgr.easySend(router.getHostId(), usageCmd);
                    if (answer != null) {
                        Transaction txn = Transaction.open(Transaction.CLOUD_DB);
                        try {
                            if ((answer.getBytesReceived() == 0) && (answer.getBytesSent() == 0)) {
                                s_logger.debug("Recieved and Sent bytes are both 0. Not updating user_statistics");
                                continue;
                            }
                            txn.start();
                            UserStatisticsVO stats = _statsDao.lock(router.getAccountId(), router.getDataCenterId(), null, router.getId(), router.getType().toString());
                            if (stats == null) {
                                s_logger.warn("unable to find stats for account: " + router.getAccountId());
                                continue;
                            }
                            if (stats.getCurrentBytesReceived() > answer.getBytesReceived()) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Received # of bytes that's less than the last one.  Assuming something went wrong and persisting it.  Reported: " + answer.getBytesReceived()
                                            + " Stored: " + stats.getCurrentBytesReceived());
                                }
                                stats.setNetBytesReceived(stats.getNetBytesReceived() + stats.getCurrentBytesReceived());
                            }
                            stats.setCurrentBytesReceived(answer.getBytesReceived());
                            if (stats.getCurrentBytesSent() > answer.getBytesSent()) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Received # of bytes that's less than the last one.  Assuming something went wrong and persisting it.  Reported: " + answer.getBytesSent()
                                            + " Stored: " + stats.getCurrentBytesSent());
                                }
                                stats.setNetBytesSent(stats.getNetBytesSent() + stats.getCurrentBytesSent());
                            }
                            stats.setCurrentBytesSent(answer.getBytesSent());
                            _statsDao.update(stats.getId(), stats);
                            txn.commit();
                        } catch (Exception e) {
                            txn.rollback();
                            s_logger.warn("Unable to update user statistics for account: " + router.getAccountId() + " Rx: " + answer.getBytesReceived() + "; Tx: " + answer.getBytesSent());
                        } finally {
                            txn.close();
                        }
                    }
                }
            }
        }
    }

    public static boolean isAdmin(short accountType) {
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN) || (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) || (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN) || (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN));
    }

    @Override
    @DB
    public DomainRouterVO deployVirtualRouter(Network guestNetwork, DeployDestination dest, Account owner, Map<Param, Object> params) throws InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException {
        long dcId = dest.getDataCenter().getId();

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Starting a router for network configurations: virtual=" + guestNetwork + " in " + dest);
        }

        // lock guest network
        Long guestNetworkId = guestNetwork.getId();
        guestNetwork = _networkDao.acquireInLockTable(guestNetworkId);

        if (guestNetwork == null) {
            throw new ConcurrentOperationException("Unable to acquire network configuration: " + guestNetworkId);
        }

        try {

            assert guestNetwork.getState() == Network.State.Implemented || guestNetwork.getState() == Network.State.Setup || guestNetwork.getState() == Network.State.Implementing : "Network is not yet fully implemented: "
                    + guestNetwork;
            assert guestNetwork.getTrafficType() == TrafficType.Guest;

            DataCenterDeployment plan = new DataCenterDeployment(dcId);

            DomainRouterVO router = _routerDao.findByNetwork(guestNetwork.getId());
            if (router == null) {
                long id = _routerDao.getNextInSequence(Long.class, "id");
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Creating the router " + id);
                }

                PublicIp sourceNatIp = _networkMgr.assignSourceNatIpAddress(owner, guestNetwork, _accountService.getSystemUser().getId());

                List<NetworkOfferingVO> offerings = _networkMgr.getSystemAccountNetworkOfferings(NetworkOfferingVO.SystemControlNetwork);
                NetworkOfferingVO controlOffering = offerings.get(0);
                NetworkVO controlConfig = _networkMgr.setupNetwork(_systemAcct, controlOffering, plan, null, null, false, false).get(0);

                List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>(3);
                NetworkOfferingVO publicOffering = _networkMgr.getSystemAccountNetworkOfferings(NetworkOfferingVO.SystemPublicNetwork).get(0);
                List<NetworkVO> publicNetworks = _networkMgr.setupNetwork(_systemAcct, publicOffering, plan, null, null, false, false);
                NicProfile defaultNic = new NicProfile();
                defaultNic.setDefaultNic(true);
                defaultNic.setIp4Address(sourceNatIp.getAddress().addr());
                defaultNic.setGateway(sourceNatIp.getGateway());
                defaultNic.setNetmask(sourceNatIp.getNetmask());
                defaultNic.setMacAddress(sourceNatIp.getMacAddress());
                defaultNic.setBroadcastType(BroadcastDomainType.Vlan);
                defaultNic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(sourceNatIp.getVlanTag()));
                defaultNic.setIsolationUri(IsolationType.Vlan.toUri(sourceNatIp.getVlanTag()));
                defaultNic.setDeviceId(2);
                networks.add(new Pair<NetworkVO, NicProfile>(publicNetworks.get(0), defaultNic));
                NicProfile gatewayNic = new NicProfile();
                gatewayNic.setIp4Address(guestNetwork.getGateway());
                gatewayNic.setBroadcastUri(guestNetwork.getBroadcastUri());
                gatewayNic.setBroadcastType(guestNetwork.getBroadcastDomainType());
                gatewayNic.setIsolationUri(guestNetwork.getBroadcastUri());
                gatewayNic.setMode(guestNetwork.getMode());

                String gatewayCidr = guestNetwork.getCidr();
                gatewayNic.setNetmask(NetUtils.getCidrNetmask(gatewayCidr));
                networks.add(new Pair<NetworkVO, NicProfile>((NetworkVO) guestNetwork, gatewayNic));
                networks.add(new Pair<NetworkVO, NicProfile>(controlConfig, null));

                /* Before starting router, already know the hypervisor type */
                VMTemplateVO template = _templateDao.findRoutingTemplate(dest.getCluster().getHypervisorType());
                router = new DomainRouterVO(id, _offering.getId(), VirtualMachineName.getRouterName(id, _instance), template.getId(), template.getHypervisorType(), template.getGuestOSId(),
                        owner.getDomainId(), owner.getId(), guestNetwork.getId(), _offering.getOfferHA());
                router = _itMgr.allocate(router, template, _offering, networks, plan, null, owner);
            }

            State state = router.getState();
            if (state != State.Running) {
                router = this.start(router, _accountService.getSystemUser(), _accountService.getSystemAccount(), params);
            }

            // Creating stats entry for router
            UserStatisticsVO stats = _userStatsDao.findBy(owner.getId(), dcId, null, router.getId(), router.getType().toString());
            if (stats == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Creating user statistics for the account: " + owner.getId() + " Router Id: " + router.getId());
                }
                stats = new UserStatisticsVO(owner.getId(), dcId, null, router.getId(), router.getType().toString(), guestNetwork.getId());
                _userStatsDao.persist(stats);
            }
            return router;
        } finally {
            _networkDao.releaseFromLockTable(guestNetworkId);
        }
    }

    @Override
    @DB
    public DomainRouterVO deployDhcp(Network guestNetwork, DeployDestination dest, Account owner, Map<Param, Object> params) throws InsufficientCapacityException, StorageUnavailableException,
            ConcurrentOperationException, ResourceUnavailableException {
        long dcId = dest.getDataCenter().getId();

        // lock guest network
        Long guestNetworkId = guestNetwork.getId();
        guestNetwork = _networkDao.acquireInLockTable(guestNetworkId);

        if (guestNetwork == null) {
            throw new ConcurrentOperationException("Unable to acquire network configuration: " + guestNetworkId);
        }

        try {

            NetworkOffering offering = _networkOfferingDao.findByIdIncludingRemoved(guestNetwork.getNetworkOfferingId());
            if (offering.isSystemOnly() || guestNetwork.getIsShared()) {
                owner = _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM);
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Starting a dhcp for network configurations: dhcp=" + guestNetwork + " in " + dest);
            }
            assert guestNetwork.getState() == Network.State.Implemented || guestNetwork.getState() == Network.State.Setup || guestNetwork.getState() == Network.State.Implementing : "Network is not yet fully implemented: "
                    + guestNetwork;

            DataCenterDeployment plan = null;
            DataCenter dc = _dcDao.findById(dcId);
            DomainRouterVO router = null;
            Long podId = dest.getPod().getId();

            // In Basic zone and Guest network we have to start domR per pod, not per network
            if ((dc.getNetworkType() == NetworkType.Basic || guestNetwork.isSecurityGroupEnabled()) && guestNetwork.getTrafficType() == TrafficType.Guest) {
                router = _routerDao.findByNetworkAndPod(guestNetwork.getId(), podId);
                plan = new DataCenterDeployment(dcId, podId, null, null, null);
            } else {
                router = _routerDao.findByNetwork(guestNetwork.getId());
                plan = new DataCenterDeployment(dcId);
            }

            if (router == null) {
                long id = _routerDao.getNextInSequence(Long.class, "id");
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Creating the router " + id);
                }

                List<NetworkOfferingVO> offerings = _networkMgr.getSystemAccountNetworkOfferings(NetworkOfferingVO.SystemControlNetwork);
                NetworkOfferingVO controlOffering = offerings.get(0);
                NetworkVO controlConfig = _networkMgr.setupNetwork(_systemAcct, controlOffering, plan, null, null, false, false).get(0);

                List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>(3);
                NicProfile gatewayNic = new NicProfile();
                gatewayNic.setDefaultNic(true);
                networks.add(new Pair<NetworkVO, NicProfile>((NetworkVO) guestNetwork, gatewayNic));
                networks.add(new Pair<NetworkVO, NicProfile>(controlConfig, null));

                /* Before starting router, already know the hypervisor type */
                VMTemplateVO template = _templateDao.findRoutingTemplate(dest.getCluster().getHypervisorType());

                router = new DomainRouterVO(id, _offering.getId(), VirtualMachineName.getRouterName(id, _instance), template.getId(), template.getHypervisorType(), template.getGuestOSId(),
                        owner.getDomainId(), owner.getId(), guestNetwork.getId(), _offering.getOfferHA());
                router.setRole(Role.DHCP_USERDATA);
                router = _itMgr.allocate(router, template, _offering, networks, plan, null, owner);
            }

            State state = router.getState();
            if (state != State.Running) {
                router = this.start(router, _accountService.getSystemUser(), _accountService.getSystemAccount(), params);
            }
            // Creating stats entry for router
            UserStatisticsVO stats = _userStatsDao.findBy(owner.getId(), dcId, null, router.getId(), router.getType().toString());
            if (stats == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Creating user statistics for the account: " + owner.getId() + " Router Id: " + router.getId());
                }
                stats = new UserStatisticsVO(owner.getId(), dcId, null, router.getId(), router.getType().toString(), guestNetwork.getId());
                _userStatsDao.persist(stats);
            }

            return router;
        } finally {
            _networkDao.releaseFromLockTable(guestNetworkId);
        }
    }

    @Override
    public boolean finalizeVirtualMachineProfile(VirtualMachineProfile<DomainRouterVO> profile, DeployDestination dest, ReservationContext context) {

        DomainRouterVO router = profile.getVirtualMachine();
        NetworkVO network = _networkDao.findById(router.getNetworkId());

        String type = null;
        String dhcpRange = null;

        DataCenter dc = dest.getDataCenter();

        if (dc.getNetworkType() == NetworkType.Advanced) {
            String cidr = network.getCidr();
            if (cidr != null) {
                dhcpRange = NetUtils.getDhcpRange(cidr);
            }
        }

        if (router.getRole() == Role.DHCP_USERDATA) {
            type = "dhcpsrvr";
        } else {
            type = "router";
        }

        StringBuilder buf = profile.getBootArgsBuilder();
        buf.append(" template=domP type=" + type);
        buf.append(" name=").append(profile.getHostName());
        NicProfile controlNic = null;
        String defaultDns1 = null;
        String defaultDns2 = null;

        for (NicProfile nic : profile.getNics()) {
            int deviceId = nic.getDeviceId();
            buf.append(" eth").append(deviceId).append("ip=").append(nic.getIp4Address());
            buf.append(" eth").append(deviceId).append("mask=").append(nic.getNetmask());
            if (nic.isDefaultNic()) {
                buf.append(" gateway=").append(nic.getGateway());
                defaultDns1 = nic.getDns1();
                defaultDns2 = nic.getDns2();

                if (dc.getNetworkType() == NetworkType.Basic) {
                    long cidrSize = NetUtils.getCidrSize(nic.getNetmask());
                    String cidr = NetUtils.getCidrSubNet(nic.getGateway(), cidrSize);
                    if (cidr != null) {
                        dhcpRange = NetUtils.getIpRangeStartIpFromCidr(cidr, cidrSize);
                    }
                }
            }
            if (nic.getTrafficType() == TrafficType.Management) {
                buf.append(" localgw=").append(dest.getPod().getGateway());
            } else if (nic.getTrafficType() == TrafficType.Control) {

                // DOMR control command is sent over management server in VMware
                if (dest.getHost().getHypervisorType() == HypervisorType.VMware) {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Check if we need to add management server explicit route to DomR. pod cidr: " + dest.getPod().getCidrAddress() + "/" + dest.getPod().getCidrSize()
                                + ", pod gateway: " + dest.getPod().getGateway() + ", management host: " + _mgmt_host);
                    }

                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Add management server explicit route to DomR.");
                    }

                    // always add management explicit route, for basic networking setup, DomR may have two interfaces while both
                    // are on the same subnet
                    _mgmt_cidr = _configDao.getValue(Config.ManagementNetwork.key());
                    if (NetUtils.isValidCIDR(_mgmt_cidr)) {
                        buf.append(" mgmtcidr=").append(_mgmt_cidr);
                        buf.append(" localgw=").append(dest.getPod().getGateway());
                    }

                    /*
                     * if(!NetUtils.sameSubnetCIDR(_mgmt_host, dest.getPod().getGateway(), dest.getPod().getCidrSize())) {
                     * if(s_logger.isInfoEnabled()) { s_logger.info("Add management server explicit route to DomR."); }
                     * 
                     * _mgmt_cidr = _configDao.getValue(Config.ManagementNetwork.key()); if (NetUtils.isValidCIDR(_mgmt_cidr)) {
                     * buf.append(" mgmtcidr=").append(_mgmt_cidr); buf.append(" localgw=").append(dest.getPod().getGateway());
                     * } } else { if(s_logger.isInfoEnabled()) {
                     * s_logger.info("Management server host is at same subnet at pod private network"); } }
                     */
                }

                controlNic = nic;
            }
        }

        if (dhcpRange != null) {
            buf.append(" dhcprange=" + dhcpRange);
        }
        String domain = network.getNetworkDomain();
        if (domain != null) {
            buf.append(" domain=" + domain);
        }

        if (!network.isDefault() && network.getGuestType() == GuestIpType.Direct) {
            buf.append(" defaultroute=false");

            String virtualNetworkElementNicIP = _networkMgr.getIpOfNetworkElementInVirtualNetwork(network.getAccountId(), network.getDataCenterId());
            if (!network.getIsShared() && virtualNetworkElementNicIP != null) {
                defaultDns1 = virtualNetworkElementNicIP;
            } else {
                s_logger.debug("No Virtual network found for account id=" + network.getAccountId() + " so setting dns to the dns of the network id=" + network.getId());
            }
        } else {
            buf.append(" defaultroute=true");
        }

        buf.append(" dns1=").append(defaultDns1);
        if (defaultDns2 != null) {
            buf.append(" dns2=").append(defaultDns2);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Boot Args for " + profile + ": " + buf.toString());
        }

        if (controlNic == null) {
            throw new CloudRuntimeException("Didn't start a control port");
        }

        return true;
    }

    @Override
    public boolean finalizeDeployment(Commands cmds, VirtualMachineProfile<DomainRouterVO> profile, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException {
        DomainRouterVO router = profile.getVirtualMachine();

        List<NicProfile> nics = profile.getNics();
        for (NicProfile nic : nics) {
            if (nic.getTrafficType() == TrafficType.Public) {
                router.setPublicIpAddress(nic.getIp4Address());
                router.setPublicNetmask(nic.getNetmask());
                router.setPublicMacAddress(nic.getMacAddress());
            } else if (nic.getTrafficType() == TrafficType.Guest) {
                router.setGuestIpAddress(nic.getIp4Address());
            } else if (nic.getTrafficType() == TrafficType.Control) {
                router.setPrivateIpAddress(nic.getIp4Address());
                router.setPrivateMacAddress(nic.getMacAddress());
            }
        }
        _routerDao.update(router.getId(), router);

        finalizeCommandsOnStart(cmds, profile);
        return true;
    }

    @Override
    public boolean finalizeCommandsOnStart(Commands cmds, VirtualMachineProfile<DomainRouterVO> profile) {
        DomainRouterVO router = profile.getVirtualMachine();

        NicProfile controlNic = null;
        for (NicProfile nic : profile.getNics()) {
            if (nic.getTrafficType() == TrafficType.Control && nic.getIp4Address() != null) {
                controlNic = nic;
            }
        }

        if (controlNic == null) {
            s_logger.error("Control network doesn't exist for the router " + router);
            return false;
        }

        cmds.addCommand("checkSsh", new CheckSshCommand(profile.getInstanceName(), controlNic.getIp4Address(), 3922, 5, 20));

        // restart network if restartNetwork = false is not specified in profile parameters
        boolean restartNetwork = true;
        if (profile.getParameter(Param.RestartNetwork) != null && (Boolean) profile.getParameter(Param.RestartNetwork) == false) {
            restartNetwork = false;
        }
        // The commands should be sent for domR only, skip for DHCP
        if (router.getRole() == VirtualRouter.Role.DHCP_FIREWALL_LB_PASSWD_USERDATA && restartNetwork) {
            s_logger.debug("Resending ipAssoc, port forwarding, load balancing rules as a part of Virtual router start");
            long networkId = router.getNetworkId();
            long ownerId = router.getAccountId();
            long zoneId = router.getDataCenterId();

            final List<IPAddressVO> userIps = _networkMgr.listPublicIpAddressesInVirtualNetwork(ownerId, zoneId, null, null);
            List<PublicIpAddress> publicIps = new ArrayList<PublicIpAddress>();
            if (userIps != null && !userIps.isEmpty()) {
                for (IPAddressVO userIp : userIps) {
                    PublicIp publicIp = new PublicIp(userIp, _vlanDao.findById(userIp.getVlanId()), NetUtils.createSequenceBasedMacAddress(userIp.getMacAddress()));
                    publicIps.add(publicIp);
                }
            }

            s_logger.debug("Found " + publicIps.size() + " ip(s) to apply as a part of domR " + router + " start.");

            if (!publicIps.isEmpty()) {

                // Re-apply public ip addresses - should come before PF/LB/VPN
                createAssociateIPCommands(router, publicIps, cmds, 0);

                List<RemoteAccessVpn> vpns = new ArrayList<RemoteAccessVpn>();
                List<PortForwardingRule> pfRules = new ArrayList<PortForwardingRule>();
                List<FirewallRule> staticNatFirewallRules = new ArrayList<FirewallRule>();

                for (PublicIpAddress ip : publicIps) {
                    pfRules.addAll(_pfRulesDao.listForApplication(ip.getId()));
                    staticNatFirewallRules.addAll(_rulesDao.listByIpAndPurpose(ip.getId(), Purpose.StaticNat));

                    RemoteAccessVpn vpn = _vpnDao.findById(ip.getId());
                    if (vpn != null) {
                        vpns.add(vpn);
                    }
                }

                // Re-apply port forwarding rules
                s_logger.debug("Found " + pfRules.size() + " port forwarding rule(s) to apply as a part of domR " + router + " start.");
                if (!pfRules.isEmpty()) {
                    createApplyPortForwardingRulesCommands(pfRules, router, cmds);
                }

                // Re-apply static nat rules
                s_logger.debug("Found " + staticNatFirewallRules.size() + " static nat rule(s) to apply as a part of domR " + router + " start.");
                if (!staticNatFirewallRules.isEmpty()) {
                    List<StaticNatRule> staticNatRules = new ArrayList<StaticNatRule>();
                    for (FirewallRule rule : staticNatFirewallRules) {
                        staticNatRules.add(_rulesMgr.buildStaticNatRule(rule));
                    }
                    createApplyStaticNatRulesCommands(staticNatRules, router, cmds);
                }

                // Re-apply vpn rules
                s_logger.debug("Found " + vpns.size() + " vpn(s) to apply as a part of domR " + router + " start.");
                if (!vpns.isEmpty()) {
                    for (RemoteAccessVpn vpn : vpns) {
                        createApplyVpnCommands(vpn, router, cmds);
                    }
                }

                // Re-apply load balancing rules
                List<LoadBalancerVO> lbs = _loadBalancerDao.listByNetworkId(networkId);
                List<LoadBalancingRule> lbRules = new ArrayList<LoadBalancingRule>();
                for (LoadBalancerVO lb : lbs) {
                    List<LbDestination> dstList = _lbMgr.getExistingDestinations(lb.getId());
                    LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList);
                    lbRules.add(loadBalancing);
                }

                s_logger.debug("Found " + lbRules.size() + " load balancing rule(s) to apply as a part of domR " + router + " start.");
                if (!lbRules.isEmpty()) {
                    createApplyLoadBalancingRulesCommands(lbRules, router, cmds);
                }
            }
        }

        // Resend dhcp
        s_logger.debug("Reapplying dhcp entries as a part of domR " + router + " start...");
        createDhcpEntriesCommands(router, cmds);

        // Resend user data
        s_logger.debug("Reapplying vm data (userData and metaData) entries as a part of domR " + router + " start...");
        createVmDataCommands(router, cmds);
        // Network usage command to create iptables rules
        cmds.addCommand("networkUsage", new NetworkUsageCommand(controlNic.getIp4Address(), router.getName(), "create"));

        return true;
    }

    @Override
    public boolean finalizeStart(VirtualMachineProfile<DomainRouterVO> profile, long hostId, Commands cmds, ReservationContext context) {
        CheckSshAnswer answer = (CheckSshAnswer) cmds.getAnswer("checkSsh");
        if (answer == null || !answer.getResult()) {
            s_logger.warn("Unable to ssh to the VM: " + answer.getDetails());
            return false;
        }

        return true;
    }

    @Override
    public void finalizeStop(VirtualMachineProfile<DomainRouterVO> profile, StopAnswer answer) {
        if (answer != null) {
            VMInstanceVO vm = profile.getVirtualMachine();
            DomainRouterVO domR = _routerDao.findById(vm.getId());
            processStopOrRebootAnswer(domR, answer);
        }
    }

    @Override
    public void finalizeExpunge(DomainRouterVO vm) {
    }

    @Override
    public boolean startRemoteAccessVpn(Network network, RemoteAccessVpn vpn) throws ResourceUnavailableException {

        DomainRouterVO router = _routerDao.findByNetwork(network.getId());
        if (router == null) {
            s_logger.warn("Failed to start remote access VPN: no router found for account and zone");
            throw new ResourceUnavailableException("Failed to start remote access VPN: no router found for account and zone", DataCenter.class, network.getDataCenterId());
        }
        if (router.getState() != State.Running) {
            s_logger.warn("Failed to start remote access VPN: router not in right state " + router.getState());
            throw new ResourceUnavailableException("Failed to start remote access VPN: router not in right state " + router.getState(), DataCenter.class, network.getDataCenterId());
        }

        Commands cmds = new Commands(OnError.Stop);

        createApplyVpnCommands(vpn, router, cmds);

        try {
            _agentMgr.send(router.getHostId(), cmds);
        } catch (OperationTimedoutException e) {
            s_logger.debug("Failed to start remote access VPN: ", e);
            throw new AgentUnavailableException("Unable to send commands to virtual router ", router.getHostId(), e);
        }
        Answer answer = cmds.getAnswer("users");
        if (!answer.getResult()) {
            s_logger.error("Unable to start vpn: unable add users to vpn in zone " + router.getDataCenterId() + " for account " + vpn.getAccountId() + " on domR: " + router.getInstanceName()
                    + " due to " + answer.getDetails());
            throw new ResourceUnavailableException("Unable to start vpn: Unable to add users to vpn in zone " + router.getDataCenterId() + " for account " + vpn.getAccountId() + " on domR: "
                    + router.getInstanceName() + " due to " + answer.getDetails(), DataCenter.class, router.getDataCenterId());
        }
        answer = cmds.getAnswer("startVpn");
        if (!answer.getResult()) {
            s_logger.error("Unable to start vpn in zone " + router.getDataCenterId() + " for account " + vpn.getAccountId() + " on domR: " + router.getInstanceName() + " due to "
                    + answer.getDetails());
            throw new ResourceUnavailableException("Unable to start vpn in zone " + router.getDataCenterId() + " for account " + vpn.getAccountId() + " on domR: " + router.getInstanceName()
                    + " due to " + answer.getDetails(), DataCenter.class, router.getDataCenterId());
        }
        return true;
    }

    @Override
    public boolean deleteRemoteAccessVpn(Network network, RemoteAccessVpn vpn) throws ResourceUnavailableException {

        DomainRouterVO router = getRouter(vpn.getAccountId(), network.getDataCenterId());
        if (router == null) {
            s_logger.warn("Failed to delete remote access VPN: no router found for account and zone");
            throw new ResourceUnavailableException("Failed to delete remote access VPN", DataCenter.class, network.getDataCenterId());
        }
        if (router.getState() != State.Running) {
            s_logger.warn("Failed to delete remote access VPN: domR is not in right state " + router.getState());
            throw new ResourceUnavailableException("Failed to delete remote access VPN: domR is not in right state " + router.getState(), DataCenter.class, network.getDataCenterId());
        }
        Commands cmds = new Commands(OnError.Continue);
        IpAddress ip = _networkMgr.getIp(vpn.getServerAddressId());

        RemoteAccessVpnCfgCommand removeVpnCmd = new RemoteAccessVpnCfgCommand(false, ip.getAddress().addr(), vpn.getLocalIp(), vpn.getIpRange(), vpn.getIpsecPresharedKey());
        removeVpnCmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, router.getPrivateIpAddress());
        removeVpnCmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        cmds.addCommand(removeVpnCmd);

        return sendCommandsToRouter(router, cmds);
    }

    private DomainRouterVO start(DomainRouterVO router, User user, Account caller, Map<Param, Object> params) throws StorageUnavailableException, InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException {
        s_logger.debug("Starting router " + router);
        if (_itMgr.start(router, params, user, caller) != null) {
            return _routerDao.findById(router.getId());
        } else {
            return null;
        }
    }

    @Override
    public DomainRouterVO stop(VirtualRouter router, boolean forced, User user, Account caller) throws ConcurrentOperationException, ResourceUnavailableException {
        s_logger.debug("Stopping router " + router);
        try {
            if (_itMgr.advanceStop((DomainRouterVO) router, forced, user, caller)) {
                return _routerDao.findById(router.getId());
            } else {
                return null;
            }
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Unable to stop " + router, e);
        }
    }

    @Override
    public VirtualRouter addVirtualMachineIntoNetwork(Network network, NicProfile nic, VirtualMachineProfile<UserVm> profile, DeployDestination dest, ReservationContext context, Boolean startDhcp)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {

        DomainRouterVO router = startDhcp ? deployDhcp(network, dest, profile.getOwner(), profile.getParameters()) : deployVirtualRouter(network, dest, profile.getOwner(), profile.getParameters());

        _userVmDao.loadDetails((UserVmVO) profile.getVirtualMachine());

        String password = (String) profile.getParameter(VirtualMachineProfile.Param.VmPassword);
        String userData = profile.getVirtualMachine().getUserData();
        String sshPublicKey = profile.getVirtualMachine().getDetail("SSH.PublicKey");
        Commands cmds = new Commands(OnError.Stop);

        String routerControlIpAddress = null;
        List<NicVO> nics = _nicDao.listByVmId(router.getId());
        for (NicVO n : nics) {
            NetworkVO nc = _networkDao.findById(n.getNetworkId());
            if (nc.getTrafficType() == TrafficType.Control) {
                routerControlIpAddress = n.getIp4Address();
            }
        }

        DhcpEntryCommand dhcpCommand = new DhcpEntryCommand(nic.getMacAddress(), nic.getIp4Address(), profile.getVirtualMachine().getName());
        dhcpCommand.setAccessDetail(NetworkElementCommand.ROUTER_IP, routerControlIpAddress);
        dhcpCommand.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        cmds.addCommand("dhcp", dhcpCommand);

        // password should be set only on default network element
        if (password != null && network.isDefault()) {
            final String encodedPassword = rot13(password);
            SavePasswordCommand cmd = new SavePasswordCommand(encodedPassword, nic.getIp4Address(), profile.getVirtualMachine().getName());
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, router.getPrivateIpAddress());
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
            cmds.addCommand("password", cmd);
        }

        String serviceOffering = _serviceOfferingDao.findByIdIncludingRemoved(profile.getServiceOfferingId()).getDisplayText();
        String zoneName = _dcDao.findById(network.getDataCenterId()).getName();

        cmds.addCommand(
                "vmdata",
                generateVmDataCommand(router, nic.getIp4Address(), userData, serviceOffering, zoneName, nic.getIp4Address(), profile.getVirtualMachine().getName(), profile.getVirtualMachine()
                        .getInstanceName(), profile.getId(), sshPublicKey));

        try {
            _agentMgr.send(router.getHostId(), cmds);
        } catch (OperationTimedoutException e) {
            throw new AgentUnavailableException("Unable to reach the agent ", router.getHostId(), e);
        }

        Answer answer = cmds.getAnswer("dhcp");
        if (!answer.getResult()) {
            s_logger.error("Unable to set dhcp entry for " + profile + " on domR: " + router.getName() + " due to " + answer.getDetails());
            throw new ResourceUnavailableException("Unable to set dhcp entry for " + profile + " due to " + answer.getDetails(), DataCenter.class, router.getDataCenterId());
        }

        answer = cmds.getAnswer("password");
        if (answer != null && !answer.getResult()) {
            s_logger.error("Unable to set password for " + profile + " due to " + answer.getDetails());
            throw new ResourceUnavailableException("Unable to set password due to " + answer.getDetails(), DataCenter.class, router.getDataCenterId());
        }

        answer = cmds.getAnswer("vmdata");
        if (answer != null && !answer.getResult()) {
            s_logger.error("Unable to set VM data for " + profile + " due to " + answer.getDetails());
            throw new ResourceUnavailableException("Unable to set VM data due to " + answer.getDetails(), DataCenter.class, router.getDataCenterId());
        }
        return router;
    }

    @Override
    public DomainRouterVO persist(DomainRouterVO router) {
        return _routerDao.persist(router);
    }

    @Override
    public String[] applyVpnUsers(Network network, List<? extends VpnUser> users) throws ResourceUnavailableException {
        DomainRouterVO router = _routerDao.findByNetwork(network.getId());
        if (router == null) {
            s_logger.warn("Failed to add/remove VPN users: no router found for account and zone");
            throw new ResourceUnavailableException("Unable to assign ip addresses, domR doesn't exist for network " + network.getId(), DataCenter.class, network.getDataCenterId());
        }
        if (router.getState() != State.Running) {
            s_logger.warn("Failed to add/remove VPN users: router not in running state");
            throw new ResourceUnavailableException("Unable to assign ip addresses, domR is not in right state " + router.getState(), DataCenter.class, network.getDataCenterId());
        }

        Commands cmds = new Commands(OnError.Continue);
        List<VpnUser> addUsers = new ArrayList<VpnUser>();
        List<VpnUser> removeUsers = new ArrayList<VpnUser>();
        for (VpnUser user : users) {
            if (user.getState() == VpnUser.State.Add || user.getState() == VpnUser.State.Active) {
                addUsers.add(user);
            } else if (user.getState() == VpnUser.State.Revoke) {
                removeUsers.add(user);
            }
        }

        VpnUsersCfgCommand cmd = new VpnUsersCfgCommand(addUsers, removeUsers);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, router.getPrivateIpAddress());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        cmds.addCommand(cmd);

        // Currently we receive just one answer from the agent. In the future we have to parse individual answers and set
        // results accordingly
        boolean agentResult = sendCommandsToRouter(router, cmds);
        ;
        String[] result = new String[users.size()];
        for (int i = 0; i < result.length; i++) {
            if (agentResult) {
                result[i] = null;
            } else {
                result[i] = String.valueOf(agentResult);
            }
        }

        return result;
    }

    @Override
    public DomainRouterVO findById(long id) {
        return _routerDao.findById(id);
    }

    @Override
    public DomainRouterVO findByName(String name) {
        if (!VirtualMachineName.isValidRouterName(name)) {
            return null;
        }

        return _routerDao.findById(VirtualMachineName.getRouterId(name));
    }

    @Override
    public VirtualRouter startRouter(long routerId, boolean restartNetwork) throws ResourceUnavailableException, InsufficientCapacityException, ConcurrentOperationException {
        Account account = UserContext.current().getCaller();
        User caller = _accountMgr.getActiveUser(UserContext.current().getCallerUserId());

        // verify parameters
        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            throw new InvalidParameterValueException("Unable to find router by id " + routerId + ".");
        }
        _accountMgr.checkAccess(account, router);

        Account owner = _accountMgr.getAccount(router.getAccountId());

        // Check if all networks are implemented for the domR; if not - implement them
        DataCenter dc = _dcDao.findById(router.getDataCenterId());
        HostPodVO pod = _podDao.findById(router.getPodId());
        DeployDestination dest = new DeployDestination(dc, pod, null, null);

        ReservationContext context = new ReservationContextImpl(null, null, caller, owner);

        List<NicVO> nics = _nicDao.listByVmId(routerId);

        for (NicVO nic : nics) {
            if (!_networkMgr.startNetwork(nic.getNetworkId(), dest, context)) {
                s_logger.warn("Failed to start network id=" + nic.getNetworkId() + " as a part of domR start");
                throw new CloudRuntimeException("Failed to start network id=" + nic.getNetworkId() + " as a part of domR start");
            }
        }

        UserVO user = _userDao.findById(UserContext.current().getCallerUserId());
        Map<Param, Object> params = new HashMap<Param, Object>();
        if (restartNetwork) {
            params.put(Param.RestartNetwork, true);
        } else {
            params.put(Param.RestartNetwork, false);
        }
        return this.start(router, user, account, params);
    }

    private void createAssociateIPCommands(final DomainRouterVO router, final List<? extends PublicIpAddress> ips, Commands cmds, long vmId) {

        // Ensure that in multiple vlans case we first send all ip addresses of vlan1, then all ip addresses of vlan2, etc..
        Map<String, ArrayList<PublicIpAddress>> vlanIpMap = new HashMap<String, ArrayList<PublicIpAddress>>();
        for (final PublicIpAddress ipAddress : ips) {
            String vlanTag = ipAddress.getVlanTag();
            ArrayList<PublicIpAddress> ipList = vlanIpMap.get(vlanTag);
            if (ipList == null) {
                ipList = new ArrayList<PublicIpAddress>();
            }
            ipList.add(ipAddress);
            vlanIpMap.put(vlanTag, ipList);
        }

        for (Map.Entry<String, ArrayList<PublicIpAddress>> vlanAndIp : vlanIpMap.entrySet()) {
            List<PublicIpAddress> ipAddrList = vlanAndIp.getValue();
            // Source nat ip address should always be sent first
            Collections.sort(ipAddrList, new Comparator<PublicIpAddress>() {
                @Override
                public int compare(PublicIpAddress o1, PublicIpAddress o2) {
                    boolean s1 = o1.isSourceNat();
                    boolean s2 = o2.isSourceNat();
                    return (s1 ^ s2) ? ((s1 ^ true) ? 1 : -1) : 0;
                }
            });

            IpAddressTO[] ipsToSend = new IpAddressTO[ipAddrList.size()];
            int i = 0;
            boolean firstIP = true;
            for (final PublicIpAddress ipAddr : ipAddrList) {

                boolean add = (ipAddr.getState() == IpAddress.State.Releasing ? false : true);
                boolean sourceNat = ipAddr.isSourceNat();
                String vlanId = ipAddr.getVlanTag();
                String vlanGateway = ipAddr.getGateway();
                String vlanNetmask = ipAddr.getNetmask();
                String vifMacAddress = ipAddr.getMacAddress();

                String vmGuestAddress = null;

                // Get network rate - required for IpAssoc
                Integer networkRate = _networkMgr.getNetworkRate(ipAddr.getNetworkId(), null);

                IpAddressTO ip = new IpAddressTO(ipAddr.getAddress().addr(), add, firstIP, sourceNat, vlanId, vlanGateway, vlanNetmask, vifMacAddress, vmGuestAddress, networkRate);
                ipsToSend[i++] = ip;
                firstIP = false;
            }
            IPAssocCommand cmd = new IPAssocCommand(ipsToSend);
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, router.getPrivateIpAddress());
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
            cmds.addCommand("IPAssocCommand", cmd);
        }
    }

    private void createApplyPortForwardingRulesCommands(List<? extends PortForwardingRule> rules, DomainRouterVO router, Commands cmds) {
        List<PortForwardingRuleTO> rulesTO = null;
        if (rules != null) {
            rulesTO = new ArrayList<PortForwardingRuleTO>();
            for (PortForwardingRule rule : rules) {
                IpAddress sourceIp = _networkMgr.getIp(rule.getSourceIpAddressId());
                PortForwardingRuleTO ruleTO = new PortForwardingRuleTO(rule, sourceIp.getAddress().addr());
                rulesTO.add(ruleTO);
            }
        }

        SetPortForwardingRulesCommand cmd = new SetPortForwardingRulesCommand(rulesTO);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, router.getPrivateIpAddress());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        cmds.addCommand(cmd);
    }

    private void createApplyStaticNatRulesCommands(List<? extends StaticNatRule> rules, DomainRouterVO router, Commands cmds) {
        List<StaticNatRuleTO> rulesTO = null;
        if (rules != null) {
            rulesTO = new ArrayList<StaticNatRuleTO>();
            for (StaticNatRule rule : rules) {
                IpAddress sourceIp = _networkMgr.getIp(rule.getSourceIpAddressId());
                StaticNatRuleTO ruleTO = new StaticNatRuleTO(rule, sourceIp.getAddress().addr(), rule.getDestIpAddress());
                rulesTO.add(ruleTO);
            }
        }

        SetStaticNatRulesCommand cmd = new SetStaticNatRulesCommand(rulesTO);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, router.getPrivateIpAddress());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        cmds.addCommand(cmd);
    }

    private void createApplyLoadBalancingRulesCommands(List<LoadBalancingRule> rules, DomainRouterVO router, Commands cmds) {

        LoadBalancerTO[] lbs = new LoadBalancerTO[rules.size()];
        int i = 0;
        for (LoadBalancingRule rule : rules) {
            boolean revoked = (rule.getState().equals(FirewallRule.State.Revoke));
            String protocol = rule.getProtocol();
            String algorithm = rule.getAlgorithm();

            String srcIp = _networkMgr.getIp(rule.getSourceIpAddressId()).getAddress().addr();
            int srcPort = rule.getSourcePortStart();
            List<LbDestination> destinations = rule.getDestinations();
            LoadBalancerTO lb = new LoadBalancerTO(srcIp, srcPort, protocol, algorithm, revoked, false, destinations);
            lbs[i++] = lb;
        }

        LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(lbs);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, router.getPrivateIpAddress());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        cmds.addCommand(cmd);

    }

    private void createApplyVpnCommands(RemoteAccessVpn vpn, DomainRouterVO router, Commands cmds) {
        List<VpnUserVO> vpnUsers = _vpnUsersDao.listByAccount(vpn.getAccountId());
        List<VpnUser> addUsers = new ArrayList<VpnUser>();
        List<VpnUser> removeUsers = new ArrayList<VpnUser>();
        for (VpnUser user : vpnUsers) {
            if (user.getState() == VpnUser.State.Add) {
                addUsers.add(user);
            } else if (user.getState() == VpnUser.State.Revoke) {
                removeUsers.add(user);
            }
        }

        VpnUsersCfgCommand addUsersCmd = new VpnUsersCfgCommand(addUsers, removeUsers);
        addUsersCmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, router.getPrivateIpAddress());
        addUsersCmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());

        IpAddress ip = _networkMgr.getIp(vpn.getServerAddressId());

        RemoteAccessVpnCfgCommand startVpnCmd = new RemoteAccessVpnCfgCommand(true, ip.getAddress().addr(), vpn.getLocalIp(), vpn.getIpRange(), vpn.getIpsecPresharedKey());
        startVpnCmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, router.getPrivateIpAddress());
        startVpnCmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());

        cmds.addCommand("users", addUsersCmd);
        cmds.addCommand("startVpn", startVpnCmd);
    }

    private void createVmDataCommands(DomainRouterVO router, Commands cmds) {
        long networkId = router.getNetworkId();
        List<UserVmVO> vms = _userVmDao.listByNetworkId(networkId);
        if (vms != null && !vms.isEmpty()) {
            for (UserVmVO vm : vms) {
                NicVO nic = _nicDao.findByInstanceIdAndNetworkId(networkId, vm.getId());
                if (nic != null) {
                    s_logger.debug("Creating user data entry for vm " + vm + " on domR " + router);
                    String serviceOffering = _serviceOfferingDao.findByIdIncludingRemoved(vm.getServiceOfferingId()).getDisplayText();
                    String zoneName = _dcDao.findById(router.getDataCenterId()).getName();
                    cmds.addCommand("vmdata",
                            generateVmDataCommand(router, nic.getIp4Address(), vm.getUserData(), serviceOffering, zoneName, nic.getIp4Address(), vm.getName(), vm.getInstanceName(), vm.getId(), null));
                }
            }
        }
    }

    private void createDhcpEntriesCommands(DomainRouterVO router, Commands cmds) {
        long networkId = router.getNetworkId();
        List<UserVmVO> vms = _userVmDao.listByNetworkId(networkId);
        if (!vms.isEmpty()) {
            for (UserVmVO vm : vms) {
                if (vm.getState() != State.Destroyed && vm.getState() != State.Expunging) {
                    NicVO nic = _nicDao.findByInstanceIdAndNetworkId(networkId, vm.getId());
                    if (nic != null) {
                        s_logger.debug("Creating dhcp entry for vm " + vm + " on domR " + router + ".");

                        DhcpEntryCommand dhcpCommand = new DhcpEntryCommand(nic.getMacAddress(), nic.getIp4Address(), vm.getName());
                        dhcpCommand.setAccessDetail(NetworkElementCommand.ROUTER_IP, router.getPrivateIpAddress());
                        dhcpCommand.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
                        cmds.addCommand("dhcp", dhcpCommand);
                    }
                }
            }
        }
    }

    private boolean sendCommandsToRouter(final DomainRouterVO router, Commands cmds) throws AgentUnavailableException {
        Answer[] answers = null;
        try {
            answers = _agentMgr.send(router.getHostId(), cmds);
        } catch (OperationTimedoutException e) {
            s_logger.warn("Timed Out", e);
            throw new AgentUnavailableException("Unable to send commands to virtual router ", router.getHostId(), e);
        }

        if (answers == null) {
            return false;
        }

        if (answers.length != cmds.size()) {
            return false;
        }

        // FIXME: Have to return state for individual command in the future
        if (answers.length > 0) {
            Answer ans = answers[0];
            return ans.getResult();
        }
        return true;
    }

    @Override
    public boolean associateIP(Network network, List<? extends PublicIpAddress> ipAddress) throws ResourceUnavailableException {
        DomainRouterVO router = _routerDao.findByNetwork(network.getId());
        if (router == null) {
            s_logger.warn("Unable to associate ip addresses, virtual router doesn't exist in the network " + network.getId());
            throw new ResourceUnavailableException("Unable to assign ip addresses", DataCenter.class, network.getDataCenterId());
        }

        if (router.getState() == State.Running) {
            Commands cmds = new Commands(OnError.Continue);
            // Have to resend all already associated ip addresses
            createAssociateIPCommands(router, ipAddress, cmds, 0);

            return sendCommandsToRouter(router, cmds);
        } else if (router.getState() == State.Stopped) {
            return true;
        } else {
            s_logger.warn("Unable to associate ip addresses, virtual router is not in the right state " + router.getState());
            throw new ResourceUnavailableException("Unable to assign ip addresses, domR is not in right state " + router.getState(), DataCenter.class, network.getDataCenterId());
        }
    }

    @Override
    public boolean applyFirewallRules(Network network, List<? extends FirewallRule> rules) throws ResourceUnavailableException {
        DomainRouterVO router = _routerDao.findByNetwork(network.getId());
        if (router == null) {
            s_logger.warn("Unable to apply lb rules, virtual router doesn't exist in the network " + network.getId());
            throw new ResourceUnavailableException("Unable to apply lb rules", DataCenter.class, network.getDataCenterId());
        }

        if (router.getState() == State.Running) {
            if (rules != null && !rules.isEmpty()) {
                if (rules.get(0).getPurpose() == Purpose.LoadBalancing) {
                    // for load balancer we have to resend all lb rules for the network
                    List<LoadBalancerVO> lbs = _loadBalancerDao.listByNetworkId(network.getId());
                    List<LoadBalancingRule> lbRules = new ArrayList<LoadBalancingRule>();
                    for (LoadBalancerVO lb : lbs) {
                        List<LbDestination> dstList = _lbMgr.getExistingDestinations(lb.getId());
                        LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList);
                        lbRules.add(loadBalancing);
                    }

                    return applyLBRules(router, lbRules);
                } else if (rules.get(0).getPurpose() == Purpose.PortForwarding) {
                    return applyPortForwardingRules(router, (List<PortForwardingRule>) rules);
                } else if (rules.get(0).getPurpose() == Purpose.StaticNat) {
                    return applyStaticNatRules(router, (List<StaticNatRule>) rules);

                } else {
                    s_logger.warn("Unable to apply rules of purpose: " + rules.get(0).getPurpose());
                    return false;
                }
            } else {
                return true;
            }
        } else if (router.getState() == State.Stopped || router.getState() == State.Stopping) {
            s_logger.debug("Router is in " + router.getState() + ", so not sending apply firewall rules commands to the backend");
            return true;
        } else {
            s_logger.warn("Unable to apply firewall rules, virtual router is not in the right state " + router.getState());
            throw new ResourceUnavailableException("Unable to apply firewall rules, virtual router is not in the right state", VirtualRouter.class, router.getId());
        }
    }

    protected boolean applyLBRules(DomainRouterVO router, List<LoadBalancingRule> rules) throws ResourceUnavailableException {
        Commands cmds = new Commands(OnError.Continue);
        createApplyLoadBalancingRulesCommands(rules, router, cmds);
        // Send commands to router
        return sendCommandsToRouter(router, cmds);
    }

    protected boolean applyPortForwardingRules(DomainRouterVO router, List<PortForwardingRule> rules) throws ResourceUnavailableException {
        Commands cmds = new Commands(OnError.Continue);
        createApplyPortForwardingRulesCommands(rules, router, cmds);
        // Send commands to router
        return sendCommandsToRouter(router, cmds);
    }

    protected boolean applyStaticNatRules(DomainRouterVO router, List<StaticNatRule> rules) throws ResourceUnavailableException {
        Commands cmds = new Commands(OnError.Continue);
        createApplyStaticNatRulesCommands(rules, router, cmds);
        // Send commands to router
        return sendCommandsToRouter(router, cmds);
    }

    @Override
    public VirtualRouter getRouterForNetwork(long networkId) {
        return _routerDao.findByNetwork(networkId);

    }
}
