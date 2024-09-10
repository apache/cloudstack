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

package org.apache.cloudstack.storage.browser;

import com.cloud.agent.api.Answer;
import com.cloud.api.query.MutualExclusiveIdsManagerBase;
import com.cloud.api.query.dao.ImageStoreJoinDao;
import com.cloud.api.query.vo.ImageStoreJoinVO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.Storage;
import com.cloud.storage.Upload;
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.command.admin.storage.DownloadImageStoreObjectCmd;
import org.apache.cloudstack.api.command.admin.storage.ListImageStoreObjectsCmd;
import org.apache.cloudstack.api.command.admin.storage.ListStoragePoolObjectsCmd;
import org.apache.cloudstack.api.response.ExtractResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.diagnostics.to.DiagnosticsDataObject;
import org.apache.cloudstack.diagnostics.to.DiagnosticsDataTO;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.storage.command.browser.ListDataStoreObjectsAnswer;
import org.apache.cloudstack.storage.command.browser.ListDataStoreObjectsCommand;
import org.apache.cloudstack.storage.datastore.db.ImageStoreObjectDownloadDao;
import org.apache.cloudstack.storage.datastore.db.ImageStoreObjectDownloadVO;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.VolumeDataStoreVO;
import org.apache.cloudstack.storage.image.datastore.ImageStoreEntity;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class StorageBrowserImpl extends MutualExclusiveIdsManagerBase implements StorageBrowser {

    @Inject
    ImageStoreJoinDao imageStoreJoinDao;

    @Inject
    ImageStoreObjectDownloadDao imageStoreObjectDownloadDao;

    @Inject
    DataStoreManager dataStoreMgr;

    @Inject
    TemplateDataStoreDao templateDataStoreDao;

    @Inject
    SnapshotDataStoreDao snapshotDataStoreDao;

    @Inject
    SnapshotDao snapshotDao;

    @Inject
    EndPointSelector endPointSelector;

    @Inject
    VMTemplatePoolDao templatePoolDao;

    @Inject
    VMTemplateDao templateDao;

    @Inject
    VolumeDao volumeDao;

    @Inject
    VolumeDataStoreDao volumeDataStoreDao;

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(ListImageStoreObjectsCmd.class);
        cmdList.add(ListStoragePoolObjectsCmd.class);
        cmdList.add(DownloadImageStoreObjectCmd.class);
        return cmdList;
    }

    @Override
    public ListResponse<DataStoreObjectResponse> listImageStoreObjects(ListImageStoreObjectsCmd cmd) {
        Long imageStoreId = cmd.getStoreId();
        String path = cmd.getPath();

        ImageStoreJoinVO imageStore = imageStoreJoinDao.findById(imageStoreId);
        DataStore dataStore = dataStoreMgr.getDataStore(imageStoreId, imageStore.getRole());
        ListDataStoreObjectsAnswer answer = listObjectsInStore(dataStore, path, cmd.getStartIndex().intValue(), cmd.getPageSizeVal().intValue());

        return getResponse(dataStore, answer);
    }

    @Override
    public ListResponse<DataStoreObjectResponse> listPrimaryStoreObjects(ListStoragePoolObjectsCmd cmd) {
        Long storeId = cmd.getStoreId();
        String path = cmd.getPath();

        DataStore dataStore = dataStoreMgr.getDataStore(storeId, DataStoreRole.Primary);
        ListDataStoreObjectsAnswer answer = listObjectsInStore(dataStore, path, cmd.getStartIndex().intValue(), cmd.getPageSizeVal().intValue());

        return getResponse(dataStore, answer);
    }

    @Override
    public ExtractResponse downloadImageStoreObject(DownloadImageStoreObjectCmd cmd) {
        ImageStoreEntity imageStore = (ImageStoreEntity) dataStoreMgr.getDataStore(cmd.getStoreId(), DataStoreRole.Image);

        String path = cmd.getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        ImageStoreObjectDownloadVO imageStoreObj = imageStoreObjectDownloadDao.findByStoreIdAndPath(cmd.getStoreId(), path);

        if (imageStoreObj == null) {
            try {
                String fileExt = path.substring(path.lastIndexOf(".") + 1);
                Storage.ImageFormat format = EnumUtils.getEnumIgnoreCase(Storage.ImageFormat.class, fileExt);

                DiagnosticsDataTO dataTO = new DiagnosticsDataTO(imageStore.getTO());
                DiagnosticsDataObject dataObject = new DiagnosticsDataObject(dataTO, imageStore);
                String downloadUrl = imageStore.createEntityExtractUrl(path, format, dataObject);
                imageStoreObj = imageStoreObjectDownloadDao.persist(new ImageStoreObjectDownloadVO(imageStore.getId(), path, downloadUrl));
            } catch (Exception e) {
                throw new CloudRuntimeException("Failed to create download url for image store object", e);
            }
        }
        ExtractResponse response = new ExtractResponse(null, null, CallContext.current().getCallingAccountUuid(), null, null);
        if (imageStoreObj != null) {
            response.setUrl(imageStoreObj.getDownloadUrl());
            response.setName(cmd.getPath().substring(cmd.getPath().lastIndexOf("/") + 1));
            response.setState(Upload.Status.DOWNLOAD_URL_CREATED.toString());
        } else {
            response.setState(Upload.Status.DOWNLOAD_URL_NOT_CREATED.toString());
        }
        return response;
    }

    ListDataStoreObjectsAnswer listObjectsInStore(DataStore dataStore, String path, int startIndex, int pageSize) {
        EndPoint ep = endPointSelector.select(dataStore);

        if (ep == null) {
            throw new CloudRuntimeException("No remote endpoint to send command");
        }

        ListDataStoreObjectsCommand listDSCmd = new ListDataStoreObjectsCommand(dataStore.getTO(), path, startIndex, pageSize);
        listDSCmd.setWait(15);
        Answer answer = null;
        try {
            answer = ep.sendMessage(listDSCmd);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to list datastore objects", e);
        }

        if (answer == null || !answer.getResult() || !(answer instanceof ListDataStoreObjectsAnswer)) {
            throw new CloudRuntimeException("Failed to list datastore objects");
        }

        ListDataStoreObjectsAnswer dsAnswer = (ListDataStoreObjectsAnswer) answer;
        if (!dsAnswer.isPathExists()) {
            throw new IllegalArgumentException("Path " + path + " doesn't exist in store: " + dataStore.getUuid());
        }
        return dsAnswer;
    }

    ListResponse<DataStoreObjectResponse> getResponse(DataStore dataStore, ListDataStoreObjectsAnswer answer) {
        List<DataStoreObjectResponse> responses = new ArrayList<>();

        List<String> paths = getFormattedPaths(answer.getPaths());
        List<String> absPaths = answer.getAbsPaths();

        Map<String, SnapshotVO> pathSnapshotMap;

        Map<String, VMTemplateVO> pathTemplateMap;

        Map<String, VolumeVO> pathVolumeMap;

        if (dataStore.getRole() != DataStoreRole.Primary) {
            pathTemplateMap = getPathTemplateMapForSecondaryDS(dataStore.getId(), paths);
            pathSnapshotMap = getPathSnapshotMapForSecondaryDS(dataStore.getId(), paths);
            pathVolumeMap = getPathVolumeMapForSecondaryDS(dataStore.getId(), paths);
        } else {
            pathTemplateMap = getPathTemplateMapForPrimaryDS(dataStore.getId(), paths);
            pathSnapshotMap = getPathSnapshotMapForPrimaryDS(dataStore.getId(), paths, absPaths);
            pathVolumeMap = getPathVolumeMapForPrimaryDS(dataStore.getId(), paths);
        }

        for (int i = 0; i < paths.size(); i++) {
            DataStoreObjectResponse response = new DataStoreObjectResponse(
                    answer.getNames().get(i),
                    answer.getIsDirs().get(i),
                    answer.getSizes().get(i),
                    new Date(answer.getLastModified().get(i)));
            String filePath = paths.get(i);
            if (pathTemplateMap.get(filePath) != null) {
                VMTemplateVO vmTemplateVO = pathTemplateMap.get(filePath);
                response.setTemplateId(vmTemplateVO.getUuid());
                response.setFormat(vmTemplateVO.getFormat().toString());
                response.setTemplateName(vmTemplateVO.getName());
            }
            if (pathSnapshotMap.get(filePath) != null) {
                SnapshotVO snapshotVO = pathSnapshotMap.get(filePath);
                response.setSnapshotId(snapshotVO.getUuid());
                response.setSnapshotName(snapshotVO.getName());
            }
            if (pathVolumeMap.get(filePath) != null) {
                VolumeVO volumeVO = pathVolumeMap.get(filePath);
                response.setVolumeId(volumeVO.getUuid());
                response.setVolumeName(volumeVO.getName());
            }
            responses.add(response);
        }

        ListResponse<DataStoreObjectResponse> listResponse = new ListResponse<>();
        listResponse.setResponses(responses, answer.getCount());
        return listResponse;
    }

    List<String> getFormattedPaths(List<String> paths) {
        List<String> formattedPaths = new ArrayList<>();
        for (String path : paths) {
            String normalizedPath = Path.of(path).normalize().toString();
            if (normalizedPath.startsWith("/")) {
                formattedPaths.add(normalizedPath.substring(1));
            } else {
                formattedPaths.add(normalizedPath);
            }
        }
        return formattedPaths;
    }

    Map<String, VMTemplateVO> getPathTemplateMapForSecondaryDS(Long dataStoreId, List<String> paths) {
        Map<String, VMTemplateVO> pathTemplateMap = new HashMap<>();
        List<TemplateDataStoreVO> templateList = templateDataStoreDao.listByStoreIdAndInstallPaths(dataStoreId, paths);
        if (!CollectionUtils.isEmpty(templateList)) {
            List<VMTemplateVO> templates = templateDao.listByIds(templateList.stream().map(TemplateDataStoreVO::getTemplateId).collect(Collectors.toList()));

            Map<Long, VMTemplateVO> templateMap = templates.stream().collect(
                    Collectors.toMap(VMTemplateVO::getId, template -> template));

            for (TemplateDataStoreVO templateDataStore : templateList) {
                pathTemplateMap.put(templateDataStore.getInstallPath(),
                        templateMap.get(templateDataStore.getTemplateId()));
            }
        }
        return pathTemplateMap;
    }

    Map<String, SnapshotVO> getPathSnapshotMapForSecondaryDS(Long dataStoreId, List<String> paths) {
        Map<String, SnapshotVO> snapshotPathMap = new HashMap<>();
        List<SnapshotDataStoreVO> snapshotDataStoreList = snapshotDataStoreDao.listByStoreAndInstallPaths(dataStoreId, DataStoreRole.Image, paths);
        if (!CollectionUtils.isEmpty(snapshotDataStoreList)) {
            List<SnapshotVO> snapshots = snapshotDao.listByIds(
                    snapshotDataStoreList.stream().map(SnapshotDataStoreVO::getSnapshotId).toArray());

            Map<Long, SnapshotVO> snapshotMap = snapshots.stream().collect(
                    Collectors.toMap(Snapshot::getId, snapshot -> snapshot));

            for (SnapshotDataStoreVO snapshotDataStore : snapshotDataStoreList) {
                snapshotPathMap.put(snapshotDataStore.getInstallPath(), snapshotMap.get(snapshotDataStore.getSnapshotId()));
            }
        }

        return snapshotPathMap;
    }

    Map<String, VMTemplateVO> getPathTemplateMapForPrimaryDS(Long dataStoreId, List<String> paths) {
        Map<String, VMTemplateVO> pathTemplateMap = new HashMap<>();
        // get a map of paths without extension to path. We do this because extension is not saved in database for xen server.
        Map<String, String> pathWithoutExtensionMap = new HashMap<>();
        for (String path : paths) {
            if (path.contains(".")) {
                String pathWithoutExtension = path.substring(0, path.lastIndexOf("."));
                pathWithoutExtensionMap.put(pathWithoutExtension, path);
            }
        }
        List<String> pathList = Stream.concat(paths.stream(), pathWithoutExtensionMap.keySet().stream()).collect(Collectors.toList());
        List<VMTemplateStoragePoolVO> templateStoragePoolList = templatePoolDao.listByPoolIdAndInstallPath(dataStoreId, pathList);
        if (!CollectionUtils.isEmpty(templateStoragePoolList)) {
            List<VMTemplateVO> templates = templateDao.listByIds
                    (templateStoragePoolList.stream().map(VMTemplateStoragePoolVO::getTemplateId).collect(Collectors.toList()));

            Map<Long, VMTemplateVO> templateMap = templates.stream().collect(
                    Collectors.toMap(VMTemplateVO::getId, template -> template));

            for (VMTemplateStoragePoolVO templatePool : templateStoragePoolList) {
                pathTemplateMap.put(templatePool.getInstallPath(), templateMap.get(templatePool.getTemplateId()));
                if (pathWithoutExtensionMap.get(templatePool.getInstallPath()) != null) {
                    pathTemplateMap.put(pathWithoutExtensionMap.get(templatePool.getInstallPath()), templateMap.get(templatePool.getTemplateId()));
                }
            }
        }
        return pathTemplateMap;
    }

    Map<String, SnapshotVO> getPathSnapshotMapForPrimaryDS(Long dataStoreId, List<String> paths,
            List<String> absPaths) {
        Map<String, SnapshotVO> snapshotPathMap = new HashMap<>();
        // get a map of paths without extension to path. We do this because extension is not saved in database for xen server.
        Map<String, String> absPathWithoutExtensionMap = new HashMap<>();
        for (String path : absPaths) {
            if (path.contains(".")) {
                String pathWithoutExtension = path.substring(0, path.lastIndexOf("."));
                absPathWithoutExtensionMap.put(pathWithoutExtension, path);
            }
        }
        List<String> absPathList = Stream.concat(absPaths.stream(), absPathWithoutExtensionMap.keySet().stream()).collect(Collectors.toList());
        // For primary dataStore, we query using absolutePaths
        List<SnapshotDataStoreVO> snapshotDataStoreList = snapshotDataStoreDao.listByStoreAndInstallPaths(dataStoreId, DataStoreRole.Primary, absPathList);
        if (!CollectionUtils.isEmpty(snapshotDataStoreList)) {
            List<SnapshotVO> snapshots = snapshotDao.listByIds(snapshotDataStoreList.stream().map(SnapshotDataStoreVO::getSnapshotId).toArray());

            Map<Long, SnapshotVO> snapshotMap = snapshots.stream().collect(
                    Collectors.toMap(Snapshot::getId, snapshot -> snapshot));

            // In case of primary data store, absolute path is stored in database.
            // We use this map to create a mapping between relative path and absolute path
            // which is used to create a mapping between relative path and snapshot.
            Map<String, String> absolutePathPathMap = new HashMap<>();
            for (int i = 0; i < paths.size(); i++) {
                absolutePathPathMap.put(absPaths.get(i), paths.get(i));
            }

            for (SnapshotDataStoreVO snapshotDataStore : snapshotDataStoreList) {
                snapshotPathMap.put(absolutePathPathMap.get(snapshotDataStore.getInstallPath()),
                        snapshotMap.get(snapshotDataStore.getSnapshotId()));

                if (absPathWithoutExtensionMap.get(snapshotDataStore.getInstallPath()) != null) {
                    snapshotPathMap.put(
                            absolutePathPathMap.get(
                                    absPathWithoutExtensionMap.get(
                                            snapshotDataStore.getInstallPath()
                                    )), snapshotMap.get(snapshotDataStore.getSnapshotId()));
                }
            }
        }

        return snapshotPathMap;
    }

    Map<String, VolumeVO> getPathVolumeMapForPrimaryDS(Long dataStoreId, List<String> paths) {
        Map<String, VolumeVO> volumePathMap = new HashMap<>();

        // get a map of paths without extension to path. We do this because extension is not saved in database for xen server.
        Map<String, String> pathWithoutExtensionMap = new HashMap<>();
        for (String path : paths) {
            if (path.contains(".")) {
                String pathWithoutExtension = path.substring(0, path.lastIndexOf("."));
                pathWithoutExtensionMap.put(pathWithoutExtension, path);
            }
        }
        List<String> pathList = Stream.concat(paths.stream(), pathWithoutExtensionMap.keySet().stream()).collect(Collectors.toList());
        List<VolumeVO> volumeList = volumeDao.listByPoolIdAndPaths(dataStoreId, pathList);
        if (!CollectionUtils.isEmpty(volumeList)) {
            for (VolumeVO volume : volumeList) {
                volumePathMap.put(volume.getPath(), volume);
                if (pathWithoutExtensionMap.get(volume.getPath()) != null) {
                    volumePathMap.put(pathWithoutExtensionMap.get(volume.getPath()), volume);
                }
            }
        }
        return volumePathMap;
    }

    Map<String, VolumeVO> getPathVolumeMapForSecondaryDS(Long dataStoreId, List<String> paths) {
        Map<String, VolumeVO> volumePathMap = new HashMap<>();
        List<VolumeDataStoreVO> volumeList = volumeDataStoreDao.listByStoreIdAndInstallPaths(dataStoreId, paths);
        if (!CollectionUtils.isEmpty(volumeList)) {
            List<Long> volumeIdList = volumeList.stream().map(VolumeDataStoreVO::getVolumeId).collect(Collectors.toList());
            List<VolumeVO> volumeVOS = volumeDao.listByIds(volumeIdList);
            Map<Long, VolumeVO> volumeMap = volumeVOS.stream().collect(Collectors.toMap(VolumeVO::getId, volume -> volume));

            for (VolumeDataStoreVO volumeDataStore : volumeList) {
                volumePathMap.put(volumeDataStore.getInstallPath(), volumeMap.get(volumeDataStore.getVolumeId()));
            }
        }
        return volumePathMap;
    }
}
