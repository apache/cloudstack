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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PhysicalNetworkResponse;
import org.apache.log4j.Logger;

import com.cloud.api.response.OvsDeviceResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.element.OvsElementService;
import com.cloud.network.ovs.dao.OvsDeviceVO;
import com.cloud.utils.exception.CloudRuntimeException;

@APICommand(name = "listOvsDevices", responseObject = OvsDeviceResponse.class, description = "Lists Ovs devices")
public class ListOvsDevicesCmd extends BaseListCmd {
	public static final Logger s_logger = Logger
			.getLogger(ListOvsDevicesCmd.class.getName());
	private static final String s_name = "listovsdeviceresponse";
	@Inject
	OvsElementService _ovsElementService;

	// ///////////////////////////////////////////////////
	// ////////////// API parameters /////////////////////
	// ///////////////////////////////////////////////////

	@Parameter(name = ApiConstants.PHYSICAL_NETWORK_ID, type = CommandType.UUID, entityType = PhysicalNetworkResponse.class, description = "the Physical Network ID")
	private Long physicalNetworkId;

	@Parameter(name = ApiConstants.OVS_DEVICE_ID, type = CommandType.UUID, entityType = OvsDeviceResponse.class, description = "ovs device ID")
	private Long ovsDeviceId;

	// ///////////////////////////////////////////////////
	// ///////////////// Accessors ///////////////////////
	// ///////////////////////////////////////////////////

	public Long getOvsDeviceId() {
		return ovsDeviceId;
	}

	public Long getPhysicalNetworkId() {
		return physicalNetworkId;
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
			List<OvsDeviceVO> ovsDevices = _ovsElementService
					.listOvsDevices(this);
			ListResponse<OvsDeviceResponse> response = new ListResponse<OvsDeviceResponse>();
			List<OvsDeviceResponse> ovsDevicesResponse = new ArrayList<OvsDeviceResponse>();

			if (ovsDevices != null && !ovsDevices.isEmpty()) {
				for (OvsDeviceVO ovsDeviceVO : ovsDevices) {
					OvsDeviceResponse ovsDeviceResponse = _ovsElementService
							.createOvsDeviceResponse(ovsDeviceVO);
					ovsDevicesResponse.add(ovsDeviceResponse);
				}
			}

			response.setResponses(ovsDevicesResponse);
			response.setResponseName(getCommandName());
			this.setResponseObject(response);
		} catch (InvalidParameterValueException invalidParamExcp) {
			throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
					invalidParamExcp.getMessage());
		} catch (CloudRuntimeException runtimeExcp) {
			throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR,
					runtimeExcp.getMessage());
		}
	}

	@Override
	public String getCommandName() {
		return s_name;
	}

}
