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
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.apache.cloudstack.api.commands.ListVpcInlineLoadBalancerVmsCmd;
import org.apache.cloudstack.api.commands.StartVpcInlineLoadBalancerVmCmd;
import org.apache.cloudstack.api.commands.StopVpcInlineLoadBalancerVmCmd;
import org.apache.cloudstack.api.response.DomainRouterResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NicResponse;
import org.apache.cloudstack.api.response.NicSecondaryIpResponse;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.network.VpcInlineLbMappingVO;
import org.apache.cloudstack.network.dao.VpcInlineLbMappingDao;
import org.apache.cloudstack.resourcedetail.dao.VpcDetailsDao;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetDomRVersionAnswer;
import com.cloud.agent.api.GetDomRVersionCmd;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.manager.Commands;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManagerImpl;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.VlanVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
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
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbHealthCheckPolicy;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.lb.LoadBalancingRulesManager;
import com.cloud.network.router.VirtualNetworkApplianceManager;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.RedundantState;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.network.rules.StaticNatImpl;
import com.cloud.network.vpc.NetworkACLItemDao;
import com.cloud.network.vpc.NetworkACLManager;
import com.cloud.network.vpc.dao.VpcDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ResourceMetaDataService;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicSecondaryIp;
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
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.NicSecondaryIpVO;

@Local(value = { VpcInlineLoadBalancerVmManager.class})
public class VpcInlineLoadBalancerVmManagerImpl extends ManagerBase implements VpcInlineLoadBalancerVmManager, VirtualMachineGuru, Configurable {
    private static final Logger s_logger = Logger.getLogger(VpcInlineLoadBalancerVmManagerImpl.class);
    static final private String s_vpcInlineLbVmNamePrefix = "b";
    public static final ArrayList<String> ANY_CIDR = Lists.newArrayList("0.0.0.0/0");

    private String _instance;
    private String _mgmtHost;
    private String _mgmtCidr;
    private long _vpcInlineLbVmOfferingId = 0L;

    @Inject
    IpAddressManager _ipAddrMgr;
    @Inject
    VirtualMachineManager _itMgr;
    @Inject
    DomainRouterDao _vpcInlineLbVmDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    VirtualRouterProviderDao _vrProviderDao;
    @Inject
    NetworkModel _ntwkModel;
    @Inject
    LoadBalancingRulesManager _lbMgr;
    @Inject
    NicDao _nicDao;
    @Inject
    NicSecondaryIpDao _nicSecondaryIpDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    NetworkDao _networkDao;
    @Inject
    NetworkOrchestrationService _ntwkMgr;
    @Inject
    NetworkACLManager _ntwkAclMgr;
    @Inject
    NetworkACLItemDao _networkACLItemDao;
    @Inject
    ServiceOfferingDao _serviceOfferingDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalProviderDao;
    @Inject
    VpcInlineLbMappingDao _vpcInlineLbMappingDao;
    @Inject
    IPAddressDao _ipAddressDao;
    @Inject
    VlanDao _vlanDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    VMTemplateDao _templateDao;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    LoadBalancerDao _loadBalancerDao;
    @Inject
    VpcDao _vpcDao;
    @Inject
    VpcDetailsDao _vpcDetailsDao;
    @Inject
    ResourceMetaDataService _resourceMetaDataService;

    @Override
    public boolean finalizeVirtualMachineProfile(VirtualMachineProfile profile, DeployDestination dest, ReservationContext context) {

        //Internal LB vm starts up with 2 Nics
        //Nic #1 - Guest Nic with IP address that would act as the LB entry point
        //Nic #2 - Control/Management Nic

        final StringBuilder buf = profile.getBootArgsBuilder();
        buf.append(" template=domP");
        buf.append(" name=").append(profile.getHostName());

        if (Boolean.valueOf(_configDao.getValue("system.vm.random.password"))) {
            buf.append(" vmpassword=").append(_configDao.getValue("system.vm.password"));
        }

        NicProfile controlNic = null;
        Network guestNetwork = null;

        for (final NicProfile nic : profile.getNics()) {
            final int deviceId = nic.getDeviceId();
            buf.append(" eth").append(deviceId).append("ip=").append(nic.getIPv4Address());
            buf.append(" eth").append(deviceId).append("mask=").append(nic.getIPv4Netmask());

            if (nic.isDefaultNic()) {
                buf.append(" gateway=").append(nic.getIPv4Gateway());
                buf.append(" dns1=").append(nic.getIPv4Gateway());
            }

            if (nic.getTrafficType() == TrafficType.Guest) {
                guestNetwork = _ntwkModel.getNetwork(nic.getNetworkId());
            } else if (nic.getTrafficType() == TrafficType.Management) {
                buf.append(" localgw=").append(dest.getPod().getGateway());
            } else if (nic.getTrafficType() == TrafficType.Control) {
                controlNic = nic;
                // VpcInline LB control command is sent over management server in VMware
                if (dest.getHost().getHypervisorType() == HypervisorType.VMware) {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Check if we need to add management server explicit route to Internal LB. pod cidr: " + dest.getPod().getCidrAddress() + "/" +
                                dest.getPod().getCidrSize() + ", pod gateway: " + dest.getPod().getGateway() + ", management host: " + _mgmtHost);
                    }

                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Add management server explicit route to VpcInline LB.");
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
            final String domain = guestNetwork.getNetworkDomain();
            if (domain != null) {
                buf.append(" domain=").append(domain);
            }
        }

        final String type = "ilbvm";
        buf.append(" type=" + type);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Boot Args for " + profile + ": " + buf.toString());
        }

