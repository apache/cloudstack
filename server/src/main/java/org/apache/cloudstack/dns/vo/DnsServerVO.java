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

import com.cloud.utils.db.Encrypt;
import com.cloud.utils.db.GenericDao;

@Entity
@Table(name = "dns_server")
public class DnsServerVO implements DnsServer {
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

    @Encrypt
    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "is_public")
    private boolean isPublic;

    @Column(name = "public_domain_suffix")
    private String publicDomainSuffix;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    private State state;

    @Column(name = "account_id")
    private long accountId;

    @Column(name = "name_servers")
    private String nameServers;

    @Column(name = GenericDao.CREATED_COLUMN)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date created = null;

    @Column(name = GenericDao.REMOVED_COLUMN)
    @Temporal(value = TemporalType.TIMESTAMP)
    private Date removed = null;

    DnsServerVO() {
        this.uuid = UUID.randomUUID().toString();
        this.created = new Date();
    }

    public DnsServerVO(String name, String url, DnsProviderType providerType, String apiKey,
                       Integer port, boolean isPublic, String publicDomainSuffix, List<String> nameServers,
                       long accountId) {
        this();
        this.name = name;
        this.url = url;
        this.port = port;
        this.providerType = providerType;
        this.apiKey = apiKey;
        this.accountId = accountId;
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

    public String getNameServers() {
        return nameServers;
    }
}
