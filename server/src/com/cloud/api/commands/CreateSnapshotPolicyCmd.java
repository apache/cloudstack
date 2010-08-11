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
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.server.ManagementServer;
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class CreateSnapshotPolicyCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateSnapshotPolicyCmd.class.getName());

    private static final String s_name = "createsnapshotpolicyresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.VOLUME_ID, Boolean.TRUE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.SCHEDULE, Boolean.TRUE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.TIMEZONE, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.INTERVAL_TYPE, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.MAX_SNAPS, Boolean.TRUE));
    }

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long userId = (Long)params.get(BaseCmd.Properties.USER_ID.getName());
    	long volumeId = (Long)params.get(BaseCmd.Properties.VOLUME_ID.getName());
        String schedule = (String)params.get(BaseCmd.Properties.SCHEDULE.getName());
        String timezone = (String)params.get(BaseCmd.Properties.TIMEZONE.getName());
        String intervalType = (String)params.get(BaseCmd.Properties.INTERVAL_TYPE.getName());
        //ToDo: make maxSnaps optional. Use system wide max when not specified
        int maxSnaps = (Integer)params.get(BaseCmd.Properties.MAX_SNAPS.getName());
        
        ManagementServer managementServer = getManagementServer();
        // Verify that a volume exists with the specified volume ID
        VolumeVO volume = managementServer.findVolumeById(volumeId);
        if (volume == null) {
            throw new ServerApiException (BaseCmd.PARAM_ERROR, "Unable to find a volume with id " + volumeId);
        }
        
        // If an account was passed in, make sure that it matches the account of the volume
        checkAccountPermissions(params, volume.getAccountId(), volume.getDomainId(), "volume", volumeId);
        
        StoragePoolVO storagePoolVO = managementServer.findPoolById(volume.getPoolId());
        if (storagePoolVO == null) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "volumeId: " + volumeId + " does not have a valid storage pool. Is it destroyed?");
        }
        if (storagePoolVO.isLocal()) {
            throw new ServerApiException(BaseCmd.PARAM_ERROR, "Cannot create a snapshot from a volume residing on a local storage pool, poolId: " + volume.getPoolId());
        }

        Long instanceId = volume.getInstanceId();
        if (instanceId != null) {
            // It is not detached, but attached to a VM
            if (managementServer.findUserVMInstanceById(instanceId) == null) {
                // It is not a UserVM but a SystemVM or DomR
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Snapshots of volumes attached to System or router VM are not allowed");
            }
        }
        
        Long accountId = volume.getAccountId();
        // If command is executed via 8096 port, set userId to the id of System account (1)
        if (userId == null) {
            userId = Long.valueOf(1);
        }

        SnapshotPolicyVO snapshotPolicy = null;
        try {
        	snapshotPolicy = managementServer.createSnapshotPolicy(accountId, userId, volumeId, schedule, intervalType, maxSnaps, timezone);
        } catch (InvalidParameterValueException ex) {
        	throw new ServerApiException (BaseCmd.VM_INVALID_PARAM_ERROR, ex.getMessage());
        }
        
        if (snapshotPolicy == null) {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create Snapshot Policy");
        }

        List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), snapshotPolicy.getId().toString()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.VOLUME_ID.getName(), snapshotPolicy.getVolumeId()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.SCHEDULE.getName(), snapshotPolicy.getSchedule()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.INTERVAL_TYPE.getName(), snapshotPolicy.getInterval()));
        returnValues.add(new Pair<String, Object>(BaseCmd.Properties.MAX_SNAPS.getName(), snapshotPolicy.getMaxSnaps()));

        return returnValues;
    }
}
