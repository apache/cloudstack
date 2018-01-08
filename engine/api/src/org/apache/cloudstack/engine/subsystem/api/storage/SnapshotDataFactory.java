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

import java.util.List;

import com.cloud.storage.DataStoreRole;

public interface SnapshotDataFactory {
    SnapshotInfo getSnapshot(long snapshotId, DataStore store);

    SnapshotInfo getSnapshot(DataObject obj, DataStore store);

    SnapshotInfo getSnapshot(long snapshotId, DataStoreRole role);

    List<SnapshotInfo> getSnapshots(long volumeId, DataStoreRole store);

    List<SnapshotInfo> listSnapshotOnCache(long snapshotId);

    SnapshotInfo getReadySnapshotOnCache(long snapshotId);
}
