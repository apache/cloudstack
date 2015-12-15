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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.lb.ApplicationLoadBalancerRuleVO;
import org.apache.cloudstack.lb.dao.ApplicationLoadBalancerRuleDao;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetDomRVersionAnswer;
import com.cloud.agent.api.GetDomRVersionCmd;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.manager.Commands;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManagerImpl;
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
import com.cloud.network.IpAddressManager;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.VirtualRouterProvider;
import com.cloud.network.VirtualRouterProvider.Type;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.VirtualRouterProviderDao;
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
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.user.dao.UserDao;
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

@Local(value = {InternalLoadBalancerVMManager.class, InternalLoadBalancerVMService.class})
public class InternalLoadBalancerVMManagerImpl extends ManagerBase implements InternalLoadBalancerVMManager, InternalLoadBalancerVMService, VirtualMachineGuru {
    private static final Logger s_logger = Logger.getLogger(InternalLoadBalancerVMManagerImpl.class);
    static final private String InternalLbVmNamePrefix = "b";

    private String _instance;
    private String _mgmtHost;
    private String _mgmtCidr;
    private long _internalLbVmOfferingId = 0L;

    @Inject
    IpAddressManager _ipAddrMgr;
    @Inject
    VirtualMachineManager _itMgr;
    @Inject
    DomainRouterDao _internalLbVmDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    VirtualRouterProviderDao _vrProviderDao;
    @Inject
    ApplicationLoadBalancerRuleDao _lbDao;
    @Inject
    NetworkModel _ntwkModel;
    @Inject
    LoadBalancingRulesManager _lbMgr;
    @Inject
    NicDao _nicDao;
    @Inject
    AccountManager _accountMgr;
    @Inject
    NetworkDao _networkDao;
    @Inject
    NetworkOrchestrationService _ntwkMgr;
    @Inject
    ServiceOfferingDao _serviceOfferingDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalProviderDao;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    VMTemplateDao _templateDao;
    @Inject
    ResourceManager _resourceMgr;
    @Inject
    UserDao _userDao;

