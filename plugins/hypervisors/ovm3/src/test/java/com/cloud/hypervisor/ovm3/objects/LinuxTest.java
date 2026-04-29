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

package com.cloud.hypervisor.ovm3.objects;

import org.junit.Test;

public class LinuxTest {
    public LinuxTest() {
    }
    ConnectionTest con = new ConnectionTest();
    Linux lin = new Linux(con);
    XmlTestResultTest results = new XmlTestResultTest();

    private final String DISCOVERSERVER = "&lt;?xml version=\"1.0\" ?&gt;"
            + "&lt;Discover_Server_Result&gt;"
            + "&lt;Server&gt;"
            + "&lt;Unique_Id&gt;1d:d5:e8:91:d9:d0:ed:bd:81:c2:a6:9a:b3:d1:b7:ea&lt;/Unique_Id&gt;"
            + "&lt;Boot_Time&gt;1413834408&lt;/Boot_Time&gt;"
            + "&lt;Date_Time&gt;"
            + "&lt;Time_Zone&gt;Europe/Amsterdam&lt;/Time_Zone&gt;"
            + "&lt;UTC&gt;True&lt;/UTC&gt;"
            + "&lt;/Date_Time&gt;"
            + "&lt;NTP&gt;"
            + "&lt;Local_Time_Source&gt;True&lt;/Local_Time_Source&gt;"
            + "&lt;Is_NTP_Running&gt;True&lt;/Is_NTP_Running&gt;"
            + "&lt;/NTP&gt;"
            + "&lt;Agent_Version&gt;3.2.1-183&lt;/Agent_Version&gt;"
            + "&lt;RPM_Version&gt;3.2.1-183&lt;/RPM_Version&gt;"
            + "&lt;OVM_Version&gt;3.2.1-517&lt;/OVM_Version&gt;"
            + "&lt;CPU_Type&gt;x86_64&lt;/CPU_Type&gt;"
            + "&lt;OS_Type&gt;Linux&lt;/OS_Type&gt;"
            + "&lt;OS_Name&gt;Oracle VM Server&lt;/OS_Name&gt;"
            + "&lt;OS_Major_Version&gt;5&lt;/OS_Major_Version&gt;"
            + "&lt;OS_Minor_Version&gt;7&lt;/OS_Minor_Version&gt;"
            + "&lt;Hypervisor_Type&gt;xen&lt;/Hypervisor_Type&gt;"
            + "&lt;Hypervisor_Name&gt;Xen&lt;/Hypervisor_Name&gt;"
            + "&lt;Host_Kernel_Release&gt;2.6.39-300.22.2.el5uek&lt;/Host_Kernel_Release&gt;"
            + "&lt;Host_Kernel_Version&gt;#1 SMP Fri Jan 4 12:40:29 PST 2013&lt;/Host_Kernel_Version&gt;"
            + "&lt;VMM&gt;"
            + "&lt;Version&gt;"
            + "&lt;Major&gt;4&lt;/Major&gt;"
            + "&lt;Minor&gt;1&lt;/Minor&gt;"
            + "&lt;Extra&gt;.3OVM&lt;/Extra&gt;"
            + "&lt;/Version&gt;"
            + "&lt;Compile_Information&gt;"
            + "&lt;Compiler&gt;gcc version 4.1.2 20080704 (Red Hat 4.1.2-48)&lt;/Compiler&gt;"
            + "&lt;By&gt;mockbuild&lt;/By&gt;"
            + "&lt;Domain&gt;us.oracle.com&lt;/Domain&gt;"
            + "&lt;Date&gt;Wed Dec  5 09:11:29 PST 2012&lt;/Date&gt;"
            + "&lt;/Compile_Information&gt;"
            + "&lt;Capabilities&gt;xen-3.0-x86_64 xen-3.0-x86_32p&lt;/Capabilities&gt;"
            + "&lt;/VMM&gt;"
            + "&lt;Pool_Unique_Id&gt;f12842eb-f5ed-3fe7-8da1-eb0e17f5ede8&lt;/Pool_Unique_Id&gt;"
            + "&lt;Manager_Unique_Id&gt;d1a749d4295041fb99854f52ea4dea97&lt;/Manager_Unique_Id&gt;"
            + "&lt;Hostname&gt;ovm-1&lt;/Hostname&gt;"
            + "&lt;Registered_IP&gt;192.168.1.64&lt;/Registered_IP&gt;"
            + "&lt;Node_Number&gt;1&lt;/Node_Number&gt;"
            + "&lt;Server_Roles&gt;xen,utility&lt;/Server_Roles&gt;"
            + "&lt;Is_Primary&gt;true&lt;/Is_Primary&gt;"
            + "&lt;Primary_Virtual_Ip&gt;192.168.1.230&lt;/Primary_Virtual_Ip&gt;"
            + "&lt;Manager_Core_API_Version&gt;3.2.1.516&lt;/Manager_Core_API_Version&gt;"
            + "&lt;Membership_State&gt;Pooled&lt;/Membership_State&gt;"
            + "&lt;Cluster_State&gt;Offline&lt;/Cluster_State&gt;"
            + "&lt;Statistic&gt;"
            + "&lt;Interval&gt;20&lt;/Interval&gt;"
            + "&lt;/Statistic&gt;"
            + "&lt;Exports/&gt;"
            + "&lt;Capabilities&gt;"
            + "&lt;ISCSI&gt;True&lt;/ISCSI&gt;"
            + "&lt;BOND_MODE_LINK_AGGREGATION&gt;True&lt;/BOND_MODE_LINK_AGGREGATION&gt;"
            + "&lt;POWER_ON_WOL&gt;True&lt;/POWER_ON_WOL&gt;"
            + "&lt;ALL_VM_CPU_OVERSUBSCRIBE&gt;True&lt;/ALL_VM_CPU_OVERSUBSCRIBE&gt;"
            + "&lt;HVM_MAX_VNICS&gt;8&lt;/HVM_MAX_VNICS&gt;"
            + "&lt;FIBRE_CHANNEL&gt;True&lt;/FIBRE_CHANNEL&gt;"
            + "&lt;MAX_CONCURRENT_MIGRATION_OUT&gt;1&lt;/MAX_CONCURRENT_MIGRATION_OUT&gt;"
            + "&lt;LOCAL_STORAGE_ELEMENT&gt;True&lt;/LOCAL_STORAGE_ELEMENT&gt;"
            + "&lt;CLUSTERS&gt;True&lt;/CLUSTERS&gt;"
            + "&lt;CONCURRENT_MIGRATION&gt;False&lt;/CONCURRENT_MIGRATION&gt;"
            + "&lt;VM_MEMORY_ALIGNMENT&gt;1048576&lt;/VM_MEMORY_ALIGNMENT&gt;"
            + "&lt;MIGRATION_SETUP&gt;False&lt;/MIGRATION_SETUP&gt;"
            + "&lt;PER_VM_CPU_OVERSUBSCRIBE&gt;True&lt;/PER_VM_CPU_OVERSUBSCRIBE&gt;"
            + "&lt;BOND_MODE_ACTIVE_BACKUP&gt;True&lt;/BOND_MODE_ACTIVE_BACKUP&gt;"
            + "&lt;NFS&gt;True&lt;/NFS&gt;"
            + "&lt;VM_VNC_CONSOLE&gt;True&lt;/VM_VNC_CONSOLE&gt;"
            + "&lt;MTU_CONFIGURATION&gt;True&lt;/MTU_CONFIGURATION&gt;"
            + "&lt;HIGH_AVAILABILITY&gt;True&lt;/HIGH_AVAILABILITY&gt;"
            + "&lt;MAX_CONCURRENT_MIGRATION_IN&gt;1&lt;/MAX_CONCURRENT_MIGRATION_IN&gt;"
            + "&lt;VM_SERIAL_CONSOLE&gt;True&lt;/VM_SERIAL_CONSOLE&gt;"
            + "&lt;BOND_MODE_ADAPTIVE_LOAD_BALANCING&gt;True&lt;/BOND_MODE_ADAPTIVE_LOAD_BALANCING&gt;"
            + "&lt;VM_SUSPEND&gt;True&lt;/VM_SUSPEND&gt;"
            + "&lt;YUM_PACKAGE_MANAGEMENT&gt;True&lt;/YUM_PACKAGE_MANAGEMENT&gt;"
            + "&lt;/Capabilities&gt;" + "&lt;/Server&gt;"
            + "&lt;/Discover_Server_Result&gt;";

