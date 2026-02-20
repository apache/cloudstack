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

package org.apache.cloudstack.dns.vo;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.dns.DnsProviderType;
import org.apache.cloudstack.dns.DnsServer;
import org.apache.cloudstack.dns.DnsZone;

import com.cloud.utils.StringUtils;
import com.cloud.utils.db.Encrypt;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "dns_server")
public class    DnsServerVO implements DnsServer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "url")
    private String url;

    @Column(name = "port")
    private Integer port;

    @Column(name = "provider_type")
    @Enumerated(EnumType.STRING)
    private DnsProviderType providerType;

    @Column(name = "dns_user_name")
    private String dnsUserName;

    @Encrypt
    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "external_server_id")
    private String externalServerId;

    @Column(name = "is_public")
    private boolean isPublic;

    @Column(name = "public_domain_suffix")
    private String publicDomainSuffix;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private State state;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "name_servers")
    private String nameServers;

    @Column(name = GenericDao.CREATED_COLUMN)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed = null;

    DnsServerVO() {
        this.uuid = UUID.randomUUID().toString();
        this.created = new Date();
    }

    public DnsServerVO(String name, String url, Integer port, String externalServerId, DnsProviderType providerType, String dnsUserName, String apiKey,
                       boolean isPublic, String publicDomainSuffix, List<String> nameServers, Long accountId, Long domainId) {
        this();
        this.name = name;
        this.url = url;
        this.port = port;
        this.externalServerId = externalServerId;
        this.providerType = providerType;
        this.dnsUserName = dnsUserName;
        this.apiKey = apiKey;
        this.accountId = accountId;
        this.domainId = domainId;
        this.publicDomainSuffix = publicDomainSuffix;
        this.isPublic = isPublic;
        this.state = State.Enabled;
        this.nameServers = String.join(",", nameServers);;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Class<?> getEntityType() {
        return DnsZone.class;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public DnsProviderType getProviderType() {
        return providerType;
    }

    @Override
    public String getApiKey() {
        return apiKey;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "DnsServerVO {" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", apiKey='*****'" +
                "}";
    }

    public void setNameServers(String nameServers) {
        this.nameServers = nameServers;
    }

    public void setIsPublic(boolean value) {
        isPublic = value;
    }

    public void setPublicDomainSuffix(String publicDomainSuffix) {
        this.publicDomainSuffix = publicDomainSuffix;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getNameServers() {
        if (StringUtils.isBlank(nameServers)) {
            return Collections.emptyList();
        }
        return Arrays.asList(nameServers.split(","));
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    public String getPublicDomainSuffix() {
        return publicDomainSuffix;
    }

    public String getExternalServerId() {
        return externalServerId;
    }

    public void setExternalServerId(String externalServerId) {
        this.externalServerId = externalServerId;
    }

    public Integer getPort() {
        return this.port;
    }
}
