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
import org.apache.cloudstack.api.EntityReference;
import org.apache.cloudstack.ldap.LdapConfiguration;

@EntityReference(value = LdapConfiguration.class)
public class LdapConfigurationResponse extends BaseResponse {
    @SerializedName(ApiConstants.HOST_NAME)
    @Param(description = "name of the host running the ldap server")
    private String hostname;

    @SerializedName(ApiConstants.PORT)
    @Param(description = "port teh ldap server is running on")
    private int port;

    @SerializedName(ApiConstants.DOMAIN_ID)
    @Param(description = "linked domain")
    private String domainId;

    public LdapConfigurationResponse() {
        super();
    }

    public LdapConfigurationResponse(final String hostname) {
        super();
        setHostname(hostname);
    }

    public LdapConfigurationResponse(final String hostname, final int port) {
        this(hostname);
        setPort(port);
    }

    public LdapConfigurationResponse(final String hostname, final int port, final String domainId) {
        this(hostname, port);
        setDomainId(domainId);
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }
}
