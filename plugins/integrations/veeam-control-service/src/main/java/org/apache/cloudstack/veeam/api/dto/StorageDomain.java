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
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "storage_domain")
public final class StorageDomain {

    // Identifiers
    public String id;
    public String href;

    public String name;
    public String description;
    public String comment;

    // oVirt returns these as strings in your sample
    public String available;
    public String used;
    public String committed;

    @JsonProperty("block_size")
    @JacksonXmlProperty(localName = "block_size")
    public String blockSize;

    @JsonProperty("warning_low_space_indicator")
    @JacksonXmlProperty(localName = "warning_low_space_indicator")
    public String warningLowSpaceIndicator;

    @JsonProperty("critical_space_action_blocker")
    @JacksonXmlProperty(localName = "critical_space_action_blocker")
    public String criticalSpaceActionBlocker;

    public String status;          // e.g. "unattached" (optional in your first object)
    public String type;            // data / image / iso / export

    public String master;          // "true"/"false"
    public String backup;          // "true"/"false"

    @JsonProperty("external_status")
    @JacksonXmlProperty(localName = "external_status")
    public String externalStatus;  // "ok"

    @JsonProperty("storage_format")
    @JacksonXmlProperty(localName = "storage_format")
    public String storageFormat;   // v5 / v1

    @JsonProperty("discard_after_delete")
    @JacksonXmlProperty(localName = "discard_after_delete")
    public String discardAfterDelete;

    @JsonProperty("wipe_after_delete")
    @JacksonXmlProperty(localName = "wipe_after_delete")
    public String wipeAfterDelete;

    @JsonProperty("supports_discard")
    @JacksonXmlProperty(localName = "supports_discard")
    public String supportsDiscard;

    @JsonProperty("supports_discard_zeroes_data")
    @JacksonXmlProperty(localName = "supports_discard_zeroes_data")
    public String supportsDiscardZeroesData;

    // Nested
    public Storage storage;

    @JsonProperty("data_centers")
    @JacksonXmlProperty(localName = "data_centers")
    public DataCenters dataCenters;

    public Actions actions;

    @JacksonXmlElementWrapper(useWrapping = false)
    public List<Link> link;

    public StorageDomain() {}
}
