/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.network.resource;

import java.util.Map;
import java.util.UUID;

import javax.naming.ConfigurationException;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;
import org.libvirt.LibvirtException;
import org.midonet.client.MidonetApi;
import org.midonet.client.resource.Bridge;
import org.midonet.client.resource.BridgePort;
import org.midonet.client.resource.Host;

import com.sun.jersey.core.util.MultivaluedMapImpl;

import com.cloud.agent.api.to.NicTO;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.kvm.resource.LibvirtVMDef;
import com.cloud.hypervisor.kvm.resource.VifDriverBase;
import com.cloud.network.Networks;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

public class MidoNetVifDriver extends VifDriverBase {

    private static final Logger s_logger = Logger.getLogger(MidoNetVifDriver.class);
    private int _timeout;
    private String _midoApiLocation = "http://localhost:8081/";
    private static final String midoPostfix = "mnet";

    @Override
    public void configure(Map<String, Object> params) throws ConfigurationException {

        super.configure(params);

        String value = (String)params.get("scripts.timeout");
        _timeout = NumbersUtil.parseInt(value, 30 * 60) * 1000;

        // Load Midonet API server location
        String midoLoc = (String)params.get("midonet.apiserver.address");
        if (midoLoc != null) {
            _midoApiLocation = midoLoc;
        }
    }

    /*
     * Grab our host id in a file written by Midonet, then
     * return a Host.
     */
    private Host getMyHost(MidonetApi api) {
        Script command = new Script("/bin/bash", _timeout);
        command.add("-c");
        command.add("awk -F'=' '{if ($1~/host/) print $2}' /etc/midolman/host_uuid.properties");

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        command.execute(parser);
        String host_uuid = parser.getLines().split("\\n")[0];
        for (Host host : api.getHosts()) {
            if (host.getId().toString().equals(host_uuid)) {
                return host;
            }
        }
        return null;
    }

    /*
     * simple script to add the tap to the host and bring it up.
     */
    private String addTap() {
        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        Script command = new Script("/bin/bash", _timeout);
        command.add("-c");
        command.add("ip tuntap add mode tap dev '%d" + midoPostfix + "' && ip link | grep " + midoPostfix + " | sort -n | tail -1 | awk -F': ' '{print $2}'");
        command.execute(parser);
        String tapName = parser.getLines().split("\\n")[0];
        command = new Script("/bin/bash", _timeout);
        command.add("-c");
        command.add("ip link set " + tapName + " up");
        command.execute();
        return tapName;
    }

    @Override
    public LibvirtVMDef.InterfaceDef plug(NicTO nic, String guestOsType) throws InternalErrorException, LibvirtException {

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("nic=" + nic);
        }

        LibvirtVMDef.InterfaceDef intf = new LibvirtVMDef.InterfaceDef();

        String trafficLabel = nic.getName();

        if (nic.getBroadcastType() == Networks.BroadcastDomainType.Mido && (nic.getType() == Networks.TrafficType.Guest || nic.getType() == Networks.TrafficType.Public)) {
            /*
            * create the tap.
            */
            String tapName = addTap();

            /*
            * grab the tenant id and the network id from the Broadcast URI.
            * We need to pluck the values out of the String. The string
            * should look like "mido://[tenant_id].[bridge_name]"
            */
            MultivaluedMap qNet = new MultivaluedMapImpl();
            String nicAuthority = nic.getBroadcastUri().getAuthority();
            String tenantId = nicAuthority.split("\\.")[0];
            qNet.add("tenant_id", tenantId);
            String url = nicAuthority.split("\\.")[1];
            String netName = url.split(":")[0];

            MidonetApi api = new MidonetApi(_midoApiLocation);
            api.enableLogging();

            for (Bridge b : api.getBridges(qNet)) {
                if (b.getName().equals(netName)) {
                    for (BridgePort p : b.getPorts()) {
                        UUID pvif = p.getVifId();
                        if (pvif != null && p.getVifId().toString().equals(nic.getUuid())) {
                            getMyHost(api).addHostInterfacePort().interfaceName(tapName).portId(p.getId()).create();
                            break;
                        }
                    }
                }
            }

            intf.defEthernet(tapName, nic.getMac(), getGuestNicModel(guestOsType), "");

        } else {
            throw new InternalErrorException("Only NICs of BroadcastDomain type Mido are supported by the MidoNetVifDriver");
        }

        return intf;
    }

    @Override
    public void unplug(LibvirtVMDef.InterfaceDef iface) {
        String netName = iface.getBrName();

        if (netName != null && netName.contains(midoPostfix)) {
            Script command = new Script("/bin/bash", _timeout);
            command.add("-c");
            command.add("ip tuntap del " + iface.getBrName() + " mode tap");
            command.execute();
        }
    }
}
