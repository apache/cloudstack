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
package com.cloud.agent.api;


public class OvsSetTagAndFlowCommand extends Command {
    String vlans;
    String vmName;
    String seqno;
    String tag;
    Long vmId;

    @Override
    public boolean executeInSequence() {
        return true;
    }

    public String getSeqNo() {
        return seqno;
    }

    public String getVlans() {
        return vlans;
    }

    public String getVmName() {
        return vmName;
    }

    public Long getVmId() {
        return vmId;
    }

    public String getTag() {
        return tag;
    }

    public OvsSetTagAndFlowCommand(String vmName, String tag, String vlans,
            String seqno, Long vmId) {
        this.vmName = vmName;
        this.tag = tag;
        this.vlans = vlans;
        this.seqno = seqno;
        this.vmId = vmId;
    }
}
