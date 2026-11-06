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

import com.cloud.api.ApiDBUtils;
import com.cloud.api.ApiResponseHelper;
import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.user.AccountVO;
import com.cloud.user.dao.AccountDao;
import com.cloud.event.ActionEvent;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.storage.Snapshot;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.Pair;
import com.cloud.utils.Ternary;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallbackWithException;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.ApiCommandResourceType;
import org.apache.cloudstack.api.command.admin.kms.MigrateVolumesToKMSCmd;
import org.apache.cloudstack.api.command.user.kms.CreateKMSKeyCmd;
import org.apache.cloudstack.api.command.user.kms.DeleteKMSKeyCmd;
import org.apache.cloudstack.api.command.user.kms.ListKMSKeysCmd;
import org.apache.cloudstack.api.command.user.kms.RotateKMSKeyCmd;
import org.apache.cloudstack.api.command.user.kms.UpdateKMSKeyCmd;
import org.apache.cloudstack.api.command.user.kms.hsm.CreateHSMProfileCmd;
import org.apache.cloudstack.api.command.user.kms.hsm.DeleteHSMProfileCmd;
import org.apache.cloudstack.api.command.user.kms.hsm.ListHSMProfilesCmd;
import org.apache.cloudstack.api.command.user.kms.hsm.UpdateHSMProfileCmd;
import org.apache.cloudstack.api.response.HSMProfileResponse;
import org.apache.cloudstack.api.response.KMSKeyResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.kms.KMSException;
import org.apache.cloudstack.framework.kms.KMSProvider;
import org.apache.cloudstack.framework.kms.KeyPurpose;
import org.apache.cloudstack.framework.kms.WrappedKey;
import org.apache.cloudstack.kms.dao.HSMProfileDao;
import org.apache.cloudstack.kms.dao.HSMProfileDetailsDao;
import org.apache.cloudstack.kms.dao.KMSKekVersionDao;
import org.apache.cloudstack.kms.dao.KMSKeyDao;
import org.apache.cloudstack.kms.dao.KMSWrappedKeyDao;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.secret.PassphraseVO;
import org.apache.cloudstack.secret.dao.PassphraseDao;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class KMSManagerImpl extends ManagerBase implements KMSManager, PluggableService {
    private static final Logger logger = LogManager.getLogger(KMSManagerImpl.class);
    private static final Map<String, KMSProvider> kmsProviderMap = new HashMap<>();
    private static final String DATABASE_PROVIDER_NAME = "database";
    // Wrap algorithms that detect a wrong KEK or a modified blob.
    private static final Set<String> AUTHENTICATED_WRAP_ALGORITHMS = Set.of("AES/GCM/NoPadding", "AES/KWP/NoPadding");
    private static final Set<Integer> VALID_DEK_SIZE_BITS = Set.of(128, 192, 256);
    // 4.23 migrated the base64 text of the legacy passphrase (64 characters) instead of its 48 raw bytes.
    private static final int LEGACY_MIGRATED_KEY_LENGTH = 64;
    private static final int LEGACY_PASSPHRASE_LENGTH = 48;
    private ExecutorService kmsOperationExecutor;

    @Inject
    private KMSWrappedKeyDao kmsWrappedKeyDao;

    @Inject
    private KMSKeyDao kmsKeyDao;

    @Inject
    private KMSKekVersionDao kmsKekVersionDao;

    @Inject
    private HSMProfileDao hsmProfileDao;

    @Inject
    private HSMProfileDetailsDao hsmProfileDetailsDao;

    @Inject
    private AccountManager accountManager;

    @Inject
    private DataCenterDao dataCenterDao;

    @Inject
    private VolumeDao volumeDao;
    @Inject
    private AccountDao accountDao;
    @Inject
    private DomainDao domainDao;

    @Inject
    private PassphraseDao passphraseDao;
    @Inject
    private SnapshotDao snapshotDao;
    private List<KMSProvider> kmsProviders;
    private ScheduledExecutorService rewrapExecutor;
    private ScheduledExecutorService keyFormatRepairExecutor;
    // Id of the last key checked by the key format repair; only used by its single thread
    private long keyFormatRepairCursor = 0;


    @Override
    public List<? extends KMSProvider> listKMSProviders() {
        return kmsProviders;
    }

    @Override
    public KMSProvider getKMSProvider(String name) {
        if (StringUtils.isEmpty(name)) {
            name = DATABASE_PROVIDER_NAME;
        }

        String providerName = name.toLowerCase();
        if (!kmsProviderMap.containsKey(providerName)) {
            throw new CloudRuntimeException(String.format("KMS provider '%s' not found", providerName));
        }

        KMSProvider provider = kmsProviderMap.get(providerName);
        if (provider == null) {
            throw new CloudRuntimeException(String.format("KMS provider '%s' returned is null", providerName));
        }

        return provider;
    }

    @Override
    public boolean hasPermission(Long callerAccountId, KMSKey key) {
        if (callerAccountId == null || key == null) {
            return false;
        }
        if (!key.isEnabled()) {
            throw new InvalidParameterValueException("KMS key is not enabled: " + key);
        }
        return callerAccountId == key.getAccountId();
    }

    @Override
    public void checkKmsKeyForVolumeEncryption(Account owner, Long kmsKeyId, Long zoneId) {
        if (kmsKeyId == null) {
            return;
        }
        KMSKeyVO key = kmsKeyDao.findById(kmsKeyId);
        if (key == null) {
            throw new InvalidParameterValueException("KMS key not found: " + kmsKeyId);
        }
        if (key.getAccountId() != owner.getId()) {
            throw new PermissionDeniedException("KMS key does not belong to the volume owner account");
        }
        if (key.getZoneId() != null && zoneId != null && !key.getZoneId().equals(zoneId)) {
            throw new InvalidParameterValueException(
                    "KMS key belongs to zone " + key.getZoneId() +
                            " but the target resource is in zone " + zoneId);
        }
        if (!key.isEnabled()) {
            throw new InvalidParameterValueException(
                    "KMS key is not enabled and cannot be used for volume encryption: " + key.getUuid());
        }
        if (key.getPurpose() != KeyPurpose.VOLUME_ENCRYPTION) {
            throw new InvalidParameterValueException(
                    "KMS key purpose must be volume encryption; key has purpose: " + key.getPurpose().getName());
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_KMS_KEY_UNWRAP, eventDescription = "unwrapping key")
    public byte[] unwrapKey(Long wrappedKeyId) throws KMSException {
        KMSWrappedKeyVO wrappedVO = kmsWrappedKeyDao.findById(wrappedKeyId);
        if (wrappedVO == null) {
            throw KMSException.kekNotFound("Wrapped key not found: " + wrappedKeyId);
        }

        KMSKeyVO kmsKey = kmsKeyDao.findById(wrappedVO.getKmsKeyId());
        if (kmsKey == null) {
            throw KMSException.kekNotFound("KMS key not found for wrapped key: " + wrappedKeyId);
        }

        Exception lastException = null;

        KMSKekVersionVO recordedVersion = wrappedVO.getKekVersionId() != null
                ? kmsKekVersionDao.findById(wrappedVO.getKekVersionId()) : null;
        if (recordedVersion != null && recordedVersion.getStatus() == KMSKekVersionVO.Status.Archived) {
            // The rewrap worker deleted its KEK; no other KEK can unwrap the key
            throw KMSException.kekNotFound(String.format("KEK version %d of wrapped key %s is archived",
                    recordedVersion.getVersionNumber(), wrappedVO.getUuid()));
        }
        if (recordedVersion != null) {
            HSMProfileVO hsmProfile = getEnabledHsmProfile(recordedVersion);
            try {
                return unwrapKeyWithVersion(wrappedVO, kmsKey, recordedVersion, hsmProfile, "");
            } catch (Exception e) {
                lastException = e;
                logger.warn("Failed to unwrap with version {}: {}", recordedVersion.getVersionNumber(), e.getMessage(), e);
            }
        }

        // Other versions only for an authenticated algorithm: with CBC another KEK can unwrap the blob to a wrong
        // key without an error, and a rewrap would then lose the real key for good.
        if (AUTHENTICATED_WRAP_ALGORITHMS.contains(wrappedVO.getWrapAlgorithm())) {
            List<KMSKekVersionVO> otherVersions = kmsKekVersionDao.getVersionsForDecryption(kmsKey.getId()).stream()
                    .filter(version -> recordedVersion == null || !version.getId().equals(recordedVersion.getId()))
                    .sorted(Comparator.comparing(KMSKekVersionVO::getVersionNumber).reversed())
                    .collect(Collectors.toList());
            for (KMSKekVersionVO version : otherVersions) {
                HSMProfileVO hsmProfile = getEnabledHsmProfile(version);
                try {
                    return unwrapKeyWithVersion(wrappedVO, kmsKey, version, hsmProfile, " (fallback)");
                } catch (Exception e) {
                    lastException = e;
                    logger.warn("Failed to unwrap with version {}: {}", version.getVersionNumber(), e.getMessage(), e);
                }
            }
        }

        if (lastException == null) {
            // Nothing to try, e.g. the KEK version was archived and its KEK deleted
            throw KMSException.kekNotFound("No usable KEK version for wrapped key " + wrappedKeyId);
        }
        throw KMSException.wrapUnwrapFailed("Failed to unwrap key: no available KEK version for wrapped key " + wrappedKeyId, lastException);
    }

    private HSMProfileVO getEnabledHsmProfile(KMSKekVersionVO version) {
        HSMProfileVO hsmProfile = hsmProfileDao.findById(version.getHsmProfileId());
        if (!hsmProfile.isEnabled()) {
            throw KMSException.providerNotInitialized("HSM profile is not enabled for wrapped key: " + version);
        }
        return hsmProfile;
    }

    private byte[] unwrapKeyWithVersion(KMSWrappedKeyVO wrappedVO, KMSKeyVO kmsKey, KMSKekVersionVO version,
                                        HSMProfileVO hsmProfile, String fallback) throws Exception {
        byte[] dek = getUnwrappedKey(wrappedVO, kmsKey, version, hsmProfile);

        CallContext.current().setEventResourceId(kmsKey.getId());
        CallContext.current().setEventResourceType(ApiCommandResourceType.KmsKey);
        CallContext.current().setEventDetails(String.format(
                "Unwrapped %s key (wrapped key uuid: %s) using KMS key: %s (uuid: %s) with KEK version %d%s",
                kmsKey.getPurpose().getName(), wrappedVO.getUuid(), kmsKey.getName(), kmsKey.getUuid(),
                version.getVersionNumber(), fallback));

        logger.debug("Successfully unwrapped key {} with KEK version {}{}", wrappedVO.getId(), version.getVersionNumber(), fallback);
        return dek;
    }

    private byte[] getUnwrappedKey(KMSWrappedKeyVO wrappedVO, KMSKeyVO kmsKey,
                                   KMSKekVersionVO version, HSMProfileVO hsmProfile) throws Exception {
        KMSProvider provider = getKMSProvider(hsmProfile.getProtocol());

        WrappedKey wrapped = new WrappedKey(wrappedVO.getUuid(), version.getKekLabel(), kmsKey.getPurpose(),
                wrappedVO.getWrapAlgorithm(), wrappedVO.getWrappedBlob(),
                hsmProfile.getProtocol(), wrappedVO.getCreated(), kmsKey.getZoneId());
        return retryOperation(() -> provider.unwrapKey(wrapped, version.getHsmProfileId()));
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_KMS_KEY_WRAP, eventDescription = "generating key with specified KEK")
    public WrappedKey generateVolumeKeyWithKek(KMSKey kmsKey, Long callerAccountId) throws KMSException {
        if (kmsKey == null) {
            throw KMSException.kekNotFound("KMS key not found");
        }

        if (!kmsKey.isEnabled()) {
            throw KMSException.invalidParameter("KMS key is not enabled: " + kmsKey);
        }

        if (kmsKey.getPurpose() != KeyPurpose.VOLUME_ENCRYPTION) {
            throw KMSException.invalidParameter("KMS key purpose is not VOLUME_ENCRYPTION: " + kmsKey);
        }

        KMSKekVersionVO activeVersion = getActiveKekVersion(kmsKey.getId());

        HSMProfileVO hsmProfile = hsmProfileDao.findById(activeVersion.getHsmProfileId());
        if (hsmProfile == null) {
            throw KMSException.invalidParameter("HSM profile not found: " + activeVersion.getHsmProfileId());
        }
        if (!hsmProfile.isEnabled()) {
            throw KMSException.invalidParameter("HSM profile is not enabled: " + hsmProfile.getName());
        }
        KMSProvider provider = getKMSProvider(hsmProfile.getProtocol());

        int dekSize = KMSDekSizeBits.value();
        if (!VALID_DEK_SIZE_BITS.contains(dekSize)) {
            throw KMSException.invalidParameter(KMSDekSizeBits.key() + " must be 128, 192 or 256: " + dekSize);
        }
        WrappedKey wrappedKey;
        try {
            wrappedKey = retryOperation(() -> provider.generateAndWrapDek(KeyPurpose.VOLUME_ENCRYPTION,
                    activeVersion.getKekLabel(), dekSize,
                    activeVersion.getHsmProfileId()));
            KMSWrappedKeyVO wrappedKeyVO = new KMSWrappedKeyVO(kmsKey.getId(), activeVersion.getId(),
                    kmsKey.getZoneId(), wrappedKey.getWrappedKeyMaterial());
            wrappedKeyVO.setWrapAlgorithm(wrappedKey.getAlgorithm());
            wrappedKeyVO = kmsWrappedKeyDao.persist(wrappedKeyVO);

            // Volume creation code looks up by UUID and sets volume.kmsWrappedKeyId
            wrappedKey = new WrappedKey(
                    wrappedKeyVO.getUuid(),
                    wrappedKey.getKekId(),
                    wrappedKey.getPurpose(),
                    wrappedKey.getAlgorithm(),
                    wrappedKey.getWrappedKeyMaterial(),
                    wrappedKey.getProviderName(),
                    wrappedKey.getCreated(),
                    wrappedKey.getZoneId());
        } catch (Exception e) {
            throw handleKmsException(e);
        }

        CallContext.current().setEventResourceId(kmsKey.getId());
        CallContext.current().setEventResourceType(ApiCommandResourceType.KmsKey);
        CallContext.current().setEventDetails(String.format(
                "Generated %s key (wrapped key uuid: %s) using KMS key: %s (uuid: %s) with KEK version %d",
                kmsKey.getPurpose().getName(), wrappedKey.getUuid(), kmsKey.getName(), kmsKey.getUuid(), activeVersion.getVersionNumber()));

        logger.debug("Generated {} key using KMS key {} with KEK version {}, wrapped key UUID: {}",
                kmsKey.getPurpose().getName(), kmsKey, activeVersion.getVersionNumber(), wrappedKey.getUuid());
        return wrappedKey;
    }

    private KMSKekVersionVO getActiveKekVersion(Long kmsKeyId) throws KMSException {
        KMSKekVersionVO activeVersion = kmsKekVersionDao.getActiveVersion(kmsKeyId);
        if (activeVersion == null) {
            throw KMSException.kekNotFound("No active KEK version found for KMS key ID: " + kmsKeyId);
        }
        return activeVersion;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_KMS_KEY_CREATE, eventDescription = "creating user KMS key")
    public KMSKeyResponse createKMSKey(CreateKMSKeyCmd cmd) throws KMSException {
        if (cmd.getDomainId() != null && StringUtils.isBlank(cmd.getAccountName())) {
            throw new InvalidParameterValueException("Account name must be provided with domain ID");
        }

        Account caller = CallContext.current().getCallingAccount();
        Account targetAccount = accountManager.finalizeOwner(caller, cmd.getAccountName(), cmd.getDomainId(),
                cmd.getProjectId());

        KeyPurpose keyPurpose = parseKeyPurpose("volume");

        int bits = cmd.getKeyBits();
        if (bits != 128 && bits != 192 && bits != 256) {
            throw new InvalidParameterValueException("Key bits must be 128, 192, or 256");
        }

        HSMProfileVO profile = getHSMProfile(cmd.getHsmProfileId());
        checkHSMProfileAccess(caller, profile, false);
        validateProfileScopeForOwner(profile, targetAccount.getId(), targetAccount.getDomainId());
        if (!profile.isEnabled()) {
            throw new InvalidParameterValueException("HSM profile is not enabled: " + profile.getName());
        }

        KMSKey kmsKey = createUserKMSKey(
                targetAccount.getId(),
                targetAccount.getDomainId(),
                cmd.getZoneId(),
                cmd.getName(),
                cmd.getDescription(),
                keyPurpose,
                bits,
                cmd.getHsmProfileId());

        CallContext.current().setEventResourceId(kmsKey.getId());
        CallContext.current().setEventResourceType(ApiCommandResourceType.KmsKey);
        CallContext.current().setEventDetails(String.format("Created KMS key: %s (uuid: %s)", kmsKey.getName(), kmsKey.getUuid()));

        return createKMSKeyResponse(kmsKey);
    }

    KMSKeyResponse createKMSKeyResponse(KMSKey kmsKey) {
        KMSKeyResponse response = new KMSKeyResponse();
        response.setId(kmsKey.getUuid());
        response.setName(kmsKey.getName());
        response.setDescription(kmsKey.getDescription());
        response.setAlgorithm(kmsKey.getAlgorithm());
        response.setKeyBits(kmsKey.getKeyBits());
        response.setEnabled(kmsKey.isEnabled());
        response.setCreated(kmsKey.getCreated());

        KMSKekVersionVO activeVersion = kmsKekVersionDao.getActiveVersion(kmsKey.getId());
        if (activeVersion != null) {
            response.setVersion(activeVersion.getVersionNumber());
        }

        HSMProfileVO hsmProfile = hsmProfileDao.findById(kmsKey.getHsmProfileId());
        if (hsmProfile != null) {
            response.setHsmProfileId(hsmProfile.getUuid());
            response.setHsmProfileName(hsmProfile.getName());
        }

        ApiResponseHelper.populateOwner(response, kmsKey);

        DataCenter zone = ApiDBUtils.findZoneById(kmsKey.getZoneId());
        if (zone != null) {
            response.setZoneId(zone.getUuid());
            response.setZoneName(zone.getName());
        }

        Account caller = CallContext.current().getCallingAccount();
        if (caller != null && (caller.getType() == Account.Type.ADMIN
                || caller.getType() == Account.Type.RESOURCE_DOMAIN_ADMIN)) {
            response.setKekLabel(kmsKey.getKekLabel());
        }

        response.setObjectName("kmskey");
        return response;
    }

    KMSKey createUserKMSKey(Long accountId, Long domainId, Long zoneId,
                            String name, String description, KeyPurpose purpose,
                            Integer keyBits, long hsmProfileId) throws KMSException {
        HSMProfileVO profile = hsmProfileDao.findById(hsmProfileId);
        if (profile == null) {
            throw KMSException.invalidParameter("HSM Profile not found");
        }
        if (kmsKeyDao.findByNameAndAccountId(name, accountId) != null) {
            throw new InvalidParameterValueException("A KMS key with name " + name + " already exists in this account");
        }

        KMSProvider provider = getKMSProvider(profile.getProtocol());
        KMSKeyVO kmsKey = new KMSKeyVO(name, description, "", purpose,
                accountId, domainId, zoneId, provider.getWrapAlgorithm(hsmProfileId), keyBits);

        String kekLabel = purpose.generateKekLabel(domainId, accountId, kmsKey.getUuid(), 1);

        String providerKekLabel;
        Long finalProfileId = hsmProfileId;
        try {
            providerKekLabel = retryOperation(() -> provider.createKek(purpose, kekLabel, keyBits, finalProfileId));
        } catch (Exception e) {
            throw handleKmsException(e);
        }
        verifyKekCanWrap(provider, purpose, providerKekLabel, profile);

        kmsKey.setKekLabel(providerKekLabel);
        kmsKey.setHsmProfileId(finalProfileId);
        kmsKey = kmsKeyDao.persist(kmsKey);

        KMSKekVersionVO initialVersion = new KMSKekVersionVO(kmsKey.getId(), 1, providerKekLabel);
        initialVersion.setHsmProfileId(finalProfileId);
        initialVersion = kmsKekVersionDao.persist(initialVersion);

        logger.info("Created KMS key ({}) with initial KEK version {} for account {} in zone {} (profile: {})",
                kmsKey, initialVersion.getVersionNumber(), accountId, zoneId, finalProfileId);
        return kmsKey;
    }

    @Override
    public ListResponse<KMSKeyResponse> listKMSKeys(ListKMSKeysCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();

        List<Long> permittedAccounts = new ArrayList<>();
        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<>(
                cmd.getDomainId(), cmd.isRecursive(), null);
        accountManager.buildACLSearchParameters(caller, cmd.getId(), cmd.getAccountName(),
                cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject,
                cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        SearchBuilder<KMSKeyVO> sb = getSearchBuilderForKMSKeys(cmd, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);
        SearchCriteria<KMSKeyVO> sc = getSearchCriteriaForKMSKeys(sb, cmd, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

        Filter searchFilter = new Filter(KMSKeyVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Pair<List<KMSKeyVO>, Integer> result = kmsKeyDao.searchAndCount(sc, searchFilter);
        List<KMSKeyVO> keys = result.first();
        Integer count = result.second();

        List<KMSKeyResponse> responses = new ArrayList<>();
        for (KMSKey key : keys) {
            responses.add(createKMSKeyResponse(key));
        }

        ListResponse<KMSKeyResponse> listResponse = new ListResponse<>();
        listResponse.setResponses(responses, count);
        return listResponse;
    }

    SearchBuilder<KMSKeyVO> getSearchBuilderForKMSKeys(ListKMSKeysCmd cmd, Long domainId, Boolean isRecursive, List<Long> permittedAccounts,
                                                       ListProjectResourcesCriteria listProjectResourcesCriteria) {
        SearchBuilder<KMSKeyVO> sb = kmsKeyDao.createSearchBuilder();
        accountManager.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("zoneId", sb.entity().getZoneId(), SearchCriteria.Op.EQ);
        sb.and("purpose", sb.entity().getPurpose(), SearchCriteria.Op.EQ);
        sb.and("enabled", sb.entity().isEnabled(), SearchCriteria.Op.EQ);
        sb.and("hsmProfileId", sb.entity().getHsmProfileId(), SearchCriteria.Op.EQ);
        if (cmd.getKeyword() != null) {
            sb.and().op("name", sb.entity().getName(), SearchCriteria.Op.LIKE);
            sb.or("description", sb.entity().getDescription(), SearchCriteria.Op.LIKE);
            sb.cp();
        }
        sb.done();
        return sb;
    }

    SearchCriteria<KMSKeyVO> getSearchCriteriaForKMSKeys(SearchBuilder<KMSKeyVO> searchBuilder, ListKMSKeysCmd cmd,
                                                         Long domainId, Boolean isRecursive, List<Long> permittedAccounts,
                                                         ListProjectResourcesCriteria listProjectResourcesCriteria) {
        SearchCriteria<KMSKeyVO> sc = searchBuilder.create();
        accountManager.buildACLSearchCriteria(sc, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);
        KeyPurpose keyPurpose = parseKeyPurpose("volume");
        sc.setParametersIfNotNull("id", cmd.getId());
        sc.setParametersIfNotNull("zoneId", cmd.getZoneId());
        sc.setParametersIfNotNull("purpose", keyPurpose);
        sc.setParametersIfNotNull("enabled", cmd.getEnabled());
        sc.setParametersIfNotNull("hsmProfileId", cmd.getHsmProfileId());
        if (cmd.getKeyword() != null) {
            sc.setParameters("name", "%" + cmd.getKeyword() + "%");
            sc.setParameters("description", "%" + cmd.getKeyword() + "%");
        }
        return sc;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_KMS_KEY_UPDATE, eventDescription = "updating KMS Key")
    public KMSKeyResponse updateKMSKey(UpdateKMSKeyCmd cmd) throws KMSException {
        Account caller = CallContext.current().getCallingAccount();
        KMSKeyVO key = findKMSKeyAndCheckAccess(cmd.getId(), caller);
        KMSKey updatedKey = updateUserKMSKey(key, cmd.getName(), cmd.getDescription(), cmd.getEnabled());
        String details = String.format("Updated KMS key: %s (uuid: %s, enabled: %s)", updatedKey.getName(), updatedKey.getUuid(), updatedKey.isEnabled());
        CallContext.current().setEventDetails(details);
        ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(),
                updatedKey.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_KMS_KEY_UPDATE, true,
                details, updatedKey.getId(), ApiCommandResourceType.Volume.toString(), CallContext.current().getStartEventId());
        return createKMSKeyResponse(updatedKey);
    }

    KMSKey updateUserKMSKey(KMSKeyVO key, String name, String description, Boolean enabled) {
        boolean updated = false;
        if (name != null && !name.equals(key.getName())) {
            key.setName(name);
            updated = true;
        }
        if (description != null && !description.equals(key.getDescription())) {
            key.setDescription(description);
            updated = true;
        }
        if (enabled != null && enabled != key.isEnabled()) {
            key.setEnabled(enabled);
            updated = true;
        }

        if (updated) {
            kmsKeyDao.update(key.getId(), key);
            logger.info("Updated KMS key {}", key);
        }

        return key;
    }

    @Override
    public boolean deleteKMSWrappedKey(Volume vol) throws KMSException {
        if (vol.getKmsWrappedKeyId() == null) {
            return true;
        }

        KMSWrappedKeyVO wrappedKey = kmsWrappedKeyDao.findById(vol.getKmsWrappedKeyId());
        if (wrappedKey != null) {
            List<VolumeVO> volumes = volumeDao.findByKmsWrappedKeyId(vol.getKmsWrappedKeyId());
            if (CollectionUtils.isNotEmpty(volumes)) {
                for (VolumeVO volume : volumes) {
                    if (volume.getId() != vol.getId()) {
                        // Normal after a volume migration, the source and the new volume share the key
                        logger.debug("KMS Wrapped Key {} is still in use by volume {}, cannot delete",
                                wrappedKey, volume);
                        return false;
                    }
                }
            }
            if (isNeededBySnapshots(wrappedKey)) {
                logger.debug("KMS Wrapped Key {} is still needed by snapshots of volume {} or of another volume "
                        + "sharing it, keeping it", wrappedKey, vol);
                return false;
            }
            logger.info("Deleted KMS Wrapped Key {} for volume {}", wrappedKey, vol);
            return kmsWrappedKeyDao.remove(wrappedKey.getId());
        }
        return true;
    }

    /**
     * Whether a snapshot that isn't destroyed exists for any volume (removed volumes included) that uses the
     * wrapped key. Restoring or deleting such a snapshot needs the key of its volume.
     */
    private boolean isNeededBySnapshots(KMSWrappedKeyVO wrappedKey) {
        List<Long> volumeIds = volumeDao.listIncludingRemovedByKmsWrappedKeyId(wrappedKey.getId()).stream()
                .map(VolumeVO::getId).collect(Collectors.toList());
        return snapshotDao.searchByVolumes(volumeIds).stream()
                .anyMatch(snapshot -> snapshot.getState() != Snapshot.State.Destroyed);
    }

    @Override
    public SuccessResponse deleteKMSKey(DeleteKMSKeyCmd cmd) throws KMSException {
        Account caller = CallContext.current().getCallingAccount();

        KMSKeyVO key = findKMSKeyAndCheckAccess(cmd.getId(), caller);
        CallContext.current().setEventResourceId(key.getId());
        CallContext.current().setEventResourceType(ApiCommandResourceType.KmsKey);
        CallContext.current().setEventDetails(String.format("Deleted KMS key: %s (uuid: %s)", key.getName(), key.getUuid()));
        deleteUserKMSKey(key);
        return new SuccessResponse();
    }

    void deleteUserKMSKey(KMSKeyVO key) throws KMSException {
        long wrappedKeyCount = kmsWrappedKeyDao.countByKmsKeyId(key.getId());
        if (wrappedKeyCount > 0) {
            throw new InvalidParameterValueException("Cannot delete KMS key: " + key + ". " + wrappedKeyCount +
                    " wrapped key(s) still reference this key");
        }

        if (volumeDao.existsWithKmsKey(key.getId())) {
            throw new InvalidParameterValueException("Cannot delete KMS key: " + key + ". " +
                    "There are Volumes which still reference this key");
        }

        List<KMSKekVersionVO> kekVersions = kmsKekVersionDao.listByKmsKeyId(key.getId());
        for (KMSKekVersionVO kekVersion : kekVersions) {
            try {
                HSMProfileVO hsmProfile = hsmProfileDao.findById(kekVersion.getHsmProfileId());
                if (hsmProfile != null) {
                    KMSProvider provider = getKMSProvider(hsmProfile.getProtocol());
                    provider.deleteKek(kekVersion.getKekLabel());
                    logger.info("Deleted KEK {} (v{}) from provider {}",
                            kekVersion.getKekLabel(), kekVersion.getVersionNumber(), provider.getProviderName());
                }
            } catch (Exception e) {
                if (e instanceof KMSException && ((KMSException) e).getErrorType() == KMSException.ErrorType.KEK_NOT_FOUND) {
                    // e.g. an archived version whose KEK the rewrap worker already deleted
                    logger.info("KEK {} (v{}) is already gone from the provider", kekVersion.getKekLabel(),
                            kekVersion.getVersionNumber());
                    continue;
                }
                logger.warn("Failed to delete KEK {} (v{}) from provider during KMS key deletion: {}",
                        kekVersion.getKekLabel(), kekVersion.getVersionNumber(), e.getMessage());
                ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(),
                        key.getAccountId(), EventVO.LEVEL_WARN, EventTypes.EVENT_KMS_KEY_DELETE, true,
                        String.format("Failed to delete KEK %s from provider during KMS key deletion", kekVersion.getKekLabel()),
                        key.getId(), ApiCommandResourceType.KmsKey.toString(), CallContext.current().getStartEventId());
                // Keep the KMS key so the KEK material isn't left in the HSM without any reference to it
                throw new CloudRuntimeException(String.format("Failed to delete KEK %s from provider, KMS key %s was "
                        + "not deleted: %s", kekVersion.getKekLabel(), key.getUuid(), e.getMessage()), e);
            }
        }

        kmsKeyDao.remove(key.getId());
        logger.info("Deleted KMS key {}", key);
    }

    @Override
    public String rotateKMSKey(RotateKMSKeyCmd cmd) throws KMSException {
        Account caller = CallContext.current().getCallingAccount();
        Integer keyBits = cmd.getKeyBits();
        Long hsmProfileId = cmd.getHsmProfileId();

        KMSKeyVO kmsKey = findKMSKeyAndCheckAccess(cmd.getId(), caller);

        if (!kmsKey.isEnabled()) {
            throw new InvalidParameterValueException("KMS key is not enabled: " + kmsKey);
        }

        HSMProfileVO profile = null;
        if (hsmProfileId != null) {
            profile = hsmProfileDao.findById(hsmProfileId);
            if (profile == null) {
                throw new InvalidParameterValueException("Target HSM Profile not found: " + hsmProfileId);
            }
            checkHSMProfileAccess(caller, profile, false);
            validateProfileScopeForOwner(profile, kmsKey.getAccountId(), kmsKey.getDomainId());
            if (!profile.isEnabled()) {
                throw new InvalidParameterValueException("HSM profile is not enabled: " + profile.getName());
            }
        }

        int newKeyBits = keyBits != null ? keyBits : kmsKey.getKeyBits();
        KMSKekVersionVO currentActive = getActiveKekVersion(kmsKey.getId());

        rotateKek(
                kmsKey,
                currentActive.getKekLabel(),
                null, // auto-generate new label
                newKeyBits,
                profile);

        KMSKekVersionVO newVersion = getActiveKekVersion(kmsKey.getId());
        CallContext.current().setEventDetails(String.format("Rotated KMS key: %s (uuid: %s) from version %d to %d", kmsKey.getName(), kmsKey.getUuid(), currentActive.getVersionNumber(), newVersion.getVersionNumber()));

        logger.info("KMS key rotation initiated: {} -> new KEK version {} (UUID: {}). " +
                        "Background job will gradually rewrap {} wrapped key(s)",
                kmsKey, newVersion.getVersionNumber(), newVersion.getUuid(),
                kmsWrappedKeyDao.countByKmsKeyId(kmsKey.getId()));

        return newVersion.getUuid();
    }

    String rotateKek(KMSKeyVO kmsKey, String oldKekLabel, String newKekLabel, int keyBits,
                     HSMProfileVO newHSMProfile) throws KMSException {
        if (StringUtils.isEmpty(oldKekLabel)) {
            throw KMSException.invalidParameter("oldKekLabel must be specified");
        }

        if (newHSMProfile == null) {
            newHSMProfile = hsmProfileDao.findById(kmsKey.getHsmProfileId());
        }

        KMSProvider provider = getKMSProvider(newHSMProfile.getProtocol());

        try {
            logger.info("Starting KEK rotation from {} to {} for kms key {}", oldKekLabel, newKekLabel, kmsKey);

            if (StringUtils.isEmpty(newKekLabel)) {
                List<KMSKekVersionVO> existingVersions = kmsKekVersionDao.listByKmsKeyId(kmsKey.getId());
                int nextVersion = existingVersions.stream().mapToInt(KMSKekVersionVO::getVersionNumber).max().orElse(0)
                        + 1;
                newKekLabel = kmsKey.getPurpose().generateKekLabel(kmsKey.getDomainId(), kmsKey.getAccountId(),
                        kmsKey.getUuid(), nextVersion);
            }
            final KMSKekVersionVO newVersionEntity = new KMSKekVersionVO(kmsKey.getId(), newKekLabel);

            String finalNewKekLabel = newKekLabel;
            Long newProfileId = newHSMProfile.getId();
            final HSMProfileVO finalHSMProfile = newHSMProfile;
            String newKekId = retryOperation(
                    () -> provider.createKek(kmsKey.getPurpose(), finalNewKekLabel, keyBits, newProfileId));
            verifyKekCanWrap(provider, kmsKey.getPurpose(), newKekId, finalHSMProfile);
            String newWrapAlgorithm = provider.getWrapAlgorithm(newProfileId);

            try {
                KMSKekVersionVO newVersion = Transaction
                        .execute((TransactionCallbackWithException<KMSKekVersionVO, KMSException>) status -> {
                            newVersionEntity.setKmsKeyId(kmsKey.getId());
                            newVersionEntity.setHsmProfileId(newProfileId);
                            newVersionEntity.setKekLabel(finalNewKekLabel);
                            KMSKekVersionVO version = createKekVersion(newVersionEntity);

                            if (!newProfileId.equals(kmsKey.getHsmProfileId())) {
                                kmsKey.setHsmProfileId(newProfileId);
                                logger.info("Updated KMS key {} to use HSM profile {}", kmsKey, finalHSMProfile);
                            }
                            kmsKey.setKekLabel(finalNewKekLabel);
                            kmsKey.setAlgorithm(newWrapAlgorithm);
                            kmsKeyDao.update(kmsKey.getId(), kmsKey);
                            return version;
                        });

                logger.info("KEK rotation: KMS key {} now has {} versions (active: v{}, previous: v{})",
                        kmsKey, newVersion.getVersionNumber(), newVersion.getVersionNumber(),
                        newVersion.getVersionNumber() - 1);

                return newKekId;
            } catch (KMSException e) {
                logger.error(
                        "Database update failed during KEK rotation for kmsKey {}. Attempting to delete orphaned KEK "
                                + "{} from provider {}",
                        kmsKey, newKekId, provider.getProviderName());
                try {
                    // The version row was rolled back, so the provider can't find the KEK's profile by itself.
                    provider.deleteKek(newKekId, newProfileId);
                } catch (KMSException ex) {
                    logger.error("Failed to delete orphaned KEK {} from provider {} after DB failure: {}",
                            newKekId, provider.getProviderName(), ex.getMessage());
                    ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(),
                            kmsKey.getAccountId(), EventVO.LEVEL_WARN, EventTypes.EVENT_KMS_KEY_ROTATE, true,
                            String.format("Failed to delete orphaned KEK %s from provider after DB rollback failure", newKekId),
                            kmsKey.getId(), ApiCommandResourceType.KmsKey.toString(), CallContext.current().getStartEventId());
                }
                throw e;
            }

        } catch (Exception e) {
            logger.error("KEK rotation failed for kmsKey {}: {}", kmsKey, e.getMessage());
            throw handleKmsException(e);
        }
    }

    /**
     * Wraps and unwraps a random key with a newly created KEK, so an HSM that doesn't support the profile's wrap
     * algorithm (e.g. GCM with a caller-supplied IV in FIPS mode) fails here rather than at the first volume
     * creation or rewrap. On failure the KEK is deleted again.
     */
    private void verifyKekCanWrap(KMSProvider provider, KeyPurpose purpose, String kekLabel, HSMProfileVO profile) {
        String algorithm = null;
        byte[] testKey = new byte[32];
        new SecureRandom().nextBytes(testKey);
        byte[] unwrapped = null;
        try {
            algorithm = provider.getWrapAlgorithm(profile.getId());
            WrappedKey wrapped = provider.wrapKey(testKey, purpose, kekLabel, profile.getId());
            unwrapped = provider.unwrapKey(wrapped, profile.getId());
            if (!Arrays.equals(testKey, unwrapped)) {
                throw KMSException.wrapUnwrapFailed("unwrapped key does not match the original");
            }
        } catch (Exception e) {
            try {
                provider.deleteKek(kekLabel, profile.getId());
            } catch (Exception ex) {
                logger.warn("Failed to delete KEK {} from HSM profile {} after a failed wrap test: {}", kekLabel,
                        profile, ex.getMessage());
            }
            throw KMSException.wrapUnwrapFailed(String.format("HSM profile %s could not wrap and unwrap a key with %s: %s",
                    profile.getName(), algorithm, e.getMessage()), e);
        } finally {
            Arrays.fill(testKey, (byte) 0);
            if (unwrapped != null) {
                Arrays.fill(unwrapped, (byte) 0);
            }
        }
    }

    private KMSKekVersionVO createKekVersion(KMSKekVersionVO newVersion) throws KMSException {
        List<KMSKekVersionVO> existingVersions = kmsKekVersionDao.listByKmsKeyId(newVersion.getKmsKeyId());
        int nextVersion = existingVersions.stream()
                .mapToInt(KMSKekVersionVO::getVersionNumber)
                .max()
                .orElse(0) + 1;

        KMSKekVersionVO currentActive = kmsKekVersionDao.getActiveVersion(newVersion.getKmsKeyId());
        if (currentActive != null) {
            currentActive.setStatus(KMSKekVersionVO.Status.Previous);
            kmsKekVersionDao.update(currentActive.getId(), currentActive);
        }

        newVersion.setVersionNumber(nextVersion);
        newVersion.setStatus(KMSKekVersionVO.Status.Active);
        newVersion = kmsKekVersionDao.persist(newVersion);
        logger.info("Created KEK version {} for KMS key {}", nextVersion, newVersion.getKmsKeyId());
        return newVersion;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_VOLUME_MIGRATE_TO_KMS, eventDescription = "Migrating Volumes to KMS")
    public int migrateVolumesToKMS(MigrateVolumesToKMSCmd cmd) throws KMSException {
        Account caller = CallContext.current().getCallingAccount();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long kmsKeyId = cmd.getKmsKeyId();
        List<Long> volumeIds = cmd.getVolumeIds();

        if (domainId != null && StringUtils.isBlank(accountName)) {
            throw new InvalidParameterValueException("accountName must be specified when domainId is provided");
        }

        if (CollectionUtils.isEmpty(volumeIds)) {
            throw new InvalidParameterValueException("volumeIds must be specified");
        }

        if (kmsKeyId == null) {
            throw new InvalidParameterValueException("kmsKeyId must be specified");
        }

        KMSKeyVO kmsKey = findKMSKeyAndCheckAccess(kmsKeyId, caller);

        if (!kmsKey.isEnabled()) {
            throw new InvalidParameterValueException("KMS key is not enabled: " + kmsKey.getUuid());
        }

        if (kmsKey.getPurpose() != KeyPurpose.VOLUME_ENCRYPTION) {
            throw new InvalidParameterValueException("KMS key purpose must be VOLUME_ENCRYPTION");
        }

        KMSProvider provider;
        if (kmsKey.getHsmProfileId() != null) {
            HSMProfileVO profile = getHSMProfile(kmsKey.getHsmProfileId());
            if (!profile.isEnabled()) {
                throw new InvalidParameterValueException("HSM profile is not enabled: " + profile.getName());
            }
            provider = getKMSProvider(profile.getProtocol());
        } else {
            provider = getKMSProvider("database");
        }

        KMSKekVersionVO activeVersion = getActiveKekVersion(kmsKey.getId());

        Long accountId = null;
        if (accountName != null) {
            accountId = accountManager.finalizeAccountId(accountName, domainId, null, true);
        }

        int successCount = 0;
        int failureCount = 0;
        logger.info("Starting migration of volumes to KMS (zone: {}, account: {}, domain: {})",
                kmsKey.getZoneId(), accountId, domainId);

        List<VolumeVO> volumes;
        volumes = volumeDao.listByIds(volumeIds);
        accountManager.checkAccess(caller, null, true, volumes.toArray(new Volume[0]));

        List<String> mismatchedVolumeUuids = new ArrayList<>();
        for (VolumeVO volume : volumes) {
            if (volume.getAccountId() != kmsKey.getAccountId()) {
                mismatchedVolumeUuids.add(volume.getUuid());
            }
        }
        if (!mismatchedVolumeUuids.isEmpty()) {
            throw new InvalidParameterValueException(
                    "The following volumes do not belong to the same account as the KMS key: " +
                            String.join(", ", mismatchedVolumeUuids));
        }

        for (VolumeVO volume : volumes) {
            try {
                if (migrateVolumeToKmsKey(provider, volume, kmsKey, activeVersion)) {
                    successCount++;
                    logger.debug("Migrated volume's encryption {} to KMS {}", volume, kmsKey);
                }
            } catch (Exception e) {
                failureCount++;
                logger.warn("Failed to migrate volume {}: {}", volume.getId(), e.getMessage());
            }
        }

        String details = String.format("Migrated %d volumes to KMS key: %s (uuid: %s). Success: %d, Failures: %d",
                successCount + failureCount, kmsKey.getName(), kmsKey.getUuid(), successCount, failureCount);
        logger.info(details);
        CallContext.current().setEventDetails(details);
        return successCount;
    }

    boolean migrateVolumeToKmsKey(KMSProvider provider, VolumeVO volume, KMSKey kmsKey,
                                          KMSKekVersionVO activeVersion) {
        if (volume.getAccountId() != kmsKey.getAccountId()) {
            throw new InvalidParameterValueException(
                    "Volume " + volume.getUuid() + " does not belong to the same account as KMS key " + kmsKey.getUuid());
        }
        PassphraseVO passphrase = passphraseDao.findById(volume.getPassphraseId());
        if (passphrase == null) {
            logger.warn(
                    "Skipping migration of volume from to the KMS key {} because passphrase id: {} not found for "
                            + "volume {}",
                    kmsKey, volume.getPassphraseId(), volume);
            return false;
        }

        // The legacy passphrase is stored as base64 text, and that text is what qemu/libvirt get as the LUKS
        // passphrase. VolumeObject.getPassphrase() base64-encodes an unwrapped KMS key, so wrap the decoded bytes
        // for the volume to keep exactly the same passphrase.
        byte[] passphraseBytes = Base64.getDecoder().decode(passphrase.getPassphraseString());
        try {
            WrappedKey wrappedKey = provider.wrapKey(
                    passphraseBytes,
                    KeyPurpose.VOLUME_ENCRYPTION,
                    activeVersion.getKekLabel(),
                    activeVersion.getHsmProfileId());

            Long passphraseId = volume.getPassphraseId();
            Transaction.execute((TransactionCallbackWithException<Void, KMSException>) status -> {
                KMSWrappedKeyVO wrappedKeyVO = new KMSWrappedKeyVO(
                        kmsKey.getId(),
                        activeVersion.getId(),
                        volume.getDataCenterId(),
                        wrappedKey.getWrappedKeyMaterial());
                wrappedKeyVO.setWrapAlgorithm(wrappedKey.getAlgorithm());
                wrappedKeyVO = kmsWrappedKeyDao.persist(wrappedKeyVO);

                volume.setKmsWrappedKeyId(wrappedKeyVO.getId());
                volume.setKmsKeyId(kmsKey.getId());
                volume.setPassphraseId(null);
                volumeDao.update(volume.getId(), volume);

                // Don't leave the volume key behind in the passphrase table, unless another volume still uses it
                if (volumeDao.listIncludingRemovedByPassphraseId(passphraseId).isEmpty()) {
                    passphraseDao.remove(passphraseId);
                }
                return null;
            });

            ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(),
                    kmsKey.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_VOLUME_MIGRATE_TO_KMS, true,
                    String.format("Successfully migrated volume encryption key to KMS key %s (v%d)", kmsKey.getUuid(), activeVersion.getVersionNumber()),
                    volume.getId(), ApiCommandResourceType.Volume.toString(), CallContext.current().getStartEventId());
            return true;
        } finally {
            if (passphraseBytes != null) {
                Arrays.fill(passphraseBytes, (byte) 0);
            }
        }
    }

    @Override
    public boolean deleteKMSKeysByAccountId(Long accountId) {
        if (accountId == null) {
            logger.warn("Cannot delete KMS keys: account ID is null");
            return false;
        }

        try {
            List<KMSKeyVO> accountKeys = kmsKeyDao.listByAccount(accountId, null, null);

            if (accountKeys == null || accountKeys.isEmpty()) {
                logger.debug("No KMS keys found for account {}", accountId);
                return true;
            }

            logger.info("Deleting {} KMS key(s) for account {}", accountKeys.size(), accountId);

            boolean allDeleted = true;
            for (KMSKeyVO key : accountKeys) {
                try {
                    List<KMSKekVersionVO> kekVersions = kmsKekVersionDao.listByKmsKeyId(key.getId());
                    if (kekVersions != null && !kekVersions.isEmpty()) {
                        logger.debug("Deleting {} KEK version(s) from provider for KMS key {}",
                                kekVersions.size(), key.getUuid());
                        for (KMSKekVersionVO kekVersion : kekVersions) {
                            HSMProfileVO hsmProfile = hsmProfileDao.findById(kekVersion.getHsmProfileId());
                            try {
                                KMSProvider provider = getKMSProvider(hsmProfile.getProtocol());
                                provider.deleteKek(kekVersion.getKekLabel());
                                logger.debug("Deleted KEK {} (v{}) from provider",
                                        kekVersion.getKekLabel(), kekVersion.getVersionNumber());
                            } catch (Exception e) {
                                logger.warn("Failed to delete KEK {} from provider: {}",
                                        kekVersion.getKekLabel(), e.getMessage());
                                ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(),
                                        key.getAccountId(), EventVO.LEVEL_WARN, EventTypes.EVENT_KMS_KEY_DELETE, true,
                                        String.format("Failed to delete KEK %s from provider during account KMS cleanup", kekVersion.getKekLabel()),
                                        key.getId(), ApiCommandResourceType.KmsKey.toString(), CallContext.current().getStartEventId());
                            }
                        }
                    }

                    // CASCADE deletes KEK versions and wrapped keys
                    boolean deleted = kmsKeyDao.remove(key.getId());
                    if (deleted) {
                        logger.debug("Deleted KMS key {} as part of account {} cleanup", key.getUuid(), accountId);
                    } else {
                        logger.warn("Failed to delete KMS key {} as part of account {} cleanup",
                                key.getUuid(), accountId);
                        allDeleted = false;
                    }
                } catch (Exception e) {
                    logger.error("Error deleting KMS key {} for account {}: {}",
                            key.getUuid(), accountId, e.getMessage(), e);
                    allDeleted = false;
                }
            }

            if (allDeleted) {
                logger.info("Successfully deleted all KMS keys for account {}", accountId);
            } else {
                logger.warn("Some KMS keys for account {} could not be deleted", accountId);
            }

            return allDeleted;
        } catch (Exception e) {
            logger.error("Error during KMS key cleanup for account {}: {}", accountId, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_HSM_PROFILE_CREATE, eventDescription = "Adding HSM profile")
    public HSMProfile addHSMProfile(CreateHSMProfileCmd cmd) throws KMSException {
        Account caller = CallContext.current().getCallingAccount();

        String protocol = cmd.getProtocol();
        if (StringUtils.isEmpty(protocol)) {
            throw new InvalidParameterValueException("Protocol cannot be empty");
        }

        KMSProvider provider;
        try {
            provider = getKMSProvider(protocol);
        } catch (CloudRuntimeException e) {
            throw new InvalidParameterValueException("No provider found for protocol: " + protocol);
        }

        Map<String, String> details = cmd.getDetails() != null ? cmd.getDetails() : new HashMap<>();
        provider.validateProfileConfig(details);

        boolean isPublic = cmd.getIsPublic();
        if (isPublic && !accountManager.isRootAdmin(caller.getId())) {
            throw new PermissionDeniedException("Only root admins can create system HSM profiles");
        }

        Long accountId;
        Long domainId;
        if (StringUtils.isNotBlank(cmd.getAccountName()) || cmd.getProjectId() != null) {
            // Account- or project-scoped profile
            Account targetAccount = accountManager.finalizeOwner(caller, cmd.getAccountName(), cmd.getDomainId(),
                    cmd.getProjectId());
            accountId = targetAccount.getId();
            domainId = targetAccount.getDomainId();
        } else if (cmd.getDomainId() != null) {
            // Domain-scoped profile: account-less, usable by all accounts directly in the domain.
            if (domainDao.findById(cmd.getDomainId()) == null) {
                throw new InvalidParameterValueException("Domain not found: " + cmd.getDomainId());
            }
            accountId = null;
            domainId = cmd.getDomainId();
        } else {
            // Fallback: owned by the caller's account.
            Account targetAccount = accountManager.finalizeOwner(caller, null, null, null);
            accountId = targetAccount.getId();
            domainId = targetAccount.getDomainId();
        }

        HSMProfileVO profile = new HSMProfileVO(
                cmd.getName(),
                protocol,
                accountId,
                domainId,
                cmd.getZoneId(),
                cmd.getVendorName());
        profile.setIsPublic(isPublic);
        profile = hsmProfileDao.persist(profile);

        // The validated details, which include the defaults the provider filled in (e.g. the wrap algorithm)
        for (Map.Entry<String, String> entry : details.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (isSensitiveKey(key)) {
                value = DBEncryptionUtil.encrypt(value);
            }

            hsmProfileDetailsDao.persist(profile.getId(), key, value);
        }

        CallContext.current().setEventResourceId(profile.getId());
        CallContext.current().setEventResourceType(ApiCommandResourceType.HsmProfile);
        CallContext.current().setEventDetails(String.format("Created HSM profile: %s (uuid: %s)", profile.getName(), profile.getUuid()));

        return profile;
    }

    @Override
    public ListResponse<HSMProfileResponse> listHSMProfiles(ListHSMProfilesCmd cmd) {
        Account caller = CallContext.current().getCallingAccount();
        if (caller == null) {
            return new ListResponse<>();
        }

        List<Long> permittedAccounts = new ArrayList<>();
        Ternary<Long, Boolean, ListProjectResourcesCriteria> domainIdRecursiveListProject = new Ternary<>(
                cmd.getDomainId(), cmd.isRecursive(), null);
        accountManager.buildACLSearchParameters(caller, cmd.getId(), cmd.getAccountName(),
                cmd.getProjectId(), permittedAccounts, domainIdRecursiveListProject,
                cmd.listAll(), false);
        Long domainId = domainIdRecursiveListProject.first();
        Boolean isRecursive = domainIdRecursiveListProject.second();
        ListProjectResourcesCriteria listProjectResourcesCriteria = domainIdRecursiveListProject.third();

        SearchBuilder<HSMProfileVO> sb = getSearchBuilderForHSMProfiles(caller, domainId, isRecursive,
                permittedAccounts, listProjectResourcesCriteria, cmd.listAll());
        SearchCriteria<HSMProfileVO> sc = getSearchCriteriaForHSMProfiles(sb, cmd, caller, domainId, isRecursive,
                permittedAccounts, listProjectResourcesCriteria);

        Filter searchFilter = new Filter(HSMProfileVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Pair<List<HSMProfileVO>, Integer> result = hsmProfileDao.searchAndCount(sc, searchFilter);
        List<HSMProfileVO> profiles = result.first();
        Integer totalCount = result.second();

        List<HSMProfileResponse> responses = new ArrayList<>();

        boolean isRootAdmin = accountManager.isRootAdmin(caller.getId());
        for (HSMProfileVO profile : profiles) {
            // When isPublic=true, non-admin users explicitly requested public profiles, so
            // don't mark as limited
            // When listall=true, also don't mark as limited since user requested all
            // profiles
            // If the profile is owned by the user, they should see full details even if it
            // is a system profile
            boolean limited = profile.getIsPublic() && !isRootAdmin && profile.getAccountId() != caller.getId();
            responses.add(createHSMProfileResponse(profile, limited));
        }

        ListResponse<HSMProfileResponse> listResponse = new ListResponse<>();
        listResponse.setResponses(responses, totalCount);
        return listResponse;
    }

    SearchBuilder<HSMProfileVO> getSearchBuilderForHSMProfiles(Account caller, Long domainId, Boolean isRecursive,
                                                               List<Long> permittedAccounts,
                                                               ListProjectResourcesCriteria listProjectResourcesCriteria,
                                                               boolean listAll) {
        SearchBuilder<HSMProfileVO> sb = hsmProfileDao.createSearchBuilder();

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("zoneId", sb.entity().getZoneId(), SearchCriteria.Op.EQ);
        sb.and("protocol", sb.entity().getProtocol(), SearchCriteria.Op.EQ);
        sb.and("enabled", sb.entity().isEnabled(), SearchCriteria.Op.EQ);
        sb.and("isPublic", sb.entity().getIsPublic(), SearchCriteria.Op.EQ);
        sb.and("name", sb.entity().getName(), SearchCriteria.Op.LIKE);

        if (listProjectResourcesCriteria == ListProjectResourcesCriteria.SkipProjectResources) {
            sb.and().op("accountIdNIN", sb.entity().getAccountId(), SearchCriteria.Op.NIN);
            sb.or("accountIdNULL", sb.entity().getAccountId(), SearchCriteria.Op.NULL);
            sb.cp();
        }

        boolean isRootAdmin = accountManager.isRootAdmin(caller.getId());

        if (!isRootAdmin) {
            if (listAll) {
                // (isPublic = true OR accountId/domainId IN (...) OR (account_id IS NULL AND domain_id = ?))
                sb.and().op("isPublicACL", sb.entity().getIsPublic(), SearchCriteria.Op.EQ);
                if (!permittedAccounts.isEmpty()) {
                    sb.or("accountIdIN", sb.entity().getAccountId(), SearchCriteria.Op.IN);
                } else if (domainId != null) {
                    if (isRecursive) {
                        sb.or("domainIdIN", sb.entity().getDomainId(), SearchCriteria.Op.IN);
                    } else {
                        sb.or("domainIdEQ", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
                    }
                }
                sb.or().op("sharedAccountNull", sb.entity().getAccountId(), SearchCriteria.Op.NULL);
                sb.and("sharedDomainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
                sb.cp();
                sb.cp();
            } else {
                // No listAll: profiles the user/domain owns, OR domain-shared for caller's domain
                if (!permittedAccounts.isEmpty()) {
                    sb.and().op("accountIdIN", sb.entity().getAccountId(), SearchCriteria.Op.IN);
                    sb.or().op("sharedAccountNull", sb.entity().getAccountId(), SearchCriteria.Op.NULL);
                    sb.and("sharedDomainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
                    sb.cp();
                    sb.cp();
                } else if (domainId != null) {
                    if (isRecursive) {
                        sb.and().op("domainIdIN", sb.entity().getDomainId(), SearchCriteria.Op.IN);
                    } else {
                        sb.and().op("domainIdEQ", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
                    }
                    sb.or().op("sharedAccountNull", sb.entity().getAccountId(), SearchCriteria.Op.NULL);
                    sb.and("sharedDomainId", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
                    sb.cp();
                    sb.cp();
                }
            }
        } else {
            if (!permittedAccounts.isEmpty()) {
                sb.and("accountIdIN", sb.entity().getAccountId(), SearchCriteria.Op.IN);
            } else if (domainId != null) {
                if (isRecursive) {
                    sb.and("domainIdIN", sb.entity().getDomainId(), SearchCriteria.Op.IN);
                } else {
                    sb.and("domainIdEQ", sb.entity().getDomainId(), SearchCriteria.Op.EQ);
                }
            }
        }

        sb.done();
        return sb;
    }

    SearchCriteria<HSMProfileVO> getSearchCriteriaForHSMProfiles(SearchBuilder<HSMProfileVO> searchBuilder,
                                                                 ListHSMProfilesCmd cmd, Account caller, Long domainId, Boolean isRecursive, List<Long> permittedAccounts,
                                                                 ListProjectResourcesCriteria listProjectResourcesCriteria) {
        SearchCriteria<HSMProfileVO> sc = searchBuilder.create();
        sc.setParametersIfNotNull("id", cmd.getId());
        sc.setParametersIfNotNull("zoneId", cmd.getZoneId());
        sc.setParametersIfNotNull("protocol", cmd.getProtocol());
        sc.setParametersIfNotNull("enabled", cmd.getEnabled());
        sc.setParametersIfNotNull("isPublic", cmd.getIsPublic());
        if (cmd.getKeyword() != null) {
            sc.setParameters("name", "%" + cmd.getKeyword() + "%");
        }

        if (listProjectResourcesCriteria == ListProjectResourcesCriteria.SkipProjectResources) {
            SearchCriteria<AccountVO> projectAcctSC = accountDao.createSearchCriteria();
            projectAcctSC.addAnd("type", SearchCriteria.Op.EQ, Account.Type.PROJECT);
            if (domainId != null) {
                if (isRecursive) {
                    List<Long> domainIds = domainDao.getDomainAndChildrenIds(domainId);
                    projectAcctSC.addAnd("domainId", SearchCriteria.Op.IN, domainIds.toArray());
                } else {
                    projectAcctSC.addAnd("domainId", SearchCriteria.Op.EQ, domainId);
                }
            }
            List<AccountVO> projectAccounts = accountDao.search(projectAcctSC, null);
            if (CollectionUtils.isNotEmpty(projectAccounts)) {
                List<Long> projectAccountIds = projectAccounts.stream().map(AccountVO::getId).collect(Collectors.toList());
                sc.setParameters("accountIdNIN", projectAccountIds.toArray());
            } else {
                // No project accounts exist; use a dummy ID so the NIN condition has no effect
                sc.setParameters("accountIdNIN", -1L);
            }
        }

        boolean isRootAdmin = accountManager.isRootAdmin(caller.getId());

        if (!isRootAdmin) {
            if (cmd.listAll()) {
                sc.setParameters("isPublicACL", true);
            }

            // Domain-shared profiles (account_id IS NULL AND domain_id = caller's domain) are visible
            // to all accounts directly in the caller's domain, alongside public and owned profiles.
            sc.setParameters("sharedDomainId", caller.getDomainId());

            if (!permittedAccounts.isEmpty()) {
                sc.setParameters("accountIdIN", permittedAccounts.toArray());
            } else if (domainId != null) {
                if (isRecursive) {
                    List<Long> domainIds = domainDao.getDomainAndChildrenIds(domainId);
                    sc.setParameters("domainIdIN", domainIds.toArray());
                } else {
                    sc.setParameters("domainIdEQ", domainId);
                }
            }
        } else {
            if (!permittedAccounts.isEmpty()) {
                sc.setParameters("accountIdIN", permittedAccounts.toArray());
            } else if (domainId != null) {
                if (isRecursive) {
                    List<Long> domainIds = domainDao.getDomainAndChildrenIds(domainId);
                    sc.setParameters("domainIdIN", domainIds.toArray());
                } else {
                    sc.setParameters("domainIdEQ", domainId);
                }
            }
        }
        return sc;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_HSM_PROFILE_DELETE, eventDescription = "Deleting HSM profile")
    public boolean deleteHSMProfile(DeleteHSMProfileCmd cmd) throws KMSException {
        HSMProfileVO profile = getHSMProfile(cmd.getId());
        Account caller = CallContext.current().getCallingAccount();
        checkHSMProfileAccess(caller, profile, true);

        if (profile.getProtocol().equals(DATABASE_PROVIDER_NAME)
                && profile.getAccountId() == Account.ACCOUNT_ID_SYSTEM) {
            throw new InvalidParameterValueException("Default database HSM profile cannot be deleted");
        }

        long keyCount = kmsKeyDao.countByHsmProfileId(profile.getId());
        if (keyCount > 0) {
            throw new InvalidParameterValueException(
                    String.format("Cannot delete HSM profile '%s': it is referenced by %d KMS key(s). " +
                            "Please delete or reassign those keys first.", profile.getName(), keyCount));
        }

        // Check if any KEK versions reference this HSM profile
        List<KMSKekVersionVO> kekVersions = kmsKekVersionDao.listByHsmProfileId(profile.getId());
        if (!kekVersions.isEmpty()) {
            // Check if any wrapped keys are using these KEK versions
            long wrappedKeyCount = 0;
            for (KMSKekVersionVO kekVersion : kekVersions) {
                wrappedKeyCount += kmsWrappedKeyDao.countByKekVersionId(kekVersion.getId());
            }
            if (wrappedKeyCount > 0) {
                throw new InvalidParameterValueException(
                        String.format("Cannot delete HSM profile '%s': it is referenced by %d wrapped key(s) " +
                                        "through KEK versions. Please wait for key rotation to complete or delete those"
                                        + " volumes first.",
                                profile.getName(), wrappedKeyCount));
            }
        }

        getKMSProvider(profile.getProtocol()).invalidateProfileCache(profile.getId());
        hsmProfileDetailsDao.deleteDetails(profile.getId());

        CallContext.current().setEventResourceId(profile.getId());
        CallContext.current().setEventResourceType(ApiCommandResourceType.HsmProfile);
        CallContext.current().setEventDetails(String.format("Deleted HSM profile: %s (uuid: %s)", profile.getName(), profile.getUuid()));

        return hsmProfileDao.remove(profile.getId());
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_HSM_PROFILE_UPDATE, eventDescription = "Updating HSM profile")
    public HSMProfile updateHSMProfile(UpdateHSMProfileCmd cmd) throws KMSException {
        HSMProfileVO profile = getHSMProfile(cmd.getId());
        Account caller = CallContext.current().getCallingAccount();
        checkHSMProfileAccess(caller, profile, true);

        if (cmd.getName() != null) {
            profile.setName(cmd.getName());
        }
        if (cmd.getEnabled() != null) {
            profile.setEnabled(cmd.getEnabled());
        }

        hsmProfileDao.update(profile.getId(), profile);

        String testingOnlyWarning = "";
        if (Boolean.TRUE.equals(cmd.getEnabled()) && DATABASE_PROVIDER_NAME.equalsIgnoreCase(profile.getProtocol())) {
            testingOnlyWarning = ". The database provider stores KEKs in the CloudStack database and is meant for "
                    + "testing only, it is not a substitute for an HSM";
            logger.warn("HSM profile {} was enabled{}", profile, testingOnlyWarning);
        }

        CallContext.current().setEventResourceId(profile.getId());
        CallContext.current().setEventResourceType(ApiCommandResourceType.HsmProfile);
        CallContext.current().setEventDetails(String.format("Updated HSM profile: %s (uuid: %s)%s", profile.getName(),
                profile.getUuid(), testingOnlyWarning));

        return profile;
    }

    @Override
    public HSMProfileResponse createHSMProfileResponse(HSMProfile profile) {
        return createHSMProfileResponse(profile, false);
    }

    private HSMProfileResponse createHSMProfileResponse(HSMProfile profile, boolean limited) {
        HSMProfileResponse response = new HSMProfileResponse();
        response.setId(profile.getUuid());
        response.setName(profile.getName());
        response.setVendorName(profile.getVendorName());
        response.setIsPublic(profile.getIsPublic());

        if (profile.getZoneId() != null) {
            DataCenterVO zone = dataCenterDao.findById(profile.getZoneId());
            if (zone != null) {
                response.setZoneId(zone.getUuid());
                response.setZoneName(zone.getName());
            }
        }

        response.setProtocol(profile.getProtocol());
        response.setEnabled(profile.isEnabled());
        response.setCreated(profile.getCreated());

        if (profile.getAccountId() == -1) {
            // Domain-scoped (account-less) profile: populateOwner would NPE on findAccountById(-1).
            DomainVO domain = profile.getDomainId() == -1 ? null : domainDao.findById(profile.getDomainId());
            if (domain != null) {
                response.setDomainId(domain.getUuid());
                response.setDomainName(domain.getName());
                response.setDomainPath(ApiResponseHelper.getPrettyDomainPath(domain.getPath()));
            }
        } else {
            ApiResponseHelper.populateOwner(response, profile);
        }

        if (!limited) {
            List<HSMProfileDetailsVO> details = hsmProfileDetailsDao.listByProfileId(profile.getId());
            Map<String, String> detailsMap = new HashMap<>();
            for (HSMProfileDetailsVO detail : details) {
                detailsMap.put(detail.getName(), detail.getValue());
            }
            response.setDetails(detailsMap);
        }
        response.setObjectName("hsmprofile");
        return response;
    }

    boolean isSensitiveKey(String key) {
        return KMSProvider.isSensitiveKey(key);
    }

    /**
     * Find an HSM profile by ID, throwing InvalidParameterValueException if not
     * found.
     */
    private HSMProfileVO getHSMProfile(Long profileId) {
        HSMProfileVO profile = hsmProfileDao.findById(profileId);
        if (profile == null) {
            throw new InvalidParameterValueException("HSM Profile not found: " + profileId);
        }
        return profile;
    }

    /**
     * Validate caller's access to an HSM profile.
     * For system profiles: read/use access is open to all; modify access requires
     * root admin.
     * For owned profiles: delegates to ACL checkAccess.
     */
    void checkHSMProfileAccess(Account caller, HSMProfileVO profile, boolean requireModifyAccess) {
        if (profile.getIsPublic()) {
            if (requireModifyAccess && !accountManager.isRootAdmin(caller.getId())) {
                throw new PermissionDeniedException("Only root admins can modify system HSM profiles");
            }
        } else if (profile.getAccountId() == -1 && profile.getDomainId() != -1) {
            // Domain-scoped (account-less) profile: usable by accounts directly in the domain,
            // modifiable only by root admins. Cannot route through accountManager.checkAccess
            // because findById(-1) on the null owner would deny everyone.
            boolean isRootAdmin = accountManager.isRootAdmin(caller.getId());
            if (requireModifyAccess) {
                if (!isRootAdmin) {
                    throw new PermissionDeniedException("Only root admins can modify domain-scoped HSM profiles");
                }
            } else if (!isRootAdmin && caller.getDomainId() != profile.getDomainId()) {
                throw new PermissionDeniedException("Access denied to HSM profile");
            }
        } else {
            accountManager.checkAccess(caller, null, requireModifyAccess, profile);
        }
    }

    /**
     * Validate that an HSM profile's scope is compatible with a prospective KMS key owner.
     * This is a consistency rule (not a caller-permission check): it applies to all callers,
     * including root admins, so a key cannot be bound to a profile outside its scope.
     * - public profile: usable by any owner.
     * - domain-scoped (account-less) profile: the owner must be directly in the profile's domain.
     * - account-owned profile: the owner must be that account.
     */
    void validateProfileScopeForOwner(HSMProfileVO profile, long ownerAccountId, long ownerDomainId) {
        if (profile.getIsPublic()) {
            return;
        }
        if (profile.getAccountId() == -1 && profile.getDomainId() != -1) {
            if (ownerDomainId != profile.getDomainId()) {
                throw new InvalidParameterValueException(String.format(
                        "HSM profile '%s' is domain-scoped and can only be used by accounts in its domain",
                        profile.getName()));
            }
        } else if (profile.getAccountId() != -1) {
            if (ownerAccountId != profile.getAccountId()) {
                throw new InvalidParameterValueException(String.format(
                        "HSM profile '%s' is owned by another account and cannot be used for this key's owner",
                        profile.getName()));
            }
        }
    }

    /**
     * Parse and validate a key purpose string. Returns null if the input is null.
     */
    KeyPurpose parseKeyPurpose(String purpose) {
        if (purpose == null) {
            return null;
        }
        try {
            return KeyPurpose.fromString(purpose);
        } catch (IllegalArgumentException e) {
            throw new InvalidParameterValueException(
                    "Invalid purpose: " + purpose + ". Valid values: volume, tls");
        }
    }

    <T> T retryOperation(KmsOperation<T> operation) throws Exception {
        int maxRetries = getRetryCount();
        int retryDelay = getRetryDelayMs();
        int timeoutSec = getOperationTimeoutSec();

        Exception lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            Future<T> future = kmsOperationExecutor.submit(operation::execute);
            try {
                return future.get(timeoutSec, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                // Note: if the underlying provider makes a native (JNI/JNA) call, the daemon
                // thread may remain blocked until the native call returns even after cancel —
                // this is a known JVM limitation. The caller is unblocked regardless.
                lastException = KMSException.transientError(
                        "KMS operation timed out after " + timeoutSec + "s", e);
                logger.warn("KMS operation timed out (attempt {}/{}), timeout={}s",
                        attempt + 1, maxRetries + 1, timeoutSec);
            } catch (ExecutionException e) {
                future.cancel(true);
                Throwable cause = e.getCause();
                lastException = (cause instanceof Exception) ? (Exception) cause : e;

                if (lastException instanceof KMSException && !((KMSException) lastException).isRetryable()) {
                    throw lastException;
                }

                logger.warn("KMS operation failed (attempt {}/{}): {}",
                        attempt + 1, maxRetries + 1, lastException.getMessage());
            } catch (InterruptedException e) {
                future.cancel(true);
                Thread.currentThread().interrupt();
                throw new CloudRuntimeException("Interrupted while waiting for KMS operation", e);
            }

            if (attempt < maxRetries) {
                try {
                    Thread.sleep((long) retryDelay * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new CloudRuntimeException("Interrupted during KMS retry delay", ie);
                }
            } else {
                logger.error("KMS operation failed after {} attempt(s)", maxRetries + 1);
            }
        }

        if (lastException != null) {
            throw lastException;
        }

        throw new CloudRuntimeException("KMS operation failed with no exception details");
    }

    protected int getOperationTimeoutSec() {
        return KMSOperationTimeoutSec.value();
    }

    protected int getRetryCount() {
        return KMSRetryCount.value();
    }

    protected int getRetryDelayMs() {
        return KMSRetryDelayMs.value();
    }

    private KMSException handleKmsException(Exception e) {
        if (e instanceof KMSException) {
            return (KMSException) e;
        }
        return KMSException.transientError("KMS operation failed: " + e.getMessage(), e);
    }

    /**
     * Find a KMS key by ID and verify the caller has access to it.
     * Throws {@link InvalidParameterValueException} if the key does not exist
     * and {@link PermissionDeniedException} if the caller lacks access.
     *
     * @return the resolved {@link KMSKeyVO}
     */
    KMSKeyVO findKMSKeyAndCheckAccess(Long keyId, Account caller) {
        KMSKeyVO key = kmsKeyDao.findById(keyId);
        if (key == null) {
            throw new InvalidParameterValueException("KMS key not found: " + keyId);
        }
        accountManager.checkAccess(caller, null, true, key);
        return key;
    }

    public void setKmsProviders(List<KMSProvider> kmsProviders) {
        this.kmsProviders = kmsProviders;
        initializeKmsProviderMap();
    }

    private void initializeKmsProviderMap() {
        if (kmsProviders == null) {
            return;
        }
        kmsProviderMap.clear();
        for (KMSProvider provider : kmsProviders) {
            if (provider != null) {
                kmsProviderMap.put(provider.getProviderName().toLowerCase(), provider);
                logger.info("Registered KMS provider: {}", provider.getProviderName());
            }
        }
    }

    @Override
    public boolean start() {
        super.start();
        initializeKmsProviderMap();
        kmsOperationExecutor = new ThreadPoolExecutor(
                KMSOperationPoolCoreSize.value(),
                KMSOperationPoolMaxSize.value(),
                60L, TimeUnit.SECONDS, new SynchronousQueue<>(), r -> {
            Thread t = new Thread(r, "kms-operation");
            t.setDaemon(true);
            return t;
        });

        for (KMSProvider provider : kmsProviderMap.values()) {
            if (provider != null) {
                try {
                    boolean healthy = provider.healthCheck();
                    if (healthy) {
                        logger.info("KMS provider {} health check passed", provider.getProviderName());
                    } else {
                        logger.warn("KMS provider {} health check failed", provider.getProviderName());
                    }
                } catch (Exception e) {
                    logger.warn("KMS provider {} health check error: {}", provider.getProviderName(), e.getMessage());
                }
            }
        }

        scheduleRewrapWorker();
        scheduleKeyFormatRepair();

        return true;
    }

    private void scheduleRewrapWorker() {
        long intervalMs = KMSRewrapIntervalMs.value();
        if (intervalMs <= 0) {
            return;
        }

        rewrapExecutor = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "KMSRewrapWorker");
            t.setDaemon(true);
            return t;
        });

        rewrapExecutor.scheduleAtFixedRate(new ManagedContextRunnable() {
            @Override
            protected void runInContext() {
                try {
                    processRewrapBatch();
                } catch (final Exception e) {
                    logger.error("Error while running KMS rewrap worker", e);
                }
            }
        }, 10000L, intervalMs, TimeUnit.MILLISECONDS);

        logger.info("KMS rewrap worker scheduled with interval: {} ms", intervalMs);
    }

    /**
     * Finds KEK versions marked as Previous and gradually rewraps wrapped keys
     * using the active version.
     */
    private void processRewrapBatch() {
        GlobalLock lock = GlobalLock.getInternLock("kms.rewrap.worker");
        try {
            if (lock.lock(5)) {
                try {
                    List<KMSKekVersionVO> previousVersions = kmsKekVersionDao
                            .findByStatus(KMSKekVersionVO.Status.Previous);

                    if (previousVersions.isEmpty()) {
                        logger.trace("No KEK versions pending rewrap");
                        return;
                    }

                    logger.debug("Found {} KEK version(s) with status Previous - processing rewrap batches",
                            previousVersions.size());

                    int batchSize = KMSRewrapBatchSize.value();

                    for (KMSKekVersionVO oldVersion : previousVersions) {
                        try {
                            processVersionRewrap(oldVersion, batchSize);
                        } catch (Exception e) {
                            logger.error("Error processing rewrap for KEK version {}: {}", oldVersion, e.getMessage(),
                                    e);
                        }
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                logger.trace("KMS rewrap worker: could not acquire cluster lock, skipping batch");
            }
        } catch (Exception e) {
            logger.error("Error in rewrap worker: {}", e.getMessage(), e);
        } finally {
            lock.releaseRef();
        }
    }

    private void processVersionRewrap(KMSKekVersionVO oldVersion, int batchSize) throws KMSException {
        KMSKeyVO kmsKey = kmsKeyDao.findById(oldVersion.getKmsKeyId());
        if (kmsKey == null) {
            logger.warn("KMS key not found for KEK version {}, skipping", oldVersion);
            return;
        }

        KMSKekVersionVO activeVersion = kmsKekVersionDao.getActiveVersion(oldVersion.getKmsKeyId());
        if (activeVersion == null) {
            logger.warn("No active KEK version found for KMS key {}, skipping", kmsKey);
            return;
        }

        List<KMSWrappedKeyVO> keysToRewrap = kmsWrappedKeyDao.listByKekVersionId(oldVersion.getId(), batchSize);

        if (keysToRewrap.isEmpty()) {
            logger.info("All wrapped keys rewrapped for KEK version {} (v{}) - archiving and deleting from provider",
                    oldVersion.getUuid(), oldVersion.getVersionNumber());

            oldVersion.setStatus(KMSKekVersionVO.Status.Archived);
            kmsKekVersionDao.update(oldVersion.getId(), oldVersion);

            // Delete the old KEK from the HSM since no wrapped keys reference it anymore
            try {
                HSMProfileVO oldProfile = hsmProfileDao.findById(oldVersion.getHsmProfileId());
                if (oldProfile != null) {
                    KMSProvider provider = getKMSProvider(oldProfile.getProtocol());
                    provider.deleteKek(oldVersion.getKekLabel());
                    logger.info("Deleted archived KEK {} (v{}) from provider {}",
                            oldVersion.getKekLabel(), oldVersion.getVersionNumber(), provider.getProviderName());

                    ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(),
                            kmsKey.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_KMS_KEY_DELETE, true,
                            String.format("Deleted archived KEK %s from provider after all wrapped keys were rewrapped", oldVersion.getKekLabel()),
                            kmsKey.getId(), ApiCommandResourceType.KmsKey.toString(), CallContext.current().getStartEventId());
                }
            } catch (Exception e) {
                logger.warn("Failed to delete archived KEK {} (v{}) from provider: {}",
                        oldVersion.getKekLabel(), oldVersion.getVersionNumber(), e.getMessage());
                ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(),
                        kmsKey.getAccountId(), EventVO.LEVEL_WARN, EventTypes.EVENT_KMS_KEY_DELETE, true,
                        String.format("Failed to delete archived KEK %s from provider: %s", oldVersion.getKekLabel(), e.getMessage()),
                        kmsKey.getId(), ApiCommandResourceType.KmsKey.toString(), CallContext.current().getStartEventId());
            }

            return;
        }

        HSMProfileVO hsmProfile = hsmProfileDao.findById(activeVersion.getHsmProfileId());
        KMSProvider provider = getKMSProvider(hsmProfile.getProtocol());

        int successCount = 0;
        int failureCount = 0;

        for (KMSWrappedKeyVO wrappedKeyVO : keysToRewrap) {
            try {
                rewrapSingleKey(wrappedKeyVO, kmsKey, activeVersion, provider);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                logger.warn("Failed to rewrap key {} for KMS key {}: {}",
                        wrappedKeyVO.getId(), kmsKey, e.getMessage());
                // Continue with next key - will retry in next run
            }
        }

        logger.info("Rewrapped batch for KMS key {} (KEK v{} -> v{}): {} success, {} failures",
                kmsKey, oldVersion.getVersionNumber(), activeVersion.getVersionNumber(),
                successCount, failureCount);
    }

    void rewrapSingleKey(KMSWrappedKeyVO wrappedKeyVO, KMSKeyVO kmsKey,
                         KMSKekVersionVO newVersion, KMSProvider provider) {
        byte[] dek = null;
        try {
            dek = unwrapKey(wrappedKeyVO.getId());

            WrappedKey newWrapped = provider.wrapKey(
                    dek,
                    kmsKey.getPurpose(),
                    newVersion.getKekLabel(),
                    newVersion.getHsmProfileId());

            wrappedKeyVO.setKekVersionId(newVersion.getId());
            wrappedKeyVO.setWrappedBlob(newWrapped.getWrappedKeyMaterial());
            wrappedKeyVO.setWrapAlgorithm(newWrapped.getAlgorithm());
            kmsWrappedKeyDao.update(wrappedKeyVO.getId(), wrappedKeyVO);

            ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(), kmsKey.getAccountId(),
                    EventVO.LEVEL_INFO, EventTypes.EVENT_KMS_KEY_WRAP, true,
                    String.format("Rewrapped %s key (wrapped key uuid: %s) with new KEK version %d", kmsKey.getPurpose().getName(), wrappedKeyVO.getUuid(), newVersion.getVersionNumber()),
                    kmsKey.getId(), ApiCommandResourceType.KmsKey.toString(), CallContext.current().getStartEventId());
        } finally {
            if (dek != null) {
                Arrays.fill(dek, (byte) 0);
            }
        }
    }

    /**
     * 4.23 migrated legacy volume passphrases to KMS as their base64 text instead of their raw bytes, so those
     * volumes got a wrong passphrase. The upgrade flags every wrapped key that existed at that point; this task
     * checks each one once and rewraps the affected ones with the raw bytes. A key that can't be unwrapped (e.g. the
     * HSM is down) stays flagged and is retried. The task stops once no flagged key is left.
     */
    private void scheduleKeyFormatRepair() {
        if (kmsWrappedKeyDao.listFormatCheckPending(0, 1).isEmpty()) {
            return;
        }
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "KMSKeyFormatRepair");
            t.setDaemon(true);
            return t;
        });
        keyFormatRepairExecutor = executor;
        executor.scheduleWithFixedDelay(new ManagedContextRunnable() {
            @Override
            protected void runInContext() {
                try {
                    if (repairKeyFormatBatch()) {
                        logger.info("All KMS wrapped keys flagged at the upgrade from 4.23 have been checked");
                        executor.shutdown();
                    }
                } catch (final Exception e) {
                    logger.error("Error while checking the format of KMS wrapped keys", e);
                }
            }
        }, 30, 60, TimeUnit.SECONDS);
    }

    /**
     * Checks the next batch of flagged keys. A batch ends at the first key that can't be checked (e.g. its HSM is
     * down), so the lock shared with the rewrap worker isn't held through retries; the next batch resumes after that
     * key so it doesn't block the others.
     *
     * @return true when no flagged key is left
     */
    boolean repairKeyFormatBatch() {
        GlobalLock lock = GlobalLock.getInternLock("kms.rewrap.worker");
        try {
            if (!lock.lock(5)) {
                return false;
            }
            try {
                List<KMSWrappedKeyVO> keys = kmsWrappedKeyDao.listFormatCheckPending(keyFormatRepairCursor,
                        KMSRewrapBatchSize.value());
                if (keys.isEmpty()) {
                    boolean done = keyFormatRepairCursor == 0;
                    keyFormatRepairCursor = 0;
                    return done;
                }
                for (KMSWrappedKeyVO key : keys) {
                    keyFormatRepairCursor = key.getId();
                    try {
                        repairKeyFormat(key);
                    } catch (Exception e) {
                        logger.warn("Could not check the format of KMS wrapped key {}, will retry: {}", key, e.getMessage());
                        logger.debug("Failed to check the format of KMS wrapped key {}", key, e);
                        break;
                    }
                }
                return false;
            } finally {
                lock.unlock();
            }
        } finally {
            lock.releaseRef();
        }
    }

    private void repairKeyFormat(KMSWrappedKeyVO wrappedKeyVO) {
        byte[] key;
        try {
            key = unwrapKey(wrappedKeyVO.getId());
        } catch (KMSException e) {
            if (e.getErrorType() != KMSException.ErrorType.KEK_NOT_FOUND) {
                throw e;
            }
            // Gone for good, e.g. its KMS key or KEK was deleted, so there is nothing to repair
            logger.warn("KMS wrapped key {} can't be unwrapped anymore, not checking its format: {}", wrappedKeyVO,
                    e.getMessage());
            markFormatChecked(wrappedKeyVO);
            return;
        }
        byte[] passphrase = decodeLegacyMigratedKey(key);
        try {
            if (passphrase != null) {
                rewrapLegacyMigratedKey(wrappedKeyVO, passphrase);
            }
            markFormatChecked(wrappedKeyVO);
        } finally {
            Arrays.fill(key, (byte) 0);
            if (passphrase != null) {
                Arrays.fill(passphrase, (byte) 0);
            }
        }
    }

    private void rewrapLegacyMigratedKey(KMSWrappedKeyVO wrappedKeyVO, byte[] passphrase) {
        List<VolumeVO> volumes = volumeDao.findByKmsWrappedKeyId(wrappedKeyVO.getId());
        List<VolumeVO> createdAfterMigration = volumes.stream()
                .filter(volume -> volume.getCreated().after(wrappedKeyVO.getCreated()))
                .collect(Collectors.toList());
        if (!createdAfterMigration.isEmpty()) {
            // e.g. the copy made by a storage migration on 4.23, which was encrypted with the passphrase 4.23 derived
            // from this key. Repairing the key would make such a volume unreadable, so leave it to the admin.
            logger.warn("Not repairing KMS wrapped key {}, which 4.23 migrated in a wrong format: volume(s) {} were "
                    + "created after the migration and may be encrypted with the passphrase 4.23 used. Check these "
                    + "volumes manually.", wrappedKeyVO, createdAfterMigration);
            return;
        }

        KMSKeyVO kmsKey = kmsKeyDao.findById(wrappedKeyVO.getKmsKeyId());
        KMSKekVersionVO activeVersion = getActiveKekVersion(kmsKey.getId());
        HSMProfileVO hsmProfile = hsmProfileDao.findById(activeVersion.getHsmProfileId());
        WrappedKey rewrapped = getKMSProvider(hsmProfile.getProtocol()).wrapKey(passphrase,
                kmsKey.getPurpose(), activeVersion.getKekLabel(), activeVersion.getHsmProfileId());
        wrappedKeyVO.setKekVersionId(activeVersion.getId());
        wrappedKeyVO.setWrappedBlob(rewrapped.getWrappedKeyMaterial());
        wrappedKeyVO.setWrapAlgorithm(rewrapped.getAlgorithm());

        for (VolumeVO volume : volumes) {
            logger.info("Repaired the KMS key of volume {}, which was migrated to KMS by 4.23", volume);
            ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(),
                    volume.getAccountId(), EventVO.LEVEL_INFO, EventTypes.EVENT_VOLUME_MIGRATE_TO_KMS, true,
                    String.format("Repaired the KMS key of the volume, which was migrated to KMS key %s by 4.23",
                            kmsKey.getUuid()),
                    volume.getId(), ApiCommandResourceType.Volume.toString(), CallContext.current().getStartEventId());
        }
    }

    private void markFormatChecked(KMSWrappedKeyVO wrappedKeyVO) {
        wrappedKeyVO.setFormatCheckPending(false);
        kmsWrappedKeyDao.update(wrappedKeyVO.getId(), wrappedKeyVO);
    }

    /**
     * @return the raw passphrase if {@code key} is the base64 text of a 48-byte legacy passphrase, as 4.23 migrated
     * it, otherwise null
     */
    static byte[] decodeLegacyMigratedKey(byte[] key) {
        if (key.length != LEGACY_MIGRATED_KEY_LENGTH) {
            return null;
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(key);
        } catch (IllegalArgumentException e) {
            return null;
        }
        if (decoded.length != LEGACY_PASSPHRASE_LENGTH || !Arrays.equals(Base64.getEncoder().encode(decoded), key)) {
            Arrays.fill(decoded, (byte) 0);
            return null;
        }
        return decoded;
    }

    @Override
    public boolean stop() {
        if (rewrapExecutor != null) {
            rewrapExecutor.shutdownNow();
            rewrapExecutor = null;
        }
        if (keyFormatRepairExecutor != null) {
            keyFormatRepairExecutor.shutdownNow();
            keyFormatRepairExecutor = null;
        }
        kmsOperationExecutor.shutdownNow();
        return super.stop();
    }

    @Override
    public String getConfigComponentName() {
        return KMSManager.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[]{
                KMSDekSizeBits,
                KMSRetryCount,
                KMSRetryDelayMs,
                KMSOperationTimeoutSec,
                KMSRewrapBatchSize,
                KMSRewrapIntervalMs,
                KMSOperationPoolCoreSize,
                KMSOperationPoolMaxSize,
        };
    }

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(ListKMSKeysCmd.class);
        cmdList.add(CreateKMSKeyCmd.class);
        cmdList.add(UpdateKMSKeyCmd.class);
        cmdList.add(DeleteKMSKeyCmd.class);
        cmdList.add(RotateKMSKeyCmd.class);
        cmdList.add(MigrateVolumesToKMSCmd.class);
        cmdList.add(CreateHSMProfileCmd.class);
        cmdList.add(ListHSMProfilesCmd.class);
        cmdList.add(UpdateHSMProfileCmd.class);
        cmdList.add(DeleteHSMProfileCmd.class);

        return cmdList;
    }

    @FunctionalInterface
    interface KmsOperation<T> {
        T execute() throws Exception;
    }
}
