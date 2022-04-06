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

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.vpc.NetworkACL;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = NetworkACL.class)
public class NetworkACLResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the ID of the ACL")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the Name of the ACL")
    private String name;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "Description of the ACL")
    private String description;

    @SerializedName(ApiConstants.VPC_ID)
    @Param(description = "Id of the VPC this ACL is associated with")
    private String vpcId;

    @SerializedName(ApiConstants.VPC_NAME)
    @Param(description = "Name of the VPC this ACL is associated with")
    private String vpcName;

    @SerializedName(ApiConstants.FOR_DISPLAY)
    @Param(description = "is ACL for display to the regular user", since = "4.4", authorized = {RoleType.Admin})
    private Boolean forDisplay;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public void setVpcName(String vpcName) {
        this.vpcName = vpcName;
    }

    public void setForDisplay(Boolean forDisplay) {
        this.forDisplay = forDisplay;
    }
}