    public String getDiscoverserver() {
        return DISCOVERSERVER;
    }

    public String getDiscoverHw() {
        return DISCOVERHW;
    }

    public String getDiscoverFs() {
        return DISCOVERFS;
    }

    private final String DISCOVERHW = "&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;"
            + "&lt;Discover_Hardware_Result&gt;"
            + "&lt;NodeInformation&gt;"
            + "&lt;VMM&gt;"
            + "&lt;PhysicalInfo&gt;"
            + "&lt;ThreadsPerCore&gt;1&lt;/ThreadsPerCore&gt;"
            + "&lt;CoresPerSocket&gt;1&lt;/CoresPerSocket&gt;"
            + "&lt;SocketsPerNode&gt;2&lt;/SocketsPerNode&gt;"
            + "&lt;Nodes&gt;1&lt;/Nodes&gt;"
            + "&lt;CPUKHz&gt;3392400&lt;/CPUKHz&gt;"
            + "&lt;TotalPages&gt;1048476&lt;/TotalPages&gt;"
            + "&lt;FreePages&gt;863459&lt;/FreePages&gt;"
            + "&lt;HW_Caps&gt;"
            + "&lt;Item_0&gt;0x0f8bf3ff&lt;/Item_0&gt;"
            + "&lt;Item_1&gt;0x28100800&lt;/Item_1&gt;"
            + "&lt;Item_2&gt;0x00000000&lt;/Item_2&gt;"
            + "&lt;Item_3&gt;0x00000040&lt;/Item_3&gt;"
            + "&lt;Item_4&gt;0xb19a2223&lt;/Item_4&gt;"
            + "&lt;Item_5&gt;0x00000000&lt;/Item_5&gt;"
            + "&lt;Item_6&gt;0x00000001&lt;/Item_6&gt;"
            + "&lt;Item_7&gt;0x00000281&lt;/Item_7&gt;"
            + "&lt;/HW_Caps&gt;"
            + "&lt;/PhysicalInfo&gt;"
            + "&lt;/VMM&gt;"
            + "&lt;CPUInfo&gt;"
            + "&lt;Proc_Info&gt;"
            + "&lt;CPU ID=\"0\"&gt;"
            + "&lt;vendor_id&gt;GenuineIntel&lt;/vendor_id&gt;"
            + "&lt;cpu_family&gt;6&lt;/cpu_family&gt;"
            + "&lt;model&gt;2&lt;/model&gt;"
            + "&lt;model_name&gt;Intel Core i7 9xx (Nehalem Class Core i7)&lt;/model_name&gt;"
            + "&lt;stepping&gt;3&lt;/stepping&gt;"
            + "&lt;cache_size&gt;4096 KB&lt;/cache_size&gt;"
            + "&lt;flags&gt;fpu de tsc msr pae mce cx8 apic mca cmov clflush mmx fxsr sse sse2 ss syscall nx lm rep_good nopl pni pclmulqdq ssse3 cx16 sse4_1 sse4_2 popcnt f16c hypervisor lahf_lm fsgsbase erms&lt;/flags&gt;"
            + "&lt;/CPU&gt;"
            + "&lt;CPU ID=\"1\"&gt;"
            + "&lt;vendor_id&gt;GenuineIntel&lt;/vendor_id&gt;"
            + "&lt;cpu_family&gt;6&lt;/cpu_family&gt;"
            + "&lt;model&gt;2&lt;/model&gt;"
            + "&lt;model_name&gt;Intel Core i7 9xx (Nehalem Class Core i7)&lt;/model_name&gt;"
            + "&lt;stepping&gt;3&lt;/stepping&gt;"
            + "&lt;cache_size&gt;4096 KB&lt;/cache_size&gt;"
            + "&lt;flags&gt;fpu de tsc msr pae mce cx8 apic mca cmov clflush mmx fxsr sse sse2 ss syscall nx lm rep_good nopl pni pclmulqdq ssse3 cx16 sse4_1 sse4_2 popcnt f16c hypervisor lahf_lm fsgsbase erms&lt;/flags&gt;"
            + "&lt;/CPU&gt;"
            + "&lt;/Proc_Info&gt;"
            + "&lt;/CPUInfo&gt;"
            + "&lt;IO&gt;"
            + "&lt;SCSI&gt;"
            + "&lt;SCSI_Host Num=\"0\"&gt;"
            + "&lt;Active_Mode&gt;Initiator&lt;/Active_Mode&gt;"
            + "&lt;Ahci_Host_Cap2&gt;0&lt;/Ahci_Host_Cap2&gt;"
            + "&lt;Ahci_Host_Caps&gt;40141f05&lt;/Ahci_Host_Caps&gt;"
            + "&lt;Ahci_Host_Version&gt;10000&lt;/Ahci_Host_Version&gt;"
            + "&lt;Ahci_Port_Cmd&gt;1000c017&lt;/Ahci_Port_Cmd&gt;"
            + "&lt;Can_Queue&gt;31&lt;/Can_Queue&gt;"
            + "&lt;Cmd_Per_Lun&gt;1&lt;/Cmd_Per_Lun&gt;"
            + "&lt;Em_Message_Supported&gt;&lt;/Em_Message_Supported&gt;"
            + "&lt;Em_Message_Type&gt;0&lt;/Em_Message_Type&gt;"
            + "&lt;Host_Busy&gt;0&lt;/Host_Busy&gt;"
            + "&lt;Link_Power_Management_Policy&gt;max_performance&lt;/Link_Power_Management_Policy&gt;"
            + "&lt;Proc_Name&gt;ahci&lt;/Proc_Name&gt;"
            + "&lt;Prot_Capabilities&gt;0&lt;/Prot_Capabilities&gt;"
            + "&lt;Prot_Guard_Type&gt;0&lt;/Prot_Guard_Type&gt;"
            + "&lt;Sg_Prot_Tablesize&gt;0&lt;/Sg_Prot_Tablesize&gt;"
            + "&lt;Sg_Tablesize&gt;168&lt;/Sg_Tablesize&gt;"
            + "&lt;State&gt;running&lt;/State&gt;"
            + "&lt;Supported_Mode&gt;Initiator&lt;/Supported_Mode&gt;"
            + "&lt;Uevent&gt;&lt;/Uevent&gt;"
            + "&lt;Unique_Id&gt;1&lt;/Unique_Id&gt;"
            + "&lt;SysFSDev&gt;"
            + "&lt;Broken_Parity_Status&gt;0&lt;/Broken_Parity_Status&gt;"
            + "&lt;Class&gt;0x010601&lt;/Class&gt;"
            + "&lt;Consistent_Dma_Mask_Bits&gt;32&lt;/Consistent_Dma_Mask_Bits&gt;"
            + "&lt;Device&gt;0x2922&lt;/Device&gt;"
            + "&lt;Dma_Mask_Bits&gt;32&lt;/Dma_Mask_Bits&gt;"
            + "&lt;Enable&gt;1&lt;/Enable&gt;"
            + "&lt;Irq&gt;58&lt;/Irq&gt;"
            + "&lt;Local_Cpulist&gt;0-1&lt;/Local_Cpulist&gt;"
            + "&lt;Msi_Bus&gt;&lt;/Msi_Bus&gt;"
            + "&lt;Msi_Irqs&gt;58&lt;/Msi_Irqs&gt;"
            + "&lt;Numa_Node&gt;-1&lt;/Numa_Node&gt;"
            + "&lt;Subsystem_Device&gt;0x1100&lt;/Subsystem_Device&gt;"
            + "&lt;Subsystem_Vendor&gt;0x1af4&lt;/Subsystem_Vendor&gt;"
            + "&lt;Uevent&gt;DRIVER=ahci PCI_CLASS=10601 PCI_ID=8086:2922 PCI_SUBSYS_ID=1AF4:1100 PCI_SLOT_NAME=0000:00:05.0 MODALIAS=pci:v00008086d00002922sv00001AF4sd00001100bc01sc06i01&lt;/Uevent&gt;"
            + "&lt;Vendor&gt;0x8086&lt;/Vendor&gt;"
            + "&lt;/SysFSDev&gt;"
            + "&lt;/SCSI_Host&gt;"
            + "&lt;SCSI_Host Num=\"1\"&gt;"
            + "&lt;Active_Mode&gt;Initiator&lt;/Active_Mode&gt;"
            + "&lt;Ahci_Host_Cap2&gt;0&lt;/Ahci_Host_Cap2&gt;"
            + "&lt;Ahci_Host_Caps&gt;40141f05&lt;/Ahci_Host_Caps&gt;"
            + "&lt;Ahci_Host_Version&gt;10000&lt;/Ahci_Host_Version&gt;"
            + "&lt;Ahci_Port_Cmd&gt;10004016&lt;/Ahci_Port_Cmd&gt;"
            + "&lt;Can_Queue&gt;31&lt;/Can_Queue&gt;"
            + "&lt;Cmd_Per_Lun&gt;1&lt;/Cmd_Per_Lun&gt;"
            + "&lt;Em_Message_Supported&gt;&lt;/Em_Message_Supported&gt;"
            + "&lt;Em_Message_Type&gt;0&lt;/Em_Message_Type&gt;"
            + "&lt;Host_Busy&gt;0&lt;/Host_Busy&gt;"
            + "&lt;Link_Power_Management_Policy&gt;max_performance&lt;/Link_Power_Management_Policy&gt;"
            + "&lt;Proc_Name&gt;ahci&lt;/Proc_Name&gt;"
            + "&lt;Prot_Capabilities&gt;0&lt;/Prot_Capabilities&gt;"
            + "&lt;Prot_Guard_Type&gt;0&lt;/Prot_Guard_Type&gt;"
            + "&lt;Sg_Prot_Tablesize&gt;0&lt;/Sg_Prot_Tablesize&gt;"
            + "&lt;Sg_Tablesize&gt;168&lt;/Sg_Tablesize&gt;"
            + "&lt;State&gt;running&lt;/State&gt;"
            + "&lt;Supported_Mode&gt;Initiator&lt;/Supported_Mode&gt;"
            + "&lt;Uevent&gt;&lt;/Uevent&gt;"
            + "&lt;Unique_Id&gt;2&lt;/Unique_Id&gt;"
            + "&lt;SysFSDev&gt;"
            + "&lt;Broken_Parity_Status&gt;0&lt;/Broken_Parity_Status&gt;"
            + "&lt;Class&gt;0x010601&lt;/Class&gt;"
            + "&lt;Consistent_Dma_Mask_Bits&gt;32&lt;/Consistent_Dma_Mask_Bits&gt;"
            + "&lt;Device&gt;0x2922&lt;/Device&gt;"
            + "&lt;Dma_Mask_Bits&gt;32&lt;/Dma_Mask_Bits&gt;"
            + "&lt;Enable&gt;1&lt;/Enable&gt;"
            + "&lt;Irq&gt;58&lt;/Irq&gt;"
            + "&lt;Local_Cpulist&gt;0-1&lt;/Local_Cpulist&gt;"
            + "&lt;Msi_Bus&gt;&lt;/Msi_Bus&gt;"
            + "&lt;Msi_Irqs&gt;58&lt;/Msi_Irqs&gt;"
            + "&lt;Numa_Node&gt;-1&lt;/Numa_Node&gt;"
            + "&lt;Subsystem_Device&gt;0x1100&lt;/Subsystem_Device&gt;"
            + "&lt;Subsystem_Vendor&gt;0x1af4&lt;/Subsystem_Vendor&gt;"
            + "&lt;Uevent&gt;DRIVER=ahci PCI_CLASS=10601 PCI_ID=8086:2922 PCI_SUBSYS_ID=1AF4:1100 PCI_SLOT_NAME=0000:00:05.0 MODALIAS=pci:v00008086d00002922sv00001AF4sd00001100bc01sc06i01&lt;/Uevent&gt;"
            + "&lt;Vendor&gt;0x8086&lt;/Vendor&gt;"
            + "&lt;/SysFSDev&gt;"
            + "&lt;/SCSI_Host&gt;"
            + "&lt;SCSI_Host Num=\"2\"&gt;"
            + "&lt;Active_Mode&gt;Initiator&lt;/Active_Mode&gt;"
            + "&lt;Ahci_Host_Cap2&gt;0&lt;/Ahci_Host_Cap2&gt;"
            + "&lt;Ahci_Host_Caps&gt;40141f05&lt;/Ahci_Host_Caps&gt;"
            + "&lt;Ahci_Host_Version&gt;10000&lt;/Ahci_Host_Version&gt;"
            + "&lt;Ahci_Port_Cmd&gt;10004016&lt;/Ahci_Port_Cmd&gt;"
            + "&lt;Can_Queue&gt;31&lt;/Can_Queue&gt;"
            + "&lt;Cmd_Per_Lun&gt;1&lt;/Cmd_Per_Lun&gt;"
            + "&lt;Em_Message_Supported&gt;&lt;/Em_Message_Supported&gt;"
            + "&lt;Em_Message_Type&gt;0&lt;/Em_Message_Type&gt;"
            + "&lt;Host_Busy&gt;0&lt;/Host_Busy&gt;"
            + "&lt;Link_Power_Management_Policy&gt;max_performance&lt;/Link_Power_Management_Policy&gt;"
            + "&lt;Proc_Name&gt;ahci&lt;/Proc_Name&gt;"
            + "&lt;Prot_Capabilities&gt;0&lt;/Prot_Capabilities&gt;"
            + "&lt;Prot_Guard_Type&gt;0&lt;/Prot_Guard_Type&gt;"
            + "&lt;Sg_Prot_Tablesize&gt;0&lt;/Sg_Prot_Tablesize&gt;"
            + "&lt;Sg_Tablesize&gt;168&lt;/Sg_Tablesize&gt;"
            + "&lt;State&gt;running&lt;/State&gt;"
            + "&lt;Supported_Mode&gt;Initiator&lt;/Supported_Mode&gt;"
            + "&lt;Uevent&gt;&lt;/Uevent&gt;"
            + "&lt;Unique_Id&gt;3&lt;/Unique_Id&gt;"
            + "&lt;SysFSDev&gt;"
            + "&lt;Broken_Parity_Status&gt;0&lt;/Broken_Parity_Status&gt;"
            + "&lt;Class&gt;0x010601&lt;/Class&gt;"
            + "&lt;Consistent_Dma_Mask_Bits&gt;32&lt;/Consistent_Dma_Mask_Bits&gt;"
            + "&lt;Device&gt;0x2922&lt;/Device&gt;"
            + "&lt;Dma_Mask_Bits&gt;32&lt;/Dma_Mask_Bits&gt;"
            + "&lt;Enable&gt;1&lt;/Enable&gt;"
            + "&lt;Irq&gt;58&lt;/Irq&gt;"
            + "&lt;Local_Cpulist&gt;0-1&lt;/Local_Cpulist&gt;"
            + "&lt;Msi_Bus&gt;&lt;/Msi_Bus&gt;"
            + "&lt;Msi_Irqs&gt;58&lt;/Msi_Irqs&gt;"
            + "&lt;Numa_Node&gt;-1&lt;/Numa_Node&gt;"
            + "&lt;Subsystem_Device&gt;0x1100&lt;/Subsystem_Device&gt;"
            + "&lt;Subsystem_Vendor&gt;0x1af4&lt;/Subsystem_Vendor&gt;"
            + "&lt;Uevent&gt;DRIVER=ahci PCI_CLASS=10601 PCI_ID=8086:2922 PCI_SUBSYS_ID=1AF4:1100 PCI_SLOT_NAME=0000:00:05.0 MODALIAS=pci:v00008086d00002922sv00001AF4sd00001100bc01sc06i01&lt;/Uevent&gt;"
            + "&lt;Vendor&gt;0x8086&lt;/Vendor&gt;"
            + "&lt;/SysFSDev&gt;"
            + "&lt;/SCSI_Host&gt;"
            + "&lt;SCSI_Host Num=\"3\"&gt;"
            + "&lt;Active_Mode&gt;Initiator&lt;/Active_Mode&gt;"
            + "&lt;Ahci_Host_Cap2&gt;0&lt;/Ahci_Host_Cap2&gt;"
            + "&lt;Ahci_Host_Caps&gt;40141f05&lt;/Ahci_Host_Caps&gt;"
            + "&lt;Ahci_Host_Version&gt;10000&lt;/Ahci_Host_Version&gt;"
            + "&lt;Ahci_Port_Cmd&gt;10004016&lt;/Ahci_Port_Cmd&gt;"
            + "&lt;Can_Queue&gt;31&lt;/Can_Queue&gt;"
            + "&lt;Cmd_Per_Lun&gt;1&lt;/Cmd_Per_Lun&gt;"
            + "&lt;Em_Message_Supported&gt;&lt;/Em_Message_Supported&gt;"
            + "&lt;Em_Message_Type&gt;0&lt;/Em_Message_Type&gt;"
            + "&lt;Host_Busy&gt;0&lt;/Host_Busy&gt;"
            + "&lt;Link_Power_Management_Policy&gt;max_performance&lt;/Link_Power_Management_Policy&gt;"
            + "&lt;Proc_Name&gt;ahci&lt;/Proc_Name&gt;"
            + "&lt;Prot_Capabilities&gt;0&lt;/Prot_Capabilities&gt;"
            + "&lt;Prot_Guard_Type&gt;0&lt;/Prot_Guard_Type&gt;"
            + "&lt;Sg_Prot_Tablesize&gt;0&lt;/Sg_Prot_Tablesize&gt;"
            + "&lt;Sg_Tablesize&gt;168&lt;/Sg_Tablesize&gt;"
            + "&lt;State&gt;running&lt;/State&gt;"
            + "&lt;Supported_Mode&gt;Initiator&lt;/Supported_Mode&gt;"
            + "&lt;Uevent&gt;&lt;/Uevent&gt;"
            + "&lt;Unique_Id&gt;4&lt;/Unique_Id&gt;"
            + "&lt;SysFSDev&gt;"
            + "&lt;Broken_Parity_Status&gt;0&lt;/Broken_Parity_Status&gt;"
            + "&lt;Class&gt;0x010601&lt;/Class&gt;"
            + "&lt;Consistent_Dma_Mask_Bits&gt;32&lt;/Consistent_Dma_Mask_Bits&gt;"
            + "&lt;Device&gt;0x2922&lt;/Device&gt;"
            + "&lt;Dma_Mask_Bits&gt;32&lt;/Dma_Mask_Bits&gt;"
            + "&lt;Enable&gt;1&lt;/Enable&gt;"
            + "&lt;Irq&gt;58&lt;/Irq&gt;"
            + "&lt;Local_Cpulist&gt;0-1&lt;/Local_Cpulist&gt;"
            + "&lt;Msi_Bus&gt;&lt;/Msi_Bus&gt;"
            + "&lt;Msi_Irqs&gt;58&lt;/Msi_Irqs&gt;"
            + "&lt;Numa_Node&gt;-1&lt;/Numa_Node&gt;"
            + "&lt;Subsystem_Device&gt;0x1100&lt;/Subsystem_Device&gt;"
            + "&lt;Subsystem_Vendor&gt;0x1af4&lt;/Subsystem_Vendor&gt;"
            + "&lt;Uevent&gt;DRIVER=ahci PCI_CLASS=10601 PCI_ID=8086:2922 PCI_SUBSYS_ID=1AF4:1100 PCI_SLOT_NAME=0000:00:05.0 MODALIAS=pci:v00008086d00002922sv00001AF4sd00001100bc01sc06i01&lt;/Uevent&gt;"
            + "&lt;Vendor&gt;0x8086&lt;/Vendor&gt;"
            + "&lt;/SysFSDev&gt;"
            + "&lt;/SCSI_Host&gt;"
            + "&lt;SCSI_Host Num=\"4\"&gt;"
            + "&lt;Active_Mode&gt;Initiator&lt;/Active_Mode&gt;"
            + "&lt;Ahci_Host_Cap2&gt;0&lt;/Ahci_Host_Cap2&gt;"
            + "&lt;Ahci_Host_Caps&gt;40141f05&lt;/Ahci_Host_Caps&gt;"
            + "&lt;Ahci_Host_Version&gt;10000&lt;/Ahci_Host_Version&gt;"
            + "&lt;Ahci_Port_Cmd&gt;10004016&lt;/Ahci_Port_Cmd&gt;"
            + "&lt;Can_Queue&gt;31&lt;/Can_Queue&gt;"
            + "&lt;Cmd_Per_Lun&gt;1&lt;/Cmd_Per_Lun&gt;"
            + "&lt;Em_Message_Supported&gt;&lt;/Em_Message_Supported&gt;"
            + "&lt;Em_Message_Type&gt;0&lt;/Em_Message_Type&gt;"
            + "&lt;Host_Busy&gt;0&lt;/Host_Busy&gt;"
            + "&lt;Link_Power_Management_Policy&gt;max_performance&lt;/Link_Power_Management_Policy&gt;"
            + "&lt;Proc_Name&gt;ahci&lt;/Proc_Name&gt;"
            + "&lt;Prot_Capabilities&gt;0&lt;/Prot_Capabilities&gt;"
            + "&lt;Prot_Guard_Type&gt;0&lt;/Prot_Guard_Type&gt;"
            + "&lt;Sg_Prot_Tablesize&gt;0&lt;/Sg_Prot_Tablesize&gt;"
            + "&lt;Sg_Tablesize&gt;168&lt;/Sg_Tablesize&gt;"
            + "&lt;State&gt;running&lt;/State&gt;"
            + "&lt;Supported_Mode&gt;Initiator&lt;/Supported_Mode&gt;"
            + "&lt;Uevent&gt;&lt;/Uevent&gt;"
            + "&lt;Unique_Id&gt;5&lt;/Unique_Id&gt;"
            + "&lt;SysFSDev&gt;"
            + "&lt;Broken_Parity_Status&gt;0&lt;/Broken_Parity_Status&gt;"
            + "&lt;Class&gt;0x010601&lt;/Class&gt;"
            + "&lt;Consistent_Dma_Mask_Bits&gt;32&lt;/Consistent_Dma_Mask_Bits&gt;"
            + "&lt;Device&gt;0x2922&lt;/Device&gt;"
            + "&lt;Dma_Mask_Bits&gt;32&lt;/Dma_Mask_Bits&gt;"
            + "&lt;Enable&gt;1&lt;/Enable&gt;"
            + "&lt;Irq&gt;58&lt;/Irq&gt;"
            + "&lt;Local_Cpulist&gt;0-1&lt;/Local_Cpulist&gt;"
            + "&lt;Msi_Bus&gt;&lt;/Msi_Bus&gt;"
            + "&lt;Msi_Irqs&gt;58&lt;/Msi_Irqs&gt;"
            + "&lt;Numa_Node&gt;-1&lt;/Numa_Node&gt;"
            + "&lt;Subsystem_Device&gt;0x1100&lt;/Subsystem_Device&gt;"
            + "&lt;Subsystem_Vendor&gt;0x1af4&lt;/Subsystem_Vendor&gt;"
            + "&lt;Uevent&gt;DRIVER=ahci PCI_CLASS=10601 PCI_ID=8086:2922 PCI_SUBSYS_ID=1AF4:1100 PCI_SLOT_NAME=0000:00:05.0 MODALIAS=pci:v00008086d00002922sv00001AF4sd00001100bc01sc06i01&lt;/Uevent&gt;"
            + "&lt;Vendor&gt;0x8086&lt;/Vendor&gt;"
            + "&lt;/SysFSDev&gt;"
            + "&lt;/SCSI_Host&gt;"
            + "&lt;SCSI_Host Num=\"5\"&gt;"
            + "&lt;Active_Mode&gt;Initiator&lt;/Active_Mode&gt;"
            + "&lt;Ahci_Host_Cap2&gt;0&lt;/Ahci_Host_Cap2&gt;"
            + "&lt;Ahci_Host_Caps&gt;40141f05&lt;/Ahci_Host_Caps&gt;"
            + "&lt;Ahci_Host_Version&gt;10000&lt;/Ahci_Host_Version&gt;"
            + "&lt;Ahci_Port_Cmd&gt;10004016&lt;/Ahci_Port_Cmd&gt;"
            + "&lt;Can_Queue&gt;31&lt;/Can_Queue&gt;"
            + "&lt;Cmd_Per_Lun&gt;1&lt;/Cmd_Per_Lun&gt;"
            + "&lt;Em_Message_Supported&gt;&lt;/Em_Message_Supported&gt;"
            + "&lt;Em_Message_Type&gt;0&lt;/Em_Message_Type&gt;"
            + "&lt;Host_Busy&gt;0&lt;/Host_Busy&gt;"
            + "&lt;Link_Power_Management_Policy&gt;max_performance&lt;/Link_Power_Management_Policy&gt;"
            + "&lt;Proc_Name&gt;ahci&lt;/Proc_Name&gt;"
            + "&lt;Prot_Capabilities&gt;0&lt;/Prot_Capabilities&gt;"
            + "&lt;Prot_Guard_Type&gt;0&lt;/Prot_Guard_Type&gt;"
            + "&lt;Sg_Prot_Tablesize&gt;0&lt;/Sg_Prot_Tablesize&gt;"
            + "&lt;Sg_Tablesize&gt;168&lt;/Sg_Tablesize&gt;"
            + "&lt;State&gt;running&lt;/State&gt;"
            + "&lt;Supported_Mode&gt;Initiator&lt;/Supported_Mode&gt;"
            + "&lt;Uevent&gt;&lt;/Uevent&gt;"
            + "&lt;Unique_Id&gt;6&lt;/Unique_Id&gt;"
            + "&lt;SysFSDev&gt;"
            + "&lt;Broken_Parity_Status&gt;0&lt;/Broken_Parity_Status&gt;"
            + "&lt;Class&gt;0x010601&lt;/Class&gt;"
            + "&lt;Consistent_Dma_Mask_Bits&gt;32&lt;/Consistent_Dma_Mask_Bits&gt;"
            + "&lt;Device&gt;0x2922&lt;/Device&gt;"
            + "&lt;Dma_Mask_Bits&gt;32&lt;/Dma_Mask_Bits&gt;"
            + "&lt;Enable&gt;1&lt;/Enable&gt;"
            + "&lt;Irq&gt;58&lt;/Irq&gt;"
            + "&lt;Local_Cpulist&gt;0-1&lt;/Local_Cpulist&gt;"
            + "&lt;Msi_Bus&gt;&lt;/Msi_Bus&gt;"
            + "&lt;Msi_Irqs&gt;58&lt;/Msi_Irqs&gt;"
            + "&lt;Numa_Node&gt;-1&lt;/Numa_Node&gt;"
            + "&lt;Subsystem_Device&gt;0x1100&lt;/Subsystem_Device&gt;"
            + "&lt;Subsystem_Vendor&gt;0x1af4&lt;/Subsystem_Vendor&gt;"
            + "&lt;Uevent&gt;DRIVER=ahci PCI_CLASS=10601 PCI_ID=8086:2922 PCI_SUBSYS_ID=1AF4:1100 PCI_SLOT_NAME=0000:00:05.0 MODALIAS=pci:v00008086d00002922sv00001AF4sd00001100bc01sc06i01&lt;/Uevent&gt;"
            + "&lt;Vendor&gt;0x8086&lt;/Vendor&gt;"
            + "&lt;/SysFSDev&gt;"
            + "&lt;/SCSI_Host&gt;"
            + "&lt;SCSI_Host Num=\"6\"&gt;"
            + "&lt;Active_Mode&gt;Initiator&lt;/Active_Mode&gt;"
            + "&lt;Can_Queue&gt;1&lt;/Can_Queue&gt;"
            + "&lt;Cmd_Per_Lun&gt;1&lt;/Cmd_Per_Lun&gt;"
            + "&lt;Host_Busy&gt;0&lt;/Host_Busy&gt;"
            + "&lt;Proc_Name&gt;ata_piix&lt;/Proc_Name&gt;"
            + "&lt;Prot_Capabilities&gt;0&lt;/Prot_Capabilities&gt;"
            + "&lt;Prot_Guard_Type&gt;0&lt;/Prot_Guard_Type&gt;"
            + "&lt;Sg_Prot_Tablesize&gt;0&lt;/Sg_Prot_Tablesize&gt;"
            + "&lt;Sg_Tablesize&gt;128&lt;/Sg_Tablesize&gt;"
            + "&lt;State&gt;running&lt;/State&gt;"
            + "&lt;Supported_Mode&gt;Initiator&lt;/Supported_Mode&gt;"
            + "&lt;Uevent&gt;&lt;/Uevent&gt;"
            + "&lt;Unique_Id&gt;7&lt;/Unique_Id&gt;"
            + "&lt;SysFSDev&gt;"
            + "&lt;Broken_Parity_Status&gt;0&lt;/Broken_Parity_Status&gt;"
            + "&lt;Class&gt;0x010180&lt;/Class&gt;"
            + "&lt;Consistent_Dma_Mask_Bits&gt;32&lt;/Consistent_Dma_Mask_Bits&gt;"
            + "&lt;Device&gt;0x7010&lt;/Device&gt;"
            + "&lt;Dma_Mask_Bits&gt;32&lt;/Dma_Mask_Bits&gt;"
            + "&lt;Enable&gt;1&lt;/Enable&gt;"
            + "&lt;Irq&gt;0&lt;/Irq&gt;"
            + "&lt;Local_Cpulist&gt;0-1&lt;/Local_Cpulist&gt;"
            + "&lt;Msi_Bus&gt;&lt;/Msi_Bus&gt;"
            + "&lt;Msi_Irqs&gt;&lt;/Msi_Irqs&gt;"
            + "&lt;Numa_Node&gt;-1&lt;/Numa_Node&gt;"
            + "&lt;Subsystem_Device&gt;0x1100&lt;/Subsystem_Device&gt;"
            + "&lt;Subsystem_Vendor&gt;0x1af4&lt;/Subsystem_Vendor&gt;"
            + "&lt;Uevent&gt;DRIVER=ata_piix PCI_CLASS=10180 PCI_ID=8086:7010 PCI_SUBSYS_ID=1AF4:1100 PCI_SLOT_NAME=0000:00:01.1 MODALIAS=pci:v00008086d00007010sv00001AF4sd00001100bc01sc01i80&lt;/Uevent&gt;"
            + "&lt;Vendor&gt;0x8086&lt;/Vendor&gt;"
            + "&lt;/SysFSDev&gt;"
            + "&lt;/SCSI_Host&gt;"
            + "&lt;SCSI_Host Num=\"7\"&gt;"
            + "&lt;Active_Mode&gt;Initiator&lt;/Active_Mode&gt;"
            + "&lt;Can_Queue&gt;1&lt;/Can_Queue&gt;"
            + "&lt;Cmd_Per_Lun&gt;1&lt;/Cmd_Per_Lun&gt;"
            + "&lt;Host_Busy&gt;0&lt;/Host_Busy&gt;"
            + "&lt;Proc_Name&gt;ata_piix&lt;/Proc_Name&gt;"
            + "&lt;Prot_Capabilities&gt;0&lt;/Prot_Capabilities&gt;"
            + "&lt;Prot_Guard_Type&gt;0&lt;/Prot_Guard_Type&gt;"
            + "&lt;Sg_Prot_Tablesize&gt;0&lt;/Sg_Prot_Tablesize&gt;"
            + "&lt;Sg_Tablesize&gt;128&lt;/Sg_Tablesize&gt;"
            + "&lt;State&gt;running&lt;/State&gt;"
            + "&lt;Supported_Mode&gt;Initiator&lt;/Supported_Mode&gt;"
            + "&lt;Uevent&gt;&lt;/Uevent&gt;"
            + "&lt;Unique_Id&gt;8&lt;/Unique_Id&gt;"
            + "&lt;SysFSDev&gt;"
            + "&lt;Broken_Parity_Status&gt;0&lt;/Broken_Parity_Status&gt;"
            + "&lt;Class&gt;0x010180&lt;/Class&gt;"
            + "&lt;Consistent_Dma_Mask_Bits&gt;32&lt;/Consistent_Dma_Mask_Bits&gt;"
            + "&lt;Device&gt;0x7010&lt;/Device&gt;"
            + "&lt;Dma_Mask_Bits&gt;32&lt;/Dma_Mask_Bits&gt;"
            + "&lt;Enable&gt;1&lt;/Enable&gt;"
            + "&lt;Irq&gt;0&lt;/Irq&gt;"
            + "&lt;Local_Cpulist&gt;0-1&lt;/Local_Cpulist&gt;"
            + "&lt;Msi_Bus&gt;&lt;/Msi_Bus&gt;"
            + "&lt;Msi_Irqs&gt;&lt;/Msi_Irqs&gt;"
            + "&lt;Numa_Node&gt;-1&lt;/Numa_Node&gt;"
            + "&lt;Subsystem_Device&gt;0x1100&lt;/Subsystem_Device&gt;"
            + "&lt;Subsystem_Vendor&gt;0x1af4&lt;/Subsystem_Vendor&gt;"
            + "&lt;Uevent&gt;DRIVER=ata_piix PCI_CLASS=10180 PCI_ID=8086:7010 PCI_SUBSYS_ID=1AF4:1100 PCI_SLOT_NAME=0000:00:01.1 MODALIAS=pci:v00008086d00007010sv00001AF4sd00001100bc01sc01i80&lt;/Uevent&gt;"
            + "&lt;Vendor&gt;0x8086&lt;/Vendor&gt;"
            + "&lt;/SysFSDev&gt;"
            + "&lt;/SCSI_Host&gt;"
            + "&lt;ISCSI_Node&gt;"
            + "&lt;Initiatorname&gt;iqn.1988-12.com.oracle:3b3f5e2f59cb&lt;/Initiatorname&gt;"
            + "&lt;/ISCSI_Node&gt;"
            + "&lt;/SCSI&gt;"
            + "&lt;IDE&gt;"
            + "&lt;/IDE&gt;"
            + "&lt;/IO&gt;"
            + "&lt;DMTF&gt;"
            + "&lt;SMBIOS Version=\"2.4.0\"&gt;"
            + "&lt;MaxSize&gt;48&lt;/MaxSize&gt;"
            + "&lt;/SMBIOS&gt;"
            + "&lt;DMI Version=\"2.4\"&gt;"
            + "&lt;TableLength&gt;346&lt;/TableLength&gt;"
            + "&lt;Items&gt;13&lt;/Items&gt;"
            + "&lt;Buffer&gt;"
            + "ABgAAAECAOgDAAgAAAAAAAAAAAQBAP//Qm9jaHMAQm9jaHMAMDEvMDEvMjAxMQAAARsAAQEC"
            + "AAAd1eiR2dDtvYHCppqz0bfqBgAAQm9jaHMAQm9jaHMAAAMUAAMBAQAAAAMDAwIAAAAAAAAA"
            + "Qm9jaHMAAAQgAQQBAwECIwYAAP/7iw8AAAAA0AfQB0EB////////Q1BVIDEAQm9jaHMAAAQg"
            + "AgQBAwECIwYAAP/7iw8AAAAA0AfQB0EB////////Q1BVIDIAQm9jaHMAABAPABABAwYAAEAA"
            + "/v8BAAAAERUAEQAQAwBAAEAAABAJAAEABwAARElNTSAwAAATDwATAAAAAP//NwAAEAEAABMP"
            + "ARMAAEAA//9HAAAQAQAAFBMAFAAAAAD//zcAABEAEwEAAAAAFBMBFAAAQAD//0cAABEBEwEA"
            + "AAAAIAsAIAAAAAAAAAAAAH8EAH8AAA=="
            + "&lt;/Buffer&gt;"
            + "&lt;/DMI&gt;"
            + "&lt;BIOS Type=\"0\" Item=\"0\" Handle=\"0x0\"&gt;"
            + "&lt;Vendor&gt;Bochs&lt;/Vendor&gt;"
            + "&lt;Version&gt;Bochs&lt;/Version&gt;"
            + "&lt;ReleaseDate&gt;01/01/2011&lt;/ReleaseDate&gt;"
            + "&lt;/BIOS&gt;"
            + "&lt;System Type=\"1\" Item=\"1\" Handle=\"0x100\"&gt;"
            + "&lt;UUID&gt;1d:d5:e8:91:d9:d0:ed:bd:81:c2:a6:9a:b3:d1:b7:ea&lt;/UUID&gt;"
            + "&lt;Manufacturer&gt;Bochs&lt;/Manufacturer&gt;"
            + "&lt;ProductName&gt;Bochs&lt;/ProductName&gt;"
            + "&lt;/System&gt;"
            + "&lt;Chassis Type=\"3\" Item=\"2\" Handle=\"0x300\"&gt;"
            + "&lt;Height&gt;0U&lt;/Height&gt;"
            + "&lt;Manufacturer&gt;Bochs&lt;/Manufacturer&gt;"
            + "&lt;/Chassis&gt;"
            + "&lt;/DMTF&gt;"
            + "&lt;/NodeInformation&gt;"
            + "&lt;/Discover_Hardware_Result&gt;";
    private final String FSTYPE = "nfs";
    private final String REMOTEHOST = "cs-mgmt";
    private final String REMOTEDIR = "/volumes/cs-data/primary/ovm";
    public String getFsType() {
        return FSTYPE;
    }

