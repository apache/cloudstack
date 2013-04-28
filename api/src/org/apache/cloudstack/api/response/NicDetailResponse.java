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

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@SuppressWarnings("unused")
public class NicDetailResponse extends BaseResponse{
    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the nic")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "name of the nic detail")
    private String name;


    @SerializedName(ApiConstants.VALUE)
    @Param(description = "value of the nic detail")
    private String value;

    @SerializedName(ApiConstants.DISPLAY_NIC) @Param(description="an optional field whether to the display the nic to the end user or not.")
    private Boolean displayNic;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getDisplayNic() {
        return displayNic;
    }

    public void setDisplayNic(Boolean displayNic) {
        this.displayNic = displayNic;
    }
}
