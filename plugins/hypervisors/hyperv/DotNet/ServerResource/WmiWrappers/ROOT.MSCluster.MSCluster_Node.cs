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

namespace CloudStack.Plugin.WmiWrappers.ROOT.MSCLUSTER {
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
    // An Early Bound class generated for the WMI class.MSCluster_Node
    public class Node : System.ComponentModel.Component {
        
        // Private property to hold the WMI namespace in which the class resides.
        private static string CreatedWmiNamespace = "ROOT\\MSCluster";
        
        // Private property to hold the name of WMI class which created this class.
        private static string CreatedClassName = "MSCluster_Node";
        
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
        public Node() {
            this.InitializeObject(null, null, null);
        }
        
        public Node(string keyName) {
            this.InitializeObject(null, new System.Management.ManagementPath(Node.ConstructPath(keyName)), null);
        }
        
        public Node(System.Management.ManagementScope mgmtScope, string keyName) {
            this.InitializeObject(((System.Management.ManagementScope)(mgmtScope)), new System.Management.ManagementPath(Node.ConstructPath(keyName)), null);
        }
        
        public Node(System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(null, path, getOptions);
        }
        
        public Node(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path) {
            this.InitializeObject(mgmtScope, path, null);
        }
        
        public Node(System.Management.ManagementPath path) {
            this.InitializeObject(null, path, null);
        }
        
        public Node(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(mgmtScope, path, getOptions);
        }
        
