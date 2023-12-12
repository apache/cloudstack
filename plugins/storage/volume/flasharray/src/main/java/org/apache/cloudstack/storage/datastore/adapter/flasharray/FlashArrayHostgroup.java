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
public class FlashArrayHostgroup {
    @JsonProperty("name")
    private String name;
    @JsonProperty("connection_count")
    private Long connectionCount;
    @JsonProperty("host_count")
    private Long hostCount;
    @JsonProperty("is_local")
    private Boolean local;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Long getConnectionCount() {
        return connectionCount;
    }
    public void setConnectionCount(Long connectionCount) {
        this.connectionCount = connectionCount;
    }
    public Long getHostCount() {
        return hostCount;
    }
    public void setHostCount(Long hostCount) {
        this.hostCount = hostCount;
    }
    public Boolean getLocal() {
        return local;
    }
    public void setLocal(Boolean local) {
        this.local = local;
    }

}
