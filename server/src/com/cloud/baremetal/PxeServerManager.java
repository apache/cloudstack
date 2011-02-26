package com.cloud.baremetal;

import com.cloud.agent.api.baremetal.PrepareLinMinPxeServerCommand;
import com.cloud.api.AddPxeServerCmd;
import com.cloud.api.response.PxeServerResponse;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.utils.component.Manager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachineProfile;

public interface PxeServerManager extends Manager {
	public static class PxeServerType {
		private String _name;
		
		public static final PxeServerType LinMin = new PxeServerType("LinMin");
		public static final PxeServerType DMCD = new PxeServerType("DMCD");
		
		public PxeServerType(String name) {
			_name = name;
		}
		
		public String getName() {
			return _name;
		}
		
	}
	
	public Host addPxeServer(AddPxeServerCmd cmd) throws InvalidParameterValueException, CloudRuntimeException;

	public PxeServerResponse getApiResponse(Host pxeServer);

	public boolean prepare(VirtualMachineProfile<UserVmVO> profile, DeployDestination dest, ReservationContext context, Long pxeServerId);
}
