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

import com.cloud.bgp.ASNumberRange;
import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.api.EntityReference;

import java.util.Date;

@EntityReference(value = ASNumberRange.class)
public class ASNRangeResponse extends BaseResponse {

    @SerializedName(ApiConstants.ID)
    @Param(description = "ID of the AS Number Range")
    private String id;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "Zone ID")
    private String zoneId;

    @SerializedName(ApiConstants.START_ASN)
    @Param(description = "Start AS Number")
    private Long startASNumber;

    @SerializedName(ApiConstants.END_ASN)
    @Param(description = "End AS Number")
    private Long endASNumber;

    @SerializedName(ApiConstants.CREATED)
    @Param(description = "Created date")
    private Date created;

    public ASNRangeResponse() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public Long getStartASNumber() {
        return startASNumber;
    }

    public void setStartASNumber(Long startASNumber) {
        this.startASNumber = startASNumber;
    }

    public Long getEndASNumber() {
        return endASNumber;
    }

    public void setEndASNumber(Long endASNumber) {
        this.endASNumber = endASNumber;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
