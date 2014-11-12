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

import org.apache.cloudstack.engine.subsystem.api.storage.ChapInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.CopyCommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.CreateCmdResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreDriver;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;
import org.apache.cloudstack.storage.command.CreateObjectAnswer;
import org.apache.cloudstack.storage.to.SnapshotObjectTO;

import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.host.Host;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;

public class FakePrimaryDataStoreDriver implements PrimaryDataStoreDriver {
    boolean snapshotResult = true;

    @Override
    public ChapInfo getChapInfo(VolumeInfo volumeInfo) {
        return null; // To change body of implemented methods, use File | Settings | File Templates.
    }

    @Override
    public boolean grantAccess(DataObject dataObject, Host host, DataStore dataStore) { return false; }

    @Override
    public void revokeAccess(DataObject dataObject, Host host, DataStore dataStore) {}

    @Override
    public long getUsedBytes(StoragePool storagePool) {
        return 0;
    }

    @Override
    public long getUsedIops(StoragePool storagePool) {
        return 0;
    }

    @Override
    public long getVolumeSizeIncludingHypervisorSnapshotReserve(Volume volume, StoragePool pool) {
        return volume.getSize();
    }

    @Override
    public void takeSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CreateCmdResult> callback) {
        CreateCmdResult result = new CreateCmdResult(null, null);
        if (snapshotResult) {
            SnapshotObjectTO newSnap = new SnapshotObjectTO();
            newSnap.setPath(UUID.randomUUID().toString());

            CreateObjectAnswer answer = new CreateObjectAnswer(newSnap);
            result.setAnswer(answer);
        } else {
            result.setResult("Failed to create snapshot");
        }
        callback.complete(result);
        return;
    }

    public void makeTakeSnapshotSucceed(boolean success) {
        snapshotResult = success;
    }

    @Override
    public void revertSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CommandResult> callback) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public DataTO getTO(DataObject data) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public DataStoreTO getStoreTO(DataStore store) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void createAsync(DataStore store, DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void deleteAsync(DataStore store, DataObject data, AsyncCompletionCallback<CommandResult> callback) {
        CommandResult result = new CommandResult();
        result.setSuccess(true);
        callback.complete(result);
        return;
    }

    @Override
    public void copyAsync(DataObject srcdata, DataObject destData, AsyncCompletionCallback<CopyCommandResult> callback) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean canCopy(DataObject srcData, DataObject destData) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void resize(DataObject data, AsyncCompletionCallback<CreateCmdResult> callback) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, String> getCapabilities() {
        // TODO Auto-generated method stub
        return null;
    }
}
