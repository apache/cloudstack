/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.storage.service;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.storage.feign.client.SANFeignClient;
import org.apache.cloudstack.storage.feign.model.Lun;
import org.apache.cloudstack.storage.feign.model.LunSpace;
import org.apache.cloudstack.storage.feign.model.OntapStorage;
import org.apache.cloudstack.storage.feign.model.Svm;
import org.apache.cloudstack.storage.feign.model.response.OntapResponse;
import org.apache.cloudstack.storage.utils.Constants;
import org.apache.cloudstack.storage.utils.Utility;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.net.URI;

public class UnifiedSANStrategy extends SANStrategy {

    private static final Logger logger = (Logger) LogManager.getLogger(UnifiedSANStrategy.class);

    @Inject
    private Utility utils;

    @Inject
    private SANFeignClient sanFeignClient;

    public UnifiedSANStrategy(OntapStorage ontapStorage) {
        super(ontapStorage);
    }

    @Override
    public Lun createLUN(String svmName, String volumeName, String lunName, long sizeBytes, String osType) {
        logger.info("Creating Lun: {} under volume {} for SVM: {} with size {} and osType {} ", lunName, volumeName, svmName, sizeBytes, osType);
        try {
            // Get AuthHeader
            String authHeader = utils.generateAuthHeader(OntapStorage.username, OntapStorage.password);

            // Create Lun Ontap Request
            Lun lunRequest = new Lun();
            //Lun name format: /vol/volume_name/lun_name, here volume_name is existing volume name
            String lunFullName = Constants.VOLUME_PATH_PREFIX + volumeName + Constants.PATH_SEPARATOR + lunName;
            lunRequest.setName(lunFullName);
            lunRequest.setOsType(Lun.OsTypeEnum.valueOf(osType));

            Svm svm = new Svm();
            svm.setName(svmName);
            lunRequest.setSvm(svm);

            LunSpace lunSpace = new LunSpace();
            lunSpace.setSize(sizeBytes);
            lunRequest.setSpace(lunSpace);

            // Create URI for lun creation
            URI url = utils.generateURI(Constants.CREATE_LUN);
            OntapResponse<Lun> createdLun =  sanFeignClient.createLun(url, authHeader, true, lunRequest);
            if (createdLun == null || createdLun.getRecords() == null || createdLun.getRecords().size() == 0) {
                logger.error("LUN creation failed for Lun: {} under volume {} for SVM: {}", lunName, volumeName, svmName);
                throw new CloudRuntimeException("Failed to create Lun: " + lunName);
            }
            Lun lun = createdLun.getRecords().get(0);
            logger.info("LUN created successfully. Lun: {}", lun);
            return lun;
        } catch (Exception e) {
            logger.error("Exception occurred while creating LUN: {} under volume {} for SVM: {}. Exception: {}", lunName, volumeName, svmName, e.getMessage());
            throw new CloudRuntimeException("Failed to create Lun: " + e.getMessage());
        }
    }

    @Override
    public String createIgroup(String svmName, String igroupName, String[] initiators) {
        return "";
    }

    @Override
    public String mapLUNToIgroup(String lunName, String igroupName) {
        return "";
    }

    @Override
    public String enableISCSI(String svmUuid) {
        return "";
    }
}