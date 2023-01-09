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
package org.apache.cloudstack.api.command.admin.network;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.StorageNetworkIpRangeResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.dc.StorageNetworkIpRange;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.user.Account;

@APICommand(name = "listStorageNetworkIpRange", description = "List a storage network IP range.", responseObject = StorageNetworkIpRangeResponse.class, since = "3.0.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListStorageNetworkIpRangeCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListStorageNetworkIpRangeCmd.class);

    String _name = "liststoragenetworkiprangeresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID,
               type = CommandType.UUID,
               entityType = StorageNetworkIpRangeResponse.class,
               description = "optional parameter. Storaget network IP range uuid, if specicied, using it to search the range.")
    private Long rangeId;

    @Parameter(name = ApiConstants.POD_ID,
               type = CommandType.UUID,
               entityType = PodResponse.class,
               description = "optional parameter. Pod uuid, if specicied and range uuid is absent, using it to search the range.")
    private Long podId;

    @Parameter(name = ApiConstants.ZONE_ID,
               type = CommandType.UUID,
               entityType = ZoneResponse.class,
               description = "optional parameter. Zone uuid, if specicied and both pod uuid and range uuid are absent, using it to search the range.")
    private Long zoneId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getRangeId() {
        return rangeId;
    }

    public Long getPodId() {
        return podId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException,
        ResourceAllocationException {
        try {
            List<StorageNetworkIpRange> results = _storageNetworkService.listIpRange(this);
            ListResponse<StorageNetworkIpRangeResponse> response = new ListResponse<StorageNetworkIpRangeResponse>();
            List<StorageNetworkIpRangeResponse> resList = new ArrayList<StorageNetworkIpRangeResponse>(results.size());
            for (StorageNetworkIpRange r : results) {
                StorageNetworkIpRangeResponse resp = _responseGenerator.createStorageNetworkIpRangeResponse(r);
                resList.add(resp);
            }
            response.setResponses(resList);
            response.setResponseName(getCommandName());
            this.setResponseObject(response);
        } catch (Exception e) {
            s_logger.warn("Failed to list storage network ip range for rangeId=" + getRangeId() + " podId=" + getPodId() + " zoneId=" + getZoneId());
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
