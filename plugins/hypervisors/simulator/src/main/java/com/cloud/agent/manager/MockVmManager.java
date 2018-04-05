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
import java.util.Map;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckRouterAnswer;
import com.cloud.agent.api.CheckRouterCommand;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.CleanupNetworkRulesCmd;
import com.cloud.agent.api.CreateVMSnapshotCommand;
import com.cloud.agent.api.DeleteVMSnapshotCommand;
import com.cloud.agent.api.FenceCommand;
import com.cloud.agent.api.GetDomRVersionAnswer;
import com.cloud.agent.api.GetDomRVersionCmd;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.NetworkRulesVmSecondaryIpCommand;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.RevertToVMSnapshotCommand;
import com.cloud.agent.api.ScaleVmCommand;
import com.cloud.agent.api.SecurityGroupRuleAnswer;
import com.cloud.agent.api.SecurityGroupRulesCmd;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.simulator.MockVMVO;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.vm.VirtualMachine.PowerState;

public interface MockVmManager extends Manager {

    Map<String, PowerState> getVmStates(String hostGuid);

    Map<String, MockVMVO> getVms(String hostGuid);

    HashMap<String, Pair<Long, Long>> syncNetworkGroups(SimulatorInfo info);

    StartAnswer startVM(StartCommand cmd, SimulatorInfo info);

    StopAnswer stopVM(StopCommand cmd);

    RebootAnswer rebootVM(RebootCommand cmd);

    Answer checkVmState(CheckVirtualMachineCommand cmd);

    Answer getVncPort(GetVncPortCommand cmd);

    Answer getVmStats(GetVmStatsCommand cmd);

    CheckSshAnswer checkSshCommand(CheckSshCommand cmd);

    Answer setVmData(VmDataCommand cmd);

    Answer checkConsoleProxyLoad(CheckConsoleProxyLoadCommand cmd);

    Answer watchConsoleProxyLoad(WatchConsoleProxyLoadCommand cmd);

    Answer savePassword(SavePasswordCommand cmd);

    MigrateAnswer migrate(MigrateCommand cmd, SimulatorInfo info);

    PrepareForMigrationAnswer prepareForMigrate(PrepareForMigrationCommand cmd);

    SecurityGroupRuleAnswer addSecurityGroupRules(SecurityGroupRulesCmd cmd, SimulatorInfo info);

    GetDomRVersionAnswer getDomRVersion(GetDomRVersionCmd cmd);

    CheckRouterAnswer checkRouter(CheckRouterCommand cmd);

    Answer cleanupNetworkRules(CleanupNetworkRulesCmd cmd, SimulatorInfo info);

    Answer scaleVm(ScaleVmCommand cmd);

    Answer plugSecondaryIp(NetworkRulesVmSecondaryIpCommand cmd);

    Answer createVmSnapshot(CreateVMSnapshotCommand cmd);

    Answer deleteVmSnapshot(DeleteVMSnapshotCommand cmd);

    Answer revertVmSnapshot(RevertToVMSnapshotCommand cmd);

    Answer fence(FenceCommand cmd);
}
