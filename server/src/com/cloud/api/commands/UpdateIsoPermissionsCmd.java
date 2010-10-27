package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.Implementation;
import com.cloud.server.ManagementServer;

@Implementation(method="updateTemplatePermissions", manager=ManagementServer.class)
public class UpdateIsoPermissionsCmd extends UpdateTemplateOrIsoPermissionsCmd {
    protected String getResponseName() {
    	return "updateisopermissionsresponse";
    }

	protected Logger getLogger() {
		return Logger.getLogger(UpdateIsoPermissionsCmd.class.getName());    
	}
}
