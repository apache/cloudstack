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
import org.apache.cloudstack.api.EntityReference;

import com.cloud.network.VirtualRouterProvider;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value=VirtualRouterProvider.class)
@SuppressWarnings("unused")
public class InternalLoadBalancerElementResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID) @Param(description="the id of the internal load balancer element")
    private String id;

    @SerializedName(ApiConstants.NSP_ID) @Param(description="the physical network service provider id of the element")
    private String nspId;

    @SerializedName(ApiConstants.ENABLED) @Param(description="Enabled/Disabled the element")
    private Boolean enabled;


    public void setId(String id) {
        this.id = id;
    }

    public void setNspId(String nspId) {
        this.nspId = nspId;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
