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

import com.cloud.utils.component.Adapter;
import org.apache.cloudstack.framework.config.Configurable;

/**
 * Abstract provider contract for Key Management Service operations.
 * <p>
 * Implementations provide the cryptographic backend (HSM via PKCS#11, database, cloud KMS, etc.)
 * for secure key wrapping/unwrapping using envelope encryption.
 * <p>
 * Design principles:
 * - KEKs (Key Encryption Keys) never leave the secure backend
 * - DEKs (Data Encryption Keys) are wrapped by KEKs for storage
 * - Plaintext DEKs exist only transiently in memory during wrap/unwrap
 * - All operations are purpose-scoped to prevent key reuse
 * <p>
 * Thread-safety: Implementations must be thread-safe for concurrent operations.
 */
public interface KMSProvider extends Configurable, Adapter {

    /**
     * Get the unique name of this provider
     *
     * @return provider name (e.g., "database", "pkcs11")
     */
    String getProviderName();

    /**
     * Create a new Key Encryption Key (KEK) in the secure backend with explicit HSM profile.
     *
     * @param purpose      the purpose/scope for this KEK
     * @param label        human-readable label for the KEK (must be unique within purpose)
     * @param keyBits      key size in bits (typically 128, 192, or 256)
     * @param hsmProfileId optional HSM profile ID to create the KEK in (null for auto-resolution/default)
     * @return the KEK identifier (label or handle) for later reference
     * @throws KMSException if KEK creation fails
     */
    String createKek(KeyPurpose purpose, String label, int keyBits, Long hsmProfileId) throws KMSException;

    /**
     * Create a new Key Encryption Key (KEK) in the secure backend.
     * Delegates to {@link #createKek(KeyPurpose, String, int, Long)} with null profile ID.
     *
     * @param purpose the purpose/scope for this KEK
     * @param label   human-readable label for the KEK (must be unique within purpose)
     * @param keyBits key size in bits (typically 128, 192, or 256)
     * @return the KEK identifier (label or handle) for later reference
     * @throws KMSException if KEK creation fails
     */
    default String createKek(KeyPurpose purpose, String label, int keyBits) throws KMSException {
        return createKek(purpose, label, keyBits, null);
    }

    /**
     * Delete a KEK from the secure backend.
     * WARNING: This will make all DEKs wrapped by this KEK unrecoverable.
     *
     * @param kekId the KEK identifier to delete
     * @throws KMSException if deletion fails or KEK not found
     */
    void deleteKek(String kekId) throws KMSException;


    /**
     * Check if a KEK exists and is accessible
     *
     * @param kekId the KEK identifier to check
     * @return true if KEK is available
     * @throws KMSException if check fails
     */
    boolean isKekAvailable(String kekId) throws KMSException;

    /**
     * Wrap (encrypt) a plaintext Data Encryption Key with a KEK using explicit HSM profile.
     *
     * @param plainDek     the plaintext DEK to wrap (caller must zeroize after call)
     * @param purpose      the intended purpose of this DEK
     * @param kekLabel     the label of the KEK to use for wrapping
     * @param hsmProfileId optional HSM profile ID to use (null for auto-resolution/default)
     * @return WrappedKey containing the encrypted DEK and metadata
     * @throws KMSException if wrapping fails or KEK not found
     */
    WrappedKey wrapKey(byte[] plainDek, KeyPurpose purpose, String kekLabel, Long hsmProfileId) throws KMSException;

    /**
     * Wrap (encrypt) a plaintext Data Encryption Key with a KEK.
     * Delegates to {@link #wrapKey(byte[], KeyPurpose, String, Long)} with null profile ID.
     *
     * @param plainDek the plaintext DEK to wrap (caller must zeroize after call)
     * @param purpose  the intended purpose of this DEK
     * @param kekLabel the label of the KEK to use for wrapping
     * @return WrappedKey containing the encrypted DEK and metadata
     * @throws KMSException if wrapping fails or KEK not found
     */
    default WrappedKey wrapKey(byte[] plainDek, KeyPurpose purpose, String kekLabel) throws KMSException {
        return wrapKey(plainDek, purpose, kekLabel, null);
    }

