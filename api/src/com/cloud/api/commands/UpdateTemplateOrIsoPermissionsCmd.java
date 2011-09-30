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

import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseCmd;
import com.cloud.api.Parameter;
import com.cloud.exception.InvalidParameterValueException;

public abstract class UpdateTemplateOrIsoPermissionsCmd extends BaseCmd {
    public Logger s_logger = getLogger();
    protected String s_name = getResponseName();

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNTS, type = CommandType.LIST, collectionType = CommandType.STRING, description = "a comma delimited list of accounts. If specified, \"op\" parameter has to be passed in.")
    private List<String> accountNames;

    @Parameter(name = ApiConstants.ID, type = CommandType.LONG, required = true, description = "the template ID")
    private Long id;

    @Parameter(name = ApiConstants.IS_FEATURED, type = CommandType.BOOLEAN, description = "true for featured template/iso, false otherwise")
    private Boolean featured;

    @Parameter(name = ApiConstants.IS_PUBLIC, type = CommandType.BOOLEAN, description = "true for public template/iso, false for private templates/isos")
    private Boolean isPublic;
    
    @Parameter(name = ApiConstants.IS_EXTRACTABLE, type = CommandType.BOOLEAN, description = "true if the template/iso is extractable, false other wise. Can be set only by root admin")
    private Boolean isExtractable;

    @Parameter(name = ApiConstants.OP, type = CommandType.STRING, description = "permission operator (add, remove, reset)")
    private String operation;
    
    @Parameter(name = ApiConstants.PROJECT_IDS, type = CommandType.LIST, collectionType = CommandType.LONG, description = "a comma delimited list of projects. If specified, \"op\" parameter has to be passed in.")
    private List<Long> projectIds;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public List<String> getAccountNames() {
        if (accountNames != null && projectIds != null) {
            throw new InvalidParameterValueException("Accounts and projectIds can't be specified together");  
        }
        
        return accountNames; 
    }

    public Long getId() {
        return id;
    }

    public Boolean isFeatured() {
        return featured;
    }

    public Boolean isPublic() {
        return isPublic;
    }
    
    public Boolean isExtractable() {
        return isExtractable;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public List<Long> getProjectIds() {
        if (accountNames != null && projectIds != null) {
            throw new InvalidParameterValueException("Accounts and projectIds can't be specified together");  
        }
        return projectIds;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    protected String getResponseName() {
        return "updatetemplateorisopermissionsresponse";
    }

    protected Logger getLogger() {
        return Logger.getLogger(UpdateTemplateOrIsoPermissionsCmd.class.getName());
    }

    @Override
    public void execute() {
        // method is implemented in updateTemplate/updateIsoPermissions
    }
}
