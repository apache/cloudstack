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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.feign.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for the ONTAP file clone API.
 *
 * <p>ONTAP REST endpoint: {@code POST /api/storage/file/clone}</p>
 *
 * <p>Creates a space-efficient copy of a file. Source and destination paths are relative to the
 * root of {@code volume}, and both must live in that same FlexVolume.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileCloneRequest {

    @JsonProperty("volume")
    private VolumeRef volume;

    @JsonProperty("source_path")
    private String sourcePath;

    @JsonProperty("destination_path")
    private String destinationPath;

    @JsonProperty("overwrite_destination")
    private Boolean overwriteDestination;

    /**
     * Optional FlexVolume snapshot to clone from. When set, ONTAP clones {@code source_path}
     * as it existed in that snapshot rather than from the live file/LUN.
     *
     * <p>Used by create-volume-from-snapshot (same FlexVol). Omitted for live template-cache clones.</p>
     */
    @JsonProperty("snapshot")
    private SnapshotRef snapshot;

    public FileCloneRequest() {
    }

    public FileCloneRequest(String flexVolUuid, String flexVolName, String sourcePath, String destinationPath) {
        this.volume = new VolumeRef(flexVolUuid, flexVolName);
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
    }

    public FileCloneRequest(String flexVolUuid, String flexVolName, String sourcePath, String destinationPath,
                            String snapshotName) {
        this(flexVolUuid, flexVolName, sourcePath, destinationPath);
        if (snapshotName != null && !snapshotName.isEmpty()) {
            this.snapshot = new SnapshotRef(snapshotName);
        }
    }

    public VolumeRef getVolume() {
        return volume;
    }

    public void setVolume(VolumeRef volume) {
        this.volume = volume;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public Boolean getOverwriteDestination() {
        return overwriteDestination;
    }

    public void setOverwriteDestination(Boolean overwriteDestination) {
        this.overwriteDestination = overwriteDestination;
    }

    public SnapshotRef getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(SnapshotRef snapshot) {
        this.snapshot = snapshot;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class VolumeRef {

        @JsonProperty("uuid")
        private String uuid;

        @JsonProperty("name")
        private String name;

        public VolumeRef() {
        }

        public VolumeRef(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * Snapshot identity for {@code POST /api/storage/file/clone} when cloning from a FlexVol snapshot.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SnapshotRef {

        @JsonProperty("name")
        private String name;

        public SnapshotRef() {
        }

        public SnapshotRef(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Override
    public String toString() {
        return "FileCloneRequest{volume=" + (volume != null ? volume.getUuid() : null)
                + ", sourcePath=" + sourcePath
                + ", destinationPath=" + destinationPath
                + ", snapshot=" + (snapshot != null ? snapshot.getName() : null) + "}";
    }
}
