//
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
//

package org.apache.cloudstack.utils.security;

import java.io.File;

import com.cloud.utils.PropertiesUtil;

public class KeyStoreUtils {
    public static final String KS_SETUP_SCRIPT = "keystore-setup";
    public static final String KS_IMPORT_SCRIPT = "keystore-cert-import";
    public static final String KS_SYSTEMVM_IMPORT_SCRIPT = "keystore-cert-import-sysvm";

    public static final String AGENT_PROPSFILE = "agent.properties";
    public static final String KS_PASSPHRASE_PROPERTY = "keystore.passphrase";

    public static final String KS_FILENAME = "cloud.jks";
    public static final char[] DEFAULT_KS_PASSPHRASE = "vmops.com".toCharArray();

    public static final String CACERT_FILENAME = "cloud.ca.crt";
    public static final String CERT_FILENAME = "cloud.crt";
    public static final String CSR_FILENAME = "cloud.csr";
    public static final String PKEY_FILENAME = "cloud.key";

    public static final String CERT_NEWLINE_ENCODER = "^";
    public static final String CERT_SPACE_ENCODER = "~";

    public static final String SSH_MODE = "ssh";
    public static final String AGENT_MODE = "agent";
    public static final String SECURED = "secured";

    public static boolean isHostSecured() {
        final File confFile = PropertiesUtil.findConfigFile("agent.properties");
        return confFile != null && confFile.exists() && new File(confFile.getParent() + "/" + CERT_FILENAME).exists();
    }
}
