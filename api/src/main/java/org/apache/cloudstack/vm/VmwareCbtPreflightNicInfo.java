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
package org.apache.cloudstack.vm;

/**
 * A source VM NIC discovered during VMware CBT preflight, so NIC-to-network
 * mappings can be validated when the migration is started instead of failing
 * at import time, after replication has already run.
 */
public class VmwareCbtPreflightNicInfo {

    private final String sourceNicId;
    private final String adapterType;
    private final String macAddress;
    private final Integer vlan;

    public VmwareCbtPreflightNicInfo(String sourceNicId, String adapterType, String macAddress, Integer vlan) {
        this.sourceNicId = sourceNicId;
        this.adapterType = adapterType;
        this.macAddress = macAddress;
        this.vlan = vlan;
    }

    public String getSourceNicId() {
        return sourceNicId;
    }

    public String getAdapterType() {
        return adapterType;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public Integer getVlan() {
        return vlan;
    }
}
