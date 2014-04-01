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
package com.cloud.hypervisor.xen.resource;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;

import org.apache.cloudstack.hypervisor.xenserver.XenserverConfigs;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetGPUStatsAnswer;
import com.cloud.agent.api.GetGPUStatsCommand;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.to.GPUDeviceTO;
import com.cloud.resource.ServerResource;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.GPUGroup;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.PGPU;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VGPU;
import com.xensource.xenapi.VGPUType;
import com.xensource.xenapi.VM;

@Local(value=ServerResource.class)
public class XenServer620SP1Resource extends XenServer620Resource {
    private static final Logger s_logger = Logger.getLogger(XenServer620SP1Resource.class);

    public XenServer620SP1Resource() {
        super();
    }

    @Override
    public Answer executeRequest(Command cmd) {
        Class<? extends Command> clazz = cmd.getClass();
        if (clazz == GetGPUStatsCommand.class) {
            return execute((GetGPUStatsCommand) cmd);
        } else {
            return super.executeRequest(cmd);
        }
    }

    protected GetGPUStatsAnswer execute(GetGPUStatsCommand cmd) {
        Connection conn = getConnection();
        HashMap<String, HashMap<String, Long>> groupDetails = new HashMap<String, HashMap<String, Long>>();
        try {
            groupDetails = getGPUGroupDetails(conn);
        } catch (Exception e) {
            String msg = "Unable to get GPU stats" + e.toString();
            s_logger.warn(msg, e);
        }
        return new GetGPUStatsAnswer(cmd, groupDetails);
    }

    @Override
    protected void fillHostInfo(Connection conn, StartupRoutingCommand cmd) {
        super.fillHostInfo(conn, cmd);
        Map<String, String> details = cmd.getHostDetails();
        details.put("XS620HotFix", XenserverConfigs.XSHotFix62ESP1);
        cmd.setHostDetails(details);
        try {
            HashMap<String, HashMap<String, Long>> groupDetails = getGPUGroupDetails(conn);
            cmd.setGpuGroupDetails(groupDetails);
        } catch (Exception e) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Error while getting GPU device info from host " + cmd.getName(), e);
            }
        }
    }

    @Override
    protected HashMap<String, HashMap<String, Long>> getGPUGroupDetails(Connection conn) throws XenAPIException, XmlRpcException {
        HashMap<String, HashMap<String, Long>> groupDetails = new HashMap<String, HashMap<String, Long>>();
        Host host = Host.getByUuid(conn, _host.uuid);
        Set<PGPU> pgpus = host.getPGPUs(conn);
        Iterator<PGPU> iter = pgpus.iterator();
        while (iter.hasNext()) {
            PGPU pgpu = iter.next();
            GPUGroup gpuGroup = pgpu.getGPUGroup(conn);
            Set<VGPUType> enabledVGPUTypes = gpuGroup.getEnabledVGPUTypes(conn);
            String groupName = gpuGroup.getNameLabel(conn);
            HashMap<String, Long> gpuCapacity = new HashMap<String, Long>();
            if (groupDetails.get(groupName) != null) {
                gpuCapacity = groupDetails.get(groupName);
            }
            // Get remaining capacity of all the enabled VGPU in a PGPU
            if(enabledVGPUTypes != null) {
                Iterator<VGPUType> it = enabledVGPUTypes.iterator();
                while (it.hasNext()) {
                    VGPUType type = it.next();
                    String modelName = type.getModelName(conn);
                    Long remainingCapacity = pgpu.getRemainingCapacity(conn, type);
                    if (gpuCapacity.get(modelName) != null) {
                        long newRemainingCapacity = gpuCapacity.get(modelName) + remainingCapacity;
                        gpuCapacity.put(modelName, newRemainingCapacity);
                    } else {
                        gpuCapacity.put(modelName, remainingCapacity);
                    }
                }
            }
            groupDetails.put(groupName, gpuCapacity);
        }
        return groupDetails;
    }

    @Override
    protected void createVGPU(Connection conn, StartCommand cmd, VM vm, GPUDeviceTO gpuDevice) throws XenAPIException, XmlRpcException {
        Set<GPUGroup> groups = GPUGroup.getByNameLabel(conn, gpuDevice.getGpuGroup());
        assert groups.size() == 1 : "Should only have 1 group but found " + groups.size();
        GPUGroup gpuGroup = groups.iterator().next();

        Set<VGPUType> vgpuTypes = gpuGroup.getEnabledVGPUTypes(conn);
        Iterator<VGPUType> iter = vgpuTypes.iterator();
        VGPUType vgpuType = null;
        while (iter.hasNext()) {
            VGPUType entry = iter.next();
            if (entry.getModelName(conn).equals(gpuDevice.getVgpuType())) {
                vgpuType = entry;
            }
        }
        String device = "0"; // Only allow device = "0" for now, as XenServer supports just a single vGPU per VM.
        Map<String, String> other_config = new HashMap<String, String>();
        VGPU.create(conn, vm, gpuGroup, device, other_config, vgpuType);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Created VGPU of VGPU type [ " + gpuDevice.getVgpuType() + " ] for VM " + cmd.getVirtualMachine().getName());
        }
        // Calculate and set remaining GPU capacity in the host.
        cmd.getVirtualMachine().getGpuDevice().setGroupDetails(getGPUGroupDetails(conn));
    }

    @Override
    protected String getGuestOsType(String stdType, boolean bootFromCD) {
        return CitrixHelper.getXenServer620SP1GuestOsType(stdType, bootFromCD);
    }

    @Override
    protected List<File> getPatchFiles() {
        List<File> files = new ArrayList<File>();
        String patch = "scripts/vm/hypervisor/xenserver/xenserver60/patch";
        String patchfilePath = Script.findScript("", patch);
        if (patchfilePath == null) {
            throw new CloudRuntimeException("Unable to find patch file " + patch);
        }
        File file = new File(patchfilePath);
        files.add(file);
        return files;
    }

    @Override
    public long getStaticMax(String os, boolean b, long dynamicMinRam, long dynamicMaxRam){
        long recommendedValue = CitrixHelper.getXenServer620SP1StaticMax(os, b);
        if(recommendedValue == 0){
            s_logger.warn("No recommended value found for dynamic max, setting static max and dynamic max equal");
            return dynamicMaxRam;
        }
        long staticMax = Math.min(recommendedValue, 4l * dynamicMinRam);  // XS constraint for stability
        if (dynamicMaxRam > staticMax){ // XS contraint that dynamic max <= static max
            s_logger.warn("dynamixMax " + dynamicMaxRam + " cant be greater than static max " + staticMax + ", can lead to stability issues. Setting static max as much as dynamic max ");
            return dynamicMaxRam;
        }
        return staticMax;
    }

    @Override
    public long getStaticMin(String os, boolean b, long dynamicMinRam, long dynamicMaxRam){
        long recommendedValue = CitrixHelper.getXenServer620SP1StaticMin(os, b);
        if(recommendedValue == 0){
            s_logger.warn("No recommended value found for dynamic min");
            return dynamicMinRam;
        }

        if(dynamicMinRam < recommendedValue){   // XS contraint that dynamic min > static min
            s_logger.warn("Vm is set to dynamixMin " + dynamicMinRam + " less than the recommended static min " + recommendedValue + ", could lead to stability issues");
        }
        return dynamicMinRam;
    }
}
