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
package org.apache.cloudstack.api;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public abstract class BaseResponseWithAssociatedNetwork extends BaseResponseWithAnnotations {

    @SerializedName(ApiConstants.ASSOCIATED_NETWORK_ID)
    @Param(description = "the ID of the Network associated with this private gateway")
    private String associatedNetworkId;

    @SerializedName(ApiConstants.ASSOCIATED_NETWORK)
    @Param(description = "the name of the Network associated with this private gateway")
    private String associatedNetworkName;

    public void setAssociatedNetworkId(String associatedNetworkId) {
        this.associatedNetworkId = associatedNetworkId;
    }

    public void setAssociatedNetworkName(String associatedNetworkName) {
        this.associatedNetworkName = associatedNetworkName;
    }

}