    @Override
    public boolean finalizeVirtualMachineProfile(final VirtualMachineProfile profile, final DeployDestination dest, final ReservationContext context) {

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
                // Internal LB control command is sent over management server in VMware
                if (dest.getHost().getHypervisorType() == HypervisorType.VMware) {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Check if we need to add management server explicit route to Internal LB. pod cidr: " + dest.getPod().getCidrAddress() + "/" +
                                dest.getPod().getCidrSize() + ", pod gateway: " + dest.getPod().getGateway() + ", management host: " + _mgmtHost);
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
            final String domain = guestNetwork.getNetworkDomain();
            if (domain != null) {
                buf.append(" domain=" + domain);
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
    public boolean finalizeDeployment(final Commands cmds, final VirtualMachineProfile profile, final DeployDestination dest, final ReservationContext context)
            throws ResourceUnavailableException {

        final DomainRouterVO internalLbVm = _internalLbVmDao.findById(profile.getId());

        final List<NicProfile> nics = profile.getNics();
        for (final NicProfile nic : nics) {
            if (nic.getTrafficType() == TrafficType.Control) {
                internalLbVm.setPrivateIpAddress(nic.getIPv4Address());
                internalLbVm.setPrivateMacAddress(nic.getMacAddress());
            }
        }
        _internalLbVmDao.update(internalLbVm.getId(), internalLbVm);

        finalizeCommandsOnStart(cmds, profile);
        return true;
    }

    @Override
    public boolean finalizeStart(final VirtualMachineProfile profile, final long hostId, final Commands cmds, final ReservationContext context) {
        DomainRouterVO internalLbVm = _internalLbVmDao.findById(profile.getId());

        boolean result = true;

        Answer answer = cmds.getAnswer("checkSsh");
        if (answer != null && answer instanceof CheckSshAnswer) {
            final CheckSshAnswer sshAnswer = (CheckSshAnswer)answer;
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
        final List<Network> guestNetworks = new ArrayList<Network>();
        final List<? extends Nic> internalLbVmNics = _nicDao.listByVmId(profile.getId());
        for (final Nic internalLbVmNic : internalLbVmNics) {
            final Network network = _ntwkModel.getNetwork(internalLbVmNic.getNetworkId());
            if (network.getTrafficType() == TrafficType.Guest) {
                guestNetworks.add(network);
            }
        }

        answer = cmds.getAnswer("getDomRVersion");
        if (answer != null && answer instanceof GetDomRVersionAnswer) {
            final GetDomRVersionAnswer versionAnswer = (GetDomRVersionAnswer)answer;
            if (answer == null || !answer.getResult()) {
                s_logger.warn("Unable to get the template/scripts version of internal LB VM " + internalLbVm.getInstanceName() + " due to: " + versionAnswer.getDetails());
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
    public boolean finalizeCommandsOnStart(final Commands cmds, final VirtualMachineProfile profile) {
        final DomainRouterVO internalLbVm = _internalLbVmDao.findById(profile.getId());
        final NicProfile controlNic = getNicProfileByTrafficType(profile, TrafficType.Control);

        if (controlNic == null) {
            s_logger.error("Control network doesn't exist for the internal LB vm " + internalLbVm);
            return false;
        }

        finalizeSshAndVersionOnStart(cmds, profile, internalLbVm, controlNic);

        // restart network if restartNetwork = false is not specified in profile parameters
        boolean reprogramGuestNtwk = true;
        if (profile.getParameter(Param.ReProgramGuestNetworks) != null && (Boolean)profile.getParameter(Param.ReProgramGuestNetworks) == false) {
            reprogramGuestNtwk = false;
        }

        final VirtualRouterProvider lbProvider = _vrProviderDao.findById(internalLbVm.getElementId());
        if (lbProvider == null) {
            throw new CloudRuntimeException("Cannot find related element " + Type.InternalLbVm + " of vm: " + internalLbVm.getHostName());
        }

        final Provider provider = Network.Provider.getProvider(lbProvider.getType().toString());
        if (provider == null) {
            throw new CloudRuntimeException("Cannot find related provider of provider: " + lbProvider.getType().toString());
        }

        if (reprogramGuestNtwk) {
            final NicProfile guestNic = getNicProfileByTrafficType(profile, TrafficType.Guest);
            finalizeLbRulesForIp(cmds, internalLbVm, provider, new Ip(guestNic.getIPv4Address()), guestNic.getNetworkId());
        }

        return true;
    }

    @Override
    public void finalizeStop(final VirtualMachineProfile profile, final Answer answer) {
    }

    @Override
    public void finalizeExpunge(final VirtualMachine vm) {
    }

    @Override
    public void prepareStop(final VirtualMachineProfile profile) {
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        final Map<String, String> configs = _configDao.getConfiguration("AgentManager", params);
        _instance = configs.get("instance.name");
        if (_instance == null) {
            _instance = "DEFAULT";
        }

        _mgmtHost = configs.get("host");
        _mgmtCidr = _configDao.getValue(Config.ManagementNetwork.key());

        final String offUUID = configs.get(Config.InternalLbVmServiceOfferingId.key());
        if (offUUID != null && !offUUID.isEmpty()) {
            //get the id by offering UUID
            final ServiceOfferingVO off = _serviceOfferingDao.findByUuid(offUUID);
            if (off != null) {
                _internalLbVmOfferingId = off.getId();
            } else {
                s_logger.warn("Invalid offering UUID is passed in " + Config.InternalLbVmServiceOfferingId.key() + "; the default offering will be used instead");
            }
        }

        //if offering wasn't set, try to get the default one
        if (_internalLbVmOfferingId == 0L) {
            List<ServiceOfferingVO> offerings = _serviceOfferingDao.createSystemServiceOfferings("System Offering For Internal LB VM",
                    ServiceOffering.internalLbVmDefaultOffUniqueName, 1, InternalLoadBalancerVMManager.DEFAULT_INTERNALLB_VM_RAMSIZE,
                    InternalLoadBalancerVMManager.DEFAULT_INTERNALLB_VM_CPU_MHZ, null, null, true, null,
                    Storage.ProvisioningType.THIN, true, null, true, VirtualMachine.Type.InternalLoadBalancerVm, true);
            if (offerings == null || offerings.size() < 2) {
                String msg = "Data integrity problem : System Offering For Internal LB VM has been removed?";
                s_logger.error(msg);
                throw new ConfigurationException(msg);
            }
        }

        _itMgr.registerGuru(VirtualMachine.Type.InternalLoadBalancerVm, this);

        if (s_logger.isInfoEnabled()) {
            s_logger.info(getName() + " has been configured");
        }

        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    protected NicProfile getNicProfileByTrafficType(final VirtualMachineProfile profile, final TrafficType trafficType) {
        for (final NicProfile nic : profile.getNics()) {
            if (nic.getTrafficType() == trafficType && nic.getIPv4Address() != null) {
                return nic;
            }
        }
        return null;
    }

    protected void finalizeSshAndVersionOnStart(final Commands cmds, final VirtualMachineProfile profile, final DomainRouterVO router, final NicProfile controlNic) {
        cmds.addCommand("checkSsh", new CheckSshCommand(profile.getInstanceName(), controlNic.getIPv4Address(), 3922));

        // Update internal lb vm template/scripts version
        final GetDomRVersionCmd command = new GetDomRVersionCmd();
        command.setAccessDetail(NetworkElementCommand.ROUTER_IP, controlNic.getIPv4Address());
        command.setAccessDetail(NetworkElementCommand.ROUTER_NAME, router.getInstanceName());
        cmds.addCommand("getDomRVersion", command);
    }

    protected void finalizeLbRulesForIp(final Commands cmds, final DomainRouterVO internalLbVm, final Provider provider, final Ip sourceIp, final long guestNtwkId) {
        s_logger.debug("Resending load balancing rules as a part of start for " + internalLbVm);
        final List<ApplicationLoadBalancerRuleVO> lbs = _lbDao.listBySrcIpSrcNtwkId(sourceIp, guestNtwkId);
        final List<LoadBalancingRule> lbRules = new ArrayList<LoadBalancingRule>();
        if (_ntwkModel.isProviderSupportServiceInNetwork(guestNtwkId, Service.Lb, provider)) {
            // Re-apply load balancing rules
            for (final ApplicationLoadBalancerRuleVO lb : lbs) {
                final List<LbDestination> dstList = _lbMgr.getExistingDestinations(lb.getId());
                final List<LbStickinessPolicy> policyList = _lbMgr.getStickinessPolicies(lb.getId());
                final List<LbHealthCheckPolicy> hcPolicyList = _lbMgr.getHealthCheckPolicies(lb.getId());
                final LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList, policyList, hcPolicyList, sourceIp);
                lbRules.add(loadBalancing);
            }
        }

        s_logger.debug("Found " + lbRules.size() + " load balancing rule(s) to apply as a part of Intenrnal LB vm" + internalLbVm + " start.");
        if (!lbRules.isEmpty()) {
            createApplyLoadBalancingRulesCommands(lbRules, internalLbVm, cmds, guestNtwkId);
        }
    }

    private void createApplyLoadBalancingRulesCommands(final List<LoadBalancingRule> rules, final VirtualRouter internalLbVm, final Commands cmds, final long guestNetworkId) {

        final LoadBalancerTO[] lbs = new LoadBalancerTO[rules.size()];
        int i = 0;
        final boolean inline = false;
        for (final LoadBalancingRule rule : rules) {
            final boolean revoked = rule.getState().equals(FirewallRule.State.Revoke);
            final String protocol = rule.getProtocol();
            final String algorithm = rule.getAlgorithm();
            final String uuid = rule.getUuid();

            final String srcIp = rule.getSourceIp().addr();
            final int srcPort = rule.getSourcePortStart();
            final List<LbDestination> destinations = rule.getDestinations();
            final List<LbStickinessPolicy> stickinessPolicies = rule.getStickinessPolicies();
            final LoadBalancerTO lb = new LoadBalancerTO(uuid, srcIp, srcPort, protocol, algorithm, revoked, false, inline, destinations, stickinessPolicies);
            lbs[i++] = lb;
        }

        final Network guestNetwork = _ntwkModel.getNetwork(guestNetworkId);
        final Nic guestNic = _nicDao.findByNtwkIdAndInstanceId(guestNetwork.getId(), internalLbVm.getId());
        final NicProfile guestNicProfile =
                new NicProfile(guestNic, guestNetwork, guestNic.getBroadcastUri(), guestNic.getIsolationUri(), _ntwkModel.getNetworkRate(guestNetwork.getId(),
                        internalLbVm.getId()), _ntwkModel.isSecurityGroupSupportedInNetwork(guestNetwork), _ntwkModel.getNetworkTag(internalLbVm.getHypervisorType(),
                                guestNetwork));

        final NetworkOffering offering = _networkOfferingDao.findById(guestNetwork.getNetworkOfferingId());
        String maxconn = null;
        if (offering.getConcurrentConnections() == null) {
            maxconn = _configDao.getValue(Config.NetworkLBHaproxyMaxConn.key());
        } else {
            maxconn = offering.getConcurrentConnections().toString();
        }
        final LoadBalancerConfigCommand cmd =
                new LoadBalancerConfigCommand(lbs, guestNic.getIPv4Address(), guestNic.getIPv4Address(), internalLbVm.getPrivateIpAddress(), _itMgr.toNicTO(guestNicProfile,
                        internalLbVm.getHypervisorType()), internalLbVm.getVpcId(), maxconn, offering.isKeepAliveEnabled());

        cmd.lbStatsVisibility = _configDao.getValue(Config.NetworkLBHaproxyStatsVisbility.key());
        cmd.lbStatsUri = _configDao.getValue(Config.NetworkLBHaproxyStatsUri.key());
        cmd.lbStatsAuth = _configDao.getValue(Config.NetworkLBHaproxyStatsAuth.key());
        cmd.lbStatsPort = _configDao.getValue(Config.NetworkLBHaproxyStatsPort.key());

        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, getInternalLbControlIp(internalLbVm.getId()));
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_GUEST_IP, guestNic.getIPv4Address());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, internalLbVm.getInstanceName());
        final DataCenterVO dcVo = _dcDao.findById(internalLbVm.getDataCenterId());
        cmd.setAccessDetail(NetworkElementCommand.ZONE_NETWORK_TYPE, dcVo.getNetworkType().toString());
        cmds.addCommand(cmd);
    }

    protected String getInternalLbControlIp(final long internalLbVmId) {
        String controlIpAddress = null;
        final List<NicVO> nics = _nicDao.listByVmId(internalLbVmId);
        for (final NicVO nic : nics) {
            final Network ntwk = _ntwkModel.getNetwork(nic.getNetworkId());
            if (ntwk.getTrafficType() == TrafficType.Control) {
                controlIpAddress = nic.getIPv4Address();
            }
        }

        if (controlIpAddress == null) {
            s_logger.warn("Unable to find Internal LB control ip in its attached NICs!. Internal LB vm: " + internalLbVmId);
            final DomainRouterVO internalLbVm = _internalLbVmDao.findById(internalLbVmId);
            return internalLbVm.getPrivateIpAddress();
        }

        return controlIpAddress;
    }

    @Override
    public boolean destroyInternalLbVm(final long vmId, final Account caller, final Long callerUserId) throws ResourceUnavailableException, ConcurrentOperationException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Attempting to destroy Internal LB vm " + vmId);
        }

        final DomainRouterVO internalLbVm = _internalLbVmDao.findById(vmId);
        if (internalLbVm == null) {
            return true;
        }

        _accountMgr.checkAccess(caller, null, true, internalLbVm);

        _itMgr.expunge(internalLbVm.getUuid());
        _internalLbVmDao.remove(internalLbVm.getId());
        return true;
    }

    @Override
    public VirtualRouter stopInternalLbVm(final long vmId, final boolean forced, final Account caller, final long callerUserId) throws ConcurrentOperationException, ResourceUnavailableException {
        final DomainRouterVO internalLbVm = _internalLbVmDao.findById(vmId);
        if (internalLbVm == null || internalLbVm.getRole() != Role.INTERNAL_LB_VM) {
            throw new InvalidParameterValueException("Can't find internal lb vm by id specified");
        }

        //check permissions
        _accountMgr.checkAccess(caller, null, true, internalLbVm);

        return stopInternalLbVm(internalLbVm, forced, caller, callerUserId);
    }

    protected VirtualRouter stopInternalLbVm(final DomainRouterVO internalLbVm, final boolean forced, final Account caller, final long callerUserId) throws ResourceUnavailableException,
    ConcurrentOperationException {
        s_logger.debug("Stopping internal lb vm " + internalLbVm);
        try {
            _itMgr.advanceStop(internalLbVm.getUuid(), forced);
            return _internalLbVmDao.findById(internalLbVm.getId());
        } catch (final OperationTimedoutException e) {
            throw new CloudRuntimeException("Unable to stop " + internalLbVm, e);
        }
    }

    @Override
    public List<DomainRouterVO> deployInternalLbVm(final Network guestNetwork, final Ip requestedGuestIp, final DeployDestination dest, final Account owner, final Map<Param, Object> params)
            throws InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {

        final List<DomainRouterVO> internalLbVms = findOrDeployInternalLbVm(guestNetwork, requestedGuestIp, dest, owner, params);

        return startInternalLbVms(params, internalLbVms);
    }

    protected List<DomainRouterVO> startInternalLbVms(final Map<Param, Object> params, final List<DomainRouterVO> internalLbVms) throws StorageUnavailableException,
    InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
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
    protected List<DomainRouterVO> findOrDeployInternalLbVm(final Network guestNetwork, final Ip requestedGuestIp, final DeployDestination dest, final Account owner, final Map<Param, Object> params)
            throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {

        List<DomainRouterVO> internalLbVms = new ArrayList<DomainRouterVO>();
        final Network lock = _networkDao.acquireInLockTable(guestNetwork.getId(), NetworkOrchestrationService.NetworkLockTimeout.value());
        if (lock == null) {
            throw new ConcurrentOperationException("Unable to lock network " + guestNetwork.getId());
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Lock is acquired for network id " + lock.getId() + " as a part of internal lb startup in " + dest);
        }

        final long internalLbProviderId = getInternalLbProviderId(guestNetwork);

        try {
            assert guestNetwork.getState() == Network.State.Implemented || guestNetwork.getState() == Network.State.Setup ||
                    guestNetwork.getState() == Network.State.Implementing : "Network is not yet fully implemented: " + guestNetwork;
            assert guestNetwork.getTrafficType() == TrafficType.Guest;

            //deploy internal lb vm
            final Pair<DeploymentPlan, List<DomainRouterVO>> planAndInternalLbVms = getDeploymentPlanAndInternalLbVms(dest, guestNetwork.getId(), requestedGuestIp);
            internalLbVms = planAndInternalLbVms.second();
            final DeploymentPlan plan = planAndInternalLbVms.first();

            if (internalLbVms.size() > 0) {
                s_logger.debug("Found " + internalLbVms.size() + " internal lb vms for the requested IP " + requestedGuestIp.addr());
                return internalLbVms;
            }

            final LinkedHashMap<Network, List<? extends NicProfile>> networks = createInternalLbVmNetworks(guestNetwork, plan, requestedGuestIp);
            long internalLbVmOfferingId = _internalLbVmOfferingId;
            if (internalLbVmOfferingId == 0L) {
                ServiceOfferingVO serviceOffering = _serviceOfferingDao.findDefaultSystemOffering(ServiceOffering.internalLbVmDefaultOffUniqueName, ConfigurationManagerImpl.SystemVMUseLocalStorage.valueIn(dest.getDataCenter().getId()));
                internalLbVmOfferingId = serviceOffering.getId();
            }
            //Pass startVm=false as we are holding the network lock that needs to be released at the end of vm allocation
            final DomainRouterVO internalLbVm =
                    deployInternalLbVm(owner, dest, plan, params, internalLbProviderId, internalLbVmOfferingId, guestNetwork.getVpcId(), networks, false);
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

    protected long getInternalLbProviderId(final Network guestNetwork) {
        final Type type = Type.InternalLbVm;
        final long physicalNetworkId = _ntwkModel.getPhysicalNetworkId(guestNetwork);

        final PhysicalNetworkServiceProvider provider = _physicalProviderDao.findByServiceProvider(physicalNetworkId, type.toString());
        if (provider == null) {
            throw new CloudRuntimeException("Cannot find service provider " + type.toString() + " in physical network " + physicalNetworkId);
        }

        final VirtualRouterProvider internalLbProvider = _vrProviderDao.findByNspIdAndType(provider.getId(), type);
        if (internalLbProvider == null) {
            throw new CloudRuntimeException("Cannot find provider " + type.toString() + " as service provider " + provider.getId());
        }

        return internalLbProvider.getId();
    }

    protected LinkedHashMap<Network, List<? extends NicProfile>> createInternalLbVmNetworks(final Network guestNetwork, final DeploymentPlan plan, final Ip guestIp) throws ConcurrentOperationException,
    InsufficientAddressCapacityException {

        //Form networks
        final LinkedHashMap<Network, List<? extends NicProfile>> networks = new LinkedHashMap<Network, List<? extends NicProfile>>(3);

        //1) Guest network - default
        if (guestNetwork != null) {
            s_logger.debug("Adding nic for Internal LB in Guest network " + guestNetwork);
            final NicProfile guestNic = new NicProfile();
            if (guestIp != null) {
                guestNic.setIPv4Address(guestIp.addr());
            } else {
                guestNic.setIPv4Address(_ipAddrMgr.acquireGuestIpAddress(guestNetwork, null));
            }
            guestNic.setIPv4Gateway(guestNetwork.getGateway());
            guestNic.setBroadcastUri(guestNetwork.getBroadcastUri());
            guestNic.setBroadcastType(guestNetwork.getBroadcastDomainType());
            guestNic.setIsolationUri(guestNetwork.getBroadcastUri());
            guestNic.setMode(guestNetwork.getMode());
            final String gatewayCidr = guestNetwork.getCidr();
            guestNic.setIPv4Netmask(NetUtils.getCidrNetmask(gatewayCidr));
            guestNic.setDefaultNic(true);
            networks.put(guestNetwork, new ArrayList<NicProfile>(Arrays.asList(guestNic)));
        }

        //2) Control network
        s_logger.debug("Adding nic for Internal LB vm in Control network ");
        final List<? extends NetworkOffering> offerings = _ntwkModel.getSystemAccountNetworkOfferings(NetworkOffering.SystemControlNetwork);
        final NetworkOffering controlOffering = offerings.get(0);
        final Network controlConfig = _ntwkMgr.setupNetwork(_accountMgr.getSystemAccount(), controlOffering, plan, null, null, false).get(0);
        networks.put(controlConfig, new ArrayList<NicProfile>());

        return networks;
    }

    protected Pair<DeploymentPlan, List<DomainRouterVO>> getDeploymentPlanAndInternalLbVms(final DeployDestination dest, final long guestNetworkId, final Ip requestedGuestIp) {
        final long dcId = dest.getDataCenter().getId();
        final DeploymentPlan plan = new DataCenterDeployment(dcId);
        final List<DomainRouterVO> internalLbVms = findInternalLbVms(guestNetworkId, requestedGuestIp);

        return new Pair<DeploymentPlan, List<DomainRouterVO>>(plan, internalLbVms);

    }

    @Override
    public List<DomainRouterVO> findInternalLbVms(final long guestNetworkId, final Ip requestedGuestIp) {
        final List<DomainRouterVO> internalLbVms = _internalLbVmDao.listByNetworkAndRole(guestNetworkId, Role.INTERNAL_LB_VM);
        if (requestedGuestIp != null && !internalLbVms.isEmpty()) {
            final Iterator<DomainRouterVO> it = internalLbVms.iterator();
            while (it.hasNext()) {
                final DomainRouterVO vm = it.next();
                final Nic nic = _nicDao.findByNtwkIdAndInstanceId(guestNetworkId, vm.getId());
                if (!nic.getIPv4Address().equalsIgnoreCase(requestedGuestIp.addr())) {
                    it.remove();
                }
            }
        }
        return internalLbVms;
    }

    protected DomainRouterVO deployInternalLbVm(final Account owner, final DeployDestination dest, final DeploymentPlan plan, final Map<Param, Object> params, final long internalLbProviderId,
            final long svcOffId, final Long vpcId, final LinkedHashMap<Network, List<? extends NicProfile>> networks, final boolean startVm) throws ConcurrentOperationException,
            InsufficientAddressCapacityException, InsufficientServerCapacityException, InsufficientCapacityException, StorageUnavailableException,
            ResourceUnavailableException {

        final ServiceOfferingVO routerOffering = _serviceOfferingDao.findById(svcOffId);

        // Internal lb is the network element, we don't know the hypervisor type yet.
        // Try to allocate the internal lb twice using diff hypervisors, and when failed both times, throw the exception up
        final List<HypervisorType> hypervisors = getHypervisors(dest, plan, null);

        int allocateRetry = 0;
        int startRetry = 0;
        DomainRouterVO internalLbVm = null;
        for (final Iterator<HypervisorType> iter = hypervisors.iterator(); iter.hasNext();) {
            final HypervisorType hType = iter.next();
            try {
                final long id = _internalLbVmDao.getNextInSequence(Long.class, "id");
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Creating the internal lb vm " + id + " in datacenter " + dest.getDataCenter() + " with hypervisor type " + hType);
                }
                String templateName = null;
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
                default:
                    break;
                }
                final VMTemplateVO template = _templateDao.findRoutingTemplate(hType, templateName);

                if (template == null) {
                    s_logger.debug(hType + " won't support system vm, skip it");
                    continue;
                }

                long userId = CallContext.current().getCallingUserId();
                if (CallContext.current().getCallingAccount().getId() != owner.getId()) {
                    userId =  _userDao.listByAccount(owner.getAccountId()).get(0).getId();
                }

                internalLbVm =
                        new DomainRouterVO(id, routerOffering.getId(), internalLbProviderId, VirtualMachineName.getSystemVmName(id, _instance, InternalLbVmNamePrefix),
                                template.getId(), template.getHypervisorType(), template.getGuestOSId(), owner.getDomainId(), owner.getId(), userId, false, RedundantState.UNKNOWN, false, false, VirtualMachine.Type.InternalLoadBalancerVm, vpcId);
                internalLbVm.setRole(Role.INTERNAL_LB_VM);
                internalLbVm = _internalLbVmDao.persist(internalLbVm);
                _itMgr.allocate(internalLbVm.getInstanceName(), template, routerOffering, networks, plan, null);
                internalLbVm = _internalLbVmDao.findById(internalLbVm.getId());
            } catch (final InsufficientCapacityException ex) {
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
                } catch (final InsufficientCapacityException ex) {
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

    protected DomainRouterVO startInternalLbVm(DomainRouterVO internalLbVm, final Account caller, final long callerUserId, final Map<Param, Object> params)
            throws StorageUnavailableException, InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException {
        s_logger.debug("Starting Internal LB VM " + internalLbVm);
        _itMgr.start(internalLbVm.getUuid(), params, null, null);
        if (internalLbVm.isStopPending()) {
            s_logger.info("Clear the stop pending flag of Internal LB VM " + internalLbVm.getHostName() + " after start router successfully!");
            internalLbVm.setStopPending(false);
            internalLbVm = _internalLbVmDao.persist(internalLbVm);
        }
        return _internalLbVmDao.findById(internalLbVm.getId());
    }

    protected List<HypervisorType> getHypervisors(final DeployDestination dest, final DeploymentPlan plan, final List<HypervisorType> supportedHypervisors)
            throws InsufficientServerCapacityException {
        List<HypervisorType> hypervisors = new ArrayList<HypervisorType>();

        final HypervisorType defaults = _resourceMgr.getDefaultHypervisor(dest.getDataCenter().getId());
        if (defaults != HypervisorType.None) {
            hypervisors.add(defaults);
        } else {
            //if there is no default hypervisor, get it from the cluster
            hypervisors = _resourceMgr.getSupportedHypervisorTypes(dest.getDataCenter().getId(), true, plan.getPodId());
        }

        //keep only elements defined in supported hypervisors
        final StringBuilder hTypesStr = new StringBuilder();
        if (supportedHypervisors != null && !supportedHypervisors.isEmpty()) {
            hypervisors.retainAll(supportedHypervisors);
            for (final HypervisorType hType : supportedHypervisors) {
                hTypesStr.append(hType).append(" ");
            }
        }

        if (hypervisors.isEmpty()) {
            throw new InsufficientServerCapacityException("Unable to create internal lb vm, " + "there are no clusters in the zone ", DataCenter.class,
                    dest.getDataCenter().getId());
        }
        return hypervisors;
    }

    @Override
    public boolean applyLoadBalancingRules(final Network network, final List<LoadBalancingRule> rules, final List<? extends VirtualRouter> internalLbVms)
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

        final VirtualRouter lbVm = internalLbVms.get(0);
        if (lbVm.getState() == State.Running) {
            return sendLBRules(lbVm, rules, network.getId());
        } else if (lbVm.getState() == State.Stopped || lbVm.getState() == State.Stopping) {
            s_logger.debug("Internal LB VM " + lbVm.getInstanceName() + " is in " + lbVm.getState() + ", so not sending apply lb rules commands to the backend");
            return true;
        } else {
            s_logger.warn("Unable to apply lb rules, Internal LB VM is not in the right state " + lbVm.getState());
            throw new ResourceUnavailableException("Unable to apply lb rules; Internal LB VM is not in the right state", DataCenter.class, lbVm.getDataCenterId());
        }
    }

    protected boolean sendLBRules(final VirtualRouter internalLbVm, final List<LoadBalancingRule> rules, final long guestNetworkId) throws ResourceUnavailableException {
        final Commands cmds = new Commands(Command.OnError.Continue);
        createApplyLoadBalancingRulesCommands(rules, internalLbVm, cmds, guestNetworkId);
        return sendCommandsToInternalLbVm(internalLbVm, cmds);
    }

    protected boolean sendCommandsToInternalLbVm(final VirtualRouter internalLbVm, final Commands cmds) throws AgentUnavailableException {
        Answer[] answers = null;
        try {
            answers = _agentMgr.send(internalLbVm.getHostId(), cmds);
        } catch (final OperationTimedoutException e) {
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
            for (final Answer answer : answers) {
                if (!answer.getResult()) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    public VirtualRouter startInternalLbVm(final long internalLbVmId, final Account caller, final long callerUserId) throws StorageUnavailableException, InsufficientCapacityException,
    ConcurrentOperationException, ResourceUnavailableException {

        final DomainRouterVO internalLbVm = _internalLbVmDao.findById(internalLbVmId);
        if (internalLbVm == null || internalLbVm.getRole() != Role.INTERNAL_LB_VM) {
            throw new InvalidParameterValueException("Can't find internal lb vm by id specified");
        }

        //check permissions
        _accountMgr.checkAccess(caller, null, true, internalLbVm);

        return startInternalLbVm(internalLbVm, caller, callerUserId, null);
    }
}
