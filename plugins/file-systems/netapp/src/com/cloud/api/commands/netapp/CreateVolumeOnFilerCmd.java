// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.api.commands.netapp;

import java.net.UnknownHostException;
import java.rmi.ServerException;

import javax.inject.Inject;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.netapp.NetappManager;
import com.cloud.server.ManagementService;
import com.cloud.server.api.response.netapp.CreateVolumeOnFilerCmdResponse;


@APICommand(name = "createVolumeOnFiler", description="Create a volume", responseObject = CreateVolumeOnFilerCmdResponse.class)
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
    
    @Inject NetappManager netappMgr;

	@Override
	public void execute() throws ResourceUnavailableException,
			InsufficientCapacityException, ServerApiException,
			ConcurrentOperationException, ResourceAllocationException {
		//param checks
		if(snapshotReservation != null && (snapshotReservation<0 || snapshotReservation>100))
			throw new InvalidParameterValueException("Invalid snapshot reservation");
		
		StringBuilder s = new StringBuilder(getVolSize().toString());
		s.append("g");
	
		try {
			netappMgr.createVolumeOnFiler(ipAddress, aggrName, poolName, volName, s.toString(), snapshotPolicy, snapshotReservation, userName, password);
			CreateVolumeOnFilerCmdResponse response = new CreateVolumeOnFilerCmdResponse();
			response.setResponseName(getCommandName());
			this.setResponseObject(response);
		} catch (ServerException e) {
			throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.toString());
		} catch (InvalidParameterValueException e) {
			throw new ServerApiException(ApiErrorCode.PARAM_ERROR, e.toString());
		} catch (UnknownHostException e) {
			throw new ServerApiException(ApiErrorCode.PARAM_ERROR, e.toString());
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
