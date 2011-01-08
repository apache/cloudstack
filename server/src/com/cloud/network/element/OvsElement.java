package com.cloud.network.element;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Networks;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.ovs.OvsNetworkManager;
import com.cloud.network.rules.FirewallRule;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value=NetworkElement.class)
public class OvsElement extends AdapterBase implements NetworkElement {
	@Inject OvsNetworkManager _ovsNetworkMgr;
	
	@Override
	public Map<Service, Map<Capability, String>> getCapabilities() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Provider getProvider() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean implement(Network network, NetworkOffering offering,
			DeployDestination dest, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException,
			InsufficientCapacityException {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean prepare(Network network, NicProfile nic,
			VirtualMachineProfile<? extends VirtualMachine> vm,
			DeployDestination dest, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException,
			InsufficientCapacityException {
		VirtualMachine instance = vm.getVirtualMachine();
		
		if (network.getTrafficType() != Networks.TrafficType.Guest ||
			instance.getType() == VirtualMachine.Type.DomainRouter) {
			return true;
		}	
		
		//_ovsNetworkMgr.CheckAndUpdateDhcpFlow(network, vm.getVirtualMachine());
		return true;
	}

	@Override
	public boolean release(Network network, NicProfile nic,
			VirtualMachineProfile<? extends VirtualMachine> vm,
			ReservationContext context) throws ConcurrentOperationException,
			ResourceUnavailableException {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean shutdown(Network network, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean applyIps(Network network,
			List<? extends PublicIpAddress> ipAddress)
			throws ResourceUnavailableException {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean applyRules(Network network,
			List<? extends FirewallRule> rules)
			throws ResourceUnavailableException {
		// TODO Auto-generated method stub
		return true;
	}

}
