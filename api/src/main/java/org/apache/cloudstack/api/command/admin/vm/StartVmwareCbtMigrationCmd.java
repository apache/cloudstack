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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.VmwareCbtMigrationResponse;
import org.apache.cloudstack.api.response.VmwareDatacenterResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.VmwareCbtMigrationManager;
import org.apache.commons.collections.MapUtils;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;

@APICommand(name = "startVmwareCbtMigration",
        description = "Start tracking a VMware CBT based migration session",
        responseObject = VmwareCbtMigrationResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        requestHasSensitiveInfo = true,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin},
        since = "4.22.1")
public class StartVmwareCbtMigrationCmd extends BaseCmd {

    @Inject
    public VmwareCbtMigrationManager vmwareCbtMigrationManager;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class,
            required = true, description = "the destination zone ID")
    private Long zoneId;

    @Parameter(name = ApiConstants.CLUSTER_ID, type = CommandType.UUID, entityType = ClusterResponse.class,
            required = true, description = "the destination KVM cluster ID")
    private Long clusterId;

    @Parameter(name = ApiConstants.DISPLAY_NAME, type = CommandType.STRING,
            description = "the display name of the migrated VM")
    private String displayName;

    @Parameter(name = ApiConstants.SOURCE_VM_NAME, type = CommandType.STRING,
            required = true, description = "the source VMware VM name")
    private String sourceVmName;

    @Parameter(name = ApiConstants.EXISTING_VCENTER_ID, type = CommandType.UUID, entityType = VmwareDatacenterResponse.class,
            description = "UUID of a linked existing vCenter")
    private Long existingVcenterId;

    @Parameter(name = ApiConstants.VCENTER, type = CommandType.STRING,
            description = "the source vCenter IP address or FQDN")
    private String vcenter;

    @Parameter(name = ApiConstants.DATACENTER_NAME, type = CommandType.STRING,
            description = "the source VMware datacenter name")
    private String datacenterName;

    @Parameter(name = ApiConstants.CLUSTER_NAME, type = CommandType.STRING,
            description = "the source VMware cluster name")
    private String sourceCluster;

    @Parameter(name = ApiConstants.HOST_IP, type = CommandType.STRING,
            description = "the source VMware ESXi host IP address or FQDN")
    private String sourceHost;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING,
            description = "the username for the source vCenter")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING,
            description = "the password for the source vCenter")
    private String password;

    @Parameter(name = ApiConstants.CONVERT_INSTANCE_HOST_ID, type = CommandType.UUID, entityType = HostResponse.class,
            description = "optional KVM host to perform virt-v2v conversion and CBT block replication")
    private Long convertInstanceHostId;

    @Parameter(name = ApiConstants.CONVERT_INSTANCE_STORAGE_POOL_ID, type = CommandType.UUID, entityType = StoragePoolResponse.class,
            description = "optional primary storage pool for converted disks")
    private Long storagePoolId;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP,
            description = "optional VDDK details for CBT replication, such as vddk.lib.dir, vddk.transports and vddk.thumbprint")
    private Map details;

    public Long getZoneId() {
        return zoneId;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSourceVmName() {
        return sourceVmName;
    }

    public Long getExistingVcenterId() {
        return existingVcenterId;
    }

    public String getVcenter() {
        return vcenter;
    }

    public String getDatacenterName() {
        return datacenterName;
    }

    public String getSourceCluster() {
        return sourceCluster;
    }

    public String getSourceHost() {
        return sourceHost;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Long getConvertInstanceHostId() {
        return convertInstanceHostId;
    }

    public Long getStoragePoolId() {
        return storagePoolId;
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> getDetails() {
        Map<String, String> params = new HashMap<>();
        if (MapUtils.isEmpty(details)) {
            return params;
        }

        for (Object value : details.values()) {
            if (!(value instanceof Map)) {
                continue;
            }
            Map<Object, Object> detailMap = (Map<Object, Object>)value;
            for (Map.Entry<Object, Object> entry : detailMap.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    params.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
        }
        return params;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException,
            ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        VmwareCbtMigrationResponse response = vmwareCbtMigrationManager.startVmwareCbtMigration(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();
        return account == null ? Account.ACCOUNT_ID_SYSTEM : account.getId();
    }
}
