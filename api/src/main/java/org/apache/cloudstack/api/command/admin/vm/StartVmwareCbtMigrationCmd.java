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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.GuestOSResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.VmwareCbtMigrationResponse;
import org.apache.cloudstack.api.response.VmwareDatacenterResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.VmwareCbtMigrationManager;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.event.EventTypes;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.offering.DiskOffering;
import com.cloud.user.Account;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.VmDetailConstants;

@APICommand(name = "startVmwareCbtMigration",
        description = "Start tracking a VMware CBT based migration session",
        responseObject = VmwareCbtMigrationResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        requestHasSensitiveInfo = true,
        responseHasSensitiveInfo = false,
        authorized = {RoleType.Admin},
        since = "4.22.1")
public class StartVmwareCbtMigrationCmd extends BaseAsyncCmd {

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

    @Parameter(name = ApiConstants.HOST_NAME, type = CommandType.STRING,
            description = "the host name of the migrated VM")
    private String hostName;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING,
            description = "optional account for the migrated VM. Must be used with domainid")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class,
            description = "import the migrated VM to the specified domain")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID, type = CommandType.UUID, entityType = ProjectResponse.class,
            description = "import the migrated VM for the project")
    private Long projectId;

    @Parameter(name = ApiConstants.TEMPLATE_ID, type = CommandType.UUID, entityType = TemplateResponse.class,
            description = "the template ID for the migrated VM")
    private Long templateId;

    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID, type = CommandType.UUID, entityType = ServiceOfferingResponse.class,
            required = true, description = "the service offering for the migrated VM")
    private Long serviceOfferingId;

    @Parameter(name = ApiConstants.OS_ID, type = CommandType.UUID, entityType = GuestOSResponse.class,
            description = "optional guest OS ID for the migrated VM")
    private Long guestOsId;

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
            description = "optional migration details, including VDDK settings such as vddk.lib.dir, vddk.transports and vddk.thumbprint")
    private Map details;

    @Parameter(name = ApiConstants.NIC_NETWORK_LIST, type = CommandType.MAP,
            description = "VM NIC to network id mapping using keys NIC and network")
    private Map nicNetworkList;

    @Parameter(name = ApiConstants.NIC_IP_ADDRESS_LIST, type = CommandType.MAP,
            description = "VM NIC to ip address mapping using keys NIC, ip4Address")
    private Map nicIpAddressList;

    @Parameter(name = ApiConstants.DATADISK_OFFERING_LIST, type = CommandType.MAP,
            description = "datadisk to disk-offering mapping using keys disk and diskOffering")
    private Map dataDiskToDiskOfferingList;

    @Parameter(name = ApiConstants.FORCED, type = CommandType.BOOLEAN,
            description = "import despite duplicate NIC MAC addresses; CloudStack generates a new MAC address when a duplicate exists")
    private Boolean forced;

    public Long getZoneId() {
        return zoneId;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getHostName() {
        return hostName;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public Long getGuestOsId() {
        return guestOsId;
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

    @SuppressWarnings("unchecked")
    public Map<String, Long> getNicNetworkList() {
        Map<String, Long> nicNetworkMap = new HashMap<>();
        if (MapUtils.isNotEmpty(nicNetworkList)) {
            for (Map<String, String> entry : (Collection<Map<String, String>>) nicNetworkList.values()) {
                String nic = entry.get(VmDetailConstants.NIC);
                String networkUuid = entry.get(VmDetailConstants.NETWORK);
                if (StringUtils.isAnyEmpty(nic, networkUuid) || _entityMgr.findByUuid(Network.class, networkUuid) == null) {
                    throw new InvalidParameterValueException(String.format("Network ID: %s for NIC ID: %s is invalid", networkUuid, nic));
                }
                nicNetworkMap.put(nic, _entityMgr.findByUuid(Network.class, networkUuid).getId());
            }
        }
        return nicNetworkMap;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Network.IpAddresses> getNicIpAddressList() {
        Map<String, Network.IpAddresses> nicIpAddressMap = new HashMap<>();
        if (MapUtils.isNotEmpty(nicIpAddressList)) {
            for (Map<String, String> entry : (Collection<Map<String, String>>) nicIpAddressList.values()) {
                String nic = entry.get(VmDetailConstants.NIC);
                String ipAddress = StringUtils.defaultIfEmpty(entry.get(VmDetailConstants.IP4_ADDRESS), null);
                if (StringUtils.isEmpty(nic)) {
                    throw new InvalidParameterValueException(String.format("NIC ID: '%s' is invalid for IP address mapping", nic));
                }
                if (StringUtils.isEmpty(ipAddress)) {
                    throw new InvalidParameterValueException(String.format("Empty address for NIC ID: %s is invalid", nic));
                }
                if (StringUtils.isNotEmpty(ipAddress) && !ipAddress.equals("auto") && !NetUtils.isValidIp4(ipAddress)) {
                    throw new InvalidParameterValueException(String.format("IP address '%s' for NIC ID: %s is invalid", ipAddress, nic));
                }
                nicIpAddressMap.put(nic, new Network.IpAddresses(ipAddress, null));
            }
        }
        return nicIpAddressMap;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Long> getDataDiskToDiskOfferingList() {
        Map<String, Long> dataDiskToDiskOfferingMap = new HashMap<>();
        if (MapUtils.isNotEmpty(dataDiskToDiskOfferingList)) {
            for (Map<String, String> entry : (Collection<Map<String, String>>) dataDiskToDiskOfferingList.values()) {
                String disk = entry.get(VmDetailConstants.DISK);
                String offeringUuid = entry.get(VmDetailConstants.DISK_OFFERING);
                if (StringUtils.isAnyEmpty(disk, offeringUuid) || _entityMgr.findByUuid(DiskOffering.class, offeringUuid) == null) {
                    throw new InvalidParameterValueException(String.format("Disk offering ID: %s for disk ID: %s is invalid", offeringUuid, disk));
                }
                dataDiskToDiskOfferingMap.put(disk, _entityMgr.findByUuid(DiskOffering.class, offeringUuid).getId());
            }
        }
        return dataDiskToDiskOfferingMap;
    }

    public boolean isForced() {
        return BooleanUtils.isTrue(forced);
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VMWARE_CBT_MIGRATION_START;
    }

    @Override
    public String getEventDescription() {
        return String.format("Starting VMware CBT migration for source VM: %s", sourceVmName);
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
        Long accountId = _accountService.finalizeAccountId(accountName, domainId, projectId, true);
        if (accountId != null) {
            return accountId;
        }
        Account account = CallContext.current().getCallingAccount();
        return account == null ? Account.ACCOUNT_ID_SYSTEM : account.getId();
    }
}
