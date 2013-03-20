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

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.api.response.ExternalLoadBalancerResponse;
import org.apache.cloudstack.network.ExternalNetworkDeviceManager.NetworkDevice;
import org.apache.log4j.Logger;

import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.commands.AddExternalLoadBalancerCmd;
import com.cloud.api.commands.AddF5LoadBalancerCmd;
import com.cloud.api.commands.ConfigureF5LoadBalancerCmd;
import com.cloud.api.commands.DeleteExternalLoadBalancerCmd;
import com.cloud.api.commands.DeleteF5LoadBalancerCmd;
import com.cloud.api.commands.ListExternalLoadBalancersCmd;
import com.cloud.api.commands.ListF5LoadBalancerNetworksCmd;
import com.cloud.api.commands.ListF5LoadBalancersCmd;
import com.cloud.api.response.F5LoadBalancerResponse;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientNetworkCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.network.ExternalLoadBalancerDeviceManager;
import com.cloud.network.ExternalLoadBalancerDeviceManagerImpl;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.dao.ExternalLoadBalancerDeviceDao;
import com.cloud.network.dao.ExternalLoadBalancerDeviceVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkExternalLoadBalancerDao;
import com.cloud.network.dao.NetworkExternalLoadBalancerVO;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.network.dao.ExternalLoadBalancerDeviceVO.LBDeviceState;
import com.cloud.network.lb.LoadBalancingRule;
import com.cloud.network.resource.F5BigIpResource;
import com.cloud.network.rules.LbStickinessMethod;
import com.cloud.network.rules.LbStickinessMethod.StickinessMethodType;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.google.gson.Gson;

@Local(value = {NetworkElement.class, LoadBalancingServiceProvider.class, IpDeployer.class})
public class F5ExternalLoadBalancerElement extends ExternalLoadBalancerDeviceManagerImpl implements LoadBalancingServiceProvider, IpDeployer, F5ExternalLoadBalancerElementService, ExternalLoadBalancerDeviceManager {

    private static final Logger s_logger = Logger.getLogger(F5ExternalLoadBalancerElement.class);

    @Inject
    NetworkModel _networkManager;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    NetworkServiceMapDao _ntwkSrvcDao;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    PhysicalNetworkDao _physicalNetworkDao;
    @Inject
    HostDao _hostDao;
    @Inject
    ExternalLoadBalancerDeviceDao _lbDeviceDao;
    @Inject
    NetworkExternalLoadBalancerDao _networkLBDao;
    @Inject
    NetworkDao _networkDao;
    @Inject
    HostDetailsDao _detailsDao;
    @Inject
    ConfigurationDao _configDao;

    private boolean canHandle(Network config) {
        if (config.getGuestType() != Network.GuestType.Isolated || config.getTrafficType() != TrafficType.Guest) {
            s_logger.trace("Not handling network with Type  " + config.getGuestType() + " and traffic type " + config.getTrafficType());
            return false;
        }

        return (_networkManager.isProviderForNetwork(getProvider(), config.getId()) && _ntwkSrvcDao.canProviderSupportServiceInNetwork(config.getId(), Service.Lb, Network.Provider.F5BigIp));
    }

