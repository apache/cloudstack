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

import java.util.List;

/**
 * Model representing an ONTAP application consistency group.
 *
 * <p>Maps to the ONTAP REST API resource at
 * {@code /api/application/consistency-groups}.</p>
 *
 * @see <a href="https://docs.netapp.com/us-en/ontap-restapi/post-application-consistency-groups.html">
 *     ONTAP REST API - Create Consistency Group</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsistencyGroup {

    @JsonProperty("uuid")
    private String uuid;

    @JsonProperty("name")
    private String name;

    @JsonProperty("svm")
    private Svm svm;

    @JsonProperty("volumes")
    private List<ConsistencyGroupVolume> volumes;

    public ConsistencyGroup() {
    }

    public ConsistencyGroup(String name) {
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

    public Svm getSvm() {
        return svm;
    }

    public void setSvm(Svm svm) {
        this.svm = svm;
    }

    public List<ConsistencyGroupVolume> getVolumes() {
        return volumes;
    }

    public void setVolumes(List<ConsistencyGroupVolume> volumes) {
        this.volumes = volumes;
    }

    @Override
    public String toString() {
        return "ConsistencyGroup{" +
                "uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", volumes=" + (volumes != null ? volumes.size() : 0) +
                '}';
    }
}
