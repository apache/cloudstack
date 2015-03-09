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
package org.apache.cloudstack.api.command.admin.volume;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.user.volume.ListVolumesCmd;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.VolumeResponse;

import com.cloud.storage.Volume;


@APICommand(name = "listVolumes", description = "Lists all volumes.", responseObject = VolumeResponse.class, responseView = ResponseView.Full, entityType = {Volume.class},
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListVolumesCmdByAdmin extends ListVolumesCmd {
    public static final Logger s_logger = Logger.getLogger(ListVolumesCmdByAdmin.class.getName());

    @Parameter(name=ApiConstants.POD_ID, type=CommandType.UUID, entityType=PodResponse.class,
            description="the pod id the disk volume belongs to")
    private Long podId;


    @Parameter(name=ApiConstants.STORAGE_ID, type=CommandType.UUID, entityType=StoragePoolResponse.class,
            description="the ID of the storage pool, available to ROOT admin only", since="4.3", authorized = { RoleType.Admin })
    private Long storageId;


    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    @Override
    public Long getPodId() {
        return podId;
    }


    @Override
    public Long getStorageId() {
        return storageId;
    }

}
