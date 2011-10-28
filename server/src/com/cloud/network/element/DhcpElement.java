/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.api.commands.ConfigureDhcpElementCmd;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.VirtualRouterElements;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.NetworkManager;
import com.cloud.network.Networks.TrafficType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.VirtualRouterElementsDao;
import com.cloud.network.VirtualRouterElements.VirtualRouterElementsType;
import com.cloud.network.router.VirtualNetworkApplianceManager;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VirtualRouter.Role;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.AccountManager;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.State;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;


@Local(value=NetworkElement.class)
public class DhcpElement extends AdapterBase implements DhcpElementService, UserDataServiceProvider {
    private static final Logger s_logger = Logger.getLogger(DhcpElement.class);
    
    private static final Map<Service, Map<Capability, String>> capabilities = setCapabilities();
    
    @Inject NetworkDao _networkConfigDao;
    @Inject NetworkManager _networkMgr;
    @Inject VirtualNetworkApplianceManager _routerMgr;
    @Inject UserVmManager _userVmMgr;
    @Inject UserVmDao _userVmDao;
    @Inject DomainRouterDao _routerDao;
    @Inject ConfigurationManager _configMgr;
    @Inject HostPodDao _podDao;
    @Inject AccountManager _accountMgr;
    @Inject HostDao _hostDao;
    @Inject VirtualRouterElementsDao _vrElementsDao;
     
    private boolean canHandle(DeployDestination dest, TrafficType trafficType, GuestType networkType, long offeringId) {
        if (_networkMgr.isProviderSupported(offeringId, Service.Gateway, Provider.JuniperSRX) && networkType == Network.GuestType.Isolated) {
            return true;
        } else if (dest.getPod() != null && dest.getPod().getExternalDhcp()){
        	//This pod is using external DHCP server
        	return false;
        } else {
            return (networkType == Network.GuestType.Shared);
        } 
    }

    @Override
    public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws ResourceUnavailableException, ConcurrentOperationException, InsufficientCapacityException {
        if (!canHandle(dest, offering.getTrafficType(), network.getGuestType(), network.getNetworkOfferingId())) {
            return false;
        }
        
        Map<VirtualMachineProfile.Param, Object> params = new HashMap<VirtualMachineProfile.Param, Object>(1);
        params.put(VirtualMachineProfile.Param.ReProgramNetwork, true);
        _routerMgr.deployDhcp(network, dest, _accountMgr.getAccount(network.getAccountId()), params);
        return true;
    }

    @Override
    public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, InsufficientCapacityException, ResourceUnavailableException {
        if (canHandle(dest, network.getTrafficType(), network.getGuestType(), network.getNetworkOfferingId())) {
            
            if (vm.getType() != VirtualMachine.Type.User) {
                return false;
            }
            
            @SuppressWarnings("unchecked")
            VirtualMachineProfile<UserVm> uservm = (VirtualMachineProfile<UserVm>)vm;
            Map<VirtualMachineProfile.Param, Object> params = new HashMap<VirtualMachineProfile.Param, Object>(1);
            params.put(VirtualMachineProfile.Param.ReProgramNetwork, true);

            List<DomainRouterVO> routers = _routerMgr.deployDhcp(network, dest, _accountMgr.getAccount(network.getAccountId()), uservm.getParameters());
            
            //for Basic zone, add all Running routers - we have to send Dhcp/vmData/password info to them when network.dns.basiczone.updates is set to "all"
            Long podId = dest.getPod().getId();
            DataCenter dc = dest.getDataCenter();
            boolean isPodBased = (dc.getNetworkType() == NetworkType.Basic || _networkMgr.isSecurityGroupSupportedInNetwork(network)) && network.getTrafficType() == TrafficType.Guest;
            if (isPodBased && _routerMgr.getDnsBasicZoneUpdate().equalsIgnoreCase("all")) {
                List<DomainRouterVO> allRunningRoutersOutsideThePod = _routerDao.findByNetworkOutsideThePod(network.getId(), podId, State.Running, Role.DHCP_USERDATA);
                routers.addAll(allRunningRoutersOutsideThePod);
            }
            
            List<VirtualRouter> rets = _routerMgr.addVirtualMachineIntoNetwork(network, nic, uservm, dest, context, routers);                                                                                                                      
            return (rets != null) && (!rets.isEmpty());
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
        List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.DHCP_USERDATA);
        if (routers == null || routers.isEmpty()) {
            return true;
        }
        boolean result = true;
        for (DomainRouterVO router : routers) {
            result = result && _routerMgr.stop(router, false, context.getCaller(), context.getAccount()) != null;
        }
        return result;
    }
    
