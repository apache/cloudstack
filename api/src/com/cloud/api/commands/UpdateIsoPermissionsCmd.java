package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.Implementation;
import com.cloud.api.ServerApiException;
import com.cloud.api.response.SuccessResponse;

@Implementation(description="Updates iso permissions", responseObject=SuccessResponse.class)
public class UpdateIsoPermissionsCmd extends UpdateTemplateOrIsoPermissionsCmd {
    protected String getResponseName() {
    	return "updateisopermissionsresponse";
    }

	protected Logger getLogger() {
		return Logger.getLogger(UpdateIsoPermissionsCmd.class.getName());    
	}
	
    @Override
    public void execute(){
        boolean result = _mgr.updateTemplatePermissions(this);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to update iso permissinos");
        }
    }
}
