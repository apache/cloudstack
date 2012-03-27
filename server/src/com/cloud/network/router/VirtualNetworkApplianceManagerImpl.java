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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BumpUpPriorityCommand;
import com.cloud.agent.api.CheckRouterAnswer;
import com.cloud.agent.api.CheckRouterCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetDomRVersionAnswer;
import com.cloud.agent.api.GetDomRVersionCmd;
import com.cloud.agent.api.ModifySshKeysCommand;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.RemoteAccessVpnCfgCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.SetFirewallRulesCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.api.routing.VpnUsersCfgCommand;
import com.cloud.agent.api.to.FirewallRuleTO;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.api.to.PortForwardingRuleTO;
import com.cloud.agent.api.to.StaticNatRuleTO;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.api.commands.UpgradeRouterCmd;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.ManagementServerNode;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ZoneConfig;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.Pod;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.dao.EventDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IPAddressVO;
import com.cloud.network.IpAddress;
import com.cloud.network.LoadBalancerVO;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.Networks.IsolationType;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.SshKeysDistriMonitor;
import com.cloud.network.VirtualNetworkApplianceService;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VirtualRouterProvider.VirtualRouterProviderType;
import com.cloud.network.VpnUser;
import com.cloud.network.VpnUserVO;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.FirewallRulesCidrsDao;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.RemoteAccessVpnDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.VirtualRouter.RedundantState;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.rules.StaticNatImpl;
import com.cloud.network.rules.StaticNatRule;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Volume.Type;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.UserStatsLogVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.user.dao.UserStatsLogDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.component.Inject;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.MacAddress;
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
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;

/**
 * VirtualNetworkApplianceManagerImpl manages the different types of virtual network appliances available in the Cloud Stack.
 */
@Local(value = { VirtualNetworkApplianceManager.class, VirtualNetworkApplianceService.class })
public class VirtualNetworkApplianceManagerImpl implements VirtualNetworkApplianceManager, VirtualNetworkApplianceService, VirtualMachineGuru<DomainRouterVO>, Listener {
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
    UserStatsLogDao _userStatsLogDao = null;
    @Inject
    AgentManager _agentMgr;
    @Inject
    StorageManager _storageMgr;
    @Inject
    AlertManager _alertMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    ServiceOfferingDao _serviceOfferingDao = null;
    @Inject
    UserVmDao _userVmDao;
    @Inject
    FirewallRulesDao _firewallRulesDao;
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
    @Inject
    VolumeDao _volumeDao = null;
    @Inject
    FirewallRulesCidrsDao _firewallCidrsDao;
    @Inject
    UserVmDetailsDao _vmDetailsDao;
    @Inject
    ClusterDao _clusterDao;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalProviderDao;
    @Inject
    VirtualRouterProviderDao _vrProviderDao;
    @Inject
    ManagementServerHostDao _msHostDao;
    
    int _routerRamSize;
    int _routerCpuMHz;
    int _retry = 2;
    String _instance;
    String _mgmt_host;
    String _mgmt_cidr;

    int _routerStatsInterval = 300;
    int _routerCheckInterval = 30;
    private ServiceOfferingVO _offering;
    private String _dnsBasicZoneUpdates = "all";

    private boolean _disable_rp_filter = false;
    int _routerExtraPublicNics = 2;
    private int _usageAggregationRange = 1440;
    private String _usageTimeZone = "GMT";
    private final long mgmtSrvrId = MacAddress.getMacAddress().toLong();
    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 5;    // 5 seconds
    
    ScheduledExecutorService _executor;
    ScheduledExecutorService _checkExecutor;
    ScheduledExecutorService _networkStatsUpdateExecutor;

    Account _systemAcct;

    @Override
    public List<DomainRouterVO> getRouters(long accountId, long dataCenterId) {
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
    public VirtualRouter destroyRouter(final long routerId) throws ResourceUnavailableException, ConcurrentOperationException {
        UserContext context = UserContext.current();
        User user = _accountMgr.getActiveUser(context.getCallerUserId());

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Attempting to destroy router " + routerId);
        }

        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            return null;
        }

        _accountMgr.checkAccess(context.getCaller(), null, true, router);

        boolean result = _itMgr.expunge(router, user, _accountMgr.getAccount(router.getAccountId()));

