/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.api.commands;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.api.ApiConstants;
import com.cloud.api.BaseListCmd;
import com.cloud.api.IdentityMapper;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.response.DiskOfferingResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.offering.DiskOffering;

@Implementation(description="Lists all available disk offerings.", responseObject=DiskOfferingResponse.class)
public class ListDiskOfferingsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListDiskOfferingsCmd.class.getName());

    private static final String s_name = "listdiskofferingsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the ID of the domain of the disk offering.")
    private Long domainId;

    @IdentityMapper(entityTableName="disk_offering")
    @Parameter(name=ApiConstants.ID, type=CommandType.LONG, description="ID of the disk offering")
    private Long id;

    @Parameter(name=ApiConstants.NAME, type=CommandType.STRING, description="name of the disk offering")
    private String diskOfferingName;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getDomainId() {
        return domainId;
    }

    public Long getId() {
        return id;
    }

    public String getDiskOfferingName() {
        return diskOfferingName;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute(){
        List<? extends DiskOffering> result = _mgr.searchForDiskOfferings(this);
        ListResponse<DiskOfferingResponse> response = new ListResponse<DiskOfferingResponse>();
        List<DiskOfferingResponse> diskOfferingResponses = new ArrayList<DiskOfferingResponse>();
        for (DiskOffering offering : result) {
            DiskOfferingResponse diskOffResp = _responseGenerator.createDiskOfferingResponse(offering);
            diskOffResp.setObjectName("diskoffering");
            diskOfferingResponses.add(diskOffResp);
        }

        response.setResponses(diskOfferingResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
