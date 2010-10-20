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
import com.cloud.async.AsyncJobVO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.server.Criteria;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Snapshot.SnapshotType;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class ListSnapshotsCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(ListSnapshotsCmd.class.getName());

    private static final String s_name = "listsnapshotsresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.VOLUME_ID, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ID, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.INTERVAL_TYPE, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.SNAPSHOT_TYPE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.KEYWORD, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGE, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.PAGESIZE, Boolean.FALSE));
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
        String name   = (String)params.get(BaseCmd.Properties.NAME.getName());
        Long id     = (Long)params.get(BaseCmd.Properties.ID.getName());
        String interval = (String)params.get(BaseCmd.Properties.INTERVAL_TYPE.getName());
        String snapshotType = (String)params.get(BaseCmd.Properties.SNAPSHOT_TYPE.getName());
        Account account = (Account)params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        String accountName = (String)params.get(BaseCmd.Properties.ACCOUNT.getName());
        Long domainId = (Long)params.get(BaseCmd.Properties.DOMAIN_ID.getName());
        String keyword = (String)params.get(BaseCmd.Properties.KEYWORD.getName());
        Integer page = (Integer)params.get(BaseCmd.Properties.PAGE.getName());
        Integer pageSize = (Integer)params.get(BaseCmd.Properties.PAGESIZE.getName());

        boolean isAdmin = false;

        //Verify parameters
        if(volumeId != null){
        	VolumeVO volume = getManagementServer().findAnyVolumeById(volumeId);
        	if (volume == null) {
        		throw new ServerApiException (BaseCmd.SNAPSHOT_INVALID_PARAM_ERROR, "unable to find a volume with id " + volumeId);
        	}
        	checkAccountPermissions(params, volume.getAccountId(), volume.getDomainId(), "volume", volumeId);
        }
        
        Long accountId = null;
        if (account == null) {
            if (domainId != null && accountName != null) {
                account = getManagementServer().findAccountByName(accountName, domainId);
            }
        }
        
        if( account != null && !isAdmin(account.getType())) {
            accountId = account.getId();
        } else {
            isAdmin = true;
            if (account != null) {
                domainId = account.getDomainId();
            }
        }
            
        Long startIndex = Long.valueOf(0);
        int pageSizeNum = 50;
    	if (pageSize != null) {
    		pageSizeNum = pageSize.intValue();
    	}
        if (page != null) {
            int pageNum = page.intValue();
            if (pageNum > 0) {
                startIndex = Long.valueOf(pageSizeNum * (pageNum-1));
            }
        }
        Criteria c = new Criteria("created", Boolean.FALSE, startIndex, Long.valueOf(pageSizeNum));

        c.addCriteria(Criteria.VOLUMEID, volumeId);
        c.addCriteria(Criteria.TYPE, snapshotType); // I don't want to create a new Criteria called SNAPSHOT_TYPE
        c.addCriteria(Criteria.NAME, name);
        c.addCriteria(Criteria.ID, id);
        c.addCriteria(Criteria.KEYWORD, keyword);
        c.addCriteria(Criteria.ACCOUNTID, accountId);
        if (isAdmin) {
            c.addCriteria(Criteria.DOMAINID, domainId);
        }

        List<SnapshotVO> snapshots = null;
		try {
			snapshots = getManagementServer().listSnapshots(c, interval);
		} catch (InvalidParameterValueException e) {
			throw new ServerApiException(SNAPSHOT_INVALID_PARAM_ERROR, e.getMessage());
		}

        if (snapshots == null) {
            throw new ServerApiException(BaseCmd.SNAPSHOT_LIST_ERROR, "unable to find snapshots for volume with id " + volumeId);
        }

        Object[] snapshotTag = new Object[snapshots.size()];
        int i = 0;

        for (Snapshot snapshot : snapshots) {
            List<Pair<String, Object>> snapshotData = new ArrayList<Pair<String, Object>>();
            snapshotData.add(new Pair<String, Object>(BaseCmd.Properties.ID.getName(), snapshot.getId().toString()));

            Account acct = getManagementServer().findAccountById(Long.valueOf(snapshot.getAccountId()));
            if (acct != null) {
                snapshotData.add(new Pair<String, Object>(BaseCmd.Properties.ACCOUNT.getName(), acct.getAccountName()));
                snapshotData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN_ID.getName(), acct.getDomainId().toString()));
                snapshotData.add(new Pair<String, Object>(BaseCmd.Properties.DOMAIN.getName(), getManagementServer().findDomainIdById(acct.getDomainId()).getName()));
            }
            volumeId = snapshot.getVolumeId();
            VolumeVO volume = getManagementServer().findAnyVolumeById(volumeId);
            String snapshotTypeStr = SnapshotType.values()[snapshot.getSnapshotType()].name();
            snapshotData.add(new Pair<String, Object>(BaseCmd.Properties.SNAPSHOT_TYPE.getName(), snapshotTypeStr));
            snapshotData.add(new Pair<String, Object>(BaseCmd.Properties.VOLUME_ID.getName(), volumeId));
            snapshotData.add(new Pair<String, Object>(BaseCmd.Properties.VOLUME_NAME.getName(), volume.getName()));
            snapshotData.add(new Pair<String, Object>(BaseCmd.Properties.VOLUME_TYPE.getName(), volume.getVolumeType()));
            snapshotData.add(new Pair<String, Object>(BaseCmd.Properties.CREATED.getName(), getDateString(snapshot.getCreated())));
            snapshotData.add(new Pair<String, Object>(BaseCmd.Properties.NAME.getName(), snapshot.getName()));
            
            AsyncJobVO asyncJob = getManagementServer().findInstancePendingAsyncJob("snapshot", snapshot.getId());
            if(asyncJob != null) {
            	snapshotData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), asyncJob.getId().toString()));
            	snapshotData.add(new Pair<String, Object>(BaseCmd.Properties.JOB_STATUS.getName(), String.valueOf(asyncJob.getStatus())));
            } 
            snapshotData.add(new Pair<String, Object>(BaseCmd.Properties.INTERVAL_TYPE.getName(), getManagementServer().getSnapshotIntervalTypes(snapshot.getId())));
            snapshotTag[i++] = snapshotData;
        }
        List<Pair<String, Object>> returnTags = new ArrayList<Pair<String, Object>>();
        Pair<String, Object> snapshotTags = new Pair<String, Object>("snapshot", snapshotTag);
        returnTags.add(snapshotTags);
        return returnTags;
    }
}
