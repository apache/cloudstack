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
package com.cloud.hypervisor.xenserver.resource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.hypervisor.xenserver.resource.wrapper.CitrixRequestWrapper;
import com.cloud.resource.ServerResource;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.utils.ssh.SSHCmdHelper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.PIF;
import com.xensource.xenapi.Types.IpConfigurationMode;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VLAN;

@Local(value = ServerResource.class)
public class XenServer56Resource extends CitrixResourceBase {
    private final static Logger s_logger = Logger.getLogger(XenServer56Resource.class);

    @Override
    public Answer executeRequest(final Command cmd) {

        final CitrixRequestWrapper wrapper = CitrixRequestWrapper.getInstance();
        try {
            return wrapper.execute(cmd, this);
        } catch (final Exception e) {
            return super.executeRequest(cmd);
        }
    }

    @Override
    protected List<File> getPatchFiles() {
        final List<File> files = new ArrayList<File>();
        final String patch = "scripts/vm/hypervisor/xenserver/xenserver56/patch";
        final String patchfilePath = Script.findScript("", patch);
        if (patchfilePath == null) {
            throw new CloudRuntimeException("Unable to find patch file " + patch);
        }
        final File file = new File(patchfilePath);
        files.add(file);

        return files;
    }

    @Override
    public void disableVlanNetwork(final Connection conn, final Network network) {
        try {
            final Network.Record networkr = network.getRecord(conn);
            if (!networkr.nameLabel.startsWith("VLAN")) {
                return;
            }
            final String bridge = networkr.bridge.trim();
            for (final PIF pif : networkr.PIFs) {
                final PIF.Record pifr = pif.getRecord(conn);
                if (!pifr.host.getUuid(conn).equalsIgnoreCase(_host.getUuid())) {
                    continue;
                }

                final VLAN vlan = pifr.VLANMasterOf;
                if (vlan != null) {
                    final String vlannum = pifr.VLAN.toString();
                    final String device = pifr.device.trim();
                    if (vlannum.equals("-1")) {
                        return;
                    }
                    try {
                        vlan.destroy(conn);
                        final Host host = Host.getByUuid(conn, _host.getUuid());
                        host.forgetDataSourceArchives(conn, "pif_" + bridge + "_tx");
                        host.forgetDataSourceArchives(conn, "pif_" + bridge + "_rx");
                        host.forgetDataSourceArchives(conn, "pif_" + device + "." + vlannum + "_tx");
                        host.forgetDataSourceArchives(conn, "pif_" + device + "." + vlannum + "_rx");
                    } catch (final XenAPIException e) {
                        s_logger.trace("Catch " + e.getClass().getName() + ": failed to destory VLAN " + device + " on host " + _host.getUuid() + " due to " + e.toString());
                    }
                }
                return;
            }
        } catch (final XenAPIException e) {
            final String msg = "Unable to disable VLAN network due to " + e.toString();
            s_logger.warn(msg, e);
        } catch (final Exception e) {
            final String msg = "Unable to disable VLAN network due to " + e.getMessage();
            s_logger.warn(msg, e);
        }
    }

    @Override
    public String networkUsage(final Connection conn, final String privateIpAddress, final String option, final String vif) {
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

    public Boolean checkHeartbeat(final String hostuuid) {
        final com.trilead.ssh2.Connection sshConnection = new com.trilead.ssh2.Connection(_host.getIp(), 22);
        try {
            sshConnection.connect(null, 60000, 60000);
            if (!sshConnection.authenticateWithPassword(_username, _password.peek())) {
                throw new CloudRuntimeException("Unable to authenticate");
            }

            final String shcmd = "/opt/cloud/bin/check_heartbeat.sh " + hostuuid + " " + Integer.toString(_heartbeatInterval * 2);
            if (!SSHCmdHelper.sshExecuteCmd(sshConnection, shcmd)) {
                s_logger.debug("Heart beat is gone so dead.");
                return false;
            }
            s_logger.debug("Heart beat is still going");
            return true;
        } catch (final Exception e) {
            s_logger.debug("health check failed due to catch exception " + e.toString());
            return null;
        } finally {
            sshConnection.close();
        }
    }

    @Override
    public boolean transferManagementNetwork(final Connection conn, final Host host, final PIF src, final PIF.Record spr, final PIF dest) throws XmlRpcException, XenAPIException {
        dest.reconfigureIp(conn, spr.ipConfigurationMode, spr.IP, spr.netmask, spr.gateway, spr.DNS);
        Host.managementReconfigure(conn, dest);
        String hostUuid = null;
        final int count = 0;
        while (count < 10) {
            try {
                Thread.sleep(10000);
                hostUuid = host.getUuid(conn);
                if (hostUuid != null) {
                    break;
                }
            } catch (final XmlRpcException e) {
                s_logger.debug("Waiting for host to come back: " + e.getMessage());
            } catch (final XenAPIException e) {
                s_logger.debug("Waiting for host to come back: " + e.getMessage());
            } catch (final InterruptedException e) {
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
        pingXAPI();
        final StartupCommand[] cmds = super.initialize();
        return cmds;
    }

    public XenServer56Resource() {
        super();
    }
}