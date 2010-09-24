package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd.Manager;
import com.cloud.api.Implementation;

@Implementation(method="updateTemplatePermissions", manager=Manager.ManagementServer)
public class UpdateIsoPermissionsCmd extends UpdateTemplateOrIsoPermissionsCmd {
    protected String getResponseName() {
    	return "updateisopermissionsresponse";
    }

	protected Logger getLogger() {
		return Logger.getLogger(UpdateIsoPermissionsCmd.class.getName());    
	}
}
