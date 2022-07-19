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

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.server.ResourceIcon;
import com.cloud.server.ResourceTag;
import org.apache.cloudstack.api.response.ResourceIconResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.template.VirtualMachineTemplate;
import com.cloud.template.VirtualMachineTemplate.TemplateFilter;
import com.cloud.user.Account;

@APICommand(name = "listTemplates", description = "List all public, private, and privileged templates.", responseObject = TemplateResponse.class, entityType = {VirtualMachineTemplate.class}, responseView = ResponseView.Restricted,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListTemplatesCmd extends BaseListTaggedResourcesCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(ListTemplatesCmd.class.getName());

    private static final String s_name = "listtemplatesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HYPERVISOR, type = CommandType.STRING, description = "the hypervisor for which to restrict the search")
    private String hypervisor;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = TemplateResponse.class, description = "the template ID")
    private Long id;

    @Parameter(name=ApiConstants.IDS, type=CommandType.LIST, collectionType=CommandType.UUID, entityType=TemplateResponse.class, description="the IDs of the templates, mutually exclusive with id", since = "4.9")
    private List<Long> ids;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the template name")
    private String templateName;

    @Parameter(name = ApiConstants.TEMPLATE_FILTER,
               type = CommandType.STRING,
               required = true,
               description = "possible values are \"featured\", \"self\", \"selfexecutable\",\"sharedexecutable\",\"executable\", and \"community\". "
                   + "* featured : templates that have been marked as featured and public. "
                   + "* self : templates that have been registered or created by the calling user. "
                   + "* selfexecutable : same as self, but only returns templates that can be used to deploy a new VM. "
                   + "* sharedexecutable : templates ready to be deployed that have been granted to the calling user by another user. "
                   + "* executable : templates that are owned by the calling user, or public templates, that can be used to deploy a VM. "
                   + "* community : templates that have been marked as public but not featured. " + "* all : all templates (only usable by admins).")
    private String templateFilter;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "list templates by zoneId")
    private Long zoneId;

    @Parameter(name = ApiConstants.SHOW_REMOVED, type = CommandType.BOOLEAN, description = "show removed templates as well")
    private Boolean showRemoved;

    @Parameter(name = ApiConstants.SHOW_UNIQUE, type = CommandType.BOOLEAN, description = "If set to true, list only unique templates across zones", since = "4.13.2")
    private Boolean showUnique;

    @Parameter(name = ApiConstants.PARENT_TEMPLATE_ID, type = CommandType.UUID, entityType = TemplateResponse.class, description = "list datadisk templates by parent template id", since = "4.4")
    private Long parentTemplateId;

    @Parameter(name = ApiConstants.DETAILS,
            type = CommandType.LIST,
            collectionType = CommandType.STRING,
            since = "4.15",
            description = "comma separated list of template details requested, value can be a list of [ all, min]")
    private List<String> viewDetails;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public EnumSet<ApiConstants.DomainDetails> getDetails() throws InvalidParameterValueException {
        EnumSet<ApiConstants.DomainDetails> dv;
        if (CollectionUtils.isEmpty(viewDetails)) {
            dv = EnumSet.of(ApiConstants.DomainDetails.all);
        } else {
            try {
                ArrayList<ApiConstants.DomainDetails> dc = new ArrayList<>();
                for (String detail : viewDetails) {
                    dc.add(ApiConstants.DomainDetails.valueOf(detail));
                }
                dv = EnumSet.copyOf(dc);
            } catch (IllegalArgumentException e) {
                throw new InvalidParameterValueException("The details parameter contains a non permitted value. The allowed values are " +
                        EnumSet.allOf(ApiConstants.DomainDetails.class));
            }
        }
        return dv;
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

    public Boolean getShowRemoved() {
        return showRemoved != null && showRemoved;
    }

    public Boolean getShowUnique() {
        return showUnique != null && showUnique;
    }

    public Long getParentTemplateId() {
        return parentTemplateId;
    }

    public boolean listInReadyState() {

        Account account = CallContext.current().getCallingAccount();
        // It is account specific if account is admin type and domainId and accountName are not null
        boolean isAccountSpecific = (account == null || _accountService.isAdmin(account.getId())) && (getAccountName() != null) && (getDomainId() != null);
        // Show only those that are downloaded.
        TemplateFilter templateFilter = TemplateFilter.valueOf(getTemplateFilter());
        boolean onlyReady =
            (templateFilter == TemplateFilter.featured) || (templateFilter == TemplateFilter.selfexecutable) || (templateFilter == TemplateFilter.sharedexecutable) ||
                (templateFilter == TemplateFilter.executable && isAccountSpecific) || (templateFilter == TemplateFilter.community);
        return onlyReady;
    }

    @Parameter(name = ApiConstants.SHOW_RESOURCE_ICON, type = CommandType.BOOLEAN, description = "flag to display the resource image for the templates")
    private Boolean showIcon;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public Boolean getShowIcon () {
        return  showIcon != null ? showIcon : false;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Template;
    }

    @Override
    public void execute() {
        ListResponse<TemplateResponse> response = _queryService.listTemplates(this);
        if (response != null && response.getCount() > 0 && getShowIcon()) {
            updateTemplateResponse(response.getResponses());
        }
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    private void updateTemplateResponse(List<TemplateResponse> response) {
        for (TemplateResponse templateResponse : response) {
            ResourceIcon resourceIcon = resourceIconManager.getByResourceTypeAndUuid(ResourceTag.ResourceObjectType.Template, templateResponse.getId());
            if (resourceIcon == null) {
                continue;
            }
            ResourceIconResponse iconResponse = _responseGenerator.createResourceIconResponse(resourceIcon);
            templateResponse.setResourceIconResponse(iconResponse);
        }
    }

    public List<Long> getIds() {
        return ids;
    }
}
