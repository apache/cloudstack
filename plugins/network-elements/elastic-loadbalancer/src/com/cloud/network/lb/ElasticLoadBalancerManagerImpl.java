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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.user.loadbalancer.CreateLoadBalancerRuleCmd;
import org.apache.cloudstack.config.ApiServiceConfiguration;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.manager.Commands;
import com.cloud.configuration.Config;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.ElasticLbVmMapVO;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.LoadBalancerDao;
import com.cloud.network.dao.LoadBalancerVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.lb.LoadBalancingRule.LbDestination;
import com.cloud.network.lb.LoadBalancingRule.LbHealthCheckPolicy;
import com.cloud.network.lb.LoadBalancingRule.LbSslCert;
import com.cloud.network.lb.LoadBalancingRule.LbStickinessPolicy;
import com.cloud.network.lb.dao.ElasticLbVmMapDao;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.LoadBalancer;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.Storage;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.Ip;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineGuru;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.NicDao;

@Component
@Local(value = {ElasticLoadBalancerManager.class})
public class ElasticLoadBalancerManagerImpl extends ManagerBase implements ElasticLoadBalancerManager, VirtualMachineGuru {
    private static final Logger s_logger = Logger.getLogger(ElasticLoadBalancerManagerImpl.class);

    @Inject
    private AgentManager _agentMgr;
    @Inject
    private NetworkModel _networkModel;
    @Inject
    private LoadBalancingRulesManager _lbMgr;
    @Inject
    private final DomainRouterDao _routerDao = null;
    @Inject
    protected HostPodDao _podDao = null;
    @Inject
    protected ClusterDao _clusterDao;
    @Inject
    private final DataCenterDao _dcDao = null;
    @Inject
    protected NetworkDao _networkDao;
    @Inject
    protected NetworkOfferingDao _networkOfferingDao;
    @Inject
    private VirtualMachineManager _itMgr;
    @Inject
    private ConfigurationDao _configDao;
    @Inject
    private final ServiceOfferingDao _serviceOfferingDao = null;
    @Inject
    private AccountService _accountService;
    @Inject
    private LoadBalancerDao _lbDao;
    @Inject
    private ElasticLbVmMapDao _elbVmMapDao;
    @Inject
    private NicDao _nicDao;

    String _instance;

    static final private String SystemVmType = "elbvm";

    boolean _enabled;
    TrafficType _frontendTrafficType = TrafficType.Guest;

    Account _systemAcct;
    ScheduledExecutorService _gcThreadPool;
    String _mgmtCidr;

