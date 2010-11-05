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

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientNetworkCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network.TrafficType;
import com.cloud.network.NetworkConfiguration;
import com.cloud.network.NetworkManager;
import com.cloud.network.dao.NetworkConfigurationDao;
import com.cloud.network.router.DomainRouterManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachine.Type;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.DomainRouterDao;
import com.cloud.vm.dao.UserVmDao;


@Local(value=NetworkElement.class)
public class DomainRouterElement extends AdapterBase implements NetworkElement {
    private static final Logger s_logger = Logger.getLogger(DomainRouterElement.class);
    
    @Inject NetworkConfigurationDao _networkConfigDao;
    @Inject NetworkManager _networkMgr;
    @Inject DomainRouterManager _routerMgr;
    @Inject UserVmManager _userVmMgr;
    @Inject UserVmDao _userVmDao;
    @Inject DomainRouterDao _routerDao;

    @Override
    public boolean implement(NetworkConfiguration guestConfig, NetworkOffering offering, DeployDestination dest, ReservationContext context) throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException {
        if (offering.getGuestIpType() != GuestIpType.Virtualized) {
            s_logger.trace("Not handling guest ip type = " + offering.getGuestIpType());
            return false;
        }
        
        DomainRouterVO router = _routerMgr.deploy(guestConfig, dest, context.getAccount());
        if (router == null) {
            throw new ResourceUnavailableException("Unable to deploy the router for " + guestConfig);
        }
        
        return true;
    }

    @Override
    public boolean prepare(NetworkConfiguration config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest, ReservationContext context) throws ConcurrentOperationException, InsufficientNetworkCapacityException, ResourceUnavailableException {
        if (config.getTrafficType() != TrafficType.Guest || vm.getType() != Type.User) {
            s_logger.trace("Domain Router only cares about guest network and User VMs");
            return false;
        }
        
        if (vm.getType() != VirtualMachine.Type.User) {
            return false;
        }
        
        @SuppressWarnings("unchecked")
        VirtualMachineProfile<UserVm> uservm = (VirtualMachineProfile<UserVm>)vm;
        
        return _routerMgr.addVirtualMachineIntoNetwork(config, nic, uservm, dest, context) != null;
    }

    @Override
    public boolean release(NetworkConfiguration config, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, ReservationContext context) {
        if (config.getTrafficType() != TrafficType.Guest || vm.getType() != Type.User) {
            s_logger.trace("Domain Router only cares about guest network and User VMs");
            return false;
        }
        
        
        return true;
    }

    @Override
    public boolean shutdown(NetworkConfiguration config, ReservationContext context) throws ConcurrentOperationException {
        if (config.getTrafficType() != TrafficType.Guest) {
            s_logger.trace("Domain Router only cares about guet network.");
            return false;
        }
        DomainRouterVO router = _routerDao.findByNetworkConfiguration(config.getId());
        if (router == null) {
            return true;
        }
        return _routerMgr.stopRouter(router.getId(), 1);
    }

    protected DomainRouterElement() {
        super();
    }
}
