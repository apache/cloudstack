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
package com.cloud.agent.api.to;

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.storage.Storage.ImageFormat;
import com.cloud.template.VirtualMachineTemplate;

public class TemplateTO implements InternalIdentity {
    private long id;
    private String uniqueName;
    private ImageFormat format;

    protected TemplateTO() {
    }

    public TemplateTO(VirtualMachineTemplate template) {
        this.id = template.getId();
        this.uniqueName = template.getUniqueName();
        this.format = template.getFormat();
    }

    public long getId() {
        return id;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    public ImageFormat getFormat() {
        return format;
    }

    @Override
    public String toString() {
        return new StringBuilder("Tmpl[").append(id).append("|").append(uniqueName).append("]").toString();
    }
}
