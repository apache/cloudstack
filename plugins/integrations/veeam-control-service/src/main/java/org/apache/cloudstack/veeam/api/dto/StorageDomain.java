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
public final class StorageDomain extends BaseDto {

    private String name;
    private String description;
    private String comment;
    private String available;
    private String used;
    private String committed;
    private String blockSize;
    private String warningLowSpaceIndicator;
    private String criticalSpaceActionBlocker;
    private String status;          // e.g. "unattached" (optional in your first object)
    private String type;            // data / image / iso / export
    private String master;          // "true"/"false"
    private String backup;          // "true"/"false"
    private String externalStatus;  // "ok"
    private String storageFormat;   // v5 / v1
    private String discardAfterDelete;
    private String wipeAfterDelete;
    private String supportsDiscard;
    private String supportsDiscardZeroesData;
    private Storage storage;
    private NamedList<DataCenter> dataCenters;
    private NamedList<Link> actions;
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Link> link;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAvailable() {
        return available;
    }

    public void setAvailable(String available) {
        this.available = available;
    }

    public String getUsed() {
        return used;
    }

    public void setUsed(String used) {
        this.used = used;
    }

    public String getCommitted() {
        return committed;
    }

    public void setCommitted(String committed) {
        this.committed = committed;
    }

    public String getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(String blockSize) {
        this.blockSize = blockSize;
    }

    public String getWarningLowSpaceIndicator() {
        return warningLowSpaceIndicator;
    }

    public void setWarningLowSpaceIndicator(String warningLowSpaceIndicator) {
        this.warningLowSpaceIndicator = warningLowSpaceIndicator;
    }

    public String getCriticalSpaceActionBlocker() {
        return criticalSpaceActionBlocker;
    }

    public void setCriticalSpaceActionBlocker(String criticalSpaceActionBlocker) {
        this.criticalSpaceActionBlocker = criticalSpaceActionBlocker;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMaster() {
        return master;
    }

    public void setMaster(String master) {
        this.master = master;
    }

    public String getBackup() {
        return backup;
    }

    public void setBackup(String backup) {
        this.backup = backup;
    }

    public String getExternalStatus() {
        return externalStatus;
    }

    public void setExternalStatus(String externalStatus) {
        this.externalStatus = externalStatus;
    }

    public String getStorageFormat() {
        return storageFormat;
    }

    public void setStorageFormat(String storageFormat) {
        this.storageFormat = storageFormat;
    }

    public String getDiscardAfterDelete() {
        return discardAfterDelete;
    }

    public void setDiscardAfterDelete(String discardAfterDelete) {
        this.discardAfterDelete = discardAfterDelete;
    }

    public String getWipeAfterDelete() {
        return wipeAfterDelete;
    }

    public void setWipeAfterDelete(String wipeAfterDelete) {
        this.wipeAfterDelete = wipeAfterDelete;
    }

    public String getSupportsDiscard() {
        return supportsDiscard;
    }

    public void setSupportsDiscard(String supportsDiscard) {
        this.supportsDiscard = supportsDiscard;
    }

    public String getSupportsDiscardZeroesData() {
        return supportsDiscardZeroesData;
    }

    public void setSupportsDiscardZeroesData(String supportsDiscardZeroesData) {
        this.supportsDiscardZeroesData = supportsDiscardZeroesData;
    }

    public Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public NamedList<DataCenter> getDataCenters() {
        return dataCenters;
    }

    public void setDataCenters(NamedList<DataCenter> dataCenters) {
        this.dataCenters = dataCenters;
    }

    public NamedList<Link> getActions() {
        return actions;
    }

    public void setActions(NamedList<Link> actions) {
        this.actions = actions;
    }

    public List<Link> getLink() {
        return link;
    }

    public void setLink(List<Link> link) {
        this.link = link;
    }
}
