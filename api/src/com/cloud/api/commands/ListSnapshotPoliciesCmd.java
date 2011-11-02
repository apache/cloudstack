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
import com.cloud.api.response.ListResponse;
import com.cloud.api.response.SnapshotPolicyResponse;
import com.cloud.storage.snapshot.SnapshotPolicy;

@Implementation(description="Lists snapshot policies.", responseObject=SnapshotPolicyResponse.class)
public class ListSnapshotPoliciesCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListSnapshotPoliciesCmd.class.getName());

    private static final String s_name = "listsnapshotpoliciesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.STRING, description="lists snapshot policies for the specified account. Must be used with domainid parameter.")
    private String accountName;

    @IdentityMapper(entityTableName="domain")
    @Parameter(name=ApiConstants.DOMAIN_ID, type=CommandType.LONG, description="the domain ID. If used with the account parameter, lists snapshot policies for the specified account in this domain.")
    private Long domainId;

    @IdentityMapper(entityTableName="volumes")
    @Parameter(name=ApiConstants.VOLUME_ID, type=CommandType.LONG, required=true, description="the ID of the disk volume")
    private Long volumeId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public Long getVolumeId() {
        return volumeId;
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
        List<? extends SnapshotPolicy> result = _snapshotService.listPoliciesforVolume(this);
        ListResponse<SnapshotPolicyResponse> response = new ListResponse<SnapshotPolicyResponse>();
        List<SnapshotPolicyResponse> policyResponses = new ArrayList<SnapshotPolicyResponse>();
        for (SnapshotPolicy policy : result) {
            SnapshotPolicyResponse policyResponse = _responseGenerator.createSnapshotPolicyResponse(policy);
            policyResponse.setObjectName("snapshotpolicy");
            policyResponses.add(policyResponse);
        }
        response.setResponses(policyResponses);
        response.setResponseName(getCommandName());   
        this.setResponseObject(response);
    }
}
