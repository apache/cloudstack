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

import java.util.ArrayList;
import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter;
import com.cloud.domain.Domain;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.offering.DiskOffering;
import com.cloud.user.Account;
import com.google.common.base.Strings;

@APICommand(name = "updateDiskOffering", description = "Updates a disk offering.", responseObject = DiskOfferingResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class UpdateDiskOfferingCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateDiskOfferingCmd.class.getName());
    private static final String s_name = "updatediskofferingresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.DISPLAY_TEXT,
            type = CommandType.STRING,
            description = "updates alternate display text of the disk offering with this value",
            length = 4096)
    private String displayText;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = DiskOfferingResponse.class, required = true, description = "ID of the disk offering")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "updates name of the disk offering with this value")
    private String diskOfferingName;

    @Parameter(name = ApiConstants.SORT_KEY, type = CommandType.INTEGER, description = "sort key of the disk offering, integer")
    private Integer sortKey;

    @Parameter(name = ApiConstants.DISPLAY_OFFERING,
            type = CommandType.BOOLEAN,
            description = "an optional field, whether to display the offering to the end user or not.")
    private Boolean displayOffering;

    @Parameter(name = ApiConstants.DOMAIN_ID,
            type = CommandType.STRING,
            description = "the ID of the containing domain(s) as comma separated string, public for public offerings",
            since = "4.13")
    private String domainIds;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.STRING,
            description = "the ID of the containing zone(s) as comma separated string, all for all zones offerings",
            since = "4.13")
    private String zoneIds;

    @Parameter(name = ApiConstants.TAGS,
            type = CommandType.STRING,
            description = "comma-separated list of tags for the disk offering, tags should match with existing storage pool tags",
            authorized = {RoleType.Admin},
            since = "4.15")
    private String tags;


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
        List<Long> validDomainIds = new ArrayList<>();
        if (!Strings.isNullOrEmpty(domainIds)) {
            if (domainIds.contains(",")) {
                String[] domains = domainIds.split(",");
                for (String domain : domains) {
                    Domain validDomain = _entityMgr.findByUuid(Domain.class, domain.trim());
                    if (validDomain != null) {
                        validDomainIds.add(validDomain.getId());
                    } else {
                        throw new InvalidParameterValueException("Failed to create disk offering because invalid domain has been specified.");
                    }
                }
            } else {
                domainIds = domainIds.trim();
                if (!domainIds.matches("public")) {
                    Domain validDomain = _entityMgr.findByUuid(Domain.class, domainIds.trim());
                    if (validDomain != null) {
                        validDomainIds.add(validDomain.getId());
                    } else {
                        throw new InvalidParameterValueException("Failed to create disk offering because invalid domain has been specified.");
                    }
                }
            }
        } else {
            validDomainIds.addAll(_configService.getDiskOfferingDomains(id));
        }
        return validDomainIds;
    }

    public List<Long> getZoneIds() {
        List<Long> validZoneIds = new ArrayList<>();
        if (!Strings.isNullOrEmpty(zoneIds)) {
            if (zoneIds.contains(",")) {
                String[] zones = zoneIds.split(",");
                for (String zone : zones) {
                    DataCenter validZone = _entityMgr.findByUuid(DataCenter.class, zone.trim());
                    if (validZone != null) {
                        validZoneIds.add(validZone.getId());
                    } else {
                        throw new InvalidParameterValueException("Failed to create disk offering because invalid zone has been specified.");
                    }
                }
            } else {
                zoneIds = zoneIds.trim();
                if (!zoneIds.matches("all")) {
                    DataCenter validZone = _entityMgr.findByUuid(DataCenter.class, zoneIds.trim());
                    if (validZone != null) {
                        validZoneIds.add(validZone.getId());
                    } else {
                        throw new InvalidParameterValueException("Failed to create disk offering because invalid zone has been specified.");
                    }
                }
            }
        } else {
            validZoneIds.addAll(_configService.getDiskOfferingZones(id));
        }
        return validZoneIds;
    }

    public String getTags() {
        return tags;
    }

/////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
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
