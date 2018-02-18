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
package com.cloud.hypervisor.vmware.mo;

import com.vmware.vim25.ManagedObjectReference;

public class NetworkDetails {

    private String _name;
    private ManagedObjectReference _morNetwork;
    private ManagedObjectReference[] _morVMsOnNetwork;
    private String _gcTag;

    public NetworkDetails(String name, ManagedObjectReference morNetwork, ManagedObjectReference[] morVMsOnNetwork, String gcTag) {
        _name = name;
        _morNetwork = morNetwork;
        _morVMsOnNetwork = morVMsOnNetwork;
        _gcTag = gcTag;
    }

    public String getName() {
        return _name;
    }

    public ManagedObjectReference getNetworkMor() {
        return _morNetwork;
    }

    public ManagedObjectReference[] getVMMorsOnNetwork() {
        return _morVMsOnNetwork;
    }

    public String getGCTag() {
        return _gcTag;
    }
}
