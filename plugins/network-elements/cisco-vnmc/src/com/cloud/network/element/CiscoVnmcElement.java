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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.network.ExternalNetworkDeviceManager.NetworkDevice;
import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ConfigureNexusVsmForAsaCommand;
import com.cloud.agent.api.CreateLogicalEdgeFirewallCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupExternalFirewallCommand;
import com.cloud.api.commands.AddCiscoVnmcResourceCmd;
import com.cloud.api.commands.DeleteCiscoVnmcResourceCmd;
import com.cloud.api.commands.ListCiscoVnmcResourcesCmd;
import com.cloud.api.response.CiscoVnmcResourceResponse;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.ClusterVSMMapVO;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.ClusterVSMMapDao;
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
import com.cloud.network.CiscoNexusVSMDeviceVO;
import com.cloud.network.Network;
import com.cloud.network.NetworkManager;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PhysicalNetworkVO;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.cisco.CiscoVnmcConnection;
import com.cloud.network.cisco.CiscoVnmcController;
import com.cloud.network.cisco.CiscoVnmcControllerVO;
import com.cloud.network.dao.CiscoNexusVSMDeviceDao;
import com.cloud.network.dao.CiscoVnmcDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderVO;
import com.cloud.network.resource.CiscoVnmcResource;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = NetworkElement.class)
public class CiscoVnmcElement extends AdapterBase implements SourceNatServiceProvider, FirewallServiceProvider,
    PortForwardingServiceProvider, IpDeployer, StaticNatServiceProvider, ResourceStateAdapter, NetworkElement, CiscoVnmcElementService {
	private static final Logger s_logger = Logger.getLogger(CiscoVnmcElement.class);
    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    @Inject
    AgentManager _agentMgr;
    @Inject
	ResourceManager _resourceMgr;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    NetworkManager _networkMgr;

    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    PhysicalNetworkServiceProviderDao _physicalNetworkServiceProviderDao;
    @Inject 
    HostDetailsDao _hostDetailsDao;
    @Inject
    HostDao _hostDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    ClusterDao _clusterDao;
    @Inject
    ClusterVSMMapDao _clusterVsmMapDao;
    @Inject
    CiscoNexusVSMDeviceDao _vsmDeviceDao;
    @Inject
    CiscoVnmcDao _ciscoVnmcDao;

    CiscoVnmcConnection _vnmcConnection;

    private boolean canHandle(Network network) {
        if (network.getBroadcastDomainType() != BroadcastDomainType.Vlan) {
            return false; //TODO: should handle VxLAN as well
        }

        return true;        
    }

    @Override
    public boolean configure(String name, Map<String, Object> params)
            throws ConfigurationException {
        super.configure(name, params);
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();
        capabilities.put(Service.Gateway, null);

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
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }

    @Override
    public Provider getProvider() {
        return Provider.CiscoVnmc;
    }

    private boolean createLogicalEdgeFirewall(long vlanId, String gateway,
            String publicIp, long hostId) {
        CreateLogicalEdgeFirewallCommand cmd = new CreateLogicalEdgeFirewallCommand(vlanId, publicIp, gateway, "255.255.255.0", "255.255.255.0");
        Answer answer = _agentMgr.easySend(hostId, cmd);
        return answer.getResult();
    }

    private boolean configureNexusVsmForAsa(long vlanId, String gateway,
            String vsmUsername, String vsmPassword, String vsmIp,
            String asaInPortProfile, long hostId) {
        ConfigureNexusVsmForAsaCommand cmd = new ConfigureNexusVsmForAsaCommand(vlanId, gateway, vsmUsername, vsmPassword, vsmIp, asaInPortProfile);
        Answer answer = _agentMgr.easySend(hostId, cmd);
        return answer.getResult();
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering,
    	    DeployDestination dest, ReservationContext context)
    	    throws ConcurrentOperationException, ResourceUnavailableException,
    	    InsufficientCapacityException {
        DataCenter zone = _configMgr.getZone(network.getDataCenterId());

        if (zone.getNetworkType() == NetworkType.Basic) {
            s_logger.debug("Not handling network implement in zone of type " + NetworkType.Basic);
            return false;
        }

        if (!canHandle(network)) {
            return false;
        }

        List<ClusterVO> clusters = _clusterDao.listByDcHyType(zone.getId(), "VMware");
        if (clusters.size() > 1) { //TODO: Actually zone should only have single Vmware cluster and no other HV clusters as Vnmc/Asa1kv requires N1kv switch
            s_logger.error("Zone " + zone.getName() + " has multiple Vmware clusters, Cisco Vnmc device requires that zone has a single Vmware cluster");
            return false;
        }

        ClusterVSMMapVO clusterVsmMap = _clusterVsmMapDao.findByClusterId(clusters.get(0).getId());
        if (clusterVsmMap == null) {
            s_logger.error("Vmware cluster " + clusters.get(0).getName() + " has no Cisco Nexus VSM device associated with it");
            return false;
        }

        CiscoNexusVSMDeviceVO vsmDevice = _vsmDeviceDao.findById(clusterVsmMap.getVsmId());
        if (vsmDevice == null) {
            s_logger.error("Unable to load details of Cisco Nexus VSM device associated with cluster " + clusters.get(0).getName());
            return false;
        }

        List<CiscoVnmcControllerVO> devices = _ciscoVnmcDao.listByPhysicalNetwork(network.getPhysicalNetworkId());
        if (devices.isEmpty()) {
            s_logger.error("No Cisco Vnmc device on network " + network.getDisplayText());
            return false;
        }

        if (!_networkMgr.isProviderSupportServiceInNetwork(network.getId(), Service.SourceNat, Provider.CiscoVnmc)) {
            s_logger.error("SourceNat service is not provided by Cisco Vnmc device on network " + network.getDisplayText());
            return false;
        }

        CiscoVnmcControllerVO ciscoVnmcDevice = devices.get(0);
        HostVO ciscoVnmcHost = _hostDao.findById(ciscoVnmcDevice.getHostId());
        _hostDao.loadDetails(ciscoVnmcHost);
        Account owner = context.getAccount();
        PublicIp sourceNatIp = _networkMgr.assignSourceNatIpAddressToGuestNetwork(owner, network);
        String vlan = network.getBroadcastUri().getHost();
        long vlanId = Long.parseLong(vlan);

        // create logical edge firewall in VNMC
        if (!createLogicalEdgeFirewall(vlanId, network.getGateway(), sourceNatIp.getAddress().addr(), ciscoVnmcHost.getId())) {
            s_logger.error("Failed to create logical edge firewall in Cisco Vnmc device for network " + network.getDisplayText());
            return false;
        }

        // create stuff in VSM for ASA device
        if (!configureNexusVsmForAsa(vlanId, network.getGateway(),
                vsmDevice.getUserName(), vsmDevice.getPassword(), vsmDevice.getipaddr(),
                "insidePortProfile" /*FIXME: read it from asa1kv device table*/, ciscoVnmcHost.getId())) {
            s_logger.error("Failed to configure Cisco Nexus VSM " + vsmDevice.getipaddr() + " for ASA device for network " + network.getDisplayText());
            return false;
        }

        // ensure that there is an ASA 1000v assigned to this network
        assignAsa1000vToNetwork(network);
        return true;
    }

	@Override
	public boolean prepare(Network network, NicProfile nic,
			VirtualMachineProfile<? extends VirtualMachine> vm,
			DeployDestination dest, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException,
			InsufficientCapacityException {
		//Ensure that there is an ASA 1000v assigned to this network
		return true;
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
		return true;
	}

	@Override
	public boolean verifyServicesCombination(Set<Service> services) {
        if (!services.contains(Service.Firewall)) {
            s_logger.warn("CiscoVnmc must be used as Firewall Service Provider in the network");
            return false;
        }
        return true;
	}

	@Override
	public boolean applyFWRules(Network network,
			List<? extends FirewallRule> rules)
			throws ResourceUnavailableException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean destroy(Network network, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<Class<?>> getCommands() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CiscoVnmcController addCiscoVnmcResource(AddCiscoVnmcResourceCmd cmd) {
        String deviceName = Provider.CiscoVnmc.getName();
        NetworkDevice networkDevice = NetworkDevice.getNetworkDevice(deviceName);
        Long physicalNetworkId = cmd.getPhysicalNetworkId();
        CiscoVnmcController ciscoVnmcResource = null;

        PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
        if (physicalNetwork == null) {
            throw new InvalidParameterValueException("Could not find phyical network with ID: " + physicalNetworkId);
        }
        long zoneId = physicalNetwork.getDataCenterId();

        PhysicalNetworkServiceProviderVO ntwkSvcProvider = _physicalNetworkServiceProviderDao.findByServiceProvider(physicalNetwork.getId(), networkDevice.getNetworkServiceProvder());
        if (ntwkSvcProvider == null) {
            throw new CloudRuntimeException("Network Service Provider: " + networkDevice.getNetworkServiceProvder() +
                    " is not enabled in the physical network: " + physicalNetworkId + "to add this device");
        } else if (ntwkSvcProvider.getState() == PhysicalNetworkServiceProvider.State.Shutdown) {
            throw new CloudRuntimeException("Network Service Provider: " + ntwkSvcProvider.getProviderName() +
                    " is in shutdown state in the physical network: " + physicalNetworkId + "to add this device");
        }

        if (_ciscoVnmcDao.listByPhysicalNetwork(physicalNetworkId).size() != 0) {
            throw new CloudRuntimeException("A Cisco Vnmc device is already configured on this physical network");
        }

        Map<String, String> params = new HashMap<String,String>();
        params.put("guid", UUID.randomUUID().toString());
        params.put("zoneId", String.valueOf(physicalNetwork.getDataCenterId()));
        params.put("physicalNetworkId", String.valueOf(physicalNetwork.getId()));
        params.put("name", "Cisco VNMC Controller - " + cmd.getHost());
        params.put("ip", cmd.getHost());
        params.put("username", cmd.getUsername());
        params.put("password", cmd.getPassword());
        params.put("transportzoneisotype", physicalNetwork.getIsolationMethods().get(0).toLowerCase()); // FIXME What to do with multiple isolation types

        Map<String, Object> hostdetails = new HashMap<String,Object>();
        hostdetails.putAll(params);

		ServerResource resource = new CiscoVnmcResource();
        Transaction txn = Transaction.currentTxn();
        try {
            resource.configure(cmd.getHost(), hostdetails);

            Host host = _resourceMgr.addHost(zoneId, resource, Host.Type.ExternalFirewall, params);
            if (host != null) {
                txn.start();

                ciscoVnmcResource = new CiscoVnmcControllerVO(host.getId(), physicalNetworkId, ntwkSvcProvider.getProviderName(), deviceName);
                _ciscoVnmcDao.persist((CiscoVnmcControllerVO)ciscoVnmcResource);
                
                DetailVO detail = new DetailVO(host.getId(), "deviceid", String.valueOf(ciscoVnmcResource.getId()));
                _hostDetailsDao.persist(detail);

                txn.commit();
                return ciscoVnmcResource;
            } else {
                throw new CloudRuntimeException("Failed to add Cisco Vnmc device due to internal error.");
            }
        } catch (ConfigurationException e) {
            txn.rollback();
            throw new CloudRuntimeException(e.getMessage());
        }
    }

	@Override
	public CiscoVnmcResourceResponse createCiscoVnmcResourceResponse(
			CiscoVnmcController ciscoVnmcResourceVO) {
		HostVO ciscoVnmcHost = _hostDao.findById(ciscoVnmcResourceVO.getHostId());

		CiscoVnmcResourceResponse response = new CiscoVnmcResourceResponse();
		response.setId(ciscoVnmcResourceVO.getUuid());
		response.setPhysicalNetworkId(ciscoVnmcResourceVO.getPhysicalNetworkId());
		response.setProviderName(ciscoVnmcResourceVO.getProviderName());
		response.setResourceName(ciscoVnmcHost.getName());

		return response;
	}

	@Override
	public boolean deleteCiscoVnmcResource(DeleteCiscoVnmcResourceCmd cmd) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<CiscoVnmcControllerVO> listCiscoVnmcResources(
			ListCiscoVnmcResourcesCmd cmd) {
		Long physicalNetworkId = cmd.getPhysicalNetworkId();
		Long ciscoVnmcResourceId = cmd.getCiscoVnmcResourceId();
		List<CiscoVnmcControllerVO> responseList = new ArrayList<CiscoVnmcControllerVO>();

		if (physicalNetworkId == null && ciscoVnmcResourceId == null) {
			throw new InvalidParameterValueException("Either physical network Id or vnmc device Id must be specified");
		}

		if (ciscoVnmcResourceId != null) {
			CiscoVnmcControllerVO ciscoVnmcResource = _ciscoVnmcDao.findById(ciscoVnmcResourceId);
			if (ciscoVnmcResource == null) {
				throw new InvalidParameterValueException("Could not find Cisco Vnmc device with id: " + ciscoVnmcResource);
			}
			responseList.add(ciscoVnmcResource);
		}
		else {
			PhysicalNetworkVO physicalNetwork = _physicalNetworkDao.findById(physicalNetworkId);
			if (physicalNetwork == null) {
				throw new InvalidParameterValueException("Could not find a physical network with id: " + physicalNetworkId);
			}
			responseList = _ciscoVnmcDao.listByPhysicalNetwork(physicalNetworkId);
		}

		return responseList;
	}


	@Override
	public void assignAsa1000vToNetwork(Network network) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public IpDeployer getIpDeployer(Network network) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean applyPFRules(Network network, List<PortForwardingRule> rules)
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
	public boolean applyIps(Network network,
			List<? extends PublicIpAddress> ipAddress, Set<Service> services)
			throws ResourceUnavailableException {
		// TODO Auto-generated method stub
		return false;
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
        if (!(startup[0] instanceof StartupExternalFirewallCommand)) {
            return null;
        }
        host.setType(Host.Type.ExternalFirewall);
        return host;
	}

	@Override
	public DeleteHostAnswer deleteHost(HostVO host, boolean isForced,
			boolean isForceDeleteStorage) throws UnableDeleteHostException {
        if (host.getType() != com.cloud.host.Host.Type.ExternalFirewall) {
            return null;
        }
        return new DeleteHostAnswer(true);
	}

}
