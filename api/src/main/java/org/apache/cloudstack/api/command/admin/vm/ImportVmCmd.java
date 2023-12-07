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

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VmwareDatacenterResponse;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

@APICommand(name = "importVm",
        description = "Import virtual machine from a unmanaged host into CloudStack",
        responseObject = UserVmResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin},
        since = "4.19.0")
public class ImportVmCmd extends ImportUnmanagedInstanceCmd {

    public static final Logger LOGGER = Logger.getLogger(ImportVmCmd.class);

    @Parameter(name = ApiConstants.HYPERVISOR,
            type = CommandType.STRING,
            required = true,
            description = "hypervisor type of the host")
    private String hypervisor;

    @Parameter(name = ApiConstants.IMPORT_SOURCE,
            type = CommandType.STRING,
            required = true,
            description = "Source location for Import" )
    private String importSource;

    // Import from Vmware to KVM migration parameters

    @Parameter(name = ApiConstants.EXISTING_VCENTER_ID,
            type = CommandType.UUID,
            entityType = VmwareDatacenterResponse.class,
            description = "(only for importing migrated VMs from Vmware to KVM) UUID of a linked existing vCenter")
    private Long existingVcenterId;

    @Parameter(name = ApiConstants.HOST_IP,
            type = BaseCmd.CommandType.STRING,
            description = "(only for importing migrated VMs from Vmware to KVM) VMware ESXi host IP/Name.")
    private String host;

    @Parameter(name = ApiConstants.VCENTER,
            type = CommandType.STRING,
            description = "(only for importing migrated VMs from Vmware to KVM) The name/ip of vCenter. Make sure it is IP address or full qualified domain name for host running vCenter server.")
    private String vcenter;

    @Parameter(name = ApiConstants.DATACENTER_NAME, type = CommandType.STRING,
            description = "(only for importing migrated VMs from Vmware to KVM) Name of VMware datacenter.")
    private String datacenterName;

    @Parameter(name = ApiConstants.CLUSTER_NAME, type = CommandType.STRING,
            description = "(only for importing migrated VMs from Vmware to KVM) Name of VMware cluster.")
    private String clusterName;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING,
            description = "(only for importing migrated VMs from Vmware to KVM) The Username required to connect to resource.")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING,
            description = "(only for importing migrated VMs from Vmware to KVM) The password for the specified username.")
    private String password;

    @Parameter(name = ApiConstants.CONVERT_INSTANCE_HOST_ID, type = CommandType.UUID, entityType = HostResponse.class,
            description = "(only for importing migrated VMs from Vmware to KVM) optional - the host to perform the virt-v2v migration from VMware to KVM.")
    private Long convertInstanceHostId;

    @Parameter(name = ApiConstants.CONVERT_INSTANCE_STORAGE_POOL_ID, type = CommandType.UUID, entityType = StoragePoolResponse.class,
            description = "(only for importing migrated VMs from Vmware to KVM) optional - the temporary storage pool to perform the virt-v2v migration from VMware to KVM.")
    private Long convertStoragePoolId;

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_IMPORT;
    }

    @Override
    public String getEventDescription() {
        String vmName = getName();
        if (ObjectUtils.anyNotNull(vcenter, existingVcenterId)) {
            String msg = StringUtils.isNotBlank(vcenter) ?
                    String.format("external vCenter: %s - datacenter: %s", vcenter, datacenterName) :
                    String.format("existing vCenter Datacenter with ID: %s", existingVcenterId);
            return String.format("Importing unmanaged VM: %s from %s - VM: %s", getDisplayName(), msg, vmName);
        }
        return String.format("Importing unmanaged VM: %s", vmName);
    }


    public Long getExistingVcenterId() {
        return existingVcenterId;
    }

    public String getHost() {
        return host;
    }

    public String getVcenter() {
        return vcenter;
    }

    public String getDatacenterName() {
        return datacenterName;
    }

    public String getClusterName() {
        return clusterName;
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

    public Long getConvertStoragePoolId() {
        return convertStoragePoolId;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public String getImportSource() {
        return importSource;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        UserVmResponse response = vmImportService.importVm(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

}
