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

namespace CloudStack.Plugin.WmiWrappers.ROOT.VIRTUALIZATION.V2 {
    using System;
    using System.ComponentModel;
    using System.Management;
    using System.Collections;
    using System.Globalization;
    using System.ComponentModel.Design.Serialization;
    using System.Reflection;


    // Functions ShouldSerialize<PropertyName> are functions used by VS property browser to check if a particular property has to be serialized. These functions are added for all ValueType properties ( properties of type Int32, BOOL etc.. which cannot be set to null). These functions use Is<PropertyName>Null function. These functions are also used in the TypeConverter implementation for the properties to check for NULL value of property so that an empty value can be shown in Property browser in case of Drag and Drop in Visual studio.
    // Functions Is<PropertyName>Null() are used to check if a property is NULL.
    // Functions Reset<PropertyName> are added for Nullable Read/Write properties. These functions are used by VS designer in property browser to set a property to NULL.
    // Every property added to the class for WMI property has attributes set to define its behavior in Visual Studio designer and also to define a TypeConverter to be used.
    // Datetime conversion functions ToDateTime and ToDmtfDateTime are added to the class to convert DMTF datetime to System.DateTime and vice-versa.
    // An Early Bound class generated for the WMI class.Msvm_VirtualSystemSettingData
    public class VirtualSystemSettingData : System.ComponentModel.Component {

        // Private property to hold the WMI namespace in which the class resides.
        private static string CreatedWmiNamespace = "ROOT\\virtualization\\v2";

        // Private property to hold the name of WMI class which created this class.
        public static string CreatedClassName = "Msvm_VirtualSystemSettingData";

        // Private member variable to hold the ManagementScope which is used by the various methods.
        private static System.Management.ManagementScope statMgmtScope = null;

        private ManagementSystemProperties PrivateSystemProperties;

        // Underlying lateBound WMI object.
        private System.Management.ManagementObject PrivateLateBoundObject;

        // Member variable to store the 'automatic commit' behavior for the class.
        private bool AutoCommitProp;

        // Private variable to hold the embedded property representing the instance.
        private System.Management.ManagementBaseObject embeddedObj;

        // The current WMI object used
        private System.Management.ManagementBaseObject curObj;

        // Flag to indicate if the instance is an embedded object.
        private bool isEmbedded;

        // Below are different overloads of constructors to initialize an instance of the class with a WMI object.
        public VirtualSystemSettingData() {
            this.InitializeObject(null, null, null);
        }

        public VirtualSystemSettingData(string keyInstanceID) {
            this.InitializeObject(null, new System.Management.ManagementPath(VirtualSystemSettingData.ConstructPath(keyInstanceID)), null);
        }

        public VirtualSystemSettingData(System.Management.ManagementScope mgmtScope, string keyInstanceID) {
            this.InitializeObject(((System.Management.ManagementScope)(mgmtScope)), new System.Management.ManagementPath(VirtualSystemSettingData.ConstructPath(keyInstanceID)), null);
        }

        public VirtualSystemSettingData(System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(null, path, getOptions);
        }

        public VirtualSystemSettingData(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path) {
            this.InitializeObject(mgmtScope, path, null);
        }

        public VirtualSystemSettingData(System.Management.ManagementPath path) {
            this.InitializeObject(null, path, null);
        }

        public VirtualSystemSettingData(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(mgmtScope, path, getOptions);
        }

        public VirtualSystemSettingData(System.Management.ManagementObject theObject) {
            Initialize();
            if ((CheckIfProperClass(theObject) == true)) {
                PrivateLateBoundObject = theObject;
                PrivateSystemProperties = new ManagementSystemProperties(PrivateLateBoundObject);
                curObj = PrivateLateBoundObject;
            }
            else {
                throw new System.ArgumentException("Class name does not match.");
            }
        }

        public VirtualSystemSettingData(System.Management.ManagementBaseObject theObject) {
            Initialize();
            if ((CheckIfProperClass(theObject) == true)) {
                embeddedObj = theObject;
                PrivateSystemProperties = new ManagementSystemProperties(theObject);
                curObj = embeddedObj;
                isEmbedded = true;
            }
            else {
                throw new System.ArgumentException("Class name does not match.");
            }
        }

