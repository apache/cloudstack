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
        ComputerSystem AddUserData(ComputerSystem vm, string userData);
        void DeleteHostKvpItem(ComputerSystem vm, string key);
        VirtualSystemManagementService GetVirtualisationSystemManagementService();
        ComputerSystem GetComputerSystem(string displayName);        
        List<string> GetVmElementNames();
        VirtualSystemSettingData GetVmSettings(ComputerSystem vm);
        KvpExchangeComponentSettingData GetKvpSettings(VirtualSystemSettingData vmSettings);
        string GetDefaultDataRoot();
    }
}
