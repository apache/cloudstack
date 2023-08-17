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

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.serializer.Param;
import com.cloud.storage.GuestOS;

@EntityReference(value = GuestOS.class)
public class GuestOSResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the OS type")
    private String id;

    @SerializedName(ApiConstants.OS_CATEGORY_ID)
    @Param(description = "the ID of the OS category")
    private String osCategoryId;

    @SerializedName(ApiConstants.OS_CATEGORY_NAME)
    @Param(description = "the name of the OS category")
    private String osCategoryName;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the OS type")
    private String name;

    /**
     * @deprecated description, as name is the correct interpretation and is needed for UI forms
     */
    @Deprecated(since = "4.19")
    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the name/description of the OS type")
    private String description;

    @SerializedName(ApiConstants.IS_USER_DEFINED)
    @Param(description = "is the guest OS user defined")
    private String isUserDefined;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOsCategoryId() {
        return osCategoryId;
    }

    public void setOsCategoryId(String osCategoryId) {
        this.osCategoryId = osCategoryId;
    }

    public String getOsCategoryName() {
        return osCategoryName;
    }

    public void setOsCategoryName(String osCategoryName) {
        this.osCategoryName = osCategoryName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIsUserDefined() {
        return isUserDefined;
    }

    public void setIsUserDefined(String isUserDefined) {
        this.isUserDefined = isUserDefined;
    }

}
