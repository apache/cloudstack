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


import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import com.cloud.agent.AgentManager;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupPxeServerCommand;
import com.cloud.baremetal.PxeServerManager.PxeServerType;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.uservm.UserVm;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.Inject;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.UserVmVO;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.VirtualMachineProfile.Param;

@Local(value = {PxeServerManager.class})
public class PxeServerManagerImpl implements PxeServerManager, ResourceStateAdapter {
	private static final org.apache.log4j.Logger s_logger = Logger.getLogger(PxeServerManagerImpl.class);
	protected String _name;
	@Inject DataCenterDao _dcDao;
	@Inject HostDao _hostDao;
	@Inject AgentManager _agentMgr;
	@Inject ExternalDhcpManager exDhcpMgr;
	@Inject ResourceManager _resourceMgr;
	@Inject(adapter=PxeServerService.class)
	protected Adapters<PxeServerService> _services;
	
	@Override
	public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
		_name = name;
		_resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
		return true;
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
    	_resourceMgr.unregisterResourceStateAdapter(this.getClass().getSimpleName());
		return true;
	}

	@Override
	public String getName() {
		return _name;
	}

	protected PxeServerService getServiceByType(String type) {
		PxeServerService _service;
		_service = _services.get(type);
		if (_service == null) {
			throw new CloudRuntimeException("Cannot find PXE service for " + type);
		}
		return _service;
	}
	
	
	@Override
	public Host addPxeServer(PxeServerProfile profile) {
		return getServiceByType(profile.getType()).addPxeServer(profile);
	}

	@Override
	public PxeServerResponse getApiResponse(Host pxeServer) {
		PxeServerResponse response = new PxeServerResponse();
		response.setId(pxeServer.getId());
		return response;
	}

	@Override
	public boolean prepare(PxeServerType type, VirtualMachineProfile<UserVmVO> profile, DeployDestination dest, ReservationContext context, Long pxeServerId) {
		return getServiceByType(type.getName()).prepare(profile, dest, context, pxeServerId);
	}

    @Override
    public boolean prepareCreateTemplate(PxeServerType type, Long pxeServerId, UserVm vm, String templateUrl) {
        return getServiceByType(type.getName()).prepareCreateTemplate(pxeServerId, vm, templateUrl);
    }
    
    @Override
    public PxeServerType getPxeServerType(HostVO host) {
        if (host.getResource().equalsIgnoreCase(PingPxeServerResource.class.getName())) {
            return PxeServerType.PING;
        } else {
            throw new CloudRuntimeException("Unkown PXE server resource " + host.getResource());
        }
    }

	@Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
	    // TODO Auto-generated method stub
	    return null;
    }

	@Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details,
            List<String> hostTags) {
        if (!(startup[0] instanceof StartupPxeServerCommand)) {
            return null;
        }
        
        host.setType(Host.Type.PxeServer);
        return host;
    }

	@Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
	    // TODO Auto-generated method stub
	    return null;
    }
}
