/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
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
