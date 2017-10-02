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

package org.apache.cloudstack.framework.ca;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

public class Certificate {
    private X509Certificate clientCertificate;
    private PrivateKey privateKey;
    private List<X509Certificate> caCertificates;

    public Certificate(final X509Certificate clientCertificate, final PrivateKey privateKey, final List<X509Certificate> caCertificates) {
        this.clientCertificate = clientCertificate;
        this.privateKey = privateKey;
        this.caCertificates = caCertificates;
    }

    public X509Certificate getClientCertificate() {
        return clientCertificate;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public List<X509Certificate> getCaCertificates() {
        return caCertificates;
    }
}
