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
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.routing.SavePasswordCommand;
import com.cloud.agent.api.routing.VmDataCommand;
import com.cloud.agent.manager.Commands;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.ZoneConfig;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkModel;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.NetworkDao;
import com.cloud.offering.NetworkOffering;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineManager;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;

public class CloudZonesNetworkElement extends AdapterBase implements NetworkElement, UserDataServiceProvider {
    private static final Logger s_logger = Logger.getLogger(CloudZonesNetworkElement.class);

    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();

    @Inject
    NetworkDao _networkConfigDao;
    @Inject
    NetworkModel _networkMgr;
    @Inject
    UserVmManager _userVmMgr;
    @Inject
    UserVmDao _userVmDao;
    @Inject
    DomainRouterDao _routerDao;
    @Inject
    ConfigurationManager _configMgr;
    @Inject
    DataCenterDao _dcDao;
    @Inject
    AgentManager _agentManager;
    @Inject
    ServiceOfferingDao _serviceOfferingDao;

    private boolean canHandle(DeployDestination dest, TrafficType trafficType) {
        DataCenterVO dc = (DataCenterVO)dest.getDataCenter();

        if (dc.getDhcpProvider().equalsIgnoreCase(Provider.ExternalDhcpServer.getName())) {
            _dcDao.loadDetails(dc);
            String dhcpStrategy = dc.getDetail(ZoneConfig.DhcpStrategy.key());
            if ("external".equalsIgnoreCase(dhcpStrategy)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException,
        ConcurrentOperationException, InsufficientCapacityException {
        if (!canHandle(dest, offering.getTrafficType())) {
            return false;
        }

        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile vmProfile, DeployDestination dest, ReservationContext context)
        throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile vm, ReservationContext context) {
        return true;
    }

    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
        return false; // assume that the agent will remove userdata etc
    }

    @Override
    public boolean destroy(Network config, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
        return false; // assume that the agent will remove userdata etc
    }

    @Override
    public Provider getProvider() {
        return Provider.ExternalDhcpServer;
    }

    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }

    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();

        capabilities.put(Service.UserData, null);

        return capabilities;
    }

    private VmDataCommand generateVmDataCommand(String vmPrivateIpAddress, String userData, String serviceOffering, String zoneName, String guestIpAddress,
        String vmName, String vmInstanceName, long vmId, String vmUuid, String publicKey, String hostname) {
        VmDataCommand cmd = new VmDataCommand(vmPrivateIpAddress, vmName, _networkMgr.getExecuteInSeqNtwkElmtCmd());
        // if you add new metadata files, also edit systemvm/patches/debian/config/var/www/html/latest/.htaccess
        cmd.addVmData("userdata", "user-data", userData);
        cmd.addVmData("metadata", "service-offering", serviceOffering);
        cmd.addVmData("metadata", "availability-zone", zoneName);
        cmd.addVmData("metadata", "local-ipv4", guestIpAddress);
        cmd.addVmData("metadata", "local-hostname", vmName);
        cmd.addVmData("metadata", "public-ipv4", guestIpAddress);
        cmd.addVmData("metadata", "public-hostname", guestIpAddress);
        if (vmUuid == null) {
            setVmInstanceId(vmInstanceName, vmId, cmd);
        } else {
            setVmInstanceId(vmUuid, cmd);
        }
        cmd.addVmData("metadata", "public-keys", publicKey);
        cmd.addVmData("metadata", "hypervisor-host-name", hostname);
        return cmd;
    }

    private void setVmInstanceId(String vmUuid, VmDataCommand cmd) {
        cmd.addVmData("metadata", "instance-id", vmUuid);
        cmd.addVmData("metadata", "vm-id", vmUuid);
    }

    private void setVmInstanceId(String vmInstanceName, long vmId, VmDataCommand cmd) {
        cmd.addVmData("metadata", "instance-id", vmInstanceName);
        cmd.addVmData("metadata", "vm-id", String.valueOf(vmId));
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        // TODO Auto-generated method stub
        return true;
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
    public boolean addPasswordAndUserdata(Network network, NicProfile nic, VirtualMachineProfile vm, DeployDestination dest, ReservationContext context)
        throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        if (canHandle(dest, network.getTrafficType())) {

            if (vm.getType() != VirtualMachine.Type.User) {
                return false;
            }
            @SuppressWarnings("unchecked")
            UserVmVO uservm = _userVmDao.findById(vm.getId());
            _userVmDao.loadDetails(uservm);
            String password = (String)vm.getParameter(VirtualMachineProfile.Param.VmPassword);
            String userData = uservm.getUserData();
            String sshPublicKey = uservm.getDetail("SSH.PublicKey");

            Commands cmds = new Commands(Command.OnError.Continue);
            if (password != null && nic.isDefaultNic()) {
                SavePasswordCommand cmd = new SavePasswordCommand(password, nic.getIPv4Address(), uservm.getHostName(), _networkMgr.getExecuteInSeqNtwkElmtCmd());
                cmds.addCommand("password", cmd);
            }
            String serviceOffering = _serviceOfferingDao.findByIdIncludingRemoved(uservm.getServiceOfferingId()).getDisplayText();
            String zoneName = _dcDao.findById(network.getDataCenterId()).getName();
            String destHostname = VirtualMachineManager.getHypervisorHostname(dest.getHost() != null ? dest.getHost().getName() : "");
            cmds.addCommand(
                "vmdata",
                generateVmDataCommand(nic.getIPv4Address(), userData, serviceOffering, zoneName, nic.getIPv4Address(), uservm.getHostName(), uservm.getInstanceName(),
                    uservm.getId(), uservm.getUuid(), sshPublicKey, destHostname));
            try {
                _agentManager.send(dest.getHost().getId(), cmds);
            } catch (OperationTimedoutException e) {
                s_logger.debug("Unable to send vm data command to host " + dest.getHost());
                return false;
            }
            Answer dataAnswer = cmds.getAnswer("vmdata");
            if (dataAnswer != null && dataAnswer.getResult()) {
                s_logger.info("Sent vm data successfully to vm " + uservm.getInstanceName());
                return true;
            }
            s_logger.info("Failed to send vm data to vm " + uservm.getInstanceName());
            return false;
        }
        return false;
    }

    @Override
    public boolean savePassword(Network network, NicProfile nic, VirtualMachineProfile vm) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean saveSSHKey(Network network, NicProfile nic, VirtualMachineProfile vm, String sshPublicKey) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean saveHypervisorHostname(NicProfile profile, Network network, VirtualMachineProfile vm, DeployDestination dest) throws ResourceUnavailableException {
        return true;
    }

    @Override
    public boolean saveUserData(Network network, NicProfile nic, VirtualMachineProfile vm) throws ResourceUnavailableException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean verifyServicesCombination(Set<Service> services) {
        return true;
    }
}
