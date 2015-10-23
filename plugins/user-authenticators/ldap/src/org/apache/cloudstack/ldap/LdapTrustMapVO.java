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
package org.apache.cloudstack.ldap;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "ldap_trust_map")
public class LdapTrustMapVO implements InternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "type")
    private LdapManager.LinkType type;

    @Column(name = "name")
    private String name;

    @Column(name = "domain_id")
    private long domainId;

    @Column(name = "account_type")
    private short accountType;


    public LdapTrustMapVO() {
    }

    public LdapTrustMapVO(long domainId, LdapManager.LinkType type, String name, short accountType) {
        this.domainId = domainId;
        this.type = type;
        this.name = name;
        this.accountType = accountType;
    }

    @Override
    public long getId() {
        return id;
    }

    public LdapManager.LinkType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public long getDomainId() {
        return domainId;
    }

    public short getAccountType() {
        return accountType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        LdapTrustMapVO that = (LdapTrustMapVO) o;

        if (domainId != that.domainId) {
            return false;
        }
        if (accountType != that.accountType) {
            return false;
        }
        if (type != that.type) {
            return false;
        }
        return name.equals(that.name);

    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + (int) (domainId ^ (domainId >>> 32));
        result = 31 * result + (int) accountType;
        return result;
    }
}
