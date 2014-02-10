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
package com.cloud.hypervisor.xen.resource;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckOnHostAnswer;
import com.cloud.agent.api.CheckOnHostCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.FenceAnswer;
import com.cloud.agent.api.FenceCommand;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.resource.ServerResource;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.PIF;
import com.xensource.xenapi.Types.IpConfigurationMode;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VLAN;
import com.xensource.xenapi.VM;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import javax.ejb.Local;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Local(value = ServerResource.class)
public class XenServer56Resource extends CitrixResourceBase {
    private final static Logger s_logger = Logger.getLogger(XenServer56Resource.class);

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof FenceCommand) {
            return execute((FenceCommand)cmd);
        } else if (cmd instanceof NetworkUsageCommand) {
            return execute((NetworkUsageCommand)cmd);
        } else {
            return super.executeRequest(cmd);
        }
    }

    @Override
    protected String getGuestOsType(String stdType, boolean bootFromCD) {
        return CitrixHelper.getXenServerGuestOsType(stdType, bootFromCD);
    }

    @Override
    protected List<File> getPatchFiles() {
        List<File> files = new ArrayList<File>();
        String patch = "scripts/vm/hypervisor/xenserver/xenserver56/patch";
        String patchfilePath = Script.findScript("", patch);
        if (patchfilePath == null) {
            throw new CloudRuntimeException("Unable to find patch file " + patch);
        }
        File file = new File(patchfilePath);
        files.add(file);

        return files;
    }

    @Override
    protected void disableVlanNetwork(Connection conn, Network network) {
        try {
            Network.Record networkr = network.getRecord(conn);
            if (!networkr.nameLabel.startsWith("VLAN")) {
                return;
            }
            String bridge = networkr.bridge.trim();
            for (PIF pif : networkr.PIFs) {
                PIF.Record pifr = pif.getRecord(conn);
                if (!pifr.host.getUuid(conn).equalsIgnoreCase(_host.uuid)) {
                    continue;
                }

                VLAN vlan = pifr.VLANMasterOf;
                if (vlan != null) {
                    String vlannum = pifr.VLAN.toString();
                    String device = pifr.device.trim();
                    if (vlannum.equals("-1")) {
                        return;
                    }
                    try {
                        vlan.destroy(conn);
                        Host host = Host.getByUuid(conn, _host.uuid);
                        host.forgetDataSourceArchives(conn, "pif_" + bridge + "_tx");
                        host.forgetDataSourceArchives(conn, "pif_" + bridge + "_rx");
                        host.forgetDataSourceArchives(conn, "pif_" + device + "." + vlannum + "_tx");
                        host.forgetDataSourceArchives(conn, "pif_" + device + "." + vlannum + "_rx");
                    } catch (XenAPIException e) {
                        s_logger.info("Catch " + e.getClass().getName() + ": failed to destory VLAN " + device + " on host " + _host.uuid + " due to " + e.toString());
                    }
                }
                return;
            }
        } catch (XenAPIException e) {
            String msg = "Unable to disable VLAN network due to " + e.toString();
            s_logger.warn(msg, e);
        } catch (Exception e) {
            String msg = "Unable to disable VLAN network due to " + e.getMessage();
            s_logger.warn(msg, e);
        }
    }

    @Override
    protected String networkUsage(Connection conn, final String privateIpAddress, final String option, final String vif) {
        String args = "";
        if (option.equals("get")) {
            args += "-g";
        } else if (option.equals("create")) {
            args += "-c";
        } else if (option.equals("reset")) {
            args += "-r";
        } else if (option.equals("addVif")) {
            args += "-a ";
            args += vif;
        } else if (option.equals("deleteVif")) {
            args += "-d ";
            args += vif;
        }

        return executeInVR(privateIpAddress, "netusage.sh", args).getDetails();
    }

    protected NetworkUsageAnswer VPCNetworkUsage(NetworkUsageCommand cmd) {
        try {
            Connection conn = getConnection();
            String option = cmd.getOption();
            String publicIp = cmd.getGatewayIP();

            String args = " -l " + publicIp + " ";
            if (option.equals("get")) {
                args += "-g";
            } else if (option.equals("create")) {
                args += "-c";
                String vpcCIDR = cmd.getVpcCIDR();
                args += " -v " + vpcCIDR;
            } else if (option.equals("reset")) {
                args += "-r";
            } else if (option.equals("vpn")) {
                args += "-n";
            } else if (option.equals("remove")) {
                args += "-d";
            } else {
                return new NetworkUsageAnswer(cmd, "success", 0L, 0L);
            }

            String result = executeInVR(cmd.getPrivateIP(), "vpc_netusage.sh", args).getDetails();
            if (option.equals("get") || option.equals("vpn")) {
                long[] stats = new long[2];
                if (result != null) {
                    String[] splitResult = result.split(":");
                    int i = 0;
                    while (i < splitResult.length - 1) {
                        stats[0] += (new Long(splitResult[i++])).longValue();
                        stats[1] += (new Long(splitResult[i++])).longValue();
                    }
                    return new NetworkUsageAnswer(cmd, "success", stats[0], stats[1]);
                }
            }
            if (result == null || result.isEmpty()) {
                throw new Exception(" vpc network usage plugin call failed ");
            }
            return new NetworkUsageAnswer(cmd, "success", 0L, 0L);
        } catch (Exception ex) {
            s_logger.warn("Failed to get network usage stats due to ", ex);
            return new NetworkUsageAnswer(cmd, ex);
        }
    }

    protected NetworkUsageAnswer execute(NetworkUsageCommand cmd) {
        if (cmd.isForVpc()) {
            return VPCNetworkUsage(cmd);
        }
        try {
            Connection conn = getConnection();
            if (cmd.getOption() != null && cmd.getOption().equals("create")) {
                String result = networkUsage(conn, cmd.getPrivateIP(), "create", null);
                NetworkUsageAnswer answer = new NetworkUsageAnswer(cmd, result, 0L, 0L);
                return answer;
            }
            long[] stats = getNetworkStats(conn, cmd.getPrivateIP());
            NetworkUsageAnswer answer = new NetworkUsageAnswer(cmd, "", stats[0], stats[1]);
            return answer;
        } catch (Exception ex) {
            s_logger.warn("Failed to get network usage stats due to ", ex);
            return new NetworkUsageAnswer(cmd, ex);
        }
    }

    protected FenceAnswer execute(FenceCommand cmd) {
        Connection conn = getConnection();
        try {
            String result = callHostPluginPremium(conn, "check_heartbeat", "host", cmd.getHostGuid(), "interval", Integer.toString(_heartbeatInterval * 2));
            if (!result.contains("> DEAD <")) {
                s_logger.debug("Heart beat is still going so unable to fence");
                return new FenceAnswer(cmd, false, "Heartbeat is still going on unable to fence");
            }

            Set<VM> vms = VM.getByNameLabel(conn, cmd.getVmName());
            for (VM vm : vms) {
                synchronized (_cluster.intern()) {
                    s_vms.remove(_cluster, _name, vm.getNameLabel(conn));
                }
                s_logger.info("Fence command for VM " + cmd.getVmName());
                vm.powerStateReset(conn);
                vm.destroy(conn);
            }
            return new FenceAnswer(cmd);
        } catch (XmlRpcException e) {
            s_logger.warn("Unable to fence", e);
            return new FenceAnswer(cmd, false, e.getMessage());
        } catch (XenAPIException e) {
            s_logger.warn("Unable to fence", e);
            return new FenceAnswer(cmd, false, e.getMessage());
        }
    }

    @Override
    protected boolean transferManagementNetwork(Connection conn, Host host, PIF src, PIF.Record spr, PIF dest) throws XmlRpcException, XenAPIException {
        dest.reconfigureIp(conn, spr.ipConfigurationMode, spr.IP, spr.netmask, spr.gateway, spr.DNS);
        Host.managementReconfigure(conn, dest);
        String hostUuid = null;
        int count = 0;
        while (count < 10) {
            try {
                Thread.sleep(10000);
                hostUuid = host.getUuid(conn);
                if (hostUuid != null) {
                    break;
                }
            } catch (XmlRpcException e) {
                s_logger.debug("Waiting for host to come back: " + e.getMessage());
            } catch (XenAPIException e) {
                s_logger.debug("Waiting for host to come back: " + e.getMessage());
            } catch (InterruptedException e) {
                s_logger.debug("Gotta run");
                return false;
            }
        }
        if (hostUuid == null) {
            s_logger.warn("Unable to transfer the management network from " + spr.uuid);
            return false;
        }

        src.reconfigureIp(conn, IpConfigurationMode.NONE, null, null, null, null);
        return true;
    }

    @Override
    public StartupCommand[] initialize() {
        pingXenServer();
        StartupCommand[] cmds = super.initialize();
        return cmds;
    }

    @Override
    protected CheckOnHostAnswer execute(CheckOnHostCommand cmd) {
        try {
            Connection conn = getConnection();
            String result = callHostPluginPremium(conn, "check_heartbeat", "host", cmd.getHost().getGuid(), "interval", Integer.toString(_heartbeatInterval * 2));
            if (result == null) {
                return new CheckOnHostAnswer(cmd, "Unable to call plugin");
            }
            if (result.contains("> DEAD <")) {
                s_logger.debug("Heart beat is gone so dead.");
                return new CheckOnHostAnswer(cmd, false, "Heart Beat is done");
            } else if (result.contains("> ALIVE <")) {
                s_logger.debug("Heart beat is still going");
                return new CheckOnHostAnswer(cmd, true, "Heartbeat is still going");
            }
            return new CheckOnHostAnswer(cmd, null, "Unable to determine");
        } catch (Exception e) {
            s_logger.warn("Unable to fence", e);
            return new CheckOnHostAnswer(cmd, e.getMessage());
        }
    }

    public XenServer56Resource() {
        super();
    }

}
