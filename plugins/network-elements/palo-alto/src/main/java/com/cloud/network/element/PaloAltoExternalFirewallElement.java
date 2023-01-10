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

import javax.inject.Inject;


import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.network.ExternalNetworkDeviceManager.NetworkDevice;

import com.cloud.api.ApiDBUtils;
import com.cloud.api.commands.AddPaloAltoFirewallCmd;
import com.cloud.api.commands.ConfigurePaloAltoFirewallCmd;
import com.cloud.api.commands.DeletePaloAltoFirewallCmd;
import com.cloud.api.commands.ListPaloAltoFirewallNetworksCmd;
import com.cloud.api.commands.ListPaloAltoFirewallsCmd;
import com.cloud.api.response.PaloAltoFirewallResponse;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.ExternalFirewallDeviceManagerImpl;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.dao.ExternalFirewallDeviceDao;
import com.cloud.network.dao.ExternalFirewallDeviceVO;
import com.cloud.network.dao.ExternalFirewallDeviceVO.FirewallDeviceState;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkExternalFirewallDao;
import com.cloud.network.dao.NetworkExternalFirewallVO;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.resource.PaloAltoResource;
import com.cloud.network.rules.FirewallRule;
import com.cloud.network.rules.PortForwardingRule;
import com.cloud.network.rules.StaticNat;
import com.cloud.offering.NetworkOffering;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;

