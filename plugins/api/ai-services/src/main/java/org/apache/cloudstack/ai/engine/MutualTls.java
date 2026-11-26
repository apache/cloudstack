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

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/** Builds the SSLContext the plugin presents to the engine. */
public final class MutualTls {

    private MutualTls() {
    }

    /**
     * @throws EngineException when mTLS is not fully configured. Refusing
     *         to start beats quietly connecting without a client
     *         certificate, which would let the engine trust an
     *         unauthenticated caller's asserted identity.
     */
    public static SSLContext contextFor(
            String keyStorePath,
            String keyStorePassword,
            String trustStorePath,
            String trustStorePassword) {
        requireConfigured(keyStorePath, "ai.services.engine.tls.client.cert");
        requireConfigured(trustStorePath, "ai.services.engine.tls.truststore");

        try {
            KeyStore keyStore = load(keyStorePath, keyStorePassword);
            KeyManagerFactory keyManagers =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagers.init(keyStore, charsOf(keyStorePassword));

            // The truststore needs its own password. It was read with
            // an empty one, which no operator could produce: keytool
            // refuses a password under six characters, and a PKCS12
            // store openssl writes with an empty password loads as
            // *zero entries* rather than failing.
            KeyStore trustStore = load(trustStorePath, trustStorePassword);
            if (trustStore.size() == 0) {
                // TrustManagerFactory would accept this and build a
                // trust manager that trusts nothing, so every call
                // would fail at the handshake with an error naming
                // neither the truststore nor the CA.
                throw new EngineException(
                        "the trust store at " + trustStorePath + " holds no certificates, "
                                + "so the plugin would trust no engine. Import the engine's "
                                + "CA into it: keytool -importcert -alias aisvc-ca -file "
                                + "ca.pem -keystore " + trustStorePath + " -storetype PKCS12");
            }
            TrustManagerFactory trustManagers =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagers.init(trustStore);

            SSLContext context = SSLContext.getInstance("TLSv1.3");
            context.init(
                    keyManagers.getKeyManagers(),
                    trustManagers.getTrustManagers(),
                    new SecureRandom());
            return context;
        } catch (EngineException passthrough) {
            throw passthrough;
        } catch (Exception failure) {
            String hint = "";
            String message = failure.getMessage() == null ? "" : failure.getMessage();
            // JDK 17 reports a wrong store password two different ways
            // depending on where it notices — "keystore password was
            // incorrect" and "Integrity check failed" — and neither
            // says which of the two stores it was reading.
            if (message.contains("password was incorrect")
                    || message.contains("Integrity check failed")) {
                // The likeliest cause by a distance, and the raw
                // message does not say which of the two stores it was.
                hint = " — check ai.services.engine.tls.client.key and "
                        + "ai.services.engine.tls.truststore.password";
            }
            throw new EngineException(
                    "could not build the mTLS context for the AI Services engine: "
                            + failure.getMessage() + hint,
                    failure);
        }
    }

    private static void requireConfigured(String value, String setting) {
        if (value == null || value.trim().isEmpty()) {
            throw new EngineException(
                    "mTLS is required to call the AI Services engine; set " + setting);
        }
        if (!Files.isReadable(Paths.get(value))) {
            throw new EngineException(setting + " points at an unreadable file: " + value);
        }
    }

    private static KeyStore load(String path, String password) throws Exception {
        KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream in = new FileInputStream(path)) {
            store.load(in, charsOf(password));
        }
        return store;
    }

    private static char[] charsOf(String password) {
        return password == null ? new char[0] : password.toCharArray();
    }
}
