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

import com.cloud.api.Implementation;
import com.cloud.api.ServerApiException;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.server.ManagementServer;
import com.cloud.storage.VMTemplateVO;

@Implementation(method="updateTemplatePermissions", manager=ManagementServer.class, description="Updates a template visibility permissions. " +
																						"A public template is visible to all accounts within the same domain. " +
																						"A private template is visible only to the owner of the template. " +
																						"A priviledged template is a private template with account permissions added. " +
																						"Only accounts specified under the template permissions are visible to them.")
public class UpdateTemplatePermissionsCmd extends UpdateTemplateOrIsoPermissionsCmd {
    protected String getResponseName() {
    	return "updatetemplatepermissionsresponse";
    }
    	
	protected Logger getLogger() {
		return Logger.getLogger(UpdateTemplatePermissionsCmd.class.getName());    
	}	
	
    @Override
    public Object execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException{
        boolean result = _mgr.updateTemplatePermissions(this);
        return result;
    }

}
