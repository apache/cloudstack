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

import com.cloud.domain.Domain;
import com.cloud.utils.net.Ip;

/**
 * @author Khosrow Moossavi
 * @since 4.12.0.0
 */
public interface PkiEngine {
    /**
     * Issue a Certificate for specific IP and specific Domain act as the CA. This will have two
     * known implementation, {@link PkiEngineDefault} and {@link PkiEngineVault}. Vault implementation
     * will delegate everything CA-related to Vault to process it, while Default will assume the
     * CA-related actions will be done within the scope of the same application.
     *
     * @param domain object to extract name and id to be used to issuing CA
     * @param publicIp to be included in the certificate
     *
     * @return details about the just signed PKI, including issuing CA, certificate, private key and serial number
     *
     * @throws Exception
     */
    PkiDetail issueCertificate(Domain domain, Ip publicIp) throws Exception;

    /**
     * Get a Certificate for specific Domain act as the CA
     *
     * @param domain object to extract its id to be find the issuing CA
     *
     * @return details about signed PKI, including issuing CA, certificate and serial number
     *
     * @throws Exception
     */
    PkiDetail getCertificate(Domain domain) throws Exception;
}
