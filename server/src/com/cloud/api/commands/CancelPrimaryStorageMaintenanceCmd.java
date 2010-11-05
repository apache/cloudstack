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
import com.cloud.api.ApiResponseHelper;
import com.cloud.api.BaseAsyncCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.StoragePoolResponse;
import com.cloud.event.EventTypes;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolVO;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(method="cancelPrimaryStorageForMaintenance", manager=StorageManager.class)
public class CancelPrimaryStorageMaintenanceCmd extends BaseAsyncCmd {
	public static final Logger s_logger = Logger.getLogger(CancelPrimaryStorageMaintenanceCmd.class.getName());
	
    private static final String s_name = "cancelprimarystoragemaintenanceresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, required=true, description="the primary storage ID")
    private Long id;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }
    
    public static String getResultObjectName() {
    	return "primarystorage";
    }

    @Override
    public long getAccountId() {
        Account account = (Account)UserContext.current().getAccount();
        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_MAINTENANCE_CANCEL_PRIMARY_STORAGE;
    }

    @Override
    public String getEventDescription() {
        return  "canceling maintenance for primary storage pool: " + getId();
    }

	@Override @SuppressWarnings("unchecked")
	public StoragePoolResponse getResponse() {
	    StoragePoolVO primaryStorage = (StoragePoolVO)getResponseObject();
	    StoragePoolResponse response = ApiResponseHelper.createStoragePoolResponse(primaryStorage);
        response.setResponseName("storagepool");
		return response;
	}
}
