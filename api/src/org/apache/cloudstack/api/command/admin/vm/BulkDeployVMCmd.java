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

import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.affinity.AffinityGroupResponse;
import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AsyncJobResponse;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.NetworkResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.SecurityGroupResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;

@APICommand(name = "bulkDeployVirtualMachine", description = "Creates and automatically start multiple virtual machines of similar configuration based on virtualmachinecount.", responseObject = AsyncJobResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,
        authorized = { RoleType.Admin }, since = "4.11")
public class BulkDeployVMCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(BulkDeployVMCmd.class.getName());
    private static final String s_name = "bulkdeployvirtualmachineresponse";

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "availability zone for the virtual machine")
    private Long zoneId;

    @ACL
    @Parameter(name = ApiConstants.SERVICE_OFFERING_ID, type = CommandType.UUID, entityType = ServiceOfferingResponse.class, required = true, description = "the ID of the service offering for the virtual machine")
    private Long serviceOfferingId;

    @ACL
    @Parameter(name = ApiConstants.TEMPLATE_ID, type = CommandType.UUID, entityType = TemplateResponse.class, required = true, description = "the ID of the template for the virtual machine")
    private Long templateId;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_COUNT, type = CommandType.INTEGER, required = true, description = "number of virtual machines to be deployed")
    private Integer count;

    @Parameter(name = ApiConstants.POD_ID, type = CommandType.UUID, entityType = PodResponse.class, description = "optional pod ID for placing the virtual machine, mutually exclusive with cluster ID")
    private Long podId;

    @Parameter(name = ApiConstants.CLUSTER_ID, type = CommandType.UUID, entityType = ClusterResponse.class, description = "optional cluster ID for placing the virtual machine, mutually exclusive with pod ID")
    private Long clusterId;

    @Parameter(name = ApiConstants.HYPERVISOR, type = CommandType.STRING, description = "hypervisor type on which to deploy the virtual machine. "
            + "The parameter is required and respected only when hypervisor info is not set on the ISO/Template passed to the call")
    private String hypervisor;

    @Parameter(name = ApiConstants.USER_DATA, type = CommandType.STRING, description = "an optional binary data that can be sent to the virtual machine upon a successful deployment. "
            + "This binary data must be base64 encoded before adding it to the request. Using HTTP GET (via querystring), you can send up to 2KB of data after base64 encoding. "
            + "Using HTTP POST (via POST body), you can send up to 32K of data after base64 encoding.", length = 32768)
    private String userData;

    @Parameter(name = ApiConstants.SSH_KEYPAIR, type = CommandType.STRING, description = "name of the ssh key pair used to login to the virtual machine")
    private String sshKeyPairName;

    @Parameter(name = ApiConstants.NETWORK_IDS, type = CommandType.LIST, collectionType = CommandType.UUID, entityType = NetworkResponse.class,
            description = "comma separated list of network IDs used by the virtual machine")
    private List<Long> networkIds;

    @ACL
    @Parameter(name = ApiConstants.DISK_OFFERING_ID, type = CommandType.UUID, entityType = DiskOfferingResponse.class,
            description = "the ID of the disk offering for the virtual machine. If the template is of ISO format, the diskOfferingId is for the root disk volume. "
            + "Otherwise this parameter is used to indicate the offering for the data disk volume.")
    private Long diskOfferingId;

    @ACL
    @Parameter(name = ApiConstants.SECURITY_GROUP_IDS, type = CommandType.LIST, collectionType = CommandType.UUID, entityType = SecurityGroupResponse.class,
            description = "comma separated list of security group IDs that are going to be applied to the virtual machine. "
            + "Should be passed only when VM is created from a zone with Basic Network support. Mutually exclusive with securitygroupnames parameter")
    private List<Long> securityGroupIds;

    @ACL
    @Parameter(name = ApiConstants.SECURITY_GROUP_NAMES, type = CommandType.LIST, collectionType = CommandType.STRING, entityType = SecurityGroupResponse.class,
            description = "comma separated list of security group names that are going to be applied to the virtual machine. "
            + "Should be passed only when VM is created from a zone with Basic Network support. Mutually exclusive with securitygroupids parameter")
    private List<String> securityGroupNames;

    @ACL
    @Parameter(name = ApiConstants.AFFINITY_GROUP_IDS, type = CommandType.LIST, collectionType = CommandType.UUID, entityType = AffinityGroupResponse.class,
            description = "comma separated list of affinity groups IDs that are going to be applied to the virtual machine. "
            + "Mutually exclusive with affinitygroupnames parameter")
    private List<Long> affinityGroupIds;

    @ACL
    @Parameter(name = ApiConstants.AFFINITY_GROUP_NAMES, type = CommandType.LIST, collectionType = CommandType.STRING, entityType = AffinityGroupResponse.class,
            description = "comma separated list of affinity groups names that are going to be applied to the virtual machine. "
            + "Mutually exclusive with affinitygroupids parameter")
    private List<String> affinityGroupNames;

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "an optional account for the virtual machine. Must be used with domainId.")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "an optional domainId for the virtual machine. If the account parameter is used, domainId must also be used.")
    private Long domainId;

    public Long getZoneId() {
        return zoneId;
    }

    public Long getPodId() {
        return podId;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public Long getServiceOfferingId() {
        return serviceOfferingId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public List<Long> getNetworkIds() {
        return networkIds;
    }

    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    public List<Long> getSecurityGroupIds() {
        return securityGroupIds;
    }

    public List<String> getSecurityGroupNames() {
        return securityGroupNames;
    }

    public String getUserData() {
        return userData;
    }

    public String getSshKeyPairName() {
        return sshKeyPairName;
    }

    public List<Long> getAffinityGroupIds() {
        return affinityGroupIds;
    }

    public List<String> getAffinityGroupNames() {
        return affinityGroupNames;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Integer getCount() {
        return count;
    }

    private void verifyInputs() {
        if (count == null || count <= 0) {
            throw new InvalidParameterValueException("Invalid value for 'virtualmachinecount' parameter, it should be a positive number");
        }
        if (podId != null && clusterId != null) {
            throw new InvalidParameterValueException("'podid' parameter is mutually exclusive with 'clusterid' parameter");
        }
        if (securityGroupIds != null && securityGroupNames != null) {
            throw new InvalidParameterValueException("'securitygroupids' parameter is mutually exclusive with 'securitygroupnames' parameter");
        }
        if (affinityGroupIds != null && affinityGroupNames != null) {
            throw new InvalidParameterValueException("'affinitygroupids' parameter is mutually exclusive with 'affinitygroupnames' parameter");
        }

        DataCenter zone = _entityMgr.findById(DataCenter.class, zoneId);
        ServiceOffering serviceOffering = _entityMgr.findById(ServiceOffering.class, serviceOfferingId);
        if (!zone.isLocalStorageEnabled() && serviceOffering.getUseLocalStorage()) {
            throw new InvalidParameterValueException("Zone " + zone.getName() + " is not configured to use local storage but service offering " + serviceOffering.getName() + " uses it");
        }
        if (serviceOffering.isDynamic()) {
            throw new InvalidParameterValueException("Service offering " + serviceOffering.getName() + " is a custom one and not supported for bulk VM deployments");
        }
        if (diskOfferingId != null) {
            DiskOffering diskOffering = _entityMgr.findById(DiskOffering.class, diskOfferingId);
            if (!zone.isLocalStorageEnabled() && diskOffering.getUseLocalStorage()) {
                throw new InvalidParameterValueException("Zone " + zone.getName() + " is not configured to use local storage but disk offering " + diskOffering.getName() + " uses it");
            }
            if (diskOffering.isCustomized()) {
                throw new InvalidParameterValueException("Disk offering " + diskOffering.getName() + " is a custom one and not supported for bulk VM deployments");
            }
        }
        if (zone.getNetworkType() == NetworkType.Basic) {
            if (CollectionUtils.isNotEmpty(getNetworkIds())) {
                throw new InvalidParameterValueException("Can't specify 'networkids' parameter in basic zone " + zone.getName());
            }
        } else {
            if (!zone.isSecurityGroupEnabled()) {
                if (CollectionUtils.isNotEmpty(getSecurityGroupIds()) || CollectionUtils.isNotEmpty(getSecurityGroupNames())) {
                    throw new InvalidParameterValueException("Can't create VM with security groups; security group feature is not enabled for zone " + zone.getName());
                }
            }
        }
    }

    @Override
    public void execute() throws ResourceAllocationException {
        verifyInputs();
        List<Long> jobIds = _userVmService.bulkDeployVirtualMachine(this);
        List<AsyncJobResponse> jobs = _responseGenerator.createAsyncJobResponse(jobIds);
        ListResponse<AsyncJobResponse> response = new ListResponse<AsyncJobResponse>();
        response.setResponses(jobs, jobs.size());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalyzeAccountId(accountName, domainId, null, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }

        return accountId;
    }


}
