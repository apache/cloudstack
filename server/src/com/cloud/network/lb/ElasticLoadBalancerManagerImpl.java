/**
 *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
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
package com.cloud.network.lb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.manager.Commands;
import com.cloud.api.commands.CreateLoadBalancerRuleCmd;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.Pod;
import com.cloud.dc.PodVlanMapVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.dc.dao.PodVlanMapDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.network.ElasticLbVmMapVO;
import com.cloud.network.IPAddressVO;
import com.cloud.network.LoadBalancerVO;
import com.cloud.network.Network;
import com.cloud.network.Network.GuestIpType;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkVO;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.dao.ElasticLbVmMapDao;
import com.cloud.network.router.VirtualNetworkApplianceManager;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.FirewallRule.Purpose;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineName;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfile.Param;
import com.cloud.vm.dao.DomainRouterDao;

@Local(value = { ElasticLoadBalancerManager.class })
public class ElasticLoadBalancerManagerImpl implements
        ElasticLoadBalancerManager, Manager {
    private static final Logger s_logger = Logger
            .getLogger(ElasticLoadBalancerManagerImpl.class);
    
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    NetworkManager _networkMgr;
    @Inject
    LoadBalancerDao _loadBalancerDao = null;
    @Inject
    LoadBalancingRulesManager _lbMgr;
    @Inject
    VirtualNetworkApplianceManager _routerMgr;
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


    String _name;
    String _instance;

    Account _systemAcct;
    ServiceOfferingVO _elasticLbVmOffering;
    
    int _elasticLbVmRamSize;
    int _elasticLbvmCpuMHz;
    
    private Long getPodIdForDirectIp(IPAddressVO ipAddr) {
        List<PodVlanMapVO> podVlanMaps = _podVlanMapDao.listPodVlanMapsByVlan(ipAddr.getVlanId());
        if (podVlanMaps.isEmpty()) {
            return null;
        } else {
            return podVlanMaps.get(0).getPodId();
        }
    }


    public DomainRouterVO deployLoadBalancerVM(Long networkId, IPAddressVO ipAddr, Long accountId) {  
        NetworkVO network = _networkDao.findById(networkId);
        DataCenter dc = _dcDao.findById(network.getDataCenterId());
        Long podId = getPodIdForDirectIp(ipAddr);
        Pod pod = podId == null?null:_podDao.findById(podId);
        Map<VirtualMachineProfile.Param, Object> params = new HashMap<VirtualMachineProfile.Param, Object>(
                1);
        params.put(VirtualMachineProfile.Param.RestartNetwork, true);
        Account owner = _accountService.getActiveAccount("system", new Long(1));
        DeployDestination dest = new DeployDestination(dc, pod, null, null);
        s_logger.debug("About to deploy elastic LB vm ");

        try {
            DomainRouterVO elbVm = deployELBVm(network, dest, owner, params);

            s_logger.debug("ELB  vm = " + elbVm);
            if (elbVm == null) {
                throw new InvalidParameterValueException("Could not deploy or find existing ELB VM");
            }
            return elbVm;
           
        } catch (Throwable t) {
            String errorMsg = "Error while deploying Loadbalancer VM:  " + t;
            s_logger.warn(errorMsg);
            return null;
        }

    }
    
    private boolean sendCommandsToRouter(final DomainRouterVO router,
            Commands cmds) throws AgentUnavailableException {
        Answer[] answers = null;
        try {
            answers = _agentMgr.send(router.getHostId(), cmds);
        } catch (OperationTimedoutException e) {
            s_logger.warn("Timed Out", e);
            throw new AgentUnavailableException(
                    "Unable to send commands to virtual router ",
                    router.getHostId(), e);
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
            List<LoadBalancingRule> rules, DomainRouterVO router, Commands cmds) {

        String elbIp = "";

        LoadBalancerTO[] lbs = new LoadBalancerTO[rules.size()];
        int i = 0;
        for (LoadBalancingRule rule : rules) {
            boolean revoked = (rule.getState()
                    .equals(FirewallRule.State.Revoke));
            String protocol = rule.getProtocol();
            String algorithm = rule.getAlgorithm();

            elbIp = _networkMgr.getIp(rule.getSourceIpAddressId()).getAddress()
                    .addr();
            int srcPort = rule.getSourcePortStart();
            List<LbDestination> destinations = rule.getDestinations();
            LoadBalancerTO lb = new LoadBalancerTO(elbIp, srcPort, protocol,
                    algorithm, revoked, false, destinations);
            lbs[i++] = lb;
        }

        LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(lbs);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP,
                router.getPrivateIpAddress());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME,
                router.getInstanceName());
        cmds.addCommand(cmd);

    }

    protected boolean applyLBRules(DomainRouterVO router,
            List<LoadBalancingRule> rules) throws ResourceUnavailableException {
        Commands cmds = new Commands(OnError.Continue);
        createApplyLoadBalancingRulesCommands(rules, router, cmds);
        // Send commands to router
        return sendCommandsToRouter(router, cmds);
    }
    
    protected DomainRouterVO findElbVmForLb(FirewallRule lb) {//TODO: use a table to lookup
        ElasticLbVmMapVO map = _elbVmMapDao.findOneByIp(lb.getSourceIpAddressId());
        if (map == null) {
            return null;
        }
        DomainRouterVO elbVm = _routerDao.findById(map.getElbVmId());
        return elbVm;
    }

    public boolean applyLoadBalancerRules(Network network,
            List<? extends FirewallRule> rules)
            throws ResourceUnavailableException {
        if (rules == null || rules.isEmpty()) {
            return true;
        }
        if (rules.get(0).getPurpose() != Purpose.LoadBalancing) {
            s_logger.warn("Not handling non-LB firewall rules");
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
                LoadBalancingRule loadBalancing = new LoadBalancingRule(
                        lb, dstList);
                lbRules.add(loadBalancing); 
            }
            return applyLBRules(elbVm, lbRules);
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
        _name = name;
        final Map<String, String> configs = _configDao.getConfiguration("AgentManager", params);
        _systemAcct = _accountService.getSystemAccount();
        _instance = configs.get("instance.name");
        if (_instance == null) {
            _instance = "VM";
        }
        boolean useLocalStorage = Boolean.parseBoolean(configs.get(Config.SystemVMUseLocalStorage.key()));

        _elasticLbVmRamSize = NumbersUtil.parseInt(configs.get("elastic.lb.vm.ram.size"), DEFAULT_ELB_VM_RAMSIZE);
        _elasticLbvmCpuMHz = NumbersUtil.parseInt(configs.get("elastic.lb.vm.cpu.mhz"), DEFAULT_ELB_VM_CPU_MHZ);
        _elasticLbVmOffering = new ServiceOfferingVO("System Offering For Elastic LB VM", 1, _elasticLbVmRamSize, _elasticLbvmCpuMHz, 0, 0, true, null, useLocalStorage, true, null, true, VirtualMachine.Type.ElasticLoadBalancerVm, true);
        _elasticLbVmOffering.setUniqueName("Cloud.Com-ElasticLBVm");
        _elasticLbVmOffering = _serviceOfferingDao.persistSystemServiceOffering(_elasticLbVmOffering);
        

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

    private DomainRouterVO findELBVmWithCapacity(Network guestNetwork, IPAddressVO ipAddr) {
        List<DomainRouterVO> elbVms = _routerDao.listByNetworkAndRole(guestNetwork.getId(), Role.LB);
        return null;
    }
    
    public DomainRouterVO deployELBVm(Network guestNetwork, DeployDestination dest, Account owner, Map<Param, Object> params) throws
                ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
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
                owner = _accountService.getSystemAccount();
            }

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Starting a elastic ip vm for network configurations: " + guestNetwork + " in " + dest);
            }
            assert guestNetwork.getState() == Network.State.Implemented 
                || guestNetwork.getState() == Network.State.Setup 
                || guestNetwork.getState() == Network.State.Implementing 
                : "Network is not yet fully implemented: "+ guestNetwork;

            DataCenterDeployment plan = null;
            DomainRouterVO elbVm = null;
            
            plan = new DataCenterDeployment(dcId, dest.getPod().getId(), null, null, null);

            if (elbVm == null) {
                long id = _routerDao.getNextInSequence(Long.class, "id");
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Creating the elastic LB vm " + id);
                }
 
                List<NetworkOfferingVO> offerings = _networkMgr.getSystemAccountNetworkOfferings(NetworkOfferingVO.SystemControlNetwork);
                NetworkOfferingVO controlOffering = offerings.get(0);
                NetworkVO controlConfig = _networkMgr.setupNetwork(_systemAcct, controlOffering, plan, null, null, false, false).get(0);

                List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>(2);
                NicProfile guestNic = new NicProfile();
                networks.add(new Pair<NetworkVO, NicProfile>((NetworkVO) guestNetwork, guestNic));
                networks.add(new Pair<NetworkVO, NicProfile>(controlConfig, null));
                
                VMTemplateVO template = _templateDao.findSystemVMTemplate(dcId);

               
                elbVm = new DomainRouterVO(id, _elasticLbVmOffering.getId(), VirtualMachineName.getRouterName(id, _instance), template.getId(), template.getHypervisorType(), template.getGuestOSId(),
                        owner.getDomainId(), owner.getId(), guestNetwork.getId(), _elasticLbVmOffering.getOfferHA());
                elbVm.setRole(Role.LB);
                elbVm = _itMgr.allocate(elbVm, template, _elasticLbVmOffering, networks, plan, null, owner);
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
    @DB
    public void handleCreateLoadBalancerRule( CreateLoadBalancerRuleCmd lb, Account account)  {
        
        long ipId = lb.getSourceIpAddressId();
        IPAddressVO ipAddr = _ipAddressDao.findById(ipId);
        Long networkId= ipAddr.getSourceNetworkId();
        NetworkVO network=_networkDao.findById(networkId);
        
        if (network.getGuestType() != GuestIpType.Direct) {
            s_logger.info("Elastic LB Manager: not handling guest traffic of type " + network.getGuestType());
            return;
        }
        DomainRouterVO elbVm = null;

        LoadBalancerVO lbvo;
        lbvo = _lbDao.findByAccountAndName(account.getId(), lb.getName());
        if (lbvo == null) {
            elbVm = findELBVmWithCapacity(network, ipAddr);
            if (elbVm == null) {
                elbVm = deployLoadBalancerVM(networkId, ipAddr, account.getId());
                if (elbVm == null) {
                    s_logger.warn("Failed to deploy a new ELB vm for ip " + ipAddr + " in network " + network + "lb name=" + lb.getName());
                    return; //TODO: throw exception
                }
            }
            
        } else {
            ElasticLbVmMapVO elbVmMap = _elbVmMapDao.findOneByIp(lb.getSourceIpAddressId());
            if (elbVmMap != null) {
                elbVm = _routerDao.findById(elbVmMap.getElbVmId());
            }
        }
        if (elbVm == null) {
            s_logger.warn("No ELB VM can be found or deployed");
            return;
        }
        Transaction txn = Transaction.currentTxn();
        txn.start();
        IPAddressVO ipvo = _ipAddressDao.findById(ipId);
        ipvo.setAssociatedWithNetworkId(networkId); 
        _ipAddressDao.update(ipvo.getId(), ipvo);
        ElasticLbVmMapVO mapping = new ElasticLbVmMapVO(ipId, elbVm.getId());
        _elbVmMapDao.persist(mapping);
        txn.commit();
        
    }
    
    private void createAssociateIPCommand(final DomainRouterVO router, final Network network, final PublicIp ip, Commands cmds) {


        IpAddressTO[] ipsToSend = new IpAddressTO[1];

        IpAddressTO ipTO = new IpAddressTO(ip.getAddress().addr(), true, false, false, ip.getVlanTag(), ip.getVlanGateway(), ip.getVlanNetmask(), ip.getMacAddress(), null, 0);
        ipTO.setTrafficType(network.getTrafficType());
        ipTO.setNetworkTags(network.getTags());
        ipsToSend[0] = ipTO;

        IpAssocCommand cmd = new IpAssocCommand(ipsToSend);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, router.getPrivateIpAddress());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, router.getGuestIpAddress());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        DataCenterVO dcVo = _dcDao.findById(router.getDataCenterIdToDeployIn());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

        cmds.addCommand("IPAssocCommand", cmd);
    
    }

}
