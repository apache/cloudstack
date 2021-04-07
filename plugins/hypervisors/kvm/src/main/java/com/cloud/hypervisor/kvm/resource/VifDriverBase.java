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

package com.cloud.hypervisor.kvm.resource;

import java.util.Map;

import javax.naming.ConfigurationException;

import org.libvirt.LibvirtException;

import com.cloud.agent.api.to.NicTO;
import com.cloud.exception.InternalErrorException;

public abstract class VifDriverBase implements VifDriver {

    protected LibvirtComputingResource _libvirtComputingResource;
    protected Map<String, String> _pifs;
    protected Map<String, String> _bridges;

    @Override
    public void configure(Map<String, Object> params) throws ConfigurationException {
        _libvirtComputingResource = (LibvirtComputingResource)params.get("libvirt.computing.resource");
        _bridges = (Map<String, String>)params.get("libvirt.host.bridges");
        _pifs = (Map<String, String>)params.get("libvirt.host.pifs");
    }

    @Override
    public abstract LibvirtVMDef.InterfaceDef plug(NicTO nic, String guestOsType, String nicAdapter, Map<String, String> extraConfig) throws InternalErrorException, LibvirtException;

    @Override
    public abstract void unplug(LibvirtVMDef.InterfaceDef iface, boolean deleteBr);

    protected LibvirtVMDef.InterfaceDef.NicModel getGuestNicModel(String platformEmulator, String nicAdapter) {
        // if nicAdapter is found in ENUM, use it. Otherwise, match guest OS type as before
        if (nicAdapter != null && !nicAdapter.isEmpty()) {
            for (LibvirtVMDef.InterfaceDef.NicModel model : LibvirtVMDef.InterfaceDef.NicModel.values()) {
                if (model.toString().equalsIgnoreCase(nicAdapter)) {
                    return model;
                }
            }
        }

        if (_libvirtComputingResource.isGuestPVEnabled(platformEmulator)) {
            return LibvirtVMDef.InterfaceDef.NicModel.VIRTIO;
        } else {
            return LibvirtVMDef.InterfaceDef.NicModel.E1000;
        }
    }

    public boolean isExistingBridge(String bridgeName) {
        return false;
    }
}
