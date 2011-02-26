package com.cloud.baremetal;

import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.baremetal.PrepareLinMinPxeServerCommand;
import com.cloud.api.AddPxeServerCmd;
import com.cloud.api.response.PxeServerResponse;
import com.cloud.deploy.DeployDestination;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachineProfile;

@Local(value = {PxeServerManager.class})
public class PxeServerManagerImpl implements PxeServerManager {
	private static final org.apache.log4j.Logger s_logger = Logger.getLogger(PxeServerManagerImpl.class);
	protected String _name;
	
	@Override
	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		_name = name;
		return true;
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

	@Override
	public String getName() {
		return _name;
	}

	protected String getPxeServerGuid(String zoneId, String name, String ip) {
		return zoneId + "-" + name + "-" + ip;
	}
	
	@Override
	public Host addPxeServer(AddPxeServerCmd cmd) throws InvalidParameterValueException, CloudRuntimeException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PxeServerResponse getApiResponse(Host pxeServer) {
		PxeServerResponse response = new PxeServerResponse();
		response.setId(pxeServer.getId());
		return response;
	}


	@Override
	public boolean prepare(VirtualMachineProfile<UserVmVO> profile, DeployDestination dest, ReservationContext context, Long pxeServerId) {
		return true;
	}

}
