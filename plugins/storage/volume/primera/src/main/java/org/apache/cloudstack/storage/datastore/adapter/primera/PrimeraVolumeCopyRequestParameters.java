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

/**
 * https://support.hpe.com/hpesc/public/docDisplay?docId=a00118636en_us&page=v24885490.html
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrimeraVolumeCopyRequestParameters {
    private String destVolume = null;
    private String destCPG = null;
    private Boolean online = false;
    private String wwn = null;
    private Boolean tpvv = null;
    private Boolean reduce = null;
    private String snapCPG = null;
    private Boolean skipZero = null;
    private Boolean saveSnapshot = null;
    // 1=HIGH, 2=MED, 3=LOW
    private Integer priority = null;
    public String getDestVolume() {
        return destVolume;
    }
    public void setDestVolume(String destVolume) {
        this.destVolume = destVolume;
    }
    public String getDestCPG() {
        return destCPG;
    }
    public void setDestCPG(String destCPG) {
        this.destCPG = destCPG;
    }
    public Boolean getOnline() {
        return online;
    }
    public void setOnline(Boolean online) {
        this.online = online;
    }
    public String getWwn() {
        return wwn;
    }
    public void setWwn(String wwn) {
        this.wwn = wwn;
    }
    public Boolean getTpvv() {
        return tpvv;
    }
    public void setTpvv(Boolean tpvv) {
        this.tpvv = tpvv;
    }
    public Boolean getReduce() {
        return reduce;
    }
    public void setReduce(Boolean reduce) {
        this.reduce = reduce;
    }
    public String getSnapCPG() {
        return snapCPG;
    }
    public void setSnapCPG(String snapCPG) {
        this.snapCPG = snapCPG;
    }
    public Boolean getSkipZero() {
        return skipZero;
    }
    public void setSkipZero(Boolean skipZero) {
        this.skipZero = skipZero;
    }
    public Boolean getSaveSnapshot() {
        return saveSnapshot;
    }
    public void setSaveSnapshot(Boolean saveSnapshot) {
        this.saveSnapshot = saveSnapshot;
    }
    public Integer getPriority() {
        return priority;
    }
    public void setPriority(Integer priority) {
        this.priority = priority;
    }

}
