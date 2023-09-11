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
import com.cloud.storage.VMTemplateStoragePoolVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.SnapshotDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplatePoolDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.command.admin.storage.ListImageStoreObjectsCmd;
import org.apache.cloudstack.api.command.admin.storage.ListStoragePoolObjectsCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.storage.command.browser.ListDataStoreObjectsAnswer;
import org.apache.cloudstack.storage.command.browser.ListDataStoreObjectsCommand;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.TemplateDataStoreVO;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class StorageBrowserImpl extends MutualExclusiveIdsManagerBase implements StorageBrowser {

    @Inject
    ImageStoreJoinDao imageStoreJoinDao;

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

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(ListImageStoreObjectsCmd.class);
        cmdList.add(ListStoragePoolObjectsCmd.class);
        return cmdList;
    }

    @Override
    public ListResponse<DataStoreObjectResponse> listImageStore(ListImageStoreObjectsCmd cmd) {
        Long imageStoreId = cmd.getStoreId();
        String path = cmd.getPath();
        int page = Objects.requireNonNullElse(cmd.getPage(), 1);

        ImageStoreJoinVO imageStore = imageStoreJoinDao.findById(imageStoreId);
        DataStore dataStore = dataStoreMgr.getDataStore(imageStoreId, imageStore.getRole());
        ListDataStoreObjectsAnswer answer = listObjectsInStore(dataStore, path, page, cmd.getPageSize());

        return getResponse(dataStore, answer);
    }

    @Override
    public ListResponse<DataStoreObjectResponse> listPrimaryStore(ListStoragePoolObjectsCmd cmd) {
        Long storeId = cmd.getStoreId();
        String path = cmd.getPath();
        int page = Objects.requireNonNullElse(cmd.getPage(), 1);

        DataStore dataStore = dataStoreMgr.getDataStore(storeId, DataStoreRole.Primary);
        ListDataStoreObjectsAnswer answer = listObjectsInStore(dataStore, path, page, cmd.getPageSize());

        return getResponse(dataStore, answer);
    }

    private ListDataStoreObjectsAnswer listObjectsInStore(DataStore dataStore, String path, int page, int pageSize) {
        EndPoint ep = endPointSelector.select(dataStore);

        if (ep == null) {
            throw new CloudRuntimeException("No remote endpoint to send command");
        }

        ListDataStoreObjectsCommand listDSCmd = new ListDataStoreObjectsCommand(dataStore.getTO(), path, page, pageSize);
        Answer answer = ep.sendMessage(listDSCmd);
        if (answer == null || !answer.getResult() || !(answer instanceof ListDataStoreObjectsAnswer)) {
            throw new CloudRuntimeException("Failed to list datastore objects");
        }

        ListDataStoreObjectsAnswer dsAnswer = (ListDataStoreObjectsAnswer) answer;
        if (!dsAnswer.isPathExists()) {
            throw new IllegalArgumentException("Path " + path + " doesn't exist in store" + dataStore.getUuid());
        }
        return dsAnswer;
    }

    private ListResponse<DataStoreObjectResponse> getResponse(DataStore dataStore, ListDataStoreObjectsAnswer answer) {
        List<DataStoreObjectResponse> responses = new ArrayList<>();

        List<String> paths = getFormattedPaths(answer.getPaths());
        List<String> absPaths = answer.getAbsPaths();

        Map<String, SnapshotVO> pathSnapshotMap = getPathSnapshotMap(dataStore, paths, absPaths);

        Map<String, VMTemplateVO> pathTemplateMap = getPathTemplateMap(dataStore, paths);

        Map<String, VolumeVO> pathVolumeMap = getPathVolumeMap(dataStore, paths);

        for (int i = 0; i < paths.size(); i++) {
            DataStoreObjectResponse response = new DataStoreObjectResponse(
                    answer.getNames().get(i),
                    answer.getIsDirs().get(i),
                    answer.getSizes().get(i),
                    new Date(answer.getLastModified().get(i)));
            String filePath = paths.get(i);
            if (pathTemplateMap.get(filePath) != null) {
                response.setTemplateId(pathTemplateMap.get(filePath).getUuid());
                response.setFormat(pathTemplateMap.get(filePath).getFormat().toString());
            }
            if (pathSnapshotMap.get(filePath) != null) {
                response.setSnapshotId(pathSnapshotMap.get(filePath).getUuid());
            }
            if (pathVolumeMap.get(filePath) != null) {
                response.setVolumeId(pathVolumeMap.get(filePath).getUuid());
            }
            responses.add(response);
        }

        ListResponse<DataStoreObjectResponse> listResponse = new ListResponse<>();
        listResponse.setResponses(responses, answer.getCount());
        return listResponse;
    }

    private List<String> getFormattedPaths(List<String> paths) {
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

    private Map<String, SnapshotVO> getPathSnapshotMap(DataStore dataStore, List<String> paths,
            List<String> absolutePaths) {
        HashMap<String, SnapshotVO> snapshotPathMap = new HashMap<>();
        // If dataStore is primary, we query using absolutePaths else we query using paths.
        List<SnapshotDataStoreVO> snapshotDataStoreList = snapshotDataStoreDao.listByStoreAndInstallPaths(dataStore.getId(), dataStore.getRole(),
                dataStore.getRole() == DataStoreRole.Primary ? absolutePaths : paths);
        if (!CollectionUtils.isEmpty(snapshotDataStoreList)) {
            List<SnapshotVO> snapshots = snapshotDao.listByIds(
                    snapshotDataStoreList.stream().map(SnapshotDataStoreVO::getSnapshotId).toArray());

            Map<Long, SnapshotVO> snapshotMap = snapshots.stream().collect(
                    Collectors.toMap(Snapshot::getId, snapshot -> snapshot));

            // In case of primary data store, absolute path is stored in database.
            // We use this map to create a mapping between relative path and absolute path
            // which is used to create a mapping between relative path and snapshot.
            Map<String, String> absolutePathPathMap = new HashMap<>();
            if (dataStore.getRole() == DataStoreRole.Primary) {
                for (int i = 0; i < paths.size(); i++) {
                    absolutePathPathMap.put(absolutePaths.get(i), paths.get(i));
                }
            }

            for (SnapshotDataStoreVO snapshotDataStore : snapshotDataStoreList) {
                if (dataStore.getRole() == DataStoreRole.Primary) {
                    snapshotPathMap.put(absolutePathPathMap.get(snapshotDataStore.getInstallPath()),
                            snapshotMap.get(snapshotDataStore.getSnapshotId()));
                } else {
                    snapshotPathMap.put(snapshotDataStore.getInstallPath(),
                            snapshotMap.get(snapshotDataStore.getSnapshotId()));
                }
            }
        }

        return snapshotPathMap;
    }

    private Map<String, VMTemplateVO> getPathTemplateMap(DataStore dataStore, List<String> paths) {
        HashMap<String, VMTemplateVO> pathTemplateMap = new HashMap<>();
        if (dataStore.getRole() != DataStoreRole.Primary) {
            List<TemplateDataStoreVO> templateList = templateDataStoreDao.listByStoreIdAndInstallPaths(dataStore.getId(), paths);
            if (!CollectionUtils.isEmpty(templateList)) {
                List<VMTemplateVO> templates = templateDao.listByIds(templateList.stream().map(TemplateDataStoreVO::getTemplateId).collect(Collectors.toList()));

                Map<Long, VMTemplateVO> templateMap = templates.stream().collect(
                        Collectors.toMap(VMTemplateVO::getId, template -> template));

                for (TemplateDataStoreVO templateDataStore : templateList) {
                    pathTemplateMap.put(templateDataStore.getInstallPath(),
                            templateMap.get(templateDataStore.getTemplateId()));
                }
            }

        } else {
            List<VMTemplateStoragePoolVO> templateStoragePoolList = templatePoolDao.listByPoolIdAndInstallPath(dataStore.getId(), paths);
            if (!CollectionUtils.isEmpty(templateStoragePoolList)) {
                List<VMTemplateVO> templates = templateDao.listByIds
                        (templateStoragePoolList.stream().map(VMTemplateStoragePoolVO::getTemplateId).collect(Collectors.toList()));

                Map<Long, VMTemplateVO> templateMap = templates.stream().collect(
                        Collectors.toMap(VMTemplateVO::getId, template -> template));

                for (VMTemplateStoragePoolVO templatePool : templateStoragePoolList) {
                    pathTemplateMap.put(templatePool.getInstallPath(),
                            templateMap.get(templatePool.getTemplateId()));
                }
            }
        }
        return pathTemplateMap;
    }

    private HashMap<String, VolumeVO> getPathVolumeMap(DataStore dataStore, List<String> paths) {
        HashMap<String, VolumeVO> volumePathMap = new HashMap<>();
        if (dataStore.getRole() != DataStoreRole.Primary) {
            return volumePathMap;
        }
        List<VolumeVO> volumeList = volumeDao.listByPoolIdAndPaths(dataStore.getId(), paths);
        if (!CollectionUtils.isEmpty(volumeList)) {
            for (VolumeVO volume : volumeList) {
                volumePathMap.put(volume.getPath(), volume);
            }
        }
        return volumePathMap;
    }
}
