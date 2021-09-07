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
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.VmImportService;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.log4j.Logger;

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
import com.google.common.base.Strings;

@APICommand(name = ImportUnmanagedInstanceCmd.API_NAME,
        description = "Import unmanaged virtual machine from a given cluster.",
        responseObject = UserVmResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin},
        since = "4.14.0")
public class ImportUnmanagedInstanceCmd extends BaseAsyncCmd {
    public static final Logger LOGGER = Logger.getLogger(ImportUnmanagedInstanceCmd.class);
    public static final String API_NAME = "importUnmanagedInstance";

    @Inject
    public VmImportService vmImportService;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.CLUSTER_ID,
            type = CommandType.UUID,
            entityType = ClusterResponse.class,
            required = true,
            description = "the cluster ID")
    private Long clusterId;

    @Parameter(name = ApiConstants.NAME,
            type = CommandType.STRING,
            required = true,
            description = "the hypervisor name of the instance")
    private String name;

    @Parameter(name = ApiConstants.DISPLAY_NAME,
            type = CommandType.STRING,
            description = "the display name of the instance")
    private String displayName;

    @Parameter(name = ApiConstants.HOST_NAME,
            type = CommandType.STRING,
            description = "the host name of the instance")
    private String hostName;

    @Parameter(name = ApiConstants.ACCOUNT,
            type = CommandType.STRING,
            description = "an optional account for the virtual machine. Must be used with domainId.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.UUID,
            entityType = DomainResponse.class,
            description = "import instance to the domain specified")
    private Long domainId;

    @Parameter(name = ApiConstants.PROJECT_ID,
            type = CommandType.UUID,
            entityType = ProjectResponse.class,
            description = "import instance for the project")
    private Long projectId;

    @Parameter(name = ApiConstants.TEMPLATE_ID,
            type = CommandType.UUID,
            entityType = TemplateResponse.class,
            description = "the ID of the template for the virtual machine")
    private Long templateId;

    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID,
            type = CommandType.UUID,
            entityType = ServiceOfferingResponse.class,
            required = true,
            description = "the ID of the service offering for the virtual machine")
    private Long serviceOfferingId;

    @Parameter(name = ApiConstants.NIC_NETWORK_LIST,
            type = CommandType.MAP,
            description = "VM nic to network id mapping using keys nic and network")
    private Map nicNetworkList;

    @Parameter(name = ApiConstants.NIC_IP_ADDRESS_LIST,
            type = CommandType.MAP,
            description = "VM nic to ip address mapping using keys nic, ip4Address")
    private Map nicIpAddressList;

    @Parameter(name = ApiConstants.DATADISK_OFFERING_LIST,
            type = CommandType.MAP,
            description = "datadisk template to disk-offering mapping using keys disk and diskOffering")
    private Map dataDiskToDiskOfferingList;

    @Parameter(name = ApiConstants.DETAILS,
            type = CommandType.MAP,
            description = "used to specify the custom parameters.")
    private Map<String, String> details;

    @Parameter(name = ApiConstants.MIGRATE_ALLOWED,
            type = CommandType.BOOLEAN,
            description = "vm and its volumes are allowed to migrate to different host/pool when offerings passed are incompatible with current host/pool")
    private Boolean migrateAllowed;

    @Parameter(name = ApiConstants.FORCED,
            type = CommandType.BOOLEAN,
            description = "VM is imported despite some of its NIC's MAC addresses are already present")
    private Boolean forced;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getClusterId() {
        return clusterId;
    }

    public String getName() {
        return name;
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

    public Long getTemplateId() {
        return templateId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public Map<String, Long> getNicNetworkList() {
        Map<String, Long> nicNetworkMap = new HashMap<>();
        if (MapUtils.isNotEmpty(nicNetworkList)) {
            for (Map<String, String> entry : (Collection<Map<String, String>>)nicNetworkList.values()) {
                String nic = entry.get(VmDetailConstants.NIC);
                String networkUuid = entry.get(VmDetailConstants.NETWORK);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("nic, '%s', goes on net, '%s'", nic, networkUuid));
                }
                if (Strings.isNullOrEmpty(nic) || Strings.isNullOrEmpty(networkUuid) || _entityMgr.findByUuid(Network.class, networkUuid) == null) {
                    throw new InvalidParameterValueException(String.format("Network ID: %s for NIC ID: %s is invalid", networkUuid, nic));
                }
                nicNetworkMap.put(nic, _entityMgr.findByUuid(Network.class, networkUuid).getId());
            }
        }
        return nicNetworkMap;
    }

    public Map<String, Network.IpAddresses> getNicIpAddressList() {
        Map<String, Network.IpAddresses> nicIpAddressMap = new HashMap<>();
        if (MapUtils.isNotEmpty(nicIpAddressList)) {
            for (Map<String, String> entry : (Collection<Map<String, String>>)nicIpAddressList.values()) {
                String nic = entry.get(VmDetailConstants.NIC);
                String ipAddress = Strings.emptyToNull(entry.get(VmDetailConstants.IP4_ADDRESS));
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("nic, '%s', gets ip, '%s'", nic, ipAddress));
                }
                if (Strings.isNullOrEmpty(nic)) {
                    throw new InvalidParameterValueException(String.format("NIC ID: '%s' is invalid for IP address mapping", nic));
                }
                if (Strings.isNullOrEmpty(ipAddress)) {
                    throw new InvalidParameterValueException(String.format("Empty address for NIC ID: %s is invalid", nic));
                }
                if (!Strings.isNullOrEmpty(ipAddress) && !ipAddress.equals("auto") && !NetUtils.isValidIp4(ipAddress)) {
                    throw new InvalidParameterValueException(String.format("IP address '%s' for NIC ID: %s is invalid", ipAddress, nic));
                }
                Network.IpAddresses ipAddresses = new Network.IpAddresses(ipAddress, null);
                nicIpAddressMap.put(nic, ipAddresses);
            }
        }
        return nicIpAddressMap;
    }

    public Map<String, Long> getDataDiskToDiskOfferingList() {
        Map<String, Long> dataDiskToDiskOfferingMap = new HashMap<>();
        if (MapUtils.isNotEmpty(dataDiskToDiskOfferingList)) {
            for (Map<String, String> entry : (Collection<Map<String, String>>)dataDiskToDiskOfferingList.values()) {
                String disk = entry.get(VmDetailConstants.DISK);
                String offeringUuid = entry.get(VmDetailConstants.DISK_OFFERING);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace(String.format("disk, '%s', gets offering, '%s'", disk, offeringUuid));
                }
                if (Strings.isNullOrEmpty(disk) || Strings.isNullOrEmpty(offeringUuid) || _entityMgr.findByUuid(DiskOffering.class, offeringUuid) == null) {
                    throw new InvalidParameterValueException(String.format("Disk offering ID: %s for disk ID: %s is invalid", offeringUuid, disk));
                }
                dataDiskToDiskOfferingMap.put(disk, _entityMgr.findByUuid(DiskOffering.class, offeringUuid).getId());
            }
        }
        return dataDiskToDiskOfferingMap;
    }

    public Map<String, String> getDetails() {
        if (MapUtils.isEmpty(details)) {
            return new HashMap<String, String>();
        }

        Collection<String> paramsCollection = details.values();
        Map<String, String> params = (Map<String, String>) (paramsCollection.toArray())[0];
        return params;
    }

    public Boolean getMigrateAllowed() {
        return migrateAllowed == Boolean.TRUE;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_IMPORT;
    }

    @Override
    public String getEventDescription() {
        return "Importing unmanaged VM";
    }

    public boolean isForced() {
        return BooleanUtils.isTrue(forced);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        UserVmResponse response = vmImportService.importUnmanagedInstance(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return API_NAME.toLowerCase() + BaseAsyncCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            Account account = CallContext.current().getCallingAccount();
            if (account != null) {
                accountId = account.getId();
            } else {
                accountId = Account.ACCOUNT_ID_SYSTEM;
            }
        }
        return accountId;
    }
}
