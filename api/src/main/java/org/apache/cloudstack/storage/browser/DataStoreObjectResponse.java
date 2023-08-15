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
package org.apache.cloudstack.storage.browser;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import java.util.Date;

public class DataStoreObjectResponse extends BaseResponse {

    @SerializedName(ApiConstants.NAME)
    @Param(description = "Name of the image store object.")
    private String name;

    @SerializedName("isdirectory")
    @Param(description = "Name of the image store object.")
    private boolean isDirectory;

    @SerializedName(ApiConstants.SIZE)
    @Param(description = "Name of the image store object.")
    private long size;

    @SerializedName(ApiConstants.TEMPLATE_ID)
    @Param(description = "Template response for the image store object.")
    private String templateId;

    @SerializedName(ApiConstants.FORMAT)
    @Param(description = "Template response for the image store object.")
    private String format;

    @SerializedName(ApiConstants.SNAPSHOT_ID)
    @Param(description = "Template response for the image store object.")
    private String snapshotId;

    @SerializedName(ApiConstants.LAST_UPDATED)
    @Param(description = "Template response for the image store object.")
    private Date lastUpdated;

    public DataStoreObjectResponse(String name, boolean isDirectory, long size, Date lastUpdated) {
        this.name = name;
        this.isDirectory = isDirectory;
        this.size = size;
        this.lastUpdated = lastUpdated;
        this.setObjectName("datastoreobject");
    }

    public DataStoreObjectResponse() {
        super();
        this.setObjectName("datastoreobject");
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }
}
