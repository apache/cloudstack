//
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

package com.cloud.hypervisor.xenserver.resource.wrapper.xenbase;

import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.SetupAnswer;
import com.cloud.agent.api.SetupCommand;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.resource.CommandWrapper;
import com.cloud.resource.ResourceWrapper;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.xensource.xenapi.Bond;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.PIF;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.XenAPIException;

@ResourceWrapper(handles =  SetupCommand.class)
public final class CitrixSetupCommandWrapper extends CommandWrapper<SetupCommand, Answer, CitrixResourceBase> {

    private static final Logger s_logger = Logger.getLogger(CitrixSetupCommandWrapper.class);

    @Override
    public Answer execute(final SetupCommand command, final CitrixResourceBase citrixResourceBase) {
        final Connection conn = citrixResourceBase.getConnection();
        try {
            final Map<Pool, Pool.Record> poolRecs = Pool.getAllRecords(conn);
            if (poolRecs.size() != 1) {
                throw new CloudRuntimeException("There are " + poolRecs.size() + " pool for host :" + citrixResourceBase.getHost().getUuid());
            }
            final Host master = poolRecs.values().iterator().next().master;
            citrixResourceBase.setupServer(conn, master);
            final Host host = Host.getByUuid(conn, citrixResourceBase.getHost().getUuid());
            citrixResourceBase.setupServer(conn, host);

            if (!citrixResourceBase.setIptables(conn)) {
                s_logger.warn("set xenserver Iptable failed");
                return null;
            }

            if (citrixResourceBase.isSecurityGroupEnabled()) {
                final boolean canBridgeFirewall = citrixResourceBase.canBridgeFirewall(conn);
                citrixResourceBase.setCanBridgeFirewall(canBridgeFirewall);
                if (!canBridgeFirewall) {
                    final String msg = "Failed to configure bridge firewall";
                    s_logger.warn(msg);
                    s_logger.warn("Check host " + citrixResourceBase.getHost().getIp() +" for CSP is installed or not and check network mode for bridge");
                    return new SetupAnswer(command, msg);
                }

            }

            final boolean r = citrixResourceBase.launchHeartBeat(conn);
            if (!r) {
                return null;
            }
            citrixResourceBase.cleanupTemplateSR(conn);
            try {
                if (command.useMultipath()) {
                    // the config value is set to true
                    host.addToOtherConfig(conn, "multipathing", "true");
                    host.addToOtherConfig(conn, "multipathhandle", "dmp");
                }

            } catch (final Types.MapDuplicateKey e) {
                s_logger.debug("multipath is already set");
            }

            if (command.needSetup() ) {
                final String result = citrixResourceBase.callHostPlugin(conn, "vmops", "setup_iscsi", "uuid", citrixResourceBase.getHost().getUuid());

                if (!result.contains("> DONE <")) {
                    s_logger.warn("Unable to setup iscsi: " + result);
                    return new SetupAnswer(command, result);
                }

                Pair<PIF, PIF.Record> mgmtPif = null;
                final Set<PIF> hostPifs = host.getPIFs(conn);
                for (final PIF pif : hostPifs) {
                    final PIF.Record rec = pif.getRecord(conn);
                    if (rec.management) {
                        if (rec.VLAN != null && rec.VLAN != -1) {
                            final String msg =
                                    new StringBuilder("Unsupported configuration.  Management network is on a VLAN.  host=").append(citrixResourceBase.getHost().getUuid())
                                    .append("; pif=")
                                    .append(rec.uuid)
                                    .append("; vlan=")
                                    .append(rec.VLAN)
                                    .toString();
                            s_logger.warn(msg);
                            return new SetupAnswer(command, msg);
                        }
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Management network is on pif=" + rec.uuid);
                        }
                        mgmtPif = new Pair<PIF, PIF.Record>(pif, rec);
                        break;
                    }
                }

                if (mgmtPif == null) {
                    final String msg = "Unable to find management network for " + citrixResourceBase.getHost().getUuid();
                    s_logger.warn(msg);
                    return new SetupAnswer(command, msg);
                }

                final Map<Network, Network.Record> networks = Network.getAllRecords(conn);
                if(networks == null) {
                    final String msg = "Unable to setup as there are no networks in the host: " +  citrixResourceBase.getHost().getUuid();
                    s_logger.warn(msg);
                    return new SetupAnswer(command, msg);
                }
                for (final Network.Record network : networks.values()) {
                    if (network.nameLabel.equals("cloud-private")) {
                        for (final PIF pif : network.PIFs) {
                            final PIF.Record pr = pif.getRecord(conn);
                            if (citrixResourceBase.getHost().getUuid().equals(pr.host.getUuid(conn))) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Found a network called cloud-private. host=" + citrixResourceBase.getHost().getUuid() + ";  Network=" + network.uuid + "; pif=" + pr.uuid);
                                }
                                if (pr.VLAN != null && pr.VLAN != -1) {
                                    final String msg =
                                            new StringBuilder("Unsupported configuration.  Network cloud-private is on a VLAN.  Network=").append(network.uuid)
                                            .append(" ; pif=")
                                            .append(pr.uuid)
                                            .toString();
                                    s_logger.warn(msg);
                                    return new SetupAnswer(command, msg);
                                }
                                if (!pr.management && pr.bondMasterOf != null && pr.bondMasterOf.size() > 0) {
                                    if (pr.bondMasterOf.size() > 1) {
                                        final String msg =
                                                new StringBuilder("Unsupported configuration.  Network cloud-private has more than one bond.  Network=").append(network.uuid)
                                                .append("; pif=")
                                                .append(pr.uuid)
                                                .toString();
                                        s_logger.warn(msg);
                                        return new SetupAnswer(command, msg);
                                    }
                                    final Bond bond = pr.bondMasterOf.iterator().next();
                                    final Set<PIF> slaves = bond.getSlaves(conn);
                                    for (final PIF slave : slaves) {
                                        final PIF.Record spr = slave.getRecord(conn);
                                        if (spr.management) {
                                            if (!citrixResourceBase.transferManagementNetwork(conn, host, slave, spr, pif)) {
                                                final String msg =
                                                        new StringBuilder("Unable to transfer management network.  slave=" + spr.uuid + "; master=" + pr.uuid + "; host=" +
                                                                citrixResourceBase.getHost().getUuid()).toString();
                                                s_logger.warn(msg);
                                                return new SetupAnswer(command, msg);
                                            }
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return new SetupAnswer(command, false);

        } catch (final XmlRpcException e) {
            s_logger.warn("Unable to setup", e);
            return new SetupAnswer(command, e.getMessage());
        } catch (final XenAPIException e) {
            s_logger.warn("Unable to setup", e);
            return new SetupAnswer(command, e.getMessage());
        } catch (final Exception e) {
            s_logger.warn("Unable to setup", e);
            return new SetupAnswer(command, e.getMessage());
        }
    }
}
