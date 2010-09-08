package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Storage.ImageFormat;

public class ListIsoPermissionsCmd extends ListTemplateOrIsoPermissionsCmd {
	protected String getResponseName() {
    	return "listisopermissionsresponse";
    }
    
	public String getMediaType() {
    	return "iso";
    }
	
	protected Logger getLogger() {
		return Logger.getLogger(ListIsoPermissionsCmd.class.getName());    
	}
	
	protected boolean templateIsCorrectType(VMTemplateVO template) {
		return template.getFormat().equals(ImageFormat.ISO);
	}
}
