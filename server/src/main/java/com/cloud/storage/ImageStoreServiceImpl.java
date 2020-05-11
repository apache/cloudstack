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
import org.apache.cloudstack.engine.orchestration.service.StorageOrchestrationService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.storage.ImageStoreService;
import org.apache.cloudstack.storage.ImageStoreService.MigrationPolicy;
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreVO;
import org.apache.commons.lang3.EnumUtils;
import org.apache.log4j.Logger;

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
            "0.5",
            "The storage imbalance threshold that is compared with the standard deviation percentage for a storage utilization metric. " +
                    "The value is a percentage in decimal format.",
            true, ConfigKey.Scope.Global);


    public Integer numConcurrentCopyTasksPerSSVM = null;



    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    @Override
    public MigrationResponse migrateData(MigrateSecondaryStorageDataCmd cmd) {
        Long srcImgStoreId = cmd.getId();
        ImageStoreVO srcImageVO = imageStoreDao.findById(srcImgStoreId);
        List<Long> destImgStoreIds = cmd.getMigrateTo();
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

        if (srcImageVO.getRole() != DataStoreRole.Image) {
            throw new CloudRuntimeException("Secondary storage is not of Image Role");
        }

        if (destImgStoreIds.contains(srcImgStoreId)) {
            s_logger.debug("One of the destination stores is the same as the source image store ... Ignoring it...");
            destImgStoreIds.remove(srcImgStoreId);
        }

        // Validate all the Ids correspond to valid Image stores
        List<Long> destDatastores = new ArrayList<>();
        for (Long id : destImgStoreIds) {
            if (imageStoreDao.findById(id) == null) {
                s_logger.warn("Secondary storage with id: " + id + "is not found. Skipping it...");
                continue;
            }
            if (imageStoreDao.findById(id).isReadonly()) {
                s_logger.warn("Secondary storage: "+ id + " cannot be considered for migration as has read-only permission, Skipping it...");
                continue;
            }
            destDatastores.add(id);
        }

        if (destDatastores.size() < 1) {
            throw new CloudRuntimeException("Invalid destination image store(s) provided. Terminating Migration of data");
        }

        if (isMigrateJobRunning()){
            message = "A migrate job is in progress, please try again later...";
            return new MigrationResponse(message, policy.toString(), false);
        }

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