        if (result) {
            return router;
        }
        return null;
    }

    @Override
    @DB
    public VirtualRouter upgradeRouter(UpgradeRouterCmd cmd) {
        Long routerId = cmd.getId();
        Long serviceOfferingId = cmd.getServiceOfferingId();
        Account caller = UserContext.current().getCaller();

        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            throw new InvalidParameterValueException("Unable to find router with id " + routerId);
        }

        _accountMgr.checkAccess(caller, null, true, router);

        if (router.getServiceOfferingId() == serviceOfferingId) {
            s_logger.debug("Router: " + routerId + "already has service offering: " + serviceOfferingId);
            return _routerDao.findById(routerId);
        }

        ServiceOffering newServiceOffering = _configMgr.getServiceOffering(serviceOfferingId);
        if (newServiceOffering == null) {
            throw new InvalidParameterValueException("Unable to find service offering with id " + serviceOfferingId);
        }

        // check if it is a system service offering, if yes return with error as it cannot be used for user vms
        if (!newServiceOffering.getSystemUse()) {
            throw new InvalidParameterValueException("Cannot upgrade router vm to a non system service offering " + serviceOfferingId);
        }

        // Check that the router is stopped
        if (!router.getState().equals(State.Stopped)) {
            s_logger.warn("Unable to upgrade router " + router.toString() + " in state " + router.getState());
            throw new InvalidParameterValueException("Unable to upgrade router " + router.toString() + " in state " + router.getState()
                    + "; make sure the router is stopped and not in an error state before upgrading.");
        }

        ServiceOfferingVO currentServiceOffering = _serviceOfferingDao.findById(router.getServiceOfferingId());

        // Check that the service offering being upgraded to has the same storage pool preference as the VM's current service
        // offering
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

    @Override
    public boolean savePasswordToRouter(Network network, NicProfile nic, VirtualMachineProfile<UserVm> profile, List<? extends VirtualRouter> routers) throws ResourceUnavailableException {
        if (routers == null || routers.isEmpty()) {
            s_logger.warn("Unable save password, router doesn't exist in network " + network.getId());
            throw new CloudRuntimeException("Unable to save password to router");
        }

        UserVm userVm = profile.getVirtualMachine();
        String password = (String) profile.getParameter(Param.VmPassword);
        String encodedPassword = PasswordGenerator.rot13(password);
        DataCenter dc = _dcDao.findById(userVm.getDataCenterIdToDeployIn());

        boolean result = true;
        for (VirtualRouter router : routers) {
            boolean sendPassword = true;
            if (dc.getNetworkType() == NetworkType.Basic && userVm.getPodIdToDeployIn().longValue() != router.getPodIdToDeployIn().longValue()) {
                sendPassword = false;
            }

            if (sendPassword) {
                Commands cmds = new Commands(OnError.Continue);
                SavePasswordCommand cmd = new SavePasswordCommand(encodedPassword, nic.getIp4Address(), userVm.getHostName());
                cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
                cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
                DataCenterVO dcVo = _dcDao.findById(router.getDataCenterIdToDeployIn());
                cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
                cmds.addCommand("password", cmd);

                result = result && sendCommandsToRouter(router, cmds);
            }
        }
        return result;
    }

    @Override @ActionEvent(eventType = EventTypes.EVENT_ROUTER_STOP, eventDescription = "stopping router Vm", async = true)
    public VirtualRouter stopRouter(long routerId, boolean forced) throws ResourceUnavailableException, ConcurrentOperationException {
        UserContext context = UserContext.current();
        Account account = context.getCaller();

        // verify parameters
        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            throw new InvalidParameterValueException("Unable to find router by id " + routerId + ".");
        }

        _accountMgr.checkAccess(account, null, true, router);

        UserVO user = _userDao.findById(UserContext.current().getCallerUserId());

        VirtualRouter virtualRouter = stop(router, forced, user, account);
        if(virtualRouter == null){
            throw new CloudRuntimeException("Failed to stop router with id " + routerId);
        }
        return virtualRouter;
    }

    @DB
    public void processStopOrRebootAnswer(final DomainRouterVO router, Answer answer) {
        final Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            final UserStatisticsVO userStats = _userStatsDao.lock(router.getAccountId(), router.getDataCenterIdToDeployIn(), router.getNetworkId(), null, router.getId(), router.getType().toString());
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
                s_logger.warn("User stats were not created for account " + router.getAccountId() + " and dc " + router.getDataCenterIdToDeployIn());
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

    @Override @ActionEvent(eventType = EventTypes.EVENT_ROUTER_REBOOT, eventDescription = "rebooting router Vm", async = true)
    public VirtualRouter rebootRouter(long routerId, boolean reprogramNetwork) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        Account caller = UserContext.current().getCaller();

        // verify parameters
        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            throw new InvalidParameterValueException("Unable to find domain router with id " + routerId + ".");
        }

        _accountMgr.checkAccess(caller, null, true, router);

        // Can reboot domain router only in Running state
        if (router == null || router.getState() != State.Running) {
            s_logger.warn("Unable to reboot, virtual router is not in the right state " + router.getState());
            throw new ResourceUnavailableException("Unable to reboot domR, it is not in right state " + router.getState(), DataCenter.class, router.getDataCenterIdToDeployIn());
        }

        UserVO user = _userDao.findById(UserContext.current().getCallerUserId());
        s_logger.debug("Stopping and starting router " + router + " as a part of router reboot");

        if (stop(router, false, user, caller) != null) {
            return startRouter(routerId, reprogramNetwork);
        } else {
            throw new CloudRuntimeException("Failed to reboot router " + router);
        }
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        _name = name;

        _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("RouterMonitor"));
        _checkExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("RouterStatusMonitor"));
        _networkStatsUpdateExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("NetworkStatsUpdater"));

        final ComponentLocator locator = ComponentLocator.getCurrentLocator();

        final Map<String, String> configs = _configDao.getConfiguration("AgentManager", params);

        _mgmt_host = configs.get("host");
        _routerRamSize = NumbersUtil.parseInt(configs.get("router.ram.size"), DEFAULT_ROUTER_VM_RAMSIZE);
        _routerCpuMHz = NumbersUtil.parseInt(configs.get("router.cpu.mhz"), DEFAULT_ROUTER_CPU_MHZ);

        _routerExtraPublicNics = NumbersUtil.parseInt(_configDao.getValue(Config.RouterExtraPublicNics.key()), 2);

        String value = configs.get("start.retry");
        _retry = NumbersUtil.parseInt(value, 2);

        value = configs.get("router.stats.interval");
        _routerStatsInterval = NumbersUtil.parseInt(value, 300);

        value = configs.get("router.check.interval");
        _routerCheckInterval = NumbersUtil.parseInt(value, 30);
        
        _instance = configs.get("instance.name");
        if (_instance == null) {
            _instance = "DEFAULT";
        }

        String rpValue = configs.get("network.disable.rpfilter");
        if (rpValue != null && rpValue.equalsIgnoreCase("true")) {
            _disable_rp_filter = true;
        }

        _dnsBasicZoneUpdates = String.valueOf(_configDao.getValue(Config.DnsBasicZoneUpdates.key()));

        s_logger.info("Router configurations: " + "ramsize=" + _routerRamSize);

        final UserStatisticsDao statsDao = locator.getDao(UserStatisticsDao.class);
        if (statsDao == null) {
            throw new ConfigurationException("Unable to get " + UserStatisticsDao.class.getName());
        }

        _agentMgr.registerForHostEvents(new SshKeysDistriMonitor(_agentMgr, _hostDao, _configDao), true, false, false);
        _itMgr.registerGuru(VirtualMachine.Type.DomainRouter, this);

        boolean useLocalStorage = Boolean.parseBoolean(configs.get(Config.SystemVMUseLocalStorage.key()));
        _offering = new ServiceOfferingVO("System Offering For Software Router", 1, _routerRamSize, _routerCpuMHz, null, null, true, null, useLocalStorage, true, null, true, VirtualMachine.Type.DomainRouter, true);
        _offering.setUniqueName(ServiceOffering.routerDefaultOffUniqueName);
        _offering = _serviceOfferingDao.persistSystemServiceOffering(_offering);

        // this can sometimes happen, if DB is manually or programmatically manipulated
        if(_offering == null) {
            String msg = "Data integrity problem : System Offering For Software router VM has been removed?";
            s_logger.error(msg);
            throw new ConfigurationException(msg);
        }

        _systemAcct = _accountMgr.getSystemAccount();

        String aggregationRange = configs.get("usage.stats.job.aggregation.range");
        _usageAggregationRange  = NumbersUtil.parseInt(aggregationRange, 1440);
        _usageTimeZone = configs.get("usage.aggregation.timezone");
        if(_usageTimeZone == null){
            _usageTimeZone = "GMT";
        }

        _agentMgr.registerForHostEvents(this, true, false, false);

        s_logger.info("DomainRouterManager is configured.");

        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        if (_routerStatsInterval > 0){
            _executor.scheduleAtFixedRate(new NetworkUsageTask(), _routerStatsInterval, _routerStatsInterval, TimeUnit.SECONDS);
        }else{
            s_logger.debug("router.stats.interval - " + _routerStatsInterval+ " so not scheduling the router stats thread");
        }

        //Schedule Network stats update task
        TimeZone usageTimezone = TimeZone.getTimeZone(_usageTimeZone);
        Calendar cal = Calendar.getInstance(usageTimezone);
        cal.setTime(new Date());
        long endDate = 0;
        int HOURLY_TIME = 60;
        final int DAILY_TIME = 60 * 24;
        if (_usageAggregationRange == DAILY_TIME) {
        	cal.roll(Calendar.DAY_OF_YEAR, false);
        	cal.set(Calendar.HOUR_OF_DAY, 0);
        	cal.set(Calendar.MINUTE, 0);
        	cal.set(Calendar.SECOND, 0);
        	cal.set(Calendar.MILLISECOND, 0);
        	cal.roll(Calendar.DAY_OF_YEAR, true);
        	cal.add(Calendar.MILLISECOND, -1);
        	endDate = cal.getTime().getTime();
        } else if (_usageAggregationRange == HOURLY_TIME) {
        	cal.roll(Calendar.HOUR_OF_DAY, false);
        	cal.set(Calendar.MINUTE, 0);
        	cal.set(Calendar.SECOND, 0);
        	cal.set(Calendar.MILLISECOND, 0);
        	cal.roll(Calendar.HOUR_OF_DAY, true);
        	cal.add(Calendar.MILLISECOND, -1);
        	endDate = cal.getTime().getTime();
        } else {
        	endDate = cal.getTime().getTime();
        }

        _networkStatsUpdateExecutor.scheduleAtFixedRate(new NetworkStatsUpdateTask(), (endDate - System.currentTimeMillis()), (_usageAggregationRange * 60 * 1000), TimeUnit.MILLISECONDS);
        
        if (_routerCheckInterval > 0) {
            _checkExecutor.scheduleAtFixedRate(new CheckRouterTask(), _routerCheckInterval, _routerCheckInterval, TimeUnit.SECONDS);
        } else {
            s_logger.debug("router.check.interval - " + _routerCheckInterval+ " so not scheduling the redundant router checking thread");
        }

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

    private VmDataCommand generateVmDataCommand(VirtualRouter router, String vmPrivateIpAddress, String userData, String serviceOffering, String zoneName, String guestIpAddress, String vmName,
            String vmInstanceName, long vmId, String publicKey) {
        VmDataCommand cmd = new VmDataCommand(vmPrivateIpAddress, vmName);

        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, router.getGuestIpAddress());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());

        DataCenterVO dcVo = _dcDao.findById(router.getDataCenterIdToDeployIn());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

        cmd.addVmData("userdata", "user-data", userData);
        cmd.addVmData("metadata", "service-offering", StringUtils.unicodeEscape(serviceOffering));
        cmd.addVmData("metadata", "availability-zone", StringUtils.unicodeEscape(zoneName));
        cmd.addVmData("metadata", "local-ipv4", guestIpAddress);
        cmd.addVmData("metadata", "local-hostname", StringUtils.unicodeEscape(vmName));
        if (dcVo.getNetworkType() == NetworkType.Basic) {
            cmd.addVmData("metadata", "public-ipv4", guestIpAddress);
            cmd.addVmData("metadata", "public-hostname",  StringUtils.unicodeEscape(vmName));
        }else
        {
        	if (router.getPublicIpAddress() == null) {
        		 cmd.addVmData("metadata", "public-ipv4", guestIpAddress);
        	} else {
        		cmd.addVmData("metadata", "public-ipv4", router.getPublicIpAddress());
        	}
            cmd.addVmData("metadata", "public-hostname", router.getPublicIpAddress());
        }
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
            try{
                final List<DomainRouterVO> routers = _routerDao.listByStateAndNetworkType(State.Running, GuestType.Isolated, mgmtSrvrId);
                s_logger.debug("Found " + routers.size() + " running routers. ");

                for (DomainRouterVO router : routers) {
                    String privateIP = router.getPrivateIpAddress();
                    if (privateIP != null) {
                        final NetworkUsageCommand usageCmd = new NetworkUsageCommand(privateIP, router.getHostName());
                        UserStatisticsVO previousStats = _statsDao.findBy(router.getAccountId(), router.getDataCenterIdToDeployIn(), router.getNetworkId(), null, router.getId(), router.getType().toString());
                        NetworkUsageAnswer answer = null;
                        try {
                            answer = (NetworkUsageAnswer) _agentMgr.easySend(router.getHostId(), usageCmd);
                        } catch (Exception e) {
                            s_logger.warn("Error while collecting network stats from router: "+router.getInstanceName()+" from host: "+router.getHostId(), e);
                            continue;
                        }
                        
                        if (answer != null) {
                            if (!answer.getResult()) {
                                s_logger.warn("Error while collecting network stats from router: "+router.getInstanceName()+" from host: "+router.getHostId() + "; details: " + answer.getDetails());
                                continue;
                            }
                            Transaction txn = Transaction.open(Transaction.CLOUD_DB);
                            try {
                                if ((answer.getBytesReceived() == 0) && (answer.getBytesSent() == 0)) {
                                    s_logger.debug("Recieved and Sent bytes are both 0. Not updating user_statistics");
                                    continue;
                                }
                                txn.start();
                                UserStatisticsVO stats = _statsDao.lock(router.getAccountId(), router.getDataCenterIdToDeployIn(), router.getNetworkId(), null, router.getId(), router.getType().toString());
                                if (stats == null) {
                                    s_logger.warn("unable to find stats for account: " + router.getAccountId());
                                    continue;
                                }

                                if(previousStats != null 
                                        && ((previousStats.getCurrentBytesReceived() != stats.getCurrentBytesReceived()) || (previousStats.getCurrentBytesSent() != stats.getCurrentBytesSent()))){
                                    s_logger.debug("Router stats changed from the time NetworkUsageCommand was sent. Ignoring current answer. Router: "+answer.getRouterName()+" Rcvd: " + answer.getBytesReceived()+ "Sent: " +answer.getBytesSent());
                                    continue;
                                }

                                if (stats.getCurrentBytesReceived() > answer.getBytesReceived()) {
                                    if (s_logger.isDebugEnabled()) {
                                        s_logger.debug("Received # of bytes that's less than the last one.  Assuming something went wrong and persisting it. Router: "+answer.getRouterName()+" Reported: " + answer.getBytesReceived()
                                                + " Stored: " + stats.getCurrentBytesReceived());
                                    }
                                    stats.setNetBytesReceived(stats.getNetBytesReceived() + stats.getCurrentBytesReceived());
                                }
                                stats.setCurrentBytesReceived(answer.getBytesReceived());
                                if (stats.getCurrentBytesSent() > answer.getBytesSent()) {
                                    if (s_logger.isDebugEnabled()) {
                                        s_logger.debug("Received # of bytes that's less than the last one.  Assuming something went wrong and persisting it. Router: "+answer.getRouterName()+" Reported: " + answer.getBytesSent()
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
            } catch (Exception e) {
                s_logger.warn("Error while collecting network stats", e);
            }
        }
    }

    protected class NetworkStatsUpdateTask implements Runnable {

        public NetworkStatsUpdateTask() {
        }

        @Override
        public void run() {
        	GlobalLock scanLock = GlobalLock.getInternLock("network.stats");
            try {
                if(scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
                	//Check for ownership
                	//msHost in UP state with min id should run the job
                	ManagementServerHostVO msHost = _msHostDao.findOneInUpState(new Filter(ManagementServerHostVO.class, "id", true, 0L, 1L));
                	if(msHost == null || (msHost.getMsid() != mgmtSrvrId)){
                		s_logger.debug("Skipping aggregate network stats update");
                		scanLock.unlock();
                		return;
                	}
                	Transaction txn = Transaction.open(Transaction.CLOUD_DB);
                    try {
                    	txn.start();
                    	//get all stats with delta > 0
                    	List<UserStatisticsVO> updatedStats = _statsDao.listUpdatedStats();
                    	Date updatedTime = new Date();
                    	for(UserStatisticsVO stat : updatedStats){
                    		//update agg bytes            		
                    		stat.setAggBytesReceived(stat.getCurrentBytesReceived() + stat.getNetBytesReceived());
                    		stat.setAggBytesSent(stat.getCurrentBytesSent() + stat.getNetBytesSent());
                    		_userStatsDao.update(stat.getId(), stat);
                    		//insert into op_user_stats_log
                    		UserStatsLogVO statsLog = new UserStatsLogVO(stat.getId(), stat.getNetBytesReceived(), stat.getNetBytesSent(), stat.getCurrentBytesReceived(), 
                    													 stat.getCurrentBytesSent(), stat.getAggBytesReceived(), stat.getAggBytesSent(), updatedTime);
                    		_userStatsLogDao.persist(statsLog);
                    	}
                    	s_logger.debug("Successfully updated aggregate network stats");
                    	txn.commit();
                    } catch (Exception e){
                    	txn.rollback();
                        s_logger.debug("Failed to update aggregate network stats", e);
                    } finally {
                        scanLock.unlock();
                        txn.close();
                    }
                }
            } catch (Exception e){
                s_logger.debug("Exception while trying to acquire network stats lock", e);
            }  finally {
                scanLock.releaseRef();
            }
           
        }

    }


    protected void updateRoutersRedundantState(List<DomainRouterVO> routers) {
        boolean updated = false;
        for (DomainRouterVO router : routers) {
            updated = false;
            if (!router.getIsRedundantRouter()) {
                continue;
            }
            RedundantState prevState = router.getRedundantState();
            if (router.getState() != State.Running) {
                router.setRedundantState(RedundantState.UNKNOWN);
                router.setIsPriorityBumpUp(false);
                updated = true;
            } else {
                String privateIP = router.getPrivateIpAddress();
                HostVO host = _hostDao.findById(router.getHostId());
                if (host == null || host.getStatus() != Status.Up) {
                    router.setRedundantState(RedundantState.UNKNOWN);
                    updated = true;
                } else if (host.getManagementServerId() != ManagementServerNode.getManagementServerId()) {
                    /* Only cover hosts managed by this management server */
                    continue;
                } else if (privateIP != null) {
                    final CheckRouterCommand command = new CheckRouterCommand();
                    command.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
                    command.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
                    command.setWait(60);
                    final Answer origAnswer = _agentMgr.easySend(router.getHostId(), command);
                    CheckRouterAnswer answer = null;
                    if (origAnswer instanceof CheckRouterAnswer) {
                        answer = (CheckRouterAnswer)origAnswer;
                    } else {
                        s_logger.warn("Unable to update router " + router.getHostName() + "'s status");
                    }
                    RedundantState state = RedundantState.UNKNOWN;
                    boolean isBumped = router.getIsPriorityBumpUp();
                    if (answer != null && answer.getResult()) {
                        state = answer.getState();
                        isBumped = answer.isBumped();
                    }
                    router.setRedundantState(state);
                    router.setIsPriorityBumpUp(isBumped);
                    updated = true;
                }
            }
            if (updated) {
                Transaction txn = Transaction.open(Transaction.CLOUD_DB);
                try {
                    txn.start();
                    _routerDao.update(router.getId(), router);
                    txn.commit();
                } catch (Exception e) {
                    txn.rollback();
                    s_logger.warn("Unable to update router status for account: " + router.getAccountId());
                } finally {
                    txn.close();
                }
            }
            RedundantState currState = router.getRedundantState();
            if (prevState != currState) {
                String title = "Redundant virtual router " + router.getInstanceName() +
                        " just switch from " + prevState + " to " + currState;
                String context =  "Redundant virtual router (name: " + router.getHostName() + ", id: " + router.getId() + ") " +
                        " just switch from " + prevState + " to " + currState;
                s_logger.info(context);
                if (currState == RedundantState.MASTER) {
                    _alertMgr.sendAlert(AlertManager.ALERT_TYPE_DOMAIN_ROUTER,
                            router.getDataCenterIdToDeployIn(), router.getPodIdToDeployIn(), title, context);
                }
            }
        }
    }

    //Ensure router status is update to date before execute this function. The function would try best to recover all routers except MASTER
    protected void recoverRedundantNetwork(DomainRouterVO masterRouter, DomainRouterVO backupRouter) {
        UserContext context = UserContext.current();
        context.setAccountId(1);                            
        if (masterRouter.getState() == State.Running && backupRouter.getState() == State.Running) {
            HostVO masterHost = _hostDao.findById(masterRouter.getHostId());
            HostVO backupHost = _hostDao.findById(backupRouter.getHostId());
            if (masterHost.getStatus() == Status.Up && backupHost.getStatus() == Status.Up) {
                String title =  "Reboot " + backupRouter.getInstanceName() + " to ensure redundant virtual routers work";
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(title);
                }
                _alertMgr.sendAlert(AlertManager.ALERT_TYPE_DOMAIN_ROUTER,
                        backupRouter.getDataCenterIdToDeployIn(), backupRouter.getPodIdToDeployIn(), title, title);
                try {
                    rebootRouter(backupRouter.getId(), false);
                } catch (ConcurrentOperationException e) {
                    s_logger.warn("Fail to reboot " + backupRouter.getInstanceName(), e);
                } catch (ResourceUnavailableException e) {
                    s_logger.warn("Fail to reboot " + backupRouter.getInstanceName(), e);
                } catch (InsufficientCapacityException e) {
                    s_logger.warn("Fail to reboot " + backupRouter.getInstanceName(), e);
                }
            }
        }
    }

    private int getRealPriority(DomainRouterVO router) {
        int priority = router.getPriority();
        if (router.getIsPriorityBumpUp()) {
            priority += DEFAULT_DELTA;
        }
        return priority;
    }

    protected class CheckRouterTask implements Runnable {

        public CheckRouterTask() {
        }

        /*
         * In order to make fail-over works well at any time, we have to ensure:
         * 1. Backup router's priority = Master's priority - DELTA + 1
         * 2. Backup router's priority hasn't been bumped up.
         */
        private void checkSanity(List<DomainRouterVO> routers) {
            Set<Long> checkedNetwork = new HashSet<Long>();
            for (DomainRouterVO router : routers) {
                if (!router.getIsRedundantRouter()) {
                    continue;
                }
                long networkId = router.getNetworkId();
                if (checkedNetwork.contains(networkId)) {
                    continue;
                }
                checkedNetwork.add(networkId);
                List<DomainRouterVO> checkingRouters = _routerDao.listByNetworkAndRole(networkId, Role.VIRTUAL_ROUTER);
                if (checkingRouters.size() != 2) {
                    continue;
                }
                DomainRouterVO masterRouter = null;
                DomainRouterVO backupRouter = null;
                for (DomainRouterVO r : checkingRouters) {
                    if (r.getRedundantState() == RedundantState.MASTER) {
                        if (masterRouter == null) {
                            masterRouter = r;
                        } else {
                            //Duplicate master! We give up, until the admin fix duplicate MASTER issue
                            break;
                        }
                    } else if (r.getRedundantState() == RedundantState.BACKUP) {
                        if (backupRouter == null) {
                            backupRouter = r;
                        } else {
                            break;
                        }
                    }
                }
                if (masterRouter != null && backupRouter != null) {
                    if (getRealPriority(masterRouter) - DEFAULT_DELTA + 1 != getRealPriority(backupRouter) || backupRouter.getIsPriorityBumpUp()) {
                        recoverRedundantNetwork(masterRouter, backupRouter);
                    }
                }
            }
        }

        private void checkDuplicateMaster(List <DomainRouterVO> routers) {
            Map<Long, DomainRouterVO> networkRouterMaps = new HashMap<Long, DomainRouterVO>();
            for (DomainRouterVO router : routers) {
                if (router.getRedundantState() == RedundantState.MASTER) {
                    if (networkRouterMaps.containsKey(router.getNetworkId())) {
                        DomainRouterVO dupRouter = networkRouterMaps.get(router.getNetworkId());
                        String title = "More than one redundant virtual router is in MASTER state! Router " + router.getHostName() + " and router " + dupRouter.getHostName();
                        String context =  "Virtual router (name: " + router.getHostName() + ", id: " + router.getId() + " and router (name: "
                                + dupRouter.getHostName() + ", id: " + router.getId() + ") are both in MASTER state! If the problem persist, restart both of routers. ";

                        _alertMgr.sendAlert(AlertManager.ALERT_TYPE_DOMAIN_ROUTER, router.getDataCenterIdToDeployIn(), router.getPodIdToDeployIn(), title, context);
                        _alertMgr.sendAlert(AlertManager.ALERT_TYPE_DOMAIN_ROUTER, dupRouter.getDataCenterIdToDeployIn(), dupRouter.getPodIdToDeployIn(), title, context);
                    } else {
                        networkRouterMaps.put(router.getNetworkId(), router);
                    }
                }
            }
        }

        @Override
        public void run() {
            try {
                final List<DomainRouterVO> routers = _routerDao.listVirtualByHostId(null);
                s_logger.debug("Found " + routers.size() + " routers. ");

                updateRoutersRedundantState(routers);

                /* FIXME assumed the a pair of redundant routers managed by same mgmt server,
                 * then the update above can get the latest status */
                checkDuplicateMaster(routers);
                checkSanity(routers);
            } catch (Exception ex) {
                s_logger.error("Fail to complete the CheckRouterTask! ", ex);
            }
        }
    }

    public static boolean isAdmin(short accountType) {
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN) || (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) || (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN) || (accountType == Account.ACCOUNT_TYPE_RESOURCE_DOMAIN_ADMIN));
    } 
    private final int DEFAULT_PRIORITY = 100;
    private final int DEFAULT_DELTA = 2;

    protected int getUpdatedPriority(Network guestNetwork, List<DomainRouterVO> routers, DomainRouterVO exclude) throws InsufficientVirtualNetworkCapcityException {
        int priority;
        if (routers.size() == 0) {
            priority = DEFAULT_PRIORITY;
        } else {
            int maxPriority = 0;
            for (DomainRouterVO r : routers) {
                if (!r.getIsRedundantRouter()) {
                    throw new CloudRuntimeException("Redundant router is mixed with single router in one network!");
                }
                //FIXME Assume the maxPriority one should be running or just created.
                if (r.getId() != exclude.getId() && getRealPriority(r) > maxPriority) {
                    maxPriority = getRealPriority(r);
                }
            }
            if (maxPriority == 0) {
                return DEFAULT_PRIORITY;
            }
            if (maxPriority < 20) {
                s_logger.error("Current maximum priority is too low!");
                throw new InsufficientVirtualNetworkCapcityException("Current maximum priority is too low as " + maxPriority + "!",
                        guestNetwork.getId());
            } else if (maxPriority > 200) {
                s_logger.error("Too many times fail-over happened! Current maximum priority is too high as " + maxPriority + "!");
                throw new InsufficientVirtualNetworkCapcityException("Too many times fail-over happened! Current maximum priority is too high as "
                        + maxPriority + "!", guestNetwork.getId());
            }
            priority = maxPriority - DEFAULT_DELTA + 1;
        }
        return priority;
    }

    /*
     * Ovm won't support any system. So we have to choose a partner cluster in the same pod to start domain router for us
     */
    private HypervisorType getClusterToStartDomainRouterForOvm(long podId) {
        List<ClusterVO> clusters = _clusterDao.listByPodId(podId);
        for (ClusterVO cv : clusters) {
            if (cv.getHypervisorType() == HypervisorType.Ovm || cv.getHypervisorType() == HypervisorType.BareMetal) {
                continue;
            }

            List<HostVO> hosts = _resourceMgr.listAllHostsInCluster(cv.getId());
            if (hosts == null || hosts.isEmpty()) {
                continue;
            }

            for (HostVO h : hosts) {
                if (h.getStatus() == Status.Up) {
                    s_logger.debug("Pick up host that has hypervisor type " + h.getHypervisorType() + " in cluster " + cv.getId() + " to start domain router for OVM");
                    return h.getHypervisorType();
                }
            }
        }

        String errMsg = "Cannot find an available cluster in Pod "
                + podId
                + " to start domain router for Ovm. \n Ovm won't support any system vm including domain router, please make sure you have a cluster with hypervisor type of any of xenserver/KVM/Vmware in the same pod with Ovm cluster. And there is at least one host in UP status in that cluster.";
        throw new CloudRuntimeException(errMsg);
    }

    @DB
    protected List<DomainRouterVO> findOrDeployVirtualRouters(Network guestNetwork, DeployDestination dest, Account owner, boolean isRedundant, Map<Param, Object> params) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {

        Network network = _networkDao.acquireInLockTable(guestNetwork.getId());
        if (network == null) {
            throw new ConcurrentOperationException("Unable to lock network " + guestNetwork.getId());
        }

        long dcId = dest.getDataCenter().getId();
        DataCenterDeployment plan = new DataCenterDeployment(dcId);
        boolean isPodBased = (dest.getDataCenter().getNetworkType() == NetworkType.Basic || _networkMgr.areServicesSupportedInNetwork(guestNetwork.getId(), Service.SecurityGroup)) && guestNetwork.getTrafficType() == TrafficType.Guest;
        boolean publicNetwork = false;
        if (_networkMgr.isProviderSupportServiceInNetwork(guestNetwork.getId(), Service.SourceNat, Provider.VirtualRouter)) {
            publicNetwork = true;
        }
        if (isRedundant && !publicNetwork) {
            s_logger.error("Didn't support redundant virtual router without public network!");
            return null;
        }
        List<DomainRouterVO> routers;
        Long podId = null;
        if (isPodBased) {
            Pod pod = dest.getPod();
            if (pod != null) {
                podId = pod.getId();
            }
        }
        
        if (publicNetwork) {
            routers = _routerDao.listByNetworkAndRole(guestNetwork.getId(), Role.VIRTUAL_ROUTER);
        } else {
            if (isPodBased && podId != null) {
                routers = _routerDao.listByNetworkAndPodAndRole(guestNetwork.getId(), podId, Role.VIRTUAL_ROUTER);
                plan = new DataCenterDeployment(dcId, podId, null, null, null, null);
            } else {
                routers = _routerDao.listByNetworkAndRole(guestNetwork.getId(), Role.VIRTUAL_ROUTER);
                plan = new DataCenterDeployment(dcId);
            }
        }

        try {
            int routerCount = 1;
            if (isRedundant) {
                routerCount = 2;
            }

            /* If it is the single router network, then keep it untouched */
            for (DomainRouterVO router : routers) {
                if (!router.getIsRedundantRouter()) {
                    routerCount = 1;
                }
            }

            /* If old network is redundant but new is single router, then routers.size() = 2 but routerCount = 1 */
            if (routers.size() >= routerCount || (isPodBased && podId == null)) {
                return routers;
            }

            if (routers.size() >= 5) {
                s_logger.error("Too much redundant routers!");
            }

            NicProfile defaultNic = new NicProfile();
            //if source nat service is supported by the network, get the source nat ip address
            if (publicNetwork) {
                PublicIp sourceNatIp = _networkMgr.assignSourceNatIpAddress(owner, guestNetwork, _accountMgr.getSystemUser().getId());
                defaultNic.setDefaultNic(true);
                defaultNic.setIp4Address(sourceNatIp.getAddress().addr());
                defaultNic.setGateway(sourceNatIp.getGateway());
                defaultNic.setNetmask(sourceNatIp.getNetmask());
                defaultNic.setMacAddress(sourceNatIp.getMacAddress());
                defaultNic.setBroadcastType(BroadcastDomainType.Vlan);
                defaultNic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(sourceNatIp.getVlanTag()));
                defaultNic.setIsolationUri(IsolationType.Vlan.toUri(sourceNatIp.getVlanTag()));
                defaultNic.setDeviceId(2);
            } 

            int count = routerCount - routers.size();

            for (int i = 0; i < count; i++) {
                long id = _routerDao.getNextInSequence(Long.class, "id");
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Creating the router " + id);
                }

                DomainRouterVO router = null;

                List<NetworkOfferingVO> offerings = _networkMgr.getSystemAccountNetworkOfferings(NetworkOfferingVO.SystemControlNetwork);
                NetworkOfferingVO controlOffering = offerings.get(0);
                NetworkVO controlConfig = _networkMgr.setupNetwork(_systemAcct, controlOffering, plan, null, null, false).get(0);

                List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>(3);
                if (publicNetwork) {
                    NetworkOfferingVO publicOffering = _networkMgr.getSystemAccountNetworkOfferings(NetworkOfferingVO.SystemPublicNetwork).get(0);
                    List<NetworkVO> publicNetworks = _networkMgr.setupNetwork(_systemAcct, publicOffering, plan, null, null, false);
                    networks.add(new Pair<NetworkVO, NicProfile>(publicNetworks.get(0), defaultNic));
                }

                String defaultNetworkStartIp = null;
                if (guestNetwork.getCidr() != null && !publicNetwork) {
                    String startIp = _networkMgr.getStartIpAddress(guestNetwork.getId());
                    if (startIp != null && _ipAddressDao.findByIpAndSourceNetworkId(guestNetwork.getId(), startIp).getAllocatedTime() == null) {
                        defaultNetworkStartIp = startIp;
                    } else if (s_logger.isDebugEnabled()){
                        s_logger.debug("First ip " + startIp + " in network id=" + guestNetwork.getId() + " is already allocated, can't use it for domain router; will get random ip address from the range");
                    }
                }

                NicProfile gatewayNic = new NicProfile(defaultNetworkStartIp);
                if (publicNetwork) {
                    if (isRedundant) {
                        gatewayNic.setIp4Address(_networkMgr.acquireGuestIpAddress(guestNetwork, null));
                    } else {
                        gatewayNic.setIp4Address(guestNetwork.getGateway());
                    }
                    gatewayNic.setBroadcastUri(guestNetwork.getBroadcastUri());
                    gatewayNic.setBroadcastType(guestNetwork.getBroadcastDomainType());
                    gatewayNic.setIsolationUri(guestNetwork.getBroadcastUri());
                    gatewayNic.setMode(guestNetwork.getMode());
                    String gatewayCidr = guestNetwork.getCidr();
                    gatewayNic.setNetmask(NetUtils.getCidrNetmask(gatewayCidr));
                } else {
                    gatewayNic.setDefaultNic(true);
                }

                networks.add(new Pair<NetworkVO, NicProfile>((NetworkVO) guestNetwork, gatewayNic));
                networks.add(new Pair<NetworkVO, NicProfile>(controlConfig, null));

                Long offering_id = _networkOfferingDao.findById(guestNetwork.getNetworkOfferingId()).getServiceOfferingId();
                if (offering_id == null) {
                    offering_id = _offering.getId();
                }
                VirtualRouterProviderType type = VirtualRouterProviderType.VirtualRouter;
                Long physicalNetworkId = _networkMgr.getPhysicalNetworkId(network);
                PhysicalNetworkServiceProvider provider = _physicalProviderDao.findByServiceProvider(physicalNetworkId, type.toString());
                if (provider == null) {
                    throw new CloudRuntimeException("Cannot find service provider " + type.toString() + " in physical network " + physicalNetworkId);
                }
                VirtualRouterProvider vrProvider = _vrProviderDao.findByNspIdAndType(provider.getId(), type);
                if (vrProvider == null) {
                    throw new CloudRuntimeException("Cannot find virtual router provider " + type.toString()+ " as service provider " + provider.getId());
                }
                ServiceOfferingVO routerOffering = _serviceOfferingDao.findById(offering_id);

                //Router is the network element, we don't know the hypervisor type yet.
                //Try to allocate the domR twice using diff hypervisors, and when failed both times, throw the exception up
                List<HypervisorType> supportedHypervisors = new ArrayList<HypervisorType>();
                HypervisorType defaults = _resourceMgr.getDefaultHypervisor(dest.getDataCenter().getId());
                if (defaults != HypervisorType.None) {
                	supportedHypervisors.add(defaults);
                }
                
                if (dest.getCluster() != null) {
                    if (dest.getCluster().getHypervisorType() == HypervisorType.Ovm) {
                    	supportedHypervisors.add(getClusterToStartDomainRouterForOvm(dest.getCluster().getPodId()));
                    } else {
                    	supportedHypervisors.add(dest.getCluster().getHypervisorType());
                    }
                } else {
                    supportedHypervisors = _resourceMgr.getSupportedHypervisorTypes(dest.getDataCenter().getId(), true, podId);
                }               
                
                if (supportedHypervisors.isEmpty()) {
                	if (podId != null) {
                    	throw new InsufficientServerCapacityException("Unable to create virtual router, there are no clusters in the pod ", Pod.class, podId);
                	}
                	throw new InsufficientServerCapacityException("Unable to create virtual router, there are no clusters in the zone ", DataCenter.class, dest.getDataCenter().getId());
                }
                
                int allocateRetry = 0;
                int startRetry = 0;
                
                
                for (Iterator<HypervisorType> iter = supportedHypervisors.iterator();iter.hasNext();) {
                    HypervisorType hType = iter.next();
                    try {
                        s_logger.debug("Allocating the domR with the hypervisor type " + hType);
                        VMTemplateVO template = _templateDao.findRoutingTemplate(hType);

                        if (template == null) {
                            s_logger.debug(hType + " won't support system vm, skip it");
                            continue;
                        }
                        
                        boolean offerHA = routerOffering.getOfferHA();
                        /* We don't provide HA to redundant router VMs, admin should own it all, and redundant router themselves are HA */
                        if (isRedundant) {
                            offerHA = false;
                        }

                        router = new DomainRouterVO(id, routerOffering.getId(), vrProvider.getId(), VirtualMachineName.getRouterName(id, _instance), template.getId(), template.getHypervisorType(),
                                template.getGuestOSId(), owner.getDomainId(), owner.getId(), guestNetwork.getId(), isRedundant, 0, false, RedundantState.UNKNOWN, offerHA, false);
                        router.setRole(Role.VIRTUAL_ROUTER);
                        router = _itMgr.allocate(router, template, routerOffering, networks, plan, null, owner);
                    } catch (InsufficientCapacityException ex) {
                        if (allocateRetry < 2 && iter.hasNext()) {
                            s_logger.debug("Failed to allocate the domR with hypervisor type " + hType + ", retrying one more time");
                            continue;
                        } else {
                            throw ex;
                        }
                    } finally {
                        allocateRetry++;
                    }
                    
                    try {
                        router = startVirtualRouter(router, _accountMgr.getSystemUser(), _accountMgr.getSystemAccount(), params);
                        break;
                    } catch (InsufficientCapacityException ex) {
                        if (startRetry < 2 && iter.hasNext()) {
                            s_logger.debug("Failed to start the domR  " + router + " with hypervisor type " + hType + ", destroying it and recreating one more time");
                            //destroy the router
                            destroyRouter(router.getId());
                            continue;
                        } else {
                            throw ex;
                        }
                    } finally {
                        startRetry++;
                    }
                }
                
                routers.add(router);

            }
        } finally {
            if (network != null) {
                _networkDao.releaseFromLockTable(network.getId());
            }
        }
        return routers;
    }

    private DomainRouterVO startVirtualRouter(DomainRouterVO router, User user, Account caller, Map<Param, Object> params) throws StorageUnavailableException, InsufficientCapacityException,
    ConcurrentOperationException, ResourceUnavailableException {
    	
    	if (router.getRole() != Role.VIRTUAL_ROUTER || !router.getIsRedundantRouter()) {
            return this.start(router, user, caller, params, null);
        }

        if (router.getState() == State.Running) {
            s_logger.debug("Redundant router " + router.getInstanceName() + " is already running!");
            return router;
        }

        DataCenterDeployment plan = new DataCenterDeployment(0, null, null, null, null, null);
        DomainRouterVO result = null;
        assert router.getIsRedundantRouter();
        List<DomainRouterVO> routerList = _routerDao.findBy(router.getAccountId(), router.getDataCenterIdToDeployIn());
        DomainRouterVO routerToBeAvoid = null;
        for (DomainRouterVO rrouter : routerList) {
            if (rrouter.getHostId() != null && rrouter.getIsRedundantRouter() && rrouter.getState() == State.Running) {
                if (routerToBeAvoid != null) {
                    throw new ResourceUnavailableException("Try to start router " + router.getInstanceName() + "(" + router.getId() + ")"
                            + ", but there are already two redundant routers with IP " + router.getPublicIpAddress()
                            + ", they are " + rrouter.getInstanceName() + "(" + rrouter.getId() + ") and "
                            + routerToBeAvoid.getInstanceName() + "(" + routerToBeAvoid.getId() + ")",
                            DataCenter.class, rrouter.getDataCenterIdToDeployIn());
                }
                routerToBeAvoid = rrouter;
            }
        }
        if (routerToBeAvoid == null) {
            return this.start(router, user, caller, params, null); 
        }
        // We would try best to deploy the router to another place
        int retryIndex = 5;
        ExcludeList[] avoids = new ExcludeList[5];
        avoids[0] = new ExcludeList();
        avoids[0].addPod(routerToBeAvoid.getPodIdToDeployIn());
        avoids[1] = new ExcludeList();
        avoids[1].addCluster(_hostDao.findById(routerToBeAvoid.getHostId()).getClusterId());
        avoids[2] = new ExcludeList();
        List<VolumeVO> volumes = _volumeDao.findByInstanceAndType(routerToBeAvoid.getId(), Type.ROOT);
        if (volumes != null && volumes.size() != 0) {
            avoids[2].addPool(volumes.get(0).getPoolId());
        }
        avoids[2].addHost(routerToBeAvoid.getHostId());
        avoids[3] = new ExcludeList();
        avoids[3].addHost(routerToBeAvoid.getHostId());
        avoids[4] = new ExcludeList();

        for (int i = 0; i < retryIndex; i++) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Try to deploy redundant virtual router:" + router.getHostName() + ", for " + i + " time");
            }
            plan.setAvoids(avoids[i]);
            try {
                result = this.start(router, user, caller, params, plan);
            } catch (InsufficientServerCapacityException ex) {
                result = null;
            }
            if (result != null) {
                break;
            }
        }
        return result;
    }

    @Override
    public List<DomainRouterVO> deployVirtualRouter(Network guestNetwork, DeployDestination dest, Account owner, Map<Param, Object> params, boolean isRedundant) throws InsufficientCapacityException,
    ConcurrentOperationException, ResourceUnavailableException {
        if (_networkMgr.isNetworkSystem(guestNetwork) || guestNetwork.getGuestType() == Network.GuestType.Shared) {
            owner = _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM);
        }

        if(dest != null){
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Starting a router for " + guestNetwork + " in datacenter:" + dest.getDataCenter());
            }
        }
        
        assert guestNetwork.getState() == Network.State.Implemented || guestNetwork.getState() == Network.State.Setup || guestNetwork.getState() == Network.State.Implementing : "Network is not yet fully implemented: "
                + guestNetwork;
        assert guestNetwork.getTrafficType() == TrafficType.Guest;

        List<DomainRouterVO> routers = findOrDeployVirtualRouters(guestNetwork, dest, owner, isRedundant, params);
        List<DomainRouterVO> runningRouters = null;

        if (routers != null) {
            runningRouters = new ArrayList<DomainRouterVO>();
        }

        for (DomainRouterVO router : routers) {
            boolean skip = false;
            State state = router.getState();
            if (router.getHostId() != null && state != State.Running) {
                HostVO host = _hostDao.findById(router.getHostId());
                if (host == null || host.getStatus() != Status.Up) {
                    skip = true;
                }
            }
            if (!skip) {
                if (state != State.Running) {
                    router = startVirtualRouter(router, _accountMgr.getSystemUser(), _accountMgr.getSystemAccount(), params);
                }
                if (router != null) {
                    runningRouters.add(router);
                }
            }
        }
        return runningRouters;
    }

    @Override
    public boolean finalizeVirtualMachineProfile(VirtualMachineProfile<DomainRouterVO> profile, DeployDestination dest, ReservationContext context) {

        DomainRouterVO router = profile.getVirtualMachine();
        Map<String, String> details = _vmDetailsDao.findDetails(router.getId());
        router.setDetails(details);
        NetworkVO network = _networkDao.findById(router.getNetworkId());

        String type = null;
        String dhcpRange = null;
        String rpFilter = " ";
        DataCenter dc = dest.getDataCenter();
        DataCenterVO dcVO = _dcDao.findById(dc.getId());
        _dcDao.loadDetails(dcVO);

        if (dc.getNetworkType() == NetworkType.Advanced) {
            String cidr = network.getCidr();
            if (cidr != null) {
                dhcpRange = NetUtils.getDhcpRange(cidr);
            }
        }

        String rpValue = _configDao.getValue(Config.NetworkRouterRpFilter.key());
        if (rpValue != null && rpValue.equalsIgnoreCase("true")) {
            _disable_rp_filter = true;
        }else
        {
            _disable_rp_filter = false;
        }

        boolean publicNetwork = false;
        if (_networkMgr.isProviderSupportServiceInNetwork(network.getId(), Service.SourceNat, Provider.VirtualRouter)) {
            publicNetwork = true;
        }
        if (!publicNetwork) {
            type = "dhcpsrvr";
        } else {
            type = "router";
            if (_disable_rp_filter) {
                rpFilter=" disable_rp_filter=true";
            }
        }

        StringBuilder buf = profile.getBootArgsBuilder();
        buf.append(" template=domP type=" + type+rpFilter);
        buf.append(" name=").append(profile.getHostName());

        if (Boolean.valueOf(_configDao.getValue("system.vm.random.password"))) {
        	buf.append(" vmpassword=").append(_configDao.getValue("system.vm.password"));
        }
        
        boolean isRedundant = router.getIsRedundantRouter();
        if (isRedundant) {
            buf.append(" redundant_router=1");
            List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.VIRTUAL_ROUTER);
            try {
                int priority = getUpdatedPriority(network, routers, router);
                router.setPriority(priority);
            } catch (InsufficientVirtualNetworkCapcityException e) {
                s_logger.error("Failed to get update priority!", e);
                throw new CloudRuntimeException("Failed to get update priority!");
            }
        }
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


                    if (dc.getNetworkType() == NetworkType.Basic) {
                        // ask domR to setup SSH on guest network
                        buf.append(" sshonguest=true");
                    }
                }

                controlNic = nic;
            } else if (nic.getTrafficType() == TrafficType.Guest && isRedundant) {
                Network net = _networkMgr.getNetwork(nic.getNetworkId());
                buf.append(" guestgw=").append(net.getGateway());
                String brd = NetUtils.long2Ip(NetUtils.ip2Long(nic.getIp4Address()) | ~NetUtils.ip2Long(nic.getNetmask()));
                buf.append(" guestbrd=").append(brd);
                buf.append(" guestcidrsize=").append(NetUtils.getCidrSize(nic.getNetmask()));
                buf.append(" router_pr=").append(router.getPriority());
            }
        }

        if (dhcpRange != null) {
            buf.append(" dhcprange=" + dhcpRange);
        }
        String domain = network.getNetworkDomain();
        if (domain != null) {
            buf.append(" domain=" + domain);
        }  
        String domain_suffix = dcVO.getDetail(ZoneConfig.DnsSearchOrder.getName());
        if (domain_suffix != null) {
            buf.append(" dnssearchorder=").append(domain_suffix);
        }

