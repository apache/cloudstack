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
package com.cloud.network.lb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.user.loadbalancer.CreateLoadBalancerRuleCmd;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.manager.Commands;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Pod;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.Vlan.VlanType;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.PodVlanMapDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.ElasticLbVmMapVO;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VirtualRouterProvider.VirtualRouterProviderType;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbHealthCheckPolicy;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.lb.dao.ElasticLbVmMapDao;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.RedundantState;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.router.VpcVirtualNetworkApplianceManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
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

@Component
@Local(value = { ElasticLoadBalancerManager.class })
public class ElasticLoadBalancerManagerImpl extends ManagerBase implements
ElasticLoadBalancerManager, VirtualMachineGuru<DomainRouterVO> {
    private static final Logger s_logger = Logger
            .getLogger(ElasticLoadBalancerManagerImpl.class);
    
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    NetworkModel _networkModel;
    @Inject
    NetworkManager _networkMgr;
    @Inject
    LoadBalancerDao _loadBalancerDao = null;
    @Inject
    LoadBalancingRulesManager _lbMgr;
    @Inject
    VpcVirtualNetworkApplianceManager _routerMgr;
    @Inject
    DomainRouterDao _routerDao = null;
    @Inject
    protected HostPodDao _podDao = null;
    @Inject
    protected ClusterDao _clusterDao;
    @Inject
    DataCenterDao _dcDao = null;
    @Inject
    protected NetworkDao _networkDao;
    @Inject
    protected NetworkOfferingDao _networkOfferingDao;
    @Inject
    VMTemplateDao _templateDao = null;
    @Inject
    VirtualMachineManager _itMgr;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    ServiceOfferingDao _serviceOfferingDao = null;
    @Inject
    AccountService _accountService;
    @Inject
    LoadBalancerDao _lbDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    PodVlanMapDao _podVlanMapDao;
    @Inject
    ElasticLbVmMapDao _elbVmMapDao;
    @Inject 
    NetworkDao _networksDao;
    @Inject 
    AccountDao _accountDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalProviderDao;
    @Inject
    VirtualRouterProviderDao _vrProviderDao;
    @Inject
    NicDao _nicDao;

    String _instance;
    static final private String _elbVmNamePrefix = "l";
    static final private String _systemVmType = "elbvm";
    
    boolean _enabled;
    TrafficType _frontendTrafficType = TrafficType.Guest;

    Account _systemAcct;
    ServiceOfferingVO _elasticLbVmOffering;
    ScheduledExecutorService _gcThreadPool;
    String _mgmtCidr;
    String _mgmtHost;
    
    Set<Long> _gcCandidateElbVmIds = Collections.newSetFromMap(new ConcurrentHashMap<Long,Boolean>());
    
    int _elasticLbVmRamSize;
    int _elasticLbvmCpuMHz;
    int _elasticLbvmNumCpu;
    
    private Long getPodIdForDirectIp(IPAddressVO ipAddr) {
        PodVlanMapVO podVlanMaps = _podVlanMapDao.listPodVlanMapsByVlan(ipAddr.getVlanId());
        if (podVlanMaps == null) {
            return null;
        } else {
            return podVlanMaps.getPodId();
        }
    }

    public DomainRouterVO deployLoadBalancerVM(Long networkId, IPAddressVO ipAddr, Long accountId) {  
        NetworkVO network = _networkDao.findById(networkId);
        DataCenter dc = _dcDao.findById(network.getDataCenterId());
        Long podId = getPodIdForDirectIp(ipAddr);
        Pod pod = podId == null?null:_podDao.findById(podId);
        Map<VirtualMachineProfile.Param, Object> params = new HashMap<VirtualMachineProfile.Param, Object>(
                1);
        params.put(VirtualMachineProfile.Param.ReProgramGuestNetworks, true);
        Account owner = _accountService.getActiveAccountByName("system", new Long(1));
        DeployDestination dest = new DeployDestination(dc, pod, null, null);
        s_logger.debug("About to deploy ELB vm ");

        try {
            DomainRouterVO elbVm = deployELBVm(network, dest, owner, params);
            if (elbVm == null) {
                throw new InvalidParameterValueException("Could not deploy or find existing ELB VM");
            }
            s_logger.debug("Deployed ELB  vm = " + elbVm);

            return elbVm;
           
        } catch (Throwable t) {
            s_logger.warn("Error while deploying ELB VM:  ", t);
            return null;
        }

    }
    
    private boolean sendCommandsToRouter(final DomainRouterVO elbVm,
            Commands cmds) throws AgentUnavailableException {
        Answer[] answers = null;
        try {
            answers = _agentMgr.send(elbVm.getHostId(), cmds);
        } catch (OperationTimedoutException e) {
            s_logger.warn("ELB: Timed Out", e);
            throw new AgentUnavailableException(
                    "Unable to send commands to virtual elbVm ",
                    elbVm.getHostId(), e);
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

    private void createApplyLoadBalancingRulesCommands(
            List<LoadBalancingRule> rules, DomainRouterVO elbVm, Commands cmds, long guestNetworkId) {

        LoadBalancerTO[] lbs = new LoadBalancerTO[rules.size()];
        int i = 0;
        for (LoadBalancingRule rule : rules) {
            boolean revoked = (rule.getState()
                    .equals(FirewallRule.State.Revoke));
            String protocol = rule.getProtocol();
            String algorithm = rule.getAlgorithm();

            String elbIp = _networkModel.getIp(rule.getSourceIpAddressId()).getAddress()
                    .addr();
            int srcPort = rule.getSourcePortStart();
            String uuid = rule.getUuid();
            List<LbDestination> destinations = rule.getDestinations();
            LoadBalancerTO lb = new LoadBalancerTO(uuid, elbIp, srcPort, protocol, algorithm, revoked, false, false, destinations);
            lbs[i++] = lb; 
        }

        LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(lbs,elbVm.getPublicIpAddress(),
                _nicDao.getIpAddress(guestNetworkId, elbVm.getId()),elbVm.getPrivateIpAddress(), null, null);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP,
                elbVm.getPrivateIpAddress());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME,
                elbVm.getInstanceName());
        //FIXME: why are we setting attributes directly? Ick!! There should be accessors and
        //the constructor should set defaults.
        cmd.lbStatsVisibility = _configDao.getValue(Config.NetworkLBHaproxyStatsVisbility.key());
        cmd.lbStatsUri = _configDao.getValue(Config.NetworkLBHaproxyStatsUri.key());
        cmd.lbStatsAuth = _configDao.getValue(Config.NetworkLBHaproxyStatsAuth.key());
        cmd.lbStatsPort = _configDao.getValue(Config.NetworkLBHaproxyStatsPort.key());
   
        cmds.addCommand(cmd);

    }

    protected boolean applyLBRules(DomainRouterVO elbVm,
            List<LoadBalancingRule> rules, long guestNetworkId) throws ResourceUnavailableException {
        Commands cmds = new Commands(OnError.Continue);
        createApplyLoadBalancingRulesCommands(rules, elbVm, cmds, guestNetworkId);
        // Send commands to elbVm
        return sendCommandsToRouter(elbVm, cmds);
    }
    
    protected DomainRouterVO findElbVmForLb(FirewallRule lb) {//TODO: use a table to lookup
        ElasticLbVmMapVO map = _elbVmMapDao.findOneByIp(lb.getSourceIpAddressId());
        if (map == null) {
            return null;
        }
        DomainRouterVO elbVm = _routerDao.findById(map.getElbVmId());
        return elbVm;
    }

    @Override
    public boolean applyLoadBalancerRules(Network network,
            List<? extends FirewallRule> rules)
            throws ResourceUnavailableException {
        if (rules == null || rules.isEmpty()) {
            return true;
        }
        if (rules.get(0).getPurpose() != Purpose.LoadBalancing) {
            s_logger.warn("ELB: Not handling non-LB firewall rules");
            return false;
        }
        
        DomainRouterVO elbVm = findElbVmForLb(rules.get(0));
                                                                          
        if (elbVm == null) {
            s_logger.warn("Unable to apply lb rules, ELB vm  doesn't exist in the network "
                    + network.getId());
            throw new ResourceUnavailableException("Unable to apply lb rules",
                    DataCenter.class, network.getDataCenterId());
        }

        if (elbVm.getState() == State.Running) {
            //resend all rules for the public ip
            List<LoadBalancerVO> lbs = _lbDao.listByIpAddress(rules.get(0).getSourceIpAddressId());
            List<LoadBalancingRule> lbRules = new ArrayList<LoadBalancingRule>();
            for (LoadBalancerVO lb : lbs) {
                List<LbDestination> dstList = _lbMgr.getExistingDestinations(lb.getId());
                List<LbStickinessPolicy> policyList = _lbMgr.getStickinessPolicies(lb.getId());
                List<LbHealthCheckPolicy> hcPolicyList = _lbMgr.getHealthCheckPolicies(lb.getId());
                LoadBalancingRule loadBalancing = new LoadBalancingRule(
                        lb, dstList, policyList, hcPolicyList);
                lbRules.add(loadBalancing);
            }
            return applyLBRules(elbVm, lbRules, network.getId());
        } else if (elbVm.getState() == State.Stopped
                || elbVm.getState() == State.Stopping) {
            s_logger.debug("ELB VM is in "
                    + elbVm.getState()
                    + ", so not sending apply LoadBalancing rules commands to the backend");
            return true;
        } else {
            s_logger.warn("Unable to apply loadbalancing rules, ELB VM is not in the right state "
                    + elbVm.getState());
            throw new ResourceUnavailableException(
                    "Unable to apply loadbalancing rules, ELB VM is not in the right state",
                    VirtualRouter.class, elbVm.getId());
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        final Map<String, String> configs = _configDao.getConfiguration("AgentManager", params);
        _systemAcct = _accountService.getSystemAccount();
        _instance = configs.get("instance.name");
        if (_instance == null) {
            _instance = "VM";
        }
        _mgmtCidr = _configDao.getValue(Config.ManagementNetwork.key());
        _mgmtHost = _configDao.getValue(Config.ManagementHostIPAdr.key());
        
        boolean useLocalStorage = Boolean.parseBoolean(configs.get(Config.SystemVMUseLocalStorage.key()));

        _elasticLbVmRamSize = NumbersUtil.parseInt(configs.get(Config.ElasticLoadBalancerVmMemory.key()), DEFAULT_ELB_VM_RAMSIZE);
        _elasticLbvmCpuMHz = NumbersUtil.parseInt(configs.get(Config.ElasticLoadBalancerVmCpuMhz.key()), DEFAULT_ELB_VM_CPU_MHZ);
        _elasticLbvmNumCpu = NumbersUtil.parseInt(configs.get(Config.ElasticLoadBalancerVmNumVcpu.key()), 1);
        _elasticLbVmOffering = new ServiceOfferingVO("System Offering For Elastic LB VM", _elasticLbvmNumCpu, 
                _elasticLbVmRamSize, _elasticLbvmCpuMHz, 0, 0, true, null, useLocalStorage, 
                true, null, true, VirtualMachine.Type.ElasticLoadBalancerVm, true);
        _elasticLbVmOffering.setUniqueName(ServiceOffering.elbVmDefaultOffUniqueName);
        _elasticLbVmOffering = _serviceOfferingDao.persistSystemServiceOffering(_elasticLbVmOffering);
        
        String enabled = _configDao.getValue(Config.ElasticLoadBalancerEnabled.key());
        _enabled = (enabled == null) ? false: Boolean.parseBoolean(enabled);
        s_logger.info("Elastic Load balancer enabled: " + _enabled);
        if (_enabled) {
            String traffType = _configDao.getValue(Config.ElasticLoadBalancerNetwork.key());
            if ("guest".equalsIgnoreCase(traffType)) {
                _frontendTrafficType = TrafficType.Guest;
            } else if ("public".equalsIgnoreCase(traffType)){
                _frontendTrafficType = TrafficType.Public;
            } else
                throw new ConfigurationException("ELB: Traffic type for front end of load balancer has to be guest or public; found : " + traffType);
            s_logger.info("ELB: Elastic Load Balancer: will balance on " + traffType );
            int gcIntervalMinutes =  NumbersUtil.parseInt(configs.get(Config.ElasticLoadBalancerVmGcInterval.key()), 5);
            if (gcIntervalMinutes < 5) 
                gcIntervalMinutes = 5;
            s_logger.info("ELB: Elastic Load Balancer: scheduling GC to run every " + gcIntervalMinutes + " minutes" );
            _gcThreadPool = Executors.newScheduledThreadPool(1, new NamedThreadFactory("ELBVM-GC"));
            _gcThreadPool.scheduleAtFixedRate(new CleanupThread(), gcIntervalMinutes, gcIntervalMinutes, TimeUnit.MINUTES);
            _itMgr.registerGuru(VirtualMachine.Type.ElasticLoadBalancerVm, this);
        }
        
        return true;
    }

    private DomainRouterVO findELBVmWithCapacity(Network guestNetwork, IPAddressVO ipAddr) {
        List<DomainRouterVO> unusedElbVms = _elbVmMapDao.listUnusedElbVms();
        if (unusedElbVms.size() > 0) {
            List<DomainRouterVO> candidateVms = new ArrayList<DomainRouterVO>();
            for (DomainRouterVO candidateVm: unusedElbVms) {
                if (candidateVm.getPodIdToDeployIn() == getPodIdForDirectIp(ipAddr))
                    candidateVms.add(candidateVm);
            }
            return candidateVms.size()==0?null:candidateVms.get(new Random().nextInt(candidateVms.size()));
        }
        return null;
    }
    
    public DomainRouterVO deployELBVm(Network guestNetwork, DeployDestination dest, Account owner, Map<Param, Object> params) throws
                ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
        long dcId = dest.getDataCenter().getId();

        // lock guest network
        Long guestNetworkId = guestNetwork.getId();
        guestNetwork = _networkDao.acquireInLockTable(guestNetworkId);

        if (guestNetwork == null) {
            throw new ConcurrentOperationException("Unable to acquire network lock: " + guestNetworkId);
        }

        try {

            if (_networkModel.isNetworkSystem(guestNetwork) || guestNetwork.getGuestType() == Network.GuestType.Shared) {
                owner = _accountService.getSystemAccount();
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Starting a ELB vm for network configurations: " + guestNetwork + " in " + dest);
            }
            assert guestNetwork.getState() == Network.State.Implemented 
                || guestNetwork.getState() == Network.State.Setup 
                    || guestNetwork.getState() == Network.State.Implementing : "Network is not yet fully implemented: " + guestNetwork;

            DataCenterDeployment plan = null;
            DomainRouterVO elbVm = null;
            
            plan = new DataCenterDeployment(dcId, dest.getPod().getId(), null, null, null, null);

            if (elbVm == null) {
                long id = _routerDao.getNextInSequence(Long.class, "id");
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Creating the ELB vm " + id);
                }
 
                List<? extends NetworkOffering> offerings = _networkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemControlNetwork);
                NetworkOffering controlOffering = offerings.get(0);
                NetworkVO controlConfig = _networkMgr.setupNetwork(_systemAcct, controlOffering, plan, null, null, false).get(0);

                List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>(2);
                NicProfile guestNic = new NicProfile();
                guestNic.setDefaultNic(true);
                networks.add(new Pair<NetworkVO, NicProfile>(controlConfig, null));
                networks.add(new Pair<NetworkVO, NicProfile>((NetworkVO) guestNetwork, guestNic));

                VMTemplateVO template = _templateDao.findSystemVMTemplate(dcId);

                String typeString = "ElasticLoadBalancerVm";
                Long physicalNetworkId = _networkModel.getPhysicalNetworkId(guestNetwork);
                PhysicalNetworkServiceProvider provider = _physicalProviderDao.findByServiceProvider(physicalNetworkId, typeString);
                if (provider == null) {
                    throw new CloudRuntimeException("Cannot find service provider " + typeString + " in physical network " + physicalNetworkId);
                }
                VirtualRouterProvider vrProvider = _vrProviderDao.findByNspIdAndType(provider.getId(), VirtualRouterProviderType.ElasticLoadBalancerVm);
                if (vrProvider == null) {
                    throw new CloudRuntimeException("Cannot find virtual router provider " + typeString + " as service provider " + provider.getId());
                }
               
                elbVm = new DomainRouterVO(id, _elasticLbVmOffering.getId(), vrProvider.getId(), 
                        VirtualMachineName.getSystemVmName(id, _instance, _elbVmNamePrefix), template.getId(), template.getHypervisorType(),
                        template.getGuestOSId(), owner.getDomainId(), owner.getId(), false, 0, false, RedundantState.UNKNOWN,
                        _elasticLbVmOffering.getOfferHA(), false, VirtualMachine.Type.ElasticLoadBalancerVm, null);
                elbVm.setRole(Role.LB);
                elbVm = _itMgr.allocate(elbVm, template, _elasticLbVmOffering, networks, plan, null, owner);
                //TODO: create usage stats
            }

            State state = elbVm.getState();
            if (state != State.Running) {
                elbVm = this.start(elbVm, _accountService.getSystemUser(), _accountService.getSystemAccount(), params);
            }

            return elbVm;
        } finally {
            _networkDao.releaseFromLockTable(guestNetworkId);
        }
    }
    
    private DomainRouterVO start(DomainRouterVO elbVm, User user, Account caller, Map<Param, Object> params) throws StorageUnavailableException, InsufficientCapacityException,
    ConcurrentOperationException, ResourceUnavailableException {
        s_logger.debug("Starting ELB VM " + elbVm);
        if (_itMgr.start(elbVm, params, user, caller) != null) {
            return _routerDao.findById(elbVm.getId());
        } else {
            return null;
        }
    }
    
    private DomainRouterVO stop(DomainRouterVO elbVm, boolean forced, User user, Account caller) throws ConcurrentOperationException, ResourceUnavailableException {
        s_logger.debug("Stopping ELB vm " + elbVm);
        try {
            if (_itMgr.advanceStop( elbVm, forced, user, caller)) {
                return _routerDao.findById(elbVm.getId());
            } else {
                return null;
            }
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Unable to stop " + elbVm, e);
        }
    }
    
    protected List<LoadBalancerVO> findExistingLoadBalancers(String lbName, Long ipId, Long accountId, Long domainId, Integer publicPort) {
        SearchBuilder<LoadBalancerVO> sb = _lbDao.createSearchBuilder();
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.EQ); 
        sb.and("accountId", sb.entity().getAccountId(), SearchCriteria.Op.EQ);
        sb.and("publicPort", sb.entity().getSourcePortStart(), SearchCriteria.Op.EQ);
        if (ipId != null) {
            sb.and("sourceIpAddress", sb.entity().getSourceIpAddressId(), SearchCriteria.Op.EQ);
        }
        if (domainId != null) {
            sb.and("domainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
        }
        if (publicPort != null) {
            sb.and("publicPort", sb.entity().getSourcePortStart(), SearchCriteria.Op.EQ);
        }
        SearchCriteria<LoadBalancerVO> sc = sb.create();
        sc.setParameters("name", lbName);
        sc.setParameters("accountId", accountId);
        if (ipId != null) {
            sc.setParameters("sourceIpAddress", ipId);
        } 
        if (domainId != null) {
            sc.setParameters("domainId",domainId);
        }
        if (publicPort != null) {
            sc.setParameters("publicPort", publicPort);
        }
        List<LoadBalancerVO> lbs = _lbDao.search(sc, null);
   
        return lbs == null || lbs.size()==0 ? null: lbs;
    }
    
    @DB
    public PublicIp allocDirectIp(Account account, long guestNetworkId) throws InsufficientAddressCapacityException {
        Network frontEndNetwork = _networkModel.getNetwork(guestNetworkId);
        Transaction txn = Transaction.currentTxn();
        txn.start();
        
        PublicIp ip = _networkMgr.assignPublicIpAddress(frontEndNetwork.getDataCenterId(), null, account, VlanType.DirectAttached, frontEndNetwork.getId(), null, true);
        IPAddressVO ipvo = _ipAddressDao.findById(ip.getId());
        ipvo.setAssociatedWithNetworkId(frontEndNetwork.getId()); 
        _ipAddressDao.update(ipvo.getId(), ipvo);
        txn.commit();
        s_logger.info("Acquired frontend IP for ELB " + ip);

        return ip;
    }
    
    public void releaseIp(long ipId, long userId, Account caller) {
        s_logger.info("ELB: Release public IP for loadbalancing " + ipId);
        IPAddressVO ipvo = _ipAddressDao.findById(ipId);
        ipvo.setAssociatedWithNetworkId(null); 
        _ipAddressDao.update(ipvo.getId(), ipvo);
       _networkMgr.disassociatePublicIpAddress(ipId, userId, caller);
       _ipAddressDao.unassignIpAddress(ipId);
    }

    @Override
    @DB
    public LoadBalancer handleCreateLoadBalancerRule(CreateLoadBalancerRuleCmd lb, Account account, long networkId) throws InsufficientAddressCapacityException, NetworkRuleConflictException  {
        //this part of code is executed when the LB provider is Elastic Load Balancer vm
        if (!_networkModel.isProviderSupportServiceInNetwork(lb.getNetworkId(), Service.Lb, Provider.ElasticLoadBalancerVm)) {
    		return null;
    	}
    	
    	Long ipId = lb.getSourceIpAddressId();
        if (ipId != null) {
            return null;
        }
        boolean newIp = false;
        account = _accountDao.acquireInLockTable(account.getId());
        if (account == null) {
            s_logger.warn("ELB: CreateLoadBalancer: Failed to acquire lock on account");
            throw new CloudRuntimeException("Failed to acquire lock on account");
        }
        try {
            List<LoadBalancerVO> existingLbs = findExistingLoadBalancers(lb.getName(), lb.getSourceIpAddressId(), lb.getAccountId(), lb.getDomainId(), lb.getSourcePortStart());
            if (existingLbs == null ){
                existingLbs = findExistingLoadBalancers(lb.getName(), lb.getSourceIpAddressId(), lb.getAccountId(), lb.getDomainId(), null);
                if (existingLbs == null) {
                    if (lb.getSourceIpAddressId() != null) {
                        existingLbs = findExistingLoadBalancers(lb.getName(), null, lb.getAccountId(), lb.getDomainId(), null);
                        if (existingLbs != null) {
                            throw new InvalidParameterValueException("Supplied LB name " + lb.getName() + " is not associated with IP " + lb.getSourceIpAddressId() );
                        } 
                    } else {
                        s_logger.debug("Could not find any existing frontend ips for this account for this LB rule, acquiring a new frontent IP for ELB");
                        PublicIp ip = allocDirectIp(account, networkId);
                        ipId = ip.getId();
                        newIp = true;
                    }
                } else {
                    ipId = existingLbs.get(0).getSourceIpAddressId();
                    s_logger.debug("ELB: Found existing frontend ip for this account for this LB rule " + ipId);
                }
            } else {
                s_logger.warn("ELB: Found existing load balancers matching requested new LB");
                throw new NetworkRuleConflictException("ELB: Found existing load balancers matching requested new LB");
            }

            Network network = _networkModel.getNetwork(networkId);
            IPAddressVO ipAddr = _ipAddressDao.findById(ipId);
            
            LoadBalancer result = null;
            try {
                lb.setSourceIpAddressId(ipId);
                result = _lbMgr.createLoadBalancer(lb, false);
            } catch (NetworkRuleConflictException e) {
                s_logger.warn("Failed to create LB rule, not continuing with ELB deployment");
                if (newIp) {
                    releaseIp(ipId, UserContext.current().getCallerUserId(), account);
                }
                throw e;
            }

            DomainRouterVO elbVm = null;

            if (existingLbs == null) {
                elbVm = findELBVmWithCapacity(network, ipAddr);
                if (elbVm == null) {
                    elbVm = deployLoadBalancerVM(networkId, ipAddr, account.getId());
                    if (elbVm == null) {
                        s_logger.warn("Failed to deploy a new ELB vm for ip " + ipAddr + " in network " + network + "lb name=" + lb.getName());
                        if (newIp)
                            releaseIp(ipId, UserContext.current().getCallerUserId(), account);
                    }
                }

            } else {
                ElasticLbVmMapVO elbVmMap = _elbVmMapDao.findOneByIp(ipId);
                if (elbVmMap != null) {
                    elbVm = _routerDao.findById(elbVmMap.getElbVmId());
                }
            }
            
            if (elbVm == null) {
                s_logger.warn("No ELB VM can be found or deployed");
                s_logger.warn("Deleting LB since we failed to deploy ELB VM");
                _lbDao.remove(result.getId());
                return null;
            }
            
            ElasticLbVmMapVO mapping = new ElasticLbVmMapVO(ipId, elbVm.getId(), result.getId());
            _elbVmMapDao.persist(mapping);
            return result;
            
        } finally {
            if (account != null) {
                _accountDao.releaseFromLockTable(account.getId());
            }
        }
        
    }
    
    void garbageCollectUnusedElbVms() {
        List<DomainRouterVO> unusedElbVms = _elbVmMapDao.listUnusedElbVms();
        if (unusedElbVms != null && unusedElbVms.size() > 0)
            s_logger.info("Found " + unusedElbVms.size() + " unused ELB vms");
        Set<Long> currentGcCandidates = new HashSet<Long>();
        for (DomainRouterVO elbVm: unusedElbVms) {
            currentGcCandidates.add(elbVm.getId());
        }
        _gcCandidateElbVmIds.retainAll(currentGcCandidates);
        currentGcCandidates.removeAll(_gcCandidateElbVmIds);
        User user = _accountService.getSystemUser();
        for (Long elbVmId : _gcCandidateElbVmIds) {
            DomainRouterVO elbVm = _routerDao.findById(elbVmId);
            boolean gceed = false;

            try {
                s_logger.info("Attempting to stop ELB VM: " + elbVm);
                stop(elbVm, true, user, _systemAcct);
                gceed = true;
            } catch (ConcurrentOperationException e) {
                s_logger.warn("Unable to stop unused ELB vm " + elbVm + " due to ", e);
            } catch (ResourceUnavailableException e) {
                s_logger.warn("Unable to stop unused ELB vm " + elbVm + " due to ", e);
                continue;
            }
            if (gceed) {
                try {
                    s_logger.info("Attempting to destroy ELB VM: " + elbVm);
                    _itMgr.expunge(elbVm, user, _systemAcct);
                } catch (ResourceUnavailableException e) {
                    s_logger.warn("Unable to destroy unused ELB vm " + elbVm + " due to ", e);
                    gceed = false;
                }
            }
            if (!gceed) {
                currentGcCandidates.add(elbVm.getId());
            }

        }
        _gcCandidateElbVmIds = currentGcCandidates;
    }
    
    public class CleanupThread implements Runnable {
        @Override
        public void run() {
            garbageCollectUnusedElbVms();
            
        }

        CleanupThread() {

        }
    }

    @Override
    public void handleDeleteLoadBalancerRule(LoadBalancer lb, long userId, Account caller) {
        if (!_enabled) {
            return;
        }
        List<LoadBalancerVO> remainingLbs = _loadBalancerDao.listByIpAddress(lb.getSourceIpAddressId());
        if (remainingLbs.size() == 0) {
            s_logger.debug("ELB mgr: releasing ip " + lb.getSourceIpAddressId() + " since  no LB rules remain for this ip address");
            releaseIp(lb.getSourceIpAddressId(), userId, caller);
        }
    }

    @Override
    public DomainRouterVO findByName(String name) {
        if (!VirtualMachineName.isValidSystemVmName(name, _instance, _elbVmNamePrefix)) {
            return null;
        }

        return _routerDao.findById(VirtualMachineName.getSystemVmId(name));
    }

    @Override
    public DomainRouterVO findById(long id) {
        return _routerDao.findById(id);
    }

    @Override
    public DomainRouterVO persist(DomainRouterVO elbVm) {
        return _routerDao.persist(elbVm);
    }

    @Override
    public boolean finalizeVirtualMachineProfile(VirtualMachineProfile<DomainRouterVO> profile, DeployDestination dest, ReservationContext context) {
        DomainRouterVO elbVm = profile.getVirtualMachine();
        
        List<NicProfile> elbNics = profile.getNics();
        Long guestNtwkId = null;
        for (NicProfile routerNic : elbNics) {
            if (routerNic.getTrafficType() == TrafficType.Guest) {
                guestNtwkId = routerNic.getNetworkId();
                break;
            }
        }
        
        NetworkVO guestNetwork = _networkDao.findById(guestNtwkId);

        DataCenter dc = dest.getDataCenter();

        StringBuilder buf = profile.getBootArgsBuilder();
        buf.append(" template=domP type=" + _systemVmType);
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
            }
            if (nic.getTrafficType() == TrafficType.Management) {
                buf.append(" localgw=").append(dest.getPod().getGateway());
            } else if (nic.getTrafficType() == TrafficType.Control) {
                //  control command is sent over management network in VMware
                if (dest.getHost().getHypervisorType() == HypervisorType.VMware) {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Check if we need to add management server explicit route to ELB vm. pod cidr: " + dest.getPod().getCidrAddress() + "/" + dest.getPod().getCidrSize()
                                + ", pod gateway: " + dest.getPod().getGateway() + ", management host: " + _mgmtHost);
                    }

                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Added management server explicit route to ELB vm.");
                    }
                    // always add management explicit route, for basic networking setup
                    buf.append(" mgmtcidr=").append(_mgmtCidr);
                    buf.append(" localgw=").append(dest.getPod().getGateway());

                    if (dc.getNetworkType() == NetworkType.Basic) {
                        // ask elb vm to setup SSH on guest network
                        buf.append(" sshonguest=true");
                    }

                }

                controlNic = nic;
            }
        }
        String domain = guestNetwork.getNetworkDomain();
        if (domain != null) {
            buf.append(" domain=" + domain);
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
        DomainRouterVO elbVm = profile.getVirtualMachine();

        List<NicProfile> nics = profile.getNics();
        for (NicProfile nic : nics) {
            if (nic.getTrafficType() == TrafficType.Public) {
                elbVm.setPublicIpAddress(nic.getIp4Address());
                elbVm.setPublicNetmask(nic.getNetmask());
                elbVm.setPublicMacAddress(nic.getMacAddress());
            } else if (nic.getTrafficType() == TrafficType.Control) {
                elbVm.setPrivateIpAddress(nic.getIp4Address());
                elbVm.setPrivateMacAddress(nic.getMacAddress());
            }
        }
        _routerDao.update(elbVm.getId(), elbVm);

        finalizeCommandsOnStart(cmds, profile);
        return true;
    }

    @Override
    public boolean finalizeStart(VirtualMachineProfile<DomainRouterVO> profile, long hostId, Commands cmds, ReservationContext context) {
        CheckSshAnswer answer = (CheckSshAnswer) cmds.getAnswer("checkSsh");
        if (answer == null || !answer.getResult()) {
            s_logger.warn("Unable to ssh to the ELB VM: " + answer.getDetails());
            return false;
        }

        return true;
    }

    @Override
    public boolean finalizeCommandsOnStart(Commands cmds, VirtualMachineProfile<DomainRouterVO> profile) {
        DomainRouterVO elbVm = profile.getVirtualMachine();
        DataCenterVO dcVo = _dcDao.findById(elbVm.getDataCenterId());

        NicProfile controlNic = null;
        Long guestNetworkId = null;
        
        if(profile.getHypervisorType() == HypervisorType.VMware && dcVo.getNetworkType() == NetworkType.Basic) {
            // TODO this is a ugly to test hypervisor type here
            // for basic network mode, we will use the guest NIC for control NIC
            for (NicProfile nic : profile.getNics()) {
                if (nic.getTrafficType() == TrafficType.Guest && nic.getIp4Address() != null) {
                    controlNic = nic;
                    guestNetworkId = nic.getNetworkId();
                }
            }
        } else {
            for (NicProfile nic : profile.getNics()) {
                if (nic.getTrafficType() == TrafficType.Control && nic.getIp4Address() != null) {
                    controlNic = nic;
                } else if (nic.getTrafficType() == TrafficType.Guest) {
                    guestNetworkId = nic.getNetworkId();
                }
            }
        }

        if (controlNic == null) {
            s_logger.error("Control network doesn't exist for the ELB vm " + elbVm);
            return false;
        }

        cmds.addCommand("checkSsh", new CheckSshCommand(profile.getInstanceName(), controlNic.getIp4Address(), 3922));

        // Re-apply load balancing rules
        List<LoadBalancerVO> lbs = _elbVmMapDao.listLbsForElbVm(elbVm.getId());
        List<LoadBalancingRule> lbRules = new ArrayList<LoadBalancingRule>();
        for (LoadBalancerVO lb : lbs) {
            List<LbDestination> dstList = _lbMgr.getExistingDestinations(lb.getId());
            List<LbStickinessPolicy> policyList = _lbMgr.getStickinessPolicies(lb.getId());
            List<LbHealthCheckPolicy> hcPolicyList = _lbMgr.getHealthCheckPolicies(lb.getId());
            LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList, policyList, hcPolicyList);
            lbRules.add(loadBalancing);
        }

        s_logger.debug("Found " + lbRules.size() + " load balancing rule(s) to apply as a part of ELB vm " + elbVm + " start.");
        if (!lbRules.isEmpty()) {
            createApplyLoadBalancingRulesCommands(lbRules, elbVm, cmds, guestNetworkId);
        }

        return true;
    }

    @Override
    public void finalizeStop(VirtualMachineProfile<DomainRouterVO> profile, StopAnswer answer) {
        if (answer != null) {
            VMInstanceVO vm = profile.getVirtualMachine();
            DomainRouterVO elbVm = _routerDao.findById(vm.getId());
            processStopOrRebootAnswer(elbVm, answer);
        }
    }
    
    public void processStopOrRebootAnswer(final DomainRouterVO elbVm, Answer answer) {
        //TODO: process network usage stats
    }

    @Override
    public void finalizeExpunge(DomainRouterVO vm) {
        // no-op
        
    }

    @Override
    public Long convertToId(String vmName) {
        if (!VirtualMachineName.isValidSystemVmName(vmName, _instance, _elbVmNamePrefix)) {
            return null;
        }

        return VirtualMachineName.getSystemVmId(vmName);
    }
    
    @Override
    public boolean plugNic(Network network, NicTO nic, VirtualMachineTO vm,
            ReservationContext context, DeployDestination dest) throws ConcurrentOperationException, ResourceUnavailableException,
            InsufficientCapacityException {
        //not supported
        throw new UnsupportedOperationException("Plug nic is not supported for vm of type " + vm.getType());
    }

    @Override
    public boolean unplugNic(Network network, NicTO nic, VirtualMachineTO vm,
            ReservationContext context, DeployDestination dest) throws ConcurrentOperationException, ResourceUnavailableException {
        //not supported
        throw new UnsupportedOperationException("Unplug nic is not supported for vm of type " + vm.getType());
     }

    @Override
    public void prepareStop(VirtualMachineProfile<DomainRouterVO> profile) {
    } 

}
