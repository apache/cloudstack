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
public class PrimeraVolumeRequest {
    private String name;
    private String cpg;
    private long sizeMiB;
    private String comment;
    private String snapCPG = null;
    private Boolean reduce;
    private Boolean tpvv;
    private Integer ssSpcAllocLimitPct;
    private Integer ssSpcAllocWarningPct;
    private Integer usrSpcAllocWarningPct;
    private Integer usrSpcAllocLimitPct;
    private PrimeraVolumePolicies policies;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getCpg() {
        return cpg;
    }
    public void setCpg(String cpg) {
        this.cpg = cpg;
    }
    public long getSizeMiB() {
        return sizeMiB;
    }
    public void setSizeMiB(long sizeMiB) {
        this.sizeMiB = sizeMiB;
    }
    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    public String getSnapCPG() {
        return snapCPG;
    }
    public void setSnapCPG(String snapCPG) {
        this.snapCPG = snapCPG;
    }
    public Boolean getReduce() {
        return reduce;
    }
    public void setReduce(Boolean reduce) {
        this.reduce = reduce;
    }
    public Boolean getTpvv() {
        return tpvv;
    }
    public void setTpvv(Boolean tpvv) {
        this.tpvv = tpvv;
    }
    public Integer getSsSpcAllocLimitPct() {
        return ssSpcAllocLimitPct;
    }
    public void setSsSpcAllocLimitPct(Integer ssSpcAllocLimitPct) {
        this.ssSpcAllocLimitPct = ssSpcAllocLimitPct;
    }
    public Integer getSsSpcAllocWarningPct() {
        return ssSpcAllocWarningPct;
    }
    public void setSsSpcAllocWarningPct(Integer ssSpcAllocWarningPct) {
        this.ssSpcAllocWarningPct = ssSpcAllocWarningPct;
    }
    public Integer getUsrSpcAllocWarningPct() {
        return usrSpcAllocWarningPct;
    }
    public void setUsrSpcAllocWarningPct(Integer usrSpcAllocWarningPct) {
        this.usrSpcAllocWarningPct = usrSpcAllocWarningPct;
    }
    public Integer getUsrSpcAllocLimitPct() {
        return usrSpcAllocLimitPct;
    }
    public void setUsrSpcAllocLimitPct(Integer usrSpcAllocLimitPct) {
        this.usrSpcAllocLimitPct = usrSpcAllocLimitPct;
    }
    public PrimeraVolumePolicies getPolicies() {
        return policies;
    }
    public void setPolicies(PrimeraVolumePolicies policies) {
        this.policies = policies;
    }

}
