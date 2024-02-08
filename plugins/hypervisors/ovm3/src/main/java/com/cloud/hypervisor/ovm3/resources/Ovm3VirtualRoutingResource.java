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

package com.cloud.hypervisor.ovm3.resources;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.joda.time.Duration;

import com.cloud.agent.api.SetupGuestNetworkCommand;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.IpAssocVpcCommand;
import com.cloud.agent.api.routing.NetworkElementCommand;
import com.cloud.agent.api.routing.SetSourceNatCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.resource.virtualnetwork.VRScripts;
import com.cloud.agent.resource.virtualnetwork.VirtualRouterDeployer;
import com.cloud.hypervisor.ovm3.objects.CloudstackPlugin;
import com.cloud.hypervisor.ovm3.objects.Connection;
import com.cloud.hypervisor.ovm3.objects.Xen;
import com.cloud.utils.ExecutionResult;

public class Ovm3VirtualRoutingResource implements VirtualRouterDeployer {
    protected Logger logger = LogManager.getLogger(getClass());
    private String domRCloudPath = "/opt/cloud/bin/";
    private Connection c;
    private String agentName;
    public Ovm3VirtualRoutingResource() {
    }
    public Ovm3VirtualRoutingResource(Connection conn) {
        c = conn;
        agentName=c.getIp();
    }
    public void setConnection(Connection conn) {
        c = conn;
    }
    @Override
    public ExecutionResult executeInVR(String routerIp, String script,
            String args) {
        return executeInVR(routerIp, script, args, VRScripts.VR_SCRIPT_EXEC_TIMEOUT);
    }

    @Override
    public ExecutionResult executeInVR(String routerIp, String script,
            String args, Duration timeout) {
        if (!script.contains(domRCloudPath)) {
            script = domRCloudPath + "/" + script;
        }
        String cmd = script + " " + args;
        logger.debug("executeInVR via " + agentName + " on " + routerIp + ": "
                + cmd);
        try {
            CloudstackPlugin cSp = new CloudstackPlugin(c);
            CloudstackPlugin.ReturnCode result;
            result = cSp.domrExec(routerIp, cmd);
            return new ExecutionResult(result.getRc(), result.getStdOut());
        } catch (Exception e) {
            logger.error("executeInVR FAILED via " + agentName + " on "
                    + routerIp + ":" + cmd + ", " + e.getMessage(), e);
        }
        return new ExecutionResult(false, "");
    }

    @Override
    public ExecutionResult createFileInVR(String routerIp, String path,
            String filename, String content) {
        String error = null;
        logger.debug("createFileInVR via " + agentName + " on " + routerIp
                + ": " + path + "/" + filename + ", content: " + content);
        try {
            CloudstackPlugin cSp = new CloudstackPlugin(c);
            boolean result = cSp.ovsDomrUploadFile(routerIp, path, filename,
                    content);
            return new ExecutionResult(result, "");
        } catch (Exception e) {
            error = e.getMessage();
            logger.warn(
                    "createFileInVR failed for " + path + "/" + filename
                            + " in VR " + routerIp + " via " + agentName + ": "
                            + error, e);
        }
        return new ExecutionResult(error == null, error);
    }

    @Override
    public ExecutionResult prepareCommand(NetworkElementCommand cmd) {
        // Update IP used to access router
        cmd.setRouterAccessIp(cmd
                .getAccessDetail(NetworkElementCommand.ROUTER_IP));
        assert cmd.getRouterAccessIp() != null;

        if (cmd instanceof IpAssocVpcCommand) {
            return prepareNetworkElementCommand((IpAssocVpcCommand) cmd);
        } else if (cmd instanceof IpAssocCommand) {
            return prepareNetworkElementCommand((IpAssocCommand) cmd);
        } else if (cmd instanceof SetupGuestNetworkCommand) {
            return prepareNetworkElementCommand((SetupGuestNetworkCommand) cmd);
        } else if (cmd instanceof SetSourceNatCommand) {
            return prepareNetworkElementCommand((SetSourceNatCommand) cmd);
        }
        return new ExecutionResult(true, null);
    }

    @Override
    public ExecutionResult cleanupCommand(NetworkElementCommand cmd) {
        if (cmd instanceof IpAssocCommand
                && !(cmd instanceof IpAssocVpcCommand)) {
            return cleanupNetworkElementCommand((IpAssocCommand) cmd);
        }
        return new ExecutionResult(true, null);
    }

    private ExecutionResult cleanupNetworkElementCommand(IpAssocCommand cmd) {
        return new ExecutionResult(true, null);
    }

    private ExecutionResult prepareNetworkElementCommand(
            SetupGuestNetworkCommand cmd) {
        return new ExecutionResult(true, null);
    }

    private ExecutionResult prepareNetworkElementCommand(IpAssocVpcCommand cmd) {
        return prepNetBoth(cmd
                .getAccessDetail(NetworkElementCommand.ROUTER_NAME),
                cmd.getIpAddresses(), "IpAssocVpcCommand");
    }

    private ExecutionResult prepareNetworkElementCommand(SetSourceNatCommand cmd) {
        return new ExecutionResult(true, null);
    }

    private ExecutionResult prepareNetworkElementCommand(IpAssocCommand cmd) {
        return prepNetBoth(cmd
                .getAccessDetail(NetworkElementCommand.ROUTER_NAME),
                cmd.getIpAddresses(), "IpAssocCommand");
    }

    private ExecutionResult prepNetBoth(String routerName, IpAddressTO[] ips, String type) {
        Xen xen = new Xen(c);
        try {
            Xen.Vm vm = xen.getVmConfig(routerName);
            for (IpAddressTO ip : ips) {
                Integer devId = vm.getVifIdByMac(ip.getVifMacAddress());
                if (devId < 0 && "IpAssocVpcCommand".equals(type)) {
                    String msg = "No valid Nic devId found for " + vm.getVmName()
                            + " with " + ip.getVifMacAddress();
                    logger.error(msg);
                    return new ExecutionResult(false, msg);
                } else if (devId < 0 && "IpAssocCommand".equals(type)) {
                    // vm.get
                    String msg = "No valid Nic devId found for " + vm.getVmName()
                            + " with " + ip.getVifMacAddress() + " "
                            + " Ignoring for now (routervm)";
                    logger.debug(msg);
                    devId=2;
                }
                ip.setNicDevId(devId);
            }
        } catch (Exception e) {
            String msg = type + " failure on applying one ip due to exception:  " + e;
            logger.error(msg);
            return new ExecutionResult(false, msg);
        }
        return new ExecutionResult(true, null);
    }
}
