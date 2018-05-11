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

package com.cloud.certificate;

import java.math.BigInteger;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.cloudstack.api.InternalIdentity;

@Entity
@Table(name = "crl")
public class CrlVO implements InternalIdentity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id = null;

    @Column(name = "serial")
    private String certSerial;

    @Column(name = "cn")
    private String certCn;

    @Column(name = "revoker_uuid")
    private String revokerUuid;

    @Temporal(value = TemporalType.TIMESTAMP)
    @Column(name = "revoked", updatable = true)
    private Date revoked;

    public CrlVO() {
    }

    public CrlVO(final BigInteger certSerial, final String certCn, final String revokerUuid) {
        this.certSerial = certSerial.toString(16);
        this.certCn = certCn;
        this.revokerUuid = revokerUuid;
        this.revoked = new Date();
    }

    @Override
    public long getId() {
        return id;
    }

    public BigInteger getCertSerial() {
        return new BigInteger(certSerial, 16);
    }

    public String getCertCn() {
        return certCn;
    }

    public String getRevokerUuid() {
        return revokerUuid;
    }

    public Date getRevoked() {
        return revoked;
    }
}
