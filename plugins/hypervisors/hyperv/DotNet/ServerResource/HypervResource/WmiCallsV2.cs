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
using System.Net.NetworkInformation;
using System.Net;

namespace HypervResource
{
    public class WmiCallsV2 : IWmiCallsV2
    {
        public static String CloudStackUserDataKey = "cloudstack-vm-userdata";

        /// <summary>
        /// Defines the migration types.
        /// </summary>
        public enum MigrationType
        {
            VirtualSystem = 32768,
            Storage = 32769,
            Staged = 32770,
            VirtualSystemAndStorage = 32771
        };

        /// <summary>
        /// Defines migration transport types.
        /// </summary>
        public enum TransportType
        {
            TCP = 5,
            SMB = 32768
        };

        public static void Initialize()
        {
            // Trigger assembly load into curren appdomain
        }

        private static ILog logger = LogManager.GetLogger(typeof(WmiCallsV2));

        /// <summary>
        /// Returns ping status of the given ip
        /// </summary>
        public static String PingHost(String ip)
        {
            return "Success";
        }

        /// <summary>
        /// Returns ComputerSystem lacking any NICs and VOLUMEs
        /// </summary>
        public ComputerSystem AddUserData(ComputerSystem vm, string userData)
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
        public ComputerSystem CreateVM(string name, long memory_mb, int vcpus)
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
        public SyntheticEthernetPortSettingData CreateNICforVm(ComputerSystem vm, string mac)
        {
            logger.DebugFormat("Creating nic for VM {0} (GUID {1})", vm.ElementName, vm.Name);

            // Obtain controller for Hyper-V networking subsystem
            var vmNetMgmtSvc = GetVirtualSwitchManagementService();

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

            //  Assign configuration to new NIC
            string normalisedMAC = string.Join("", (mac.Split(new char[] { ':' })));
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

        public const string IDE_CONTROLLER = "Microsoft:Hyper-V:Emulated IDE Controller";
        public const string SCSI_CONTROLLER = "Microsoft:Hyper-V:Synthetic SCSI Controller";
        public const string HARDDISK_DRIVE = "Microsoft:Hyper-V:Synthetic Disk Drive";
        public const string ISO_DRIVE = "Microsoft:Hyper-V:Synthetic DVD Drive";

        // TODO: names harvested from Msvm_ResourcePool, not clear how to create new instances
        public const string ISO_DISK = "Microsoft:Hyper-V:Virtual CD/DVD Disk"; // For IDE_ISO_DRIVE
        public const string HARDDISK_DISK = "Microsoft:Hyper-V:Virtual Hard Disk"; // For IDE_HARDDISK_DRIVE

        /// <summary>
        /// Create new VM.  By default we start it. 
        /// </summary>
        public ComputerSystem DeployVirtualMachine(dynamic jsonObj, string systemVmIso)
        {
            var vmInfo = jsonObj.vm;
            string vmName = vmInfo.name;
            var nicInfo = vmInfo.nics;
            int vcpus = vmInfo.cpus;
            int memSize = vmInfo.maxRam / 1048576;
            string errMsg = vmName;
            var diskDrives = vmInfo.disks;
            var bootArgs = vmInfo.bootArgs;

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
                else if (vmWmiObj.EnabledState == EnabledState.Enabled)
                {
                    string infoMsg = string.Format("Create VM discovered there exists a VM with name {0}, state {1}",
                        vmName,
                        EnabledState.ToString(vmWmiObj.EnabledState));
                    logger.Info(infoMsg);
                    return vmWmiObj;
                }
                else
                {
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
            var newVm = CreateVM(vmName, memSize, vcpus);

            // Add a SCSI controller for attaching/detaching data volumes.
            AddScsiController(newVm);

            foreach (var diskDrive in diskDrives)
            {
                string vhdFile = null;
                string diskName = null;
                string isoPath = null;
                VolumeObjectTO volInfo = VolumeObjectTO.ParseJson(diskDrive.data);
                TemplateObjectTO templateInfo = TemplateObjectTO.ParseJson(diskDrive.data);

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
                    if (String.IsNullOrEmpty(volInfo.primaryDataStore.Path))
                    {
                        logger.Error(errMsg);
                        throw new ArgumentException(errMsg);
                    }
                    errMsg = vmName + ": Missing folder PrimaryDataStore for disk " + diskDrive.ToString() + ", missing path: " +  volInfo.primaryDataStore.Path;
                    if (!Directory.Exists(volInfo.primaryDataStore.Path))
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
                else if (templateInfo != null && templateInfo.nfsDataStoreTO != null)
                {
                    NFSTO share = templateInfo.nfsDataStoreTO;
                    // The share is mapped, now attach the iso
                    isoPath = Utils.NormalizePath(Path.Combine(share.UncPath, templateInfo.path));
                }

                string driveType = diskDrive.type;
                string ideCtrllr = "0";
                string driveResourceType = null;
                switch (driveType) {
                    case "ROOT":
                        ideCtrllr = "0";
                        driveResourceType = HARDDISK_DRIVE;
                        break;
                    case "ISO":
                        ideCtrllr = "1";
                        driveResourceType = ISO_DRIVE;
                        break;
                    case "DATADISK":
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
                        string.IsNullOrEmpty(vhdFile) ? " no disk to insert" : ", inserting disk" + vhdFile);
                if (driveType.Equals("DATADISK"))
                {
                    AttachDisk(vmName, vhdFile, (string)diskDrive.diskSeq);
                }
                else
                {
                    AddDiskDriveToIdeController(newVm, vhdFile, ideCtrllr, driveResourceType);
                    if (isoPath != null)
                    {
                        AttachIso(vmName, isoPath);
                    }
                }
            }

            int nicCount = 0;
            int enableState = 2;

            // Add the Nics to the VM in the deviceId order.
            foreach (var nc in nicInfo)
            {
                foreach (var nic in nicInfo)
                {

                    int nicid = nic.deviceId;
                    Int32 networkRateMbps = nic.networkRateMbps;
                    string mac = nic.mac;
                    string vlan = null;
                    string isolationUri = nic.isolationUri;
                    string broadcastUri = nic.broadcastUri;
                    string nicIp = nic.ip;
                    string nicNetmask = nic.netmask;
                    if ( (broadcastUri != null ) || (isolationUri != null && isolationUri.StartsWith("vlan://")))
                    {
                        if (broadcastUri != null && broadcastUri.StartsWith("storage"))
                        {
                            vlan = broadcastUri.Substring("storage://".Length);
                        }
                        else
                        {
                            vlan = isolationUri.Substring("vlan://".Length);
                        }
                        int tmp;
                        if (vlan.Equals("untagged", StringComparison.CurrentCultureIgnoreCase) ) {
                            // recevied vlan is untagged, don't parse for the vlan in the isolation uri
                            vlan = null;
                        }
                        else if (!int.TryParse(vlan, out tmp))
                        {
                            // TODO: double check exception type
                            errMsg = string.Format("Invalid VLAN value {0} for on vm {1} for nic uuid {2}", isolationUri, vmName, nic.uuid);
                            var ex = new WmiException(errMsg);
                            logger.Error(errMsg, ex);
                            throw ex;
                        }
                    }


                    if (nicid == nicCount)
                    {
                        if (nicIp.Equals("0.0.0.0") && nicNetmask.Equals("255.255.255.255"))
                        {
                            // this is the extra nic added to VR.
                            vlan = null;
                            enableState = 3;
                        }

                        // Create network adapter
                        var newAdapter = CreateNICforVm(newVm, mac);
                        String switchName ="";
                        if (nic.name != null)
                        {
                            switchName =  nic.name;
                        }
                        EthernetPortAllocationSettingData portSettings = null;
                        // connection to vswitch
                        portSettings = AttachNicToPort(newVm, newAdapter, switchName, enableState);
                        //reset the flag for other nics
                        enableState = 2;
                        // set vlan
                        if (vlan != null)
                        {
                            SetPortVlan(vlan, portSettings);
                        }

                        if (networkRateMbps > 0)
                        {
                            SetBandWidthLimit((ulong)networkRateMbps, portSettings);
                        }

                        logger.DebugFormat("Created adapter {0} on port {1}, {2}", 
                            newAdapter.Path, portSettings.Path, (vlan == null ? "No VLAN" : "VLAN " + vlan));
                     //   logger.DebugFormat("Created adapter {0} on port {1}, {2}", 
                    //       newAdapter.Path, portSettings.Path, (vlan == null ? "No VLAN" : "VLAN " + vlan));
                    }
                }
                nicCount++;
            }


            // pass the boot args for the VM using KVP component.
            // We need to pass the boot args to system vm's to get them configured with cloudstack configuration.
            // Add new user data
            var vm = GetComputerSystem(vmName);
            if (bootArgs != null && !String.IsNullOrEmpty((string)bootArgs))
            {
               
                String bootargs = bootArgs;
                AddUserData(vm, bootargs);
            }

            // call patch systemvm iso only for systemvms
            if (vmName.StartsWith("r-") || vmName.StartsWith("s-") || vmName.StartsWith("v-"))
            {
                if (systemVmIso != null && systemVmIso.Length != 0)
                {
                    patchSystemVmIso(vmName, systemVmIso);
                }
            }

            logger.DebugFormat("Starting VM {0}", vmName);
            SetState(newVm, RequiredState.Enabled);
            // Mark the VM as created by cloudstack tag
            TagVm(newVm);

            // we need to reboot to get the hv kvp daemon get started vr gets configured.
            if (vmName.StartsWith("r-") || vmName.StartsWith("s-") || vmName.StartsWith("v-"))
            {
                System.Threading.Thread.Sleep(90000);
            }
            logger.InfoFormat("Started VM {0}", vmName);
            return newVm;
        }

        public static Boolean pingResource(String ip)
        {
            PingOptions pingOptions = null;
            PingReply pingReply = null;
            IPAddress ipAddress = null;
            Ping pingSender = new Ping();
            int numberOfPings = 6;
            int pingTimeout = 1000;
            int byteSize = 32;
            byte[] buffer = new byte[byteSize];
            ipAddress = IPAddress.Parse(ip);
            pingOptions = new PingOptions();
            for (int i = 0; i < numberOfPings; i++)
            {
                pingReply = pingSender.Send(ipAddress, pingTimeout, buffer, pingOptions);
                if (pingReply.Status == IPStatus.Success)
                {
                    System.Threading.Thread.Sleep(30000);
                    return true;
                }
                else
                {
                    // wait for the second boot and then return with suces
                    System.Threading.Thread.Sleep(30000);
                }
            }
            return false;
        }

        private EthernetPortAllocationSettingData AttachNicToPort(ComputerSystem newVm, SyntheticEthernetPortSettingData newAdapter, String vSwitchName, int enableState)
        {
            // Get the virtual switch
            VirtualEthernetSwitch vSwitch = GetExternalVirtSwitch(vSwitchName);
            //check the recevied vSwitch is the same as vSwitchName.
            if (!vSwitchName.Equals("")  && !vSwitch.ElementName.Equals(vSwitchName))
            {
               var errMsg = string.Format("Internal error, coudl not find Virtual Switch with the name : " +vSwitchName);
               var ex = new WmiException(errMsg);
               logger.Error(errMsg, ex);
               throw ex;
            }

            // Create port for adapter
            var defaultEthernetPortSettings = EthernetPortAllocationSettingData.GetInstances(vSwitch.Scope, "InstanceID LIKE \"%Default\"");

            // assert
            if (defaultEthernetPortSettings.Count != 1)
            {
                var errMsg = string.Format("Internal error, coudl not find default EthernetPortAllocationSettingData instance");
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            var defaultEthernetPortSettingsObj = defaultEthernetPortSettings.OfType<EthernetPortAllocationSettingData>().First();
            var newEthernetPortSettings = new EthernetPortAllocationSettingData((ManagementBaseObject)defaultEthernetPortSettingsObj.LateBoundObject.Clone());
            newEthernetPortSettings.LateBoundObject["Parent"] = newAdapter.Path.Path;
            newEthernetPortSettings.LateBoundObject["HostResource"] = new string[] { vSwitch.Path.Path };
            newEthernetPortSettings.LateBoundObject["EnabledState"] = enableState; //3 disabled 2 Enabled
            // Insert NIC into vm
            string[] newResources = new string[] { newEthernetPortSettings.LateBoundObject.GetText(System.Management.TextFormat.CimDtd20) };
            ManagementPath[] newResourcePaths = AddVirtualResource(newResources, newVm);

            // assert
            if (newResourcePaths.Length != 1)
            {
                var errMsg = string.Format(
                    "Failed to properly insert a single NIC on VM {0} (GUID {1}): number of resource created {2}",
                    newVm.ElementName,
                    newVm.Name,
                    newResourcePaths.Length);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            return new EthernetPortAllocationSettingData(newResourcePaths[0]);
        }

        /// this method is to add a dvd drive and attach the systemvm iso.
        /// 
        public void patchSystemVmIso(String vmName, String systemVmIso)
        {
            ComputerSystem vmObject = GetComputerSystem(vmName);
            AddDiskDriveToIdeController(vmObject, "", "1", ISO_DRIVE);
            AttachIso(vmName, systemVmIso);
        }

        public void AttachDisk(string vmName, string diskPath, string addressOnController)
        {
            logger.DebugFormat("Got request to attach disk {0} to vm {1}", diskPath, vmName);

            ComputerSystem vm = GetComputerSystem(vmName);
            if (vm == null)
            {
                logger.DebugFormat("VM {0} not found", vmName);
                return;
            }
            else
            {
                ManagementPath newDrivePath = GetDiskDriveOnScsiController(vm, addressOnController);
                if (newDrivePath == null)
                {
                    newDrivePath = AttachDiskDriveToScsiController(vm, addressOnController);
                }
                InsertDiskImage(vm, diskPath, HARDDISK_DISK, newDrivePath);
            }
        }

        /// </summary>
        /// <param name="vm"></param>
        /// <param name="cntrllerAddr"></param>
        /// <param name="driveResourceType">IDE_HARDDISK_DRIVE or IDE_ISO_DRIVE</param>
        public ManagementPath AddDiskDriveToIdeController(ComputerSystem vm, string vhdfile, string cntrllerAddr, string driveResourceType)
        {
            logger.DebugFormat("Creating DISK for VM {0} (GUID {1}) by attaching {2}", 
                        vm.ElementName,
                        vm.Name,
                        vhdfile);

            // Determine disk type for drive and assert drive type valid
            string diskResourceSubType = null;
            switch(driveResourceType) {
                case HARDDISK_DRIVE:
                    diskResourceSubType = HARDDISK_DISK;
                    break;
                case ISO_DRIVE: 
                    diskResourceSubType = ISO_DISK;
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

            ManagementPath newDrivePath = AttachNewDrive(vm, cntrllerAddr, driveResourceType);

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


        public void DetachDisk(string displayName, string diskFileName)
        {
            logger.DebugFormat("Got request to detach virtual disk {0} from vm {1}", diskFileName, displayName);

            ComputerSystem vm = GetComputerSystem(displayName);
            if (vm == null)
            {
                logger.DebugFormat("VM {0} not found", displayName);
                return;
            }
            else
            {
                RemoveStorageImage(vm, diskFileName);
            }
        }

        /// <summary>
        /// Removes a disk image from a drive, but does not remove the drive itself.
        /// </summary>
        /// <param name="vm"></param>
        /// <param name="diskFileName"></param>
        private void RemoveStorageImage(ComputerSystem vm, string diskFileName)
        {
            // Obtain StorageAllocationSettingData for disk
            StorageAllocationSettingData.StorageAllocationSettingDataCollection storageSettingsObjs = StorageAllocationSettingData.GetInstances();

            StorageAllocationSettingData imageToRemove = null;
            foreach (StorageAllocationSettingData item in storageSettingsObjs)
            {
                if (item.HostResource == null || item.HostResource.Length != 1)
                {
                    continue;
                }

                string hostResource = item.HostResource[0];
                if (Path.Equals(hostResource, diskFileName))
                {
                    imageToRemove = item;
                    break;
                }
            }

            // assert
            if (imageToRemove  == null)
            {
                var errMsg = string.Format(
                    "Failed to remove disk image {0} from VM {1} (GUID {2}): the disk image is not attached.",
                    diskFileName,
                    vm.ElementName,
                    vm.Name);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            RemoveStorageResource(imageToRemove.Path, vm);

            logger.InfoFormat("Removed disk image {0} from VM {1} (GUID {2}): the disk image is not attached.",
                    diskFileName,
                    vm.ElementName,
                    vm.Name);
        }

        private ManagementPath AttachNewDrive(ComputerSystem vm, string cntrllerAddr, string driveType)
        {
            // Disk drives are attached to a 'Parent' IDE controller.  We IDE Controller's settings for the 'Path', which our new Disk drive will use to reference it.
            VirtualSystemSettingData vmSettings = GetVmSettings(vm);
            var ctrller = GetIDEControllerSettings(vmSettings, cntrllerAddr);

            // A description of the drive is created by modifying a clone of the default ResourceAllocationSettingData for that drive type
            string defaultDriveQuery = String.Format("ResourceSubType LIKE \"{0}\" AND InstanceID LIKE \"%Default\"", driveType);
            var newDiskDriveSettings = CloneResourceAllocationSetting(defaultDriveQuery);

            // Set IDE controller and address on the controller for the new drive
            newDiskDriveSettings.LateBoundObject["Parent"] = ctrller.Path.ToString();
            newDiskDriveSettings.LateBoundObject["AddressOnParent"] = "0";
            newDiskDriveSettings.CommitObject();

            // Add this new disk drive to the VM
            logger.DebugFormat("Creating disk drive type {0}, parent IDE controller is {1} and address on controller is {2}",
                newDiskDriveSettings.ResourceSubType,
                newDiskDriveSettings.Parent,
                newDiskDriveSettings.AddressOnParent);
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

        private ManagementPath AddScsiController(ComputerSystem vm)
        {
            // A description of the controller is created by modifying a clone of the default ResourceAllocationSettingData for scsi controller
            string scsiQuery = String.Format("ResourceSubType LIKE \"{0}\" AND InstanceID LIKE \"%Default\"", SCSI_CONTROLLER);
            var scsiSettings = CloneResourceAllocationSetting(scsiQuery);

            scsiSettings.LateBoundObject["ElementName"] = "SCSI Controller";
            scsiSettings.CommitObject();

            // Insert SCSI controller into vm
            string[] newResources = new string[] { scsiSettings.LateBoundObject.GetText(System.Management.TextFormat.CimDtd20) };
            ManagementPath[] newResourcePaths = AddVirtualResource(newResources, vm);

            // assert
            if (newResourcePaths.Length != 1)
            {
                var errMsg = string.Format(
                    "Failed to add scsi controller to VM {0} (GUID {1}): number of resource created {2}",
                    vm.ElementName,
                    vm.Name,
                    newResourcePaths.Length);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            logger.DebugFormat("New controller type {0} WMI path is {1}s",
                scsiSettings.ResourceSubType,
                newResourcePaths[0].Path);
            return newResourcePaths[0];
        }

        private ManagementPath GetDiskDriveOnScsiController(ComputerSystem vm, string addrOnController)
        {
            VirtualSystemSettingData vmSettings = GetVmSettings(vm);
            var wmiObjCollection = GetResourceAllocationSettings(vmSettings);
            foreach (ResourceAllocationSettingData wmiObj in wmiObjCollection)
            {
                if (wmiObj.ResourceSubType == HARDDISK_DRIVE)
                {
                    ResourceAllocationSettingData parent = new ResourceAllocationSettingData(new ManagementObject(wmiObj.Parent));
                    if (parent.ResourceSubType == SCSI_CONTROLLER && wmiObj.AddressOnParent == addrOnController)
                    {
                        return wmiObj.Path;
                    }
                }
            }
            return null;
        }

        private ManagementPath AttachDiskDriveToScsiController(ComputerSystem vm, string addrOnController)
        {
            // Disk drives are attached to a 'Parent' Scsi controller.
            VirtualSystemSettingData vmSettings = GetVmSettings(vm);
            var ctrller = GetScsiControllerSettings(vmSettings);

            // A description of the drive is created by modifying a clone of the default ResourceAllocationSettingData for that drive type
            string defaultDriveQuery = String.Format("ResourceSubType LIKE \"{0}\" AND InstanceID LIKE \"%Default\"", HARDDISK_DRIVE);
            var newDiskDriveSettings = CloneResourceAllocationSetting(defaultDriveQuery);

            // Set IDE controller and address on the controller for the new drive
            newDiskDriveSettings.LateBoundObject["Parent"] = ctrller.Path.ToString();
            newDiskDriveSettings.LateBoundObject["AddressOnParent"] = addrOnController;
            newDiskDriveSettings.CommitObject();

            // Add this new disk drive to the VM
            logger.DebugFormat("Creating disk drive type {0}, parent IDE controller is {1} and address on controller is {2}",
                newDiskDriveSettings.ResourceSubType,
                newDiskDriveSettings.Parent,
                newDiskDriveSettings.AddressOnParent);
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
                    HARDDISK_DRIVE);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
            logger.DebugFormat("New disk drive type {0} WMI path is {1}s",
                newDiskDriveSettings.ResourceSubType,
                newDrivePaths[0].Path);
            return newDrivePaths[0];
        }


        private void InsertDiskImage(ComputerSystem vm, string diskImagePath, string diskResourceSubType, ManagementPath drivePath)
        {
            // A description of the disk is created by modifying a clone of the default ResourceAllocationSettingData for that disk type
            string defaultDiskQuery = String.Format("ResourceSubType LIKE \"{0}\" AND InstanceID LIKE \"%Default\"", diskResourceSubType);
            var newDiskSettings = CloneStorageAllocationSetting(defaultDiskQuery);

            // Set file containing the disk image
            newDiskSettings.LateBoundObject["Parent"] = drivePath.Path;

            // V2 API uses HostResource to specify image, see http://msdn.microsoft.com/en-us/library/hh859775(v=vs.85).aspx
            newDiskSettings.LateBoundObject["HostResource"] = new string[] { diskImagePath };
            newDiskSettings.CommitObject();

            // Add the new Msvm_StorageAllocationSettingData object as a virtual hard disk to the vm.
            string[] newDiskResource = new string[] { newDiskSettings.LateBoundObject.GetText(System.Management.TextFormat.CimDtd20) };
            ManagementPath[] newDiskPaths = AddStorageResource(newDiskResource, vm);
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
                    diskImagePath);
        }

        /// <summary>
        /// Create Msvm_StorageAllocationSettingData corresponding to the ISO image, and 
        /// associate this with the VM's DVD drive.
        /// </summary>
        private void AttachIso(ComputerSystem vm, string isoPath)
        {
            // Disk drives are attached to a 'Parent' IDE controller.  We IDE Controller's settings for the 'Path', which our new Disk drive will use to reference it.
            VirtualSystemSettingData vmSettings = GetVmSettings(vm);
            var driveWmiObj = GetDvdDriveSettings(vmSettings);
            InsertDiskImage(vm, isoPath, ISO_DISK, driveWmiObj.Path);
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

        // Modify the systemvm nic's VLAN id
        public void ModifyVmVLan(string vmName, String vlanid, String mac)
        {
            int enableState = 2;
            bool enable = true;
            ComputerSystem vm = GetComputerSystem(vmName);
            SyntheticEthernetPortSettingData[] nicSettingsViaVm = GetEthernetPortSettings(vm);
            // Obtain controller for Hyper-V virtualisation subsystem
            VirtualSystemManagementService vmMgmtSvc = GetVirtualisationSystemManagementService();
            EthernetPortAllocationSettingData networkAdapter = null;
            string normalisedMAC = string.Join("", (mac.Split(new char[] { ':' })));
            int index = 0;
            foreach (SyntheticEthernetPortSettingData item in nicSettingsViaVm)
            {
                if (normalisedMAC.ToLower().Equals(item.Address.ToLower()))
                {
                    break;
                }
                index++;
            }
            String vSwitchName = "";
            VirtualEthernetSwitch vSwitch = GetExternalVirtSwitch(vSwitchName);
            EthernetPortAllocationSettingData[] ethernetConnections = GetEthernetConnections(vm);
            networkAdapter = ethernetConnections[index];
            networkAdapter.LateBoundObject["EnabledState"] = enableState; //3 disabled 2 Enabled
            networkAdapter.LateBoundObject["HostResource"] = new string[] { vSwitch.Path.Path };

            ModifyVmResources(vmMgmtSvc, vm, new String[] {
                networkAdapter.LateBoundObject.GetText(TextFormat.CimDtd20)
                });

            EthernetSwitchPortVlanSettingData vlanSettings = GetVlanSettings(ethernetConnections[index]);

            if (vlanid.Equals("untagged", StringComparison.CurrentCultureIgnoreCase))
            {
                // recevied vlan is untagged, don't parse for the vlan in the isolation uri
                vlanid = null;
            }

            if (vlanSettings == null)
            {
                // when modifying  nic to not connected don't create vlan
                if (enable)
                {
                    if (vlanid != null)
                    {
                        SetPortVlan(vlanid, networkAdapter);
                    }
                }
            }
            else
            {
                if (enable)
                {
                    if (vlanid != null)
                    {
                        //Assign vlan configuration to nic
                        vlanSettings.LateBoundObject["AccessVlanId"] = vlanid;
                        vlanSettings.LateBoundObject["OperationMode"] = 1;
                        ModifyFeatureVmResources(vmMgmtSvc, vm, new String[] {
                            vlanSettings.LateBoundObject.GetText(TextFormat.CimDtd20)});
                    }
                }
                else
                {
                    var virtSysMgmtSvc = GetVirtualisationSystemManagementService();

                    // This method will remove the vlan settings present on the Nic
                    ManagementPath jobPath;
                    var ret_val = virtSysMgmtSvc.RemoveFeatureSettings(new ManagementPath[] { vlanSettings.Path },
                        out jobPath);

                    // If the Job is done asynchronously
                    if (ret_val == ReturnCode.Started)
                    {
                        JobCompleted(jobPath);
                    }
                    else if (ret_val != ReturnCode.Completed)
                    {
                        var errMsg = string.Format(
                            "Failed to remove vlan resource {0} from VM {1} (GUID {2}) due to {3}",
                            vlanSettings.Path,
                            vm.ElementName,
                            vm.Name,
                            ReturnCode.ToString(ret_val));
                        var ex = new WmiException(errMsg);
                        logger.Error(errMsg, ex);
                    }
                }
            }
        }

        // This is disabling the VLAN settings on the specified nic. It works Awesome.
        public void DisableNicVlan(String mac, String vmName)
        {
            ComputerSystem vm = GetComputerSystem(vmName);
            SyntheticEthernetPortSettingData[] nicSettingsViaVm = GetEthernetPortSettings(vm);
            // Obtain controller for Hyper-V virtualisation subsystem
            VirtualSystemManagementService vmMgmtSvc = GetVirtualisationSystemManagementService();
            string normalisedMAC = string.Join("", (mac.Split(new char[] { ':' })));
            int index = 0;
            foreach (SyntheticEthernetPortSettingData item in nicSettingsViaVm)
            {
                if (normalisedMAC.ToLower().Equals(item.Address.ToLower()))
                {
                    break;
                }
                index++;
            }

            //TODO: make sure the index won't be out of range.

            EthernetPortAllocationSettingData[] ethernetConnections = GetEthernetConnections(vm);
            EthernetSwitchPortVlanSettingData vlanSettings = GetVlanSettings(ethernetConnections[index]);

            var virtSysMgmtSvc = GetVirtualisationSystemManagementService();

            // This method will remove the vlan settings present on the Nic
            ManagementPath jobPath;
            var ret_val = virtSysMgmtSvc.RemoveFeatureSettings(new ManagementPath[]{ vlanSettings.Path},
                out jobPath);

            // If the Job is done asynchronously
            if (ret_val == ReturnCode.Started)
            {
                JobCompleted(jobPath);
            }
            else if (ret_val != ReturnCode.Completed)
            {
                var errMsg = string.Format(
                    "Failed to remove vlan resource {0} from VM {1} (GUID {2}) due to {3}",
                    vlanSettings.Path,
                    vm.ElementName,
                    vm.Name,
                    ReturnCode.ToString(ret_val));
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
        }

        // Modify All VM Nics to disable
        public void DisableVmNics()
        {
            ComputerSystem vm = GetComputerSystem("test");
            EthernetPortAllocationSettingData[] ethernetConnections = GetEthernetConnections(vm);
            // Get the virtual switch
            VirtualEthernetSwitch vSwitch = GetExternalVirtSwitch("vswitch2");

            foreach (EthernetPortAllocationSettingData epasd in ethernetConnections)
            {
                epasd.LateBoundObject["EnabledState"] = 2; //3 disabled 2 Enabled
                epasd.LateBoundObject["HostResource"] = new string[] { vSwitch.Path.Path };

                VirtualSystemManagementService vmMgmtSvc = GetVirtualisationSystemManagementService();
                ModifyVmResources(vmMgmtSvc, vm, new String[] {
                epasd.LateBoundObject.GetText(TextFormat.CimDtd20)
                });
            }
        }

        // Modify the systemvm nic's VLAN id
        public void ModifyVmVLan(string vmName, String vlanid, uint pos, bool enable, string switchLabelName)
        {
            // This if to modify the VPC VR nics
            // 1. Enable the network adapter and connect to a switch
            // 2. modify the vlan id
            int enableState = 2;
            ComputerSystem vm = GetComputerSystem(vmName);
                EthernetPortAllocationSettingData[] ethernetConnections = GetEthernetConnections(vm);
            // Obtain controller for Hyper-V virtualisation subsystem
            EthernetPortAllocationSettingData networkAdapter = null;
            VirtualSystemManagementService vmMgmtSvc = GetVirtualisationSystemManagementService();

            String vSwitchName = "";
            if (switchLabelName != null)
                vSwitchName = switchLabelName;
            VirtualEthernetSwitch vSwitch = GetExternalVirtSwitch(vSwitchName);
            if (pos <= ethernetConnections.Length)
            {
                if (enable == false)
                {
                    enableState = 3;
                }

                networkAdapter = ethernetConnections[pos];
                networkAdapter.LateBoundObject["EnabledState"] = enableState; //3 disabled 2 Enabled
                networkAdapter.LateBoundObject["HostResource"] = new string[] { vSwitch.Path.Path };
                ModifyVmResources(vmMgmtSvc, vm, new String[] {
                networkAdapter.LateBoundObject.GetText(TextFormat.CimDtd20)
                });
            }

            // check when nic is disabled, removing vlan is required or not.
            EthernetPortAllocationSettingData[] vmEthernetConnections = GetEthernetConnections(vm);
            EthernetSwitchPortVlanSettingData vlanSettings = GetVlanSettings(vmEthernetConnections[pos]);

            if (vlanid.Equals("untagged", StringComparison.CurrentCultureIgnoreCase))
            {
                // recevied vlan is untagged, don't parse for the vlan in the isolation uri
                vlanid = null;
            }

            if (vlanSettings == null)
            {
                // when modifying  nic to not connected don't create vlan
                if (enable)
                {
                    if (vlanid != null)
                    {
                        SetPortVlan(vlanid, networkAdapter);
                    }
                }
            }
            else
            {
                if (enable)
                {
                    if (vlanid != null)
                    {
                        //Assign vlan configuration to nic
                        vlanSettings.LateBoundObject["AccessVlanId"] = vlanid;
                        vlanSettings.LateBoundObject["OperationMode"] = 1;
                        ModifyFeatureVmResources(vmMgmtSvc, vm, new String[] {
                            vlanSettings.LateBoundObject.GetText(TextFormat.CimDtd20)});
                    }
                }
                else
                {
                    var virtSysMgmtSvc = GetVirtualisationSystemManagementService();

                    // This method will remove the vlan settings present on the Nic
                    ManagementPath jobPath;
                    var ret_val = virtSysMgmtSvc.RemoveFeatureSettings(new ManagementPath[] { vlanSettings.Path },
                        out jobPath);

                    // If the Job is done asynchronously
                    if (ret_val == ReturnCode.Started)
                    {
                        JobCompleted(jobPath);
                    }
                    else if (ret_val != ReturnCode.Completed)
                    {
                        var errMsg = string.Format(
                            "Failed to remove vlan resource {0} from VM {1} (GUID {2}) due to {3}",
                            vlanSettings.Path,
                            vm.ElementName,
                            vm.Name,
                            ReturnCode.ToString(ret_val));
                        var ex = new WmiException(errMsg);
                        logger.Error(errMsg, ex);
                    }
                }
            }
        }

        public void AttachIso(string displayName, string iso)
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
                AttachIso(vm, iso);
            }
        }

        public void DestroyVm(dynamic jsonObj)
        {
            string vmToDestroy = jsonObj.vmName;
            DestroyVm(vmToDestroy);
        }
        
        /// <summary>
        /// Remove all VMs and all SwitchPorts with the displayName.  VHD gets deleted elsewhere.
        /// </summary>
        /// <param name="displayName"></param>
        public void DestroyVm(string displayName)
        {
            logger.DebugFormat("Got request to destroy vm {0}", displayName);

            var vm = GetComputerSystem(displayName);
            if ( vm  == null )
            {
                logger.DebugFormat("VM {0} already destroyed (or never existed)", displayName);
                return;
            }

            //try to shutdown vm first
            ShutdownVm(vm);

            if(GetComputerSystem(vm.ElementName).EnabledState != EnabledState.Disabled)
            {
                logger.Info("Could not shutdown system cleanly, will forcefully delete the system");
            }

            // Remove VM
            logger.DebugFormat("Remove VM {0} (GUID {1})", vm.ElementName, vm.Name);
            SetState(vm, RequiredState.Disabled);

            // Delete SwitchPort
            logger.DebugFormat("Remove associated switch ports for VM {0} (GUID {1})", vm.ElementName, vm.Name);
            DeleteSwitchPort(vm.ElementName);

            // Delete VM
            var virtSysMgmtSvc = GetVirtualisationSystemManagementService();
            ManagementPath jobPath;

            do
            {
                logger.DebugFormat("Delete VM {0} (GUID {1})", vm.ElementName, vm.Name);
                var ret_val = virtSysMgmtSvc.DestroySystem(vm.Path, out jobPath);

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

        public void ShutdownVm(ComputerSystem vm)
        {
            ShutdownComponent sc = GetShutdownComponent(vm);
            if (sc != null)
            {
                var ret_val = sc.InitiateShutdown(true, "need to shutdown");
                if (ret_val != ReturnCode.Completed)
                {
                    logger.Info("Shutting down of system failed, may be shutdown integration services are missing");
                }
                else
                {
                    // shutdown job is not returned so checking for shutdown completion by checking the current state of system.
                    // poll every one second and timeout after 10 minutes
                    for (int period = 0 ; period < 600 && (GetComputerSystem(vm.ElementName).EnabledState != EnabledState.Disabled); period++)
                    {
                        System.Threading.Thread.Sleep(1000);
                    }
                }
            }
            else
            {
                logger.Info("Shutting down of system failed; may be shutdown integration services are missing");
            }
        }

        /// <summary>
        /// Migrates a vm to the given destination host
        /// </summary>
        /// <param name="desplayName"></param>
        /// <param name="destination host"></param>
        public void MigrateVm(string vmName, string destination)
        {
            ComputerSystem vm = GetComputerSystem(vmName);
            VirtualSystemMigrationSettingData migrationSettingData = VirtualSystemMigrationSettingData.CreateInstance();
            VirtualSystemMigrationService service = GetVirtualisationSystemMigrationService();

            IPAddress addr = IPAddress.Parse(destination);
            IPHostEntry entry = Dns.GetHostEntry(addr);
            string[] destinationHost = new string[] { destination };

            migrationSettingData.LateBoundObject["MigrationType"] = MigrationType.VirtualSystem;
            migrationSettingData.LateBoundObject["TransportType"] = TransportType.TCP;
            migrationSettingData.LateBoundObject["DestinationIPAddressList"] = destinationHost;
            string migrationSettings = migrationSettingData.LateBoundObject.GetText(System.Management.TextFormat.CimDtd20);

            ManagementPath jobPath;
            var ret_val = service.MigrateVirtualSystemToHost(vm.Path, entry.HostName, migrationSettings, null, null, out jobPath);
            if (ret_val == ReturnCode.Started)
            {
                MigrationJobCompleted(jobPath);
            }
            else if (ret_val != ReturnCode.Completed)
            {
                var errMsg = string.Format(
                    "Failed migrating VM {0} (GUID {1}) due to {2}",
                    vm.ElementName,
                    vm.Name,
                    ReturnCode.ToString(ret_val));
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
        }

        /// <summary>
        /// Migrates the volume of a vm to a given destination storage
        /// </summary>
        /// <param name="displayName"></param>
        /// <param name="volume"></param>
        /// <param name="destination storage pool"></param>
        public void MigrateVolume(string vmName, string volume, string destination)
        {
            ComputerSystem vm = GetComputerSystem(vmName);
            VirtualSystemMigrationSettingData migrationSettingData = VirtualSystemMigrationSettingData.CreateInstance();
            VirtualSystemMigrationService service = GetVirtualisationSystemMigrationService();
            StorageAllocationSettingData[] sasd = GetStorageSettings(vm);

            string[] rasds = null;
            if (sasd != null)
            {
                rasds = new string[1];
                foreach (StorageAllocationSettingData item in sasd)
                {
                    string vhdFileName = Path.GetFileNameWithoutExtension(item.HostResource[0]);
                    if (!String.IsNullOrEmpty(vhdFileName) && vhdFileName.Equals(volume))
                    {
                        string newVhdPath = Path.Combine(destination, Path.GetFileName(item.HostResource[0]));
                        item.LateBoundObject["HostResource"] = new string[] { newVhdPath };
                        item.LateBoundObject["PoolId"] = "";
                        rasds[0] = item.LateBoundObject.GetText(System.Management.TextFormat.CimDtd20);
                        break;
                    }
                }
            }

            migrationSettingData.LateBoundObject["MigrationType"] = MigrationType.Storage;
            migrationSettingData.LateBoundObject["TransportType"] = TransportType.TCP;
            string migrationSettings = migrationSettingData.LateBoundObject.GetText(System.Management.TextFormat.CimDtd20);

            ManagementPath jobPath;
            var ret_val = service.MigrateVirtualSystemToHost(vm.Path, null, migrationSettings, rasds, null, out jobPath);
            if (ret_val == ReturnCode.Started)
            {
                MigrationJobCompleted(jobPath);
            }
            else if (ret_val != ReturnCode.Completed)
            {
                var errMsg = string.Format(
                    "Failed migrating volume {0} of VM {1} (GUID {2}) due to {3}",
                    volume,
                    vm.ElementName,
                    vm.Name,
                    ReturnCode.ToString(ret_val));
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
        }

        /// <summary>
        /// Migrates the volume of a vm to a given destination storage
        /// </summary>
        /// <param name="displayName"></param>
        /// <param name="destination host"></param>
        /// <param name="volumeToPool"> volume to me migrated to which pool</param>
        public void MigrateVmWithVolume(string vmName, string destination, Dictionary<string, string> volumeToPool)
        {
            ComputerSystem vm = GetComputerSystem(vmName);
            VirtualSystemMigrationSettingData migrationSettingData = VirtualSystemMigrationSettingData.CreateInstance();
            VirtualSystemMigrationService service = GetVirtualisationSystemMigrationService();
            StorageAllocationSettingData[] sasd = GetStorageSettings(vm);

            string[] rasds = null;
            if (sasd != null)
            {
                rasds = new string[sasd.Length];
                uint index = 0;
                foreach (StorageAllocationSettingData item in sasd)
                {
                    string vhdFileName = Path.GetFileNameWithoutExtension(item.HostResource[0]);
                    if (!String.IsNullOrEmpty(vhdFileName) && volumeToPool.ContainsKey(vhdFileName))
                    {
                        string newVhdPath = Path.Combine(volumeToPool[vhdFileName], Path.GetFileName(item.HostResource[0]));
                        item.LateBoundObject["HostResource"] = new string[] { newVhdPath };
                        item.LateBoundObject["PoolId"] = "";
                    }

                    rasds[index++] = item.LateBoundObject.GetText(System.Management.TextFormat.CimDtd20);
                }
            }

            IPAddress addr = IPAddress.Parse(destination);
            IPHostEntry entry = Dns.GetHostEntry(addr);
            string[] destinationHost = new string[] { destination };

            migrationSettingData.LateBoundObject["MigrationType"] = MigrationType.VirtualSystemAndStorage;
            migrationSettingData.LateBoundObject["TransportType"] = TransportType.TCP;
            migrationSettingData.LateBoundObject["DestinationIPAddressList"] = destinationHost;
            string migrationSettings = migrationSettingData.LateBoundObject.GetText(System.Management.TextFormat.CimDtd20);

            ManagementPath jobPath;
            var ret_val = service.MigrateVirtualSystemToHost(vm.Path, entry.HostName, migrationSettings, rasds, null, out jobPath);
            if (ret_val == ReturnCode.Started)
            {
                MigrationJobCompleted(jobPath);
            }
            else if (ret_val != ReturnCode.Completed)
            {
                var errMsg = string.Format(
                    "Failed migrating VM {0} and its volumes to destination {1} (GUID {2}) due to {3}",
                    vm.ElementName,
                    destination,
                    vm.Name,
                    ReturnCode.ToString(ret_val));
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
        }

        /// <summary>
        /// Create new storage media resources, e.g. hard disk images and ISO disk images
        /// see http://msdn.microsoft.com/en-us/library/hh859775(v=vs.85).aspx
        /// </summary>
        /// <param name="wmiQuery"></param>
        /// <returns></returns>
        private static StorageAllocationSettingData CloneStorageAllocationSetting(string wmiQuery)
        {
            var defaultDiskImageSettingsObjs = StorageAllocationSettingData.GetInstances(wmiQuery);

            // assert
            if (defaultDiskImageSettingsObjs.Count != 1)
            {
                var errMsg = string.Format("Failed to find Msvm_StorageAllocationSettingData for the query {0}", wmiQuery);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            StorageAllocationSettingData defaultDiskDriveSettings = defaultDiskImageSettingsObjs.OfType<StorageAllocationSettingData>().First();
            return new StorageAllocationSettingData((ManagementBaseObject)defaultDiskDriveSettings.LateBoundObject.Clone());
        }

        /// < summary>
        /// Removes a storage resource from a computer system.
        /// </summary>
        /// <param name="storageSettings">Path that uniquely identifies the resource.</param>
        /// <param name="vm">VM to which the disk image will be attached.</param>
        // Add new 
        private void RemoveNetworkResource(ManagementPath resourcePath)
        {
            var virtSwitchMgmtSvc = GetVirtualSwitchManagementService();
            ManagementPath jobPath;
            var ret_val = virtSwitchMgmtSvc.RemoveResourceSettings(
                new ManagementPath[] { resourcePath },
                out jobPath);

            // If the Job is done asynchronously
            if (ret_val == ReturnCode.Started)
            {
                JobCompleted(jobPath);
            }
            else if (ret_val != ReturnCode.Completed)
            {
                var errMsg = string.Format(
                    "Failed to remove network resources {0} from switch due to {1}",
                    resourcePath.Path,
                    ReturnCode.ToString(ret_val));
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
        }

        /// < summary>
        /// Removes a storage resource from a computer system.
        /// </summary>
        /// <param name="storageSettings">Path that uniquely identifies the resource.</param>
        /// <param name="vm">VM to which the disk image will be attached.</param>
        private void RemoveStorageResource(ManagementPath resourcePath, ComputerSystem vm)
        {
            var virtSysMgmtSvc = GetVirtualisationSystemManagementService();

            ManagementPath jobPath;
            var ret_val = virtSysMgmtSvc.RemoveResourceSettings(
                new ManagementPath[] { resourcePath },
                out jobPath);

            // If the Job is done asynchronously
            if (ret_val == ReturnCode.Started)
            {
                JobCompleted(jobPath);
            }
            else if (ret_val != ReturnCode.Completed)
            {
                var errMsg = string.Format(
                    "Failed to remove resource {0} from VM {1} (GUID {2}) due to {3}",
                    resourcePath.Path,
                    vm.ElementName,
                    vm.Name,
                    ReturnCode.ToString(ret_val));
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
        }

        public void SetState(ComputerSystem vm, ushort requiredState)
        {
            logger.InfoFormat(
                "Changing state of {0} (GUID {1}) to {2}", 
                vm.ElementName, 
                vm.Name,  
                RequiredState.ToString(requiredState));

            ManagementPath jobPath;
            // DateTime is unused
            var ret_val = vm.RequestStateChange(requiredState, new DateTime(), out jobPath);

            // If the Job is done asynchronously
            if (ret_val == ReturnCode.Started)
            {
                JobCompleted(jobPath);
            }
            else if (ret_val == 32775)
            {   // TODO: check
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
        /// <summary>
        /// Delete switch port by removing settings from the switch
        /// </summary>
        /// <param name="elementName"></param>
        /// <returns></returns>
        public void DeleteSwitchPort(string elementName)
        {
            // Get NIC path
            var condition = string.Format("ElementName=\"{0}\"", elementName);
            var virtSwitchMgmtSvc = GetVirtualSwitchManagementService();

            var switchPortCollection = EthernetSwitchPort.GetInstances(virtSwitchMgmtSvc.Scope, condition);
            if (switchPortCollection.Count == 0)
            {
                return;
            }

            foreach (EthernetSwitchPort port in switchPortCollection)
            {
                var settings = GetSyntheticEthernetPortSettings(port);
                RemoveNetworkResource(settings.Path);
            }
        }

        public SyntheticEthernetPortSettingData GetSyntheticEthernetPortSettings(EthernetSwitchPort port)
        {
            // An ASSOCIATOR object provides the cross reference from the EthernetSwitchPort and the 
            // SyntheticEthernetPortSettingData, but generated wrappers do not expose a ASSOCIATOR OF query as a method.
            // Instead, we use the System.Management to code the equivalant of 
            //  string query = string.Format( "ASSOCIATORS OF {{{0}}} WHERE ResultClass = {1}", vm.path, resultclassName);
            //
            var wmiObjQuery = new RelatedObjectQuery(port.Path.Path, SyntheticEthernetPortSettingData.CreatedClassName);

            // NB: default scope of ManagementObjectSearcher is '\\.\root\cimv2', which does not contain
            // the virtualisation objects.
            var wmiObjectSearch = new ManagementObjectSearcher(port.Scope, wmiObjQuery);
            var wmiObjCollection = new SyntheticEthernetPortSettingData.SyntheticEthernetPortSettingDataCollection(wmiObjectSearch.Get());

            // When snapshots are taken into account, there can be multiple settings objects
            // take the first one that isn't a snapshot
            foreach (SyntheticEthernetPortSettingData wmiObj in wmiObjCollection)
            {
                return wmiObj;
            }

            var errMsg = string.Format("No SyntheticEthernetPortSettingData for port {0}, path {1}", port.ElementName, port.Path.Path);
            var ex = new WmiException(errMsg);
            logger.Error(errMsg, ex);
            throw ex;
        }

        /// <summary>
        /// Adds storage images to coputer system (disk image, iso image).
        /// </summary>
        /// <param name="storageSettings">Msvm_StorageAllocationSettings with HostResource configured with image
        /// file and Parent set to a controller associated with the ComputerSystem</param>
        /// <param name="vm">VM to which the disk image will be attached.</param>
        // Add new 
        private ManagementPath[] AddStorageResource(string[] storageSettings, ComputerSystem vm)
        {
            return AddVirtualResource(storageSettings, vm);
        }

        private ManagementPath[] AddVirtualResource(string[] resourceSettings, ComputerSystem vm )
        {
            var virtSysMgmtSvc = GetVirtualisationSystemManagementService();

            ManagementPath jobPath;
            ManagementPath[] resourcePaths;
            var ret_val = virtSysMgmtSvc.AddResourceSettings(
                vm.Path,
                resourceSettings,
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

        private ManagementPath[] AddFeatureSettings(string[] featureSettings, ManagementPath affectedConfiguration)
        {
            var virtSysMgmtSvc = GetVirtualisationSystemManagementService();

            ManagementPath jobPath;
            ManagementPath[] resultSettings;
            var ret_val = virtSysMgmtSvc.AddFeatureSettings(
                affectedConfiguration,
                featureSettings,
                out jobPath,
                out resultSettings);

            // If the Job is done asynchronously
            if (ret_val == ReturnCode.Started)
            {
                JobCompleted(jobPath);
            }
            else if (ret_val != ReturnCode.Completed)
            {
                var errMsg = string.Format(
                    "Failed to add features settings {0} to resource {1} due to {2}",
                    featureSettings,
                    affectedConfiguration,
                    ReturnCode.ToString(ret_val));
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            return resultSettings;
        }

        private ManagementPath SetPortVlan(string vlan, EthernetPortAllocationSettingData portPath)
        {
            logger.DebugFormat("Setting VLAN to {0}", vlan);

            var vmVirtMgmtSvc = GetVirtualisationSystemManagementService();
            EthernetSwitchPortVlanSettingData.GetInstances();

            // Create NIC resource by cloning the default NIC 
            var vlanSettings = EthernetSwitchPortVlanSettingData.GetInstances(vmVirtMgmtSvc.Scope, "InstanceID LIKE \"%Default\"");

            // Assert
            if (vlanSettings.Count != 1)
            {
                var errMsg = string.Format("Internal error, could not find default EthernetSwitchPortVlanSettingData instance");
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
            var defaultVlanSettings = vlanSettings.OfType<EthernetSwitchPortVlanSettingData>().First();

            var newVlanSettings = new EthernetSwitchPortVlanSettingData((ManagementBaseObject)defaultVlanSettings.LateBoundObject.Clone());

            //  Assign configuration to new NIC
            newVlanSettings.LateBoundObject["AccessVlanId"] = vlan;
            newVlanSettings.LateBoundObject["OperationMode"] = 1; // Access=1, trunk=2, private=3 ;
            newVlanSettings.CommitObject();

            // Insert NIC into vm
            string[] newResources = new string[] { newVlanSettings.LateBoundObject.GetText(System.Management.TextFormat.CimDtd20) };
            ManagementPath[] newResourcePaths = AddFeatureSettings(newResources, portPath.Path);

            // assert
            if (newResourcePaths.Length != 1)
            {
                var errMsg = string.Format(
                    "Failed to properly set VLAN to {0} for NIC on port {1}",
                    vlan,
                    portPath.Path);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            return newResourcePaths[0];
        }

        private void SetBandWidthLimit(ulong limit, EthernetPortAllocationSettingData portPath)
        {
            logger.DebugFormat("Setting network rate limit to {0}", limit);

            var vmVirtMgmtSvc = GetVirtualisationSystemManagementService();
            var bandwidthSettings = EthernetSwitchPortBandwidthSettingData.GetInstances(vmVirtMgmtSvc.Scope, "InstanceID LIKE \"%Default\"");

            // Assert
            if (bandwidthSettings.Count != 1)
            {
                var errMsg = string.Format("Internal error, could not find default EthernetSwitchPortBandwidthSettingData instance");
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
            var defaultBandwidthSettings = bandwidthSettings.OfType<EthernetSwitchPortBandwidthSettingData>().First();

            var newBandwidthSettings = new EthernetSwitchPortBandwidthSettingData((ManagementBaseObject)defaultBandwidthSettings.LateBoundObject.Clone());
            newBandwidthSettings.Limit = limit * 1000000;

            // Insert bandwidth settings to nic
            string[] newResources = new string[] { newBandwidthSettings.LateBoundObject.GetText(System.Management.TextFormat.CimDtd20) };
            ManagementPath[] newResourcePaths = AddFeatureSettings(newResources, portPath.Path);

            // assert
            if (newResourcePaths.Length != 1)
            {
                var errMsg = string.Format(
                    "Failed to properly apply network rate limit {0} for NIC on port {1}",
                    limit,
                    portPath.Path);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
        }


        /// <summary>
        /// External VSwitch has an external NIC, and we assume there is only one external NIC and one external vswitch.
        /// </summary>
        /// <param name="vmSettings"></param>
        /// <returns></returns>
        /// <throw>Throws if there is no vswitch</throw>
        /// <remarks>
        /// With V1 API, external ethernet port was attached to the land endpoint, which was attached to the switch.
        /// e.g. Msvm_ExternalEthernetPort -> SwitchLANEndpoint -> SwitchPort -> VirtualSwitch
        /// 
        /// With V2 API, there are two kinds of lan endpoint:  one on the computer system and one on the switch
        /// e.g. Msvm_ExternalEthernetPort -> LANEndpoint -> LANEdnpoint -> EthernetSwitchPort -> VirtualEthernetSwitch
        /// </remarks>
        public static VirtualEthernetSwitch GetExternalVirtSwitch(String vSwitchName)
        {
            // Work back from the first *bound* external NIC we find.
            var externNICs = ExternalEthernetPort.GetInstances("IsBound = TRUE");
            VirtualEthernetSwitch vSwitch = null;
            // Assert
            if (externNICs.Count == 0 )
            {
                var errMsg = "No ExternalEthernetPort available to Hyper-V";
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
            foreach(ExternalEthernetPort externNIC in externNICs.OfType<ExternalEthernetPort>()) { 
            // A sequence of ASSOCIATOR objects need to be traversed to get from external NIC the vswitch.
            // We use ManagementObjectSearcher objects to execute this sequence of questions
            // NB: default scope of ManagementObjectSearcher is '\\.\root\cimv2', which does not contain
            // the virtualisation objects.
            var endpointQuery = new RelatedObjectQuery(externNIC.Path.Path, LANEndpoint.CreatedClassName);
            var endpointSearch = new ManagementObjectSearcher(externNIC.Scope, endpointQuery);
            var endpointCollection = new LANEndpoint.LANEndpointCollection(endpointSearch.Get());

            // assert
            if (endpointCollection.Count < 1 )
            {
                var errMsg = string.Format("No adapter-based LANEndpoint for external NIC {0} on Hyper-V server", externNIC.Name);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            LANEndpoint adapterEndPoint = endpointCollection.OfType<LANEndpoint>().First();
            var switchEndpointQuery = new RelatedObjectQuery(adapterEndPoint.Path.Path, LANEndpoint.CreatedClassName);
            var switchEndpointSearch = new ManagementObjectSearcher(externNIC.Scope, switchEndpointQuery);
            var switchEndpointCollection = new LANEndpoint.LANEndpointCollection(switchEndpointSearch.Get());
        
            // assert
            if (endpointCollection.Count < 1)
            {
                var errMsg = string.Format("No Switch-based LANEndpoint for external NIC {0} on Hyper-V server", externNIC.Name);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
        
            LANEndpoint switchEndPoint = switchEndpointCollection.OfType<LANEndpoint>().First();
            var switchPortQuery = new RelatedObjectQuery(switchEndPoint.Path.Path, EthernetSwitchPort.CreatedClassName);
            var switchPortSearch = new ManagementObjectSearcher(switchEndPoint.Scope, switchPortQuery);
            var switchPortCollection = new EthernetSwitchPort.EthernetSwitchPortCollection(switchPortSearch.Get());
        
            // assert
            if (switchPortCollection.Count < 1 )
            {
                var errMsg = string.Format("No SwitchPort for external NIC {0} on Hyper-V server", externNIC.Name);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
        
            EthernetSwitchPort switchPort = switchPortCollection.OfType<EthernetSwitchPort>().First();
            var vSwitchQuery = new RelatedObjectQuery(switchPort.Path.Path, VirtualEthernetSwitch.CreatedClassName);
            var vSwitchSearch = new ManagementObjectSearcher(externNIC.Scope, vSwitchQuery);
            var vSwitchCollection = new VirtualEthernetSwitch.VirtualEthernetSwitchCollection(vSwitchSearch.Get());

            // assert
            if (vSwitchCollection.Count < 1)
            {
                var errMsg = string.Format("No virtual switch for external NIC {0} on Hyper-V server", externNIC.Name);
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
            vSwitch = vSwitchCollection.OfType<VirtualEthernetSwitch>().First();
            if (vSwitch.ElementName.Equals(vSwitchName) == true)
            {
                return vSwitch;
            }
            }
            return vSwitch;
        }


        private static void ModifyFeatureVmResources(VirtualSystemManagementService vmMgmtSvc, ComputerSystem vm, string[] resourceSettings)
        {
            // Resource settings are changed through the management service
            System.Management.ManagementPath jobPath;
            System.Management.ManagementPath[] results;

            var ret_val = vmMgmtSvc.ModifyFeatureSettings(
                resourceSettings,
                out jobPath,
                out results);

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

        private static void ModifyVmResources(VirtualSystemManagementService vmMgmtSvc, ComputerSystem vm, string[] resourceSettings)
        {
            // Resource settings are changed through the management service
            System.Management.ManagementPath jobPath;
            System.Management.ManagementPath[] results;

            var ret_val = vmMgmtSvc.ModifyResourceSettings(
                resourceSettings,
                out jobPath,
                out results);

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

        private static void ModifySystemSetting(VirtualSystemManagementService vmMgmtSvc, string systemSettings)
        {
            // Resource settings are changed through the management service
            System.Management.ManagementPath jobPath;

            var ret_val = vmMgmtSvc.ModifySystemSettings(
                systemSettings,
                out jobPath);

            // If the Job is done asynchronously
            if (ret_val == ReturnCode.Started)
            {
                JobCompleted(jobPath);
            }
            else if (ret_val != ReturnCode.Completed)
            {
                var errMsg = string.Format(
                    "Failed to update system setting {0}",
                    ReturnCode.ToString(ret_val));
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
        }

        public void DeleteHostKvpItem(ComputerSystem vm, string key)
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

        public Boolean TagVm(ComputerSystem vm)
        {
            VirtualSystemManagementService vmMgmtSvc = GetVirtualisationSystemManagementService();
            VirtualSystemSettingData vmSettings = GetVmSettings(vm);

            vmSettings.LateBoundObject["Notes"] = new string[] { "Created by CloudStack, do not edit. \n" };
            ModifySystemSetting(vmMgmtSvc, vmSettings.LateBoundObject.GetText(TextFormat.CimDtd20));
            return true;
        }

        private static ComputerSystem CreateDefaultVm(VirtualSystemManagementService vmMgmtSvc, string name)
        {
            // Tweak default settings by basing new VM on default global setting object 
            // with designed display name.
            UInt16 startupAction = 2; // Do nothing.
            UInt16 stopAction = 4; // Shutdown.
            VirtualSystemSettingData vs_gs_data = VirtualSystemSettingData.CreateInstance();
            vs_gs_data.LateBoundObject["ElementName"] = name;
            vs_gs_data.LateBoundObject["AutomaticStartupAction"] = startupAction.ToString();
            vs_gs_data.LateBoundObject["AutomaticShutdownAction"] = stopAction.ToString();
            vs_gs_data.LateBoundObject["Notes"] = new string[] { "CloudStack creating VM, do not edit. \n" };

            System.Management.ManagementPath jobPath;
            System.Management.ManagementPath defined_sys;
            var ret_val = vmMgmtSvc.DefineSystem(
                null,
                new string[0],
                vs_gs_data.LateBoundObject.GetText(System.Management.TextFormat.CimDtd20),
                out jobPath,
                out defined_sys);

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

        public VirtualEthernetSwitchManagementService GetVirtualSwitchManagementService()
        {
            // VirtualSwitchManagementService is a singleton, most anonymous way of lookup is by asking for the set
            // of local instances, which should be size 1.
            var virtSwtichSvcCollection = VirtualEthernetSwitchManagementService.GetInstances();
            foreach (VirtualEthernetSwitchManagementService item in virtSwtichSvcCollection)
            {
                return item;
            }

            var errMsg = string.Format("No Hyper-V subsystem on server");
            var ex = new WmiException(errMsg);
            logger.Error(errMsg, ex);
            throw ex;
        }

        /// <summary>
        /// Always produces a VHDX.
        /// </summary>
        /// <param name="MaxInternalSize"></param>
        /// <param name="Path"></param>
        public void CreateDynamicVirtualHardDisk(ulong MaxInternalSize, string Path)
        {
            // Produce description of the virtual disk in the format of a Msvm_VirtualHardDiskSettings object

            // Example at http://www.getcodesamples.com/src/FC025DDC/76689747, but
            // Is there a template we can use to fill in the settings?
            var newVirtHDSettings = VirtualHardDiskSettingData.CreateInstance();
            newVirtHDSettings.LateBoundObject["Type"] = 3; // Dynamic
            newVirtHDSettings.LateBoundObject["Format"] = 3; // VHDX
            newVirtHDSettings.LateBoundObject["Path"] = Path;
            newVirtHDSettings.LateBoundObject["MaxInternalSize"] = MaxInternalSize;
            newVirtHDSettings.LateBoundObject["BlockSize"] = 0; // Use defaults
            newVirtHDSettings.LateBoundObject["LogicalSectorSize"] = 0; // Use defaults
            newVirtHDSettings.LateBoundObject["PhysicalSectorSize"] = 0; // Use defaults

            // Optional: newVirtHDSettings.CommitObject();

            // Add the new vhd object as a virtual hard disk to the vm.
            string newVirtHDSettingsString = newVirtHDSettings.LateBoundObject.GetText(System.Management.TextFormat.CimDtd20);

            // Resource settings are changed through the management service
            System.Management.ManagementPath jobPath;
            var imgMgr = GetImageManagementService();
            var ret_val = imgMgr.CreateVirtualHardDisk(newVirtHDSettingsString, out jobPath);

            // If the Job is done asynchronously
            if (ret_val == ReturnCode.Started)
            {
                StorageJobCompleted(jobPath);
            }
            else if (ret_val != ReturnCode.Completed)
            {
                var errMsg = string.Format(
                    "Failed to CreateVirtualHardDisk size {0}, path {1} due to {2}",
                    MaxInternalSize,
                    Path,
                    ReturnCode.ToString(ret_val));
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }
        }

        public ImageManagementService GetImageManagementService()
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


        public VirtualSystemManagementService GetVirtualisationSystemManagementService()
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

        public VirtualSystemMigrationService GetVirtualisationSystemMigrationService()
        {

            var virtSysMigSvcCollection = VirtualSystemMigrationService.GetInstances();
            foreach (VirtualSystemMigrationService item in virtSysMigSvcCollection)
            {
                return item;
            }

            var errMsg = string.Format("No Hyper-V migration service subsystem on server");
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

        private static void MigrationJobCompleted(ManagementPath jobPath)
        {
            MigrationJob jobObj = null;
            for (;;)
            {
                jobObj = new MigrationJob(jobPath);
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
        }

        private static void StorageJobCompleted(ManagementPath jobPath)
        {
            StorageJob jobObj = null;
            for (; ; )
            {
                jobObj = new StorageJob(jobPath);
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
        }

        public void GetProcessorResources(out uint sockets, out uint cores, out uint mhz)
        {
            //  Processor processors
            cores = 0;
            mhz = 0;
            sockets = 0;
            Processor.ProcessorCollection procCol = Processor.GetInstances();
            foreach (Processor procInfo in procCol)
            {
                cores += procInfo.NumberOfCores;
                mhz = procInfo.MaxClockSpeed;
                sockets++;
           }
        }
        
        public void GetProcessorUsageInfo(out double cpuUtilization)
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


        public void GetMemoryResources(out ulong physicalRamKBs, out ulong freeMemoryKBs)
        {
            OperatingSystem0 os = new OperatingSystem0();
            physicalRamKBs = os.TotalVisibleMemorySize;
            freeMemoryKBs = os.FreePhysicalMemory;
        }

        public string GetDefaultVirtualDiskFolder()
        {
            VirtualSystemManagementServiceSettingData.VirtualSystemManagementServiceSettingDataCollection coll = VirtualSystemManagementServiceSettingData.GetInstances();
            string defaultVirtualHardDiskPath = null;
            foreach (VirtualSystemManagementServiceSettingData settings in coll)
            {
                defaultVirtualHardDiskPath = settings.DefaultVirtualHardDiskPath;
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

        public ComputerSystem GetComputerSystem(string displayName)
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

        public ComputerSystem.ComputerSystemCollection GetComputerSystemCollection()
        {
            var wmiQuery = String.Format("ProcessId >= 0");
            return ComputerSystem.GetInstances(wmiQuery);
        }

        public ShutdownComponent GetShutdownComponent(ComputerSystem vm)
        {
            var wmiQuery = String.Format("SystemName=\"{0}\"", vm.Name);
            ShutdownComponent.ShutdownComponentCollection vmCollection = ShutdownComponent.GetInstances(wmiQuery);

            // Return the first one
            foreach (ShutdownComponent sc in vmCollection)
            {
                return sc;
            }
            return null;
        }

        public Dictionary<String, VmState> GetVmSync(String privateIpAddress)
        {
            List<String> vms = GetVmElementNames();
            Dictionary<String, VmState> vmSyncStates = new Dictionary<string, VmState>();
            String vmState;
            foreach (String vm in vms)
            {
                 vmState = EnabledState.ToCloudStackState(GetComputerSystem(vm).EnabledState);
                 vmSyncStates.Add(vm, new VmState(vmState, privateIpAddress));
            }
            return vmSyncStates;
        }

        public List<string> GetVmElementNames()
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

        public ProcessorSettingData GetProcSettings(VirtualSystemSettingData vmSettings)
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

        public MemorySettingData GetMemSettings(VirtualSystemSettingData vmSettings)
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


        public ResourceAllocationSettingData GetDvdDriveSettings(VirtualSystemSettingData vmSettings)
        {
            var wmiObjCollection = GetResourceAllocationSettings(vmSettings);

            foreach (ResourceAllocationSettingData wmiObj in wmiObjCollection)
            {
                // DVD drive is '16', see http://msdn.microsoft.com/en-us/library/hh850200(v=vs.85).aspx 
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

        public ResourceAllocationSettingData GetIDEControllerSettings(VirtualSystemSettingData vmSettings, string cntrllerAddr)
        {
            var wmiObjCollection = GetResourceAllocationSettings(vmSettings);

            foreach (ResourceAllocationSettingData wmiObj in wmiObjCollection)
            {
                if (wmiObj.ResourceSubType == IDE_CONTROLLER && wmiObj.Address == cntrllerAddr)
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

        public ResourceAllocationSettingData GetScsiControllerSettings(VirtualSystemSettingData vmSettings)
        {
            var wmiObjCollection = GetResourceAllocationSettings(vmSettings);

            foreach (ResourceAllocationSettingData wmiObj in wmiObjCollection)
            {
                if (wmiObj.ResourceSubType == SCSI_CONTROLLER)
                {
                    return wmiObj;
                }
            }

            var errMsg = string.Format(
                                "Cannot find the Microsoft Synthetic SCSI Controller in VirtualSystemSettingData {1}",
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
        public ResourceAllocationSettingData.ResourceAllocationSettingDataCollection GetResourceAllocationSettings(VirtualSystemSettingData vmSettings)
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

        public EthernetPortAllocationSettingData[] GetEthernetConnections(ComputerSystem vm)
        {
            // ComputerSystem -> VirtualSystemSettingData -> EthernetPortAllocationSettingData
            VirtualSystemSettingData vmSettings = GetVmSettings(vm);

            // An ASSOCIATOR object provides the cross reference from the VirtualSystemSettingData and the 
            // EthernetPortAllocationSettingData, but generated wrappers do not expose a ASSOCIATOR OF query as a method.
            // Instead, we use the System.Management to code the equivalant of 
            //  string query = string.Format( "ASSOCIATORS OF {{{0}}} WHERE ResultClass = {1}", vmSettings.path, resultclassName);
            //
            var wmiObjQuery = new RelatedObjectQuery(vmSettings.Path.Path, EthernetPortAllocationSettingData.CreatedClassName);

            // NB: default scope of ManagementObjectSearcher is '\\.\root\cimv2', which does not contain
            // the virtualisation objects.
            var wmiObjectSearch = new ManagementObjectSearcher(vmSettings.Scope, wmiObjQuery);
            var wmiObjCollection = new EthernetPortAllocationSettingData.EthernetPortAllocationSettingDataCollection(wmiObjectSearch.Get());

            var result = new List<EthernetPortAllocationSettingData>(wmiObjCollection.Count);
            foreach (EthernetPortAllocationSettingData item in wmiObjCollection)
            {
                result.Add(item);
            }
            return result.ToArray();
        }

        public StorageAllocationSettingData[] GetStorageSettings(ComputerSystem vm)
        {
            // ComputerSystem -> VirtualSystemSettingData -> EthernetPortAllocationSettingData
            VirtualSystemSettingData vmSettings = GetVmSettings(vm);

            var wmiObjQuery = new RelatedObjectQuery(vmSettings.Path.Path, StorageAllocationSettingData.CreatedClassName);

            // NB: default scope of ManagementObjectSearcher is '\\.\root\cimv2', which does not contain
            // the virtualisation objects.
            var wmiObjectSearch = new ManagementObjectSearcher(vmSettings.Scope, wmiObjQuery);
            var wmiObjCollection = new StorageAllocationSettingData.StorageAllocationSettingDataCollection(wmiObjectSearch.Get());

            var result = new List<StorageAllocationSettingData>(wmiObjCollection.Count);
            foreach (StorageAllocationSettingData item in wmiObjCollection)
            {
                result.Add(item);
            }
            return result.ToArray();
        }


        public EthernetSwitchPortVlanSettingData GetVlanSettings(EthernetPortAllocationSettingData ethernetConnection)
        {
            // An ASSOCIATOR object provides the cross reference from the VirtualSystemSettingData and the 
            // EthernetPortAllocationSettingData, but generated wrappers do not expose a ASSOCIATOR OF query as a method.
            // Instead, we use the System.Management to code the equivalant of 
            //  string query = string.Format( "ASSOCIATORS OF {{{0}}} WHERE ResultClass = {1}", vmSettings.path, resultclassName);
            //
            var wmiObjQuery = new RelatedObjectQuery(ethernetConnection.Path.Path, EthernetSwitchPortVlanSettingData.CreatedClassName);

            // NB: default scope of ManagementObjectSearcher is '\\.\root\cimv2', which does not contain
            // the virtualisation objects.
            var wmiObjectSearch = new ManagementObjectSearcher(ethernetConnection.Scope, wmiObjQuery);
            var wmiObjCollection = new EthernetSwitchPortVlanSettingData.EthernetSwitchPortVlanSettingDataCollection(wmiObjectSearch.Get());

            if (wmiObjCollection.Count == 0)
            {
                return null;
            }

            // Assert
            if (wmiObjCollection.Count > 1)
            {
                var errMsg = string.Format("Internal error, morn one VLAN settings for a single ethernetConnection");
                var ex = new WmiException(errMsg);
                logger.Error(errMsg, ex);
                throw ex;
            }

            return wmiObjCollection.OfType<EthernetSwitchPortVlanSettingData>().First();
        }
        

        public SyntheticEthernetPortSettingData[] GetEthernetPortSettings(ComputerSystem vm)
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

        public string GetDefaultDataRoot()
        {
            string defaultRootPath = null;
            VirtualSystemManagementServiceSettingData vs_mgmt_data = VirtualSystemManagementServiceSettingData.CreateInstance();
            defaultRootPath = vs_mgmt_data.DefaultVirtualHardDiskPath;
            if (defaultRootPath == null) {
                defaultRootPath = Path.GetPathRoot(Environment.SystemDirectory)  +
                    "\\Users\\Public\\Documents\\Hyper-V\\Virtual hard disks";
            }

            return defaultRootPath;
        }

        public VirtualSystemSettingData GetVmSettings(ComputerSystem vm)
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

        public KvpExchangeComponentSettingData GetKvpSettings(VirtualSystemSettingData vmSettings)
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

        public void GetSummaryInfo(Dictionary<string, VmStatsEntry> vmProcessorInfo, List<System.Management.ManagementPath> vmsToInspect)
        {
            if (vmsToInspect == null || vmsToInspect.Count == 0)
            {
                return;
            }
            // Process info available from WMI, 
            // See http://msdn.microsoft.com/en-us/library/hh850062(v=vs.85).aspx
            uint[] requestedInfo = new uint[] {  // TODO: correct?
                    0, // Name
                    1, // ElementName
                    4, // Number of processes
                    101 // ProcessorLoad
                };

            System.Management.ManagementBaseObject[] sysSummary;
            var vmsvc = GetVirtualisationSystemManagementService();
            System.Management.ManagementPath[] vmPaths = vmsToInspect.ToArray();
            vmsvc.GetSummaryInformation(requestedInfo, vmPaths, out sysSummary);

            foreach (var summary in sysSummary)
            {

                var summaryInfo = new SummaryInformation(summary);

                logger.Debug("VM " + summaryInfo.Name + "(elementName " + summaryInfo.ElementName + ") has " +
                                summaryInfo.NumberOfProcessors + " CPUs, and load of " + summaryInfo.ProcessorLoad);
                var vmInfo = new VmStatsEntry
                {
                    cpuUtilization = summaryInfo.ProcessorLoad,
                    numCPUs = summaryInfo.NumberOfProcessors,
                    networkReadKBs = 1,
                    networkWriteKBs = 1,
                    entityType = "vm"
                };
                vmProcessorInfo.Add(summaryInfo.ElementName, vmInfo);
            }
        }

        public string GetVmNote(System.Management.ManagementPath sysPath)
        {
            uint[] requestedInfo = new uint[] { 3 };
            System.Management.ManagementPath[] vmPaths = new System.Management.ManagementPath[] { sysPath };
            var vmsvc = GetVirtualisationSystemManagementService();
            System.Management.ManagementBaseObject[] sysSummary;
            vmsvc.GetSummaryInformation(requestedInfo, vmPaths, out sysSummary);
            foreach (var summary in sysSummary)
            {
                var summaryInfo = new SummaryInformation(summary);
                return summaryInfo.Notes;
            }

            return null;
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
        //public const UInt16 Test = 7;
        public const UInt16 Defer = 8;
        // public const UInt16 Quiesce = 9;
        // public const UInt16 Reboot = 10;        // A hard reset of the VM.
        public const UInt16 Reset = 11;         // For future use.
        public const UInt16 Paused = 9;     // Pauses the VM.
        public const UInt16 Suspended = 32779;  // Saves the state of the VM.

        public static string ToString(UInt16 value)
        {
            string result = "Unknown RequiredState code";
            switch (value)
            {
                case Enabled: result = "Enabled"; break;
                case Disabled: result = "Disabled"; break;
                case ShutDown: result = "ShutDown"; break;
                case Offline: result = "Offline"; break;
                case Defer: result = "Defer"; break;
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

        public static string ToCloudStackPowerState(UInt16 value)
        {
            string result = "PowerUnknown";
            switch (value)
            {
                case Enabled: result = "PowerOn"; break;
                case Disabled: result = "PowerOff"; break;
                case Paused: result = "PowerUnknown"; break;
                case Suspended: result = "PowerUnknown"; break;
                case Starting: result = "PowerOn"; break;
                case Snapshotting: result = "PowerUnknown"; break; // NOT used
                case Saving: result = "PowerOn"; break;
                case Stopping: result = "PowerOff"; break;
                case Pausing: result = "PowerUnknown"; break;
                case Resuming: result = "PowerOn"; break;
            }
            return result;
        }
    }
}
