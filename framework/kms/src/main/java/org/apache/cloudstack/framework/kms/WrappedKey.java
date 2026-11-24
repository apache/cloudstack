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

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

/**
 * Immutable Data Transfer Object representing an encrypted (wrapped) Data Encryption Key.
 * The wrapped key material contains the DEK encrypted by a Key Encryption Key (KEK)
 * stored in a secure backend (HSM, database, etc.).
 * <p>
 * This follows the envelope encryption pattern:
 * - DEK: encrypts actual data (e.g., disk volume)
 * - KEK: encrypts the DEK (never leaves secure storage)
 * - Wrapped Key: DEK encrypted by KEK, safe to store in database
 */
public class WrappedKey {
    private final String uuid;
    private final String kekId;
    private final KeyPurpose purpose;
    private final String algorithm;
    private final byte[] wrappedKeyMaterial;
    private final String providerName;
    private final Date created;
    private final Long zoneId;

    /**
     * Create a new WrappedKey instance
     *
     * @param kekId              ID/label of the KEK used to wrap this key
     * @param purpose            the intended use of this key
     * @param algorithm          encryption algorithm (e.g., "AES/GCM/NoPadding")
     * @param wrappedKeyMaterial the encrypted DEK blob
     * @param providerName       name of the KMS provider that created this key
     * @param created            timestamp when key was wrapped
     * @param zoneId             optional zone ID for zone-scoped keys
     */
    public WrappedKey(String kekId, KeyPurpose purpose, String algorithm,
            byte[] wrappedKeyMaterial, String providerName,
            Date created, Long zoneId) {
        this(null, kekId, purpose, algorithm, wrappedKeyMaterial, providerName, created, zoneId);
    }

    /**
     * Constructor for database-loaded keys with ID
     */
    public WrappedKey(String uuid, String kekId, KeyPurpose purpose, String algorithm,
            byte[] wrappedKeyMaterial, String providerName,
            Date created, Long zoneId) {
        this.uuid = uuid;
        this.kekId = Objects.requireNonNull(kekId, "kekId cannot be null");
        this.purpose = Objects.requireNonNull(purpose, "purpose cannot be null");
        this.algorithm = Objects.requireNonNull(algorithm, "algorithm cannot be null");
        this.providerName = providerName;

        if (wrappedKeyMaterial == null || wrappedKeyMaterial.length == 0) {
            throw new IllegalArgumentException("wrappedKeyMaterial cannot be null or empty");
        }
        this.wrappedKeyMaterial = Arrays.copyOf(wrappedKeyMaterial, wrappedKeyMaterial.length);

        this.created = created != null ? new Date(created.getTime()) : new Date();
        this.zoneId = zoneId;
    }

    public String getUuid() {
        return uuid;
    }

    public String getKekId() {
        return kekId;
    }

    public KeyPurpose getPurpose() {
        return purpose;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Get wrapped key material. Returns a defensive copy to prevent modification.
     * Caller is responsible for zeroizing the returned array after use.
     */
    public byte[] getWrappedKeyMaterial() {
        return Arrays.copyOf(wrappedKeyMaterial, wrappedKeyMaterial.length);
    }

    public String getProviderName() {
        return providerName;
    }

    public Date getCreated() {
        return created != null ? new Date(created.getTime()) : null;
    }

    public Long getZoneId() {
        return zoneId;
    }

    @Override
    public String toString() {
        return "WrappedKey{" +
               "uuid='" + uuid + '\'' +
               ", kekId='" + kekId + '\'' +
               ", purpose=" + purpose +
               ", algorithm='" + algorithm + '\'' +
               ", providerName='" + providerName + '\'' +
               ", materialLength=" + (wrappedKeyMaterial != null ? wrappedKeyMaterial.length : 0) +
               ", created=" + created +
               ", zoneId=" + zoneId +
               '}';
    }
}
