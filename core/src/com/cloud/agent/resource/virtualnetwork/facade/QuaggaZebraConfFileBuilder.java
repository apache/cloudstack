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

package com.cloud.agent.resource.virtualnetwork.facade;

import com.cloud.agent.api.routing.QuaggaConfigCommand;

public class QuaggaZebraConfFileBuilder {
    private String vrIp;
    private String vrName;

    public QuaggaZebraConfFileBuilder(final QuaggaConfigCommand command) {
        vrIp = command.getRouterPublicIp();
        vrName = command.getRouterName();
    }

    public String getConfig() {
        StringBuilder zebra = new StringBuilder();
        zebra.append("hostname " + vrName).append(",");
        zebra.append("interface eth1").append(",");
        zebra.append(" description link to area 0").append(",");
        zebra.append("  ip address " + vrIp + "/24").append(",");
        zebra.append("  link-detect").append(",");
        zebra.append("log file /var/log/quagga/zebra.log").append(",");
        return zebra.toString();
    }
}
