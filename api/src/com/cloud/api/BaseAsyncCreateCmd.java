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

package com.cloud.api;

import com.cloud.api.response.CreateCmdResponse;
import com.cloud.exception.ResourceAllocationException;

public abstract class BaseAsyncCreateCmd extends BaseAsyncCmd {
    @Parameter(name="id", type=CommandType.LONG)
    private Long id;
    
    public abstract void create() throws ResourceAllocationException;

    public Long getEntityId() {
        return id;
    }

    public void setEntityId(Long id) {
        this.id = id;
    }
    
    public abstract String getEntityTable();

    public String getResponse(long jobId, long objectId, String objectEntityTable) {
        CreateCmdResponse response = new CreateCmdResponse();
        response.setJobId(jobId);
        response.setId(objectId);
        response.setIdEntityTable(objectEntityTable);
        response.setResponseName(getCommandName());
        return _responseGenerator.toSerializedString(response, getResponseType());
    }

    public String getCreateEventType() {
        return null;
    }

    public String getCreateEventDescription() {
        return null;
    }
}
