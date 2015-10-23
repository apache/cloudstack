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
package org.apache.cloudstack.engine.subsystem.api.storage;

import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.command.CommandResult;

import com.cloud.host.Host;
import com.cloud.storage.StoragePool;
import com.cloud.storage.Volume;

public interface PrimaryDataStoreDriver extends DataStoreDriver {
    public ChapInfo getChapInfo(VolumeInfo volumeInfo);

    public boolean grantAccess(DataObject dataObject, Host host, DataStore dataStore);

    public void revokeAccess(DataObject dataObject, Host host, DataStore dataStore);

    // intended for managed storage (cloud.storage_pool.managed = true)
    // if not managed, return volume.getSize()
    public long getVolumeSizeIncludingHypervisorSnapshotReserve(Volume volume, StoragePool storagePool);

    // intended for managed storage (cloud.storage_pool.managed = true)
    // if managed storage, return the total number of bytes currently in use for the storage pool in question
    // if not managed storage, return 0
    public long getUsedBytes(StoragePool storagePool);

    // intended for managed storage (cloud.storage_pool.managed = true)
    // if managed storage, return the total number of IOPS currently in use for the storage pool in question
    // if not managed storage, return 0
    public long getUsedIops(StoragePool storagePool);

    public void takeSnapshot(SnapshotInfo snapshot, AsyncCompletionCallback<CreateCmdResult> callback);

    public void revertSnapshot(SnapshotInfo snapshotOnImageStore, SnapshotInfo snapshotOnPrimaryStore, AsyncCompletionCallback<CommandResult> callback);
}
