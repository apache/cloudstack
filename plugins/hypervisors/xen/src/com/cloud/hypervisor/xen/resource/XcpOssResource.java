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

package com.cloud.hypervisor.xen.resource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.ejb.Local;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;


import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.NetworkUsageAnswer;
import com.cloud.agent.api.NetworkUsageCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.network.Networks.TrafficType;
import com.cloud.resource.ServerResource;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.vm.VirtualMachine;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VM;


@Local(value=ServerResource.class)
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
    protected String getGuestOsType(String stdType, boolean bootFromCD) {
    	if (stdType.equalsIgnoreCase("Debian GNU/Linux 6(64-bit)")) {
    		return "Debian Squeeze 6.0 (64-bit)";
    	} else {
    		return CitrixHelper.getXcpGuestOsType(stdType);
    	}
    }

    protected VBD createPatchVbd(Connection conn, String vmName, VM vm) throws XmlRpcException, XenAPIException {
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
            if(cmd.getOption()!=null && cmd.getOption().equals("create") ){
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
    public StartAnswer execute(StartCommand cmd) {
    	StartAnswer answer = super.execute(cmd);

    	VirtualMachineTO vmSpec = cmd.getVirtualMachine();
    	if (vmSpec.getType() == VirtualMachine.Type.ConsoleProxy) {
    		Connection conn = getConnection();
    		String publicIp = null;
    		for (NicTO nic : vmSpec.getNics()) {
    			if (nic.getType() == TrafficType.Guest) {
    				publicIp = nic.getIp();
    			}
    		}
    		callHostPlugin(conn, "vmops", "setDNATRule", "ip", publicIp, "port", "8443", "add", "true");
    	}

    	return answer;
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
