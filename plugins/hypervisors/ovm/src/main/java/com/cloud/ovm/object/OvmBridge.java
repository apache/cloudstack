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

import java.util.List;

import org.apache.xmlrpc.XmlRpcException;

public class OvmBridge extends OvmObject {
    public static class Details {
        public String name;
        public String attach;
        public List<String> interfaces;

        public String toJson() {
            return Coder.toJson(this);
        }
    }

    public static void create(Connection c, Details d) throws XmlRpcException {
        Object[] params = {d.toJson()};
        c.call("OvmNetwork.createBridge", params);
    }

    public static void delete(Connection c, String name) throws XmlRpcException {
        Object[] params = {name};
        c.call("OvmNetwork.deleteBridge", params);
    }

    public static List<String> getAllBridges(Connection c) throws XmlRpcException {
        String res = (String)c.call("OvmNetwork.getAllBridges", Coder.s_emptyParams);
        return Coder.listFromJson(res);
    }

    public static String getBridgeByIp(Connection c, String ip) throws XmlRpcException {
        Object[] params = {ip};
        String res = (String)c.call("OvmNetwork.getBridgeByIp", params);
        return Coder.mapFromJson(res).get("bridge");
    }

    public static void createVlanBridge(Connection c, OvmBridge.Details bDetails, OvmVlan.Details vDetails) throws XmlRpcException {
        Object[] params = {bDetails.toJson(), vDetails.toJson()};
        c.call("OvmNetwork.createVlanBridge", params);
    }

    public static void deleteVlanBridge(Connection c, String name) throws XmlRpcException {
        Object[] params = {name};
        c.call("OvmNetwork.deleteVlanBridge", params);
    }

    public static Details getDetails(Connection c, String name) throws XmlRpcException {
        Object[] params = {name};
        String res = (String)c.call("OvmNetwork.getBridgeDetails", params);
        return Coder.fromJson(res, Details.class);
    }
}
