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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateParsingException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

public interface CAService {
    /**
     * Returns a SSLEngine to be used for handling client connections
     * @param context
     * @param remoteAddress
     * @return
     * @throws GeneralSecurityException
     * @throws IOException
     */
    SSLEngine createSSLEngine(final SSLContext context, final String remoteAddress) throws GeneralSecurityException, IOException;

    /**
     * Returns the management server keystore used to connect to peers
     * @return returns KeyStore instance
     */
    KeyStore getManagementKeyStore() throws KeyStoreException;

    /**
     * Returns the keystore passphrase to use
     * @return returns char[] passphrase
     */
    char[] getKeyStorePassphrase();

    boolean isManagementCertificate(java.security.cert.Certificate certificate) throws CertificateParsingException;
}
