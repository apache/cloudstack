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

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.TemplateResponse;
import com.cloud.async.AsyncJob;
import com.cloud.storage.Storage;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.template.VirtualMachineTemplate.TemplateFilter;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

@Implementation(description="List all public, private, and privileged templates.", responseObject=TemplateResponse.class)
public class ListTemplatesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListTemplatesCmd.class.getName());

    private static final String s_name = "listtemplatesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="list template by account. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="list all templates in specified domain. If used with the account parameter, lists all templates for an account in the specified domain.")
    private Long domainId;

    @Parameter(name=ApiConstants.HYPERVISOR, type=CommandType.STRING, description="the hypervisor for which to restrict the search")
    private String hypervisor;

    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="the template ID")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="the template name")
    private String templateName;

    @Parameter(name=ApiConstants.TEMPLATE_FILTER, type=CommandType.STRING, required=true, description="possible values are \"featured\", \"self\", \"self-executable\", \"executable\", and \"community\"." +
    																					"* featured-templates that are featured and are public" +
    																					"* self-templates that have been registered/created by the owner" +
    																					"* selfexecutable-templates that have been registered/created by the owner that can be used to deploy a new VM" +
    																					"* executable-all templates that can be used to deploy a new VM* community-templates that are public.")
    private String templateFilter;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="list templates by zoneId")
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public Long getId() {
        return id;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getTemplateFilter() {
        return templateFilter;
    }

    public Long getZoneId() {
        return zoneId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }
    
    public AsyncJob.Type getInstanceType() {
    	return AsyncJob.Type.Template;
    }
    
    @Override
    public void execute(){
        List<? extends VirtualMachineTemplate> templates = _mgr.listTemplates(this);
        TemplateFilter templateFilterObj;
        try {
            templateFilterObj = TemplateFilter.valueOf(templateFilter);
        } catch (IllegalArgumentException e) {
            // how did we get this far?  The request should've been rejected already before the response stage...
            templateFilterObj = TemplateFilter.selfexecutable;
        }

        boolean isAdmin = false;
        boolean isAccountSpecific = true;
        Account account = UserContext.current().getAccount();
        if ((account == null) || (account.getType() == Account.ACCOUNT_TYPE_ADMIN) || (account.getType() == Account.ACCOUNT_TYPE_DOMAIN_ADMIN)) {
            isAdmin = true;
            if ((accountName == null) || (domainId == null)) {
                isAccountSpecific = false;
            }
        }

        boolean showDomr = (templateFilterObj != TemplateFilter.selfexecutable);

        ListResponse<TemplateResponse> response = new ListResponse<TemplateResponse>();
        List<TemplateResponse> templateResponses = new ArrayList<TemplateResponse>();

        for (VirtualMachineTemplate template : templates) {
            if (!showDomr && template.getTemplateType() == Storage.TemplateType.SYSTEM) {
                continue;
            }
            _responseGenerator.createTemplateResponse(templateResponses, template, zoneId, isAdmin, account);
        }

        response.setResponses(templateResponses);
        response.setResponseName(getName());
        this.setResponseObject(response);
    }
}
