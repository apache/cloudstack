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

package org.apache.cloudstack.quota.activationrule.presetvariables;

import java.util.List;

import com.cloud.storage.ScopeType;

public class Storage extends GenericPresetVariable {
    @PresetVariableDefinition(description = "List of string representing the tags of the storage where the volume is (i.e.: [\"a\", \"b\"]).")
    private List<String> tags;

    @PresetVariableDefinition(description = "Whether the tag is a rule interpreted in JavaScript. Applicable only for primary storages.")
    private Boolean isTagARule;

    @PresetVariableDefinition(description = "Scope of the storage where the volume is. Values can be: ZONE, CLUSTER or HOST. Applicable only for primary storages.")
    private ScopeType scope;

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
        fieldNamesToIncludeInToString.add("tags");
    }

    public Boolean getIsTagARule() {
        return isTagARule;
    }

    public void setIsTagARule(Boolean isTagARule) {
        this.isTagARule = isTagARule;
        fieldNamesToIncludeInToString.add("isTagARule");
    }

    public ScopeType getScope() {
        return scope;
    }

    public void setScope(ScopeType scope) {
        this.scope = scope;
        fieldNamesToIncludeInToString.add("scope");
    }

}
