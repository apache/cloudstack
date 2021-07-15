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

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.network.lb.LoadBalancerConfig;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = LoadBalancerConfig.class)
@SuppressWarnings("unused")
public class LoadBalancerConfigResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "the load balancer rule ID")
    private String id;

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the load balancer config")
    private String name;

    @SerializedName(ApiConstants.VALUE)
    @Param(description = "the value of the load balancer config")
    private String value;

    @SerializedName(ApiConstants.SCOPE)
    @Param(description = "the scope of the load balancer config")
    private String scope;

    @SerializedName(ApiConstants.NETWORK_ID)
    @Param(description = "the id of the guest network the lb config belongs to")
    private String networkId;

    @SerializedName(ApiConstants.VPC_ID)
    @Param(description = "the id of the vpc the lb config belongs to")
    private String vpcId;

    @SerializedName(ApiConstants.LOAD_BALANCER_ID)
    @Param(description = "the id of the load balancer rule the config belongs to")
    private String loadBalancerId;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "the date when the load balancer config is created")
    private Date created;

    @SerializedName(ApiConstants.DESCRIPTION)
    @Param(description = "the description of the load balancer config")
    private String description;

    @SerializedName(ApiConstants.DEFAULT_VALUE)
    @Param(description = "the default value of the load balancer config")
    private String defaultValue;

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setNetworkId(String networkId) {
        this.networkId = networkId;
    }

    public void setVpcId(String vpcId) {
        this.vpcId = vpcId;
    }

    public void setLoadBalancerId(String loadBalancerId) {
        this.loadBalancerId = loadBalancerId;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }
}
