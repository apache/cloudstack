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
package com.cloud.hypervisor.vmware.mo;

import java.util.List;

import com.vmware.vim25.HostInternetScsiHbaStaticTarget;
import com.vmware.vim25.HostStorageDeviceInfo;
import com.vmware.vim25.ManagedObjectReference;

import com.cloud.hypervisor.vmware.util.VmwareContext;

public class HostStorageSystemMO extends BaseMO {
    public HostStorageSystemMO(VmwareContext context, ManagedObjectReference morHostDatastore) {
        super(context, morHostDatastore);
    }

    public HostStorageSystemMO(VmwareContext context, String morType, String morValue) {
        super(context, morType, morValue);
    }

    public HostStorageDeviceInfo getStorageDeviceInfo() throws Exception {
        return (HostStorageDeviceInfo)_context.getVimClient().getDynamicProperty(_mor, "storageDeviceInfo");
    }

    public void addInternetScsiStaticTargets(String iScsiHbaDevice, List<HostInternetScsiHbaStaticTarget> lstTargets) throws Exception {
        _context.getService().addInternetScsiStaticTargets(_mor, iScsiHbaDevice, lstTargets);
    }

    public void removeInternetScsiStaticTargets(String iScsiHbaDevice, List<HostInternetScsiHbaStaticTarget> lstTargets) throws Exception {
        _context.getService().removeInternetScsiStaticTargets(_mor, iScsiHbaDevice, lstTargets);
    }

    public void rescanHba(String iScsiHbaDevice) throws Exception {
        _context.getService().rescanHba(_mor, iScsiHbaDevice);
    }

    public void rescanVmfs() throws Exception {
        _context.getService().rescanVmfs(_mor);
    }
}
