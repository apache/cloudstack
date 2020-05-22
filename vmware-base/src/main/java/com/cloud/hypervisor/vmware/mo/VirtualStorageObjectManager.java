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

import com.vmware.vim25.ID;
import com.vmware.vim25.VStorageObject;
import org.apache.log4j.Logger;

import com.vmware.vim25.ManagedObjectReference;

import com.cloud.hypervisor.vmware.util.VmwareContext;

public class VirtualStorageObjectManager extends BaseMO {
    @SuppressWarnings("unused")
    private static final Logger s_logger = Logger.getLogger(VirtualStorageObjectManager.class);

    public VirtualStorageObjectManager(VmwareContext context) {
        super(context, context.getServiceContent().getVStorageObjectManager());
    }

    public VirtualStorageObjectManager(VmwareContext context, ManagedObjectReference morDiskMgr) {
        super(context, morDiskMgr);
    }

    public VirtualStorageObjectManager(VmwareContext context, String morType, String morValue) {
        super(context, morType, morValue);
    }

    public VStorageObject registerVirtualDisk(DatastoreFile datastoreFile, String name, String dcName) throws Exception {
        StringBuilder sb = new StringBuilder();
        //https://10.2.2.254/folder/i-2-4-VM/89e3756d9b7444dc92388eb36ddd026b.vmdk?dcPath=datacenter-21&dsName=c84e4af9b6ac33e887a25d9242650091
        sb.append("https://").append(_context.getServerAddress()).append("/folder/");
        sb.append(datastoreFile.getRelativePath());
        sb.append("?dcPath=");
        sb.append(dcName);
        sb.append("&dsName=");
        sb.append(datastoreFile.getDatastoreName());
        return _context.getService().registerDisk(_mor, sb.toString(), name);
    }

    public VStorageObject retrieveVirtualDisk (ID id, ManagedObjectReference morDS) throws Exception {
        return _context.getService().retrieveVStorageObject(_mor, id, morDS);
    }
}
