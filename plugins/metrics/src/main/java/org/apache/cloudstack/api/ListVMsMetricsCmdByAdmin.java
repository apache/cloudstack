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

package org.apache.cloudstack.api;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.command.admin.AdminCmd;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.response.VmMetricsResponse;

@APICommand(name = ListVMsMetricsCmd.APINAME, description = "Lists VM metrics", responseObject = VmMetricsResponse.class,
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false,  responseView = ResponseObject.ResponseView.Full,
        authorized = {RoleType.Admin})
public class ListVMsMetricsCmdByAdmin extends ListVMsMetricsCmd implements AdminCmd {

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name=ApiConstants.HOST_ID, type= BaseCmd.CommandType.UUID, entityType= HostResponse.class,
            description="the host ID")
    private Long hostId;

    @Parameter(name=ApiConstants.POD_ID, type= BaseCmd.CommandType.UUID, entityType= PodResponse.class,
            description="the pod ID")
    private Long podId;

    @Parameter(name=ApiConstants.STORAGE_ID, type= BaseCmd.CommandType.UUID, entityType= StoragePoolResponse.class,
            description="the storage ID where vm's volumes belong to")
    private Long storageId;

    @Parameter(name = ApiConstants.CLUSTER_ID, type = BaseCmd.CommandType.UUID, entityType = ClusterResponse.class,
            description = "the cluster ID")
    private Long clusterId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getHostId() {
        return hostId;
    }

    public Long getPodId() {
        return podId;
    }

    public Long getStorageId() {
        return storageId;
    }

    public Long getClusterId() {
        return clusterId;
    }
}
