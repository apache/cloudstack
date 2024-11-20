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
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VmwareDatacenterResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.vm.VmImportService;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;

@APICommand(name = "importVm",
        description = "Import virtual machine from a unmanaged host into CloudStack",
        responseObject = UserVmResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin},
        since = "4.19.0")
public class ImportVmCmd extends ImportUnmanagedInstanceCmd {

    @Inject
    public VmImportService vmImportService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////


    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            required = true,
            description = "the zone ID")
    private Long zoneId;

    @Parameter(name = ApiConstants.USERNAME,
            type = CommandType.STRING,
            description = "the username for the host")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD,
            type = CommandType.STRING,
            description = "the password for the host")
    private String password;

    @Parameter(name = ApiConstants.HOST,
            type = CommandType.STRING,
            description = "the host name or IP address")
    private String host;

    @Parameter(name = ApiConstants.HYPERVISOR,
            type = CommandType.STRING,
            required = true,
            description = "hypervisor type of the host")
    private String hypervisor;

    @Parameter(name = ApiConstants.DISK_PATH,
            type = CommandType.STRING,
            description = "path of the disk image")
    private String diskPath;

    @Parameter(name = ApiConstants.IMPORT_SOURCE,
            type = CommandType.STRING,
            required = true,
            description = "Source location for Import" )
    private String importSource;

    @Parameter(name = ApiConstants.NETWORK_ID,
            type = CommandType.UUID,
            entityType = NetworkResponse.class,
            description = "the network ID")
    private Long networkId;

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class, description = "Host where local disk is located")
    private Long hostId;

    @Parameter(name = ApiConstants.STORAGE_ID, type = CommandType.UUID, entityType = StoragePoolResponse.class, description = "Shared storage pool where disk is located")
    private Long storagePoolId;

    @Parameter(name = ApiConstants.TEMP_PATH,
            type = CommandType.STRING,
            description = "Temp Path on external host for disk image copy" )
    private String tmpPath;

    // Import from VMware to KVM migration parameters

    @Parameter(name = ApiConstants.EXISTING_VCENTER_ID,
            type = CommandType.UUID,
            entityType = VmwareDatacenterResponse.class,
            description = "(only for importing VMs from VMware to KVM) UUID of a linked existing vCenter")
    private Long existingVcenterId;

    @Parameter(name = ApiConstants.HOST_IP,
            type = BaseCmd.CommandType.STRING,
            description = "(only for importing VMs from VMware to KVM) VMware ESXi host IP/Name.")
    private String hostip;

    @Parameter(name = ApiConstants.VCENTER,
            type = CommandType.STRING,
            description = "(only for importing VMs from VMware to KVM) The name/ip of vCenter. Make sure it is IP address or full qualified domain name for host running vCenter server.")
    private String vcenter;

    @Parameter(name = ApiConstants.DATACENTER_NAME, type = CommandType.STRING,
            description = "(only for importing VMs from VMware to KVM) Name of VMware datacenter.")
    private String datacenterName;

    @Parameter(name = ApiConstants.CLUSTER_NAME, type = CommandType.STRING,
            description = "(only for importing VMs from VMware to KVM) Name of VMware cluster.")
    private String clusterName;

    @Parameter(name = ApiConstants.CONVERT_INSTANCE_HOST_ID, type = CommandType.UUID, entityType = HostResponse.class,
            description = "(only for importing VMs from VMware to KVM) optional - the host to perform the virt-v2v migration from VMware to KVM.")
    private Long convertInstanceHostId;

    @Parameter(name = ApiConstants.CONVERT_INSTANCE_STORAGE_POOL_ID, type = CommandType.UUID, entityType = StoragePoolResponse.class,
            description = "(only for importing VMs from VMware to KVM) optional - the temporary storage pool to perform the virt-v2v migration from VMware to KVM.")
    private Long convertStoragePoolId;

    @Parameter(name = ApiConstants.FORCE_MS_TO_IMPORT_VM_FILES, type = CommandType.BOOLEAN,
            description = "(only for importing VMs from VMware to KVM) optional - if true, forces MS to import VM file(s) to temporary storage, else uses KVM Host if ovftool is available, falls back to MS if not.")
    private Boolean forceMsToImportVmFiles;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getZoneId() {
        return zoneId;
    }

    public Long getExistingVcenterId() {
        return existingVcenterId;
    }

    public String getHostIp() {
        return hostip;
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

    public String getHost() {
        return host;
    }

    public Long getConvertInstanceHostId() {
        return convertInstanceHostId;
    }

    public Long getConvertStoragePoolId() {
        return convertStoragePoolId;
    }

    public Boolean getForceMsToImportVmFiles() {
        return BooleanUtils.toBooleanDefaultIfNull(forceMsToImportVmFiles, false);
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public String getDiskPath() {
        return diskPath;
    }

    public String getImportSource() {
        return importSource;
    }

    public Long getHostId() {
        return hostId;
    }

    public Long getStoragePoolId() {
        return storagePoolId;
    }

    public String getTmpPath() {
        return tmpPath;
    }

    public Long getNetworkId() {
        return networkId;
    }

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
