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
package org.apache.cloudstack.pki;

/**
 * @author Khosrow Moossavi
 * @since 4.12.0.0
 */
public class PkiDetail {
    private String certificate;
    private String issuingCa;
    private String privateKey;
    private String privateKeyType;
    private String serialNumber;

    public PkiDetail certificate(final String certificate) {
        this.certificate = certificate;
        return this;
    }

    public PkiDetail issuingCa(final String issuingCa) {
        this.issuingCa = issuingCa;
        return this;
    }

    public PkiDetail privateKey(final String privateKey) {
        this.privateKey = privateKey;
        return this;
    }

    public PkiDetail privateKeyType(final String privateKeyType) {
        this.privateKeyType = privateKeyType;
        return this;
    }

    public PkiDetail serialNumber(final String serialNumber) {
        this.serialNumber = serialNumber;
        return this;
    }

    public String getCertificate() {
        return certificate;
    }

    public String getIssuingCa() {
        return issuingCa;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPrivateKeyType() {
        return privateKeyType;
    }

    public String getSerialNumber() {
        return serialNumber;
    }
}