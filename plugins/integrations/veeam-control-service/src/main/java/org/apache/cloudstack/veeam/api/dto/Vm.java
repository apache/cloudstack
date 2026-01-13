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

/**
 * VM DTO intentionally uses snake_case field names to match the required JSON.
 * Configure Jackson globally with SNAKE_CASE or keep as-is.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JacksonXmlRootElement(localName = "vm")
public final class Vm {
    public String href;
    public String id;
    public String name;
    public String description;

    public String status;        // "up", "down", ...

    @JsonProperty("stop_reason")
    @JacksonXmlProperty(localName = "stop_reason")
    public String stopReason;   // empty string allowed

    @JsonProperty("stop_time")
    @JacksonXmlProperty(localName = "stop_time")
    public Long stopTime;       // epoch millis

    public Ref template;

    @JsonProperty("original_template")
    @JacksonXmlProperty(localName = "original_template")
    public Ref originalTemplate;

    public Ref cluster;
    public Ref host;

    public Long memory;          // bytes
    public Cpu cpu;
    public Os os;
    public Bios bios;

    public Actions actions;      // actions.link[]
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<Link> link;      // related resources

    public Vm() {}
}
