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

package org.apache.cloudstack.veeam.api.response;

import java.util.List;

import org.apache.cloudstack.veeam.api.dto.Vm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Required list response:
 *   { "vm": [ {..}, {..} ] }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "vm" })
@JacksonXmlRootElement(localName = "vms")
public final class VmCollectionResponse {
    @JsonProperty("vm")
    @JacksonXmlElementWrapper(useWrapping = false)
    public List<Vm> vm;

    public VmCollectionResponse() {}

    public VmCollectionResponse(final List<Vm> vm) {
        this.vm = vm;
    }
}
