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
package org.apache.cloudstack.api.command.admin.vm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.VmwareCbtMigrationResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.VmwareCbtMigrationManager;
import org.apache.cloudstack.vm.VmwareCbtTargetDiskInfo;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;

@APICommand(name = "registerVmwareCbtMigrationTarget",
        description = "Register KVM target disk paths produced by the initial full sync for a VMware CBT migration",
        responseObject = VmwareCbtMigrationResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin},
        since = "4.22.1")
public class RegisterVmwareCbtMigrationTargetCmd extends BaseCmd {

    @Inject
    public VmwareCbtMigrationManager vmwareCbtMigrationManager;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = VmwareCbtMigrationResponse.class,
            required = true, description = "the VMware CBT migration ID")
    private Long id;

    @Parameter(name = ApiConstants.TARGET_DISK_LIST, type = CommandType.MAP, required = true,
            description = "source disk to KVM target disk mapping. Example: targetdisklist[0].sourcediskid=scsi0:0&" +
                    "targetdisklist[0].targetpath=/var/lib/libvirt/images/vm-disk.qcow2&targetdisklist[0].targetformat=qcow2&" +
                    "targetdisklist[0].changeid=<vmware-change-id>")
    private Map targetDiskList;

    public Long getId() {
        return id;
    }

    @SuppressWarnings("unchecked")
    public List<VmwareCbtTargetDiskInfo> getTargetDisks() {
        if (MapUtils.isEmpty(targetDiskList)) {
            throw new InvalidParameterValueException("Target disk list cannot be empty");
        }

        List<VmwareCbtTargetDiskInfo> targetDisks = new ArrayList<>();
        for (Map<String, String> entry : (Collection<Map<String, String>>)targetDiskList.values()) {
            String sourceDiskId = StringUtils.trimToNull(entry.get(ApiConstants.SOURCE_DISK_ID));
            String targetPath = StringUtils.trimToNull(entry.get(ApiConstants.TARGET_PATH));
            String targetFormat = StringUtils.trimToNull(entry.get(ApiConstants.TARGET_FORMAT));
            String changeId = StringUtils.trimToNull(entry.get(ApiConstants.CHANGE_ID));
            String snapshotMor = StringUtils.trimToNull(entry.get(ApiConstants.SNAPSHOT_MOR));

            if (StringUtils.isAnyBlank(sourceDiskId, targetPath)) {
                throw new InvalidParameterValueException(String.format("%s and %s are required for each target disk",
                        ApiConstants.SOURCE_DISK_ID, ApiConstants.TARGET_PATH));
            }
            targetDisks.add(new VmwareCbtTargetDiskInfo(sourceDiskId, targetPath, targetFormat, changeId, snapshotMor));
        }
        return targetDisks;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        VmwareCbtMigrationResponse response = vmwareCbtMigrationManager.registerVmwareCbtMigrationTarget(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();
        return account == null ? Account.ACCOUNT_ID_SYSTEM : account.getId();
    }
}