    @Override
    public boolean implement(Network guestConfig, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException, ConcurrentOperationException,
    InsufficientNetworkCapacityException {

        if (!canHandle(guestConfig)) {
            return false;
        }

        try {
            return manageGuestNetworkWithExternalLoadBalancer(true, guestConfig);
        } catch (InsufficientCapacityException capacityException) {
            throw new ResourceUnavailableException("There are no F5 load balancer devices with the free capacity for implementing this network", DataCenter.class, guestConfig.getDataCenterId());
        }
    }

    @Override
    public boolean prepare(Network config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException,
    InsufficientNetworkCapacityException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean release(Network config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, ReservationContext context) {
        return true;
    }

    @Override
    public boolean shutdown(Network guestConfig, ReservationContext context, boolean cleanup) throws ResourceUnavailableException, ConcurrentOperationException {
        if (!canHandle(guestConfig)) {
            return false;
        }

        try {
            return manageGuestNetworkWithExternalLoadBalancer(false, guestConfig);
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
    public boolean validateLBRule(Network network, LoadBalancingRule rule) {
        String algo = rule.getAlgorithm();
        return (algo.equals("roundrobin") || algo.equals("leastconn"));
    }

    @Override
    public boolean applyLBRules(Network config, List<LoadBalancingRule> rules) throws ResourceUnavailableException {
        if (!canHandle(config)) {
            return false;
        }

        return applyLoadBalancerRules(config, rules);
    }

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();

        // Set capabilities for LB service
        Map<Capability, String> lbCapabilities = new HashMap<Capability, String>();

        // Specifies that the RoundRobin and Leastconn algorithms are supported for load balancing rules
        lbCapabilities.put(Capability.SupportedLBAlgorithms, "roundrobin,leastconn");

        // specifies that F5 BIG IP network element can provide shared mode only
        lbCapabilities.put(Capability.SupportedLBIsolation, "dedicated, shared");

        // Specifies that load balancing rules can be made for either TCP or UDP traffic
        lbCapabilities.put(Capability.SupportedProtocols, "tcp,udp");

        // Specifies that this element can measure network usage on a per public IP basis
        lbCapabilities.put(Capability.TrafficStatistics, "per public ip");

        // Specifies that load balancing rules can only be made with public IPs that aren't source NAT IPs
        lbCapabilities.put(Capability.LoadBalancingSupportedIps, "additional");

        // Support inline mode with firewall
        lbCapabilities.put(Capability.InlineMode, "true");

        LbStickinessMethod method;
        List<LbStickinessMethod> methodList = new ArrayList<LbStickinessMethod>();
        method = new LbStickinessMethod(StickinessMethodType.LBCookieBased, "This is cookie based sticky method, can be used only for http");
        methodList.add(method);
        method.addParam("holdtime", false, "time period (in seconds) for which persistence is in effect.", false);

        Gson gson = new Gson();
        String stickyMethodList = gson.toJson(methodList);
        lbCapabilities.put(Capability.SupportedStickinessMethods, stickyMethodList);

        capabilities.put(Service.Lb, lbCapabilities);

        return capabilities;
    }

    @Override
    public Provider getProvider() {
        return Provider.F5BigIp;
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        List<ExternalLoadBalancerDeviceVO> lbDevices = _lbDeviceDao.listByPhysicalNetworkAndProvider(provider.getPhysicalNetworkId(), Provider.F5BigIp.getName());

        // true if at-least one F5 device is added in to physical network and is in configured (in enabled state) state
        if (lbDevices != null && !lbDevices.isEmpty()) {
            for (ExternalLoadBalancerDeviceVO lbDevice : lbDevices) {
                if (lbDevice.getState() == LBDeviceState.Enabled) {
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
        return false;
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(AddExternalLoadBalancerCmd.class);
        cmdList.add(AddF5LoadBalancerCmd.class);
        cmdList.add(ConfigureF5LoadBalancerCmd.class);
        cmdList.add(DeleteExternalLoadBalancerCmd.class);
        cmdList.add(DeleteF5LoadBalancerCmd.class);
        cmdList.add(ListExternalLoadBalancersCmd.class);
        cmdList.add(ListF5LoadBalancerNetworksCmd.class);
        cmdList.add(ListF5LoadBalancersCmd.class);
        return cmdList;
    }

    @Override
    @Deprecated
    public Host addExternalLoadBalancer(AddExternalLoadBalancerCmd cmd) {
        Long zoneId = cmd.getZoneId();
        DataCenterVO zone = null;
        PhysicalNetworkVO pNetwork = null;
        ExternalLoadBalancerDeviceVO lbDeviceVO = null;
        HostVO lbHost = null;

        zone = _dcDao.findById(zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Could not find zone with ID: " + zoneId);
        }

        List<PhysicalNetworkVO> physicalNetworks = _physicalNetworkDao.listByZone(zoneId);
        if ((physicalNetworks == null) || (physicalNetworks.size() > 1)) {
            throw new InvalidParameterValueException("There are no physical networks or multiple physical networks configured in zone with ID: "
                    + zoneId + " to add this device.");
        }
        pNetwork = physicalNetworks.get(0);

        String deviceType = NetworkDevice.F5BigIpLoadBalancer.getName();
        lbDeviceVO = addExternalLoadBalancer(pNetwork.getId(), cmd.getUrl(), cmd.getUsername(), cmd.getPassword(), deviceType, new F5BigIpResource());

        if (lbDeviceVO != null) {
            lbHost = _hostDao.findById(lbDeviceVO.getHostId());
        }

        return lbHost;
    }

    @Override
    @Deprecated
    public boolean deleteExternalLoadBalancer(DeleteExternalLoadBalancerCmd cmd) {
        return deleteExternalLoadBalancer(cmd.getId());
    }

    @Override
    @Deprecated
    public List<Host> listExternalLoadBalancers(ListExternalLoadBalancersCmd cmd) {
        Long zoneId = cmd.getZoneId();
        DataCenterVO zone = null;
        PhysicalNetworkVO pNetwork = null;

        if (zoneId != null) {
            zone = _dcDao.findById(zoneId);
            if (zone == null) {
                throw new InvalidParameterValueException("Could not find zone with ID: " + zoneId);
            }

            List<PhysicalNetworkVO> physicalNetworks = _physicalNetworkDao.listByZone(zoneId);
            if ((physicalNetworks == null) || (physicalNetworks.size() > 1)) {
                throw new InvalidParameterValueException("There are no physical networks or multiple physical networks configured in zone with ID: "
                        + zoneId + " to add this device.");
            }
            pNetwork = physicalNetworks.get(0);
            return listExternalLoadBalancers(pNetwork.getId(), NetworkDevice.F5BigIpLoadBalancer.getName());
        } else {
            throw new InvalidParameterValueException("Zone Id must be specified to list the external load balancers");
        }
    }

    @Override
    @Deprecated
    public ExternalLoadBalancerResponse createExternalLoadBalancerResponse(Host externalLb) {
        return super.createExternalLoadBalancerResponse(externalLb);
    }

    @Override
    public ExternalLoadBalancerDeviceVO addF5LoadBalancer(AddF5LoadBalancerCmd cmd) {
        String deviceName = cmd.getDeviceType();
        if (!deviceName.equalsIgnoreCase(NetworkDevice.F5BigIpLoadBalancer.getName())) {
            throw new InvalidParameterValueException("Invalid F5 load balancer device type");
        }

        return addExternalLoadBalancer(cmd.getPhysicalNetworkId(), cmd.getUrl(), cmd.getUsername(), cmd.getPassword(), deviceName, new F5BigIpResource());

    }

    @Override
    public boolean deleteF5LoadBalancer(DeleteF5LoadBalancerCmd cmd) {
        Long lbDeviceId = cmd.getLoadBalancerDeviceId();

        ExternalLoadBalancerDeviceVO lbDeviceVo = _lbDeviceDao.findById(lbDeviceId);
        if ((lbDeviceVo == null) || !lbDeviceVo.getDeviceName().equalsIgnoreCase(NetworkDevice.F5BigIpLoadBalancer.getName())) {
            throw new InvalidParameterValueException("No F5 load balancer device found with ID: " + lbDeviceId);
        }

        return deleteExternalLoadBalancer(lbDeviceVo.getHostId());
    }

    @Override
    public ExternalLoadBalancerDeviceVO configureF5LoadBalancer(ConfigureF5LoadBalancerCmd cmd) {
        Long lbDeviceId = cmd.getLoadBalancerDeviceId();
        Long capacity = cmd.getLoadBalancerCapacity();

        ExternalLoadBalancerDeviceVO lbDeviceVo = _lbDeviceDao.findById(lbDeviceId);
        if ((lbDeviceVo == null) || !lbDeviceVo.getDeviceName().equalsIgnoreCase(NetworkDevice.F5BigIpLoadBalancer.getName())) {
            throw new InvalidParameterValueException("No F5 load balancer device found with ID: " + lbDeviceId);
        }

        if (capacity != null) {
            // check if any networks are using this F5 device
            List<NetworkExternalLoadBalancerVO> networks = _networkLBDao.listByLoadBalancerDeviceId(lbDeviceId);
            if ((networks != null) && !networks.isEmpty()) {
                if (capacity < networks.size()) {
                    throw new CloudRuntimeException("There are more number of networks already using this F5 device than configured capacity");
                }
            }
            if (capacity != null) {
                lbDeviceVo.setCapacity(capacity);
            }
        }

        lbDeviceVo.setState(LBDeviceState.Enabled);
        _lbDeviceDao.update(lbDeviceId, lbDeviceVo);
        return lbDeviceVo;
    }

    @Override
    public List<ExternalLoadBalancerDeviceVO> listF5LoadBalancers(ListF5LoadBalancersCmd cmd) {
        Long physcialNetworkId = cmd.getPhysicalNetworkId();
        Long lbDeviceId = cmd.getLoadBalancerDeviceId();
        PhysicalNetworkVO pNetwork = null;
        List<ExternalLoadBalancerDeviceVO> lbDevices = new ArrayList<ExternalLoadBalancerDeviceVO>();

        if (physcialNetworkId == null && lbDeviceId == null) {
            throw new InvalidParameterValueException("Either physical network Id or load balancer device Id must be specified");
        }

        if (lbDeviceId != null) {
            ExternalLoadBalancerDeviceVO lbDeviceVo = _lbDeviceDao.findById(lbDeviceId);
            if (lbDeviceVo == null || !lbDeviceVo.getDeviceName().equalsIgnoreCase(NetworkDevice.F5BigIpLoadBalancer.getName())) {
                throw new InvalidParameterValueException("Could not find F5 load balancer device with ID: " + lbDeviceId);
            }
            lbDevices.add(lbDeviceVo);
            return lbDevices;
        }

        if (physcialNetworkId != null) {
            pNetwork = _physicalNetworkDao.findById(physcialNetworkId);
            if (pNetwork == null) {
                throw new InvalidParameterValueException("Could not find phyical network with ID: " + physcialNetworkId);
            }
            lbDevices = _lbDeviceDao.listByPhysicalNetworkAndProvider(physcialNetworkId, Provider.F5BigIp.getName());
            return lbDevices;
        }

        return null;
    }

    @Override
    public List<? extends Network> listNetworks(ListF5LoadBalancerNetworksCmd cmd) {
        Long lbDeviceId = cmd.getLoadBalancerDeviceId();
        List<NetworkVO> networks = new ArrayList<NetworkVO>();

        ExternalLoadBalancerDeviceVO lbDeviceVo = _lbDeviceDao.findById(lbDeviceId);
        if (lbDeviceVo == null || !lbDeviceVo.getDeviceName().equalsIgnoreCase(NetworkDevice.F5BigIpLoadBalancer.getName())) {
            throw new InvalidParameterValueException("Could not find F5 load balancer device with ID " + lbDeviceId);
        }

        List<NetworkExternalLoadBalancerVO> networkLbMaps = _networkLBDao.listByLoadBalancerDeviceId(lbDeviceId);
        if (networkLbMaps != null && !networkLbMaps.isEmpty()) {
            for (NetworkExternalLoadBalancerVO networkLbMap : networkLbMaps) {
                NetworkVO network = _networkDao.findById(networkLbMap.getNetworkId());
                networks.add(network);
            }
        }

        return networks;
    }

    @Override
    public F5LoadBalancerResponse createF5LoadBalancerResponse(ExternalLoadBalancerDeviceVO lbDeviceVO) {
        F5LoadBalancerResponse response = new F5LoadBalancerResponse();
        Host lbHost = _hostDao.findById(lbDeviceVO.getHostId());
        Map<String, String> lbDetails = _detailsDao.findDetails(lbDeviceVO.getHostId());

        response.setId(lbDeviceVO.getUuid());
        response.setIpAddress(lbHost.getPrivateIpAddress());
        PhysicalNetwork pnw = ApiDBUtils.findPhysicalNetworkById(lbDeviceVO.getPhysicalNetworkId());
        if (pnw != null) {
            response.setPhysicalNetworkId(pnw.getUuid());
        }
        response.setPublicInterface(lbDetails.get("publicInterface"));
        response.setPrivateInterface(lbDetails.get("privateInterface"));
        response.setDeviceName(lbDeviceVO.getDeviceName());
        if (lbDeviceVO.getCapacity() == 0) {
            long defaultLbCapacity = NumbersUtil.parseLong(_configDao.getValue(Config.DefaultExternalLoadBalancerCapacity.key()), 50);
            response.setDeviceCapacity(defaultLbCapacity);
        } else {
            response.setDeviceCapacity(lbDeviceVO.getCapacity());
        }
        response.setDedicatedLoadBalancer(lbDeviceVO.getIsDedicatedDevice());
        response.setProvider(lbDeviceVO.getProviderName());
        response.setDeviceState(lbDeviceVO.getState().name());
        response.setObjectName("f5loadbalancer");
        return response;
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {
        return true;
    }

    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress, Set<Service> service) throws ResourceUnavailableException {
        // return true, as IP will be associated as part of LB rule configuration
        return true;
    }

    @Override
    public IpDeployer getIpDeployer(Network network) {
        ExternalLoadBalancerDeviceVO lbDevice = getExternalLoadBalancerForNetwork(network);
        if (lbDevice == null) {
            s_logger.error("Cannot find external load balanacer for network " + network.getName());
            s_logger.error("Make F5 as dummy ip deployer, since we likely met this when clean up resource after shutdown network");
            return this;
        }
        if (_networkManager.isNetworkInlineMode(network)) {
            return getIpDeployerForInlineMode(network);
        }
        return this;
    }

	@Override
	public List<LoadBalancerTO> updateHealthChecks(Network network,
			List<LoadBalancingRule> lbrules) {
		// TODO Auto-generated method stub
		return null;
	}
}
