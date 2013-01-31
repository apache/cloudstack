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
package org.apache.cloudstack.storage.snapshot;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObjectType;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.ObjectInDataStoreStateMachine.Event;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.disktype.DiskFormat;
import org.apache.cloudstack.storage.snapshot.db.SnapshotVO;

public class SnapshotObject implements SnapshotInfo {
    private SnapshotVO snapshot;
    private DataStore store;

    public SnapshotObject(SnapshotVO snapshot, DataStore store) {
        this.snapshot = snapshot;
        this.store = store;
    }

    public DataStore getStore() {
        return this.store;
    }

    @Override
    public SnapshotInfo getParent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public SnapshotInfo getChild() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VolumeInfo getBaseVolume() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getId() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getUri() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DataStore getDataStore() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Long getSize() {
        // TODO Auto-generated method stub
        return 0L;
    }

    @Override
    public DataObjectType getType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DiskFormat getFormat() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getUuid() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void processEvent(Event event) {
        // TODO Auto-generated method stub
        
    }

}
