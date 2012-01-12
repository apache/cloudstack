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
 * @author-aj
 */
package com.cloud.api.commands.netapp;

import java.net.UnknownHostException;
import java.rmi.ServerException;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.netapp.NetappManager;
import com.cloud.server.ManagementService;
import com.cloud.server.api.response.netapp.CreateVolumeOnFilerCmdResponse;
import com.cloud.utils.component.ComponentLocator;

@Implementation(description="Create a volume", responseObject = CreateVolumeOnFilerCmdResponse.class)
public class CreateVolumeOnFilerCmd extends BaseCmd {
    private static final String s_name = "createvolumeresponse";

    @Parameter(name=ApiConstants.IP_ADDRESS, type=CommandType.STRING, required = true, description="ip address.")
	private String ipAddress;
    
    @Parameter(name=ApiConstants.AGGREGATE_NAME, type=CommandType.STRING, required = true, description="aggregate name.")
	private String aggrName;

    @Parameter(name=ApiConstants.POOL_NAME, type=CommandType.STRING, required = true, description="pool name.")
	private String poolName;
    
    @Parameter(name=ApiConstants.VOLUME_NAME, type=CommandType.STRING, required = true, description="volume name.")
	private String volName;
    
    @Parameter(name=ApiConstants.SIZE, type=CommandType.INTEGER, required = true, description="volume size.")
	private Integer volSize;
    
    @Parameter(name=ApiConstants.SNAPSHOT_POLICY, type=CommandType.STRING, required = false, description="snapshot policy.")
	private String snapshotPolicy;
    
    @Parameter(name=ApiConstants.SNAPSHOT_RESERVATION, type=CommandType.INTEGER, required = false, description="snapshot reservation.")
	private Integer snapshotReservation;
    
    @Parameter(name=ApiConstants.USERNAME, type=CommandType.STRING, required = true, description="user name.")
	private String userName;
    
    @Parameter(name=ApiConstants.PASSWORD, type=CommandType.STRING, required = true, description="password.")
	private String password;
    

    public String getIpAddress() {
    	return ipAddress;
    }
    
    public String getAggrName() {
    	return aggrName;
    }
    
    public String getPoolName() {
    	return poolName;
    }
    
    public String volName() {
    	return volName;
    }
    
    public Integer getVolSize() {
    	return volSize;
    }
    
    public String getSnapshotPolicy() {
    	return snapshotPolicy;
    }
    
    public Integer getSnapshotReservation() {
    	return snapshotReservation;
    }
    
    public String getUserName() {
    	return userName;
    }
    
    public String getPassword() {
    	return password;
    }

	@Override
	public void execute() throws ResourceUnavailableException,
			InsufficientCapacityException, ServerApiException,
			ConcurrentOperationException, ResourceAllocationException {
		//param checks
		if(snapshotReservation != null && (snapshotReservation<0 || snapshotReservation>100))
			throw new InvalidParameterValueException("Invalid snapshot reservation");
		
		ComponentLocator locator = ComponentLocator.getLocator(ManagementService.Name);
    	NetappManager netappMgr = locator.getManager(NetappManager.class);
    	
		StringBuilder s = new StringBuilder(getVolSize().toString());
		s.append("g");
	
		try {
			netappMgr.createVolumeOnFiler(ipAddress, aggrName, poolName, volName, s.toString(), snapshotPolicy, snapshotReservation, userName, password);
			CreateVolumeOnFilerCmdResponse response = new CreateVolumeOnFilerCmdResponse();
			response.setResponseName(getCommandName());
			this.setResponseObject(response);
		} catch (ServerException e) {
			throw new ServerApiException(BaseCmd.INTERNAL_ERROR, e.toString());
		} catch (InvalidParameterValueException e) {
			throw new ServerApiException(BaseCmd.PARAM_ERROR, e.toString());
		} catch (UnknownHostException e) {
			throw new ServerApiException(BaseCmd.PARAM_ERROR, e.toString());
		}
		
	}

	@Override
	public String getCommandName() {
		// TODO Auto-generated method stub
		return s_name;
	}

	@Override
	public long getEntityOwnerId() {
		// TODO Auto-generated method stub
		return 0;
	}
    
}
