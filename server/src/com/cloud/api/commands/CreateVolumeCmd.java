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
import com.cloud.async.executor.VolumeOperationResultObject;
import com.cloud.serializer.SerializerHelper;
import com.cloud.storage.Snapshot;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class CreateVolumeCmd extends BaseCmd {
	public static final Logger s_logger = Logger.getLogger(CreateVolumeCmd.class.getName());
    private static final String s_name = "createvolumeresponse";
    private static final List<Pair<Enum, Boolean>> s_properties = new ArrayList<Pair<Enum, Boolean>>();

    static {
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT_OBJ, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.USER_ID, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ACCOUNT, Boolean.FALSE));
    	s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DOMAIN_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.NAME, Boolean.TRUE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.ZONE_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.DISK_OFFERING_ID, Boolean.FALSE));
        s_properties.add(new Pair<Enum, Boolean>(BaseCmd.Properties.SNAPSHOT_ID, Boolean.FALSE));
    }

    public String getName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
    	return "volume";
    }
    
    public List<Pair<Enum, Boolean>> getProperties() {
        return s_properties;
    }

    @Override
    public List<Pair<String, Object>> execute(Map<String, Object> params) {
    	Account account = (Account) params.get(BaseCmd.Properties.ACCOUNT_OBJ.getName());
        Long userId = (Long) params.get(BaseCmd.Properties.USER_ID.getName());
    	String accountName = (String) params.get(BaseCmd.Properties.ACCOUNT.getName());
    	Long domainId = (Long) params.get(BaseCmd.Properties.DOMAIN_ID.getName());
    	String name = (String) params.get(BaseCmd.Properties.NAME.getName());
    	Long zoneId = (Long) params.get(BaseCmd.Properties.ZONE_ID.getName());
    	Long diskOfferingId = (Long) params.get(BaseCmd.Properties.DISK_OFFERING_ID.getName());
        Long snapshotId = (Long)params.get(BaseCmd.Properties.SNAPSHOT_ID.getName());

    	if (account == null) {
    		// Admin API call

    		// Check if accountName was passed in
    		if ((accountName == null) || (domainId == null)) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Account and domainId must be passed in.");
    		}

    		// Look up the account by name and domain ID
    		account = getManagementServer().findActiveAccount(accountName, domainId);    		

    		// If the account is null, this means that the accountName and domainId passed in were invalid
    		if (account == null)
    			throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find account with name: " + accountName + " and domain ID: " + domainId);
    	} else {
    		// User API call

    	    // If the account is an admin, and accountName/domainId were passed in, use the account specified by these parameters
    		if (isAdmin(account.getType())) {
    		    if (domainId != null) {
    		        if (!getManagementServer().isChildDomain(account.getDomainId(), domainId)) {
    	                throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to create volume in domain " + domainId + ", permission denied.");
    		        }
    		        if (accountName != null) {
    	                account = getManagementServer().findActiveAccount(accountName, domainId);

    	                if (account == null)
    	                    throw new ServerApiException(BaseCmd.PARAM_ERROR, "Unable to find account with name: " + accountName + " and domain ID: " + domainId);
    		        }
    		    }
    		}
    	}

    	// If command is executed via the Admin API, set userId to the id of System account (1)
        if (userId == null) {
            userId = Long.valueOf(Account.ACCOUNT_ID_SYSTEM);
        }

        boolean useSnapshot = false;
        if (snapshotId == null) {
            if ((zoneId == null) || (diskOfferingId == null)) {
                throw new ServerApiException(BaseCmd.PARAM_ERROR, "Missing parameter(s), both zoneid and diskofferingid must be specified.");
            }
        } else {
            useSnapshot = true;
            //Verify parameters
            Snapshot snapshotCheck = getManagementServer().findSnapshotById(snapshotId);
            if (snapshotCheck == null) {
                throw new ServerApiException (BaseCmd.SNAPSHOT_INVALID_PARAM_ERROR, "unable to find a snapshot with id " + snapshotId);
            }
            
            if (account != null) {
                if (isAdmin(account.getType())) {
                    Account snapshotOwner = getManagementServer().findAccountById(snapshotCheck.getAccountId());
                    if (!getManagementServer().isChildDomain(account.getDomainId(), snapshotOwner.getDomainId())) {
                        throw new ServerApiException(BaseCmd.ACCOUNT_ERROR, "Unable to create volume from snapshot with id " + snapshotId + ", permission denied.");
                    }
                } else if (account.getId().longValue() != snapshotCheck.getAccountId()) {
                    throw new ServerApiException(BaseCmd.SNAPSHOT_INVALID_PARAM_ERROR, "unable to find a snapshot with id " + snapshotId + " for this account");
                }
            }
        }

    	try {
    		long jobId = 0;
    		if (useSnapshot) {
                jobId = getManagementServer().createVolumeFromSnapshotAsync(userId, account.getId(), snapshotId, name);
    		} else {
    		    jobId = getManagementServer().createVolumeAsync(userId, account.getId(), name, zoneId, diskOfferingId);
    		}
    		
    		if (jobId == 0) {
            	s_logger.warn("Unable to schedule async-job for CreateVolume command");
            } else {
    	        if(s_logger.isDebugEnabled())
    	        	s_logger.debug("CreateVolume command has been accepted, job id: " + jobId);
            }
    		
    		long volumeId = waitInstanceCreation(jobId);
    		List<Pair<String, Object>> returnValues = new ArrayList<Pair<String, Object>>();
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.JOB_ID.getName(), Long.valueOf(jobId))); 
            returnValues.add(new Pair<String, Object>(BaseCmd.Properties.VOLUME_ID.getName(), Long.valueOf(volumeId))); 
            
            return returnValues;
    	} catch (Exception ex) {
    	    s_logger.error("Failed to create volume " + (useSnapshot ? ("from snapshot " + snapshotId) : ("in zone " + zoneId + " with disk offering " + diskOfferingId)), ex);
    	    if (useSnapshot) {
    	        throw new ServerApiException(BaseCmd.CREATE_VOLUME_FROM_SNAPSHOT_ERROR, "Unable to create a volume from snapshot with id " + snapshotId + " for this account.");
    	    } else {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create volume in zone " + zoneId + " with disk offering " + diskOfferingId);
    	    }
    	}
    	
    }
    
    protected long getInstanceIdFromJobSuccessResult(String result) {
		VolumeOperationResultObject resultObject = (VolumeOperationResultObject) SerializerHelper.fromSerializedString(result);
		if(resultObject != null) {
			return resultObject.getId();
		}
		
		return 0;
	}
}
