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
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.VsphereStoragePoliciesResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.offering.DiskOffering;
import com.cloud.offering.ServiceOffering;
import com.cloud.storage.Storage.ProvisioningType;
import com.cloud.user.Account;

@APICommand(name = "createDiskOffering", description = "Creates a disk offering.", responseObject = DiskOfferingResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class CreateDiskOfferingCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(CreateDiskOfferingCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.DISK_SIZE, type = CommandType.LONG, required = false, description = "size of the disk offering in GB (1GB = 1,073,741,824 bytes)")
    private Long diskSize;

    @Parameter(name = ApiConstants.DISPLAY_TEXT, type = CommandType.STRING, description = "An alternate display text of the disk offering, defaults to 'name'.", length = 4096)
    private String displayText;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true, description = "name of the disk offering")
    private String offeringName;

    @Parameter(name = ApiConstants.TAGS, type = CommandType.STRING, description = "tags for the disk offering", length = 4096)
    private String tags;

    @Parameter(name = ApiConstants.CUSTOMIZED, type = CommandType.BOOLEAN, description = "whether disk offering size is custom or not")
    private Boolean customized;

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

    @Parameter(name = ApiConstants.STORAGE_TYPE, type = CommandType.STRING, description = "the storage type of the disk offering. Values are local and shared.")
    private String storageType = ServiceOffering.StorageType.shared.toString();

    @Parameter(name = ApiConstants.PROVISIONINGTYPE,
            type = CommandType.STRING,
            description = "provisioning type used to create volumes. Valid values are thin, sparse, fat.")
    private String provisioningType = ProvisioningType.THIN.toString();

    @Parameter(name = ApiConstants.DISPLAY_OFFERING,
            type = CommandType.BOOLEAN,
            description = "an optional field, whether to display the offering to the end user or not.")
    private Boolean displayOffering;

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

    @Parameter(name = ApiConstants.CUSTOMIZED_IOPS, type = CommandType.BOOLEAN, required = false, description = "whether disk offering iops is custom or not")
    private Boolean customizedIops;

    @Parameter(name = ApiConstants.MIN_IOPS, type = CommandType.LONG, required = false, description = "min iops of the disk offering")
    private Long minIops;

    @Parameter(name = ApiConstants.MAX_IOPS, type = CommandType.LONG, required = false, description = "max iops of the disk offering")
    private Long maxIops;

    @Parameter(name = ApiConstants.HYPERVISOR_SNAPSHOT_RESERVE,
            type = CommandType.INTEGER,
            required = false,
            description = "Hypervisor snapshot reserve space as a percent of a volume (for managed storage using Xen or VMware)")
    private Integer hypervisorSnapshotReserve;

    @Parameter(name = ApiConstants.CACHE_MODE,
            type = CommandType.STRING,
            required = false,
            description = "the cache mode to use for this disk offering. none, writeback or writethrough",
            since = "4.14")
    private String cacheMode;

    @Parameter(name = ApiConstants.STORAGE_POLICY, type = CommandType.UUID, entityType = VsphereStoragePoliciesResponse.class,required = false, description = "Name of the storage policy defined at vCenter, this is applicable only for VMware", since = "4.15")
    private Long storagePolicy;

    @Parameter(name = ApiConstants.DISK_SIZE_STRICTNESS, type = CommandType.BOOLEAN, description = "To allow or disallow the resize operation on the disks created from this disk offering, if the flag is true then resize is not allowed", since = "4.17")
    private Boolean diskSizeStrictness;

    @Parameter(name = ApiConstants.ENCRYPT, type = CommandType.BOOLEAN, required=false, description = "Volumes using this offering should be encrypted", since = "4.18")
    private Boolean encrypt;

    @Parameter(name = ApiConstants.DETAILS, type = CommandType.MAP, description = "details to specify disk offering parameters", since = "4.16")
    private Map details;



    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getDiskSize() {
        return diskSize;
    }

    public String getDisplayText() {
        return StringUtils.isEmpty(displayText) ? offeringName : displayText;
    }

    public String getOfferingName() {
        return offeringName;
    }

    public String getTags() {
        return tags;
    }

    public Boolean isCustomized() {
        return customized;
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

    public boolean getEncrypt() {
        if (encrypt == null) {
            return false;
        }
        return encrypt;
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

    public String getStorageType() {
        return storageType;
    }

    public String getProvisioningType() {
        return provisioningType;
    }

    public Boolean getDisplayOffering() {
        return displayOffering;
    }

    public Integer getHypervisorSnapshotReserve() {
        return hypervisorSnapshotReserve;
    }

    public String getCacheMode() {
        return cacheMode;
    }

    public Map<String, String> getDetails() {
        Map<String, String> detailsMap = new HashMap<>();
        if (MapUtils.isNotEmpty(details)) {
            Collection<?> props = details.values();
            for (Object prop : props) {
                HashMap<String, String> detail = (HashMap<String, String>) prop;
                for (Map.Entry<String, String> entry: detail.entrySet()) {
                    detailsMap.put(entry.getKey(),entry.getValue());
                }
            }
        }
        return detailsMap;
    }

    public Long getStoragePolicy() {
        return storagePolicy;
    }

    public boolean getDiskSizeStrictness() {
        return diskSizeStrictness != null ? diskSizeStrictness : false;
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
        DiskOffering offering = _configService.createDiskOffering(this);
        if (offering != null) {
            DiskOfferingResponse response = _responseGenerator.createDiskOfferingResponse(offering);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create disk offering");
        }
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.DiskOffering;
    }
}
