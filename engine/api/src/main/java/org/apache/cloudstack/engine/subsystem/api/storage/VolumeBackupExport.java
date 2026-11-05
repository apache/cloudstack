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

import java.util.Map;

/**
 * Where a storage driver made a volume accessible as a raw device for an image based backup
 * (read only, see {@link PrimaryDataStoreDriver#startBackupExport}) or a restore into it
 * (see {@link PrimaryDataStoreDriver#prepareVolumeForRestore}).
 */
public class VolumeBackupExport {
    private long volumeId;
    private long hostId;
    private String devicePath;
    private long sizeBytes;
    private boolean incremental;
    private String changeTrackingId;
    private Map<String, String> extentsSource;

    public VolumeBackupExport() {
    }

    public VolumeBackupExport(long volumeId, long hostId, String devicePath, long sizeBytes) {
        this.volumeId = volumeId;
        this.hostId = hostId;
        this.devicePath = devicePath;
        this.sizeBytes = sizeBytes;
    }

    public long getVolumeId() {
        return volumeId;
    }

    /**
     * The host the device is accessible on.
     */
    public long getHostId() {
        return hostId;
    }

    public String getDevicePath() {
        return devicePath;
    }

    /**
     * Size of the volume in bytes. The device can be larger, only this much of it is the volume's data.
     */
    public long getSizeBytes() {
        return sizeBytes;
    }

    /**
     * True if only the extents {@link #getExtentsSource()} lists as dirty changed since the backup with the
     * change tracking id given to {@link PrimaryDataStoreDriver#startBackupExport}, false if the whole volume
     * has to be read.
     */
    public boolean isIncremental() {
        return incremental;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    /**
     * The storage's change tracking id of this backup, to pass to the next {@link PrimaryDataStoreDriver#startBackupExport}
     * once this backup succeeded. Null if the storage does not track changes for the volume.
     */
    public String getChangeTrackingId() {
        return changeTrackingId;
    }

    public void setChangeTrackingId(String changeTrackingId) {
        this.changeTrackingId = changeTrackingId;
    }

    /**
     * Driver specific parameters the host's storage adaptor uses to list the extents of the export: the ones that
     * changed (of an incremental export) and the ones that read as zeros. Null if the storage lists none, then the
     * whole volume counts as changed data.
     */
    public Map<String, String> getExtentsSource() {
        return extentsSource;
    }

    public void setExtentsSource(Map<String, String> extentsSource) {
        this.extentsSource = extentsSource;
    }
}
