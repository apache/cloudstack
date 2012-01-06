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
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListProjectAndAccountResourcesCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.TemplateResponse;
import com.cloud.async.AsyncJob;
import com.cloud.template.VirtualMachineTemplate.TemplateFilter;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.utils.Pair;

@Implementation(description="List all public, private, and privileged templates.", responseObject=TemplateResponse.class)
public class ListTemplatesCmd extends BaseListProjectAndAccountResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListTemplatesCmd.class.getName());

    private static final String s_name = "listtemplatesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.HYPERVISOR, type=CommandType.STRING, description="the hypervisor for which to restrict the search")
    private String hypervisor;

    @IdentityMapper(entityTableName="vm_template")
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

    @IdentityMapper(entityTableName="data_center")
    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.LONG, description="list templates by zoneId")
    private Long zoneId;
    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

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
    
    public boolean listInReadyState() {
   	
        Account account = UserContext.current().getCaller();
        // It is account specific if account is admin type and domainId and accountName are not null
        boolean isAccountSpecific = (account == null || isAdmin(account.getType())) && (getAccountName() != null) && (getDomainId() != null);
        // Show only those that are downloaded.
        TemplateFilter templateFilter = TemplateFilter.valueOf(getTemplateFilter());
        boolean onlyReady = (templateFilter == TemplateFilter.featured) || (templateFilter == TemplateFilter.selfexecutable) || (templateFilter == TemplateFilter.sharedexecutable)
        || (templateFilter == TemplateFilter.executable && isAccountSpecific) || (templateFilter == TemplateFilter.community);
        return onlyReady;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }
    
    public AsyncJob.Type getInstanceType() {
    	return AsyncJob.Type.Template;
    }
    
    @Override
    public void execute(){
        Set<Pair<Long, Long>> templateZonePairSet = _mgr.listTemplates(this);

        ListResponse<TemplateResponse> response = new ListResponse<TemplateResponse>();
        List<TemplateResponse> templateResponses = new ArrayList<TemplateResponse>();

        for (Pair<Long, Long> template : templateZonePairSet) {
            List<TemplateResponse> responses = new ArrayList<TemplateResponse>();
            responses = _responseGenerator.createTemplateResponses(template.first().longValue(), template.second(), listInReadyState());
            templateResponses.addAll(responses);
        }

        response.setResponses(templateResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