        // Property returns the namespace of the WMI class.
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string OriginatingNamespace {
            get {
                return "ROOT\\virtualization\\v2";
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string ManagementClassName {
            get {
                string strRet = CreatedClassName;
                if ((curObj != null)) {
                    if ((curObj.ClassPath != null)) {
                        strRet = ((string)(curObj["__CLASS"]));
                        if (((strRet == null)
                                    || (strRet == string.Empty))) {
                            strRet = CreatedClassName;
                        }
                    }
                }
                return strRet;
            }
        }

        // Property pointing to an embedded object to get System properties of the WMI object.
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public ManagementSystemProperties SystemProperties {
            get {
                return PrivateSystemProperties;
            }
        }

        // Property returning the underlying lateBound object.
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public System.Management.ManagementBaseObject LateBoundObject {
            get {
                return curObj;
            }
        }

        // ManagementScope of the object.
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public System.Management.ManagementScope Scope {
            get {
                if ((isEmbedded == false)) {
                    return PrivateLateBoundObject.Scope;
                }
                else {
                    return null;
                }
            }
            set {
                if ((isEmbedded == false)) {
                    PrivateLateBoundObject.Scope = value;
                }
            }
        }

        // Property to show the commit behavior for the WMI object. If true, WMI object will be automatically saved after each property modification.(ie. Put() is called after modification of a property).
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool AutoCommit {
            get {
                return AutoCommitProp;
            }
            set {
                AutoCommitProp = value;
            }
        }

        // The ManagementPath of the underlying WMI object.
        [Browsable(true)]
        public System.Management.ManagementPath Path {
            get {
                if ((isEmbedded == false)) {
                    return PrivateLateBoundObject.Path;
                }
                else {
                    return null;
                }
            }
            set {
                if ((isEmbedded == false)) {
                    if ((CheckIfProperClass(null, value, null) != true)) {
                        throw new System.ArgumentException("Class name does not match.");
                    }
                    PrivateLateBoundObject.Path = value;
                }
            }
        }

        // Public static scope property which is used by the various methods.
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public static System.Management.ManagementScope StaticScope {
            get {
                return statMgmtScope;
            }
            set {
                statMgmtScope = value;
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"Any additional information provided to the recovery action. The meaning of this property is defined by the action in AutomaticRecoveryAction. If AutomaticRecoveryAction is 0 (""None"") or 1 (""Restart""), this value is NULL. If AutomaticRecoveryAction is 2 (""Revert to Snapshot""), this is the object path to a snapshot that should be applied on failure of the virtual machine worker process.
This is a read-only property, but it can be changed using the ModifyVirtualSystem method of the Msvm_VirtualSystemManagementService class.")]
        public string AdditionalRecoveryInformation {
            get {
                return ((string)(curObj["AdditionalRecoveryInformation"]));
            }
            set {
                curObj["AdditionalRecoveryInformation"] = value;
                if (((isEmbedded == false)
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsAllowFullSCSICommandSetNull {
            get {
                if ((curObj["AllowFullSCSICommandSet"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"Indicates whether SCSI commands from the guest operating system are passed to pass-through disks. If TRUE, SCSI commands emitted by the guest operating system to pass-through disks are not filtered.It is recommended that SCSI filtering remains enabled for production deployments.
This is a read-only property, but it can be changed using the ModifyVirtualSystem method of the Msvm_VirtualSystemManagementService class.
Windows Server 2008:  The AllowFullSCSICommandSet property is not supported.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool AllowFullSCSICommandSet {
            get {
                if ((curObj["AllowFullSCSICommandSet"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["AllowFullSCSICommandSet"]));
            }
            set {
                curObj["AllowFullSCSICommandSet"] = value;
                if (((isEmbedded == false)
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsAllowReducedFcRedundancyNull {
            get {
                if ((curObj["AllowReducedFcRedundancy"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"Indicates whether live migration of a virtual machine that is configured with a virtual FC adapter is allowed to a destination machine, without doing any checks for the existence of paths to the storage devices on the destination
The default value of this property is FALSE. If set to TRUE, the VM can be live migrated to a target machine which may have no or reduced paths to the target FC devices. The guest operating system may lose connectivity to storage and may behave in an unpredictable manner.
 This property should be cleared after a live migration")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool AllowReducedFcRedundancy {
            get {
                if ((curObj["AllowReducedFcRedundancy"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["AllowReducedFcRedundancy"]));
            }
            set {
                curObj["AllowReducedFcRedundancy"] = value;
                if (((isEmbedded == false)
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsAutomaticRecoveryActionNull {
            get {
                if ((curObj["AutomaticRecoveryAction"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort AutomaticRecoveryAction {
            get {
                if ((curObj["AutomaticRecoveryAction"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["AutomaticRecoveryAction"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsAutomaticShutdownActionNull {
            get {
                if ((curObj["AutomaticShutdownAction"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort AutomaticShutdownAction {
            get {
                if ((curObj["AutomaticShutdownAction"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["AutomaticShutdownAction"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsAutomaticStartupActionNull {
            get {
                if ((curObj["AutomaticStartupAction"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort AutomaticStartupAction {
            get {
                if ((curObj["AutomaticStartupAction"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["AutomaticStartupAction"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsAutomaticStartupActionDelayNull {
            get {
                if ((curObj["AutomaticStartupActionDelay"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public System.DateTime AutomaticStartupActionDelay {
            get {
                if ((curObj["AutomaticStartupActionDelay"] != null)) {
                    return ToDateTime(((string)(curObj["AutomaticStartupActionDelay"])));
                }
                else {
                    return System.DateTime.MinValue;
                }
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsAutomaticStartupActionSequenceNumberNull {
            get {
                if ((curObj["AutomaticStartupActionSequenceNumber"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort AutomaticStartupActionSequenceNumber {
            get {
                if ((curObj["AutomaticStartupActionSequenceNumber"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["AutomaticStartupActionSequenceNumber"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The serial number of the base board for the virtual computer system.\nThis is a re" +
            "ad-only property, but it can be changed using the ModifyVirtualSystem method of " +
            "the Msvm_VirtualSystemManagementService class.")]
        public string BaseBoardSerialNumber {
            get {
                return ((string)(curObj["BaseBoardSerialNumber"]));
            }
            set {
                curObj["BaseBoardSerialNumber"] = value;
                if (((isEmbedded == false)
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The globally-unique identifier for the BIOS of the virtual computer system.\nThis " +
            "is a read-only property, but it can be changed using the ModifyVirtualSystem met" +
            "hod of the Msvm_VirtualSystemManagementService class.")]
        public string BIOSGUID {
            get {
                return ((string)(curObj["BIOSGUID"]));
            }
            set {
                curObj["BIOSGUID"] = value;
                if (((isEmbedded == false)
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsBIOSNumLockNull {
            get {
                if ((curObj["BIOSNumLock"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"This property is set to TRUE if the num lock key is set to on by the BIOS, FALSE if the num lock key is set to off by the BIOS.
This is a read-only property, but it can be changed using the ModifyVirtualSystem method of the Msvm_VirtualSystemManagementService class.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool BIOSNumLock {
            get {
                if ((curObj["BIOSNumLock"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["BIOSNumLock"]));
            }
            set {
                curObj["BIOSNumLock"] = value;
                if (((isEmbedded == false)
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The serial number of the BIOS for the virtual computer system.\nThis is a read-onl" +
            "y property, but it can be changed using the ModifyVirtualSystem method of the Ms" +
            "vm_VirtualSystemManagementService class.")]
        public string BIOSSerialNumber {
            get {
                return ((string)(curObj["BIOSSerialNumber"]));
            }
            set {
                curObj["BIOSSerialNumber"] = value;
                if (((isEmbedded == false)
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The boot order set within the BIOS of the virtual computer system. This property is an array of values, max length 4, where each value indicates a device to boot from. The virtual computer system will first attempt to boot from the device indicated by the first value within the array. If that device does not contain a boot sector, the virtual computer system will attempt to boot from the next device specified by the BootOrder property and so on. If no device specified within the BootOrder contains a boot sector the virtual computer system will fail to boot. The default value for a virtual computer system is [0, 1, 2, 3, 4].
Value definitions:
0 (Floppy): The virtual computer system will attempt to boot from the floppy disk within the floppy drive.
1 (CD-ROM): The virtual computer system will attempt to boot from the first CD or DVD disk found with a boot sector.
2 (IDE Hard Drive): The virtual computer system will attempt to boot from the first hard drive found attached to an IDE controller with a boot sector.
3 (PXE Boot): The virtual computer system will attempt to PXE boot from the network.
4 (SCSI Hard Drive): The virtual computer system will attempt to boot from the first hard drive found attached to a SCSI controller with a boot sector.
5-65535: Reserved
This is a read-only property, but it can be changed using the ModifyVirtualSystem method of the Msvm_VirtualSystemManagementService class.")]
        public ushort[] BootOrder {
            get {
                return ((ushort[])(curObj["BootOrder"]));
            }
            set {
                curObj["BootOrder"] = value;
                if (((isEmbedded == false)
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string Caption {
            get {
                return ((string)(curObj["Caption"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The asset tag of the chassis for the virtual computer system.\nThis is a read-only" +
            " property, but it can be changed using the ModifyVirtualSystem method of the Msv" +
            "m_VirtualSystemManagementService class.")]
        public string ChassisAssetTag {
            get {
                return ((string)(curObj["ChassisAssetTag"]));
            }
            set {
                curObj["ChassisAssetTag"] = value;
                if (((isEmbedded == false)
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The serial number of the chassis for the virtual computer system.\nThis is a read-" +
            "only property, but it can be changed using the ModifyVirtualSystem method of the" +
            " Msvm_VirtualSystemManagementService class.")]
        public string ChassisSerialNumber {
            get {
                return ((string)(curObj["ChassisSerialNumber"]));
            }
            set {
                curObj["ChassisSerialNumber"] = value;
                if (((isEmbedded == false)
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string ConfigurationDataRoot {
            get {
                return ((string)(curObj["ConfigurationDataRoot"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string ConfigurationFile {
            get {
                return ((string)(curObj["ConfigurationFile"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string ConfigurationID {
            get {
                return ((string)(curObj["ConfigurationID"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsCreationTimeNull {
            get {
                if ((curObj["CreationTime"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public System.DateTime CreationTime {
            get {
                if ((curObj["CreationTime"] != null)) {
                    return ToDateTime(((string)(curObj["CreationTime"])));
                }
                else {
                    return System.DateTime.MinValue;
                }
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsDebugChannelIdNull {
            get {
                if ((curObj["DebugChannelId"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The channel identifier used to debug the virtual system using the VUD unified deb" +
            "ugger.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint DebugChannelId {
            get {
                if ((curObj["DebugChannelId"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["DebugChannelId"]));
            }
            set {
                curObj["DebugChannelId"] = value;
                if (((isEmbedded == false)
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsDebugPortNull {
            get {
                if ((curObj["DebugPort"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The tcpip port used to debug the virtual system using synthetic debugging.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint DebugPort {
            get {
                if ((curObj["DebugPort"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["DebugPort"]));
            }
            set {
                curObj["DebugPort"] = value;
                if (((isEmbedded == false)
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsDebugPortEnabledNull {
            get {
                if ((curObj["DebugPortEnabled"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Whether the virtual system is using synthetic debugging.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public DebugPortEnabledValues DebugPortEnabled {
            get {
                if ((curObj["DebugPortEnabled"] == null)) {
                    return ((DebugPortEnabledValues)(System.Convert.ToInt32(3)));
                }
                return ((DebugPortEnabledValues)(System.Convert.ToInt32(curObj["DebugPortEnabled"])));
            }
            set {
                if ((DebugPortEnabledValues.NULL_ENUM_VALUE == value)) {
                    curObj["DebugPortEnabled"] = null;
                }
                else {
                    curObj["DebugPortEnabled"] = value;
                }
                if (((isEmbedded == false)
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string Description {
            get {
                return ((string)(curObj["Description"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string ElementName {
            get {
                return ((string)(curObj["ElementName"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsIncrementalBackupEnabledNull {
            get {
                if ((curObj["IncrementalBackupEnabled"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Indicates whether the Hyper-V VSS writer supports taking incremental backup of th" +
            "is Virtual machine.\nThis is a read-write property.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool IncrementalBackupEnabled {
            get {
                if ((curObj["IncrementalBackupEnabled"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["IncrementalBackupEnabled"]));
            }
            set {
                curObj["IncrementalBackupEnabled"] = value;
                if (((isEmbedded == false)
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string InstanceID {
            get {
                return ((string)(curObj["InstanceID"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsIsSavedNull {
            get {
                if ((curObj["IsSaved"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"This property is set to TRUE if the configuration has a reference to a saved state file, FALSE if not. Note that this does not indicate the presence of such a file, only that the configuration specifies one.
This is a read-only property, it cannot be changed.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool IsSaved {
            get {
                if ((curObj["IsSaved"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["IsSaved"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string LogDataRoot {
            get {
                return ((string)(curObj["LogDataRoot"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string[] Notes {
            get {
                return ((string[])(curObj["Notes"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The object path for the snapshot Msvm_VirtualSystemSettingData from which this ob" +
            "ject is based. This property will be NULL if this object is not based off a snap" +
            "shot.")]
        public string Parent {
            get {
                return ((string)(curObj["Parent"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string RecoveryFile {
            get {
                return ((string)(curObj["RecoveryFile"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string SnapshotDataRoot {
            get {
                return ((string)(curObj["SnapshotDataRoot"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string SuspendDataRoot {
            get {
                return ((string)(curObj["SuspendDataRoot"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string SwapFileDataRoot {
            get {
                return ((string)(curObj["SwapFileDataRoot"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The version of the virtual system in a format of \"major.minor\", for example \"2.0\"" +
            ".\nWindows Server 2008:  The Version property is not supported.")]
        public string Version {
            get {
                return ((string)(curObj["Version"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsVirtualNumaEnabledNull {
            get {
                if ((curObj["VirtualNumaEnabled"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"Indicates whether virtual non-uniform memory access (NUMA) nodes are projected into the virtual machine. If FALSE, the virtual machine will have a single node. If TRUE, the number of virtual NUMA nodes projected into the virtual machine is determined from the values of the Msvm_ProcessorSettingData.MaxProcessorsPerNumaNode and Msvm_MemorySettingData.MaxMemoryBlocksPerNumaNode properties.
")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool VirtualNumaEnabled {
            get {
                if ((curObj["VirtualNumaEnabled"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["VirtualNumaEnabled"]));
            }
            set {
                curObj["VirtualNumaEnabled"] = value;
                if (((isEmbedded == false)
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The name of the CIM_ComputerSystem to which this setting data belongs")]
        public string VirtualSystemIdentifier {
            get {
                return ((string)(curObj["VirtualSystemIdentifier"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string VirtualSystemType {
            get {
                return ((string)(curObj["VirtualSystemType"]));
            }
        }

        private bool CheckIfProperClass(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path, System.Management.ObjectGetOptions OptionsParam) {
            if (((path != null)
                        && (string.Compare(path.ClassName, this.ManagementClassName, true, System.Globalization.CultureInfo.InvariantCulture) == 0))) {
                return true;
            }
            else {
                return CheckIfProperClass(new System.Management.ManagementObject(mgmtScope, path, OptionsParam));
            }
        }

        private bool CheckIfProperClass(System.Management.ManagementBaseObject theObj) {
            if (((theObj != null)
                        && (string.Compare(((string)(theObj["__CLASS"])), this.ManagementClassName, true, System.Globalization.CultureInfo.InvariantCulture) == 0))) {
                return true;
            }
            else {
                System.Array parentClasses = ((System.Array)(theObj["__DERIVATION"]));
                if ((parentClasses != null)) {
                    int count = 0;
                    for (count = 0; (count < parentClasses.Length); count = (count + 1)) {
                        if ((string.Compare(((string)(parentClasses.GetValue(count))), this.ManagementClassName, true, System.Globalization.CultureInfo.InvariantCulture) == 0)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private void ResetAdditionalRecoveryInformation() {
            curObj["AdditionalRecoveryInformation"] = null;
            if (((isEmbedded == false)
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }

        private bool ShouldSerializeAllowFullSCSICommandSet() {
            if ((this.IsAllowFullSCSICommandSetNull == false)) {
                return true;
            }
            return false;
        }

        private void ResetAllowFullSCSICommandSet() {
            curObj["AllowFullSCSICommandSet"] = null;
            if (((isEmbedded == false)
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }

        private bool ShouldSerializeAllowReducedFcRedundancy() {
            if ((this.IsAllowReducedFcRedundancyNull == false)) {
                return true;
            }
            return false;
        }

        private void ResetAllowReducedFcRedundancy() {
            curObj["AllowReducedFcRedundancy"] = null;
            if (((isEmbedded == false)
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }

        private bool ShouldSerializeAutomaticRecoveryAction() {
            if ((this.IsAutomaticRecoveryActionNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeAutomaticShutdownAction() {
            if ((this.IsAutomaticShutdownActionNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeAutomaticStartupAction() {
            if ((this.IsAutomaticStartupActionNull == false)) {
                return true;
            }
            return false;
        }

        // Converts a given datetime in DMTF format to System.DateTime object.
        static System.DateTime ToDateTime(string dmtfDate) {
            System.DateTime initializer = System.DateTime.MinValue;
            int year = initializer.Year;
            int month = initializer.Month;
            int day = initializer.Day;
            int hour = initializer.Hour;
            int minute = initializer.Minute;
            int second = initializer.Second;
            long ticks = 0;
            string dmtf = dmtfDate;
            System.DateTime datetime = System.DateTime.MinValue;
            string tempString = string.Empty;
            if ((dmtf == null)) {
                throw new System.ArgumentOutOfRangeException();
            }
            if ((dmtf.Length == 0)) {
                throw new System.ArgumentOutOfRangeException();
            }
            if ((dmtf.Length != 25)) {
                throw new System.ArgumentOutOfRangeException();
            }
            try {
                tempString = dmtf.Substring(0, 4);
                if (("****" != tempString)) {
                    year = int.Parse(tempString);
                }
                tempString = dmtf.Substring(4, 2);
                if (("**" != tempString)) {
                    month = int.Parse(tempString);
                }
                tempString = dmtf.Substring(6, 2);
                if (("**" != tempString)) {
                    day = int.Parse(tempString);
                }
                tempString = dmtf.Substring(8, 2);
                if (("**" != tempString)) {
                    hour = int.Parse(tempString);
                }
                tempString = dmtf.Substring(10, 2);
                if (("**" != tempString)) {
                    minute = int.Parse(tempString);
                }
                tempString = dmtf.Substring(12, 2);
                if (("**" != tempString)) {
                    second = int.Parse(tempString);
                }
                tempString = dmtf.Substring(15, 6);
                if (("******" != tempString)) {
                    ticks = (long.Parse(tempString) * ((long)((System.TimeSpan.TicksPerMillisecond / 1000))));
                }
                if (((((((((year < 0)
                            || (month < 0))
                            || (day < 0))
                            || (hour < 0))
                            || (minute < 0))
                            || (minute < 0))
                            || (second < 0))
                            || (ticks < 0))) {
                    throw new System.ArgumentOutOfRangeException();
                }
            }
            catch (System.Exception e) {
                throw new System.ArgumentOutOfRangeException(null, e.Message);
            }
            datetime = new System.DateTime(year, month, day, hour, minute, second, 0);
            datetime = datetime.AddTicks(ticks);
            System.TimeSpan tickOffset = System.TimeZone.CurrentTimeZone.GetUtcOffset(datetime);
            int UTCOffset = 0;
            int OffsetToBeAdjusted = 0;
            long OffsetMins = ((long)((tickOffset.Ticks / System.TimeSpan.TicksPerMinute)));
            tempString = dmtf.Substring(22, 3);
            if ((tempString != "******")) {
                tempString = dmtf.Substring(21, 4);
                try {
                    UTCOffset = int.Parse(tempString);
                }
                catch (System.Exception e) {
                    throw new System.ArgumentOutOfRangeException(null, e.Message);
                }
                OffsetToBeAdjusted = ((int)((OffsetMins - UTCOffset)));
                datetime = datetime.AddMinutes(((double)(OffsetToBeAdjusted)));
            }
            return datetime;
        }

        // Converts a given System.DateTime object to DMTF datetime format.
        static string ToDmtfDateTime(System.DateTime date) {
            string utcString = string.Empty;
            System.TimeSpan tickOffset = System.TimeZone.CurrentTimeZone.GetUtcOffset(date);
            long OffsetMins = ((long)((tickOffset.Ticks / System.TimeSpan.TicksPerMinute)));
            if ((System.Math.Abs(OffsetMins) > 999)) {
                date = date.ToUniversalTime();
                utcString = "+000";
            }
            else {
                if ((tickOffset.Ticks >= 0)) {
                    utcString = string.Concat("+", ((long)((tickOffset.Ticks / System.TimeSpan.TicksPerMinute))).ToString().PadLeft(3, '0'));
                }
                else {
                    string strTemp = ((long)(OffsetMins)).ToString();
                    utcString = string.Concat("-", strTemp.Substring(1, (strTemp.Length - 1)).PadLeft(3, '0'));
                }
            }
            string dmtfDateTime = ((int)(date.Year)).ToString().PadLeft(4, '0');
            dmtfDateTime = string.Concat(dmtfDateTime, ((int)(date.Month)).ToString().PadLeft(2, '0'));
            dmtfDateTime = string.Concat(dmtfDateTime, ((int)(date.Day)).ToString().PadLeft(2, '0'));
            dmtfDateTime = string.Concat(dmtfDateTime, ((int)(date.Hour)).ToString().PadLeft(2, '0'));
            dmtfDateTime = string.Concat(dmtfDateTime, ((int)(date.Minute)).ToString().PadLeft(2, '0'));
            dmtfDateTime = string.Concat(dmtfDateTime, ((int)(date.Second)).ToString().PadLeft(2, '0'));
            dmtfDateTime = string.Concat(dmtfDateTime, ".");
            System.DateTime dtTemp = new System.DateTime(date.Year, date.Month, date.Day, date.Hour, date.Minute, date.Second, 0);
            long microsec = ((long)((((date.Ticks - dtTemp.Ticks)
                        * 1000)
                        / System.TimeSpan.TicksPerMillisecond)));
            string strMicrosec = ((long)(microsec)).ToString();
            if ((strMicrosec.Length > 6)) {
                strMicrosec = strMicrosec.Substring(0, 6);
            }
            dmtfDateTime = string.Concat(dmtfDateTime, strMicrosec.PadLeft(6, '0'));
            dmtfDateTime = string.Concat(dmtfDateTime, utcString);
            return dmtfDateTime;
        }

        private bool ShouldSerializeAutomaticStartupActionDelay() {
            if ((this.IsAutomaticStartupActionDelayNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeAutomaticStartupActionSequenceNumber() {
            if ((this.IsAutomaticStartupActionSequenceNumberNull == false)) {
                return true;
            }
            return false;
        }

        private void ResetBaseBoardSerialNumber() {
            curObj["BaseBoardSerialNumber"] = null;
            if (((isEmbedded == false)
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }

        private void ResetBIOSGUID() {
            curObj["BIOSGUID"] = null;
            if (((isEmbedded == false)
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }

        private bool ShouldSerializeBIOSNumLock() {
            if ((this.IsBIOSNumLockNull == false)) {
                return true;
            }
            return false;
        }

        private void ResetBIOSNumLock() {
            curObj["BIOSNumLock"] = null;
            if (((isEmbedded == false)
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }

        private void ResetBIOSSerialNumber() {
            curObj["BIOSSerialNumber"] = null;
            if (((isEmbedded == false)
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }

        private void ResetBootOrder() {
            curObj["BootOrder"] = null;
            if (((isEmbedded == false)
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }

        private void ResetChassisAssetTag() {
            curObj["ChassisAssetTag"] = null;
            if (((isEmbedded == false)
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }

        private void ResetChassisSerialNumber() {
            curObj["ChassisSerialNumber"] = null;
            if (((isEmbedded == false)
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }

        private bool ShouldSerializeCreationTime() {
            if ((this.IsCreationTimeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeDebugChannelId() {
            if ((this.IsDebugChannelIdNull == false)) {
                return true;
            }
            return false;
        }

        private void ResetDebugChannelId() {
            curObj["DebugChannelId"] = null;
            if (((isEmbedded == false)
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }

        private bool ShouldSerializeDebugPort() {
            if ((this.IsDebugPortNull == false)) {
                return true;
            }
            return false;
        }

        private void ResetDebugPort() {
            curObj["DebugPort"] = null;
            if (((isEmbedded == false)
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }

        private bool ShouldSerializeDebugPortEnabled() {
            if ((this.IsDebugPortEnabledNull == false)) {
                return true;
            }
            return false;
        }

        private void ResetDebugPortEnabled() {
            curObj["DebugPortEnabled"] = null;
            if (((isEmbedded == false)
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }

        private bool ShouldSerializeIncrementalBackupEnabled() {
            if ((this.IsIncrementalBackupEnabledNull == false)) {
                return true;
            }
            return false;
        }

        private void ResetIncrementalBackupEnabled() {
            curObj["IncrementalBackupEnabled"] = null;
            if (((isEmbedded == false)
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }

        private bool ShouldSerializeIsSaved() {
            if ((this.IsIsSavedNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeVirtualNumaEnabled() {
            if ((this.IsVirtualNumaEnabledNull == false)) {
                return true;
            }
            return false;
        }

        private void ResetVirtualNumaEnabled() {
            curObj["VirtualNumaEnabled"] = null;
            if (((isEmbedded == false)
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }

        [Browsable(true)]
        public void CommitObject() {
            if ((isEmbedded == false)) {
                PrivateLateBoundObject.Put();
            }
        }

        [Browsable(true)]
        public void CommitObject(System.Management.PutOptions putOptions) {
            if ((isEmbedded == false)) {
                PrivateLateBoundObject.Put(putOptions);
            }
        }

        private void Initialize() {
            AutoCommitProp = true;
            isEmbedded = false;
        }

        private static string ConstructPath(string keyInstanceID) {
            string strPath = "ROOT\\virtualization\\v2:Msvm_VirtualSystemSettingData";
            strPath = string.Concat(strPath, string.Concat(".InstanceID=", string.Concat("\"", string.Concat(keyInstanceID, "\""))));
            return strPath;
        }

        private void InitializeObject(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            Initialize();
            if ((path != null)) {
                if ((CheckIfProperClass(mgmtScope, path, getOptions) != true)) {
                    throw new System.ArgumentException("Class name does not match.");
                }
            }
            PrivateLateBoundObject = new System.Management.ManagementObject(mgmtScope, path, getOptions);
            PrivateSystemProperties = new ManagementSystemProperties(PrivateLateBoundObject);
            curObj = PrivateLateBoundObject;
        }

        // Different overloads of GetInstances() help in enumerating instances of the WMI class.
        public static VirtualSystemSettingDataCollection GetInstances() {
            return GetInstances(null, null, null);
        }

        public static VirtualSystemSettingDataCollection GetInstances(string condition) {
            return GetInstances(null, condition, null);
        }

        public static VirtualSystemSettingDataCollection GetInstances(string[] selectedProperties) {
            return GetInstances(null, null, selectedProperties);
        }

        public static VirtualSystemSettingDataCollection GetInstances(string condition, string[] selectedProperties) {
            return GetInstances(null, condition, selectedProperties);
        }

        public static VirtualSystemSettingDataCollection GetInstances(System.Management.ManagementScope mgmtScope, System.Management.EnumerationOptions enumOptions) {
            if ((mgmtScope == null)) {
                if ((statMgmtScope == null)) {
                    mgmtScope = new System.Management.ManagementScope();
                    mgmtScope.Path.NamespacePath = "root\\virtualization\\v2";
                }
                else {
                    mgmtScope = statMgmtScope;
                }
            }
            System.Management.ManagementPath pathObj = new System.Management.ManagementPath();
            pathObj.ClassName = "Msvm_VirtualSystemSettingData";
            pathObj.NamespacePath = "root\\virtualization\\v2";
            System.Management.ManagementClass clsObject = new System.Management.ManagementClass(mgmtScope, pathObj, null);
            if ((enumOptions == null)) {
                enumOptions = new System.Management.EnumerationOptions();
                enumOptions.EnsureLocatable = true;
            }
            return new VirtualSystemSettingDataCollection(clsObject.GetInstances(enumOptions));
        }

        public static VirtualSystemSettingDataCollection GetInstances(System.Management.ManagementScope mgmtScope, string condition) {
            return GetInstances(mgmtScope, condition, null);
        }

        public static VirtualSystemSettingDataCollection GetInstances(System.Management.ManagementScope mgmtScope, string[] selectedProperties) {
            return GetInstances(mgmtScope, null, selectedProperties);
        }

        public static VirtualSystemSettingDataCollection GetInstances(System.Management.ManagementScope mgmtScope, string condition, string[] selectedProperties) {
            if ((mgmtScope == null)) {
                if ((statMgmtScope == null)) {
                    mgmtScope = new System.Management.ManagementScope();
                    mgmtScope.Path.NamespacePath = "root\\virtualization\\v2";
                }
                else {
                    mgmtScope = statMgmtScope;
                }
            }
            System.Management.ManagementObjectSearcher ObjectSearcher = new System.Management.ManagementObjectSearcher(mgmtScope, new SelectQuery("Msvm_VirtualSystemSettingData", condition, selectedProperties));
            System.Management.EnumerationOptions enumOptions = new System.Management.EnumerationOptions();
            enumOptions.EnsureLocatable = true;
            ObjectSearcher.Options = enumOptions;
            return new VirtualSystemSettingDataCollection(ObjectSearcher.Get());
        }

        [Browsable(true)]
        public static VirtualSystemSettingData CreateInstance() {
            System.Management.ManagementScope mgmtScope = null;
            if ((statMgmtScope == null)) {
                mgmtScope = new System.Management.ManagementScope();
                mgmtScope.Path.NamespacePath = CreatedWmiNamespace;
            }
            else {
                mgmtScope = statMgmtScope;
            }
            System.Management.ManagementPath mgmtPath = new System.Management.ManagementPath(CreatedClassName);
            System.Management.ManagementClass tmpMgmtClass = new System.Management.ManagementClass(mgmtScope, mgmtPath, null);
            return new VirtualSystemSettingData(tmpMgmtClass.CreateInstance());
        }

        [Browsable(true)]
        public void Delete() {
            PrivateLateBoundObject.Delete();
        }

        public enum DebugPortEnabledValues {

            Val__Off = 0,

            On = 1,

            OnAutoAssigned = 2,

            NULL_ENUM_VALUE = 3,
        }

        // Enumerator implementation for enumerating instances of the class.
        public class VirtualSystemSettingDataCollection : object, ICollection {

            private ManagementObjectCollection privColObj;

            public VirtualSystemSettingDataCollection(ManagementObjectCollection objCollection) {
                privColObj = objCollection;
            }

            public virtual int Count {
                get {
                    return privColObj.Count;
                }
            }

            public virtual bool IsSynchronized {
                get {
                    return privColObj.IsSynchronized;
                }
            }

            public virtual object SyncRoot {
                get {
                    return this;
                }
            }

            public virtual void CopyTo(System.Array array, int index) {
                privColObj.CopyTo(array, index);
                int nCtr;
                for (nCtr = 0; (nCtr < array.Length); nCtr = (nCtr + 1)) {
                    array.SetValue(new VirtualSystemSettingData(((System.Management.ManagementObject)(array.GetValue(nCtr)))), nCtr);
                }
            }

            public virtual System.Collections.IEnumerator GetEnumerator() {
                return new VirtualSystemSettingDataEnumerator(privColObj.GetEnumerator());
            }

            public class VirtualSystemSettingDataEnumerator : object, System.Collections.IEnumerator {

                private ManagementObjectCollection.ManagementObjectEnumerator privObjEnum;

                public VirtualSystemSettingDataEnumerator(ManagementObjectCollection.ManagementObjectEnumerator objEnum) {
                    privObjEnum = objEnum;
                }

                public virtual object Current {
                    get {
                        return new VirtualSystemSettingData(((System.Management.ManagementObject)(privObjEnum.Current)));
                    }
                }

                public virtual bool MoveNext() {
                    return privObjEnum.MoveNext();
                }

                public virtual void Reset() {
                    privObjEnum.Reset();
                }
            }
        }

        // TypeConverter to handle null values for ValueType properties
        public class WMIValueTypeConverter : TypeConverter {

            private TypeConverter baseConverter;

            private System.Type baseType;

            public WMIValueTypeConverter(System.Type inBaseType) {
                baseConverter = TypeDescriptor.GetConverter(inBaseType);
                baseType = inBaseType;
            }

            public override bool CanConvertFrom(System.ComponentModel.ITypeDescriptorContext context, System.Type srcType) {
                return baseConverter.CanConvertFrom(context, srcType);
            }

            public override bool CanConvertTo(System.ComponentModel.ITypeDescriptorContext context, System.Type destinationType) {
                return baseConverter.CanConvertTo(context, destinationType);
            }

            public override object ConvertFrom(System.ComponentModel.ITypeDescriptorContext context, System.Globalization.CultureInfo culture, object value) {
                return baseConverter.ConvertFrom(context, culture, value);
            }

            public override object CreateInstance(System.ComponentModel.ITypeDescriptorContext context, System.Collections.IDictionary dictionary) {
                return baseConverter.CreateInstance(context, dictionary);
            }

            public override bool GetCreateInstanceSupported(System.ComponentModel.ITypeDescriptorContext context) {
                return baseConverter.GetCreateInstanceSupported(context);
            }

            public override PropertyDescriptorCollection GetProperties(System.ComponentModel.ITypeDescriptorContext context, object value, System.Attribute[] attributeVar) {
                return baseConverter.GetProperties(context, value, attributeVar);
            }

            public override bool GetPropertiesSupported(System.ComponentModel.ITypeDescriptorContext context) {
                return baseConverter.GetPropertiesSupported(context);
            }

            public override System.ComponentModel.TypeConverter.StandardValuesCollection GetStandardValues(System.ComponentModel.ITypeDescriptorContext context) {
                return baseConverter.GetStandardValues(context);
            }

            public override bool GetStandardValuesExclusive(System.ComponentModel.ITypeDescriptorContext context) {
                return baseConverter.GetStandardValuesExclusive(context);
            }

            public override bool GetStandardValuesSupported(System.ComponentModel.ITypeDescriptorContext context) {
                return baseConverter.GetStandardValuesSupported(context);
            }

            public override object ConvertTo(System.ComponentModel.ITypeDescriptorContext context, System.Globalization.CultureInfo culture, object value, System.Type destinationType) {
                if ((baseType.BaseType == typeof(System.Enum))) {
                    if ((value.GetType() == destinationType)) {
                        return value;
                    }
                    if ((((value == null)
                                && (context != null))
                                && (context.PropertyDescriptor.ShouldSerializeValue(context.Instance) == false))) {
                        return  "NULL_ENUM_VALUE" ;
                    }
                    return baseConverter.ConvertTo(context, culture, value, destinationType);
                }
                if (((baseType == typeof(bool))
                            && (baseType.BaseType == typeof(System.ValueType)))) {
                    if ((((value == null)
                                && (context != null))
                                && (context.PropertyDescriptor.ShouldSerializeValue(context.Instance) == false))) {
                        return "";
                    }
                    return baseConverter.ConvertTo(context, culture, value, destinationType);
                }
                if (((context != null)
                            && (context.PropertyDescriptor.ShouldSerializeValue(context.Instance) == false))) {
                    return "";
                }
                return baseConverter.ConvertTo(context, culture, value, destinationType);
            }
        }

        // Embedded class to represent WMI system Properties.
        [TypeConverter(typeof(System.ComponentModel.ExpandableObjectConverter))]
        public class ManagementSystemProperties {

            private System.Management.ManagementBaseObject PrivateLateBoundObject;

            public ManagementSystemProperties(System.Management.ManagementBaseObject ManagedObject) {
                PrivateLateBoundObject = ManagedObject;
            }

            [Browsable(true)]
            public int GENUS {
                get {
                    return ((int)(PrivateLateBoundObject["__GENUS"]));
                }
            }

            [Browsable(true)]
            public string CLASS {
                get {
                    return ((string)(PrivateLateBoundObject["__CLASS"]));
                }
            }

            [Browsable(true)]
            public string SUPERCLASS {
                get {
                    return ((string)(PrivateLateBoundObject["__SUPERCLASS"]));
                }
            }

            [Browsable(true)]
            public string DYNASTY {
                get {
                    return ((string)(PrivateLateBoundObject["__DYNASTY"]));
                }
            }

            [Browsable(true)]
            public string RELPATH {
                get {
                    return ((string)(PrivateLateBoundObject["__RELPATH"]));
                }
            }

            [Browsable(true)]
            public int PROPERTY_COUNT {
                get {
                    return ((int)(PrivateLateBoundObject["__PROPERTY_COUNT"]));
                }
            }

            [Browsable(true)]
            public string[] DERIVATION {
                get {
                    return ((string[])(PrivateLateBoundObject["__DERIVATION"]));
                }
            }

            [Browsable(true)]
            public string SERVER {
                get {
                    return ((string)(PrivateLateBoundObject["__SERVER"]));
                }
            }

            [Browsable(true)]
            public string NAMESPACE {
                get {
                    return ((string)(PrivateLateBoundObject["__NAMESPACE"]));
                }
            }

            [Browsable(true)]
            public string PATH {
                get {
                    return ((string)(PrivateLateBoundObject["__PATH"]));
                }
            }
        }
    }
}
