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
package com.cloud.hypervisor.xenserver.resource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.VgpuTypesInfo;
import com.cloud.agent.api.to.GPUDeviceTO;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.GPUGroup;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.PGPU;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VGPU;
import com.xensource.xenapi.VGPUType;
import com.xensource.xenapi.VGPUType.Record;
import com.xensource.xenapi.VM;

public class XenServer620SP1Resource extends XenServer620Resource {

    private static final Logger s_logger = Logger.getLogger(XenServer620SP1Resource.class);

    @Override
    protected void fillHostInfo(final Connection conn, final StartupRoutingCommand cmd) {
        super.fillHostInfo(conn, cmd);
        try {
            final HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails = getGPUGroupDetails(conn);
            cmd.setGpuGroupDetails(groupDetails);
            if (groupDetails != null && !groupDetails.isEmpty()) {
                cmd.setHostTags("GPU");
            }
        } catch (final Exception e) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Error while getting GPU device info from host " + cmd.getName(), e);
            }
        }
    }

    @Override
    public HashMap<String, HashMap<String, VgpuTypesInfo>> getGPUGroupDetails(final Connection conn) throws XenAPIException, XmlRpcException {
        final HashMap<String, HashMap<String, VgpuTypesInfo>> groupDetails = new HashMap<String, HashMap<String, VgpuTypesInfo>>();
        final Host host = Host.getByUuid(conn, _host.getUuid());
        final Set<PGPU> pgpus = host.getPGPUs(conn);
        final Iterator<PGPU> iter = pgpus.iterator();
        while (iter.hasNext()) {
            final PGPU pgpu = iter.next();
            final GPUGroup gpuGroup = pgpu.getGPUGroup(conn);
            final Set<VGPUType> enabledVGPUTypes = gpuGroup.getEnabledVGPUTypes(conn);
            final String groupName = gpuGroup.getNameLabel(conn);
            HashMap<String, VgpuTypesInfo> gpuCapacity = new HashMap<String, VgpuTypesInfo>();
            if (groupDetails.get(groupName) != null) {
                gpuCapacity = groupDetails.get(groupName);
            }
            // Get remaining capacity of all the enabled VGPU in a PGPU
            if(enabledVGPUTypes != null) {
                final Iterator<VGPUType> it = enabledVGPUTypes.iterator();
                while (it.hasNext()) {
                    final VGPUType type = it.next();
                    final Record record = type.getRecord(conn);
                    Long remainingCapacity = pgpu.getRemainingCapacity(conn, type);
                    Long maxCapacity = pgpu.getSupportedVGPUMaxCapacities(conn).get(type);
                    VgpuTypesInfo entry;
                    if ((entry = gpuCapacity.get(record.modelName)) != null) {
                        remainingCapacity += entry.getRemainingCapacity();
                        maxCapacity += entry.getMaxCapacity();
                        entry.setRemainingCapacity(remainingCapacity);
                        entry.setMaxVmCapacity(maxCapacity);
                        gpuCapacity.put(record.modelName, entry);
                    } else {
                        final VgpuTypesInfo vgpuTypeRecord = new VgpuTypesInfo(null, record.modelName, record.framebufferSize, record.maxHeads,
                                record.maxResolutionX, record.maxResolutionY, maxCapacity, remainingCapacity, maxCapacity);
                        gpuCapacity.put(record.modelName, vgpuTypeRecord);
                    }
                }
            }
            groupDetails.put(groupName, gpuCapacity);
        }
        return groupDetails;
    }

    @Override
    public void createVGPU(final Connection conn, final StartCommand cmd, final VM vm, final GPUDeviceTO gpuDevice) throws XenAPIException, XmlRpcException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating VGPU of VGPU type [ " + gpuDevice.getVgpuType() + " ] in gpu group" + gpuDevice.getGpuGroup()
                    + " for VM " + cmd.getVirtualMachine().getName());
        }

        final Set<GPUGroup> groups = GPUGroup.getByNameLabel(conn, gpuDevice.getGpuGroup());
        assert groups.size() == 1 : "Should only have 1 group but found " + groups.size();
        final GPUGroup gpuGroup = groups.iterator().next();

        final Set<VGPUType> vgpuTypes = gpuGroup.getEnabledVGPUTypes(conn);
        final Iterator<VGPUType> iter = vgpuTypes.iterator();
        VGPUType vgpuType = null;
        while (iter.hasNext()) {
            final VGPUType entry = iter.next();
            if (entry.getModelName(conn).equals(gpuDevice.getVgpuType())) {
                vgpuType = entry;
            }
        }
        final String device = "0"; // Only allow device = "0" for now, as XenServer supports just a single vGPU per VM.
        final Map<String, String> other_config = new HashMap<String, String>();
        VGPU.create(conn, vm, gpuGroup, device, other_config, vgpuType);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Created VGPU of VGPU type [ " + gpuDevice.getVgpuType() + " ] for VM " + cmd.getVirtualMachine().getName());
        }
        // Calculate and set remaining GPU capacity in the host.
        cmd.getVirtualMachine().getGpuDevice().setGroupDetails(getGPUGroupDetails(conn));
    }
}
