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
package com.cloud.agent.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckRouterAnswer;
import com.cloud.agent.api.CheckRouterCommand;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.CleanupNetworkRulesCmd;
import com.cloud.agent.api.CreateVMSnapshotAnswer;
import com.cloud.agent.api.CreateVMSnapshotCommand;
import com.cloud.agent.api.DeleteVMSnapshotAnswer;
import com.cloud.agent.api.DeleteVMSnapshotCommand;
import com.cloud.agent.api.FenceAnswer;
import com.cloud.agent.api.FenceCommand;
import com.cloud.agent.api.GetDomRVersionAnswer;
import com.cloud.agent.api.GetDomRVersionCmd;
import com.cloud.agent.api.GetVmStatsAnswer;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.GetVncPortAnswer;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.NetworkRulesVmSecondaryIpCommand;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RevertToVMSnapshotAnswer;
import com.cloud.agent.api.RevertToVMSnapshotCommand;
import com.cloud.agent.api.ScaleVmAnswer;
import com.cloud.agent.api.ScaleVmCommand;
import com.cloud.agent.api.SecurityGroupRuleAnswer;
import com.cloud.agent.api.SecurityGroupRulesCmd;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.router.VirtualRouter;
import com.cloud.simulator.MockHost;
import com.cloud.simulator.MockSecurityRulesVO;
import com.cloud.simulator.MockVMVO;
import com.cloud.simulator.MockVm;
import com.cloud.simulator.dao.MockHostDao;
import com.cloud.simulator.dao.MockSecurityRulesDao;
import com.cloud.simulator.dao.MockVMDao;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine.PowerState;

@Component
public class MockVmManagerImpl extends ManagerBase implements MockVmManager {
    private static final Logger s_logger = Logger.getLogger(MockVmManagerImpl.class);

    @Inject
    MockVMDao _mockVmDao = null;
    @Inject
    MockAgentManager _mockAgentMgr = null;
    @Inject
    MockHostDao _mockHostDao = null;
    @Inject
    MockSecurityRulesDao _mockSecurityDao = null;
    private final Map<String, Map<String, Ternary<String, Long, Long>>> _securityRules = new ConcurrentHashMap<String, Map<String, Ternary<String, Long, Long>>>();

    public MockVmManagerImpl() {
    }

    @Override
    public boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {

        return true;
    }

