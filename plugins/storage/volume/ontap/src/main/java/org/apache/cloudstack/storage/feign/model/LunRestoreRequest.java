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
package org.apache.cloudstack.storage.feign.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request body for the ONTAP LUN Restore API.
 *
 * <p>ONTAP REST endpoint:
 * {@code POST /api/storage/luns/{lun.uuid}/restore}</p>
 *
 * <p>This API restores a LUN from a FlexVolume snapshot to a specified
 * destination path. Unlike file restore, this is LUN-specific.</p>
 *
 * <p>Example payload:
 * <pre>
 * {
 *   "snapshot": {
 *     "name": "snapshot_name"
 *   },
 *   "destination": {
 *     "path": "/vol/volume_name/lun_name"
 *   }
 * }
 * </pre>
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LunRestoreRequest {

    @JsonProperty("snapshot")
    private SnapshotRef snapshot;

    @JsonProperty("destination")
    private Destination destination;

    public LunRestoreRequest() {
    }

    public LunRestoreRequest(String snapshotName, String destinationPath) {
        this.snapshot = new SnapshotRef(snapshotName);
        this.destination = new Destination(destinationPath);
    }

    public SnapshotRef getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(SnapshotRef snapshot) {
        this.snapshot = snapshot;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    /**
     * Nested class for snapshot reference.
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

    /**
     * Nested class for destination path.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Destination {

        @JsonProperty("path")
        private String path;

        public Destination() {
        }

        public Destination(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}
