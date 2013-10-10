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
using log4net;
using System.Globalization;
using System.Management;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using CloudStack.Plugin.WmiWrappers.ROOT.CIMV2;
using System.IO;

namespace HypervResource
{
    public class WmiCallsV2
    {
        public static String CloudStackUserDataKey = "cloudstack-vm-userdata";

        public static void Initialize()
        {
            // Trigger assembly load into curren appdomain
        }

        private static ILog logger = LogManager.GetLogger(typeof(WmiCallsV2));

        /// <summary>
        /// Returns ComputerSystem lacking any NICs and VOLUMEs
        /// </summary>
        public static ComputerSystem AddUserData(ComputerSystem vm, string userData)
        {
            // Obtain controller for Hyper-V virtualisation subsystem
            VirtualSystemManagementService vmMgmtSvc = GetVirtualisationSystemManagementService();

            // Create object to hold the data.
            KvpExchangeDataItem kvpItem = KvpExchangeDataItem.CreateInstance();
            kvpItem.LateBoundObject["Name"] = WmiCallsV2.CloudStackUserDataKey;
            kvpItem.LateBoundObject["Data"] = userData;
            kvpItem.LateBoundObject["Source"] = 0;
            logger.Debug("VM " + vm.Name + " gets userdata " + userData);

            // Update the resource settings for the VM.
            System.Management.ManagementBaseObject kvpMgmtObj = kvpItem.LateBoundObject;
            System.Management.ManagementPath jobPath;
            String kvpStr = kvpMgmtObj.GetText(System.Management.TextFormat.CimDtd20);
            uint ret_val = vmMgmtSvc.AddKvpItems(new String[] { kvpStr }, vm.Path, out jobPath);

            // If the Job is done asynchronously
            if (ret_val == ReturnCode.Started)
            {
                JobCompleted(jobPath);
            }
            else if (ret_val != ReturnCode.Completed)
            {
                var errMsg = string.Format(
                    "Failed to update VM {0} (GUID {1}) due to {2} (ModifyVirtualSystem call), existing VM not deleted",
                    vm.ElementName,
                    vm.Name,
                    ReturnCode.ToString(ret_val));
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            return vm;
        }

        /// <summary>
        /// Returns ComputerSystem lacking any NICs and VOLUMEs
        /// </summary>
        public static void DeleteHostKvpItem(ComputerSystem vm, string key)
        {
            // Obtain controller for Hyper-V virtualisation subsystem
            VirtualSystemManagementService vmMgmtSvc = GetVirtualisationSystemManagementService();

            // Create object to hold the data.
            KvpExchangeDataItem kvpItem = KvpExchangeDataItem.CreateInstance();
            kvpItem.LateBoundObject["Name"] = WmiCallsV2.CloudStackUserDataKey;
            kvpItem.LateBoundObject["Data"] = "dummy";
            kvpItem.LateBoundObject["Source"] = 0;
            logger.Debug("VM " + vm.Name + " will have KVP key " + key + " removed.");

            String kvpStr = kvpItem.LateBoundObject.GetText(TextFormat.CimDtd20);

            // Update the resource settings for the VM.
            ManagementPath jobPath;

            uint ret_val = vmMgmtSvc.RemoveKvpItems(new String[] { kvpStr }, vm.Path, out jobPath);

            // If the Job is done asynchronously
            if (ret_val == ReturnCode.Started)
            {
                JobCompleted(jobPath);
            }
            else if (ret_val != ReturnCode.Completed)
            {
                var errMsg = string.Format(
                    "Failed to update VM {0} (GUID {1}) due to {2} (ModifyVirtualSystem call), existing VM not deleted",
                    vm.ElementName,
                    vm.Name,
                    ReturnCode.ToString(ret_val));
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
        }

        public static VirtualSystemManagementService GetVirtualisationSystemManagementService()
        {
            // VirtualSystemManagementService is a singleton, most anonymous way of lookup is by asking for the set
            // of local instances, which should be size 1.
           
            var virtSysMgmtSvcCollection = VirtualSystemManagementService.GetInstances();
            foreach (VirtualSystemManagementService item in virtSysMgmtSvcCollection)
            {
                return item;
            }

            var errMsg = string.Format("No Hyper-V subsystem on server");
            var ex = new WmiException(errMsg);
            logger.Error(errMsg, ex);
            throw ex;
        }

        /// <summary>
        /// Similar to http://msdn.microsoft.com/en-us/library/hh850031%28v=vs.85%29.aspx
        /// </summary>
        /// <param name="jobPath"></param>
        /// <returns></returns>
        private static void JobCompleted(ManagementPath jobPath)
        {
            ConcreteJob jobObj = null;
            for(;;)
            {
                jobObj = new ConcreteJob(jobPath);
                if (jobObj.JobState != JobState.Starting && jobObj.JobState != JobState.Running)
                {
                    break;
                }
                logger.InfoFormat("In progress... {0}% completed.", jobObj.PercentComplete);
                System.Threading.Thread.Sleep(1000);
            }

            if (jobObj.JobState != JobState.Completed)
            {
                var errMsg = string.Format(
                    "Hyper-V Job failed, Error Code:{0}, Description: {1}", 
                    jobObj.ErrorCode, 
                    jobObj.ErrorDescription);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            logger.DebugFormat("WMI job succeeded: {0}, Elapsed={1}", jobObj.Description, jobObj.ElapsedTime);
        }

        public static ComputerSystem GetComputerSystem(string displayName)
        {
            var wmiQuery = String.Format("ElementName=\"{0}\"", displayName);
            ComputerSystem.ComputerSystemCollection vmCollection = ComputerSystem.GetInstances(wmiQuery);

            // Return the first one
            foreach (ComputerSystem vm in vmCollection)
            {
                return vm;
            }
            return null;
        }

        public static List<string> GetVmElementNames()
        {
            List<string> result = new List<string>();
            ComputerSystem.ComputerSystemCollection vmCollection = ComputerSystem.GetInstances();

            // Return the first one
            foreach (ComputerSystem vm in vmCollection)
            {
                if (vm.Caption.StartsWith("Hosting Computer System") )
                {
                    continue;
                }
                result.Add(vm.ElementName);
            }
            return result;
        }

        public static VirtualSystemSettingData GetVmSettings(ComputerSystem vm)
        {
            // An ASSOCIATOR object provides the cross reference from the ComputerSettings and the 
            // VirtualSystemSettingData, but generated wrappers do not expose a ASSOCIATOR OF query as a method.
            // Instead, we use the System.Management to code the equivalant of 
            //  string query = string.Format( "ASSOCIATORS OF {{{0}}} WHERE ResultClass = {1}", vm.path, resultclassName);
            //
            var wmiObjQuery = new RelatedObjectQuery(vm.Path.Path, VirtualSystemSettingData.CreatedClassName);

            // NB: default scope of ManagementObjectSearcher is '\\.\root\cimv2', which does not contain
            // the virtualisation objects.
            var wmiObjectSearch = new ManagementObjectSearcher(vm.Scope, wmiObjQuery);
            var wmiObjCollection = new VirtualSystemSettingData.VirtualSystemSettingDataCollection(wmiObjectSearch.Get());

            // When snapshots are taken into account, there can be multiple settings objects
            // take the first one that isn't a snapshot
            foreach (VirtualSystemSettingData wmiObj in wmiObjCollection)
            {
                if (wmiObj.VirtualSystemType == "Microsoft:Hyper-V:System:Realized" ||
                    wmiObj.VirtualSystemType == "Microsoft:Hyper-V:System:Planned")
                {
                    return wmiObj;
                }
            }

            var errMsg = string.Format("No VirtualSystemSettingData for VM {0}, path {1}", vm.ElementName, vm.Path.Path);
            var ex = new WmiException(errMsg);
            logger.Error(errMsg, ex);
            throw ex;
        }

        public static KvpExchangeComponentSettingData GetKvpSettings(VirtualSystemSettingData vmSettings)
        {
            // An ASSOCIATOR object provides the cross reference from the VirtualSystemSettingData and the 
            // KvpExchangeComponentSettingData, but generated wrappers do not expose a ASSOCIATOR OF query as a method.
            // Instead, we use the System.Management to code the equivalant of 
            //  string query = string.Format( "ASSOCIATORS OF {{{0}}} WHERE ResultClass = {1}", vmSettings.path, resultclassName);
            //
            var wmiObjQuery = new RelatedObjectQuery(vmSettings.Path.Path, KvpExchangeComponentSettingData.CreatedClassName);

            // NB: default scope of ManagementObjectSearcher is '\\.\root\cimv2', which does not contain
            // the virtualisation objects.
            var wmiObjectSearch = new ManagementObjectSearcher(vmSettings.Scope, wmiObjQuery);
            var wmiObjCollection = new KvpExchangeComponentSettingData.KvpExchangeComponentSettingDataCollection(wmiObjectSearch.Get());

            foreach (KvpExchangeComponentSettingData wmiObj in wmiObjCollection)
            {
                return wmiObj;
            }

            var errMsg = string.Format("No KvpExchangeComponentSettingData in VirtualSystemSettingData {0}", vmSettings.Path.Path);
            var ex = new WmiException(errMsg);
            logger.Error(errMsg, ex);
            throw ex;
        }
    }

    public class WmiException : Exception
    {
        public WmiException()
        {
        }

        public WmiException(string message)
            : base(message)
        {
        }

        public WmiException(string message, Exception inner)
            : base(message, inner)
        {
        }
    }

    /// <summary>
    /// Covers V2 API, see
    /// http://msdn.microsoft.com/en-us/library/hh850031%28v=vs.85%29.aspx
    /// </summary>
    public static class ReturnCode
    {
        public const UInt32 Completed = 0;
        public const UInt32 Started = 4096;
        public const UInt32 Failed = 32768;
        public const UInt32 AccessDenied = 32769;
        public const UInt32 NotSupported = 32770;
        public const UInt32 Unknown = 32771;
        public const UInt32 Timeout = 32772;
        public const UInt32 InvalidParameter = 32773;
        public const UInt32 SystemInUse = 32774;
        public const UInt32 InvalidState = 32775;
        public const UInt32 IncorrectDataType = 32776;
        public const UInt32 SystemNotAvailable = 32777;
        public const UInt32 OutofMemory = 32778;
        public static string ToString(UInt32 value)
        {
            string result = "Unknown return code";
            switch (value)
            {
                case Completed: result = "Completed"; break;
                case Started: result = "Started"; break;
                case Failed: result = "Failed"; break;
                case AccessDenied: result = "AccessDenied"; break;
                case NotSupported: result = "NotSupported"; break;
                case Unknown: result = "Unknown"; break;
                case Timeout: result = "Timeout"; break;
                case InvalidParameter: result = "InvalidParameter"; break;
                case SystemInUse: result = "SystemInUse"; break;
                case InvalidState: result = "InvalidState"; break;
                case IncorrectDataType: result = "IncorrectDataType"; break;
                case SystemNotAvailable: result = "SystemNotAvailable"; break;
                case OutofMemory: result = "OutofMemory"; break;
            }
            return result;
        }
    }

    /// <summary>
    /// Covers V2 API, see
    /// http://msdn.microsoft.com/en-us/library/hh850031%28v=vs.85%29.aspx
    /// </summary>
    public static class JobState
    {
        public const UInt16 New = 2;
        public const UInt16 Starting = 3;
        public const UInt16 Running = 4;
        public const UInt16 Suspended = 5;
        public const UInt16 ShuttingDown = 6;
        public const UInt16 Completed = 7;
        public const UInt16 Terminated = 8;
        public const UInt16 Killed = 9;
        public const UInt16 Exception = 10;
        public const UInt16 Service = 11;
        public static string ToString(UInt16 value)
        {
            string result = "Unknown JobState code";
            switch (value)
            {
                case New: result = "New"; break;
                case Starting: result = "Starting"; break;
                case Running: result = "Running"; break;
                case Suspended: result = "Suspended"; break;
                case ShuttingDown: result = "ShuttingDown"; break;
                case Completed: result = "Completed"; break;
                case Terminated: result = "Terminated"; break;
                case Killed: result = "Killed"; break;
                case Exception: result = "Exception"; break;
                case Service: result = "Service"; break;
            }
            return result;
        }
    }

    /// <summary>
    /// V2 API (see http://msdn.microsoft.com/en-us/library/hh850279(v=vs.85).aspx)
    /// has removed 'Paused' and 'Suspended' as compared to the
    /// V1 API (see http://msdn.microsoft.com/en-us/library/cc723874%28v=vs.85%29.aspx)
    /// However, Paused and Suspended appear on the VM state transition table
    /// (see http://msdn.microsoft.com/en-us/library/hh850116(v=vs.85).aspx#methods)
    /// </summary>
    public class RequiredState
    {
        public const UInt16 Enabled = 2;        // Turns the VM on.
        public const UInt16 Disabled = 3;       // Turns the VM off.
        public const UInt16 ShutDown = 4;
        public const UInt16 Offline = 6;
        public const UInt16 Test = 7;
        public const UInt16 Defer = 8;
        public const UInt16 Quiesce = 9;
        public const UInt16 Reboot = 10;        // A hard reset of the VM.
        public const UInt16 Reset = 11;         // For future use.
        public const UInt16 Paused = 32768;     // Pauses the VM.
        public const UInt16 Suspended = 32769;  // Saves the state of the VM.

        public static string ToString(UInt16 value)
        {
            string result = "Unknown RequiredState code";
            switch (value)
            {
                case Enabled: result = "Enabled"; break;
                case Disabled: result = "Disabled"; break;
                case ShutDown: result = "ShutDown"; break;
                case Offline: result = "Offline"; break;
                case Test: result = "Test"; break;
                case Defer: result = "Defer"; break;
                case Quiesce: result = "Quiesce"; break;
                case Reboot: result = "Reboot"; break;
                case Reset: result = "Reset"; break;
            }
            return result;
        }
    }

    /// <summary>
    /// V2 API specifies the states below in its  state transition graph at
    /// http://msdn.microsoft.com/en-us/library/hh850116(v=vs.85).aspx#methods
    /// However, the CIM standard has additional possibilities based on the description
    /// of EnabledState.
    /// The previous V1 API is described by 
    /// http://msdn.microsoft.com/en-us/library/cc136822%28v=vs.85%29.aspx
    /// </summary>
        public class EnabledState
    {
            /// <summary>
            /// The state of the VM could not be determined.
            /// </summary>
        public const UInt16 Unknown = 0;
            /// <summary>
            /// The VM is running.
            /// </summary>
        public const UInt16 Enabled = 2;
            /// <summary>
            /// The VM is turned off.
            /// </summary>
        public const UInt16 Disabled = 3;
            /// <summary>
            /// The VM is paused.
            /// </summary>
        public const UInt16 Paused = 32768;
            /// <summary>
            /// The VM is in a saved state.
            /// </summary>
        public const UInt16 Suspended = 32769;
            /// <summary>
            /// The VM is starting. This is a transitional state between 3 (Disabled)
            /// or 32769 (Suspended) and 2 (Enabled) initiated by a call to the 
            /// RequestStateChange method with a RequestedState parameter of 2 (Enabled).
            /// </summary>
        public const UInt16 Starting = 32770;
            /// <summary>
            /// Starting with Windows Server 2008 R2 this value is not supported. 
            /// If the VM is performing a snapshot operation, the element at index 1 
            /// of the OperationalStatus property array will contain 32768 (Creating Snapshot), 
            /// 32769 (Applying Snapshot), or 32770 (Deleting Snapshot).
            /// </summary>
        public const UInt16 Snapshotting = 32771;
            /// <summary>
            /// The VM is saving its state. This is a transitional state between 2 (Enabled)
            /// and 32769 (Suspended) initiated by a call to the RequestStateChange method 
            /// with a RequestedState parameter of 32769 (Suspended).
            /// </summary>
        public const UInt16 Saving = 32773;
            /// <summary>
            /// The VM is turning off. This is a transitional state between 2 (Enabled) 
            /// and 3 (Disabled) initiated by a call to the RequestStateChange method 
            /// with a RequestedState parameter of 3 (Disabled) or a guest operating system 
            /// initiated power off.
            /// </summary>
        public const UInt16 Stopping = 32774;
            /// <summary>
            /// The VM is pausing. This is a transitional state between 2 (Enabled) and 32768 (Paused) initiated by a call to the RequestStateChange method with a RequestedState parameter of 32768 (Paused).
            /// </summary>
        public const UInt16 Pausing = 32776;
            /// <summary>
            /// The VM is resuming from a paused state. This is a transitional state between 32768 (Paused) and 2 (Enabled).
            /// </summary>
        public const UInt16 Resuming = 32777;

        public static string ToString(UInt16 value)
        {
            string result = "Unknown";
            switch (value)
            {
                case Enabled: result = "Enabled"; break;
                case Disabled: result = "Disabled"; break;
                case Paused: result = "Paused"; break;
                case Suspended: result = "Suspended"; break;
                case Starting: result = "Starting"; break;
                case Snapshotting: result = "Snapshotting"; break; // NOT used
                case Saving: result = "Saving"; break;
                case Stopping: result = "Stopping"; break;
                case Pausing: result = "Pausing"; break;
                case Resuming: result = "Resuming"; break;
            }
            return result;
        }

        public static string ToCloudStackState(UInt16 value)
        {
            string result = "Unknown";
            switch (value)
            {
                case Enabled: result = "Running"; break;
                case Disabled: result = "Stopped"; break;
                case Paused: result = "Unknown"; break;
                case Suspended: result = "Unknown"; break;
                case Starting: result = "Starting"; break;
                case Snapshotting: result = "Unknown"; break; // NOT used
                case Saving: result = "Saving"; break;
                case Stopping: result = "Stopping"; break;
                case Pausing: result = "Unknown"; break;
                case Resuming: result = "Starting"; break; 
            }
            return result;
        }
    }
}
