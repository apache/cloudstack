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
package com.cloud.hypervisor.vmware.manager;

import com.cloud.api.query.dao.TemplateJoinDao;
import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.HypervisorGuru;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.template.TemplateManager;
import com.cloud.template.VirtualMachineTemplate;
import com.cloud.vm.UserVmCloneSettingVO;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.dao.UserVmCloneSettingDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.engine.orchestration.VolumeOrchestrator;
import org.apache.cloudstack.managed.context.ManagedContextRunnable;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * This Task marks templates that are only used as fully cloned templates and have been deleted from CloudStack for removal from primary stores.
 */
public class CleanupFullyClonedTemplatesTask extends ManagedContextRunnable {

    private static final Logger s_logger = Logger.getLogger(CleanupFullyClonedTemplatesTask.class);

    private PrimaryDataStoreDao primaryStorageDao;
    private VMTemplatePoolDao templateDataStoreDao;
    private TemplateJoinDao templateDao;
    private VMInstanceDao vmInstanceDao;
    private UserVmCloneSettingDao cloneSettingDao;
    private TemplateManager templateManager;

    private Thread mine;

    CleanupFullyClonedTemplatesTask(PrimaryDataStoreDao primaryStorageDao,
                                    VMTemplatePoolDao templateDataStoreDao,
                                    TemplateJoinDao templateDao,
                                    VMInstanceDao vmInstanceDao,
                                    UserVmCloneSettingDao cloneSettingDao,
                                    TemplateManager templateManager) {
        this.primaryStorageDao = primaryStorageDao;
        this.templateDataStoreDao = templateDataStoreDao;
        this.templateDao = templateDao;
        this.vmInstanceDao = vmInstanceDao;
        this.cloneSettingDao = cloneSettingDao;
        this.templateManager = templateManager;
        if(s_logger.isDebugEnabled()) {
            s_logger.debug("new task created: " + this);
        }
    }

    @Override
    public void runInContext() {
        mine = Thread.currentThread();
        s_logger.info("running job to mark fully cloned templates for gc in thread " + mine.getName());

        if (HypervisorGuru.VmwareFullClone.value()) { // only run if full cloning is being used (might need to be more fine grained)
            try {
                queryAllPools();
            } catch (Throwable t) {
                s_logger.error("error during job to mark fully cloned templates for gc in thread " + mine.getName());
                if(s_logger.isDebugEnabled()) {
                    s_logger.debug("running job to mark fully cloned templates for gc in thread " + mine.getName(),t);
                }
            }
        }
    }

    private void queryAllPools() {
        List<StoragePoolVO> storagePools = primaryStorageDao.listAll();
        for (StoragePoolVO pool : storagePools) {
            long zoneId = pool.getDataCenterId();
            queryPoolForTemplates(pool, zoneId);
        }
    }

    private void queryPoolForTemplates(StoragePoolVO pool, long zoneId) {
        // we don't need those specific to other hypervisor types
        if (pool.getHypervisor() == null || Hypervisor.HypervisorType.VMware.equals(pool.getHypervisor())) {
            if(s_logger.isDebugEnabled()) {
                s_logger.debug(mine.getName() + " is marking fully cloned templates in pool " + pool.getName());
            }
            List<VMTemplateStoragePoolVO> templatePrimaryDataStoreVOS = templateDataStoreDao.listByPoolId(pool.getId());
            for (VMTemplateStoragePoolVO templateMapping : templatePrimaryDataStoreVOS) {
                if (canRemoveTemplateFromZone(zoneId, templateMapping)) {
                    templateManager.evictTemplateFromStoragePool(templateMapping);
                }
            }
        } else {
            if(s_logger.isDebugEnabled()) {
                s_logger.debug(mine.getName() + " is ignoring pool " + pool.getName() + " id == " + pool.getId());
            }
        }
    }

    private boolean canRemoveTemplateFromZone(long zoneId, VMTemplateStoragePoolVO templateMapping) {
        if (!templateMapping.getMarkedForGC()) {
            if(s_logger.isDebugEnabled()) {
                s_logger.debug(mine.getName() + " is checking template with id " + templateMapping.getTemplateId() + " for deletion from pool with id " + templateMapping.getPoolId());
            }

            TemplateJoinVO templateJoinVO = templateDao.findByIdIncludingRemoved(templateMapping.getTemplateId());
            //  check if these are deleted (not removed is null)
            if (VirtualMachineTemplate.State.Inactive.equals(templateJoinVO.getTemplateState())) { // meaning it is removed!
                //  see if we can find vms using it with user_vm_clone_setting != full
                return markedForGc(templateMapping, zoneId);
            }
        }
        return false;
    }

    private boolean markedForGc(VMTemplateStoragePoolVO templateMapping, long zoneId) {
        boolean used = false;
        List<VMInstanceVO> vms = vmInstanceDao.listNonExpungedByZoneAndTemplate(zoneId, templateMapping.getTemplateId());
        for (VMInstanceVO vm : vms) {
            try {
                UserVmCloneSettingVO cloneSetting = cloneSettingDao.findByVmId(vm.getId());
                // VolumeOrchestrator or UserVmManagerImpl depending on version
                if (VolumeOrchestrator.UserVmCloneType.linked.equals(VolumeOrchestrator.UserVmCloneType.valueOf(cloneSetting.getCloneType()))) {
                    used = true;
                    break;
                }
            } catch (Exception e) {
                s_logger.error("failed to retrieve vm clone setting for vm " + vm.toString());
                if(s_logger.isDebugEnabled()) {
                    s_logger.debug("failed to retrieve vm clone setting for vm " + vm.toString(), e);
                }
            }
        }
        if (!used) {
            s_logger.info(mine.getName() + " is marking template with id " + templateMapping.getTemplateId() + " for gc in pool with id " + templateMapping.getPoolId());
            // else
            //  mark it for removal from primary store
            templateMapping.setMarkedForGC(true);
        }
        return !used;
    }
}
