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

import java.util.ArrayList;
import java.util.List;

public class SecStorageFirewallCfgCommand extends Command {

    public static class PortConfig {
        boolean add;
        String sourceIp;
        String port;
        String intf;

        public PortConfig(String sourceIp, String port, boolean add, String intf) {
            this.add = add;
            this.sourceIp = sourceIp;
            this.port = port;
            this.intf = intf;
        }

        public PortConfig() {

        }

        public boolean isAdd() {
            return add;
        }

        public String getSourceIp() {
            return sourceIp;
        }

        public String getPort() {
            return port;
        }

        public String getIntf() {
            return intf;
        }
    }

    private List<PortConfig> portConfigs = new ArrayList<PortConfig>();
    private boolean isAppendAIp = false;

    public SecStorageFirewallCfgCommand() {

    }

    public SecStorageFirewallCfgCommand(boolean isAppend) {
        this.isAppendAIp = isAppend;
    }

    public void addPortConfig(String sourceIp, String port, boolean add, String intf) {
        PortConfig pc = new PortConfig(sourceIp, port, add, intf);
        this.portConfigs.add(pc);

    }

    public boolean getIsAppendAIp() {
        return isAppendAIp;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }

    public List<PortConfig> getPortConfigs() {
        return portConfigs;
    }
}
