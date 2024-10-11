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
package org.apache.cloudstack.storage.heuristics.presetvariables;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.Storage;

public class Template extends GenericHeuristicPresetVariable {

    private Hypervisor.HypervisorType hypervisorType;

    private Storage.ImageFormat format;

    private Storage.TemplateType templateType;

    public Hypervisor.HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(Hypervisor.HypervisorType hypervisorType) {
        this.hypervisorType = hypervisorType;
        fieldNamesToIncludeInToString.add("hypervisorType");
    }

    public Storage.ImageFormat getFormat() {
        return format;
    }

    public void setFormat(Storage.ImageFormat format) {
        this.format = format;
        fieldNamesToIncludeInToString.add("format");
    }

    public Storage.TemplateType getTemplateType() {
        return templateType;
    }

    public void setTemplateType(Storage.TemplateType templateType) {
        this.templateType = templateType;
        fieldNamesToIncludeInToString.add("templateType");
    }
}
