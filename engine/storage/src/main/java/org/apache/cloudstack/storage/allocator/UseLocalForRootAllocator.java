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
package org.apache.cloudstack.storage.allocator;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.StoragePoolAllocator;

import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.deploy.DeploymentPlanner.ExcludeList;
import com.cloud.storage.StoragePool;
import com.cloud.vm.DiskProfile;
import com.cloud.vm.VirtualMachineProfile;

public class UseLocalForRootAllocator extends LocalStoragePoolAllocator implements StoragePoolAllocator {

    @Inject
    DataCenterDao _dcDao;

    @Override
    public List<StoragePool> allocateToPool(DiskProfile dskCh, VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo) {
        return allocateToPool(dskCh, vmProfile, plan, avoid, returnUpTo, false);
    }

    @Override
    public List<StoragePool> allocateToPool(DiskProfile dskCh, VirtualMachineProfile vmProfile, DeploymentPlan plan, ExcludeList avoid, int returnUpTo, boolean bypassStorageTypeCheck) {
        DataCenterVO dc = _dcDao.findById(plan.getDataCenterId());
        if (!dc.isLocalStorageEnabled()) {
            return null;
        }

        return super.allocateToPool(dskCh, vmProfile, plan, avoid, returnUpTo, bypassStorageTypeCheck);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        return true;
    }

    protected UseLocalForRootAllocator() {
    }
}
