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
 * Model representing an ONTAP FlexVolume-level snapshot.
 *
 * <p>Maps to the ONTAP REST API resource at
 * {@code /api/storage/volumes/{volume.uuid}/snapshots}.</p>
 *
 * <p>For creation, only the {@code name} field is required in the POST body.
 * ONTAP returns the full representation including {@code uuid}, {@code name},
 * and {@code create_time} on GET requests.</p>
 *
 * @see <a href="https://docs.netapp.com/us-en/ontap-restapi/ontap/storage_volumes_volume.uuid_snapshots_endpoint_overview.html">
 *     ONTAP REST API - Volume Snapshots</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlexVolSnapshot {

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("name")
    private String name;

    @JsonProperty("create_time")
    private String createTime;

    @JsonProperty("comment")
    private String comment;

    /** Concise reference to the parent volume (returned in GET responses). */
    @JsonProperty("volume")
    private VolumeConcise volume;

    public FlexVolSnapshot() {
        // default constructor for Jackson
    }

    public FlexVolSnapshot(String name) {
        this.name = name;
    }

    public FlexVolSnapshot(String name, String comment) {
        this.name = name;
        this.comment = comment;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

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

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public VolumeConcise getVolume() {
        return volume;
    }

    public void setVolume(VolumeConcise volume) {
        this.volume = volume;
    }

    @Override
    public String toString() {
        return "FlexVolSnapshot{" +
                "uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", createTime='" + createTime + '\'' +
                ", comment='" + comment + '\'' +
                '}';
    }
}
