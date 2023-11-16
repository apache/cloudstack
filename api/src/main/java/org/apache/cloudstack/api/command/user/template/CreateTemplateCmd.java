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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.GuestOSResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.lang3.StringUtils;

import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Volume;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;

@APICommand(name = "createTemplate", responseObject = TemplateResponse.class, description = "Creates a template of a virtual machine. " + "The virtual machine must be in a STOPPED state. "
        + "A template created from this command is automatically designated as a private template visible to the account that created it.", responseView = ResponseView.Restricted,
    requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateTemplateCmd extends BaseAsyncCreateCmd implements UserCmd {
    private static final String s_name = "createtemplateresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.BITS, type = CommandType.INTEGER, description = "32 or 64 bit")
    private Integer bits;

    @Parameter(name = ApiConstants.DISPLAY_TEXT,
               type = CommandType.STRING,
               description = "The display text of the template, defaults to the 'name'.",
               length = 4096)
    private String displayText;

    @Parameter(name = ApiConstants.IS_FEATURED, type = CommandType.BOOLEAN, description = "true if this template is a featured template, false otherwise")
    private Boolean featured;

    @Parameter(name = ApiConstants.IS_PUBLIC, type = CommandType.BOOLEAN, description = "true if this template is a public template, false otherwise")
    private Boolean publicTemplate;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "the name of the template")
    private String templateName;

    @Parameter(name = ApiConstants.OS_TYPE_ID,
               type = CommandType.UUID,
               entityType = GuestOSResponse.class,
               required = true,
               description = "the ID of the OS Type that best represents the OS of this template.")
    private Long osTypeId;

    @Parameter(name = ApiConstants.PASSWORD_ENABLED,
               type = CommandType.BOOLEAN,
               description = "true if the template supports the password reset feature; default is false")
    private Boolean passwordEnabled;

    @Parameter(name = ApiConstants.SSHKEY_ENABLED, type = CommandType.BOOLEAN, description = "true if the template supports the sshkey upload feature; default is false")
    private Boolean sshKeyEnabled;

    @Parameter(name = ApiConstants.REQUIRES_HVM, type = CommandType.BOOLEAN, description = "true if the template requires HVM, false otherwise")
    private Boolean requiresHvm;

    @Parameter(name = ApiConstants.SNAPSHOT_ID,
               type = CommandType.UUID,
               entityType = SnapshotResponse.class,
            description = "the ID of the snapshot the template is being created from. Either this parameter, or volumeId has to be passed in")
    protected Long snapshotId;

    @Parameter(name = ApiConstants.VOLUME_ID,
               type = CommandType.UUID,
               entityType = VolumeResponse.class,
            description = "the ID of the disk volume the template is being created from. Either this parameter, or snapshotId has to be passed in")
    protected Long volumeId;

    @Parameter(name=ApiConstants.VIRTUAL_MACHINE_ID, type=CommandType.UUID, entityType = UserVmResponse.class,
            description="Optional, VM ID. If this presents, it is going to create a baremetal template for VM this ID refers to. This is only for VM whose hypervisor type is BareMetal")
    protected Long vmId;

    @Parameter(name = ApiConstants.URL,
               type = CommandType.STRING,
               length = 2048,
               description = "Optional, only for baremetal hypervisor. The directory name where template stored on CIFS server")
    private String url;

    @Parameter(name = ApiConstants.TEMPLATE_TAG, type = CommandType.STRING, description = "the tag for this template.")
    private String templateTag;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, description = "Template details in key/value pairs using format details[i].keyname=keyvalue. Example: details[0].hypervisortoolsversion=xenserver61")
    protected Map details;

    @Parameter(name = ApiConstants.IS_DYNAMICALLY_SCALABLE,
               type = CommandType.BOOLEAN,
               description = "true if template contains XS/VMWare tools inorder to support dynamic scaling of VM cpu/memory")
    protected Boolean isDynamicallyScalable;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class, description = "create template for the project")
    private Long projectId;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "the zone for the template. Can be specified with snapshot only", since = "4.19.0")
    private Long zoneId;

    @Parameter(name = ApiConstants.DOMAIN_ID,
          type = CommandType.UUID,
          entityType = DomainResponse.class,
          description = "an optional domainId. If the account parameter is used, domainId must also be used.",
          since = "4.19.0")
    private Long domainId;

    @Parameter(name = ApiConstants.ACCOUNT,
          type = CommandType.STRING,
          description = "an optional accountName. Must be used with domainId.",
          since = "4.19.0")
    private String accountName;

    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Integer getBits() {
        return bits;
    }

    public String getDisplayText() {
        return StringUtils.isEmpty(displayText) ? templateName : displayText;
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

    public Boolean getRequiresHvm() {
        return requiresHvm;
    }

    public Long getSnapshotId() {
        return snapshotId;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public Long getVmId() {
        return vmId;
    }

    public String getUrl() {
        return url;
    }

    public String getTemplateTag() {
        return templateTag;
    }

    public Map getDetails() {
        if (details == null || details.isEmpty()) {
            return null;
        }

        Collection paramsCollection = details.values();
        Map params = (Map)(paramsCollection.toArray())[0];
        return params;
    }

    public boolean isDynamicallyScalable() {
        return isDynamicallyScalable == null ? false : isDynamicallyScalable;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getAccountName() {
        return accountName;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "template";
    }

    @Override
    public long getEntityOwnerId() {
        Account callingAccount = CallContext.current().getCallingAccount();
        ensureAccessCheck(callingAccount);
        return findAccountIdToUse(callingAccount);
    }


    @Override
    public String getEventType() {
        return EventTypes.EVENT_TEMPLATE_CREATE;
    }

    @Override
    public String getEventDescription() {
        return "creating template: " + getTemplateName();
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Template;
    }

    protected boolean isBareMetal() {
        return (getVmId() != null && getUrl() != null);
    }

    @Override
    public void create() throws ResourceAllocationException {
        VirtualMachineTemplate template = null;
        //TemplateOwner should be the caller https://issues.citrite.net/browse/CS-17530
        template = _templateService.createPrivateTemplateRecord(this, _accountService.getAccount(getEntityOwnerId()));
        if (template != null) {
            setEntityId(template.getId());
            setEntityUuid(template.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create a template");
        }

    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails(
            "Template Id: " + getEntityUuid() + ((getSnapshotId() == null) ? " from volume Id: " + this._uuidMgr.getUuid(Volume.class, getVolumeId()) : " from snapshot Id: " + this._uuidMgr.getUuid(Snapshot.class, getSnapshotId())));
        VirtualMachineTemplate template = _templateService.createPrivateTemplate(this);

        if (template != null) {
            List<TemplateResponse> templateResponses;
            if (isBareMetal()) {
                templateResponses = _responseGenerator.createTemplateResponses(getResponseView(), template.getId(), vmId);
            } else {
                templateResponses = _responseGenerator.createTemplateResponses(getResponseView(), template.getId(), snapshotId, volumeId, false);
            }
            TemplateResponse response = new TemplateResponse();
            if (templateResponses != null && !templateResponses.isEmpty()) {
                response = templateResponses.get(0);
            }
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create private template");
        }

    }

    /***
     * Performs access check on volume and snapshot for given account
     * @param account
     */
    private void ensureAccessCheck(Account account) {
        if (volumeId != null) {
            Volume volume = _entityMgr.findById(Volume.class, volumeId);
            if (volume != null) {
                _accountService.checkAccess(account, SecurityChecker.AccessType.UseEntry, false, volume);
            } else {
                throw new InvalidParameterValueException("Unable to find volume by id=" + volumeId);
            }
        } else {
            Snapshot snapshot = _entityMgr.findById(Snapshot.class, snapshotId);
            if (snapshot != null) {
                _accountService.checkAccess(account, SecurityChecker.AccessType.UseEntry, false, snapshot);
            } else {
                throw new InvalidParameterValueException("Unable to find snapshot by id=" + snapshotId);
            }
        }
    }

    /***
     * Find accountId based on accountName and domainId or projectId
     * if not found, return callingAccountId for further use
     * @param callingAccount
     * @return accountId
     */
    private Long findAccountIdToUse(Account callingAccount) {
        Long accountIdToUse = null;
        try {
            accountIdToUse = _accountService.finalyzeAccountId(accountName, domainId, projectId, true);
        } catch (InvalidParameterValueException | PermissionDeniedException ex) {
            if (logger.isDebugEnabled()) {
                logger.debug(String.format("An exception occurred while finalizing account id with accountName, domainId and projectId" +
                      "using callingAccountId=%s", callingAccount.getUuid()), ex);
            }
            logger.warn("Unable to find accountId associated with accountName=" + accountName + " and domainId="
                  + domainId + " or projectId=" + projectId + ", using callingAccountId=" + callingAccount.getUuid());
        }
        return accountIdToUse != null ? accountIdToUse : callingAccount.getAccountId();
    }
}
