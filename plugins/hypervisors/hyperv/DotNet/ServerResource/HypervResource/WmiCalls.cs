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
using CloudStack.Plugin.WmiWrappers.ROOT.VIRTUALIZATION;
using log4net;
using System.Globalization;
using System.Management;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using CloudStack.Plugin.WmiWrappers.ROOT.CIMV2;
using System.IO;

namespace HypervResource
{
    public class WmiCalls
    {
        public static void Initialize()
        {
            // Trigger assembly load into curren appdomain
        }

        private static ILog logger = LogManager.GetLogger(typeof(WmiCalls));

        /// <summary>
        /// Returns ComputerSystem lacking any NICs and VOLUMEs
        /// </summary>
        public static ComputerSystem CreateVM(string name, long memory_mb, int vcpus)
        {
            // Obtain controller for Hyper-V virtualisation subsystem
            VirtualSystemManagementService vmMgmtSvc = GetVirtualisationSystemManagementService();

            // Create VM with correct name and default resources
            ComputerSystem vm = CreateDefaultVm(vmMgmtSvc, name);

            // Update the resource settings for the VM.

            // Resource settings are referenced through the Msvm_VirtualSystemSettingData object.
            VirtualSystemSettingData vmSettings = GetVmSettings(vm);

            // For memory settings, there is no Dynamic Memory, so reservation, limit and quantity are identical.
            MemorySettingData memSettings = GetMemSettings(vmSettings);
            memSettings.LateBoundObject["VirtualQuantity"] = memory_mb;
            memSettings.LateBoundObject["Reservation"] = memory_mb;
            memSettings.LateBoundObject["Limit"] = memory_mb;

            // Update the processor settings for the VM, static assignment of 100% for CPU limit
            ProcessorSettingData procSettings = GetProcSettings(vmSettings);
            procSettings.LateBoundObject["VirtualQuantity"] = vcpus;
            procSettings.LateBoundObject["Reservation"] = vcpus;
            procSettings.LateBoundObject["Limit"] = 100000;

            ModifyVmResources(vmMgmtSvc, vm, new String[] {
                memSettings.LateBoundObject.GetText(TextFormat.CimDtd20),
                procSettings.LateBoundObject.GetText(TextFormat.CimDtd20)
                });
            logger.InfoFormat("VM with display name {0} has GUID {1}", vm.ElementName, vm.Name);
            logger.DebugFormat("Resources for vm {0}: {1} MB memory, {2} vcpus", name, memory_mb, vcpus);

            return vm;
        }

