// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.command.user.template;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.log4j.Logger;

import com.cloud.async.AsyncJob;
import com.cloud.template.VirtualMachineTemplate.TemplateFilter;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.utils.Pair;

@APICommand(name = "listTemplates", description="List all public, private, and privileged templates.", responseObject=TemplateResponse.class)
public class ListTemplatesCmd extends BaseListTaggedResourcesCmd {
    public static final Logger s_logger = Logger.getLogger(ListTemplatesCmd.class.getName());

    private static final String s_name = "listtemplatesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.HYPERVISOR, type=CommandType.STRING, description="the hypervisor for which to restrict the search")
    private String hypervisor;

    @Parameter(name=ApiConstants.ID, type=CommandType.UUID, entityType=TemplateResponse.class,
            description="the template ID")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="the template name")
    private String templateName;

    @Parameter(name=ApiConstants.TEMPLATE_FILTER, type=CommandType.STRING, required=true, description="possible values are \"featured\", \"self\", \"selfexecutable\",\"sharedexecutable\",\"executable\", and \"community\". " + 
                                                                                        "* featured : templates that have been marked as featured and public. " +
                                                                                        "* self : templates that have been registered or created by the calling user. " +
                                                                                        "* selfexecutable : same as self, but only returns templates that can be used to deploy a new VM. " +
                                                                                        "* sharedexecutable : templates ready to be deployed that have been granted to the calling user by another user. " +
                                                                                        "* executable : templates that are owned by the calling user, or public templates, that can be used to deploy a VM. " +
                                                                                        "* community : templates that have been marked as public but not featured. " +
                                                                                        "* all : all templates (only usable by admins).")
    private String templateFilter;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.UUID, entityType = ZoneResponse.class,
            description="list templates by zoneId")
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
