/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.baremetal;

import com.cloud.deploy.DeployDestination;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.Manager;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachineProfile;

public interface PxeServerManager extends Manager {
	public static class PxeServerType {
		private String _name;
		
		public static final PxeServerType PING = new PxeServerType("PING");
		public static final PxeServerType DMCD = new PxeServerType("DMCD");
		
		public PxeServerType(String name) {
			_name = name;
		}
		
		public String getName() {
			return _name;
		}
		
	}

	public PxeServerResponse getApiResponse(Host pxeServer);
	
	public boolean prepare(PxeServerType type, VirtualMachineProfile<UserVmVO> profile, DeployDestination dest, ReservationContext context, Long pxeServerId);

	Host addPxeServer(PxeServerProfile profile);
	
	public boolean prepareCreateTemplate(PxeServerType type, Long pxeServerId, UserVm vm, String templateUrl);
	
	public PxeServerType getPxeServerType(HostVO host);
}
