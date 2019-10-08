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
package com.cloud.storage.copy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.storage.ImageStoreDetailsUtil;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.mount.MountManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

@Component
public class NfsManagementServerCopier implements ManagementServerCopier {
    private static final Logger s_logger = Logger.getLogger(NfsManagementServerCopier.class);

    @Inject
    private MountManager mountManager;
    @Inject
    private ImageStoreDetailsUtil imageStoreDetailsUtil;
    @Inject
    private TemplateDataStoreDao vmTemplateStoreDao;


    public boolean copy(TemplateInfo srcTemplate, DataStore destStore) {
        Integer nfsVersion = imageStoreDetailsUtil.getNfsVersion(srcTemplate.getDataStore().getId());
        String sourceMountPoint = mountManager.getMountPoint(srcTemplate.getDataStore().getUri(), nfsVersion);
        String destMountPoint = mountManager.getMountPoint(destStore.getUri(), nfsVersion);
        String installPath  = getTemplateDataStore(srcTemplate).getInstallPath();
        installPath = installPath.substring(0, installPath.lastIndexOf("/"));
        String destinationPath = Paths.get(destMountPoint, installPath).toString();
        String sourcePath = Paths.get(sourceMountPoint, installPath).toString();
        createFolder(Paths.get(destMountPoint, installPath));
        if (copyFolderContent(sourcePath, destinationPath)) {
            persistTemplateStoreRef(srcTemplate, destStore, destinationPath);
            return true;
        }
        return false;
    }

    private boolean copyFolderContent(String sourcePath, String destinationPath) {
        String command = String.format("cp %s%s* %s", sourcePath, File.separator, destinationPath);
        String result = Script.runSimpleBashScript(command);
        if (result != null) {
            s_logger.warn(String.format("Unable to copy from %s to %s due to %s.%n", sourcePath, destinationPath, result));
            return false;
        }
        return true;
    }

    /**
     * Create folder on path if it does not exist
     */
    public static void createFolder(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            s_logger.error(String.format("Unable to create directory from path %s: %s.%n", path, e.toString()));
            throw new CloudRuntimeException(e);
        }
    }

    private TemplateDataStoreVO getTemplateDataStore(TemplateInfo template) {
        return Optional.ofNullable(vmTemplateStoreDao.findByStoreTemplate(template.getDataStore().getId(), template.getId()))
                .orElseThrow(() -> new CloudRuntimeException(String.format("Unable to find template store ref by template id %d.%n", template.getId())));
    }

    private void persistTemplateStoreRef(TemplateInfo template, DataStore destStore, String destinationPath) {
        TemplateDataStoreVO vmTemplateStore = vmTemplateStoreDao.findByStoreTemplate(destStore.getId(), template.getId());
        if (vmTemplateStore == null) {
            vmTemplateStore = new TemplateDataStoreVO(destStore.getId(), template.getId(), new Date(), 100,
                    VMTemplateStorageResourceAssoc.Status.DOWNLOADED, template.getInstallPath(), null, null, template.getInstallPath(), template.getUri());
            vmTemplateStore.setDataStoreRole(destStore.getRole());
            vmTemplateStoreDao.persist(vmTemplateStore);
        } else {
            vmTemplateStore.setDownloadPercent(100);
            vmTemplateStore.setDownloadState(VMTemplateStorageResourceAssoc.Status.DOWNLOADED);
            vmTemplateStore.setSize(Optional.ofNullable(template.getSize()).orElse(0L));
            vmTemplateStore.setPhysicalSize(Optional.ofNullable(template.getSize()).orElse(0L));
            vmTemplateStore.setLastUpdated(new Date());
            vmTemplateStore.setInstallPath(template.getInstallPath());
            vmTemplateStore.setLocalDownloadPath(destinationPath);
            vmTemplateStore.setDownloadUrl(template.getUri());
            vmTemplateStore.setState(ObjectInDataStoreStateMachine.State.Ready);
            vmTemplateStoreDao.update(vmTemplateStore.getId(), vmTemplateStore);
        }
    }
}
