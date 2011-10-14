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
 */

package com.cloud.network.element;

import java.util.Map;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.baremetal.ExternalDhcpManager;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.dc.Pod;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.GuestIpType;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.network.Network.Type;
import com.cloud.network.Networks.TrafficType;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value=NetworkElement.class)
public class ExternalDhcpElement extends AdapterBase implements NetworkElement {
	private static final Logger s_logger = Logger.getLogger(ExternalDhcpElement.class);
	@Inject ExternalDhcpManager _dhcpMgr;
	private boolean canHandle(DeployDestination dest, TrafficType trafficType, Type networkType) {
		DataCenter dc = dest.getDataCenter();
		Pod pod = dest.getPod();
		
		if (pod.getExternalDhcp() && dc.getNetworkType() == NetworkType.Basic && trafficType == TrafficType.Guest
				&& networkType == Network.Type.Shared) {
			s_logger.debug("External DHCP can handle");
			return true;
		}

		return false;
	}

	@Override
	public Map<Service, Map<Capability, String>> getCapabilities() {
		return null;
	}

	@Override
	public Provider getProvider() {
		return Provider.ExternalDhcpServer;
	}

	@Override
	public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
		if (!canHandle(dest, offering.getTrafficType(), network.getType())) {
			return false;
		}
		return true;
	}

	@Override
	public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest,
			ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
		Host host = dest.getHost();
		if (host.getHypervisorType() == HypervisorType.BareMetal || !canHandle(dest, network.getTrafficType(), network.getType())) {
			//BareMetalElement or DhcpElement handle this
			return false;
		}
		
		return _dhcpMgr.addVirtualMachineIntoNetwork(network, nic, vm, dest, context);
	}

	@Override
	public boolean release(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException {
		return true;
	}

	@Override
	public boolean shutdown(Network network, ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException {
		return true;
	}

	@Override
	public boolean restart(Network network, ReservationContext context, boolean cleanup) throws ConcurrentOperationException, ResourceUnavailableException,
			InsufficientCapacityException {
		return true;
	}

	@Override
	public boolean destroy(Network network) throws ConcurrentOperationException, ResourceUnavailableException {
		return true;
	}
}
