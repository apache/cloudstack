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

package org.apache.cloudstack.framework.kms;

/**
 * Defines the purpose/usage scope for cryptographic keys in the KMS system.
 * This enables proper key segregation and prevents key reuse across different contexts.
 */
public enum KeyPurpose {
    /**
     * Keys used for encrypting VM disk volumes (LUKS, encrypted storage)
     */
    VOLUME_ENCRYPTION("volume", "Volume disk encryption keys"),

    /**
     * Keys used for protecting TLS certificate private keys
     */
    TLS_CERT("tls", "TLS certificate private keys"),

    /**
     * Keys used for encrypting configuration secrets and sensitive settings
     */
    CONFIG_SECRET("config", "Configuration secrets");

    private final String name;
    private final String description;

    KeyPurpose(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Convert string name to KeyPurpose enum
     *
     * @param name the string representation of the purpose
     * @return matching KeyPurpose
     * @throws IllegalArgumentException if no matching purpose found
     */
    public static KeyPurpose fromString(String name) {
        for (KeyPurpose purpose : KeyPurpose.values()) {
            if (purpose.getName().equalsIgnoreCase(name)) {
                return purpose;
            }
        }
        throw new IllegalArgumentException("Unknown KeyPurpose: " + name);
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Generate a KEK label with purpose prefix
     *
     * @param customLabel optional custom label suffix
     * @return formatted KEK label (e.g., "volume-kek-v1")
     */
    public String generateKekLabel(String customLabel) {
        return name + "-kek-" + (customLabel != null ? customLabel : "v1");
    }
}

