/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
*
 *
 * This software is licensed under the GNU General Public License v3 or later.
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.network.element;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.AgentManager.OnError;
import com.cloud.agent.api.Answer;
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
import com.cloud.network.NetworkManager;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.router.VirtualNetworkApplianceManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.uservm.UserVm;
import com.cloud.utils.PasswordGenerator;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;


@Local(value=NetworkElement.class)
public class CloudZonesNetworkElement extends AdapterBase implements NetworkElement {
    private static final Logger s_logger = Logger.getLogger(CloudZonesNetworkElement.class);
    
    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();
    
    @Inject NetworkDao _networkConfigDao;
    @Inject NetworkManager _networkMgr;
    @Inject VirtualNetworkApplianceManager _routerMgr;
    @Inject UserVmManager _userVmMgr;
    @Inject UserVmDao _userVmDao;
    @Inject DomainRouterDao _routerDao;
    @Inject ConfigurationManager _configMgr;
    @Inject DataCenterDao _dcDao;
    @Inject AgentManager _agentManager;
    @Inject ServiceOfferingDao _serviceOfferingDao;
    
    private boolean canHandle(DeployDestination dest, TrafficType trafficType) {
        DataCenterVO dc = (DataCenterVO)dest.getDataCenter();
        
        if (dc.getDhcpProvider().equalsIgnoreCase(Provider.ExternalDhcpServer.getName())){
            _dcDao.loadDetails(dc);
            String dhcpStrategy = dc.getDetail(ZoneConfig.DhcpStrategy.key());
            if ("external".equalsIgnoreCase(dhcpStrategy)) {
               return true;
            }
        } 
        
        return false;
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException, ConcurrentOperationException, InsufficientCapacityException {
        if (!canHandle(dest, offering.getTrafficType())) {
            return false;
        }
        
        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vmProfile, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        if (canHandle(dest, network.getTrafficType())) {
            
            if (vmProfile.getType() != VirtualMachine.Type.User) {
                return false;
            }
            @SuppressWarnings("unchecked")
            VirtualMachineProfile<UserVm> uservm = (VirtualMachineProfile<UserVm>)vmProfile;
            _userVmDao.loadDetails((UserVmVO) uservm.getVirtualMachine());
            String password = (String)uservm.getParameter(VirtualMachineProfile.Param.VmPassword);
            String userData = uservm.getVirtualMachine().getUserData();
            String sshPublicKey = uservm.getVirtualMachine().getDetail("SSH.PublicKey");

            Commands cmds = new Commands(OnError.Continue);
            if (password != null && network.isDefault()) {
                final String encodedPassword = PasswordGenerator.rot13(password);
                SavePasswordCommand cmd = new SavePasswordCommand(encodedPassword, nic.getIp4Address(), uservm.getVirtualMachine().getHostName());
                cmds.addCommand("password", cmd);
            }
            String serviceOffering = _serviceOfferingDao.findByIdIncludingRemoved(uservm.getServiceOfferingId()).getDisplayText();
            String zoneName = _dcDao.findById(network.getDataCenterId()).getName();

            cmds.addCommand(
                    "vmdata",
                    generateVmDataCommand(nic.getIp4Address(), userData, serviceOffering, zoneName, nic.getIp4Address(), uservm.getVirtualMachine().getHostName(), uservm.getVirtualMachine().getInstanceName(), uservm.getId(), sshPublicKey));
            try {
                _agentManager.send(dest.getHost().getId(), cmds);
            } catch (OperationTimedoutException e) {
                s_logger.debug("Unable to send vm data command to host " + dest.getHost());
                return false;
            }
            Answer dataAnswer = cmds.getAnswer("vmdata");
            if (dataAnswer != null && dataAnswer.getResult()) {
                s_logger.info("Sent vm data successfully to vm " + uservm.getVirtualMachine().getInstanceName());
                return true;
            }
            s_logger.info("Failed to send vm data to vm " + uservm.getVirtualMachine().getInstanceName());
            return false;
        } else {
            return false;
        }
    }

    @Override
    public boolean release(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, ReservationContext context) {
        return true;
    }
    
    @Override
    public boolean shutdown(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException {
       return false; //assume that the agent will remove userdata etc
    }
    
    @Override
    public boolean destroy(Network config) throws ConcurrentOperationException, ResourceUnavailableException{
        return false; //assume that the agent will remove userdata etc
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
    
    private VmDataCommand generateVmDataCommand( String vmPrivateIpAddress,
            String userData, String serviceOffering, String zoneName, String guestIpAddress, String vmName, String vmInstanceName, long vmId, String publicKey) {
        VmDataCommand cmd = new VmDataCommand(vmPrivateIpAddress, vmName);
        
        cmd.addVmData("userdata", "user-data", userData);
        cmd.addVmData("metadata", "service-offering", serviceOffering);
        cmd.addVmData("metadata", "availability-zone", zoneName);
        cmd.addVmData("metadata", "local-ipv4", guestIpAddress);
        cmd.addVmData("metadata", "local-hostname", vmName);
        cmd.addVmData("metadata", "public-ipv4", guestIpAddress);
        cmd.addVmData("metadata", "public-hostname", guestIpAddress);
        cmd.addVmData("metadata", "instance-id", vmInstanceName);
        cmd.addVmData("metadata", "vm-id", String.valueOf(vmId));
        cmd.addVmData("metadata", "public-keys", publicKey);

        return cmd;
    }
}
