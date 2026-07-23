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
 * Request body for the ONTAP Snapshot File Restore API.
 *
 * <p>ONTAP REST endpoint:
 * {@code POST /api/storage/volumes/{volume.uuid}/snapshots/{snapshot.uuid}/files/{file.path}/restore}</p>
 *
 * <p>This API restores a single file or LUN from a FlexVolume snapshot to a
 * specified destination path, without reverting the entire FlexVolume.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SnapshotFileRestoreRequest {

    @JsonProperty("destination_path")
    private String destinationPath;

    public SnapshotFileRestoreRequest() {
    }

    public SnapshotFileRestoreRequest(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }
}
