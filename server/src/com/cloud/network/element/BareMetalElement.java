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
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.Host;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.Network;
import com.cloud.network.Network.Capability;
import com.cloud.network.Network.Provider;
import com.cloud.network.Network.Service;
import com.cloud.offering.NetworkOffering;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.component.Inject;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;

@Local(value=NetworkElement.class)
public class BareMetalElement extends AdapterBase implements NetworkElement {
	private static final Logger s_logger = Logger.getLogger(BareMetalElement.class);
	@Inject NicDao _nicDao;
	@Inject ExternalDhcpManager _dhcpMgr;
	
	@Override
	public Map<Service, Map<Capability, String>> getCapabilities() {
		return null;
	}

	@Override
	public Provider getProvider() {
		return null;
	}

	@Override
	public boolean implement(Network network, NetworkOffering offering, DeployDestination dest, ReservationContext context)
			throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
		return true;
	}

	@Override @DB
	public boolean prepare(Network network, NicProfile nic, VirtualMachineProfile<? extends VirtualMachine> vm, DeployDestination dest,
			ReservationContext context) throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
		Host host = dest.getHost();
		if (host.getHypervisorType() != HypervisorType.BareMetal) {
			return true;
		}
		
		Transaction txn = Transaction.currentTxn();
        txn.start();
		nic.setMacAddress(host.getPrivateMacAddress());
		NicVO vo = _nicDao.findById(nic.getId());
		assert vo != null : "Where ths nic " + nic.getId() + " going???";
		vo.setMacAddress(nic.getMacAddress());
		_nicDao.update(vo.getId(), vo);
		txn.commit();
		s_logger.debug("Bare Metal changes mac address of nic " + nic.getId() + " to " + nic.getMacAddress());
		
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