    Set<Long> _gcCandidateElbVmIds = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());

    int _elasticLbVmRamSize;
    int _elasticLbvmCpuMHz;
    int _elasticLbvmNumCpu;

    private LoadBalanceRuleHandler loadBalanceRuleHandler;

    private boolean sendCommandsToRouter(final DomainRouterVO elbVm, Commands cmds) throws AgentUnavailableException {
        Answer[] answers = null;
        try {
            answers = _agentMgr.send(elbVm.getHostId(), cmds);
        } catch (OperationTimedoutException e) {
            s_logger.warn("ELB: Timed Out", e);
            throw new AgentUnavailableException("Unable to send commands to virtual elbVm ", elbVm.getHostId(), e);
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

    private void createApplyLoadBalancingRulesCommands(List<LoadBalancingRule> rules, DomainRouterVO elbVm, Commands cmds, long guestNetworkId) {

        /* XXX: cert */
        LoadBalancerTO[] lbs = new LoadBalancerTO[rules.size()];
        int i = 0;
        for (LoadBalancingRule rule : rules) {
            boolean revoked = (rule.getState().equals(FirewallRule.State.Revoke));
            String protocol = rule.getProtocol();
            String algorithm = rule.getAlgorithm();

            String elbIp = rule.getSourceIp().addr();
            int srcPort = rule.getSourcePortStart();
            String uuid = rule.getUuid();
            List<LbDestination> destinations = rule.getDestinations();
            LoadBalancerTO lb = new LoadBalancerTO(uuid, elbIp, srcPort, protocol, algorithm, revoked, false, false, destinations);
            lbs[i++] = lb;
        }

        NetworkOffering offering = _networkOfferingDao.findById(guestNetworkId);
        String maxconn = null;
        if (offering.getConcurrentConnections() == null) {
            maxconn = _configDao.getValue(Config.NetworkLBHaproxyMaxConn.key());
        } else {
            maxconn = offering.getConcurrentConnections().toString();
        }
        LoadBalancerConfigCommand cmd = new LoadBalancerConfigCommand(lbs, elbVm.getPublicIpAddress(), _nicDao.getIpAddress(guestNetworkId, elbVm.getId()),
                elbVm.getPrivateIpAddress(), null, null, maxconn, offering.isKeepAliveEnabled());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_IP, elbVm.getPrivateIpAddress());
        cmd.setAccessDetail(NetworkElementCommand.ROUTER_NAME, elbVm.getInstanceName());
        //FIXME: why are we setting attributes directly? Ick!! There should be accessors and
        //the constructor should set defaults.
        cmd.lbStatsVisibility = _configDao.getValue(Config.NetworkLBHaproxyStatsVisbility.key());
        cmd.lbStatsUri = _configDao.getValue(Config.NetworkLBHaproxyStatsUri.key());
        cmd.lbStatsAuth = _configDao.getValue(Config.NetworkLBHaproxyStatsAuth.key());
        cmd.lbStatsPort = _configDao.getValue(Config.NetworkLBHaproxyStatsPort.key());

        cmds.addCommand(cmd);

    }

    protected boolean applyLBRules(DomainRouterVO elbVm, List<LoadBalancingRule> rules, long guestNetworkId) throws ResourceUnavailableException {
        Commands cmds = new Commands(Command.OnError.Continue);
        createApplyLoadBalancingRulesCommands(rules, elbVm, cmds, guestNetworkId);
        // Send commands to elbVm
        return sendCommandsToRouter(elbVm, cmds);
    }

    protected DomainRouterVO findElbVmForLb(LoadBalancingRule lb) {//TODO: use a table to lookup
        Network ntwk = _networkModel.getNetwork(lb.getNetworkId());
        long sourceIpId = _networkModel.getPublicIpAddress(lb.getSourceIp().addr(), ntwk.getDataCenterId()).getId();
        ElasticLbVmMapVO map = _elbVmMapDao.findOneByIp(sourceIpId);
        if (map == null) {
            return null;
        }
        DomainRouterVO elbVm = _routerDao.findById(map.getElbVmId());
        return elbVm;
    }

    @Override
    public boolean applyLoadBalancerRules(Network network, List<LoadBalancingRule> rules) throws ResourceUnavailableException {
        if (rules == null || rules.isEmpty()) {
            return true;
        }

        DomainRouterVO elbVm = findElbVmForLb(rules.get(0));

        if (elbVm == null) {
            s_logger.warn("Unable to apply lb rules, ELB vm  doesn't exist in the network " + network.getId());
            throw new ResourceUnavailableException("Unable to apply lb rules", DataCenter.class, network.getDataCenterId());
        }

        if (elbVm.getState() == State.Running) {
            //resend all rules for the public ip
            long sourceIpId = _networkModel.getPublicIpAddress(rules.get(0).getSourceIp().addr(), network.getDataCenterId()).getId();
            List<LoadBalancerVO> lbs = _lbDao.listByIpAddress(sourceIpId);
            List<LoadBalancingRule> lbRules = new ArrayList<LoadBalancingRule>();
            for (LoadBalancerVO lb : lbs) {
                List<LbDestination> dstList = _lbMgr.getExistingDestinations(lb.getId());
                List<LbStickinessPolicy> policyList = _lbMgr.getStickinessPolicies(lb.getId());
                List<LbHealthCheckPolicy> hcPolicyList = _lbMgr.getHealthCheckPolicies(lb.getId());
                Ip sourceIp = _networkModel.getPublicIpAddress(lb.getSourceIpAddressId()).getAddress();
                LbSslCert sslCert = _lbMgr.getLbSslCert(lb.getId());
                LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList, policyList, hcPolicyList, sourceIp, sslCert, lb.getLbProtocol());
                lbRules.add(loadBalancing);
            }
            return applyLBRules(elbVm, lbRules, network.getId());
        } else if (elbVm.getState() == State.Stopped || elbVm.getState() == State.Stopping) {
            s_logger.debug("ELB VM is in " + elbVm.getState() + ", so not sending apply LoadBalancing rules commands to the backend");
            return true;
        } else {
            s_logger.warn("Unable to apply loadbalancing rules, ELB VM is not in the right state " + elbVm.getState());
            throw new ResourceUnavailableException("Unable to apply loadbalancing rules, ELB VM is not in the right state", VirtualRouter.class, elbVm.getId());
        }
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        final Map<String, String> configs = _configDao.getConfiguration("AgentManager", params);
        _systemAcct = _accountService.getSystemAccount();
        _instance = configs.get("instance.name");
        if (_instance == null) {
            _instance = "VM";
        }
        _mgmtCidr = _configDao.getValue(Config.ManagementNetwork.key());

        _elasticLbVmRamSize = NumbersUtil.parseInt(configs.get(Config.ElasticLoadBalancerVmMemory.key()), DEFAULT_ELB_VM_RAMSIZE);
        _elasticLbvmCpuMHz = NumbersUtil.parseInt(configs.get(Config.ElasticLoadBalancerVmCpuMhz.key()), DEFAULT_ELB_VM_CPU_MHZ);
        _elasticLbvmNumCpu = NumbersUtil.parseInt(configs.get(Config.ElasticLoadBalancerVmNumVcpu.key()), 1);
        List<ServiceOfferingVO> offerings = _serviceOfferingDao.createSystemServiceOfferings("System Offering For Elastic LB VM",
                ServiceOffering.elbVmDefaultOffUniqueName, _elasticLbvmNumCpu, _elasticLbVmRamSize, _elasticLbvmCpuMHz, 0, 0, true, null,
                Storage.ProvisioningType.THIN, true, null, true, VirtualMachine.Type.ElasticLoadBalancerVm, true);
        // this can sometimes happen, if DB is manually or programmatically manipulated
        if (offerings == null || offerings.size() < 2) {
            String msg = "Data integrity problem : System Offering For Elastic LB VM has been removed?";
            s_logger.error(msg);
            throw new ConfigurationException(msg);
        }

        String enabled = _configDao.getValue(Config.ElasticLoadBalancerEnabled.key());
        _enabled = (enabled == null) ? false : Boolean.parseBoolean(enabled);
        s_logger.info("Elastic Load balancer enabled: " + _enabled);
        if (_enabled) {
            String traffType = _configDao.getValue(Config.ElasticLoadBalancerNetwork.key());
            if ("guest".equalsIgnoreCase(traffType)) {
                _frontendTrafficType = TrafficType.Guest;
            } else if ("public".equalsIgnoreCase(traffType)) {
                _frontendTrafficType = TrafficType.Public;
            } else
                throw new ConfigurationException("ELB: Traffic type for front end of load balancer has to be guest or public; found : " + traffType);
            s_logger.info("ELB: Elastic Load Balancer: will balance on " + traffType);
            int gcIntervalMinutes = NumbersUtil.parseInt(configs.get(Config.ElasticLoadBalancerVmGcInterval.key()), 5);
            if (gcIntervalMinutes < 5)
                gcIntervalMinutes = 5;
            s_logger.info("ELB: Elastic Load Balancer: scheduling GC to run every " + gcIntervalMinutes + " minutes");
            _gcThreadPool = Executors.newScheduledThreadPool(1, new NamedThreadFactory("ELBVM-GC"));
            _gcThreadPool.scheduleAtFixedRate(new CleanupThread(), gcIntervalMinutes, gcIntervalMinutes, TimeUnit.MINUTES);
            _itMgr.registerGuru(VirtualMachine.Type.ElasticLoadBalancerVm, this);
        }

        loadBalanceRuleHandler = new LoadBalanceRuleHandler(_instance, _systemAcct);

        return true;
    }

    private DomainRouterVO stop(DomainRouterVO elbVm, boolean forced) throws ConcurrentOperationException, ResourceUnavailableException {
        s_logger.debug("Stopping ELB vm " + elbVm);
        try {
            _itMgr.advanceStop(elbVm.getUuid(), forced);
            return _routerDao.findById(elbVm.getId());
        } catch (OperationTimedoutException e) {
            throw new CloudRuntimeException("Unable to stop " + elbVm, e);
        }
    }

    @Override
    public LoadBalancer handleCreateLoadBalancerRule(CreateLoadBalancerRuleCmd lb, Account account, long networkId) throws InsufficientAddressCapacityException,
            NetworkRuleConflictException {
        return loadBalanceRuleHandler.handleCreateLoadBalancerRule(lb, account, networkId);
    }

    void garbageCollectUnusedElbVms() {
        List<DomainRouterVO> unusedElbVms = _elbVmMapDao.listUnusedElbVms();
        if (unusedElbVms != null) {
            if (unusedElbVms.size() > 0) {
                s_logger.info("Found " + unusedElbVms.size() + " unused ELB vms");
            }
            Set<Long> currentGcCandidates = new HashSet<Long>();
            for (DomainRouterVO elbVm : unusedElbVms) {
                currentGcCandidates.add(elbVm.getId());
            }
            _gcCandidateElbVmIds.retainAll(currentGcCandidates);
            currentGcCandidates.removeAll(_gcCandidateElbVmIds);
            for (Long elbVmId : _gcCandidateElbVmIds) {
                DomainRouterVO elbVm = _routerDao.findById(elbVmId);
                boolean gceed = false;

                try {
                    s_logger.info("Attempting to stop ELB VM: " + elbVm);
                    stop(elbVm, true);
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
                        _itMgr.expunge(elbVm.getUuid());
                        _routerDao.remove(elbVm.getId());
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
    }

    public class CleanupThread extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
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
        loadBalanceRuleHandler.handleDeleteLoadBalancerRule(lb, userId, caller);
    }

    @Override
    public boolean finalizeVirtualMachineProfile(VirtualMachineProfile profile, DeployDestination dest, ReservationContext context) {

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
        buf.append(" template=domP type=" + SystemVmType);
        buf.append(" name=").append(profile.getHostName());
        NicProfile controlNic = null;
        String defaultDns1 = null;
        String defaultDns2 = null;

        for (NicProfile nic : profile.getNics()) {
            int deviceId = nic.getDeviceId();
            buf.append(" eth").append(deviceId).append("ip=").append(nic.getIPv4Address());
            buf.append(" eth").append(deviceId).append("mask=").append(nic.getIPv4Netmask());
            if (nic.isDefaultNic()) {
                buf.append(" gateway=").append(nic.getIPv4Gateway());
                defaultDns1 = nic.getIPv4Dns1();
                defaultDns2 = nic.getIPv4Dns2();
            }
            if (nic.getTrafficType() == TrafficType.Management) {
                buf.append(" localgw=").append(dest.getPod().getGateway());
            } else if (nic.getTrafficType() == TrafficType.Control) {
                //  control command is sent over management network in VMware
                if (dest.getHost().getHypervisorType() == HypervisorType.VMware) {
                    if (s_logger.isInfoEnabled()) {
                        s_logger.info("Check if we need to add management server explicit route to ELB vm. pod cidr: " + dest.getPod().getCidrAddress() + "/"
                                + dest.getPod().getCidrSize() + ", pod gateway: " + dest.getPod().getGateway() + ", management host: "
                                + ApiServiceConfiguration.ManagementHostIPAdr.value());
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
    public boolean finalizeDeployment(Commands cmds, VirtualMachineProfile profile, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException {
        DomainRouterVO elbVm = _routerDao.findById(profile.getVirtualMachine().getId());

        List<NicProfile> nics = profile.getNics();
        for (NicProfile nic : nics) {
            if (nic.getTrafficType() == TrafficType.Public) {
                elbVm.setPublicIpAddress(nic.getIPv4Address());
                elbVm.setPublicNetmask(nic.getIPv4Netmask());
                elbVm.setPublicMacAddress(nic.getMacAddress());
            } else if (nic.getTrafficType() == TrafficType.Control) {
                elbVm.setPrivateIpAddress(nic.getIPv4Address());
                elbVm.setPrivateMacAddress(nic.getMacAddress());
            }
        }
        _routerDao.update(elbVm.getId(), elbVm);

        finalizeCommandsOnStart(cmds, profile);
        return true;
    }

    @Override
    public boolean finalizeStart(VirtualMachineProfile profile, long hostId, Commands cmds, ReservationContext context) {
        CheckSshAnswer answer = (CheckSshAnswer)cmds.getAnswer("checkSsh");
        if (answer == null || !answer.getResult()) {
            s_logger.warn("Unable to ssh to the ELB VM: " + (answer != null ? answer.getDetails() : "No answer (answer for \"checkSsh\" was null)"));
            return false;
        }

        return true;
    }

    @Override
    public boolean finalizeCommandsOnStart(Commands cmds, VirtualMachineProfile profile) {
        DomainRouterVO elbVm = _routerDao.findById(profile.getVirtualMachine().getId());
        DataCenterVO dcVo = _dcDao.findById(elbVm.getDataCenterId());

        NicProfile controlNic = null;
        Long guestNetworkId = null;

        if (profile.getHypervisorType() == HypervisorType.VMware && dcVo.getNetworkType() == NetworkType.Basic) {
            // TODO this is a ugly to test hypervisor type here
            // for basic network mode, we will use the guest NIC for control NIC
            for (NicProfile nic : profile.getNics()) {
                if (nic.getTrafficType() == TrafficType.Guest && nic.getIPv4Address() != null) {
                    controlNic = nic;
                    guestNetworkId = nic.getNetworkId();
                }
            }
        } else {
            for (NicProfile nic : profile.getNics()) {
                if (nic.getTrafficType() == TrafficType.Control && nic.getIPv4Address() != null) {
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

        cmds.addCommand("checkSsh", new CheckSshCommand(profile.getInstanceName(), controlNic.getIPv4Address(), 3922));

        // Re-apply load balancing rules
        List<LoadBalancerVO> lbs = _elbVmMapDao.listLbsForElbVm(elbVm.getId());
        List<LoadBalancingRule> lbRules = new ArrayList<LoadBalancingRule>();
        for (LoadBalancerVO lb : lbs) {
            List<LbDestination> dstList = _lbMgr.getExistingDestinations(lb.getId());
            List<LbStickinessPolicy> policyList = _lbMgr.getStickinessPolicies(lb.getId());
            List<LbHealthCheckPolicy> hcPolicyList = _lbMgr.getHealthCheckPolicies(lb.getId());
            Ip sourceIp = _networkModel.getPublicIpAddress(lb.getSourceIpAddressId()).getAddress();
            LoadBalancingRule loadBalancing = new LoadBalancingRule(lb, dstList, policyList, hcPolicyList, sourceIp);
            lbRules.add(loadBalancing);
        }

        s_logger.debug("Found " + lbRules.size() + " load balancing rule(s) to apply as a part of ELB vm " + elbVm + " start.");
        if (!lbRules.isEmpty()) {
            createApplyLoadBalancingRulesCommands(lbRules, elbVm, cmds, guestNetworkId);
        }

        return true;
    }

    @Override
    public void finalizeStop(VirtualMachineProfile profile, Answer answer) {
        if (answer != null) {
            DomainRouterVO elbVm = _routerDao.findById(profile.getVirtualMachine().getId());
            processStopOrRebootAnswer(elbVm, answer);
        }
    }

    @SuppressWarnings("unused")
    public void processStopOrRebootAnswer(final DomainRouterVO elbVm, Answer answer) {
        //TODO: process network usage stats
    }

    @Override
    public void finalizeExpunge(VirtualMachine vm) {
        // no-op
    }

    @Override
    public void prepareStop(VirtualMachineProfile profile) {
    }

}
