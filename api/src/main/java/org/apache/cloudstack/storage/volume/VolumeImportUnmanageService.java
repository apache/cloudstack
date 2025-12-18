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

package org.apache.cloudstack.storage.volume;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.Storage;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.api.command.admin.volume.ListVolumesForImportCmd;
import org.apache.cloudstack.api.command.admin.volume.ImportVolumeCmd;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.VolumeForImportResponse;
import org.apache.cloudstack.api.response.VolumeResponse;

import java.util.Arrays;
import java.util.List;

public interface VolumeImportUnmanageService extends PluggableService {

     List<Hypervisor.HypervisorType> SUPPORTED_HYPERVISORS =
            Arrays.asList(Hypervisor.HypervisorType.KVM, Hypervisor.HypervisorType.VMware);

    List<Storage.StoragePoolType> SUPPORTED_STORAGE_POOL_TYPES_FOR_KVM = Arrays.asList(Storage.StoragePoolType.NetworkFilesystem,
            Storage.StoragePoolType.Filesystem, Storage.StoragePoolType.RBD);

    ListResponse<VolumeForImportResponse> listVolumesForImport(ListVolumesForImportCmd cmd);

    VolumeResponse importVolume(ImportVolumeCmd cmd);

    boolean unmanageVolume(long volumeId);

}
