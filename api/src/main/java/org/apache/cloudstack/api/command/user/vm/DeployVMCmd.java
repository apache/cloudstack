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
package org.apache.cloudstack.api.command.user.vm;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.SnapshotResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.context.CallContext;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.uservm.UserVm;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "deployVirtualMachine", description = "Creates and automatically starts an Instance based on a service offering, disk offering, and Template.", responseObject = UserVmResponse.class, responseView = ResponseView.Restricted, entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false)
public class DeployVMCmd extends BaseDeployVMCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL
    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID, type = CommandType.UUID, entityType = ServiceOfferingResponse.class, required = true, description = "The ID of the Service offering for the Instance")
    private Long serviceOfferingId;

    @ACL
    @Parameter(name = ApiConstants.TEMPLATE_ID, type = CommandType.UUID, entityType = TemplateResponse.class, description = "The ID of the Template for the Instance")
    private Long templateId;

    @Parameter(name = ApiConstants.VOLUME_ID, type = CommandType.UUID, entityType = VolumeResponse.class, since = "4.21")
    private Long volumeId;

    @Parameter(name = ApiConstants.SNAPSHOT_ID, type = CommandType.UUID, entityType = SnapshotResponse.class, since = "4.21")
    private Long snapshotId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Long getVolumeId() {
        return volumeId;
    }

    public Long getSnapshotId() {
        return snapshotId;
    }

    public boolean isVolumeOrSnapshotProvided() {
        return volumeId != null || snapshotId != null;
    }

    public boolean isBlankInstance() {
        return false;
    }



    /////////////////////////////////////////////////////
    ////////////////// Setters //////////////////////////
    /////////////////////////////////////////////////////
    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public void setDomainId(Long domainId) {
        this.domainId = domainId;
    }

    public void setNetworkIds(List<Long> networkIds) {
        this.networkIds = networkIds;
    }

    public void setBootType(String bootType) {
        this.bootType = bootType;
    }

    public void setBootMode(String bootMode) {
        this.bootMode = bootMode;
    }

    public void setHypervisor(String hypervisor) {
        this.hypervisor = hypervisor;
    }

    public void setUserData(String userData) {
        this.userData = userData;
    }

    public void setKeyboard(String keyboard) {
        this.keyboard = keyboard;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public void setDisplayVm(Boolean displayVm) {
        this.displayVm = displayVm;
    }

    public void setUserDataId(Long userDataId) {
        this.userdataId = userDataId;
    }

    public void setAffinityGroupIds(List<Long> ids) {
        this.affinityGroupIdList = ids;
    }

    public void setDetails(Map details) {
        this.details = details;
    }

    public void setExtraConfig(String extraConfig) {
        this.extraConfig = extraConfig;
    }

    public void setDynamicScalingEnabled(Boolean dynamicScalingEnabled) {
        this.dynamicScalingEnabled = dynamicScalingEnabled;
    }

    public void setServiceOfferingId(Long serviceOfferingId) {
        this.serviceOfferingId = serviceOfferingId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public void setVolumeId(Long volumeId) {
        this.volumeId = volumeId;
    }

    public void setSnapshotId(Long snapshotId) {
        this.snapshotId = snapshotId;
    }

    @Override
    public void execute() {
        UserVm result;

        CallContext.current().setEventDetails("Instance ID: " + getEntityUuid());
        if (getStartVm()) {
            try {
                result = _userVmService.startVirtualMachine(this);
            } catch (ResourceUnavailableException ex) {
                logger.warn("Exception: ", ex);
                throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
            } catch (ResourceAllocationException ex) {
                logger.warn("Exception: ", ex);
                throw new ServerApiException(ApiErrorCode.RESOURCE_ALLOCATION_ERROR, ex.getMessage());
            } catch (ConcurrentOperationException ex) {
                logger.warn("Exception: ", ex);
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
            } catch (InsufficientCapacityException ex) {
                StringBuilder message = new StringBuilder(ex.getMessage());
                if (ex instanceof InsufficientServerCapacityException) {
                    if (((InsufficientServerCapacityException)ex).isAffinityApplied()) {
                        message.append(", Please check the affinity groups provided, there may not be sufficient capacity to follow them");
                    }
                }
                logger.info("{}: {}", message.toString(), ex.getLocalizedMessage());
                logger.debug(message.toString(), ex);
                throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, message.toString());
            }
        } else {
            logger.info("Instance {} already created, load UserVm from DB", getEntityUuid());
            result = _userVmService.finalizeCreateVirtualMachine(getEntityId());
        }

        if (result != null) {
            UserVmResponse response = _responseGenerator.createUserVmResponse(getResponseView(), "virtualmachine", result).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to deploy Instance UUID:"+getEntityUuid());
        }
    }

    @Override
    public void create() throws ResourceAllocationException {
        if (!isBlankInstance() && Stream.of(templateId, snapshotId, volumeId).filter(Objects::nonNull).count() != 1) {
            throw new CloudRuntimeException("Please provide only one of the following parameters - template ID, volume ID or snapshot ID");
        }

        try {
            UserVm vm = _userVmService.createVirtualMachine(this);

            if (vm != null) {
                setEntityId(vm.getId());
                setEntityUuid(vm.getUuid());
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to deploy Instance");
            }
        } catch (InsufficientCapacityException ex) {
            logger.info(ex);
            logger.trace(ex.getMessage(), ex);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, ex.getMessage());
        } catch (ResourceUnavailableException ex) {
            logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        }  catch (ConcurrentOperationException ex) {
            logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        } catch (ResourceAllocationException ex) {
            logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.RESOURCE_ALLOCATION_ERROR, ex.getMessage());
        }
    }
}
