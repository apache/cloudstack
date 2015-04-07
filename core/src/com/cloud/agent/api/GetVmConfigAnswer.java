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

import java.util.List;

public class GetVmConfigAnswer extends Answer {

    String vmName;
    List<NicDetails> nics;

    protected GetVmConfigAnswer() {
    }

    public GetVmConfigAnswer(String vmName, List<NicDetails> nics) {
        this.vmName = vmName;
        this.nics = nics;
    }

    public String getVmName() {
        return vmName;
    }

    public List<NicDetails> getNics() {
        return nics;
    }

    public class NicDetails {
        String macAddress;
        String vlanid;
        boolean state;

        public NicDetails() {
        }

        public NicDetails(String macAddress, String vlanid, boolean state) {
            this.macAddress = macAddress;
            this.vlanid = vlanid;
            this.state = state;
        }

        public String getMacAddress() {
            return macAddress;
        }

        public String getVlanid() {
            return vlanid;
        }

        public boolean getState() {
            return state;
        }
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}