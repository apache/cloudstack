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

package org.apache.cloudstack.kms;

import com.cloud.utils.component.Manager;
import org.apache.cloudstack.api.command.admin.kms.MigrateVolumesToKMSCmd;
import org.apache.cloudstack.api.command.admin.kms.RotateKMSKeyCmd;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.api.command.user.kms.CreateKMSKeyCmd;
import org.apache.cloudstack.api.command.user.kms.DeleteKMSKeyCmd;
import org.apache.cloudstack.api.command.user.kms.ListKMSKeysCmd;
import org.apache.cloudstack.api.command.user.kms.UpdateKMSKeyCmd;
import org.apache.cloudstack.api.response.KMSKeyResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.framework.kms.KMSException;
import org.apache.cloudstack.framework.kms.KMSProvider;
import org.apache.cloudstack.framework.kms.KeyPurpose;
import org.apache.cloudstack.framework.kms.WrappedKey;

import java.util.List;

/**
 * Manager interface for Key Management Service operations.
 * Provides high-level API for cryptographic key management with zone-scoping,
 * provider abstraction, and integration with CloudStack's configuration system.
 */
public interface KMSManager extends Manager, Configurable {

    // ==================== Configuration Keys ====================

    /**
     * Global: which KMS provider plugin to use by default
     * Supported values: "database" (default), "pkcs11", or custom provider names
     */
    ConfigKey<String> KMSProviderPlugin = new ConfigKey<>(
            "Advanced",
            String.class,
            "kms.provider.plugin",
            "database",
            "The KMS provider plugin to use for cryptographic operations (database, pkcs11, etc.)",
            true,
            ConfigKey.Scope.Global
    );

    /**
     * Zone-scoped: enable KMS for a specific zone
     * When false (default), new volumes use legacy passphrase encryption
     * When true, new volumes use KMS envelope encryption
     */
    ConfigKey<Boolean> KMSEnabled = new ConfigKey<>(
            "Advanced",
            Boolean.class,
            "kms.enabled",
            "false",
            "Enable Key Management Service for disk encryption in this zone",
            true,
            ConfigKey.Scope.Zone
    );

    /**
     * Global: DEK size in bits for volume encryption
     * Supported: 128, 192, 256
     */
    ConfigKey<Integer> KMSDekSizeBits = new ConfigKey<>(
            "Advanced",
            Integer.class,
            "kms.dek.size.bits",
            "256",
            "The size of Data Encryption Keys (DEK) in bits (128, 192, or 256)",
            true,
            ConfigKey.Scope.Global
    );

    /**
     * Global: retry count for transient KMS failures
     */
    ConfigKey<Integer> KMSRetryCount = new ConfigKey<>(
            "Advanced",
            Integer.class,
            "kms.retry.count",
            "3",
            "Number of retry attempts for transient KMS failures",
            true,
            ConfigKey.Scope.Global
    );

    /**
     * Global: retry delay in milliseconds
     */
    ConfigKey<Integer> KMSRetryDelayMs = new ConfigKey<>(
            "Advanced",
            Integer.class,
            "kms.retry.delay.ms",
            "1000",
            "Delay in milliseconds between KMS retry attempts (exponential backoff)",
            true,
            ConfigKey.Scope.Global
    );

    /**
     * Global: timeout for KMS operations in seconds
     */
    ConfigKey<Integer> KMSOperationTimeoutSec = new ConfigKey<>(
            "Advanced",
            Integer.class,
            "kms.operation.timeout.sec",
            "30",
            "Timeout in seconds for KMS cryptographic operations",
            true,
            ConfigKey.Scope.Global
    );

    // ==================== Provider Management ====================

    /**
     * List all registered KMS providers
     *
     * @return list of available providers
     */
    List<? extends KMSProvider> listKMSProviders();

    /**
     * Get a specific KMS provider by name
     *
     * @param name provider name
     * @return the provider, or null if not found
     */
    KMSProvider getKMSProvider(String name);

