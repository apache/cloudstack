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
import com.cloud.api.query.dao.StoragePoolJoinDao;
import com.cloud.api.query.dao.TemplateJoinDao;
import com.cloud.api.query.vo.ImageStoreJoinVO;
import com.cloud.api.query.vo.TemplateJoinVO;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Snapshot;
import com.cloud.storage.SnapshotVO;
import com.cloud.storage.dao.SnapshotDao;
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
import org.apache.cloudstack.storage.datastore.db.ImageStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.SnapshotDataStoreVO;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class StorageBrowserImpl extends MutualExclusiveIdsManagerBase implements StorageBrowser {

    @Inject
    ImageStoreJoinDao imageStoreJoinDao;

    @Inject
    ImageStoreDao imageStoreDao;

    @Inject
    DataStoreManager dataStoreMgr;

    @Inject
    TemplateJoinDao templateJoinDao;

    @Inject
    SnapshotDataStoreDao snapshotDataStoreDao;

    @Inject
    SnapshotDao snapshotDao;

    @Inject
    EndPointSelector endPointSelector;

    @Inject
    StoragePoolJoinDao storagePoolJoinDao;

    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<>();
        cmdList.add(ListImageStoreObjectsCmd.class);
        cmdList.add(ListStoragePoolObjectsCmd.class);
        return cmdList;
    }

    private ListResponse<DataStoreObjectResponse> listObjectsInStore(DataStore dataStore, String path) {
        EndPoint ep = endPointSelector.select(dataStore);

        if (ep == null) {
            throw new CloudRuntimeException("No secondary storage VM for image store " + dataStore.getUuid());
        }

        ListDataStoreObjectsCommand listDSCmd = new ListDataStoreObjectsCommand(dataStore.getTO(), path);
        Answer answer = ep.sendMessage(listDSCmd);
        if (answer == null || !answer.getResult() || !(answer instanceof ListDataStoreObjectsAnswer)) {
            throw new CloudRuntimeException("Failed to list image store objects");
        }
        ListDataStoreObjectsAnswer dsAnswer = (ListDataStoreObjectsAnswer) answer;

        List<DataStoreObjectResponse> responses = new ArrayList<>();

        List<String> paths = dsAnswer.getPaths();

        ArrayList<String> installPathList = new ArrayList<>();
        for (String filePath : paths) {
            installPathList.add(filePath);
            if (filePath.startsWith("/")) {
                installPathList.add(filePath.substring(1));
            } else {
                installPathList.add("/" + filePath);
            }
        }

        HashMap<String, String> snapshotPathUuidMap = new HashMap<>();
        List<SnapshotDataStoreVO> snapshotDataStoreList = snapshotDataStoreDao.listByStoreAndInstallPath(
                dataStore.getId(), installPathList);
        if (snapshotDataStoreList != null && !snapshotDataStoreList.isEmpty()) {
            List<SnapshotVO> snapshots = snapshotDao.listByIds(
                    snapshotDataStoreList.stream().map(SnapshotDataStoreVO::getSnapshotId).toArray());

            Map<Long, SnapshotVO> snapshotMap = snapshots.stream().collect(
                    Collectors.toMap(Snapshot::getId, snapshot -> snapshot));


            for (SnapshotDataStoreVO snapshotDataStore : snapshotDataStoreList) {
                snapshotPathUuidMap.put(snapshotDataStore.getInstallPath(),
                        snapshotMap.get(snapshotDataStore.getSnapshotId()).getUuid());
            }
        }

        List<TemplateJoinVO> templateJoinList = templateJoinDao.listByStoreAndInstallPath(dataStore.getId(),
                installPathList);
        HashMap<String, TemplateJoinVO> templateJoinMap = new HashMap<>();
        for (TemplateJoinVO templateJoin : templateJoinList) {
            templateJoinMap.put(templateJoin.getInstallPath(), templateJoin);
        }

        for (int i = 0; i < paths.size(); i++) {
            DataStoreObjectResponse response = new DataStoreObjectResponse(
                    dsAnswer.getNames().get(i),
                    dsAnswer.getIsDirs().get(i),
                    dsAnswer.getSizes().get(i),
                    new Date(dsAnswer.getLastModified().get(i)));
            if (dsAnswer.getIsDirs().get(i) != null) {
                String filePath = dsAnswer.getPaths().get(i);
                if (templateJoinMap.get(filePath) != null) {
                    response.setTemplateId(templateJoinMap.get(filePath).getUuid());
                    response.setFormat(templateJoinMap.get(filePath).getFormat().toString());
                }
                if (snapshotPathUuidMap.get(filePath) != null) {
                    response.setSnapshotId(snapshotPathUuidMap.get(filePath));
                }

                if (filePath.startsWith("/")) {
                    filePath = filePath.substring(1);
                } else {
                    filePath = "/".concat(filePath);
                }
                if (templateJoinMap.get(filePath) != null) {
                    response.setTemplateId(templateJoinMap.get(filePath).getUuid());
                    response.setFormat(templateJoinMap.get(filePath).getFormat().toString());
                }
                if (snapshotPathUuidMap.get(filePath) != null) {
                    response.setSnapshotId(snapshotPathUuidMap.get(filePath));
                }
            }
            responses.add(response);
        }

        ListResponse<DataStoreObjectResponse> listResponse = new ListResponse<>();
        if (dsAnswer.isPathExists()) {
            listResponse.setResponses(responses);
            return listResponse;
        } else {
            throw new IllegalArgumentException("Path " + path + " doesn't exist in image store " + dataStore.getUuid());
        }
    }

    @Override
    public ListResponse<DataStoreObjectResponse> listImageStore(ListImageStoreObjectsCmd cmd) {
        Long imageStoreId = cmd.getStoreId();
        String path = cmd.getPath();

        ImageStoreJoinVO imageStore = imageStoreJoinDao.findById(imageStoreId);

        DataStore dataStore = dataStoreMgr.getDataStore(imageStoreId, imageStore.getRole());

        return listObjectsInStore(dataStore, path);
    }

    @Override
    public ListResponse<DataStoreObjectResponse> listPrimaryStore(ListStoragePoolObjectsCmd cmd) {
        Long storeId = cmd.getStoreId();
        String path = cmd.getPath();

        DataStore dataStore = dataStoreMgr.getDataStore(storeId, DataStoreRole.Primary);

        return listObjectsInStore(dataStore, path);
    }
}
