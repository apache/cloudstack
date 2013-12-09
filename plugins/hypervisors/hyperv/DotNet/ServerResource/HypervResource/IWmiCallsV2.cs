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

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using CloudStack.Plugin.WmiWrappers.ROOT.VIRTUALIZATION.V2;
using System.Management;

namespace HypervResource
{
    public interface IWmiCallsV2
    {
        System.Management.ManagementPath AddDiskDriveToVm(ComputerSystem vm, string vhdfile, string cntrllerAddr, string driveResourceType);
        ComputerSystem AddUserData(ComputerSystem vm, string userData);
        void AttachIso(string displayName, string iso);
        void CreateDynamicVirtualHardDisk(ulong MaxInternalSize, string Path);
        SyntheticEthernetPortSettingData CreateNICforVm(ComputerSystem vm, string mac);
        ComputerSystem CreateVM(string name, long memory_mb, int vcpus);
        void DeleteHostKvpItem(ComputerSystem vm, string key);
        void DeleteSwitchPort(string elementName);
        ComputerSystem DeployVirtualMachine(dynamic jsonObj, string systemVmIso);
        void DestroyVm(dynamic jsonObj);
        void DestroyVm(string displayName);
        void MigrateVm(string vmName, string destination);
        void DetachDisk(string displayName, string diskFileName);
        ComputerSystem GetComputerSystem(string displayName);
        string GetDefaultDataRoot();
        string GetDefaultVirtualDiskFolder();
        ResourceAllocationSettingData GetDvdDriveSettings(VirtualSystemSettingData vmSettings);
        EthernetPortAllocationSettingData[] GetEthernetConnections(ComputerSystem vm);
        SyntheticEthernetPortSettingData[] GetEthernetPortSettings(ComputerSystem vm);
        ResourceAllocationSettingData GetIDEControllerSettings(VirtualSystemSettingData vmSettings, string cntrllerAddr);
        ImageManagementService GetImageManagementService();
        KvpExchangeComponentSettingData GetKvpSettings(VirtualSystemSettingData vmSettings);
        void GetMemoryResources(out ulong physicalRamKBs, out ulong freeMemoryKBs);
        MemorySettingData GetMemSettings(VirtualSystemSettingData vmSettings);
        void GetProcessorResources(out uint cores, out uint mhz);
        void GetProcessorUsageInfo(out double cpuUtilization);
        ProcessorSettingData GetProcSettings(VirtualSystemSettingData vmSettings);
        ResourceAllocationSettingData.ResourceAllocationSettingDataCollection GetResourceAllocationSettings(VirtualSystemSettingData vmSettings);
        void GetSummaryInfo(System.Collections.Generic.Dictionary<string, VmStatsEntry> vmProcessorInfo, System.Collections.Generic.List<System.Management.ManagementPath> vmsToInspect);
        SyntheticEthernetPortSettingData GetSyntheticEthernetPortSettings(EthernetSwitchPort port);
        VirtualSystemManagementService GetVirtualisationSystemManagementService();
        VirtualEthernetSwitchManagementService GetVirtualSwitchManagementService();
        EthernetSwitchPortVlanSettingData GetVlanSettings(EthernetPortAllocationSettingData ethernetConnection);
        System.Collections.Generic.List<string> GetVmElementNames();
        VirtualSystemSettingData GetVmSettings(ComputerSystem vm);
        void patchSystemVmIso(string vmName, string systemVmIso);
        void SetState(ComputerSystem vm, ushort requiredState);
        Dictionary<String, VmState> GetVmSync(String privateIpAddress);
    }
}
