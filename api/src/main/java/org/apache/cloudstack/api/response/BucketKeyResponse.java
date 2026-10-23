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
package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.storage.object.BucketCredentialKey;

import java.util.Date;

@EntityReference(value = BucketCredentialKey.class)
public class BucketKeyResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the bucket key")
    private String id;

    @SerializedName(ApiConstants.KEY_SLOT)
    @Param(description = "the key slot (1 or 2) this key occupies")
    private Integer keySlot;

    @SerializedName(ApiConstants.ACCESS_KEY)
    @Param(description = "the access key")
    private String accessKey;

    @SerializedName(ApiConstants.SECRET_KEY)
    @Param(description = "the secret key; absent once the key is revoked", isSensitive = true)
    private String secretKey;

    @SerializedName(ApiConstants.STATE)
    @Param(description = "state of the key: Active or Revoked")
    private String state;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the date the key was created")
    private Date created;

    @SerializedName(ApiConstants.LAST_USED)
    @Param(description = "the date the key was last used, if known")
    private Date lastUsed;

    public BucketKeyResponse() {
        setObjectName("bucketkey");
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setKeySlot(Integer keySlot) {
        this.keySlot = keySlot;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed;
    }

    public String getId() {
        return id;
    }

    public Integer getKeySlot() {
        return keySlot;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getState() {
        return state;
    }
}