public class PaloAltoExternalFirewallElement extends ExternalFirewallDeviceManagerImpl implements SourceNatServiceProvider, FirewallServiceProvider,
        PortForwardingServiceProvider, IpDeployer, PaloAltoFirewallElementService, StaticNatServiceProvider {


    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    @Inject
    NetworkModel _networkManager;
    @Inject
    HostDao _hostDao;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    NetworkOfferingDao _networkOfferingDao;
    @Inject
    NetworkDao _networksDao;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    ExternalFirewallDeviceDao _fwDevicesDao;
    @Inject
    NetworkExternalFirewallDao _networkFirewallDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    HostDetailsDao _hostDetailDao;
    @Inject
    ConfigurationDao _configDao;
    @Inject
    EntityManager _entityMgr;

    private boolean canHandle(Network network, Service service) {
        DataCenter zone = _entityMgr.findById(DataCenter.class, network.getDataCenterId());
        if (zone.getNetworkType() == NetworkType.Advanced && network.getGuestType() != Network.GuestType.Isolated) {
            logger.trace("Element " + getProvider().getName() + "is not handling network type = " + network.getGuestType());
            return false;
        }

        if (service == null) {
            if (!_networkManager.isProviderForNetwork(getProvider(), network.getId())) {
                logger.trace("Element " + getProvider().getName() + " is not a provider for the network " + network);
                return false;
            }
        } else {
            if (!_networkManager.isProviderSupportServiceInNetwork(network.getId(), service, getProvider())) {
                logger.trace("Element " + getProvider().getName() + " doesn't support service " + service.getName() + " in the network " + network);
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException,
        ConcurrentOperationException, InsufficientNetworkCapacityException {
        DataCenter zone = _entityMgr.findById(DataCenter.class, network.getDataCenterId());

        // don't have to implement network is Basic zone
        if (zone.getNetworkType() == NetworkType.Basic) {
            logger.debug("Not handling network implement in zone of type " + NetworkType.Basic);
            return false;
        }

        if (!canHandle(network, null)) {
            return false;
        }

        try {
            return manageGuestNetworkWithExternalFirewall(true, network);
        } catch (InsufficientCapacityException capacityException) {
            // TODO: handle out of capacity exception in more gracefule manner when multiple providers are present for
            // the network
            logger.error("Fail to implement the Palo Alto for network " + network, capacityException);
            return false;
        }
    }

    @Override
    public boolean prepare(Network config, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws ConcurrentOperationException, InsufficientNetworkCapacityException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean release(Network config, NicProfile nic, VirtualMachineProfile vm, ReservationContext context) {
        return true;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ResourceUnavailableException, ConcurrentOperationException {
        DataCenter zone = _entityMgr.findById(DataCenter.class, network.getDataCenterId());

        // don't have to implement network is Basic zone
        if (zone.getNetworkType() == NetworkType.Basic) {
            logger.debug("Not handling network shutdown in zone of type " + NetworkType.Basic);
            return false;
        }

        if (!canHandle(network, null)) {
            return false;
        }
        try {
            return manageGuestNetworkWithExternalFirewall(false, network);
        } catch (InsufficientCapacityException capacityException) {
            // TODO: handle out of capacity exception
            return false;
        }
    }

    @Override
    public boolean destroy(Network config, ReservationContext context) {
        return true;
    }

    @Override
    public boolean applyFWRules(Network config, List<? extends FirewallRule> rules) throws ResourceUnavailableException {
        if (!canHandle(config, Service.Firewall)) {
            return false;
        }

        return applyFirewallRules(config, rules);
    }

    @Override
    public Provider getProvider() {
        return Provider.PaloAlto;
    }

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }

    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();

        // Set capabilities for Firewall service
        Map<Capability, String> firewallCapabilities = new HashMap<Capability, String>();
        firewallCapabilities.put(Capability.SupportedProtocols, "tcp,udp,icmp");
        firewallCapabilities.put(Capability.SupportedEgressProtocols, "tcp,udp,icmp,all");
        firewallCapabilities.put(Capability.MultipleIps, "true");
        firewallCapabilities.put(Capability.TrafficStatistics, "per public ip");
        firewallCapabilities.put(Capability.SupportedTrafficDirection, "ingress, egress");
        capabilities.put(Service.Firewall, firewallCapabilities);

        capabilities.put(Service.Gateway, null);

        Map<Capability, String> sourceNatCapabilities = new HashMap<Capability, String>();
        // Specifies that this element supports either one source NAT rule per account;
        sourceNatCapabilities.put(Capability.SupportedSourceNatTypes, "peraccount");
        capabilities.put(Service.SourceNat, sourceNatCapabilities);

        // Specifies that port forwarding rules are supported by this element
        capabilities.put(Service.PortForwarding, null);

        // Specifies that static NAT rules are supported by this element
        capabilities.put(Service.StaticNat, null);

        return capabilities;
    }

    @Override
    public boolean applyPFRules(Network network, List<PortForwardingRule> rules) throws ResourceUnavailableException {
        if (!canHandle(network, Service.PortForwarding)) {
            return false;
        }

        return applyPortForwardingRules(network, rules);
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {

        List<ExternalFirewallDeviceVO> fwDevices = _fwDevicesDao.listByPhysicalNetworkAndProvider(provider.getPhysicalNetworkId(), Provider.PaloAlto.getName());
        // true if at-least one Palo Alto device is added in to physical network and is in configured (in enabled state) state
        if (fwDevices != null && !fwDevices.isEmpty()) {
            for (ExternalFirewallDeviceVO fwDevice : fwDevices) {
                if (fwDevice.getDeviceState() == FirewallDeviceState.Enabled) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context) throws ConcurrentOperationException,
        ResourceUnavailableException {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return true;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(AddPaloAltoFirewallCmd.class);
        cmdList.add(ConfigurePaloAltoFirewallCmd.class);
        cmdList.add(DeletePaloAltoFirewallCmd.class);
        cmdList.add(ListPaloAltoFirewallNetworksCmd.class);
        cmdList.add(ListPaloAltoFirewallsCmd.class);
        return cmdList;
    }

    @Override
    public ExternalFirewallDeviceVO addPaloAltoFirewall(AddPaloAltoFirewallCmd cmd) {
        String deviceName = cmd.getDeviceType();
        if (!deviceName.equalsIgnoreCase(NetworkDevice.PaloAltoFirewall.getName())) {
            throw new InvalidParameterValueException("Invalid Palo Alto firewall device type");
        }
        return addExternalFirewall(cmd.getPhysicalNetworkId(), cmd.getUrl(), cmd.getUsername(), cmd.getPassword(), deviceName, new PaloAltoResource());
    }

    @Override
    public boolean deletePaloAltoFirewall(DeletePaloAltoFirewallCmd cmd) {
        Long fwDeviceId = cmd.getFirewallDeviceId();

        ExternalFirewallDeviceVO fwDeviceVO = _fwDevicesDao.findById(fwDeviceId);
        if (fwDeviceVO == null || !fwDeviceVO.getDeviceName().equalsIgnoreCase(NetworkDevice.PaloAltoFirewall.getName())) {
            throw new InvalidParameterValueException("No Palo Alto firewall device found with ID: " + fwDeviceId);
        }
        return deleteExternalFirewall(fwDeviceVO.getHostId());
    }

    @Override
    public ExternalFirewallDeviceVO configurePaloAltoFirewall(ConfigurePaloAltoFirewallCmd cmd) {
        Long fwDeviceId = cmd.getFirewallDeviceId();
        Long deviceCapacity = cmd.getFirewallCapacity();

        ExternalFirewallDeviceVO fwDeviceVO = _fwDevicesDao.findById(fwDeviceId);
        if (fwDeviceVO == null || !fwDeviceVO.getDeviceName().equalsIgnoreCase(NetworkDevice.PaloAltoFirewall.getName())) {
            throw new InvalidParameterValueException("No Palo Alto firewall device found with ID: " + fwDeviceId);
        }

        if (deviceCapacity != null) {
            // check if any networks are using this Palo Alto device
            List<NetworkExternalFirewallVO> networks = _networkFirewallDao.listByFirewallDeviceId(fwDeviceId);
            if ((networks != null) && !networks.isEmpty()) {
                if (deviceCapacity < networks.size()) {
                    throw new CloudRuntimeException("There are more number of networks already using this Palo Alto firewall device than configured capacity");
                }
            }
            if (deviceCapacity != null) {
                fwDeviceVO.setCapacity(deviceCapacity);
            }
        }

        fwDeviceVO.setDeviceState(FirewallDeviceState.Enabled);
        _fwDevicesDao.update(fwDeviceId, fwDeviceVO);
        return fwDeviceVO;
    }

    @Override
    public List<ExternalFirewallDeviceVO> listPaloAltoFirewalls(ListPaloAltoFirewallsCmd cmd) {
        Long physcialNetworkId = cmd.getPhysicalNetworkId();
        Long fwDeviceId = cmd.getFirewallDeviceId();
        PhysicalNetworkVO pNetwork = null;
        List<ExternalFirewallDeviceVO> fwDevices = new ArrayList<ExternalFirewallDeviceVO>();

        if (physcialNetworkId == null && fwDeviceId == null) {
            throw new InvalidParameterValueException("Either physical network Id or load balancer device Id must be specified");
        }

        if (fwDeviceId != null) {
            ExternalFirewallDeviceVO fwDeviceVo = _fwDevicesDao.findById(fwDeviceId);
            if (fwDeviceVo == null || !fwDeviceVo.getDeviceName().equalsIgnoreCase(NetworkDevice.PaloAltoFirewall.getName())) {
                throw new InvalidParameterValueException("Could not find Palo Alto firewall device with ID: " + fwDeviceId);
            }
            fwDevices.add(fwDeviceVo);
        }

        if (physcialNetworkId != null) {
            pNetwork = _physicalNetworkDao.findById(physcialNetworkId);
            if (pNetwork == null) {
                throw new InvalidParameterValueException("Could not find phyical network with ID: " + physcialNetworkId);
            }
            fwDevices = _fwDevicesDao.listByPhysicalNetworkAndProvider(physcialNetworkId, Provider.PaloAlto.getName());
        }

        return fwDevices;
    }

    @Override
    public List<? extends Network> listNetworks(ListPaloAltoFirewallNetworksCmd cmd) {
        Long fwDeviceId = cmd.getFirewallDeviceId();
        List<NetworkVO> networks = new ArrayList<NetworkVO>();

        ExternalFirewallDeviceVO fwDeviceVo = _fwDevicesDao.findById(fwDeviceId);
        if (fwDeviceVo == null || !fwDeviceVo.getDeviceName().equalsIgnoreCase(NetworkDevice.PaloAltoFirewall.getName())) {
            throw new InvalidParameterValueException("Could not find Palo Alto firewall device with ID " + fwDeviceId);
        }

        List<NetworkExternalFirewallVO> networkFirewallMaps = _networkFirewallDao.listByFirewallDeviceId(fwDeviceId);
        if (networkFirewallMaps != null && !networkFirewallMaps.isEmpty()) {
            for (NetworkExternalFirewallVO networkFirewallMap : networkFirewallMaps) {
                NetworkVO network = _networkDao.findById(networkFirewallMap.getNetworkId());
                networks.add(network);
            }
        }

        return networks;
    }

    @Override
    public PaloAltoFirewallResponse createPaloAltoFirewallResponse(ExternalFirewallDeviceVO fwDeviceVO) {
        PaloAltoFirewallResponse response = new PaloAltoFirewallResponse();
        Map<String, String> fwDetails = _hostDetailDao.findDetails(fwDeviceVO.getHostId());
        Host fwHost = _hostDao.findById(fwDeviceVO.getHostId());

        response.setId(fwDeviceVO.getUuid());
        PhysicalNetwork pnw = ApiDBUtils.findPhysicalNetworkById(fwDeviceVO.getPhysicalNetworkId());
        if (pnw != null) {
            response.setPhysicalNetworkId(pnw.getUuid());
        }
        response.setDeviceName(fwDeviceVO.getDeviceName());
        if (fwDeviceVO.getCapacity() == 0) {
            long defaultFwCapacity = NumbersUtil.parseLong(_configDao.getValue(Config.DefaultExternalFirewallCapacity.key()), 50);
            response.setDeviceCapacity(defaultFwCapacity);
        } else {
            response.setDeviceCapacity(fwDeviceVO.getCapacity());
        }
        response.setProvider(fwDeviceVO.getProviderName());
        response.setDeviceState(fwDeviceVO.getDeviceState().name());
        response.setIpAddress(fwHost.getPrivateIpAddress());
        response.setPublicInterface(fwDetails.get("publicInterface"));
        response.setUsageInterface(fwDetails.get("usageInterface"));
        response.setPrivateInterface(fwDetails.get("privateInterface"));
        response.setPublicZone(fwDetails.get("publicZone"));
        response.setPrivateZone(fwDetails.get("privateZone"));
        response.setNumRetries(fwDetails.get("numRetries"));
        response.setTimeout(fwDetails.get("timeout"));
        response.setObjectName("paloaltofirewall");
        return response;
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {
        if (!services.contains(Service.Firewall)) {
            logger.warn("Palo Alto must be used as Firewall Service Provider in the network");
            return false;
        }
        return true;
    }

    @Override
    public IpDeployer getIpDeployer(Network network) {
        return this;
    }

    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress, Set<Service> service) throws ResourceUnavailableException {
        // return true, as IP will be associated as part of static NAT/port forwarding rule configuration
        return true;
    }

    @Override
    public boolean applyStaticNats(Network config, List<? extends StaticNat> rules) throws ResourceUnavailableException {
        if (!canHandle(config, Service.StaticNat)) {
            return false;
        }
        return applyStaticNatRules(config, rules);
    }
}
