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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The storage space related properties of the LUN.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LunSpace {

    @JsonProperty("scsi_thin_provisioning_support_enabled")
    private Boolean scsiThinProvisioningSupportEnabled = null;

    @JsonProperty("size")
    private Long size = null;

    @JsonProperty("used")
    private Long used = null;
    @JsonProperty("physical_used")
    private Long physicalUsed = null;

    public LunSpace scsiThinProvisioningSupportEnabled(Boolean scsiThinProvisioningSupportEnabled) {
        this.scsiThinProvisioningSupportEnabled = scsiThinProvisioningSupportEnabled;
        return this;
    }

    public Boolean isScsiThinProvisioningSupportEnabled() {
        return scsiThinProvisioningSupportEnabled;
    }

    public void setScsiThinProvisioningSupportEnabled(Boolean scsiThinProvisioningSupportEnabled) {
        this.scsiThinProvisioningSupportEnabled = scsiThinProvisioningSupportEnabled;
    }

    public LunSpace size(Long size) {
        this.size = size;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getUsed() {
        return used;
    }

    public Long getPhysicalUsed() {
        return physicalUsed;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class LunSpace {\n");
        sb.append("    scsiThinProvisioningSupportEnabled: ").append(toIndentedString(scsiThinProvisioningSupportEnabled)).append("\n");
        sb.append("    size: ").append(toIndentedString(size)).append("\n");
        sb.append("    used: ").append(toIndentedString(used)).append("\n");
        sb.append("    physicalUsed: ").append(toIndentedString(physicalUsed)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
