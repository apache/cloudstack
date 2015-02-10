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

package com.cloud.network.router;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.alert.AlertService;
import org.apache.cloudstack.alert.AlertService.AlertType;
import org.apache.cloudstack.api.command.admin.router.RebootRouterCmd;
import org.apache.cloudstack.api.command.admin.router.UpgradeRouterCmd;
import org.apache.cloudstack.api.command.admin.router.UpgradeRouterTemplateCmd;
import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigDepot;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.AsyncJobVO;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.network.topology.NetworkTopology;
import org.apache.cloudstack.network.topology.NetworkTopologyContext;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.cloudstack.utils.usage.UsageUtils;
import org.apache.log4j.Logger;
import org.cloud.network.router.deployment.RouterDeploymentDefinitionBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckRouterAnswer;
import com.cloud.agent.api.CheckRouterCommand;
import com.cloud.agent.api.CheckS2SVpnConnectionsAnswer;
import com.cloud.agent.api.CheckS2SVpnConnectionsCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetDomRVersionAnswer;
import com.cloud.agent.api.GetDomRVersionCmd;
import com.cloud.agent.api.GetRouterAlertsAnswer;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.routing.AggregationControlCommand;
import com.cloud.agent.api.routing.AggregationControlCommand.Action;
import com.cloud.agent.api.routing.GetRouterAlertsCommand;
import com.cloud.agent.api.routing.IpAliasTO;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetMonitorServiceCommand;
import com.cloud.agent.api.to.MonitorServiceTO;
import com.cloud.agent.manager.Commands;
import com.cloud.alert.AlertManager;
import com.cloud.api.ApiAsyncJobDispatcher;
import com.cloud.api.ApiGsonHelper;
import com.cloud.cluster.ManagementServerHostVO;
import com.cloud.cluster.dao.ManagementServerHostDao;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ZoneConfig;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IpAddress;
import com.cloud.network.IpAddressManager;
import com.cloud.network.MonitoringService;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.Site2SiteCustomerGateway;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.SshKeysDistriMonitor;
import com.cloud.network.VirtualNetworkApplianceService;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.FirewallRulesDao;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVMMapDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.MonitoringServiceDao;
import com.cloud.network.dao.MonitoringServiceVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.OpRouterMonitorServiceDao;
import com.cloud.network.dao.OpRouterMonitorServiceVO;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.RemoteAccessVpnDao;
import com.cloud.network.dao.Site2SiteCustomerGatewayDao;
import com.cloud.network.dao.Site2SiteVpnConnectionDao;
import com.cloud.network.dao.Site2SiteVpnConnectionVO;
import com.cloud.network.dao.Site2SiteVpnGatewayDao;
import com.cloud.network.dao.UserIpv6AddressDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.dao.VpnUserDao;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbHealthCheckPolicy;
import com.cloud.network.lb.LoadBalancingRule.LbSslCert;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.VirtualRouter.RedundantState;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.FirewallRuleVO;
import com.cloud.network.rules.LoadBalancerContainer.Scheme;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.RulesManager;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.rules.StaticNatImpl;
import com.cloud.network.rules.StaticNatRule;
import com.cloud.network.rules.dao.PortForwardingRulesDao;
import com.cloud.network.vpc.Vpc;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.network.vpn.Site2SiteVpnManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ConfigurationServer;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.user.UserStatisticsVO;
import com.cloud.user.UserStatsLogVO;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import com.cloud.user.dao.UserStatisticsDao;
import com.cloud.user.dao.UserStatsLogDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.StateListener;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.MacAddress;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic;
import com.cloud.vm.NicIpAlias;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineGuru;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfile.Param;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicIpAliasDao;
import com.cloud.vm.dao.NicIpAliasVO;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.UserVmDetailsDao;
import com.cloud.vm.dao.VMInstanceDao;

/**
 * VirtualNetworkApplianceManagerImpl manages the different types of virtual
 * network appliances available in the Cloud Stack.
 */
