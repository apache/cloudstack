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

package org.apache.cloudstack.backup.veeam.api;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement(localName = "VmRestorePoint")
public class VmRestorePoint {
    @JacksonXmlProperty(localName = "Type", isAttribute = true)
    private String type;

    @JacksonXmlProperty(localName = "Href", isAttribute = true)
    private String href;

    @JacksonXmlProperty(localName = "Name", isAttribute = true)
    private String name;

    @JacksonXmlProperty(localName = "UID", isAttribute = true)
    private String uid;

    @JacksonXmlProperty(localName = "VmDisplayName", isAttribute = true)
    private String vmDisplayName;

    @JacksonXmlProperty(localName = "Link")
    @JacksonXmlElementWrapper(localName = "Links")
    private List<Link> link;

    @JacksonXmlProperty(localName = "CreationTimeUTC")
    private String creationTimeUtc;

    @JacksonXmlProperty(localName = "VmName")
    private String vmName;

    @JacksonXmlProperty(localName = "Algorithm")
    private String algorithm;

    @JacksonXmlProperty(localName = "PointType")
    private String pointType;

    @JacksonXmlProperty(localName = "HierarchyObjRef")
    private String hierarchyObjRef;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getVmDisplayName() {
        return vmDisplayName;
    }

    public void setVmDisplayName(String vmDisplayName) {
        this.vmDisplayName = vmDisplayName;
    }

    public List<Link> getLink() {
        return link;
    }

    public void setLink(List<Link> link) {
        this.link = link;
    }

    public String getCreationTimeUtc() {
        return creationTimeUtc;
    }

    public void setCreationTimeUtc(String creationTimeUtc) {
        this.creationTimeUtc = creationTimeUtc;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getPointType() {
        return pointType;
    }

    public void setPointType(String pointType) {
        this.pointType = pointType;
    }

    public String getHierarchyObjRef() {
        return hierarchyObjRef;
    }

    public void setHierarchyObjRef(String hierarchyObjRef) {
        this.hierarchyObjRef = hierarchyObjRef;
    }
}
