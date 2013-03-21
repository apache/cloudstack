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
package org.apache.cloudstack.entity.identity;

import java.util.List;

import com.cloud.domain.Domain.State;

/**
 * Domain Resource
 */
public class DomainResource {

    // attributes
    private String name;
    private String path;
    private State state;
    private String networkDomain;


    // relationship
    private DomainResource parent;
    private List<DomainResource> children;


    public DomainResource getParent() {
        return parent;
    }
    public void setParent(DomainResource parent) {
        this.parent = parent;
    }
    public List<DomainResource> getChildren() {
        return children;
    }
    public void setChildren(List<DomainResource> children) {
        this.children = children;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public State getState() {
        return state;
    }
    public void setState(State state) {
        this.state = state;
    }
    public String getNetworkDomain() {
        return networkDomain;
    }
    public void setNetworkDomain(String networkDomain) {
        this.networkDomain = networkDomain;
    }




}
