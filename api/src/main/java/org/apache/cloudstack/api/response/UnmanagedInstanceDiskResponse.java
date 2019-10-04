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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class UnmanagedInstanceDiskResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the disk")
    private String diskId;

    @SerializedName(ApiConstants.LABEL)
    @Param(description = "the label of the disk")
    private String label;

    @SerializedName(ApiConstants.CAPACITY)
    @Param(description = "the capacity of the disk in bytes")
    private Long capacity;

    @SerializedName(ApiConstants.IMAGE_PATH)
    @Param(description = "the file path of the disk image")
    private String imagePath;

    @SerializedName(ApiConstants.CONTROLLER)
    @Param(description = "the controller of the disk")
    private String controller;

    @SerializedName(ApiConstants.CONTROLLER_UNIT)
    @Param(description = "the controller unit of the disk")
    private Integer controllerUnit;

    @SerializedName(ApiConstants.POSITION)
    @Param(description = "the position of the disk")
    private Integer position;

    @SerializedName(ApiConstants.DATASTORE_NAME)
    @Param(description = "the controller of the disk")
    private String datastoreName;

    @SerializedName(ApiConstants.DATASTORE_HOST)
    @Param(description = "the controller of the disk")
    private String datastoreHost;

    @SerializedName(ApiConstants.DATASTORE_PATH)
    @Param(description = "the controller of the disk")
    private String datastorePath;

    @SerializedName(ApiConstants.DATASTORE_TYPE)
    @Param(description = "the controller of the disk")
    private String datastoreType;

    public String getDiskId() {
        return diskId;
    }

    public void setDiskId(String diskId) {
        this.diskId = diskId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Long getCapacity() {
        return capacity;
    }

    public void setCapacity(Long capacity) {
        this.capacity = capacity;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getController() {
        return controller;
    }

    public void setController(String controller) {
        this.controller = controller;
    }

    public Integer getControllerUnit() {
        return controllerUnit;
    }

    public void setControllerUnit(Integer controllerUnit) {
        this.controllerUnit = controllerUnit;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getDatastoreName() {
        return datastoreName;
    }

    public void setDatastoreName(String datastoreName) {
        this.datastoreName = datastoreName;
    }

    public String getDatastoreHost() {
        return datastoreHost;
    }

    public void setDatastoreHost(String datastoreHost) {
        this.datastoreHost = datastoreHost;
    }

    public String getDatastorePath() {
        return datastorePath;
    }

    public void setDatastorePath(String datastorePath) {
        this.datastorePath = datastorePath;
    }

    public String getDatastoreType() {
        return datastoreType;
    }

    public void setDatastoreType(String datastoreType) {
        this.datastoreType = datastoreType;
    }
}
