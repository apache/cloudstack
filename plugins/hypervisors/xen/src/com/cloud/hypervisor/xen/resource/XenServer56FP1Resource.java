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


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.FenceAnswer;
import com.cloud.agent.api.FenceCommand;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.resource.ServerResource;
import com.cloud.storage.Volume;
import com.cloud.template.VirtualMachineTemplate.BootloaderType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Console;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VM;
import com.xensource.xenapi.Types.XenAPIException;

@Local(value=ServerResource.class)
public class XenServer56FP1Resource extends XenServer56Resource {
    private static final Logger s_logger = Logger.getLogger(XenServer56FP1Resource.class);
    
    public XenServer56FP1Resource() {
        super();
    }
    
    @Override
    protected String getGuestOsType(String stdType, boolean bootFromCD) {
    	return CitrixHelper.getXenServer56FP1GuestOsType(stdType, bootFromCD);
    }
   
    @Override
    protected List<File> getPatchFiles() {      
        List<File> files = new ArrayList<File>();
        String patch = "scripts/vm/hypervisor/xenserver/xenserver56fp1/patch";    
        String patchfilePath = Script.findScript("" , patch);
        if ( patchfilePath == null ) {
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
            String result = callHostPluginPremium(conn, "check_heartbeat", "host", cmd.getHostGuid(), "interval",
                    Integer.toString(_heartbeatInterval * 2));
            if (!result.contains("> DEAD <")) {
                s_logger.debug("Heart beat is still going so unable to fence");
                return new FenceAnswer(cmd, false, "Heartbeat is still going on unable to fence");
            }

            Set<VM> vms = VM.getByNameLabel(conn, cmd.getVmName());
            for (VM vm : vms) {
                Set<VDI> vdis = new HashSet<VDI>();
                Set<VBD> vbds = vm.getVBDs(conn);
                for( VBD vbd : vbds ) {
                    VDI vdi = vbd.getVDI(conn);
                    if( !isRefNull(vdi) ) {
                        vdis.add(vdi);
                    }
                }
                synchronized (_cluster.intern()) {
                    s_vms.remove(_cluster, _name, vm.getNameLabel(conn));
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

    @Override
    protected VM createVmFromTemplate(Connection conn, VirtualMachineTO vmSpec, Host host) throws XenAPIException, XmlRpcException {
        String guestOsTypeName = getGuestOsType(vmSpec.getOs(), vmSpec.getBootloader() == BootloaderType.CD);
        Set<VM> templates = VM.getByNameLabel(conn, guestOsTypeName);
        assert templates.size() == 1 : "Should only have 1 template but found " + templates.size();
        VM template = templates.iterator().next();

        VM.Record record = template.getRecord(conn);
        record.affinity = host;
        record.otherConfig.remove("disks");
        record.otherConfig.remove("default_template");
        record.isATemplate = false;
        record.nameLabel = vmSpec.getName();
        record.actionsAfterCrash = Types.OnCrashBehaviour.DESTROY;
        record.actionsAfterShutdown = Types.OnNormalExit.DESTROY;
        record.memoryDynamicMax = vmSpec.getMaxRam();
        record.memoryDynamicMin = vmSpec.getMinRam();
        record.memoryStaticMax = vmSpec.getMaxRam();
        record.memoryStaticMin = vmSpec.getMinRam();
        record.VCPUsMax = (long) vmSpec.getCpus();
        record.VCPUsAtStartup = (long) vmSpec.getCpus();
        record.consoles.clear();

        VM vm = VM.create(conn, record);
        VM.Record vmr = vm.getRecord(conn);
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Created VM " + vmr.uuid + " for " + vmSpec.getName());
        }

        Map<String, String> vcpuParams = new HashMap<String, String>();

        Integer speed = vmSpec.getMinSpeed();
        if (speed != null) {

            int cpuWeight = _maxWeight; // cpu_weight
            int utilization = 0; // max CPU cap, default is unlimited

            // weight based allocation
            cpuWeight = (int) ((speed * 0.99) / _host.speed * _maxWeight);
            if (cpuWeight > _maxWeight) {
                cpuWeight = _maxWeight;
            }

            if (vmSpec.getLimitCpuUse()) {
                utilization = (int) ((speed * 0.99) / _host.speed * 100);
            }

            vcpuParams.put("weight", Integer.toString(cpuWeight));
            vcpuParams.put("cap", Integer.toString(utilization));

        }

        if (vcpuParams.size() > 0) {
            vm.setVCPUsParams(conn, vcpuParams);
        }

        String bootArgs = vmSpec.getBootArgs();
        if (bootArgs != null && bootArgs.length() > 0) {
            String pvargs = vm.getPVArgs(conn);
            pvargs = pvargs + vmSpec.getBootArgs().replaceAll(" ", "%");
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("PV args are " + pvargs);
            }
            vm.setPVArgs(conn, pvargs);
        }

        if (!(guestOsTypeName.startsWith("Windows") || guestOsTypeName.startsWith("Citrix") || guestOsTypeName.startsWith("Other"))) {
            if (vmSpec.getBootloader() == BootloaderType.CD) {
                VolumeTO[] disks = vmSpec.getDisks();
                for (VolumeTO disk : disks) {
                    if (disk.getType() == Volume.Type.ISO && disk.getOsType() != null) {
                        String isoGuestOsName = getGuestOsType(disk.getOsType(), vmSpec.getBootloader() == BootloaderType.CD);
                        if (!isoGuestOsName.equals(guestOsTypeName)) {
                            vmSpec.setBootloader(BootloaderType.PyGrub);
                        }
                    }
                }
            }
            if (vmSpec.getBootloader() == BootloaderType.CD) {
                vm.setPVBootloader(conn, "eliloader");
                if (!vm.getOtherConfig(conn).containsKey("install-repository")) {
                    vm.addToOtherConfig(conn, "install-repository", "cdrom");
                }
            } else if (vmSpec.getBootloader() == BootloaderType.PyGrub) {
                vm.setPVBootloader(conn, "pygrub");
            } else {
                vm.destroy(conn);
                throw new CloudRuntimeException("Unable to handle boot loader type: " + vmSpec.getBootloader());
            }
        }
        return vm;
    }

}
