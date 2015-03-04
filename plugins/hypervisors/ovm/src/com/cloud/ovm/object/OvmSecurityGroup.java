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

import org.apache.xmlrpc.XmlRpcException;

public class OvmSecurityGroup extends OvmObject {

    // canBridgeFirewall
    // cleanupRules
    // defaultNetworkRulesUserVm
    // addNetworkRules
    // deleteAllNetworkRulesForVm

    public static boolean canBridgeFirewall(Connection c) throws XmlRpcException {
        Object[] params = {};
        return (Boolean)c.call("OvmSecurityGroup.can_bridge_firewall", params);
    }

    public static boolean cleanupNetworkRules(Connection c) throws XmlRpcException {
        Object[] params = {};
        return (Boolean)c.call("OvmSecurityGroup.cleanup_rules", params);
    }

    public static boolean defaultNetworkRulesForUserVm(Connection c, String vmName, String vmId, String ipAddress, String macAddress, String vifName, String bridgeName)
        throws XmlRpcException {
        Object[] params = {vmName, vmId, ipAddress, macAddress, vifName, bridgeName};
        return (Boolean)c.call("OvmSecurityGroup.default_network_rules_user_vm", params);
    }

    public static boolean addNetworkRules(Connection c, String vmName, String vmId, String guestIp, String signature, String seqno, String vifMacAddress,
        String newRules, String vifDeviceName, String bridgeName) throws XmlRpcException {
        Object[] params = {vmName, vmId, guestIp, signature, seqno, vifMacAddress, newRules, vifDeviceName, bridgeName};
        return (Boolean)c.call("OvmSecurityGroup.add_network_rules", params);
    }

    public static boolean deleteAllNetworkRulesForVm(Connection c, String vmName, String vif) throws XmlRpcException {
        Object[] params = {vmName, vif};
        return (Boolean)c.call("OvmSecurityGroup.delete_all_network_rules_for_vm", params);
    }

}
