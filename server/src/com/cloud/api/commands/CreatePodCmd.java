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

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.PodResponse;
import com.cloud.dc.HostPodVO;
import com.cloud.serializer.SerializerHelper;

@Implementation(method="createPod", manager=Manager.ConfigManager)
public class CreatePodCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreatePodCmd.class.getName());

    private static final String s_name = "createpodresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="cidr", type=CommandType.STRING, required=true)
    private String cidr;

    @Parameter(name="endip", type=CommandType.STRING)
    private String endIp;

    @Parameter(name="gateway", type=CommandType.STRING, required=true)
    private String gateway;

    @Parameter(name="name", type=CommandType.STRING, required=true)
    private String podName;

    @Parameter(name="startip", type=CommandType.STRING, required=true)
    private String startIp;

    @Parameter(name="zoneid", type=CommandType.LONG, required=true)
    private Long zoneId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getCidr() {
        return cidr;
    }

    public String getEndIp() {
        return endIp;
    }

    public String getGateway() {
        return gateway;
    }

    public String getPodName() {
        return podName;
    }

    public String getStartIp() {
        return startIp;
    }

    public Long getZoneId() {
        return zoneId;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public String getName() {
        return s_name;
    }

    @Override
    public String getResponse() {
        HostPodVO pod = (HostPodVO)getResponseObject();

        PodResponse response = new PodResponse();
        response.setId(pod.getId());
        response.setCidr(pod.getCidrAddress() + "/" + pod.getCidrSize());
        // TODO: implement
//        response.setEndIp(pod.getEndIp());
//      response.setStartIp(pod.getStartIp());
//      response.setZoneName(pod.getZoneName());
        response.setGateway(pod.getGateway());
        response.setName(pod.getName());
        response.setZoneId(pod.getDataCenterId());

        return SerializerHelper.toSerializedString(response);
    }
}
