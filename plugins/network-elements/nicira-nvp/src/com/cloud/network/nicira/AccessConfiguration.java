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

package com.cloud.network.nicira;

import java.util.List;

public abstract class AccessConfiguration<T extends AccessRule> extends BaseNiciraNamedEntity {

    protected List<T> logicalPortEgressRules;
    protected List<T> logicalPortIngressRules;

    public List<T> getLogicalPortEgressRules() {
        return logicalPortEgressRules;
    }

    public void setLogicalPortEgressRules(final List<T> logicalPortEgressRules) {
        this.logicalPortEgressRules = logicalPortEgressRules;
    }

    public List<T> getLogicalPortIngressRules() {
        return logicalPortIngressRules;
    }

    public void setLogicalPortIngressRules(final List<T> logicalPortIngressRules) {
        this.logicalPortIngressRules = logicalPortIngressRules;
    }
}