//        if (!network.isDefault() && network.getGuestType() == Network.GuestType.Shared) {
//            buf.append(" defaultroute=false");
//
//            String virtualNetworkElementNicIP = _networkMgr.getIpOfNetworkElementInVirtualNetwork(network.getAccountId(), network.getDataCenterId());
//            if (network.getGuestType() != Network.GuestType.Shared && virtualNetworkElementNicIP != null) {
//                defaultDns1 = virtualNetworkElementNicIP;
//            } else {
//                s_logger.debug("No Virtual network found for account id=" + network.getAccountId() + " so setting dns to the dns of the network id=" + network.getId());
//            }
//        } else {
//            buf.append(" defaultroute=true");
//        }

        boolean dnsProvided = _networkMgr.isProviderSupportServiceInNetwork(network.getId(), Service.Dns, Provider.VirtualRouter);
        boolean dhcpProvided = _networkMgr.isProviderSupportServiceInNetwork(network.getId(), Service.Dhcp, Provider.VirtualRouter);
        /* If virtual router didn't provide DNS service but provide DHCP service, we need to override the DHCP response to return DNS server rather than 
         * virtual router itself. */
        if (dnsProvided || dhcpProvided) {
            buf.append(" dns1=").append(defaultDns1);
            if (defaultDns2 != null) {
                buf.append(" dns2=").append(defaultDns2);
            }

            boolean useExtDns = !dnsProvided;
            /* For backward compatibility */
            String use_external_dns =  _configDao.getValue(Config.UseExternalDnsServers.key());
            if (use_external_dns != null && use_external_dns.equals("true")) {
                useExtDns = true;
            }

            if (useExtDns) {
                buf.append(" useextdns=true");
            }
        }

        if(profile.getHypervisorType() == HypervisorType.VMware) {
            buf.append(" extra_pubnics=" + _routerExtraPublicNics);
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
        DataCenterVO dcVo = _dcDao.findById(router.getDataCenterIdToDeployIn());

        NicProfile controlNic = null;

        if(profile.getHypervisorType() == HypervisorType.VMware && dcVo.getNetworkType() == NetworkType.Basic) {
            // TODO this is a ugly to test hypervisor type here
            // for basic network mode, we will use the guest NIC for control NIC
            for (NicProfile nic : profile.getNics()) {
                if (nic.getTrafficType() == TrafficType.Guest && nic.getIp4Address() != null) {
                    controlNic = nic;
                }
            }
        } else {
            for (NicProfile nic : profile.getNics()) {
                if (nic.getTrafficType() == TrafficType.Control && nic.getIp4Address() != null) {
                    controlNic = nic;
                }
            }
        }

        if (controlNic == null) {
            s_logger.error("Control network doesn't exist for the router " + router);
            return false;
        }

        cmds.addCommand("checkSsh", new CheckSshCommand(profile.getInstanceName(), controlNic.getIp4Address(), 3922));

        // Update router template/scripts version
        final GetDomRVersionCmd command = new GetDomRVersionCmd();
        command.setAccessDetail(NetworkElementCommand.ROUTER_IP, controlNic.getIp4Address());
        command.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        cmds.addCommand("getDomRVersion", command);

        // Network usage command to create iptables rules
        cmds.addCommand("networkUsage", new NetworkUsageCommand(controlNic.getIp4Address(), router.getHostName(), "create"));

        // restart network if restartNetwork = false is not specified in profile parameters
        boolean reprogramNetwork = true;
        if (profile.getParameter(Param.ReProgramNetwork) != null && (Boolean) profile.getParameter(Param.ReProgramNetwork) == false) {
            reprogramNetwork = false;
        }

        VirtualRouterProvider vrProvider = _vrProviderDao.findById(router.getElementId());
        if (vrProvider == null) {
            throw new CloudRuntimeException("Cannot find related virtual router provider of router: " + router.getHostName());
        }
        Provider provider = Network.Provider.getProvider(vrProvider.getType().toString());
        if (provider == null) {
            throw new CloudRuntimeException("Cannot find related provider of virtual router provider: " + vrProvider.getType().toString());
        }

        if (reprogramNetwork) {
            s_logger.debug("Resending ipAssoc, port forwarding, load balancing rules as a part of Virtual router start");
            long networkId = router.getNetworkId();
            long ownerId = router.getAccountId();
            long zoneId = router.getDataCenterIdToDeployIn();

            final List<IPAddressVO> userIps = _networkMgr.listPublicIpAddressesInVirtualNetwork(ownerId, zoneId, null, networkId);
            List<PublicIp> allPublicIps = new ArrayList<PublicIp>();
            if (userIps != null && !userIps.isEmpty()) {
                for (IPAddressVO userIp : userIps) {
                    PublicIp publicIp = new PublicIp(userIp, _vlanDao.findById(userIp.getVlanId()), NetUtils.createSequenceBasedMacAddress(userIp.getMacAddress()));
                    allPublicIps.add(publicIp);
                }
            }
            
            //Get public Ips that should be handled by router
            Network network = _networkDao.findById(networkId);
            Map<PublicIp, Set<Service>> ipToServices = _networkMgr.getIpToServices(allPublicIps, false, false);
            Map<Provider, ArrayList<PublicIp>> providerToIpList = _networkMgr.getProviderToIpList(network, ipToServices);
            // Only cover virtual router for now, if ELB use it this need to be modified
            ArrayList<PublicIp> publicIps = providerToIpList.get(Provider.VirtualRouter);

            s_logger.debug("Found " + publicIps.size() + " ip(s) to apply as a part of domR " + router + " start.");

            if (!publicIps.isEmpty()) {

                List<RemoteAccessVpn> vpns = new ArrayList<RemoteAccessVpn>();
                List<PortForwardingRule> pfRules = new ArrayList<PortForwardingRule>();
                List<FirewallRule> staticNatFirewallRules = new ArrayList<FirewallRule>();
                List<StaticNat> staticNats = new ArrayList<StaticNat>();
                List<FirewallRule> firewallRules = new ArrayList<FirewallRule>();

                // Re-apply public ip addresses - should come before PF/LB/VPN
                if (_networkMgr.isProviderSupportServiceInNetwork(router.getNetworkId(), Service.Firewall, provider)) {
                    createAssociateIPCommands(router, publicIps, cmds, 0);
                }

                //Get information about all the rules (StaticNats and StaticNatRules; PFVPN to reapply on domR start)
                for (PublicIp ip : publicIps) {
                    if (_networkMgr.isProviderSupportServiceInNetwork(router.getNetworkId(), Service.PortForwarding, provider)) {
                        pfRules.addAll(_pfRulesDao.listForApplication(ip.getId()));
                    }
                    if (_networkMgr.isProviderSupportServiceInNetwork(router.getNetworkId(), Service.StaticNat, provider)) {
                        staticNatFirewallRules.addAll(_rulesDao.listByIpAndPurpose(ip.getId(), Purpose.StaticNat));
                    }
                    if (_networkMgr.isProviderSupportServiceInNetwork(router.getNetworkId(), Service.Firewall, provider)) {
                        firewallRules.addAll(_rulesDao.listByIpAndPurpose(ip.getId(), Purpose.Firewall));
                    }

                    if (_networkMgr.isProviderSupportServiceInNetwork(router.getNetworkId(), Service.Vpn, provider)) {
                        RemoteAccessVpn vpn = _vpnDao.findById(ip.getId());
                        if (vpn != null) {
                            vpns.add(vpn);
                        }
                    }

                    if (_networkMgr.isProviderSupportServiceInNetwork(router.getNetworkId(), Service.StaticNat, provider)) {
                        if (ip.isOneToOneNat()) {
                            String dstIp = _networkMgr.getIpInNetwork(ip.getAssociatedWithVmId(), networkId);
                            StaticNatImpl staticNat = new StaticNatImpl(ip.getAccountId(), ip.getDomainId(), networkId, ip.getId(), dstIp, false);
                            staticNats.add(staticNat);
                        }
                    }
                }

                //Re-apply static nats
                s_logger.debug("Found " + staticNats.size() + " static nat(s) to apply as a part of domR " + router + " start.");
                if (!staticNats.isEmpty()) {
                    createApplyStaticNatCommands(staticNats, router, cmds);
                }

                //Re-apply firewall rules
                s_logger.debug("Found " + staticNats.size() + " firewall rule(s) to apply as a part of domR " + router + " start.");
                if (!firewallRules.isEmpty()) {
                    createFirewallRulesCommands(firewallRules, router, cmds);
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
                        staticNatRules.add(_rulesMgr.buildStaticNatRule(rule, false));
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

                List<LoadBalancerVO> lbs = _loadBalancerDao.listByNetworkId(networkId);
                List<LoadBalancingRule> lbRules = new ArrayList<LoadBalancingRule>();
                if (_networkMgr.isProviderSupportServiceInNetwork(router.getNetworkId(), Service.Lb, provider)) {
                    // Re-apply load balancing rules
                    for (LoadBalancerVO lb : lbs) {
                        List<LbDestination> dstList = _lbMgr.getExistingDestinations(lb.getId());
                        List<LbStickinessPolicy> policyList = _lbMgr.getStickinessPolicies(lb.getId());
                        LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList, policyList);
                        lbRules.add(loadBalancing);
                    }
                }

                s_logger.debug("Found " + lbRules.size() + " load balancing rule(s) to apply as a part of domR " + router + " start.");
                if (!lbRules.isEmpty()) {
                    createApplyLoadBalancingRulesCommands(lbRules, router, cmds);
                }
            }
        }

        if (_networkMgr.isProviderSupportServiceInNetwork(router.getNetworkId(), Service.Dhcp, provider)) {
            // Resend dhcp
            s_logger.debug("Reapplying dhcp entries as a part of domR " + router + " start...");
            createDhcpEntryCommandsForVMs(router, cmds);
        }

        if (_networkMgr.isProviderSupportServiceInNetwork(router.getNetworkId(), Service.UserData, provider)) {
            // Resend user data
            s_logger.debug("Reapplying vm data (userData and metaData) entries as a part of domR " + router + " start...");
            createVmDataCommandForVMs(router, cmds);
        }

        return true;
    }

    @Override
    public boolean finalizeStart(VirtualMachineProfile<DomainRouterVO> profile, long hostId, Commands cmds, ReservationContext context) {
        DomainRouterVO router = profile.getVirtualMachine();
        boolean result = true;

        Answer answer = cmds.getAnswer("checkSsh");
        if (answer != null && answer instanceof CheckSshAnswer) {
            CheckSshAnswer sshAnswer = (CheckSshAnswer) answer;
            if (sshAnswer == null || !sshAnswer.getResult()) {
                s_logger.warn("Unable to ssh to the VM: " + sshAnswer.getDetails());
                result = false;
            }
        } else {
            result = false;
        }
        if (result == false) {
            return false;
        }
        answer = cmds.getAnswer("getDomRVersion");
        if (answer != null && answer instanceof GetDomRVersionAnswer) {
            GetDomRVersionAnswer versionAnswer = (GetDomRVersionAnswer)answer;
            if (answer == null || !answer.getResult()) {
                /* Try to push on because it's not a critical error */
                s_logger.warn("Unable to get the template/scripts version of router " + router.getInstanceName() + " due to: " + versionAnswer.getDetails() + ", but we would continue");
            } else {
                router.setTemplateVersion(versionAnswer.getTemplateVersion());
                router.setScriptsVersion(versionAnswer.getScriptsVersion());
                router = _routerDao.persist(router);
            }
        }

        return result;
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
    public boolean startRemoteAccessVpn(Network network, RemoteAccessVpn vpn, List<? extends VirtualRouter> routers) throws ResourceUnavailableException {
        if (routers == null || routers.isEmpty()) {
            s_logger.warn("Failed to start remote access VPN: no router found for account and zone");
            throw new ResourceUnavailableException("Failed to start remote access VPN: no router found for account and zone", DataCenter.class, network.getDataCenterId());
        }

        for (VirtualRouter router : routers) {
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
                s_logger.error("Unable to start vpn: unable add users to vpn in zone " + router.getDataCenterIdToDeployIn() + " for account " + vpn.getAccountId() + " on domR: " + router.getInstanceName()
                        + " due to " + answer.getDetails());
                throw new ResourceUnavailableException("Unable to start vpn: Unable to add users to vpn in zone " + router.getDataCenterIdToDeployIn() + " for account " + vpn.getAccountId() + " on domR: "
                        + router.getInstanceName() + " due to " + answer.getDetails(), DataCenter.class, router.getDataCenterIdToDeployIn());
            }
            answer = cmds.getAnswer("startVpn");
            if (!answer.getResult()) {
                s_logger.error("Unable to start vpn in zone " + router.getDataCenterIdToDeployIn() + " for account " + vpn.getAccountId() + " on domR: " + router.getInstanceName() + " due to "
                        + answer.getDetails());
                throw new ResourceUnavailableException("Unable to start vpn in zone " + router.getDataCenterIdToDeployIn() + " for account " + vpn.getAccountId() + " on domR: " + router.getInstanceName()
                        + " due to " + answer.getDetails(), DataCenter.class, router.getDataCenterIdToDeployIn());
            }

        }
        return true;
    }


    @Override
    public boolean deleteRemoteAccessVpn(Network network, RemoteAccessVpn vpn, List<? extends VirtualRouter> routers) throws ResourceUnavailableException {
        if (routers == null || routers.isEmpty()) {
            s_logger.warn("Failed to delete remote access VPN: no router found for account and zone");
            throw new ResourceUnavailableException("Failed to delete remote access VPN", DataCenter.class, network.getDataCenterId());
        }

        boolean result = true;
        for (VirtualRouter router : routers) {
            if (router.getState() == State.Running) {
                Commands cmds = new Commands(OnError.Continue);
                IpAddress ip = _networkMgr.getIp(vpn.getServerAddressId());

                RemoteAccessVpnCfgCommand removeVpnCmd = new RemoteAccessVpnCfgCommand(false, ip.getAddress().addr(), vpn.getLocalIp(), vpn.getIpRange(), vpn.getIpsecPresharedKey());
                removeVpnCmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
                removeVpnCmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, router.getGuestIpAddress());
                removeVpnCmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());

                DataCenterVO dcVo = _dcDao.findById(router.getDataCenterIdToDeployIn());
                removeVpnCmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

                cmds.addCommand(removeVpnCmd);

                result = result && sendCommandsToRouter(router, cmds);
            } else if (router.getState() == State.Stopped) {
                s_logger.debug("Router " + router + " is in Stopped state, not sending deleteRemoteAccessVpn command to it");
                continue;
            } else {
                s_logger.warn("Failed to delete remote access VPN: domR " + router + " is not in right state " + router.getState());
                throw new ResourceUnavailableException("Failed to delete remote access VPN: domR is not in right state " + router.getState(), DataCenter.class, network.getDataCenterId());
            }
        }

        return result;
    }


    private DomainRouterVO start(DomainRouterVO router, User user, Account caller, Map<Param, Object> params, DeploymentPlan planToDeploy) throws StorageUnavailableException, InsufficientCapacityException,
    ConcurrentOperationException, ResourceUnavailableException {
        s_logger.debug("Starting router " + router);
        if (_itMgr.start(router, params, user, caller, planToDeploy) != null) {
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
    public boolean applyDhcpEntry(Network network, final NicProfile nic, VirtualMachineProfile<UserVm> profile, DeployDestination dest, List<DomainRouterVO> routers)
            throws ResourceUnavailableException {
        _userVmDao.loadDetails((UserVmVO) profile.getVirtualMachine());
        
        final VirtualMachineProfile<UserVm> updatedProfile = profile;
        final boolean isZoneBasic = (dest.getDataCenter().getNetworkType() == NetworkType.Basic);
        final Long podId = isZoneBasic ? dest.getPod().getId() : null;
        
        boolean podLevelException = false;
        //for user vm in Basic zone we should try to re-deploy vm in a diff pod if it fails to deploy in original pod; so throwing exception with Pod scope
        if (isZoneBasic && podId != null && updatedProfile.getVirtualMachine().getType() == VirtualMachine.Type.User && network.getTrafficType() == TrafficType.Guest && network.getGuestType() == Network.GuestType.Shared) {
            podLevelException = true;
        }
        
        return applyRules(network, routers, "dhcp entry", podLevelException, podId, new RuleApplier() {
            @Override
            public boolean execute(Network network, VirtualRouter router) throws ResourceUnavailableException {
                //for basic zone, send dhcp/dns information to all routers in the basic network only when _dnsBasicZoneUpdates is set to "all" value
                Commands cmds = new Commands(OnError.Stop);
                if (!(isZoneBasic && router.getPodIdToDeployIn().longValue() != podId.longValue() && _dnsBasicZoneUpdates.equalsIgnoreCase("pod"))) {
                    NicVO nicVo = _nicDao.findById(nic.getId());
                    createDhcpEntryCommand(router, updatedProfile.getVirtualMachine(), nicVo, cmds);
                    return sendCommandsToRouter(router, cmds);
                }
                return true;
            }
        });
    }

	private String findDefaultDnsIp(long userVmId) {
		NicVO defaultNic = _nicDao.findDefaultNicForVM(userVmId);
		
		//check if DNS provider is the domR
		if (!_networkMgr.isProviderSupportServiceInNetwork(defaultNic.getNetworkId(), Service.Dns, Provider.VirtualRouter)) {
			return null;
		}
		
		NetworkOfferingVO offering = _networkOfferingDao.findById(_networkDao.findById(defaultNic.getNetworkId()).getNetworkOfferingId());
		if (offering.getRedundantRouter()) {
		    return findGatewayIp(userVmId);
		}
		
		//find domR's nic in the network
		NicVO domrDefaultNic = _nicDao.findByNetworkIdAndType(defaultNic.getNetworkId(), VirtualMachine.Type.DomainRouter);
		return domrDefaultNic.getIp4Address();
	}
	
	private String findGatewayIp(long userVmId) {
		NicVO defaultNic = _nicDao.findDefaultNicForVM(userVmId);
		return defaultNic.getGateway();
	}

    @Override
    public boolean applyUserData(Network network, final NicProfile nic, VirtualMachineProfile<UserVm> profile, DeployDestination dest, List<DomainRouterVO> routers)
            throws ResourceUnavailableException {
        _userVmDao.loadDetails((UserVmVO) profile.getVirtualMachine());
        
        final VirtualMachineProfile<UserVm> updatedProfile = profile;
        final boolean isZoneBasic = (dest.getDataCenter().getNetworkType() == NetworkType.Basic);
        final Long podId = isZoneBasic ? dest.getPod().getId() : null;
        
        boolean podLevelException = false;
        //for user vm in Basic zone we should try to re-deploy vm in a diff pod if it fails to deploy in original pod; so throwing exception with Pod scope
        if (isZoneBasic && podId != null && updatedProfile.getVirtualMachine().getType() == VirtualMachine.Type.User && network.getTrafficType() == TrafficType.Guest && network.getGuestType() == Network.GuestType.Shared) {
            podLevelException = true;
        }
        
        return applyRules(network, routers, "userdata and password entry", podLevelException, podId, new RuleApplier() {
            @Override
            public boolean execute(Network network, VirtualRouter router) throws ResourceUnavailableException {
                //for basic zone, send vm data/password information only to the router in the same pod
                Commands cmds = new Commands(OnError.Stop);
                if (!(isZoneBasic && router.getPodIdToDeployIn().longValue() != podId.longValue())) {
                    NicVO nicVo = _nicDao.findById(nic.getId());
                    createPasswordCommand(router, updatedProfile, nicVo, cmds);
                    createVmDataCommand(router, updatedProfile.getVirtualMachine(), nicVo, updatedProfile.getVirtualMachine().getDetail("SSH.PublicKey"), cmds);
                    return sendCommandsToRouter(router, cmds);
                }
                return true;
            }
        });
    }

    @Override
    public DomainRouterVO persist(DomainRouterVO router) {
    	DomainRouterVO virtualRouter =  _routerDao.persist(router);
        // Creating stats entry for router
        UserStatisticsVO stats = _userStatsDao.findBy(virtualRouter.getAccountId(), virtualRouter.getDataCenterIdToDeployIn(), router.getNetworkId(), null, router.getId(), router.getType().toString());
        if (stats == null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Creating user statistics for the account: " + virtualRouter.getAccountId() + " Router Id: " + router.getId());
            }
            stats = new UserStatisticsVO(virtualRouter.getAccountId(), virtualRouter.getDataCenterIdToDeployIn(), null, router.getId(), router.getType().toString(), router.getNetworkId());
            _userStatsDao.persist(stats);
        }
        return virtualRouter;
    }

    @Override
    //FIXME add partial success and STOP state support
    public String[] applyVpnUsers(Network network, List<? extends VpnUser> users, List<DomainRouterVO> routers) throws ResourceUnavailableException {
        if (routers == null || routers.isEmpty()) {
            s_logger.warn("Failed to add/remove VPN users: no router found for account and zone");
            throw new ResourceUnavailableException("Unable to assign ip addresses, domR doesn't exist for network " + network.getId(), DataCenter.class, network.getDataCenterId());
        }

        boolean agentResults = true;

        for (DomainRouterVO router : routers) {
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
            cmd.setAccessDetail(NetworkElementCommand.ACCOUNT_ID, String.valueOf(router.getAccountId()));
            cmd.setAccessDetail(NetworkElementCommand.GUEST_NETWORK_CIDR, network.getCidr());
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, router.getGuestIpAddress());
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
            DataCenterVO dcVo = _dcDao.findById(router.getDataCenterIdToDeployIn());
            cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

            cmds.addCommand(cmd);


            // Currently we receive just one answer from the agent. In the future we have to parse individual answers and set
            // results accordingly
            boolean agentResult = sendCommandsToRouter(router, cmds);
            agentResults = agentResults && agentResult;
        }

        String[] result = new String[users.size()];
        for (int i = 0; i < result.length; i++) {
            if (agentResults) {
                result[i] = null;
            } else {
                result[i] = String.valueOf(agentResults);
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

    @Override @ActionEvent(eventType = EventTypes.EVENT_ROUTER_START, eventDescription = "starting router Vm", async = true)
    public VirtualRouter startRouter(long id) throws ResourceUnavailableException, InsufficientCapacityException, ConcurrentOperationException{
        return startRouter(id, true);
    }

    @Override
    public VirtualRouter startRouter(long routerId, boolean reprogramNetwork) throws ResourceUnavailableException, InsufficientCapacityException, ConcurrentOperationException {
        Account caller = UserContext.current().getCaller();
        User callerUser = _accountMgr.getActiveUser(UserContext.current().getCallerUserId());

        // verify parameters
        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            throw new InvalidParameterValueException("Unable to find router by id " + routerId + ".");
        }
        _accountMgr.checkAccess(caller, null, true, router);

        Account owner = _accountMgr.getAccount(router.getAccountId());

        // Check if all networks are implemented for the domR; if not - implement them
        DataCenter dc = _dcDao.findById(router.getDataCenterIdToDeployIn());
        HostPodVO pod = null;
        if (router.getPodIdToDeployIn() != null) {
            pod = _podDao.findById(router.getPodIdToDeployIn());
        }
        DeployDestination dest = new DeployDestination(dc, pod, null, null);

        ReservationContext context = new ReservationContextImpl(null, null, callerUser, owner);

        List<NicVO> nics = _nicDao.listByVmId(routerId);

        for (NicVO nic : nics) {
            if (!_networkMgr.startNetwork(nic.getNetworkId(), dest, context)) {
                s_logger.warn("Failed to start network id=" + nic.getNetworkId() + " as a part of domR start");
                throw new CloudRuntimeException("Failed to start network id=" + nic.getNetworkId() + " as a part of domR start");
            }
        }

        UserVO user = _userDao.findById(UserContext.current().getCallerUserId());
        Map<Param, Object> params = new HashMap<Param, Object>();
        if (reprogramNetwork) {
            params.put(Param.ReProgramNetwork, true);
        } else {
            params.put(Param.ReProgramNetwork, false);
        }
        VirtualRouter virtualRouter = startVirtualRouter(router, user, caller, params);
        if(virtualRouter == null){
            throw new CloudRuntimeException("Failed to start router with id " + routerId);
        }
        return virtualRouter;
    }

    private void createAssociateIPCommands(final VirtualRouter router, final List<? extends PublicIpAddress> ips, Commands cmds, long vmId) {

        // Ensure that in multiple vlans case we first send all ip addresses of vlan1, then all ip addresses of vlan2, etc..
        Map<String, ArrayList<PublicIpAddress>> vlanIpMap = new HashMap<String, ArrayList<PublicIpAddress>>();
        for (final PublicIpAddress ipAddress : ips) {
            String vlanTag = ipAddress.getVlanTag();
            ArrayList<PublicIpAddress> ipList = vlanIpMap.get(vlanTag);
            if (ipList == null) {
                ipList = new ArrayList<PublicIpAddress>();
            }
            //domR doesn't support release for sourceNat IP address; so reset the state
            if (ipAddress.isSourceNat() && ipAddress.getState() == IpAddress.State.Releasing) {
            	ipAddress.setState(IpAddress.State.Allocated);
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

            // Get network rate - required for IpAssoc
            Integer networkRate = _networkMgr.getNetworkRate(ipAddrList.get(0).getNetworkId(), router.getId());
            Network network = _networkMgr.getNetwork(ipAddrList.get(0).getNetworkId());

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

                IpAddressTO ip = new IpAddressTO(ipAddr.getAccountId(), ipAddr.getAddress().addr(), add, firstIP, sourceNat, vlanId, vlanGateway, vlanNetmask, vifMacAddress, vmGuestAddress, networkRate, ipAddr.isOneToOneNat());

                ip.setTrafficType(network.getTrafficType());
                ip.setNetworkName(_networkMgr.getNetworkTag(router.getHypervisorType(), network));
                ipsToSend[i++] = ip;
                /* send the firstIP = true for the first Add, this is to create primary on interface*/
                if (!firstIP || add)  {
                    firstIP = false;
                }
            }
            IpAssocCommand cmd = new IpAssocCommand(ipsToSend);
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, router.getGuestIpAddress());
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
            DataCenterVO dcVo = _dcDao.findById(router.getDataCenterIdToDeployIn());
            cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

            cmds.addCommand("IPAssocCommand", cmd);
        }
    }

    private void createApplyPortForwardingRulesCommands(List<? extends PortForwardingRule> rules, VirtualRouter router, Commands cmds) {
        List<PortForwardingRuleTO> rulesTO = null;
        if (rules != null) {
            rulesTO = new ArrayList<PortForwardingRuleTO>();
            for (PortForwardingRule rule : rules) {
                IpAddress sourceIp = _networkMgr.getIp(rule.getSourceIpAddressId());
                PortForwardingRuleTO ruleTO = new PortForwardingRuleTO(rule, null, sourceIp.getAddress().addr());
                rulesTO.add(ruleTO);
            }
        }

        SetPortForwardingRulesCommand cmd = new SetPortForwardingRulesCommand(rulesTO);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, router.getGuestIpAddress());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        DataCenterVO dcVo = _dcDao.findById(router.getDataCenterIdToDeployIn());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

        cmds.addCommand(cmd);
    }

    private void createApplyStaticNatRulesCommands(List<? extends StaticNatRule> rules, VirtualRouter router, Commands cmds) {
        List<StaticNatRuleTO> rulesTO = null;
        if (rules != null) {
            rulesTO = new ArrayList<StaticNatRuleTO>();
            for (StaticNatRule rule : rules) {
                IpAddress sourceIp = _networkMgr.getIp(rule.getSourceIpAddressId());
                StaticNatRuleTO ruleTO = new StaticNatRuleTO(rule, null, sourceIp.getAddress().addr(), rule.getDestIpAddress());
                rulesTO.add(ruleTO);
            }
        }

        SetStaticNatRulesCommand cmd = new SetStaticNatRulesCommand(rulesTO);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, router.getGuestIpAddress());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        DataCenterVO dcVo = _dcDao.findById(router.getDataCenterIdToDeployIn());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        cmds.addCommand(cmd);
    }

    private void createApplyLoadBalancingRulesCommands(List<LoadBalancingRule> rules, VirtualRouter router, Commands cmds) {

        LoadBalancerTO[] lbs = new LoadBalancerTO[rules.size()];
        int i = 0;
        for (LoadBalancingRule rule : rules) {
            boolean revoked = (rule.getState().equals(FirewallRule.State.Revoke));
            String protocol = rule.getProtocol();
            String algorithm = rule.getAlgorithm();

            String srcIp = _networkMgr.getIp(rule.getSourceIpAddressId()).getAddress().addr();
            int srcPort = rule.getSourcePortStart();
            List<LbDestination> destinations = rule.getDestinations();
            List<LbStickinessPolicy> stickinessPolicies = rule.getStickinessPolicies();
            LoadBalancerTO lb = new LoadBalancerTO(srcIp, srcPort, protocol, algorithm, revoked, false, destinations, stickinessPolicies);
            lbs[i++] = lb;
        }
        String RouterPublicIp = null;

        if (router instanceof DomainRouterVO) {
            DomainRouterVO domr = (DomainRouterVO)router;
            RouterPublicIp = domr.getPublicIpAddress();
        }

        LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(lbs,RouterPublicIp, router.getGuestIpAddress(),router.getPrivateIpAddress());

        cmd.lbStatsVisibility = _configDao.getValue(Config.NetworkLBHaproxyStatsVisbility.key());
        cmd.lbStatsUri = _configDao.getValue(Config.NetworkLBHaproxyStatsUri.key());
        cmd.lbStatsAuth = _configDao.getValue(Config.NetworkLBHaproxyStatsAuth.key());
        cmd.lbStatsPort = _configDao.getValue(Config.NetworkLBHaproxyStatsPort.key());


        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, router.getGuestIpAddress());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        DataCenterVO dcVo = _dcDao.findById(router.getDataCenterIdToDeployIn());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        cmds.addCommand(cmd);

    }

    private void createApplyVpnCommands(RemoteAccessVpn vpn, VirtualRouter router, Commands cmds) {
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
        addUsersCmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
        addUsersCmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, router.getGuestIpAddress());
        addUsersCmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());

        IpAddress ip = _networkMgr.getIp(vpn.getServerAddressId());

        RemoteAccessVpnCfgCommand startVpnCmd = new RemoteAccessVpnCfgCommand(true, ip.getAddress().addr(), vpn.getLocalIp(), vpn.getIpRange(), vpn.getIpsecPresharedKey());
        startVpnCmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
        startVpnCmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, router.getGuestIpAddress());
        startVpnCmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        DataCenterVO dcVo = _dcDao.findById(router.getDataCenterIdToDeployIn());
        startVpnCmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

        cmds.addCommand("users", addUsersCmd);
        cmds.addCommand("startVpn", startVpnCmd);
    }
    
    private void createPasswordCommand(VirtualRouter router, VirtualMachineProfile<UserVm> profile, NicVO nic, Commands cmds) {
        String password = (String) profile.getParameter(VirtualMachineProfile.Param.VmPassword);
        DataCenterVO dcVo = _dcDao.findById(router.getDataCenterIdToDeployIn());

        // password should be set only on default network element
        if (password != null && nic.isDefaultNic()) {
            final String encodedPassword = PasswordGenerator.rot13(password);
            SavePasswordCommand cmd = new SavePasswordCommand(encodedPassword, nic.getIp4Address(), profile.getVirtualMachine().getHostName());
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, router.getGuestIpAddress());
            cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
            cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

            cmds.addCommand("password", cmd);
        }
        
    }
    
    private void createVmDataCommand(VirtualRouter router, UserVm vm, NicVO nic, String publicKey, Commands cmds) {
        String serviceOffering = _serviceOfferingDao.findByIdIncludingRemoved(vm.getServiceOfferingId()).getDisplayText();
        String zoneName = _dcDao.findById(router.getDataCenterIdToDeployIn()).getName();
        cmds.addCommand("vmdata",
                generateVmDataCommand(router, nic.getIp4Address(), vm.getUserData(), serviceOffering, zoneName, nic.getIp4Address(),
                        vm.getHostName(), vm.getInstanceName(), vm.getId(), publicKey));
        
    }

    private void createVmDataCommandForVMs(DomainRouterVO router, Commands cmds) {
        long networkId = router.getNetworkId();
        List<UserVmVO> vms = _userVmDao.listByNetworkIdAndStates(networkId, State.Running, State.Migrating, State.Stopping);
        DataCenterVO dc = _dcDao.findById(router.getDataCenterIdToDeployIn());
        for (UserVmVO vm : vms) {
            boolean createVmData = true;
            if (dc.getNetworkType() == NetworkType.Basic && router.getPodIdToDeployIn().longValue() != vm.getPodIdToDeployIn().longValue()) {
                createVmData = false;
            }

            if (createVmData) {
                NicVO nic = _nicDao.findByInstanceIdAndNetworkId(networkId, vm.getId());
                if (nic != null) {
                    s_logger.debug("Creating user data entry for vm " + vm + " on domR " + router);
                    createVmDataCommand(router, vm, nic, null, cmds);
                }
            }
        }
    }
    
    private void createDhcpEntryCommand(VirtualRouter router, UserVm vm, NicVO nic, Commands cmds) {
        DhcpEntryCommand dhcpCommand = new DhcpEntryCommand(nic.getMacAddress(), nic.getIp4Address(), vm.getHostName());
        DataCenterVO dcVo = _dcDao.findById(router.getDataCenterIdToDeployIn());
        String gatewayIp = findGatewayIp(vm.getId());
        if (!gatewayIp.equals(nic.getGateway())) {
            GuestOSVO guestOS = _guestOSDao.findById(vm.getGuestOSId());
            // Don't set dhcp:router option for non-default nic on CentOS/RHEL, because they would set routing on wrong interface
            // This is tricky, we may need to update this when we have more information on various OS's behavior
            if (guestOS.getDisplayName().startsWith("CentOS") || guestOS.getDisplayName().startsWith("Red Hat Enterprise")) {
                gatewayIp = "0.0.0.0";
            }
        }
        dhcpCommand.setDefaultRouter(gatewayIp);
        dhcpCommand.setDefaultDns(findDefaultDnsIp(vm.getId()));

        dhcpCommand.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
        dhcpCommand.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        dhcpCommand.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, router.getGuestIpAddress());
        dhcpCommand.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

        cmds.addCommand("dhcp", dhcpCommand);
    }

    private void createDhcpEntryCommandsForVMs(DomainRouterVO router, Commands cmds) {
        long networkId = router.getNetworkId();
        List<UserVmVO> vms = _userVmDao.listByNetworkIdAndStates(networkId, State.Running, State.Migrating, State.Stopping);
        DataCenterVO dc = _dcDao.findById(router.getDataCenterIdToDeployIn());
        for (UserVmVO vm : vms) {
            boolean createDhcp = true;
            if (dc.getNetworkType() == NetworkType.Basic && router.getPodIdToDeployIn().longValue() != vm.getPodIdToDeployIn().longValue() && _dnsBasicZoneUpdates.equalsIgnoreCase("pod")) {
                createDhcp = false;
            }
            if (createDhcp) {
                NicVO nic = _nicDao.findByInstanceIdAndNetworkId(networkId, vm.getId());
                if (nic != null) {
                    s_logger.debug("Creating dhcp entry for vm " + vm + " on domR " + router + ".");
                    createDhcpEntryCommand(router, vm, nic, cmds);
                }
            }
        }
    }

    private boolean sendCommandsToRouter(final VirtualRouter router, Commands cmds) throws AgentUnavailableException {
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
        boolean result = true;
        if (answers.length > 0) {
            for (Answer answer : answers) {
                if (!answer.getResult()) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    protected void handleSingleWorkingRedundantRouter(List<? extends VirtualRouter> connectedRouters, List<? extends VirtualRouter> disconnectedRouters, String reason) throws ResourceUnavailableException
    {
        if (connectedRouters.isEmpty() || disconnectedRouters.isEmpty()) {
            return;
        }
        if (connectedRouters.size() != 1 || disconnectedRouters.size() != 1) {
            s_logger.warn("How many redundant routers do we have?? ");
            return;
        }
        if (!connectedRouters.get(0).getIsRedundantRouter()) {
            throw new ResourceUnavailableException("Who is calling this with non-redundant router or non-domain router?", DataCenter.class, connectedRouters.get(0).getDataCenterIdToDeployIn());
        }
        if (!disconnectedRouters.get(0).getIsRedundantRouter()) {
            throw new ResourceUnavailableException("Who is calling this with non-redundant router or non-domain router?", DataCenter.class, disconnectedRouters.get(0).getDataCenterIdToDeployIn());
        }

        DomainRouterVO connectedRouter = (DomainRouterVO)connectedRouters.get(0);
        DomainRouterVO disconnectedRouter = (DomainRouterVO)disconnectedRouters.get(0);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("About to stop the router " + disconnectedRouter.getInstanceName() + " due to: " + reason);
        }
        String title = "Virtual router " + disconnectedRouter.getInstanceName() + " would be stopped after connecting back, due to " + reason;
        String context =  "Virtual router (name: " + disconnectedRouter.getInstanceName() + ", id: " + disconnectedRouter.getId() + ") would be stopped after connecting back, due to: " + reason;
        _alertMgr.sendAlert(AlertManager.ALERT_TYPE_DOMAIN_ROUTER,
                disconnectedRouter.getDataCenterIdToDeployIn(), disconnectedRouter.getPodIdToDeployIn(), title, context);
        disconnectedRouter.setStopPending(true);
        disconnectedRouter = _routerDao.persist(disconnectedRouter);

        int connRouterPR = getRealPriority(connectedRouter);
        int disconnRouterPR = getRealPriority(disconnectedRouter);
        if (connRouterPR < disconnRouterPR) {
            //connRouterPR < disconnRouterPR, they won't equal at anytime
            if (!connectedRouter.getIsPriorityBumpUp()) {
                final BumpUpPriorityCommand command = new BumpUpPriorityCommand();
                command.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(connectedRouter.getId()));
                command.setAccessDetail(NetworkElementCommand.ROUTER_NAME, connectedRouter.getInstanceName());
                final Answer answer = _agentMgr.easySend(connectedRouter.getHostId(), command);
                if (!answer.getResult()) {
                    s_logger.error("Failed to bump up " + connectedRouter.getInstanceName() + "'s priority! " + answer.getDetails());
                }
            } else {
                String t = "Can't bump up virtual router " + connectedRouter.getInstanceName() + "'s priority due to it's already bumped up!";
                _alertMgr.sendAlert(AlertManager.ALERT_TYPE_DOMAIN_ROUTER,
                        connectedRouter.getDataCenterIdToDeployIn(), connectedRouter.getPodIdToDeployIn(), t, t);
            }
        }
    }

    @Override
    public boolean associateIP(Network network, final List<? extends PublicIpAddress> ipAddress, List<? extends VirtualRouter> routers) throws ResourceUnavailableException {
        if (ipAddress == null || ipAddress.isEmpty()) {
            s_logger.debug("No ip association rules to be applied for network " + network.getId());
            return true;
        }
        return applyRules(network, routers, "ip association", false, null, new RuleApplier() {
            @Override
            public boolean execute(Network network, VirtualRouter router) throws ResourceUnavailableException {
                Commands cmds = new Commands(OnError.Continue);
                createAssociateIPCommands(router, ipAddress, cmds, 0);
                return sendCommandsToRouter(router, cmds);
            }
        });
    }

    @Override
    public boolean applyFirewallRules(Network network, final List<? extends FirewallRule> rules, List<? extends VirtualRouter> routers) throws ResourceUnavailableException {
        if (rules == null || rules.isEmpty()) {
            s_logger.debug("No firewall rules to be applied for network " + network.getId());
            return true;
        }
        return applyRules(network, routers, "firewall rules", false, null, new RuleApplier() {
            @Override
            public boolean execute(Network network, VirtualRouter router) throws ResourceUnavailableException {
                if (rules.get(0).getPurpose() == Purpose.LoadBalancing) {
                    // for load balancer we have to resend all lb rules for the network
                    List<LoadBalancerVO> lbs = _loadBalancerDao.listByNetworkId(network.getId());
                    List<LoadBalancingRule> lbRules = new ArrayList<LoadBalancingRule>();
                    for (LoadBalancerVO lb : lbs) {
                        List<LbDestination> dstList = _lbMgr.getExistingDestinations(lb.getId());
                        List<LbStickinessPolicy> policyList = _lbMgr.getStickinessPolicies(lb.getId());
                        LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList,policyList);
                        lbRules.add(loadBalancing);
                    }
                    return sendLBRules(router, lbRules);
                } else if (rules.get(0).getPurpose() == Purpose.PortForwarding) {
                    return sendPortForwardingRules(router, (List<PortForwardingRule>) rules);
                } else if (rules.get(0).getPurpose() == Purpose.StaticNat) {
                    return sendStaticNatRules(router, (List<StaticNatRule>) rules);
                } else if (rules.get(0).getPurpose() == Purpose.Firewall) {
                    return sendFirewallRules(router, (List<FirewallRule>) rules);
                } else {
                    s_logger.warn("Unable to apply rules of purpose: " + rules.get(0).getPurpose());
                    return false;
                }
            }
        });
    }

    protected boolean sendLBRules(VirtualRouter router, List<LoadBalancingRule> rules) throws ResourceUnavailableException {
        Commands cmds = new Commands(OnError.Continue);
        createApplyLoadBalancingRulesCommands(rules, router, cmds);
        return sendCommandsToRouter(router, cmds);
    }

    protected boolean sendPortForwardingRules(VirtualRouter router, List<PortForwardingRule> rules) throws ResourceUnavailableException {
        Commands cmds = new Commands(OnError.Continue);
        createApplyPortForwardingRulesCommands(rules, router, cmds);
        return sendCommandsToRouter(router, cmds);
    }

    protected boolean sendStaticNatRules(VirtualRouter router, List<StaticNatRule> rules) throws ResourceUnavailableException {
        Commands cmds = new Commands(OnError.Continue);
        createApplyStaticNatRulesCommands(rules, router, cmds);
        return sendCommandsToRouter(router, cmds);
    }

    @Override
    public List<VirtualRouter> getRoutersForNetwork(long networkId) {
        List<DomainRouterVO> routers = _routerDao.findByNetwork(networkId);
        List<VirtualRouter> vrs = new ArrayList<VirtualRouter>(routers.size());
        for (DomainRouterVO router : routers) {
            vrs.add(router);
        }
        return vrs;
    }

    private void createFirewallRulesCommands(List<? extends FirewallRule> rules, VirtualRouter router, Commands cmds) {
        List<FirewallRuleTO> rulesTO = null;
        if (rules != null) {
            rulesTO = new ArrayList<FirewallRuleTO>();
            for (FirewallRule rule : rules) {
                IpAddress sourceIp = _networkMgr.getIp(rule.getSourceIpAddressId());
                FirewallRuleTO ruleTO = new FirewallRuleTO(rule, null, sourceIp.getAddress().addr());
                rulesTO.add(ruleTO);
            }
        }

        SetFirewallRulesCommand cmd = new SetFirewallRulesCommand(rulesTO);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, router.getGuestIpAddress());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        DataCenterVO dcVo = _dcDao.findById(router.getDataCenterIdToDeployIn());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        cmds.addCommand(cmd);
    }


    protected boolean sendFirewallRules(VirtualRouter router, List<FirewallRule> rules) throws ResourceUnavailableException {
        Commands cmds = new Commands(OnError.Continue);
        createFirewallRulesCommands(rules, router, cmds);
        return sendCommandsToRouter(router, cmds);
    }

    @Override
    public String getDnsBasicZoneUpdate() {
        return _dnsBasicZoneUpdates;
    }
    
    private interface RuleApplier {
        boolean execute(Network network, VirtualRouter router) throws ResourceUnavailableException;
    }
    
    private boolean applyRules(Network network, List<? extends VirtualRouter> routers, String typeString, boolean isPodLevelException, Long podId, RuleApplier applier) throws ResourceUnavailableException {
        if (routers == null || routers.isEmpty()) {
            s_logger.warn("Unable to apply " + typeString + ", virtual router doesn't exist in the network " + network.getId());
            throw new ResourceUnavailableException("Unable to apply " + typeString , DataCenter.class, network.getDataCenterId());
        }

        DataCenter dc = _dcDao.findById(network.getDataCenterId());
        boolean isZoneBasic = (dc.getNetworkType() == NetworkType.Basic);
        
        // isPodLevelException and podId is only used for basic zone
        assert !((!isZoneBasic && isPodLevelException) || (isZoneBasic && isPodLevelException && podId == null));
        
        List<VirtualRouter> connectedRouters = new ArrayList<VirtualRouter>();
        List<VirtualRouter> disconnectedRouters = new ArrayList<VirtualRouter>();
        boolean result = true;
        String msg = "Unable to apply " + typeString + " on disconnected router ";
        for (VirtualRouter router : routers) {
            if (router.getState() == State.Running) {
                s_logger.debug("Applying " + typeString + " in network " + network);

                if (router.isStopPending()) {
                    if (_hostDao.findById(router.getHostId()).getStatus() == Status.Up) {
                        throw new ResourceUnavailableException("Unable to process due to the stop pending router " + router.getInstanceName() + " haven't been stopped after it's host coming back!",
                                DataCenter.class, router.getDataCenterIdToDeployIn());
                    }
                    s_logger.debug("Router " + router.getInstanceName() + " is stop pending, so not sending apply " + typeString + " commands to the backend");
                    continue;
                }
                try {
                    result = applier.execute(network, router);
                    connectedRouters.add(router);
                } catch (AgentUnavailableException e) {
                    s_logger.warn(msg + router.getInstanceName(), e);
                    disconnectedRouters.add(router);
                }

                //If rules fail to apply on one domR and not due to disconnection, no need to proceed with the rest
                if (!result) {
                    if (isZoneBasic && isPodLevelException) {
                        throw new ResourceUnavailableException("Unable to apply " + typeString + " on router ", Pod.class, podId);
                    }
                    throw new ResourceUnavailableException("Unable to apply " + typeString + " on router ", DataCenter.class, router.getDataCenterIdToDeployIn());
                }

            } else if (router.getState() == State.Stopped || router.getState() == State.Stopping) {
                s_logger.debug("Router " + router.getInstanceName() + " is in " + router.getState() + ", so not sending apply " + typeString + " commands to the backend");
            } else {
                s_logger.warn("Unable to apply " + typeString +", virtual router is not in the right state " + router.getState());
                if (isZoneBasic && isPodLevelException) {
                    throw new ResourceUnavailableException("Unable to apply " + typeString + ", virtual router is not in the right state", Pod.class, podId);
                }
                throw new ResourceUnavailableException("Unable to apply " + typeString + ", virtual router is not in the right state", DataCenter.class, router.getDataCenterIdToDeployIn());
            }
        }

        if (!connectedRouters.isEmpty()) {
            if (!isZoneBasic && !disconnectedRouters.isEmpty() && disconnectedRouters.get(0).getIsRedundantRouter()) {
                // These disconnected redundant virtual routers are out of sync now, stop them for synchronization
                handleSingleWorkingRedundantRouter(connectedRouters, disconnectedRouters, msg);
            }
        } else if (!disconnectedRouters.isEmpty()) {
            for (VirtualRouter router : disconnectedRouters) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(msg + router.getInstanceName() + "(" + router.getId() + ")");
                }
            }
            if (isZoneBasic && isPodLevelException) {
                throw new ResourceUnavailableException(msg, Pod.class, podId);
            }
            throw new ResourceUnavailableException(msg, DataCenter.class, disconnectedRouters.get(0).getDataCenterIdToDeployIn());
        }

        return !connectedRouters.isEmpty();
    }

    @Override
    public boolean applyStaticNats(Network network, final List<? extends StaticNat> rules, List<? extends VirtualRouter> routers) throws ResourceUnavailableException {
        if (rules == null || rules.isEmpty()) {
            s_logger.debug("No static nat rules to be applied for network " + network.getId());
            return true;
        }
        return applyRules(network, routers, "static nat rules", false, null, new RuleApplier() {
            @Override
            public boolean execute(Network network, VirtualRouter router) throws ResourceUnavailableException {
                return applyStaticNat(router, rules);
            }
        });
    }


    protected boolean applyStaticNat(VirtualRouter router, List<? extends StaticNat> rules) throws ResourceUnavailableException {
        Commands cmds = new Commands(OnError.Continue);
        createApplyStaticNatCommands(rules, router, cmds);
        return sendCommandsToRouter(router, cmds);
    }

    private void createApplyStaticNatCommands(List<? extends StaticNat> rules, VirtualRouter router, Commands cmds) {
        List<StaticNatRuleTO> rulesTO = null;
        if (rules != null) {
            rulesTO = new ArrayList<StaticNatRuleTO>();
            for (StaticNat rule : rules) {
                IpAddress sourceIp = _networkMgr.getIp(rule.getSourceIpAddressId());
                StaticNatRuleTO ruleTO = new StaticNatRuleTO(0, sourceIp.getAddress().addr(), null, null, rule.getDestIpAddress(), null, null, null, rule.isForRevoke(), false);
                rulesTO.add(ruleTO);
            }
        }

        SetStaticNatRulesCommand cmd = new SetStaticNatRulesCommand(rulesTO);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getRouterControlIp(router.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, router.getGuestIpAddress());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        DataCenterVO dcVo = _dcDao.findById(router.getDataCenterIdToDeployIn());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        cmds.addCommand(cmd);
    }

    @Override
    public int getTimeout() {
        return -1;
    }

    @Override
    public boolean isRecurring() {
        return false;
    }

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        return false;
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        return false;
    }

    @Override
    public void processConnect(HostVO host, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
        UserContext context = UserContext.current();
        context.setAccountId(1);
        List<DomainRouterVO> routers = _routerDao.listVirtualByHostId(host.getId());
        for (DomainRouterVO router : routers) {
            if (router.isStopPending()) {
                State state = router.getState();
                if (state != State.Stopped && state != State.Destroyed) {
                    try {
                        stopRouter(router.getId(), false);
                    } catch (ResourceUnavailableException e) {
                        s_logger.warn("Fail to stop router " + router.getInstanceName(), e);
                        throw new ConnectionException(false, "Fail to stop router " + router.getInstanceName());
                    } catch (ConcurrentOperationException e) {
                        s_logger.warn("Fail to stop router " + router.getInstanceName(), e);
                        throw new ConnectionException(false, "Fail to stop router " + router.getInstanceName());
                    }
                }
                router.setStopPending(false);
                router = _routerDao.persist(router);
            }
        }
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        return null;
    }

    @Override
    public boolean processDisconnect(long agentId, Status state) {
        return false;
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        return false;
    }

    @Override
    public long getDefaultVirtualRouterServiceOfferingId() {
        if (_offering != null) {
            return _offering.getId();
        }
        return 0;
    }

    private String getRouterControlIp(long routerId) {
        String routerControlIpAddress = null;
        List<NicVO> nics = _nicDao.listByVmId(routerId);
        for (NicVO n : nics) {
            NetworkVO nc = _networkDao.findById(n.getNetworkId());
            if (nc.getTrafficType() == TrafficType.Control) {
                routerControlIpAddress = n.getIp4Address();
            }
        }
        
        if(routerControlIpAddress == null) {
            s_logger.warn("Unable to find router's control ip in its attached NICs!. routerId: " + routerId);
            DomainRouterVO router = _routerDao.findById(routerId);
            return router.getPrivateIpAddress();
        }
            
        return routerControlIpAddress;
    }
}
