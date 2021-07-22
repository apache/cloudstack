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
package org.apache.cloudstack.storage.motion;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionService;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageStrategyFactory;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.Host;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.utils.StringUtils;


@Component
public class DataMotionServiceImpl implements DataMotionService {
    private static final Logger LOGGER = Logger.getLogger(DataMotionServiceImpl.class);

    @Inject
    StorageStrategyFactory storageStrategyFactory;
    @Inject
    VolumeDao volDao;

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback) {
        if (srcData.getDataStore() == null || destData.getDataStore() == null) {
            String errMsg = "can't find data store";
            invokeCallback(errMsg, callback);
            return;
        }

        if (srcData.getDataStore().getDriver().canCopy(srcData, destData)) {
            srcData.getDataStore().getDriver().copyAsync(srcData, destData, destHost, callback);
            return;
        } else if (destData.getDataStore().getDriver().canCopy(srcData, destData)) {
            destData.getDataStore().getDriver().copyAsync(srcData, destData, destHost, callback);
            return;
        }

        DataMotionStrategy strategy = storageStrategyFactory.getDataMotionStrategy(srcData, destData);
        if (strategy == null) {
            // OfflineVmware volume migration
            // Cleanup volumes from target and reset the state of volume at source
            cleanUpVolumesForFailedMigrations(srcData, destData);
            String errMsg = "Can't find strategy to move data. " + "Source: " + srcData.getType().name() + " '" + srcData.getUuid() + ", Destination: " +
                    destData.getType().name() + " '" + destData.getUuid() + "'";
            invokeCallback(errMsg, callback);
            return;
        }

        strategy.copyAsync(srcData, destData, destHost, callback);
    }

    /**
     * Offline Vmware volume migration
     * Cleanup volumes after failed migrations and reset state of source volume
     *
     * @param srcData
     * @param destData
     */
    private void cleanUpVolumesForFailedMigrations(DataObject srcData, DataObject destData) {
        VolumeVO destinationVO = volDao.findById(destData.getId());
        VolumeVO sourceVO = volDao.findById(srcData.getId());
        sourceVO.setState(Volume.State.Ready);
        volDao.update(sourceVO.getId(), sourceVO);
        destinationVO.setState(Volume.State.Expunged);
        destinationVO.setRemoved(new Date());
        volDao.update(destinationVO.getId(), destinationVO);
    }

    @Override
    public void copyAsync(DataObject srcData, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
        copyAsync(srcData, destData, null, callback);
    }

    @Override
    public void copyAsync(Map<VolumeInfo, DataStore> volumeMap, VirtualMachineTO vmTo, Host srcHost, Host destHost, AsyncCompletionCallback<CopyCommandResult> callback) {

        DataMotionStrategy strategy = storageStrategyFactory.getDataMotionStrategy(volumeMap, srcHost, destHost);
        if (strategy == null) {
            List<String> volumeIds = new LinkedList<String>();
            for (final VolumeInfo volumeInfo : volumeMap.keySet()) {
                volumeIds.add(volumeInfo.getUuid());
            }

            String errMsg = "Can't find strategy to move data. " + "Source Host: " + srcHost.getName() + ", Destination Host: " + destHost.getName() +
                    ", Volume UUIDs: " + StringUtils.join(volumeIds, ",");
            invokeCallback(errMsg, callback);
            return;
        }

        strategy.copyAsync(volumeMap, vmTo, srcHost, destHost, callback);
    }

    private void invokeCallback(String errMsg, AsyncCompletionCallback<CopyCommandResult> callback) {
        CopyCmdAnswer copyCmdAnswer = new CopyCmdAnswer(errMsg);

        CopyCommandResult result = new CopyCommandResult(null, copyCmdAnswer);

        result.setResult(errMsg);

        callback.complete(result);
    }
}
