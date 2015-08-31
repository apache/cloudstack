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

package org.apache.cloudstack.network.contrail.management;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.network.contrail.api.command.CreateServiceInstanceCmd;
import org.apache.cloudstack.network.contrail.model.InstanceIpModel;
import org.apache.cloudstack.network.contrail.model.VMInterfaceModel;
import org.apache.cloudstack.network.contrail.model.VirtualMachineModel;
import org.apache.cloudstack.network.contrail.model.VirtualNetworkModel;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.PublicIpAddress;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.element.DhcpServiceProvider;
import com.cloud.network.element.IpDeployer;
import com.cloud.network.element.SourceNatServiceProvider;
import com.cloud.network.element.StaticNatServiceProvider;
import com.cloud.network.rules.StaticNat;
import com.cloud.offering.NetworkOffering;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ConfigurationServer;
import com.cloud.server.ConfigurationServerImpl;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;

@Component
@Local(value = {ContrailElement.class, StaticNatServiceProvider.class, IpDeployer.class, SourceNatServiceProvider.class})

public class ContrailElementImpl extends AdapterBase
    implements ContrailElement, StaticNatServiceProvider, IpDeployer, SourceNatServiceProvider, DhcpServiceProvider {
    private final Map<Service, Map<Capability, String>> _capabilities = InitCapabilities();

    @Inject
    ResourceManager _resourceMgr;
    @Inject
    ConfigurationServer _configServer;
    @Inject
    NetworkDao _networksDao;
    @Inject
    ContrailManager _manager;
    @Inject
    NicDao _nicDao;
    @Inject
    ServerDBSync _dbSync;
    private static final Logger s_logger = Logger.getLogger(ContrailElement.class);

    // PluggableService
    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(CreateServiceInstanceCmd.class);
        return cmdList;
    }

    // NetworkElement API
    @Override
    public Provider getProvider() {
        return Provider.JuniperContrailRouter;
    }

    private static Map<Service, Map<Capability, String>> InitCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();
        capabilities.put(Service.Connectivity, null);
        capabilities.put(Service.Dhcp, new HashMap<Capability, String>());
        capabilities.put(Service.StaticNat, null);
        capabilities.put(Service.SourceNat, null);

        return capabilities;
    }

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return _capabilities;
    }

    /**
     * Network add/update.
     */
    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException,
        ResourceUnavailableException, InsufficientCapacityException {
        s_logger.debug("NetworkElement implement: " + network.getName() + ", traffic type: " + network.getTrafficType());
        if (network.getTrafficType() == TrafficType.Guest) {
            s_logger.debug("ignore network " + network.getName());
            return true;
        }
        VirtualNetworkModel vnModel = _manager.getDatabase().lookupVirtualNetwork(network.getUuid(), _manager.getCanonicalName(network), network.getTrafficType());

        if (vnModel == null) {
            vnModel = new VirtualNetworkModel(network, network.getUuid(), _manager.getCanonicalName(network), network.getTrafficType());
            vnModel.setProperties(_manager.getModelController(), network);
        }
        try {
            if (!vnModel.verify(_manager.getModelController())) {
                vnModel.update(_manager.getModelController());
            }
            _manager.getDatabase().getVirtualNetworks().add(vnModel);
        } catch (Exception ex) {
            s_logger.warn("virtual-network update: ", ex);
        }
        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nicProfile, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {

        s_logger.debug("NetworkElement prepare: " + network.getName() + ", traffic type: " + network.getTrafficType());

        if (network.getTrafficType() == TrafficType.Guest) {
            s_logger.debug("ignore network " + network.getName());
            return true;
        }

        s_logger.debug("network: " + network.getId());

        VirtualNetworkModel vnModel = _manager.getDatabase().lookupVirtualNetwork(network.getUuid(), _manager.getCanonicalName(network), network.getTrafficType());

        if (vnModel == null) {
            // There is no notification after a physical network is associated with the VRouter NetworkOffering
            // this may be the first time we see this network.
            return false;
        }

        VirtualMachineModel vmModel = _manager.getDatabase().lookupVirtualMachine(vm.getUuid());
        if (vmModel == null) {
            VMInstanceVO vmVo = (VMInstanceVO)vm.getVirtualMachine();
            vmModel = new VirtualMachineModel(vmVo, vm.getUuid());
            vmModel.setProperties(_manager.getModelController(), vmVo);
        }

        NicVO nic = _nicDao.findById(nicProfile.getId());
        assert nic != null;

        VMInterfaceModel vmiModel = vmModel.getVMInterface(nic.getUuid());
        if (vmiModel == null) {
            vmiModel = new VMInterfaceModel(nic.getUuid());
            vmiModel.addToVirtualMachine(vmModel);
            vmiModel.addToVirtualNetwork(vnModel);
        }

        try {
            vmiModel.build(_manager.getModelController(), (VMInstanceVO)vm.getVirtualMachine(), nic);
        } catch (IOException ex) {
            s_logger.warn("vm interface set", ex);
            return false;
        }

        InstanceIpModel ipModel = vmiModel.getInstanceIp();
        if (ipModel == null) {
            ipModel = new InstanceIpModel(vm.getInstanceName(), nic.getDeviceId());
            ipModel.addToVMInterface(vmiModel);
        }
        ipModel.setAddress(nicProfile.getIPv4Address());

        try {
            vmModel.update(_manager.getModelController());
        } catch (Exception ex) {
            s_logger.warn("virtual-machine-update", ex);
            return false;
        }
        _manager.getDatabase().getVirtualMachines().add(vmModel);

        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nicProfile, VirtualMachineProfile vm, ReservationContext context) throws ConcurrentOperationException,
        ResourceUnavailableException {
        if (network.getTrafficType() == TrafficType.Guest) {
            return true;
        } else if (!_manager.isManagedPhysicalNetwork(network)) {
            s_logger.debug("release ignore network " + network.getId());
            return true;
        }

        NicVO nic = _nicDao.findById(nicProfile.getId());
        assert nic != null;

        VirtualMachineModel vmModel = _manager.getDatabase().lookupVirtualMachine(vm.getUuid());
        if (vmModel == null) {
            s_logger.debug("vm " + vm.getInstanceName() + " not in local database");
            return true;
        }
        VMInterfaceModel vmiModel = vmModel.getVMInterface(nic.getUuid());
        if (vmiModel != null) {
            try {
                vmiModel.destroy(_manager.getModelController());
            } catch (IOException ex) {
                s_logger.warn("virtual-machine-interface delete", ex);
            }
            vmModel.removeSuccessor(vmiModel);
        }

        if (!vmModel.hasDescendents()) {
            _manager.getDatabase().getVirtualMachines().remove(vmModel);
            try {
                vmModel.delete(_manager.getModelController());
            } catch (IOException e) {
                return false;
            }
        }

        return true;
    }

    /**
     * Network disable
     */
    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
        s_logger.debug("NetworkElement shutdown");
        return true;
    }

    /**
     * Network delete
     */
    @Override
    public boolean destroy(Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        s_logger.debug("NetworkElement destroy");
        return true;
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
                Map<String, String> serviceMap = ((ConfigurationServerImpl)_configServer).getServicesAndProvidersForNetwork( _manager.getRouterOffering().getId());
                List<TrafficType> types = new ArrayList<TrafficType>();
                types.add(TrafficType.Control);
                types.add(TrafficType.Management);
                types.add(TrafficType.Storage);
                List<NetworkVO> systemNets = _manager.findSystemNetworks(types);
                if (systemNets != null && !systemNets.isEmpty()) {
                    for (NetworkVO net: systemNets) {
                        s_logger.debug("update system network service: " + net.getName() + "; service provider: " + serviceMap);
                        _networksDao.update(net.getId(), net, serviceMap);
                    }
                } else {
                    s_logger.debug("no system networks created yet");
                }
                serviceMap = ((ConfigurationServerImpl)_configServer).getServicesAndProvidersForNetwork( _manager.getPublicRouterOffering().getId());
                types = new ArrayList<TrafficType>();
                types.add(TrafficType.Public);
                systemNets = _manager.findSystemNetworks(types);
                if (systemNets != null && !systemNets.isEmpty()) {
                    for (NetworkVO net: systemNets) {
                        s_logger.debug("update system network service: " + net.getName() + "; service provider: " + serviceMap);
                        _networksDao.update(net.getId(), net, serviceMap);
                    }
                } else {
                    s_logger.debug("no system networks created yet");
                }
                return true;
       }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context) throws ConcurrentOperationException,
        ResourceUnavailableException {
        s_logger.debug("NetworkElement shutdown ProviderInstances");
        return true;
    }

    @Override
    public boolean canEnableIndividualServices() {
        return true;
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {
        // TODO Auto-generated method stub
        s_logger.debug("NetworkElement verifyServices");
        s_logger.debug("Services: " + services);
        return true;
    }

    @Override
    public IpDeployer getIpDeployer(Network network) {
        return this;
    }

    @Override
    public boolean applyIps(Network network, List<? extends PublicIpAddress> ipAddress, Set<Service> services) throws ResourceUnavailableException {

        for (PublicIpAddress ip : ipAddress) {
            if (ip.isSourceNat()) {
                continue;
            }
            if (isFloatingIpCreate(ip)) {
                if (_manager.createFloatingIp(ip)) {
                    s_logger.debug("Successfully created floating ip: " + ip.getAddress().addr());
                }
            } else {
                if (_manager.deleteFloatingIp(ip)) {
                    s_logger.debug("Successfully deleted floating ip: " + ip.getAddress().addr());
                }
            }
        }
        return true;
    }

    @Override
    public boolean applyStaticNats(Network config, List<? extends StaticNat> rules) throws ResourceUnavailableException {
        return true;
    }

    private boolean isFloatingIpCreate(PublicIpAddress ip) {
        if (ip.getState() == IpAddress.State.Allocated && ip.getAssociatedWithVmId() != null && !ip.isSourceNat()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean addDhcpEntry(Network network, NicProfile nic,
               VirtualMachineProfile vm,
               DeployDestination dest, ReservationContext context)
                               throws ConcurrentOperationException, InsufficientCapacityException,
                               ResourceUnavailableException {
       return false;
    }

    @Override
    public boolean configDhcpSupportForSubnet(Network network, NicProfile nic,
               VirtualMachineProfile vm,
               DeployDestination dest, ReservationContext context)
                               throws ConcurrentOperationException, InsufficientCapacityException,
                               ResourceUnavailableException {
       return false;
    }

    @Override
    public boolean removeDhcpSupportForSubnet(Network network)
               throws ResourceUnavailableException {
       return false;
    }
}