    /**
     * Get the configured provider for a zone
     *
     * @param zoneId the zone ID (null for global default)
     * @return the configured provider
     * @throws KMSException if no provider configured
     */
    KMSProvider getKMSProviderForZone(Long zoneId) throws KMSException;

    /**
     * Check if KMS is enabled for a zone
     *
     * @param zoneId the zone ID
     * @return true if KMS is enabled
     */
    boolean isKmsEnabled(Long zoneId);

    // ==================== KEK Management ====================

    /**
     * Create a new KEK for a zone and purpose
     *
     * @param zoneId  the zone ID
     * @param purpose the key purpose
     * @param label   optional custom label (null for auto-generated)
     * @param keyBits key size in bits
     * @return the KEK identifier
     * @throws KMSException if creation fails
     */
    String createKek(Long zoneId, KeyPurpose purpose, String label, int keyBits) throws KMSException;

    /**
     * Delete a KEK (WARNING: makes all DEKs wrapped by it unrecoverable)
     *
     * @param zoneId the zone ID
     * @param kekId  the KEK identifier
     * @throws KMSException if deletion fails
     */
    void deleteKek(Long zoneId, String kekId) throws KMSException;

    /**
     * List KEKs for a zone and purpose
     *
     * @param zoneId  the zone ID
     * @param purpose the purpose filter (null for all)
     * @return list of KEK identifiers
     * @throws KMSException if listing fails
     */
    List<String> listKeks(Long zoneId, KeyPurpose purpose) throws KMSException;

    /**
     * Check if a KEK is available
     *
     * @param zoneId the zone ID
     * @param kekId  the KEK identifier
     * @return true if available
     * @throws KMSException if check fails
     */
    boolean isKekAvailable(Long zoneId, String kekId) throws KMSException;

    /**
     * Rotate a KEK (create new one and rewrap all DEKs)
     *
     * @param zoneId      the zone ID
     * @param purpose     the purpose
     * @param oldKekLabel the old KEK label (must be specified)
     * @param newKekLabel the new KEK label (null for auto-generated)
     * @param keyBits     the new KEK size
     * @return the new KEK identifier
     * @throws KMSException if rotation fails
     */
    String rotateKek(Long zoneId, KeyPurpose purpose, String oldKekLabel,
            String newKekLabel, int keyBits) throws KMSException;

    // ==================== DEK Operations ====================

    /**
     * Unwrap a DEK from a wrapped key
     * SECURITY: Caller must zeroize returned byte array after use!
     *
     * @param wrappedKey the wrapped key from database
     * @param zoneId     the zone ID
     * @return plaintext DEK (caller must zeroize!)
     * @throws KMSException if unwrap fails
     */
    byte[] unwrapVolumeKey(WrappedKey wrappedKey, Long zoneId) throws KMSException;

    // ==================== Health & Status ====================

    /**
     * Check KMS provider health for a zone
     *
     * @param zoneId the zone ID (null for global)
     * @return true if healthy
     * @throws KMSException if health check fails critically
     */
    boolean healthCheck(Long zoneId) throws KMSException;

    // ==================== User KEK Management ====================

    /**
     * Create a new KMS key (KEK) for a user account
     *
     * @param accountId   the account ID
     * @param domainId    the domain ID
     * @param zoneId      the zone ID
     * @param name        user-friendly name
     * @param description optional description
     * @param purpose     key purpose
     * @param keyBits     key size in bits
     * @return the created KMS key
     * @throws KMSException if creation fails
     */
    KMSKey createUserKMSKey(Long accountId, Long domainId, Long zoneId,
                            String name, String description, KeyPurpose purpose,
                            Integer keyBits) throws KMSException;

    /**
     * List KMS keys accessible to a user account
     *
     * @param accountId the account ID
     * @param domainId  the domain ID
     * @param zoneId    optional zone filter
     * @param purpose   optional purpose filter
     * @param state     optional state filter
     * @return list of accessible KMS keys
     */
    List<? extends KMSKey> listUserKMSKeys(Long accountId, Long domainId, Long zoneId,
                                          KeyPurpose purpose, KMSKey.State state);