@Local(value = { VirtualNetworkApplianceManager.class, VirtualNetworkApplianceService.class })
public class VirtualNetworkApplianceManagerImpl extends ManagerBase implements VirtualNetworkApplianceManager, VirtualNetworkApplianceService, VirtualMachineGuru, Listener,
Configurable, StateListener<State, VirtualMachine.Event, VirtualMachine> {
    private static final Logger s_logger = Logger.getLogger(VirtualNetworkApplianceManagerImpl.class);

    @Inject
    EntityManager _entityMgr;
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
    HostDao _hostDao = null;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    HostPodDao _podDao = null;
    @Inject
    UserStatsLogDao _userStatsLogDao = null;
    @Inject
    AgentManager _agentMgr;
    @Inject
    AlertManager _alertMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    ConfigurationServer _configServer;
    @Inject
    ServiceOfferingDao _serviceOfferingDao = null;
    @Inject
    UserVmDao _userVmDao;
    @Inject
    VMInstanceDao _vmDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao = null;
    @Inject
    GuestOSDao _guestOSDao = null;
    @Inject
    NetworkOrchestrationService _networkMgr;
    @Inject
    NetworkModel _networkModel;
    @Inject
    VirtualMachineManager _itMgr;
    @Inject
    VpnUserDao _vpnUsersDao;
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
    NicDao _nicDao;
    @Inject
    NicIpAliasDao _nicIpAliasDao;
    @Inject
    VolumeDao _volumeDao = null;
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
    @Inject
    Site2SiteCustomerGatewayDao _s2sCustomerGatewayDao;
    @Inject
    Site2SiteVpnGatewayDao _s2sVpnGatewayDao;
    @Inject
    Site2SiteVpnConnectionDao _s2sVpnConnectionDao;
    @Inject
    Site2SiteVpnManager _s2sVpnMgr;
    @Inject
    UserIpv6AddressDao _ipv6Dao;
    @Inject
    NetworkService _networkSvc;
    @Inject
    IpAddressManager _ipAddrMgr;
    @Inject
    ConfigDepot _configDepot;
    @Inject
    MonitoringServiceDao _monitorServiceDao;
    @Inject
    AsyncJobManager _asyncMgr;
    @Inject
    protected VpcDao _vpcDao;
    @Inject
    protected ApiAsyncJobDispatcher _asyncDispatcher;
    @Inject
    OpRouterMonitorServiceDao _opRouterMonitorServiceDao;

    @Inject
    protected NetworkTopologyContext _networkTopologyContext;

    @Autowired
    @Qualifier("networkHelper")
    protected NetworkHelper _nwHelper;

    @Inject
    protected RouterControlHelper _routerControlHelper;

    @Inject
    protected CommandSetupHelper _commandSetupHelper;
    @Inject
    protected RouterDeploymentDefinitionBuilder _routerDeploymentManagerBuilder;

    int _routerRamSize;
    int _routerCpuMHz;
    int _retry = 2;
    String _mgmtCidr;

    int _routerStatsInterval = 300;
    int _routerCheckInterval = 30;
    int _rvrStatusUpdatePoolSize = 10;
    private String _dnsBasicZoneUpdates = "all";
    private final Set<String> _guestOSNeedGatewayOnNonDefaultNetwork = new HashSet<String>();

    private boolean _disableRpFilter = false;
    int _routerExtraPublicNics = 2;
    private int _usageAggregationRange = 1440;
    private String _usageTimeZone = "GMT";
    private final long mgmtSrvrId = MacAddress.getMacAddress().toLong();
    private static final int ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION = 5; // 5 seconds
    private boolean _dailyOrHourly = false;

    ScheduledExecutorService _executor;
    ScheduledExecutorService _checkExecutor;
    ScheduledExecutorService _networkStatsUpdateExecutor;
    ExecutorService _rvrStatusUpdateExecutor;

    BlockingQueue<Long> _vrUpdateQueue = null;

    @Override
    public VirtualRouter destroyRouter(final long routerId, final Account caller, final Long callerUserId) throws ResourceUnavailableException, ConcurrentOperationException {
        return _nwHelper.destroyRouter(routerId, caller, callerUserId);
    }

    @Override
    @DB
    public VirtualRouter upgradeRouter(final UpgradeRouterCmd cmd) {
        final Long routerId = cmd.getId();
        final Long serviceOfferingId = cmd.getServiceOfferingId();
        final Account caller = CallContext.current().getCallingAccount();

        final DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            throw new InvalidParameterValueException("Unable to find router with id " + routerId);
        }

        _accountMgr.checkAccess(caller, null, true, router);

        if (router.getServiceOfferingId() == serviceOfferingId) {
            s_logger.debug("Router: " + routerId + "already has service offering: " + serviceOfferingId);
            return _routerDao.findById(routerId);
        }

        final ServiceOffering newServiceOffering = _entityMgr.findById(ServiceOffering.class, serviceOfferingId);
        if (newServiceOffering == null) {
            throw new InvalidParameterValueException("Unable to find service offering with id " + serviceOfferingId);
        }

        // check if it is a system service offering, if yes return with error as
        // it cannot be used for user vms
        if (!newServiceOffering.getSystemUse()) {
            throw new InvalidParameterValueException("Cannot upgrade router vm to a non system service offering " + serviceOfferingId);
        }

        // Check that the router is stopped
        if (!router.getState().equals(State.Stopped)) {
            s_logger.warn("Unable to upgrade router " + router.toString() + " in state " + router.getState());
            throw new InvalidParameterValueException("Unable to upgrade router " + router.toString() + " in state " + router.getState()
                    + "; make sure the router is stopped and not in an error state before upgrading.");
        }

        final ServiceOfferingVO currentServiceOffering = _serviceOfferingDao.findById(router.getServiceOfferingId());

        // Check that the service offering being upgraded to has the same
        // storage pool preference as the VM's current service
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

    @ActionEvent(eventType = EventTypes.EVENT_ROUTER_STOP, eventDescription = "stopping router Vm", async = true)
    @Override
    public VirtualRouter stopRouter(final long routerId, final boolean forced) throws ResourceUnavailableException, ConcurrentOperationException {
        final CallContext context = CallContext.current();
        final Account account = context.getCallingAccount();

        // verify parameters
        final DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            throw new InvalidParameterValueException("Unable to find router by id " + routerId + ".");
        }

        _accountMgr.checkAccess(account, null, true, router);

        final UserVO user = _userDao.findById(CallContext.current().getCallingUserId());

        final VirtualRouter virtualRouter = stop(router, forced, user, account);
        if (virtualRouter == null) {
            throw new CloudRuntimeException("Failed to stop router with id " + routerId);
        }

        // Clear stop pending flag after stopped successfully
        if (router.isStopPending()) {
            s_logger.info("Clear the stop pending flag of router " + router.getHostName() + " after stop router successfully");
            router.setStopPending(false);
            _routerDao.persist(router);
            virtualRouter.setStopPending(false);
        }
        return virtualRouter;
    }

    @DB
    public void processStopOrRebootAnswer(final DomainRouterVO router, final Answer answer) {
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(final TransactionStatus status) {
                // FIXME!!! - UserStats command should grab bytesSent/Received
                // for all guest interfaces of the VR
                final List<Long> routerGuestNtwkIds = _routerDao.getRouterNetworks(router.getId());
                for (final Long guestNtwkId : routerGuestNtwkIds) {
                    final UserStatisticsVO userStats = _userStatsDao.lock(router.getAccountId(), router.getDataCenterId(), guestNtwkId, null, router.getId(), router.getType()
                            .toString());
                    if (userStats != null) {
                        final long currentBytesRcvd = userStats.getCurrentBytesReceived();
                        userStats.setCurrentBytesReceived(0);
                        userStats.setNetBytesReceived(userStats.getNetBytesReceived() + currentBytesRcvd);

                        final long currentBytesSent = userStats.getCurrentBytesSent();
                        userStats.setCurrentBytesSent(0);
                        userStats.setNetBytesSent(userStats.getNetBytesSent() + currentBytesSent);
                        _userStatsDao.update(userStats.getId(), userStats);
                        s_logger.debug("Successfully updated user statistics as a part of domR " + router + " reboot/stop");
                    } else {
                        s_logger.warn("User stats were not created for account " + router.getAccountId() + " and dc " + router.getDataCenterId());
                    }
                }
            }
        });
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ROUTER_REBOOT, eventDescription = "rebooting router Vm", async = true)
    public VirtualRouter rebootRouter(final long routerId, final boolean reprogramNetwork) throws ConcurrentOperationException, ResourceUnavailableException,
    InsufficientCapacityException {
        final Account caller = CallContext.current().getCallingAccount();

        // verify parameters
        final DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            throw new InvalidParameterValueException("Unable to find domain router with id " + routerId + ".");
        }

        _accountMgr.checkAccess(caller, null, true, router);

        // Can reboot domain router only in Running state
        if (router == null || router.getState() != State.Running) {
            s_logger.warn("Unable to reboot, virtual router is not in the right state " + router.getState());
            throw new ResourceUnavailableException("Unable to reboot domR, it is not in right state " + router.getState(), DataCenter.class, router.getDataCenterId());
        }

        final UserVO user = _userDao.findById(CallContext.current().getCallingUserId());
        s_logger.debug("Stopping and starting router " + router + " as a part of router reboot");

        if (stop(router, false, user, caller) != null) {
            return startRouter(routerId, reprogramNetwork);
        } else {
            throw new CloudRuntimeException("Failed to reboot router " + router);
        }
    }

    static final ConfigKey<Boolean> UseExternalDnsServers = new ConfigKey<Boolean>(Boolean.class, "use.external.dns", "Advanced", "false",
            "Bypass internal dns, use external dns1 and dns2", true, ConfigKey.Scope.Zone, null);

    static final ConfigKey<Boolean> routerVersionCheckEnabled = new ConfigKey<Boolean>("Advanced", Boolean.class, "router.version.check", "true",
            "If true, router minimum required version is checked before sending command", false);

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {

        _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("RouterMonitor"));
        _checkExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("RouterStatusMonitor"));
        _networkStatsUpdateExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("NetworkStatsUpdater"));

        VirtualMachine.State.getStateMachine().registerListener(this);

        final Map<String, String> configs = _configDao.getConfiguration("AgentManager", params);

        _routerRamSize = NumbersUtil.parseInt(configs.get("router.ram.size"), DEFAULT_ROUTER_VM_RAMSIZE);
        _routerCpuMHz = NumbersUtil.parseInt(configs.get("router.cpu.mhz"), DEFAULT_ROUTER_CPU_MHZ);

        _routerExtraPublicNics = NumbersUtil.parseInt(_configDao.getValue(Config.RouterExtraPublicNics.key()), 2);

        final String guestOSString = configs.get("network.dhcp.nondefaultnetwork.setgateway.guestos");
        if (guestOSString != null) {
            final String[] guestOSList = guestOSString.split(",");
            for (final String os : guestOSList) {
                _guestOSNeedGatewayOnNonDefaultNetwork.add(os);
            }
        }

        String value = configs.get("start.retry");
        _retry = NumbersUtil.parseInt(value, 2);

        value = configs.get("router.stats.interval");
        _routerStatsInterval = NumbersUtil.parseInt(value, 300);

        value = configs.get("router.check.interval");
        _routerCheckInterval = NumbersUtil.parseInt(value, 30);

        value = configs.get("router.check.poolsize");
        _rvrStatusUpdatePoolSize = NumbersUtil.parseInt(value, 10);

        /*
         * We assume that one thread can handle 20 requests in 1 minute in
         * normal situation, so here we give the queue size up to 50 minutes.
         * It's mostly for buffer, since each time CheckRouterTask running, it
         * would add all the redundant networks in the queue immediately
         */
        _vrUpdateQueue = new LinkedBlockingQueue<Long>(_rvrStatusUpdatePoolSize * 1000);

        _rvrStatusUpdateExecutor = Executors.newFixedThreadPool(_rvrStatusUpdatePoolSize, new NamedThreadFactory("RedundantRouterStatusMonitor"));

        String instance = configs.get("instance.name");
        if (instance == null) {
            instance = "DEFAULT";
        }

        NetworkHelperImpl.setVMInstanceName(instance);

        final String rpValue = configs.get("network.disable.rpfilter");
        if (rpValue != null && rpValue.equalsIgnoreCase("true")) {
            _disableRpFilter = true;
        }

        _dnsBasicZoneUpdates = String.valueOf(_configDao.getValue(Config.DnsBasicZoneUpdates.key()));

        s_logger.info("Router configurations: " + "ramsize=" + _routerRamSize);

        _agentMgr.registerForHostEvents(new SshKeysDistriMonitor(_agentMgr, _hostDao, _configDao), true, false, false);

        final boolean useLocalStorage = Boolean.parseBoolean(configs.get(Config.SystemVMUseLocalStorage.key()));

        ServiceOfferingVO offering = new ServiceOfferingVO("System Offering For Software Router", 1, _routerRamSize, _routerCpuMHz, null, null, true, null, ProvisioningType.THIN,
                useLocalStorage, true, null, true, VirtualMachine.Type.DomainRouter, true);
        offering.setUniqueName(ServiceOffering.routerDefaultOffUniqueName);
        offering = _serviceOfferingDao.persistSystemServiceOffering(offering);
        _routerDeploymentManagerBuilder.setOfferingId(offering.getId());

        NetworkHelperImpl.setSystemAccount(_accountMgr.getSystemAccount());

        final String aggregationRange = configs.get("usage.stats.job.aggregation.range");
        _usageAggregationRange = NumbersUtil.parseInt(aggregationRange, 1440);
        _usageTimeZone = configs.get("usage.aggregation.timezone");
        if (_usageTimeZone == null) {
            _usageTimeZone = "GMT";
        }

        _agentMgr.registerForHostEvents(this, true, false, false);

        s_logger.info("DomainRouterManager is configured.");

        return true;
    }

    @Override
    public boolean start() {
        if (_routerStatsInterval > 0) {
            _executor.scheduleAtFixedRate(new NetworkUsageTask(), _routerStatsInterval, _routerStatsInterval, TimeUnit.SECONDS);
        } else {
            s_logger.debug("router.stats.interval - " + _routerStatsInterval + " so not scheduling the router stats thread");
        }

        //Schedule Network stats update task
        //Network stats aggregation should align with aggregation range
        //For daily aggregation, update stats at the end of the day
        //For hourly aggregation, update stats at the end of the hour
        final TimeZone usageTimezone = TimeZone.getTimeZone(_usageTimeZone);
        final Calendar cal = Calendar.getInstance(usageTimezone);
        cal.setTime(new Date());
        //aggDate is the time in millis when the aggregation should happen
        long aggDate = 0;
        final int HOURLY_TIME = 60;
        final int DAILY_TIME = 60 * 24;
        if (_usageAggregationRange == DAILY_TIME) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.roll(Calendar.DAY_OF_YEAR, true);
            cal.add(Calendar.MILLISECOND, -1);
            aggDate = cal.getTime().getTime();
            _dailyOrHourly = true;
        } else if (_usageAggregationRange == HOURLY_TIME) {
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.roll(Calendar.HOUR_OF_DAY, true);
            cal.add(Calendar.MILLISECOND, -1);
            aggDate = cal.getTime().getTime();
            _dailyOrHourly = true;
        } else {
            aggDate = cal.getTime().getTime();
            _dailyOrHourly = false;
        }

        if (_usageAggregationRange < UsageUtils.USAGE_AGGREGATION_RANGE_MIN) {
            s_logger.warn("Usage stats job aggregation range is to small, using the minimum value of " + UsageUtils.USAGE_AGGREGATION_RANGE_MIN);
            _usageAggregationRange = UsageUtils.USAGE_AGGREGATION_RANGE_MIN;
        }

        // We cannot schedule a job at specific time. Provide initial delay instead, from current time, so that the job runs at desired time
        final long initialDelay = aggDate - System.currentTimeMillis();

        if( initialDelay < 0){
            s_logger.warn("Initial delay for network usage stats update task is incorrect. Stats update task will run immediately");
        }

        _networkStatsUpdateExecutor.scheduleAtFixedRate(new NetworkStatsUpdateTask(), initialDelay, _usageAggregationRange * 60 * 1000,
                TimeUnit.MILLISECONDS);

        if (_routerCheckInterval > 0) {
            _checkExecutor.scheduleAtFixedRate(new CheckRouterTask(), _routerCheckInterval, _routerCheckInterval, TimeUnit.SECONDS);
            for (int i = 0; i < _rvrStatusUpdatePoolSize; i++) {
                _rvrStatusUpdateExecutor.execute(new RvRStatusUpdateTask());
            }
        } else {
            s_logger.debug("router.check.interval - " + _routerCheckInterval + " so not scheduling the redundant router checking thread");
        }

        final int routerAlertsCheckInterval = RouterAlertsCheckInterval.value();
        if (routerAlertsCheckInterval > 0) {
            _checkExecutor.scheduleAtFixedRate(new CheckRouterAlertsTask(), routerAlertsCheckInterval, routerAlertsCheckInterval, TimeUnit.SECONDS);
        } else {
            s_logger.debug("router.alerts.check.interval - " + routerAlertsCheckInterval + " so not scheduling the router alerts checking thread");
        }

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    protected VirtualNetworkApplianceManagerImpl() {
    }

    protected class NetworkUsageTask extends ManagedContextRunnable {

        public NetworkUsageTask() {
        }

        @Override
        protected void runInContext() {
            try {
                final List<DomainRouterVO> routers = _routerDao.listByStateAndNetworkType(State.Running, GuestType.Isolated, mgmtSrvrId);
                s_logger.debug("Found " + routers.size() + " running routers. ");

                for (final DomainRouterVO router : routers) {
                    final String privateIP = router.getPrivateIpAddress();

                    if (privateIP != null) {
                        final boolean forVpc = router.getVpcId() != null;
                        final List<? extends Nic> routerNics = _nicDao.listByVmId(router.getId());
                        for (final Nic routerNic : routerNics) {
                            final Network network = _networkModel.getNetwork(routerNic.getNetworkId());
                            // Send network usage command for public nic in VPC VR
                            // Send network usage command for isolated guest nic of non) VPC VR

                            //[TODO] Avoiding the NPE now, but I have to find out what is going on with the network. - Wilder Rodrigues
                            if (network == null) {
                                s_logger.error("Could not find a network with ID => " + routerNic.getNetworkId() + ". It might be a problem!");
                                continue;
                            }
                            if (forVpc && network.getTrafficType() == TrafficType.Public || !forVpc && network.getTrafficType() == TrafficType.Guest
                                    && network.getGuestType() == Network.GuestType.Isolated) {
                                final NetworkUsageCommand usageCmd = new NetworkUsageCommand(privateIP, router.getHostName(), forVpc, routerNic.getIp4Address());
                                final String routerType = router.getType().toString();
                                final UserStatisticsVO previousStats = _userStatsDao.findBy(router.getAccountId(), router.getDataCenterId(), network.getId(),
                                        forVpc ? routerNic.getIp4Address() : null, router.getId(), routerType);
                                NetworkUsageAnswer answer = null;
                                try {
                                    answer = (NetworkUsageAnswer) _agentMgr.easySend(router.getHostId(), usageCmd);
                                } catch (final Exception e) {
                                    s_logger.warn("Error while collecting network stats from router: " + router.getInstanceName() + " from host: " + router.getHostId(), e);
                                    continue;
                                }

                                if (answer != null) {
                                    if (!answer.getResult()) {
                                        s_logger.warn("Error while collecting network stats from router: " + router.getInstanceName() + " from host: " + router.getHostId()
                                                + "; details: " + answer.getDetails());
                                        continue;
                                    }
                                    try {
                                        if (answer.getBytesReceived() == 0 && answer.getBytesSent() == 0) {
                                            s_logger.debug("Recieved and Sent bytes are both 0. Not updating user_statistics");
                                            continue;
                                        }
                                        final NetworkUsageAnswer answerFinal = answer;
                                        Transaction.execute(new TransactionCallbackNoReturn() {
                                            @Override
                                            public void doInTransactionWithoutResult(final TransactionStatus status) {
                                                final UserStatisticsVO stats = _userStatsDao.lock(router.getAccountId(), router.getDataCenterId(), network.getId(),
                                                        forVpc ? routerNic.getIp4Address() : null, router.getId(), routerType);
                                                if (stats == null) {
                                                    s_logger.warn("unable to find stats for account: " + router.getAccountId());
                                                    return;
                                                }

                                                if (previousStats != null
                                                        && (previousStats.getCurrentBytesReceived() != stats.getCurrentBytesReceived() || previousStats.getCurrentBytesSent() != stats
                                                        .getCurrentBytesSent())) {
                                                    s_logger.debug("Router stats changed from the time NetworkUsageCommand was sent. " + "Ignoring current answer. Router: "
                                                            + answerFinal.getRouterName() + " Rcvd: " + answerFinal.getBytesReceived() + "Sent: " + answerFinal.getBytesSent());
                                                    return;
                                                }

                                                if (stats.getCurrentBytesReceived() > answerFinal.getBytesReceived()) {
                                                    if (s_logger.isDebugEnabled()) {
                                                        s_logger.debug("Received # of bytes that's less than the last one.  "
                                                                + "Assuming something went wrong and persisting it. Router: " + answerFinal.getRouterName() + " Reported: "
                                                                + answerFinal.getBytesReceived() + " Stored: " + stats.getCurrentBytesReceived());
                                                    }
                                                    stats.setNetBytesReceived(stats.getNetBytesReceived() + stats.getCurrentBytesReceived());
                                                }
                                                stats.setCurrentBytesReceived(answerFinal.getBytesReceived());
                                                if (stats.getCurrentBytesSent() > answerFinal.getBytesSent()) {
                                                    if (s_logger.isDebugEnabled()) {
                                                        s_logger.debug("Received # of bytes that's less than the last one.  "
                                                                + "Assuming something went wrong and persisting it. Router: " + answerFinal.getRouterName() + " Reported: "
                                                                + answerFinal.getBytesSent() + " Stored: " + stats.getCurrentBytesSent());
                                                    }
                                                    stats.setNetBytesSent(stats.getNetBytesSent() + stats.getCurrentBytesSent());
                                                }
                                                stats.setCurrentBytesSent(answerFinal.getBytesSent());
                                                if (!_dailyOrHourly) {
                                                    // update agg bytes
                                                    stats.setAggBytesSent(stats.getNetBytesSent() + stats.getCurrentBytesSent());
                                                    stats.setAggBytesReceived(stats.getNetBytesReceived() + stats.getCurrentBytesReceived());
                                                }
                                                _userStatsDao.update(stats.getId(), stats);
                                            }
                                        });

                                    } catch (final Exception e) {
                                        s_logger.warn("Unable to update user statistics for account: " + router.getAccountId() + " Rx: " + answer.getBytesReceived() + "; Tx: "
                                                + answer.getBytesSent());
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (final Exception e) {
                s_logger.warn("Error while collecting network stats", e);
            }
        }
    }

    protected class NetworkStatsUpdateTask extends ManagedContextRunnable {

        public NetworkStatsUpdateTask() {
        }

        @Override
        protected void runInContext() {
            final GlobalLock scanLock = GlobalLock.getInternLock("network.stats");
            try {
                if (scanLock.lock(ACQUIRE_GLOBAL_LOCK_TIMEOUT_FOR_COOPERATION)) {
                    // Check for ownership
                    // msHost in UP state with min id should run the job
                    final ManagementServerHostVO msHost = _msHostDao.findOneInUpState(new Filter(ManagementServerHostVO.class, "id", false, 0L, 1L));
                    if (msHost == null || msHost.getMsid() != mgmtSrvrId) {
                        s_logger.debug("Skipping aggregate network stats update");
                        scanLock.unlock();
                        return;
                    }
                    try {
                        Transaction.execute(new TransactionCallbackNoReturn() {
                            @Override
                            public void doInTransactionWithoutResult(final TransactionStatus status) {
                                // get all stats with delta > 0
                                final List<UserStatisticsVO> updatedStats = _userStatsDao.listUpdatedStats();
                                final Date updatedTime = new Date();
                                for (final UserStatisticsVO stat : updatedStats) {
                                    // update agg bytes
                                    stat.setAggBytesReceived(stat.getCurrentBytesReceived() + stat.getNetBytesReceived());
                                    stat.setAggBytesSent(stat.getCurrentBytesSent() + stat.getNetBytesSent());
                                    _userStatsDao.update(stat.getId(), stat);
                                    // insert into op_user_stats_log
                                    final UserStatsLogVO statsLog = new UserStatsLogVO(stat.getId(), stat.getNetBytesReceived(), stat.getNetBytesSent(), stat
                                            .getCurrentBytesReceived(), stat.getCurrentBytesSent(), stat.getAggBytesReceived(), stat.getAggBytesSent(), updatedTime);
                                    _userStatsLogDao.persist(statsLog);
                                }
                                s_logger.debug("Successfully updated aggregate network stats");
                            }
                        });
                    } catch (final Exception e) {
                        s_logger.debug("Failed to update aggregate network stats", e);
                    } finally {
                        scanLock.unlock();
                    }
                }
            } catch (final Exception e) {
                s_logger.debug("Exception while trying to acquire network stats lock", e);
            } finally {
                scanLock.releaseRef();
            }
        }
    }

    @DB
    protected void updateSite2SiteVpnConnectionState(final List<DomainRouterVO> routers) {
        for (final DomainRouterVO router : routers) {
            final List<Site2SiteVpnConnectionVO> conns = _s2sVpnMgr.getConnectionsForRouter(router);
            if (conns == null || conns.isEmpty()) {
                continue;
            }
            if (router.getState() != State.Running) {
                for (final Site2SiteVpnConnectionVO conn : conns) {
                    if (conn.getState() != Site2SiteVpnConnection.State.Error) {
                        conn.setState(Site2SiteVpnConnection.State.Disconnected);
                        _s2sVpnConnectionDao.persist(conn);
                    }
                }
                continue;
            }
            final List<String> ipList = new ArrayList<String>();
            for (final Site2SiteVpnConnectionVO conn : conns) {
                if (conn.getState() != Site2SiteVpnConnection.State.Connected && conn.getState() != Site2SiteVpnConnection.State.Disconnected) {
                    continue;
                }
                final Site2SiteCustomerGateway gw = _s2sCustomerGatewayDao.findById(conn.getCustomerGatewayId());
                ipList.add(gw.getGatewayIp());
            }
            final String privateIP = router.getPrivateIpAddress();
            final HostVO host = _hostDao.findById(router.getHostId());
            if (host == null || host.getState() != Status.Up) {
                continue;
            } else if (host.getManagementServerId() != ManagementServerNode.getManagementServerId()) {
                /* Only cover hosts managed by this management server */
                continue;
            } else if (privateIP != null) {
                final CheckS2SVpnConnectionsCommand command = new CheckS2SVpnConnectionsCommand(ipList);
                command.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
                command.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
                command.setWait(30);
                final Answer origAnswer = _agentMgr.easySend(router.getHostId(), command);
                CheckS2SVpnConnectionsAnswer answer = null;
                if (origAnswer instanceof CheckS2SVpnConnectionsAnswer) {
                    answer = (CheckS2SVpnConnectionsAnswer) origAnswer;
                } else {
                    s_logger.warn("Unable to update router " + router.getHostName() + "'s VPN connection status");
                    continue;
                }
                if (!answer.getResult()) {
                    s_logger.warn("Unable to update router " + router.getHostName() + "'s VPN connection status");
                    continue;
                }
                for (final Site2SiteVpnConnectionVO conn : conns) {
                    final Site2SiteVpnConnectionVO lock = _s2sVpnConnectionDao.acquireInLockTable(conn.getId());
                    if (lock == null) {
                        throw new CloudRuntimeException("Unable to acquire lock for site to site vpn connection id " + conn.getId());
                    }
                    try {
                        if (conn.getState() != Site2SiteVpnConnection.State.Connected && conn.getState() != Site2SiteVpnConnection.State.Disconnected) {
                            continue;
                        }
                        final Site2SiteVpnConnection.State oldState = conn.getState();
                        final Site2SiteCustomerGateway gw = _s2sCustomerGatewayDao.findById(conn.getCustomerGatewayId());
                        if (answer.isConnected(gw.getGatewayIp())) {
                            conn.setState(Site2SiteVpnConnection.State.Connected);
                        } else {
                            conn.setState(Site2SiteVpnConnection.State.Disconnected);
                        }
                        _s2sVpnConnectionDao.persist(conn);
                        if (oldState != conn.getState()) {
                            final String title = "Site-to-site Vpn Connection to " + gw.getName() + " just switch from " + oldState + " to " + conn.getState();
                            final String context = "Site-to-site Vpn Connection to " + gw.getName() + " on router " + router.getHostName() + "(id: " + router.getId() + ") "
                                    + " just switch from " + oldState + " to " + conn.getState();
                            s_logger.info(context);
                            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_DOMAIN_ROUTER, router.getDataCenterId(), router.getPodIdToDeployIn(), title, context);
                        }
                    } finally {
                        _s2sVpnConnectionDao.releaseFromLockTable(lock.getId());
                    }
                }
            }
        }
    }

    protected void updateRoutersRedundantState(final List<DomainRouterVO> routers) {
        boolean updated;
        for (final DomainRouterVO router : routers) {
            updated = false;
            if (!router.getIsRedundantRouter()) {
                continue;
            }
            final RedundantState prevState = router.getRedundantState();
            if (router.getState() != State.Running) {
                router.setRedundantState(RedundantState.UNKNOWN);
                router.setIsPriorityBumpUp(false);
                updated = true;
            } else {
                final String privateIP = router.getPrivateIpAddress();
                final HostVO host = _hostDao.findById(router.getHostId());
                if (host == null || host.getState() != Status.Up) {
                    router.setRedundantState(RedundantState.UNKNOWN);
                    updated = true;
                } else if (privateIP != null) {
                    final CheckRouterCommand command = new CheckRouterCommand();
                    command.setAccessDetail(NetworkElementCommand.ROUTER_IP, _routerControlHelper.getRouterControlIp(router.getId()));
                    command.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
                    command.setWait(30);
                    final Answer origAnswer = _agentMgr.easySend(router.getHostId(), command);
                    CheckRouterAnswer answer = null;
                    if (origAnswer instanceof CheckRouterAnswer) {
                        answer = (CheckRouterAnswer) origAnswer;
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
                _routerDao.update(router.getId(), router);
            }
            final RedundantState currState = router.getRedundantState();
            if (prevState != currState) {
                final String title = "Redundant virtual router " + router.getInstanceName() + " just switch from " + prevState + " to " + currState;
                final String context = "Redundant virtual router (name: " + router.getHostName() + ", id: " + router.getId() + ") " + " just switch from " + prevState + " to "
                        + currState;
                s_logger.info(context);
                if (currState == RedundantState.MASTER) {
                    _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_DOMAIN_ROUTER, router.getDataCenterId(), router.getPodIdToDeployIn(), title, context);
                }
            }
        }
    }

    // Ensure router status is update to date before execute this function. The
    // function would try best to recover all routers except MASTER
    protected void recoverRedundantNetwork(final DomainRouterVO masterRouter, final DomainRouterVO backupRouter) {
        if (masterRouter.getState() == State.Running && backupRouter.getState() == State.Running) {
            final HostVO masterHost = _hostDao.findById(masterRouter.getHostId());
            final HostVO backupHost = _hostDao.findById(backupRouter.getHostId());
            if (masterHost.getState() == Status.Up && backupHost.getState() == Status.Up) {
                final String title = "Reboot " + backupRouter.getInstanceName() + " to ensure redundant virtual routers work";
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(title);
                }
                _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_DOMAIN_ROUTER, backupRouter.getDataCenterId(), backupRouter.getPodIdToDeployIn(), title, title);
                try {
                    rebootRouter(backupRouter.getId(), true);
                } catch (final ConcurrentOperationException e) {
                    s_logger.warn("Fail to reboot " + backupRouter.getInstanceName(), e);
                } catch (final ResourceUnavailableException e) {
                    s_logger.warn("Fail to reboot " + backupRouter.getInstanceName(), e);
                } catch (final InsufficientCapacityException e) {
                    s_logger.warn("Fail to reboot " + backupRouter.getInstanceName(), e);
                }
            }
        }
    }

    protected class RvRStatusUpdateTask extends ManagedContextRunnable {

        /*
         * In order to make fail-over works well at any time, we have to ensure:
         * 1. Backup router's priority = Master's priority - DELTA + 1
         * 2. Backup router's priority hasn't been bumped up.
         */
        private void checkSanity(final List<DomainRouterVO> routers) {
            final Set<Long> checkedNetwork = new HashSet<Long>();
            for (final DomainRouterVO router : routers) {
                if (!router.getIsRedundantRouter()) {
                    continue;
                }

                final List<Long> routerGuestNtwkIds = _routerDao.getRouterNetworks(router.getId());

                for (final Long routerGuestNtwkId : routerGuestNtwkIds) {
                    if (checkedNetwork.contains(routerGuestNtwkId)) {
                        continue;
                    }
                    checkedNetwork.add(routerGuestNtwkId);

                    final List<DomainRouterVO> checkingRouters;
                    final Long vpcId = router.getVpcId();
                    if (vpcId != null) {
                        checkingRouters = _routerDao.listByVpcId(vpcId);
                    } else {
                        checkingRouters = _routerDao.listByNetworkAndRole(routerGuestNtwkId, Role.VIRTUAL_ROUTER);
                    }

                    if (checkingRouters.size() != 2) {
                        continue;
                    }

                    DomainRouterVO masterRouter = null;
                    DomainRouterVO backupRouter = null;
                    for (final DomainRouterVO r : checkingRouters) {
                        if (r.getRedundantState() == RedundantState.MASTER) {
                            if (masterRouter == null) {
                                masterRouter = r;
                            } else {
                                // Wilder Rodrigues (wrodrigues@schubergphilis.com
                                // Force a restart in order to fix the conflict
                                recoverRedundantNetwork(masterRouter, r);
                                break;
                            }
                        } else if (r.getRedundantState() == RedundantState.BACKUP) {
                            if (backupRouter == null) {
                                backupRouter = r;
                            } else {
                                // Wilder Rodrigues (wrodrigues@schubergphilis.com
                                // Do we have 2 routers in Backup state? Perhaps a restart of 1 router is needed.
                                recoverRedundantNetwork(backupRouter, r);
                                break;
                            }
                        }
                    }
                }
            }
        }

        private void checkDuplicateMaster(final List<DomainRouterVO> routers) {
            final Map<Long, DomainRouterVO> networkRouterMaps = new HashMap<Long, DomainRouterVO>();
            for (final DomainRouterVO router : routers) {
                final List<Long> routerGuestNtwkIds = _routerDao.getRouterNetworks(router.getId());

                for (final Long routerGuestNtwkId : routerGuestNtwkIds) {
                    if (router.getRedundantState() == RedundantState.MASTER) {
                        if (networkRouterMaps.containsKey(routerGuestNtwkId)) {
                            final DomainRouterVO dupRouter = networkRouterMaps.get(routerGuestNtwkId);
                            final String title = "More than one redundant virtual router is in MASTER state! Router " + router.getHostName() + " and router "
                                    + dupRouter.getHostName();
                            final String context = "Virtual router (name: " + router.getHostName() + ", id: " + router.getId() + " and router (name: " + dupRouter.getHostName()
                                    + ", id: " + router.getId() + ") are both in MASTER state! If the problem persist, restart both of routers. ";
                            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_DOMAIN_ROUTER, router.getDataCenterId(), router.getPodIdToDeployIn(), title, context);
                            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_DOMAIN_ROUTER, dupRouter.getDataCenterId(), dupRouter.getPodIdToDeployIn(), title, context);
                            s_logger.warn(context);
                        } else {
                            networkRouterMaps.put(routerGuestNtwkId, router);
                        }
                    }
                }
            }
        }

        @Override
        protected void runInContext() {
            while (true) {
                try {
                    final Long networkId = _vrUpdateQueue.take(); // This is a blocking call so this thread won't run all the time if no work item in queue.

                    final NetworkVO network = _networkDao.findById(networkId);
                    final Long vpcId = network.getVpcId();

                    final List<DomainRouterVO> routers;
                    if (vpcId != null) {
                        routers = _routerDao.listByVpcId(vpcId);
                    } else {
                        routers = _routerDao.listByNetworkAndRole(networkId, Role.VIRTUAL_ROUTER);
                    }

                    if (routers.size() != 2) {
                        continue;
                    }
                    /*
                     * We update the router pair which the lower id router owned
                     * by this mgmt server, in order to prevent duplicate update
                     * of router status from cluster mgmt servers
                     */
                    final DomainRouterVO router0 = routers.get(0);
                    final DomainRouterVO router1 = routers.get(1);
                    DomainRouterVO router = router0;
                    if (router0.getId() < router1.getId()) {
                        router = router0;
                    } else {
                        router = router1;
                    }
                    // && router.getState() == State.Stopped
                    if (router.getHostId() == null && router.getState() == State.Running) {
                        s_logger.debug("Skip router pair (" + router0.getInstanceName() + "," + router1.getInstanceName() + ") due to can't find host");
                        continue;
                    }
                    final HostVO host = _hostDao.findById(router.getHostId());
                    if (host == null || host.getManagementServerId() == null || host.getManagementServerId() != ManagementServerNode.getManagementServerId()) {
                        s_logger.debug("Skip router pair (" + router0.getInstanceName() + "," + router1.getInstanceName() + ") due to not belong to this mgmt server");
                        continue;
                    }
                    updateRoutersRedundantState(routers);
                    checkDuplicateMaster(routers);
                    checkSanity(routers);
                } catch (final Exception ex) {
                    s_logger.error("Fail to complete the RvRStatusUpdateTask! ", ex);
                }
            }
        }
    }

    protected class CheckRouterTask extends ManagedContextRunnable {

        public CheckRouterTask() {
        }

        @Override
        protected void runInContext() {
            try {
                final List<DomainRouterVO> routers = _routerDao.listIsolatedByHostId(null);
                s_logger.debug("Found " + routers.size() + " routers to update status. ");

                updateSite2SiteVpnConnectionState(routers);

                List<NetworkVO> networks = _networkDao.listVpcNetworks();
                s_logger.debug("Found " + networks.size() + " VPC networks to update Redundant State. ");
                pushToUpdateQueue(networks);

                networks = _networkDao.listRedundantNetworks();
                s_logger.debug("Found " + networks.size() + " networks to update RvR status. ");
                pushToUpdateQueue(networks);
            } catch (final Exception ex) {
                s_logger.error("Fail to complete the CheckRouterTask! ", ex);
            }
        }

        protected void pushToUpdateQueue(final List<NetworkVO> networks) throws InterruptedException {
            for (final NetworkVO network : networks) {
                if (!_vrUpdateQueue.offer(network.getId(), 500, TimeUnit.MILLISECONDS)) {
                    s_logger.warn("Cannot insert into virtual router update queue! Adjustment of router.check.interval and router.check.poolsize maybe needed.");
                    break;
                }
            }
        }
    }

    protected class CheckRouterAlertsTask extends ManagedContextRunnable {
        public CheckRouterAlertsTask() {
        }

        @Override
        protected void runInContext() {
            try {
                getRouterAlerts();
            } catch (final Exception ex) {
                s_logger.error("Fail to complete the CheckRouterAlertsTask! ", ex);
            }
        }
    }

    protected void getRouterAlerts() {
        try {
            final List<DomainRouterVO> routers = _routerDao.listByStateAndManagementServer(State.Running, mgmtSrvrId);

            s_logger.debug("Found " + routers.size() + " running routers. ");

            for (final DomainRouterVO router : routers) {
                final String serviceMonitoringFlag = SetServiceMonitor.valueIn(router.getDataCenterId());
                // Skip the routers in VPC network or skip the routers where
                // Monitor service is not enabled in the corresponding Zone
                if (!Boolean.parseBoolean(serviceMonitoringFlag) || router.getVpcId() != null) {
                    continue;
                }

                final String privateIP = router.getPrivateIpAddress();

                if (privateIP != null) {
                    OpRouterMonitorServiceVO opRouterMonitorServiceVO = _opRouterMonitorServiceDao.findById(router.getId());

                    GetRouterAlertsCommand command = null;
                    if (opRouterMonitorServiceVO == null) {
                        command = new GetRouterAlertsCommand(new String("1970-01-01 00:00:00")); // To
                        // avoid
                        // sending
                        // null
                        // value
                    } else {
                        command = new GetRouterAlertsCommand(opRouterMonitorServiceVO.getLastAlertTimestamp());
                    }

                    command.setAccessDetail(NetworkElementCommand.ROUTER_IP, router.getPrivateIpAddress());

                    try {
                        final Answer origAnswer = _agentMgr.easySend(router.getHostId(), command);
                        GetRouterAlertsAnswer answer = null;

                        if (origAnswer == null) {
                            s_logger.warn("Unable to get alerts from router " + router.getHostName());
                            continue;
                        }
                        if (origAnswer instanceof GetRouterAlertsAnswer) {
                            answer = (GetRouterAlertsAnswer) origAnswer;
                        } else {
                            s_logger.warn("Unable to get alerts from router " + router.getHostName());
                            continue;
                        }
                        if (!answer.getResult()) {
                            s_logger.warn("Unable to get alerts from router " + router.getHostName() + " " + answer.getDetails());
                            continue;
                        }

                        final String alerts[] = answer.getAlerts();
                        if (alerts != null) {
                            final String lastAlertTimeStamp = answer.getTimeStamp();
                            final SimpleDateFormat sdfrmt = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                            sdfrmt.setLenient(false);
                            try {
                                sdfrmt.parse(lastAlertTimeStamp);
                            } catch (final ParseException e) {
                                s_logger.warn("Invalid last alert timestamp received while collecting alerts from router: " + router.getInstanceName());
                                continue;
                            }
                            for (final String alert : alerts) {
                                _alertMgr.sendAlert(AlertType.ALERT_TYPE_DOMAIN_ROUTER, router.getDataCenterId(), router.getPodIdToDeployIn(), "Monitoring Service on VR "
                                        + router.getInstanceName(), alert);
                            }
                            if (opRouterMonitorServiceVO == null) {
                                opRouterMonitorServiceVO = new OpRouterMonitorServiceVO(router.getId(), router.getHostName(), lastAlertTimeStamp);
                                _opRouterMonitorServiceDao.persist(opRouterMonitorServiceVO);
                            } else {
                                opRouterMonitorServiceVO.setLastAlertTimestamp(lastAlertTimeStamp);
                                _opRouterMonitorServiceDao.update(opRouterMonitorServiceVO.getId(), opRouterMonitorServiceVO);
                            }
                        }
                    } catch (final Exception e) {
                        s_logger.warn("Error while collecting alerts from router: " + router.getInstanceName(), e);
                        continue;
                    }
                }
            }
        } catch (final Exception e) {
            s_logger.warn("Error while collecting alerts from router", e);
        }
    }

    protected int getUpdatedPriority(final Network network, final List<DomainRouterVO> routers, final DomainRouterVO masterRouter)
            throws InsufficientVirtualNetworkCapacityException {
        int priority;
        if (routers.size() == 0) {
            priority = DEFAULT_PRIORITY;
        } else {
            int maxPriority = 0;

            final DomainRouterVO router0 = routers.get(0);
            if (router0.getId() == masterRouter.getId()) {
                if (!router0.getIsRedundantRouter()) {
                    throw new CloudRuntimeException("Redundant router is mixed with single router in one network!");
                }
                maxPriority = _nwHelper.getRealPriority(router0);
            } else {
                maxPriority = DEFAULT_PRIORITY;
            }

            if (maxPriority == 0) {
                return DEFAULT_PRIORITY;
            }
            if (maxPriority < 20) {
                s_logger.error("Current maximum priority is too low!");
                throw new InsufficientVirtualNetworkCapacityException("Current maximum priority is too low as " + maxPriority + "!", network.getId());
            } else if (maxPriority > 200) {
                s_logger.error("Too many times fail-over happened! Current maximum priority is too high as " + maxPriority + "!");
                throw new InsufficientVirtualNetworkCapacityException("Too many times fail-over happened! Current maximum priority is too high as " + maxPriority + "!",
                        network.getId());
            }

            priority = maxPriority - DEFAULT_DELTA + 1;
        }
        return priority;
    }

    @Override
    public boolean finalizeVirtualMachineProfile(final VirtualMachineProfile profile, final DeployDestination dest, final ReservationContext context) {

        boolean dnsProvided = true;
        boolean dhcpProvided = true;
        boolean publicNetwork = false;
        final DataCenterVO dc = _dcDao.findById(dest.getDataCenter().getId());
        _dcDao.loadDetails(dc);

        // 1) Set router details
        final DomainRouterVO router = _routerDao.findById(profile.getVirtualMachine().getId());
        final Map<String, String> details = _vmDetailsDao.listDetailsKeyPairs(router.getId());
        router.setDetails(details);

        // 2) Prepare boot loader elements related with Control network

        final StringBuilder buf = profile.getBootArgsBuilder();
        buf.append(" template=domP");
        buf.append(" name=").append(profile.getHostName());

        if (Boolean.valueOf(_configDao.getValue("system.vm.random.password"))) {
            buf.append(" vmpassword=").append(_configDao.getValue("system.vm.password"));
        }

        NicProfile controlNic = null;
        String defaultDns1 = null;
        String defaultDns2 = null;
        String defaultIp6Dns1 = null;
        String defaultIp6Dns2 = null;
        for (final NicProfile nic : profile.getNics()) {
            final int deviceId = nic.getDeviceId();
            boolean ipv4 = false, ipv6 = false;
            if (nic.getIp4Address() != null) {
                ipv4 = true;
                buf.append(" eth").append(deviceId).append("ip=").append(nic.getIp4Address());
                buf.append(" eth").append(deviceId).append("mask=").append(nic.getNetmask());
            }
            if (nic.getIp6Address() != null) {
                ipv6 = true;
                buf.append(" eth").append(deviceId).append("ip6=").append(nic.getIp6Address());
                buf.append(" eth").append(deviceId).append("ip6prelen=").append(NetUtils.getIp6CidrSize(nic.getIp6Cidr()));
            }

            if (nic.isDefaultNic()) {
                if (ipv4) {
                    buf.append(" gateway=").append(nic.getGateway());
                }
                if (ipv6) {
                    buf.append(" ip6gateway=").append(nic.getIp6Gateway());
                }
                defaultDns1 = nic.getDns1();
                defaultDns2 = nic.getDns2();
                defaultIp6Dns1 = nic.getIp6Dns1();
                defaultIp6Dns2 = nic.getIp6Dns2();
            }

            if (nic.getTrafficType() == TrafficType.Management) {
                buf.append(" localgw=").append(dest.getPod().getGateway());
            } else if (nic.getTrafficType() == TrafficType.Control) {
                controlNic = nic;
                buf.append(createRedundantRouterArgs(controlNic, router));

                // DOMR control command is sent over management server in VMware
                if (dest.getHost().getHypervisorType() == HypervisorType.VMware || dest.getHost().getHypervisorType() == HypervisorType.Hyperv) {
                    s_logger.info("Check if we need to add management server explicit route to DomR. pod cidr: " + dest.getPod().getCidrAddress() + "/"
                            + dest.getPod().getCidrSize() + ", pod gateway: " + dest.getPod().getGateway() + ", management host: "
                            + ApiServiceConfiguration.ManagementHostIPAdr.value());

                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Add management server explicit route to DomR.");
                    }

                    // always add management explicit route, for basic
                    // networking setup, DomR may have two interfaces while both
                    // are on the same subnet
                    _mgmtCidr = _configDao.getValue(Config.ManagementNetwork.key());
                    if (NetUtils.isValidCIDR(_mgmtCidr)) {
                        buf.append(" mgmtcidr=").append(_mgmtCidr);
                        buf.append(" localgw=").append(dest.getPod().getGateway());
                    }

                    if (dc.getNetworkType() == NetworkType.Basic) {
                        // ask domR to setup SSH on guest network
                        buf.append(" sshonguest=true");
                    }

                }
            } else if (nic.getTrafficType() == TrafficType.Guest) {
                dnsProvided = _networkModel.isProviderSupportServiceInNetwork(nic.getNetworkId(), Service.Dns, Provider.VirtualRouter);
                dhcpProvided = _networkModel.isProviderSupportServiceInNetwork(nic.getNetworkId(), Service.Dhcp, Provider.VirtualRouter);
                // build bootloader parameter for the guest
                buf.append(createGuestBootLoadArgs(nic, defaultDns1, defaultDns2, router));
            } else if (nic.getTrafficType() == TrafficType.Public) {
                publicNetwork = true;
            }
        }

        if (controlNic == null) {
            throw new CloudRuntimeException("Didn't start a control port");
        }

        final String rpValue = _configDao.getValue(Config.NetworkRouterRpFilter.key());
        if (rpValue != null && rpValue.equalsIgnoreCase("true")) {
            _disableRpFilter = true;
        } else {
            _disableRpFilter = false;
        }

        String rpFilter = " ";
        String type = null;
        if (router.getVpcId() != null) {
            type = "vpcrouter";
            if (_disableRpFilter) {
                rpFilter = " disable_rp_filter=true";
            }
        } else if (!publicNetwork) {
            type = "dhcpsrvr";
        } else {
            type = "router";
            if (_disableRpFilter) {
                rpFilter = " disable_rp_filter=true";
            }
        }

        if (_disableRpFilter) {
            rpFilter = " disable_rp_filter=true";
        }

        buf.append(" type=" + type + rpFilter);

        final String domain_suffix = dc.getDetail(ZoneConfig.DnsSearchOrder.getName());
        if (domain_suffix != null) {
            buf.append(" dnssearchorder=").append(domain_suffix);
        }

        if (profile.getHypervisorType() == HypervisorType.VMware || profile.getHypervisorType() == HypervisorType.Hyperv) {
            buf.append(" extra_pubnics=" + _routerExtraPublicNics);
        }

        /*
         * If virtual router didn't provide DNS service but provide DHCP
         * service, we need to override the DHCP response to return DNS server
         * rather than virtual router itself.
         */
        if (dnsProvided || dhcpProvided) {
            if (defaultDns1 != null) {
                buf.append(" dns1=").append(defaultDns1);
            }
            if (defaultDns2 != null) {
                buf.append(" dns2=").append(defaultDns2);
            }
            if (defaultIp6Dns1 != null) {
                buf.append(" ip6dns1=").append(defaultIp6Dns1);
            }
            if (defaultIp6Dns2 != null) {
                buf.append(" ip6dns2=").append(defaultIp6Dns2);
            }

            boolean useExtDns = !dnsProvided;
            /* For backward compatibility */
            useExtDns = useExtDns || UseExternalDnsServers.valueIn(dc.getId());

            if (useExtDns) {
                buf.append(" useextdns=true");
            }
        }

        if (Boolean.valueOf(_configDao.getValue(Config.BaremetalProvisionDoneNotificationEnabled.key()))) {
            final QueryBuilder<UserVO> acntq = QueryBuilder.create(UserVO.class);
            acntq.and(acntq.entity().getUsername(), SearchCriteria.Op.EQ, "baremetal-system-account");
            final UserVO user = acntq.find();
            if (user == null) {
                s_logger.warn(String
                        .format("global setting[baremetal.provision.done.notification] is enabled but user baremetal-system-account is not found. Baremetal provision done notification will not be enabled"));
            } else {
                buf.append(String.format(" baremetalnotificationsecuritykey=%s", user.getSecretKey()));
                buf.append(String.format(" baremetalnotificationapikey=%s", user.getApiKey()));
                buf.append(" host=").append(ApiServiceConfiguration.ManagementHostIPAdr.value());
                buf.append(" port=").append(_configDao.getValue(Config.BaremetalProvisionDoneNotificationPort.key()));
            }
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Boot Args for " + profile + ": " + buf.toString());
        }

        return true;
    }

    protected StringBuilder createGuestBootLoadArgs(final NicProfile guestNic, final String defaultDns1, final String defaultDns2, final DomainRouterVO router) {
        final long guestNetworkId = guestNic.getNetworkId();
        final NetworkVO guestNetwork = _networkDao.findById(guestNetworkId);
        String dhcpRange = null;
        final DataCenterVO dc = _dcDao.findById(guestNetwork.getDataCenterId());

        final StringBuilder buf = new StringBuilder();

        final boolean isRedundant = router.getIsRedundantRouter();
        if (isRedundant) {
            buf.append(createRedundantRouterArgs(guestNic, router));
            final Network net = _networkModel.getNetwork(guestNic.getNetworkId());
            buf.append(" guestgw=").append(net.getGateway());
            final String brd = NetUtils.long2Ip(NetUtils.ip2Long(guestNic.getIp4Address()) | ~NetUtils.ip2Long(guestNic.getNetmask()));
            buf.append(" guestbrd=").append(brd);
            buf.append(" guestcidrsize=").append(NetUtils.getCidrSize(guestNic.getNetmask()));

            final int advertInt = NumbersUtil.parseInt(_configDao.getValue(Config.RedundantRouterVrrpInterval.key()), 1);
            buf.append(" advert_int=").append(advertInt);
        }

        // setup network domain
        final String domain = guestNetwork.getNetworkDomain();
        if (domain != null) {
            buf.append(" domain=" + domain);
        }

        long cidrSize = 0;

        // setup dhcp range
        if (dc.getNetworkType() == NetworkType.Basic) {
            if (guestNic.isDefaultNic()) {
                cidrSize = NetUtils.getCidrSize(guestNic.getNetmask());
                final String cidr = NetUtils.getCidrSubNet(guestNic.getGateway(), cidrSize);
                if (cidr != null) {
                    dhcpRange = NetUtils.getIpRangeStartIpFromCidr(cidr, cidrSize);
                }
            }
        } else if (dc.getNetworkType() == NetworkType.Advanced) {
            final String cidr = guestNetwork.getCidr();
            if (cidr != null) {
                cidrSize = NetUtils.getCidrSize(NetUtils.getCidrNetmask(cidr));
                dhcpRange = NetUtils.getDhcpRange(cidr);
            }
        }

        if (dhcpRange != null) {
            // To limit DNS to the cidr range
            buf.append(" cidrsize=" + String.valueOf(cidrSize));
            buf.append(" dhcprange=" + dhcpRange);
        }

        return buf;
    }

    protected StringBuilder createRedundantRouterArgs(final NicProfile nic, DomainRouterVO router) {
        final StringBuilder buf = new StringBuilder();

        final long networkId = nic.getNetworkId();
        final NetworkVO network = _networkDao.findById(networkId);

        final boolean isRedundant = router.getIsRedundantRouter();
        if (isRedundant) {
            buf.append(" redundant_router=1");

            final Long vpcId = router.getVpcId();
            final List<DomainRouterVO> routers;
            if (vpcId != null) {
                routers = _routerDao.listByVpcId(vpcId);
                // For a redundant VPC router, both shall have the same router id. It will be used by the VRRP virtural_router_id attribute.
                // So we use the VPC id to avoid group problems.
                buf.append(" router_id=").append(vpcId);

                // Will build the routers password based on the VPC ID and UUID.
                final Vpc vpc = _vpcDao.findById(vpcId);

                try {
                    final MessageDigest digest = MessageDigest.getInstance("SHA-512");
                    final byte [] rawDigest = vpc.getUuid().getBytes();
                    digest.update(rawDigest);

                    final BigInteger password = new BigInteger(1, digest.digest());
                    buf.append(" router_password=").append(password);

                } catch (final NoSuchAlgorithmException e) {
                    s_logger.error("Failed to pssword! Will use the plan B instead.");
                    buf.append(" router_password=").append(vpc.getUuid());
                }

            } else {
                routers = _routerDao.listByNetworkAndRole(nic.getNetworkId(), Role.VIRTUAL_ROUTER);
            }

            String redundantState = RedundantState.BACKUP.toString();
            router.setRedundantState(RedundantState.BACKUP);
            if (routers.size() == 0) {
                redundantState = RedundantState.MASTER.toString();
                router.setRedundantState(RedundantState.MASTER);
            } else {
                final DomainRouterVO router0 = routers.get(0);
                if (router.getId() == router0.getId()) {
                    redundantState = RedundantState.MASTER.toString();
                    router.setRedundantState(RedundantState.MASTER);
                }
            }

            buf.append(" redundant_state=").append(redundantState);

            try {
                final int priority = getUpdatedPriority(network, routers, router);
                router.setPriority(priority);
                router = _routerDao.persist(router);

                buf.append(" router_pr=").append(router.getPriority());
            } catch (final InsufficientVirtualNetworkCapacityException e) {
                s_logger.error("Failed to get update priority!", e);
                throw new CloudRuntimeException("Failed to get update priority!");
            }
        }

        return buf;
    }

    @Override
    public boolean finalizeDeployment(final Commands cmds, final VirtualMachineProfile profile, final DeployDestination dest, final ReservationContext context)
            throws ResourceUnavailableException {
        final DomainRouterVO router = _routerDao.findById(profile.getId());

        final List<NicProfile> nics = profile.getNics();
        for (final NicProfile nic : nics) {
            if (nic.getTrafficType() == TrafficType.Public) {
                router.setPublicIpAddress(nic.getIp4Address());
                router.setPublicNetmask(nic.getNetmask());
                router.setPublicMacAddress(nic.getMacAddress());
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
    public boolean finalizeCommandsOnStart(final Commands cmds, final VirtualMachineProfile profile) {
        final DomainRouterVO router = _routerDao.findById(profile.getId());
        final NicProfile controlNic = getControlNic(profile);

        if (controlNic == null) {
            s_logger.error("Control network doesn't exist for the router " + router);
            return false;
        }

        finalizeSshAndVersionAndNetworkUsageOnStart(cmds, profile, router, controlNic);

        // restart network if restartNetwork = false is not specified in profile
        // parameters
        boolean reprogramGuestNtwks = true;
        if (profile.getParameter(Param.ReProgramGuestNetworks) != null && (Boolean) profile.getParameter(Param.ReProgramGuestNetworks) == false) {
            reprogramGuestNtwks = false;
        }

        final VirtualRouterProvider vrProvider = _vrProviderDao.findById(router.getElementId());
        if (vrProvider == null) {
            throw new CloudRuntimeException("Cannot find related virtual router provider of router: " + router.getHostName());
        }
        final Provider provider = Network.Provider.getProvider(vrProvider.getType().toString());
        if (provider == null) {
            throw new CloudRuntimeException("Cannot find related provider of virtual router provider: " + vrProvider.getType().toString());
        }

        final List<Long> routerGuestNtwkIds = _routerDao.getRouterNetworks(router.getId());
        for (final Long guestNetworkId : routerGuestNtwkIds) {
            final AggregationControlCommand startCmd = new AggregationControlCommand(Action.Start, router.getInstanceName(), controlNic.getIp4Address(), _routerControlHelper.getRouterIpInNetwork(
                    guestNetworkId, router.getId()));
            cmds.addCommand(startCmd);

            if (reprogramGuestNtwks) {
                finalizeIpAssocForNetwork(cmds, router, provider, guestNetworkId, null);
                finalizeNetworkRulesForNetwork(cmds, router, provider, guestNetworkId);

                final NetworkOffering offering = _networkOfferingDao.findById(_networkDao.findById(guestNetworkId).getNetworkOfferingId());
                // service monitoring is currently not added in RVR
                if (!offering.getRedundantRouter()) {
                    final String serviceMonitringSet = _configDao.getValue(Config.EnableServiceMonitoring.key());

                    if (serviceMonitringSet != null && serviceMonitringSet.equalsIgnoreCase("true")) {
                        finalizeMonitorServiceOnStrat(cmds, profile, router, provider, guestNetworkId, true);
                    } else {
                        finalizeMonitorServiceOnStrat(cmds, profile, router, provider, guestNetworkId, false);
                    }
                }

            }

            finalizeUserDataAndDhcpOnStart(cmds, router, provider, guestNetworkId);

            final AggregationControlCommand finishCmd = new AggregationControlCommand(Action.Finish, router.getInstanceName(), controlNic.getIp4Address(), _routerControlHelper.getRouterIpInNetwork(
                    guestNetworkId, router.getId()));
            cmds.addCommand(finishCmd);
        }

        return true;
    }

    private void finalizeMonitorServiceOnStrat(final Commands cmds, final VirtualMachineProfile profile, final DomainRouterVO router, final Provider provider,
            final long networkId, final Boolean add) {

        final NetworkVO network = _networkDao.findById(networkId);

        s_logger.debug("Creating  monitoring services on " + router + " start...");

        // get the list of sevices for this network to monitor
        final List<MonitoringServiceVO> services = new ArrayList<MonitoringServiceVO>();
        if (_networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.Dhcp, Provider.VirtualRouter)
                || _networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.Dns, Provider.VirtualRouter)) {
            final MonitoringServiceVO dhcpService = _monitorServiceDao.getServiceByName(MonitoringService.Service.Dhcp.toString());
            if (dhcpService != null) {
                services.add(dhcpService);
            }
        }

        if (_networkModel.isProviderSupportServiceInNetwork(network.getId(), Service.Lb, Provider.VirtualRouter)) {
            final MonitoringServiceVO lbService = _monitorServiceDao.getServiceByName(MonitoringService.Service.LoadBalancing.toString());
            if (lbService != null) {
                services.add(lbService);
            }
        }
        final List<MonitoringServiceVO> defaultServices = _monitorServiceDao.listDefaultServices(true);
        services.addAll(defaultServices);

        final List<MonitorServiceTO> servicesTO = new ArrayList<MonitorServiceTO>();
        for (final MonitoringServiceVO service : services) {
            final MonitorServiceTO serviceTO = new MonitorServiceTO(service.getService(), service.getProcessName(), service.getServiceName(), service.getServicePath(),
                    service.getServicePidFile(), service.isDefaultService());
            servicesTO.add(serviceTO);
        }

        // TODO : This is a hacking fix
        // at VR startup time, information in VirtualMachineProfile may not
        // updated to DB yet,
        // getRouterControlIp() may give wrong IP under basic network mode in
        // VMware environment
        final NicProfile controlNic = getControlNic(profile);
        if (controlNic == null) {
            throw new CloudRuntimeException("VirtualMachine " + profile.getInstanceName() + " doesn't have a control interface");
        }
        final SetMonitorServiceCommand command = new SetMonitorServiceCommand(servicesTO);
        command.setAccessDetail(NetworkElementCommand.ROUTER_IP, controlNic.getIp4Address());
        command.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, _routerControlHelper.getRouterIpInNetwork(networkId, router.getId()));
        command.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());

        if (!add) {
            command.setAccessDetail(NetworkElementCommand.ROUTER_MONITORING_ENABLE, add.toString());
        }
        cmds.addCommand("monitor", command);
    }

    protected NicProfile getControlNic(final VirtualMachineProfile profile) {
        final DomainRouterVO router = _routerDao.findById(profile.getId());
        final DataCenterVO dcVo = _dcDao.findById(router.getDataCenterId());
        NicProfile controlNic = null;
        if (profile.getHypervisorType() == HypervisorType.VMware && dcVo.getNetworkType() == NetworkType.Basic) {
            // TODO this is a ugly to test hypervisor type here
            // for basic network mode, we will use the guest NIC for control NIC
            for (final NicProfile nic : profile.getNics()) {
                if (nic.getTrafficType() == TrafficType.Guest && nic.getIp4Address() != null) {
                    controlNic = nic;
                }
            }
        } else {
            for (final NicProfile nic : profile.getNics()) {
                if (nic.getTrafficType() == TrafficType.Control && nic.getIp4Address() != null) {
                    controlNic = nic;
                }
            }
        }
        return controlNic;
    }

    protected void finalizeSshAndVersionAndNetworkUsageOnStart(final Commands cmds, final VirtualMachineProfile profile, final DomainRouterVO router, final NicProfile controlNic) {
        final DomainRouterVO vr = _routerDao.findById(profile.getId());
        cmds.addCommand("checkSsh", new CheckSshCommand(profile.getInstanceName(), controlNic.getIp4Address(), 3922));

        // Update router template/scripts version
        final GetDomRVersionCmd command = new GetDomRVersionCmd();
        command.setAccessDetail(NetworkElementCommand.ROUTER_IP, controlNic.getIp4Address());
        command.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        cmds.addCommand("getDomRVersion", command);

        // Network usage command to create iptables rules
        final boolean forVpc = vr.getVpcId() != null;
        if (!forVpc) {
            cmds.addCommand("networkUsage", new NetworkUsageCommand(controlNic.getIp4Address(), router.getHostName(), "create", forVpc));
        }
    }

    protected void finalizeUserDataAndDhcpOnStart(final Commands cmds, final DomainRouterVO router, final Provider provider, final Long guestNetworkId) {
        if (_networkModel.isProviderSupportServiceInNetwork(guestNetworkId, Service.Dhcp, provider)) {
            // Resend dhcp
            s_logger.debug("Reapplying dhcp entries as a part of domR " + router + " start...");
            _commandSetupHelper.createDhcpEntryCommandsForVMs(router, cmds, guestNetworkId);
        }

        if (_networkModel.isProviderSupportServiceInNetwork(guestNetworkId, Service.UserData, provider)) {
            // Resend user data
            s_logger.debug("Reapplying vm data (userData and metaData) entries as a part of domR " + router + " start...");
            _commandSetupHelper.createVmDataCommandForVMs(router, cmds, guestNetworkId);
        }
    }

    protected void finalizeNetworkRulesForNetwork(final Commands cmds, final DomainRouterVO router, final Provider provider, final Long guestNetworkId) {
        s_logger.debug("Resending ipAssoc, port forwarding, load balancing rules as a part of Virtual router start");

        final ArrayList<? extends PublicIpAddress> publicIps = getPublicIpsToApply(router, provider, guestNetworkId);
        final List<FirewallRule> firewallRulesEgress = new ArrayList<FirewallRule>();

        // Fetch firewall Egress rules.
        if (_networkModel.isProviderSupportServiceInNetwork(guestNetworkId, Service.Firewall, provider)) {
            firewallRulesEgress.addAll(_rulesDao.listByNetworkPurposeTrafficType(guestNetworkId, Purpose.Firewall, FirewallRule.TrafficType.Egress));
            if (firewallRulesEgress.isEmpty()) {
                //create egress default rule for VR
                createDefaultEgressFirewallRule(firewallRulesEgress, guestNetworkId);
            }
        }

        // Re-apply firewall Egress rules
        s_logger.debug("Found " + firewallRulesEgress.size() + " firewall Egress rule(s) to apply as a part of domR " + router + " start.");
        if (!firewallRulesEgress.isEmpty()) {
            _commandSetupHelper.createFirewallRulesCommands(firewallRulesEgress, router, cmds, guestNetworkId);
        }

        if (publicIps != null && !publicIps.isEmpty()) {
            final List<RemoteAccessVpn> vpns = new ArrayList<RemoteAccessVpn>();
            final List<PortForwardingRule> pfRules = new ArrayList<PortForwardingRule>();
            final List<FirewallRule> staticNatFirewallRules = new ArrayList<FirewallRule>();
            final List<StaticNat> staticNats = new ArrayList<StaticNat>();
            final List<FirewallRule> firewallRulesIngress = new ArrayList<FirewallRule>();

            // Get information about all the rules (StaticNats and
            // StaticNatRules; PFVPN to reapply on domR start)
            for (final PublicIpAddress ip : publicIps) {
                if (_networkModel.isProviderSupportServiceInNetwork(guestNetworkId, Service.PortForwarding, provider)) {
                    pfRules.addAll(_pfRulesDao.listForApplication(ip.getId()));
                }
                if (_networkModel.isProviderSupportServiceInNetwork(guestNetworkId, Service.StaticNat, provider)) {
                    staticNatFirewallRules.addAll(_rulesDao.listByIpAndPurpose(ip.getId(), Purpose.StaticNat));
                }
                if (_networkModel.isProviderSupportServiceInNetwork(guestNetworkId, Service.Firewall, provider)) {
                    firewallRulesIngress.addAll(_rulesDao.listByIpAndPurpose(ip.getId(), Purpose.Firewall));
                }

                if (_networkModel.isProviderSupportServiceInNetwork(guestNetworkId, Service.Vpn, provider)) {
                    final RemoteAccessVpn vpn = _vpnDao.findByPublicIpAddress(ip.getId());
                    if (vpn != null) {
                        vpns.add(vpn);
                    }
                }

                if (_networkModel.isProviderSupportServiceInNetwork(guestNetworkId, Service.StaticNat, provider)) {
                    if (ip.isOneToOneNat()) {
                        final StaticNatImpl staticNat = new StaticNatImpl(ip.getAccountId(), ip.getDomainId(), guestNetworkId, ip.getId(), ip.getVmIp(), false);
                        staticNats.add(staticNat);
                    }
                }
            }

            // Re-apply static nats
            s_logger.debug("Found " + staticNats.size() + " static nat(s) to apply as a part of domR " + router + " start.");
            if (!staticNats.isEmpty()) {
                _commandSetupHelper.createApplyStaticNatCommands(staticNats, router, cmds, guestNetworkId);
            }

            // Re-apply firewall Ingress rules
            s_logger.debug("Found " + firewallRulesIngress.size() + " firewall Ingress rule(s) to apply as a part of domR " + router + " start.");
            if (!firewallRulesIngress.isEmpty()) {
                _commandSetupHelper.createFirewallRulesCommands(firewallRulesIngress, router, cmds, guestNetworkId);
            }

            // Re-apply port forwarding rules
            s_logger.debug("Found " + pfRules.size() + " port forwarding rule(s) to apply as a part of domR " + router + " start.");
            if (!pfRules.isEmpty()) {
                _commandSetupHelper.createApplyPortForwardingRulesCommands(pfRules, router, cmds, guestNetworkId);
            }

            // Re-apply static nat rules
            s_logger.debug("Found " + staticNatFirewallRules.size() + " static nat rule(s) to apply as a part of domR " + router + " start.");
            if (!staticNatFirewallRules.isEmpty()) {
                final List<StaticNatRule> staticNatRules = new ArrayList<StaticNatRule>();
                for (final FirewallRule rule : staticNatFirewallRules) {
                    staticNatRules.add(_rulesMgr.buildStaticNatRule(rule, false));
                }
                _commandSetupHelper.createApplyStaticNatRulesCommands(staticNatRules, router, cmds, guestNetworkId);
            }

            // Re-apply vpn rules
            s_logger.debug("Found " + vpns.size() + " vpn(s) to apply as a part of domR " + router + " start.");
            if (!vpns.isEmpty()) {
                for (final RemoteAccessVpn vpn : vpns) {
                    _commandSetupHelper.createApplyVpnCommands(true, vpn, router, cmds);
                }
            }

            final List<LoadBalancerVO> lbs = _loadBalancerDao.listByNetworkIdAndScheme(guestNetworkId, Scheme.Public);
            final List<LoadBalancingRule> lbRules = new ArrayList<LoadBalancingRule>();
            if (_networkModel.isProviderSupportServiceInNetwork(guestNetworkId, Service.Lb, provider)) {
                // Re-apply load balancing rules
                for (final LoadBalancerVO lb : lbs) {
                    final List<LbDestination> dstList = _lbMgr.getExistingDestinations(lb.getId());
                    final List<LbStickinessPolicy> policyList = _lbMgr.getStickinessPolicies(lb.getId());
                    final List<LbHealthCheckPolicy> hcPolicyList = _lbMgr.getHealthCheckPolicies(lb.getId());
                    final Ip sourceIp = _networkModel.getPublicIpAddress(lb.getSourceIpAddressId()).getAddress();
                    final LbSslCert sslCert = _lbMgr.getLbSslCert(lb.getId());
                    final LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList, policyList, hcPolicyList, sourceIp, sslCert, lb.getLbProtocol());
                    lbRules.add(loadBalancing);
                }
            }

            s_logger.debug("Found " + lbRules.size() + " load balancing rule(s) to apply as a part of domR " + router + " start.");
            if (!lbRules.isEmpty()) {
                _commandSetupHelper.createApplyLoadBalancingRulesCommands(lbRules, router, cmds, guestNetworkId);
            }
        }
        // Reapply dhcp and dns configuration.
        final Network guestNetwork = _networkDao.findById(guestNetworkId);
        if (guestNetwork.getGuestType() == GuestType.Shared && _networkModel.isProviderSupportServiceInNetwork(guestNetworkId, Service.Dhcp, provider)) {
            final Map<Network.Capability, String> dhcpCapabilities = _networkSvc.getNetworkOfferingServiceCapabilities(
                    _networkOfferingDao.findById(_networkDao.findById(guestNetworkId).getNetworkOfferingId()), Service.Dhcp);
            final String supportsMultipleSubnets = dhcpCapabilities.get(Network.Capability.DhcpAccrossMultipleSubnets);
            if (supportsMultipleSubnets != null && Boolean.valueOf(supportsMultipleSubnets)) {
                final List<NicIpAliasVO> revokedIpAliasVOs = _nicIpAliasDao.listByNetworkIdAndState(guestNetworkId, NicIpAlias.state.revoked);
                s_logger.debug("Found" + revokedIpAliasVOs.size() + "ip Aliases to revoke on the router as a part of dhcp configuration");
                removeRevokedIpAliasFromDb(revokedIpAliasVOs);

                final List<NicIpAliasVO> aliasVOs = _nicIpAliasDao.listByNetworkIdAndState(guestNetworkId, NicIpAlias.state.active);
                s_logger.debug("Found" + aliasVOs.size() + "ip Aliases to apply on the router as a part of dhcp configuration");
                final List<IpAliasTO> activeIpAliasTOs = new ArrayList<IpAliasTO>();
                for (final NicIpAliasVO aliasVO : aliasVOs) {
                    activeIpAliasTOs.add(new IpAliasTO(aliasVO.getIp4Address(), aliasVO.getNetmask(), aliasVO.getAliasCount().toString()));
                }
                if (activeIpAliasTOs.size() != 0) {
                    _commandSetupHelper.createIpAlias(router, activeIpAliasTOs, guestNetworkId, cmds);
                    _commandSetupHelper.configDnsMasq(router, _networkDao.findById(guestNetworkId), cmds);
                }
            }
        }
    }

    private void createDefaultEgressFirewallRule(final List<FirewallRule> rules, final long networkId) {
        String systemRule = null;

        Boolean defaultEgressPolicy = false;
        final NetworkVO network = _networkDao.findById(networkId);
        final NetworkOfferingVO offering = _networkOfferingDao.findById(network.getNetworkOfferingId());
        defaultEgressPolicy = offering.getEgressDefaultPolicy();


        // construct rule when egress policy is true. In true case for VR we default allow rule need to be added
        if (!defaultEgressPolicy) {
            systemRule = String.valueOf(FirewallRule.FirewallRuleType.System);

            final List<String> sourceCidr = new ArrayList<String>();

            sourceCidr.add(NetUtils.ALL_CIDRS);
            final FirewallRule rule = new FirewallRuleVO(null, null, null, null, "all", networkId, network.getAccountId(), network.getDomainId(), Purpose.Firewall, sourceCidr,
                    null, null, null, FirewallRule.TrafficType.Egress, FirewallRule.FirewallRuleType.System);

            rules.add(rule);
        } else {
            s_logger.debug(" Egress policy for the Network "+ networkId +" is "+defaultEgressPolicy + " So no need"+
                    " of default rule is needed. ");
        }
    }


    private void removeRevokedIpAliasFromDb(final List<NicIpAliasVO> revokedIpAliasVOs) {
        for (final NicIpAliasVO ipalias : revokedIpAliasVOs) {
            _nicIpAliasDao.expunge(ipalias.getId());
        }
    }

    protected void finalizeIpAssocForNetwork(final Commands cmds, final VirtualRouter router, final Provider provider, final Long guestNetworkId,
            final Map<String, String> vlanMacAddress) {

        final ArrayList<? extends PublicIpAddress> publicIps = getPublicIpsToApply(router, provider, guestNetworkId);

        if (publicIps != null && !publicIps.isEmpty()) {
            s_logger.debug("Found " + publicIps.size() + " ip(s) to apply as a part of domR " + router + " start.");
            // Re-apply public ip addresses - should come before PF/LB/VPN
            if (_networkModel.isProviderSupportServiceInNetwork(guestNetworkId, Service.Firewall, provider)) {
                _commandSetupHelper.createAssociateIPCommands(router, publicIps, cmds, 0);
            }
        }
    }

    protected ArrayList<? extends PublicIpAddress> getPublicIpsToApply(final VirtualRouter router, final Provider provider, final Long guestNetworkId,
            final com.cloud.network.IpAddress.State... skipInStates) {
        final long ownerId = router.getAccountId();
        final List<? extends IpAddress> userIps;

        final Network guestNetwork = _networkDao.findById(guestNetworkId);
        if (guestNetwork.getGuestType() == GuestType.Shared) {
            // ignore the account id for the shared network
            userIps = _networkModel.listPublicIpsAssignedToGuestNtwk(guestNetworkId, null);
        } else {
            userIps = _networkModel.listPublicIpsAssignedToGuestNtwk(ownerId, guestNetworkId, null);
        }

        final List<PublicIp> allPublicIps = new ArrayList<PublicIp>();
        if (userIps != null && !userIps.isEmpty()) {
            boolean addIp = true;
            for (final IpAddress userIp : userIps) {
                if (skipInStates != null) {
                    for (final IpAddress.State stateToSkip : skipInStates) {
                        if (userIp.getState() == stateToSkip) {
                            s_logger.debug("Skipping ip address " + userIp + " in state " + userIp.getState());
                            addIp = false;
                            break;
                        }
                    }
                }

                if (addIp) {
                    final IPAddressVO ipVO = _ipAddressDao.findById(userIp.getId());
                    final PublicIp publicIp = PublicIp.createFromAddrAndVlan(ipVO, _vlanDao.findById(userIp.getVlanId()));
                    allPublicIps.add(publicIp);
                }
            }
        }

        // Get public Ips that should be handled by router
        final Network network = _networkDao.findById(guestNetworkId);
        final Map<PublicIpAddress, Set<Service>> ipToServices = _networkModel.getIpToServices(allPublicIps, false, true);
        final Map<Provider, ArrayList<PublicIpAddress>> providerToIpList = _networkModel.getProviderToIpList(network, ipToServices);
        // Only cover virtual router for now, if ELB use it this need to be
        // modified

        final ArrayList<PublicIpAddress> publicIps = providerToIpList.get(provider);
        return publicIps;
    }

    @Override
    public boolean finalizeStart(final VirtualMachineProfile profile, final long hostId, final Commands cmds, final ReservationContext context) {
        final DomainRouterVO router = _routerDao.findById(profile.getId());

        // process all the answers
        for (final Answer answer : cmds.getAnswers()) {
            // handle any command failures
            if (!answer.getResult()) {
                final String cmdClassName = answer.getClass().getCanonicalName().replace("Answer", "Command");
                final String errorMessage = "Command: " + cmdClassName + " failed while starting virtual router";
                final String errorDetails = "Details: " + answer.getDetails() + " " + answer.toString();
                // add alerts for the failed commands
                _alertMgr.sendAlert(AlertService.AlertType.ALERT_TYPE_DOMAIN_ROUTER, router.getDataCenterId(), router.getPodIdToDeployIn(), errorMessage, errorDetails);
                s_logger.error(answer.getDetails());
                s_logger.warn(errorMessage);
                // Stop the router if any of the commands failed
                return false;
            }
        }

        // at this point, all the router command are successful.
        boolean result = true;
        // Get guest networks info
        final List<Network> guestNetworks = new ArrayList<Network>();

        final List<? extends Nic> routerNics = _nicDao.listByVmId(profile.getId());
        for (final Nic nic : routerNics) {
            final Network network = _networkModel.getNetwork(nic.getNetworkId());

            final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());

            if (network.getTrafficType() == TrafficType.Guest) {
                guestNetworks.add(network);
                if (nic.getBroadcastUri().getScheme().equals("pvlan")) {
                    final NicProfile nicProfile = new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri(), 0, false, "pvlan-nic");

                    final NetworkTopology networkTopology = _networkTopologyContext.retrieveNetworkTopology(dcVO);
                    try {
                        result = networkTopology.setupDhcpForPvlan(true, router, router.getHostId(), nicProfile);
                    } catch (final ResourceUnavailableException e) {
                        s_logger.debug("ERROR in finalizeStart: ", e);
                    }
                }
            }
        }
        if (result) {
            final GetDomRVersionAnswer versionAnswer = (GetDomRVersionAnswer) cmds.getAnswer("getDomRVersion");
            router.setTemplateVersion(versionAnswer.getTemplateVersion());
            router.setScriptsVersion(versionAnswer.getScriptsVersion());
            _routerDao.persist(router, guestNetworks);
        }

        return result;
    }

    @Override
    public void finalizeStop(final VirtualMachineProfile profile, final Answer answer) {
        if (answer != null) {
            final VirtualMachine vm = profile.getVirtualMachine();
            final DomainRouterVO domR = _routerDao.findById(vm.getId());
            processStopOrRebootAnswer(domR, answer);
            final List<? extends Nic> routerNics = _nicDao.listByVmId(profile.getId());
            for (final Nic nic : routerNics) {
                final Network network = _networkModel.getNetwork(nic.getNetworkId());
                final DataCenterVO dcVO = _dcDao.findById(network.getDataCenterId());

                if (network.getTrafficType() == TrafficType.Guest && nic.getBroadcastUri() != null && nic.getBroadcastUri().getScheme().equals("pvlan")) {
                    final NicProfile nicProfile = new NicProfile(nic, network, nic.getBroadcastUri(), nic.getIsolationUri(), 0, false, "pvlan-nic");

                    final NetworkTopology networkTopology = _networkTopologyContext.retrieveNetworkTopology(dcVO);
                    try {
                        networkTopology.setupDhcpForPvlan(false, domR, domR.getHostId(), nicProfile);
                    } catch (final ResourceUnavailableException e) {
                        s_logger.debug("ERROR in finalizeStop: ", e);
                    }
                }
            }

        }
    }

    @Override
    public void finalizeExpunge(final VirtualMachine vm) {
    }

    @Override
    public boolean startRemoteAccessVpn(final Network network, final RemoteAccessVpn vpn, final List<? extends VirtualRouter> routers) throws ResourceUnavailableException {
        if (routers == null || routers.isEmpty()) {
            s_logger.warn("Failed to start remote access VPN: no router found for account and zone");
            throw new ResourceUnavailableException("Failed to start remote access VPN: no router found for account and zone", DataCenter.class, network.getDataCenterId());
        }

        for (final VirtualRouter router : routers) {
            if (router.getState() != State.Running) {
                s_logger.warn("Failed to start remote access VPN: router not in right state " + router.getState());
                throw new ResourceUnavailableException("Failed to start remote access VPN: router not in right state " + router.getState(), DataCenter.class,
                        network.getDataCenterId());
            }

            final Commands cmds = new Commands(Command.OnError.Stop);
            _commandSetupHelper.createApplyVpnCommands(true, vpn, router, cmds);

            if (!_nwHelper.sendCommandsToRouter(router, cmds)) {
                throw new AgentUnavailableException("Unable to send commands to virtual router ", router.getHostId());
            }

            Answer answer = cmds.getAnswer("users");
            if (!answer.getResult()) {
                s_logger.error("Unable to start vpn: unable add users to vpn in zone " + router.getDataCenterId() + " for account " + vpn.getAccountId() + " on domR: "
                        + router.getInstanceName() + " due to " + answer.getDetails());
                throw new ResourceUnavailableException("Unable to start vpn: Unable to add users to vpn in zone " + router.getDataCenterId() + " for account "
                        + vpn.getAccountId() + " on domR: " + router.getInstanceName() + " due to " + answer.getDetails(), DataCenter.class, router.getDataCenterId());
            }
            answer = cmds.getAnswer("startVpn");
            if (!answer.getResult()) {
                s_logger.error("Unable to start vpn in zone " + router.getDataCenterId() + " for account " + vpn.getAccountId() + " on domR: " + router.getInstanceName()
                        + " due to " + answer.getDetails());
                throw new ResourceUnavailableException("Unable to start vpn in zone " + router.getDataCenterId() + " for account " + vpn.getAccountId() + " on domR: "
                        + router.getInstanceName() + " due to " + answer.getDetails(), DataCenter.class, router.getDataCenterId());
            }

        }
        return true;
    }

    @Override
    public boolean deleteRemoteAccessVpn(final Network network, final RemoteAccessVpn vpn, final List<? extends VirtualRouter> routers) throws ResourceUnavailableException {
        if (routers == null || routers.isEmpty()) {
            s_logger.warn("Failed to delete remote access VPN: no router found for account and zone");
            throw new ResourceUnavailableException("Failed to delete remote access VPN", DataCenter.class, network.getDataCenterId());
        }

        boolean result = true;
        for (final VirtualRouter router : routers) {
            if (router.getState() == State.Running) {
                final Commands cmds = new Commands(Command.OnError.Continue);
                _commandSetupHelper.createApplyVpnCommands(false, vpn, router, cmds);
                result = result && _nwHelper.sendCommandsToRouter(router, cmds);
            } else if (router.getState() == State.Stopped) {
                s_logger.debug("Router " + router + " is in Stopped state, not sending deleteRemoteAccessVpn command to it");
                continue;
            } else {
                s_logger.warn("Failed to delete remote access VPN: domR " + router + " is not in right state " + router.getState());
                throw new ResourceUnavailableException("Failed to delete remote access VPN: domR is not in right state " + router.getState(), DataCenter.class,
                        network.getDataCenterId());
            }
        }

        return result;
    }

    @Override
    public DomainRouterVO stop(final VirtualRouter router, final boolean forced, final User user, final Account caller) throws ConcurrentOperationException,
    ResourceUnavailableException {
        s_logger.debug("Stopping router " + router);
        try {
            _itMgr.advanceStop(router.getUuid(), forced);
            return _routerDao.findById(router.getId());
        } catch (final OperationTimedoutException e) {
            throw new CloudRuntimeException("Unable to stop " + router, e);
        }
    }

    @Override
    public boolean removeDhcpSupportForSubnet(final Network network, final List<DomainRouterVO> routers) throws ResourceUnavailableException {
        if (routers == null || routers.isEmpty()) {
            s_logger.warn("Failed to add/remove VPN users: no router found for account and zone");
            throw new ResourceUnavailableException("Unable to assign ip addresses, domR doesn't exist for network " + network.getId(), DataCenter.class, network.getDataCenterId());
        }

        for (final DomainRouterVO router : routers) {
            if (router.getState() != State.Running) {
                s_logger.warn("Failed to add/remove VPN users: router not in running state");
                throw new ResourceUnavailableException("Unable to assign ip addresses, domR is not in right state " + router.getState(), DataCenter.class,
                        network.getDataCenterId());
            }

            final Commands cmds = new Commands(Command.OnError.Continue);
            final List<NicIpAliasVO> revokedIpAliasVOs = _nicIpAliasDao.listByNetworkIdAndState(network.getId(), NicIpAlias.state.revoked);
            s_logger.debug("Found" + revokedIpAliasVOs.size() + "ip Aliases to revoke on the router as a part of dhcp configuration");
            final List<IpAliasTO> revokedIpAliasTOs = new ArrayList<IpAliasTO>();
            for (final NicIpAliasVO revokedAliasVO : revokedIpAliasVOs) {
                revokedIpAliasTOs.add(new IpAliasTO(revokedAliasVO.getIp4Address(), revokedAliasVO.getNetmask(), revokedAliasVO.getAliasCount().toString()));
            }
            final List<NicIpAliasVO> aliasVOs = _nicIpAliasDao.listByNetworkIdAndState(network.getId(), NicIpAlias.state.active);
            s_logger.debug("Found" + aliasVOs.size() + "ip Aliases to apply on the router as a part of dhcp configuration");
            final List<IpAliasTO> activeIpAliasTOs = new ArrayList<IpAliasTO>();
            for (final NicIpAliasVO aliasVO : aliasVOs) {
                activeIpAliasTOs.add(new IpAliasTO(aliasVO.getIp4Address(), aliasVO.getNetmask(), aliasVO.getAliasCount().toString()));
            }
            _commandSetupHelper.createDeleteIpAliasCommand(router, revokedIpAliasTOs, activeIpAliasTOs, network.getId(), cmds);
            _commandSetupHelper.configDnsMasq(router, network, cmds);
            final boolean result = _nwHelper.sendCommandsToRouter(router, cmds);
            if (result) {
                Transaction.execute(new TransactionCallbackNoReturn() {
                    @Override
                    public void doInTransactionWithoutResult(final TransactionStatus status) {
                        for (final NicIpAliasVO revokedAliasVO : revokedIpAliasVOs) {
                            _nicIpAliasDao.expunge(revokedAliasVO.getId());
                        }
                    }
                });
                return true;
            }
        }
        return false;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_ROUTER_START, eventDescription = "starting router Vm", async = true)
    public VirtualRouter startRouter(final long id) throws ResourceUnavailableException, InsufficientCapacityException, ConcurrentOperationException {
        return startRouter(id, true);
    }

    @Override
    public VirtualRouter startRouter(final long routerId, final boolean reprogramNetwork) throws ResourceUnavailableException, InsufficientCapacityException,
    ConcurrentOperationException {
        final Account caller = CallContext.current().getCallingAccount();
        final User callerUser = _accountMgr.getActiveUser(CallContext.current().getCallingUserId());

        // verify parameters
        DomainRouterVO router = _routerDao.findById(routerId);
        if (router == null) {
            throw new InvalidParameterValueException("Unable to find router by id " + routerId + ".");
        }
        _accountMgr.checkAccess(caller, null, true, router);

        final Account owner = _accountMgr.getAccount(router.getAccountId());

        // Check if all networks are implemented for the domR; if not -
        // implement them
        final DataCenter dc = _dcDao.findById(router.getDataCenterId());
        HostPodVO pod = null;
        if (router.getPodIdToDeployIn() != null) {
            pod = _podDao.findById(router.getPodIdToDeployIn());
        }
        final DeployDestination dest = new DeployDestination(dc, pod, null, null);

        final ReservationContext context = new ReservationContextImpl(null, null, callerUser, owner);

        final List<NicVO> nics = _nicDao.listByVmId(routerId);

        for (final NicVO nic : nics) {
            if (!_networkMgr.startNetwork(nic.getNetworkId(), dest, context)) {
                s_logger.warn("Failed to start network id=" + nic.getNetworkId() + " as a part of domR start");
                throw new CloudRuntimeException("Failed to start network id=" + nic.getNetworkId() + " as a part of domR start");
            }
        }

        // After start network, check if it's already running
        router = _routerDao.findById(routerId);
        if (router.getState() == State.Running) {
            return router;
        }

        final UserVO user = _userDao.findById(CallContext.current().getCallingUserId());
        final Map<Param, Object> params = new HashMap<Param, Object>();
        if (reprogramNetwork) {
            params.put(Param.ReProgramGuestNetworks, true);
        } else {
            params.put(Param.ReProgramGuestNetworks, false);
        }
        final VirtualRouter virtualRouter = _nwHelper.startVirtualRouter(router, user, caller, params);
        if (virtualRouter == null) {
            throw new CloudRuntimeException("Failed to start router with id " + routerId);
        }
        return virtualRouter;
    }

    @Override
    public List<VirtualRouter> getRoutersForNetwork(final long networkId) {
        final List<DomainRouterVO> routers = _routerDao.findByNetwork(networkId);
        final List<VirtualRouter> vrs = new ArrayList<VirtualRouter>(routers.size());
        for (final DomainRouterVO router : routers) {
            vrs.add(router);
        }
        return vrs;
    }

    @Override
    public String getDnsBasicZoneUpdate() {
        return _dnsBasicZoneUpdates;
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
    public boolean processAnswers(final long agentId, final long seq, final Answer[] answers) {
        return false;
    }

    @Override
    public boolean processCommands(final long agentId, final long seq, final Command[] commands) {
        return false;
    }

    @Override
    public void processConnect(final Host host, final StartupCommand cmd, final boolean forRebalance) throws ConnectionException {
        final List<DomainRouterVO> routers = _routerDao.listIsolatedByHostId(host.getId());
        for (DomainRouterVO router : routers) {
            if (router.isStopPending()) {
                s_logger.info("Stopping router " + router.getInstanceName() + " due to stop pending flag found!");
                final State state = router.getState();
                if (state != State.Stopped && state != State.Destroyed) {
                    try {
                        stopRouter(router.getId(), false);
                    } catch (final ResourceUnavailableException e) {
                        s_logger.warn("Fail to stop router " + router.getInstanceName(), e);
                        throw new ConnectionException(false, "Fail to stop router " + router.getInstanceName());
                    } catch (final ConcurrentOperationException e) {
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
    public AgentControlAnswer processControlCommand(final long agentId, final AgentControlCommand cmd) {
        return null;
    }

    @Override
    public boolean processDisconnect(final long agentId, final Status state) {
        return false;
    }

    @Override
    public boolean processTimeout(final long agentId, final long seq) {
        return false;
    }

    @Override
    public void prepareStop(final VirtualMachineProfile profile) {
        // Collect network usage before stopping Vm

        final DomainRouterVO router = _routerDao.findById(profile.getVirtualMachine().getId());
        if (router == null) {
            return;
        }

        final String privateIP = router.getPrivateIpAddress();

        if (privateIP != null) {
            final boolean forVpc = router.getVpcId() != null;
            final List<? extends Nic> routerNics = _nicDao.listByVmId(router.getId());
            for (final Nic routerNic : routerNics) {
                final Network network = _networkModel.getNetwork(routerNic.getNetworkId());
                // Send network usage command for public nic in VPC VR
                // Send network usage command for isolated guest nic of non VPC
                // VR
                if (forVpc && network.getTrafficType() == TrafficType.Public || !forVpc && network.getTrafficType() == TrafficType.Guest
                        && network.getGuestType() == Network.GuestType.Isolated) {
                    final NetworkUsageCommand usageCmd = new NetworkUsageCommand(privateIP, router.getHostName(), forVpc, routerNic.getIp4Address());
                    final String routerType = router.getType().toString();
                    final UserStatisticsVO previousStats = _userStatsDao.findBy(router.getAccountId(), router.getDataCenterId(), network.getId(),
                            forVpc ? routerNic.getIp4Address() : null, router.getId(), routerType);
                    NetworkUsageAnswer answer = null;
                    try {
                        answer = (NetworkUsageAnswer) _agentMgr.easySend(router.getHostId(), usageCmd);
                    } catch (final Exception e) {
                        s_logger.warn("Error while collecting network stats from router: " + router.getInstanceName() + " from host: " + router.getHostId(), e);
                        continue;
                    }

                    if (answer != null) {
                        if (!answer.getResult()) {
                            s_logger.warn("Error while collecting network stats from router: " + router.getInstanceName() + " from host: " + router.getHostId() + "; details: "
                                    + answer.getDetails());
                            continue;
                        }
                        try {
                            if (answer.getBytesReceived() == 0 && answer.getBytesSent() == 0) {
                                s_logger.debug("Recieved and Sent bytes are both 0. Not updating user_statistics");
                                continue;
                            }

                            final NetworkUsageAnswer answerFinal = answer;
                            Transaction.execute(new TransactionCallbackNoReturn() {
                                @Override
                                public void doInTransactionWithoutResult(final TransactionStatus status) {
                                    final UserStatisticsVO stats = _userStatsDao.lock(router.getAccountId(), router.getDataCenterId(), network.getId(),
                                            forVpc ? routerNic.getIp4Address() : null, router.getId(), routerType);
                                    if (stats == null) {
                                        s_logger.warn("unable to find stats for account: " + router.getAccountId());
                                        return;
                                    }

                                    if (previousStats != null
                                            && (previousStats.getCurrentBytesReceived() != stats.getCurrentBytesReceived() || previousStats.getCurrentBytesSent() != stats
                                            .getCurrentBytesSent())) {
                                        s_logger.debug("Router stats changed from the time NetworkUsageCommand was sent. " + "Ignoring current answer. Router: "
                                                + answerFinal.getRouterName() + " Rcvd: " + answerFinal.getBytesReceived() + "Sent: " + answerFinal.getBytesSent());
                                        return;
                                    }

                                    if (stats.getCurrentBytesReceived() > answerFinal.getBytesReceived()) {
                                        if (s_logger.isDebugEnabled()) {
                                            s_logger.debug("Received # of bytes that's less than the last one.  " + "Assuming something went wrong and persisting it. Router: "
                                                    + answerFinal.getRouterName() + " Reported: " + answerFinal.getBytesReceived() + " Stored: " + stats.getCurrentBytesReceived());
                                        }
                                        stats.setNetBytesReceived(stats.getNetBytesReceived() + stats.getCurrentBytesReceived());
                                    }
                                    stats.setCurrentBytesReceived(answerFinal.getBytesReceived());
                                    if (stats.getCurrentBytesSent() > answerFinal.getBytesSent()) {
                                        if (s_logger.isDebugEnabled()) {
                                            s_logger.debug("Received # of bytes that's less than the last one.  " + "Assuming something went wrong and persisting it. Router: "
                                                    + answerFinal.getRouterName() + " Reported: " + answerFinal.getBytesSent() + " Stored: " + stats.getCurrentBytesSent());
                                        }
                                        stats.setNetBytesSent(stats.getNetBytesSent() + stats.getCurrentBytesSent());
                                    }
                                    stats.setCurrentBytesSent(answerFinal.getBytesSent());
                                    if (!_dailyOrHourly) {
                                        // update agg bytes
                                        stats.setAggBytesSent(stats.getNetBytesSent() + stats.getCurrentBytesSent());
                                        stats.setAggBytesReceived(stats.getNetBytesReceived() + stats.getCurrentBytesReceived());
                                    }
                                    _userStatsDao.update(stats.getId(), stats);
                                }
                            });
                        } catch (final Exception e) {
                            s_logger.warn("Unable to update user statistics for account: " + router.getAccountId() + " Rx: " + answer.getBytesReceived() + "; Tx: "
                                    + answer.getBytesSent());
                        }
                    }
                }
            }
        }
    }

    @Override
    public VirtualRouter findRouter(final long routerId) {
        return _routerDao.findById(routerId);
    }

    @Override
    public List<Long> upgradeRouterTemplate(final UpgradeRouterTemplateCmd cmd) {

        List<DomainRouterVO> routers = new ArrayList<DomainRouterVO>();
        int params = 0;

        final Long routerId = cmd.getId();
        if (routerId != null) {
            params++;
            final DomainRouterVO router = _routerDao.findById(routerId);
            if (router != null) {
                routers.add(router);
            }
        }

        final Long domainId = cmd.getDomainId();
        if (domainId != null) {
            final String accountName = cmd.getAccount();
            // List by account, if account Name is specified along with domainId
            if (accountName != null) {
                final Account account = _accountMgr.getActiveAccountByName(accountName, domainId);
                if (account == null) {
                    throw new InvalidParameterValueException("Account :" + accountName + " does not exist in domain: " + domainId);
                }
                routers = _routerDao.listRunningByAccountId(account.getId());
            } else {
                // List by domainId, account name not specified
                routers = _routerDao.listRunningByDomain(domainId);
            }
            params++;
        }

        final Long clusterId = cmd.getClusterId();
        if (clusterId != null) {
            params++;
            routers = _routerDao.listRunningByClusterId(clusterId);
        }

        final Long podId = cmd.getPodId();
        if (podId != null) {
            params++;
            routers = _routerDao.listRunningByPodId(podId);
        }

        final Long zoneId = cmd.getZoneId();
        if (zoneId != null) {
            params++;
            routers = _routerDao.listRunningByDataCenter(zoneId);
        }

        if (params > 1) {
            throw new InvalidParameterValueException("Multiple parameters not supported. Specify only one among routerId/zoneId/podId/clusterId/accountId/domainId");
        }

        if (routers != null) {
            return rebootRouters(routers);
        }

        return null;
    }

    private List<Long> rebootRouters(final List<DomainRouterVO> routers) {
        final List<Long> jobIds = new ArrayList<Long>();
        for (final DomainRouterVO router : routers) {
            if (!_nwHelper.checkRouterVersion(router)) {
                s_logger.debug("Upgrading template for router: " + router.getId());
                final Map<String, String> params = new HashMap<String, String>();
                params.put("ctxUserId", "1");
                params.put("ctxAccountId", "" + router.getAccountId());

                final RebootRouterCmd cmd = new RebootRouterCmd();
                ComponentContext.inject(cmd);
                params.put("id", "" + router.getId());
                params.put("ctxStartEventId", "1");
                final AsyncJobVO job = new AsyncJobVO("", User.UID_SYSTEM, router.getAccountId(), RebootRouterCmd.class.getName(), ApiGsonHelper.getBuilder().create().toJson(params),
                        router.getId(), cmd.getInstanceType() != null ? cmd.getInstanceType().toString() : null, null);
                job.setDispatcher(_asyncDispatcher.getName());
                final long jobId = _asyncMgr.submitAsyncJob(job);
                jobIds.add(jobId);
            } else {
                s_logger.debug("Router: " + router.getId() + " is already at the latest version. No upgrade required");
            }
        }
        return jobIds;
    }

    @Override
    public String getConfigComponentName() {
        return VirtualNetworkApplianceManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] { UseExternalDnsServers, routerVersionCheckEnabled, SetServiceMonitor, RouterAlertsCheckInterval };
    }

    @Override
    public boolean preStateTransitionEvent(final State oldState, final VirtualMachine.Event event, final State newState, final VirtualMachine vo, final boolean status,
            final Object opaque) {
        return true;
    }

    @Override
    public boolean postStateTransitionEvent(final StateMachine2.Transition<State, VirtualMachine.Event> transition, final VirtualMachine vo, final boolean status, final Object opaque) {
        final State newState = transition.getToState();
        final VirtualMachine.Event event = transition.getEvent();
        if (event == VirtualMachine.Event.FollowAgentPowerOnReport && newState == State.Running) {
            if (vo.getType() == VirtualMachine.Type.DomainRouter) {
                // opaque -> <hostId, powerHostId>
                if (opaque != null && opaque instanceof Pair<?, ?>) {
                    final Pair<?, ?> pair = (Pair<?, ?>)opaque;
                    final Object first = pair.first();
                    final Object second = pair.second();
                    // powerHostId cannot be null in case of out-of-band VM movement
                    if (second != null && second instanceof Long) {
                        final Long powerHostId = (Long)second;
                        Long hostId = null;
                        if (first != null && first instanceof Long) {
                            hostId = (Long)first;
                        }
                        // The following scenarios are due to out-of-band VM movement
                        // 1. If VM is in stopped state in CS due to 'PowerMissing' report from old host (hostId is null) and then there is a 'PowerOn' report from new host
                        // 2. If VM is in running state in CS and there is a 'PowerOn' report from new host
                        if (hostId == null || hostId.longValue() != powerHostId.longValue()) {
                            s_logger.info("Schedule a router reboot task as router " + vo.getId() + " is powered-on out-of-band, need to reboot to refresh network rules");
                            _executor.schedule(new RebootTask(vo.getId()), 1000, TimeUnit.MICROSECONDS);
                        }
                    }
                }
            }
        }
        return true;
    }

    protected class RebootTask extends ManagedContextRunnable {

        long _routerId;

        public RebootTask(final long routerId) {
            _routerId = routerId;
        }

        @Override
        protected void runInContext() {
            try {
                s_logger.info("Reboot router " + _routerId + " to refresh network rules");
                rebootRouter(_routerId, true);
            } catch (final Exception e) {
                s_logger.warn("Error while rebooting the router", e);
            }
        }
    }

    protected boolean aggregationExecution(final AggregationControlCommand.Action action, final Network network, final List<DomainRouterVO> routers)
            throws AgentUnavailableException, ResourceUnavailableException {

        int errors = 0;

        for (final DomainRouterVO router : routers) {

            final String routerControlIp = _routerControlHelper.getRouterControlIp(router.getId());
            final String routerIpInNetwork = _routerControlHelper.getRouterIpInNetwork(network.getId(), router.getId());

            if (routerIpInNetwork == null) {
                // Nic hasn't been created in this router yet. Try to configure the next one.
                s_logger.warn("The Network is not configured in the router " + router.getHostName() + " yet. Try the next router!");
                errors++;
                continue;
            }

            final AggregationControlCommand cmd = new AggregationControlCommand(action, router.getInstanceName(), routerControlIp, routerIpInNetwork);
            final Commands cmds = new Commands(cmd);
            if (!_nwHelper.sendCommandsToRouter(router, cmds)) {
                return false;
            }
        }
        if (errors == routers.size()) {
            s_logger.error("aggregationExecution() on " + getClass().getName() + " failed! Network is not configured in any router.");
            return false;
        }
        return true;
    }

    @Override
    public boolean prepareAggregatedExecution(final Network network, final List<DomainRouterVO> routers) throws AgentUnavailableException, ResourceUnavailableException {
        return aggregationExecution(Action.Start, network, routers);
    }

    @Override
    public boolean completeAggregatedExecution(final Network network, final List<DomainRouterVO> routers) throws AgentUnavailableException, ResourceUnavailableException {
        return aggregationExecution(Action.Finish, network, routers);
    }
}