//
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
//

package org.apache.cloudstack.storage.datastore.util;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class ListElastistorVolumeResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the id of the volume")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the volume")
    private String name;

    @SerializedName("graceallowed")
    @Param(description = "graceallowed")
    private String graceAllowed;

    @SerializedName("deduplication")
    @Param(description = "deduplication")
    private String deduplication;

    @SerializedName("compression")
    @Param(description = "compression")
    private String compression;

    @SerializedName("sync")
    @Param(description = "synchronization")
    private String sync;

    public String getGraceAllowed() {
        return graceAllowed;
    }

    public void setGraceAllowed(String graceAllowed) {
        this.graceAllowed = graceAllowed;
    }

    public String getDeduplication() {
        return deduplication;
    }

    public void setDeduplication(String deduplication) {
        this.deduplication = deduplication;
    }

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public String getSync() {
        return sync;
    }

    public void setSync(String sync) {
        this.sync = sync;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return id;
    }

}
