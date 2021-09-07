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

package com.cloud.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.api.command.admin.storage.MigrateSecondaryStorageDataCmd;
import org.apache.cloudstack.api.response.MigrationResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.StorageOrchestrationService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.storage.ImageStoreService;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.commons.lang3.EnumUtils;
import org.apache.log4j.Logger;

import com.cloud.event.ActionEvent;
import com.cloud.event.EventTypes;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;

public class ImageStoreServiceImpl extends ManagerBase implements ImageStoreService {

    private static final Logger s_logger = Logger.getLogger(ImageStoreServiceImpl.class);
    @Inject
    ImageStoreDao imageStoreDao;
    @Inject
    private AsyncJobManager jobMgr;
    @Inject
    private StorageOrchestrationService stgService;

    ConfigKey<Double> ImageStoreImbalanceThreshold = new ConfigKey<>("Advanced", Double.class,
            "image.store.imbalance.threshold",
            "0.3",
            "The storage imbalance threshold that is compared with the standard deviation percentage for a storage utilization metric. " +
                    "The value is a percentage in decimal format.",
            true, ConfigKey.Scope.Global);


    public Integer numConcurrentCopyTasksPerSSVM = null;



    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    @Override
    @ActionEvent(eventType = EventTypes.EVENT_IMAGE_STORE_DATA_MIGRATE, eventDescription = "migrating Image store data", async = true)
    public MigrationResponse migrateData(MigrateSecondaryStorageDataCmd cmd) {
        Long srcImgStoreId = cmd.getId();
        ImageStoreVO srcImageVO = imageStoreDao.findById(srcImgStoreId);
        List<Long> destImgStoreIds = cmd.getMigrateTo();
        List<String> imagestores = new ArrayList<String>();
        String migrationType = cmd.getMigrationType();

        // default policy is complete
        MigrationPolicy policy = MigrationPolicy.COMPLETE;

        if (migrationType != null) {
            if (!EnumUtils.isValidEnum(MigrationPolicy.class, migrationType.toUpperCase())) {
                throw new CloudRuntimeException("Not a valid migration policy");
            }
            policy = MigrationPolicy.valueOf(migrationType.toUpperCase());
        }

        String message = null;

        if (srcImageVO == null) {
            throw new CloudRuntimeException("Cannot find secondary storage with id: " + srcImgStoreId);
        }

        Long srcStoreDcId = srcImageVO.getDataCenterId();
        imagestores.add(srcImageVO.getName());
        if (srcImageVO.getRole() != DataStoreRole.Image) {
            throw new CloudRuntimeException("Secondary storage is not of Image Role");
        }

        if (!srcImageVO.getProviderName().equals(DataStoreProvider.NFS_IMAGE)) {
            throw new InvalidParameterValueException("Migration of datastore objects is supported only for NFS based image stores");
        }

        if (destImgStoreIds.contains(srcImgStoreId)) {
            s_logger.debug("One of the destination stores is the same as the source image store ... Ignoring it...");
            destImgStoreIds.remove(srcImgStoreId);
        }

        // Validate all the Ids correspond to valid Image stores
        List<Long> destDatastores = new ArrayList<>();
        for (Long id : destImgStoreIds) {
            ImageStoreVO store = imageStoreDao.findById(id);
            if (store == null) {
                s_logger.warn("Secondary storage with id: " + id + "is not found. Skipping it...");
                continue;
            }
            if (store.isReadonly()) {
                s_logger.warn("Secondary storage: "+ id + " cannot be considered for migration as has read-only permission, Skipping it... ");
                continue;
            }

            if (!store.getProviderName().equals(DataStoreProvider.NFS_IMAGE)) {
                s_logger.warn("Destination image store : " + store.getName() + " not NFS based. Store not suitable for migration!");
                continue;
            }

            if (srcStoreDcId != null && store.getDataCenterId() != null && !srcStoreDcId.equals(store.getDataCenterId())) {
                s_logger.warn("Source and destination stores are not in the same zone. Skipping destination store: " + store.getName());
                continue;
            }

            destDatastores.add(id);
            imagestores.add(store.getName());
        }

        if (destDatastores.size() < 1) {
            throw new CloudRuntimeException("No destination valid store(s) available to migrate. Could" +
                    "be due to invalid store ID(s) or store(s) are read-only. Terminating Migration of data");
        }

        if (isMigrateJobRunning()){
            message = "A migrate job is in progress, please try again later...";
            return new MigrationResponse(message, policy.toString(), false);
        }

        CallContext.current().setEventDetails("Migrating files/data objects " + "from : " + imagestores.get(0) + " to: " + imagestores.subList(1, imagestores.size()));
        return  stgService.migrateData(srcImgStoreId, destDatastores, policy);
    }


    // Ensures that only one migrate job may occur at a time, in order to reduce load
    private boolean isMigrateJobRunning() {
        long count = jobMgr.countPendingJobs(null, MigrateSecondaryStorageDataCmd.class.getName());
        if (count > 1) {
            return true;
        }
        return false;
    }
}