    public String getRemoteHost() {
        return REMOTEHOST;
    }

    public String getRemoteDir() {
        return REMOTEDIR;
    }

    public String getRemote() {
        return REMOTE;
    }

    public String getRepoId() {
        return REPOID;
    }

    public String getRepoMnt() {
        return REPOMNT;
    }
    public String getVirtualDisksDir() {
        return REPOMNT + "/VirtualDisks";
    }
    public String getTemplatesDir() {
        return REPOMNT + "/Templates";
    }
    public String getIsoDir() {
        return REPOMNT + "/ISOs";
    }
    private final String REMOTE = REMOTEHOST + ":" + REMOTEDIR;
    private final String REPOID = "f12842eb-f5ed-3fe7-8da1-eb0e17f5ede8";
    private final String DDREPOID = lin.deDash(REPOID);
    private final String REPOMNT = "/OVS/Repositories/" + DDREPOID;
    private final String VMMNT = "/nfsmnt/" + REPOID;
    private final String DISCOVERFS = "&lt;?xml version=\"1.0\" ?&gt;"
            + "&lt;Discover_Mounted_File_Systems_Result&gt;"
            + "&lt;Filesystem Type=\""
            + FSTYPE
            + "\"&gt;"
            + "&lt;Mount Dir=\""
            + REPOMNT
            + "\"&gt;"
            + "&lt;Device&gt;"
            + REMOTE
            + "&lt;/Device&gt;"
            + "&lt;Mount_Options&gt;rw,relatime,vers=3,rsize=524288,wsize=524288,namlen=255,hard,proto=tcp,port=65535,timeo=600,retrans=2,sec=sys,local_lock=none,addr=192.168.1.61&lt;/Mount_Options&gt;"
            + "&lt;/Mount&gt;"
            + "&lt;Mount Dir=\""
            + VMMNT
            + "\"&gt;"
            + "&lt;Device&gt;"
            + REMOTE
            + "/VirtualMachines&lt;/Device&gt;"
            + "&lt;Mount_Options&gt;rw,relatime,vers=3,rsize=524288,wsize=524288,namlen=255,hard,proto=tcp,port=65535,timeo=600,retrans=2,sec=sys,local_lock=none,addr=192.168.1.61&lt;/Mount_Options&gt;"
            + "&lt;/Mount&gt;" + "&lt;/Filesystem&gt;"
            + "&lt;/Discover_Mounted_File_Systems_Result&gt;";
    private final String LASTBOOT = "<struct>" + "<member>"
            + "<name>last_boot_time</name>"
            + "<value><i8>1413834408</i8></value>" + "</member>" + "<member>"
            + "<name>local_time</name>" + "<value><i8>1414082517</i8></value>"
            + "</member>" + "</struct>";
    private final String TIMEZONE = "<array><data>"
            + "<value><string>Europe/Amsterdam</string></value>"
            + "<value><boolean>1</boolean></value>" + "</data></array>";

