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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.StartupCommand;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SSHCmdHelper;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.PIF;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VLAN;

public class XenServer56Resource extends CitrixResourceBase {
    private final static Logger s_logger = Logger.getLogger(XenServer56Resource.class);

    @Override
    protected String getPatchFilePath() {
        return "scripts/vm/hypervisor/xenserver/xenserver56/patch";
    }

    @Override
    public void disableVlanNetwork(final Connection conn, final Network network, boolean deleteVlan) {
        try {
            final Network.Record networkr = network.getRecord(conn);
            if (!networkr.nameLabel.startsWith("VLAN")) {
                return;
            }
            if (deleteVlan) {
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
                            s_logger.trace("Catch " + e.getClass().getName() + ": failed to destroy VLAN " + device + " on host " + _host.getUuid() + " due to " + e.toString());
                        }
                    }
                    return;
                }
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
    public String networkUsage(final Connection conn, final String privateIpAddress, final String option, final String vif, final String publicIp) {
        String args = "";
        if (option.equals("get")) {
            args += "-g";
            if (StringUtils.isNotEmpty(publicIp)) {
                args += " -l " + publicIp;
            }
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
    public StartupCommand[] initialize() {
        pingXAPI();
        return super.initialize();
    }
}
