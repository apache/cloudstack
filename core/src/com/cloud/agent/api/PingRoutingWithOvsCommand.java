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
import java.util.Map;

import com.cloud.host.Host;
import com.cloud.utils.Pair;

public class PingRoutingWithOvsCommand extends PingRoutingCommand {
    List<Pair<String, Long>> states;

    protected PingRoutingWithOvsCommand() {
        super();
    }

    public PingRoutingWithOvsCommand(Host.Type type, long id, Map<String, HostVmStateReportEntry> hostVmStateReport,
            List<Pair<String, Long>> ovsStates) {
        super(type, id, hostVmStateReport);

        this.states = ovsStates;
    }

    public List<Pair<String, Long>> getStates() {
        return states;
    }
}
