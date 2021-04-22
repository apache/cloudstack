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

import java.util.Map;

import com.cloud.domain.Domain;
import com.cloud.utils.net.Ip;

/**
 * @author Khosrow Moossavi
 * @since 4.12.0.0
 */
public class PkiEngineDefault implements PkiEngine {
    public PkiEngineDefault(Map<String, String> configs) {
    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.pki.PkiEngine#issueCertificate(com.cloud.domain.Domain, com.cloud.utils.net.Ip)
     */
    @Override
    public PkiDetail issueCertificate(Domain domain, Ip publicIp) throws Exception {
        throw new UnsupportedOperationException("Cannot issue certificate with Default implementation, use Vault instead.");
    }

    /* (non-Javadoc)
     * @see org.apache.cloudstack.pki.PkiEngine#getCertificate(com.cloud.domain.Domain)
     */
    @Override
    public PkiDetail getCertificate(Domain domain) {
        throw new UnsupportedOperationException("Cannot get certificate with Default implementation, use Vault instead.");
    }
}
