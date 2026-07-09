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
 * Model representing an ONTAP consistency group snapshot.
 *
 * <p>Maps to the ONTAP REST API resource at
 * {@code /api/application/consistency-groups/{consistency_group.uuid}/snapshots}.</p>
 *
 * @see <a href="https://docs.netapp.com/us-en/ontap-restapi/get-application-consistency-groups-snapshots.html">
 *     ONTAP REST API - Consistency Group Snapshots</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsistencyGroupSnapshot {

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("name")
    private String name;

    @JsonProperty("create_time")
    private String createTime;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("consistency_type")
    private String consistencyType;

    @JsonProperty("snapmirror_label")
    private String snapmirrorLabel;

    @JsonProperty("action")
    private String action;

    @JsonProperty("consistency_group")
    private VolumeConcise consistencyGroup;

    public ConsistencyGroupSnapshot() {
        // default constructor for Jackson
    }

    public ConsistencyGroupSnapshot(String name) {
        this.name = name;
    }

    public ConsistencyGroupSnapshot(String name, String action) {
        this.name = name;
        this.action = action;
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

    public String getConsistencyType() {
        return consistencyType;
    }

    public void setConsistencyType(String consistencyType) {
        this.consistencyType = consistencyType;
    }

    public String getSnapmirrorLabel() {
        return snapmirrorLabel;
    }

    public void setSnapmirrorLabel(String snapmirrorLabel) {
        this.snapmirrorLabel = snapmirrorLabel;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public VolumeConcise getConsistencyGroup() {
        return consistencyGroup;
    }

    public void setConsistencyGroup(VolumeConcise consistencyGroup) {
        this.consistencyGroup = consistencyGroup;
    }

    @Override
    public String toString() {
        return "ConsistencyGroupSnapshot{" +
                "uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", createTime='" + createTime + '\'' +
                ", comment='" + comment + '\'' +
                ", consistencyType='" + consistencyType + '\'' +
                '}';
    }
}
