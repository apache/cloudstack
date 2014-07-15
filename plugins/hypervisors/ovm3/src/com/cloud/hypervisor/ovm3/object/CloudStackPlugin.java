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
import java.util.HashMap;
import java.util.Map;

import org.apache.xmlrpc.XmlRpcException;

public class CloudStackPlugin extends OvmObject {

    public CloudStackPlugin(Connection c) {
        client = c;
    }

    public String getVncPort(String vmName) throws XmlRpcException {
        String x = (String) callWrapper("get_vncport", vmName);
        return x;
    }

    public boolean ovsUploadSshKey(String key, String content)
            throws XmlRpcException {
        Object x = callWrapper("ovs_upload_ssh_key", key, content);
        if (x==null) {
            return false;
        }
        return true;
    }

    public class ReturnCode {
        public ReturnCode() {
        }

        private Map<String, Object> _rc = new HashMap<String, Object>() {
            {
                put("rc", null);
                put("exit", null);
                put("err", null);
                put("out", null);
            }
        };
        public void setValues(Map<String, String> m) {
            this._rc.putAll(m);
        }
        public Boolean getRc() {
            try {
                Long rc = (Long) _rc.get("rc");
                _rc.put("exit", rc);
                if (rc != 0)
                    return false;
                return true;
            } catch (Exception e) {

            }
            return false;
        }
        public String getStdOut() {
            return (String) _rc.get("out");
        }
        public String getStdErr() {
            return (String) _rc.get("err");
        }
        public Integer getExit() {
            if (_rc.get("exit") == null)
                _rc.put("exit", _rc.get("rc"));
            return Integer.valueOf((String) _rc.get("exit"));
        }
    }
    public ReturnCode domrExec(String ip, String cmd) throws XmlRpcException {
        ReturnCode rc = new ReturnCode();
        rc.setValues((Map<String, String>) callWrapper("exec_domr", ip, cmd));
        return rc;
    }

    public boolean domrCheckPort(String ip, Integer port, Integer retries, Integer interval)
                throws XmlRpcException, InterruptedException {
        Boolean x= false;
        /* should deduct the interval from the timeout and sleep on it */
        Integer sleep=interval;
        while(x == false && retries > 0) {
            x = (Boolean) callWrapper("check_domr_port", ip, port, interval);
            retries--;
            Thread.sleep(sleep * 1000);
        }
        return x;
    }

    public Map<String, String> ovsDom0Stats(String bridge) throws XmlRpcException {
        Map<String, String> stats  = (Map<String, String>)
                callWrapper("ovs_dom0_stats", bridge);
        return stats;
    }


    public Map<String, String> ovsDomUStats(String domain) throws XmlRpcException {
        Map<String, String> stats  = (Map<String, String>)
                callWrapper("ovs_domU_stats", domain);
        return stats;
    }
    public boolean domrCheckPort(String ip, Integer port) throws XmlRpcException {
        Object x = callWrapper("check_domr_port", ip, port);
        return (Boolean) x;
    }

    public boolean domrCheckSsh(String ip) throws XmlRpcException {
        Object x = callWrapper("check_domr_ssh", ip);
        return (Boolean) x;
    }

    public boolean ovsControlInterface(String dev, String ip, String mask) throws XmlRpcException {
        Object x = callWrapper("ovs_control_interface", dev, ip, mask);
        return (Boolean) x;
    }

    public boolean ping(String host) throws XmlRpcException {
        Object x = callWrapper("ping", host);
        return (Boolean) x;
    }

    public boolean ovsCheckFile(String file) throws XmlRpcException {
        Object x = callWrapper("ovs_check_file", file);
        return (Boolean) x;
    }
}
