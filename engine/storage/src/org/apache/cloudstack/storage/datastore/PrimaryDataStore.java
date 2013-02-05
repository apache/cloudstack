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
package org.apache.cloudstack.storage.datastore;

import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.CommandResult;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.PrimaryDataStoreInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.DiskFormat;
import org.apache.cloudstack.framework.async.AsyncCompletionCallback;
import org.apache.cloudstack.storage.image.TemplateInfo;
import org.apache.cloudstack.storage.snapshot.SnapshotInfo;
import org.apache.cloudstack.storage.volume.PrimaryDataStoreDriver;
import org.apache.cloudstack.storage.volume.TemplateOnPrimaryDataStoreInfo;

public interface PrimaryDataStore extends DataStore, PrimaryDataStoreInfo {
    VolumeInfo getVolume(long id);

    List<VolumeInfo> getVolumes();

/*    void deleteVolumeAsync(VolumeInfo volume, AsyncCompletionCallback<CommandResult> callback);

    void createVolumeAsync(VolumeInfo vo, VolumeDiskType diskType, AsyncCompletionCallback<CommandResult> callback);

    void createVoluemFromBaseImageAsync(VolumeInfo volume, TemplateInfo templateStore, AsyncCompletionCallback<CommandResult> callback);
 */   

    boolean exists(DataObject data);

    TemplateInfo getTemplate(long templateId);
    
    SnapshotInfo getSnapshot(long snapshotId);


    DiskFormat getDefaultDiskType();

/*    void takeSnapshot(SnapshotInfo snapshot,
            AsyncCompletionCallback<CommandResult> callback);

    void revertSnapshot(SnapshotInfo snapshot,
            AsyncCompletionCallback<CommandResult> callback);

    void deleteSnapshot(SnapshotInfo snapshot,
            AsyncCompletionCallback<CommandResult> callback);*/
}
