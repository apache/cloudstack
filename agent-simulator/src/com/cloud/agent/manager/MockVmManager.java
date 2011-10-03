/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.agent.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.CleanupNetworkRulesCmd;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.SecurityIngressRuleAnswer;
import com.cloud.agent.api.SecurityIngressRulesCmd;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.check.CheckSshAnswer;
import com.cloud.agent.api.check.CheckSshCommand;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.agent.api.routing.DhcpEntryCommand;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.SetPortForwardingRulesAnswer;
import com.cloud.agent.api.routing.SetPortForwardingRulesCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesAnswer;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.simulator.MockVm;
import com.cloud.utils.Pair;
import com.cloud.utils.component.Manager;
import com.cloud.vm.VirtualMachine.State;

public interface MockVmManager extends Manager {
    public Answer stopVM(StopCommand cmd);
	public Answer rebootVM(RebootCommand cmd);
    
    public Answer checkVmState(CheckVirtualMachineCommand cmd, String hostGuid);
    public Map<String, State> getVmStates(String hostGuid);
    public Answer getVncPort(GetVncPortCommand cmd);

	Answer startVM(StartCommand cmd, SimulatorInfo info);

	Answer getVmStats(GetVmStatsCommand cmd);
    public CheckSshAnswer checkSshCommand(CheckSshCommand cmd);
    
    Answer SetStaticNatRules(SetStaticNatRulesCommand cmd);
    
    Answer SetPortForwardingRules(SetPortForwardingRulesCommand cmd);
    
    Answer getNetworkUsage(NetworkUsageCommand cmd);
    
    Answer IpAssoc(IpAssocCommand cmd);

    Answer LoadBalancerConfig(LoadBalancerConfigCommand cmd);
    
    Answer AddDhcpEntry(DhcpEntryCommand cmd);
    
    Answer setVmData(VmDataCommand cmd);
    Answer CleanupNetworkRules(CleanupNetworkRulesCmd cmd, SimulatorInfo info);
    
    Answer CheckConsoleProxyLoad(CheckConsoleProxyLoadCommand cmd);
    Answer WatchConsoleProxyLoad(WatchConsoleProxyLoadCommand cmd);
    
    Answer SavePassword(SavePasswordCommand cmd);
    HashMap<String, Pair<Long, Long>> syncNetworkGroups(SimulatorInfo info);
    SecurityIngressRuleAnswer AddSecurityIngressRules(SecurityIngressRulesCmd cmd, SimulatorInfo info);
	MigrateAnswer Migrate(MigrateCommand cmd, SimulatorInfo info);
    
}
