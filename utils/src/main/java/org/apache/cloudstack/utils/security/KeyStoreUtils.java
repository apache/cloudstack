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
import java.io.IOException;

import com.cloud.utils.script.Script;
import com.google.common.base.Strings;

public class KeyStoreUtils {

    public static String defaultTmpKeyStoreFile = "/tmp/tmp.jks";
    public static String defaultKeystoreFile = "/cloud.jks";
    public static String defaultPrivateKeyFile = "/cloud.key";
    public static String defaultCsrFile = "/cloud.csr";
    public static String defaultCertFile = "/cloud.crt";
    public static String defaultCaCertFile = "/cloud.ca.crt";
    public static char[] defaultKeystorePassphrase = "vmops.com".toCharArray();

    public static String certNewlineEncoder = "^";
    public static String certSpaceEncoder = "~";

    public static String keyStoreSetupScript = "keystore-setup";
    public static String keyStoreImportScript = "keystore-cert-import";
    public static String passphrasePropertyName = "keystore.passphrase";

    public static String sshMode = "ssh";
    public static String agentMode = "agent";

    public static void copyKeystore(final String keystorePath, final String tmpKeystorePath) throws IOException {
        if (Strings.isNullOrEmpty(keystorePath) || Strings.isNullOrEmpty(tmpKeystorePath)) {
            throw new IOException("Invalid keystore path provided");
        }
        try {
            final Script script = new Script(true, "cp", 5000, null);
            script.add("-f");
            script.add(tmpKeystorePath);
            script.add(keystorePath);
            final String result = script.execute();
            if (result != null) {
                throw new IOException("Failed to execute cp to copy keystore file to mgmt server conf location");
            }
        } catch (final Exception e) {
            throw new IOException("Failed to create keystore file: " + keystorePath, e);
        }
        try {
            new File(tmpKeystorePath).delete();
        } catch (Exception ignored) {
        }
    }

}
