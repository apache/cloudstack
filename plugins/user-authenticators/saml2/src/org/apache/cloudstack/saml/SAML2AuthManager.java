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

import org.apache.cloudstack.api.auth.PluggableAPIAuthenticator;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

public interface SAML2AuthManager extends PluggableAPIAuthenticator {
    public String getServiceProviderId();
    public String getIdentityProviderId();

    public X509Certificate getIdpSigningKey();
    public X509Certificate getIdpEncryptionKey();
    public X509Certificate getSpX509Certificate();
    public KeyPair getSpKeyPair();

    public String getSpSingleSignOnUrl();
    public String getIdpSingleSignOnUrl();

    public String getSpSingleLogOutUrl();
    public String getIdpSingleLogOutUrl();
}
