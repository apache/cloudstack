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

package com.cloud.utils.cisco.n1kv.vsm;

import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.BindingType;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.PortProfileType;
import com.cloud.utils.cisco.n1kv.vsm.VsmCommand.SwitchPortMode;

public class PortProfile {
    public PortProfileType type;
    public SwitchPortMode mode;
    public BindingType binding;
    public String profileName;
    public String inputPolicyMap;
    public String outputPolicyMap;
    public String vlan;
    public boolean status;
    public int maxPorts;

    PortProfile() {
        profileName = null;
        inputPolicyMap = null;
        outputPolicyMap = null;
        vlan = null;
        status = false;
        maxPorts = 32;
        type = PortProfileType.none;
        mode = SwitchPortMode.none;
        binding = BindingType.none;
    }
}
