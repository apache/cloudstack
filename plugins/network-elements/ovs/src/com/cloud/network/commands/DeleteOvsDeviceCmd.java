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
package com.cloud.network.commands;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;

import com.cloud.api.response.OvsDeviceResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.element.OvsElementService;
import com.cloud.user.UserContext;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "deleteOvsDevice", responseObject = SuccessResponse.class, description = " delete a ovs device")
public class DeleteOvsDeviceCmd extends BaseAsyncCmd {
	private static final String s_name = "deleteovsdeviceresponse";
	@Inject
	OvsElementService _ovsElementService;

	// ///////////////////////////////////////////////////
	// ////////////// API parameters /////////////////////
	// ///////////////////////////////////////////////////

	@Parameter(name = ApiConstants.OVS_DEVICE_ID, type = CommandType.UUID, entityType = OvsDeviceResponse.class, required = true, description = "Ovs device ID")
	private Long ovsDeviceId;

	// ///////////////////////////////////////////////////
	// ///////////////// Accessors ///////////////////////
	// ///////////////////////////////////////////////////

	public Long getOvsDeviceId() {
		return ovsDeviceId;
	}

	// ///////////////////////////////////////////////////
	// ///////////// API Implementation///////////////////
	// ///////////////////////////////////////////////////
	@Override
	public void execute() throws ResourceUnavailableException,
			InsufficientCapacityException, ServerApiException,
			ConcurrentOperationException, ResourceAllocationException,
			NetworkRuleConflictException {
		try {
			boolean result = _ovsElementService.deleteOvsDevice(this);
			if (result) {
				SuccessResponse response = new SuccessResponse(getCommandName());
				response.setResponseName(getCommandName());
				this.setResponseObject(response);
			} else {
				throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
						"Failed to delete Ovs device.");
			}

		} catch (InvalidParameterValueException invalidParamExcp) {
			throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
					invalidParamExcp.getMessage());
		} catch (CloudRuntimeException runtimeExcp) {
			throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
					runtimeExcp.getMessage());
		}
	}

	@Override
	public String getEventType() {
		return EventTypes.EVENT_EXTERNAL_OVS_CONTROLLER_DELETE;
	}

	@Override
	public String getEventDescription() {
		return "Deleting Ovs Controller";
	}

	@Override
	public String getCommandName() {
		return s_name;
	}

	@Override
	public long getEntityOwnerId() {
		return UserContext.current().getCaller().getId();
	}

}
