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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseCmd;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ResponseObject;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.VolumeResponse;
import com.cloud.async.AsyncJobVO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;
import com.cloud.vm.VMInstanceVO;

@Implementation(method="searchForVolumes")
public class ListVolumesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListVolumesCmd.class.getName());

    private static final String s_name = "listvolumesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="hostid", type=CommandType.LONG)
    private Long hostId;

    @Parameter(name="id", type=CommandType.LONG)
    private Long id;

    @Parameter(name="name", type=CommandType.STRING)
    private String volumeName;

    @Parameter(name="podid", type=CommandType.LONG)
    private Long podId;

    @Parameter(name="type", type=CommandType.STRING)
    private String type;

    @Parameter(name="virtualmachineid", type=CommandType.LONG)
    private Long virtualMachineId;

    @Parameter(name="zoneid", type=CommandType.LONG)
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
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public ResponseObject getResponse() {
        List<VolumeVO> volumes = (List<VolumeVO>)getResponseObject();

        ListResponse response = new ListResponse();
        List<VolumeResponse> volResponses = new ArrayList<VolumeResponse>();
        for (VolumeVO volume : volumes) {
            VolumeResponse volResponse = new VolumeResponse();
            volResponse.setId(volume.getId());

            AsyncJobVO asyncJob = ApiDBUtils.findInstancePendingAsyncJob("volume", volume.getId());
            if (asyncJob != null) {
                volResponse.setJobId(asyncJob.getId());
                volResponse.setJobStatus(asyncJob.getStatus());
            } 

            if (volume.getName() != null) {
                volResponse.setName(volume.getName());
            } else {
                volResponse.setName("");
            }
            
            volResponse.setZoneId(volume.getDataCenterId());
            volResponse.setZoneName(ApiDBUtils.findZoneById(volume.getDataCenterId()).getName());

            volResponse.setVolumeType(volume.getVolumeType().toString());
            volResponse.setDeviceId(volume.getDeviceId());
            
            Long instanceId = volume.getInstanceId();
            if (instanceId != null) {
                VMInstanceVO vm = ApiDBUtils.findVMInstanceById(instanceId);
                volResponse.setVirtualMachineId(vm.getId());
                volResponse.setVirtualMachineName(vm.getName());
                volResponse.setVirtualMachineDisplayName(vm.getName());
                volResponse.setVirtualMachineState(vm.getState().toString());
            }             

            // Show the virtual size of the volume
            volResponse.setSize(volume.getSize());

            volResponse.setCreated(volume.getCreated());
            volResponse.setState(volume.getStatus().toString());
            
            Account accountTemp = ApiDBUtils.findAccountById(volume.getAccountId());
            if (accountTemp != null) {
                volResponse.setAccountName(accountTemp.getAccountName());
                volResponse.setDomainId(accountTemp.getDomainId());
                volResponse.setDomainName(ApiDBUtils.findDomainById(accountTemp.getDomainId()).getName());
            }

            String storageType;
            try {
                if(volume.getPoolId() == null){
                    storageType = "unknown";
                } else {
                    storageType = ApiDBUtils.volumeIsOnSharedStorage(volume.getId()) ? "shared" : "local";
                }
            } catch (InvalidParameterValueException e) {
                s_logger.error(e.getMessage(), e);
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Volume " + volume.getName() + " does not have a valid ID");
            }

            volResponse.setStorageType(storageType);
            
            volResponse.setDiskOfferingId(volume.getDiskOfferingId());
            if (volume.getDiskOfferingId() != null) {
                DiskOfferingVO diskOffering = ApiDBUtils.findDiskOfferingById(volume.getDiskOfferingId());
                volResponse.setDiskOfferingName(diskOffering.getName());
                volResponse.setDiskOfferingDisplayText(diskOffering.getDisplayText());
            }

            Long poolId = volume.getPoolId();
            String poolName = (poolId == null) ? "none" : ApiDBUtils.findStoragePoolById(poolId).getName();
            volResponse.setStoragePoolName(poolName);

            volResponse.setResponseName("volume");
            volResponses.add(volResponse);
        }

        response.setResponses(volResponses);
        response.setResponseName(getName());
        return response;
    }
}
