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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.BackupResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserVmResponse;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InsufficientServerCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.uservm.UserVm;
import com.cloud.vm.VirtualMachine;

@APICommand(name = "createVMFromBackup",
        description = "Creates and automatically starts a VM from a backup.",
        responseObject = UserVmResponse.class,
        responseView = ResponseObject.ResponseView.Restricted,
        entityType = {VirtualMachine.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = true,
        since = "4.21.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CreateVMFromBackupCmd extends BaseDeployVMCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.BACKUP_ID,
            type = CommandType.UUID,
            entityType = BackupResponse.class,
            required = true,
            description = "backup ID to create the VM from")
    private Long backupId;

    @ACL
    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID, type = CommandType.UUID, entityType = ServiceOfferingResponse.class, description = "the ID of the service offering for the virtual machine")
    private Long serviceOfferingId;

    @ACL
    @Parameter(name = ApiConstants.TEMPLATE_ID, type = CommandType.UUID, entityType = TemplateResponse.class, description = "the ID of the template for the virtual machine")
    private Long templateId;

    @Parameter(name = ApiConstants.PRESERVE_IP, type = CommandType.BOOLEAN, description = "Use the same IP/MAC addresses as stored in the backup metadata. Works only if the original Instance is deleted and the IP/MAC address is available.")
    private Boolean preserveIp;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getBackupId() {
        return backupId;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public boolean getPreserveIp() {
        return (preserveIp != null) ? preserveIp : false;
    }

    @Override
    public void create() {
        UserVm vm;
        try {
            vm = _userVmService.allocateVMFromBackup(this);
            if (vm != null) {
                setEntityId(vm.getId());
                setEntityUuid(vm.getUuid());
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to deploy vm");
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

    @Override
    public void execute () {
        UserVm vm = null;
        try {
            vm = _userVmService.restoreVMFromBackup(this);
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
            logger.info(String.format("%s: %s", message.toString(), ex.getLocalizedMessage()));
            logger.debug(message.toString(), ex);
            throw new ServerApiException(ApiErrorCode.INSUFFICIENT_CAPACITY_ERROR, message.toString());
        }

        if (vm != null) {
            UserVmResponse response = _responseGenerator.createUserVmResponse(getResponseView(), "virtualmachine", vm).get(0);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to deploy vm uuid:"+getEntityUuid());
        }
    }
}
