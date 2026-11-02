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

package org.apache.cloudstack.dns.vo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;
import org.apache.cloudstack.dns.DnsServer;

import com.cloud.api.query.vo.BaseViewVO;
import com.cloud.utils.StringUtils;

@Entity
@Table(name = "dns_server_view")
public class DnsServerJoinVO extends BaseViewVO implements InternalIdentity, Identity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "provider_type")
    private String providerType;

    @Column(name = "url")
    private String url;

    @Column(name = "port")
    private Integer port;

    @Column(name = "name_servers")
    private String nameServers;

    @Column(name = "is_public")
    private boolean isPublic;

    @Column(name = "public_domain_suffix")
    private String publicDomainSuffix;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private DnsServer.State state;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "domain_name")
    private String domainName;

    @Column(name = "domain_uuid")
    private String domainUuid;

    @Column(name = "domain_path")
    private String domainPath;

    public DnsServerJoinVO() {
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public String getProviderType() {
        return providerType;
    }

    public String getUrl() {
        return url;
    }

    public Integer getPort() {
        return port;
    }

    public List<String> getNameServers() {
        if (StringUtils.isBlank(nameServers)) {
            return Collections.emptyList();
        }
        return Arrays.asList(nameServers.split(","));
    }

    public boolean isPublicServer() {
        return isPublic;
    }

    public String getPublicDomainSuffix() {
        return publicDomainSuffix;
    }

    public DnsServer.State getState() {
        return state;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getDomainPath() {
        return domainPath;
    }

    public String getDomainUuid() {
        return domainUuid;
    }
}
