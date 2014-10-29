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
package com.cloud.stack.models;

import com.google.gson.annotations.SerializedName;

public class CloudStackResourceLimit {
    @SerializedName(ApiConstants.ACCOUNT)
    private String accountName;
    @SerializedName(ApiConstants.DOMAIN_ID)
    private String domainId;
    @SerializedName(ApiConstants.DOMAIN)
    private String domainName;
    @SerializedName(ApiConstants.RESOURCE_TYPE)
    private String resourceType;
    @SerializedName(ApiConstants.MAX)
    private Long max;

    public CloudStackResourceLimit() {
    }

    public String getAccountName() {
        return accountName;
    }

    public String getDomainId() {
        return domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Long getMax() {
        return max;
    }
}
