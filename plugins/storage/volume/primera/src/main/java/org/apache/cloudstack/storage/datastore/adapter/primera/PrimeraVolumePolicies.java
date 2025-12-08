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
public class PrimeraVolumePolicies {
    private Boolean tpZeroFill;
    private Boolean staleSS;
    private Boolean oneHost;
    private Boolean zeroDetect;
    private Boolean system;
    private Boolean caching;
    private Boolean fsvc;
    private Integer hostDIF;
    public Boolean getTpZeroFill() {
        return tpZeroFill;
    }
    public void setTpZeroFill(Boolean tpZeroFill) {
        this.tpZeroFill = tpZeroFill;
    }
    public Boolean getStaleSS() {
        return staleSS;
    }
    public void setStaleSS(Boolean staleSS) {
        this.staleSS = staleSS;
    }
    public Boolean getOneHost() {
        return oneHost;
    }
    public void setOneHost(Boolean oneHost) {
        this.oneHost = oneHost;
    }
    public Boolean getZeroDetect() {
        return zeroDetect;
    }
    public void setZeroDetect(Boolean zeroDetect) {
        this.zeroDetect = zeroDetect;
    }
    public Boolean getSystem() {
        return system;
    }
    public void setSystem(Boolean system) {
        this.system = system;
    }
    public Boolean getCaching() {
        return caching;
    }
    public void setCaching(Boolean caching) {
        this.caching = caching;
    }
    public Boolean getFsvc() {
        return fsvc;
    }
    public void setFsvc(Boolean fsvc) {
        this.fsvc = fsvc;
    }
    public Integer getHostDIF() {
        return hostDIF;
    }
    public void setHostDIF(Integer hostDIF) {
        this.hostDIF = hostDIF;
    }

}
