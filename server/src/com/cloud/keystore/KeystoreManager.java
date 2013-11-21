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
package com.cloud.keystore;

import com.cloud.agent.api.SecStorageSetupCommand.Certificates;
import com.cloud.utils.component.Manager;

public interface KeystoreManager extends Manager {
    boolean validateCertificate(String certificate, String key, String domainSuffix);

    void saveCertificate(String name, String certificate, String key, String domainSuffix);

    byte[] getKeystoreBits(String name, String aliasForCertificateInStore, String storePassword);

    void saveCertificate(String name, String certificate, Integer index, String domainSuffix);

    Certificates getCertificates(String name);
}
