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

package com.cloud.hypervisor.vmware;

import com.cloud.dc.VsphereStoragePolicy;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.ResourceInUseException;
import com.cloud.storage.StoragePool;
import com.cloud.utils.component.PluggableService;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.command.admin.zone.AddVmwareDcCmd;
import org.apache.cloudstack.api.command.admin.zone.ImportVsphereStoragePoliciesCmd;
import org.apache.cloudstack.api.command.admin.zone.ListVmwareDcsCmd;
import org.apache.cloudstack.api.command.admin.zone.ListVsphereStoragePoliciesCmd;
import org.apache.cloudstack.api.command.admin.zone.ListVsphereStoragePolicyCompatiblePoolsCmd;
import org.apache.cloudstack.api.command.admin.zone.RemoveVmwareDcCmd;
import org.apache.cloudstack.api.command.admin.zone.UpdateVmwareDcCmd;

import java.util.List;

public interface VmwareDatacenterService extends PluggableService {

    VmwareDatacenterVO addVmwareDatacenter(AddVmwareDcCmd cmd) throws IllegalArgumentException, DiscoveryException, ResourceInUseException;

    VmwareDatacenter updateVmwareDatacenter(UpdateVmwareDcCmd updateVmwareDcCmd);

    boolean removeVmwareDatacenter(RemoveVmwareDcCmd cmd) throws IllegalArgumentException, ResourceInUseException;

    List<? extends VmwareDatacenter> listVmwareDatacenters(ListVmwareDcsCmd cmd) throws IllegalArgumentException, CloudRuntimeException;

    List<? extends VsphereStoragePolicy> importVsphereStoragePolicies(ImportVsphereStoragePoliciesCmd cmd);

    List<? extends VsphereStoragePolicy> listVsphereStoragePolicies(ListVsphereStoragePoliciesCmd cmd);

    List<StoragePool> listVsphereStoragePolicyCompatibleStoragePools(ListVsphereStoragePolicyCompatiblePoolsCmd cmd);
}
