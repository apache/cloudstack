/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.api.command.user.template;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Map;

import com.cloud.cpu.CPU;
import com.cloud.hypervisor.Hypervisor;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.AbstractGetUploadParamsCmd;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.GetUploadParamsResponse;
import org.apache.cloudstack.api.response.GuestOSResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.StringUtils;

import com.cloud.exception.ResourceAllocationException;

@APICommand(name = "getUploadParamsForTemplate", description = "Upload an existing Template into the CloudStack cloud. ",
        responseObject = GetUploadParamsResponse.class, since = "4.6.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class GetUploadParamsForTemplateCmd extends AbstractGetUploadParamsCmd {

    private static final String s_name = "postuploadtemplateresponse";

    @Parameter(name = ApiConstants.DISPLAY_TEXT, type = CommandType.STRING, description = "The display text of the Template. This is usually used for display purposes.", length = 4096)
    private String displayText;

    @Parameter(name = ApiConstants.HYPERVISOR, type = CommandType.STRING, required = true, description = "The target hypervisor for the Template")
    private String hypervisor;

    @Parameter(name = ApiConstants.OS_TYPE_ID, type = CommandType.UUID, entityType = GuestOSResponse.class, required = false,
            description = "The ID of the OS Type that best represents the OS of this Template. Not required for VMware as the guest OS is obtained from the OVF file.")
    private Long osTypeId;

    @Parameter(name = ApiConstants.ARCH, type = CommandType.STRING,
            description = "the CPU arch of the template. Valid options are: x86_64, aarch64, s390x",
            since = "4.20")
    private String arch;

    @Parameter(name = ApiConstants.BITS, type = CommandType.INTEGER, description = "32 or 64 bits support. 64 by default")
    private Integer bits;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, description = "Template details in key/value pairs.")
    private Map details;

    @Parameter(name = ApiConstants.IS_DYNAMICALLY_SCALABLE, type = CommandType.BOOLEAN, description = "True if Template contains XS/VMWare tools in order to support dynamic scaling of Instance CPU/memory")
    private Boolean isDynamicallyScalable;

    @Parameter(name = ApiConstants.IS_EXTRACTABLE, type = CommandType.BOOLEAN, description = "True if the Template or its derivatives are extractable; default is false")
    private Boolean extractable;

    @Parameter(name = ApiConstants.IS_FEATURED, type = CommandType.BOOLEAN, description = "True if this Template is a featured Template, false otherwise")
    private Boolean featured;

    @Parameter(name = ApiConstants.IS_PUBLIC, type = CommandType.BOOLEAN, description = "True if the Template is available to all accounts; default is true")
    private Boolean publicTemplate;

    @Parameter(name = ApiConstants.ROUTING, type = CommandType.BOOLEAN, description = "True if the Template type is routing i.e., if Template is used to deploy router")
    private Boolean isRoutingType;

    @Parameter(name = ApiConstants.PASSWORD_ENABLED, type = CommandType.BOOLEAN, description = "True if the Template supports the password reset feature; default is false")
    private Boolean passwordEnabled;

    @Parameter(name = ApiConstants.REQUIRES_HVM, type = CommandType.BOOLEAN, description = "True if this Template requires HVM")
    private Boolean requiresHvm;

    @Parameter(name = ApiConstants.SSHKEY_ENABLED, type = CommandType.BOOLEAN, description = "True if the Template supports the SSHkey upload feature; default is false")
    private Boolean sshKeyEnabled;

    @Parameter(name = ApiConstants.TEMPLATE_TAG, type = CommandType.STRING, description = "The tag for this Template.")
    private String templateTag;

    @Parameter(name=ApiConstants.DEPLOY_AS_IS,
            type = CommandType.BOOLEAN,
            description = "(VMware only) true if Instance deployments should preserve all the configurations defined for this Template", since = "4.15.1")
    private Boolean deployAsIs;

    @Parameter(name=ApiConstants.FOR_CKS,
            type = CommandType.BOOLEAN,
            description = "if true, the templates would be available for deploying CKS clusters", since = "4.21.0")
    protected Boolean forCks;

    @Parameter(name = ApiConstants.TEMPLATE_TYPE, type = CommandType.STRING,
            description = "the type of the template. Valid options are: USER/VNF (for all users) and SYSTEM/ROUTING/BUILTIN (for admins only).",
            since = "4.22.0")
    private String templateType;

    public String getDisplayText() {
        return StringUtils.isBlank(displayText) ? getName() : displayText;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public Long getOsTypeId() {
        return osTypeId;
    }

    public Integer getBits() {
        return bits;
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
        if (isDynamicallyScalable == null) {
            return Boolean.FALSE;
        }
        return isDynamicallyScalable;
    }

    public Boolean isExtractable() {
        return extractable;
    }

    public Boolean isFeatured() {
        return featured;
    }

    public Boolean isPublic() {
        return publicTemplate;
    }

    public Boolean isRoutingType() {
        return isRoutingType;
    }

    public Boolean isPasswordEnabled() {
        return passwordEnabled;
    }

    public Boolean getRequiresHvm() {
        return requiresHvm;
    }

    public Boolean isSshKeyEnabled() {
        return sshKeyEnabled;
    }

    public String getTemplateTag() {
        return StringUtils.isBlank(templateTag) ? null : templateTag;
    }

    public boolean isDeployAsIs() {
        return Hypervisor.HypervisorType.VMware.toString().equalsIgnoreCase(hypervisor) &&
                Boolean.TRUE.equals(deployAsIs);
    }

    public boolean isForCks() {
        return Boolean.TRUE.equals(forCks);
    }

    public CPU.CPUArch getArch() {
        return CPU.CPUArch.fromType(arch);
    }

    public String getTemplateType() {
        return templateType;
    }

    @Override
    public void execute() throws ServerApiException {
        validateRequest();
        try {
            GetUploadParamsResponse response = _templateService.registerTemplateForPostUpload(this);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } catch (ResourceAllocationException | MalformedURLException e) {
            logger.error("Exception while registering Template", e);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Exception while registering Template: " + e.getMessage());
        }
    }

    private void validateRequest() {
        if (getZoneId() <= 0) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid zoneid");
        }
        if (!isDeployAsIs() && osTypeId == null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Missing parameter ostypeid");
        }
        if (isDeployAsIs() && osTypeId != null) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid parameter ostypeid, not applicable for" +
                    "VMware when deploy-as-is is set to true");
        }
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalizeAccountId(getAccountName(), getDomainId(), getProjectId(), true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }
        return accountId;
    }
}
