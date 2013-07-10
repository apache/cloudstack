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
package com.cloud.ucs.manager;

import com.cloud.utils.xmlobject.XmlObject;

public class UcsCommands {
    public static String loginCmd(String username, String password) {
        XmlObject cmd = new XmlObject("aaaLogin");
        cmd.putElement("inName", username);
        cmd.putElement("inPassword", password);
        return cmd.dump();
    }

    public static String listComputeBlades(String cookie) {
        XmlObject cmd = new XmlObject("configResolveClass");
        cmd.putElement("classId", "computeBlade");
        cmd.putElement("cookie", cookie);
        cmd.putElement("inHierarchical", "false");
        return cmd.dump();
    }

    public static String listProfiles(String cookie) {
        XmlObject cmd = new XmlObject("configFindDnsByClassId");
        cmd.putElement("classId", "lsServer");
        cmd.putElement("cookie", cookie);
        return cmd.dump();
    }

    public static String cloneProfile(String cookie, String srcDn, String newProfileName) {
        XmlObject cmd = new XmlObject("lsClone");
        cmd.putElement("cookie", cookie);
        cmd.putElement("dn", srcDn);
        cmd.putElement("inTargetOrg", "org-root");
        cmd.putElement("inServerName", newProfileName);
        cmd.putElement("inHierarchical", "false");
        return cmd.dump();
    }

    public static String configResolveDn(String cookie, String dn) {
        XmlObject cmd = new XmlObject("configResolveDn");
        cmd.putElement("cookie", cookie);
        cmd.putElement("dn", dn);
        return cmd.toString();
    }

    public static String associateProfileToBlade(String cookie, String profileDn, String bladeDn) {
        XmlObject cmd = new XmlObject("configConfMos").putElement("cookie", cookie).putElement("inHierarchical", "true").putElement(
                "inConfigs", new XmlObject("inConfigs").putElement(
                        "pair", new XmlObject("pair").putElement("key", profileDn).putElement(
                                "lsServer", new XmlObject("lsServer")
                                .putElement("agentPolicyName", "")
                                .putElement("biosProfileName", "")
                                .putElement("bootPolicyName", "")
                                .putElement("descr", "")
                                .putElement("dn", profileDn)
                                .putElement("dynamicConPolicyName", "")
                                .putElement("extIPState", "none")
                                .putElement("hostFwPolicyName", "")
                                .putElement("identPoolName", "")
                                .putElement("localDiskPolicyName", "")
                                .putElement("maintPolicyName", "")
                                .putElement("mgmtAccessPolicyName", "")
                                .putElement("mgmtFwPolicyName", "")
                                .putElement("powerPolicyName", "")
                                .putElement("scrubPolicyName", "")
                                .putElement("solPolicyName", "")
                                .putElement("srcTemplName", "")
                                .putElement("statsPolicyName", "default")
                                .putElement("status", "")
                                .putElement("usrLbl", "")
                                .putElement("uuid", "")
                                .putElement("vconProfileName", "")
                                .putElement("lsBinding", new XmlObject("lsBinding")
                                            .putElement("pnDn", bladeDn)
                                            .putElement("restrictMigration", "no")
                                            .putElement("rn", "pn")
                                        )
                                )
                        )
                );

        return cmd.dump();
    }
}
