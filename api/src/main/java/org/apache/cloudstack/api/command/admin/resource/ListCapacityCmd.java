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
package org.apache.cloudstack.api.command.admin.resource;

import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.CapacityResponse;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.capacity.Capacity;
import com.cloud.exception.InvalidParameterValueException;

@APICommand(name = "listCapacity", description = "Lists all the system wide capacities.", responseObject = CapacityResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListCapacityCmd extends BaseListCmd {

    public static final Logger s_logger = Logger.getLogger(ListCapacityCmd.class.getName());
    private static final DecimalFormat s_percentFormat = new DecimalFormat("##.##");


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "lists capacity by the Zone ID")
    private Long zoneId;

    @Parameter(name = ApiConstants.POD_ID, type = CommandType.UUID, entityType = PodResponse.class, description = "lists capacity by the Pod ID")
    private Long podId;

    @Parameter(name = ApiConstants.CLUSTER_ID,
               type = CommandType.UUID,
               entityType = ClusterResponse.class,
               since = "3.0.0",
               description = "lists capacity by the Cluster ID")
    private Long clusterId;

    @Parameter(name = ApiConstants.FETCH_LATEST, type = CommandType.BOOLEAN, since = "3.0.0", description = "recalculate capacities and fetch the latest")
    private Boolean fetchLatest;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.INTEGER, description = "lists capacity by type" + "* CAPACITY_TYPE_MEMORY = 0" + "* CAPACITY_TYPE_CPU = 1"
        + "* CAPACITY_TYPE_STORAGE = 2" + "* CAPACITY_TYPE_STORAGE_ALLOCATED = 3" + "* CAPACITY_TYPE_VIRTUAL_NETWORK_PUBLIC_IP = 4" + "* CAPACITY_TYPE_PRIVATE_IP = 5"
        + "* CAPACITY_TYPE_SECONDARY_STORAGE = 6" + "* CAPACITY_TYPE_VLAN = 7" + "* CAPACITY_TYPE_DIRECT_ATTACHED_PUBLIC_IP = 8" + "* CAPACITY_TYPE_LOCAL_STORAGE = 9"
        + "* CAPACITY_TYPE_GPU = 19" + "* CAPACITY_TYPE_CPU_CORE = 90.")
    private Integer type;

    @Parameter(name = ApiConstants.SORT_BY, type = CommandType.STRING, since = "3.0.0", description = "Sort the results. Available values: Usage")
    private String sortBy;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getZoneId() {
        return zoneId;
    }

    public Long getPodId() {
        return podId;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public Boolean getFetchLatest() {
        return fetchLatest;
    }

    public Integer getType() {
        return type;
    }

    public String getSortBy() {
        if (sortBy != null) {
            if (sortBy.equalsIgnoreCase("usage")) {
                return sortBy;
            } else {
                throw new InvalidParameterValueException("Only value supported for sortBy parameter is : usage");
            }
        }

        return null;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public void execute() {
        List<? extends Capacity> result = null;
        if (getSortBy() != null) {
            result = _mgr.listTopConsumedResources(this);
        } else {
            result = _mgr.listCapacities(this);
        }

        ListResponse<CapacityResponse> response = new ListResponse<CapacityResponse>();
        List<CapacityResponse> capacityResponses = _responseGenerator.createCapacityResponse(result, s_percentFormat);
        Collections.sort(capacityResponses, new Comparator<CapacityResponse>() {
            public int compare(CapacityResponse resp1, CapacityResponse resp2) {
                int res = resp1.getZoneName().compareTo(resp2.getZoneName());
                if (res != 0) {
                    return res;
                } else {
                    return resp1.getCapacityType().compareTo(resp2.getCapacityType());
                }
            }
        });

        response.setResponses(capacityResponses);
        response.setResponseName(getCommandName());
        this.setResponseObject(response);
    }
}
