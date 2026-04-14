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
import com.cloud.event.ActionEvent;
import com.cloud.event.ActionEventUtils;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.projects.Project.ListProjectResourcesCriteria;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
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
import org.apache.cloudstack.api.ApiCommandResourceType;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class KMSManagerImpl extends ManagerBase implements KMSManager, PluggableService {
    private static final Logger logger = LogManager.getLogger(KMSManagerImpl.class);
    private static final Map<String, KMSProvider> kmsProviderMap = new HashMap<>();
    private final ExecutorService kmsOperationExecutor = new ThreadPoolExecutor(
            2, 100, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), r -> {
        Thread t = new Thread(r, "kms-operation");
        t.setDaemon(true);
        return t;
    });
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
    private PassphraseDao passphraseDao;
    private List<KMSProvider> kmsProviders;
    private ScheduledExecutorService rewrapExecutor;

    @Override
    public List<? extends KMSProvider> listKMSProviders() {
        return kmsProviders;
    }

    @Override
    public KMSProvider getKMSProvider(String name) {
        if (StringUtils.isEmpty(name)) {
            name = "database";
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
        if (callerAccountId == null) {
            return false;
        }
        if (key == null) {
            return false;
        }
        if (!key.isEnabled()) {
            throw new InvalidParameterValueException("KMS key is not enabled: " + key);
        }
        Account caller = accountManager.getAccount(callerAccountId);
        if (caller == null) {
            return false;
        }
        Account owner = accountManager.getAccount(key.getAccountId());
        try {
            accountManager.checkAccess(caller, null, true, owner);
            return true;
        } catch (PermissionDeniedException e) {
            return false;
        }
    }

    @Override
    public void checkKmsKeyForVolumeEncryption(Account caller, Long kmsKeyId, Long zoneId) {
        if (kmsKeyId == null) {
            return;
        }
        KMSKeyVO key = findKMSKeyAndCheckAccess(kmsKeyId, caller);
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

        if (wrappedVO.getKekVersionId() != null) {
            KMSKekVersionVO version = kmsKekVersionDao.findById(wrappedVO.getKekVersionId());
            if (version != null && version.getStatus() != KMSKekVersionVO.Status.Archived) {
                try {
                    byte[] dek = getUnwrappedKey(wrappedVO, kmsKey, version);

                    CallContext.current().setEventResourceId(kmsKey.getId());
                    CallContext.current().setEventResourceType(ApiCommandResourceType.KmsKey);
                    CallContext.current().setEventDetails(String.format(
                            "Unwrapped %s key (wrapped key uuid: %s) using KMS key: %s (uuid: %s) with KEK version %d",
                            kmsKey.getPurpose().getName(), wrappedVO.getUuid(), kmsKey.getName(), kmsKey.getUuid(), version.getVersionNumber()));

                    logger.debug("Successfully unwrapped key {} with KEK version {}", wrappedKeyId, version.getVersionNumber());
                    return dek;
                } catch (Exception e) {
                    logger.warn("Failed to unwrap with version {}: {}", version.getVersionNumber(), e.getMessage());
                }
            }
        }

        // Fallback: try all available versions for decryption
        List<KMSKekVersionVO> versions = kmsKekVersionDao.getVersionsForDecryption(kmsKey.getId());
        for (KMSKekVersionVO version : versions) {
            try {
                byte[] dek = getUnwrappedKey(wrappedVO, kmsKey, version);

                CallContext.current().setEventResourceId(kmsKey.getId());
                CallContext.current().setEventResourceType(ApiCommandResourceType.KmsKey);
                CallContext.current().setEventDetails(String.format(
                        "Unwrapped %s key (wrapped key uuid: %s) using KMS key: %s (uuid: %s) with KEK version %d (fallback)",
                        kmsKey.getPurpose().getName(), wrappedVO.getUuid(), kmsKey.getName(), kmsKey.getUuid(), version.getVersionNumber()));

                logger.info("Successfully unwrapped key {} with KEK version {} (fallback)",
                        wrappedKeyId, version.getVersionNumber());
                return dek;
            } catch (Exception e) {
                logger.debug("Failed to unwrap with version {}: {}", version.getVersionNumber(), e.getMessage());
            }
        }

        throw KMSException.wrapUnwrapFailed("Failed to unwrap key with any available KEK version");
    }

    private byte[] getUnwrappedKey(KMSWrappedKeyVO wrappedVO, KMSKeyVO kmsKey,
            KMSKekVersionVO version) throws Exception {
        HSMProfileVO hsmProfile = hsmProfileDao.findById(version.getHsmProfileId());
        KMSProvider provider = getKMSProvider(hsmProfile.getProtocol());

        WrappedKey wrapped = new WrappedKey(wrappedVO.getUuid(), version.getKekLabel(), kmsKey.getPurpose(),
                kmsKey.getAlgorithm(), wrappedVO.getWrappedBlob(),
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
        WrappedKey wrappedKey;
        try {
            wrappedKey = retryOperation(() -> provider.generateAndWrapDek(KeyPurpose.VOLUME_ENCRYPTION,
                    activeVersion.getKekLabel(), dekSize,
                    activeVersion.getHsmProfileId()));
            KMSWrappedKeyVO wrappedKeyVO = new KMSWrappedKeyVO(kmsKey.getId(), activeVersion.getId(),
                    kmsKey.getZoneId(), wrappedKey.getWrappedKeyMaterial());
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

        KMSKeyVO kmsKey = new KMSKeyVO(name, description, "", purpose,
                accountId, domainId, zoneId, "AES/GCM/NoPadding", keyBits);

        KMSProvider provider = getKMSProvider(profile.getProtocol());
        String kekLabel = purpose.generateKekLabel(domainId, accountId, kmsKey.getUuid(), 1);

        String providerKekLabel;
        Long finalProfileId = hsmProfileId;
        try {
            providerKekLabel = retryOperation(() -> provider.createKek(purpose, kekLabel, keyBits, finalProfileId));
        } catch (Exception e) {
            throw handleKmsException(e);
        }

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

        SearchBuilder<KMSKeyVO> sb = getSearchBuilderForKMSKeys(domainId, isRecursive, permittedAccounts,
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

    SearchBuilder<KMSKeyVO> getSearchBuilderForKMSKeys(Long domainId, Boolean isRecursive, List<Long> permittedAccounts,
            ListProjectResourcesCriteria listProjectResourcesCriteria) {
        SearchBuilder<KMSKeyVO> sb = kmsKeyDao.createSearchBuilder();
        accountManager.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("zoneId", sb.entity().getZoneId(), SearchCriteria.Op.EQ);
        sb.and("purpose", sb.entity().getPurpose(), SearchCriteria.Op.EQ);
        sb.and("enabled", sb.entity().isEnabled(), SearchCriteria.Op.EQ);
        sb.and("hsmProfileId", sb.entity().getHsmProfileId(), SearchCriteria.Op.EQ);
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
        if (cmd.getId() != null) {
            sc.setParameters("id", cmd.getId());
        }
        if (cmd.getZoneId() != null) {
            sc.setParameters("zoneId", cmd.getZoneId());
        }
        if (keyPurpose != null) {
            sc.setParameters("purpose", keyPurpose);
        }
        if (cmd.getEnabled() != null) {
            sc.setParameters("enabled", cmd.getEnabled());
        }
        if (cmd.getHsmProfileId() != null) {
            sc.setParameters("hsmProfileId", cmd.getHsmProfileId());
        }
        return sc;
    }

    @Override
    public KMSKeyResponse updateKMSKey(UpdateKMSKeyCmd cmd) throws KMSException {
        Account caller = CallContext.current().getCallingAccount();
        KMSKeyVO key = findKMSKeyAndCheckAccess(cmd.getId(), caller);
        KMSKey updatedKey = updateUserKMSKey(key, cmd.getName(), cmd.getDescription(), cmd.getEnabled());
        CallContext.current().setEventDetails(String.format("Updated KMS key: %s (uuid: %s)", updatedKey.getName(), updatedKey.getUuid()));
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
                logger.warn("Failed to delete KEK {} (v{}) from provider during KMS key deletion: {}",
                        kekVersion.getKekLabel(), kekVersion.getVersionNumber(), e.getMessage());
                ActionEventUtils.onCompletedActionEvent(CallContext.current().getCallingUserId(),
                        key.getAccountId(), EventVO.LEVEL_WARN, EventTypes.EVENT_KMS_KEY_DELETE, true,
                        String.format("Failed to delete KEK %s from provider during KMS key deletion", kekVersion.getKekLabel()),
                        key.getId(), ApiCommandResourceType.KmsKey.toString(), CallContext.current().getStartEventId());
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
                    provider.deleteKek(newKekId);
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
    public int migrateVolumesToKMS(MigrateVolumesToKMSCmd cmd) throws KMSException {
        Account caller = CallContext.current().getCallingAccount();
        Long zoneId = cmd.getZoneId();
        String accountName = cmd.getAccountName();
        Long domainId = cmd.getDomainId();
        Long kmsKeyId = cmd.getKmsKeyId();
        List<Long> volumeIds = cmd.getVolumeIds();

        if (zoneId == null && CollectionUtils.isEmpty(volumeIds)) {
            throw new InvalidParameterValueException("Need to specify either ZoneId or Volume IDs");
        }

        if (zoneId != null && CollectionUtils.isNotEmpty(volumeIds)) {
            throw new InvalidParameterValueException("Specify either ZoneId or Volume IDs");
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

        int pageSize = 100;

        int successCount = 0;
        int failureCount = 0;
        int totalCount;
        logger.info("Starting migration of volumes to KMS (zone: {}, account: {}, domain: {})",
                zoneId, accountId, domainId);

        List<VolumeVO> volumes;
        if (CollectionUtils.isNotEmpty(volumeIds)) {
            volumes = volumeDao.listByIds(volumeIds);
            accountManager.checkAccess(caller, null, true, volumes.toArray(new Volume[0]));
            totalCount = volumes.size();
        } else {
            Pair<List<VolumeVO>, Integer> volumeListPair = volumeDao.listVolumesForKMSMigration(zoneId, accountId,
                    domainId,
                    pageSize);
            volumes = volumeListPair.first();
            totalCount = volumeListPair.second();
        }
        while (true) {

            if (CollectionUtils.isEmpty(volumes) || totalCount == 0) {
                break;
            }

            for (VolumeVO volume : volumes) {
                try {
                    if (migrateVolumeToKmsKey(provider, volume, kmsKey, activeVersion)) {
                        successCount++;
                        logger.debug("Migrated volume's encryption {} to KMS (batch {})", volume, kmsKey);
                    }
                } catch (Exception e) {
                    failureCount++;
                    logger.warn("Failed to migrate volume {}: {}", volume.getId(), e.getMessage());
                }
            }
            logger.debug("Processed {} volumes. success: {}, failure: {}, total: {}", volumes.size(),
                    successCount, failureCount, totalCount);

            if (CollectionUtils.isNotEmpty(volumeIds)) {
                break;
            }

            Pair<List<VolumeVO>, Integer> volumeListPair = volumeDao.listVolumesForKMSMigration(zoneId, accountId,
                    domainId, pageSize);
            volumes = volumeListPair.first();
            if (totalCount == volumeListPair.second()) {
                logger.debug(
                        "{} volumes pending for migration because passphrase was not found or migration failed",
                        totalCount);
                break;
            }
            totalCount = volumeListPair.second();
        }
        logger.info("Migration operation completed: {} total volumes processed, {} success, {} failures",
                successCount + failureCount, successCount, failureCount);

        CallContext.current().setEventDetails(String.format("Migrated %d volumes to KMS key: %s (uuid: %s). Success: %d, Failures: %d", successCount + failureCount, kmsKey.getName(), kmsKey.getUuid(), successCount, failureCount));

        return successCount;
    }

    private boolean migrateVolumeToKmsKey(KMSProvider provider, VolumeVO volume, KMSKey kmsKey,
            KMSKekVersionVO activeVersion) {
        PassphraseVO passphrase = passphraseDao.findById(volume.getPassphraseId());
        if (passphrase == null) {
            logger.warn(
                    "Skipping migration of volume from to the KMS key {} because passphrase id: {} not found for "
                    + "volume {}",
                    kmsKey, volume.getPassphraseId(), volume);
            return false;
        }

        // PassphraseVO.getPassphrase() returns Base64-encoded bytes matching KVM/QEMU
        // format
        byte[] passphraseBytes = passphrase.getPassphrase();
        try {
            WrappedKey wrappedKey = provider.wrapKey(
                    passphraseBytes,
                    KeyPurpose.VOLUME_ENCRYPTION,
                    activeVersion.getKekLabel(),
                    activeVersion.getHsmProfileId());

            KMSWrappedKeyVO wrappedKeyVO = new KMSWrappedKeyVO(
                    kmsKey.getId(),
                    activeVersion.getId(),
                    volume.getDataCenterId(),
                    wrappedKey.getWrappedKeyMaterial());
            wrappedKeyVO = kmsWrappedKeyDao.persist(wrappedKeyVO);

            volume.setKmsWrappedKeyId(wrappedKeyVO.getId());
            volume.setKmsKeyId(kmsKey.getId());
            volume.setPassphraseId(null);
            volumeDao.update(volume.getId(), volume);

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

        Account targetAccount = accountManager.finalizeOwner(caller, cmd.getAccountName(), cmd.getDomainId(),
                cmd.getProjectId());

        Long accountId = targetAccount.getId();
        Long domainId = targetAccount.getDomainId();

        HSMProfileVO profile = new HSMProfileVO(
                cmd.getName(),
                protocol,
                accountId,
                domainId,
                cmd.getZoneId(),
                cmd.getVendorName());
        profile.setIsPublic(isPublic);
        profile = hsmProfileDao.persist(profile);

        if (cmd.getDetails() != null) {
            for (Map.Entry<String, String> entry : cmd.getDetails().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (isSensitiveKey(key)) {
                    value = DBEncryptionUtil.encrypt(value);
                }

                hsmProfileDetailsDao.persist(profile.getId(), key, value);
            }
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

        SearchBuilder<HSMProfileVO> sb = getSearchBuilderForHSMProfiles(domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);
        SearchCriteria<HSMProfileVO> sc = getSearchCriteriaForHSMProfiles(sb, cmd, caller, domainId, isRecursive,
                permittedAccounts, listProjectResourcesCriteria);

        Filter searchFilter = new Filter(HSMProfileVO.class, "id", true, cmd.getStartIndex(), cmd.getPageSizeVal());
        Pair<List<HSMProfileVO>, Integer> result = hsmProfileDao.searchAndCount(sc, searchFilter);
        List<HSMProfileVO> profiles = result.first();
        Integer totalCount = result.second();

        List<HSMProfileResponse> responses = new ArrayList<>();

        boolean isRootAdmin = accountManager.isRootAdmin(caller.getId());
        for (HSMProfileVO profile : profiles) {
            // When isSystem=true, non-admin users explicitly requested system profiles, so
            // don't mark as limited
            // When listall=true, also don't mark as limited since user requested all
            // profiles
            // If the profile is owned by the user, they should see full details even if it
            // is a system profile
            boolean limited = profile.getIsPublic() && !isRootAdmin && !(cmd.getIsSystem() || cmd.listAll())
                              && profile.getAccountId() != caller.getId();
            responses.add(createHSMProfileResponse(profile, limited));
        }

        ListResponse<HSMProfileResponse> listResponse = new ListResponse<>();
        listResponse.setResponses(responses, totalCount);
        return listResponse;
    }

    SearchBuilder<HSMProfileVO> getSearchBuilderForHSMProfiles(Long domainId, Boolean isRecursive,
            List<Long> permittedAccounts, ListProjectResourcesCriteria listProjectResourcesCriteria) {
        SearchBuilder<HSMProfileVO> sb = hsmProfileDao.createSearchBuilder();
        accountManager.buildACLSearchBuilder(sb, domainId, isRecursive, permittedAccounts,
                listProjectResourcesCriteria);

        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        sb.and("zoneId", sb.entity().getZoneId(), SearchCriteria.Op.EQ);
        sb.and("protocol", sb.entity().getProtocol(), SearchCriteria.Op.EQ);
        sb.and("enabled", sb.entity().isEnabled(), SearchCriteria.Op.EQ);
        sb.and("system", sb.entity().getIsPublic(), SearchCriteria.Op.EQ);
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
        sc.setParametersIfNotNull("system", cmd.getIsSystem());

        // Access control for non-root-admins:
        // system profiles (null account_id/domain_id) are globally visible to all
        // users,
        // so they must always be reachable via "system=true OR <ACL conditions>".
        // ANDing ACL criteria directly onto sc would exclude them because their
        // account_id is NULL.
        //
        // The `system` field filter already set above (line sc.setParametersIfNotNull)
        // correctly
        // narrows the final result when the caller passes isSystem=true/false:
        // isSystem=true → sc already has system=true → effective: WHERE system=true
        // isSystem=false → sc already has system=false → effective: WHERE system=false
        // AND ACL
        // isSystem=null → no extra filter → effective: WHERE (system=true OR ACL)
        //
        // Root admins bypass ACL entirely and see everything filtered only by explicit
        // params.
        boolean isRootAdmin = accountManager.isRootAdmin(caller.getId());

        if (!isRootAdmin) {
            SearchCriteria<HSMProfileVO> systemOrAclSC = hsmProfileDao.createSearchCriteria();
            if (cmd.listAll()) {
                systemOrAclSC.addOr("system", SearchCriteria.Op.EQ, true);
            }

            SearchCriteria<HSMProfileVO> aclSC = searchBuilder.create();
            accountManager.buildACLSearchCriteria(aclSC, domainId, isRecursive, permittedAccounts,
                    listProjectResourcesCriteria);

            if (StringUtils.isNotBlank(aclSC.getWhereClause()) && StringUtils.isNotBlank(
                    systemOrAclSC.getWhereClause())) {
                systemOrAclSC.addOr("id", SearchCriteria.Op.SC, aclSC);
            } else if (StringUtils.isNotBlank(aclSC.getWhereClause()) && StringUtils.isBlank(
                    systemOrAclSC.getWhereClause())) {
                systemOrAclSC = aclSC;
            }

            if (StringUtils.isNotBlank(systemOrAclSC.getWhereClause())) {
                sc.addAnd("id", SearchCriteria.Op.SC, systemOrAclSC);
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

        if (hsmProfileDao.remove(profile.getId())) {
            return true;
        }
        return false;
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

        CallContext.current().setEventResourceId(profile.getId());
        CallContext.current().setEventResourceType(ApiCommandResourceType.HsmProfile);
        CallContext.current().setEventDetails(String.format("Updated HSM profile: %s (uuid: %s)", profile.getName(), profile.getUuid()));

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

        if (limited) {
            return response;
        }

        response.setProtocol(profile.getProtocol());
        response.setEnabled(profile.isEnabled());
        response.setCreated(profile.getCreated());

        ApiResponseHelper.populateOwner(response, profile);

        List<HSMProfileDetailsVO> details = hsmProfileDetailsDao.listByProfileId(profile.getId());
        Map<String, String> detailsMap = new HashMap<>();
        for (HSMProfileDetailsVO detail : details) {
            detailsMap.put(detail.getName(), detail.getValue());
        }
        response.setDetails(detailsMap);
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
        } else {
            accountManager.checkAccess(caller, null, requireModifyAccess, profile);
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

    @Override
    public boolean stop() {
        if (rewrapExecutor != null) {
            rewrapExecutor.shutdownNow();
            rewrapExecutor = null;
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
                KMSRewrapIntervalMs
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
