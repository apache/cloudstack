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
import org.apache.cloudstack.dns.DnsZone;

import com.cloud.api.query.vo.BaseViewVO;

@Entity
@Table(name = "dns_zone_view")
public class DnsZoneJoinVO extends BaseViewVO implements InternalIdentity, Identity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "state")
    @Enumerated(value = EnumType.STRING)
    private DnsZone.State state;

    @Column(name = "dns_server_uuid")
    private String dnsServerUuid;

    @Column(name = "dns_server_name")
    private String dnsServerName;

    @Column(name = "dns_server_account_name")
    private String dnsServerAccountName;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "domain_name")
    private String domainName;

    @Column(name = "domain_uuid")
    private String domainUuid;

    @Column(name = "domain_path")
    private String domainPath;

    @Column(name = "description")
    private String description;

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public DnsZone.State getState() {
        return state;
    }

    public String getDnsServerUuid() {
        return dnsServerUuid;
    }

    public String getDnsServerName() {
        return dnsServerName;
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

    public String getName() {
        return name;
    }

    public String getDnsServerAccountName() {
        return dnsServerAccountName;
    }

    public String getDomainUuid() {
        return domainUuid;
    }

    public String getDescription() {
        return description;
    }

}
