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

package org.apache.cloudstack.veeam.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Snapshot extends BaseDto {

    // epoch millis
    private Long date;
    private String persistMemorystate;
    private String snapshotStatus;
    private String snapshotType;
    private NamedList<Link> actions;
    private String description;
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Link> link;
    private Vm vm;

    public Snapshot() {
    }

    public Long getDate() {
        return date;
    }

    public void setDate(final Long date) {
        this.date = date;
    }

    public String getPersistMemorystate() {
        return persistMemorystate;
    }

    public void setPersistMemorystate(final String persistMemorystate) {
        this.persistMemorystate = persistMemorystate;
    }

    public String getSnapshotStatus() {
        return snapshotStatus;
    }

    public void setSnapshotStatus(final String snapshotStatus) {
        this.snapshotStatus = snapshotStatus;
    }

    public String getSnapshotType() {
        return snapshotType;
    }

    public void setSnapshotType(final String snapshotType) {
        this.snapshotType = snapshotType;
    }

    public NamedList<Link> getActions() {
        return actions;
    }

    public void setActions(final NamedList<Link> actions) {
        this.actions = actions;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public List<Link> getLink() {
        return link;
    }

    public void setLink(final List<Link> link) {
        this.link = link;
    }

    public Vm getVm() {
        return vm;
    }

    public void setVm(Vm vm) {
        this.vm = vm;
    }
}
