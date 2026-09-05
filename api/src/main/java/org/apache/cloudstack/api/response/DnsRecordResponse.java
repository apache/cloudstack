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

import java.util.List;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;
import org.apache.cloudstack.dns.DnsRecord;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class DnsRecordResponse extends BaseResponse {

    @SerializedName(ApiConstants.NAME)
    @Param(description = "The record name (e.g., www.example.com.)")
    private String name;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "The record type (e.g., A, CNAME, TXT)")
    private DnsRecord.RecordType type;

    @SerializedName("contents")
    @Param(description = "The contents of the record (IP address or target)")
    private List<String> contents;

    @SerializedName("ttl")
    @Param(description = "Time to live (TTL) in seconds")
    private Integer ttl;

    public DnsRecordResponse() {
        super();
        setObjectName("dnsrecord");
    }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setType(DnsRecord.RecordType type) { this.type = type; }
    public void setContent(List<String> contents) { this.contents = contents; }
    public void setTtl(Integer ttl) { this.ttl = ttl; }
}
