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
package com.cloud.network.dao;

import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.apache.cloudstack.network.tls.SslCert;
import com.cloud.utils.db.Encrypt;

@Entity
@Table(name = "sslcerts")
public class SslCertVO implements SslCert {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "certificate", length = 16384)
    private String certificate;

    @Column(name = "chain", length = 2097152)
    private String chain;

    @Encrypt
    @Column(name = "key", length = 16384)
    private String key;

    @Encrypt
    @Column(name = "password")
    private String password;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "domain_id")
    long domainId;

    @Column(name = "fingerprint")
    String fingerPrint;

    @Column(name = "name")
    String name;

    public SslCertVO() {
        uuid = UUID.randomUUID().toString();
    }

    public SslCertVO(String cert, String key, String password, String chain, Long accountId, Long domainId, String fingerPrint, String name) {
        certificate = cert;
        this.key = key;
        this.chain = chain;
        this.password = password;
        this.accountId = accountId;
        this.domainId = domainId;
        this.fingerPrint = fingerPrint;
        uuid = UUID.randomUUID().toString();
        this.name = name;
    }

    // Getters
    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public String getCertificate() {
        return certificate;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getChain() {
        return chain;
    }

    @Override
    public long getAccountId() {
        return accountId;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public long getDomainId() {
        return domainId;
    }

    @Override
    public String getFingerPrint() {
        return fingerPrint;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Class<?> getEntityType() {
        return SslCert.class;
    }

}
