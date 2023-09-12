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

import com.google.gson.annotations.SerializedName;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import org.apache.cloudstack.api.LdapConstants;

public class LdapUserResponse extends BaseResponse {
    @SerializedName(ApiConstants.EMAIL)
    @Param(description = "The user's email")
    private String email;

    @SerializedName(LdapConstants.PRINCIPAL)
    @Param(description = "The user's principle")
    private String principal;

    @SerializedName(ApiConstants.FIRSTNAME)
    @Param(description = "The user's firstname")
    private String firstname;

    @SerializedName(ApiConstants.LASTNAME)
    @Param(description = "The user's lastname")
    private String lastname;

    @SerializedName(ApiConstants.USERNAME)
    @Param(description = "The user's username")
    private String username;

    @SerializedName(ApiConstants.DOMAIN)
    @Param(description = "The user's domain")
    private String domain;

    @SerializedName(ApiConstants.USER_CONFLICT_SOURCE)
    @Param(description = "The authentication source for this user as known to the system or empty if the user is not yet in cloudstack.")
    private String userSource;

    public LdapUserResponse() {
        super();
    }

    public LdapUserResponse(final String username, final String email, final String firstname, final String lastname, final String principal, String domain) {
        super();
        this.username = username;
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
        this.principal = principal;
        this.domain = domain;
    }

    public LdapUserResponse(final String username, final String email, final String firstname, final String lastname, final String principal, String domain, String userSource) {
        this(username, email, firstname, lastname, principal, domain);
        setUserSource(userSource);
    }

    public String getEmail() {
        return email;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getPrincipal() {
        return principal;
    }

    public String getUsername() {
        return username;
    }

    public String getDomain() {
        return domain;
    }

    public String getUserSource() {
        return userSource;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public void setFirstname(final String firstname) {
        this.firstname = firstname;
    }

    public void setLastname(final String lastname) {
        this.lastname = lastname;
    }

    public void setPrincipal(final String principal) {
        this.principal = principal;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setUserSource(String userSource) {
        this.userSource = userSource;
    }

    public String toString() {
        final String COLUMN = ": ";
        final String COMMA = ", ";
        StringBuilder selfRepresentation = new StringBuilder();
        selfRepresentation.append(this.getClass().getName());
        selfRepresentation.append('{');
        boolean hascontent = false;
        if (this.getUsername() != null) {
            selfRepresentation.append(ApiConstants.USERNAME);
            selfRepresentation.append(COLUMN);
            selfRepresentation.append(this.getUsername());
            hascontent = true;
        }
        if (this.getFirstname() != null) {
            if(hascontent) selfRepresentation.append(COMMA);
            selfRepresentation.append(ApiConstants.FIRSTNAME);
            selfRepresentation.append(COLUMN);
            selfRepresentation.append(this.getFirstname());
            hascontent = true;
        }
        if (this.getLastname() != null) {
            if(hascontent) selfRepresentation.append(COMMA);
            selfRepresentation.append(ApiConstants.LASTNAME);
            selfRepresentation.append(COLUMN);
            selfRepresentation.append(this.getLastname());
            hascontent = true;
        }
        if(this.getDomain() != null) {
            if(hascontent) selfRepresentation.append(COMMA);
            selfRepresentation.append(ApiConstants.DOMAIN);
            selfRepresentation.append(COLUMN);
            selfRepresentation.append(this.getDomain());
            hascontent = true;
        }
        if (this.getEmail() != null) {
            if(hascontent) selfRepresentation.append(COMMA);
            selfRepresentation.append(ApiConstants.EMAIL);
            selfRepresentation.append(COLUMN);
            selfRepresentation.append(this.getEmail());
            hascontent = true;
        }
        if (this.getPrincipal() != null) {
            if(hascontent) selfRepresentation.append(COMMA);
            selfRepresentation.append(LdapConstants.PRINCIPAL);
            selfRepresentation.append(COLUMN);
            selfRepresentation.append(this.getPrincipal());
            hascontent = true;
        }
        if (this.getUserSource() != null) {
            if (hascontent) selfRepresentation.append(COMMA);
            selfRepresentation.append(ApiConstants.USER_CONFLICT_SOURCE);
            selfRepresentation.append(COLUMN);
            selfRepresentation.append(this.getUserSource());
        }
        selfRepresentation.append('}');

        return selfRepresentation.toString();
    }
}
