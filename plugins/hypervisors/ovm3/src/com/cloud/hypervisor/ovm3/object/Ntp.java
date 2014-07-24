/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.cloud.hypervisor.ovm3.object;

import java.util.ArrayList;
import java.util.List;

import org.apache.xmlrpc.XmlRpcException;

/*
 * should become an interface implementation
 */
public class Ntp extends OvmObject {
    private List<String> Servers = new ArrayList<String>();
    private Boolean isServer = null;
    private Boolean isRunning = null;

    public Ntp(Connection c) {
        client = c;
    }

    public List<String> addServer(String server) {
        if (Servers.contains(server) == false) {
            Servers.add(server);
        }
        return Servers;
    }

    public List<String> removeServer(String server) {
        if (Servers.contains(server)) {
            Servers.remove(server);
        }
        return Servers;
    }

    public List<String> servers() {
        return Servers;
    }

    public Boolean isRunning() {
        return isRunning;
    }

    public Boolean isServer() {
        return isServer;
    }

    public Boolean getDetails() throws XmlRpcException {
        return this.getNtp();
    }

    /*
     * get_ntp, <class 'agent.api.host.linux.Linux'> argument: self - default:
     * None
     */
    public Boolean getNtp() throws XmlRpcException {
        Object[] v = (Object[]) callWrapper("get_ntp");
        int c = 0;
        for (Object o : v) {
            // System.out.println(o.getClass());
            if (o instanceof java.lang.Boolean) {
                if (c == 0) {
                    this.isServer = (Boolean) o;
                }
                if (c == 1) {
                    this.isRunning = (Boolean) o;
                }
                // should not get here
                if (c > 1) {
                    return false;
                }
                c += 1;
            } else if (o instanceof java.lang.Object) {
                Object[] S = (Object[]) o;
                for (Object m : S) {
                    this.addServer((String) m);
                }
            }
        }
        return true;
    }

    /*
     * set_ntp, <class 'agent.api.host.linux.Linux'> argument: self - default:
     * None argument: servers - default: None argument: local_time_source -
     * default: None argument: allow_query - default: None // right, can't be
     * set eh
     */
    public Boolean setNtp(List<String> servers, Boolean running)
            throws XmlRpcException {
        if (callWrapper("set_ntp", servers, running) == null) {
            return this.getNtp();
        } else {
            return false;
        }
    }

    /* also cleans the vector */
    public Boolean setNtp(String server, Boolean running)
            throws XmlRpcException {
        this.Servers = new ArrayList<String>();
        this.Servers.add(server);
        return setNtp(this.Servers, running);
    }

    public Boolean setNtp(Boolean running) throws XmlRpcException {
        return setNtp(this.Servers, running);
    }

    /*
     * disable_ntp, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None
     */
    public Boolean disableNtp() throws XmlRpcException {
        if (callWrapper("disable_ntp") == null) {
            return true;
        }
        return false;
    }

    /*
     * enable_ntp, <class 'agent.api.host.linux.Linux'> argument: self -
     * default: None
     */
    public Boolean enableNtp() throws XmlRpcException {
        if (callWrapper("enable_ntp") == null) {
            return true;
        }
        return false;
    }
}
