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

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Network extends BaseDto {
    private String mtu;           // oVirt prints as string
    private String portIsolation; // "false"
    private String stp;           // "false"
    private NetworkUsages usages;     // { usage: ["vm"] }
    private String vdsmName;

    private Ref dataCenter;

    private String name;
    private String description;
    private String comment;

    @JsonProperty("link")
    private List<Link> link;

    public Network() {}

    // ---- getters / setters ----

    public String getMtu() { return mtu; }
    public void setMtu(final String mtu) { this.mtu = mtu; }

    public String getPortIsolation() { return portIsolation; }
    public void setPortIsolation(final String portIsolation) { this.portIsolation = portIsolation; }

    public String getStp() { return stp; }
    public void setStp(final String stp) { this.stp = stp; }

    public NetworkUsages getUsages() { return usages; }
    public void setUsages(final NetworkUsages usages) { this.usages = usages; }

    public String getVdsmName() { return vdsmName; }
    public void setVdsmName(final String vdsmName) { this.vdsmName = vdsmName; }

    public Ref getDataCenter() { return dataCenter; }
    public void setDataCenter(final Ref dataCenter) { this.dataCenter = dataCenter; }

    public String getName() { return name; }
    public void setName(final String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(final String description) { this.description = description; }

    public String getComment() { return comment; }
    public void setComment(final String comment) { this.comment = comment; }

    public List<Link> getLink() { return link; }
    public void setLink(final List<Link> link) { this.link = link; }
}