    @Test
    public void testDiscoverServer() throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrapWrapper(DISCOVERSERVER));
        results.basicStringTest(lin.getMembershipState(), "Pooled");
        lin.discoverServer();
        results.basicStringTest(lin.getCapabilities(),
                "xen-3.0-x86_64 xen-3.0-x86_32p");
        results.basicStringTest(lin.getOvmVersion(), "3.2.1-517");
        results.basicStringTest(lin.getHypervisorVersion(), "4.1.3OVM");
        results.basicStringTest(lin.get("MAX_CONCURRENT_MIGRATION_IN"), "1");
    }

    @Test
    public void testGetTimeZone() throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrapWrapper(TIMEZONE));
        results.basicBooleanTest(lin.getTimeZone());
    }

    @Test
    public void testLastBootTime() throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrapWrapper(LASTBOOT));
        results.basicIntTest(lin.getLastBootTime(), 1413834408);
    }

    @Test
    public void testDiscoverHardware() throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrapWrapper(DISCOVERHW));
        lin.discoverHardware();
        results.basicDoubleTest(lin.getMemory(),
                Double.valueOf("1048476") * 4096);
        results.basicDoubleTest(lin.getFreeMemory(),
                Double.valueOf("863459") * 4096);
        results.basicStringTest(lin.get("UUID"),
                "1d:d5:e8:91:d9:d0:ed:bd:81:c2:a6:9a:b3:d1:b7:ea");
    }

    @Test
    public void testDiscoverMountedFileSystems() throws Ovm3ResourceException {
        con.setResult(results.simpleResponseWrapWrapper(DISCOVERFS));
        lin.discoverMountedFs(FSTYPE);
        results.basicBooleanTest(
                results.basicListHasString(lin.getFileSystemList(), REPOMNT),
                true);
        results.basicBooleanTest(
                results.basicListHasString(lin.getFileSystemList(), VMMNT),
                true);
        results.basicBooleanTest(
                results.basicListHasString(lin.getFileSystemList(), REMOTE),
                false);
        results.basicStringTest(lin.getFileSystem(VMMNT, FSTYPE)
                .getMountPoint(), VMMNT);
        results.basicStringTest(lin.getFileSystem(VMMNT, FSTYPE).getHost(),
                REMOTEHOST);
        results.basicStringTest(lin.getFileSystem(VMMNT, FSTYPE).getUuid(),
                REPOID);
        results.basicStringTest(lin.getFileSystem(REPOMNT, FSTYPE).getUuid(),
                DDREPOID);
        results.basicStringTest(lin.getFileSystem(REPOMNT, FSTYPE)
                .getRemoteDir(), REMOTEDIR);
        results.basicBooleanTest(lin.getFileSystem(VMMNT, FSTYPE).getDetails()
                .containsKey("Uuid"), true);
    }
}
