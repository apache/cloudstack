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

import java.util.List;

import com.cloud.offering.DiskOffering.State;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.offering.DomainAndZoneIdResolver;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.offering.DiskOffering;
import com.cloud.user.Account;

@APICommand(name = "updateDiskOffering", description = "Updates a disk offering.", responseObject = DiskOfferingResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateDiskOfferingCmd extends BaseCmd implements DomainAndZoneIdResolver {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.DISPLAY_TEXT,
            type = CommandType.STRING,
            description = "Updates alternate display text of the disk offering with this value",
            length = 4096)
    private String displayText;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DiskOfferingResponse.class, required = true, description = "ID of the disk offering")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "Updates name of the disk offering with this value")
    private String diskOfferingName;

    @Parameter(name = ApiConstants.SORT_KEY, type = CommandType.INTEGER, description = "Sort key of the disk offering, integer")
    private Integer sortKey;

    @Parameter(name = ApiConstants.DISPLAY_OFFERING,
            type = CommandType.BOOLEAN,
            description = "An optional field, whether to display the offering to the end user or not.")
    private Boolean displayOffering;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.STRING,
            description = "The ID of the containing domain(s) as comma separated string, public for public offerings",
            since = "4.13",
            length = 4096)
    private String domainIds;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.STRING,
            description = "The ID of the containing zone(s) as comma separated string, all for all zones offerings",
            length = 4096,
            since = "4.13")
    private String zoneIds;

    @Parameter(name = ApiConstants.TAGS,
            type = CommandType.STRING,
            description = "Comma-separated list of tags for the disk offering, tags should match with existing storage pool tags",
            since = "4.15")
    private String tags;

    @Parameter(name = ApiConstants.BYTES_READ_RATE, type = CommandType.LONG, description = "Bytes read rate of the disk offering", since = "4.15")
    private Long bytesReadRate;

    @Parameter(name = ApiConstants.BYTES_READ_RATE_MAX, type = CommandType.LONG, description = "Burst bytes read rate of the disk offering", since = "4.15")
    private Long bytesReadRateMax;

    @Parameter(name = ApiConstants.BYTES_READ_RATE_MAX_LENGTH, type = CommandType.LONG, description = "Length (in seconds) of the burst", since = "4.15")
    private Long bytesReadRateMaxLength;

    @Parameter(name = ApiConstants.BYTES_WRITE_RATE, type = CommandType.LONG, description = "Bytes write rate of the disk offering", since = "4.15")
    private Long bytesWriteRate;

    @Parameter(name = ApiConstants.BYTES_WRITE_RATE_MAX, type = CommandType.LONG, description = "Burst bytes write rate of the disk offering", since = "4.15")
    private Long bytesWriteRateMax;

    @Parameter(name = ApiConstants.BYTES_WRITE_RATE_MAX_LENGTH,  type = CommandType.LONG, description = "Length (in seconds) of the burst", since = "4.15")
    private Long bytesWriteRateMaxLength;

    @Parameter(name = ApiConstants.IOPS_READ_RATE, type = CommandType.LONG, description = "I/O requests read rate of the disk offering", since = "4.15")
    private Long iopsReadRate;

    @Parameter(name = ApiConstants.IOPS_READ_RATE_MAX, type = CommandType.LONG, description = "Burst requests read rate of the disk offering", since = "4.15")
    private Long iopsReadRateMax;

    @Parameter(name = ApiConstants.IOPS_READ_RATE_MAX_LENGTH, type = CommandType.LONG, description = "Length (in seconds) of the burst", since = "4.15")
    private Long iopsReadRateMaxLength;

    @Parameter(name = ApiConstants.IOPS_WRITE_RATE, type = CommandType.LONG, description = "I/O requests write rate of the disk offering", since = "4.15")
    private Long iopsWriteRate;

    @Parameter(name = ApiConstants.IOPS_WRITE_RATE_MAX, type = CommandType.LONG, description = "Burst io requests write rate of the disk offering", since = "4.15")
    private Long iopsWriteRateMax;

    @Parameter(name = ApiConstants.IOPS_WRITE_RATE_MAX_LENGTH, type = CommandType.LONG, description = "Length (in seconds) of the burst", since = "4.15")
    private Long iopsWriteRateMaxLength;

    @Parameter(name = ApiConstants.CACHE_MODE, type = CommandType.STRING, description = "The cache mode to use for this disk offering", since = "4.15")
    private String cacheMode;

    @Parameter(name = ApiConstants.STATE, type = CommandType.STRING, description = "state of the disk offering")
    private String diskOfferingState;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getDisplayText() {
        return displayText;
    }

    public Long getId() {
        return id;
    }

    public String getDiskOfferingName() {
        return diskOfferingName;
    }

    public Integer getSortKey() {
        return sortKey;
    }

    public Boolean getDisplayOffering() {
        return displayOffering;
    }

    public List<Long> getDomainIds() {
        return resolveDomainIds(domainIds, id, _configService::getDiskOfferingDomains, "disk offering");
    }

    public List<Long> getZoneIds() {
        return resolveZoneIds(zoneIds, id, _configService::getDiskOfferingZones, "disk offering");
    }

    public String getTags() {
        return tags;
    }

    public String getCacheMode() {
        return cacheMode;
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
    public State getState() {
        State state = EnumUtils.getEnumIgnoreCase(State.class, diskOfferingState);
        if (StringUtils.isNotBlank(diskOfferingState) && state == null) {
            throw new InvalidParameterValueException("Invalid state value: " + diskOfferingState);
        }
        return state;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public Long getApiResourceId() {
        return id;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.DiskOffering;
    }

    @Override
    public void execute() {
        DiskOffering result = _configService.updateDiskOffering(this);
        if (result != null) {
            DiskOfferingResponse response = _responseGenerator.createDiskOfferingResponse(result);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update disk offering");
        }
    }
}
