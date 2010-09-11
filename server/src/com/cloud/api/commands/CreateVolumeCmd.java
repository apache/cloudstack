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

import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.VolumeResponse;
import com.cloud.serializer.SerializerHelper;
import com.cloud.storage.VolumeVO;

@Implementation(createMethod="createVolumeDB", method="createVolume", manager=Manager.StorageManager)
public class CreateVolumeCmd extends BaseAsyncCreateCmd {
	public static final Logger s_logger = Logger.getLogger(CreateVolumeCmd.class.getName());
    private static final String s_name = "createvolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING)
    private String accountName;

    @Parameter(name="diskofferingid", type=CommandType.LONG)
    private Long diskOfferingId;

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="name", type=CommandType.STRING, required=true)
    private String volumeName;

    @Parameter(name="size", type=CommandType.LONG)
    private Long size;

    @Parameter(name="snapshotid", type=CommandType.LONG)
    private Long snapshotId;

    @Parameter(name="zoneid", type=CommandType.LONG)
    private Long zoneId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public Long getSize() {
        return size;
    }

    public Long getSnapshotId() {
        return snapshotId;
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
    
    public static String getResultObjectName() {
    	return "volume";
    }
    
    @Override
    public String getResponse() {
        VolumeVO volume = (VolumeVO)getResponseObject();

        VolumeResponse response = new VolumeResponse();
        response.setId(volume.getId());
        response.setName(param.getName());
        response.setVolumeType(volume.getVolumeType().toString());
        response.setSize(volume.getSize());
        response.setCreated(volume.getCreated());
        response.setState(volume.getStatus().toString());
        response.setAccountName(ggetManagementServer().findAccountById(volume.getAccountId()).getAccountName());
        response.setDomainId(volume.getDomainId());
        response.setDiskOfferingId(volume.getDiskOfferingId());
        
        if (volume.getDiskOfferingId() != null) {
            response.setDiskOfferingName(getManagementServer().findDiskOfferingById(volume.getDiskOfferingId()).getName());
            response.setDiskOfferingDisplayText(getManagementServer().findDiskOfferingById(volume.getDiskOfferingId()).getDisplayText());
        }
        response.setDomain(getManagementServer().findDomainIdById(volume.getDomainId()).getName());
        response.setStorageType("shared"); // NOTE: You can never create a local disk volume but if that changes, we need to change this
        if (volume.getPoolId() != null)
            response.setStorage(getManagementServer().findPoolById(volume.getPoolId()).getName());
        response.setZoneId(volume.getDataCenterId());
        response.setZoneName(getManagementServer().getDataCenterBy(volume.getDataCenterId()).getName());

        return SerializerHelper.toSerializedString(response);
    }
}
