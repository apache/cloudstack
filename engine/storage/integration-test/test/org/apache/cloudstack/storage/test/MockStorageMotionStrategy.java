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
package org.apache.cloudstack.storage.test;

import java.util.Map;
import java.util.UUID;

import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataMotionStrategy;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StrategyPriority;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CopyCmdAnswer;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;
import org.apache.cloudstack.storage.to.TemplateObjectTO;

import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.host.Host;
import com.cloud.storage.Storage;

public class MockStorageMotionStrategy implements DataMotionStrategy {

    boolean success = true;
    @Override
    public StrategyPriority canHandle(DataObject srcData, DataObject destData) {
        return StrategyPriority.HIGHEST;
    }

    public void makeBackupSnapshotSucceed(boolean success) {
        this.success = success;
    }

    @Override
    public StrategyPriority canHandle(Map<VolumeInfo, DataStore> volumeMap, Host srcHost, Host destHost) {
        return StrategyPriority.HIGHEST;
    }

    @Override
    public Void copyAsync(DataObject srcData, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
        CopyCmdAnswer answer = null;
        DataTO data = null;
        if (!success) {
            CopyCommandResult result = new CopyCommandResult(null, null);
            result.setResult("Failed");
            callback.complete(result);
        }
        if (destData.getType() == DataObjectType.SNAPSHOT) {
            SnapshotInfo srcSnapshot = (SnapshotInfo)srcData;

            SnapshotObjectTO newSnapshot = new SnapshotObjectTO();
            newSnapshot.setPath(UUID.randomUUID().toString());
            if (srcSnapshot.getParent() != null) {
                newSnapshot.setParentSnapshotPath(srcSnapshot.getParent().getPath());
            }
            data = newSnapshot;
        } else if (destData.getType() == DataObjectType.TEMPLATE) {
            TemplateObjectTO newTemplate = new TemplateObjectTO();
            newTemplate.setPath(UUID.randomUUID().toString());
            newTemplate.setFormat(Storage.ImageFormat.QCOW2);
            newTemplate.setSize(10L);
            newTemplate.setPhysicalSize(10L);
            data = newTemplate;
        }
        answer = new CopyCmdAnswer(data);
        CopyCommandResult result = new CopyCommandResult("something", answer);
        callback.complete(result);
        return null;
    }

    @Override
    public Void copyAsync(Map<VolumeInfo, DataStore> volumeMap, VirtualMachineTO vmTo, Host srcHost, Host destHost,
            AsyncCompletionCallback<CopyCommandResult> callback) {
        CopyCommandResult result = new CopyCommandResult("something", null);
        callback.complete(result);
        return null;
    }
}
