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
package org.apache.cloudstack.storage.datastore.adapter.primera;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrimeraVolumePromoteRequest {
    private Integer action = 4; // PROMOTE_VIRTUAL_COPY, https://support.hpe.com/hpesc/public/docDisplay?docId=a00114827en_us&page=v25706371.html
    private Boolean online = true;
    private Integer priority = 2; // MEDIUM
    private Boolean allowRemoteCopyParent = true;
    public Integer getAction() {
        return action;
    }
    public void setAction(Integer action) {
        this.action = action;
    }
    public Boolean getOnline() {
        return online;
    }
    public void setOnline(Boolean online) {
        this.online = online;
    }
    public Integer getPriority() {
        return priority;
    }
    public void setPriority(Integer priority) {
        this.priority = priority;
    }
    public Boolean getAllowRemoteCopyParent() {
        return allowRemoteCopyParent;
    }
    public void setAllowRemoteCopyParent(Boolean allowRemoteCopyParent) {
        this.allowRemoteCopyParent = allowRemoteCopyParent;
    }

}
