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
package org.apache.cloudstack.api.command.user.guest;

import java.util.ArrayList;
import java.util.List;


import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.GuestOSCategoryResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.cpu.CPU;
import com.cloud.storage.GuestOsCategory;
import com.cloud.utils.Pair;

@APICommand(name = "listOsCategories", description = "Lists all supported OS categories for this cloud.", responseObject = GuestOSCategoryResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListGuestOsCategoriesCmd extends BaseListCmd {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = GuestOSCategoryResponse.class, description = "List OS category by id")
    private Long id;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "List OS category by name", since = "3.0.1")
    private String name;

    @Parameter(name = ApiConstants.IS_FEATURED,
            type = CommandType.BOOLEAN,
            description = "List available OS categories by featured or not",
            since = "4.21.0")
    private Boolean featured;

    @Parameter(name = ApiConstants.IS_ISO,
            type = CommandType.BOOLEAN,
            description = "List OS categories for which an ISO is available",
            since = "4.21.0")
    private Boolean iso;

    @Parameter(name = ApiConstants.IS_VNF, type = CommandType.BOOLEAN,
            description = "List OS categories for which a VNF template is available",
            since = "4.21.0")
    private Boolean vnf;

    @Parameter(name = ApiConstants.ZONE_ID,
            type = CommandType.UUID,
            entityType = ZoneResponse.class,
            description = "List available OS categories types for the zone",
            since = "4.21.0")
    private Long zoneId;

    @Parameter(name = ApiConstants.ARCH,
            type = CommandType.STRING,
            description = "List OS categories types available for given CPU architecture",
            since = "4.21.0")
    private String arch;

    @Parameter(name = ApiConstants.SHOW_RESOURCE_ICON,
            type = CommandType.BOOLEAN,
            description = "flag to display the resource image for the OS category",
            since = "4.21.0")
    private Boolean showIcon;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Boolean isFeatured() {
        return featured;
    }

    public Boolean isIso() {
        return iso;
    }

    public Boolean isVnf() {
        return vnf;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public CPU.CPUArch getArch() {
        return arch == null ? null : CPU.CPUArch.fromType(arch);
    }

    public boolean isShowIcon() {
        return Boolean.TRUE.equals(showIcon);
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        Pair<List<? extends GuestOsCategory>, Integer> result = _mgr.listGuestOSCategoriesByCriteria(this);
        ListResponse<GuestOSCategoryResponse> response = new ListResponse<>();
        List<GuestOSCategoryResponse> osCatResponses = new ArrayList<>();
        for (GuestOsCategory osCategory : result.first()) {
            GuestOSCategoryResponse categoryResponse = _responseGenerator.createGuestOSCategoryResponse(osCategory,
                    isShowIcon());
            osCatResponses.add(categoryResponse);
        }

        response.setResponses(osCatResponses, result.second());
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
