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

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.storage.SnapshotScheduleVO;
import com.cloud.storage.VolumeVO;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class ListRecurringSnapshotScheduleCmd extends BaseCmd {
    private static final String s_name = "listrecurringsnapshotscheduleresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.VOLUME_ID, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.SNAPSHOT_POLICY_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long volumeId = (Long)params.get(BaseCmd.Properties.VOLUME_ID.getName());
        Long policyId = (Long)params.get(BaseCmd.Properties.SNAPSHOT_POLICY_ID.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());

        //Verify parameters
        VolumeVO volume = getManagementServer().findVolumeById(volumeId);
        if (volume == null) {
            throw new ServerApiException (BaseCmd.SNAPSHOT_INVALID_PARAM_ERROR, "unable to find a volume with id " + volumeId);
        }

        if (account != null) {
            long volAcctId = volume.getAccountId();
            if (isAdmin(account.getType())) {
                Account userAccount = getManagementServer().findAccountById(Long.valueOf(volAcctId));
                if (!getManagementServer().isChildDomain(account.getDomainId(), userAccount.getDomainId())) {
                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Invalid volume id (" + volumeId + ") given, unable to list snapshots.");
                }
            } else if (account.getId().longValue() != volAcctId) {
                throw new ServerApiException(BaseCmd.SNAPSHOT_INVALID_PARAM_ERROR, "account " + account.getAccountName() + " does not own volume id " + volAcctId);
            }
        }

        List<SnapshotScheduleVO> recurringSnapshotSchedules = getManagementServer().findRecurringSnapshotSchedule(volumeId, policyId);
        Object[] snapshotTag = new Object[recurringSnapshotSchedules.size()];
        int i = 0;
        
        for (SnapshotScheduleVO recurringSnapshotSchedule : recurringSnapshotSchedules) {
            List<Pair<String, Object>> snapshotData = new ArrayList<Pair<String, Object>>();
            snapshotData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), recurringSnapshotSchedule.getId().toString()));
            snapshotData.add(new Pair<String, Object>(BaseCmd.Properties.VOLUME_ID.getName(), recurringSnapshotSchedule.getVolumeId().toString()));
            snapshotData.add(new Pair<String, Object>(BaseCmd.Properties.SNAPSHOT_POLICY_ID.getName(), recurringSnapshotSchedule.getPolicyId().toString()));
            snapshotData.add(new Pair<String, Object>(BaseCmd.Properties.SCHEDULED.getName(), recurringSnapshotSchedule.getScheduledTimestamp().toString()));
            snapshotTag[i++] = snapshotData;
        }
        List<Pair<String, Object>> returnTags = new ArrayList<Pair<String, Object>>();
        Pair<String, Object> snapshotTags = new Pair<String, Object>("snapshot", snapshotTag);
        returnTags.add(snapshotTags);
        return returnTags;
    }
}
