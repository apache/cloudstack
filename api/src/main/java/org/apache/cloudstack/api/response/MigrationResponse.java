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

import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.storage.ImageStore;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = ImageStore.class)
public class MigrationResponse extends BaseResponse {
    @SerializedName("message")
    @Param(description = "Response message from migration of secondary storage data objects")
    private String message;

    @SerializedName("migrationtype")
    @Param(description = "Type of migration requested for")
    private String migrationType;

    @SerializedName("success")
    @Param(description = "true if operation is executed successfully")
    private boolean success;

    MigrationResponse() {
    }

    public MigrationResponse(String message, String migrationType, boolean success) {
        this.message = message;
        this.migrationType = migrationType;
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMigrationType() {
        return migrationType;
    }

    public void setMigrationType(String migrationType) {
        this.migrationType = migrationType;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
