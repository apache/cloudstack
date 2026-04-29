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
public class FlashArrayVolumeSpace {
    @JsonProperty("data_reduction")
    private Float dataReduction;
    @JsonProperty("snapshots")
    private Integer snapshots;
    @JsonProperty("snapshots_effective")
    private Integer snapshotsEffective;
    @JsonProperty("thin_provisioning")
    private Float thinProvisioning;
    @JsonProperty("total_effective")
    private Long totalEffective;
    @JsonProperty("total_physical")
    private Long totalPhysical;
    @JsonProperty("total_provisioned")
    private Long totalProvisioned;
    @JsonProperty("total_reduction")
    private Float totalReduction;
    @JsonProperty("unique")
    private Long unique;
    @JsonProperty("unique_effective")
    private Long uniqueEffective;
    @JsonProperty("user_provisioned")
    private Long usedProvisioned;
    @JsonProperty("virtual")
    private Long virtual;
    public Float getData_reduction() {
        return dataReduction;
    }
    public void setData_reduction(Float dataReduction) {
        this.dataReduction = dataReduction;
    }
    public Integer getSnapshots() {
        return snapshots;
    }
    public void setSnapshots(Integer snapshots) {
        this.snapshots = snapshots;
    }
    public Integer getSnapshotsEffective() {
        return snapshotsEffective;
    }
    public void setSnapshotsEffective(Integer snapshotsEffective) {
        this.snapshotsEffective = snapshotsEffective;
    }
    public Float getThinProvisioning() {
        return thinProvisioning;
    }
    public void setThinProvisioning(Float thinProvisioning) {
        this.thinProvisioning = thinProvisioning;
    }
    public Long getTotalEffective() {
        return totalEffective;
    }
    public void setTotalEffective(Long totalEffective) {
        this.totalEffective = totalEffective;
    }
    public Long getTotalPhysical() {
        return totalPhysical;
    }
    public void setTotal_physical(Long totalPhysical) {
        this.totalPhysical = totalPhysical;
    }
    public Long getTotalProvisioned() {
        return totalProvisioned;
    }
    public void setTotalProvisioned(Long totalProvisioned) {
        this.totalProvisioned = totalProvisioned;
    }
    public Float getTotalReduction() {
        return totalReduction;
    }
    public void setTotalReduction(Float totalReduction) {
        this.totalReduction = totalReduction;
    }
    public Long getUnique() {
        return unique;
    }
    public void setUnique(Long unique) {
        this.unique = unique;
    }
    public Long getUniqueEffective() {
        return uniqueEffective;
    }
    public void setUniqueEffective(Long uniqueEffective) {
        this.uniqueEffective = uniqueEffective;
    }
    public Long getUsedProvisioned() {
        return usedProvisioned;
    }
    public void setUsed_provisioned(Long usedProvisioned) {
        this.usedProvisioned = usedProvisioned;
    }
    public Long getVirtual() {
        return virtual;
    }
    public void setVirtual(Long virtual) {
        this.virtual = virtual;
    }

}
