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

import java.util.Map;

import javax.ejb.Local;

import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.network.PhysicalNetworkServiceProvider;
import com.cloud.network.ovs.OvsNetworkManager;
import com.cloud.network.ovs.OvsTunnelManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value=NetworkElement.class)
public class OvsElement extends AdapterBase implements NetworkElement {
	@Inject OvsNetworkManager _ovsVlanMgr;
	@Inject OvsTunnelManager _ovsTunnelMgr;
	
	@Override
	public boolean destroy(Network network)
			throws ConcurrentOperationException, ResourceUnavailableException {
		return true;
	}

	@Override
	public Map<Service, Map<Capability, String>> getCapabilities() {
		return null;
	}

	@Override
	public Provider getProvider() {
		return null;
	}

	@Override
	public boolean implement(Network network, NetworkOffering offering,
			DeployDestination dest, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException,
			InsufficientCapacityException {
		return true;
	}

	@Override
	public boolean prepare(Network network, NicProfile nic,
			VirtualMachineProfile<? extends VirtualMachine> vm,
			DeployDestination dest, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException,
			InsufficientCapacityException {
		if (nic.getBroadcastType() != Networks.BroadcastDomainType.Vswitch) {
			return true;
		}
		
		if (nic.getTrafficType() != Networks.TrafficType.Guest) {
		    return true;
		}
		
		_ovsVlanMgr.VmCheckAndCreateTunnel(vm, dest);
		String command = _ovsVlanMgr.applyDefaultFlow(vm.getVirtualMachine(), dest);
		if (command != null) {
		    nic.setBroadcastUri(BroadcastDomainType.Vswitch.toUri(command));
		}
		_ovsTunnelMgr.VmCheckAndCreateTunnel(vm, dest);
		
		return true;
	}

	@Override
	public boolean release(Network network, NicProfile nic,
			VirtualMachineProfile<? extends VirtualMachine> vm,
			ReservationContext context) throws ConcurrentOperationException,
			ResourceUnavailableException {
	    if (nic.getBroadcastType() != Networks.BroadcastDomainType.Vswitch) {
            return true;
        }
        
        if (nic.getTrafficType() != Networks.TrafficType.Guest) {
            return true;
        }
        
		_ovsTunnelMgr.CheckAndDestroyTunnel(vm.getVirtualMachine());
		return true;
	}
	
	
	@Override
	public boolean shutdown(Network network, ReservationContext context, boolean cleanup)
			throws ConcurrentOperationException, ResourceUnavailableException {
		return true;
	}

    @Override
    public boolean isReady(PhysicalNetworkServiceProvider provider) {
        return true;
    }

    @Override
    public boolean shutdownProviderInstances(PhysicalNetworkServiceProvider provider, ReservationContext context, boolean forceShutdown) 
    throws ConcurrentOperationException, ResourceUnavailableException {
        return true;
    }
}
