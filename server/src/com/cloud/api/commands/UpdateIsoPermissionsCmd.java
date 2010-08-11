package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Storage.ImageFormat;

public class UpdateIsoPermissionsCmd extends UpdateTemplateOrIsoPermissionsCmd {
    protected String getResponseName() {
    	return "updateisopermissionsresponse";
    }
    
	protected String getMediaType() {
    	return "iso";
    }
	
	protected Logger getLogger() {
		return Logger.getLogger(UpdateIsoPermissionsCmd.class.getName());    
	}
	
	protected boolean templateIsCorrectType(VMTemplateVO template) {
		return template.getFormat().equals(ImageFormat.ISO);
	}
}