    /**
     * Unwrap (decrypt) a wrapped DEK to obtain the plaintext key using explicit HSM profile.
     * <p>
     * SECURITY: Caller MUST zeroize the returned byte array after use
     *
     * @param wrappedKey   the wrapped key to decrypt
     * @param hsmProfileId optional HSM profile ID to use (null for auto-resolution/default)
     * @return plaintext DEK (caller must zeroize!)
     * @throws KMSException if unwrapping fails or KEK not found
     */
    byte[] unwrapKey(WrappedKey wrappedKey, Long hsmProfileId) throws KMSException;

    /**
     * Unwrap (decrypt) a wrapped DEK to obtain the plaintext key.
     * Delegates to {@link #unwrapKey(WrappedKey, Long)} with null profile ID.
     * <p>
     * SECURITY: Caller MUST zeroize the returned byte array after use
     *
     * @param wrappedKey the wrapped key to decrypt
     * @return plaintext DEK (caller must zeroize!)
     * @throws KMSException if unwrapping fails or KEK not found
     */
    default byte[] unwrapKey(WrappedKey wrappedKey) throws KMSException {
        return unwrapKey(wrappedKey, null);
    }

    /**
     * Generate a new random DEK and immediately wrap it with a KEK using explicit HSM profile.
     * (convenience method combining generation + wrapping)
     *
     * @param purpose      the intended purpose of the new DEK
     * @param kekLabel     the label of the KEK to use for wrapping
     * @param keyBits      DEK size in bits (typically 128, 192, or 256)
     * @param hsmProfileId optional HSM profile ID to use (null for auto-resolution/default)
     * @return WrappedKey containing the newly generated and wrapped DEK
     * @throws KMSException if generation or wrapping fails
     */
    WrappedKey generateAndWrapDek(KeyPurpose purpose, String kekLabel, int keyBits,
            Long hsmProfileId) throws KMSException;

    /**
     * Generate a new random DEK and immediately wrap it with a KEK.
     * Delegates to {@link #generateAndWrapDek(KeyPurpose, String, int, Long)} with null profile ID.
     * (convenience method combining generation + wrapping)
     *
     * @param purpose  the intended purpose of the new DEK
     * @param kekLabel the label of the KEK to use for wrapping
     * @param keyBits  DEK size in bits (typically 128, 192, or 256)
     * @return WrappedKey containing the newly generated and wrapped DEK
     * @throws KMSException if generation or wrapping fails
     */
    default WrappedKey generateAndWrapDek(KeyPurpose purpose, String kekLabel, int keyBits) throws KMSException {
        return generateAndWrapDek(purpose, kekLabel, keyBits, null);
    }

    /**
     * Rewrap a DEK with a different KEK (used during key rotation) using explicit target HSM profile.
     * This unwraps with the old KEK and wraps with the new KEK without exposing the plaintext DEK.
     *
     * @param oldWrappedKey      the currently wrapped key
     * @param newKekLabel        the label of the new KEK to wrap with
     * @param targetHsmProfileId optional target HSM profile ID to wrap with (null for auto-resolution/default)
     * @return new WrappedKey encrypted with the new KEK
     * @throws KMSException if rewrapping fails
     */
    WrappedKey rewrapKey(WrappedKey oldWrappedKey, String newKekLabel, Long targetHsmProfileId) throws KMSException;

    /**
     * Rewrap a DEK with a different KEK (used during key rotation).
     * Delegates to {@link #rewrapKey(WrappedKey, String, Long)} with null profile ID.
     * This unwraps with the old KEK and wraps with the new KEK without exposing the plaintext DEK.
     *
     * @param oldWrappedKey the currently wrapped key
     * @param newKekLabel   the label of the new KEK to wrap with
     * @return new WrappedKey encrypted with the new KEK
     * @throws KMSException if rewrapping fails
     */
    default WrappedKey rewrapKey(WrappedKey oldWrappedKey, String newKekLabel) throws KMSException {
        return rewrapKey(oldWrappedKey, newKekLabel, null);
    }

    /**
     * Perform health check on the provider backend
     *
     * @return true if provider is healthy and operational
     * @throws KMSException if health check fails with critical error
     */
    boolean healthCheck() throws KMSException;

    /**
     * Invalidates any cached state (config, sessions) associated with the given HSM profile.
     * Must be called after an HSM profile is updated or deleted so that the next operation
     * re-reads the profile details from the database instead of using stale cached values.
     *
     * <p>Providers that do not cache per-profile state (e.g. the database provider) can
     * leave this as a no-op.
     *
     * @param profileId the HSM profile ID whose cache should be evicted
     */
    default void invalidateProfileCache(Long profileId) {
        // no-op for providers that don't cache per-profile state
    }
}
