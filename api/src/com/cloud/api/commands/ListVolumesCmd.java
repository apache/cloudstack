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
import com.cloud.api.BaseListProjectAndAccountResourcesCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.VolumeResponse;
import com.cloud.async.AsyncJob;
import com.cloud.storage.Volume;

@Implementation(description="Lists all volumes.", responseObject=VolumeResponse.class)
public class ListVolumesCmd extends BaseListProjectAndAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListVolumesCmd.class.getName());

    private static final String s_name = "listvolumesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="host")
    @Parameter(name=ApiConstants.HOST_ID, type=CommandType.LONG, description="list volumes on specified host")
    private Long hostId;

    @IdentityMapper(entityTableName="volumes")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="the ID of the disk volume")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="the name of the disk volume")
    private String volumeName;

    @IdentityMapper(entityTableName="host_pod_ref")
    @Parameter(name=ApiConstants.POD_ID, type=CommandType.LONG, description="the pod id the disk volume belongs to")
    private Long podId;

    @Parameter(name=ApiConstants.TYPE, type=CommandType.STRING, description="the type of disk volume")
    private String type;

    @IdentityMapper(entityTableName="vm_instance")
    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.LONG, description="the ID of the virtual machine")
    private Long virtualMachineId;

    @IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="the ID of the availability zone")
    private Long zoneId;
    
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getHostId() {
        return hostId;
    }

    public Long getId() {
        return id;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public Long getPodId() {
        return podId;
    }

    public String getType() {
        return type;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public Long getZoneId() {
        return zoneId;
    }
    
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    @Override
    public AsyncJob.Type getInstanceType() {
    	return AsyncJob.Type.Volume;
    }
    
    @Override
    public void execute(){
        List<? extends Volume> volumes = _storageService.searchForVolumes(this);

        ListResponse<VolumeResponse> response = new ListResponse<VolumeResponse>();
        List<VolumeResponse> volResponses = new ArrayList<VolumeResponse>();
        for (Volume volume : volumes) {
            VolumeResponse volResponse = _responseGenerator.createVolumeResponse(volume);
            volResponse.setObjectName("volume");
            volResponses.add(volResponse);
        }

        response.setResponses(volResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
