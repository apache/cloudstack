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

package org.apache.cloudstack.agent.manager;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.cloudstack.api.command.user.iso.DeleteIsoCmd;
import org.apache.cloudstack.api.command.user.iso.GetUploadParamsForIsoCmd;
import org.apache.cloudstack.api.command.user.iso.RegisterIsoCmd;
import org.apache.cloudstack.api.command.user.template.RegisterTemplateCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.storage.command.TemplateOrVolumePostUploadCommand;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.configuration.Resource;
import com.cloud.dc.DataCenterVO;
import com.cloud.event.EventTypes;
import com.cloud.event.UsageEventVO;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.Storage;
import com.cloud.storage.TemplateProfile;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.template.TemplateAdapter;
import com.cloud.template.TemplateAdapterBase;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.user.Account;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.TransactionCallback;
import com.cloud.utils.exception.CloudRuntimeException;

public class ExternalTemplateAdapter extends TemplateAdapterBase implements TemplateAdapter {

    @Override
    public String getName() {
        return TemplateAdapterType.External.getName();
    }

    @Override
    public TemplateProfile prepare(RegisterTemplateCmd cmd) throws ResourceAllocationException {
        Account caller = CallContext.current().getCallingAccount();
        Account owner = _accountMgr.getAccount(cmd.getEntityOwnerId());
        _accountMgr.checkAccess(caller, null, true, owner);
        Storage.TemplateType templateType = templateMgr.validateTemplateType(cmd, _accountMgr.isAdmin(caller.getAccountId()),
                CollectionUtils.isEmpty(cmd.getZoneIds()), Hypervisor.HypervisorType.External);

        List<Long> zoneId = cmd.getZoneIds();
        // ignore passed zoneId if we are using region wide image store
        List<ImageStoreVO> stores = _imgStoreDao.findRegionImageStores();
        if (stores != null && stores.size() > 0) {
            zoneId = null;
        }

        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.getType(cmd.getHypervisor());
        if(hypervisorType == Hypervisor.HypervisorType.None) {
            throw new InvalidParameterValueException(String.format(
                    "Hypervisor Type: %s is invalid. Supported Hypervisor types are: %s",
                    cmd.getHypervisor(),
                    StringUtils.join(Arrays.stream(Hypervisor.HypervisorType.values()).filter(h -> h != Hypervisor.HypervisorType.None).map(Hypervisor.HypervisorType::name).toArray(), ", ")));
        }

        Map details = cmd.getDetails();
        Map externalDetails = cmd.getExternalDetails();
        if (details != null) {
            details.putAll(externalDetails);
        } else {
            details = externalDetails;
        }

        return prepare(false, CallContext.current().getCallingUserId(), cmd.getTemplateName(), cmd.getDisplayText(), cmd.getArch(), cmd.getBits(), cmd.isPasswordEnabled(), cmd.getRequiresHvm(),
                cmd.getUrl(), cmd.isPublic(), cmd.isFeatured(), cmd.isExtractable(), cmd.getFormat(), cmd.getOsTypeId(), zoneId, hypervisorType, cmd.getChecksum(), true,
                cmd.getTemplateTag(), owner, details, cmd.isSshKeyEnabled(), null, cmd.isDynamicallyScalable(), templateType,
                cmd.isDirectDownload(), cmd.isDeployAsIs(), cmd.isForCks(), cmd.getExtensionId());
    }

    @Override
    public TemplateProfile prepare(RegisterIsoCmd cmd) throws ResourceAllocationException {
        throw new CloudRuntimeException("External hypervisor doesn't support ISO template");
    }

    @Override
    public TemplateProfile prepare(GetUploadParamsForIsoCmd cmd) throws ResourceAllocationException {
        throw new CloudRuntimeException("External hypervisor doesn't support ISO template");
    }

    private void templateCreateUsage(VMTemplateVO template, long dcId) {
        if (template.getAccountId() != Account.ACCOUNT_ID_SYSTEM) {
            UsageEventVO usageEvent =
                    new UsageEventVO(EventTypes.EVENT_TEMPLATE_CREATE, template.getAccountId(), dcId, template.getId(), template.getName(), null,
                            template.getSourceTemplateId(), 0L);
            _usageEventDao.persist(usageEvent);
        }
    }

    @Override
    public VMTemplateVO create(TemplateProfile profile) {
        VMTemplateVO template = persistTemplate(profile, VirtualMachineTemplate.State.Active);
        List<Long> zones = profile.getZoneIdList();

        // create an entry at template_store_ref with store_id = null to represent that this template is ready for use.
        TemplateDataStoreVO vmTemplateHost =
                new TemplateDataStoreVO(null, template.getId(), new Date(), 100, VMTemplateStorageResourceAssoc.Status.DOWNLOADED, null, null, null, null, template.getUrl());
        this._tmpltStoreDao.persist(vmTemplateHost);

        if (zones == null) {
            List<DataCenterVO> dcs = _dcDao.listAllIncludingRemoved();
            if (dcs != null && dcs.size() > 0) {
                templateCreateUsage(template, dcs.get(0).getId());
            }
        } else {
            for (Long zoneId: zones) {
                templateCreateUsage(template, zoneId);
            }
        }

        _resourceLimitMgr.incrementResourceCount(profile.getAccountId(), Resource.ResourceType.template);
        return template;
    }

