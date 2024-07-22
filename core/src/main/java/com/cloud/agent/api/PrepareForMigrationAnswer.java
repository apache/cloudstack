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

package com.cloud.agent.api;

import com.cloud.agent.api.to.DpdkTO;

import java.util.HashMap;
import java.util.Map;

public class PrepareForMigrationAnswer extends Answer {

    private Map<String, DpdkTO> dpdkInterfaceMapping = new HashMap<>();

    private Integer newVmCpuShares = null;

    protected PrepareForMigrationAnswer() {
    }

    public PrepareForMigrationAnswer(PrepareForMigrationCommand cmd, String detail) {
        super(cmd, false, detail);
    }

    public PrepareForMigrationAnswer(PrepareForMigrationCommand cmd, Exception ex) {
        super(cmd, ex);
    }

    public PrepareForMigrationAnswer(PrepareForMigrationCommand cmd) {
        super(cmd, true, null);
    }

    public void setDpdkInterfaceMapping(Map<String, DpdkTO> mapping) {
        this.dpdkInterfaceMapping = mapping;
    }

    public Map<String, DpdkTO> getDpdkInterfaceMapping() {
        return this.dpdkInterfaceMapping;
    }

    public Integer getNewVmCpuShares() {
        return newVmCpuShares;
    }

    public void setNewVmCpuShares(Integer newVmCpuShares) {
        this.newVmCpuShares = newVmCpuShares;
    }
}
