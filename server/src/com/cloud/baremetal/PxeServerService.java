/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.baremetal;

import com.cloud.baremetal.PxeServerManager.PxeServerType;
import com.cloud.deploy.DeployDestination;
import com.cloud.host.Host;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.Adapter;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachineProfile;

public interface PxeServerService extends Adapter {
	
	public Host addPxeServer(PxeServerProfile profile);
	
	public boolean prepare(VirtualMachineProfile<UserVmVO> profile, DeployDestination dest, ReservationContext context, Long pxeServerId);

    public boolean prepareCreateTemplate(Long pxeServerId, UserVm vm, String templateUrl);
}
