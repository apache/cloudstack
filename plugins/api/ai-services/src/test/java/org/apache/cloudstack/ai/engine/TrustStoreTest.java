// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements. See the NOTICE file distributed with this
// work for additional information regarding copyright ownership. The ASF
// licenses this file to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance with the
// License. You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.ai.engine;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.KeyStore;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The engine's CA, as the plugin reads it.
 *
 * <p>The truststore was loaded with an empty password, which no
 * operator could satisfy. A PKCS12 store written by openssl with an
 * empty password loads as **zero entries** rather than failing, and
 * `TrustManagerFactory` then builds a trust manager that trusts
 * nothing — so every call to the engine fails at the handshake with an
 * error naming neither the truststore nor the CA. keytool cannot write
 * an empty-password store at all: it requires six characters.
 *
 * <p>So the password is configurable, and an empty truststore is an
 * error naming the file rather than a context that refuses everything.
 */
class TrustStoreTest {

    private static Path emptyStore(Path dir, String password) throws Exception {
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        Path path = dir.resolve("empty.p12");
        try (OutputStream out = new FileOutputStream(path.toFile())) {
            store.store(out, password.toCharArray());
        }
        return path;
    }

    @Test
    void anEmptyTrustStoreIsRefusedRatherThanTrustingNothing(@TempDir Path dir)
            throws Exception {
        // The silent case. Without this the operator sees a handshake
        // failure that says nothing about the truststore.
        Path store = emptyStore(dir, "changeit");

        EngineException thrown = assertThrows(
                EngineException.class,
                () -> MutualTls.contextFor(
                        store.toString(), "changeit", store.toString(), "changeit"));

        assertTrue(thrown.getMessage().contains("no certificates"),
                thrown.getMessage());
    }

    @Test
    void theTrustStorePasswordIsUsed(@TempDir Path dir) throws Exception {
        // Loading with the wrong password throws before anything can
        // read an entry, so the message must point at the password.
        Path store = emptyStore(dir, "changeit");

        EngineException thrown = assertThrows(
                EngineException.class,
                () -> MutualTls.contextFor(
                        store.toString(), "changeit", store.toString(), "wrong"));

        assertTrue(thrown.getMessage().toLowerCase().contains("password"),
                thrown.getMessage());
    }
}
