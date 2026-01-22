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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "data_center")
public final class DataCenter {

    // keep strings to match oVirt JSON ("false", "disabled", "up", "v5", etc.)
    public String local;

    @JsonProperty("quota_mode")
    public String quotaMode;

    public String status;

    @JsonProperty("storage_format")
    public String storageFormat;

    @JsonProperty("supported_versions")
    public SupportedVersions supportedVersions;

    public Version version;

    @JsonProperty("mac_pool")
    public Ref macPool;

    public Actions actions;

    public String name;
    public String description;

    @JacksonXmlElementWrapper(useWrapping = false)
    public List<Link> link;

    public String href;
    public String id;

    public DataCenter() {}
}