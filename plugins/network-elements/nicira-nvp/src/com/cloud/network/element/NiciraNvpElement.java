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
/** Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package com.cloud.network.element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.CreateLogicalRouterAnswer;
import com.cloud.agent.api.CreateLogicalRouterCommand;
import com.cloud.agent.api.CreateLogicalSwitchPortAnswer;
import com.cloud.agent.api.CreateLogicalSwitchPortCommand;
import com.cloud.agent.api.DeleteLogicalRouterAnswer;
import com.cloud.agent.api.DeleteLogicalRouterCommand;
import com.cloud.agent.api.DeleteLogicalSwitchPortAnswer;
import com.cloud.agent.api.DeleteLogicalSwitchPortCommand;
import com.cloud.agent.api.FindLogicalSwitchPortAnswer;
import com.cloud.agent.api.FindLogicalSwitchPortCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupNiciraNvpCommand;
import com.cloud.agent.api.UpdateLogicalSwitchPortAnswer;
import com.cloud.agent.api.UpdateLogicalSwitchPortCommand;
import com.cloud.api.commands.AddNiciraNvpDeviceCmd;
import com.cloud.api.commands.DeleteNiciraNvpDeviceCmd;
import com.cloud.api.commands.ListNiciraNvpDeviceNetworksCmd;
import com.cloud.api.commands.ListNiciraNvpDevicesCmd;
import com.cloud.api.response.NiciraNvpDeviceResponse;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.Vlan;
import com.cloud.dc.dao.VlanDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.DetailVO;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.Network;
import com.cloud.network.ExternalNetworkDeviceManager.NetworkDevice;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkVO;
import com.cloud.network.Networks;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.NetworkManager;
import com.cloud.network.NiciraNvpDeviceVO;
import com.cloud.network.NiciraNvpNicMappingVO;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PhysicalNetworkVO;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NiciraNvpDao;
import com.cloud.network.dao.NiciraNvpNicMappingDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.guru.NiciraNvpGuestNetworkGuru;
import com.cloud.network.resource.NiciraNvpResource;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceState;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;

@Local(value = NetworkElement.class)
public class NiciraNvpElement extends AdapterBase implements
		ConnectivityProvider, SourceNatServiceProvider,
		PortForwardingServiceProvider, StaticNatServiceProvider,
		NiciraNvpElementService, ResourceStateAdapter, IpDeployer {
	private static final Logger s_logger = Logger
			.getLogger(NiciraNvpElement.class);

	private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

	@Inject
	NicDao _nicDao;
	@Inject
	ResourceManager _resourceMgr;
	@Inject
	PhysicalNetworkDao _physicalNetworkDao;
	@Inject
	PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
	@Inject
	NiciraNvpDao _niciraNvpDao;
	@Inject
	HostDetailsDao _hostDetailsDao;
	@Inject
	HostDao _hostDao;
	@Inject
	AgentManager _agentMgr;
	@Inject
	NiciraNvpNicMappingDao _niciraNvpNicMappingDao;
	@Inject
	NetworkDao _networkDao;
	@Inject
	NetworkManager _networkManager;
	@Inject
	ConfigurationManager _configMgr;
	@Inject
	NetworkServiceMapDao _ntwkSrvcDao;
	@Inject
	VlanDao _vlanDao;

	@Override
	public Map<Service, Map<Capability, String>> getCapabilities() {
		return capabilities;
	}

	@Override
	public Provider getProvider() {
		return Provider.NiciraNvp;
	}

	private boolean canHandle(Network network, Service service) {
		s_logger.debug("Checking if NiciraNvpElement can handle service "
				+ service.getName() + " on network " + network.getDisplayText());
		if (network.getBroadcastDomainType() != BroadcastDomainType.Lswitch) {
			return false;
		}

		if (!_networkManager.isProviderForNetwork(getProvider(),
				network.getId())) {
			s_logger.debug("NiciraNvpElement is not a provider for network "
					+ network.getDisplayText());
			return false;
		}

		if (!_ntwkSrvcDao.canProviderSupportServiceInNetwork(network.getId(),
				service, Network.Provider.NiciraNvp)) {
			s_logger.debug("NiciraNvpElement can't provide the "
					+ service.getName() + " service on network "
					+ network.getDisplayText());
			return false;
		}

		return true;
	}

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		super.configure(name, params);
		_resourceMgr.registerResourceStateAdapter(this.getClass()
				.getSimpleName(), this);
		return true;
	}

	@Override
	public boolean implement(Network network, NetworkOffering offering,
			DeployDestination dest, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException,
			InsufficientCapacityException {
		s_logger.debug("entering NiciraNvpElement implement function for network "
				+ network.getDisplayText()
				+ " (state "
				+ network.getState()
				+ ")");

		if (!canHandle(network, Service.Connectivity)) {
			return false;
		}

		if (network.getBroadcastUri() == null) {
			s_logger.error("Nic has no broadcast Uri with the LSwitch Uuid");
			return false;
		}

		List<NiciraNvpDeviceVO> devices = _niciraNvpDao
				.listByPhysicalNetwork(network.getPhysicalNetworkId());
		if (devices.isEmpty()) {
			s_logger.error("No NiciraNvp Controller on physical network "
					+ network.getPhysicalNetworkId());
			return false;
		}
		NiciraNvpDeviceVO niciraNvpDevice = devices.get(0);
		HostVO niciraNvpHost = _hostDao.findById(niciraNvpDevice.getHostId());
		_hostDao.loadDetails(niciraNvpHost);

		Account owner = context.getAccount();

		/**
		 * Lock the network as we might need to do multiple operations that
		 * should be done only once.
		 */
		Network lock = _networkDao.acquireInLockTable(network.getId(),
				_networkManager.getNetworkLockTimeout());
		if (lock == null) {
			throw new ConcurrentOperationException("Unable to lock network "
					+ network.getId());
		}
		try {
			if (_networkManager.isProviderSupportServiceInNetwork(
					network.getId(), Service.SourceNat, Provider.NiciraNvp)) {
				s_logger.debug("Apparently we are supposed to provide SourceNat on this network");

				PublicIp sourceNatIp = _networkManager
						.assignSourceNatIpAddressToGuestNetwork(owner, network);
				String publicCidr = sourceNatIp.getAddress().addr() + "/"
						+ NetUtils.getCidrSize(sourceNatIp.getVlanNetmask());
				String internalCidr = network.getGateway() + "/"
						+ network.getCidr().split("/")[1];
				long vlanid = (Vlan.UNTAGGED.equals(sourceNatIp.getVlanTag())) ? 0
						: Long.parseLong(sourceNatIp.getVlanTag());

				CreateLogicalRouterCommand cmd = new CreateLogicalRouterCommand(
						niciraNvpHost.getDetail("l3gatewayserviceuuid"), vlanid,
						network.getBroadcastUri().getSchemeSpecificPart(),
						"router-" + network.getDisplayText(), publicCidr,
						sourceNatIp.getGateway(), internalCidr, context
								.getDomain().getName()
								+ "-"
								+ context.getAccount().getAccountName());
				CreateLogicalRouterAnswer answer = (CreateLogicalRouterAnswer) _agentMgr
						.easySend(niciraNvpHost.getId(), cmd);
				if (answer.getResult() == false) {
					s_logger.error("Failed to create Logical Router for network "
							+ network.getDisplayText());
					return false;
				}

			}
		} finally {
			if (lock != null) {
				_networkDao.releaseFromLockTable(lock.getId());
				if (s_logger.isDebugEnabled()) {
					s_logger.debug("Lock is released for network id "
							+ lock.getId() + " as a part of router startup in "
							+ dest);
				}
			}
		}
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

		if (network.getBroadcastUri() == null) {
			s_logger.error("Nic has no broadcast Uri with the LSwitch Uuid");
			return false;
		}

		NicVO nicVO = _nicDao.findById(nic.getId());

		List<NiciraNvpDeviceVO> devices = _niciraNvpDao
				.listByPhysicalNetwork(network.getPhysicalNetworkId());
		if (devices.isEmpty()) {
			s_logger.error("No NiciraNvp Controller on physical network "
					+ network.getPhysicalNetworkId());
			return false;
		}
		NiciraNvpDeviceVO niciraNvpDevice = devices.get(0);
		HostVO niciraNvpHost = _hostDao.findById(niciraNvpDevice.getHostId());

		NiciraNvpNicMappingVO existingNicMap = _niciraNvpNicMappingDao
				.findByNicUuid(nicVO.getUuid());
		if (existingNicMap != null) {
			FindLogicalSwitchPortCommand findCmd = new FindLogicalSwitchPortCommand(
					existingNicMap.getLogicalSwitchUuid(),
					existingNicMap.getLogicalSwitchPortUuid());
			FindLogicalSwitchPortAnswer answer = (FindLogicalSwitchPortAnswer) _agentMgr
					.easySend(niciraNvpHost.getId(), findCmd);

			if (answer.getResult()) {
				s_logger.warn("Existing Logical Switchport found for nic "
						+ nic.getName() + " with uuid "
						+ existingNicMap.getLogicalSwitchPortUuid());
				UpdateLogicalSwitchPortCommand cmd = new UpdateLogicalSwitchPortCommand(
						existingNicMap.getLogicalSwitchPortUuid(), network
								.getBroadcastUri().getSchemeSpecificPart(),
						nicVO.getUuid(), context.getDomain().getName() + "-"
								+ context.getAccount().getAccountName(),
						nic.getName());
				_agentMgr.easySend(niciraNvpHost.getId(), cmd);
				return true;
			} else {
				s_logger.error("Stale entry found for nic " + nic.getName()
						+ " with logical switchport uuid "
						+ existingNicMap.getLogicalSwitchPortUuid());
				_niciraNvpNicMappingDao.remove(existingNicMap.getId());
			}
		}

		CreateLogicalSwitchPortCommand cmd = new CreateLogicalSwitchPortCommand(
				network.getBroadcastUri().getSchemeSpecificPart(),
				nicVO.getUuid(), context.getDomain().getName() + "-"
						+ context.getAccount().getAccountName(), nic.getName());
		CreateLogicalSwitchPortAnswer answer = (CreateLogicalSwitchPortAnswer) _agentMgr
				.easySend(niciraNvpHost.getId(), cmd);

		if (answer == null || !answer.getResult()) {
			s_logger.error("CreateLogicalSwitchPortCommand failed");
			return false;
		}

		NiciraNvpNicMappingVO nicMap = new NiciraNvpNicMappingVO(network
				.getBroadcastUri().getSchemeSpecificPart(),
				answer.getLogicalSwitchPortUuid(), nicVO.getUuid());
		_niciraNvpNicMappingDao.persist(nicMap);

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

		if (network.getBroadcastUri() == null) {
			s_logger.error("Nic has no broadcast Uri with the LSwitch Uuid");
			return false;
		}

		NicVO nicVO = _nicDao.findById(nic.getId());

		List<NiciraNvpDeviceVO> devices = _niciraNvpDao
				.listByPhysicalNetwork(network.getPhysicalNetworkId());
		if (devices.isEmpty()) {
			s_logger.error("No NiciraNvp Controller on physical network "
					+ network.getPhysicalNetworkId());
			return false;
		}
		NiciraNvpDeviceVO niciraNvpDevice = devices.get(0);
		HostVO niciraNvpHost = _hostDao.findById(niciraNvpDevice.getHostId());

		NiciraNvpNicMappingVO nicMap = _niciraNvpNicMappingDao
				.findByNicUuid(nicVO.getUuid());
		if (nicMap == null) {
			s_logger.error("No mapping for nic " + nic.getName());
			return false;
		}

		DeleteLogicalSwitchPortCommand cmd = new DeleteLogicalSwitchPortCommand(
				nicMap.getLogicalSwitchUuid(),
				nicMap.getLogicalSwitchPortUuid());
		DeleteLogicalSwitchPortAnswer answer = (DeleteLogicalSwitchPortAnswer) _agentMgr
				.easySend(niciraNvpHost.getId(), cmd);

		if (answer == null || !answer.getResult()) {
			s_logger.error("DeleteLogicalSwitchPortCommand failed");
			return false;
		}

		_niciraNvpNicMappingDao.remove(nicMap.getId());

		return true;
	}

	@Override
	public boolean shutdown(Network network, ReservationContext context,
			boolean cleanup) throws ConcurrentOperationException,
			ResourceUnavailableException {
		if (!canHandle(network, Service.Connectivity)) {
			return false;
		}

		List<NiciraNvpDeviceVO> devices = _niciraNvpDao
				.listByPhysicalNetwork(network.getPhysicalNetworkId());
		if (devices.isEmpty()) {
			s_logger.error("No NiciraNvp Controller on physical network "
					+ network.getPhysicalNetworkId());
			return false;
		}
		NiciraNvpDeviceVO niciraNvpDevice = devices.get(0);
		HostVO niciraNvpHost = _hostDao.findById(niciraNvpDevice.getHostId());

		if (_networkManager.isProviderSupportServiceInNetwork(network.getId(),
				Service.SourceNat, Provider.NiciraNvp)) {
			s_logger.debug("Apparently we were providing SourceNat on this network");

			// Deleting the LogicalRouter will also take care of all provisioned
			// nat rules.
			/*
			 * DeleteLogicalRouterCommand cmd = new
			 * DeleteLogicalRouterCommand(""); DeleteLogicalRouterAnswer answer
			 * = (DeleteLogicalRouterAnswer)
			 * _agentMgr.easySend(niciraNvpHost.getId(), cmd); if
			 * (answer.getResult() == false) {
			 * s_logger.error("Failed to delete LogicalRouter for network " +
			 * network.getDisplayText()); return false; }
			 */}

		return true;
	}

	@Override
	public boolean destroy(Network network)
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
		// Nothing to do here.
		return true;
	}

	@Override
	public boolean canEnableIndividualServices() {
		return true;
	}

	@Override
	public boolean verifyServicesCombination(Set<Service> services) {
		// This element can only function in a Nicra Nvp based
		// SDN network, so Connectivity needs to be present here
		if (services.contains(Service.Connectivity)) {
			return true;
		}
		s_logger.debug("Unable to provide services without Connectivity service enabled for this element");
		return false;
	}

	private static Map<Service, Map<Capability, String>> setCapabilities() {
		Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();

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
	public String getPropertiesFile() {
		return "nicira-nvp_commands.properties";
	}

	@Override
	@DB
	public NiciraNvpDeviceVO addNiciraNvpDevice(AddNiciraNvpDeviceCmd cmd) {
		ServerResource resource = new NiciraNvpResource();
		String deviceName = Network.Provider.NiciraNvp.getName();
		NetworkDevice networkDevice = NetworkDevice
				.getNetworkDevice(deviceName);
		Long physicalNetworkId = cmd.getPhysicalNetworkId();
		NiciraNvpDeviceVO niciraNvpDevice = null;

		PhysicalNetworkVO physicalNetwork = _physicalNetworkDao
				.findById(physicalNetworkId);
		if (physicalNetwork == null) {
			throw new InvalidParameterValueException(
					"Could not find phyical network with ID: "
							+ physicalNetworkId);
		}
		long zoneId = physicalNetwork.getDataCenterId();

		PhysicalNetworkServiceProviderVO ntwkSvcProvider = _physicalNetworkServiceProviderDao
				.findByServiceProvider(physicalNetwork.getId(),
						networkDevice.getNetworkServiceProvder());
		if (ntwkSvcProvider == null) {
			throw new CloudRuntimeException("Network Service Provider: "
					+ networkDevice.getNetworkServiceProvder()
					+ " is not enabled in the physical network: "
					+ physicalNetworkId + "to add this device");
		} else if (ntwkSvcProvider.getState() == PhysicalNetworkServiceProvider.State.Shutdown) {
			throw new CloudRuntimeException("Network Service Provider: "
					+ ntwkSvcProvider.getProviderName()
					+ " is in shutdown state in the physical network: "
					+ physicalNetworkId + "to add this device");
		}

		if (_niciraNvpDao.listByPhysicalNetwork(physicalNetworkId).size() != 0) {
			throw new CloudRuntimeException(
					"A NiciraNvp device is already configured on this physical network");
		}

		Map<String, String> params = new HashMap<String, String>();
		params.put("guid", UUID.randomUUID().toString());
		params.put("zoneId", String.valueOf(physicalNetwork.getDataCenterId()));
		params.put("physicalNetworkId", String.valueOf(physicalNetwork.getId()));
		params.put("name", "Nicira Controller - " + cmd.getHost());
		params.put("ip", cmd.getHost());
		params.put("adminuser", cmd.getUsername());
		params.put("adminpass", cmd.getPassword());
		params.put("transportzoneuuid", cmd.getTransportzoneUuid());
		// FIXME What to do with multiple isolation types
		params.put("transportzoneisotype", 
				physicalNetwork.getIsolationMethods().get(0).toLowerCase()); 
		if (cmd.getL3GatewayServiceUuid() != null) {
			params.put("l3gatewayserviceuuid", cmd.getL3GatewayServiceUuid());
		}

		Map<String, Object> hostdetails = new HashMap<String, Object>();
		hostdetails.putAll(params);

		Transaction txn = Transaction.currentTxn();
		try {
			resource.configure(cmd.getHost(), hostdetails);

			Host host = _resourceMgr.addHost(zoneId, resource,
					Host.Type.L2Networking, params);
			if (host != null) {
				txn.start();

				niciraNvpDevice = new NiciraNvpDeviceVO(host.getId(),
						physicalNetworkId, ntwkSvcProvider.getProviderName(),
						deviceName);
				_niciraNvpDao.persist(niciraNvpDevice);

				DetailVO detail = new DetailVO(host.getId(),
						"niciranvpdeviceid", String.valueOf(niciraNvpDevice
								.getId()));
				_hostDetailsDao.persist(detail);

				txn.commit();
				return niciraNvpDevice;
			} else {
				throw new CloudRuntimeException(
						"Failed to add Nicira Nvp Device due to internal error.");
			}
		} catch (ConfigurationException e) {
			txn.rollback();
			throw new CloudRuntimeException(e.getMessage());
		}
	}

	@Override
	public NiciraNvpDeviceResponse createNiciraNvpDeviceResponse(
			NiciraNvpDeviceVO niciraNvpDeviceVO) {
		NiciraNvpDeviceResponse response = new NiciraNvpDeviceResponse();
		response.setDeviceName(niciraNvpDeviceVO.getDeviceName());
		response.setPhysicalNetworkId(niciraNvpDeviceVO.getPhysicalNetworkId());
		response.setId(niciraNvpDeviceVO.getId());
		response.setProviderName(niciraNvpDeviceVO.getProviderName());
		return response;
	}

	@Override
	public boolean deleteNiciraNvpDevice(DeleteNiciraNvpDeviceCmd cmd) {
		Long niciraDeviceId = cmd.getNiciraNvpDeviceId();
		NiciraNvpDeviceVO niciraNvpDevice = _niciraNvpDao
				.findById(niciraDeviceId);
		if (niciraNvpDevice == null) {
			throw new InvalidParameterValueException(
					"Could not find a nicira device with id " + niciraDeviceId);
		}

		// Find the physical network we work for
		Long physicalNetworkId = niciraNvpDevice.getPhysicalNetworkId();
		PhysicalNetworkVO physicalNetwork = _physicalNetworkDao
				.findById(physicalNetworkId);
		if (physicalNetwork != null) {
			// Lets see if there are networks that use us
			// Find the nicira networks on this physical network
			List<NetworkVO> networkList = _networkDao
					.listByPhysicalNetwork(physicalNetworkId);

			// Networks with broadcast type lswitch are ours
			for (NetworkVO network : networkList) {
				if (network.getBroadcastDomainType() == Networks.BroadcastDomainType.Lswitch) {
					if ((network.getState() != Network.State.Shutdown)
							&& (network.getState() != Network.State.Destroy)) {
						throw new CloudRuntimeException(
								"This Nicira Nvp device can not be deleted as there are one or more logical networks provisioned by cloudstack.");
					}
				}
			}
		}

		HostVO niciraHost = _hostDao.findById(niciraNvpDevice.getHostId());
		Long hostId = niciraHost.getId();

		niciraHost.setResourceState(ResourceState.Maintenance);
		_hostDao.update(hostId, niciraHost);
		_resourceMgr.deleteHost(hostId, false, false);

		_niciraNvpDao.remove(niciraDeviceId);

		return true;
	}

	@Override
	public List<NiciraNvpDeviceVO> listNiciraNvpDevices(
			ListNiciraNvpDevicesCmd cmd) {
		Long physicalNetworkId = cmd.getPhysicalNetworkId();
		Long niciraNvpDeviceId = cmd.getNiciraNvpDeviceId();
		List<NiciraNvpDeviceVO> responseList = new ArrayList<NiciraNvpDeviceVO>();

		if (physicalNetworkId == null && niciraNvpDeviceId == null) {
			throw new InvalidParameterValueException(
					"Either physical network Id or nicira device Id must be specified");
		}

		if (niciraNvpDeviceId != null) {
			NiciraNvpDeviceVO niciraNvpDevice = _niciraNvpDao
					.findById(niciraNvpDeviceId);
			if (niciraNvpDevice == null) {
				throw new InvalidParameterValueException(
						"Could not find Nicira Nvp device with id: "
								+ niciraNvpDevice);
			}
			responseList.add(niciraNvpDevice);
		} else {
			PhysicalNetworkVO physicalNetwork = _physicalNetworkDao
					.findById(physicalNetworkId);
			if (physicalNetwork == null) {
				throw new InvalidParameterValueException(
						"Could not find a physical network with id: "
								+ physicalNetworkId);
			}
			responseList = _niciraNvpDao
					.listByPhysicalNetwork(physicalNetworkId);
		}

		return responseList;
	}

	@Override
	public List<? extends Network> listNiciraNvpDeviceNetworks(
			ListNiciraNvpDeviceNetworksCmd cmd) {
		Long niciraDeviceId = cmd.getNiciraNvpDeviceId();
		NiciraNvpDeviceVO niciraNvpDevice = _niciraNvpDao
				.findById(niciraDeviceId);
		if (niciraNvpDevice == null) {
			throw new InvalidParameterValueException(
					"Could not find a nicira device with id " + niciraDeviceId);
		}

		// Find the physical network we work for
		Long physicalNetworkId = niciraNvpDevice.getPhysicalNetworkId();
		PhysicalNetworkVO physicalNetwork = _physicalNetworkDao
				.findById(physicalNetworkId);
		if (physicalNetwork == null) {
			// No such physical network, so no provisioned networks
			return Collections.emptyList();
		}

		// Find the nicira networks on this physical network
		List<NetworkVO> networkList = _networkDao
				.listByPhysicalNetwork(physicalNetworkId);

		// Networks with broadcast type lswitch are ours
		List<NetworkVO> responseList = new ArrayList<NetworkVO>();
		for (NetworkVO network : networkList) {
			if (network.getBroadcastDomainType() == Networks.BroadcastDomainType.Lswitch) {
				responseList.add(network);
			}
		}

		return responseList;
	}

	@Override
	public HostVO createHostVOForConnectedAgent(HostVO host,
			StartupCommand[] cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HostVO createHostVOForDirectConnectAgent(HostVO host,
			StartupCommand[] startup, ServerResource resource,
			Map<String, String> details, List<String> hostTags) {
		if (!(startup[0] instanceof StartupNiciraNvpCommand)) {
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

	/**
	 * From interface SourceNatServiceProvider
	 */
	@Override
	public IpDeployer getIpDeployer(Network network) {
		return this;
	}

	/**
	 * From interface IpDeployer
	 * 
	 * @param network
	 * @param ipAddress
	 * @param services
	 * @return
	 * @throws ResourceUnavailableException
	 */
	@Override
	public boolean applyIps(Network network,
			List<? extends PublicIpAddress> ipAddress, Set<Service> services)
			throws ResourceUnavailableException {
		s_logger.debug("Entering applyIps"); // TODO Remove this line
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * From interface StaticNatServiceProvider
	 */
	@Override
	public boolean applyStaticNats(Network config,
			List<? extends StaticNat> rules)
			throws ResourceUnavailableException {
		s_logger.debug("Entering applyStaticNats"); // TODO Remove this line
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * From interface PortForwardingServiceProvider
	 */
	@Override
	public boolean applyPFRules(Network network, List<PortForwardingRule> rules)
			throws ResourceUnavailableException {
		s_logger.debug("Entering applyPFRules"); // TODO Remove this line
		// TODO Auto-generated method stub
		return false;
	}

}
