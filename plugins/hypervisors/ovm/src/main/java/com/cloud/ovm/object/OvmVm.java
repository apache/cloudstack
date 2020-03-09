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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;

public class OvmVm extends OvmObject {
    public static final String CD = "CD";
    public static final String HDD = "HDD";
    public static final String HVM = "HVM";
    public static final String PV = "PV";
    public static final String FROMCONFIGFILE = "FROMCONFIGFILE";

    public static class Details {
        public int cpuNum;
        public long memory;
        public OvmDisk.Details rootDisk;
        public List<OvmDisk.Details> disks;
        public List<OvmVif.Details> vifs;
        public String name;
        public String uuid;
        public String powerState;
        public String bootDev;
        public String type;

        public Details() {
            disks = new ArrayList<OvmDisk.Details>();
            vifs = new ArrayList<OvmVif.Details>();
        }

        public String toJson() {
            return Coder.toJson(this);
        }
    }

    public OvmVm() {
    }

    /*********** XML RPC Call **************/
    public static void create(Connection c, Details d) throws XmlRpcException {
        Object[] params = {d.toJson()};
        c.call("OvmVm.create", params);
    }

    public static Map<String, String> reboot(Connection c, String vmName) throws XmlRpcException {
        Object[] params = {vmName};
        String res = (String)c.call("OvmVm.reboot", params);
        return Coder.mapFromJson(res);
    }

    public static void stop(Connection c, String vmName) throws XmlRpcException {
        Object[] params = {vmName};
        /* Agent will destroy vm if vm shutdowns failed due to timout after 10 mins, so we set timeout to 20 mins here*/
        c.callTimeoutInSec("OvmVm.stop", params, 1200);
    }

    public static Details getDetails(Connection c, String vmName) throws XmlRpcException {
        Object[] params = {vmName};
        String res = (String)c.call("OvmVm.getDetails", params);
        return Coder.fromJson(res, OvmVm.Details.class);
    }

    public static Map<String, String> getVmStats(Connection c, String vmName) throws XmlRpcException {
        Object[] params = {vmName};
        String res = (String)c.call("OvmVm.getVmStats", params);
        return Coder.mapFromJson(res);
    }

    public static void migrate(Connection c, String vmName, String dest) throws XmlRpcException {
        Object[] params = {vmName, dest};
        c.call("OvmVm.migrate", params);
    }

    public static Map<String, String> register(Connection c, String vmName) throws XmlRpcException {
        Object[] params = {vmName};
        String res = (String)c.call("OvmVm.register", params);
        return Coder.mapFromJson(res);
    }

    public static Integer getVncPort(Connection c, String vmName) throws XmlRpcException {
        Object[] params = {vmName};
        String res = (String)c.call("OvmVm.getVncPort", params);
        Map<String, String> result = Coder.mapFromJson(res);
        return Integer.parseInt(result.get("vncPort"));
    }

    public static void detachOrAttachIso(Connection c, String vmName, String iso, Boolean isAttach) throws XmlRpcException {
        Object[] params = {vmName, iso, isAttach};
        c.call("OvmVm.detachOrAttachIso", params);
    }

}
