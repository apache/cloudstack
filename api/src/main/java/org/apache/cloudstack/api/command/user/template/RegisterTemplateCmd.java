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

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ExtensionResponse;
import org.apache.cloudstack.api.response.GuestOSResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.StringUtils;

import com.cloud.cpu.CPU;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.template.VirtualMachineTemplate;

@APICommand(name = "registerTemplate", description = "Registers an existing Template into the CloudStack cloud. ", responseObject = TemplateResponse.class, responseView = ResponseView.Restricted,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class RegisterTemplateCmd extends BaseCmd implements UserCmd {

    private static final String s_name = "registertemplateresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.BITS, type = CommandType.INTEGER, description = "32 or 64 bits support. 64 by default")
    private Integer bits;

    @Parameter(name = ApiConstants.DISPLAY_TEXT,
               type = CommandType.STRING,
               description = "The display text of the Template, defaults to 'name'.",
               length = 4096)
    private String displayText;

    @Parameter(name = ApiConstants.FORMAT,
               type = CommandType.STRING,
               required = true,
               description = "The format for the Template. Possible values include QCOW2, RAW, VHD and OVA.")
    private String format;

    @Parameter(name = ApiConstants.HYPERVISOR, type = CommandType.STRING, required = true, description = "The target hypervisor for the Template")
    protected String hypervisor;

    @Parameter(name = ApiConstants.IS_FEATURED, type = CommandType.BOOLEAN, description = "True if this Template is a featured Template, false otherwise")
    private Boolean featured;

    @Parameter(name = ApiConstants.IS_PUBLIC, type = CommandType.BOOLEAN, description = "True if the Template is available to all accounts; default is true")
    private Boolean publicTemplate;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "The name of the Template")
    private String templateName;

    @Parameter(name = ApiConstants.OS_TYPE_ID,
               type = CommandType.UUID,
               entityType = GuestOSResponse.class,
               required = false,
               description = "The ID of the OS Type that best represents the OS of this Template. Not applicable with VMware, as we honour what is defined in the Template")
    private Long osTypeId;

    @Parameter(name = ApiConstants.PASSWORD_ENABLED,
               type = CommandType.BOOLEAN,
               description = "True if the Template supports the password reset feature; default is false")
    private Boolean passwordEnabled;

    @Parameter(name = ApiConstants.SSHKEY_ENABLED, type = CommandType.BOOLEAN, description = "True if the Template supports the sshkey upload feature; default is false")
    private Boolean sshKeyEnabled;

    @Parameter(name = ApiConstants.IS_EXTRACTABLE, type = CommandType.BOOLEAN, description = "True if the Template or its derivatives are extractable; default is false")
    private Boolean extractable;

    @Parameter(name = ApiConstants.REQUIRES_HVM, type = CommandType.BOOLEAN, description = "True if this Template requires HVM")
    private Boolean requiresHvm;

    @Parameter(name = ApiConstants.URL,
               type = CommandType.STRING,
               required = true,
               length = 2048,
               description = "The URL of where the Template is hosted. Possible URL include http:// and https://")
    private String url;

    @Parameter(name=ApiConstants.ZONE_ID, type=CommandType.UUID, entityType = ZoneResponse.class,
            required=false, description = "The ID of the zone the Template is to be hosted on")
    protected Long zoneId;

    @Parameter(name = ApiConstants.DOMAIN_ID,
               type = CommandType.UUID,
               entityType = DomainResponse.class,
               description = "An optional domainId. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "An optional accountName. Must be used with domainId.")
    private String accountName;

    @Parameter(name = ApiConstants.CHECKSUM, type = CommandType.STRING, description = "The checksum value of this Template. " + ApiConstants.CHECKSUM_PARAMETER_PREFIX_DESCRIPTION)
    private String checksum;

    @Parameter(name = ApiConstants.TEMPLATE_TAG, type = CommandType.STRING, description = "The tag for this Template.")
    private String templateTag;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "Register Template for the project")
    private Long projectId;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP,
            description = "Template details in key/value pairs using format details[i].keyname=keyvalue. Example: details[0].hypervisortoolsversion=xenserver61")
    protected Map details;

    @Parameter(name = ApiConstants.IS_DYNAMICALLY_SCALABLE,
               type = CommandType.BOOLEAN,
               description = "True if Template contains XS/VMWare tools in order to support dynamic scaling of Instance cpu/memory")
    protected Boolean isDynamicallyScalable;

    @Deprecated
    @Parameter(name = ApiConstants.ROUTING, type = CommandType.BOOLEAN, description = "True if the Template type is routing i.e., if Template is used to deploy router")
    protected Boolean isRoutingType;

    @Parameter(name=ApiConstants.ZONE_ID_LIST,
            type=CommandType.LIST,
            collectionType = CommandType.UUID,
            entityType = ZoneResponse.class,
            required=false,
            description = "A list of zone IDs where the Template will be hosted. Use this parameter if the Template needs " +
                    "to be registered to multiple zones in one go. Use zoneid if the Template " +
                    "needs to be registered to only one zone." +
                    "Passing only -1 to this will cause the Template to be registered as a cross " +
                    "zone Template and will be copied to all zones. ")
    protected List<Long> zoneIds;

    @Parameter(name=ApiConstants.DIRECT_DOWNLOAD,
                type = CommandType.BOOLEAN,
                description = "True if Template should bypass Secondary Storage and be downloaded to Primary Storage on deployment")
    private Boolean directDownload;

    @Parameter(name=ApiConstants.DEPLOY_AS_IS,
            type = CommandType.BOOLEAN,
            description = "(VMware only) true if Instance deployments should preserve all the configurations defined for this Template", since = "4.15.1")
    protected Boolean deployAsIs;

    @Parameter(name=ApiConstants.FOR_CKS,
            type = CommandType.BOOLEAN,
            description = "if true, the templates would be available for deploying CKS clusters", since = "4.21.0")
    protected Boolean forCks;

    @Parameter(name = ApiConstants.TEMPLATE_TYPE, type = CommandType.STRING,
            description = "the type of the template. Valid options are: USER/VNF (for all users) and SYSTEM/ROUTING/BUILTIN (for admins only).",
            since = "4.19.0")
    private String templateType;

    @Parameter(name = ApiConstants.ARCH, type = CommandType.STRING,
            description = "the CPU arch of the template. Valid options are: x86_64, aarch64, s390x",
            since = "4.20")
    private String arch;

    @Parameter(name = ApiConstants.EXTENSION_ID, type = CommandType.UUID, entityType = ExtensionResponse.class,
            description = "ID of the extension",
            since = "4.21.0")
    private Long extensionId;

    @Parameter(name = ApiConstants.EXTERNAL_DETAILS, type = CommandType.MAP, description = "Details in key/value pairs using format externaldetails[i].keyname=keyvalue. Example: externaldetails[0].endpoint.url=urlvalue", since = "4.21.0")
    protected Map externalDetails;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Integer getBits() {
        return bits;
    }

    public String getDisplayText() {
        return StringUtils.isEmpty(displayText) ? templateName : displayText;
    }

    public String getFormat() {
        return format;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public Boolean isFeatured() {
        return featured;
    }

    public Boolean isPublic() {
        return publicTemplate;
    }

    public String getTemplateName() {
        return templateName;
    }

    public Long getOsTypeId() {
        return osTypeId;
    }

    public Boolean isPasswordEnabled() {
        return passwordEnabled;
    }

    public Boolean isSshKeyEnabled() {
        return sshKeyEnabled;
    }

    public Boolean isExtractable() {
        return extractable;
    }

    public Boolean getRequiresHvm() {
        return requiresHvm;
    }

    public String getUrl() {
        return url;
    }

    public List<Long> getZoneIds() {
        // This function will return null when the zoneId
        //is -1 which means all zones.
        if (zoneIds != null && !(zoneIds.isEmpty())) {
            if ((zoneIds.size() == 1) && (zoneIds.get(0) == -1L))
                return null;
            else
                return zoneIds;
        }
        if (zoneId == null)
            return null;
        if (zoneId!= null && zoneId == -1)
            return null;
        List<Long> zones = new ArrayList<>();
        zones.add(zoneId);
        return zones;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getTemplateTag() {
        return StringUtils.isBlank(templateTag) ? null : templateTag;
    }

    public Map getDetails() {
        if (details == null || details.isEmpty()) {
            return null;
        }

        Collection paramsCollection = details.values();
        Map params = (Map)(paramsCollection.toArray())[0];
        return params;
    }

    public Boolean isDynamicallyScalable() {
        return isDynamicallyScalable == null ? Boolean.FALSE : isDynamicallyScalable;
    }

    public Boolean isRoutingType() {
        return isRoutingType;
    }

    public boolean isDirectDownload() {
        return directDownload == null ? false : directDownload;
    }

    public boolean isDeployAsIs() {
        return Hypervisor.HypervisorType.VMware.toString().equalsIgnoreCase(hypervisor) &&
                Boolean.TRUE.equals(deployAsIs);
    }

    public boolean isForCks() {
        return Boolean.TRUE.equals(forCks);
    }

    public String getTemplateType() {
        return templateType;
    }

    public CPU.CPUArch getArch() {
        return CPU.CPUArch.fromType(arch);
    }

    public Long getExtensionId() {
        return extensionId;
    }

    public Map<String, String> getExternalDetails() {
        return convertExternalDetailsToMap(externalDetails);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public ApiCommandResourceType getInstanceType() {
        return ApiCommandResourceType.Template;
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalizeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }

        return accountId;
    }

    @Override
    public void execute() throws ResourceAllocationException {
        try {
            validateParameters();

            VirtualMachineTemplate template = _templateService.registerTemplate(this);
            if (template != null) {
                ListResponse<TemplateResponse> response = new ListResponse<>();
                List<TemplateResponse> templateResponses = _responseGenerator.createTemplateResponses(getResponseView(),
                        template, getZoneIds(), false);
                response.setResponses(templateResponses);
                response.setResponseName(getCommandName());
                setResponseObject(response);
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to register Template");
            }
        } catch (URISyntaxException ex1) {
            logger.info(ex1);
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, ex1.getMessage());
        }
    }

    protected void validateParameters() {
        if ((zoneId != null) && (zoneIds != null && !zoneIds.isEmpty()))
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    "Both zoneid and zoneids cannot be specified at the same time");

        if (zoneId == null && (zoneIds == null || zoneIds.isEmpty()))
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    "Either zoneid or zoneids is required. Both cannot be null.");

        if (zoneIds != null && zoneIds.size() > 1 && zoneIds.contains(-1L))
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR,
                    "Parameter zoneids cannot combine all zones (-1) option with other zones");

        String customHypervisor = HypervisorGuru.HypervisorCustomDisplayName.value();
        if (isDirectDownload() &&
                !(Hypervisor.HypervisorType.getType(getHypervisor())
                        .isFunctionalitySupported(Hypervisor.HypervisorType.Functionality.DirectDownloadTemplate)
                || getHypervisor().equalsIgnoreCase(customHypervisor))) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("Parameter directdownload " +
                    "is only allowed for KVM or %s Templates", customHypervisor));
        }

        if (!isDeployAsIs() && osTypeId == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Please provide a guest OS type");
        }
    }
}
