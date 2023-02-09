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
package org.apache.cloudstack.api.command.admin.offering;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ServiceOfferingResponse;
import org.apache.cloudstack.api.response.VsphereStoragePoliciesResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.Logger;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.Storage;
import com.cloud.user.Account;

@APICommand(name = "createServiceOffering", description = "Creates a service offering.", responseObject = ServiceOfferingResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateServiceOfferingCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateServiceOfferingCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.CPU_NUMBER, type = CommandType.INTEGER, required = false, description = "the CPU number of the service offering")
    private Integer cpuNumber;

    @Parameter(name = ApiConstants.CPU_SPEED, type = CommandType.INTEGER, required = false, description = "the CPU speed of the service offering in MHz.")
    private Integer cpuSpeed;

    @Parameter(name = ApiConstants.DISPLAY_TEXT, type = CommandType.STRING, description = "The display text of the service offering, defaults to 'name'.")
    private String displayText;

    @Parameter(name = ApiConstants.PROVISIONINGTYPE, type = CommandType.STRING, description = "provisioning type used to create volumes. Valid values are thin, sparse, fat.")
    private String provisioningType = Storage.ProvisioningType.THIN.toString();

    @Parameter(name = ApiConstants.MEMORY, type = CommandType.INTEGER, required = false, description = "the total memory of the service offering in MB")
    private Integer memory;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "the name of the service offering")
    private String serviceOfferingName;

    @Parameter(name = ApiConstants.OFFER_HA, type = CommandType.BOOLEAN, description = "the HA for the service offering")
    private Boolean offerHa;

    @Parameter(name = ApiConstants.LIMIT_CPU_USE, type = CommandType.BOOLEAN, description = "restrict the CPU usage to committed service offering")
    private Boolean limitCpuUse;

    @Parameter(name = ApiConstants.IS_VOLATILE,
            type = CommandType.BOOLEAN,
            description = "true if the virtual machine needs to be volatile so that on every reboot of VM, original root disk is dettached then destroyed and a fresh root disk is created and attached to VM")
    private Boolean isVolatile;

    @Parameter(name = ApiConstants.STORAGE_TYPE, type = CommandType.STRING, description = "the storage type of the service offering. Values are local and shared.")
    private String storageType;

    @Parameter(name = ApiConstants.TAGS, type = CommandType.STRING, description = "the tags for this service offering.")
    private String tags;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.LIST,
            collectionType = CommandType.UUID,
            entityType = DomainResponse.class,
            description = "the ID of the containing domain(s), null for public offerings")
    private List<Long> domainIds;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.LIST,
            collectionType = CommandType.UUID,
            entityType = ZoneResponse.class,
            description = "the ID of the containing zone(s), null for public offerings",
            since = "4.13")
    private List<Long> zoneIds;

    @Parameter(name = ApiConstants.HOST_TAGS, type = CommandType.STRING, description = "the host tag for this service offering.")
    private String hostTag;

    @Parameter(name = ApiConstants.IS_SYSTEM_OFFERING, type = CommandType.BOOLEAN, description = "is this a system vm offering")
    private Boolean isSystem;

    @Parameter(name = ApiConstants.SYSTEM_VM_TYPE,
            type = CommandType.STRING,
            description = "the system VM type. Possible types are \"domainrouter\", \"consoleproxy\" and \"secondarystoragevm\".")
    private String systemVmType;

    @Parameter(name = ApiConstants.NETWORKRATE,
            type = CommandType.INTEGER,
            description = "data transfer rate in megabits per second allowed. Supported only for non-System offering and system offerings having \"domainrouter\" systemvmtype")
    private Integer networkRate;

    @Parameter(name = ApiConstants.DEPLOYMENT_PLANNER,
            type = CommandType.STRING,
            description = "The deployment planner heuristics used to deploy a VM of this offering. If null, value of global config vm.deployment.planner is used")
    private String deploymentPlanner;

    @Parameter(name = ApiConstants.SERVICE_OFFERING_DETAILS, type = CommandType.MAP, description = "details for planner, used to store specific parameters")
    private Map details;

    @Parameter(name = ApiConstants.ROOT_DISK_SIZE, type = CommandType.LONG, since = "4.15", description = "the Root disk size in GB.")
    private Long rootDiskSize;

    @Parameter(name = ApiConstants.BYTES_READ_RATE, type = CommandType.LONG, required = false, description = "bytes read rate of the disk offering")
    private Long bytesReadRate;

    @Parameter(name = ApiConstants.BYTES_READ_RATE_MAX, type = CommandType.LONG, required = false, description = "burst bytes read rate of the disk offering")
    private Long bytesReadRateMax;

    @Parameter(name = ApiConstants.BYTES_READ_RATE_MAX_LENGTH, type = CommandType.LONG, required = false, description = "length (in seconds) of the burst")
    private Long bytesReadRateMaxLength;

    @Parameter(name = ApiConstants.BYTES_WRITE_RATE, type = CommandType.LONG, required = false, description = "bytes write rate of the disk offering")
    private Long bytesWriteRate;

    @Parameter(name = ApiConstants.BYTES_WRITE_RATE_MAX, type = CommandType.LONG, required = false, description = "burst bytes write rate of the disk offering")
    private Long bytesWriteRateMax;

    @Parameter(name = ApiConstants.BYTES_WRITE_RATE_MAX_LENGTH, type = CommandType.LONG, required = false, description = "length (in seconds) of the burst")
    private Long bytesWriteRateMaxLength;

    @Parameter(name = ApiConstants.IOPS_READ_RATE, type = CommandType.LONG, required = false, description = "io requests read rate of the disk offering")
    private Long iopsReadRate;

    @Parameter(name = ApiConstants.IOPS_READ_RATE_MAX, type = CommandType.LONG, required = false, description = "burst requests read rate of the disk offering")
    private Long iopsReadRateMax;

    @Parameter(name = ApiConstants.IOPS_READ_RATE_MAX_LENGTH, type = CommandType.LONG, required = false, description = "length (in seconds) of the burst")
    private Long iopsReadRateMaxLength;

    @Parameter(name = ApiConstants.IOPS_WRITE_RATE, type = CommandType.LONG, required = false, description = "io requests write rate of the disk offering")
    private Long iopsWriteRate;

    @Parameter(name = ApiConstants.IOPS_WRITE_RATE_MAX, type = CommandType.LONG, required = false, description = "burst io requests write rate of the disk offering")
    private Long iopsWriteRateMax;

    @Parameter(name = ApiConstants.IOPS_WRITE_RATE_MAX_LENGTH, type = CommandType.LONG, required = false, description = "length (in seconds) of the burst")
    private Long iopsWriteRateMaxLength;

    @Parameter(name = ApiConstants.CUSTOMIZED_IOPS, type = CommandType.BOOLEAN, required = false, description = "whether compute offering iops is custom or not", since = "4.4")
    private Boolean customizedIops;

    @Parameter(name = ApiConstants.MIN_IOPS, type = CommandType.LONG, required = false, description = "min iops of the compute offering", since = "4.4")
    private Long minIops;

    @Parameter(name = ApiConstants.MAX_IOPS, type = CommandType.LONG, required = false, description = "max iops of the compute offering", since = "4.4")
    private Long maxIops;

    @Parameter(name = ApiConstants.HYPERVISOR_SNAPSHOT_RESERVE,
            type = CommandType.INTEGER,
            required = false,
            description = "Hypervisor snapshot reserve space as a percent of a volume (for managed storage using Xen or VMware)",
            since = "4.4")
    private Integer hypervisorSnapshotReserve;

    @Parameter(name = ApiConstants.CACHE_MODE,
            type = CommandType.STRING,
            required = false,
            description = "the cache mode to use for this disk offering. none, writeback or writethrough",
            since = "4.14")
    private String cacheMode;

    // Introduce 4 new optional parameters to work custom compute offerings
    @Parameter(name = ApiConstants.CUSTOMIZED,
            type = CommandType.BOOLEAN,
            since = "4.13",
            description = "Whether service offering size is custom or not")
    private Boolean customized;

    @Parameter(name = ApiConstants.MAX_CPU_NUMBER,
            type = CommandType.INTEGER,
            description = "The maximum number of CPUs to be set with Custom Computer Offering",
            since = "4.13")
    private Integer maxCPU;

    @Parameter(name = ApiConstants.MIN_CPU_NUMBER,
            type = CommandType.INTEGER,
            description = "The minimum number of CPUs to be set with Custom Computer Offering",
            since = "4.13")
    private Integer minCPU;

    @Parameter(name = ApiConstants.MAX_MEMORY,
            type = CommandType.INTEGER,
            description = "The maximum memory size of the custom service offering in MB",
            since = "4.13")
    private Integer maxMemory;

    @Parameter(name = ApiConstants.MIN_MEMORY,
            type = CommandType.INTEGER,
            description = "The minimum memory size of the custom service offering in MB",
            since = "4.13")
    private Integer minMemory;

    @Parameter(name = ApiConstants.STORAGE_POLICY, type = CommandType.UUID, entityType = VsphereStoragePoliciesResponse.class,required = false, description = "Name of the storage policy defined at vCenter, this is applicable only for VMware", since = "4.15")
    private Long storagePolicy;

    @Parameter(name = ApiConstants.DYNAMIC_SCALING_ENABLED, type = CommandType.BOOLEAN, since = "4.16",
            description = "true if virtual machine needs to be dynamically scalable of cpu or memory")
    protected Boolean isDynamicScalingEnabled;

    @Parameter(name = ApiConstants.DISK_OFFERING_ID,
            required = false,
            type = CommandType.UUID,
            entityType = DiskOfferingResponse.class,
            description = "the ID of the disk offering to which service offering should be mapped",
            since = "4.17")
    private Long diskOfferingId;

    @Parameter(name = ApiConstants.DISK_OFFERING_STRICTNESS,
            type = CommandType.BOOLEAN,
            description = "True/False to indicate the strictness of the disk offering association with the compute offering. When set to true, override of disk offering is not allowed when VM is deployed and change disk offering is not allowed for the ROOT disk after the VM is deployed",
            since = "4.17")
    private Boolean diskOfferingStrictness;

    @Parameter(name = ApiConstants.ENCRYPT_ROOT, type = CommandType.BOOLEAN, description = "VMs using this offering require root volume encryption", since="4.18")
    private Boolean encryptRoot;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Integer getCpuNumber() {
        return cpuNumber;
    }

    public Integer getCpuSpeed() {
        return cpuSpeed;
    }

    public String getDisplayText() {
        return StringUtils.isEmpty(displayText) ? serviceOfferingName : displayText;
    }

    public String getProvisioningType() {
        return provisioningType;
    }

    public Integer getMemory() {
        return memory;
    }

    public String getServiceOfferingName() {
        if (StringUtils.isEmpty(serviceOfferingName)) {
            throw new InvalidParameterValueException("Failed to create service offering because offering name has not been spified.");
        }
        return serviceOfferingName;
    }

    public Boolean isOfferHa() {
        return offerHa == null ? Boolean.FALSE : offerHa;
    }

    public Boolean isLimitCpuUse() {
        return limitCpuUse == null ? Boolean.FALSE : limitCpuUse;
    }

    public Boolean isVolatileVm() {
        return isVolatile == null ? Boolean.FALSE : isVolatile;
    }

    public String getStorageType() {
        return storageType;
    }

    public String getTags() {
        return tags;
    }

    public List<Long> getDomainIds() {
        if (CollectionUtils.isNotEmpty(domainIds)) {
            Set<Long> set = new LinkedHashSet<>(domainIds);
            domainIds.clear();
            domainIds.addAll(set);
        }
        return domainIds;
    }

    public List<Long> getZoneIds() {
        if (CollectionUtils.isNotEmpty(zoneIds)) {
            Set<Long> set = new LinkedHashSet<>(zoneIds);
            zoneIds.clear();
            zoneIds.addAll(set);
        }
        return zoneIds;
    }

    public String getHostTag() {
        return hostTag;
    }

    public Boolean isSystem() {
        return isSystem == null ? false : isSystem;
    }

    public String getSystemVmType() {
        return systemVmType;
    }

    public Integer getNetworkRate() {
        return networkRate;
    }

    public String getDeploymentPlanner() {
        return deploymentPlanner;
    }

    public Map<String, String> getDetails() {
        Map<String, String> detailsMap = new HashMap<>();
        if (MapUtils.isNotEmpty(details)) {
            Collection<?> props = details.values();
            for (Object prop : props) {
                HashMap<String, String> detail = (HashMap<String, String>) prop;
                // Compatibility with key and value pairs input from API cmd for details map parameter
                if (StringUtils.isNoneEmpty(detail.get("key"), detail.get("value"))) {
                    detailsMap.put(detail.get("key"), detail.get("value"));
                    continue;
                }

                for (Map.Entry<String, String> entry: detail.entrySet()) {
                    detailsMap.put(entry.getKey(),entry.getValue());
                }
            }
        }
        return detailsMap;
    }

    public Long getRootDiskSize() {
        return rootDiskSize;
    }

    public Long getBytesReadRate() {
        return bytesReadRate;
    }

    public Long getBytesReadRateMax() {
        return bytesReadRateMax;
    }

    public Long getBytesReadRateMaxLength() {
        return bytesReadRateMaxLength;
    }

    public Long getBytesWriteRate() {
        return bytesWriteRate;
    }

    public Long getBytesWriteRateMax() {
        return bytesWriteRateMax;
    }

    public Long getBytesWriteRateMaxLength() {
        return bytesWriteRateMaxLength;
    }

    public Long getIopsReadRate() {
        return iopsReadRate;
    }

    public Long getIopsReadRateMax() {
        return iopsReadRateMax;
    }

    public Long getIopsReadRateMaxLength() {
        return iopsReadRateMaxLength;
    }

    public Long getIopsWriteRate() {
        return iopsWriteRate;
    }

    public Long getIopsWriteRateMax() {
        return iopsWriteRateMax;
    }

    public Long getIopsWriteRateMaxLength() {
        return iopsWriteRateMaxLength;
    }

    public Boolean isCustomizedIops() {
        return customizedIops;
    }

    public Long getMinIops() {
        return minIops;
    }

    public Long getMaxIops() {
        return maxIops;
    }

    public Integer getHypervisorSnapshotReserve() {
        return hypervisorSnapshotReserve;
    }

    public String getCacheMode() {
        return cacheMode;
    }

    /**
     * If customized parameter is true, then cpuNumber, memory and cpuSpeed must be null
     * Check if the optional params min/max CPU/Memory have been specified
     * @return true if the following conditions are satisfied;
     * - cpuNumber, memory and cpuSpeed are all null when customized parameter is set to true
     * - min/max CPU/Memory params are all null or all set
     */
    public boolean isCustomized() {
        if (customized != null){
            return customized;
        }
        return (cpuNumber == null || memory == null);
    }

    public Integer getMaxCPUs() {
        return maxCPU;
    }

    public Integer getMinCPUs() {
        return minCPU;
    }

    public Integer getMaxMemory() {
        return maxMemory;
    }

    public Integer getMinMemory() {
        return minMemory;
    }

    public Long getStoragePolicy() {
        return storagePolicy;
    }

    public boolean getDynamicScalingEnabled() {
        return isDynamicScalingEnabled == null ? true : isDynamicScalingEnabled;
    }

    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    public boolean getDiskOfferingStrictness() {
        return diskOfferingStrictness == null ? false : diskOfferingStrictness;
    }

    public boolean getEncryptRoot() {
        if (encryptRoot != null) {
            return encryptRoot;
        }
        return false;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        ServiceOffering result = _configService.createServiceOffering(this);
        if (result != null) {
            ServiceOfferingResponse response = _responseGenerator.createServiceOfferingResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create service offering");
        }
    }
}
