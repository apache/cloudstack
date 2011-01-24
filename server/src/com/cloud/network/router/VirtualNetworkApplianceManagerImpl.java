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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ModifySshKeysCommand;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.IPAssocCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.RemoteAccessVpnCfgCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.api.routing.VpnUsersCfgCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.LoadBalancerTO;
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
import com.cloud.ha.HighAvailabilityManager;
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
import com.cloud.network.ovs.GreTunnelException;
import com.cloud.network.ovs.OvsNetworkManager;
import com.cloud.network.ovs.OvsTunnelManager;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.PortForwardingRuleVO;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.StoragePoolDao;
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
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineGuru;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

/**
 * VirtualNetworkApplianceManagerImpl manages the different types of
 * virtual network appliances available in the Cloud Stack.
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
    UserVmDao _vmDao = null;
    @Inject
    ResourceLimitDao _limitDao = null;
    @Inject
    CapacityDao _capacityDao = null;
    @Inject
    AgentManager _agentMgr;
    @Inject
    StorageManager _storageMgr;
    @Inject
    HighAvailabilityManager _haMgr;
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
    StoragePoolDao _storagePoolDao = null;
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
    NetworkDao _networksDao = null;
    @Inject
    NicDao _nicDao;
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
    OvsNetworkManager _ovsNetworkMgr;
    @Inject
    OvsTunnelManager _ovsTunnelMgr;

    long _routerTemplateId = -1;
    int _routerRamSize;
    int _retry = 2;
    String _domain;
    String _instance;
    String _defaultHypervisorType;
    String _mgmt_host;

    int _routerCleanupInterval = 3600;
    int _routerStatsInterval = 300;
    private ServiceOfferingVO _offering;
    String _networkDomain;

    private VMTemplateVO _template;

    ScheduledExecutorService _executor;

    Account _systemAcct;

    @Override
    public DomainRouterVO getRouter(long accountId, long dataCenterId) {
        return _routerDao.findBy(accountId, dataCenterId);
    }

    @Override
    public DomainRouterVO getRouter(String publicIpAddress) {
        return _routerDao.findByPublicIpAddress(publicIpAddress);
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
    public boolean destroyRouter(final long routerId) throws ResourceUnavailableException, ConcurrentOperationException{
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
    public VirtualRouter upgradeRouter(UpgradeRouterCmd cmd) throws InvalidParameterValueException, PermissionDeniedException {
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
            throw new InvalidParameterValueException("Unable to upgrade router " + router.toString() + " in state " + router.getState() + "; make sure the router is stopped and not in an error state before upgrading.");
        }

        ServiceOfferingVO currentServiceOffering = _serviceOfferingDao.findById(router.getServiceOfferingId());

        if (!currentServiceOffering.getGuestIpType().equals(newServiceOffering.getGuestIpType())) {
            throw new InvalidParameterValueException("Can't upgrade router, due to the new network type: " + newServiceOffering.getGuestIpType()
                    + " being different from " + "current network type: " + currentServiceOffering.getGuestIpType());
        }
        if (currentServiceOffering.getUseLocalStorage() != newServiceOffering.getUseLocalStorage()) {
            throw new InvalidParameterValueException("Can't upgrade, due to new local storage status : " + newServiceOffering.getGuestIpType()
                    + " is different from " + "curruent local storage status: " + currentServiceOffering.getUseLocalStorage());
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
    public boolean savePasswordToRouter(final long routerId, final String vmIpAddress, final String password) {

        final DomainRouterVO router = _routerDao.findById(routerId);
        final String routerPrivateIpAddress = router.getPrivateIpAddress();
        final String vmName = router.getName();
        final String encodedPassword = rot13(password);
        final SavePasswordCommand cmdSavePassword = new SavePasswordCommand(encodedPassword, vmIpAddress, routerPrivateIpAddress, vmName);

        if (router != null && router.getHostId() != null) {
            final Answer answer = _agentMgr.easySend(router.getHostId(), cmdSavePassword);
            return (answer != null && answer.getResult());
        } else {
            // either the router doesn't exist or router isn't running at all
            return false;
        }
    }
    
    
    @Override
    public VirtualRouter stopRouter(long routerId) throws ResourceUnavailableException, ConcurrentOperationException {
        UserContext context = UserContext.current();
        Account account = context.getCaller();
        
        // verify parameters
        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            throw new InvalidParameterValueException("Unable to find router by id " + routerId + ".");
        }
        
        _accountMgr.checkAccess(account, router);
        
        UserVO user = _userDao.findById(UserContext.current().getCallerUserId());
        
        return this.stop(router, user, account);
    }
    
    @DB
    public void processStopOrRebootAnswer(final DomainRouterVO router, Answer answer) {
        final Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            final UserStatisticsVO userStats = _userStatsDao.lock(router.getAccountId(), router.getDataCenterId(), null, null);
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
         * final GetVmStatsCommand cmd = new GetVmStatsCommand(router,
         * router.getInstanceName());
         * final Answer answer = _agentMgr.easySend(router.getHostId(), cmd);
         * if (answer == null) {
         * return false;
         * }
         * 
         * final GetVmStatsAnswer stats = (GetVmStatsAnswer)answer;
         * 
         * netStats.putAll(stats.getNetworkStats());
         * diskStats.putAll(stats.getDiskStats());
         */

        return true;
    }

    @Override
    public VirtualRouter rebootRouter(long routerId) throws InvalidParameterValueException, PermissionDeniedException, ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        Account account = UserContext.current().getCaller();

        // verify parameters
        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            throw new InvalidParameterValueException("Unable to find domain router with id " + routerId + ".");
        }

        if ((account != null) && !_domainDao.isChildDomain(account.getDomainId(), router.getDomainId())) {
            throw new PermissionDeniedException("Unable to reboot domain router with id " + routerId + ". Permission denied");
        }
        
        //Can reboot domain router only in Running state
        if (router == null || router.getState() != State.Running) {
            s_logger.warn("Unable to reboot, virtual router is not in the right state " + router.getState());
            throw new ResourceUnavailableException("Unable to reboot domR, it is not in right state " + router.getState(), DataCenter.class, router.getDataCenterId());
        }
        
        s_logger.debug("Stopping and starting router " + router + " as a part of router reboot");
        
        if (stopRouter(routerId) != null) {
            return startRouter(routerId);
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
        _routerRamSize = NumbersUtil.parseInt(configs.get("router.ram.size"), 128);

        String value = configs.get("start.retry");
        _retry = NumbersUtil.parseInt(value, 2);

        _defaultHypervisorType = _configDao.getValue(Config.HypervisorDefaultType.key());

        value = configs.get("router.stats.interval");
        _routerStatsInterval = NumbersUtil.parseInt(value, 300);

        value = configs.get("router.cleanup.interval");
        _routerCleanupInterval = NumbersUtil.parseInt(value, 3600);

        _domain = configs.get("domain");
        if (_domain == null) {
            _domain = "foo.com";
        }

        _instance = configs.get("instance.name");
        if (_instance == null) {
            _instance = "DEFAULT";
        }

        _networkDomain = configs.get("guest.domain.suffix");

        s_logger.info("Router configurations: " + "ramsize=" + _routerRamSize + "; templateId=" + _routerTemplateId);

        final UserStatisticsDao statsDao = locator.getDao(UserStatisticsDao.class);
        if (statsDao == null) {
            throw new ConfigurationException("Unable to get " + UserStatisticsDao.class.getName());
        }

        _agentMgr.registerForHostEvents(new SshKeysDistriMonitor(this, _hostDao, _configDao), true, false, false);
        _haMgr.registerHandler(VirtualMachine.Type.DomainRouter, this);
        _itMgr.registerGuru(VirtualMachine.Type.DomainRouter, this);

        boolean useLocalStorage = Boolean.parseBoolean(configs.get(Config.SystemVMUseLocalStorage.key()));
        _offering = new ServiceOfferingVO("System Offering For Software Router", 1, _routerRamSize, 0, 0, 0, true, null,
                Network.GuestIpType.Virtual, useLocalStorage, true, null, true);
        _offering.setUniqueName("Cloud.Com-SoftwareRouter");
        _offering = _serviceOfferingDao.persistSystemServiceOffering(_offering);
        _template = _templateDao.findRoutingTemplate();
        if (_template == null) {
            s_logger.error("Unable to find system vm template.");
        } else {
            _routerTemplateId = _template.getId();
        }

        _systemAcct = _accountService.getSystemAccount();

        s_logger.info("DomainRouterManager is configured.");

        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        _executor.scheduleAtFixedRate(new RouterCleanupTask(), _routerCleanupInterval, _routerCleanupInterval, TimeUnit.SECONDS);
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
    public Command cleanup(final DomainRouterVO vm, final String vmName) {
        if (vmName != null) {
            return new StopCommand(vm, vmName, VirtualMachineName.getVnet(vmName));
        } else if (vm != null) {
            final DomainRouterVO vo = vm;
            return new StopCommand(vo, vo.getVnet());
        } else {
            throw new CloudRuntimeException("Shouldn't even be here!");
        }
    }

    @Override
    public Long convertToId(final String vmName) {
        if (!VirtualMachineName.isValidRouterName(vmName, _instance)) {
            return null;
        }

        return VirtualMachineName.getRouterId(vmName);
    }

//    @Override
//    public HostVO prepareForMigration(final DomainRouterVO router) throws StorageUnavailableException {
//        final long routerId = router.getId();
//        final boolean mirroredVols = router.isMirroredVols();
//        final DataCenterVO dc = _dcDao.findById(router.getDataCenterId());
//        final HostPodVO pod = _podDao.findById(router.getPodId());
//        final ServiceOfferingVO offering = _serviceOfferingDao.findById(router.getServiceOfferingId());
//        StoragePoolVO sp = _storageMgr.getStoragePoolForVm(router.getId());
//
//        final List<VolumeVO> vols = _volsDao.findCreatedByInstance(routerId);
//
//        final String[] storageIps = new String[2];
//        final VolumeVO vol = vols.get(0);
//        storageIps[0] = vol.getHostIp();
//        if (mirroredVols && (vols.size() == 2)) {
//            storageIps[1] = vols.get(1).getHostIp();
//        }
//
//        final PrepareForMigrationCommand cmd = new PrepareForMigrationCommand(router.getInstanceName(), router.getVnet(), storageIps, vols,
//                mirroredVols);
//
//        HostVO routingHost = null;
//        final HashSet<Host> avoid = new HashSet<Host>();
//
//        final HostVO fromHost = _hostDao.findById(router.getHostId());
//        if (fromHost.getHypervisorType() != HypervisorType.KVM && fromHost.getClusterId() == null) {
//            s_logger.debug("The host is not in a cluster");
//            return null;
//        }
//        avoid.add(fromHost);
//
//        while ((routingHost = (HostVO) _agentMgr.findHost(Host.Type.Routing, dc, pod, sp, offering, _template, router, fromHost, avoid)) != null) {
//            avoid.add(routingHost);
//            if (s_logger.isDebugEnabled()) {
//                s_logger.debug("Trying to migrate router to host " + routingHost.getName());
//            }
//
//            if (!_storageMgr.share(router, vols, routingHost, false)) {
//                s_logger.warn("Can not share " + vol.getPath() + " to " + router.getName());
//                throw new StorageUnavailableException("Can not share " + vol.getPath() + " to " + router.getName(), sp.getId());
//            }
//
//            final Answer answer = _agentMgr.easySend(routingHost.getId(), cmd);
//            if (answer != null && answer.getResult()) {
//                return routingHost;
//            }
//
//            _storageMgr.unshare(router, vols, routingHost);
//        }
//
//        return null;
//    }
//
//    @Override
//    public boolean migrate(final DomainRouterVO router, final HostVO host) {
//        final HostVO fromHost = _hostDao.findById(router.getHostId());
//
//        if (!_itMgr.stateTransitTo(router, VirtualMachine.Event.MigrationRequested, router.getHostId())) {
//            s_logger.debug("State for " + router.toString() + " has changed so migration can not take place.");
//            return false;
//        }
//
//        final MigrateCommand cmd = new MigrateCommand(router.getInstanceName(), host.getPrivateIpAddress(), false);
//        final Answer answer = _agentMgr.easySend(fromHost.getId(), cmd);
//        if (answer == null) {
//            return false;
//        }
//
//        final List<VolumeVO> vols = _volsDao.findCreatedByInstance(router.getId());
//        if (vols.size() == 0) {
//            return true;
//        }
//
//        _storageMgr.unshare(router, vols, fromHost);
//
//        return true;
//    }
//
//    @Override
//    public boolean completeMigration(final DomainRouterVO router, final HostVO host) throws OperationTimedoutException, AgentUnavailableException {
//        final CheckVirtualMachineCommand cvm = new CheckVirtualMachineCommand(router.getInstanceName());
//        final CheckVirtualMachineAnswer answer = (CheckVirtualMachineAnswer) _agentMgr.send(host.getId(), cvm);
//        if (answer == null || !answer.getResult()) {
//            s_logger.debug("Unable to complete migration for " + router.getId());
//            _itMgr.stateTransitTo(router, VirtualMachine.Event.AgentReportStopped, null);
//            return false;
//        }
//
//        final State state = answer.getState();
//        if (state == State.Stopped) {
//            s_logger.warn("Unable to complete migration as we can not detect it on " + host.getId());
//            _itMgr.stateTransitTo(router, VirtualMachine.Event.AgentReportStopped, null);
//            return false;
//        }
//
//        _itMgr.stateTransitTo(router, VirtualMachine.Event.OperationSucceeded, host.getId());
//
//        return true;
//    }

    protected class RouterCleanupTask implements Runnable {

        public RouterCleanupTask() {
        }

        @Override
        public void run() {
            try {
                final List<Long> ids = findLonelyRouters();
                Long size;
                if (ids == null || ids.isEmpty()) {
                    size = 0L;
                } else {
                    size = Long.valueOf(ids.size());
                }
                
                s_logger.info("Found " + size + " routers to stop. ");
                
                if (ids != null) {
                    for (final Long id : ids) {
                        stopRouter(id);
                    } 
                }
                
                s_logger.info("Done my job.  Time to rest.");
            } catch (Exception e) {
                s_logger.warn("Unable to stop routers.  Will retry. ", e);
            }
        }
    }

    private VmDataCommand generateVmDataCommand(VirtualRouter router, String vmPrivateIpAddress,
            String userData, String serviceOffering, String zoneName, String guestIpAddress, String vmName, String vmInstanceName, long vmId, String publicKey) {
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

        return cmd;
    }

    protected class NetworkUsageTask implements Runnable {

        public NetworkUsageTask() {
        }

        @Override
        public void run() {
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
                            UserStatisticsVO stats = _statsDao.lock(router.getAccountId(), router.getDataCenterId(), null, null);
                            if (stats == null) {
                                s_logger.warn("unable to find stats for account: " + router.getAccountId());
                                continue;
                            }
                            if (stats.getCurrentBytesReceived() > answer.getBytesReceived()) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Received # of bytes that's less than the last one.  Assuming something went wrong and persisting it.  Reported: "
                                            + answer.getBytesReceived() + " Stored: " + stats.getCurrentBytesReceived());
                                }
                                stats.setNetBytesReceived(stats.getNetBytesReceived() + stats.getCurrentBytesReceived());
                            }
                            stats.setCurrentBytesReceived(answer.getBytesReceived());
                            if (stats.getCurrentBytesSent() > answer.getBytesSent()) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Received # of bytes that's less than the last one.  Assuming something went wrong and persisting it.  Reported: "
                                            + answer.getBytesSent() + " Stored: " + stats.getCurrentBytesSent());
                                }
                                stats.setNetBytesSent(stats.getNetBytesSent() + stats.getCurrentBytesSent());
                            }
                            stats.setCurrentBytesSent(answer.getBytesSent());
                            _statsDao.update(stats.getId(), stats);
                            txn.commit();
                        } catch (Exception e) {
                            txn.rollback();
                            s_logger.warn("Unable to update user statistics for account: " + router.getAccountId() + " Rx: "
                                    + answer.getBytesReceived() + "; Tx: " + answer.getBytesSent());
                        } finally {
                            txn.close();
                        }
                    }
                }
            }
        }
    }

    public static boolean isAdmin(short accountType) {
        return ((accountType == Account.ACCOUNT_TYPE_ADMIN) || (accountType == Account.ACCOUNT_TYPE_DOMAIN_ADMIN) || (accountType == Account.ACCOUNT_TYPE_READ_ONLY_ADMIN));
    }

    @Override
    public VirtualRouter deployVirtualRouter(Network guestNetwork, DeployDestination dest, Account owner) throws InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException {
        long dcId = dest.getDataCenter().getId();

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Starting a router for network configurations: virtual=" + guestNetwork + " in " + dest);
        }
        
        assert guestNetwork.getState() == Network.State.Implemented || guestNetwork.getState() == Network.State.Setup || guestNetwork.getState() == Network.State.Implementing: "Network is not yet fully implemented: "
                + guestNetwork;
        assert guestNetwork.getTrafficType() == TrafficType.Guest;

        DataCenterDeployment plan = new DataCenterDeployment(dcId);

        DomainRouterVO router = _routerDao.findByNetworkConfiguration(guestNetwork.getId());
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
            List<NetworkVO> publicConfigs = _networkMgr.setupNetwork(_systemAcct, publicOffering, plan, null, null, false, false);
            NicProfile defaultNic = new NicProfile();
            defaultNic.setDefaultNic(true);
            defaultNic.setIp4Address(sourceNatIp.getAddress().addr());
            defaultNic.setGateway(sourceNatIp.getGateway());
            defaultNic.setNetmask(sourceNatIp.getNetmask());
            defaultNic.setTrafficType(TrafficType.Public);
            defaultNic.setMacAddress(sourceNatIp.getMacAddress());
            defaultNic.setBroadcastType(BroadcastDomainType.Vlan);
            defaultNic.setBroadcastUri(BroadcastDomainType.Vlan.toUri(sourceNatIp.getVlanTag()));
            defaultNic.setIsolationUri(IsolationType.Vlan.toUri(sourceNatIp.getVlanTag()));
            defaultNic.setDeviceId(2);
            networks.add(new Pair<NetworkVO, NicProfile>(publicConfigs.get(0), defaultNic));
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

            router = new DomainRouterVO(id, _offering.getId(), VirtualMachineName.getRouterName(id, _instance), _template.getId(),
                    _template.getHypervisorType(), _template.getGuestOSId(), owner.getDomainId(), owner.getId(), guestNetwork.getId(), _offering.getOfferHA(), guestNetwork.getNetworkDomain());
            router = _itMgr.allocate(router, _template, _offering, networks, plan, null, owner);
        }

        State state = router.getState();
        if (state != State.Starting && state != State.Running) {
            router = this.start(router, _accountService.getSystemUser(), _accountService.getSystemAccount());
        }
        
        return router;
    }

    @Override
    public VirtualRouter deployDhcp(Network guestNetwork, DeployDestination dest, Account owner) throws InsufficientCapacityException,
            StorageUnavailableException, ConcurrentOperationException, ResourceUnavailableException {
        long dcId = dest.getDataCenter().getId();

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Starting a dhcp for network configurations: dhcp=" + guestNetwork + " in " + dest);
        }
        assert guestNetwork.getState() == Network.State.Implemented || guestNetwork.getState() == Network.State.Setup || guestNetwork.getState() == Network.State.Implementing : "Network is not yet fully implemented: "
                + guestNetwork;

        DataCenterDeployment plan = new DataCenterDeployment(dcId);
        DataCenter dc = _dcDao.findById(dcId);
        DomainRouterVO router = null;
        Long podId = dest.getPod().getId();
        
        //In Basic zone and Guest network we have to start domR per pod, not per network
        if (dc.getNetworkType() == NetworkType.Basic && guestNetwork.getTrafficType() == TrafficType.Guest) {
            router = _routerDao.findByNetworkConfigurationAndPod(guestNetwork.getId(), podId);
        } else {
            router = _routerDao.findByNetworkConfiguration(guestNetwork.getId());
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

            router = new DomainRouterVO(id, _offering.getId(), VirtualMachineName.getRouterName(id, _instance), _template.getId(),
                    _template.getHypervisorType(), _template.getGuestOSId(), owner.getDomainId(), owner.getId(), guestNetwork.getId(), _offering.getOfferHA(), guestNetwork.getNetworkDomain());
            router.setRole(Role.DHCP_USERDATA);
            router = _itMgr.allocate(router, _template, _offering, networks, plan, null, owner);
        }
        State state = router.getState();
        if (state != State.Starting && state != State.Running) {
            router = this.start(router, _accountService.getSystemUser(), _accountService.getSystemAccount());
        }
        return router;
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
            dhcpRange = NetUtils.getDhcpRange(cidr);
        } 
        
        String domain = network.getNetworkDomain();
        if (router.getRole() == Role.DHCP_USERDATA) {
            type = "dhcpsrvr";
        } else {
            type = "router";
        }

        StringBuilder buf = profile.getBootArgsBuilder();
        buf.append(" template=domP type=" + type);
        buf.append(" name=").append(profile.getHostName());
        NicProfile controlNic = null;
        NicProfile managementNic = null;

        for (NicProfile nic : profile.getNics()) {
            int deviceId = nic.getDeviceId();
            buf.append(" eth").append(deviceId).append("ip=").append(nic.getIp4Address());
            buf.append(" eth").append(deviceId).append("mask=").append(nic.getNetmask());
            if (nic.isDefaultNic()) {
                buf.append(" gateway=").append(nic.getGateway());
                buf.append(" dns1=").append(nic.getDns1());
                if (nic.getDns2() != null) {
                    buf.append(" dns2=").append(nic.getDns2());
                }
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
                managementNic = nic;
            } else if (nic.getTrafficType() == TrafficType.Control) {
            	
                // DOMR control command is sent over management server in VMware
                if (dest.getHost().getHypervisorType() == HypervisorType.VmWare) {
                	if(s_logger.isInfoEnabled()) {
                        s_logger.info("Check if we need to add management server explicit route to DomR. pod cidr: " + dest.getPod().getCidrAddress() + "/" + dest.getPod().getCidrSize()
                			+ ", pod gateway: " + dest.getPod().getGateway() + ", management host: " + _mgmt_host);
                    }
                	
                	if(!NetUtils.sameSubnetCIDR(_mgmt_host, dest.getPod().getGateway(), dest.getPod().getCidrSize())) {
                    	if(s_logger.isInfoEnabled()) {
                            s_logger.info("Add management server explicit route to DomR.");
                        }

                		buf.append(" mgmtcidr=").append(_mgmt_host);
	                    buf.append(" localgw=").append(dest.getPod().getGateway());
                	} else {
                    	if(s_logger.isInfoEnabled()) {
                            s_logger.info("Management server host is at same subnet at pod private network, don't add explict route to DomR");
                        }
                	}
                }

                controlNic = nic;
            }
        }

        if (dhcpRange != null) {
            buf.append(" dhcprange=" + dhcpRange);
        }
        if (domain != null) {
            buf.append(" domain=" + router.getDomain());
        }
        
        if (!network.isDefault() && network.getGuestType() == GuestIpType.Direct) {
            buf.append(" defaultroute=false");
        } else {
            buf.append(" defaultroute=true");
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Boot Args for " + profile + ": " + buf.toString());
        }

        if (controlNic == null) {
            throw new CloudRuntimeException("Didn't start a control port");
        }

        profile.setParameter("control.nic", controlNic);

        return true;
    }

    @Override
    public boolean finalizeDeployment(Commands cmds, VirtualMachineProfile<DomainRouterVO> profile, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException{
        NicProfile controlNic = (NicProfile) profile.getParameter("control.nic");

        _ovsNetworkMgr.RouterCheckAndCreateTunnel(cmds, profile, dest);
        _ovsNetworkMgr.applyDefaultFlowToRouter(cmds, profile, dest);
        _ovsTunnelMgr.RouterCheckAndCreateTunnel(cmds, profile, dest);
		
        cmds.addCommand("checkSsh", new CheckSshCommand(profile.getInstanceName(), controlNic.getIp4Address(), 3922, 5, 20));

        DomainRouterVO router = profile.getVirtualMachine();

        List<NicVO> nics = _nicDao.listBy(router.getId());
        for (NicVO nic : nics) {
        	NetworkVO network = _networkDao.findById(nic.getNetworkId());
        	if (network.getTrafficType() == TrafficType.Public) {
        		router.setPublicIpAddress(nic.getIp4Address());
        		router.setPublicNetmask(nic.getNetmask());
        		router.setPublicMacAddress(nic.getMacAddress());
        	} else if (network.getTrafficType() == TrafficType.Guest) {
        		router.setGuestIpAddress(nic.getIp4Address());
        		router.setGuestMacAddress(nic.getMacAddress());
        	} else if (network.getTrafficType() == TrafficType.Control) {
        		router.setPrivateIpAddress(nic.getIp4Address());
        		router.setPrivateNetmask(nic.getNetmask());
        		router.setPrivateMacAddress(nic.getMacAddress());
        	}
        }
        _routerDao.update(router.getId(), router);
       
        
        //The commands should be sent for domR only, skip for DHCP
        if (router.getRole() == VirtualRouter.Role.DHCP_FIREWALL_LB_PASSWD_USERDATA) {
            long networkId = router.getNetworkId();
            long ownerId = router.getAccountId();
            long zoneId = router.getDataCenterId();
            
            
            final List<IPAddressVO> userIps = _networkMgr.listPublicIpAddressesInVirtualNetwork(ownerId, zoneId, null);
            List<PublicIpAddress> publicIps = new ArrayList<PublicIpAddress>();
            if (userIps != null && !userIps.isEmpty()) {
                for (IPAddressVO userIp : userIps) {
                    PublicIp publicIp = new PublicIp(userIp, _vlanDao.findById(userIp.getVlanId()), userIp.getMacAddress());
                    publicIps.add(publicIp);
                }
            }
            
            s_logger.debug("Found " + publicIps.size() + " ip(s) to apply as a part of domR " + router.getId() + " start.");
            
            if (!publicIps.isEmpty()) {   
                
                //Re-apply public ip addresses - should come before PF/LB/VPN
                createAssociateIPCommands(router, publicIps, cmds, 0);   
                
                //Re-apply port forwarding rules for all public ips
                List<PortForwardingRuleVO> rulesToReapply = new ArrayList<PortForwardingRuleVO>();
                List<RemoteAccessVpn> vpns = new ArrayList<RemoteAccessVpn>();
                
                for (PublicIpAddress ip : publicIps) {
                    List<PortForwardingRuleVO> rules = _pfRulesDao.listForApplication(ip.getAddress());
                    rulesToReapply.addAll(rules);
                    RemoteAccessVpn vpn = _vpnDao.findById(ip.getAddress());
                    if (vpn != null) {
                        vpns.add(vpn);
                    }
                }
                
                s_logger.debug("Found " + rulesToReapply.size() + " port forwarding rule(s) to apply as a part of domR " + router + " start.");
                if (!rulesToReapply.isEmpty()) {
                    createApplyPortForwardingRulesCommands(rulesToReapply, router, cmds);
                } 
                
                s_logger.debug("Found " + vpns.size() + " vpn(s) to apply as a part of domR " + router + " start.");
                if (!vpns.isEmpty()) {
                    for (RemoteAccessVpn vpn : vpns) {
                        createApplyVpnCommands(vpn, router, cmds);
                    }
                } 
                
                //Re-apply load balancing rules
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
        
        //Resend dhcp
        s_logger.debug("Reapplying dhcp entries as a part of domR " + router + " start...");
        createDhcpEntriesCommands(router, cmds);
        
        //Resend user data
        s_logger.debug("Reapplying user data entries as a part of domR " + router + " start...");
        createUserDataCommands(router, cmds);
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

        DomainRouterVO router = profile.getVirtualMachine();
        _ovsNetworkMgr.handleVmStateTransition(router, State.Running);
        return true;
    }

    @Override
    public void finalizeStop(VirtualMachineProfile<DomainRouterVO> profile, StopAnswer answer) {
        if (answer != null) {
            processStopOrRebootAnswer(profile.getVirtualMachine(), answer);
        }

    	DomainRouterVO router = profile.getVirtualMachine();
    	_ovsNetworkMgr.handleVmStateTransition(router, State.Stopped);
    	_ovsTunnelMgr.CheckAndDestroyTunnel(router);
    }

    @Override
    public boolean startRemoteAccessVpn(Network network, RemoteAccessVpn vpn) throws ResourceUnavailableException {
        
        DomainRouterVO router = _routerDao.findByNetworkConfiguration(network.getId());
        if (router == null) {
            s_logger.warn("Failed to start remote access VPN: no router found for account and zone");
            throw new ResourceUnavailableException("Unable to apply lb rules", DataCenter.class, network.getDataCenterId());
        }
        if (router.getState() != State.Running && router.getState() != State.Starting) {
            s_logger.warn("Failed to start remote access VPN: router not in running state");
            throw new ResourceUnavailableException("Unable to assign ip addresses, domR is not in right state " + router.getState(), DataCenter.class, network.getDataCenterId());
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
            s_logger.error("Unable to start vpn: unable add users to vpn in zone " + router.getDataCenterId() + " for account " + vpn.getAccountId()
                    + " on domR: " + router.getInstanceName() + " due to " + answer.getDetails());
            throw new ResourceUnavailableException("Unable to start vpn: Unable to add users to vpn in zone " + router.getDataCenterId() + " for account "
                    + vpn.getAccountId() + " on domR: " + router.getInstanceName() + " due to " + answer.getDetails(), DataCenter.class,
                    router.getDataCenterId());
        }
        answer = cmds.getAnswer("startVpn");
        if (!answer.getResult()) {
            s_logger.error("Unable to start vpn in zone " + router.getDataCenterId() + " for account " + vpn.getAccountId() + " on domR: "
                    + router.getInstanceName() + " due to " + answer.getDetails());
            throw new ResourceUnavailableException("Unable to start vpn in zone " + router.getDataCenterId() + " for account " + vpn.getAccountId()
                    + " on domR: " + router.getInstanceName() + " due to " + answer.getDetails(), DataCenter.class, router.getDataCenterId());
        }
        return true;
    }

    @Override
    public boolean deleteRemoteAccessVpn(Network network, RemoteAccessVpn vpn) throws ResourceUnavailableException{
        
        DomainRouterVO router = getRouter(vpn.getAccountId(), network.getDataCenterId());
        if (router == null) {
            s_logger.warn("Failed to delete remote access VPN: no router found for account and zone");
            throw new ResourceUnavailableException("Unable to apply lb rules", DataCenter.class, network.getDataCenterId());
        }
        if (router.getState() != State.Running) {
            s_logger.warn("Failed to delete remote access VPN: router not in running state");
            throw new ResourceUnavailableException("Failed to delete remote access VPN: domR is not in right state " + router.getState(), DataCenter.class, network.getDataCenterId());
        }
            Commands cmds = new Commands(OnError.Continue);
            RemoteAccessVpnCfgCommand removeVpnCmd = new RemoteAccessVpnCfgCommand(false, vpn.getServerAddress().addr(), vpn.getLocalIp(), vpn.getIpRange(), vpn.getIpsecPresharedKey());
            removeVpnCmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, router.getPrivateIpAddress());
            removeVpnCmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
            cmds.addCommand(removeVpnCmd);
            
            return sendCommandsToRouter(router, cmds);
    }

    private DomainRouterVO start(DomainRouterVO router, User user, Account caller) throws StorageUnavailableException, InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException {
        s_logger.debug("Starting router " + router);
        if (_itMgr.start(router, null, user, caller) != null) {
            return _routerDao.findById(router.getId());
        } else {
            return null;
        }
    }
    
    private DomainRouterVO stop(DomainRouterVO router, User user, Account caller) throws ConcurrentOperationException, ResourceUnavailableException {
        s_logger.debug("Stopping router " + router);
        if (_itMgr.stop(router, user, caller)) {
            return _routerDao.findById(router.getId());
        }  else {
            return null;
        }
    }
    

    @Override
    public VirtualRouter addVirtualMachineIntoNetwork(Network network, NicProfile nic, VirtualMachineProfile<UserVm> profile, DeployDestination dest,
            ReservationContext context, Boolean startDhcp) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        
        VirtualRouter router = startDhcp ? deployDhcp(network, dest, profile.getOwner()) : deployVirtualRouter(network, dest, profile.getOwner());

        _userVmDao.loadDetails((UserVmVO) profile.getVirtualMachine());
        
        String password = profile.getVirtualMachine().getPassword();
        String userData = profile.getVirtualMachine().getUserData();
        String sshPublicKey = profile.getVirtualMachine().getDetail("SSH.PublicKey");
        Commands cmds = new Commands(OnError.Stop);

        String routerControlIpAddress = null;
        List<NicVO> nics = _nicDao.listBy(router.getId());
        for (NicVO n : nics) {
            NetworkVO nc = _networksDao.findById(n.getNetworkId());
            if (nc.getTrafficType() == TrafficType.Control) {
                routerControlIpAddress = n.getIp4Address();
            }
        }
        
        DhcpEntryCommand dhcpCommand = new DhcpEntryCommand(nic.getMacAddress(), nic.getIp4Address(), profile.getVirtualMachine()
                .getName());
        dhcpCommand.setAccessDetail(NetworkElementCommand.ROUTER_IP, routerControlIpAddress);
        dhcpCommand.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        cmds.addCommand("dhcp", dhcpCommand);
        
        if (password != null) {
            final String encodedPassword = rot13(password);
            cmds.addCommand("password", new SavePasswordCommand(encodedPassword, nic.getIp4Address(), routerControlIpAddress, profile
                    .getVirtualMachine().getName()));
        }

        String serviceOffering = _serviceOfferingDao.findById(profile.getServiceOfferingId()).getDisplayText();
        String zoneName = _dcDao.findById(network.getDataCenterId()).getName();
        
        cmds.addCommand(
                "vmdata",
                generateVmDataCommand(router, nic.getIp4Address(), userData, serviceOffering, zoneName,
                        nic.getIp4Address(), profile.getVirtualMachine().getName(), profile.getVirtualMachine().getInstanceName(), profile.getId(), sshPublicKey));

        try {
            _agentMgr.send(router.getHostId(), cmds);
        } catch (OperationTimedoutException e) {
            throw new AgentUnavailableException("Unable to reach the agent ", router.getHostId(), e);
        }

        Answer answer = cmds.getAnswer("dhcp");
        if (!answer.getResult()) {
            s_logger.error("Unable to set dhcp entry for " + profile + " on domR: " + router.getName() + " due to " + answer.getDetails());
            throw new ResourceUnavailableException("Unable to set dhcp entry for " + profile + " due to " + answer.getDetails(), DataCenter.class,
                    router.getDataCenterId());
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
    public String[] applyVpnUsers(Network network, List<? extends VpnUser> users) throws ResourceUnavailableException{
        DomainRouterVO router = _routerDao.findByNetworkConfiguration(network.getId());
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
        for (VpnUser user: users) {
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
        
        //Currently we receive just one answer from the agent. In the future we have to parse individual answers and set results accordingly
        boolean agentResult = sendCommandsToRouter(router, cmds);;
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
    public VirtualRouter startRouter(long routerId) throws ResourceUnavailableException, InsufficientCapacityException, ConcurrentOperationException {
        Account account = UserContext.current().getCaller();

        // verify parameters
        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            throw new InvalidParameterValueException("Unable to find router by id " + routerId + ".");
        }
        _accountMgr.checkAccess(account, router);

        UserVO user = _userDao.findById(UserContext.current().getCallerUserId());
        
        return this.start(router, user, account);
    }

    private void createAssociateIPCommands(final DomainRouterVO router, final List<? extends PublicIpAddress> ips, Commands cmds, long vmId) {  
        
        //Ensure that in multiple vlans case we first send all ip addresses of vlan1, then all ip addresses of vlan2, etc..
        Map<String, ArrayList<PublicIpAddress>> vlanIpMap = new HashMap<String, ArrayList<PublicIpAddress>>();
        for (final PublicIpAddress ipAddress: ips) {
            String vlanTag = ipAddress.getVlanTag();
            ArrayList<PublicIpAddress> ipList = vlanIpMap.get(vlanTag);
            if (ipList == null) {
                ipList = new ArrayList<PublicIpAddress>();
            }
            ipList.add(ipAddress);
            vlanIpMap.put(vlanTag, ipList);
        }
        
        for (Map.Entry<String, ArrayList<PublicIpAddress>> vlanAndIp: vlanIpMap.entrySet()) {
            List<PublicIpAddress> ipAddrList = vlanAndIp.getValue();
            //Source nat ip address should always be sent first
            Collections.sort(ipAddrList, new Comparator<PublicIpAddress>() {
                @Override
                public int compare(PublicIpAddress o1, PublicIpAddress o2) {
                    boolean s1 = o1.isSourceNat();
                    boolean s2 = o2.isSourceNat();
                    return (s1 ^ s2) ? ((s1 ^ true) ? 1 : -1) : 0;
                } });
             
             IpAddressTO[] ipsToSend = new IpAddressTO[ipAddrList.size()];
             int i = 0;
             boolean firstIP = true;
             for (final PublicIpAddress ipAddr: ipAddrList) {
                 
                 boolean add = (ipAddr.getState() == IpAddress.State.Releasing ? false : true);
                 boolean sourceNat = ipAddr.isSourceNat();
                 String vlanId = ipAddr.getVlanTag();
                 String vlanGateway = ipAddr.getGateway();
                 String vlanNetmask = ipAddr.getNetmask();
                 String vifMacAddress = ipAddr.getMacAddress();
                 
                 String vmGuestAddress = null;
                 
                 //Get network rate - required for IpAssoc
                 Network network = _networkMgr.getNetwork(ipAddr.getNetworkId());
                 NetworkOffering no = _configMgr.getNetworkOffering(network.getNetworkOfferingId());
                 Integer networkRate = _configMgr.getNetworkRate(no.getId());
                 
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
        
        SetPortForwardingRulesCommand cmd = new SetPortForwardingRulesCommand(rules);
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
            String srcIp = rule.getSourceIpAddress().addr();
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
        for (VpnUser user: vpnUsers) {
            if (user.getState() == VpnUser.State.Add) {
                addUsers.add(user);
            } else if (user.getState() == VpnUser.State.Revoke) {
                removeUsers.add(user);
            }
        }
        
        VpnUsersCfgCommand addUsersCmd = new VpnUsersCfgCommand(addUsers, removeUsers);
        addUsersCmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, router.getPrivateIpAddress());
        addUsersCmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        
        RemoteAccessVpnCfgCommand startVpnCmd = new RemoteAccessVpnCfgCommand(true, vpn.getServerAddress().addr(),
                vpn.getLocalIp(), vpn.getIpRange(), vpn.getIpsecPresharedKey());
        startVpnCmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, router.getPrivateIpAddress());
        startVpnCmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        
        cmds.addCommand("users", addUsersCmd);
        cmds.addCommand("startVpn", startVpnCmd);
    }
    
    
    private void createUserDataCommands(DomainRouterVO router, Commands cmds) {
        long networkId = router.getNetworkId();
        List<UserVmVO> vms = _userVmDao.listByNetworkId(networkId);
        if (vms != null && !vms.isEmpty()) {
            for (UserVmVO vm : vms) {
                if (vm.getUserData() != null) {
                    NicVO nic = _nicDao.findByInstanceIdAndNetworkId(networkId, vm.getId());
                    if (nic != null) {
                        s_logger.debug("Creating user data entry for vm " + vm + " on domR " + router);
                        String serviceOffering = _serviceOfferingDao.findById(vm.getServiceOfferingId()).getDisplayText();
                        String zoneName = _dcDao.findById(router.getDataCenterId()).getName();
                        cmds.addCommand(
                                "vmdata",
                                generateVmDataCommand(router, nic.getIp4Address(), vm.getUserData(), serviceOffering, zoneName,
                                        nic.getIp4Address(), vm.getName(), vm.getInstanceName(), vm.getId(), null));
                    }
                }
            }
        }
    }
    
    private void createDhcpEntriesCommands(DomainRouterVO router, Commands cmds) {
        long networkId = router.getNetworkId();
        List<UserVmVO> vms = _userVmDao.listByNetworkId(networkId);
        if (vms != null && !vms.isEmpty()) {
            for (UserVmVO vm : vms) {
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
        DomainRouterVO router = _routerDao.findByNetworkConfiguration(network.getId());
        if (router == null) {
            //Return true only when domR entry exists, has Destroyed state and not null Removed field 
            //because it happens just in case when this method is called as a part of account cleanup.
            //In all other cases return false
            router = _routerDao.findByNetworkConfigurationIncludingRemoved(network.getId());
            if (router != null && (router.getState() == State.Destroyed || router.getState() == State.Expunging)) {
                return true;
            }
            s_logger.warn("Unable to associate ip addresses, virtual router doesn't exist in the network " + network.getId());
            throw new ResourceUnavailableException("Unable to assign ip addresses", DataCenter.class, network.getDataCenterId());
        }

        if (router.getState() == State.Running) {
            Commands cmds = new Commands(OnError.Continue);
            //Have to resend all already associated ip addresses
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
    public boolean applyLBRules(Network network, List<LoadBalancingRule> rules) throws ResourceUnavailableException {
        DomainRouterVO router = _routerDao.findByNetworkConfiguration(network.getId());
        if (router == null) {
            s_logger.warn("Unable to apply lb rules, virtual router doesn't exist in the network " + network.getId());
            throw new ResourceUnavailableException("Unable to apply lb rules", DataCenter.class, network.getDataCenterId());
        }
        
        Commands cmds = new Commands(OnError.Continue);
        createApplyLoadBalancingRulesCommands(rules, router, cmds);     
        //Send commands to router
        return sendCommandsToRouter(router, cmds);   
    }

    @Override
    public boolean applyPortForwardingRules(Network network, List<PortForwardingRule> rules) throws AgentUnavailableException {
        DomainRouterVO router = _routerDao.findByNetworkConfiguration(network.getId());
        
        Commands cmds = new Commands(OnError.Continue);
        createApplyPortForwardingRulesCommands(rules, router, cmds);     
        //Send commands to router
        return sendCommandsToRouter(router, cmds); 
    }

    @Override
    public DomainRouterVO start(long vmId) throws InsufficientCapacityException, StorageUnavailableException,
            ConcurrentOperationException {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    public boolean stop(DomainRouterVO router) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }
    
    
    private List<Long> findLonelyRouters() {
        List<Long> routersToStop = new ArrayList<Long>();
        List<VMInstanceVO> runningRouters = _instanceDao.listByTypeAndState(State.Running, VirtualMachine.Type.DomainRouter);
        
        for (VMInstanceVO router : runningRouters) {
            DataCenter dc = _configMgr.getZone(router.getDataCenterId());
            if (dc.getNetworkType() == NetworkType.Advanced) {
                //Only non-system networks should be reviewed as system network can always have other system vms running
                List<NetworkVO> routerNetworks = _networkMgr.listNetworksUsedByVm(router.getId(), false); 
                List<Network> networksToCheck = new ArrayList<Network>();
                for (Network routerNetwork : routerNetworks){
                   if ((routerNetwork.getGuestType() == GuestIpType.Direct && routerNetwork.getTrafficType() == TrafficType.Public) || (routerNetwork.getGuestType() == GuestIpType.Virtual && routerNetwork.getTrafficType() == TrafficType.Guest)) {
                       networksToCheck.add(routerNetwork);
                   }
                }
                
                boolean toStop = true;
                for (Network network : networksToCheck) {
                    int count = _networkMgr.getActiveNicsInNetwork(network.getId());
                    if (count > 1) {
                        s_logger.trace("Network id=" + network.getId() + " used by router " + router + " has more than 1 active nic (number of nics is " + count + ")");
                        toStop = false;
                        break;
                    }
                }
                
                if (toStop) {
                    s_logger.trace("Adding router " + router + " to stop list of Router Monitor"); 
                    routersToStop.add(router.getId());
                }
            }
        }
        
        return routersToStop;
    }
}
