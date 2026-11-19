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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * mTLS is mandatory (SPEC.md section 11).
 *
 * The engine trusts the caller context this plugin asserts. If the plugin
 * ever connected without a client certificate, anything that could reach
 * the engine could claim to be any tenant — so an unconfigured or broken
 * setup has to fail loudly rather than fall back.
 */
class MutualTlsTest {

    @Test
    void refusesWhenNoClientCertificateIsConfigured() {
        EngineException failure = assertThrows(
                EngineException.class, () -> MutualTls.contextFor(null, "pw", "/tmp/ca", "changeit"));

        assertTrue(failure.getMessage().contains("ai.services.engine.tls.client.cert"));
    }

    @Test
    void refusesWhenTheClientCertificateIsBlank() {
        assertThrows(
                EngineException.class, () -> MutualTls.contextFor("   ", "pw", "/tmp/ca", "changeit"));
    }

    @Test
    void refusesWhenNoTrustStoreIsConfigured(@TempDir Path dir) throws IOException {
        Path keystore = Files.createFile(dir.resolve("client.p12"));

        EngineException failure = assertThrows(
                EngineException.class,
                () -> MutualTls.contextFor(keystore.toString(), "pw", "", "changeit"));

        assertTrue(failure.getMessage().contains("ai.services.engine.tls.truststore"));
    }

    @Test
    void refusesWhenTheCertificateFileIsMissing(@TempDir Path dir) {
        EngineException failure = assertThrows(
                EngineException.class,
                () -> MutualTls.contextFor(
                        dir.resolve("absent.p12").toString(), "pw",
                        dir.resolve("ca.p12").toString(), "changeit"));

        assertTrue(failure.getMessage().contains("unreadable"));
    }

    @Test
    void reportsAnUnusableKeyStoreRatherThanConnectingAnyway(@TempDir Path dir)
            throws IOException {
        Path keystore = Files.write(dir.resolve("client.p12"), new byte[] {1, 2, 3});
        Path truststore = Files.write(dir.resolve("ca.p12"), new byte[] {4, 5, 6});

        EngineException failure = assertThrows(
                EngineException.class,
                () -> MutualTls.contextFor(
                        keystore.toString(), "pw", truststore.toString(), "changeit"));

        assertTrue(failure.getMessage().contains("mTLS context"));
    }
}