    @Override
    public List<TemplateOrVolumePostUploadCommand> createTemplateForPostUpload(TemplateProfile profile) {
        return Transaction.execute((TransactionCallback<List<TemplateOrVolumePostUploadCommand>>) status -> {
            if (Storage.ImageFormat.ISO.equals(profile.getFormat())) {
                throw new CloudRuntimeException("ISO upload is not supported for External hypervisor");
            }
            List<Long> zoneIdList = profile.getZoneIdList();
            if (zoneIdList == null) {
                throw new CloudRuntimeException("Zone ID is null, cannot upload template.");
            }
            if (zoneIdList.size() > 1) {
                throw new CloudRuntimeException("Operation is not supported for more than one zone id at a time.");
            }
            VMTemplateVO template = persistTemplate(profile, VirtualMachineTemplate.State.NotUploaded);
            if (template == null) {
                throw new CloudRuntimeException("Unable to persist the template " + profile.getTemplate());
            }
            // Set Event Details for Template/ISO Upload
            String eventResourceId = template.getUuid();
            CallContext.current().setEventDetails(String.format("Template Id: %s", eventResourceId));
            CallContext.current().putContextParameter(VirtualMachineTemplate.class, eventResourceId);
            Long zoneId = zoneIdList.get(0);
            DataStore imageStore = verifyHeuristicRulesForZone(template, zoneId);
            List<TemplateOrVolumePostUploadCommand> payloads = new LinkedList<>();
            if (imageStore == null) {
                List<DataStore> imageStores = getImageStoresThrowsExceptionIfNotFound(zoneId, profile);
                postUploadAllocation(imageStores, template, payloads);
            } else {
                postUploadAllocation(List.of(imageStore), template, payloads);
            }
            if (payloads.isEmpty()) {
                throw new CloudRuntimeException("Unable to find zone or an image store with enough capacity");
            }
            _resourceLimitMgr.incrementResourceCount(profile.getAccountId(), Resource.ResourceType.template);
            return payloads;
        });
    }

    @Override
    public TemplateProfile prepareDelete(DeleteIsoCmd cmd) {
        throw new CloudRuntimeException("External hypervisor doesn't support ISO, how the delete get here???");
    }

    @Override
    @DB
    public boolean delete(TemplateProfile profile) {
        VMTemplateVO template = profile.getTemplate();
        Long templateId = template.getId();
        boolean success = true;
        String zoneName;

        if (profile.getZoneIdList() != null && profile.getZoneIdList().size() > 1)
            throw new CloudRuntimeException("Operation is not supported for more than one zone id at a time");

        if (!template.isCrossZones() && profile.getZoneIdList() != null) {
            //get the first element in the list
            zoneName = profile.getZoneIdList().get(0).toString();
        } else {
            zoneName = "all zones";
        }

        logger.debug("Attempting to mark template host refs for {} as destroyed in zone: {}", template, zoneName);
        Account account = _accountDao.findByIdIncludingRemoved(template.getAccountId());
        String eventType = EventTypes.EVENT_TEMPLATE_DELETE;
        List<TemplateDataStoreVO> templateHostVOs = this._tmpltStoreDao.listByTemplate(templateId);

        for (TemplateDataStoreVO vo : templateHostVOs) {
            TemplateDataStoreVO lock = null;
            try {
                lock = _tmpltStoreDao.acquireInLockTable(vo.getId());
                if (lock == null) {
                    logger.debug("Failed to acquire lock when deleting templateDataStoreVO with ID: {}", vo.getId());
                    success = false;
                    break;
                }

                vo.setDestroyed(true);
                _tmpltStoreDao.update(vo.getId(), vo);

            } finally {
                if (lock != null) {
                    _tmpltStoreDao.releaseFromLockTable(lock.getId());
                }
            }
        }

        if (profile.getZoneIdList() != null) {
            UsageEventVO usageEvent = new UsageEventVO(eventType, account.getId(), profile.getZoneIdList().get(0),
                    templateId, null);
            _usageEventDao.persist(usageEvent);

            VMTemplateZoneVO templateZone = _tmpltZoneDao.findByZoneTemplate(profile.getZoneIdList().get(0), templateId);

            if (templateZone != null) {
                _tmpltZoneDao.remove(templateZone.getId());
            }
        } else {
            List<DataCenterVO> dcs = _dcDao.listAllIncludingRemoved();
            for (DataCenterVO dc : dcs) {
                UsageEventVO usageEvent = new UsageEventVO(eventType, account.getId(), dc.getId(), templateId, null);
                _usageEventDao.persist(usageEvent);
            }
        }

        logger.debug("Successfully marked template host refs for {}} as destroyed in zone: {}", template, zoneName);

        // If there are no more non-destroyed template host entries for this template, delete it
        if (success && _tmpltStoreDao.listByTemplate(templateId).isEmpty()) {
            long accountId = template.getAccountId();

            VMTemplateVO lock = _tmpltDao.acquireInLockTable(templateId);

            try {
                if (lock == null) {
                    logger.debug("Failed to acquire lock when deleting template with ID: {}", templateId);
                    success = false;
                } else if (_tmpltDao.remove(templateId)) {
                    // Decrement the number of templates and total secondary storage space used by the account.
                    _resourceLimitMgr.decrementResourceCount(accountId, Resource.ResourceType.template);
                    _resourceLimitMgr.recalculateResourceCount(accountId, _accountMgr.getAccount(accountId).getDomainId(), Resource.ResourceType.secondary_storage.getOrdinal());
                }

            } finally {
                if (lock != null) {
                    _tmpltDao.releaseFromLockTable(lock.getId());
                }
            }
            logger.debug("Removed template: {} because all of its template host refs were marked as destroyed.", template.getName());
        }

        return success;
    }
}
