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

/**
 * oVirt-like vNIC profile element.
 * Every vNIC profile MUST reference exactly one network.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VnicProfile {

    private String href;
    private String id;
    private String name;
    private String description;

    private Ref network;
    private Ref dataCenter;

    private List<Link> link;

    public VnicProfile() {
    }

    public String getHref() {
        return href;
    }

    public void setHref(final String href) {
        this.href = href;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public Ref getNetwork() {
        return network;
    }

    public void setNetwork(final Ref network) {
        this.network = network;
    }

    public Ref getDataCenter() {
        return dataCenter;
    }

    public void setDataCenter(final Ref dataCenter) {
        this.dataCenter = dataCenter;
    }

    public List<Link> getLink() {
        return link;
    }

    public void setLink(final List<Link> link) {
        this.link = link;
    }
}
