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

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VolumeQosPolicy {
    @JsonProperty("fixed")
    private Fixed fixed;
    @JsonProperty("name")
    private String name = null;
    @JsonProperty("uuid")
    private String uuid = null;
    @JsonProperty("svm")
    private Svm svm;
    @JsonProperty("object_count")
    private Integer objectCount;

    public Fixed getFixed() {
        return fixed;
    }

    public void setFixed(Fixed fixed) {
        this.fixed = fixed;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Svm getSvm() {
        return svm;
    }

    public void setSvm(Svm svm) {
        this.svm = svm;
    }

    public Integer getObjectCount() {
        return objectCount;
    }

    public void setObjectCount(Integer objectCount) {
        this.objectCount = objectCount;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Fixed {
        @JsonProperty("capacity_shared")
        private Boolean capacityShared;

        @JsonProperty("min_throughput_iops")
        private Long minThroughputIops;

        @JsonProperty("max_throughput_iops")
        private Long maxThroughputIops;

        public Boolean getCapacityShared() {
            return capacityShared;
        }

        public void setCapacityShared(Boolean capacityShared) {
            this.capacityShared = capacityShared;
        }

        public Long getMinThroughputIops() {
            return minThroughputIops;
        }

        public void setMinThroughputIops(Long minThroughputIops) {
            this.minThroughputIops = minThroughputIops;
        }

        public Long getMaxThroughputIops() {
            return maxThroughputIops;
        }

        public void setMaxThroughputIops(Long maxThroughputIops) {
            this.maxThroughputIops = maxThroughputIops;
        }
    }
}
