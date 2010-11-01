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

import com.cloud.api.ApiConstants;
import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseAsyncCreateCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.VolumeResponse;
import com.cloud.event.EventTypes;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.StorageManager;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(createMethod="allocVolume", method="createVolume", manager=StorageManager.class, description="Creates a disk volume from a disk offering. " +
																				  "This disk volume must still be attached to a virtual machine to make use of it.")
public class CreateVolumeCmd extends BaseAsyncCreateCmd {
	public static final Logger s_logger = Logger.getLogger(CreateVolumeCmd.class.getName());
    private static final String s_name = "createvolumeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="the account associated with the disk volume. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name=ApiConstants.DISK_OFFERING_ID, type=CommandType.LONG, description="the ID of the disk offering. Either diskOfferingId or snapshotId must be passed in.")
    private Long diskOfferingId;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the domain ID associated with the disk offering. If used with the account parameter returns the disk volume associated with the account for the specified domain.")
    private Long domainId;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, required=true, description="the name of the disk volume")
    private String volumeName;

    @Parameter(name=ApiConstants.SIZE, type=CommandType.LONG, description="Arbitrary volume size. Mutually exclusive with diskOfferingId")
    private Long size;

    @Parameter(name=ApiConstants.SNAPSHOT_ID, type=CommandType.LONG, description="the snapshot ID for the disk volume. Either diskOfferingId or snapshotId must be passed in.")
    private Long snapshotId;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="the ID of the availability zone")
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
    public long getAccountId() {
        Account account = (Account)UserContext.current().getAccount();
        if ((account == null) || isAdmin(account.getType())) {
            if ((domainId != null) && (accountName != null)) {
                Account userAccount = ApiDBUtils.findAccountByNameDomain(accountName, domainId);
                if (userAccount != null) {
                    return userAccount.getId();
                }
            }
        }

        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VOLUME_CREATE;
    }

    @Override
    public String getEventDescription() {
        return  "creating volume: " + getVolumeName() + ((getSnapshotId() == null) ? "" : " from snapshot: " + getSnapshotId());
    }

    @Override @SuppressWarnings("unchecked")
    public VolumeResponse getResponse() {
        VolumeVO volume = (VolumeVO)getResponseObject();

        VolumeResponse response = new VolumeResponse();
        response.setId(volume.getId());
        response.setName(volume.getName());
        response.setVolumeType(volume.getVolumeType().toString());
        response.setSize(volume.getSize());
        response.setCreated(volume.getCreated());
        response.setState(volume.getStatus().toString());
        response.setAccountName(ApiDBUtils.findAccountById(volume.getAccountId()).getAccountName());
        response.setDomainId(volume.getDomainId());
        response.setDiskOfferingId(volume.getDiskOfferingId());

        DiskOfferingVO diskOffering = ApiDBUtils.findDiskOfferingById(volume.getDiskOfferingId());
        response.setDiskOfferingName(diskOffering.getName());
        response.setDiskOfferingDisplayText(diskOffering.getDisplayText());

        response.setDomainName(ApiDBUtils.findDomainById(volume.getDomainId()).getName());
        response.setStorageType("shared"); // NOTE: You can never create a local disk volume but if that changes, we need to change this
        if (volume.getPoolId() != null) {
            response.setStoragePoolName(ApiDBUtils.findStoragePoolById(volume.getPoolId()).getName());
        }

        // if the volume was created from a snapshot, snapshotId will be set so we pass it back in the response
        response.setSnapshotId(getSnapshotId());
        response.setZoneId(volume.getDataCenterId());
        response.setZoneName(ApiDBUtils.findZoneById(volume.getDataCenterId()).getName());
        if(volume.getDeviceId() != null){
        	response.setDeviceId(volume.getDeviceId());
        }
        response.setResponseName(getName());
        return response;
    }
}
