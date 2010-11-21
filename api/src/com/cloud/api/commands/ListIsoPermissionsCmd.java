package com.cloud.api.commands;

import org.apache.log4j.Logger;

import com.cloud.storage.Storage.ImageFormat;
import com.cloud.template.VirtualMachineTemplate;

public class ListIsoPermissionsCmd extends ListTemplateOrIsoPermissionsCmd {
	protected String getResponseName() {
    	return "listisopermissionsresponse";
    }
    
	@Override
    public String getMediaType() {
    	return "iso";
    }
	
	@Override
    protected Logger getLogger() {
		return Logger.getLogger(ListIsoPermissionsCmd.class.getName());    
	}
	
	protected boolean templateIsCorrectType(VirtualMachineTemplate template) {
		return template.getFormat().equals(ImageFormat.ISO);
	}
}
