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

package org.apache.cloudstack.storage.datastore.api;

import java.io.Serializable;
import java.util.Map;

public class StorPoolVolumeDef implements Serializable {

    private static final long serialVersionUID = 1L;
    private transient String name;
    private Long size;
    private Map<String, String> tags;
    private String parent;
    private Long iops;
    private String template;
    private String baseOn;
    private String rename;
    private Boolean shrinkOk;

    public StorPoolVolumeDef() {
    }

    public StorPoolVolumeDef(String name, Long size, Map<String, String> tags, String parent, Long iops, String template,
                             String baseOn, String rename, Boolean shrinkOk) {
        super();
        this.name = name;
        this.size = size;
        this.tags = tags;
        this.parent = parent;
        this.iops = iops;
        this.template = template;
        this.baseOn = baseOn;
        this.rename = rename;
        this.shrinkOk = shrinkOk;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Long getSize() {
        return size;
    }
    public void setSize(Long size) {
        this.size = size;
    }
    public Map<String, String> getTags() {
        return tags;
    }
    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }
    public String getParent() {
        return parent;
    }
    public void setParent(String parent) {
        this.parent = parent;
    }
    public Long getIops() {
        return iops;
    }
    public void setIops(Long iops) {
        this.iops = iops;
    }
    public String getTemplate() {
        return template;
    }
    public void setTemplate(String template) {
        this.template = template;
    }
    public String getBaseOn() {
        return baseOn;
    }
    public void setBaseOn(String baseOn) {
        this.baseOn = baseOn;
    }
    public String getRename() {
        return rename;
    }
    public void setRename(String rename) {
        this.rename = rename;
    }

    public Boolean getShrinkOk() {
        return shrinkOk;
    }

    public void setShrinkOk(Boolean shrinkOk) {
        this.shrinkOk = shrinkOk;
    }
}
