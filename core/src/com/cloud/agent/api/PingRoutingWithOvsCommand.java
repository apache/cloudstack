package com.cloud.agent.api;

import java.util.List;
import java.util.Map;

import com.cloud.host.Host;
import com.cloud.utils.Pair;
import com.cloud.vm.VirtualMachine.State;

public class PingRoutingWithOvsCommand extends PingRoutingCommand {
	List<Pair<String, Long>> states;
	
	protected PingRoutingWithOvsCommand() {
		super();
	}
	
	public PingRoutingWithOvsCommand(Host.Type type, long id,
			Map<String, State> states, List<Pair<String, Long>> ovsStates) {
		super(type, id, states);
		this.states = ovsStates;
	}
	
	public List<Pair<String, Long>> getStates() {
		return states;
	}
}
