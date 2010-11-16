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
import com.cloud.api.response.PodResponse;
import com.cloud.dc.HostPodVO;

@Implementation(description="Lists all Pods.", responseObject=PodResponse.class)
public class ListPodsByCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListPodsByCmd.class.getName());

    private static final String s_name = "listpodsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="list Pods by ID")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="list Pods by name")
    private String podName;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="list Pods by Zone ID")
    private Long zoneId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getPodName() {
        return podName;
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

    @Override
    public void execute(){
        List<HostPodVO> result = _mgr.searchForPods(this);
        ListResponse<PodResponse> response = new ListResponse<PodResponse>();
        List<PodResponse> podResponses = new ArrayList<PodResponse>();
        for (HostPodVO pod : result) {
            PodResponse podResponse = ApiResponseHelper.createPodResponse(pod);
            podResponse.setObjectName("pod");
            podResponses.add(podResponse);
        }

        response.setResponses(podResponses);
        response.setResponseName(getName());
        this.setResponseObject(response);
    }
}
