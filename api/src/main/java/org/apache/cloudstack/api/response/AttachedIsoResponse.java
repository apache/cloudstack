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

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class AttachedIsoResponse extends BaseResponse {

    @SerializedName("id")
    @Param(description = "The ID of the attached ISO")
    private String id;

    @SerializedName("name")
    @Param(description = "The name of the attached ISO")
    private String name;

    @SerializedName("displaytext")
    @Param(description = "The display text of the attached ISO")
    private String displayText;

    @SerializedName("deviceseq")
    @Param(description = "The cdrom slot that holds this ISO (3=hdc, 4=hdd, ...)")
    private Integer deviceSeq;

    public AttachedIsoResponse() {
    }

    public AttachedIsoResponse(String id, String name, String displayText, Integer deviceSeq) {
        this.id = id;
        this.name = name;
        this.displayText = displayText;
        this.deviceSeq = deviceSeq;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDisplayText() {
        return displayText;
    }

    public Integer getDeviceSeq() {
        return deviceSeq;
    }
}
