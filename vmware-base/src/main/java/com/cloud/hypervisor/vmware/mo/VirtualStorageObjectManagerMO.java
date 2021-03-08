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
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.VStorageObject;
import com.vmware.vim25.VirtualDiskType;
import com.vmware.vim25.VslmCreateSpec;
import com.vmware.vim25.VslmCreateSpecDiskFileBackingSpec;
import org.apache.log4j.Logger;

import com.vmware.vim25.ManagedObjectReference;

import com.cloud.hypervisor.vmware.util.VmwareContext;

public class VirtualStorageObjectManagerMO extends BaseMO {
    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(VirtualStorageObjectManagerMO.class);

    public VirtualStorageObjectManagerMO(VmwareContext context) {
        super(context, context.getServiceContent().getVStorageObjectManager());
    }

    public VirtualStorageObjectManagerMO(VmwareContext context, ManagedObjectReference morDiskMgr) {
        super(context, morDiskMgr);
    }

    public VirtualStorageObjectManagerMO(VmwareContext context, String morType, String morValue) {
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

    public VStorageObject createDisk(ManagedObjectReference morDS, VirtualDiskType diskType, long currentSizeInBytes, String datastoreFilepath, String filename) throws Exception {
        long currentSizeInMB = currentSizeInBytes/(1024*1024);

        VslmCreateSpecDiskFileBackingSpec diskFileBackingSpec = new VslmCreateSpecDiskFileBackingSpec();
        diskFileBackingSpec.setDatastore(morDS);
        diskFileBackingSpec.setProvisioningType(diskType.value());
        // path should be just the folder name. For example, instead of '[datastore1] folder1/filename.vmdk' you would just do 'folder1'.
        // path is introduced from 6.7. In 6.5 disk will be created in the default folder "fcd"
        diskFileBackingSpec.setPath(null);

        VslmCreateSpec vslmCreateSpec = new VslmCreateSpec();
        vslmCreateSpec.setBackingSpec(diskFileBackingSpec);
        vslmCreateSpec.setCapacityInMB(currentSizeInMB);
        vslmCreateSpec.setName(filename);

        ManagedObjectReference morTask = _context.getService().createDiskTask(_mor, vslmCreateSpec);
        boolean result = _context.getVimClient().waitForTask(morTask);

        VStorageObject vStorageObject = null;
        if (result) {
            _context.waitForTaskProgressDone(morTask);
            //_context.getService().reconcileDatastoreInventoryTask(_mor, morDS);
            TaskInfo taskInfo = TaskMO.getTaskInfo(_context, morTask);
            vStorageObject = (VStorageObject)taskInfo.getResult();

        } else {
            LOGGER.error("VMware CreateDisk_Task failed due to " + TaskMO.getTaskFailureInfo(_context, morTask));
        }

        return vStorageObject;
    }
}
