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
package org.apache.cloudstack.api.command.admin.host;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.ZoneResponse;

import com.cloud.exception.DiscoveryException;
import com.cloud.host.Host;
import com.cloud.user.Account;

@APICommand(name = "addHost", description = "Adds a new host.", responseObject = HostResponse.class,
        requestHasSensitiveInfo = true, responseHasSensitiveInfo = false)
public class AddHostCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(AddHostCmd.class.getName());


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.CLUSTER_ID, type = CommandType.UUID, entityType = ClusterResponse.class, description = "the cluster ID for the host")
    private Long clusterId;

    @Parameter(name = ApiConstants.CLUSTER_NAME, type = CommandType.STRING, description = "the cluster name for the host")
    private String clusterName;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, description = "the username for the host; required to be passed for hypervisors other than VMWare")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, description = "the password for the host; required to be passed for hypervisors other than VMWare")
    private String password;

    @Parameter(name = ApiConstants.POD_ID, type = CommandType.UUID, entityType = PodResponse.class, required = true, description = "the Pod ID for the host")
    private Long podId;

    @Parameter(name = ApiConstants.URL, type = CommandType.STRING, required = true, description = "the host URL")
    private String url;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, required = true, description = "the Zone ID for the host")
    private Long zoneId;

    @Parameter(name = ApiConstants.HYPERVISOR, type = CommandType.STRING, required = true, description = "hypervisor type of the host")
    private String hypervisor;

    @Parameter(name = ApiConstants.ALLOCATION_STATE, type = CommandType.STRING, description = "Allocation state of this Host for allocation of new resources")
    private String allocationState;

    @Parameter(name = ApiConstants.HOST_TAGS, type = CommandType.LIST, collectionType = CommandType.STRING, description = "list of tags to be added to the host")
    private List<String> hostTags;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getClusterId() {
        return clusterId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getPassword() {
        return password;
    }

    public Long getPodId() {
        return podId;
    }

    public String getUrl() {
        return url;
    }

    public String getUsername() {
        return username;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getHypervisor() {
        return hypervisor;
    }

    public List<String> getHostTags() {
        return hostTags;
    }

    public String getAllocationState() {
        return allocationState;
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
        try {
            List<? extends Host> result = _resourceService.discoverHosts(this);
            ListResponse<HostResponse> response = new ListResponse<HostResponse>();
            List<HostResponse> hostResponses = new ArrayList<HostResponse>();
            if (result != null && result.size() > 0) {
                for (Host host : result) {
                    HostResponse hostResponse = _responseGenerator.createHostResponse(host);
                    hostResponses.add(hostResponse);
                }
            } else {
                throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to add host");
            }

            response.setResponses(hostResponses);
            response.setResponseName(getCommandName());

            this.setResponseObject(response);
        } catch (DiscoveryException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, ex.getMessage());
        }
    }
}
