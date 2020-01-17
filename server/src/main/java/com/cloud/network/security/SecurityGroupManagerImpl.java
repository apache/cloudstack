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
package com.cloud.network.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.acl.SecurityChecker.AccessType;
import org.apache.cloudstack.api.command.user.securitygroup.AuthorizeSecurityGroupEgressCmd;
import org.apache.cloudstack.api.command.user.securitygroup.AuthorizeSecurityGroupIngressCmd;
import org.apache.cloudstack.api.command.user.securitygroup.CreateSecurityGroupCmd;
import org.apache.cloudstack.api.command.user.securitygroup.DeleteSecurityGroupCmd;
import org.apache.cloudstack.api.command.user.securitygroup.RevokeSecurityGroupEgressCmd;
import org.apache.cloudstack.api.command.user.securitygroup.RevokeSecurityGroupIngressCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.NetworkRulesSystemVmCommand;
import com.cloud.agent.api.NetworkRulesVmSecondaryIpCommand;
import com.cloud.agent.api.SecurityGroupRulesCmd;
import com.cloud.agent.api.SecurityGroupRulesCmd.IpPortAndProto;
import com.cloud.agent.manager.Commands;
import com.cloud.api.query.dao.SecurityGroupJoinDao;
import com.cloud.configuration.Config;
import com.cloud.domain.dao.DomainDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventUtils;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.security.SecurityGroupWork.Step;
import com.cloud.network.security.SecurityRule.SecurityRuleType;
import com.cloud.network.security.dao.SecurityGroupDao;
import com.cloud.network.security.dao.SecurityGroupRuleDao;
import com.cloud.network.security.dao.SecurityGroupRulesDao;
import com.cloud.network.security.dao.SecurityGroupVMMapDao;
import com.cloud.network.security.dao.SecurityGroupWorkDao;
import com.cloud.network.security.dao.VmRulesetLogDao;
import com.cloud.projects.ProjectManager;
import com.cloud.tags.dao.ResourceTagDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.dao.AccountDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.db.TransactionCallbackNoReturn;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.db.TransactionStatus;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.fsm.StateListener;
import com.cloud.utils.fsm.StateMachine2;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Event;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.NicSecondaryIpDao;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

public class SecurityGroupManagerImpl extends ManagerBase implements SecurityGroupManager, SecurityGroupService, StateListener<State, VirtualMachine.Event, VirtualMachine> {
    public static final Logger s_logger = Logger.getLogger(SecurityGroupManagerImpl.class);

    @Inject
    SecurityGroupDao _securityGroupDao;
    @Inject
    SecurityGroupJoinDao _securityGroupJoinDao;
    @Inject
    SecurityGroupRuleDao _securityGroupRuleDao;
    @Inject
    SecurityGroupVMMapDao _securityGroupVMMapDao;
    @Inject
    SecurityGroupRulesDao _securityGroupRulesDao;
    @Inject
    UserVmDao _userVMDao;
    @Inject
    AccountDao _accountDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    SecurityGroupWorkDao _workDao;
    @Inject
    VmRulesetLogDao _rulesetLogDao;
    @Inject
    DomainDao _domainDao;
    @Inject
    AgentManager _agentMgr;
    @Inject
    VirtualMachineManager _itMgr;
    @Inject
    UserVmManager _userVmMgr;
    @Inject
    VMInstanceDao _vmDao;
    @Inject
    NetworkOrchestrationService _networkMgr;
    @Inject
    NetworkModel _networkModel;
    @Inject
    AccountManager _accountMgr;
    @Inject
    DomainManager _domainMgr;
    @Inject
    ProjectManager _projectMgr;
    @Inject
    ResourceTagDao _resourceTagDao;
    @Inject
    NicDao _nicDao;
    @Inject
    NicSecondaryIpDao _nicSecIpDao;

    ScheduledExecutorService _executorPool;
    ScheduledExecutorService _cleanupExecutor;

    protected long _serverId;

    private int _timeBetweenCleanups = TIME_BETWEEN_CLEANUPS; // seconds
    protected int _numWorkerThreads = WORKER_THREAD_COUNT;
    private int _globalWorkLockTimeout = 300; // 5 minutes

    private final GlobalLock _workLock = GlobalLock.getInternLock("SecurityGroupWork");

    SecurityGroupListener _answerListener;

    private final class SecurityGroupVOComparator implements Comparator<SecurityGroupVO> {
        @Override
        public int compare(SecurityGroupVO o1, SecurityGroupVO o2) {
            return o1.getId() == o2.getId() ? 0 : o1.getId() < o2.getId() ? -1 : 1;
        }
    }

