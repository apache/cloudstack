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

package com.cloud.baremetal;

import javax.ejb.Local;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.hypervisor.HypervisorGuruBase;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.GuestOSVO;
import com.cloud.storage.dao.GuestOSDao;
import com.cloud.utils.component.Inject;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

@Local(value=HypervisorGuru.class)
public class BareMetalGuru extends HypervisorGuruBase implements HypervisorGuru {
	@Inject GuestOSDao _guestOsDao;
	
	protected BareMetalGuru() {
		super();
	}
	
	@Override
	public HypervisorType getHypervisorType() {
		return HypervisorType.BareMetal;
	}

	@Override
	public <T extends VirtualMachine> VirtualMachineTO implement(VirtualMachineProfile<T> vm) {
		VirtualMachineTO to = toVirtualMachineTO(vm);

		// Determine the VM's OS description
		GuestOSVO guestOS = _guestOsDao.findById(vm.getVirtualMachine().getGuestOSId());
		to.setOs(guestOS.getDisplayName());

		return to;
	}
	
	@Override
    public boolean trackVmHostChange() {
    	return true;
    }
}
