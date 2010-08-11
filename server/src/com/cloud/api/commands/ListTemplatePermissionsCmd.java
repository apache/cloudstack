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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.api.BaseCmd;
import com.cloud.api.ServerApiException;
import com.cloud.domain.DomainVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.user.Account;
import com.cloud.utils.Pair;

public class ListTemplatePermissionsCmd extends ListTemplateOrIsoPermissionsCmd {
	protected String getResponseName() {
    	return "listtemplatepermissionsresponse";
    }
    
	protected String getMediaType() {
    	return "template";
    }
	
	protected Logger getLogger() {
		return Logger.getLogger(ListTemplatePermissionsCmd.class.getName());    
	}
	
	protected boolean templateIsCorrectType(VMTemplateVO template) {
		return !template.getFormat().equals(ImageFormat.ISO);
	}
}
