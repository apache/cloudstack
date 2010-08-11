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
import com.cloud.storage.SnapshotPolicyVO;
import com.cloud.storage.VolumeVO;
import com.cloud.utils.Pair;

public class ListSnapshotPoliciesCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(ListSnapshotPoliciesCmd.class.getName());

    private static final String s_name = "listsnapshotpoliciesresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.VOLUME_ID, Boolean.TRUE));
    }

    @Override
    public String getName() {
        return s_name;
    }
    @Override
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
        Long volumeId = (Long)params.get(BaseCmd.Properties.VOLUME_ID.getName());

        // Verify that a volume exists with the specified volume ID
        VolumeVO volume = getManagementServer().findVolumeById(volumeId);
        if (volume == null) {
            throw new ServerApiException (BaseCmd.PARAM_ERROR, "Unable to find a volume with id " + volumeId);
        }
        checkAccountPermissions(params, volume.getAccountId(), volume.getDomainId(), "volume", volumeId);

        List<SnapshotPolicyVO> polices = getManagementServer().listSnapshotPolicies(volumeId);

        List<Pair<String, Object>> policesTags = new ArrayList<Pair<String, Object>>();
        Object[] policyTag = new Object[polices.size()];
        int i = 0;
        for (SnapshotPolicyVO policy : polices) {
            List<Pair<String, Object>> policyData = new ArrayList<Pair<String, Object>>();
            policyData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), policy.getId()));
            policyData.add(new Pair<String, Object>(BaseCmd.Properties.VOLUME_ID.getName(), policy.getVolumeId()));
            policyData.add(new Pair<String, Object>(BaseCmd.Properties.SCHEDULE.getName(), policy.getSchedule()));
            policyData.add(new Pair<String, Object>(BaseCmd.Properties.INTERVAL_TYPE.getName(), policy.getInterval()));
            policyData.add(new Pair<String, Object>(BaseCmd.Properties.MAX_SNAPS.getName(), policy.getMaxSnaps()));
            policyData.add(new Pair<String, Object>(BaseCmd.Properties.TIMEZONE.getName(), policy.getTimezone()));
            policyTag[i++] = policyData;
        }
        Pair<String, Object> eventTag = new Pair<String, Object>("snapshotpolicy", policyTag);
        policesTags.add(eventTag);
        return policesTags;
        
    }
}