        public Node(System.Management.ManagementObject theObject) {
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
        
        public Node(System.Management.ManagementBaseObject theObject) {
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
                return "ROOT\\MSCluster";
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
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsBuildNumberNull {
            get {
                if ((curObj["BuildNumber"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The BuildNumber property specifies the build number of Microsoft® Windows version" +
            " installed on the node.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint BuildNumber {
            get {
                if ((curObj["BuildNumber"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["BuildNumber"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Caption property is a short textual description (one-line string) of the obje" +
            "ct.")]
        public string Caption {
            get {
                return ((string)(curObj["Caption"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsCharacteristicsNull {
            get {
                if ((curObj["Characteristics"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Retrieves the intrinsic characteristics of the node.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint Characteristics {
            get {
                if ((curObj["Characteristics"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["Characteristics"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("CreationClassName indicates the name of the class or the subclass used in the cre" +
            "ation of an instance. When used with the other key properties of this class, thi" +
            "s property allows all instances of this class and its subclasses to be uniquely " +
            "identified.")]
        public string CreationClassName {
            get {
                return ((string)(curObj["CreationClassName"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The CSDVersion property specifies the most recent service pack installed on the n" +
            "ode (if any).")]
        public string CSDVersion {
            get {
                return ((string)(curObj["CSDVersion"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"Enumeration indicating whether the ComputerSystem is a special-purpose System (ie, dedicated to a particular use), versus being 'general purpose'. For example, one could specify that the System is dedicated to ""Print"" (value=11) or acts as a ""Hub"" (value=8).")]
        public DedicatedValues[] Dedicated {
            get {
                System.Array arrEnumVals = ((System.Array)(curObj["Dedicated"]));
                DedicatedValues[] enumToRet = new DedicatedValues[arrEnumVals.Length];
                int counter = 0;
                for (counter = 0; (counter < arrEnumVals.Length); counter = (counter + 1)) {
                    enumToRet[counter] = ((DedicatedValues)(System.Convert.ToInt32(arrEnumVals.GetValue(counter))));
                }
                return enumToRet;
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Description property provides comments about the node.")]
        public string Description {
            get {
                return ((string)(curObj["Description"]));
            }
            set {
                curObj["Description"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsDynamicWeightNull {
            get {
                if ((curObj["DynamicWeight"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The DynamicWeight property provides dynamic vote weight of the node adjusted by d" +
            "ynamic quorum feature")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint DynamicWeight {
            get {
                if ((curObj["DynamicWeight"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["DynamicWeight"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsFlagsNull {
            get {
                if ((curObj["Flags"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Retrieves the flags that are set for the node.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint Flags {
            get {
                if ((curObj["Flags"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["Flags"]));
            }
            set {
                curObj["Flags"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Id of the node.")]
        public string Id {
            get {
                return ((string)(curObj["Id"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("An array of free-form strings providing explanations and details behind the entri" +
            "es in the OtherIdentifyingInfo array. Note, each entry of this array is related " +
            "to the entry in OtherIdentifyingInfo that is located at the same index.")]
        public string[] IdentifyingDescriptions {
            get {
                return ((string[])(curObj["IdentifyingDescriptions"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("This object contains the data needed to find either the initial load device (its " +
            "key) or the boot service to request the operating system to start up. In additio" +
            "n, the load parameters (ie, a pathname and parameters) may also be specified.")]
        public string[] InitialLoadInfo {
            get {
                return ((string[])(curObj["InitialLoadInfo"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsInstallDateNull {
            get {
                if ((curObj["InstallDate"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("A datetime value indicating when the object was installed. A lack of a value does" +
            " not indicate that the object is not installed.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public System.DateTime InstallDate {
            get {
                if ((curObj["InstallDate"] != null)) {
                    return ToDateTime(((string)(curObj["InstallDate"])));
                }
                else {
                    return System.DateTime.MinValue;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("This object contains the data identifying either the initial load device (its key" +
            ") or the boot service that requested the last operating system load. In addition" +
            ", the load parameters (ie, a pathname and parameters) may also be specified.")]
        public string LastLoadInfo {
            get {
                return ((string)(curObj["LastLoadInfo"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsMajorVersionNull {
            get {
                if ((curObj["MajorVersion"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The MajorVersion property specifies the integer component of the Microsoft® Windo" +
            "ws version installed on the node.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint MajorVersion {
            get {
                if ((curObj["MajorVersion"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["MajorVersion"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsMinorVersionNull {
            get {
                if ((curObj["MinorVersion"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The MinorVersion property specifies the decimal component of the Microsoft® Windo" +
            "ws version installed on the node.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint MinorVersion {
            get {
                if ((curObj["MinorVersion"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["MinorVersion"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The inherited Name serves as key of a System instance in an enterprise environmen" +
            "t.")]
        public string Name {
            get {
                return ((string)(curObj["Name"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The ComputerSystem object and its derivatives are Top Level Objects of CIM. They provide the scope for numerous components. Having unique System keys is required. A heuristic is defined to create the ComputerSystem Name to attempt to always generate the same Name, independent of discovery protocol. This prevents inventory and management problems where the same asset or entity is discovered multiple times, but can not be resolved to a single object. Use of the heuristic is optional, but recommended.

The NameFormat property identifies how the ComputerSystem Name is generated, using a heuristic. The heuristic is outlined, in detail, in the CIM V2 System Model spec. It assumes that the documented rules are traversed in order, to determine and assign a Name. The NameFormat Values list defines the precedence order for assigning the ComputerSystem Name. Several rules do map to the same Value.

Note that the ComputerSystem Name calculated using the heuristic is the System's key value. Other names can be assigned and used for the ComputerSystem, that better suit a business, using Aliases.")]
        public string NameFormat {
            get {
                return ((string)(curObj["NameFormat"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsNeedsPreventQuorumNull {
            get {
                if ((curObj["NeedsPreventQuorum"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Instructs if the cluster service on that node should be started with prevent quor" +
            "um flag.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint NeedsPreventQuorum {
            get {
                if ((curObj["NeedsPreventQuorum"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["NeedsPreventQuorum"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsNodeDrainStatusNull {
            get {
                if ((curObj["NodeDrainStatus"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The current node drain status of a node.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public NodeDrainStatusValues NodeDrainStatus {
            get {
                if ((curObj["NodeDrainStatus"] == null)) {
                    return ((NodeDrainStatusValues)(System.Convert.ToInt32(4)));
                }
                return ((NodeDrainStatusValues)(System.Convert.ToInt32(curObj["NodeDrainStatus"])));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The NodeDrainTarget property specifies the target specified for the node drain.")]
        public string NodeDrainTarget {
            get {
                return ((string)(curObj["NodeDrainTarget"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsNodeHighestVersionNull {
            get {
                if ((curObj["NodeHighestVersion"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The NodeHighestVersion property specifies the highest possible version of Cluster" +
            "ing Service with which the node can join or communicate.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint NodeHighestVersion {
            get {
                if ((curObj["NodeHighestVersion"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["NodeHighestVersion"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The NodeInstanceID property specifies the instance of node.")]
        public string NodeInstanceID {
            get {
                return ((string)(curObj["NodeInstanceID"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsNodeLowestVersionNull {
            get {
                if ((curObj["NodeLowestVersion"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The NodeLowestVersion property specifies the lowest possible version of Clusterin" +
            "g Service with which the node can join or communicate.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint NodeLowestVersion {
            get {
                if ((curObj["NodeLowestVersion"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["NodeLowestVersion"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsNodeWeightNull {
            get {
                if ((curObj["NodeWeight"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The NodeWeight property provides vote weight of the node.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint NodeWeight {
            get {
                if ((curObj["NodeWeight"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["NodeWeight"]));
            }
            set {
                curObj["NodeWeight"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"OtherIdentifyingInfo captures additional data, beyond System Name information, that could be used to identify a ComputerSystem. One example would be to hold the Fibre Channel World-Wide Name (WWN) of a node. Note that if only the Fibre Channel name is available and is unique (able to be used as the System key), then this property would be NULL and the WWN would become the System key, its data placed in the Name property.")]
        public string[] OtherIdentifyingInfo {
            get {
                return ((string[])(curObj["OtherIdentifyingInfo"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"Indicates the specific power-related capabilities of a ComputerSystem and its associated running OperatingSystem. The values, 0=""Unknown"", 1=""Not Supported"", and 2=""Disabled"" are self-explanatory. The value, 3=""Enabled"" indicates that the power management features are currently enabled but the exact feature set is unknown or the information is unavailable. ""Power Saving Modes Entered Automatically"" (4) describes that a system can change its power state based on usage or other criteria. ""Power State Settable"" (5) indicates that the SetPowerState method is supported. ""Power Cycling Supported"" (6) indicates that the SetPowerState method can be invoked with the PowerState input variable set to 5 (""Power Cycle""). ""Timed Power On Supported"" (7) indicates that the SetPowerState method can be invoked with the PowerState input variable set to 5 (""Power Cycle"") and the Time parameter set to a specific date and time, or interval, for power-on.")]
        public PowerManagementCapabilitiesValues[] PowerManagementCapabilities {
            get {
                System.Array arrEnumVals = ((System.Array)(curObj["PowerManagementCapabilities"]));
                PowerManagementCapabilitiesValues[] enumToRet = new PowerManagementCapabilitiesValues[arrEnumVals.Length];
                int counter = 0;
                for (counter = 0; (counter < arrEnumVals.Length); counter = (counter + 1)) {
                    enumToRet[counter] = ((PowerManagementCapabilitiesValues)(System.Convert.ToInt32(arrEnumVals.GetValue(counter))));
                }
                return enumToRet;
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPowerManagementSupportedNull {
            get {
                if ((curObj["PowerManagementSupported"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"Boolean indicating that the ComputerSystem, with its running OperatingSystem, supports power management. This boolean does not indicate that power management features are currently enabled, or if enabled, what features are supported. Refer to the PowerManagementCapabilities array for this information. If this boolean is false, the integer value 1 for the string, ""Not Supported"", should be the only entry in the PowerManagementCapabilities array.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool PowerManagementSupported {
            get {
                if ((curObj["PowerManagementSupported"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["PowerManagementSupported"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPowerStateNull {
            get {
                if ((curObj["PowerState"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"Indicates the current power state of the ComputerSystem and its associated OperatingSystem. Regarding the Power Save states, these are defined as follows: Value 4 (""Power Save - Unknown"") indicates that the System is known to be in a power save mode, but its exact status in this mode is unknown; 2 (""Power Save - Low Power Mode"") indicates that the System is in a power save state but still functioning, and may exhibit degraded performance; 3 (""Power Save - Standby"") describes that the System is not functioning but could be brought to full power 'quickly'; and value 7 (""Power Save - Warning"") indicates that the ComputerSystem is in a warning state, though also in a power save mode.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public PowerStateValues PowerState {
            get {
                if ((curObj["PowerState"] == null)) {
                    return ((PowerStateValues)(System.Convert.ToInt32(8)));
                }
                return ((PowerStateValues)(System.Convert.ToInt32(curObj["PowerState"])));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("A string that provides information on how the primary system owner can be reached" +
            " (e.g. phone number, email address, ...).")]
        public string PrimaryOwnerContact {
            get {
                return ((string)(curObj["PrimaryOwnerContact"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The name of the primary system owner.")]
        public string PrimaryOwnerName {
            get {
                return ((string)(curObj["PrimaryOwnerName"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Private properties of the Node.")]
        public System.Management.ManagementBaseObject PrivateProperties {
            get {
                return ((System.Management.ManagementBaseObject)(curObj["PrivateProperties"]));
            }
            set {
                curObj["PrivateProperties"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsResetCapabilityNull {
            get {
                if ((curObj["ResetCapability"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"If enabled (value = 4), the UnitaryComputerSystem can be reset via hardware (e.g. the power and reset buttons). If disabled (value = 3), hardware reset is not allowed. In addition to Enabled and Disabled, other Values for the property are also defined - ""Not Implemented"" (5), ""Other"" (1) and ""Unknown"" (2).")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ResetCapabilityValues ResetCapability {
            get {
                if ((curObj["ResetCapability"] == null)) {
                    return ((ResetCapabilityValues)(System.Convert.ToInt32(5)));
                }
                return ((ResetCapabilityValues)(System.Convert.ToInt32(curObj["ResetCapability"])));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"An array (bag) of strings that specify the roles this System plays in the IT-environment. Subclasses of System may override this property to define explicit Roles values. Alternately, a Working Group may describe the heuristics, conventions and guidelines for specifying Roles. For example, for an instance of a networking system, the Roles property might contain the string, 'Switch' or 'Bridge'.")]
        public string[] Roles {
            get {
                return ((string[])(curObj["Roles"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsStateNull {
            get {
                if ((curObj["State"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The current state of a node.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public StateValues State {
            get {
                if ((curObj["State"] == null)) {
                    return ((StateValues)(System.Convert.ToInt32(4)));
                }
                return ((StateValues)(System.Convert.ToInt32(curObj["State"])));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"A string indicating the current status of the object. Various operational and non-operational statuses are defined. Operational statuses are ""OK"", ""Degraded"", ""Stressed"" and ""Pred Fail"". ""Stressed"" indicates that the Element is functioning, but needs attention. Examples of ""Stressed"" states are overload, overheated, etc. The condition ""Pred Fail"" (failure predicted) indicates that an Element is functioning properly but predicting a failure in the near future. An example is a SMART-enabled hard drive. Non-operational statuses can also be specified. These are ""Error"", ""NonRecover"", ""Starting"", ""Stopping"" and ""Service"". ""NonRecover"" indicates that a non-recoverable error has occurred. ""Service"" describes an Element being configured, maintained or cleaned, or otherwise administered. This status could apply during mirror-resilvering of a disk, reload of a user permissions list, or other administrative task. Not all such work is on-line, yet the Element is neither ""OK"" nor in one of the other states.")]
        public string Status {
            get {
                return ((string)(curObj["Status"]));
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
        
        private bool ShouldSerializeBuildNumber() {
            if ((this.IsBuildNumberNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeCharacteristics() {
            if ((this.IsCharacteristicsNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetDescription() {
            curObj["Description"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeDynamicWeight() {
            if ((this.IsDynamicWeightNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeFlags() {
            if ((this.IsFlagsNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetFlags() {
            curObj["Flags"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
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
        
        private bool ShouldSerializeInstallDate() {
            if ((this.IsInstallDateNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeMajorVersion() {
            if ((this.IsMajorVersionNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeMinorVersion() {
            if ((this.IsMinorVersionNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeNeedsPreventQuorum() {
            if ((this.IsNeedsPreventQuorumNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeNodeDrainStatus() {
            if ((this.IsNodeDrainStatusNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeNodeHighestVersion() {
            if ((this.IsNodeHighestVersionNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeNodeLowestVersion() {
            if ((this.IsNodeLowestVersionNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeNodeWeight() {
            if ((this.IsNodeWeightNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetNodeWeight() {
            curObj["NodeWeight"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializePowerManagementSupported() {
            if ((this.IsPowerManagementSupportedNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializePowerState() {
            if ((this.IsPowerStateNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetPrivateProperties() {
            curObj["PrivateProperties"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeResetCapability() {
            if ((this.IsResetCapabilityNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeState() {
            if ((this.IsStateNull == false)) {
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
        
        private static string ConstructPath(string keyName) {
            string strPath = "ROOT\\MSCluster:MSCluster_Node";
            strPath = string.Concat(strPath, string.Concat(".Name=", string.Concat("\"", string.Concat(keyName, "\""))));
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
        public static NodeCollection GetInstances() {
            return GetInstances(null, null, null);
        }
        
        public static NodeCollection GetInstances(string condition) {
            return GetInstances(null, condition, null);
        }
        
        public static NodeCollection GetInstances(string[] selectedProperties) {
            return GetInstances(null, null, selectedProperties);
        }
        
        public static NodeCollection GetInstances(string condition, string[] selectedProperties) {
            return GetInstances(null, condition, selectedProperties);
        }
        
        public static NodeCollection GetInstances(System.Management.ManagementScope mgmtScope, System.Management.EnumerationOptions enumOptions) {
            if ((mgmtScope == null)) {
                if ((statMgmtScope == null)) {
                    mgmtScope = new System.Management.ManagementScope();
                    mgmtScope.Path.NamespacePath = "root\\MSCluster";
                }
                else {
                    mgmtScope = statMgmtScope;
                }
            }
            System.Management.ManagementPath pathObj = new System.Management.ManagementPath();
            pathObj.ClassName = "MSCluster_Node";
            pathObj.NamespacePath = "root\\MSCluster";
            System.Management.ManagementClass clsObject = new System.Management.ManagementClass(mgmtScope, pathObj, null);
            if ((enumOptions == null)) {
                enumOptions = new System.Management.EnumerationOptions();
                enumOptions.EnsureLocatable = true;
            }
            return new NodeCollection(clsObject.GetInstances(enumOptions));
        }
        
        public static NodeCollection GetInstances(System.Management.ManagementScope mgmtScope, string condition) {
            return GetInstances(mgmtScope, condition, null);
        }
        
        public static NodeCollection GetInstances(System.Management.ManagementScope mgmtScope, string[] selectedProperties) {
            return GetInstances(mgmtScope, null, selectedProperties);
        }
        
        public static NodeCollection GetInstances(System.Management.ManagementScope mgmtScope, string condition, string[] selectedProperties) {
            if ((mgmtScope == null)) {
                if ((statMgmtScope == null)) {
                    mgmtScope = new System.Management.ManagementScope();
                    mgmtScope.Path.NamespacePath = "root\\MSCluster";
                }
                else {
                    mgmtScope = statMgmtScope;
                }
            }
            System.Management.ManagementObjectSearcher ObjectSearcher = new System.Management.ManagementObjectSearcher(mgmtScope, new SelectQuery("MSCluster_Node", condition, selectedProperties));
            System.Management.EnumerationOptions enumOptions = new System.Management.EnumerationOptions();
            enumOptions.EnsureLocatable = true;
            ObjectSearcher.Options = enumOptions;
            return new NodeCollection(ObjectSearcher.Get());
        }
        
        [Browsable(true)]
        public static Node CreateInstance() {
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
            return new Node(tmpMgmtClass.CreateInstance());
        }
        
        [Browsable(true)]
        public void Delete() {
            PrivateLateBoundObject.Delete();
        }
        
        public void ExecuteNodeControl(int ControlCode, byte[] InputBuffer, out byte[] OutputBuffer, out int OutputBufferSize) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("ExecuteNodeControl");
                inParams["ControlCode"] = ((int)(ControlCode));
                inParams["InputBuffer"] = ((byte[])(InputBuffer));
                System.Management.ManagementBaseObject outParams = PrivateLateBoundObject.InvokeMethod("ExecuteNodeControl", inParams, null);
                OutputBuffer = ((byte[])(outParams.Properties["OutputBuffer"].Value));
                OutputBufferSize = System.Convert.ToInt32(outParams.Properties["OutputBufferSize"].Value);
            }
            else {
                OutputBuffer = null;
                OutputBufferSize = System.Convert.ToInt32(0);
            }
        }
        
        public void Pause(uint DrainType, string TargetNode) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("Pause");
                inParams["DrainType"] = ((uint)(DrainType));
                inParams["TargetNode"] = ((string)(TargetNode));
                PrivateLateBoundObject.InvokeMethod("Pause", inParams, null);
            }
        }
        
        public void Resume(uint FailbackType) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("Resume");
                inParams["FailbackType"] = ((uint)(FailbackType));
                PrivateLateBoundObject.InvokeMethod("Resume", inParams, null);
            }
        }
        
        public uint SetPowerState(ushort PowerState, System.DateTime Time) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("SetPowerState");
                inParams["PowerState"] = ((ushort)(PowerState));
                inParams["Time"] = ToDmtfDateTime(((System.DateTime)(Time)));
                System.Management.ManagementBaseObject outParams = PrivateLateBoundObject.InvokeMethod("SetPowerState", inParams, null);
                return System.Convert.ToUInt32(outParams.Properties["ReturnValue"].Value);
            }
            else {
                return System.Convert.ToUInt32(0);
            }
        }
        
        public bool WillEvictLoseQuorum() {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                System.Management.ManagementBaseObject outParams = PrivateLateBoundObject.InvokeMethod("WillEvictLoseQuorum", inParams, null);
                return System.Convert.ToBoolean(outParams.Properties["ReturnValue"].Value);
            }
            else {
                return System.Convert.ToBoolean(0);
            }
        }
        
        public bool WillOfflineLoseQuorum() {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                System.Management.ManagementBaseObject outParams = PrivateLateBoundObject.InvokeMethod("WillOfflineLoseQuorum", inParams, null);
                return System.Convert.ToBoolean(outParams.Properties["ReturnValue"].Value);
            }
            else {
                return System.Convert.ToBoolean(0);
            }
        }
        
        public enum DedicatedValues {
            
            Not_Dedicated = 0,
            
            Unknown0 = 1,
            
            Other0 = 2,
            
            Storage = 3,
            
            Router = 4,
            
            Switch = 5,
            
            Layer_3_Switch = 6,
            
            Central_Office_Switch = 7,
            
            Hub = 8,
            
            Access_Server = 9,
            
            Firewall = 10,
            
            Print = 11,
            
            I_O = 12,
            
            Web_Caching = 13,
            
            NULL_ENUM_VALUE = 14,
        }
        
        public enum NodeDrainStatusValues {
            
            Not_Initiated = 0,
            
            In_Progress = 1,
            
            Completed = 2,
            
            Failed = 3,
            
            NULL_ENUM_VALUE = 4,
        }
        
        public enum PowerManagementCapabilitiesValues {
            
            Unknown0 = 0,
            
            Not_Supported = 1,
            
            Disabled = 2,
            
            Enabled = 3,
            
            Power_Saving_Modes_Entered_Automatically = 4,
            
            Power_State_Settable = 5,
            
            Power_Cycling_Supported = 6,
            
            Timed_Power_On_Supported = 7,
            
            NULL_ENUM_VALUE = 8,
        }
        
        public enum PowerStateValues {
            
            Unknown0 = 0,
            
            Full_Power = 1,
            
            Power_Save_Low_Power_Mode = 2,
            
            Power_Save_Standby = 3,
            
            Power_Save_Unknown = 4,
            
            Power_Cycle = 5,
            
            Power_Off = 6,
            
            Power_Save_Warning = 7,
            
            NULL_ENUM_VALUE = 8,
        }
        
        public enum ResetCapabilityValues {
            
            Other0 = 0,
            
            Unknown0 = 1,
            
            Disabled = 2,
            
            Enabled = 3,
            
            Not_Implemented = 4,
            
            NULL_ENUM_VALUE = 5,
        }
        
        public enum StateValues {
            
            Unknown0 = -1,
            
            Up = 0,
            
            Down = 1,
            
            Paused = 2,
            
            Joining = 3,
            
            NULL_ENUM_VALUE = 4,
        }
        
        // Enumerator implementation for enumerating instances of the class.
        public class NodeCollection : object, ICollection {
            
            private ManagementObjectCollection privColObj;
            
            public NodeCollection(ManagementObjectCollection objCollection) {
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
                    array.SetValue(new Node(((System.Management.ManagementObject)(array.GetValue(nCtr)))), nCtr);
                }
            }
            
            public virtual System.Collections.IEnumerator GetEnumerator() {
                return new NodeEnumerator(privColObj.GetEnumerator());
            }
            
            public class NodeEnumerator : object, System.Collections.IEnumerator {
                
                private ManagementObjectCollection.ManagementObjectEnumerator privObjEnum;
                
                public NodeEnumerator(ManagementObjectCollection.ManagementObjectEnumerator objEnum) {
                    privObjEnum = objEnum;
                }
                
                public virtual object Current {
                    get {
                        return new Node(((System.Management.ManagementObject)(privObjEnum.Current)));
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