    public String startVM(final String vmName, final NicTO[] nics, final int cpuHz, final long ramSize, final String bootArgs, final String hostGuid) {

        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        MockHost host = null;
        MockVm vm = null;
        try {
            txn.start();
            host = _mockHostDao.findByGuid(hostGuid);
            if (host == null) {
                return "can't find host";
            }

            vm = _mockVmDao.findByVmName(vmName);
            txn.commit();
        } catch (final Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Unable to start VM " + vmName, ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }

        if (vm == null) {
            final int vncPort = 0;
            if (vncPort < 0) {
                return "Unable to allocate VNC port";
            }
            vm = new MockVMVO();
            vm.setCpu(cpuHz);
            vm.setMemory(ramSize);
            vm.setPowerState(PowerState.PowerOn);
            vm.setName(vmName);
            vm.setVncPort(vncPort);
            vm.setHostId(host.getId());
            vm.setBootargs(bootArgs);
            if (vmName.startsWith("s-")) {
                vm.setType("SecondaryStorageVm");
            } else if (vmName.startsWith("v-")) {
                vm.setType("ConsoleProxy");
            } else if (vmName.startsWith("r-")) {
                vm.setType("DomainRouter");
            } else if (vmName.startsWith("i-")) {
                vm.setType("User");
            }
            txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
            try {
                txn.start();
                vm = _mockVmDao.persist((MockVMVO)vm);
                txn.commit();
            } catch (final Exception ex) {
                txn.rollback();
                throw new CloudRuntimeException("unable to save vm to db " + vm.getName(), ex);
            } finally {
                txn.close();
                txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
                txn.close();
            }
        } else {
            if (vm.getPowerState() == PowerState.PowerOff) {
                vm.setPowerState(PowerState.PowerOn);
                txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
                try {
                    txn.start();
                    _mockVmDao.update(vm.getId(), (MockVMVO)vm);
                    txn.commit();
                } catch (final Exception ex) {
                    txn.rollback();
                    throw new CloudRuntimeException("unable to update vm " + vm.getName(), ex);
                } finally {
                    txn.close();
                    txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
                    txn.close();
                }
            }
        }

        if (vm.getPowerState() == PowerState.PowerOn && vmName.startsWith("s-")) {
            String prvIp = null;
            String prvMac = null;
            String prvNetMask = null;

            for (final NicTO nic : nics) {
                if (nic.getType() == TrafficType.Management) {
                    prvIp = nic.getIp();
                    prvMac = nic.getMac();
                    prvNetMask = nic.getNetmask();
                }
            }
            long dcId = 0;
            long podId = 0;
            String name = null;
            String vmType = null;
            String url = null;
            final String[] args = bootArgs.trim().split(" ");
            for (final String arg : args) {
                final String[] params = arg.split("=");
                if (params.length < 1) {
                    continue;
                }

                if (params[0].equalsIgnoreCase("zone")) {
                    dcId = Long.parseLong(params[1]);
                } else if (params[0].equalsIgnoreCase("name")) {
                    name = params[1];
                } else if (params[0].equalsIgnoreCase("type")) {
                    vmType = params[1];
                } else if (params[0].equalsIgnoreCase("url")) {
                    url = params[1];
                } else if (params[0].equalsIgnoreCase("pod")) {
                    podId = Long.parseLong(params[1]);
                }
            }

            _mockAgentMgr.handleSystemVMStart(vm.getId(), prvIp, prvMac, prvNetMask, dcId, podId, name, vmType, url);
        }

        return null;
    }

    @Override
    public Map<String, MockVMVO> getVms(final String hostGuid) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            final List<MockVMVO> vms = _mockVmDao.findByHostGuid(hostGuid);
            final Map<String, MockVMVO> vmMap = new HashMap<String, MockVMVO>();
            for (final MockVMVO vm : vms) {
                vmMap.put(vm.getName(), vm);
            }
            txn.commit();
            return vmMap;
        } catch (final Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("unable to fetch vms from host " + hostGuid, ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
    }

    @Override
    public CheckRouterAnswer checkRouter(final CheckRouterCommand cmd) {
        final String router_name = cmd.getAccessDetail(NetworkElementCommand.ROUTER_NAME);
        final MockVm vm = _mockVmDao.findByVmName(router_name);
        final String args = vm.getBootargs();
        if (args.indexOf("router_pr=100") > 0) {
            s_logger.debug("Router priority is for PRIMARY");
            final CheckRouterAnswer ans = new CheckRouterAnswer(cmd, "Status: PRIMARY", true);
            ans.setState(VirtualRouter.RedundantState.PRIMARY);
            return ans;
        } else {
            s_logger.debug("Router priority is for BACKUP");
            final CheckRouterAnswer ans = new CheckRouterAnswer(cmd, "Status: BACKUP", true);
            ans.setState(VirtualRouter.RedundantState.BACKUP);
            return ans;
        }
    }

    @Override
    public Map<String, PowerState> getVmStates(final String hostGuid) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            final Map<String, PowerState> states = new HashMap<String, PowerState>();
            final List<MockVMVO> vms = _mockVmDao.findByHostGuid(hostGuid);
            if (vms.isEmpty()) {
                txn.commit();
                return states;
            }
            for (final MockVm vm : vms) {
                states.put(vm.getName(), vm.getPowerState());
            }
            txn.commit();
            return states;
        } catch (final Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("unable to fetch vms from host " + hostGuid, ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
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
        return this.getClass().getSimpleName();
    }

    @Override
    public Answer getVmStats(final GetVmStatsCommand cmd) {
        final HashMap<String, VmStatsEntry> vmStatsNameMap = new HashMap<String, VmStatsEntry>();
        final List<String> vmNames = cmd.getVmNames();
        for (final String vmName : vmNames) {
            final VmStatsEntry entry = new VmStatsEntry(0, 0, 0, 0, 0, 0, 0, "vm");
            entry.setNetworkReadKBs(32768); // default values 256 KBps
            entry.setNetworkWriteKBs(16384);
            entry.setCPUUtilization(10);
            entry.setNumCPUs(1);
            vmStatsNameMap.put(vmName, entry);
        }
        return new GetVmStatsAnswer(cmd, vmStatsNameMap);
    }

    @Override
    public CheckVirtualMachineAnswer checkVmState(final CheckVirtualMachineCommand cmd) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            final MockVMVO vm = _mockVmDao.findByVmName(cmd.getVmName());
            if (vm == null) {
                return new CheckVirtualMachineAnswer(cmd, "can't find vm:" + cmd.getVmName());
            }

            txn.commit();
            return new CheckVirtualMachineAnswer(cmd, vm.getPowerState(), vm.getVncPort());
        } catch (final Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("unable to fetch vm state " + cmd.getVmName(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
    }

    @Override
    public StartAnswer startVM(final StartCommand cmd, final SimulatorInfo info) {
        final VirtualMachineTO vm = cmd.getVirtualMachine();
        final String result = startVM(vm.getName(), vm.getNics(), vm.getCpus() * vm.getMaxSpeed(), vm.getMaxRam(), vm.getBootArgs(), info.getHostUuid());
        if (result != null) {
            return new StartAnswer(cmd, result);
        } else {
            return new StartAnswer(cmd);
        }
    }

    @Override
    public CheckSshAnswer checkSshCommand(final CheckSshCommand cmd) {
        return new CheckSshAnswer(cmd);
    }

    @Override
    public MigrateAnswer migrate(final MigrateCommand cmd, final SimulatorInfo info) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            final String vmName = cmd.getVmName();
            final String destGuid = cmd.getHostGuid();
            final MockVMVO vm = _mockVmDao.findByVmNameAndHost(vmName, info.getHostUuid());
            if (vm == null) {
                return new MigrateAnswer(cmd, false, "can't find vm:" + vmName + " on host:" + info.getHostUuid(), null);
            }

            final MockHost destHost = _mockHostDao.findByGuid(destGuid);
            if (destHost == null) {
                return new MigrateAnswer(cmd, false, "can't find destination host:" + destGuid, null);
            }
            vm.setHostId(destHost.getId());
            _mockVmDao.update(vm.getId(), vm);
            txn.commit();
            return new MigrateAnswer(cmd, true, null, 0);
        } catch (final Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("unable to migrate vm " + cmd.getVmName(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
    }

    @Override
    public PrepareForMigrationAnswer prepareForMigrate(final PrepareForMigrationCommand cmd) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        final VirtualMachineTO vmTo = cmd.getVirtualMachine();
        try {
            txn.start();
            final MockVMVO vm = _mockVmDao.findById(vmTo.getId());
            _mockVmDao.update(vm.getId(), vm);
            txn.commit();
        } catch (final Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("unable to find vm " + vmTo.getName(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
            return new PrepareForMigrationAnswer(cmd);
        }
    }

    @Override
    public Answer setVmData(final VmDataCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public Answer cleanupNetworkRules(final CleanupNetworkRulesCmd cmd, final SimulatorInfo info) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            final List<MockSecurityRulesVO> rules = _mockSecurityDao.findByHost(info.getHostUuid());
            for (final MockSecurityRulesVO rule : rules) {
                final MockVMVO vm = _mockVmDao.findByVmNameAndHost(rule.getVmName(), info.getHostUuid());
                if (vm == null) {
                    _mockSecurityDao.remove(rule.getId());
                }
            }
            txn.commit();
            return new Answer(cmd);
        } catch (final Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("unable to clean up rules", ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
    }

    @Override
    public Answer scaleVm(final ScaleVmCommand cmd) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            final String vmName = cmd.getVmName();
            final MockVMVO vm = _mockVmDao.findByVmName(vmName);
            if (vm == null) {
                return new ScaleVmAnswer(cmd, false, "Can't find VM " + vmName);
            }
            vm.setCpu(cmd.getCpus() * cmd.getMaxSpeed());
            vm.setMemory(cmd.getMaxRam());
            _mockVmDao.update(vm.getId(), vm);
            s_logger.debug("Scaled up VM " + vmName);
            txn.commit();
            return new ScaleVmAnswer(cmd, true, null);
        } catch (final Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("Unable to scale up VM", ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
    }

    @Override
    public Answer plugSecondaryIp(final NetworkRulesVmSecondaryIpCommand cmd) {
        s_logger.debug("Plugged secondary IP to VM " + cmd.getVmName());
        return new Answer(cmd, true, null);
    }

    @Override
    public Answer createVmSnapshot(final CreateVMSnapshotCommand cmd) {
        final String vmName = cmd.getVmName();
        final String vmSnapshotName = cmd.getTarget().getSnapshotName();

        s_logger.debug("Created snapshot " + vmSnapshotName + " for vm " + vmName);
        return new CreateVMSnapshotAnswer(cmd, cmd.getTarget(), cmd.getVolumeTOs());
    }

    @Override
    public Answer deleteVmSnapshot(final DeleteVMSnapshotCommand cmd) {
        final String vm = cmd.getVmName();
        final String snapshotName = cmd.getTarget().getSnapshotName();
        if (_mockVmDao.findByVmName(cmd.getVmName()) == null) {
            return new DeleteVMSnapshotAnswer(cmd, false, "No VM by name " + cmd.getVmName());
        }
        s_logger.debug("Removed snapshot " + snapshotName + " of VM " + vm);
        return new DeleteVMSnapshotAnswer(cmd, cmd.getVolumeTOs());
    }

    @Override
    public Answer revertVmSnapshot(final RevertToVMSnapshotCommand cmd) {
        final String vm = cmd.getVmName();
        final String snapshot = cmd.getTarget().getSnapshotName();
        final MockVMVO vmVo = _mockVmDao.findByVmName(cmd.getVmName());
        if (vmVo == null) {
            return new RevertToVMSnapshotAnswer(cmd, false, "No VM by name " + cmd.getVmName());
        }
        s_logger.debug("Reverted to snapshot " + snapshot + " of VM " + vm);
        return new RevertToVMSnapshotAnswer(cmd, cmd.getVolumeTOs(), vmVo.getPowerState());
    }

    @Override
    public StopAnswer stopVM(final StopCommand cmd) {
        TransactionLegacy txn = TransactionLegacy.open(TransactionLegacy.SIMULATOR_DB);
        try {
            txn.start();
            final String vmName = cmd.getVmName();
            final MockVm vm = _mockVmDao.findByVmName(vmName);
            if (vm != null) {
                vm.setPowerState(PowerState.PowerOff);
                _mockVmDao.update(vm.getId(), (MockVMVO)vm);
            }

            if (vmName.startsWith("s-")) {
                _mockAgentMgr.handleSystemVMStop(vm.getId());
            }
            txn.commit();
            return new StopAnswer(cmd, null, true);
        } catch (final Exception ex) {
            txn.rollback();
            throw new CloudRuntimeException("unable to stop vm " + cmd.getVmName(), ex);
        } finally {
            txn.close();
            txn = TransactionLegacy.open(TransactionLegacy.CLOUD_DB);
            txn.close();
        }
    }

    @Override
    public RebootAnswer rebootVM(final RebootCommand cmd) {
        return new RebootAnswer(cmd, "Rebooted " + cmd.getVmName(), true);
    }

    @Override
    public Answer getVncPort(final GetVncPortCommand cmd) {
        return new GetVncPortAnswer(cmd, 0);
    }

    @Override
    public Answer checkConsoleProxyLoad(final CheckConsoleProxyLoadCommand cmd) {
        return Answer.createUnsupportedCommandAnswer(cmd);
    }

    @Override
    public Answer watchConsoleProxyLoad(final WatchConsoleProxyLoadCommand cmd) {
        return Answer.createUnsupportedCommandAnswer(cmd);
    }

    @Override
    public GetDomRVersionAnswer getDomRVersion(final GetDomRVersionCmd cmd) {
        final String template_version = "CloudStack Release "+ NetworkOrchestrationService.MinVRVersion.defaultValue();
        return new GetDomRVersionAnswer(cmd, null, template_version, UUID.randomUUID().toString());
    }

    @Override
    public SecurityGroupRuleAnswer addSecurityGroupRules(final SecurityGroupRulesCmd cmd, final SimulatorInfo info) {
        if (!info.isEnabled()) {
            return new SecurityGroupRuleAnswer(cmd, false, "Disabled", SecurityGroupRuleAnswer.FailureReason.CANNOT_BRIDGE_FIREWALL);
        }

        Map<String, Ternary<String, Long, Long>> rules = _securityRules.get(info.getHostUuid());

        if (rules == null) {
            logSecurityGroupAction(cmd, null);
            rules = new ConcurrentHashMap<String, Ternary<String, Long, Long>>();
            rules.put(cmd.getVmName(), new Ternary<String, Long, Long>(cmd.getSignature(), cmd.getVmId(), cmd.getSeqNum()));
            _securityRules.put(info.getHostUuid(), rules);
        } else {
            logSecurityGroupAction(cmd, rules.get(cmd.getVmName()));
            rules.put(cmd.getVmName(), new Ternary<String, Long, Long>(cmd.getSignature(), cmd.getVmId(), cmd.getSeqNum()));
        }

        return new SecurityGroupRuleAnswer(cmd);
    }

    private boolean logSecurityGroupAction(final SecurityGroupRulesCmd cmd, final Ternary<String, Long, Long> rule) {
        String action = ", do nothing";
        String reason = ", reason=";
        final Long currSeqnum = rule == null ? null : rule.third();
        final String currSig = rule == null ? null : rule.first();
        boolean updateSeqnoAndSig = false;
        if (currSeqnum != null) {
            if (cmd.getSeqNum() > currSeqnum) {
                s_logger.info("New seqno received: " + cmd.getSeqNum() + " curr=" + currSeqnum);
                updateSeqnoAndSig = true;
                if (!cmd.getSignature().equals(currSig)) {
                    s_logger.info("New seqno received: " + cmd.getSeqNum() + " curr=" + currSeqnum + " new signature received:" + cmd.getSignature() + " curr=" +
                            currSig + ", updated iptables");
                    action = ", updated iptables";
                    reason = reason + "seqno_increased_sig_changed";
                } else {
                    s_logger.info("New seqno received: " + cmd.getSeqNum() + " curr=" + currSeqnum + " no change in signature:" + cmd.getSignature() + ", do nothing");
                    reason = reason + "seqno_increased_sig_same";
                }
            } else if (cmd.getSeqNum() < currSeqnum) {
                s_logger.info("Older seqno received: " + cmd.getSeqNum() + " curr=" + currSeqnum + ", do nothing");
                reason = reason + "seqno_decreased";
            } else {
                if (!cmd.getSignature().equals(currSig)) {
                    s_logger.info("Identical seqno received: " + cmd.getSeqNum() + " new signature received:" + cmd.getSignature() + " curr=" + currSig +
                            ", updated iptables");
                    action = ", updated iptables";
                    reason = reason + "seqno_same_sig_changed";
                    updateSeqnoAndSig = true;
                } else {
                    s_logger.info("Identical seqno received: " + cmd.getSeqNum() + " curr=" + currSeqnum + " no change in signature:" + cmd.getSignature() +
                            ", do nothing");
                    reason = reason + "seqno_same_sig_same";
                }
            }
        } else {
            s_logger.info("New seqno received: " + cmd.getSeqNum() + " old=null");
            updateSeqnoAndSig = true;
            action = ", updated iptables";
            reason = ", seqno_new";
        }
        s_logger.info("Programmed network rules for vm " + cmd.getVmName() + " seqno=" + cmd.getSeqNum() + " signature=" + cmd.getSignature() + " guestIp=" +
                cmd.getGuestIp() + ", numIngressRules=" + cmd.getIngressRuleSet().size() + ", numEgressRules=" + cmd.getEgressRuleSet().size() + " total cidrs=" +
                cmd.getTotalNumCidrs() + action + reason);
        return updateSeqnoAndSig;
    }

    @Override
    public Answer savePassword(final SavePasswordCommand cmd) {
        return new Answer(cmd);
    }

    @Override
    public HashMap<String, Pair<Long, Long>> syncNetworkGroups(final SimulatorInfo info) {
        final HashMap<String, Pair<Long, Long>> maps = new HashMap<String, Pair<Long, Long>>();

        final Map<String, Ternary<String, Long, Long>> rules = _securityRules.get(info.getHostUuid());
        if (rules == null) {
            return maps;
        }
        for (final Map.Entry<String, Ternary<String, Long, Long>> rule : rules.entrySet()) {
            maps.put(rule.getKey(), new Pair<Long, Long>(rule.getValue().second(), rule.getValue().third()));
        }
        return maps;
    }

    @Override
    public Answer fence(final FenceCommand cmd) {
        return new FenceAnswer(cmd);
    }
}