    /**
     * Get a KMS key by UUID (with permission check)
     *
     * @param uuid           the key UUID
     * @param callerAccountId the caller's account ID
     * @return the KMS key, or null if not found or no permission
     */
    KMSKey getUserKMSKey(String uuid, Long callerAccountId);

    /**
     * Check if caller has permission to use a KMS key
     *
     * @param callerAccountId the caller's account ID
     * @param keyUuid         the key UUID
     * @return true if caller has permission
     */
    boolean hasPermission(Long callerAccountId, KMSKey key);


    /**
     * Unwrap a DEK by wrapped key ID, trying multiple KEK versions if needed
     *
     * @param wrappedKeyId the wrapped key database ID
     * @return plaintext DEK (caller must zeroize!)
     * @throws KMSException if unwrap fails
     */
    byte[] unwrapKey(Long wrappedKeyId) throws KMSException;

    /**
     * Generate and wrap a DEK using a specific KMS key UUID
     *
     * @param kekUuid        the KMS key UUID
     * @param callerAccountId the caller's account ID
     * @return wrapped key ready for database storage
     * @throws KMSException if operation fails
     */
    WrappedKey generateVolumeKeyWithKek(KMSKey kmsKey, Long callerAccountId) throws KMSException;

    // ==================== API Response Methods ====================

    /**
     * Create a KMS key and return the response object.
     * Handles validation, account resolution, and permission checks.
     *
     * @param cmd the create command with all parameters
     * @return KMSKeyResponse
     * @throws KMSException if creation fails
     */
    KMSKeyResponse createKMSKey(CreateKMSKeyCmd cmd) throws KMSException;

    /**
     * List KMS keys and return the response object.
     * Handles validation and permission checks.
     *
     * @param cmd the list command with all parameters
     * @return ListResponse with KMSKeyResponse objects
     */
    ListResponse<KMSKeyResponse> listKMSKeys(ListKMSKeysCmd cmd);

    /**
     * Update a KMS key and return the response object.
     * Handles validation and permission checks.
     *
     * @param cmd the update command with all parameters
     * @return KMSKeyResponse
     * @throws KMSException if update fails
     */
    KMSKeyResponse updateKMSKey(UpdateKMSKeyCmd cmd) throws KMSException;

    /**
     * Delete a KMS key and return the response object.
     * Handles validation and permission checks.
     *
     * @param cmd the delete command with all parameters
     * @return SuccessResponse
     * @throws KMSException if deletion fails
     */
    SuccessResponse deleteKMSKey(DeleteKMSKeyCmd cmd) throws KMSException;

    // ==================== Admin Operations ====================

    /**
     * Rotate KEK by creating new version and scheduling gradual re-encryption
     *
     * @param cmd the rotate command with all parameters
     * @return New KEK version UUID
     * @throws KMSException if rotation fails
     */
    String rotateKMSKey(RotateKMSKeyCmd cmd) throws KMSException;

    /**
     * Gradually rewrap all wrapped keys for a KMS key to use new KEK version
     *
     * @param kmsKeyId KMS key ID
     * @param newKekVersionId New active KEK version ID
     * @param batchSize Number of keys to process per batch
     * @return Number of keys successfully rewrapped
     * @throws KMSException if rewrap fails
     */
    int rewrapWrappedKeysForKMSKey(Long kmsKeyId, Long newKekVersionId, int batchSize) throws KMSException;

    /**
     * Migrate passphrase-based volumes to KMS encryption
     *
     * @param cmd the migrate command with all parameters
     * @return Number of volumes successfully migrated
     * @throws KMSException if migration fails
     */
    int migrateVolumesToKMS(MigrateVolumesToKMSCmd cmd) throws KMSException;
}
