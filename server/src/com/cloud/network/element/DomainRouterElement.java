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

import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network.TrafficType;
import com.cloud.network.NetworkConfiguration;
import com.cloud.network.NetworkConfiguration.State;
import com.cloud.network.NetworkConfigurationVO;
import com.cloud.network.NetworkManager;
import com.cloud.network.dao.NetworkConfigurationDao;
import com.cloud.network.router.DomainRouterManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.offering.NetworkOffering.GuestIpType;
import com.cloud.user.Account;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.vm.DomainRouterVO;
import com.cloud.vm.NicProfile;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmVO;
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
    public Boolean implement(NetworkConfiguration config, NetworkOffering offering, Account user) throws InsufficientCapacityException, ResourceUnavailableException, ConcurrentOperationException {
        if (offering.getGuestIpType() != GuestIpType.Virtualized) {
            s_logger.trace("Not handling guest ip type = " + offering.getGuestIpType());
            return null;
        }
        
        List<NetworkConfigurationVO> configs = _networkConfigDao.getRelatedNetworkConfigurations(config.getRelated());
        NetworkConfigurationVO publicConfig = null;
        NetworkConfigurationVO guestConfig = null;
        for (NetworkConfigurationVO c : configs) {
            if (c.getState() != State.Implemented && c.getState() != State.Setup) {
                s_logger.debug("Not all network is ready to be implemented yet.");
                return true;
            }
            if (c.getTrafficType() == TrafficType.Public) {
                publicConfig = c;
            } else if (c.getTrafficType() == TrafficType.Guest) {
                guestConfig = c;
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("The network configurations are different than what I expected: " + c);
                }
                return null;
            }
        }
        
        if (publicConfig == null || guestConfig == null) {
            s_logger.debug("Expected to find the network configuration for " + (publicConfig == null ? "public" : "") + (guestConfig == null ? " guest" : ""));
            return null;
        }
        
        DomainRouterVO router;
        router = _routerMgr.deploy(publicConfig, guestConfig, offering, user);
        if (router == null) {
            s_logger.debug("Unable to deploy the router for " + guestConfig);
            return false;
        }
        
        return true;
    }

    @Override
    public Boolean prepare(NetworkConfiguration config, NicProfile nic, VirtualMachineProfile vm, NetworkOffering offering, Account user) throws ConcurrentOperationException {
        if (config.getTrafficType() != TrafficType.Guest || vm.getType() != Type.User) {
            s_logger.trace("Domain Router only cares about guest network and User VMs");
            return null;  
        }
        
        UserVmVO userVm = _userVmDao.findById(vm.getId());
        
        return _routerMgr.addVirtualMachineToGuestNetwork(userVm, vm.getPassword(), 1) != null; 
    }

    @Override
    public Boolean release(NetworkConfiguration config, NicProfile nic, VirtualMachineProfile vm, NetworkOffering offering, Account user) {
        if (config.getTrafficType() != TrafficType.Guest || vm.getType() != Type.User) {
            s_logger.trace("Domain Router only cares about guest network and User VMs");
            return null;  
        }
        
        
        return true;
    }

    @Override
    public Boolean shutdown(NetworkConfiguration config, NetworkOffering offering, Account user) throws ConcurrentOperationException {
        if (config.getTrafficType() != TrafficType.Guest) {
            s_logger.trace("Domain Router only cares about guet network.");
            return null;
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
