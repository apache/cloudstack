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

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.StoragePodSummary;
import org.apache.log4j.Logger;

import java.util.List;

public class StoragepodMO extends BaseMO {

    private static final Logger LOGGER = Logger.getLogger(StoragepodMO.class);

    public StoragepodMO(VmwareContext context, ManagedObjectReference mor) {
        super(context, mor);
    }

    public StoragepodMO(VmwareContext context, String morType, String morValue) {
        super(context, morType, morValue);
    }

    public StoragePodSummary getDatastoreClusterSummary() throws Exception {
        return (StoragePodSummary)_context.getVimClient().getDynamicProperty(_mor, "summary");
    }

    public List<ManagedObjectReference> getDatastoresInDatastoreCluster() throws Exception {
        List<ManagedObjectReference> datastoresInCluster = _context.getVimClient().getDynamicProperty(_mor, "childEntity");
        return datastoresInCluster;
    }

}
