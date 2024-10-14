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
    @Param(description = "Name of the data store object.")
    private String name;

    @SerializedName("isdirectory")
    @Param(description = "Is it a directory.")
    private boolean isDirectory;

    @SerializedName(ApiConstants.SIZE)
    @Param(description = "Size is in Bytes.")
    private long size;

    @SerializedName(ApiConstants.TEMPLATE_ID)
    @Param(description = "Template ID associated with the data store object.")
    private String templateId;

    @SerializedName(ApiConstants.TEMPLATE_NAME)
    @Param(description = "Template Name associated with the data store object.")
    private String templateName;

    @SerializedName(ApiConstants.FORMAT)
    @Param(description = "Format of template associated with the data store object.")
    private String format;

    @SerializedName(ApiConstants.SNAPSHOT_ID)
    @Param(description = "Snapshot ID associated with the data store object.")
    private String snapshotId;

    @SerializedName("snapshotname")
    @Param(description = "Snapshot Name associated with the data store object.")
    private String snapshotName;

    @SerializedName(ApiConstants.VOLUME_ID)
    @Param(description = "Volume ID associated with the data store object.")
    private String volumeId;

    @SerializedName(ApiConstants.VOLUME_NAME)
    @Param(description = "Volume Name associated with the data store object.")
    private String volumeName;

    @SerializedName(ApiConstants.LAST_UPDATED)
    @Param(description = "Last modified date of the file/directory.")
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

    public void setVolumeId(String volumeId) {
        this.volumeId = volumeId;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public void setSnapshotName(String snapshotName) {
        this.snapshotName = snapshotName;
    }

    public void setVolumeName(String volumeName) {
        this.volumeName = volumeName;
    }

    public String getName() {
        return name;
    }

    public boolean isDirectory() {
        return isDirectory;
    }

    public long getSize() {
        return size;
    }

    public String getTemplateId() {
        return templateId;
    }

    public String getFormat() {
        return format;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public String getVolumeId() {
        return volumeId;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public String getTemplateName() {
        return templateName;
    }

    public String getSnapshotName() {
        return snapshotName;
    }

    public String getVolumeName() {
        return volumeName;
    }
}
