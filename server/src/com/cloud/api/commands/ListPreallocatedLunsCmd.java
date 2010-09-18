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

import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.PreallocatedLunResponse;
import com.cloud.storage.preallocatedlun.PreallocatedLunVO;

@Implementation(method="getPreAllocatedLuns")
public class ListPreallocatedLunsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListPreallocatedLunsCmd.class.getName());

    private static final String s_name = "listpreallocatedlunsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="scope", type=CommandType.STRING)
    private String scope;

    @Parameter(name="targetiqn", type=CommandType.STRING)
    private String targetIqn;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getScope() {
        return scope;
    }

    public String getTargetIqn() {
        return targetIqn;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public String getResponse() {
        List<PreallocatedLunVO> preallocatedLuns = (List<PreallocatedLunVO>)getResponseObject();

        ListResponse response = new ListResponse();
        List<PreallocatedLunResponse> lunResponses = new ArrayList<PreallocatedLunResponse>();
        for (PreallocatedLunVO preallocatedLun : preallocatedLuns) {
            PreallocatedLunResponse preallocLunResponse = new PreallocatedLunResponse();
            preallocLunResponse.setId(preallocatedLun.getId());
            preallocLunResponse.setVolumeId(preallocatedLun.getVolumeId());
            preallocLunResponse.setZoneId(preallocatedLun.getDataCenterId());
            preallocLunResponse.setLun(preallocatedLun.getLun());
            preallocLunResponse.setPortal(preallocatedLun.getPortal());
            preallocLunResponse.setSize(preallocatedLun.getSize());
            preallocLunResponse.setTaken(preallocatedLun.getTaken());
            preallocLunResponse.setTargetIqn(preallocatedLun.getTargetIqn());

            preallocLunResponse.setResponseName("preallocatedlun");
            lunResponses.add(preallocLunResponse);
        }

        response.setResponses(lunResponses);
        response.setResponseName(getName());
        return ApiResponseSerializer.toSerializedString(response);
    }
}