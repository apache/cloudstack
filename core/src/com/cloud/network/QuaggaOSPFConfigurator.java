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

package com.cloud.network;

import java.util.Vector;

import com.cloud.agent.api.routing.QuaggaConfigCommand;

public class QuaggaOSPFConfigurator implements QuaggaConfigurator {

    @Override
    public String[] generateZebraConfiguration(QuaggaConfigCommand cmd) {
        Vector<String> zebra = new Vector<String>();
        zebra.add("hostname " + cmd.getRouterName());
        zebra.add("password zebra");
        zebra.add("enable password zebra");
        zebra.add("interface eth1");
        zebra.add(" description link to area 0");
        zebra.add("  ip address " + cmd.getRouterIp() + "/24");
        zebra.add("  link-detect");
        zebra.add("log file /var/log/quagga/zebra.log");
        return (String[])zebra.toArray();
    }

    @Override
    public String[] generateOSPFConfiguration(QuaggaConfigCommand cmd) {
        Vector<String> ospfd = new Vector<String>();
        ospfd.add("hostname " + cmd.getRouterName());
        ospfd.add("password zebra");
        ospfd.add("router ospf");
        ospfd.add(" ospf router-id " + cmd.getRouterIp());
        ospfd.add(" redistribute connected");
        ospfd.add(" no passive-interface eth1");
        ospfd.add(" network " + cmd.getRouterIp() + "/24 area 0");
        for (String cidr: cmd.getTierCidrs()){
            ospfd.add(" network " + cidr +" area " + cmd.getRouterIp());
        }
        ospfd.add("log file /var/log/quagga/ospfd.log");

        return (String[])ospfd.toArray();
    }

}
