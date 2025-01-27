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

package org.apache.cloudstack.vm;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.component.PluggableService;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import static com.cloud.hypervisor.Hypervisor.HypervisorType.KVM;
import static com.cloud.hypervisor.Hypervisor.HypervisorType.VMware;

public interface UnmanagedVMsManager extends VmImportService, UnmanageVMService, PluggableService, Configurable {

    ConfigKey<Boolean> UnmanageVMPreserveNic = new ConfigKey<>("Advanced", Boolean.class, "unmanage.vm.preserve.nics", "false",
            "If set to true, do not remove VM nics (and its MAC addresses) when unmanaging a VM, leaving them allocated but not reserved. " +
                    "If set to false, nics are removed and MAC addresses can be reassigned", true, ConfigKey.Scope.Zone);

    ConfigKey<Integer> RemoteKvmInstanceDisksCopyTimeout = new ConfigKey<>(Integer.class,
            "remote.kvm.instance.disks.copy.timeout",
            "Advanced",
            "30",
            "Timeout (in mins) to prepare and copy the disks of remote KVM instance while importing the instance from an external host",
            true,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Integer> ConvertVmwareInstanceToKvmTimeout = new ConfigKey<>(Integer.class,
            "convert.vmware.instance.to.kvm.timeout",
            "Advanced",
            "3",
            "Timeout (in hours) for the instance conversion process from VMware through the virt-v2v binary on a KVM host",
            true,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Integer> ThreadsOnMSToImportVMwareVMFiles = new ConfigKey<>(Integer.class,
            "threads.on.ms.to.import.vmware.vm.files",
            "Advanced",
            "0",
            "Threads to use on the management server when importing VM files from VMWare." +
                    " -1 or 1 disables threads, 0 uses a thread per VM disk (disabled for single disk) and >1 for manual thread configuration." +
                    " Max number is 10, Default is 0.",
            true,
            ConfigKey.Scope.Global,
            null);

    ConfigKey<Integer> ThreadsOnKVMHostToImportVMwareVMFiles = new ConfigKey<>(Integer.class,
            "threads.on.kvm.host.to.import.vmware.vm.files",
            "Advanced",
            "0",
            "Threads to use on the KVM host (by the ovftool, if the version is 4.4.0+) when importing VM files from VMWare." +
                    " -1 or 1 disables threads, 0 uses a thread per VM disk (disabled for single disk) and >1 for manual thread configuration." +
                    " Max number is 10, Default is 0.",
            true,
            ConfigKey.Scope.Global,
            null);

    static boolean isSupported(Hypervisor.HypervisorType hypervisorType) {
        return hypervisorType == VMware || hypervisorType == KVM;
    }
}
