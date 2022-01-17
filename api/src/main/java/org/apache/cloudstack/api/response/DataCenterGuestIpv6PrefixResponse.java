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

import com.cloud.dc.DataCenterGuestIpv6Prefix;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

@EntityReference(value = DataCenterGuestIpv6Prefix.class)
public class DataCenterGuestIpv6PrefixResponse extends BaseResponse {
    @SerializedName(ApiConstants.ID)
    @Param(description = "id of the guest IPv6 prefix")
    private String id;

    @SerializedName(ApiConstants.PREFIX)
    @Param(description = "guest IPv6 prefix")
    private String prefix;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "id of zone to which the IPv6 prefix belongs to." )
    private String zoneId;

    @SerializedName(ApiConstants.USED_SUBNETS)
    @Param(description = "count of the used IPv6 subnets for the prefix." )
    private Integer usedSubnets;

    @SerializedName(ApiConstants.AVAILABLE_SUBNETS)
    @Param(description = "count of the available IPv6 subnets for the prefix." )
    private Integer availableSubnets;

    @SerializedName(ApiConstants.TOTAL_SUBNETS)
    @Param(description = "count of the total IPv6 subnets for the prefix." )
    private Integer totalSubnets;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = " date when this IPv6 prefix was created." )
    private Date created;

    public void setId(String id) {
        this.id = id;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public void setUsedSubnets(Integer usedSubnets) {
        this.usedSubnets = usedSubnets;
    }

    public void setAvailableSubnets(Integer availableSubnets) {
        this.availableSubnets = availableSubnets;
    }

    public void setTotalSubnets(Integer totalSubnets) {
        this.totalSubnets = totalSubnets;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
