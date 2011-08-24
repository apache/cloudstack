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

import java.util.Map;

import javax.naming.ConfigurationException;

import com.cloud.agent.AgentManager;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.host.Host;
import com.cloud.host.dao.HostDao;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;

public abstract class BareMetalPxeServiceBase implements PxeServerService {
	protected String _name;
	@Inject DataCenterDao _dcDao;
	@Inject HostDao _hostDao;
	@Inject AgentManager _agentMgr;
	@Inject ExternalDhcpManager exDhcpMgr;
	@Inject HostPodDao _podDao;
	@Inject NicDao _nicDao;
	
	@Override
	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		_name = name;
		return true;
	}

	@Override
	public String getName() {
		return _name;
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
	public boolean prepare(VirtualMachineProfile<UserVmVO> profile, DeployDestination dest, ReservationContext context, Long pxeServerId) {
		throw new CloudRuntimeException("Dervied class should implement this method");
	}
	
	protected String getPxeServerGuid(String zoneId, String name, String ip) {
		return zoneId + "-" + name + "-" + ip;
	}

	@Override
	public abstract Host addPxeServer(PxeServerProfile profile);
}
