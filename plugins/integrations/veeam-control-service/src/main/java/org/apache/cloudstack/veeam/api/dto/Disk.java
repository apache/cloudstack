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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "disk")
public final class Disk {

    private String bootable;

    @JsonProperty("actual_size")
    public String actualSize;

    public String alias;
    public String backup;

    @JsonProperty("content_type")
    public String contentType;

    public String format;

    @JsonProperty("image_id")
    public String imageId;

    @JsonProperty("propagate_errors")
    public String propagateErrors;

    @JsonProperty("initial_size")
    public String initialSize;

    @JsonProperty("provisioned_size")
    public String provisionedSize;

    @JsonProperty("qcow_version")
    public String qcowVersion;

    public String shareable;
    public String sparse;
    public String status;

    @JsonProperty("storage_type")
    public String storageType;

    @JsonProperty("total_size")
    public String totalSize;

    @JsonProperty("wipe_after_delete")
    public String wipeAfterDelete;

    @JsonProperty("disk_profile")
    public Ref diskProfile;

    public Ref quota;

    @JsonProperty("storage_domains")
    public StorageDomains storageDomains;

    public Actions actions;

    public String name;
    public String description;

    @JacksonXmlElementWrapper(useWrapping = false)
    public List<Link> link;

    public String href;
    public String id;

    public Disk() {}

    public String getBootable() {
        return bootable;
    }

    public void setBootable(String bootable) {
        this.bootable = bootable;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JacksonXmlRootElement(localName = "storage_domains")
    public static final class StorageDomains {
        @JsonProperty("storage_domain")
        @JacksonXmlElementWrapper(useWrapping = false)
        public List<Ref> storageDomain;
        public StorageDomains() {}
    }
}
