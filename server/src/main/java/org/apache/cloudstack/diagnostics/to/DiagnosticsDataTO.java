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
package org.apache.cloudstack.diagnostics.to;

import com.cloud.agent.api.to.DataObjectType;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.DataTO;
import com.cloud.hypervisor.Hypervisor;

public class DiagnosticsDataTO implements DataTO {
    private DataStoreTO dataStoreTO;
    private Hypervisor.HypervisorType hypervisorType;
    private String path;
    private long id;

    public DiagnosticsDataTO(Hypervisor.HypervisorType hypervisorType, DataStoreTO dataStoreTO) {
        this.hypervisorType = hypervisorType;
        this.dataStoreTO = dataStoreTO;
    }

    @Override
    public DataObjectType getObjectType() {
        return DataObjectType.ARCHIVE;
    }

    @Override
    public DataStoreTO getDataStore() {
        return dataStoreTO;
    }

    @Override
    public Hypervisor.HypervisorType getHypervisorType() {
        return hypervisorType;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public long getId() {
        return id;
    }
}