        /// <summary>
        /// Create a (synthetic) nic, and attach it to the vm
        /// </summary>
        /// <param name="vm"></param>
        /// <param name="mac"></param>
        /// <param name="vlan"></param>
        /// <returns></returns>
        public static SyntheticEthernetPortSettingData CreateNICforVm(ComputerSystem vm, string mac, string vlan)
        {
            logger.DebugFormat("Creating nic for VM {0} (GUID {1})", vm.ElementName, vm.Name);

            // Obtain controller for Hyper-V networking subsystem
            VirtualSwitchManagementService vmNetMgmtSvc = GetVirtualSwitchManagementService();

            // Create NIC resource by cloning the default NIC 
            var synthNICsSettings = SyntheticEthernetPortSettingData.GetInstances(vmNetMgmtSvc.Scope, "InstanceID LIKE \"%Default\"");

            // Assert
            if (synthNICsSettings.Count != 1)
            {
                var errMsg = string.Format("Internal error, coudl not find default SyntheticEthernetPort instance");
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
            var defaultSynthNICSettings = synthNICsSettings.OfType<SyntheticEthernetPortSettingData>().First();

            var newSynthNICSettings = new SyntheticEthernetPortSettingData((ManagementBaseObject)defaultSynthNICSettings.LateBoundObject.Clone());

            // Get the virtual switch
            VirtualSwitch vSwitch = GetExternalVirtSwitch();

            // Crate switch port for new VM
            ManagementPath newSwitchPath = CreateSwitchPortForVm(vm, vmNetMgmtSvc, vSwitch);

            // Add required VLAND support
            if (vlan != null)
            {
                SetPortVlan(vlan, vmNetMgmtSvc, newSwitchPath);
            }

            logger.DebugFormat("Created switch port {0} on switch {1}", newSwitchPath.Path, vSwitch.Path.Path);

            //  Assign configuration to new NIC
            string normalisedMAC = string.Join("", (mac.Split(new char[] { ':' })));
            newSynthNICSettings.LateBoundObject["Connection"] = new string[] { newSwitchPath.Path };
            newSynthNICSettings.LateBoundObject["ElementName"] = vm.ElementName;
            newSynthNICSettings.LateBoundObject["Address"] = normalisedMAC;
            newSynthNICSettings.LateBoundObject["StaticMacAddress"] = "TRUE";
            newSynthNICSettings.LateBoundObject["VirtualSystemIdentifiers"] = new string[] { "{" + Guid.NewGuid().ToString() + "}" };
            newSynthNICSettings.CommitObject();

            // Insert NIC into vm
            string[] newResources = new string[] { newSynthNICSettings.LateBoundObject.GetText(System.Management.TextFormat.CimDtd20)};
            ManagementPath[] newResourcePaths = AddVirtualResource(newResources, vm );

            // assert
            if (newResourcePaths.Length != 1)
            {
                var errMsg = string.Format(
                    "Failed to properly insert a single NIC on VM {0} (GUID {1}): number of resource created {2}",
                    vm.ElementName,
                    vm.Name,
                    newResourcePaths.Length);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            return new SyntheticEthernetPortSettingData(newResourcePaths[0]);
        }

        public const string IDE_HARDDISK_DRIVE = "Microsoft Synthetic Disk Drive";
        public const string IDE_ISO_DRIVE  = "Microsoft Synthetic DVD Drive";

        public const string IDE_ISO_DISK = "Microsoft Virtual CD/DVD Disk"; // For IDE_ISO_DRIVE
        public const string IDE_HARDDISK_DISK = "Microsoft Virtual Hard Disk"; // For IDE_HARDDISK_DRIVE

        /// <summary>
        /// Create new VM.  By default we start it. 
        /// </summary>
        public static ComputerSystem DeployVirtualMachine(dynamic jsonObj)
        {
            var vmInfo = jsonObj.vm;
            string vmName = vmInfo.name;
            var nicInfo = vmInfo.nics;
            int vcpus = vmInfo.cpus;
            int memSize = vmInfo.maxRam / 1048576;
            string errMsg = vmName;
            var diskDrives = vmInfo.disks;

            // assert
            errMsg = vmName + ": missing disk information, array empty or missing, agent expects *at least* one disk for a VM";
            if (diskDrives == null)
            {
                logger.Error(errMsg);
                throw new ArgumentException(errMsg);
            }
            // assert
            errMsg = vmName + ": missing NIC information, array empty or missing, agent expects at least an empty array.";
            if (nicInfo == null )
            {
                logger.Error(errMsg);
                throw new ArgumentException(errMsg);
            }


            // For existing VMs, return when we spot one of this name not stopped.  In the meantime, remove any existing VMs of same name.
            ComputerSystem vmWmiObj = null;
            while ((vmWmiObj = GetComputerSystem(vmName)) != null)
            {
                logger.WarnFormat("Create request for existing vm, name {0}", vmName);
                if (vmWmiObj.EnabledState == EnabledState.Disabled)
                {
                    logger.InfoFormat("Deleting existing VM with name {0}, before we go on to create a VM with the same name", vmName);
                    DestroyVm(vmName);
                }
                else
                {
                    // TODO: revise exception type
                    errMsg = string.Format("Create VM failing, because there exists a VM with name {0}, state {1}", 
                        vmName,
                        EnabledState.ToString(vmWmiObj.EnabledState));
                    var ex = new WmiException(errMsg);
                    logger.Error(errMsg, ex);
                    throw ex;
                }
            }

            // Create vm carcase
            logger.DebugFormat("Going ahead with create VM {0}, {1} vcpus, {2}MB RAM", vmName, vcpus, memSize);
            var newVm = WmiCalls.CreateVM(vmName, memSize, vcpus);

            foreach (var diskDrive in diskDrives)
            {
                string vhdFile = null;
                string diskName = null;
                VolumeObjectTO volInfo = VolumeObjectTO.ParseJson(diskDrive.data);
                if (volInfo != null)
                {
                    // assert
                    errMsg = vmName + ": volume missing primaryDataStore for disk " + diskDrive.ToString();
                    if (volInfo.primaryDataStore == null)
                    {
                        logger.Error(errMsg);
                        throw new ArgumentException(errMsg);
                    }
                    diskName = volInfo.name;

                    // assert
                    errMsg = vmName + ": can't deal with DataStore type for disk " + diskDrive.ToString();
                    if (volInfo.primaryDataStore == null)
                    {
                        logger.Error(errMsg);
                        throw new ArgumentException(errMsg);
                    }
                    errMsg = vmName + ": Malformed PrimaryDataStore for disk " + diskDrive.ToString();
                    if (String.IsNullOrEmpty(volInfo.primaryDataStore.path))
                    {
                        logger.Error(errMsg);
                        throw new ArgumentException(errMsg);
                    }
                    errMsg = vmName + ": Missing folder PrimaryDataStore for disk " + diskDrive.ToString() + ", missing path: " +  volInfo.primaryDataStore.path;
                    if (!Directory.Exists(volInfo.primaryDataStore.path))
                    {
                        logger.Error(errMsg);
                        throw new ArgumentException(errMsg);
                    }

                    vhdFile = volInfo.FullFileName;
                    if (!System.IO.File.Exists(vhdFile))
                    {
                        errMsg = vmName + ": non-existent volume, missing " + vhdFile + " for drive " + diskDrive.ToString();
                        logger.Error(errMsg);
                        throw new ArgumentException(errMsg);
                    }
                    logger.Debug("Going to create " + vmName + " with attached voluem " + diskName + " at " + vhdFile);
                }

                string driveType = diskDrive.type;

                string ideCtrllr = "0";
                string driveResourceType = null;
                switch (driveType) {
                    case "ROOT":
                        ideCtrllr = "0";
                        driveResourceType = IDE_HARDDISK_DRIVE;
                        break;
                    case "ISO":
                        ideCtrllr = "1";
                        driveResourceType = IDE_ISO_DRIVE;
                        break;
                    default: 
                        // TODO: double check exception type
                        errMsg = string.Format("Unknown disk type {0} for disk {1}, vm named {2}", 
                                string.IsNullOrEmpty(driveType) ? "NULL" : driveType,
                                string.IsNullOrEmpty(diskName) ? "NULL" : diskName, vmName);
                        var ex = new WmiException(errMsg);
                        logger.Error(errMsg, ex);
                        throw ex;
                }
                logger.DebugFormat("Create disk type {1} (Named: {0}), on vm {2} {3}", diskName, driveResourceType, vmName, 
                                        string.IsNullOrEmpty(vhdFile) ? " no disk to insert" : ", inserting disk" +vhdFile );
                AddDiskDriveToVm(newVm, vhdFile, ideCtrllr, driveResourceType);
            }

            // add nics
            foreach (var nic in nicInfo)
            {
                string mac = nic.mac;
                string vlan = null;
                string isolationUri = nic.isolationUri;
                if (isolationUri != null && isolationUri.StartsWith("vlan://") && !isolationUri.Equals("vlan://untagged"))
                {
                    vlan = isolationUri.Substring("vlan://".Length);
                    int tmp;
                    if (!int.TryParse(vlan, out tmp))
                    {
                        // TODO: double check exception type
                        errMsg = string.Format("Invalid VLAN value {0} for on vm {1} for nic uuid {2}", isolationUri, vmName, nic.uuid);
                        var ex = new WmiException(errMsg);
                        logger.Error(errMsg, ex);
                        throw ex;
                    }
                }
                CreateNICforVm(newVm, mac, vlan);
            }

            logger.DebugFormat("Starting VM {0}", vmName);
            SetState(newVm, RequiredState.Enabled);
            logger.InfoFormat("Started VM {0}", vmName);
            return newVm;
       }

        /// <summary>
        /// Create a disk and attach it to the vm
        /// </summary>
        /// <param name="vm"></param>
        /// <param name="cntrllerAddr"></param>
        /// <param name="driveResourceType">IDE_HARDDISK_DRIVE or IDE_ISO_DRIVE</param>
        public static ManagementPath AddDiskDriveToVm(ComputerSystem vm, string vhdfile, string cntrllerAddr, string driveResourceType)
        {
            logger.DebugFormat("Creating DISK for VM {0} (GUID {1}) by attaching {2}", 
                        vm.ElementName,
                        vm.Name,
                        vhdfile);

            // Determine disk type for drive and assert drive type valid
            string diskResourceSubType = null;
            switch(driveResourceType) {
                case IDE_HARDDISK_DRIVE:
                    diskResourceSubType = IDE_HARDDISK_DISK;
                    break;
                case IDE_ISO_DRIVE: 
                    diskResourceSubType = IDE_ISO_DISK;
                    break;
                default:
                    var errMsg = string.Format(
                        "Unrecognised disk drive type {0} for VM {1} (GUID {2})",
                        string.IsNullOrEmpty(driveResourceType) ? "NULL": driveResourceType, 
                        vm.ElementName,
                        vm.Name);
                    var ex = new WmiException(errMsg);
                    logger.Error(errMsg, ex);
                    throw ex;
            }

            ManagementPath newDrivePath = AttachNewDriveToVm(vm, cntrllerAddr, driveResourceType);

            // If there's not disk to insert, we are done.
            if (String.IsNullOrEmpty(vhdfile))
            {
                logger.DebugFormat("No disk to be added to drive, disk drive {0} is complete", newDrivePath.Path);
            }
            else
            {
                InsertDiskImage(vm, vhdfile, diskResourceSubType, newDrivePath);
            }
            return newDrivePath;
    }

        private static ManagementPath AttachNewDriveToVm(ComputerSystem vm, string cntrllerAddr, string driveType)
        {
            // Disk drives are attached to a 'Parent' IDE controller.  We IDE Controller's settings for the 'Path', which our new Disk drive will use to reference it.
            VirtualSystemSettingData vmSettings = GetVmSettings(vm);
            var ctrller = GetIDEControllerSettings(vmSettings, cntrllerAddr);

            // A description of the drive is created by modifying a clone of the default ResourceAllocationSettingData for that drive type
            string defaultDriveQuery = String.Format("ResourceSubType LIKE \"{0}\" AND InstanceID LIKE \"%Default\"", driveType);
            var newDiskDriveSettings = CloneResourceAllocationSetting(defaultDriveQuery);

            // Set IDE controller and address on the controller for the new drive
            newDiskDriveSettings.LateBoundObject["Parent"] = ctrller.Path.ToString();
            newDiskDriveSettings.LateBoundObject["Address"] = "0";
            newDiskDriveSettings.CommitObject();

            // Add this new disk drive to the VM
            logger.DebugFormat("Creating disk drive type {0}, parent IDE controller is {1} and address on controller is {2}",
                newDiskDriveSettings.ResourceSubType,
                newDiskDriveSettings.Parent,
                newDiskDriveSettings.Address);
            string[] newDriveResource = new string[] { newDiskDriveSettings.LateBoundObject.GetText(System.Management.TextFormat.CimDtd20) };
            ManagementPath[] newDrivePaths = AddVirtualResource(newDriveResource, vm);

            // assert
            if (newDrivePaths.Length != 1)
            {
                var errMsg = string.Format(
                    "Failed to add disk drive type {3} to VM {0} (GUID {1}): number of resource created {2}",
                    vm.ElementName,
                    vm.Name,
                    newDrivePaths.Length,
                    driveType);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
            logger.DebugFormat("New disk drive type {0} WMI path is {1}s",
                newDiskDriveSettings.ResourceSubType,
                newDrivePaths[0].Path);
            return newDrivePaths[0];
        }

        /// <summary>
        /// Attach iso to the vm
        /// </summary>
        /// <param name="vm"></param>
        /// <param name="isoPath"></param>
        private static void AttachIsoToVm(ComputerSystem vm, string isoPath)
        {
            // Disk drives are attached to a 'Parent' IDE controller.  We IDE Controller's settings for the 'Path', which our new Disk drive will use to reference it.
            VirtualSystemSettingData vmSettings = GetVmSettings(vm);
            var ctrller = GetDvdDriveSettings(vmSettings);

            // A description of the drive is created by modifying a clone of the default ResourceAllocationSettingData for that drive type
            string defaultDiskQuery = String.Format("ResourceSubType LIKE \"{0}\" AND InstanceID LIKE \"%Default\"", IDE_ISO_DISK);
            var newDiskSettings = CloneResourceAllocationSetting(defaultDiskQuery);

            // Set IDE controller and address on the controller for the new drive
            newDiskSettings.LateBoundObject["Parent"] = ctrller.Path.ToString();
            newDiskSettings.LateBoundObject["Connection"] = new string[] { isoPath };
            newDiskSettings.CommitObject();

            // Add the new vhd object as a virtual hard disk to the vm.
            string[] newDiskResource = new string[] { newDiskSettings.LateBoundObject.GetText(System.Management.TextFormat.CimDtd20) };
            ManagementPath[] newDiskPaths = AddVirtualResource(newDiskResource, vm);
            // assert
            if (newDiskPaths.Length != 1)
            {
                var errMsg = string.Format(
                    "Failed to add disk image to VM {0} (GUID {1}): number of resource created {2}",
                    vm.ElementName,
                    vm.Name,
                    newDiskPaths.Length);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
            logger.InfoFormat("Created disk {2} for VM {0} (GUID {1}), image {3} ",
                    vm.ElementName,
                    vm.Name,
                    newDiskPaths[0].Path,
                    isoPath);
        }

        private static void InsertDiskImage(ComputerSystem vm, string vhdfile, string diskResourceSubType, ManagementPath drivePath)
        {
            // A description of the disk is created by modifying a clone of the default ResourceAllocationSettingData for that disk type
            string defaultDiskQuery = String.Format("ResourceSubType LIKE \"{0}\" AND InstanceID LIKE \"%Default\"", diskResourceSubType);
            var newDiskSettings = CloneResourceAllocationSetting(defaultDiskQuery);

            // Set disk drive and VHD file on disk for new disk
            newDiskSettings.LateBoundObject["Parent"] = drivePath.Path;
            newDiskSettings.LateBoundObject["Connection"] = new string[] { vhdfile };
            newDiskSettings.CommitObject();

            // Add the new vhd object as a virtual hard disk to the vm.
            string[] newDiskResource = new string[] { newDiskSettings.LateBoundObject.GetText(System.Management.TextFormat.CimDtd20) };
            ManagementPath[] newDiskPaths = AddVirtualResource(newDiskResource, vm);
            // assert
            if (newDiskPaths.Length != 1)
            {
                var errMsg = string.Format(
                    "Failed to add disk image type {3} to VM {0} (GUID {1}): number of resource created {2}",
                    vm.ElementName,
                    vm.Name,
                    newDiskPaths.Length,
                    diskResourceSubType);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
            logger.InfoFormat("Created disk {2} for VM {0} (GUID {1}), image {3} ",
                    vm.ElementName,
                    vm.Name,
                    newDiskPaths[0].Path,
                    vhdfile);
        }

        private static ResourceAllocationSettingData CloneResourceAllocationSetting(string wmiQuery)
        {
            var defaultDiskDriveSettingsObjs = ResourceAllocationSettingData.GetInstances(wmiQuery);

            // assert
            if (defaultDiskDriveSettingsObjs.Count != 1)
            {
                var errMsg = string.Format("Failed to find Msvm_ResourceAllocationSettingData for the query {0}", wmiQuery);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            ResourceAllocationSettingData defaultDiskDriveSettings = defaultDiskDriveSettingsObjs.OfType<ResourceAllocationSettingData>().First();
            return new ResourceAllocationSettingData((ManagementBaseObject)defaultDiskDriveSettings.LateBoundObject.Clone());
        }

        public static void AttachIso(string displayName, string iso)
        {
            logger.DebugFormat("Got request to attach iso {0} to vm {1}", iso, displayName);

            ComputerSystem vm = GetComputerSystem(displayName);
            if (vm == null)
            {
                logger.DebugFormat("VM {0} not found", displayName);
                return;
            }
            else
            {
                AttachIsoToVm(vm, iso);
            }
        }

        public static void DestroyVm(dynamic jsonObj)
        {
            string vmToDestroy = jsonObj.vmName;
            DestroyVm(vmToDestroy);
        }
        
        /// <summary>
        /// Remove all VMs and all SwitchPorts with the displayName.  VHD gets deleted elsewhere.
        /// </summary>
        /// <param name="displayName"></param>
        public static void DestroyVm(string displayName)
        {
            logger.DebugFormat("Got request to destroy vm {0}", displayName);

            var vm = GetComputerSystem(displayName);
            if ( vm  == null )
            {
                logger.DebugFormat("VM {0} already destroyed (or never existed)", displayName);
                return;
            }

            // Stop VM
            logger.DebugFormat("Stop VM {0} (GUID {1})", vm.ElementName, vm.Name);
            SetState(vm, RequiredState.Disabled);

            // Delete SwitchPort
            DeleteSwitchPort(vm.ElementName);

            // Delete VM
            var virtSysMgmtSvc = GetVirtualisationSystemManagementService();
            ManagementPath jobPath;

            do
            {
                logger.DebugFormat("Delete VM {0} (GUID {1})", vm.ElementName, vm.Name);
                var ret_val = virtSysMgmtSvc.DestroyVirtualSystem(vm.Path, out jobPath);

                if (ret_val == ReturnCode.Started)
                {
                    JobCompleted(jobPath);
                }
                else if (ret_val != ReturnCode.Completed)
                {
                    var errMsg = string.Format(
                        "Failed Delete VM {0} (GUID {1}) due to {2}",
                        vm.ElementName,
                        vm.Name,
                        ReturnCode.ToString(ret_val));
                    var ex = new WmiException(errMsg);
                    logger.Error(errMsg, ex);
                    throw ex;
                }
                vm = GetComputerSystem(displayName);
            }
            while (vm != null);
        }

        public static void SetState(ComputerSystem vm, ushort requiredState)
        {
            logger.InfoFormat(
                "Changing state of {0} (GUID {1}) to {2}", 
                vm.ElementName, 
                vm.Name,  
                RequiredState.ToString(requiredState));

            ManagementPath jobPath;
            // TimeSpan is a value type; default ctor is equivalent to 0.
            var ret_val = vm.RequestStateChange(requiredState, new TimeSpan(), out jobPath);

            // If the Job is done asynchronously
            if (ret_val == ReturnCode.Started)
            {
                JobCompleted(jobPath);
            }
            else if (ret_val == 32775)
            {
                logger.InfoFormat("RequestStateChange returned 32775, which means vm in wrong state for requested state change.  Treating as if requested state was reached");
            }
            else if (ret_val != ReturnCode.Completed)
            {
                var errMsg = string.Format(
                    "Failed to change state of VM {0} (GUID {1}) to {2} due to {3}",
                    vm.ElementName,
                    vm.Name,
                    RequiredState.ToString(requiredState),
                    ReturnCode.ToString(ret_val));
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            logger.InfoFormat(
                "Successfully changed vm state of {0} (GUID {1} to requested state {2}", 
                vm.ElementName, 
                vm.Name,  
                requiredState);
        }


        //TODO:  Write method to delete SwitchPort based on Name
        public static bool DeleteSwitchPort(string elementName)
        {
            var virtSwitchMgmtSvc = GetVirtualSwitchManagementService();
            // Get NIC path
            var condition = string.Format("ElementName=\"{0}\"", elementName);
            var switchPortCollection = SwitchPort.GetInstances(virtSwitchMgmtSvc.Scope, condition);
            if (switchPortCollection.Count == 0)
            {
                return true;
            }

            foreach (SwitchPort port in switchPortCollection)
            {
                // Destroy
                var ret_val = virtSwitchMgmtSvc.DeleteSwitchPort(port.Path);

                if (ret_val != ReturnCode.Completed)
                {
                    return false;
                }
            }

            return true;
        }

        // Add new 
        private static ManagementPath[] AddVirtualResource(string[] resourceSettings, ComputerSystem vm )
        {
            var virtSysMgmtSvc = GetVirtualisationSystemManagementService();

            ManagementPath jobPath;
            ManagementPath[] resourcePaths;
            var ret_val = virtSysMgmtSvc.AddVirtualSystemResources(
                resourceSettings, 
                vm.Path,
                out jobPath,
                out resourcePaths);

            // If the Job is done asynchronously
            if (ret_val == ReturnCode.Started)
            {
                JobCompleted(jobPath);
            }
            else if (ret_val != ReturnCode.Completed)
            {
                var errMsg = string.Format(
                    "Failed to add resources to VM {0} (GUID {1}) due to {2}",
                    vm.ElementName,
                    vm.Name,
                    ReturnCode.ToString(ret_val));
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            return resourcePaths;
        }

        private static ManagementPath CreateSwitchPortForVm(ComputerSystem vm, VirtualSwitchManagementService vmNetMgmtSvc, VirtualSwitch vSwitch)
        {
            ManagementPath newSwitchPath = null;
            var ret_val = vmNetMgmtSvc.CreateSwitchPort(
                vm.ElementName,
                Guid.NewGuid().ToString(),
                "",
                vSwitch.Path,
                out newSwitchPath);
            // Job is always done synchronously
            if (ret_val != ReturnCode.Completed)
            {
                var errMsg = string.Format(
                    "Failed to create switch for NIC on VM {0} (GUID {1}), error code {2}",
                    vm.ElementName,
                    vm.Name,
                    ret_val);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
            return newSwitchPath;
        }

        // add vlan support by setting AccessVLAN on VLANEndpointSettingData for port
        private static void SetPortVlan(string vlan, VirtualSwitchManagementService vmNetMgmtSvc, ManagementPath newSwitchPath)
        {
            logger.DebugFormat("Setting VLAN to {0}", vlan);

            VLANEndpointSettingData vlanEndpointSettings = GetVlanEndpointSettings(vmNetMgmtSvc, newSwitchPath);
            vlanEndpointSettings.LateBoundObject["AccessVLAN"] = vlan;
            vlanEndpointSettings.CommitObject();
        }

        public static VLANEndpointSettingData GetVlanEndpointSettings(VirtualSwitchManagementService vmNetMgmtSvc, ManagementPath newSwitchPath)
        {
            // Get Msvm_VLANEndpoint through associated with new Port
            var vlanEndpointQuery = new RelatedObjectQuery(newSwitchPath.Path, VLANEndpoint.CreatedClassName);
            var vlanEndpointSearch = new ManagementObjectSearcher(vmNetMgmtSvc.Scope, vlanEndpointQuery);
            var vlanEndpointCollection = new VLANEndpoint.VLANEndpointCollection(vlanEndpointSearch.Get());

            // assert
            if (vlanEndpointCollection.Count != 1)
            {
                var errMsg = string.Format("No VLANs for vSwitch on Hyper-V server for switch {0}", newSwitchPath.Path);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            VLANEndpoint vlanEndpoint = vlanEndpointCollection.OfType<VLANEndpoint>().First();

            // Get Msvm_VLANEndpointSettingData assocaited with Msvm_VLANEndpoint
            var vlanEndpointSettingsQuery = new RelatedObjectQuery(vlanEndpoint.Path.Path, VLANEndpointSettingData.CreatedClassName);
            var vlanEndpointSettingsSearch = new ManagementObjectSearcher(vmNetMgmtSvc.Scope, vlanEndpointSettingsQuery);
            var vlanEndpointSettingsCollection = new VLANEndpointSettingData.VLANEndpointSettingDataCollection(vlanEndpointSettingsSearch.Get());

            // assert
            if (vlanEndpointSettingsCollection.Count != 1)
            {
                var errMsg = string.Format("Internal error, VLAN for vSwitch not setup propertly Hyper-V");
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            VLANEndpointSettingData vlanEndpointSettings = vlanEndpointSettingsCollection.OfType<VLANEndpointSettingData>().First();
            return vlanEndpointSettings;
        }

        /// <summary>
        /// External VSwitch has an external NIC, and we assume there is only one external NIC
        /// </summary>
        /// <param name="vmSettings"></param>
        /// <returns></returns>
        /// <throw>Throws if there is no vswitch</throw>
        public static VirtualSwitch GetExternalVirtSwitch()
        {
            // Work back from the first *bound* external NIC we find.
            var externNICs = ExternalEthernetPort.GetInstances("IsBound = TRUE");

            if (externNICs.Count == 0 )
            {
                var errMsg = "No ExternalEthernetPort available to Hyper-V";
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            ExternalEthernetPort externNIC = externNICs.OfType<ExternalEthernetPort>().First();

            // A sequence of ASSOCIATOR objects need to be traversed to get from external NIC the vswitch.
            // We use ManagementObjectSearcher objects to execute this sequence of questions
            // NB: default scope of ManagementObjectSearcher is '\\.\root\cimv2', which does not contain
            // the virtualisation objects.
            var endpointQuery = new RelatedObjectQuery(externNIC.Path.Path, SwitchLANEndpoint.CreatedClassName);
            var endpointSearch = new ManagementObjectSearcher(externNIC.Scope, endpointQuery);
            var endpointCollection = new SwitchLANEndpoint.SwitchLANEndpointCollection(endpointSearch.Get());

            // assert
            if (endpointCollection.Count < 1 )
            {
                var errMsg = string.Format("No SwitchLANEndpoint for external NIC {0} on Hyper-V server", externNIC.Name);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            SwitchLANEndpoint endPoint = endpointCollection.OfType<SwitchLANEndpoint>().First();
            var switchPortQuery = new RelatedObjectQuery(endPoint.Path.Path, SwitchPort.CreatedClassName);
            var switchPortSearch = new ManagementObjectSearcher(externNIC.Scope, switchPortQuery);
            var switchPortCollection = new SwitchPort.SwitchPortCollection(switchPortSearch.Get());

            // assert
            if (switchPortCollection.Count < 1 )
            {
                var errMsg = string.Format("No SwitchPort for external NIC {0} on Hyper-V server", externNIC.Name);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            SwitchPort switchPort = switchPortCollection.OfType<SwitchPort>().First();
            var vSwitchQuery = new RelatedObjectQuery(switchPort.Path.Path, VirtualSwitch.CreatedClassName);
            var vSwitchSearch = new ManagementObjectSearcher(externNIC.Scope, vSwitchQuery);
            var vSwitchCollection = new VirtualSwitch.VirtualSwitchCollection(vSwitchSearch.Get());

            // assert
            if (vSwitchCollection.Count < 1)
            {
                var errMsg = string.Format("No virtual switch for external NIC {0} on Hyper-V server", externNIC.Name);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            VirtualSwitch vSwitch = vSwitchCollection.OfType<VirtualSwitch>().First();

            return vSwitch;
        }


        private static void ModifyVmResources(VirtualSystemManagementService vmMgmtSvc, ComputerSystem vm, string[] resourceSettings)
        {
            // Resource settings are changed through the management service
            System.Management.ManagementPath jobPath;

            var ret_val = vmMgmtSvc.ModifyVirtualSystemResources(vm.Path,
                resourceSettings,
                out jobPath);
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

        private static ComputerSystem CreateDefaultVm(VirtualSystemManagementService vmMgmtSvc, string name)
        {
            // Tweak default settings by basing new VM on default global setting object 
            // with designed display name.

            VirtualSystemGlobalSettingData vs_gs_data = VirtualSystemGlobalSettingData.CreateInstance();
            vs_gs_data.LateBoundObject["ElementName"] = name;

            System.Management.ManagementPath jobPath;
            System.Management.ManagementPath defined_sys;
            var ret_val = vmMgmtSvc.DefineVirtualSystem(
                new string[0],
                null,
                vs_gs_data.LateBoundObject.GetText(System.Management.TextFormat.CimDtd20),
                out defined_sys,
                out jobPath);

            // If the Job is done asynchronously
            if (ret_val == ReturnCode.Started)
            {
                JobCompleted(jobPath);
            }
            else if (ret_val != ReturnCode.Completed)
            {
                var errMsg = string.Format(
                    "Failed to create VM {0} due to {1} (DefineVirtualSystem call)",
                    name, ReturnCode.ToString(ret_val));
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            logger.DebugFormat(CultureInfo.InvariantCulture, "Created VM {0}", name);

            // Is the defined_system real?
            var vm = new ComputerSystem(defined_sys);

            // Assertion
            if (vm.ElementName.CompareTo(name) != 0)
            {
                var errMsg = string.Format(
                    "New VM created with wrong name (is {0}, should be {1}, GUID {2})",
                    vm.ElementName,
                    name,
                    vm.Name);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            return vm;
        }

        public static VirtualSwitchManagementService GetVirtualSwitchManagementService()
        {
            // VirtualSwitchManagementService is a singleton, most anonymous way of lookup is by asking for the set
            // of local instances, which should be size 1.
            var virtSwtichSvcCollection = VirtualSwitchManagementService.GetInstances();
            foreach (VirtualSwitchManagementService item in virtSwtichSvcCollection)
            {
                return item;
            }

            var errMsg = string.Format("No Hyper-V subsystem on server");
            var ex = new WmiException(errMsg);
            logger.Error(errMsg, ex);
            throw ex;
        }

        public static void CreateDynamicVirtualHardDisk(ulong MaxInternalSize, string Path)
        {
            // Resource settings are changed through the management service
            System.Management.ManagementPath jobPath;
            var imgMgr = GetImageManagementService();
            var ret_val = imgMgr.CreateDynamicVirtualHardDisk(MaxInternalSize, Path, out jobPath);
            // If the Job is done asynchronously
            if (ret_val == ReturnCode.Started)
            {
                JobCompleted(jobPath);
            }
            else if (ret_val != ReturnCode.Completed)
            {
                var errMsg = string.Format(
                    "Failed to CreateDynamicVirtualHardDisk size {0}, path {1} to {2}",
                    MaxInternalSize,
                    Path,
                    ReturnCode.ToString(ret_val));
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
        }

        public static ImageManagementService GetImageManagementService()
        {
            // VirtualSystemManagementService is a singleton, most anonymous way of lookup is by asking for the set
            // of local instances, which should be size 1.

            var coll = ImageManagementService.GetInstances();
            foreach (ImageManagementService item in coll)
            {
                return item;
            }

            var errMsg = string.Format("No Hyper-V subsystem on server");
            var ex = new WmiException(errMsg);
            logger.Error(errMsg, ex);
            throw ex;
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

        public static void GetProcessorResources(out uint cores, out uint mhz)
        {
            //  Processor processors
            cores = 0;
            mhz = 0;
            Processor.ProcessorCollection procCol = Processor.GetInstances();
            foreach (Processor procInfo in procCol)
            {
                cores += procInfo.NumberOfCores;
                mhz = procInfo.MaxClockSpeed;
           }
        }
        
        public static void GetProcessorUsageInfo(out double cpuUtilization)
        {
            PerfFormattedData_Counters_ProcessorInformation.PerfFormattedData_Counters_ProcessorInformationCollection coll = 
                            PerfFormattedData_Counters_ProcessorInformation.GetInstances("Name=\"_Total\"");
            cpuUtilization = 100;
            // Use the first one
            foreach (PerfFormattedData_Counters_ProcessorInformation procInfo in coll)
            {
                // Idle during a given internal 
                // See http://library.wmifun.net/cimv2/win32_perfformatteddata_counters_processorinformation.html
                cpuUtilization = 100.0 - (double)procInfo.PercentIdleTime;            
            }
        }


        public static void GetMemoryResources(out ulong physicalRamKBs, out ulong freeMemoryKBs)
        {
            OperatingSystem0 os = new OperatingSystem0();
            physicalRamKBs = os.TotalVisibleMemorySize;
            freeMemoryKBs = os.FreePhysicalMemory;
        }

        public static string GetDefaultVirtualDiskFolder()
        {
            VirtualSystemManagementServiceSettingData.VirtualSystemManagementServiceSettingDataCollection coll = VirtualSystemManagementServiceSettingData.GetInstances();
            string defaultVirtualHardDiskPath = null;
            foreach (VirtualSystemManagementServiceSettingData settings in coll)
            {
                return settings.DefaultVirtualHardDiskPath;
            }

            // assert
            if (!System.IO.Directory.Exists(defaultVirtualHardDiskPath) ){
                var errMsg = string.Format(
                    "Hyper-V DefaultVirtualHardDiskPath is invalid!");
                logger.Error(errMsg);
                return null;
            }
            
            return defaultVirtualHardDiskPath;
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

        public static ProcessorSettingData GetProcSettings(VirtualSystemSettingData vmSettings)
        {
            // An ASSOCIATOR object provides the cross reference from the VirtualSystemSettingData and the 
            // ProcessorSettingData, but generated wrappers do not expose a ASSOCIATOR OF query as a method.
            // Instead, we use the System.Management to code the equivalant of 
            //  string query = string.Format( "ASSOCIATORS OF {{{0}}} WHERE ResultClass = {1}", vmSettings.path, resultclassName);
            //
            var wmiObjQuery = new RelatedObjectQuery(vmSettings.Path.Path, ProcessorSettingData.CreatedClassName);

            // NB: default scope of ManagementObjectSearcher is '\\.\root\cimv2', which does not contain
            // the virtualisation objects.
            var wmiObjectSearch = new ManagementObjectSearcher(vmSettings.Scope, wmiObjQuery);
            var wmiObjCollection = new ProcessorSettingData.ProcessorSettingDataCollection(wmiObjectSearch.Get());

            foreach (ProcessorSettingData wmiObj in wmiObjCollection)
            {
                return wmiObj;
            }

            var errMsg = string.Format("No ProcessorSettingData in VirtualSystemSettingData {0}", vmSettings.Path.Path);
            var ex = new WmiException(errMsg);
            logger.Error(errMsg, ex);
            throw ex;
        }

        public static MemorySettingData GetMemSettings(VirtualSystemSettingData vmSettings)
        {
            // An ASSOCIATOR object provides the cross reference from the VirtualSystemSettingData and the 
            // MemorySettingData, but generated wrappers do not expose a ASSOCIATOR OF query as a method.
            // Instead, we use the System.Management to code the equivalant of 
            //  string query = string.Format( "ASSOCIATORS OF {{{0}}} WHERE ResultClass = {1}", vmSettings.path, resultclassName);
            //
            var wmiObjQuery = new RelatedObjectQuery(vmSettings.Path.Path, MemorySettingData.CreatedClassName);

            // NB: default scope of ManagementObjectSearcher is '\\.\root\cimv2', which does not contain
            // the virtualisation objects.
            var wmiObjectSearch = new ManagementObjectSearcher(vmSettings.Scope, wmiObjQuery);
            var wmiObjCollection = new MemorySettingData.MemorySettingDataCollection(wmiObjectSearch.Get());

            foreach (MemorySettingData wmiObj in wmiObjCollection)
            {
                return wmiObj;
            }

            var errMsg = string.Format("No MemorySettingData in VirtualSystemSettingData {0}", vmSettings.Path.Path);
            var ex = new WmiException(errMsg);
            logger.Error(errMsg, ex);
            throw ex;
        }

        public static ResourceAllocationSettingData GetDvdDriveSettings(VirtualSystemSettingData vmSettings)
        {
            var wmiObjCollection = GetResourceAllocationSettings(vmSettings);

            foreach (ResourceAllocationSettingData wmiObj in wmiObjCollection)
            {
                if (wmiObj.ResourceType == 16)
                {
                    return wmiObj;
                }
            }

            var errMsg = string.Format(
                                "Cannot find the Dvd drive in VirtualSystemSettingData {0}",
                                vmSettings.Path.Path);
            var ex = new WmiException(errMsg);
            logger.Error(errMsg, ex);
            throw ex;
        }

        public static ResourceAllocationSettingData GetIDEControllerSettings(VirtualSystemSettingData vmSettings, string cntrllerAddr)
        {
            var wmiObjCollection = GetResourceAllocationSettings(vmSettings);

            foreach (ResourceAllocationSettingData wmiObj in wmiObjCollection)
            {
                if (wmiObj.ResourceSubType == "Microsoft Emulated IDE Controller" && wmiObj.Address == cntrllerAddr)
                {
                    return wmiObj;
                }
            }

            var errMsg = string.Format(
                                "Cannot find the Microsoft Emulated IDE Controlle at address {0} in VirtualSystemSettingData {1}", 
                                cntrllerAddr, 
                                vmSettings.Path.Path);
            var ex = new WmiException(errMsg);
            logger.Error(errMsg, ex);
            throw ex;
        }

        /// <summary>
        /// VM resources, typically hardware a described by a generic MSVM_ResourceAllocationSettingData object.  The hardware type being 
        /// described is identified in two ways:  in general terms using an enum in the ResourceType field, and in terms of the implementation 
        /// using text in the ResourceSubType field.
        /// See http://msdn.microsoft.com/en-us/library/cc136877%28v=vs.85%29.aspx
        /// </summary>
        /// <param name="vmSettings"></param>
        /// <returns></returns>
        public static ResourceAllocationSettingData.ResourceAllocationSettingDataCollection GetResourceAllocationSettings(VirtualSystemSettingData vmSettings)
        {
            // An ASSOCIATOR object provides the cross reference from the VirtualSystemSettingData and the 
            // ResourceAllocationSettingData, but generated wrappers do not expose a ASSOCIATOR OF query as a method.
            // Instead, we use the System.Management to code the equivalant of 
            //  string query = string.Format( "ASSOCIATORS OF {{{0}}} WHERE ResultClass = {1}", vmSettings.path, resultclassName);
            //
            var wmiObjQuery = new RelatedObjectQuery(vmSettings.Path.Path, ResourceAllocationSettingData.CreatedClassName);

            // NB: default scope of ManagementObjectSearcher is '\\.\root\cimv2', which does not contain
            // the virtualisation objects.
            var wmiObjectSearch = new ManagementObjectSearcher(vmSettings.Scope, wmiObjQuery);
            var wmiObjCollection = new ResourceAllocationSettingData.ResourceAllocationSettingDataCollection(wmiObjectSearch.Get());

            if (wmiObjCollection != null)
            {
                return wmiObjCollection;
            }

            var errMsg = string.Format("No ResourceAllocationSettingData in VirtualSystemSettingData {0}", vmSettings.Path.Path);
            var ex = new WmiException(errMsg);
            logger.Error(errMsg, ex);
            throw ex;
        }

        public static SwitchPort[] GetSwitchPorts(ComputerSystem vm)
        {
            var virtSwitchMgmtSvc = GetVirtualSwitchManagementService();
            // Get NIC path
            var condition = string.Format("ElementName=\"{0}\"", vm.ElementName);
            var switchPortCollection = SwitchPort.GetInstances(virtSwitchMgmtSvc.Scope, condition);

            List<SwitchPort> result = new List<SwitchPort>(switchPortCollection.Count);
            foreach (SwitchPort item in switchPortCollection)
            {
                result.Add(item);
            }
            return result.ToArray();
        }


        /// <summary>
        /// Deprecated
        /// </summary>
        /// <param name="nic"></param>
        /// <returns></returns>
        public static SwitchPort GetSwitchPort(SyntheticEthernetPort nic)
        {
            // An ASSOCIATOR object provides the cross reference between WMI objects, 
            // but generated wrappers do not expose a ASSOCIATOR OF query as a method.
            // Instead, we use the System.Management to code the equivalant of 
            //  string query = string.Format( "ASSOCIATORS OF {{{0}}} WHERE ResultClass = {1}", wmiObject.path, resultclassName);
            //
            var wmiObjQuery = new RelatedObjectQuery(nic.Path.Path, VmLANEndpoint.CreatedClassName);

            // NB: default scope of ManagementObjectSearcher is '\\.\root\cimv2', which does not contain
            // the virtualisation objects.
            var wmiObjectSearch = new ManagementObjectSearcher(nic.Scope, wmiObjQuery);
            var wmiObjCollection = new VmLANEndpoint.VmLANEndpointCollection(wmiObjectSearch.Get());

            // assert
            if (wmiObjCollection.Count < 1)
            {
                var errMsg = string.Format("No VmLANEndpoint for external NIC {0} on Hyper-V server", nic.Name);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            VmLANEndpoint vmEndPoint = wmiObjCollection.OfType<VmLANEndpoint>().First();
            var switchPortQuery = new RelatedObjectQuery(vmEndPoint.Path.Path, SwitchPort.CreatedClassName);
            var switchPortSearch = new ManagementObjectSearcher(nic.Scope, switchPortQuery);
            var switchPortCollection = new SwitchPort.SwitchPortCollection(switchPortSearch.Get());

            // assert
            if (switchPortCollection.Count < 1)
            {
                var errMsg = string.Format("No SwitchPort for external NIC {0} on Hyper-V server", nic.Name);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            SwitchPort switchPort = wmiObjCollection.OfType<SwitchPort>().First();

            return switchPort;
        }

        public static SyntheticEthernetPortSettingData[] GetEthernetPorts(ComputerSystem vm)
        {
            // An ASSOCIATOR object provides the cross reference from the ComputerSettings and the 
            // SyntheticEthernetPortSettingData, via the VirtualSystemSettingData.
            // However, generated wrappers do not expose a ASSOCIATOR OF query as a method.
            // Instead, we use the System.Management to code the equivalant of 
            //
            // string query = string.Format( "ASSOCIATORS OF {{{0}}} WHERE ResultClass = {1}", vm.path, resultclassName);
            //
            VirtualSystemSettingData vmSettings = GetVmSettings(vm);

            var wmiObjQuery = new RelatedObjectQuery(vmSettings.Path.Path, SyntheticEthernetPortSettingData.CreatedClassName);

            // NB: default scope of ManagementObjectSearcher is '\\.\root\cimv2', which does not contain
            // the virtualisation objects.
            var wmiObjectSearch = new ManagementObjectSearcher(vm.Scope, wmiObjQuery);
            var wmiObjCollection = new SyntheticEthernetPortSettingData.SyntheticEthernetPortSettingDataCollection(wmiObjectSearch.Get());

            List<SyntheticEthernetPortSettingData> results = new List<SyntheticEthernetPortSettingData>(wmiObjCollection.Count);
            foreach (SyntheticEthernetPortSettingData item in wmiObjCollection)
            {
                results.Add(item);
            }

            return results.ToArray();
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
                if (wmiObj.SettingType == 3)
                {
                    return wmiObj;
                }
            }

            var errMsg = string.Format("No VirtualSystemSettingData for VM {0}, path {1}", vm.ElementName, vm.Path.Path);
            var ex = new WmiException(errMsg);
            logger.Error(errMsg, ex);
            throw ex;
        }
    }
}
