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

import com.cloud.user.Account;
import com.cloud.utils.component.Manager;
import org.apache.cloudstack.api.command.admin.kms.MigrateVolumesToKMSCmd;
import org.apache.cloudstack.api.command.user.kms.RotateKMSKeyCmd;
import org.apache.cloudstack.api.command.user.kms.CreateKMSKeyCmd;
import org.apache.cloudstack.api.command.user.kms.DeleteKMSKeyCmd;
import org.apache.cloudstack.api.command.user.kms.ListKMSKeysCmd;
import org.apache.cloudstack.api.command.user.kms.UpdateKMSKeyCmd;
import org.apache.cloudstack.api.command.user.kms.hsm.AddHSMProfileCmd;
import org.apache.cloudstack.api.command.user.kms.hsm.DeleteHSMProfileCmd;
import org.apache.cloudstack.api.command.user.kms.hsm.ListHSMProfilesCmd;
import org.apache.cloudstack.api.command.user.kms.hsm.UpdateHSMProfileCmd;
import org.apache.cloudstack.api.response.HSMProfileResponse;
import org.apache.cloudstack.api.response.KMSKeyResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.framework.kms.KMSException;
import org.apache.cloudstack.framework.kms.KMSProvider;
import org.apache.cloudstack.framework.kms.WrappedKey;

import java.util.List;

public interface KMSManager extends Manager, Configurable {

    ConfigKey<Integer> KMSDekSizeBits = new ConfigKey<>(
            "Advanced",
            Integer.class,
            "kms.dek.size.bits",
            "256",
            "The size of Data Encryption Keys (DEK) in bits (128, 192, or 256)",
            true,
            ConfigKey.Scope.Global
    );

    ConfigKey<Integer> KMSRetryCount = new ConfigKey<>(
            "Advanced",
            Integer.class,
            "kms.retry.count",
            "3",
            "Number of retry attempts for transient KMS failures",
            true,
            ConfigKey.Scope.Global
    );

    ConfigKey<Integer> KMSRetryDelayMs = new ConfigKey<>(
            "Advanced",
            Integer.class,
            "kms.retry.delay.ms",
            "1000",
            "Delay in milliseconds between KMS retry attempts (exponential backoff)",
            true,
            ConfigKey.Scope.Global
    );

    ConfigKey<Integer> KMSOperationTimeoutSec = new ConfigKey<>(
            "Advanced",
            Integer.class,
            "kms.operation.timeout.sec",
            "30",
            "Timeout in seconds for KMS cryptographic operations",
            true,
            ConfigKey.Scope.Global
    );

    ConfigKey<Integer> KMSRewrapBatchSize = new ConfigKey<>(
            "Advanced",
            Integer.class,
            "kms.rewrap.batch.size",
            "50",
            "Number of wrapped keys to rewrap per batch in background job",
            true,
            ConfigKey.Scope.Global
    );

    ConfigKey<Long> KMSRewrapIntervalMs = new ConfigKey<>(
            "Advanced",
            Long.class,
            "kms.rewrap.interval.ms",
            "300000",
            "Interval in milliseconds between background rewrap job executions (default: 5 minutes)",
            true,
            ConfigKey.Scope.Global
    );

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
     * Unwrap a DEK from a wrapped key
     * SECURITY: Caller must zeroize returned byte array after use!
     *
     * @param wrappedKey the wrapped key from database
     * @param zoneId     the zone ID
     * @return plaintext DEK (caller must zeroize!)
     * @throws KMSException if unwrap fails
     */
    byte[] unwrapVolumeKey(WrappedKey wrappedKey, Long zoneId) throws KMSException;

    /**
     * Check if caller has permission to use a KMS key
     *
     * @param callerAccountId the caller's account ID
     * @param key             the KMS key
     * @return true if caller has permission
     */
    boolean hasPermission(Long callerAccountId, KMSKey key);

    /**
     * Validates that the KMS key can be used for volume encryption: key exists, not deleted,
     * caller has access, key state is Enabled, and key purpose is VOLUME_ENCRYPTION.
     * No-op if kmsKeyId is null.
     *
     * @param caller   the caller's account
     * @param kmsKeyId the KMS key database ID
     * @param zoneId   the zone ID of the target resource (volume/VM)
     * @throws InvalidParameterValueException if key not found, deleted, disabled, wrong purpose, or zone mismatch
     * @throws PermissionDeniedException      if caller lacks access
     */
    void checkKmsKeyForVolumeEncryption(Account caller, Long kmsKeyId, Long zoneId);

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
     * @param kmsKey          the KMS key
     * @param callerAccountId the caller's account ID
     * @return wrapped key ready for database storage
     * @throws KMSException if operation fails
     */
    WrappedKey generateVolumeKeyWithKek(KMSKey kmsKey, Long callerAccountId) throws KMSException;

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

    /**
     * Rotate KEK by creating new version and scheduling gradual re-encryption
     *
     * @param cmd the rotate command with all parameters
     * @return New KEK version UUID
     * @throws KMSException if rotation fails
     */
    String rotateKMSKey(RotateKMSKeyCmd cmd) throws KMSException;

    /**
     * Migrate passphrase-based volumes to KMS encryption
     *
     * @param cmd the migrate command with all parameters
     * @return Number of volumes successfully migrated
     * @throws KMSException if migration fails
     */
    int migrateVolumesToKMS(MigrateVolumesToKMSCmd cmd) throws KMSException;

    /**
     * Delete all KMS keys owned by an account (called during account cleanup)
     *
     * @param accountId the account ID
     * @return true if all keys were successfully deleted
     */
    boolean deleteKMSKeysByAccountId(Long accountId);

    /**
     * Add a new HSM profile
     *
     * @param cmd the add command
     * @return the created HSM profile
     * @throws KMSException if addition fails
     */
    HSMProfile addHSMProfile(AddHSMProfileCmd cmd) throws KMSException;

    /**
     * List HSM profiles
     *
     * @param cmd the list command
     * @return list of HSM profiles
     */
    ListResponse<HSMProfileResponse> listHSMProfiles(ListHSMProfilesCmd cmd);

    /**
     * Delete an HSM profile
     *
     * @param cmd the delete command
     * @return true if deletion was successful
     * @throws KMSException if deletion fails
     */
    boolean deleteHSMProfile(DeleteHSMProfileCmd cmd) throws KMSException;

    /**
     * Update an HSM profile
     *
     * @param cmd the update command
     * @return the updated HSM profile
     * @throws KMSException if update fails
     */
    HSMProfile updateHSMProfile(UpdateHSMProfileCmd cmd) throws KMSException;

    /**
     * Create a response object for an HSM profile
     *
     * @param profile the HSM profile
     * @return the response object
     */
    HSMProfileResponse createHSMProfileResponse(HSMProfile profile);
}
