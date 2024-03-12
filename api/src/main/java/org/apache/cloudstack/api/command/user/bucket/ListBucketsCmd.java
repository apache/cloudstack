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
package org.apache.cloudstack.api.command.user.bucket;

import org.apache.cloudstack.storage.object.Bucket;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListTaggedResourcesCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.BucketResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.StoragePoolResponse;

import java.util.List;

@APICommand(name = "listBuckets", description = "Lists all Buckets.", responseObject = BucketResponse.class, responseView = ResponseView.Restricted, entityType = {
        Bucket.class}, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.19.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class ListBucketsCmd extends BaseListTaggedResourcesCmd implements UserCmd {

    private static final String s_name = "listbucketsresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ID, type = CommandType.UUID, entityType = BucketResponse.class, description = "the ID of the bucket")
    private Long id;

    @Parameter(name = ApiConstants.IDS, type = CommandType.LIST, collectionType = CommandType.UUID, entityType = BucketResponse.class, description = "the IDs of the Buckets, mutually exclusive with id")
    private List<Long> ids;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, description = "the name of the bucket")
    private String bucketName;

    @Parameter(name = ApiConstants.OBJECT_STORAGE_ID, type = CommandType.UUID, entityType = StoragePoolResponse.class, description = "the ID of the object storage pool, available to ROOT admin only", authorized = {
            RoleType.Admin})
    private Long objectStorageId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public Long getId() {
        return id;
    }

    public String getBucketName() {
        return bucketName;
    }

    public Long getObjectStorageId() {
        return objectStorageId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Bucket;
    }

    @Override
    public void execute() {
        ListResponse<BucketResponse> response = _queryService.searchForBuckets(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    public List<Long> getIds() {
        return ids;
    }
}
