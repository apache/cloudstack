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

import com.cloud.event.EventTypes;
import com.cloud.exception.ResourceAllocationException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.storage.object.Bucket;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseAsyncCreateCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ResponseObject.ResponseView;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.command.user.UserCmd;
import org.apache.cloudstack.api.response.BucketResponse;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ObjectStoreResponse;
import org.apache.cloudstack.api.response.ProjectResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = "createBucket", responseObject = BucketResponse.class,
        description = "Creates a bucket in the specified object storage pool. ", responseView = ResponseView.Restricted,
        entityType = {Bucket.class}, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false, since = "4.19.0",
        authorized = {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User})
public class CreateBucketCmd extends BaseAsyncCreateCmd implements UserCmd {
    private static final String s_name = "createbucketresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.ACCOUNT,
               type = CommandType.STRING,
               description = "the account associated with the bucket. Must be used with the domainId parameter.")
    private String accountName;

    @Parameter(name = ApiConstants.PROJECT_ID,
               type = CommandType.UUID,
               entityType = ProjectResponse.class,
               description = "the project associated with the bucket. Mutually exclusive with account parameter")
    private Long projectId;

    @Parameter(name = ApiConstants.DOMAIN_ID,
               type = CommandType.UUID,
               entityType = DomainResponse.class,
               description = "the domain ID associated with the bucket. If used with the account parameter"
                   + " returns the bucket associated with the account for the specified domain.")
    private Long domainId;

    @Parameter(name = ApiConstants.NAME, type = CommandType.STRING, required = true,description = "the name of the bucket")
    private String bucketName;

    @Parameter(name = ApiConstants.OBJECT_STORAGE_ID, type = CommandType.UUID,
            entityType = ObjectStoreResponse.class, required = true,
            description = "Id of the Object Storage Pool where bucket is created")
    private long objectStoragePoolId;

    @Parameter(name = ApiConstants.QUOTA, type = CommandType.INTEGER,description = "Bucket Quota in GB")
    private Integer quota;

    @Parameter(name = ApiConstants.ENCRYPTION, type = CommandType.BOOLEAN, description = "Enable bucket encryption")
    private boolean encryption;

    @Parameter(name = ApiConstants.VERSIONING, type = CommandType.BOOLEAN, description = "Enable bucket versioning")
    private boolean versioning;

    @Parameter(name = ApiConstants.OBJECT_LOCKING, type = CommandType.BOOLEAN, description = "Enable object locking in bucket")
    private boolean objectLocking;

    @Parameter(name = ApiConstants.POLICY, type = CommandType.STRING,description = "The Bucket access policy")
    private String policy;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getAccountName() {
        return accountName;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getBucketName() {
        return bucketName;
    }

    private Long getProjectId() {
        return projectId;
    }

    public long getObjectStoragePoolId() {
        return objectStoragePoolId;
    }

    public Integer getQuota() {
        return quota;
    }

    public boolean isEncryption() {
        return encryption;
    }

    public boolean isVersioning() {
        return versioning;
    }

    public boolean isObjectLocking() {
        return objectLocking;
    }

    public String getPolicy() {
        return policy;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////
    @Override
    public String getCommandName() {
        return s_name;
    }

    public static String getResultObjectName() {
        return "bucket";
    }

    @Override
    public ApiCommandResourceType getApiResourceType() {
        return ApiCommandResourceType.Bucket;
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalyzeAccountId(accountName, domainId, projectId, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }

        return accountId;
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_BUCKET_CREATE;
    }

    @Override
    public String getEventDescription() {
        return  "creating bucket: " + getBucketName();
    }

    @Override
    public void create() throws ResourceAllocationException {
        Bucket bucket = _bucketService.allocBucket(this);
        if (bucket != null) {
            setEntityId(bucket.getId());
            setEntityUuid(bucket.getUuid());
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create bucket");
        }
    }

    @Override
    public void execute() {
        CallContext.current().setEventDetails("Bucket Id: " + getEntityUuid());

        Bucket bucket;
        try {
            bucket = _bucketService.createBucket(this);
        } catch (Exception e) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, e.getMessage());
        }
        if (bucket != null) {
            BucketResponse response = _responseGenerator.createBucketResponse(bucket);
            response.setResponseName(getCommandName());
            setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to create bucket with name: "+getBucketName());
        }
    }
}
