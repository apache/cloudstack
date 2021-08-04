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
package com.cloud.ovm.object;

import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;

public class OvmHost extends OvmObject {
    public static final String SITE = "site";
    public static final String UTILITY = "utility";
    public static final String XEN = "xen";

    public static class Details {
        public String primaryIp;
        public Integer cpuNum;
        public Integer cpuSpeed;
        public Long totalMemory;
        public Long freeMemory;
        public Long dom0Memory;
        public String agentVersion;
        public String name;
        public String dom0KernelVersion;
        public String hypervisorVersion;

        public String toJson() {
            return Coder.toJson(this);
        }
    }

    public static void registerAsPrimary(Connection c) throws XmlRpcException {
        Object[] params = {c.getIp(), c.getUserName(), c.getPassword(), c.getPort(), c.getIsSsl()};
        c.call("OvmHost.registerAsPrimary", params, false);
    }

    public static void registerAsVmServer(Connection c) throws XmlRpcException {
        Object[] params = {c.getIp(), c.getUserName(), c.getPassword(), c.getPort(), c.getIsSsl()};
        c.call("OvmHost.registerAsVmServer", params);
    }

    public static Details getDetails(Connection c) throws XmlRpcException {
        String res = (String)c.call("OvmHost.getDetails", Coder.s_emptyParams);
        return Coder.fromJson(res, OvmHost.Details.class);
    }

    public static void ping(Connection c) throws XmlRpcException {
        Object[] params = {c.getIp()};
        c.call("OvmHost.ping", params);
    }

    public static Map<String, String> getPerformanceStats(Connection c, String bridge) throws XmlRpcException {
        Object[] params = {bridge};
        String res = (String)c.call("OvmHost.getPerformanceStats", params);
        return Coder.mapFromJson(res);
    }

    public static Map<String, String> getAllVms(Connection c) throws XmlRpcException {
        String res = (String)c.call("OvmHost.getAllVms", Coder.s_emptyParams);
        return Coder.mapFromJson(res);
    }

    public static void setupHeartBeat(Connection c, String poolUuid, String ip) throws XmlRpcException {
        Object[] params = {poolUuid, ip};
        c.call("OvmHost.setupHeartBeat", params);
    }

    public static Boolean fence(Connection c, String ip) throws XmlRpcException {
        Object[] params = {ip};
        String res = (String)c.call("OvmHost.fence", params);
        Map<String, String> result = Coder.mapFromJson(res);
        return Boolean.parseBoolean(result.get("isLive"));
    }

    public static void pingAnotherHost(Connection c, String ip) throws XmlRpcException {
        Object[] params = {ip};
        c.call("OvmHost.pingAnotherHost", params);
    }
}
