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

@JacksonXmlRootElement(localName = "RestoreSession")
public class RestoreSession {

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

    @JacksonXmlProperty(localName = "JobType")
    private String jobType;

    @JacksonXmlProperty(localName = "CreationTimeUTC")
    private String creationTimeUTC;

    @JacksonXmlProperty(localName = "EndTimeUTC")
    private String endTimeUTC;

    @JacksonXmlProperty(localName = "State")
    private String state;

    @JacksonXmlProperty(localName = "Result")
    private String result;

    @JacksonXmlProperty(localName = "Progress")
    private String progress;

    @JacksonXmlProperty(localName = "RestoredObjRef")
    private String restoredObjRef;

    public List<Link> getLink() {
        return link;
    }

    public String getJobType() {
        return jobType;
    }

    public String getState() {
        return state;
    }

    public String getResult() {
        return result;
    }

    public String getType() {
        return type;
    }

    public String getHref() {
        return href;
    }

    public String getVmDisplayName() {
        return vmDisplayName;
    }

    public String getCreationTimeUTC() {
        return creationTimeUTC;
    }

    public String getEndTimeUTC() {
        return endTimeUTC;
    }

    public String getProgress() {
        return progress;
    }

    public String getName() {
        return name;
    }

    public String getUid() {
        return uid;
    }

    public String getRestoredObjRef() {
        return restoredObjRef;
    }
}
