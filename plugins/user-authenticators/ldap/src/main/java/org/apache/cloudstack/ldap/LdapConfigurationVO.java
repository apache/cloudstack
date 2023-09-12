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
package org.apache.cloudstack.ldap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "ldap_configuration")
public class LdapConfigurationVO implements InternalIdentity {
    @Column(name = "hostname")
    private String hostname;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "port")
    private int port;

    @Column(name = "domain_id")
    private Long domainId;

    public LdapConfigurationVO() {
    }

    public LdapConfigurationVO(final String hostname, final int port, final Long domainId) {
        this.hostname = hostname;
        this.port = port;
        this.domainId = domainId;
    }

    public String getHostname() {
        return hostname;
    }

    @Override
    public long getId() {
        return id;
    }

    public int getPort() {
        return port;
    }

    public Long getDomainId() {
        return domainId;
    }

    public void setId(final long id) {
        this.id = id;
    }
}
