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

package org.apache.cloudstack.saml;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

public class SAMLProviderMetadata {
    private String entityId;
    private String organizationName;
    private String organizationUrl;
    private String contactPersonName;
    private String contactPersonEmail;
    private String ssoUrl;
    private String sloUrl;
    private KeyPair keyPair;
    private X509Certificate signingCertificate;
    private X509Certificate encryptionCertificate;

    public SAMLProviderMetadata() {
    }

    public void setCommonCertificate(X509Certificate certificate) {
        this.signingCertificate = certificate;
        this.encryptionCertificate = certificate;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getContactPersonName() {
        return contactPersonName;
    }

    public void setContactPersonName(String contactPersonName) {
        this.contactPersonName = contactPersonName;
    }

    public String getContactPersonEmail() {
        return contactPersonEmail;
    }

    public void setContactPersonEmail(String contactPersonEmail) {
        this.contactPersonEmail = contactPersonEmail;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getOrganizationUrl() {
        return organizationUrl;
    }

    public void setOrganizationUrl(String organizationUrl) {
        this.organizationUrl = organizationUrl;
    }

    public KeyPair getKeyPair() {
        return keyPair;
    }

    public void setKeyPair(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public X509Certificate getSigningCertificate() {
        return signingCertificate;
    }

    public void setSigningCertificate(X509Certificate signingCertificate) {
        this.signingCertificate = signingCertificate;
    }

    public X509Certificate getEncryptionCertificate() {
        return encryptionCertificate;
    }

    public void setEncryptionCertificate(X509Certificate encryptionCertificate) {
        this.encryptionCertificate = encryptionCertificate;
    }

    public String getSsoUrl() {
        return ssoUrl;
    }

    public void setSsoUrl(String ssoUrl) {
        this.ssoUrl = ssoUrl;
    }

    public String getSloUrl() {
        return sloUrl;
    }

    public void setSloUrl(String sloUrl) {
        this.sloUrl = sloUrl;
    }
}
