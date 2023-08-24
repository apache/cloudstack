/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.api.response;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

public class LinkDomainToLdapResponse extends BaseResponse {

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "id of the Domain which is linked to LDAP")
    private String domainId;

    @Deprecated
    @SerializedName(ApiConstants.NAME)
    @Param(description = "name of the group or OU in LDAP which is linked to the domain")
    private String name;

    @SerializedName(ApiConstants.LDAP_DOMAIN)
    @Param(description = "name of the group or OU in LDAP which is linked to the domain")
    private String ldapDomain;

    @SerializedName(ApiConstants.TYPE)
    @Param(description = "type of the name in LDAP which is linke to the domain")
    private String type;

    @SerializedName(ApiConstants.ACCOUNT_TYPE)
    @Param(description = "Type of the account to auto import")
    private int accountType;

    @SerializedName(ApiConstants.ACCOUNT_ID)
    @Param(description = "Domain Admin accountId that is created")
    private String adminId;

    public LinkDomainToLdapResponse(String domainId, String type, String ldapDomain, int accountType) {
        this.domainId = domainId;
        this.name = ldapDomain;
        this.ldapDomain = ldapDomain;
        this.type = type;
        this.accountType = accountType;
    }

    public String getDomainId() {
        return domainId;
    }

    public String getLdapDomain() {
        return ldapDomain == null ? name : ldapDomain;
    }

    public String getType() {
        return type;
    }

    public int getAccountType() {
        return accountType;
    }

    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }
}
