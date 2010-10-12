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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.TemplatePermissionsResponse;
import com.cloud.storage.VMTemplateVO;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(method="listTemplatePermissions", description="List template visibility and all accounts that have permissions to view this template.")
public class ListTemplateOrIsoPermissionsCmd extends BaseListCmd {
	public Logger s_logger = getLogger();
    protected String s_name = getResponseName();

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="account", type=CommandType.STRING, description="List template visibility and permissions for the specified account. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name="domainid", type=CommandType.LONG, description="List template visibility and permissions by domain. If used with the account parameter, specifies in which domain the specified account exists.")
    private Long domainId;

    @Parameter(name="id", type=CommandType.LONG, required=true, description="the template ID")
    private Long id;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getId() {
        return id;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public TemplatePermissionsResponse getResponse() {
        List<String> accountNames = (List<String>)getResponseObject();
        Account account = (Account)UserContext.current().getAccountObject();
        boolean isAdmin = ((account == null) || isAdmin(account.getType()));
        Long templateOwnerDomain = null;
        VMTemplateVO template = ApiDBUtils.findTemplateById(id);
        if (isAdmin) {
            // FIXME:  we have just template id and need to get template owner from that
            Account templateOwner = ApiDBUtils.findAccountById(template.getAccountId());
            if (templateOwner != null) {
                templateOwnerDomain = templateOwner.getDomainId();
            }
        }

        TemplatePermissionsResponse response = new TemplatePermissionsResponse();
        response.setId(template.getId());
        response.setPublicTemplate(template.isPublicTemplate());
        if (isAdmin && (templateOwnerDomain != null)) {
            response.setDomainId(templateOwnerDomain);
        }

        response.setAccountNames(accountNames);

        response.setResponseName(getName());
        return response;
    }
    
    protected boolean templateIsCorrectType(VMTemplateVO template) {
    	return true;
    }
    
    protected String getResponseName() {
    	return "updatetemplateorisopermissionsresponse";
    }
    
    public String getMediaType() {
    	return "templateOrIso";
    }
    
    protected Logger getLogger() {
    	return Logger.getLogger(UpdateTemplateOrIsoPermissionsCmd.class.getName());    
    }
}
