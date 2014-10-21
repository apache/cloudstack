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

package org.apache.cloudstack.network.opendaylight.api.responses;

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.network.opendaylight.dao.OpenDaylightControllerVO;

import com.cloud.serializer.Param;

@EntityReference(value = OpenDaylightControllerVO.class)
public class OpenDaylightControllerResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "device id of the controller")
    private String id;

    @SerializedName(ApiConstants.PHYSICAL_NETWORK_ID)
    @Param(description = "the physical network to which this controller belongs to")
    private String physicalNetworkId;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name assigned to the controller")
    private String name;

    @SerializedName(ApiConstants.URL)
    @Param(description = "the url of the controller api")
    private String url;

    @SerializedName(ApiConstants.USERNAME)
    @Param(description = "the username to authenticate to the controller")
    private String username;

    public void setId(String id) {
        this.id = id;
    }

    public void setPhysicalNetworkId(String physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
