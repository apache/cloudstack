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

import org.apache.log4j.Logger;

public class CloudStackPlugin extends OvmObject {
    private static final Logger LOGGER = Logger
            .getLogger(CloudStackPlugin.class);

    public CloudStackPlugin(Connection c) {
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
        };
        public ReturnCode() {
        }

        public void setValues(Map<String, String> m) {
            returnCode.putAll(m);
        }

        public Boolean getRc() {
            try {
                Long rc = (Long) returnCode.get("rc");
                returnCode.put("exit", rc);
                if (rc != 0) {
                    return false;
                }
                return true;
            } catch (Exception e) {
                LOGGER.error("Unable to get return code!", e);
            }
            return false;
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
            return Integer.valueOf((String) returnCode.get("exit"));
        }
    }

    public ReturnCode domrExec(String ip, String cmd) throws Ovm3ResourceException {
        ReturnCode rc = new ReturnCode();
        rc.setValues((Map<String, String>) callWrapper("exec_domr", ip, cmd));
        return rc;
    }

    public boolean domrCheckPort(String ip, Integer port, Integer retries,
            Integer interval) throws Ovm3ResourceException {
        Boolean x = false;
        /* should deduct the interval from the timeout and sleep on it */
        Integer sleep = interval;
        while (!x && retries > 0) {
            x = (Boolean) callWrapper("check_domr_port", ip, port, interval);
            retries--;
            try {
                Thread.sleep(sleep * 1000);
            } catch (InterruptedException e) {
                LOGGER.info("Domr port check interrupted: " + e.getMessage());
            }
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
        Object x = callWrapper("check_domr_port", ip, port);
        return (Boolean) x;
    }

    public boolean domrCheckSsh(String ip) throws Ovm3ResourceException {
        Object x = callWrapper("check_domr_ssh", ip);
        return (Boolean) x;
    }

    public boolean ovsControlInterface(String dev, String ip, String mask) throws Ovm3ResourceException {
        Object x = callWrapper("ovs_control_interface", dev, ip, mask);
        return (Boolean) x;
    }

    public boolean ping(String host) throws Ovm3ResourceException {
        Object x = callWrapper("ping", host);
        return (Boolean) x;
    }

    public boolean ovsCheckFile(String file) throws Ovm3ResourceException {
        Object x = callWrapper("ovs_check_file", file);
        return (Boolean) x;
    }
}
