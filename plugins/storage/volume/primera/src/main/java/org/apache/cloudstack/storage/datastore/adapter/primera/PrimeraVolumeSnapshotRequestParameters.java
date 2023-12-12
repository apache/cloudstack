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
 * https://support.hpe.com/hpesc/public/docDisplay?docId=a00118636en_us&page=s_creating_snapshot_volumes.html
 */
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrimeraVolumeSnapshotRequestParameters {
    private String name = null;
    private String id = null;
    private String comment = null;
    private Boolean readOnly = false;
    private Integer expirationHours = null;
    private Integer retentionHours = null;
    private String addToSet = null;
    private Boolean syncSnapRcopy = false;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getComment() {
        return comment;
    }
    public void setComment(String comment) {
        this.comment = comment;
    }
    public Boolean getReadOnly() {
        return readOnly;
    }
    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }
    public Integer getExpirationHours() {
        return expirationHours;
    }
    public void setExpirationHours(Integer expirationHours) {
        this.expirationHours = expirationHours;
    }
    public Integer getRetentionHours() {
        return retentionHours;
    }
    public void setRetentionHours(Integer retentionHours) {
        this.retentionHours = retentionHours;
    }
    public String getAddToSet() {
        return addToSet;
    }
    public void setAddToSet(String addToSet) {
        this.addToSet = addToSet;
    }
    public Boolean getSyncSnapRcopy() {
        return syncSnapRcopy;
    }
    public void setSyncSnapRcopy(Boolean syncSnapRcopy) {
        this.syncSnapRcopy = syncSnapRcopy;
    }

}
