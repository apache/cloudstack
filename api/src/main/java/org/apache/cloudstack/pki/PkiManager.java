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
import com.cloud.exception.RemoteAccessVpnException;
import com.cloud.utils.net.Ip;

/**
 * @author Khosrow Moossavi
 * @since 4.12.0.0
 */
public interface PkiManager {
    String CREDENTIAL_ISSUING_CA = "credential.issuing.ca";
    String CREDENTIAL_SERIAL_NUMBER = "credential.serial.number";
    String CREDENTIAL_CERTIFICATE = "credential.certificate";
    String CREDENTIAL_PRIVATE_KEY = "credential.private.key";

    /**
     * Issue a Certificate for specific IP and specific Domain act as the CA
     *
     * @param domain object to extract name and id to be used to issuing CA
     * @param publicIp to be included in the certificate
     *
     * @return detail about just signed PKI, including issuing CA, certificate, private key and serial number
     *
     * @throws RemoteAccessVpnException
     */
    PkiDetail issueCertificate(Domain domain, Ip publicIp) throws RemoteAccessVpnException;

    /**
     * Get a Certificate for specific Domain act as the CA
     *
     * @param domain object to extract its id to be find the issuing CA
     *
     * @return details about signed PKI, including issuing CA, certificate and serial number
     *
     * @throws RemoteAccessVpnException
     */
    PkiDetail getCertificate(Domain domain) throws RemoteAccessVpnException;
}
