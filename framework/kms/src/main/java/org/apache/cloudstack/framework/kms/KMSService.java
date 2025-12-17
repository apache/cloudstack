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

import java.util.List;

/**
 * High-level service interface for Key Management Service operations.
 * <p>
 * This facade abstracts provider-specific details and provides zone-aware
 * routing, retry logic, and audit logging for KMS operations.
 * <p>
 * The service handles:
 * - Zone-scoped provider selection
 * - Configuration management (which provider, which KEK)
 * - Retry logic for transient failures
 * - Audit event emission
 * - Health monitoring
 */
public interface KMSService {

    /**
     * Get the service name
     *
     * @return service name
     */
    String getName();

    // ==================== Provider Management ====================

    /**
     * List all registered KMS providers
     *
     * @return list of available providers
     */
    List<? extends KMSProvider> listProviders();

    /**
     * Get a specific provider by name
     *
     * @param name provider name
     * @return the provider, or null if not found
     */
    KMSProvider getProvider(String name);

    /**
     * Get the configured provider for a specific zone.
     * Falls back to global default if zone has no specific configuration.
     *
     * @param zoneId the zone ID (null for global)
     * @return the configured provider for the zone
     * @throws KMSException if no provider configured or provider not found
     */
    KMSProvider getProviderForZone(Long zoneId) throws KMSException;

    // ==================== KEK Management ====================

    /**
     * Create a new KEK for a specific zone and purpose
     *
     * @param zoneId  the zone ID (null for global)
     * @param purpose the purpose of the KEK
     * @param label   optional custom label (null for auto-generated)
     * @param keyBits key size in bits
     * @return the KEK identifier
     * @throws KMSException if creation fails
     */
    String createKek(Long zoneId, KeyPurpose purpose, String label, int keyBits) throws KMSException;

    /**
     * Delete a KEK (use with extreme caution!)
     *
     * @param zoneId the zone ID
     * @param kekId  the KEK identifier to delete
     * @throws KMSException if deletion fails
     */
    void deleteKek(Long zoneId, String kekId) throws KMSException;

    /**
     * List KEKs for a zone and purpose
     *
     * @param zoneId  the zone ID (null for all zones)
     * @param purpose the purpose filter (null for all purposes)
     * @return list of KEK identifiers
     * @throws KMSException if listing fails
     */
    List<String> listKeks(Long zoneId, KeyPurpose purpose) throws KMSException;

    /**
     * Check if a KEK is available in a zone
     *
     * @param zoneId the zone ID
     * @param kekId  the KEK identifier
     * @return true if available
     * @throws KMSException if check fails
     */
    boolean isKekAvailable(Long zoneId, String kekId) throws KMSException;

    /**
     * Rotate a KEK by creating a new one and rewrapping all associated DEKs.
     * This is an async operation that may take time for large deployments.
     *
     * @param zoneId      the zone ID
     * @param purpose     the purpose of keys to rotate
     * @param oldKekLabel the current KEK label (null for configured default)
     * @param newKekLabel the new KEK label (null for auto-generated)
     * @param keyBits     the new KEK size in bits
     * @return the new KEK identifier
     * @throws KMSException if rotation fails
     */
    String rotateKek(Long zoneId, KeyPurpose purpose, String oldKekLabel,
            String newKekLabel, int keyBits) throws KMSException;

    // ==================== DEK Operations ====================

    /**
     * Generate and wrap a new DEK for volume encryption
     *
     * @param zoneId   the zone ID where the volume resides
     * @param purpose  the key purpose (typically VOLUME_ENCRYPTION)
     * @param kekLabel the KEK label to use (null for configured default)
     * @param keyBits  DEK size in bits
     * @return wrapped key ready for database storage
     * @throws KMSException if operation fails
     */
    WrappedKey generateAndWrapDek(Long zoneId, KeyPurpose purpose,
            String kekLabel, int keyBits) throws KMSException;

    /**
     * Unwrap a DEK for use (e.g., attaching encrypted volume)
     * <p>
     * SECURITY: Caller must zeroize the returned byte array after use
     *
     * @param wrappedKey the wrapped key from database
     * @return plaintext DEK (caller must zeroize!)
     * @throws KMSException if unwrap fails
     */
    byte[] unwrapDek(WrappedKey wrappedKey) throws KMSException;

    // ==================== Health & Status ====================

    /**
     * Check health of KMS provider for a zone
     *
     * @param zoneId the zone ID (null for global check)
     * @return true if healthy
     * @throws KMSException if health check fails critically
     */
    boolean healthCheck(Long zoneId) throws KMSException;
}