        return true;
    }

    @Override
    public boolean finalizeDeployment(Commands cmds, VirtualMachineProfile profile, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException {

        DomainRouterVO vpcInlineLbVm = _vpcInlineLbVmDao.findById(profile.getId());

        Network guestNetwork = null;
        NicProfile guestNic = null;

        List<NicProfile> nics = profile.getNics();
        for (NicProfile nic : nics) {
            if (nic.getTrafficType() == TrafficType.Guest) {
                guestNic = nic;
                guestNetwork = _ntwkModel.getNetwork(nic.getNetworkId());
            } else if (nic.getTrafficType() == TrafficType.Control) {
                vpcInlineLbVm.setPrivateIpAddress(nic.getIPv4Address());
                vpcInlineLbVm.setPrivateMacAddress(nic.getMacAddress());
            }
        }

        _vpcInlineLbVmDao.update(vpcInlineLbVm.getId(), vpcInlineLbVm);

        // On the firewall provider for the network, create a static NAT rule between the source IP
        // address and the load balancing IP address
        if (guestNic != null) {
            for (VpcInlineLbMappingVO mapping : _vpcInlineLbMappingDao.listByNicId(guestNic.getId())) {
                final IPAddressVO publicIp = _ipAddressDao.findById(mapping.getPublicIpId());
                NicSecondaryIpVO secondaryIp = _nicSecondaryIpDao.findById(mapping.getNicSecondaryIpId());
                applyStaticNatRuleForInlineLBRule(guestNetwork, false, publicIp.getId(), secondaryIp.getIp4Address());
            }
        }

        finalizeCommandsOnStart(cmds, profile);
        return true;
    }

    @Override
    public boolean finalizeStart(VirtualMachineProfile profile, long hostId, Commands cmds, ReservationContext context) {
        DomainRouterVO vpcInlineLbVm = _vpcInlineLbVmDao.findById(profile.getId());

        boolean result = true;

        Answer answer = cmds.getAnswer("checkSsh");
        if (answer != null && answer instanceof CheckSshAnswer) {
            CheckSshAnswer sshAnswer = (CheckSshAnswer) answer;
            if (!sshAnswer.getResult()) {
                s_logger.warn("Unable to ssh to the vpcInline LB VM: " + sshAnswer.getDetails());
                result = false;
            }
        } else {
            result = false;
        }

        if (!result) {
            return false;
        }

        //Get guest network info
        List<Network> guestNetworks = new ArrayList<Network>();
        List<? extends Nic> vpcInlineLbVmNics = _nicDao.listByVmId(profile.getId());
        for (Nic vpcInlineLbVmNic : vpcInlineLbVmNics) {
            Network network = _ntwkModel.getNetwork(vpcInlineLbVmNic.getNetworkId());
            if (network.getTrafficType() == TrafficType.Guest) {
                guestNetworks.add(network);
            }
        }

        answer = cmds.getAnswer("getDomRVersion");
        if (answer != null && answer instanceof GetDomRVersionAnswer) {
            GetDomRVersionAnswer versionAnswer = (GetDomRVersionAnswer)answer;
            if (!answer.getResult()) {
                s_logger.warn("Unable to get the template/scripts version of vpcInline LB VM " + vpcInlineLbVm.getInstanceName() +
                        " due to: " + versionAnswer.getDetails());
                result = false;
            } else {
                vpcInlineLbVm.setTemplateVersion(versionAnswer.getTemplateVersion());
                vpcInlineLbVm.setScriptsVersion(versionAnswer.getScriptsVersion());
                _vpcInlineLbVmDao.persist(vpcInlineLbVm, guestNetworks);
            }
        } else {
            result = false;
        }

        return result;
    }

    @Override
    public boolean finalizeCommandsOnStart(Commands cmds, VirtualMachineProfile profile) {
        DomainRouterVO vpcInlineLbVm = _vpcInlineLbVmDao.findById(profile.getId());
        NicProfile controlNic = getNicProfileByTrafficType(profile, TrafficType.Control);

        if (controlNic == null) {
            s_logger.error("Control network doesn't exist for the vpcInline LB vm " + vpcInlineLbVm);
            return false;
        }

        finalizeSshAndVersionOnStart(cmds, profile, vpcInlineLbVm, controlNic);

        // restart network if restartNetwork = false is not specified in profile parameters
        boolean reprogramGuestNtwk = true;
        if (Boolean.FALSE.equals(profile.getParameter(Param.ReProgramGuestNetworks))) {
            reprogramGuestNtwk = false;
        }

        VirtualRouterProvider lbProvider = _vrProviderDao.findById(vpcInlineLbVm.getElementId());
        if (lbProvider == null) {
            throw new CloudRuntimeException("Cannot find related element " + Type.VpcInlineLbVm + " of vm: " + vpcInlineLbVm.getHostName());
        }

        Provider provider = Provider.getProvider(lbProvider.getType().toString());
        if (provider == null) {
            throw new CloudRuntimeException("Cannot find related provider of provider: " + lbProvider.getType().toString());
        }

        try {
            final NicProfile guestNic = getNicProfileByTrafficType(profile, TrafficType.Guest);
            finalizeLbRulesForIp(cmds, vpcInlineLbVm, provider, guestNic.getNetworkId(), guestNic.getNetworkId(), reprogramGuestNtwk);
        } catch (ResourceUnavailableException e) {
            return false;
        }

        return true;
    }

    @Override
    public void finalizeStop(VirtualMachineProfile profile, Answer answer) {
    }

    @Override
    public void finalizeExpunge(VirtualMachine vm) {
    }

    @Override
    public void prepareStop(VirtualMachineProfile profile) {
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

        String offUUID = LB_SERVICE_OFFERING.value();
        if (offUUID != null && !offUUID.isEmpty()) {
            //get the id by offering UUID
            ServiceOfferingVO off = _serviceOfferingDao.findByUuid(offUUID);
            if (off != null) {
                _vpcInlineLbVmOfferingId = off.getId();
            } else {
                s_logger.warn("Invalid offering UUID is passed in " + LB_SERVICE_OFFERING.key() + "; the default offering will be used instead");
            }
        }

        //if offering wasn't set, try to get the default one
        if (_vpcInlineLbVmOfferingId == 0L) {
            boolean useLocalStorage = ConfigurationManagerImpl.SystemVMUseLocalStorage.value();
            ServiceOfferingVO newOff = new ServiceOfferingVO("System Offering For VpcInline LB VM", 1, VpcInlineLoadBalancerVmManager.DEFAULT_LB_VM_RAMSIZE, VpcInlineLoadBalancerVmManager.DEFAULT_LB_VM_CPU_MHZ, null,
                    null, true, null, Storage.ProvisioningType.THIN, useLocalStorage, true, null, true, VirtualMachine.Type.VpcInlineLoadBalancerVm, true);
            newOff.setUniqueName(ServiceOffering.vpcInlineLbVmDefaultOffUniqueName);
            newOff = _serviceOfferingDao.persistSystemServiceOffering(newOff);
            _vpcInlineLbVmOfferingId = newOff.getId();
        }

        _itMgr.registerGuru(VirtualMachine.Type.VpcInlineLoadBalancerVm, this);

        if (s_logger.isInfoEnabled()) {
            s_logger.info(getName()  +  " has been configured");
        }

        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    private NicProfile getNicProfileByTrafficType(VirtualMachineProfile profile, TrafficType trafficType) {
        for (NicProfile nic : profile.getNics()) {
            if (nic.getTrafficType() == trafficType && nic.getIPv4Address() != null) {
                return nic;
            }
        }
        return null;
     }

    private void finalizeSshAndVersionOnStart(Commands cmds, VirtualMachineProfile profile, DomainRouterVO router, NicProfile controlNic) {
        cmds.addCommand("checkSsh", new CheckSshCommand(profile.getInstanceName(), controlNic.getIPv4Address(), 3922));

        // Update vpcInline lb vm template/scripts version
        final GetDomRVersionCmd command = new GetDomRVersionCmd();
        command.setAccessDetail(NetworkElementCommand.ROUTER_IP, controlNic.getIPv4Address());
        command.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        cmds.addCommand("getDomRVersion", command);
    }

    private void finalizeLbRulesForIp(Commands cmds, DomainRouterVO vpcInlineLbVm, Provider provider, long guestNtwkId, long lbNtwkId, boolean reprogramGuestNtwk) throws ResourceUnavailableException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Resending load balancing rules as a part of start for " + vpcInlineLbVm);
        }
        List<LoadBalancingRule> lbRules = new ArrayList<LoadBalancingRule>();
        Map<Ip, String> secondaryIpMap = Maps.newHashMap();

        if (_ntwkModel.isProviderSupportServiceInNetwork(guestNtwkId, Service.Lb, provider)) {
            Network lbNtwk = _networkDao.findById(lbNtwkId);

            for (VpcInlineLbMappingVO mapping : _vpcInlineLbMappingDao.listByVmId(vpcInlineLbVm.getId())) {
                final NicVO nicVO = _nicDao.findById(mapping.getNicId());
                final NicSecondaryIpVO secondaryIp = _nicSecondaryIpDao.findById(mapping.getNicSecondaryIpId());
                final IPAddressVO ip = _ipAddressDao.findById(mapping.getPublicIpId());

                createIpAssocCommands(vpcInlineLbVm, lbNtwk, nicVO, secondaryIp.getIp4Address(), true, cmds);

                secondaryIpMap.put(ip.getAddress(), secondaryIp.getIp4Address());

                if (reprogramGuestNtwk) {
                    List<LoadBalancerVO> lbs = _loadBalancerDao.listByIpAddress(ip.getId());
                    // Re-apply load balancing rules
                    for (LoadBalancerVO lb : lbs) {
                        List<LbDestination> dstList = _lbMgr.getExistingDestinations(lb.getId());
                        List<LbStickinessPolicy> policyList = _lbMgr.getStickinessPolicies(lb.getId());
                        List<LbHealthCheckPolicy> hcPolicyList = _lbMgr.getHealthCheckPolicies(lb.getId());
                        LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList, policyList, hcPolicyList,  ip.getAddress());
                        lbRules.add(loadBalancing);
                    }
                }
            }

        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Found " + lbRules.size() + " load balancing rule(s) to apply as a part of VPC inline LB vm" + vpcInlineLbVm + " start.");
        }
        if (!lbRules.isEmpty()) {
            createApplyLoadBalancingRulesCommands(lbRules, vpcInlineLbVm, cmds, lbNtwkId, secondaryIpMap);
        }
    }

    /**
     * Translates the rules into a LoadBalancerConfigCommand, which will be added to the Commands provided
     */
    private void createApplyLoadBalancingRulesCommands(List<LoadBalancingRule> rules, VirtualRouter vpcInlineLbVm, Commands cmds, long guestNetworkId, Map<Ip, String> publicIpGuestIpMapping) {
        Network guestNetwork = _ntwkModel.getNetwork(guestNetworkId);
        Nic guestNic = _nicDao.findByNtwkIdAndInstanceId(guestNetwork.getId(), vpcInlineLbVm.getId());
        NicProfile guestNicProfile = new NicProfile(guestNic, guestNetwork, guestNic.getBroadcastUri(), guestNic.getIsolationUri(),
                _ntwkModel.getNetworkRate(guestNetwork.getId(), vpcInlineLbVm.getId()),
                _ntwkModel.isSecurityGroupSupportedInNetwork(guestNetwork),
                _ntwkModel.getNetworkTag(vpcInlineLbVm.getHypervisorType(), guestNetwork));

        LoadBalancerTO[] lbs = new LoadBalancerTO[rules.size()];
        int i = 0;
        for (LoadBalancingRule rule : rules) {
            boolean revoked = (rule.getState().equals(FirewallRule.State.Revoke));
            String protocol = rule.getProtocol();
            String algorithm = rule.getAlgorithm();
            String uuid = rule.getUuid();

            String srcIp = publicIpGuestIpMapping.get(rule.getSourceIp());

            int srcPort = rule.getSourcePortStart();
            List<LbDestination> destinations = rule.getDestinations();
            List<LbStickinessPolicy> stickinessPolicies = rule.getStickinessPolicies();

            LoadBalancerTO lb = new LoadBalancerTO(uuid, srcIp, srcPort, protocol, algorithm, revoked, false, true, destinations, stickinessPolicies,
                    rule.getHealthCheckPolicies(), rule.getLbSslCert(), rule.getLbProtocol());
            lbs[i++] = lb;
        }


        NetworkOffering offering = _networkOfferingDao.findById(guestNetwork.getNetworkOfferingId());
        String maxconn;
        if (offering.getConcurrentConnections() == null) {
            maxconn =  _configDao.getValue(Config.NetworkLBHaproxyMaxConn.key());
        } else {
            maxconn = offering.getConcurrentConnections().toString();
        }

        LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(lbs, guestNic.getIPv4Address(),
                guestNic.getIPv4Address(), vpcInlineLbVm.getPrivateIpAddress(),
                _itMgr.toNicTO(guestNicProfile, vpcInlineLbVm.getHypervisorType()), vpcInlineLbVm.getVpcId(), maxconn, offering.isKeepAliveEnabled());

        cmd.lbStatsVisibility = _configDao.getValue(Config.NetworkLBHaproxyStatsVisbility.key());
        cmd.lbStatsUri = _configDao.getValue(Config.NetworkLBHaproxyStatsUri.key());
        cmd.lbStatsAuth = _configDao.getValue(Config.NetworkLBHaproxyStatsAuth.key());
        cmd.lbStatsPort = _configDao.getValue(Config.NetworkLBHaproxyStatsPort.key());

        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getVpcInlineLbControlIp(vpcInlineLbVm.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, guestNic.getIPv4Address());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, vpcInlineLbVm.getInstanceName());
        DataCenterVO dcVo = _dcDao.findById(vpcInlineLbVm.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        cmds.addCommand(cmd);
    }

    private String getVpcInlineLbControlIp(long vpcInlineLbVmId) {
        String controlIpAddress = null;
        List<NicVO> nics = _nicDao.listByVmId(vpcInlineLbVmId);
        for (NicVO nic : nics) {
            Network ntwk = _ntwkModel.getNetwork(nic.getNetworkId());
            if (ntwk.getTrafficType() == TrafficType.Control) {
                controlIpAddress = nic.getIPv4Address();
            }
        }

        if(controlIpAddress == null) {
            s_logger.warn("Unable to find VpcInline LB control ip in its attached NICs!. VpcInline LB vm: " + vpcInlineLbVmId);
            DomainRouterVO vpcInlineLbVm = _vpcInlineLbVmDao.findById(vpcInlineLbVmId);
            return vpcInlineLbVm.getPrivateIpAddress();
        }

        return controlIpAddress;
    }

    private NicVO getVpcInlineLbGuestNic(long vpcInlineLbVmId) {
        List<NicVO> nics = _nicDao.listByVmId(vpcInlineLbVmId);
        for (NicVO nic : nics) {
            Network ntwk = _ntwkModel.getNetwork(nic.getNetworkId());
            if (ntwk.getTrafficType() == TrafficType.Guest) {
                return nic;
            }
        }

        return null;
    }

    private NicSecondaryIp findOrCreateSecondaryIp(final Account owner, final IPAddressVO publicIp, final Network guestNetwork, final NicVO guestNic) throws ResourceUnavailableException, InsufficientAddressCapacityException {
        String secondaryIp = publicIp.getVmIp();
        NicSecondaryIp secondaryIpVO = null;

        if (secondaryIp != null) {
            secondaryIpVO = _nicSecondaryIpDao.findByIp4AddressAndNicId(secondaryIp, guestNic.getId());
        }

        if (secondaryIpVO == null) {
            final String addrFinal = _ipAddrMgr.allocateGuestIP(guestNetwork, secondaryIp);

            long id = Transaction.execute(new TransactionCallbackWithException<Long, ResourceUnavailableException>() {
                @Override
                public Long doInTransaction(TransactionStatus status) throws ResourceUnavailableException {
                    boolean nicSecondaryIpSet = guestNic.getSecondaryIp();
                    if (!nicSecondaryIpSet) {
                        guestNic.setSecondaryIp(true);
                        _nicDao.update(guestNic.getId(), guestNic);
                    }

                    Long vmId = guestNic.getInstanceId();
                    NicSecondaryIpVO secondaryIpVO = new NicSecondaryIpVO(guestNic.getId(), addrFinal, vmId, owner.getAccountId(), owner.getDomainId(), guestNetwork.getId());
                    _nicSecondaryIpDao.persist(secondaryIpVO);

                    publicIp.setAssociatedWithVmId(guestNic.getInstanceId());
                    publicIp.setVmIp(addrFinal);

                    _ipAddressDao.update(publicIp.getId(), publicIp);

                    applyStaticNatRuleForInlineLBRule(guestNetwork, false, publicIp.getId(), secondaryIpVO.getIp4Address());

                    return secondaryIpVO.getId();
                }
            });

            secondaryIpVO = _nicSecondaryIpDao.findById(id);
        }

        return secondaryIpVO;
    }

    @Override
    public boolean destroyVpcInlineLbVm(long vmId, Account caller, Long callerUserId)
            throws ResourceUnavailableException, ConcurrentOperationException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Attempting to destroy VpcInline LB vm " + vmId);
        }

        DomainRouterVO vpcInlineLbVm = _vpcInlineLbVmDao.findById(vmId);
        if (vpcInlineLbVm == null) {
            return true;
        }

        _accountMgr.checkAccess(caller, null, true, vpcInlineLbVm);

        NicVO guestNic = getVpcInlineLbGuestNic(vpcInlineLbVm.getId());
        Network network = _ntwkModel.getNetwork(guestNic.getNetworkId());

        //removeSecondaryIpMapping
        for (VpcInlineLbMappingVO mapping : _vpcInlineLbMappingDao.listByNicId(guestNic.getId())) {
            revokeLoadBalancingIpNic(network, mapping);
        }

        _itMgr.expunge(vpcInlineLbVm.getUuid());
        _vpcInlineLbVmDao.remove(vpcInlineLbVm.getId());
        return true;
    }

    @Override
    public VirtualRouter stopVpcInlineLbVm(long vmId, boolean forced, Account caller, long callerUserId)
            throws ConcurrentOperationException, ResourceUnavailableException {
        DomainRouterVO vpcInlineLbVm = _vpcInlineLbVmDao.findById(vmId);
        if (vpcInlineLbVm == null || vpcInlineLbVm.getRole() != Role.LB) {
            throw new InvalidParameterValueException("Can't find vpcInline lb vm by id specified");
        }

        //check permissions
        _accountMgr.checkAccess(caller, null, true, vpcInlineLbVm);
        return stopVpcInlineLbVm(vpcInlineLbVm, forced);
    }

    private VirtualRouter stopVpcInlineLbVm(DomainRouterVO vpcInlineLbVm, boolean forced) throws ResourceUnavailableException, ConcurrentOperationException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Stopping vpcInline lb vm " + vpcInlineLbVm);
        }
        try {
            _itMgr.advanceStop(vpcInlineLbVm.getUuid(), forced);
            return _vpcInlineLbVmDao.findById(vpcInlineLbVm.getId());
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Unable to stop " + vpcInlineLbVm, e);
        }
    }

    @Override
    public List<DomainRouterVO> deployVpcInlineLbVm(Network guestNetwork, DeployDestination dest, Account owner, Map<Param, Object> params)
            throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        List<DomainRouterVO> vpcInlineLbVms = findOrDeployVpcInlineLbVm(guestNetwork, null, dest, owner, params);
        return startVpcInlineLbVms(params, vpcInlineLbVms);
    }

    @Override
    public List<DomainRouterVO> deployVpcInlineLbVm(Network guestNetwork, DeployDestination dest, Account owner, Map<Param, Object> params, Collection<IPAddressVO> lbIps)
            throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        List<DomainRouterVO> vpcInlineLbVms = findOrDeployVpcInlineLbVm(guestNetwork, null, dest, owner, params);

        DomainRouterVO vpcInlineLbVm = Iterables.getFirst(vpcInlineLbVms, null);

        if(vpcInlineLbVm != null && CollectionUtils.isNotEmpty(lbIps)) {
            for (IPAddressVO lbIp : lbIps) {
                createSecondaryIpMapping(guestNetwork, dest, owner, lbIp.getAddress(), vpcInlineLbVm);
            }
        }

        return startVpcInlineLbVms(params, vpcInlineLbVms);
    }

    @Override
    public String createSecondaryIpMapping(Network guestNetwork, DeployDestination dest, Account owner, Ip requestedPublicIp, VirtualRouter vpcInlineLbVm)
            throws ResourceUnavailableException, InsufficientAddressCapacityException {
        final IPAddressVO publicIp = _ipAddressDao.findByIpAndDcId(dest.getDataCenter().getId(), requestedPublicIp.addr());
        final NicVO guestNic = _nicDao.findByNtwkIdAndInstanceId(guestNetwork.getId(), vpcInlineLbVm.getId());
        NicSecondaryIp secondaryIp = findOrCreateSecondaryIp(owner, publicIp, guestNetwork, guestNic);

        VpcInlineLbMappingVO mapping = _vpcInlineLbMappingDao.findByPublicIpAddress(publicIp.getId());
        if (mapping == null) {
            mapping = new VpcInlineLbMappingVO(publicIp.getId(), vpcInlineLbVm.getId(), guestNic.getId(), secondaryIp.getId());
            _vpcInlineLbMappingDao.persist(mapping);

            if (vpcInlineLbVm.getState() == State.Running) {
                sendIpAssoc(vpcInlineLbVm, guestNetwork, guestNic, secondaryIp.getIp4Address(), true);
            }
        } else if (mapping.getVmId() != vpcInlineLbVm.getId()) {
            s_logger.error("VPC Inline LB Mapping found, but the VM id is incorrect: expected:" + vpcInlineLbVm.getId() + ", was: "  + mapping.getVmId());
        } else if (mapping.getNicSecondaryIpId() != secondaryIp.getId()) {
            s_logger.error("VPC Inline LB Mapping found, but the VM details are incorrect: " + mapping + ", ");
        }

        return secondaryIp.getIp4Address();
    }

    @Override
    public boolean removeSecondaryIpMapping(final Network guestNetwork, Account owner, Ip requestedPublicIp, final VirtualRouter vpcInlineLbVm) throws ResourceUnavailableException {
        final IPAddressVO publicIp = _ipAddressDao.findByIpAndDcId(guestNetwork.getDataCenterId(), requestedPublicIp.addr());
        final VpcInlineLbMappingVO mapping = _vpcInlineLbMappingDao.findByPublicIpAddress(publicIp.getId());

        boolean result = false;
        if (mapping != null) {
            result = Transaction.execute(new TransactionCallbackWithException<Boolean, ResourceUnavailableException>() {
                @Override
                public Boolean doInTransaction(TransactionStatus status) throws ResourceUnavailableException {
                    final NicVO guestNic = _nicDao.findById(mapping.getNicId());
                    NicSecondaryIpVO secondaryIp = _nicSecondaryIpDao.findById(mapping.getNicSecondaryIpId());

                    applyStaticNatRuleForInlineLBRule(guestNetwork, true, publicIp.getId(), secondaryIp.getIp4Address());
                    sendIpAssoc(vpcInlineLbVm, guestNetwork, guestNic, secondaryIp.getIp4Address(), false);

                    publicIp.setAssociatedWithVmId(null);
                    publicIp.setAssociatedWithNetworkId(null);
                    publicIp.setVmIp(null);
                    _ipAddressDao.update(publicIp.getId(), publicIp);

                    boolean result = _vpcInlineLbMappingDao.remove(mapping.getId());
                    _nicSecondaryIpDao.remove(mapping.getNicSecondaryIpId());

                    return result;
                }
            });
        }

        return result;
    }

    private List<DomainRouterVO> startVpcInlineLbVms(Map<Param, Object> params, List<DomainRouterVO> vpcInlineLbVms)
            throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        List<DomainRouterVO> runningVpcInlineLbVms = null;
        if (vpcInlineLbVms != null) {
            runningVpcInlineLbVms = new ArrayList<DomainRouterVO>();
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Have no vpcInline lb vms to start");
            }
            return null;
        }

        for (DomainRouterVO vpcInlineLbVm : vpcInlineLbVms) {
            if (vpcInlineLbVm.getState() != State.Running) {
                vpcInlineLbVm = startVpcInlineLbVm(vpcInlineLbVm, params);
            }

            if (vpcInlineLbVm != null) {
                runningVpcInlineLbVms.add(vpcInlineLbVm);
            }
        }
        return runningVpcInlineLbVms;
    }

    @DB
    private List<DomainRouterVO> findOrDeployVpcInlineLbVm(Network guestNetwork, Ip requestedPublicIp, DeployDestination dest, Account owner, Map<Param, Object> params)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        List<DomainRouterVO> vpcInlineLbVms = new ArrayList<DomainRouterVO>();
        Network lock = _networkDao.acquireInLockTable(guestNetwork.getId(), NetworkOrchestrationService.NetworkLockTimeout.value());
        if (lock == null) {
            throw new ConcurrentOperationException("Unable to lock network " + guestNetwork.getId());
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Lock is acquired for network id " + lock.getId() + " as a part of vpcInline lb startup in " + dest);
        }

        try {
            if (guestNetwork.getState() != Network.State.Implemented && guestNetwork.getState() != Network.State.Setup &&
                    guestNetwork.getState() != Network.State.Implementing) {
                s_logger.warn("Network is not yet fully implemented: " + guestNetwork);
                throw new IllegalStateException("Network is not yet fully implemented: " + guestNetwork);
            } else if (guestNetwork.getTrafficType() != TrafficType.Guest) {
                s_logger.warn("Network doesn't use guest traffic type: " + guestNetwork);
                throw new IllegalStateException("Network doesn't use guest traffic type: " + guestNetwork);
            }

            //deploy vpcInline lb vm
            vpcInlineLbVms = findVpcInlineLbVms(guestNetwork, requestedPublicIp);
            if (CollectionUtils.isNotEmpty(vpcInlineLbVms)) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Found " + vpcInlineLbVms.size() + " vpcInline lb vms for the requested IP " + requestedPublicIp);
                }
                return vpcInlineLbVms;
            }

            // Check if another inline VM exists for the same guest network, if so fail.
            if (!findVpcInlineLbVms(guestNetwork, null).isEmpty()) {
                throw new ResourceUnavailableException("Only one VPC Inline LB allowed per network", Network.class, guestNetwork.getId());
            }

            DeploymentPlan plan = new DataCenterDeployment(dest.getDataCenter().getId());
            long vpcInlineLbProviderId = getVpcInlineLbProviderId(guestNetwork);

            LinkedHashMap<Network, List<? extends NicProfile>> networks = createVpcInlineLbVmNetworks(guestNetwork, plan);
            //Pass startVm=false as we are holding the network lock that needs to be released at the end of vm allocation
            DomainRouterVO vpcInlineLbVm = deployVpcInlineLbVm(owner, dest, plan, params, vpcInlineLbProviderId, _vpcInlineLbVmOfferingId,
                    guestNetwork.getVpcId(), networks, false);
            if (vpcInlineLbVm != null) {
                _vpcInlineLbVmDao.addRouterToGuestNetwork(vpcInlineLbVm, guestNetwork);
                vpcInlineLbVms.add(vpcInlineLbVm);
            }
        } finally {
            _networkDao.releaseFromLockTable(lock.getId());
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Lock is released for network id " + lock.getId() + " as a part of vpcInline lb vm startup in " + dest);
            }
        }
        return vpcInlineLbVms;
    }

    private long getVpcInlineLbProviderId(Network guestNetwork) {
        Type providerType = Type.VpcInlineLbVm;
        long physicalNetworkId = _ntwkModel.getPhysicalNetworkId(guestNetwork);

        PhysicalNetworkServiceProvider provider = _physicalProviderDao.findByServiceProvider(physicalNetworkId, providerType.toString());
        if (provider == null) {
            throw new CloudRuntimeException("Cannot find service provider " + providerType.toString() + " in physical network " + physicalNetworkId);
        }

        VirtualRouterProvider vpcInlineLbProvider = _vrProviderDao.findByNspIdAndType(provider.getId(), providerType);
        if (vpcInlineLbProvider == null) {
            throw new CloudRuntimeException("Cannot find provider " + providerType.toString() + " as service provider " + provider.getId());
        }
        return vpcInlineLbProvider.getId();
    }

    private Nic revokeLoadBalancingIpNic(Network network, VpcInlineLbMappingVO mapping) throws ResourceUnavailableException {
        Nic guestNic = null;
        if (mapping != null) {
            final IPAddressVO sourceIp = _ipAddressDao.findById(mapping.getPublicIpId());

            // Find the NIC that the mapping refers to

            NicSecondaryIpVO secondaryIp = _nicSecondaryIpDao.findById(mapping.getNicSecondaryIpId());
            guestNic = _nicDao.findById(mapping.getNicId());

            // On the firewall provider for the network, delete the static NAT rule between the source IP
            // address and the load balancing IP address
            applyStaticNatRuleForInlineLBRule(network, true, sourceIp.getId(), secondaryIp.getIp4Address());

            // Remove the mapping from public IP to LB secondary IP
            final IPAddressVO publicIp = _ipAddressDao.findById(mapping.getPublicIpId());
            publicIp.setAssociatedWithVmId(null);
            publicIp.setVmIp(null);
            _ipAddressDao.update(publicIp.getId(), publicIp);

            // Delete the mapping between the source IP address and the load balancing IP address
            _vpcInlineLbMappingDao.expunge(mapping.getId());

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Revoked static nat rule for inline load balancer");
            }
        } else {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Revoking a rule for an inline load balancer that has not been programmed yet.");
            }
        }
        return guestNic;
    }

    private void applyStaticNatRuleForInlineLBRule(Network network, final boolean revoked, long publicIp, String privateIp) throws ResourceUnavailableException {
        IPAddressVO ipVO = _ipAddressDao.findById(publicIp);
        StaticNatImpl staticNat = new StaticNatImpl(ipVO.getAccountId(), ipVO.getDomainId(),
                network.getId(), ipVO.getId(), privateIp, revoked);

        final VlanVO vlanVO = _vlanDao.findById(ipVO.getVlanId());

        PublicIp publicIpAddress = new PublicIp(ipVO, vlanVO, ipVO.getMacAddress()) {
            @Override public State getState() {
                if (revoked) {
                    return State.Releasing;
                } else {
                    return super.getState();
                }
            }
        };

        List<? extends StaticNat> staticNats = Arrays.asList(staticNat);
        List<? extends PublicIpAddress> ips = Arrays.asList(publicIpAddress);
        StaticNatServiceProvider element = _ntwkMgr.getStaticNatProviderForNetwork(network);

        if (!revoked) {
            element.getIpDeployer(network).applyIps(network, ips, Sets.newHashSet(Service.StaticNat));
        }

        element.applyStaticNats(network, staticNats);

        if (revoked) {
            element.getIpDeployer(network).applyIps(network, ips, Sets.newHashSet(Service.StaticNat));
        }
    }

    private LinkedHashMap<Network, List<? extends NicProfile>> createVpcInlineLbVmNetworks(Network guestNetwork, DeploymentPlan plan)
            throws ConcurrentOperationException, InsufficientAddressCapacityException {
        //Form networks
        LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<Network, List<? extends NicProfile>>(3);

        //1) Guest network - default
        if (guestNetwork != null) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Adding nic for VpcInline LB in Guest network " + guestNetwork);
            }
            NicProfile guestNic = new NicProfile();
            guestNic.setIPv4Address(_ipAddrMgr.acquireGuestIpAddress(guestNetwork, null));
            guestNic.setFormat(Networks.AddressFormat.Ip4);
            guestNic.setIPv4Gateway(guestNetwork.getGateway());
            guestNic.setBroadcastUri(guestNetwork.getBroadcastUri());
            guestNic.setBroadcastType(guestNetwork.getBroadcastDomainType());
            guestNic.setIsolationUri(guestNetwork.getBroadcastUri());
            guestNic.setMode(guestNetwork.getMode());
            String gatewayCidr = guestNetwork.getCidr();
            guestNic.setIPv4Netmask(NetUtils.getCidrNetmask(gatewayCidr));
            guestNic.setDefaultNic(true);
            networks.put(guestNetwork, new ArrayList<NicProfile>(Arrays.asList(guestNic)));
        }

        //2) Control network
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Adding nic for VpcInline LB vm in Control network");
        }
        List<? extends NetworkOffering> offerings = _ntwkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemControlNetwork);
        NetworkOffering controlOffering = Iterables.getFirst(offerings, null);
        Network controlConfig = Iterables.getFirst(_ntwkMgr.setupNetwork(_accountMgr.getSystemAccount(), controlOffering, plan, null, null, false), null);
        networks.put(controlConfig, new ArrayList<NicProfile>());

        return networks;
    }

    @Override
    public List<DomainRouterVO> findVpcInlineLbVms(Network guestNetwork, Ip requestedPublicIp) {
        List<DomainRouterVO> vpcInlineLbVms = _vpcInlineLbVmDao.listByNetworkAndRole(guestNetwork.getId(), Role.LB);
        if (requestedPublicIp != null && !vpcInlineLbVms.isEmpty()) {
            String guestIp = findGuestIpForPublicIp(guestNetwork.getDataCenterId(), requestedPublicIp);
            if (guestIp == null) {
                vpcInlineLbVms = new LinkedList<DomainRouterVO>();
            }

            Iterator<DomainRouterVO> it = vpcInlineLbVms.iterator();
            while (it.hasNext()) {
                DomainRouterVO vm = it.next();
                Nic nic = _nicDao.findByIp4AddressAndNetworkIdAndInstanceId(guestNetwork.getId(), vm.getId(), guestIp);
                if (nic == null) {
                    it.remove();
                }
            }
        }

        return vpcInlineLbVms;
    }

    @Override
    public void fillPublicIps(ListResponse<DomainRouterResponse> vpcInlineLbVms) {
        for (DomainRouterResponse domainRouterResponse : vpcInlineLbVms.getResponses()) {
            final String id = domainRouterResponse.getObjectId();
            DomainRouterVO lbVm = _vpcInlineLbVmDao.findByUuid(id);
            final NicVO vpcInlineLbGuestNic = getVpcInlineLbGuestNic(lbVm.getId());

            List<VpcInlineLbMappingVO> mappings = _vpcInlineLbMappingDao.listByNicId(vpcInlineLbGuestNic.getId());
            if (CollectionUtils.isNotEmpty(mappings)) {
                StringBuilder sb = new StringBuilder();
                List<NicSecondaryIpResponse> nicSecondaryIpResponses = new LinkedList<NicSecondaryIpResponse>();

                for (VpcInlineLbMappingVO mapping : mappings) {
                    IPAddressVO ipAddressVO = _ipAddressDao.findById(mapping.getPublicIpId());
                    NicSecondaryIpVO secondaryIpVO = _nicSecondaryIpDao.findById(mapping.getNicSecondaryIpId());
                    sb.append(", ").append(ipAddressVO.getAddress().addr());

                    NicSecondaryIpResponse secondaryIpResponse = new NicSecondaryIpResponse();
                    secondaryIpResponse.setId(secondaryIpVO.getUuid());
                    secondaryIpResponse.setIpAddr(secondaryIpVO.getIp4Address());
                    nicSecondaryIpResponses.add(secondaryIpResponse);
                }

                domainRouterResponse.setPublicIp(sb.substring(2));

                for (NicResponse nicResponse : domainRouterResponse.getNics()) {
                    if (nicResponse.getId().equals(vpcInlineLbGuestNic.getUuid())) {
                        nicResponse.setSecondaryIps(nicSecondaryIpResponses);
                        break;
                    }
                }
            }

        }
    }

    @Override
    public boolean cleanupUnusedVpcInlineLbVms(long guestNetworkId, Account caller, Long callerUserId)
            throws ResourceUnavailableException, ConcurrentOperationException {
        List<DomainRouterVO> vpcInlineLbVms = _vpcInlineLbVmDao.listByNetworkAndRole(guestNetworkId, Role.LB);
        for (DomainRouterVO vpcInlineLbVm : vpcInlineLbVms) {
            Nic nic = _nicDao.findByNtwkIdAndInstanceId(guestNetworkId, vpcInlineLbVm.getId());

            List<VpcInlineLbMappingVO> mappings = _vpcInlineLbMappingDao.listByNicId(nic.getId());

            if (mappings.isEmpty()) {
                destroyVpcInlineLbVm(vpcInlineLbVm.getId(), caller, callerUserId);
            }
        }

        return true;
    }

    private String findGuestIpForPublicIp(long dataCenterId, Ip requestedPublicIp) {
        final IPAddressVO sourceIp = _ipAddressDao.findByIpAndDcId(dataCenterId, requestedPublicIp.addr());
        return sourceIp.getVmIp();
    }

    private DomainRouterVO deployVpcInlineLbVm(Account owner, DeployDestination dest, DeploymentPlan plan, Map<Param, Object> params,
            long vpcInlineLbProviderId, long svcOffId, Long vpcId, LinkedHashMap<Network, List<? extends NicProfile>> networks,
        boolean startVm) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {

        ServiceOfferingVO routerOffering = _serviceOfferingDao.findById(svcOffId);

        // VpcInline lb is the network element, we don't know the hypervisor type yet.
        // Try to allocate the vpcInline lb twice using diff hypervisors, and when failed both times, throw the exception up
        List<HypervisorType> hypervisors = getHypervisors(dest, plan, null);

        int allocateRetry = 0;
        int startRetry = 0;
        DomainRouterVO vpcInlineLbVm = null;
        for (Iterator<HypervisorType> iter = hypervisors.iterator(); iter.hasNext();) {
            HypervisorType hType = iter.next();
            try {
                long id = _vpcInlineLbVmDao.getNextInSequence(Long.class, "id");
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Creating the vpcInline lb vm " + id + " in datacenter "  + dest.getDataCenter() + " with hypervisor type " + hType);
                }                String templateName = null;
                switch (hType) {
                    case XenServer:
                        templateName = VirtualNetworkApplianceManager.RouterTemplateXen.valueIn(dest.getDataCenter().getId());
                        break;
                    case KVM:
                        templateName = VirtualNetworkApplianceManager.RouterTemplateKvm.valueIn(dest.getDataCenter().getId());
                        break;
                    case VMware:
                        templateName = VirtualNetworkApplianceManager.RouterTemplateVmware.valueIn(dest.getDataCenter().getId());
                        break;
                    case Hyperv:
                        templateName = VirtualNetworkApplianceManager.RouterTemplateHyperV.valueIn(dest.getDataCenter().getId());
                        break;
                    case LXC:
                        templateName = VirtualNetworkApplianceManager.RouterTemplateLxc.valueIn(dest.getDataCenter().getId());
                        break;
                    default: break;
                }
                VMTemplateVO template = _templateDao.findRoutingTemplate(hType, templateName);

                if (template == null) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug(hType + " won't support system vm, skip it");
                    }
                    continue;
                }

                String vpcInlineLbVmName = VirtualMachineName.getSystemVmName(id, _instance, s_vpcInlineLbVmNamePrefix);
                vpcInlineLbVm = new DomainRouterVO(id, routerOffering.getId(), vpcInlineLbProviderId, vpcInlineLbVmName, template.getId(),
                        template.getHypervisorType(), template.getGuestOSId(), owner.getDomainId(), owner.getAccountId(), owner.getId(), false,
                        RedundantState.UNKNOWN, false, false, VirtualMachine.Type.VpcInlineLoadBalancerVm, vpcId);
                vpcInlineLbVm.setRole(Role.LB);
                vpcInlineLbVm = _vpcInlineLbVmDao.persist(vpcInlineLbVm);
                _itMgr.allocate(vpcInlineLbVm.getInstanceName(), template, routerOffering, networks, plan, null);
                vpcInlineLbVm = _vpcInlineLbVmDao.findById(vpcInlineLbVm.getId());
            } catch (InsufficientCapacityException ex) {
                if (allocateRetry < 2 && iter.hasNext()) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Failed to allocate the VpcInline lb vm with hypervisor type " + hType + ", retrying one more time");
                    }
                    continue;
                } else {
                    throw ex;
                }
            } finally {
                allocateRetry++;
            }

            if (startVm) {
                try {
                    vpcInlineLbVm = startVpcInlineLbVm(vpcInlineLbVm, params);
                    break;
                } catch (InsufficientCapacityException ex) {
                    if (startRetry < 2 && iter.hasNext()) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Failed to start the VpcInline lb vm  " + vpcInlineLbVm + " with hypervisor type " + hType + ", " +
                                    "destroying it and recreating one more time");
                        }
                        // destroy the vpcInline lb vm
                        destroyVpcInlineLbVm(vpcInlineLbVm.getId(), _accountMgr.getSystemAccount(), User.UID_SYSTEM);
                    } else {
                        throw ex;
                    }
                } finally {
                    startRetry++;
                }
            } else {
                //return stopped vpcInline lb vm
                return vpcInlineLbVm;
            }
        }
        return vpcInlineLbVm;
    }



    private DomainRouterVO startVpcInlineLbVm(DomainRouterVO vpcInlineLbVm, Map<Param, Object> params)
            throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Starting VpcInline LB VM " + vpcInlineLbVm);
        }
        _itMgr.start(vpcInlineLbVm.getUuid(), params);
        if (vpcInlineLbVm.isStopPending()) {
            s_logger.info("Clear the stop pending flag of VpcInline LB VM " + vpcInlineLbVm.getHostName() + " after start router successfully!");
            vpcInlineLbVm.setStopPending(false);
            vpcInlineLbVm = _vpcInlineLbVmDao.persist(vpcInlineLbVm);
        }
        return _vpcInlineLbVmDao.findById(vpcInlineLbVm.getId());
    }

    private List<HypervisorType> getHypervisors(DeployDestination dest, DeploymentPlan plan, List<HypervisorType> supportedHypervisors)
            throws InsufficientServerCapacityException {
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
            throw new InsufficientServerCapacityException("Unable to create vpcInline lb vm, " +
                    "there are no clusters in the zone ", DataCenter.class, dest.getDataCenter().getId());
        }
        return hypervisors;
    }

    @Override
    public boolean applyLoadBalancingRules(Network network, List<LoadBalancingRule> rules, DomainRouterVO lbVm, Map<Ip, String> publicIpGuestIpMapping)
            throws ResourceUnavailableException {
        if (CollectionUtils.isEmpty(rules)) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("No lb rules to be applied for network " + network);
            }
            return true;
        }
        s_logger.info("lb rules to be applied for network ");

        //only one vpcInline lb vm is supported per ip address at this time
        if (lbVm == null) {
            throw new CloudRuntimeException("Can't apply the lb rules on network " + network + " as the list of vpcInline lb vms is empty");
        }

        if (lbVm.getState() == State.Running) {
            return sendLBRules(lbVm, rules, network.getId(), publicIpGuestIpMapping);
            //        && applyLoadBalancingRulesToAclList(network, rules);
        } else if (lbVm.getState() == State.Stopped || lbVm.getState() == State.Stopping) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("VpcInline LB VM " + lbVm.getInstanceName() + " is in " + lbVm.getState() +
                        ", so not sending apply lb rules commands to the backend");
            }
            return true;
        } else {
            s_logger.warn("Unable to apply lb rules, VpcInline LB VM is not in the right state " + lbVm.getState());
            throw new ResourceUnavailableException("Unable to apply lb rules; VpcInline LB VM is not in the right state", DataCenter.class, lbVm.getDataCenterId());
        }
    }

    private boolean sendLBRules(VirtualRouter vpcInlineLbVm, List<LoadBalancingRule> rules, long guestNetworkId, Map<Ip, String> publicIpGuestIpMapping) throws ResourceUnavailableException {
        Commands cmds = new Commands(Command.OnError.Continue);
        createApplyLoadBalancingRulesCommands(rules, vpcInlineLbVm, cmds, guestNetworkId, publicIpGuestIpMapping);
        return sendCommandsToVpcInlineLbVm(vpcInlineLbVm, cmds);
    }

    private boolean sendIpAssoc(VirtualRouter vpcInlineLbVm, Network guestNetwork, NicVO guestNic, String ipAddress, boolean add) throws ResourceUnavailableException {
        Commands cmds = new Commands(Command.OnError.Continue);
        createIpAssocCommands(vpcInlineLbVm, guestNetwork, guestNic, ipAddress, add, cmds);
        return sendCommandsToVpcInlineLbVm(vpcInlineLbVm, cmds);
    }

    private void createIpAssocCommands(VirtualRouter vpcInlineLbVm, Network guestNetwork, NicVO guestNic, String ipAddress, boolean add, Commands cmds) {
        IpAddressTO[] ips = new IpAddressTO[1];
        String netmask = NetUtils.getCidrNetmask(guestNetwork.getCidr());
        ips[0] = new IpAddressTO(0, ipAddress, add, false, false, "vlan://untagged", guestNetwork.getGateway(), netmask, guestNic.getMacAddress(), null, false);
        ips[0].setNicDevId(guestNic.getDeviceId());
        ips[0].setTrafficType(TrafficType.Guest);

        DataCenterVO dcVo = _dcDao.findById(vpcInlineLbVm.getDataCenterId());
        IpAssocVpcCommand cmd = new IpAssocVpcCommand(ips);
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getVpcInlineLbControlIp(vpcInlineLbVm.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, guestNic.getIPv4Address());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, vpcInlineLbVm.getInstanceName());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());

        cmds.addCommand(cmd);
    }

    private boolean sendCommandsToVpcInlineLbVm(final VirtualRouter vpcInlineLbVm, Commands cmds) throws AgentUnavailableException {
        Answer[] answers = null;
        try {
            answers = _agentMgr.send(vpcInlineLbVm.getHostId(), cmds);
        } catch (OperationTimedoutException e) {
            s_logger.warn("Timed Out", e);
            throw new AgentUnavailableException("Unable to send commands to virtual router ", vpcInlineLbVm.getHostId(), e);
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
    public VirtualRouter startVpcInlineLbVm(long vpcInlineLbVmId, Account caller, long callerUserId) throws InsufficientCapacityException,
            ConcurrentOperationException, ResourceUnavailableException {
        DomainRouterVO vpcInlineLbVm = _vpcInlineLbVmDao.findById(vpcInlineLbVmId);
        if (vpcInlineLbVm == null || vpcInlineLbVm.getRole() != Role.LB) {
            throw new InvalidParameterValueException("Can't find vpcInline lb vm by id specified");
        }

        //check permissions
        _accountMgr.checkAccess(caller, null, true, vpcInlineLbVm);
        return startVpcInlineLbVm(vpcInlineLbVm, null);
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(ListVpcInlineLoadBalancerVmsCmd.class);
        cmdList.add(StartVpcInlineLoadBalancerVmCmd.class);
        cmdList.add(StopVpcInlineLoadBalancerVmCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return VpcInlineLoadBalancerVmManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] { LB_SERVICE_OFFERING };
    }
}
