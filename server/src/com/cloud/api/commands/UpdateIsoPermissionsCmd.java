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

@Implementation(method="updateTemplatePermissions", manager=ManagementServer.class)
public class UpdateIsoPermissionsCmd extends UpdateTemplateOrIsoPermissionsCmd {
    protected String getResponseName() {
    	return "updateisopermissionsresponse";
    }

	protected Logger getLogger() {
		return Logger.getLogger(UpdateIsoPermissionsCmd.class.getName());    
	}
	
    @Override
    public Object execute() throws ServerApiException, InvalidParameterValueException, PermissionDeniedException, InsufficientAddressCapacityException, InsufficientCapacityException, ConcurrentOperationException, ResourceUnavailableException{
        boolean result = _mgr.updateTemplatePermissions(this);
        return result;
    }
}
