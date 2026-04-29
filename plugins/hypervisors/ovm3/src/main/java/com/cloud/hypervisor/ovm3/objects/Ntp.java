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

package com.cloud.hypervisor.ovm3.objects;

import java.util.ArrayList;
import java.util.List;

public class Ntp extends OvmObject {
    private List<String> ntpHosts = new ArrayList<String>();
    private Boolean isServer = null;
    private Boolean isRunning = null;

    public Ntp(Connection c) {
        setClient(c);
    }

    public List<String> addServer(String server) {
        if (!ntpHosts.contains(server)) {
            ntpHosts.add(server);
        }
        return ntpHosts;
    }

    public List<String> removeServer(String server) {
        if (ntpHosts.contains(server)) {
            ntpHosts.remove(server);
        }
        return ntpHosts;
    }

    public List<String> getServers() {
        return ntpHosts;
    }
    public void setServers(List<String> s) {
        ntpHosts = s;
    }

    public Boolean isRunning() {
        return isRunning;
    }

    public Boolean isServer() {
        return isServer;
    }

    public Boolean getDetails() throws Ovm3ResourceException {
        return getNtp();
    }

    /*
     * get_ntp, <class 'agent.api.host.linux.Linux'> argument: self - default:
     * None
     */
    public Boolean getNtp() throws Ovm3ResourceException {
        Object[] v = (Object[]) callWrapper("get_ntp");
        int c = 0;
        for (Object o : v) {
            if (o instanceof java.lang.Boolean) {
                if (c == 0) {
                    isServer = (Boolean) o;
                }
                if (c == 1) {
                    isRunning = (Boolean) o;
                }
                c += 1;
            } else if (o instanceof java.lang.Object) {
                Object[] s = (Object[]) o;
                for (Object m : s) {
                    addServer((String) m);
                }
            }
        }
        return true;
    }

    public Boolean setNtp(List<String> ntpHosts, Boolean running)
            throws Ovm3ResourceException {
        if (ntpHosts.isEmpty()) {
            return false;
        }
        return nullIsTrueCallWrapper("set_ntp", ntpHosts, running);
    }

    /* also cleans the vector */
    public Boolean setNtp(String server, Boolean running)
            throws Ovm3ResourceException {
        ntpHosts = new ArrayList<String>();
        ntpHosts.add(server);
        return setNtp(ntpHosts, running);
    }

    public Boolean setNtp(Boolean running) throws Ovm3ResourceException {
        return setNtp(ntpHosts, running);
    }

    public Boolean disableNtp() throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("disable_ntp");

    }

    public Boolean enableNtp() throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("enable_ntp");
    }
}
