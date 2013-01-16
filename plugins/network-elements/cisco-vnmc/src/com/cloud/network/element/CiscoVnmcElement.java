package com.cloud.network.element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.rules.FirewallRule;
import com.cloud.offering.NetworkOffering;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = NetworkElement.class)
public class CiscoVnmcElement implements DhcpServiceProvider,
		FirewallServiceProvider, NetworkElement {
	private static final Logger s_logger = Logger.getLogger(CiscoVnmcElement.class);
    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();
    

    private boolean canHandle(Network network) {
        if (network.getBroadcastDomainType() != BroadcastDomainType.Vlan) {
            return false; //TODO: should handle VxLAN as well
        }
        
        return true;        
    }
    
	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		
        return true;
	}

	private static Map<Service, Map<Capability, String>> setCapabilities() {
		Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();
        capabilities.put(Service.Gateway, null);
        capabilities.put(Service.Dhcp, null);
        Map<Capability, String> firewallCapabilities = new HashMap<Capability, String>();
        firewallCapabilities.put(Capability.TrafficStatistics, "per public ip");
        firewallCapabilities.put(Capability.SupportedProtocols, "tcp,udp,icmp");
        firewallCapabilities.put(Capability.MultipleIps, "true");
        capabilities.put(Service.Firewall, firewallCapabilities);
        
        capabilities.put(Service.StaticNat, null);
        capabilities.put(Service.PortForwarding, null);
        
        Map<Capability, String> sourceNatCapabilities = new HashMap<Capability, String>();
        sourceNatCapabilities.put(Capability.SupportedSourceNatTypes, "peraccount");
        sourceNatCapabilities.put(Capability.RedundantRouter, "false"); //TODO:
        capabilities.put(Service.SourceNat, sourceNatCapabilities);
        return capabilities;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean start() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
	}

	@Override
	public Provider getProvider() {
		return CiscoVnmcElementService.CiscoVnmc;
	}

	@Override
	public boolean implement(Network network, NetworkOffering offering,
			DeployDestination dest, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException,
			InsufficientCapacityException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean prepare(Network network, NicProfile nic,
			VirtualMachineProfile<? extends VirtualMachine> vm,
			DeployDestination dest, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException,
			InsufficientCapacityException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean release(Network network, NicProfile nic,
			VirtualMachineProfile<? extends VirtualMachine> vm,
			ReservationContext context) throws ConcurrentOperationException,
			ResourceUnavailableException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean shutdown(Network network, ReservationContext context,
			boolean cleanup) throws ConcurrentOperationException,
			ResourceUnavailableException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean destroy(Network network)
			throws ConcurrentOperationException, ResourceUnavailableException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isReady(PhysicalNetworkServiceProvider provider) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean shutdownProviderInstances(
			PhysicalNetworkServiceProvider provider, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canEnableIndividualServices() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean verifyServicesCombination(Set<Service> services) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean applyFWRules(Network network,
			List<? extends FirewallRule> rules)
			throws ResourceUnavailableException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addDhcpEntry(Network network, NicProfile nic,
			VirtualMachineProfile<? extends VirtualMachine> vm,
			DeployDestination dest, ReservationContext context)
			throws ConcurrentOperationException, InsufficientCapacityException,
			ResourceUnavailableException {
		// TODO Auto-generated method stub
		return false;
	}

}
