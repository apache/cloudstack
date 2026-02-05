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


import org.apache.cloudstack.quota.constant.QuotaTypes;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class ResourceCounting {

    @PresetVariableDefinition(description = "The number of running user instances.", supportedTypes = {QuotaTypes.NETWORK})
    private int runningVms;
    @PresetVariableDefinition(description = "The number of stopped user instances.", supportedTypes = {QuotaTypes.NETWORK})
    private int stoppedVms;

    public int getRunningVms() {
        return runningVms;
    }

    public void setRunningVms(int runningVms) {
        this.runningVms = runningVms;
    }

    public int getStoppedVms() {
        return stoppedVms;
    }

    public void setStoppedVms(int stoppedVms) {
        this.stoppedVms = stoppedVms;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.JSON_STYLE);
    }
}
