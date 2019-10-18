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
package com.cloud.vm;

public interface VmDetailConstants {
    String KEYBOARD = "keyboard";
    String CPU_CORE_PER_SOCKET = "cpu.corespersocket";
    String ROOT_DISK_SIZE = "rootdisksize";
    String BOOT_MODE = "boot.mode";

    // VMware specific
    String NIC_ADAPTER = "nicAdapter";
    String ROOT_DISK_CONTROLLER = "rootDiskController";
    String DATA_DISK_CONTROLLER = "dataDiskController";
    String SVGA_VRAM_SIZE = "svga.vramSize";
    String NESTED_VIRTUALIZATION_FLAG = "nestedVirtualizationFlag";

    // XenServer specific (internal)
    String HYPERVISOR_TOOLS_VERSION = "hypervisortoolsversion";
    String PLATFORM = "platform";
    String TIME_OFFSET = "timeoffset";

    // KVM specific (internal)
    String KVM_VNC_PORT = "kvm.vnc.port";
    String KVM_VNC_ADDRESS = "kvm.vnc.address";

    // Mac OSX guest specific (internal)
    String SMC_PRESENT = "smc.present";
    String FIRMWARE = "firmware";

    // VM deployment with custom compute offering params
    String CPU_NUMBER = "cpuNumber";
    String CPU_SPEED = "cpuSpeed";
    String MEMORY = "memory";

    // Misc details for internal usage (not to be set/changed by user or admin)
    String CPU_OVER_COMMIT_RATIO = "cpuOvercommitRatio";
    String MEMORY_OVER_COMMIT_RATIO = "memoryOvercommitRatio";
    String MESSAGE_RESERVED_CAPACITY_FREED_FLAG = "Message.ReservedCapacityFreed.Flag";
    String DEPLOY_VM = "deployvm";
    String SSH_PUBLIC_KEY = "SSH.PublicKey";
    String PASSWORD = "password";
    String ENCRYPTED_PASSWORD = "Encrypted.Password";
}
