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

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Policy {
    private int minThroughputIops;
    private int minThroughputMbps;
    private int maxThroughputIops;
    private int maxThroughputMbps;
    private String uuid;
    private String name;
    public int getMinThroughputIops() { return minThroughputIops; }
    public void setMinThroughputIops(int minThroughputIops) { this.minThroughputIops = minThroughputIops; }
    public int getMinThroughputMbps() { return minThroughputMbps; }
    public void setMinThroughputMbps(int minThroughputMbps) { this.minThroughputMbps = minThroughputMbps; }
    public int getMaxThroughputIops() { return maxThroughputIops; }
    public void setMaxThroughputIops(int maxThroughputIops) { this.maxThroughputIops = maxThroughputIops; }
    public int getMaxThroughputMbps() { return maxThroughputMbps; }
    public void setMaxThroughputMbps(int maxThroughputMbps) { this.maxThroughputMbps = maxThroughputMbps; }
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Policy policy = (Policy) o;
        return Objects.equals(getUuid(), policy.getUuid());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUuid());
    }
}
