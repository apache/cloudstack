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
package com.cloud.network.element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupOvsCommand;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.ovs.OvsTunnelManager;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = { NetworkElement.class, ConnectivityProvider.class,
		SourceNatServiceProvider.class, StaticNatServiceProvider.class,
		PortForwardingServiceProvider.class, IpDeployer.class })
public class OvsElement extends AdapterBase implements NetworkElement,
		OvsElementService, ConnectivityProvider, ResourceStateAdapter,
		SourceNatServiceProvider, PortForwardingServiceProvider,
		StaticNatServiceProvider, IpDeployer {
	@Inject
	OvsTunnelManager _ovsTunnelMgr;
	@Inject
	NetworkModel _networkModel;
	@Inject
	NetworkServiceMapDao _ntwkSrvcDao;
	@Inject
	ResourceManager _resourceMgr;

	private static final Logger s_logger = Logger.getLogger(OvsElement.class);
	private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

	@Override
	public Map<Service, Map<Capability, String>> getCapabilities() {
		return capabilities;
	}

	@Override
	public Provider getProvider() {
		return Provider.Ovs;
	}

	protected boolean canHandle(Network network, Service service) {
		s_logger.debug("Checking if OvsElement can handle service "
				+ service.getName() + " on network " + network.getDisplayText());
		if (network.getBroadcastDomainType() != BroadcastDomainType.Vswitch) {
			return false;
		}

		if (!_networkModel.isProviderForNetwork(getProvider(), network.getId())) {
			s_logger.debug("OvsElement is not a provider for network "
					+ network.getDisplayText());
			return false;
		}

		if (!_ntwkSrvcDao.canProviderSupportServiceInNetwork(network.getId(),
				service, Network.Provider.Ovs)) {
			s_logger.debug("OvsElement can't provide the " + service.getName()
					+ " service on network " + network.getDisplayText());
			return false;
		}

		return true;
	}

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		super.configure(name, params);
		_resourceMgr.registerResourceStateAdapter(name, this);
		return true;
	}

	@Override
	public boolean implement(Network network, NetworkOffering offering,
			DeployDestination dest, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException,
			InsufficientCapacityException {
		s_logger.debug("entering OvsElement implement function for network "
				+ network.getDisplayText() + " (state " + network.getState()
				+ ")");

		if (!canHandle(network, Service.Connectivity)) {
			return false;
		}
		// TODO: implement SourceNat immediately when we code L3 services

		return true;
	}

	@Override
	public boolean prepare(Network network, NicProfile nic,
			VirtualMachineProfile<? extends VirtualMachine> vm,
			DeployDestination dest, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException,
			InsufficientCapacityException {
		if (!canHandle(network, Service.Connectivity)) {
			return false;
		}

		if (nic.getBroadcastType() != Networks.BroadcastDomainType.Vswitch) {
			return false;
		}

		if (nic.getTrafficType() != Networks.TrafficType.Guest) {
			return false;
		}

		_ovsTunnelMgr.VmCheckAndCreateTunnel(vm, network, dest);

		return true;
	}

	@Override
	public boolean release(Network network, NicProfile nic,
			VirtualMachineProfile<? extends VirtualMachine> vm,
			ReservationContext context) throws ConcurrentOperationException,
			ResourceUnavailableException {
		if (!canHandle(network, Service.Connectivity)) {
			return false;
		}
		if (nic.getBroadcastType() != Networks.BroadcastDomainType.Vswitch) {
			return false;
		}

		if (nic.getTrafficType() != Networks.TrafficType.Guest) {
			return false;
		}

		_ovsTunnelMgr.CheckAndDestroyTunnel(vm.getVirtualMachine(), network);
		return true;
	}

	@Override
	public boolean shutdown(Network network, ReservationContext context,
			boolean cleanup) throws ConcurrentOperationException,
			ResourceUnavailableException {
		if (!canHandle(network, Service.Connectivity)) {
			return false;
		}
		return true;
	}

	@Override
	public boolean destroy(Network network, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException {
		if (!canHandle(network, Service.Connectivity)) {
			return false;
		}
		return true;
	}

	@Override
	public boolean isReady(PhysicalNetworkServiceProvider provider) {
		return true;
	}

	@Override
	public boolean shutdownProviderInstances(
			PhysicalNetworkServiceProvider provider, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException {
		return true;
	}

	@Override
	public boolean canEnableIndividualServices() {
		return true;
	}

	@Override
	public boolean verifyServicesCombination(Set<Service> services) {
		if (!services.contains(Service.Connectivity)) {
			s_logger.warn("Unable to provide services without Connectivity service enabled for this element");
			return false;
		}

		if ((services.contains(Service.PortForwarding) || services
				.contains(Service.StaticNat))
				&& !services.contains(Service.SourceNat)) {
			s_logger.warn("Unable to provide StaticNat and/or PortForwarding without the SourceNat service");
			return false;
		}
		return true;
	}

	private static Map<Service, Map<Capability, String>> setCapabilities() {
		Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();

		// we need L3 support for coding L3 services in next period

		// L2 Support : SDN provisioning
		capabilities.put(Service.Connectivity, null);

		// L3 Support : Generic?
		capabilities.put(Service.Gateway, null);

		// L3 Support : SourceNat
		Map<Capability, String> sourceNatCapabilities = new HashMap<Capability, String>();
		sourceNatCapabilities.put(Capability.SupportedSourceNatTypes,
				"peraccount");
		sourceNatCapabilities.put(Capability.RedundantRouter, "false");
		capabilities.put(Service.SourceNat, sourceNatCapabilities);

		// L3 Support : Port Forwarding
		capabilities.put(Service.PortForwarding, null);

		// L3 support : StaticNat
		capabilities.put(Service.StaticNat, null);

		return capabilities;
	}

	@Override
	public List<Class<?>> getCommands() {
		return null;
	}

	@Override
	public HostVO createHostVOForConnectedAgent(HostVO host,
			StartupCommand[] cmd) {
		return null;
	}

	@Override
	public HostVO createHostVOForDirectConnectAgent(HostVO host,
			StartupCommand[] startup, ServerResource resource,
			Map<String, String> details, List<String> hostTags) {
		if (!(startup[0] instanceof StartupOvsCommand)) {
			return null;
		}
		host.setType(Host.Type.L2Networking);
		return host;
	}

	@Override
	public DeleteHostAnswer deleteHost(HostVO host, boolean isForced,
			boolean isForceDeleteStorage) throws UnableDeleteHostException {
		if (!(host.getType() == Host.Type.L2Networking)) {
			return null;
		}
		return new DeleteHostAnswer(true);
	}

	// TODO: Adding L3 services below
	@Override
	public IpDeployer getIpDeployer(Network network) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean applyIps(Network network,
			List<? extends PublicIpAddress> ipAddress, Set<Service> services)
			throws ResourceUnavailableException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean applyStaticNats(Network config,
			List<? extends StaticNat> rules)
			throws ResourceUnavailableException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean applyPFRules(Network network, List<PortForwardingRule> rules)
			throws ResourceUnavailableException {
		// TODO Auto-generated method stub
		return false;
	}
}
