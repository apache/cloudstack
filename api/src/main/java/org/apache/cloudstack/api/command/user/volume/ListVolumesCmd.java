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
package org.apache.cloudstack.api.command.user.volume;

import java.util.List;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandJobType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.ClusterResponse;
import org.apache.cloudstack.api.response.DiskOfferingResponse;
import org.apache.cloudstack.api.response.HostResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.PodResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;
import org.apache.cloudstack.api.response.UserVmResponse;
import org.apache.cloudstack.api.response.VolumeResponse;
import org.apache.cloudstack.api.response.ZoneResponse;
import org.apache.log4j.Logger;

import com.cloud.storage.Volume;

@APICommand(name = "listVolumes", description = "Lists all volumes.", responseObject = VolumeResponse.class, responseView = ResponseView.Restricted, entityType = {
        Volume.class}, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListVolumesCmd extends BaseListTaggedResourcesCmd implements UserCmd {
    public static final Logger s_logger = Logger.getLogger(ListVolumesCmd.class.getName());

    private static final String s_name = "listvolumesresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.HOST_ID, type = CommandType.UUID, entityType = HostResponse.class, description = "list volumes on specified host")
    private Long hostId;

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = VolumeResponse.class, description = "the ID of the disk volume")
    private Long id;

    @Parameter(name = ApiConstants.IDS, type = CommandType.LIST, collectionType = CommandType.UUID, entityType = VolumeResponse.class, description = "the IDs of the volumes, mutually exclusive with id", since = "4.9")
    private List<Long> ids;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the name of the disk volume")
    private String volumeName;

    @Parameter(name = ApiConstants.POD_ID, type = CommandType.UUID, entityType = PodResponse.class, description = "the pod id the disk volume belongs to")
    private Long podId;

    @Parameter(name = ApiConstants.CLUSTER_ID, type = CommandType.UUID, entityType = ClusterResponse.class, description = "the cluster id the disk volume belongs to", authorized = {RoleType.Admin})
    private Long clusterId;

    @Parameter(name = ApiConstants.TYPE, type = CommandType.STRING, description = "the type of disk volume")
    private String type;

    @Parameter(name = ApiConstants.VIRTUAL_MACHINE_ID, type = CommandType.UUID, entityType = UserVmResponse.class, description = "the ID of the virtual machine")
    private Long virtualMachineId;

    @Parameter(name = ApiConstants.ZONE_ID, type = CommandType.UUID, entityType = ZoneResponse.class, description = "the ID of the availability zone")
    private Long zoneId;

    @Parameter(name = ApiConstants.STORAGE_ID, type = CommandType.STRING, entityType = StoragePoolResponse.class, description = "the ID of the storage pool, available to ROOT admin only", since = "4.3", authorized = {
            RoleType.Admin})
    private String storageId;

    @Parameter(name = ApiConstants.DISK_OFFERING_ID, type = CommandType.UUID, entityType = DiskOfferingResponse.class, description = "list volumes by disk offering", since = "4.4")
    private Long diskOfferingId;

    @Parameter(name = ApiConstants.DISPLAY_VOLUME, type = CommandType.BOOLEAN, description = "list resources by display flag; only ROOT admin is eligible to pass this parameter", since = "4.4", authorized = {
            RoleType.Admin})
    private Boolean display;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getHostId() {
        return hostId;
    }

    public Long getClusterId() {
        return clusterId;
    }

    public Long getId() {
        return id;
    }

    public String getVolumeName() {
        return volumeName;
    }

    public Long getPodId() {
        return podId;
    }

    public Long getDiskOfferingId() {
        return diskOfferingId;
    }

    public String getType() {
        return type;
    }

    public Long getVirtualMachineId() {
        return virtualMachineId;
    }

    public Long getZoneId() {
        return zoneId;
    }

    public String getStorageId() {
        return storageId;
    }

    @Override
    public Boolean getDisplay() {
        if (display != null) {
            return display;
        }
        return super.getDisplay();
    }
    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public ApiCommandJobType getInstanceType() {
        return ApiCommandJobType.Volume;
    }

    @Override
    public void execute() {
        ListResponse<VolumeResponse> response = _queryService.searchForVolumes(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    public List<Long> getIds() {
        return ids;
    }
}
