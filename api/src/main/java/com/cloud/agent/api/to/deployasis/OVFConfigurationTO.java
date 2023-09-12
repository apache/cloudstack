//
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
//
package com.cloud.agent.api.to.deployasis;

import java.util.List;

/**
 * This class represents a template deployment option (configuration) parsed from the OVF
 */
public class OVFConfigurationTO implements TemplateDeployAsIsInformationTO {

    private final String id;
    private final String label;
    private final String description;
    private List<OVFVirtualHardwareItemTO> hardwareItems;
    private int index;

    public OVFConfigurationTO(String id, String label, String description, int index) {
        this.id = id;
        this.label = label;
        this.description = description;
        this.index = index;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public void setHardwareItems(List<OVFVirtualHardwareItemTO> items) {
        this.hardwareItems = items;
    }

    public List<OVFVirtualHardwareItemTO> getHardwareItems() {
        return hardwareItems;
    }

    public int getIndex() {
        return index;
    }
}
