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

import java.util.HashMap;

import com.cloud.agent.api.LogLevel.Log4jLevel;

@LogLevel(Log4jLevel.Trace)
public class GetVmStatsAnswer extends Answer {

    HashMap<String, VmStatsEntry> vmStatsMap;

    public GetVmStatsAnswer(GetVmStatsCommand cmd, HashMap<String, VmStatsEntry> vmStatsMap) {
        super(cmd);
        this.vmStatsMap = vmStatsMap;
    }

    public HashMap<String, VmStatsEntry> getVmStatsMap() {
        return vmStatsMap;
    }

    protected GetVmStatsAnswer() {
        //no-args constructor for json serialization-deserialization
    }
}
