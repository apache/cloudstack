// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.storage.datastore.adapter.flasharray;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlashArrayPod {
    @JsonProperty("name")
    private String name;
    @JsonProperty("id")
    private String id;
    @JsonProperty("destroyed")
    private Boolean destroyed;
    @JsonProperty("footprint")
    private Long footprint;
    @JsonProperty("quota_limit")
    private Long quotaLimit;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public Boolean getDestroyed() {
        return destroyed;
    }
    public void setDestroyed(Boolean destroyed) {
        this.destroyed = destroyed;
    }
    public Long getFootprint() {
        return footprint;
    }
    public void setFootprint(Long footprint) {
        this.footprint = footprint;
    }
    public Long getQuotaLimit() {
        return quotaLimit;
    }
    public void setQuotaLimit(Long quotaLimit) {
        this.quotaLimit = quotaLimit;
    }

}
