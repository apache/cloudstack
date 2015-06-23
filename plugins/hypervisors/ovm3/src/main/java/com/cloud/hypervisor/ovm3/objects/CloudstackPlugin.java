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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class CloudstackPlugin extends OvmObject {
    private static final Logger LOGGER = Logger
            .getLogger(CloudstackPlugin.class);
    private boolean checkstoragestarted = false;
    public CloudstackPlugin(Connection c) {
        setClient(c);
    }

    public String getVncPort(String vmName) throws Ovm3ResourceException {
        return (String) callWrapper("get_vncport", vmName);
    }

    public boolean ovsUploadSshKey(String key, String content) throws Ovm3ResourceException{
        return nullIsFalseCallWrapper("ovs_upload_ssh_key", key, content);
    }

    public boolean ovsUploadFile(String path, String file, String content) throws Ovm3ResourceException {
        return nullIsFalseCallWrapper("ovs_upload_file", path, file, content);
    }

    public boolean ovsDomrUploadFile(String domr, String path, String file,
            String content) throws Ovm3ResourceException {
        return nullIsFalseCallWrapper("ovs_domr_upload_file", domr, path, file,
                content);
    }

    public static class ReturnCode {
        private Map<String, Object> returnCode = new HashMap<String, Object>() {
            {
                put("rc", null);
                put("exit", null);
                put("err", null);
                put("out", null);
            }
            private static final long serialVersionUID = 5L;
        };
        public ReturnCode() {
        }

        public void setValues(Map<String, String> m) {
            returnCode.putAll(m);
        }

        public Boolean getRc() throws Ovm3ResourceException {
            Object rc = returnCode.get("rc");
            Long c = 1L;
            if (rc instanceof Integer) {
                c = new Long((Integer) rc);
            } else if (rc instanceof Long) {
                c = (Long) rc;
            } else {
                LOGGER.debug("Incorrect return code: " + rc);
                return false;
            }
            returnCode.put("exit", c);
            if (c != 0) {
                return false;
            }
            return true;
        }

        public String getStdOut() {
            return (String) returnCode.get("out");
        }

        public String getStdErr() {
            return (String) returnCode.get("err");
        }

        public Integer getExit() {
            if (returnCode.get("exit") == null) {
                returnCode.put("exit", returnCode.get("rc"));
            }
            return ((Long) returnCode.get("exit")).intValue();
        }
    }

    public ReturnCode domrExec(String ip, String cmd) throws Ovm3ResourceException {
        ReturnCode rc = new ReturnCode();
        rc.setValues((Map<String, String>) callWrapper("exec_domr", ip, cmd));
        return rc;
    }

    /**
     * Checks a tcp port of a host reachable from dom0
     * @param ip
     * @param port
     * @param retries
     * @param interval
     * @return
     * @throws Ovm3ResourceException
     */
    public boolean dom0CheckPort(String ip, Integer port, Integer retries,
            Integer interval) throws Ovm3ResourceException {
        Boolean x = false;
        /* should deduct the interval from the timeout and sleep on it */
        Integer sleep = interval;
        try {
            while (!x && retries > 0) {
                x = nullIsFalseCallWrapper("check_dom0_port", ip, port, interval);
                retries--;
                Thread.sleep(sleep * 1000);
            }
        } catch (Exception e) {
            LOGGER.error("Dom0 port check failed: " + e);
        }
        return x;
    }

    public Map<String, String> ovsDom0Stats(String bridge) throws Ovm3ResourceException {
        return (Map<String, String>) callWrapper(
                "ovs_dom0_stats", bridge);
    }

    public Map<String, String> ovsDomUStats(String domain) throws Ovm3ResourceException {
        return (Map<String, String>) callWrapper(
                "ovs_domU_stats", domain);
    }

    public boolean domrCheckPort(String ip, Integer port) throws Ovm3ResourceException{
        return (Boolean) callWrapper("check_domr_port", ip, port);
    }

    public boolean domrCheckSsh(String ip) throws Ovm3ResourceException {
        return (Boolean) callWrapper("check_domr_ssh", ip);
    }

    public boolean ovsControlInterface(String dev, String cidr) throws Ovm3ResourceException {
        return (Boolean) callWrapper("ovs_control_interface", dev, cidr);
    }

    public boolean ping(String host) throws Ovm3ResourceException {
        return (Boolean) callWrapper("ping", host);
    }

    public boolean ovsCheckFile(String file) throws Ovm3ResourceException {
        return (Boolean) callWrapper("ovs_check_file", file);
    }

    public boolean dom0HasIp(String ovm3PoolVip) throws Ovm3ResourceException {
        return (Boolean) callWrapper("check_dom0_ip", ovm3PoolVip);
    }
    public boolean dom0CheckStorageHealthCheck(String path, String script, String guid, Integer timeout, Integer interval) throws Ovm3ResourceException {
        Object[] x = (Object[]) callWrapper("check_dom0_storage_health_check", path, script, guid, timeout, interval);
        Boolean running = (Boolean) x[0];
        checkstoragestarted = (Boolean) x[1];
        return running;
    }
    public boolean dom0CheckStorageHealthCheck() {
        return checkstoragestarted;
    }
    /* return something else in the future */
    public boolean dom0CheckStorageHealth(String path, String script, String guid, Integer timeout) throws Ovm3ResourceException {
        return (Boolean) callWrapper("check_dom0_storage_health", path, script, guid, timeout);
    }
    public boolean ovsMkdirs(String dir) throws Ovm3ResourceException{
        return nullIsTrueCallWrapper("ovs_mkdirs", dir);
    }
    public boolean ovsMkdirs(String dir, Integer mode) throws Ovm3ResourceException{
        return nullIsTrueCallWrapper("ovs_mkdirs", dir, mode);
    }
}
