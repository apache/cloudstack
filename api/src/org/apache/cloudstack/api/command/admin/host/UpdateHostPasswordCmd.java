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

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.log4j.Logger;

import com.cloud.user.Account;

@APICommand(name = "updateHostPassword", description = "Update password of a host/pool on management server.", responseObject = SuccessResponse.class,
requestHasSensitiveInfo = true, responseHasSensitiveInfo = false)
public class UpdateHostPasswordCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(UpdateHostPasswordCmd.class.getName());

    private static final String s_name = "updatehostpasswordresponse";

    // ///////////////////////////////////////////////////
    // ////////////// API parameters /////////////////////
    // ///////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class, description = "the host ID")
    private Long hostId;

    @Parameter(name = ApiConstants.CLUSTER_ID, type = CommandType.UUID, entityType = ClusterResponse.class, description = "the cluster ID")
    private Long clusterId;

    @Parameter(name = ApiConstants.SHOULD_UPDATE_PASSWORD, type = CommandType.BOOLEAN, description = "if the password should also be updated on the hosts")
    private Boolean updatePasswdOnHost;

    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, required = true, description = "the username for the host/cluster")
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, required = true, description = "the new password for the host/cluster")
    private String password;


    // ///////////////////////////////////////////////////
    // ///////////////// Accessors ///////////////////////
    // ///////////////////////////////////////////////////

    public Long getHostId() {
        return hostId;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public Boolean getUpdatePasswdOnHost() {
        return updatePasswdOnHost == null ? false : true;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    // ///////////////////////////////////////////////////
    // ///////////// API Implementation///////////////////
    // ///////////////////////////////////////////////////

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
        if (getClusterId() == null) {
            _mgr.updateHostPassword(this);
            _resourceService.updateHostPassword(this);
        } else {
            _mgr.updateClusterPassword(this);
            _resourceService.updateClusterPassword(this);
        }

        setResponseObject(new SuccessResponse(getCommandName()));
    }
}
