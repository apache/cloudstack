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
import org.apache.cloudstack.datacenter.DataCenterIpv4GuestSubnet;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = DataCenterIpv4GuestSubnet.class)
public class DataCenterIpv4SubnetResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "id of the guest IPv4 subnet")
    private String id;

    @SerializedName(ApiConstants.SUBNET)
    @Param(description = "guest IPv4 subnet")
    private String subnet;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "id of zone to which the IPv4 subnet belongs to." )
    private String zoneId;

    @SerializedName(ApiConstants.USED_SUBNETS)
    @Param(description = "count of the used IPv4 subnets for the subnet." )
    private Integer usedSubnets;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "date when this IPv4 subnet was created." )
    private Date created;

    public void setId(String id) {
        this.id = id;
    }

    public void setSubnet(String subnet) {
        this.subnet = subnet;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setUsedSubnets(Integer usedSubnets) {
        this.usedSubnets = usedSubnets;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