    @Override
    public boolean destroy(Network config) throws ConcurrentOperationException, ResourceUnavailableException{
        List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(config.getId(), Role.DHCP_USERDATA);
        if (routers == null || routers.isEmpty()) {
            return true;
        }
        boolean result = true;
        for (DomainRouterVO router : routers) {
            result = result && (_routerMgr.destroyRouter(router.getId()) != null);
        }
        return result;
    }

    @Override
    public Provider getProvider() {
        return Provider.DhcpServer;
    }
    
    @Override
    public Map<Service, Map<Capability, String>> getCapabilities() {
        return capabilities;
    }
    
    private static Map<Service, Map<Capability, String>> setCapabilities() {
        Map<Service, Map<Capability, String>> capabilities = new HashMap<Service, Map<Capability, String>>();
        
        Map<Capability, String> dnsCapabilities = new HashMap<Capability, String>();
        dnsCapabilities.put(Capability.AllowDnsSuffixModification, "true");
        capabilities.put(Service.Dns, dnsCapabilities);  
        
        capabilities.put(Service.UserData, null);
        capabilities.put(Service.Dhcp, null);
        
        return capabilities;
    }
    
    @Override
    public boolean savePassword(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm) throws ResourceUnavailableException{

        List<DomainRouterVO> routers = _routerDao.listByNetworkAndRole(network.getId(), Role.DHCP_USERDATA);
        if (routers == null || routers.isEmpty()) {
            s_logger.trace("Can't find dhcp element in network " + network.getId());
            return true;
        }
        
        @SuppressWarnings("unchecked")
        VirtualMachineProfile<UserVm> uservm = (VirtualMachineProfile<UserVm>)vm;
 
        return _routerMgr.savePasswordToRouter(network, nic, uservm, routers);
    }

    @Override
    public String getPropertiesFile() {
        return "virtualrouter_commands.properties";
    }
    
    @Override
    public boolean configure(ConfigureDhcpElementCmd cmd) {
        VirtualRouterElementsVO element = _vrElementsDao.findByNspIdAndType(cmd.getNspId(), VirtualRouterElementsType.DhcpElement);
        if (element == null) {
            s_logger.trace("Can't find element with network service provider ID " + cmd.getNspId());
            return false;
        }
        element.setEnabled(cmd.getEnabled());
        
        _vrElementsDao.persist(element);
        
        return true;
    }
    
    @Override
    public VirtualRouterElements addElement(Long nspId) {
        long serviceOfferingId = _routerMgr.getDefaultVirtualRouterServiceOfferingId();
        if (serviceOfferingId == 0) {
            return null;
        }
        VirtualRouterElementsVO element = _vrElementsDao.findByNspIdAndType(nspId, VirtualRouterElementsType.DhcpElement);
        if (element != null) {
            s_logger.trace("There is already a dhcp element with service provider id " + nspId);
            return null;
        }
        element = new VirtualRouterElementsVO(nspId, null, VirtualRouterElementsType.DhcpElement);
        _vrElementsDao.persist(element);
        return element;
    }
    
    @Override
    public Long getIdByNspId(Long nspId) {
        VirtualRouterElementsVO element = _vrElementsDao.findByNspIdAndType(nspId, VirtualRouterElementsType.DhcpElement);
        return element.getId();
    }
    
    @Override
    public boolean isReady(long nspId) {
        VirtualRouterElementsVO element = _vrElementsDao.findByNspIdAndType(nspId, VirtualRouterElementsType.DhcpElement);
        if (element == null) {
            return false;
        }
        return element.isEnabled();
    }

    @Override
    public VirtualRouterElements getCreatedElement(long id) {
        return _vrElementsDao.findById(id);
    }

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context, boolean forceShutdown) throws ConcurrentOperationException,
            ResourceUnavailableException {
        // TODO Auto-generated method stub
        return true;
    }
}
