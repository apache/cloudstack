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

import org.apache.cloudstack.utils.net.cidr.CIDR;

import com.cloud.agent.api.routing.QuaggaConfigCommand;
import com.cloud.network.vpc.OSPFZoneConfig;

public class QuaggaOSPFConfFileBuilder {
    private String vrIp;
    private String vrName;
    private CIDR[] cidrs;
    private OSPFZoneConfig qzc;

    public QuaggaOSPFConfFileBuilder(final QuaggaConfigCommand command) {
        vrIp = command.getRouterPublicIp();
        vrName = command.getRouterName();
        cidrs = command.getTierCidrs();
        qzc = command.getZoneConfig();
    }

    public String getConfig() {
        StringBuilder ospfd = new StringBuilder();
        ospfd.append("hostname " + vrName).append(",");
        ospfd.append("interface eth1").append(",");
        ospfd.append("  ip ospf hello-interval ").append(qzc.getHelloInterval()).append(",");
        ospfd.append("  ip ospf dead-interval ").append(qzc.getDeadInterval()).append(",");
        ospfd.append("  ip ospf retransmit-interval ").append(qzc.getRetransmitInterval()).append(",");
        ospfd.append("  ip ospf transmit-delay ").append(qzc.getTransitDelay()).append(",");
        if (qzc.getAuthentication().equals(OSPFZoneConfig.Authentication.MD5) && qzc.getPassword().length() > 3){
            ospfd.append("  ip ospf authentication message-digest").append(",");
            ospfd.append("  ip ospf message-digest-key 1 md5 ").append(qzc.getPassword()).append(",");
        }
        ospfd.append("router ospf").append(",");
        ospfd.append("  ospf router-id " + vrIp).append(",");
        ospfd.append("  redistribute connected").append(",");
        ospfd.append("  no passive-interface eth1").append(",");
        ospfd.append("  network " + vrIp + "/24 area ").append(qzc.getOspfArea()).append(",");
        if (qzc.getAuthentication().equals(OSPFZoneConfig.Authentication.MD5) && qzc.getPassword().length() > 3){
            ospfd.append("  area ").append(qzc.getOspfArea()).append(" authentication message-digest").append(",");
        }
        if (cidrs != null) {
            for (CIDR cidr : cidrs) {
                ospfd.append("  network " + cidr + " area ").append(qzc.getOspfArea()).append(",");
            }
        }
        ospfd.append("log file /var/log/quagga/ospfd.log").append(",");
        return ospfd.toString();
    }
}
