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
 * Request body for the ONTAP CLI-based Snapshot File Restore API.
 *
 * <p>ONTAP REST endpoint (CLI passthrough):
 * {@code POST /api/private/cli/volume/snapshot/restore-file}</p>
 *
 * <p>This API restores a single file or LUN from a FlexVolume snapshot to a
 * specified destination path using the CLI native implementation.
 * It works for both NFS files and iSCSI LUNs.</p>
 *
 * <p>Example payload:
 * <pre>
 * {
 *   "vserver": "vs0",
 *   "volume": "rajiv_ONTAP_SP1",
 *   "snapshot": "DATA-3-428726fe-7440-4b41-8d47-3f654e5d9814",
 *   "path": "/d266bb2c-d479-47ad-81c3-a070e8bb58c0"
 * }
 * </pre>
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CliSnapshotRestoreRequest {

    @JsonProperty("vserver")
    private String vserver;

    @JsonProperty("volume")
    private String volume;

    @JsonProperty("snapshot")
    private String snapshot;

    @JsonProperty("path")
    private String path;

    public CliSnapshotRestoreRequest() {
    }

    /**
     * Creates a CLI snapshot restore request.
     *
     * @param vserver  The SVM (vserver) name
     * @param volume   The FlexVolume name
     * @param snapshot The snapshot name
     * @param path     The file/LUN path to restore (e.g., "/uuid.qcow2" or "/lun_name")
     */
    public CliSnapshotRestoreRequest(String vserver, String volume, String snapshot, String path) {
        this.vserver = vserver;
        this.volume = volume;
        this.snapshot = snapshot;
        this.path = path;
    }

    public String getVserver() {
        return vserver;
    }

    public void setVserver(String vserver) {
        this.vserver = vserver;
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public String getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(String snapshot) {
        this.snapshot = snapshot;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "CliSnapshotRestoreRequest{" +
                "vserver='" + vserver + '\'' +
                ", volume='" + volume + '\'' +
                ", snapshot='" + snapshot + '\'' +
                ", path='" + path + '\'' +
                '}';
    }
}
