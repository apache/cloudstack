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

public class CloudStackInstanceGroup {
    @SerializedName(ApiConstants.ID)
    private Long id;
    @SerializedName(ApiConstants.ACCOUNT)
    private String account;
    @SerializedName(ApiConstants.CREATED)
    private String created;
    @SerializedName(ApiConstants.DOMAIN)
    private String domain;
    @SerializedName(ApiConstants.DOMAIN_ID)
    private Long domainId;
    @SerializedName(ApiConstants.NAME)
    private String name;

    /**
     *
     */
    public CloudStackInstanceGroup() {
        // TODO Auto-generated constructor stub
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @return the account
     */
    public String getAccount() {
        return account;
    }

    /**
     * @return the created
     */
    public String getCreated() {
        return created;
    }

    /**
     * @return the domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @return the domainId
     */
    public Long getDomainId() {
        return domainId;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

}
