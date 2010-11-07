/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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
package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.VlanIpRangeResponse;
import com.cloud.dc.VlanVO;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;

@Implementation(method="searchForVlans", description="Lists all VLAN IP ranges.")
public class ListVlanIpRangesCmd extends BaseListCmd {
	public static final Logger s_logger = Logger.getLogger(ListVlanIpRangesCmd.class.getName());

    private static final String s_name = "listvlaniprangesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="the account with which the VLAN IP range is associated. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the domain ID with which the VLAN IP range is associated.  If used with the account parameter, returns all VLAN IP ranges for that account in the specified domain.")
    private Long domainId;

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=false, description="the ID of the VLAN IP range")
    private Long id;

    @Parameter(name=ApiConstants.POD_ID, type=CommandType.LONG, description="the Pod ID of the VLAN IP range")
    private Long podId;

    @Parameter(name=ApiConstants.VLAN, type=CommandType.STRING, description="the ID or VID of the VLAN. Default is an \"untagged\" VLAN.")
    private String vlan;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="the Zone ID of the VLAN IP range")
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getId() {
        return id;
    }

    public Long getPodId() {
        return podId;
    }

    public String getVlan() {
        return vlan;
    }

    public Long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public ListResponse<VlanIpRangeResponse> getResponse() {
        List<VlanVO> vlans = (List<VlanVO>)getResponseObject();

        ListResponse<VlanIpRangeResponse> response = new ListResponse<VlanIpRangeResponse>();
        List<VlanIpRangeResponse> vlanResponses = new ArrayList<VlanIpRangeResponse>();
        for (VlanVO vlan : vlans) {  
            VlanIpRangeResponse vlanResponse = ApiResponseHelper.createVlanIpRangeResponse(vlan);
            vlanResponse.setObjectName("vlaniprange");
            vlanResponses.add(vlanResponse);
        }

        response.setResponses(vlanResponses);
        response.setResponseName(getName());
        return response;
    }
    
    @Override
    public Object execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException{
        List<VlanVO> result = _mgr.searchForVlans(this);
        return result;
    }
}
