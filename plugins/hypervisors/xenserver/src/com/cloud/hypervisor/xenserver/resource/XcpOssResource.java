// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Set;

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VM;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.resource.ServerResource;
import com.cloud.storage.Storage;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

@Local(value = ServerResource.class)
public class XcpOssResource extends CitrixResourceBase {
    private final static Logger s_logger = Logger.getLogger(XcpOssResource.class);
    private static final long mem_32m = 33554432L;

    @Override
    protected List<File> getPatchFiles() {
        List<File> files = new ArrayList<File>();
        String patch = "scripts/vm/hypervisor/xenserver/xcposs/patch";
        String patchfilePath = Script.findScript("", patch);
        if (patchfilePath == null) {
            throw new CloudRuntimeException("Unable to find patch file " + patch);
        }
        File file = new File(patchfilePath);
        files.add(file);
        return files;
    }

    @Override
    protected void fillHostInfo(Connection conn, StartupRoutingCommand cmd) {
        super.fillHostInfo(conn, cmd);
        cmd.setCaps(cmd.getCapabilities() + " , hvm");
    }

    @Override
    protected boolean launchHeartBeat(Connection conn) {
        return true;
    }

    @Override
    protected StartupStorageCommand initializeLocalSR(Connection conn) {
        SR extsr = getLocalEXTSR(conn);
        if (extsr != null) {
            try {
                String extuuid = extsr.getUuid(conn);
                _host.localSRuuid = extuuid;
                long cap = extsr.getPhysicalSize(conn);
                if (cap > 0) {
                    long avail = cap - extsr.getPhysicalUtilisation(conn);
                    String name = "Cloud Stack Local EXT Storage Pool for " + _host.uuid;
                    extsr.setNameDescription(conn, name);
                    Host host = Host.getByUuid(conn, _host.uuid);
                    String address = host.getAddress(conn);
                    StoragePoolInfo pInfo = new StoragePoolInfo(extsr.getNameLabel(conn), address, SRType.EXT.toString(), SRType.EXT.toString(), Storage.StoragePoolType.EXT, cap, avail);
                    StartupStorageCommand cmd = new StartupStorageCommand();
                    cmd.setPoolInfo(pInfo);
                    cmd.setGuid(_host.uuid);
                    cmd.setDataCenter(Long.toString(_dcId));
                    cmd.setResourceType(Storage.StorageResourceType.STORAGE_POOL);
                    return cmd;
                }
            } catch (XenAPIException e) {
                String msg = "build local EXT info err in host:" + _host.uuid + e.toString();
                s_logger.warn(msg);
            } catch (XmlRpcException e) {
                String msg = "build local EXT info err in host:" + _host.uuid + e.getMessage();
                s_logger.warn(msg);
            }
        }
        return null;
    }

    @Override
    protected String getGuestOsType(String stdType, String platformEmulator, boolean bootFromCD) {
        if (stdType.equalsIgnoreCase("Debian GNU/Linux 6(64-bit)")) {
            return "Debian Squeeze 6.0 (64-bit)";
        } else if (stdType.equalsIgnoreCase("CentOS 5.6 (64-bit)")) {
            return "CentOS 5 (64-bit)";
        } else {
            return super.getGuestOsType(stdType, platformEmulator, bootFromCD);
        }
    }

    @Override
    protected synchronized VBD createPatchVbd(Connection conn, String vmName, VM vm) throws XmlRpcException, XenAPIException {
        if (_host.localSRuuid != null) {
            //create an iso vdi on it
            String result = callHostPlugin(conn, "vmops", "createISOVHD", "uuid", _host.localSRuuid);
            if (result == null || result.equalsIgnoreCase("Failed")) {
                throw new CloudRuntimeException("can not create systemvm vdi");
            }

            Set<VDI> vdis = VDI.getByNameLabel(conn, "systemvm-vdi");
            if (vdis.size() != 1) {
                throw new CloudRuntimeException("can not find systemvmiso");
            }
            VDI systemvmVDI = vdis.iterator().next();

            VBD.Record cdromVBDR = new VBD.Record();
            cdromVBDR.VM = vm;
            cdromVBDR.empty = false;
            cdromVBDR.bootable = false;
            cdromVBDR.userdevice = "3";
            cdromVBDR.mode = Types.VbdMode.RO;
            cdromVBDR.type = Types.VbdType.DISK;
            cdromVBDR.VDI = systemvmVDI;
            VBD cdromVBD = VBD.create(conn, cdromVBDR);
            return cdromVBD;
        } else {
            throw new CloudRuntimeException("can not find local sr");
        }
    }

    protected NetworkUsageAnswer execute(NetworkUsageCommand cmd) {
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

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof NetworkUsageCommand) {
            return execute((NetworkUsageCommand) cmd);
        } else {
            return super.executeRequest(cmd);
        }
    }

    @Override
    public StopAnswer execute(StopCommand cmd) {
        StopAnswer answer = super.execute(cmd);
        String vmName = cmd.getVmName();
        if (vmName.startsWith("v-")) {
            Connection conn = getConnection();
            callHostPlugin(conn, "vmops", "setDNATRule", "add", "false");
        }
        return answer;
    }

    @Override
    protected void setMemory(Connection conn, VM vm, long minMemsize, long maxMemsize) throws XmlRpcException, XenAPIException {
        vm.setMemoryLimits(conn, mem_32m, maxMemsize, minMemsize, maxMemsize);
    }
}
