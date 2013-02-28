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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.vmware.vim25.DVPortgroupConfigSpec;
import com.vmware.vim25.ManagedObjectReference;

public class DistributedVirtualSwitchMO extends BaseMO {
    private static final Logger s_logger = Logger.getLogger(DistributedVirtualSwitchMO.class);

    public DistributedVirtualSwitchMO(VmwareContext context, ManagedObjectReference morDvs) {
        super(context, morDvs);
    }

    public DistributedVirtualSwitchMO(VmwareContext context, String morType, String morValue) {
        super(context, morType, morValue);
    }

    public void createDVPortGroup(DVPortgroupConfigSpec dvPortGroupSpec) throws Exception {
        List<DVPortgroupConfigSpec> dvPortGroupSpecArray = new ArrayList<DVPortgroupConfigSpec>();
        dvPortGroupSpecArray.add(dvPortGroupSpec);
        _context.getService().addDVPortgroupTask(_mor, dvPortGroupSpecArray);
    }

    public void updateDvPortGroup(ManagedObjectReference dvPortGroupMor, DVPortgroupConfigSpec dvPortGroupSpec) throws Exception {
        // TODO(sateesh): Update numPorts
        _context.getService().reconfigureDVPortgroupTask(dvPortGroupMor, dvPortGroupSpec);
    }
}