    public class WorkerThread extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                work();
            } catch (Throwable th) {
                s_logger.error("Problem with SG work", th);
            }
        }
    }

    public class CleanupThread extends ManagedContextRunnable {
        @Override
        protected void runInContext() {
            try {
                cleanupFinishedWork();
                cleanupUnfinishedWork();
                //processScheduledWork();
            } catch (Throwable th) {
                s_logger.error("Problem with SG Cleanup", th);
            }
        }
    }

    public static class PortAndProto implements Comparable<PortAndProto> {
        String proto;
        int startPort;
        int endPort;

        public PortAndProto(String proto, int startPort, int endPort) {
            this.proto = proto;
            this.startPort = startPort;
            this.endPort = endPort;
        }

        public String getProto() {
            return proto;
        }

        public int getStartPort() {
            return startPort;
        }

        public int getEndPort() {
            return endPort;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + endPort;
            result = prime * result + ((proto == null) ? 0 : proto.hashCode());
            result = prime * result + startPort;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            PortAndProto other = (PortAndProto)obj;
            if (endPort != other.endPort) {
                return false;
            }
            if (proto == null) {
                if (other.proto != null) {
                    return false;
                }
            } else if (!proto.equals(other.proto)) {
                return false;
            }
            if (startPort != other.startPort) {
                return false;
            }
            return true;
        }

        @Override
        public int compareTo(PortAndProto obj) {
            if (this == obj) {
                return 0;
            }
            if (obj == null) {
                return 1;
            }
            if (proto == null) {
                if (obj.proto != null) {
                    return -1;
                } else {
                    return 0;
                }
            }
            if (!obj.proto.equalsIgnoreCase(proto)) {
                return proto.compareTo(obj.proto);
            }
            if (startPort < obj.startPort) {
                return -1;
            } else if (startPort > obj.startPort) {
                return 1;
            }

            if (endPort < obj.endPort) {
                return -1;
            } else if (endPort > obj.endPort) {
                return 1;
            }

            return 0;
        }

    }

    public static class CidrComparator implements Comparator<String> {

        @Override
        public int compare(String cidr1, String cidr2) {
            // parse both to find significance first (low number of bits is high)
            // if equal then just do a string compare
            if (significance(cidr1) == significance(cidr2)) {
                return cidr1.compareTo(cidr2);
            } else {
                return significance(cidr2) - significance(cidr1);
            }
        }

        private int significance(String cidr) {
            return Integer.parseInt(cidr.substring(cidr.indexOf('/') + 1));
        }

    }

    protected Map<PortAndProto, Set<String>> generateRulesForVM(Long userVmId, SecurityRuleType type) {

        Map<PortAndProto, Set<String>> allowed = new TreeMap<PortAndProto, Set<String>>();

        List<SecurityGroupVMMapVO> groupsForVm = _securityGroupVMMapDao.listByInstanceId(userVmId);
        for (SecurityGroupVMMapVO mapVO : groupsForVm) {
            List<SecurityGroupRuleVO> rules = _securityGroupRuleDao.listBySecurityGroupId(mapVO.getSecurityGroupId(), type);
            for (SecurityGroupRuleVO rule : rules) {
                PortAndProto portAndProto = new PortAndProto(rule.getProtocol(), rule.getStartPort(), rule.getEndPort());
                Set<String> cidrs = allowed.get(portAndProto);
                if (cidrs == null) {
                    cidrs = new TreeSet<String>(new CidrComparator());
                }
                if (rule.getAllowedNetworkId() != null) {
                    List<SecurityGroupVMMapVO> allowedInstances = _securityGroupVMMapDao.listBySecurityGroup(rule.getAllowedNetworkId(), State.Running);
                    for (SecurityGroupVMMapVO ngmapVO : allowedInstances) {
                        Nic defaultNic = _networkModel.getDefaultNic(ngmapVO.getInstanceId());
                        if (defaultNic != null) {
                            String cidr = defaultNic.getIPv4Address();
                            cidr = cidr + "/32";
                            cidrs.add(cidr);
                        }
                    }
                } else if (rule.getAllowedSourceIpCidr() != null) {
                    cidrs.add(rule.getAllowedSourceIpCidr());
                }
                if (cidrs.size() > 0) {
                    allowed.put(portAndProto, cidrs);
                }
            }
        }

        return allowed;
    }

    protected String generateRulesetSignature(Map<PortAndProto, Set<String>> ingress, Map<PortAndProto, Set<String>> egress) {
        String ruleset = ingress.toString();
        ruleset = ruleset.concat(egress.toString());
        return DigestUtils.md5Hex(ruleset);
    }

    public void handleVmStarted(VMInstanceVO vm) {
        if (vm.getType() != VirtualMachine.Type.User || !isVmSecurityGroupEnabled(vm.getId())) {
            return;
        }
        List<Long> affectedVms = getAffectedVmsForVmStart(vm);
        scheduleRulesetUpdateToHosts(affectedVms, true, null);
    }

    @DB
    public void scheduleRulesetUpdateToHosts(final List<Long> affectedVms, final boolean updateSeqno, Long delayMs) {
        if (affectedVms.size() == 0) {
            return;
        }

        if (delayMs == null) {
            delayMs = new Long(100l);
        }

        Collections.sort(affectedVms);
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Security Group Mgr: scheduling ruleset updates for " + affectedVms.size() + " vms");
        }
        boolean locked = _workLock.lock(_globalWorkLockTimeout);
        if (!locked) {
            s_logger.warn("Security Group Mgr: failed to acquire global work lock");
            return;
        }

        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Security Group Mgr: acquired global work lock");
        }

        try {
            Transaction.execute(new TransactionCallbackNoReturn() {
                @Override
                public void doInTransactionWithoutResult(TransactionStatus status) {
                    for (Long vmId : affectedVms) {
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("Security Group Mgr: scheduling ruleset update for " + vmId);
                        }
                        VmRulesetLogVO log = null;
                        SecurityGroupWorkVO work = null;

                        log = _rulesetLogDao.findByVmId(vmId);
                        if (log == null) {
                            log = new VmRulesetLogVO(vmId);
                            log = _rulesetLogDao.persist(log);
                        }

                        if (log != null && updateSeqno) {
                            log.incrLogsequence();
                            _rulesetLogDao.update(log.getId(), log);
                        }
                        work = _workDao.findByVmIdStep(vmId, Step.Scheduled);
                        if (work == null) {
                            work = new SecurityGroupWorkVO(vmId, null, null, SecurityGroupWork.Step.Scheduled, null);
                            work = _workDao.persist(work);
                            if (s_logger.isTraceEnabled()) {
                                s_logger.trace("Security Group Mgr: created new work item for " + vmId + "; id = " + work.getId());
                            }
                        }

                        work.setLogsequenceNumber(log.getLogsequence());
                        _workDao.update(work.getId(), work);
                    }
                }
            });
            for (Long vmId : affectedVms) {
                _executorPool.schedule(new WorkerThread(), delayMs, TimeUnit.MILLISECONDS);
            }
        } finally {
            _workLock.unlock();
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Security Group Mgr: released global work lock");
            }
        }
    }

    protected List<Long> getAffectedVmsForVmStart(VMInstanceVO vm) {
        List<Long> affectedVms = new ArrayList<Long>();
        affectedVms.add(vm.getId());
        List<SecurityGroupVMMapVO> groupsForVm = _securityGroupVMMapDao.listByInstanceId(vm.getId());
        // For each group, find the security rules that allow the group
        for (SecurityGroupVMMapVO mapVO : groupsForVm) {// FIXME: use custom sql in the dao
            //Add usage events for security group assign
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_SECURITY_GROUP_ASSIGN, vm.getAccountId(), vm.getDataCenterId(), vm.getId(), mapVO.getSecurityGroupId(), vm
                    .getClass().getName(), vm.getUuid());

            List<SecurityGroupRuleVO> allowingRules = _securityGroupRuleDao.listByAllowedSecurityGroupId(mapVO.getSecurityGroupId());
            // For each security rule that allows a group that the vm belongs to, find the group it belongs to
            affectedVms.addAll(getAffectedVmsForSecurityRules(allowingRules));
        }
        return affectedVms;
    }

    protected List<Long> getAffectedVmsForVmStop(VMInstanceVO vm) {
        List<Long> affectedVms = new ArrayList<Long>();
        List<SecurityGroupVMMapVO> groupsForVm = _securityGroupVMMapDao.listByInstanceId(vm.getId());
        // For each group, find the security rules rules that allow the group
        for (SecurityGroupVMMapVO mapVO : groupsForVm) {// FIXME: use custom sql in the dao
            //Add usage events for security group remove
            UsageEventUtils.publishUsageEvent(EventTypes.EVENT_SECURITY_GROUP_REMOVE, vm.getAccountId(), vm.getDataCenterId(), vm.getId(), mapVO.getSecurityGroupId(), vm
                    .getClass().getName(), vm.getUuid());

            List<SecurityGroupRuleVO> allowingRules = _securityGroupRuleDao.listByAllowedSecurityGroupId(mapVO.getSecurityGroupId());
            // For each security rule that allows a group that the vm belongs to, find the group it belongs to
            affectedVms.addAll(getAffectedVmsForSecurityRules(allowingRules));
        }
        return affectedVms;
    }

    protected List<Long> getAffectedVmsForSecurityRules(List<SecurityGroupRuleVO> allowingRules) {
        Set<Long> distinctGroups = new HashSet<Long>();
        List<Long> affectedVms = new ArrayList<Long>();

        for (SecurityGroupRuleVO allowingRule : allowingRules) {
            distinctGroups.add(allowingRule.getSecurityGroupId());
        }
        for (Long groupId : distinctGroups) {
            // allVmUpdates.putAll(generateRulesetForGroupMembers(groupId));
            affectedVms.addAll(_securityGroupVMMapDao.listVmIdsBySecurityGroup(groupId));
        }
        return affectedVms;
    }

    protected SecurityGroupRulesCmd generateRulesetCmd(String vmName, String guestIp, String guestIp6, String guestMac, Long vmId, String signature, long seqnum,
            Map<PortAndProto, Set<String>> ingressRules, Map<PortAndProto, Set<String>> egressRules, List<String> secIps) {
        List<IpPortAndProto> ingressResult = new ArrayList<IpPortAndProto>();
        List<IpPortAndProto> egressResult = new ArrayList<IpPortAndProto>();
        for (PortAndProto pAp : ingressRules.keySet()) {
            Set<String> cidrs = ingressRules.get(pAp);
            if (cidrs.size() > 0) {
                IpPortAndProto ipPortAndProto = new SecurityGroupRulesCmd.IpPortAndProto(pAp.getProto(), pAp.getStartPort(), pAp.getEndPort(), cidrs.toArray(new String[cidrs
                                                                                                                                                                        .size()]));
                ingressResult.add(ipPortAndProto);
            }
        }
        for (PortAndProto pAp : egressRules.keySet()) {
            Set<String> cidrs = egressRules.get(pAp);
            if (cidrs.size() > 0) {
                IpPortAndProto ipPortAndProto = new SecurityGroupRulesCmd.IpPortAndProto(pAp.getProto(), pAp.getStartPort(), pAp.getEndPort(), cidrs.toArray(new String[cidrs
                                                                                                                                                                        .size()]));
                egressResult.add(ipPortAndProto);
            }
        }
        return new SecurityGroupRulesCmd(guestIp, guestIp6, guestMac, vmName, vmId, signature, seqnum, ingressResult.toArray(new IpPortAndProto[ingressResult.size()]),
                egressResult.toArray(new IpPortAndProto[egressResult.size()]), secIps);
    }

    protected void handleVmStopped(VMInstanceVO vm) {
        if (vm.getType() != VirtualMachine.Type.User || !isVmSecurityGroupEnabled(vm.getId())) {
            return;
        }
        List<Long> affectedVms = getAffectedVmsForVmStop(vm);
        scheduleRulesetUpdateToHosts(affectedVms, true, null);
    }

    protected void handleVmMigrated(VMInstanceVO vm) {
        if (!isVmSecurityGroupEnabled(vm.getId())) {
            return;
        }
        if (vm.getType() != VirtualMachine.Type.User) {
            Commands cmds = null;
            NetworkRulesSystemVmCommand nrc = new NetworkRulesSystemVmCommand(vm.getInstanceName(), vm.getType());
            cmds = new Commands(nrc);
            try {
                _agentMgr.send(vm.getHostId(), cmds);
            } catch (AgentUnavailableException e) {
                s_logger.debug(e.toString());
            } catch (OperationTimedoutException e) {
                s_logger.debug(e.toString());
            }

        } else {
            List<Long> affectedVms = new ArrayList<Long>();
            affectedVms.add(vm.getId());
            scheduleRulesetUpdateToHosts(affectedVms, true, null);
        }
    }

    @Override
    @DB
    @SuppressWarnings("rawtypes")
    @ActionEvent(eventType = EventTypes.EVENT_SECURITY_GROUP_AUTHORIZE_EGRESS, eventDescription = "Adding Egress Rule ", async = true)
    public List<SecurityGroupRuleVO> authorizeSecurityGroupEgress(AuthorizeSecurityGroupEgressCmd cmd) {
        Long securityGroupId = cmd.getSecurityGroupId();
        String protocol = cmd.getProtocol();
        Integer startPort = cmd.getStartPort();
        Integer endPort = cmd.getEndPort();
        Integer icmpType = cmd.getIcmpType();
        Integer icmpCode = cmd.getIcmpCode();
        List<String> cidrList = cmd.getCidrList();
        Map groupList = cmd.getUserSecurityGroupList();
        return authorizeSecurityGroupRule(securityGroupId, protocol, startPort, endPort, icmpType, icmpCode, cidrList, groupList, SecurityRuleType.EgressRule);
    }

    @Override
    @DB
    @SuppressWarnings("rawtypes")
    @ActionEvent(eventType = EventTypes.EVENT_SECURITY_GROUP_AUTHORIZE_INGRESS, eventDescription = "Adding Ingress Rule ", async = true)
    public List<SecurityGroupRuleVO> authorizeSecurityGroupIngress(AuthorizeSecurityGroupIngressCmd cmd) {
        Long securityGroupId = cmd.getSecurityGroupId();
        String protocol = cmd.getProtocol();
        Integer startPort = cmd.getStartPort();
        Integer endPort = cmd.getEndPort();
        Integer icmpType = cmd.getIcmpType();
        Integer icmpCode = cmd.getIcmpCode();
        List<String> cidrList = cmd.getCidrList();
        Map groupList = cmd.getUserSecurityGroupList();
        return authorizeSecurityGroupRule(securityGroupId, protocol, startPort, endPort, icmpType, icmpCode, cidrList, groupList, SecurityRuleType.IngressRule);
    }

    private List<SecurityGroupRuleVO> authorizeSecurityGroupRule(final Long securityGroupId, String protocol, Integer startPort, Integer endPort, Integer icmpType,
            Integer icmpCode, final List<String> cidrList, Map groupList, final SecurityRuleType ruleType) {
        Integer startPortOrType = null;
        Integer endPortOrCode = null;

        // Validate parameters
        SecurityGroup securityGroup = _securityGroupDao.findById(securityGroupId);
        if (securityGroup == null) {
            throw new InvalidParameterValueException("Unable to find security group by id " + securityGroupId);
        }

        if (cidrList == null && groupList == null) {
            throw new InvalidParameterValueException("At least one cidr or at least one security group needs to be specified");
        }

        Account caller = CallContext.current().getCallingAccount();
        Account owner = _accountMgr.getAccount(securityGroup.getAccountId());

        if (owner == null) {
            throw new InvalidParameterValueException("Unable to find security group owner by id=" + securityGroup.getAccountId());
        }

        // Verify permissions
        _accountMgr.checkAccess(caller, null, true, securityGroup);
        Long domainId = owner.getDomainId();

        if (protocol == null) {
            protocol = NetUtils.ALL_PROTO;
        }

        if (cidrList != null) {
            for (String cidr : cidrList) {
                if (!NetUtils.isValidIp4Cidr(cidr) && !NetUtils.isValidIp6Cidr(cidr)) {
                    throw new InvalidParameterValueException("Invalid cidr " + cidr);
                }
            }
        }

        //Validate Protocol
        protocol = protocol.trim().toLowerCase();
        //Check if protocol is a number
        if(StringUtils.isNumeric(protocol)){
            int protoNumber = Integer.parseInt(protocol);
            // Deal with ICMP(protocol number 1) specially because it need to be paired with icmp type and code
            if (protoNumber == 1) {
                protocol = "icmp";
                icmpCode = -1;
                icmpType = -1;
            } else if(protoNumber < 0 || protoNumber > 255){
                throw new InvalidParameterValueException("Invalid protocol number: " + protoNumber);
            }
        } else {
            //Protocol is not number
            //Check for valid protocol strings
            if (!NetUtils.isValidSecurityGroupProto(protocol)) {
                throw new InvalidParameterValueException("Invalid protocol " + protocol);
            }
        }
        if (protocol.equals(NetUtils.ICMP_PROTO)) {
            if ((icmpType == null) || (icmpCode == null)) {
                throw new InvalidParameterValueException("Invalid ICMP type/code specified, icmpType = " + icmpType + ", icmpCode = " + icmpCode);
            }
            if (icmpType == -1 && icmpCode != -1) {
                throw new InvalidParameterValueException("Invalid icmp code");
            }
            if (icmpType != -1 && icmpCode == -1) {
                throw new InvalidParameterValueException("Invalid icmp code: need non-negative icmp code ");
            }
            if (icmpCode > 255 || icmpType > 255 || icmpCode < -1 || icmpType < -1) {
                throw new InvalidParameterValueException("Invalid icmp type/code ");
            }
            startPortOrType = icmpType;
            endPortOrCode = icmpCode;
        } else if (protocol.equals(NetUtils.ALL_PROTO)) {
            if ((startPort != null) || (endPort != null)) {
                throw new InvalidParameterValueException("Cannot specify startPort or endPort without specifying protocol");
            }
            startPortOrType = 0;
            endPortOrCode = 0;
        } else if (protocol.equals(NetUtils.TCP_PROTO) || protocol.equals(NetUtils.UDP_PROTO)) {
            if ((startPort == null) || (endPort == null)) {
                throw new InvalidParameterValueException("Invalid port range specified, startPort = " + startPort + ", endPort = " + endPort);
            }
            if (startPort == 0 && endPort == 0) {
                endPort = 65535;
            }
            if (startPort > endPort) {
                throw new InvalidParameterValueException("Invalid port range " + startPort + ":" + endPort);
            }
            if (startPort > 65535 || endPort > 65535 || startPort < -1 || endPort < -1) {
                throw new InvalidParameterValueException("Invalid port numbers " + startPort + ":" + endPort);
            }

            if (startPort < 0 || endPort < 0) {
                throw new InvalidParameterValueException("Invalid port range " + startPort + ":" + endPort);
            }
            startPortOrType = startPort;
            endPortOrCode = endPort;
        } else {
            // in 4.6, the start port and end port are ignored in definition of ProtocolAclRule
            // see core/src/com/cloud/agent/resource/virtualnetwork/facade/SetNetworkAclConfigItem.java
            startPortOrType = 0;
            endPortOrCode = 0;
        }

        List<SecurityGroupVO> authorizedGroups = new ArrayList<SecurityGroupVO>();
        if (groupList != null) {
            Collection userGroupCollection = groupList.values();
            Iterator iter = userGroupCollection.iterator();
            while (iter.hasNext()) {
                HashMap userGroup = (HashMap)iter.next();
                String group = (String)userGroup.get("group");
                String authorizedAccountName = (String)userGroup.get("account");

                if ((group == null) || (authorizedAccountName == null)) {
                    throw new InvalidParameterValueException(
                            "Invalid user group specified, fields 'group' and 'account' cannot be null, please specify groups in the form:  userGroupList[0].group=XXX&userGroupList[0].account=YYY");
                }

                Account authorizedAccount = _accountDao.findActiveAccount(authorizedAccountName, domainId);
                if (authorizedAccount == null) {
                    throw new InvalidParameterValueException("Nonexistent account: " + authorizedAccountName + " when trying to authorize security group rule  for "
                            + securityGroupId + ":" + protocol + ":" + startPortOrType + ":" + endPortOrCode);
                }

                SecurityGroupVO groupVO = _securityGroupDao.findByAccountAndName(authorizedAccount.getId(), group);
                if (groupVO == null) {
                    throw new InvalidParameterValueException("Nonexistent group " + group + " for account " + authorizedAccountName + "/" + domainId
                            + " is given, unable to authorize security group rule.");
                }

                // Check permissions
                if (domainId != groupVO.getDomainId()) {
                    throw new PermissionDeniedException("Can't add security group id=" + groupVO.getDomainId() + " as it belongs to different domain");
                }

                authorizedGroups.add(groupVO);
            }
        }

        final Set<SecurityGroupVO> authorizedGroups2 = new TreeSet<SecurityGroupVO>(new SecurityGroupVOComparator());

        authorizedGroups2.addAll(authorizedGroups); // Ensure we don't re-lock the same row

        final Integer startPortOrTypeFinal = startPortOrType;
        final Integer endPortOrCodeFinal = endPortOrCode;
        final String protocolFinal = protocol;
        List<SecurityGroupRuleVO> newRules = Transaction.execute(new TransactionCallback<List<SecurityGroupRuleVO>>() {
            @Override
            public List<SecurityGroupRuleVO> doInTransaction(TransactionStatus status) {
                // Prevents other threads/management servers from creating duplicate security rules
                SecurityGroup securityGroup = _securityGroupDao.acquireInLockTable(securityGroupId);
                if (securityGroup == null) {
                    s_logger.warn("Could not acquire lock on network security group: id= " + securityGroupId);
                    return null;
                }
                List<SecurityGroupRuleVO> newRules = new ArrayList<SecurityGroupRuleVO>();
                try {
                    for (final SecurityGroupVO ngVO : authorizedGroups2) {
                        final Long ngId = ngVO.getId();
                        // Don't delete the referenced group from under us
                        if (ngVO.getId() != securityGroup.getId()) {
                            final SecurityGroupVO tmpGrp = _securityGroupDao.lockRow(ngId, false);
                            if (tmpGrp == null) {
                                s_logger.warn("Failed to acquire lock on security group: " + ngId);
                                throw new CloudRuntimeException("Failed to acquire lock on security group: " + ngId);
                            }
                        }
                        SecurityGroupRuleVO securityGroupRule = _securityGroupRuleDao.findByProtoPortsAndAllowedGroupId(securityGroup.getId(), protocolFinal, startPortOrTypeFinal,
                                endPortOrCodeFinal, ngVO.getId());
                        if ((securityGroupRule != null) && (securityGroupRule.getRuleType() == ruleType)) {
                            continue; // rule already exists.
                        }
                        securityGroupRule = new SecurityGroupRuleVO(ruleType, securityGroup.getId(), startPortOrTypeFinal, endPortOrCodeFinal, protocolFinal, ngVO.getId());
                        securityGroupRule = _securityGroupRuleDao.persist(securityGroupRule);
                        newRules.add(securityGroupRule);
                    }
                    if (cidrList != null) {
                        for (String cidr : cidrList) {
                            SecurityGroupRuleVO securityGroupRule = _securityGroupRuleDao.findByProtoPortsAndCidr(securityGroup.getId(), protocolFinal, startPortOrTypeFinal,
                                    endPortOrCodeFinal, cidr);
                            if ((securityGroupRule != null) && (securityGroupRule.getRuleType() == ruleType)) {
                                continue;
                            }
                            securityGroupRule = new SecurityGroupRuleVO(ruleType, securityGroup.getId(), startPortOrTypeFinal, endPortOrCodeFinal, protocolFinal, cidr);
                            securityGroupRule = _securityGroupRuleDao.persist(securityGroupRule);
                            newRules.add(securityGroupRule);
                        }
                    }
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Added " + newRules.size() + " rules to security group " + securityGroup.getName());
                    }
                    return newRules;
                } catch (Exception e) {
                    s_logger.warn("Exception caught when adding security group rules ", e);
                    throw new CloudRuntimeException("Exception caught when adding security group rules", e);
                } finally {
                    if (securityGroup != null) {
                        _securityGroupDao.releaseFromLockTable(securityGroup.getId());
                    }
                }
            }
        });

        try {
            final ArrayList<Long> affectedVms = new ArrayList<Long>();
            affectedVms.addAll(_securityGroupVMMapDao.listVmIdsBySecurityGroup(securityGroup.getId()));
            scheduleRulesetUpdateToHosts(affectedVms, true, null);
        } catch (Exception e) {
            s_logger.debug("can't update rules on host, ignore", e);
        }

        return newRules;
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_SECURITY_GROUP_REVOKE_EGRESS, eventDescription = "Revoking Egress Rule ", async = true)
    public boolean revokeSecurityGroupEgress(RevokeSecurityGroupEgressCmd cmd) {
        Long id = cmd.getId();
        return revokeSecurityGroupRule(id, SecurityRuleType.EgressRule);
    }

    @Override
    @DB
    @ActionEvent(eventType = EventTypes.EVENT_SECURITY_GROUP_REVOKE_INGRESS, eventDescription = "Revoking Ingress Rule ", async = true)
    public boolean revokeSecurityGroupIngress(RevokeSecurityGroupIngressCmd cmd) {

        Long id = cmd.getId();
        return revokeSecurityGroupRule(id, SecurityRuleType.IngressRule);
    }

    private boolean revokeSecurityGroupRule(final Long id, SecurityRuleType type) {
        // input validation
        Account caller = CallContext.current().getCallingAccount();

        final SecurityGroupRuleVO rule = _securityGroupRuleDao.findById(id);
        if (rule == null) {
            s_logger.debug("Unable to find security rule with id " + id);
            throw new InvalidParameterValueException("Unable to find security rule with id " + id);
        }

        // check type
        if (type != rule.getRuleType()) {
            s_logger.debug("Mismatch in rule type for security rule with id " + id);
            throw new InvalidParameterValueException("Mismatch in rule type for security rule with id " + id);
        }

        // Check permissions
        SecurityGroup securityGroup = _securityGroupDao.findById(rule.getSecurityGroupId());
        _accountMgr.checkAccess(caller, AccessType.OperateEntry, true, securityGroup);

        long securityGroupId = rule.getSecurityGroupId();
        Boolean result = Transaction.execute(new TransactionCallback<Boolean>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) {
                SecurityGroupVO groupHandle = null;

                try {
                    // acquire lock on parent group (preserving this logic)
                    groupHandle = _securityGroupDao.acquireInLockTable(rule.getSecurityGroupId());
                    if (groupHandle == null) {
                        s_logger.warn("Could not acquire lock on security group id: " + rule.getSecurityGroupId());
                        return false;
                    }

                    _securityGroupRuleDao.remove(id);
                    s_logger.debug("revokeSecurityGroupRule succeeded for security rule id: " + id);

                    return true;
                } catch (Exception e) {
                    s_logger.warn("Exception caught when deleting security rules ", e);
                    throw new CloudRuntimeException("Exception caught when deleting security rules", e);
                } finally {
                    if (groupHandle != null) {
                        _securityGroupDao.releaseFromLockTable(groupHandle.getId());
                    }
                }
            }
        });

        try {
            final ArrayList<Long> affectedVms = new ArrayList<Long>();
            affectedVms.addAll(_securityGroupVMMapDao.listVmIdsBySecurityGroup(securityGroupId));
            scheduleRulesetUpdateToHosts(affectedVms, true, null);
        } catch (Exception e) {
            s_logger.debug("Can't update rules for host, ignore", e);
        }

        return result;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SECURITY_GROUP_CREATE, eventDescription = "creating security group")
    public SecurityGroupVO createSecurityGroup(CreateSecurityGroupCmd cmd) throws PermissionDeniedException, InvalidParameterValueException {
        String name = cmd.getSecurityGroupName();
        Account caller = CallContext.current().getCallingAccount();
        Account owner = _accountMgr.finalizeOwner(caller, cmd.getAccountName(), cmd.getDomainId(), cmd.getProjectId());

        if (_securityGroupDao.isNameInUse(owner.getId(), owner.getDomainId(), cmd.getSecurityGroupName())) {
            throw new InvalidParameterValueException("Unable to create security group, a group with name " + name + " already exists.");
        }

        return createSecurityGroup(cmd.getSecurityGroupName(), cmd.getDescription(), owner.getDomainId(), owner.getAccountId(), owner.getAccountName());
    }

    @Override
    public SecurityGroupVO createSecurityGroup(String name, String description, Long domainId, Long accountId, String accountName) {
        SecurityGroupVO group = _securityGroupDao.findByAccountAndName(accountId, name);
        if (group == null) {
            group = new SecurityGroupVO(name, description, domainId, accountId);
            group = _securityGroupDao.persist(group);
            s_logger.debug("Created security group " + group + " for account id=" + accountId);
        } else {
            s_logger.debug("Returning existing security group " + group + " for account id=" + accountId);
        }

        return group;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {

        Map<String, String> configs = _configDao.getConfiguration("Network", params);
        _numWorkerThreads = NumbersUtil.parseInt(configs.get(Config.SecurityGroupWorkerThreads.key()), WORKER_THREAD_COUNT);
        _timeBetweenCleanups = NumbersUtil.parseInt(configs.get(Config.SecurityGroupWorkCleanupInterval.key()), TIME_BETWEEN_CLEANUPS);
        _globalWorkLockTimeout = NumbersUtil.parseInt(configs.get(Config.SecurityGroupWorkGlobalLockTimeout.key()), 300);
        /* register state listener, no matter security group is enabled or not */
        VirtualMachine.State.getStateMachine().registerListener(this);

        _answerListener = new SecurityGroupListener(this, _agentMgr, _workDao);
        _agentMgr.registerForHostEvents(_answerListener, true, true, true);

        _serverId = ManagementServerNode.getManagementServerId();

        s_logger.info("SecurityGroupManager: num worker threads=" + _numWorkerThreads + ", time between cleanups=" + _timeBetweenCleanups + " global lock timeout="
                + _globalWorkLockTimeout);
        createThreadPools();

        return true;
    }

    protected void createThreadPools() {
        _executorPool = Executors.newScheduledThreadPool(_numWorkerThreads, new NamedThreadFactory("NWGRP"));
        _cleanupExecutor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("NWGRP-Cleanup"));
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public boolean start() {
        _cleanupExecutor.scheduleAtFixedRate(new CleanupThread(), _timeBetweenCleanups, _timeBetweenCleanups, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public SecurityGroupVO createDefaultSecurityGroup(Long accountId) {
        SecurityGroupVO groupVO = _securityGroupDao.findByAccountAndName(accountId, SecurityGroupManager.DEFAULT_GROUP_NAME);
        if (groupVO == null) {
            Account accVO = _accountDao.findById(accountId);
            if (accVO != null) {
                return createSecurityGroup(SecurityGroupManager.DEFAULT_GROUP_NAME, SecurityGroupManager.DEFAULT_GROUP_DESCRIPTION, accVO.getDomainId(), accVO.getId(),
                        accVO.getAccountName());
            }
        }
        return groupVO;
    }

    @DB
    public void work() {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Checking the database");
        }
        final SecurityGroupWorkVO work = _workDao.take(_serverId);
        if (work == null) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Security Group work: no work found");
            }
            return;
        }
        final Long userVmId = work.getInstanceId();
        if (work.getStep() == Step.Done) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Security Group work: found a job in done state, rescheduling for vm: " + userVmId);
            }
            ArrayList<Long> affectedVms = new ArrayList<Long>();
            affectedVms.add(userVmId);
            scheduleRulesetUpdateToHosts(affectedVms, false, _timeBetweenCleanups * 1000l);
            return;
        }
        s_logger.debug("Working on " + work);

        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                UserVm vm = null;
                Long seqnum = null;

                boolean locked = false;
                try {
                    vm = _userVMDao.acquireInLockTable(work.getInstanceId());
                    if (vm == null) {
                        vm = _userVMDao.findById(work.getInstanceId());
                        if (vm == null) {
                            s_logger.info("VM " + work.getInstanceId() + " is removed");
                            locked = true;
                            return;
                        }
                        s_logger.warn("Unable to acquire lock on vm id=" + userVmId);
                        return;
                    }
                    locked = true;
                    Long agentId = null;
                    VmRulesetLogVO log = _rulesetLogDao.findByVmId(userVmId);
                    if (log == null) {
                        s_logger.warn("Cannot find log record for vm id=" + userVmId);
                        return;
                    }
                    seqnum = log.getLogsequence();

                    if (vm != null && vm.getState() == State.Running) {
                        Map<PortAndProto, Set<String>> ingressRules = generateRulesForVM(userVmId, SecurityRuleType.IngressRule);
                        Map<PortAndProto, Set<String>> egressRules = generateRulesForVM(userVmId, SecurityRuleType.EgressRule);
                        agentId = vm.getHostId();
                        if (agentId != null) {
                            // get nic secondary ip address
                            String privateIp = vm.getPrivateIpAddress();
                            NicVO nic = _nicDao.findByIp4AddressAndVmId(privateIp, vm.getId());
                            List<String> nicSecIps = null;
                            if (nic != null) {
                                if (nic.getSecondaryIp()) {
                                    //get secondary ips of the vm
                                    nicSecIps = _nicSecIpDao.getSecondaryIpAddressesForNic(nic.getId());
                                }
                            }
                            SecurityGroupRulesCmd cmd = generateRulesetCmd(vm.getInstanceName(), nic.getIPv6Address(), vm.getPrivateIpAddress(), vm.getPrivateMacAddress(), vm.getId(),
                                    generateRulesetSignature(ingressRules, egressRules), seqnum, ingressRules, egressRules, nicSecIps);
                            Commands cmds = new Commands(cmd);
                            try {
                                _agentMgr.send(agentId, cmds, _answerListener);
                            } catch (AgentUnavailableException e) {
                                s_logger.debug("Unable to send ingress rules updates for vm: " + userVmId + "(agentid=" + agentId + ")");
                                _workDao.updateStep(work.getInstanceId(), seqnum, Step.Done);
                            }

                        }
                    }
                } finally {
                    if (locked) {
                        _userVMDao.releaseFromLockTable(userVmId);
                        _workDao.updateStep(work.getId(), Step.Done);
                    }
                }
            }
        });

    }

    @Override
    @DB
    public boolean addInstanceToGroups(final Long userVmId, final List<Long> groups) {
        if (!isVmSecurityGroupEnabled(userVmId)) {
            s_logger.trace("User vm " + userVmId + " is not security group enabled, not adding it to security group");
            return false;
        }
        if (groups != null && !groups.isEmpty()) {
            return Transaction.execute(new TransactionCallback<Boolean>() {
                @Override
                public Boolean doInTransaction(TransactionStatus status) {
                    UserVm userVm = _userVMDao.acquireInLockTable(userVmId); // ensures that duplicate entries are not created.
                    List<SecurityGroupVO> sgs = new ArrayList<SecurityGroupVO>();
                    for (Long sgId : groups) {
                        sgs.add(_securityGroupDao.findById(sgId));
                    }
                    final Set<SecurityGroupVO> uniqueGroups = new TreeSet<SecurityGroupVO>(new SecurityGroupVOComparator());
                    uniqueGroups.addAll(sgs);
                    if (userVm == null) {
                        s_logger.warn("Failed to acquire lock on user vm id=" + userVmId);
                    }
                    try {
                        for (SecurityGroupVO securityGroup : uniqueGroups) {
                            // don't let the group be deleted from under us.
                            SecurityGroupVO ngrpLock = _securityGroupDao.lockRow(securityGroup.getId(), false);
                            if (ngrpLock == null) {
                                s_logger.warn("Failed to acquire lock on network group id=" + securityGroup.getId() + " name=" + securityGroup.getName());
                                throw new ConcurrentModificationException("Failed to acquire lock on network group id=" + securityGroup.getId() + " name="
                                        + securityGroup.getName());
                            }
                            if (_securityGroupVMMapDao.findByVmIdGroupId(userVmId, securityGroup.getId()) == null) {
                                SecurityGroupVMMapVO groupVmMapVO = new SecurityGroupVMMapVO(securityGroup.getId(), userVmId);
                                _securityGroupVMMapDao.persist(groupVmMapVO);
                            }
                        }
                        return true;
                    } finally {
                        if (userVm != null) {
                            _userVMDao.releaseFromLockTable(userVmId);
                        }
                    }
                }
            });
        }
        return false;

    }

    @Override
    @DB
    public void removeInstanceFromGroups(final long userVmId) {
        if (_securityGroupVMMapDao.countSGForVm(userVmId) < 1) {
            s_logger.trace("No security groups found for vm id=" + userVmId + ", returning");
            return;
        }
        Transaction.execute(new TransactionCallbackNoReturn() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                UserVm userVm = _userVMDao.acquireInLockTable(userVmId); // ensures that duplicate entries are not created in
                // addInstance
                if (userVm == null) {
                    s_logger.warn("Failed to acquire lock on user vm id=" + userVmId);
                }
                int n = _securityGroupVMMapDao.deleteVM(userVmId);
                s_logger.info("Disassociated " + n + " network groups " + " from uservm " + userVmId);
                _userVMDao.releaseFromLockTable(userVmId);
            }
        });
        s_logger.debug("Security group mappings are removed successfully for vm id=" + userVmId);
    }

    @DB
    @Override
    @ActionEvent(eventType = EventTypes.EVENT_SECURITY_GROUP_DELETE, eventDescription = "deleting security group")
    public boolean deleteSecurityGroup(DeleteSecurityGroupCmd cmd) throws ResourceInUseException {
        final Long groupId = cmd.getId();
        Account caller = CallContext.current().getCallingAccount();

        SecurityGroupVO group = _securityGroupDao.findById(groupId);
        if (group == null) {
            throw new InvalidParameterValueException("Unable to find network group: " + groupId + "; failed to delete group.");
        }

        // check permissions
        _accountMgr.checkAccess(caller, null, true, group);

        return Transaction.execute(new TransactionCallbackWithException<Boolean, ResourceInUseException>() {
            @Override
            public Boolean doInTransaction(TransactionStatus status) throws ResourceInUseException {
                SecurityGroupVO group = _securityGroupDao.lockRow(groupId, true);
                if (group == null) {
                    throw new InvalidParameterValueException("Unable to find security group by id " + groupId);
                }

                if (group.getName().equalsIgnoreCase(SecurityGroupManager.DEFAULT_GROUP_NAME)) {
                    throw new InvalidParameterValueException("The network group default is reserved");
                }

                List<SecurityGroupRuleVO> allowingRules = _securityGroupRuleDao.listByAllowedSecurityGroupId(groupId);
                List<SecurityGroupVMMapVO> securityGroupVmMap = _securityGroupVMMapDao.listBySecurityGroup(groupId);
                if (!allowingRules.isEmpty()) {
                    throw new ResourceInUseException("Cannot delete group when there are security rules that allow this group");
                } else if (!securityGroupVmMap.isEmpty()) {
                    throw new ResourceInUseException("Cannot delete group when it's in use by virtual machines");
                }

                _securityGroupDao.expunge(groupId);

                s_logger.debug("Deleted security group id=" + groupId);

                return true;
            }
        });

    }

    @Override
    public void fullSync(long agentId, HashMap<String, Pair<Long, Long>> newGroupStates) {
        ArrayList<Long> affectedVms = new ArrayList<Long>();
        for (String vmName : newGroupStates.keySet()) {
            Long vmId = newGroupStates.get(vmName).first();
            Long seqno = newGroupStates.get(vmName).second();

            VmRulesetLogVO log = _rulesetLogDao.findByVmId(vmId);
            if (log != null && log.getLogsequence() != seqno) {
                affectedVms.add(vmId);
            }
        }
        if (affectedVms.size() > 0) {
            s_logger.info("Network Group full sync for agent " + agentId + " found " + affectedVms.size() + " vms out of sync");
            scheduleRulesetUpdateToHosts(affectedVms, false, null);
        }

    }

    public void cleanupFinishedWork() {
        Date before = new Date(System.currentTimeMillis() - 6 * 3600 * 1000l);
        int numDeleted = _workDao.deleteFinishedWork(before);
        if (numDeleted > 0) {
            s_logger.info("Network Group Work cleanup deleted " + numDeleted + " finished work items older than " + before.toString());
        }

    }

    private void cleanupUnfinishedWork() {
        Date before = new Date(System.currentTimeMillis() - 2 * _timeBetweenCleanups * 1000l);
        List<SecurityGroupWorkVO> unfinished = _workDao.findUnfinishedWork(before);
        if (unfinished.size() > 0) {
            s_logger.info("Network Group Work cleanup found " + unfinished.size() + " unfinished work items older than " + before.toString());
            ArrayList<Long> affectedVms = new ArrayList<Long>();
            for (SecurityGroupWorkVO work : unfinished) {
                affectedVms.add(work.getInstanceId());
                work.setStep(Step.Error);
                _workDao.update(work.getId(), work);
            }
            scheduleRulesetUpdateToHosts(affectedVms, false, null);
        } else {
            s_logger.debug("Network Group Work cleanup found no unfinished work items older than " + before.toString());
        }
    }

    @Override
    public String getSecurityGroupsNamesForVm(long vmId) {
        try {
            List<SecurityGroupVMMapVO> networkGroupsToVmMap = _securityGroupVMMapDao.listByInstanceId(vmId);
            int size = 0;
            int j = 0;
            StringBuilder networkGroupNames = new StringBuilder();

            if (networkGroupsToVmMap != null) {
                size = networkGroupsToVmMap.size();

                for (SecurityGroupVMMapVO nG : networkGroupsToVmMap) {
                    // get the group id and look up for the group name
                    SecurityGroupVO currentNetworkGroup = _securityGroupDao.findById(nG.getSecurityGroupId());
                    networkGroupNames.append(currentNetworkGroup.getName());

                    if (j < (size - 1)) {
                        networkGroupNames.append(",");
                        j++;
                    }
                }
            }

            return networkGroupNames.toString();
        } catch (Exception e) {
            s_logger.warn("Error trying to get network groups for a vm: " + e);
            return null;
        }

    }

    @Override
    public List<SecurityGroupVO> getSecurityGroupsForVm(long vmId) {
        List<SecurityGroupVMMapVO> securityGroupsToVmMap = _securityGroupVMMapDao.listByInstanceId(vmId);
        List<SecurityGroupVO> secGrps = new ArrayList<SecurityGroupVO>();
        if (securityGroupsToVmMap != null && securityGroupsToVmMap.size() > 0) {
            for (SecurityGroupVMMapVO sG : securityGroupsToVmMap) {
                SecurityGroupVO currSg = _securityGroupDao.findById(sG.getSecurityGroupId());
                secGrps.add(currSg);
            }
        }
        return secGrps;
    }

    @Override
    public boolean preStateTransitionEvent(State oldState, Event event, State newState, VirtualMachine vo, boolean status, Object opaque) {
        return true;
    }

    @Override
    public boolean postStateTransitionEvent(StateMachine2.Transition<State, Event> transition, VirtualMachine vm, boolean status, Object opaque) {
        if (!status) {
            return false;
        }

        State oldState = transition.getCurrentState();
        State newState = transition.getToState();
        Event event = transition.getEvent();
        if (VirtualMachine.State.isVmStarted(oldState, event, newState)) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Security Group Mgr: handling start of vm id" + vm.getId());
            }
            handleVmStarted((VMInstanceVO)vm);
        } else if (VirtualMachine.State.isVmStopped(oldState, event, newState)) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Security Group Mgr: handling stop of vm id" + vm.getId());
            }
            handleVmStopped((VMInstanceVO)vm);
        } else if (VirtualMachine.State.isVmMigrated(oldState, event, newState)) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Security Group Mgr: handling migration of vm id" + vm.getId());
            }
            handleVmMigrated((VMInstanceVO)vm);
        }

        return true;
    }

    @Override
    public boolean isVmSecurityGroupEnabled(Long vmId) {
        VirtualMachine vm = _vmDao.findByIdIncludingRemoved(vmId);
        List<NicProfile> nics = _networkMgr.getNicProfiles(vm);
        for (NicProfile nic : nics) {
            Network network = _networkModel.getNetwork(nic.getNetworkId());
            if (_networkModel.isSecurityGroupSupportedInNetwork(network) && vm.getHypervisorType() != HypervisorType.VMware) {
                return true;
            }
        }
        return false;
    }

    @Override
    public SecurityGroupVO getDefaultSecurityGroup(long accountId) {
        return _securityGroupDao.findByAccountAndName(accountId, DEFAULT_GROUP_NAME);
    }

    @Override
    public SecurityGroup getSecurityGroup(String name, long accountId) {
        return _securityGroupDao.findByAccountAndName(accountId, name);
    }

    @Override
    public boolean isVmMappedToDefaultSecurityGroup(long vmId) {
        UserVmVO vm = _userVmMgr.getVirtualMachine(vmId);
        SecurityGroup defaultGroup = getDefaultSecurityGroup(vm.getAccountId());
        if (defaultGroup == null) {
            s_logger.warn("Unable to find default security group for account id=" + vm.getAccountId());
            return false;
        }
        SecurityGroupVMMapVO map = _securityGroupVMMapDao.findByVmIdGroupId(vmId, defaultGroup.getId());
        if (map == null) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public boolean securityGroupRulesForVmSecIp(long nicId, String secondaryIp, boolean ruleAction) {
        Account caller = CallContext.current().getCallingAccount();

        if (secondaryIp == null) {
            throw new InvalidParameterValueException("Vm secondaryIp can't be null");
        }

        NicVO nic = _nicDao.findById(nicId);
        long vmId = nic.getInstanceId();
        UserVm vm = _userVMDao.findById(vmId);
        if (vm == null || vm.getType() != VirtualMachine.Type.User) {
            throw new InvalidParameterValueException("Can't configure the SG ipset, arprules rules for the non existing or non user vm");
        }

        // Verify permissions
        _accountMgr.checkAccess(caller, null, false, vm);

        // Validate parameters
        List<SecurityGroupVO> vmSgGrps = getSecurityGroupsForVm(vmId);
        if (vmSgGrps.isEmpty()) {
            s_logger.debug("Vm is not in any Security group ");
            return true;
        }

        //If network does not support SG service, no need add SG rules for secondary ip
        Network network = _networkModel.getNetwork(nic.getNetworkId());
        if (!_networkModel.isSecurityGroupSupportedInNetwork(network)) {
            s_logger.debug("Network " + network + " is not enabled with security group service, "+
                    "so not applying SG rules for secondary ip");
            return true;
        }

        String vmMac = vm.getPrivateMacAddress();
        String vmName = vm.getInstanceName();
        if (vmMac == null || vmName == null) {
            throw new InvalidParameterValueException("vm name or vm mac can't be null");
        }

        //create command for the to add ip in ipset and arptables rules
        NetworkRulesVmSecondaryIpCommand cmd = new NetworkRulesVmSecondaryIpCommand(vmName, vmMac, secondaryIp, ruleAction);
        s_logger.debug("Asking agent to configure rules for vm secondary ip");
        Commands cmds = null;

        cmds = new Commands(cmd);
        try {
            _agentMgr.send(vm.getHostId(), cmds);
        } catch (AgentUnavailableException e) {
            s_logger.debug(e.toString());
        } catch (OperationTimedoutException e) {
            s_logger.debug(e.toString());
        }

        return true;
    }
}
