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
