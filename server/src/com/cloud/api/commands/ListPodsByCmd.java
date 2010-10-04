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
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.PodResponse;
import com.cloud.dc.HostPodVO;
import com.cloud.test.PodZoneConfig;

@Implementation(method="searchForPods")
public class ListPodsByCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListPodsByCmd.class.getName());

    private static final String s_name = "listpodsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG)
    private Long id;

    @Parameter(name="name", type=CommandType.STRING)
    private String podName;

    @Parameter(name="zoneid", type=CommandType.LONG)
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

    @Override @SuppressWarnings("unchecked")
    public ListResponse<PodResponse> getResponse() {
        List<HostPodVO> pods = (List<HostPodVO>)getResponseObject();

        ListResponse<PodResponse> response = new ListResponse<PodResponse>();
        List<PodResponse> podResponses = new ArrayList<PodResponse>();
        for (HostPodVO pod : pods) {
            String[] ipRange = new String[2];
            if (pod.getDescription() != null && pod.getDescription().length() > 0) {
                ipRange = pod.getDescription().split("-");
            } else {
                ipRange[0] = pod.getDescription();
            }

            PodResponse podResponse = new PodResponse();
            podResponse.setId(pod.getId());
            podResponse.setName(pod.getName());
            podResponse.setZoneId(pod.getDataCenterId());
            podResponse.setZoneName(PodZoneConfig.getZoneName(pod.getDataCenterId()));
            podResponse.setCidr(pod.getCidrAddress() +"/" + pod.getCidrSize());
            podResponse.setStartIp(ipRange[0]);
            podResponse.setEndIp(((ipRange.length > 1) && (ipRange[1] != null)) ? ipRange[1] : "");
            podResponse.setGateway(pod.getGateway());

            podResponse.setResponseName("pod");
            podResponses.add(podResponse);
        }

        response.setResponses(podResponses);
        response.setResponseName(getName());
        return response;
    }
}
