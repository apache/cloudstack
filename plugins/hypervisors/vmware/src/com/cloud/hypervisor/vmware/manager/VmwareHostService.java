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
package com.cloud.hypervisor.vmware.manager;

import java.util.List;

import com.cloud.agent.api.Command;
import com.cloud.hypervisor.vmware.mo.DatastoreMO;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHost;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.vmware.vim25.ManagedObjectReference;

public interface VmwareHostService {
    VmwareContext getServiceContext(Command cmd);
    void invalidateServiceContext(VmwareContext context);
    VmwareHypervisorHost getHyperHost(VmwareContext context, Command cmd);

    String getWorkerName(VmwareContext context, Command cmd, int workerSequence);

    ManagedObjectReference getVmfsDatastore(VmwareHypervisorHost hyperHost, String datastoreName, String storageIpAddress, int storagePortNumber,
            String iqn, String initiatorChapName, String initiatorChapSecret, String mutualChapName, String mutualChapSecret) throws Exception;
    void createVmdk(Command cmd, DatastoreMO dsMo, String volumeDatastorePath, Long volumeSize) throws Exception;
    void handleDatastoreAndVmdkDetach(String iqn, String storageHost, int storagePort) throws Exception;
    void removeManagedTargetsFromCluster(List<String> managedIqns) throws Exception;
}
