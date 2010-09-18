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
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.api.response.DomainResponse;
import com.cloud.api.response.ListResponse;
import com.cloud.domain.DomainVO;

@Implementation(method="searchForDomainChildren")
public class ListDomainChildrenCmd extends BaseListCmd {
	public static final Logger s_logger = Logger.getLogger(ListDomainChildrenCmd.class.getName());

    private static final String s_name = "listdomainchildrenresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name="id", type=CommandType.LONG)
    private Long id;

    @Parameter(name="isrecursive", type=CommandType.BOOLEAN)
    private Boolean recursive;

    @Parameter(name="name", type=CommandType.STRING)
    private String domainName;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public Boolean isRecursive() {
        return recursive;
    }

    public String getDomainName() {
        return domainName;
    }


    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getName() {
        return s_name;
    }

    @Override @SuppressWarnings("unchecked")
    public String getResponse() {
        List<DomainVO> domains = (List<DomainVO>)getResponseObject();

        ListResponse response = new ListResponse();
        List<DomainResponse> domainResponses = new ArrayList<DomainResponse>();
        for (DomainVO domain : domains) {
            DomainResponse domainResponse = new DomainResponse();
            domainResponse.setDomainName(domain.getName());
            domainResponse.setId(domain.getId());
            domainResponse.setLevel(domain.getLevel());
            domainResponse.setParentDomainId(domain.getParent());
            if (domain.getParent() != null) {
                domainResponse.setParentDomainName(ApiDBUtils.findDomainById(domain.getParent()).getName());
            }

            domainResponse.setResponseName("domain");
            domainResponses.add(domainResponse);
        }

        response.setResponses(domainResponses);
        response.setResponseName(getName());
        return ApiResponseSerializer.toSerializedString(response);
    }
}
