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
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.ZoneResponse;
import com.cloud.dc.DataCenterVO;

@Implementation(method="listDataCenters")
public class ListZonesByCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListZonesByCmd.class.getName());

    private static final String s_name = "listzonesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.AVAILABLE, type=CommandType.BOOLEAN, description="true if you want to retrieve all available Zones. False if you only want to return the Zones from which you have at least one VM. Default is false.")
    private Boolean available;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the ID of the domain associated with the zone")
    private Long domainId;
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Boolean isAvailable() {
        return available;
    }
    
    public Long getDomainId(){
    	return domainId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public ListResponse<ZoneResponse> getResponse() {
        List<DataCenterVO> dataCenters = (List<DataCenterVO>)getResponseObject();

        ListResponse<ZoneResponse> response = new ListResponse<ZoneResponse>();
        List<ZoneResponse> zoneResponses = new ArrayList<ZoneResponse>();
        for (DataCenterVO dataCenter : dataCenters) {
            ZoneResponse zoneResponse = ApiResponseHelper.createZoneResponse(dataCenter);

            ZoneResponse zoneResponse = new ZoneResponse();
            zoneResponse.setId(dataCenter.getId());
            zoneResponse.setName(dataCenter.getName());

            if ((dataCenter.getDescription() != null) && !dataCenter.getDescription().equalsIgnoreCase("null")) {
                zoneResponse.setDescription(dataCenter.getDescription());
            }

            if ((account == null) || (account.getType() == Account.ACCOUNT_TYPE_ADMIN)) {
                zoneResponse.setDns1(dataCenter.getDns1());
                zoneResponse.setDns2(dataCenter.getDns2());
                zoneResponse.setInternalDns1(dataCenter.getInternalDns1());
                zoneResponse.setInternalDns2(dataCenter.getInternalDns2());
                zoneResponse.setVlan(dataCenter.getVnet());
                zoneResponse.setGuestCidrAddress(dataCenter.getGuestNetworkCidr());
            }

            zoneResponse.setDomain(dataCenter.getDomain());
            zoneResponse.setDomainId(dataCenter.getDomainId());
            zoneResponse.setResponseName("zone");
            zoneResponses.add(zoneResponse);
        }

        response.setResponses(zoneResponses);
        response.setResponseName(getName());
        return response;
    }
}
