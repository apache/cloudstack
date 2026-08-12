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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Svm {
    @JsonProperty("uuid")
    private String uuid = null;

    @JsonProperty("name")
    private String name = null;

    @JsonIgnore
    private Boolean iscsiEnabled = null;

    @JsonIgnore
    private Boolean fcpEnabled = null;

    @JsonIgnore
    private Boolean nfsEnabled = null;

    @JsonProperty("aggregates")
    private List<Aggregate> aggregates = null;

    @JsonProperty("aggregates_delegated")
    private Boolean aggregatesDelegated = null;

    @JsonProperty("state")
    private String state = null;

    @JsonIgnore
    private Links links = null;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getNfsEnabled() {
        return Boolean.TRUE.equals(nfsEnabled);
    }

    public void setNfsEnabled(Boolean nfsEnabled) {
        this.nfsEnabled = nfsEnabled;
    }

    @JsonSetter("nfs")
    public void setNfs(Map<String, Object> nfs) {
        this.nfsEnabled = nfs != null ? Boolean.TRUE.equals(nfs.get("enabled")) : false;
    }

    public Boolean getIscsiEnabled() {
        return Boolean.TRUE.equals(iscsiEnabled);
    }

    public void setIscsiEnabled(Boolean iscsiEnabled) {
        this.iscsiEnabled = iscsiEnabled;
    }

    @JsonSetter("iscsi")
    public void setIscsi(Map<String, Object> iscsi) {
        this.iscsiEnabled = iscsi != null ? Boolean.TRUE.equals(iscsi.get("enabled")) : false;
    }

    public Boolean getFcpEnabled() {
        return Boolean.TRUE.equals(fcpEnabled);
    }

    public void setFcpEnabled(Boolean fcpEnabled) {
        this.fcpEnabled = fcpEnabled;
    }

    @JsonSetter("fcp")
    public void setFcp(Map<String, Object> fcp) {
        this.fcpEnabled = fcp != null ? Boolean.TRUE.equals(fcp.get("enabled")) : false;
    }

    public List<Aggregate> getAggregates() {
        return aggregates;
    }

    public void setAggregates(List<Aggregate> aggregates) {
        this.aggregates = aggregates;
    }

    public Boolean getAggregatesDelegated() {
        return aggregatesDelegated;
    }

    public void setAggregatesDelegated(Boolean aggregatesDelegated) {
        this.aggregatesDelegated = aggregatesDelegated;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Links getLinks() {
        return links;
    }

    public void setLinks(Links links) {
        this.links = links;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Svm svm = (Svm) o;
        return Objects.equals(getUuid(), svm.getUuid());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUuid());
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Links { }
}
