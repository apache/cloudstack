using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using CloudStack.Plugin.WmiWrappers.ROOT.VIRTUALIZATION;
using System.Management;

namespace HypervResource
{
    public interface IWmiCalls
    {
        ComputerSystem CreateVM(string name, long memory_mb, int vcpus);
        void DestroyVm(string displayName);
        void DestroyVm(dynamic jsonObj);
        void patchSystemVmIso(String vmName, String systemVmIso);
        void AttachIso(string displayName, string iso);        
        void GetProcessorResources(out uint cores, out uint mhz);
        void GetMemoryResources(out ulong physicalRamKBs, out ulong freeMemoryKBs);
        string GetDefaultVirtualDiskFolder();
        ComputerSystem DeployVirtualMachine(dynamic jsonObj, string systemVmIso);
        ComputerSystem GetComputerSystem(string displayName);
        void GetProcessorUsageInfo(out double cpuUtilization);
        SyntheticEthernetPortSettingData CreateNICforVm(ComputerSystem vm, string mac, string vlan);
        ManagementPath AddDiskDriveToVm(ComputerSystem vm, string vhdfile, string cntrllerAddr, string driveResourceType);
        void SetState(ComputerSystem vm, ushort requiredState);
        bool DeleteSwitchPort(string elementName);
        VLANEndpointSettingData GetVlanEndpointSettings(VirtualSwitchManagementService vmNetMgmtSvc, ManagementPath newSwitchPath);
        VirtualSwitch GetExternalVirtSwitch();
        VirtualSwitchManagementService GetVirtualSwitchManagementService();
        void CreateDynamicVirtualHardDisk(ulong MaxInternalSize, string Path);
        ImageManagementService GetImageManagementService();
        VirtualSystemManagementService GetVirtualisationSystemManagementService();
        List<string> GetVmElementNames();
        ProcessorSettingData GetProcSettings(VirtualSystemSettingData vmSettings);
        MemorySettingData GetMemSettings(VirtualSystemSettingData vmSettings);
        ResourceAllocationSettingData GetIDEControllerSettings(VirtualSystemSettingData vmSettings, string cntrllerAddr);
        ResourceAllocationSettingData.ResourceAllocationSettingDataCollection GetResourceAllocationSettings(VirtualSystemSettingData vmSettings);
        SwitchPort[] GetSwitchPorts(ComputerSystem vm);
        SwitchPort GetSwitchPort(SyntheticEthernetPort nic);
        SyntheticEthernetPortSettingData[] GetEthernetPorts(ComputerSystem vm);
        VirtualSystemSettingData GetVmSettings(ComputerSystem vm);
    }
}
