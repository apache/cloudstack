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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.BaseListCmd;
import com.cloud.api.Implementation;
import com.cloud.api.Parameter;
import com.cloud.api.ResponseObject;
import com.cloud.api.response.DiskOfferingResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.domain.DomainVO;
import com.cloud.storage.DiskOfferingVO;

@Implementation(method="searchForDiskOfferings")
public class ListDiskOfferingsCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListDiskOfferingsCmd.class.getName());

    private static final String s_name = "listdiskofferingsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="domainid", type=CommandType.LONG)
    private Long domainId;

    @Parameter(name="id", type=CommandType.LONG)
    private Long id;

    @Parameter(name="name", type=CommandType.STRING)
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
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public ResponseObject getResponse() {
        List<DiskOfferingVO> offerings = (List<DiskOfferingVO>)getResponseObject();

        ListResponse response = new ListResponse();
        List<DiskOfferingResponse> diskOfferingResponses = new ArrayList<DiskOfferingResponse>();
        for (DiskOfferingVO offering : offerings) {
            DiskOfferingResponse diskOffResp = new DiskOfferingResponse();
            diskOffResp.setCreated(offering.getCreated());
            diskOffResp.setDiskSize(offering.getDiskSize());
            diskOffResp.setDisplayText(offering.getDisplayText());
            diskOffResp.setDomainId(offering.getDomainId());
            diskOffResp.setId(offering.getId());
            diskOffResp.setName(offering.getName());
            diskOffResp.setTags(offering.getTags());
            DomainVO domain = ApiDBUtils.findDomainById(offering.getDomainId());
            diskOffResp.setDomain(domain.getName());

            diskOffResp.setResponseName("diskoffering");
            diskOfferingResponses.add(diskOffResp);
        }

        response.setResponses(diskOfferingResponses);
        response.setResponseName(getName());
        return response;
    }
}
