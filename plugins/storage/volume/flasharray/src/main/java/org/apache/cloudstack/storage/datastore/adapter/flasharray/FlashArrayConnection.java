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
public class FlashArrayConnection {
    @JsonProperty("host_group")
    private FlashArrayConnectionHostgroup hostGroup;
    @JsonProperty("host")
    private FlashArrayConnectionHost host;
    @JsonProperty("volume")
    private FlashArrayVolume volume;
    @JsonProperty("lun")
    private Integer lun;

    public FlashArrayConnectionHostgroup getHostGroup() {
        return hostGroup;
    }

    public void setHostGroup(FlashArrayConnectionHostgroup hostGroup) {
        this.hostGroup = hostGroup;
    }

    public FlashArrayConnectionHost getHost() {
        return host;
    }

    public void setHost(FlashArrayConnectionHost host) {
        this.host = host;
    }

    public FlashArrayVolume getVolume() {
        return volume;
    }

    public void setVolume(FlashArrayVolume volume) {
        this.volume = volume;
    }

    public Integer getLun() {
        return lun;
    }

    public void setLun(Integer lun) {
        this.lun = lun;
    }


}
