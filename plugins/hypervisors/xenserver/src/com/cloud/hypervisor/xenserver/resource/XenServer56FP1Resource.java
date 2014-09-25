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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VM;

import com.cloud.agent.api.FenceAnswer;
import com.cloud.agent.api.FenceCommand;
import com.cloud.resource.ServerResource;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

@Local(value = ServerResource.class)
public class XenServer56FP1Resource extends XenServer56Resource {
    private static final long mem_128m = 134217728L;
    private static final Logger s_logger = Logger.getLogger(XenServer56FP1Resource.class);

    public XenServer56FP1Resource() {
        super();
    }

    @Override
    protected List<File> getPatchFiles() {
        List<File> files = new ArrayList<File>();
        String patch = "scripts/vm/hypervisor/xenserver/xenserver56fp1/patch";
        String patchfilePath = Script.findScript("", patch);
        if (patchfilePath == null) {
            throw new CloudRuntimeException("Unable to find patch file " + patch);
        }
        File file = new File(patchfilePath);
        files.add(file);
        return files;
    }

    @Override
    protected FenceAnswer execute(FenceCommand cmd) {
        Connection conn = getConnection();
        try {
            Boolean alive = check_heartbeat(cmd.getHostGuid());
            if ( alive == null ) {
                s_logger.debug("Failed to check heartbeat,  so unable to fence");
                return new FenceAnswer(cmd, false, "Failed to check heartbeat, so unable to fence");
            }
            if ( alive ) {
                s_logger.debug("Heart beat is still going so unable to fence");
                return new FenceAnswer(cmd, false, "Heartbeat is still going on unable to fence");
            }
            Set<VM> vms = VM.getByNameLabel(conn, cmd.getVmName());
            for (VM vm : vms) {
                Set<VDI> vdis = new HashSet<VDI>();
                Set<VBD> vbds = vm.getVBDs(conn);
                for (VBD vbd : vbds) {
                    VDI vdi = vbd.getVDI(conn);
                    if (!isRefNull(vdi)) {
                        vdis.add(vdi);
                    }
                }
                s_logger.info("Fence command for VM " + cmd.getVmName());
                vm.powerStateReset(conn);
                vm.destroy(conn);
                for (VDI vdi : vdis) {
                    Map<String, String> smConfig = vdi.getSmConfig(conn);
                    for (String key : smConfig.keySet()) {
                        if (key.startsWith("host_")) {
                            vdi.removeFromSmConfig(conn, key);
                            break;
                        }
                    }
                }
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

    /**
     * When Dynamic Memory Control (DMC) is enabled -
     * xenserver allows scaling the guest memory while the guest is running
     *
     * This is determined by the 'restrict_dmc' option on the host.
     * When false, scaling is allowed hence DMC is enabled
     */
    @Override
    protected boolean isDmcEnabled(Connection conn, Host host) throws XenAPIException, XmlRpcException {
        Map<String, String> hostParams = host.getLicenseParams(conn);
        Boolean isDmcEnabled = hostParams.get("restrict_dmc").equalsIgnoreCase("false");
        return isDmcEnabled;
    }
}
