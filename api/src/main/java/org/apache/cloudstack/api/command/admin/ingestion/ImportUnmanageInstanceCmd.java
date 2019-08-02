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

package org.apache.cloudstack.api.command.admin.ingestion;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseAsyncCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.vm.VmImportService;
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
import com.cloud.offering.ServiceOffering;
import com.cloud.org.Cluster;
import com.cloud.user.Account;
import com.cloud.utils.net.NetUtils;
import com.google.common.base.Strings;

@APICommand(name = ImportUnmanageInstanceCmd.API_NAME,
        description = "Import unmanaged virtual machine from a given cluster.",
        responseObject = UserVmResponse.class,
        responseView = ResponseObject.ResponseView.Full,
        requestHasSensitiveInfo = false,
        responseHasSensitiveInfo = true,
        authorized = {RoleType.Admin})
public class ImportUnmanageInstanceCmd extends BaseAsyncCmd {
    public static final Logger s_logger = Logger.getLogger(ImportUnmanageInstanceCmd.class);
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

    @ACL
    @Parameter(name = ApiConstants.TEMPLATE_ID,
            type = CommandType.UUID,
            entityType = TemplateResponse.class,
            required = true,
            description = "the ID of the template for the virtual machine")
    private Long templateId;

    @ACL
    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID,
            type = CommandType.UUID,
            entityType = ServiceOfferingResponse.class,
            required = true,
            description = "the ID of the service offering for the virtual machine")
    private Long serviceOfferingId;

    @ACL
    @Parameter(name = ApiConstants.DISK_OFFERING_ID,
            type = CommandType.UUID,
            entityType = DiskOfferingResponse.class,
            required = true,
            description = "the ID of the root disk offering for the virtual machine")
    private Long diskOfferingId;

    @Parameter(name = ApiConstants.NIC_NETWORK_LIST,
            type = CommandType.MAP,
            description = "VM nic to network id mapping")
    private Map nicNetworkList;

    @Parameter(name = ApiConstants.NIC_IP_ADDRESS_LIST,
            type = CommandType.MAP,
            description = "VM nic to ip address mapping")
    private Map nicIpAddressList;

    @Parameter(name = ApiConstants.DATADISK_OFFERING_LIST,
            type = CommandType.MAP,
            description = "datadisk template to disk-offering mapping")
    private Map dataDiskToDiskOfferingList;

    @Parameter(name = ApiConstants.DETAILS,
            type = CommandType.MAP,
            description = "used to specify the custom parameters.")
    private Map<String, String> details;

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

    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    public Map<String, Long> getNicNetworkList() {
        Map<String, Long> nicNetworkMap = new HashMap<>();
        if (nicNetworkList != null && !nicNetworkList.isEmpty()) {
            Collection parameterCollection = nicNetworkList.values();
            Iterator iter = parameterCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> value = (HashMap<String, String>)iter.next();
                String nic = value.get("nic");
                String networkUuid = value.get("network");
                if (_entityMgr.findByUuid(Network.class, networkUuid) != null) {
                    nicNetworkMap.put(nic, _entityMgr.findByUuid(Network.class, networkUuid).getId());
                } else {
                    throw new InvalidParameterValueException(String.format("Unable to find network ID: %s for NIC ID: %s", networkUuid, nic));
                }
            }
        }
        return nicNetworkMap;
    }

    public Map<String, String> getNicIpAddressList() {
        Map<String, String> nicIpAddressMap = new HashMap<>();
        if (nicIpAddressList != null && !nicIpAddressList.isEmpty()) {
            Collection parameterCollection = nicIpAddressList.values();
            Iterator iter = parameterCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> value = (HashMap<String, String>)iter.next();
                String nic = value.get("nic");
                String ipAddress = value.get("ipAddress");
                if (!Strings.isNullOrEmpty(ipAddress) && NetUtils.isValidIp4(ipAddress)) {
                    nicIpAddressMap.put(nic, ipAddress);
                } else {
                    throw new InvalidParameterValueException(String.format("IP Address: %s for NIC ID: %s is invalid", ipAddress, nic));
                }
            }
        }
        return nicIpAddressMap;
    }

    public Map<String, Long> getDataDiskToDiskOfferingList() {
        Map<String, Long> dataDiskToDiskOfferingMap = new HashMap<>();
        if (dataDiskToDiskOfferingList != null && !dataDiskToDiskOfferingList.isEmpty()) {
            Collection parameterCollection = dataDiskToDiskOfferingList.values();
            Iterator iter = parameterCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> value = (HashMap<String, String>)iter.next();
                String disk = value.get("disk");
                String offeringUuid = value.get("diskOffering");
                if (_entityMgr.findByUuid(DiskOffering.class, offeringUuid) != null) {
                    dataDiskToDiskOfferingMap.put(disk, _entityMgr.findByUuid(DiskOffering.class, offeringUuid).getId());
                } else {
                    throw new InvalidParameterValueException(String.format("Unable to find disk offering ID: %s for data disk ID: %s", offeringUuid, disk));
                }
            }
        }
        return dataDiskToDiskOfferingMap;
    }

    public Map<String, String> getDetails() {
        Map<String, String> customParameterMap = new HashMap<String, String>();
        if (details != null && details.size() != 0) {
            Collection parameterCollection = details.values();
            Iterator iter = parameterCollection.iterator();
            while (iter.hasNext()) {
                HashMap<String, String> value = (HashMap<String, String>)iter.next();
                for (Map.Entry<String,String> entry: value.entrySet()) {
                    customParameterMap.put(entry.getKey(),entry.getValue());
                }
            }
        }
        return customParameterMap;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_VM_IMPORT;
    }

    @Override
    public String getEventDescription() {
        return "Importing unmanaged VM";
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    private void validateInput() {
        if (_entityMgr.findById(Cluster.class, clusterId) == null) {
            throw new InvalidParameterValueException(String.format("Unable to find cluster with ID: %d", clusterId));
        }
        if (_entityMgr.findById(ServiceOffering.class, serviceOfferingId) == null) {
            throw new InvalidParameterValueException(String.format("Unable to find service offering with ID: %d", serviceOfferingId));
        }
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        validateInput();
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
