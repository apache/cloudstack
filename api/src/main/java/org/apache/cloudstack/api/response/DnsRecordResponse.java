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
import org.apache.cloudstack.dns.DnsRecord;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class DnsRecordResponse extends BaseResponse {

    @SerializedName(ApiConstants.NAME)
    @Param(description = "the name of the DNS record")
    private String name;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "the type of the DNS record (A, CNAME, etc)")
    private String type;

    @SerializedName("content")
    @Param(description = "the content of the record (IP address or target)")
    private String content;

    @SerializedName("ttl")
    @Param(description = "the time to live (TTL) in seconds")
    private Integer ttl;

    @SerializedName(ApiConstants.ZONE_ID)
    @Param(description = "the ID of the zone this record belongs to")
    private String zoneId;

    @SerializedName("sourceid")
    @Param(description = "the external ID of the record on the provider")
    private String sourceId;

    public DnsRecordResponse() {
        super();
        setObjectName("dnsrecord");
    }

    // Setters
    public void setName(String name) { this.name = name; }

    // Accepts String or Enum.toString()
    public void setType(String type) { this.type = type; }
    public void setType(DnsRecord.RecordType type) {
        this.type = (type != null) ? type.name() : null;
    }

    public void setContent(String content) { this.content = content; }
    public void setTtl(Integer ttl) { this.ttl = ttl; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
}
