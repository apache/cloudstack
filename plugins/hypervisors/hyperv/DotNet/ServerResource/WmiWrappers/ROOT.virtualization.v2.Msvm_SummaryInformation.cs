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
    //
    //
    //
    //
    // If the embedded property is strongly typed then, to strongly type the property to the type of
    // the embedded object, you have to do the following things.
    // 	1. Generate Managed class for the WMI class of the embedded property. This can be done with MgmtClassGen.exe tool or from Server Explorer.
    // 	2. Include the namespace of the generated class.
    // 	3. Change the property get/set functions so as return the instance of the Managed class.
    // Below is a sample code.
    //
    // VB Code
    //
    //
    //
    // Property name
    // Managed class name of Embedded Property
    //
    //
    //
    //
    //
    //
    // C# Code
    //
    //
    //
    // Managed class name of Embedded property
    // Property name
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    // An Early Bound class generated for the WMI class.Msvm_SummaryInformation
    public class SummaryInformation : System.ComponentModel.Component {

        // Private property to hold the WMI namespace in which the class resides.
        private static string CreatedWmiNamespace = "ROOT\\virtualization\\v2";

        // Private property to hold the name of WMI class which created this class.
        private static string CreatedClassName = "Msvm_SummaryInformation";

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
        public SummaryInformation() {
            this.InitializeObject(null, null, null);
        }

        public SummaryInformation(System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(null, path, getOptions);
        }

        public SummaryInformation(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path) {
            this.InitializeObject(mgmtScope, path, null);
        }

        public SummaryInformation(System.Management.ManagementPath path) {
            this.InitializeObject(null, path, null);
        }

        public SummaryInformation(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(mgmtScope, path, getOptions);
        }

        public SummaryInformation(System.Management.ManagementObject theObject) {
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

        public SummaryInformation(System.Management.ManagementBaseObject theObject) {
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
        [Description("The identifier of the physical graphics processing unit (GPU) allocated to this v" +
            "irtual machine (VM). This property only applies to VMs that use RemoteFX.")]
        public string AllocatedGPU {
            get {
                return ((string)(curObj["AllocatedGPU"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsApplicationHealthNull {
            get {
                if ((curObj["ApplicationHealth"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The current application health status for the virtual system. This property may be one of the following values: ""OK""; ""Application Critical""; ""Disabled"".For more information, see the documentation for the StatusDescriptions property of the Msvm_HeartbeatComponent class. This property is not valid for instances of Msvm_SummaryInformation representing a virtual system snapshot.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ApplicationHealthValues ApplicationHealth {
            get {
                if ((curObj["ApplicationHealth"] == null)) {
                    return ((ApplicationHealthValues)(System.Convert.ToInt32(0)));
                }
                return ((ApplicationHealthValues)(System.Convert.ToInt32(curObj["ApplicationHealth"])));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("An array of Msvm_ConcreteJob instances representing any asynchronous operations r" +
            "elated to the virtual system which are currently executing. This property is not" +
            " valid for instances of Msvm_SummaryInformation representing a virtual system sn" +
            "apshot.")]
        public System.Management.ManagementBaseObject[] AsynchronousTasks {
            get {
                return ((System.Management.ManagementBaseObject[])(curObj["AsynchronousTasks"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsAvailableMemoryBufferNull {
            get {
                if ((curObj["AvailableMemoryBuffer"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The available memory buffer percentage in the virtual system.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public int AvailableMemoryBuffer {
            get {
                if ((curObj["AvailableMemoryBuffer"] == null)) {
                    return System.Convert.ToInt32(0);
                }
                return ((int)(curObj["AvailableMemoryBuffer"]));
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
        [Description("The time at which the virtual system or snapshot was created.")]
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

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The friendly name for the virtual system or snapshot.")]
        public string ElementName {
            get {
                return ((string)(curObj["ElementName"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsEnabledStateNull {
            get {
                if ((curObj["EnabledState"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The current state of the virtual system or snapshot.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort EnabledState {
            get {
                if ((curObj["EnabledState"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["EnabledState"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The name of the guest operating system, if available. If this information is not " +
            "available, the value of this property is NULL. This property is not valid for in" +
            "stances of Msvm_SummaryInformation representing a virtual system snapshot.")]
        public string GuestOperatingSystem {
            get {
                return ((string)(curObj["GuestOperatingSystem"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsHealthStateNull {
            get {
                if ((curObj["HealthState"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The current health state for the virtual system. This property is not valid for i" +
            "nstances of Msvm_SummaryInformation representing a virtual system snapshot.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort HealthState {
            get {
                if ((curObj["HealthState"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["HealthState"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsHeartbeatNull {
            get {
                if ((curObj["Heartbeat"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The current heartbeat status for the virtual system. This property may be one of the following values: ""OK""; ""Error""; ""No Contact""; or ""Lost Communication"". For more information, see the documentation for the StatusDescriptions property of the Msvm_HeartbeatComponent class. This property is not valid for instances of Msvm_SummaryInformation representing a virtual system snapshot.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort Heartbeat {
            get {
                if ((curObj["Heartbeat"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["Heartbeat"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsIntegrationServicesVersionStateNull {
            get {
                if ((curObj["IntegrationServicesVersionState"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Whether or not the integration services installed in the virtual machine are up t" +
            "o date")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public IntegrationServicesVersionStateValues IntegrationServicesVersionState {
            get {
                if ((curObj["IntegrationServicesVersionState"] == null)) {
                    return ((IntegrationServicesVersionStateValues)(System.Convert.ToInt32(3)));
                }
                return ((IntegrationServicesVersionStateValues)(System.Convert.ToInt32(curObj["IntegrationServicesVersionState"])));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsMemoryAvailableNull {
            get {
                if ((curObj["MemoryAvailable"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The memory available percentage in the virtual system.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public int MemoryAvailable {
            get {
                if ((curObj["MemoryAvailable"] == null)) {
                    return System.Convert.ToInt32(0);
                }
                return ((int)(curObj["MemoryAvailable"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsMemorySpansPhysicalNumaNodesNull {
            get {
                if ((curObj["MemorySpansPhysicalNumaNodes"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Indicates whether or not the memory of the one or more of the virtual non-uniform" +
            " memory access (NUMA) nodes of the virtual machine spans multiple physical NUMA " +
            "nodes of the hosting computer system.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool MemorySpansPhysicalNumaNodes {
            get {
                if ((curObj["MemorySpansPhysicalNumaNodes"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["MemorySpansPhysicalNumaNodes"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsMemoryUsageNull {
            get {
                if ((curObj["MemoryUsage"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The current memory usage of the virtual system. This property is not valid for in" +
            "stances of Msvm_SummaryInformation representing a virtual system snapshot.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong MemoryUsage {
            get {
                if ((curObj["MemoryUsage"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["MemoryUsage"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The unique name for the virtual system or snapshot.")]
        public string Name {
            get {
                return ((string)(curObj["Name"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The notes associated with the virtual system or snapshot.")]
        public string Notes {
            get {
                return ((string)(curObj["Notes"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsNumberOfProcessorsNull {
            get {
                if ((curObj["NumberOfProcessors"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The total number of virtual processors allocated to the virtual system or snapsho" +
            "t.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort NumberOfProcessors {
            get {
                if ((curObj["NumberOfProcessors"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["NumberOfProcessors"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The current status of the element.")]
        public ushort[] OperationalStatus {
            get {
                return ((ushort[])(curObj["OperationalStatus"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("A string that describes the enabled or disabled state of the element when the Ena" +
            "bledState property is set to 1 (\"Other\"). This property must be set to null when" +
            " EnabledState is any value other than 1.")]
        public string OtherEnabledState {
            get {
                return ((string)(curObj["OtherEnabledState"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsProcessorLoadNull {
            get {
                if ((curObj["ProcessorLoad"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The current processor usage of the virtual system. This property is not valid for" +
            " instances of Msvm_SummaryInformation representing a virtual system snapshot.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort ProcessorLoad {
            get {
                if ((curObj["ProcessorLoad"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["ProcessorLoad"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("An array of the previous 100 samples of the processor usage for the virtual syste" +
            "m. This property is not valid for instances of Msvm_SummaryInformation represent" +
            "ing a virtual system snapshot.")]
        public ushort[] ProcessorLoadHistory {
            get {
                return ((ushort[])(curObj["ProcessorLoadHistory"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsReplicationHealthNull {
            get {
                if ((curObj["ReplicationHealth"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Replication health for the virtual machine.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ReplicationHealthValues ReplicationHealth {
            get {
                if ((curObj["ReplicationHealth"] == null)) {
                    return ((ReplicationHealthValues)(System.Convert.ToInt32(4)));
                }
                return ((ReplicationHealthValues)(System.Convert.ToInt32(curObj["ReplicationHealth"])));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsReplicationModeNull {
            get {
                if ((curObj["ReplicationMode"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Identifies replication type for the virtual machine.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ReplicationModeValues ReplicationMode {
            get {
                if ((curObj["ReplicationMode"] == null)) {
                    return ((ReplicationModeValues)(System.Convert.ToInt32(4)));
                }
                return ((ReplicationModeValues)(System.Convert.ToInt32(curObj["ReplicationMode"])));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsReplicationStateNull {
            get {
                if ((curObj["ReplicationState"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Replication state for the virtual machine.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ReplicationStateValues ReplicationState {
            get {
                if ((curObj["ReplicationState"] == null)) {
                    return ((ReplicationStateValues)(System.Convert.ToInt32(12)));
                }
                return ((ReplicationStateValues)(System.Convert.ToInt32(curObj["ReplicationState"])));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("An array of Msvm_VirtualSystemSettingData instances representing the snapshots fo" +
            "r the virtual system. This property is not valid for instances of Msvm_SummaryIn" +
            "formation representing a virtual system snapshot.")]
        public System.Management.ManagementBaseObject[] Snapshots {
            get {
                return ((System.Management.ManagementBaseObject[])(curObj["Snapshots"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Strings that describe the various OperationalStatus array values.")]
        public string[] StatusDescriptions {
            get {
                return ((string[])(curObj["StatusDescriptions"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsSwapFilesInUseNull {
            get {
                if ((curObj["SwapFilesInUse"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Indecates if smart paging is active.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool SwapFilesInUse {
            get {
                if ((curObj["SwapFilesInUse"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["SwapFilesInUse"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Reference to the CIM_ComputerSystem instance representing the test replica virtua" +
            "l system for the virtual machine. This property is not valid for instances of Ms" +
            "vm_SummaryInformation representing a virtual system snapshot.")]
        public System.Management.ManagementPath TestReplicaSystem {
            get {
                if ((curObj["TestReplicaSystem"] != null)) {
                    return new System.Management.ManagementPath(curObj["TestReplicaSystem"].ToString());
                }
                return null;
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("An array containing a small, thumbnail-sized image of the desktop for the virtual" +
            " system or snapshot in RGB565 format.")]
        public byte[] ThumbnailImage {
            get {
                return ((byte[])(curObj["ThumbnailImage"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsUpTimeNull {
            get {
                if ((curObj["UpTime"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The amount of time since the virtual system was last booted. This property is not" +
            " valid for instances of Msvm_SummaryInformation representing a virtual system sn" +
            "apshot.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong UpTime {
            get {
                if ((curObj["UpTime"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["UpTime"]));
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

        private bool ShouldSerializeApplicationHealth() {
            if ((this.IsApplicationHealthNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeAvailableMemoryBuffer() {
            if ((this.IsAvailableMemoryBufferNull == false)) {
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

        private bool ShouldSerializeCreationTime() {
            if ((this.IsCreationTimeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeEnabledState() {
            if ((this.IsEnabledStateNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeHealthState() {
            if ((this.IsHealthStateNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeHeartbeat() {
            if ((this.IsHeartbeatNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeIntegrationServicesVersionState() {
            if ((this.IsIntegrationServicesVersionStateNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeMemoryAvailable() {
            if ((this.IsMemoryAvailableNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeMemorySpansPhysicalNumaNodes() {
            if ((this.IsMemorySpansPhysicalNumaNodesNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeMemoryUsage() {
            if ((this.IsMemoryUsageNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeNumberOfProcessors() {
            if ((this.IsNumberOfProcessorsNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeProcessorLoad() {
            if ((this.IsProcessorLoadNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeReplicationHealth() {
            if ((this.IsReplicationHealthNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeReplicationMode() {
            if ((this.IsReplicationModeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeReplicationState() {
            if ((this.IsReplicationStateNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeSwapFilesInUse() {
            if ((this.IsSwapFilesInUseNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeUpTime() {
            if ((this.IsUpTimeNull == false)) {
                return true;
            }
            return false;
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

        private static string ConstructPath() {
            string strPath = "ROOT\\virtualization\\v2:Msvm_SummaryInformation";
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
        public static SummaryInformationCollection GetInstances() {
            return GetInstances(null, null, null);
        }

        public static SummaryInformationCollection GetInstances(string condition) {
            return GetInstances(null, condition, null);
        }

        public static SummaryInformationCollection GetInstances(string[] selectedProperties) {
            return GetInstances(null, null, selectedProperties);
        }

        public static SummaryInformationCollection GetInstances(string condition, string[] selectedProperties) {
            return GetInstances(null, condition, selectedProperties);
        }

        public static SummaryInformationCollection GetInstances(System.Management.ManagementScope mgmtScope, System.Management.EnumerationOptions enumOptions) {
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
            pathObj.ClassName = "Msvm_SummaryInformation";
            pathObj.NamespacePath = "root\\virtualization\\v2";
            System.Management.ManagementClass clsObject = new System.Management.ManagementClass(mgmtScope, pathObj, null);
            if ((enumOptions == null)) {
                enumOptions = new System.Management.EnumerationOptions();
                enumOptions.EnsureLocatable = true;
            }
            return new SummaryInformationCollection(clsObject.GetInstances(enumOptions));
        }

        public static SummaryInformationCollection GetInstances(System.Management.ManagementScope mgmtScope, string condition) {
            return GetInstances(mgmtScope, condition, null);
        }

        public static SummaryInformationCollection GetInstances(System.Management.ManagementScope mgmtScope, string[] selectedProperties) {
            return GetInstances(mgmtScope, null, selectedProperties);
        }

        public static SummaryInformationCollection GetInstances(System.Management.ManagementScope mgmtScope, string condition, string[] selectedProperties) {
            if ((mgmtScope == null)) {
                if ((statMgmtScope == null)) {
                    mgmtScope = new System.Management.ManagementScope();
                    mgmtScope.Path.NamespacePath = "root\\virtualization\\v2";
                }
                else {
                    mgmtScope = statMgmtScope;
                }
            }
            System.Management.ManagementObjectSearcher ObjectSearcher = new System.Management.ManagementObjectSearcher(mgmtScope, new SelectQuery("Msvm_SummaryInformation", condition, selectedProperties));
            System.Management.EnumerationOptions enumOptions = new System.Management.EnumerationOptions();
            enumOptions.EnsureLocatable = true;
            ObjectSearcher.Options = enumOptions;
            return new SummaryInformationCollection(ObjectSearcher.Get());
        }

        [Browsable(true)]
        public static SummaryInformation CreateInstance() {
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
            return new SummaryInformation(tmpMgmtClass.CreateInstance());
        }

        [Browsable(true)]
        public void Delete() {
            PrivateLateBoundObject.Delete();
        }

        public enum ApplicationHealthValues {

            OK = 2,

            Application_Critical = 32782,

            Disabled = 32896,

            NULL_ENUM_VALUE = 0,
        }

        public enum IntegrationServicesVersionStateValues {

            Unknown0 = 0,

            UpToDate = 1,

            Mismatch = 2,

            NULL_ENUM_VALUE = 3,
        }

        public enum ReplicationHealthValues {

            Not_applicable = 0,

            Ok = 1,

            Warning = 2,

            Critical = 3,

            NULL_ENUM_VALUE = 4,
        }

        public enum ReplicationModeValues {

            None = 0,

            Primary = 1,

            Recovery = 2,

            Test_Replica = 3,

            NULL_ENUM_VALUE = 4,
        }

        public enum ReplicationStateValues {

            Disabled = 0,

            Ready_for_replication = 1,

            Waiting_to_complete_initial_replication = 2,

            Replicating = 3,

            Synced_replication_complete = 4,

            Recovered = 5,

            Committed = 6,

            Suspended = 7,

            Critical = 8,

            Waiting_to_start_resynchronization = 9,

            Resynchronizing = 10,

            Resynchronization_suspended = 11,

            NULL_ENUM_VALUE = 12,
        }

        // Enumerator implementation for enumerating instances of the class.
        public class SummaryInformationCollection : object, ICollection {

            private ManagementObjectCollection privColObj;

            public SummaryInformationCollection(ManagementObjectCollection objCollection) {
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
                    array.SetValue(new SummaryInformation(((System.Management.ManagementObject)(array.GetValue(nCtr)))), nCtr);
                }
            }

            public virtual System.Collections.IEnumerator GetEnumerator() {
                return new SummaryInformationEnumerator(privColObj.GetEnumerator());
            }

            public class SummaryInformationEnumerator : object, System.Collections.IEnumerator {

                private ManagementObjectCollection.ManagementObjectEnumerator privObjEnum;

                public SummaryInformationEnumerator(ManagementObjectCollection.ManagementObjectEnumerator objEnum) {
                    privObjEnum = objEnum;
                }

                public virtual object Current {
                    get {
                        return new SummaryInformation(((System.Management.ManagementObject)(privObjEnum.Current)));
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
