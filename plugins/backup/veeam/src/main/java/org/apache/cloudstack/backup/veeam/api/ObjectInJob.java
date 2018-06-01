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

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "ObjectInJob")
public class ObjectInJob {
    @JacksonXmlProperty(localName = "Href", isAttribute = true)
    private String href;

    @JacksonXmlProperty(localName = "Type", isAttribute = true)
    private String type;

    @JacksonXmlProperty(localName = "Link")
    @JacksonXmlElementWrapper(localName = "Links")
    private List<Link> link;

    @JacksonXmlProperty(localName = "ObjectInJobId", isAttribute = true)
    private String objectInJobId;

    @JacksonXmlProperty(localName = "HierarchyObjRef", isAttribute = true)
    private String hierarchyObjRef;

    @JacksonXmlProperty(localName = "Name", isAttribute = true)
    private String name;

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<Link> getLink() {
        return link;
    }

    public void setLink(List<Link> link) {
        this.link = link;
    }

    public String getObjectInJobId() {
        return objectInJobId;
    }

    public void setObjectInJobId(String objectInJobId) {
        this.objectInJobId = objectInJobId;
    }

    public String getHierarchyObjRef() {
        return hierarchyObjRef;
    }

    public void setHierarchyObjRef(String hierarchyObjRef) {
        this.hierarchyObjRef = hierarchyObjRef;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
