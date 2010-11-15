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

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;

@Implementation(description="Deletes snapshot policies for the account.", responseObject=SuccessResponse.class)
public class DeleteSnapshotPoliciesCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(DeleteSnapshotPoliciesCmd.class.getName());

    private static final String s_name = "deletesnapshotpoliciesresponse";
    
    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="the Id of the snapshot")
    private Long id;

    @Parameter(name=ApiConstants.IDS, type=CommandType.LIST, collectionType=CommandType.LONG, description="list of snapshots IDs separated by comma")
    private List<Long> ids;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public List<Long> getIds() {
        return ids;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override
    public void execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException{
        boolean result = _snapshotMgr.deleteSnapshotPolicies(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to delete snapshot policy");
        }
    }
}
