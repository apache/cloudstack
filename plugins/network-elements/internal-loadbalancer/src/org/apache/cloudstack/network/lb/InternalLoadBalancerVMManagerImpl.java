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
package org.apache.cloudstack.network.lb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.lb.ApplicationLoadBalancerRuleVO;
import org.apache.cloudstack.lb.dao.ApplicationLoadBalancerRuleDao;

import com.cloud.agent.AgentManager;
import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.GetDomRVersionAnswer;
import com.cloud.agent.api.GetDomRVersionCmd;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.manager.Commands;
import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DataCenterDeployment;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.exception.StorageUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VirtualRouterProvider.VirtualRouterProviderType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbHealthCheckPolicy;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.RedundantState;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.rules.FirewallRule;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ConfigurationServer;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
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
@Local(value = { InternalLoadBalancerVMManager.class, InternalLoadBalancerVMService.class})
public class InternalLoadBalancerVMManagerImpl extends ManagerBase implements
    InternalLoadBalancerVMManager, VirtualMachineGuru<DomainRouterVO> {
    private static final Logger s_logger = Logger
            .getLogger(InternalLoadBalancerVMManagerImpl.class);
    static final private String _internalLbVmNamePrefix = "b";
    
    private String _instance;
    private String _mgmtHost;
    private String _mgmtCidr;
    private long _internalLbVmOfferingId = 0L;
    
    @Inject VirtualMachineManager _itMgr;
    @Inject DomainRouterDao _internalLbVmDao;
    @Inject ConfigurationDao _configDao;
    @Inject AgentManager _agentMgr;
    @Inject DataCenterDao _dcDao;
    @Inject VirtualRouterProviderDao _vrProviderDao;
    @Inject ApplicationLoadBalancerRuleDao _lbDao;
    @Inject NetworkModel _ntwkModel;
    @Inject LoadBalancingRulesManager _lbMgr;
    @Inject NicDao _nicDao;
    @Inject AccountManager _accountMgr;
    @Inject NetworkDao _networkDao;
    @Inject NetworkManager _ntwkMgr;
    @Inject ServiceOfferingDao _serviceOfferingDao;
    @Inject PhysicalNetworkServiceProviderDao _physicalProviderDao;
    @Inject NetworkOfferingDao _networkOfferingDao;
    @Inject VMTemplateDao _templateDao;
    @Inject ResourceManager _resourceMgr;
    @Inject ConfigurationServer _configServer;

    @Override
    public DomainRouterVO findByName(String name) {
        if (!VirtualMachineName.isValidSystemVmName(name, _instance, _internalLbVmNamePrefix)) {
            return null;
        }

        return _internalLbVmDao.findById(VirtualMachineName.getRouterId(name));
    }

    @Override
    public DomainRouterVO findById(long id) {
        return _internalLbVmDao.findById(id);
    }

    @Override
    public DomainRouterVO persist(DomainRouterVO vm) {
        DomainRouterVO virtualRouter =  _internalLbVmDao.persist(vm);
        return virtualRouter;
    }

    @Override
    public boolean finalizeVirtualMachineProfile(VirtualMachineProfile<DomainRouterVO> profile,
            DeployDestination dest, ReservationContext context) {

        //Internal LB vm starts up with 2 Nics
        //Nic #1 - Guest Nic with IP address that would act as the LB entry point
        //Nic #2 - Control/Management Nic
        
        StringBuilder buf = profile.getBootArgsBuilder();
        buf.append(" template=domP");
        buf.append(" name=").append(profile.getHostName());

        if (Boolean.valueOf(_configDao.getValue("system.vm.random.password"))) {
            buf.append(" vmpassword=").append(_configDao.getValue("system.vm.password"));
        }
        
        NicProfile controlNic = null;
        Network guestNetwork = null;
      
        for (NicProfile nic : profile.getNics()) {
            int deviceId = nic.getDeviceId();
            buf.append(" eth").append(deviceId).append("ip=").append(nic.getIp4Address());
            buf.append(" eth").append(deviceId).append("mask=").append(nic.getNetmask());
            
            if (nic.isDefaultNic()) {
                buf.append(" gateway=").append(nic.getGateway());
                buf.append(" dns1=").append(nic.getGateway());
            }

            if (nic.getTrafficType() == TrafficType.Guest) {
                guestNetwork = _ntwkModel.getNetwork(nic.getNetworkId());
            } else if (nic.getTrafficType() == TrafficType.Management) {
                buf.append(" localgw=").append(dest.getPod().getGateway());
            } else if (nic.getTrafficType() == TrafficType.Control) {
                controlNic = nic;
                // Internal LB control command is sent over management server in VMware
                if (dest.getHost().getHypervisorType() == HypervisorType.VMware) {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Check if we need to add management server explicit route to Internal LB. pod cidr: " 
                                + dest.getPod().getCidrAddress() + "/" + dest.getPod().getCidrSize()
                                + ", pod gateway: " + dest.getPod().getGateway() + ", management host: " + _mgmtHost);
                    }

                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Add management server explicit route to Internal LB.");
                    }
                    
               
                    buf.append(" mgmtcidr=").append(_mgmtCidr);
                    buf.append(" localgw=").append(dest.getPod().getGateway());
                }
            }
        }

        if (controlNic == null) {
            throw new CloudRuntimeException("Didn't start a control port");
        }
                
        if (guestNetwork != null) {
            String domain = guestNetwork.getNetworkDomain();
            if (domain != null) {
                buf.append(" domain=" + domain);
            }
        }

        String type = "ilbvm";
        buf.append(" type=" + type);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Boot Args for " + profile + ": " + buf.toString());
        }

        return true;
    }

    @Override
    public boolean finalizeDeployment(Commands cmds, VirtualMachineProfile<DomainRouterVO> profile, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException {
        DomainRouterVO internalLbVm = profile.getVirtualMachine();

        List<NicProfile> nics = profile.getNics();
        for (NicProfile nic : nics) {
            if (nic.getTrafficType() == TrafficType.Control) {
                internalLbVm.setPrivateIpAddress(nic.getIp4Address());
                internalLbVm.setPrivateMacAddress(nic.getMacAddress());
            }
        }
        _internalLbVmDao.update(internalLbVm.getId(), internalLbVm);

        finalizeCommandsOnStart(cmds, profile);
        return true;
    }

    @Override
    public boolean finalizeStart(VirtualMachineProfile<DomainRouterVO> profile, long hostId, Commands cmds, ReservationContext context) {
        DomainRouterVO internalLbVm = profile.getVirtualMachine();
        
        boolean result = true;

        Answer answer = cmds.getAnswer("checkSsh");
        if (answer != null && answer instanceof CheckSshAnswer) {
            CheckSshAnswer sshAnswer = (CheckSshAnswer) answer;
            if (sshAnswer == null || !sshAnswer.getResult()) {
                s_logger.warn("Unable to ssh to the internal LB VM: " + sshAnswer.getDetails());
                result = false;
            }
        } else {
            result = false;
        }
        if (result == false) {
            return result;
        }
        
        //Get guest network info
        List<Network> guestNetworks = new ArrayList<Network>();
        List<? extends Nic> internalLbVmNics = _nicDao.listByVmId(profile.getId());
        for (Nic internalLbVmNic : internalLbVmNics) {
            Network network = _ntwkModel.getNetwork(internalLbVmNic.getNetworkId());
            if (network.getTrafficType() == TrafficType.Guest) {
                guestNetworks.add(network);
            }
        }
        
        answer = cmds.getAnswer("getDomRVersion");
        if (answer != null && answer instanceof GetDomRVersionAnswer) {
            GetDomRVersionAnswer versionAnswer = (GetDomRVersionAnswer)answer;
            if (answer == null || !answer.getResult()) {
                s_logger.warn("Unable to get the template/scripts version of internal LB VM " + internalLbVm.getInstanceName() +
                        " due to: " + versionAnswer.getDetails());
                result = false;
            } else {
                internalLbVm.setTemplateVersion(versionAnswer.getTemplateVersion());
                internalLbVm.setScriptsVersion(versionAnswer.getScriptsVersion());
                internalLbVm = _internalLbVmDao.persist(internalLbVm, guestNetworks);
            }
        } else {
            result = false;
        }

        return result;
    }

    @Override
    public boolean finalizeCommandsOnStart(Commands cmds, VirtualMachineProfile<DomainRouterVO> profile) {
        DomainRouterVO internalLbVm = profile.getVirtualMachine();
        NicProfile controlNic = getNicProfileByTrafficType(profile, TrafficType.Control);

        if (controlNic == null) {
            s_logger.error("Control network doesn't exist for the internal LB vm " + internalLbVm);
            return false;
        }

        finalizeSshAndVersionOnStart(cmds, profile, internalLbVm, controlNic);

        // restart network if restartNetwork = false is not specified in profile parameters
        boolean reprogramGuestNtwk = true;
        if (profile.getParameter(Param.ReProgramGuestNetworks) != null 
                && (Boolean) profile.getParameter(Param.ReProgramGuestNetworks) == false) {
            reprogramGuestNtwk = false;
        }

        VirtualRouterProvider lbProvider = _vrProviderDao.findById(internalLbVm.getElementId());
        if (lbProvider == null) {
            throw new CloudRuntimeException("Cannot find related element " + VirtualRouterProviderType.InternalLbVm + " of vm: " + internalLbVm.getHostName());
        }
        
        Provider provider = Network.Provider.getProvider(lbProvider.getType().toString());
        if (provider == null) {
            throw new CloudRuntimeException("Cannot find related provider of provider: " + lbProvider.getType().toString());
        }

        if (reprogramGuestNtwk) {
            NicProfile guestNic = getNicProfileByTrafficType(profile, TrafficType.Guest);
            finalizeLbRulesForIp(cmds, internalLbVm, provider, new Ip(guestNic.getIp4Address()), guestNic.getNetworkId());
        }

        return true;
    }

    @Override
    public void finalizeStop(VirtualMachineProfile<DomainRouterVO> profile, StopAnswer answer) {
    }

    @Override
    public void finalizeExpunge(DomainRouterVO vm) {
    }

    @Override
    public Long convertToId(String vmName) {
        if (!VirtualMachineName.isValidSystemVmName(vmName, _instance, _internalLbVmNamePrefix)) {
            return null;
        }

        return VirtualMachineName.getRouterId(vmName);
    }

    @Override
    public void prepareStop(VirtualMachineProfile<DomainRouterVO> profile) {
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        final Map<String, String> configs = _configDao.getConfiguration("AgentManager", params);
        _instance = configs.get("instance.name");
        if (_instance == null) {
            _instance = "DEFAULT";
        }
        
        _mgmtHost = configs.get("host");
        _mgmtCidr = _configDao.getValue(Config.ManagementNetwork.key());
        
        String offUUID = configs.get(Config.InternalLbVmServiceOfferingId.key());
        if (offUUID != null && !offUUID.isEmpty()) {
            //get the id by offering UUID
            ServiceOfferingVO off = _serviceOfferingDao.findByUuid(offUUID);
            if (off != null) {
                _internalLbVmOfferingId = off.getId();
            } else {
                s_logger.warn("Invalid offering UUID is passed in " + Config.InternalLbVmServiceOfferingId.key() + "; the default offering will be used instead");
            }
        }
        
        //if offering wasn't set, try to get the default one
        if (_internalLbVmOfferingId == 0L) {
            boolean useLocalStorage = Boolean.parseBoolean(configs.get(Config.SystemVMUseLocalStorage.key()));
            ServiceOfferingVO newOff = new ServiceOfferingVO("System Offering For Internal LB VM", 1, InternalLoadBalancerVMManager.DEFAULT_INTERNALLB_VM_RAMSIZE, InternalLoadBalancerVMManager.DEFAULT_INTERNALLB_VM_CPU_MHZ, null,
                    null, true, null, useLocalStorage, true, null, true, VirtualMachine.Type.InternalLoadBalancerVm, true);
            newOff.setUniqueName(ServiceOffering.internalLbVmDefaultOffUniqueName);
            newOff = _serviceOfferingDao.persistSystemServiceOffering(newOff);
            _internalLbVmOfferingId = newOff.getId();
        }
        
        _itMgr.registerGuru(VirtualMachine.Type.InternalLoadBalancerVm, this);

        if (s_logger.isInfoEnabled()) {
            s_logger.info(getName()  +  " has been configured");
        }
        
        return true;
    }
    
    @Override
    public String getName() {
        return _name;
    }
    
    protected NicProfile getNicProfileByTrafficType(VirtualMachineProfile<DomainRouterVO> profile, TrafficType trafficType) {
        for (NicProfile nic : profile.getNics()) {
            if (nic.getTrafficType() == trafficType && nic.getIp4Address() != null) {
                return nic;
            }
        }
        return null;
     }
    
    protected void finalizeSshAndVersionOnStart(Commands cmds, VirtualMachineProfile<DomainRouterVO> profile, DomainRouterVO router, NicProfile controlNic) {
        cmds.addCommand("checkSsh", new CheckSshCommand(profile.getInstanceName(), controlNic.getIp4Address(), 3922));

        // Update internal lb vm template/scripts version
        final GetDomRVersionCmd command = new GetDomRVersionCmd();
        command.setAccessDetail(NetworkElementCommand.ROUTER_IP, controlNic.getIp4Address());
        command.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        cmds.addCommand("getDomRVersion", command);
    }
    
    
    protected void finalizeLbRulesForIp(Commands cmds, DomainRouterVO internalLbVm, Provider provider, Ip sourceIp, long guestNtwkId) {
        s_logger.debug("Resending load balancing rules as a part of start for " + internalLbVm);
        List<ApplicationLoadBalancerRuleVO> lbs = _lbDao.listBySrcIpSrcNtwkId(sourceIp, guestNtwkId);
        List<LoadBalancingRule> lbRules = new ArrayList<LoadBalancingRule>();
        if (_ntwkModel.isProviderSupportServiceInNetwork(guestNtwkId, Service.Lb, provider)) {
            // Re-apply load balancing rules
            for (ApplicationLoadBalancerRuleVO lb : lbs) {
                List<LbDestination> dstList = _lbMgr.getExistingDestinations(lb.getId());
                List<LbStickinessPolicy> policyList = _lbMgr.getStickinessPolicies(lb.getId());
                List<LbHealthCheckPolicy> hcPolicyList = _lbMgr.getHealthCheckPolicies(lb.getId());
                LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList, policyList, hcPolicyList, sourceIp);
                lbRules.add(loadBalancing);
            }
        }

        s_logger.debug("Found " + lbRules.size() + " load balancing rule(s) to apply as a part of Intenrnal LB vm" + internalLbVm + " start.");
        if (!lbRules.isEmpty()) {
            createApplyLoadBalancingRulesCommands(lbRules, internalLbVm, cmds, guestNtwkId);
        }
    }
    
    private void createApplyLoadBalancingRulesCommands(List<LoadBalancingRule> rules, VirtualRouter internalLbVm, Commands cmds, long guestNetworkId) {

        LoadBalancerTO[] lbs = new LoadBalancerTO[rules.size()];
        int i = 0;
        boolean inline = false;
        for (LoadBalancingRule rule : rules) {
            boolean revoked = (rule.getState().equals(FirewallRule.State.Revoke));
            String protocol = rule.getProtocol();
            String algorithm = rule.getAlgorithm();
            String uuid = rule.getUuid();

            String srcIp = rule.getSourceIp().addr();
            int srcPort = rule.getSourcePortStart();
            List<LbDestination> destinations = rule.getDestinations();
            List<LbStickinessPolicy> stickinessPolicies = rule.getStickinessPolicies();
            LoadBalancerTO lb = new LoadBalancerTO(uuid, srcIp, srcPort, protocol, algorithm, revoked, false, inline, destinations, stickinessPolicies);
            lbs[i++] = lb;
        }
        
        Network guestNetwork = _ntwkModel.getNetwork(guestNetworkId);
        Nic guestNic = _nicDao.findByNtwkIdAndInstanceId(guestNetwork.getId(), internalLbVm.getId());
        NicProfile guestNicProfile = new NicProfile(guestNic, guestNetwork, guestNic.getBroadcastUri(), guestNic.getIsolationUri(), 
                _ntwkModel.getNetworkRate(guestNetwork.getId(), internalLbVm.getId()), 
                _ntwkModel.isSecurityGroupSupportedInNetwork(guestNetwork), 
                _ntwkModel.getNetworkTag(internalLbVm.getHypervisorType(), guestNetwork));

        NetworkOffering offering =_networkOfferingDao.findById(guestNetworkId);
        String maxconn= null;
        if (offering.getConcurrentConnections() == null) {
            maxconn =  _configDao.getValue(Config.NetworkLBHaproxyMaxConn.key());
        }
        else {
            maxconn = offering.getConcurrentConnections().toString();
        }
        LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(lbs, guestNic.getIp4Address(),
                guestNic.getIp4Address(), internalLbVm.getPrivateIpAddress(), 
                _itMgr.toNicTO(guestNicProfile, internalLbVm.getHypervisorType()), internalLbVm.getVpcId(), maxconn);

        cmd.lbStatsVisibility = _configDao.getValue(Config.NetworkLBHaproxyStatsVisbility.key());
        cmd.lbStatsUri = _configDao.getValue(Config.NetworkLBHaproxyStatsUri.key());
        cmd.lbStatsAuth = _configDao.getValue(Config.NetworkLBHaproxyStatsAuth.key());
        cmd.lbStatsPort = _configDao.getValue(Config.NetworkLBHaproxyStatsPort.key());

        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getInternalLbControlIp(internalLbVm.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, guestNic.getIp4Address());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, internalLbVm.getInstanceName());
        DataCenterVO dcVo = _dcDao.findById(internalLbVm.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        cmds.addCommand(cmd);
    }
    
    
    protected String getInternalLbControlIp(long internalLbVmId) {
        String controlIpAddress = null;
        List<NicVO> nics = _nicDao.listByVmId(internalLbVmId);
        for (NicVO nic : nics) {
            Network ntwk = _ntwkModel.getNetwork(nic.getNetworkId());
            if (ntwk.getTrafficType() == TrafficType.Control) {
                controlIpAddress = nic.getIp4Address();
            }
        }
        
        if(controlIpAddress == null) {
            s_logger.warn("Unable to find Internal LB control ip in its attached NICs!. Internal LB vm: " + internalLbVmId);
            DomainRouterVO internalLbVm = _internalLbVmDao.findById(internalLbVmId);
            return internalLbVm.getPrivateIpAddress();
        }
            
        return controlIpAddress;
    }

    @Override
    public boolean destroyInternalLbVm(long vmId, Account caller, Long callerUserId)
            throws ResourceUnavailableException, ConcurrentOperationException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Attempting to destroy Internal LB vm " + vmId);
        }

        DomainRouterVO internalLbVm = _internalLbVmDao.findById(vmId);
        if (internalLbVm == null) {
            return true;
        }

        _accountMgr.checkAccess(caller, null, true, internalLbVm);

        return _itMgr.expunge(internalLbVm, _accountMgr.getActiveUser(callerUserId), caller); 
    }

    
    @Override
    public VirtualRouter stopInternalLbVm(long vmId, boolean forced, Account caller, long callerUserId) throws ConcurrentOperationException,
                                        ResourceUnavailableException {
        DomainRouterVO internalLbVm = _internalLbVmDao.findById(vmId);
        if (internalLbVm == null || internalLbVm.getRole() != Role.INTERNAL_LB_VM) {
            throw new InvalidParameterValueException("Can't find internal lb vm by id specified");
        }
        
        //check permissions
        _accountMgr.checkAccess(caller, null, true, internalLbVm);
        
        return stopInternalLbVm(internalLbVm, forced, caller, callerUserId);
    }

    protected VirtualRouter stopInternalLbVm(DomainRouterVO internalLbVm, boolean forced, Account caller, long callerUserId) throws ResourceUnavailableException, ConcurrentOperationException {
        s_logger.debug("Stopping internal lb vm " + internalLbVm);
        try {
            if (_itMgr.advanceStop(internalLbVm, forced, _accountMgr.getActiveUser(callerUserId), caller)) {
                return _internalLbVmDao.findById(internalLbVm.getId());
            } else {
                return null;
            }
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Unable to stop " + internalLbVm, e);
        }
    }
    
    
    @Override
    public List<DomainRouterVO> deployInternalLbVm(Network guestNetwork, Ip requestedGuestIp, DeployDestination dest, 
            Account owner, Map<Param, Object> params) throws InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException {

        List<DomainRouterVO> internalLbVms = findOrDeployInternalLbVm(guestNetwork, requestedGuestIp, dest, owner, params);
        
        return startInternalLbVms(params, internalLbVms);
    }
    
    protected List<DomainRouterVO> startInternalLbVms(Map<Param, Object> params, List<DomainRouterVO> internalLbVms) 
            throws StorageUnavailableException, InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        List<DomainRouterVO> runningInternalLbVms = null;

        if (internalLbVms != null) {
            runningInternalLbVms = new ArrayList<DomainRouterVO>();
        } else {
            s_logger.debug("Have no internal lb vms to start");
            return null;
        }

        for (DomainRouterVO internalLbVm : internalLbVms) {
            if (internalLbVm.getState() != VirtualMachine.State.Running) {
                internalLbVm = startInternalLbVm(internalLbVm, _accountMgr.getSystemAccount(), User.UID_SYSTEM, params);
            }
            
            if (internalLbVm != null) {
                runningInternalLbVms.add(internalLbVm);
            }
        }
        return runningInternalLbVms;
    }
    
    
    
    @DB
    protected List<DomainRouterVO> findOrDeployInternalLbVm(Network guestNetwork, Ip requestedGuestIp, DeployDestination dest, 
            Account owner, Map<Param, Object> params) throws ConcurrentOperationException, 
            InsufficientCapacityException, ResourceUnavailableException {

        List<DomainRouterVO> internalLbVms = new ArrayList<DomainRouterVO>();
        Network lock = _networkDao.acquireInLockTable(guestNetwork.getId(), _ntwkMgr.getNetworkLockTimeout());
        if (lock == null) {
            throw new ConcurrentOperationException("Unable to lock network " + guestNetwork.getId());
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Lock is acquired for network id " + lock.getId() + " as a part of internal lb startup in " + dest);
        }
        
        long internalLbProviderId = getInternalLbProviderId(guestNetwork);
        
        try {
            assert guestNetwork.getState() == Network.State.Implemented || guestNetwork.getState() == Network.State.Setup ||
                    guestNetwork.getState() == Network.State.Implementing : "Network is not yet fully implemented: "
                    + guestNetwork;
            assert guestNetwork.getTrafficType() == TrafficType.Guest;

            //deploy internal lb vm
            Pair<DeploymentPlan, List<DomainRouterVO>> planAndInternalLbVms = getDeploymentPlanAndInternalLbVms(dest, guestNetwork.getId(), requestedGuestIp);
            internalLbVms = planAndInternalLbVms.second();
            DeploymentPlan plan = planAndInternalLbVms.first();
            
            if (internalLbVms.size() > 0) {
                s_logger.debug("Found " + internalLbVms.size() + " internal lb vms for the requested IP " + requestedGuestIp.addr());
                return internalLbVms;
            }

            List<Pair<NetworkVO, NicProfile>> networks = createInternalLbVmNetworks(guestNetwork, plan, requestedGuestIp);
            //Pass startVm=false as we are holding the network lock that needs to be released at the end of vm allocation
            DomainRouterVO internalLbVm = deployInternalLbVm(owner, dest, plan, params, internalLbProviderId, _internalLbVmOfferingId, guestNetwork.getVpcId(),
                networks, false);
            if (internalLbVm != null) {
                _internalLbVmDao.addRouterToGuestNetwork(internalLbVm, guestNetwork);
                internalLbVms.add(internalLbVm);
            }
        } finally {
            if (lock != null) {
                _networkDao.releaseFromLockTable(lock.getId());
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Lock is released for network id " + lock.getId() + " as a part of internal lb vm startup in " + dest);
                }
            }
        }
        return internalLbVms;
    }

    protected long getInternalLbProviderId(Network guestNetwork) {
        VirtualRouterProviderType type = VirtualRouterProviderType.InternalLbVm;
        long physicalNetworkId = _ntwkModel.getPhysicalNetworkId(guestNetwork);
        
        PhysicalNetworkServiceProvider provider = _physicalProviderDao.findByServiceProvider(physicalNetworkId, type.toString());
        if (provider == null) {
            throw new CloudRuntimeException("Cannot find service provider " + type.toString() + " in physical network " + physicalNetworkId);
        }
        
        VirtualRouterProvider internalLbProvider = _vrProviderDao.findByNspIdAndType(provider.getId(), type);
        if (internalLbProvider == null) {
            throw new CloudRuntimeException("Cannot find provider " + type.toString() + " as service provider " + provider.getId());
        }
        
        return internalLbProvider.getId();
    }
    
    protected List<Pair<NetworkVO, NicProfile>> createInternalLbVmNetworks(Network guestNetwork, DeploymentPlan plan, Ip guestIp) throws ConcurrentOperationException,
            InsufficientAddressCapacityException {

        //Form networks
        List<Pair<NetworkVO, NicProfile>> networks = new ArrayList<Pair<NetworkVO, NicProfile>>(3);
        
        //1) Guest network - default
        if (guestNetwork != null) {
            s_logger.debug("Adding nic for Internal LB in Guest network " + guestNetwork);
            NicProfile guestNic = new NicProfile();
            if (guestIp != null) {
                guestNic.setIp4Address(guestIp.addr());  
            } else {
                guestNic.setIp4Address(_ntwkMgr.acquireGuestIpAddress(guestNetwork, null));
            }
            guestNic.setGateway(guestNetwork.getGateway());
            guestNic.setBroadcastUri(guestNetwork.getBroadcastUri());
            guestNic.setBroadcastType(guestNetwork.getBroadcastDomainType());
            guestNic.setIsolationUri(guestNetwork.getBroadcastUri());
            guestNic.setMode(guestNetwork.getMode());
            String gatewayCidr = guestNetwork.getCidr();
            guestNic.setNetmask(NetUtils.getCidrNetmask(gatewayCidr));
            guestNic.setDefaultNic(true);
            networks.add(new Pair<NetworkVO, NicProfile>((NetworkVO) guestNetwork, guestNic));
        }

        //2) Control network
        s_logger.debug("Adding nic for Internal LB vm in Control network ");
        List<? extends NetworkOffering> offerings = _ntwkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemControlNetwork);
        NetworkOffering controlOffering = offerings.get(0);
        NetworkVO controlConfig = _ntwkMgr.setupNetwork(_accountMgr.getSystemAccount(), controlOffering, plan, null, null, false).get(0);
        networks.add(new Pair<NetworkVO, NicProfile>(controlConfig, null));

        return networks;
    }
    
    
    protected Pair<DeploymentPlan, List<DomainRouterVO>> getDeploymentPlanAndInternalLbVms(DeployDestination dest, long guestNetworkId, Ip requestedGuestIp) {
        long dcId = dest.getDataCenter().getId();
        DeploymentPlan plan = new DataCenterDeployment(dcId);
        List<DomainRouterVO> internalLbVms = findInternalLbVms(guestNetworkId, requestedGuestIp);

        return new Pair<DeploymentPlan, List<DomainRouterVO>>(plan, internalLbVms);
    
    }

    @Override
    public List<DomainRouterVO> findInternalLbVms(long guestNetworkId, Ip requestedGuestIp) {
        List<DomainRouterVO> internalLbVms = _internalLbVmDao.listByNetworkAndRole(guestNetworkId, Role.INTERNAL_LB_VM);
        if (requestedGuestIp != null && !internalLbVms.isEmpty()) {
            Iterator<DomainRouterVO> it = internalLbVms.iterator();
            while (it.hasNext()) {
                DomainRouterVO vm = it.next();
                Nic nic = _nicDao.findByNtwkIdAndInstanceId(guestNetworkId, vm.getId());
                if (!nic.getIp4Address().equalsIgnoreCase(requestedGuestIp.addr())) {
                    it.remove();
                }
            }
        }
        return internalLbVms;
    }
    
    
    protected DomainRouterVO deployInternalLbVm(Account owner, DeployDestination dest, DeploymentPlan plan, Map<Param, Object> params,
            long internalLbProviderId, long svcOffId, Long vpcId,
            List<Pair<NetworkVO, NicProfile>> networks, boolean startVm) throws ConcurrentOperationException,
            InsufficientAddressCapacityException, InsufficientServerCapacityException, InsufficientCapacityException,
            StorageUnavailableException, ResourceUnavailableException {
        
       

        ServiceOfferingVO routerOffering = _serviceOfferingDao.findById(svcOffId);

        // Internal lb is the network element, we don't know the hypervisor type yet.
        // Try to allocate the internal lb twice using diff hypervisors, and when failed both times, throw the exception up
        List<HypervisorType> hypervisors = getHypervisors(dest, plan, null);

        int allocateRetry = 0;
        int startRetry = 0;
        DomainRouterVO internalLbVm = null;
        for (Iterator<HypervisorType> iter = hypervisors.iterator(); iter.hasNext();) {
            HypervisorType hType = iter.next();
            try {
                long id = _internalLbVmDao.getNextInSequence(Long.class, "id");
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Creating the internal lb vm " + id + " in datacenter "  + dest.getDataCenter() + " with hypervisor type " + hType);
                }                String templateName = null;
                switch (hType) {
                    case XenServer:
                        templateName = _configServer.getConfigValue(Config.RouterTemplateXen.key(), Config.ConfigurationParameterScope.zone.toString(), dest.getDataCenter().getId());
                        break;
                    case KVM:
                        templateName = _configServer.getConfigValue(Config.RouterTemplateKVM.key(), Config.ConfigurationParameterScope.zone.toString(), dest.getDataCenter().getId());
                        break;
                    case VMware:
                        templateName = _configServer.getConfigValue(Config.RouterTemplateVmware.key(), Config.ConfigurationParameterScope.zone.toString(), dest.getDataCenter().getId());
                        break;
                    case Hyperv:
                        templateName = _configServer.getConfigValue(Config.RouterTemplateHyperv.key(), Config.ConfigurationParameterScope.zone.toString(), dest.getDataCenter().getId());
                        break;
                    case LXC:
                        templateName = _configServer.getConfigValue(Config.RouterTemplateLXC.key(), Config.ConfigurationParameterScope.zone.toString(), dest.getDataCenter().getId());
                        break;
                    default: break;
                }
                VMTemplateVO template = _templateDao.findRoutingTemplate(hType, templateName);

                if (template == null) {
                    s_logger.debug(hType + " won't support system vm, skip it");
                    continue;
                }

                internalLbVm = new DomainRouterVO(id, routerOffering.getId(), internalLbProviderId, 
                VirtualMachineName.getSystemVmName(id, _instance, _internalLbVmNamePrefix), template.getId(), template.getHypervisorType(),
                template.getGuestOSId(), owner.getDomainId(), owner.getId(), false, 0, false, 
                RedundantState.UNKNOWN, false, false, VirtualMachine.Type.InternalLoadBalancerVm, vpcId);
                internalLbVm.setRole(Role.INTERNAL_LB_VM);
                internalLbVm = _itMgr.allocate(internalLbVm, template, routerOffering, networks, plan, null, owner);
            } catch (InsufficientCapacityException ex) {
                if (allocateRetry < 2 && iter.hasNext()) {
                    s_logger.debug("Failed to allocate the Internal lb vm with hypervisor type " + hType + ", retrying one more time");
                    continue;
                } else {
                    throw ex;
                }
            } finally {
                allocateRetry++;
            }

            if (startVm) {
                try {
                    internalLbVm = startInternalLbVm(internalLbVm, _accountMgr.getSystemAccount(), User.UID_SYSTEM, params);
                    break;
                } catch (InsufficientCapacityException ex) {
                    if (startRetry < 2 && iter.hasNext()) {
                        s_logger.debug("Failed to start the Internal lb vm  " + internalLbVm + " with hypervisor type " + hType + ", " +
                                "destroying it and recreating one more time");
                        // destroy the internal lb vm
                        destroyInternalLbVm(internalLbVm.getId(), _accountMgr.getSystemAccount(), User.UID_SYSTEM);
                        continue;
                    } else {
                        throw ex;
                    }
                } finally {
                    startRetry++;
                }
            } else {
                //return stopped internal lb vm
                return internalLbVm;
            }
        }
        return internalLbVm;
    }
    
    

    protected DomainRouterVO startInternalLbVm(DomainRouterVO internalLbVm, Account caller, long callerUserId, Map<Param, Object> params) 
            throws StorageUnavailableException, InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException {
        s_logger.debug("Starting Internal LB VM " + internalLbVm);
        if (_itMgr.start(internalLbVm, params, _accountMgr.getUserIncludingRemoved(callerUserId), caller, null) != null) {
            if (internalLbVm.isStopPending()) {
                s_logger.info("Clear the stop pending flag of Internal LB VM " + internalLbVm.getHostName() + " after start router successfully!");
                internalLbVm.setStopPending(false);
                internalLbVm = _internalLbVmDao.persist(internalLbVm);
            }
            return _internalLbVmDao.findById(internalLbVm.getId());
        } else {
            return null;
        }
    }
    
    
    protected List<HypervisorType> getHypervisors(DeployDestination dest, DeploymentPlan plan, 
            List<HypervisorType> supportedHypervisors) throws InsufficientServerCapacityException {
        List<HypervisorType> hypervisors = new ArrayList<HypervisorType>();

        HypervisorType defaults = _resourceMgr.getDefaultHypervisor(dest.getDataCenter().getId());
        if (defaults != HypervisorType.None) {
            hypervisors.add(defaults);
        } else {
            //if there is no default hypervisor, get it from the cluster
            hypervisors = _resourceMgr.getSupportedHypervisorTypes(dest.getDataCenter().getId(), true,
                plan.getPodId());
        }

        //keep only elements defined in supported hypervisors
        StringBuilder hTypesStr = new StringBuilder();
        if (supportedHypervisors != null && !supportedHypervisors.isEmpty()) {
            hypervisors.retainAll(supportedHypervisors);
            for (HypervisorType hType : supportedHypervisors) {
                hTypesStr.append(hType).append(" ");
            }
        }

        if (hypervisors.isEmpty()) {
            throw new InsufficientServerCapacityException("Unable to create internal lb vm, " +
                    "there are no clusters in the zone ", DataCenter.class, dest.getDataCenter().getId());
        }
        return hypervisors;
    }
    
    @Override
    public boolean applyLoadBalancingRules(Network network, final List<LoadBalancingRule> rules, List<? extends VirtualRouter> internalLbVms) 
            throws ResourceUnavailableException {
        if (rules == null || rules.isEmpty()) {
            s_logger.debug("No lb rules to be applied for network " + network);
            return true;
        }
        s_logger.info("lb rules to be applied for network ");
        //only one internal lb vm is supported per ip address at this time
        if (internalLbVms == null || internalLbVms.isEmpty()) {
            throw new CloudRuntimeException("Can't apply the lb rules on network " + network + " as the list of internal lb vms is empty");
        }
        
        VirtualRouter lbVm = internalLbVms.get(0);
        if (lbVm.getState() == State.Running) {
            return sendLBRules(lbVm, rules, network.getId());
        } else if (lbVm.getState() == State.Stopped || lbVm.getState() == State.Stopping) {
            s_logger.debug("Internal LB VM " + lbVm.getInstanceName() + " is in " + lbVm.getState() + 
                    ", so not sending apply lb rules commands to the backend");
            return true;
        } else {
            s_logger.warn("Unable to apply lb rules, Internal LB VM is not in the right state " + lbVm.getState());
            throw new ResourceUnavailableException("Unable to apply lb rules; Internal LB VM is not in the right state", DataCenter.class, lbVm.getDataCenterId());
        }
    }
    
    protected boolean sendLBRules(VirtualRouter internalLbVm, List<LoadBalancingRule> rules, long guestNetworkId) throws ResourceUnavailableException {
        Commands cmds = new Commands(OnError.Continue);
        createApplyLoadBalancingRulesCommands(rules, internalLbVm, cmds, guestNetworkId);
        return sendCommandsToInternalLbVm(internalLbVm, cmds);
    }
    
    
    protected boolean sendCommandsToInternalLbVm(final VirtualRouter internalLbVm, Commands cmds) throws AgentUnavailableException {
        Answer[] answers = null;
        try {
            answers = _agentMgr.send(internalLbVm.getHostId(), cmds);
        } catch (OperationTimedoutException e) {
            s_logger.warn("Timed Out", e);
            throw new AgentUnavailableException("Unable to send commands to virtual router ", internalLbVm.getHostId(), e);
        }

        if (answers == null) {
            return false;
        }

        if (answers.length != cmds.size()) {
            return false;
        }

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
    
    
    @Override
    public VirtualRouter startInternalLbVm(long internalLbVmId, Account caller, long callerUserId) 
            throws StorageUnavailableException, InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException {
        
        DomainRouterVO internalLbVm = _internalLbVmDao.findById(internalLbVmId);
        if (internalLbVm == null || internalLbVm.getRole() != Role.INTERNAL_LB_VM) {
            throw new InvalidParameterValueException("Can't find internal lb vm by id specified");
        }
        
        //check permissions
        _accountMgr.checkAccess(caller, null, true, internalLbVm);
        
        return startInternalLbVm(internalLbVm, caller, callerUserId, null);
    }
}
