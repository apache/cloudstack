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
    // An Early Bound class generated for the WMI class.MSCluster_Resource
    public class Resource : System.ComponentModel.Component {
        
        // Private property to hold the WMI namespace in which the class resides.
        private static string CreatedWmiNamespace = "ROOT\\MSCluster";
        
        // Private property to hold the name of WMI class which created this class.
        private static string CreatedClassName = "MSCluster_Resource";
        
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
        public Resource() {
            this.InitializeObject(null, null, null);
        }
        
        public Resource(string keyName) {
            this.InitializeObject(null, new System.Management.ManagementPath(Resource.ConstructPath(keyName)), null);
        }
        
        public Resource(System.Management.ManagementScope mgmtScope, string keyName) {
            this.InitializeObject(((System.Management.ManagementScope)(mgmtScope)), new System.Management.ManagementPath(Resource.ConstructPath(keyName)), null);
        }
        
        public Resource(System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(null, path, getOptions);
        }
        
        public Resource(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path) {
            this.InitializeObject(mgmtScope, path, null);
        }
        
        public Resource(System.Management.ManagementPath path) {
            this.InitializeObject(null, path, null);
        }
        
        public Resource(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(mgmtScope, path, getOptions);
        }
        
        public Resource(System.Management.ManagementObject theObject) {
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
        
        public Resource(System.Management.ManagementBaseObject theObject) {
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
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint Characteristics {
            get {
                if ((curObj["Characteristics"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["Characteristics"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsCoreResourceNull {
            get {
                if ((curObj["CoreResource"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Indicates that the resource is essential to the cluster and cannot be deleted.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool CoreResource {
            get {
                if ((curObj["CoreResource"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["CoreResource"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("List of crypto checkpoints for this resource.")]
        public string[] CryptoCheckpoints {
            get {
                return ((string[])(curObj["CryptoCheckpoints"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsDeadlockTimeoutNull {
            get {
                if ((curObj["DeadlockTimeout"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The DeadLockTimeout property controls how long we wait, in milliseconds, to decla" +
            "re a deadlock in any call into a resource.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint DeadlockTimeout {
            get {
                if ((curObj["DeadlockTimeout"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["DeadlockTimeout"]));
            }
            set {
                curObj["DeadlockTimeout"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsDeleteRequiresAllNodesNull {
            get {
                if ((curObj["DeleteRequiresAllNodes"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The resource cannot be deleted unless all nodes are active.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool DeleteRequiresAllNodes {
            get {
                if ((curObj["DeleteRequiresAllNodes"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["DeleteRequiresAllNodes"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Description property provides comments about the Resource.")]
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
        public bool IsEmbeddedFailureActionNull {
            get {
                if ((curObj["EmbeddedFailureAction"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The EmbeddedFailureAction property specifies the action to be taken by the Cluste" +
            "r Service if the resource experiences an embedded failure.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint EmbeddedFailureAction {
            get {
                if ((curObj["EmbeddedFailureAction"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["EmbeddedFailureAction"]));
            }
            set {
                curObj["EmbeddedFailureAction"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
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
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint Flags {
            get {
                if ((curObj["Flags"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["Flags"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Id property provides the Id of the Resource.")]
        public string Id {
            get {
                return ((string)(curObj["Id"]));
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
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsIsAlivePollIntervalNull {
            get {
                if ((curObj["IsAlivePollInterval"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The IsAlivePollInterval property provides the recommended interval in milliseconds at which the Cluster Service should poll the resource to determine if it is operational. If it is sets to 0xFFFFFFFF, the Cluster Service uses the IsAlivePollInterval property for the resource type associated with the resource.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint IsAlivePollInterval {
            get {
                if ((curObj["IsAlivePollInterval"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["IsAlivePollInterval"]));
            }
            set {
                curObj["IsAlivePollInterval"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsIsClusterSharedVolumeNull {
            get {
                if ((curObj["IsClusterSharedVolume"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Specifies if the resource is a cluster shared volume resource.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool IsClusterSharedVolume {
            get {
                if ((curObj["IsClusterSharedVolume"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["IsClusterSharedVolume"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsLastOperationStatusCodeNull {
            get {
                if ((curObj["LastOperationStatusCode"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The last Application Specific Error Code returned by the Resource DLL during a Cl" +
            "uster Operation.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong LastOperationStatusCode {
            get {
                if ((curObj["LastOperationStatusCode"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["LastOperationStatusCode"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsLocalQuorumCapableNull {
            get {
                if ((curObj["LocalQuorumCapable"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("This property is obsolete.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool LocalQuorumCapable {
            get {
                if ((curObj["LocalQuorumCapable"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["LocalQuorumCapable"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsLooksAlivePollIntervalNull {
            get {
                if ((curObj["LooksAlivePollInterval"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The LooksAlivePollInterval property provides the recommended interval in milliseconds at which the Cluster Service should poll the resource to determine if it appears operational. If it is sets to 0xFFFFFFFF, the Cluster Service uses the LooksAlivePollInterval property for the resource type associated with the resource.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint LooksAlivePollInterval {
            get {
                if ((curObj["LooksAlivePollInterval"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["LooksAlivePollInterval"]));
            }
            set {
                curObj["LooksAlivePollInterval"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsMonitorProcessIdNull {
            get {
                if ((curObj["MonitorProcessId"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The MonitorProcessId property provides the process ID of the Resource Host Servic" +
            "e that is currently hosting the resource.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint MonitorProcessId {
            get {
                if ((curObj["MonitorProcessId"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["MonitorProcessId"]));
            }
            set {
                curObj["MonitorProcessId"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Name property provides the name of the Resource.")]
        public string Name {
            get {
                return ((string)(curObj["Name"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The resource group which the resource belongs to.")]
        public string OwnerGroup {
            get {
                return ((string)(curObj["OwnerGroup"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The node hosting the resource.")]
        public string OwnerNode {
            get {
                return ((string)(curObj["OwnerNode"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPendingTimeoutNull {
            get {
                if ((curObj["PendingTimeout"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("If a resource cannot be brought online or taken offline in the number of millisec" +
            "onds specified by the PendingTimeout property, the resource is forcibly terminat" +
            "ed.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint PendingTimeout {
            get {
                if ((curObj["PendingTimeout"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["PendingTimeout"]));
            }
            set {
                curObj["PendingTimeout"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPersistentStateNull {
            get {
                if ((curObj["PersistentState"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The PersistentState property specifies whether the resource should be brought onl" +
            "ine or left offline when the Cluster Service is started.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool PersistentState {
            get {
                if ((curObj["PersistentState"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["PersistentState"]));
            }
            set {
                curObj["PersistentState"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Private properties of the resource.")]
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
        public bool IsQuorumCapableNull {
            get {
                if ((curObj["QuorumCapable"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The resource can be selected as the quorum resource for the cluster.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool QuorumCapable {
            get {
                if ((curObj["QuorumCapable"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["QuorumCapable"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("List of registry checkpoints for this resource.")]
        public string[] RegistryCheckpoints {
            get {
                return ((string[])(curObj["RegistryCheckpoints"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The resource classes which the resource must depend on.")]
        public RequiredDependencyClassesValues[] RequiredDependencyClasses {
            get {
                System.Array arrEnumVals = ((System.Array)(curObj["RequiredDependencyClasses"]));
                RequiredDependencyClassesValues[] enumToRet = new RequiredDependencyClassesValues[arrEnumVals.Length];
                int counter = 0;
                for (counter = 0; (counter < arrEnumVals.Length); counter = (counter + 1)) {
                    enumToRet[counter] = ((RequiredDependencyClassesValues)(System.Convert.ToInt32(arrEnumVals.GetValue(counter))));
                }
                return enumToRet;
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The resource types which the resource must depend on.")]
        public string[] RequiredDependencyTypes {
            get {
                return ((string[])(curObj["RequiredDependencyTypes"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsResourceClassNull {
            get {
                if ((curObj["ResourceClass"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The resource class.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ResourceClassValues ResourceClass {
            get {
                if ((curObj["ResourceClass"] == null)) {
                    return ((ResourceClassValues)(System.Convert.ToInt32(32769)));
                }
                return ((ResourceClassValues)(System.Convert.ToInt32(curObj["ResourceClass"])));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsResourceSpecificData1Null {
            get {
                if ((curObj["ResourceSpecificData1"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Resource Specific Information.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong ResourceSpecificData1 {
            get {
                if ((curObj["ResourceSpecificData1"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["ResourceSpecificData1"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsResourceSpecificData2Null {
            get {
                if ((curObj["ResourceSpecificData2"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Resource Specific Information.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong ResourceSpecificData2 {
            get {
                if ((curObj["ResourceSpecificData2"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["ResourceSpecificData2"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("A resource specific status message that compliments the current resource state.")]
        public string ResourceSpecificStatus {
            get {
                return ((string)(curObj["ResourceSpecificStatus"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsRestartActionNull {
            get {
                if ((curObj["RestartAction"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The RestartAction property describes the action to be taken by the Cluster Servic" +
            "e if the resource fails.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint RestartAction {
            get {
                if ((curObj["RestartAction"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["RestartAction"]));
            }
            set {
                curObj["RestartAction"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsRestartDelayNull {
            get {
                if ((curObj["RestartDelay"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The time delay before a failed resource is restarted.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint RestartDelay {
            get {
                if ((curObj["RestartDelay"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["RestartDelay"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsRestartPeriodNull {
            get {
                if ((curObj["RestartPeriod"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The RestartPeriod property defines an interval of time, in milliseconds, during w" +
            "hich a specified number of restart attempts can be made on a nonresponsive resou" +
            "rce.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint RestartPeriod {
            get {
                if ((curObj["RestartPeriod"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["RestartPeriod"]));
            }
            set {
                curObj["RestartPeriod"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsRestartThresholdNull {
            get {
                if ((curObj["RestartThreshold"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The RestartThreshold property specifies the maximum number of restart attempts th" +
            "at can be made on a resource within an interval defined by the RestartPeriod pro" +
            "perty before the Cluster Service initiates the action specified by the RestartAc" +
            "tion property.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint RestartThreshold {
            get {
                if ((curObj["RestartThreshold"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["RestartThreshold"]));
            }
            set {
                curObj["RestartThreshold"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsRetryPeriodOnFailureNull {
            get {
                if ((curObj["RetryPeriodOnFailure"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The RetryPeriodOnFailure property specifies the interval of time (in milliseconds" +
            ") that a resource should remain in a failed state before the Cluster service att" +
            "empts to restart it.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint RetryPeriodOnFailure {
            get {
                if ((curObj["RetryPeriodOnFailure"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["RetryPeriodOnFailure"]));
            }
            set {
                curObj["RetryPeriodOnFailure"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsSeparateMonitorNull {
            get {
                if ((curObj["SeparateMonitor"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The SeparateMonitor property indicates whether the resource requires its own Reso" +
            "urce Monitor.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool SeparateMonitor {
            get {
                if ((curObj["SeparateMonitor"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["SeparateMonitor"]));
            }
            set {
                curObj["SeparateMonitor"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
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
        [Description("The current state of the resource.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public StateValues State {
            get {
                if ((curObj["State"] == null)) {
                    return ((StateValues)(System.Convert.ToInt32(131)));
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
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsStatusInformationNull {
            get {
                if ((curObj["StatusInformation"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("List of various Status Flags for the Resource. These are set by Cluster Service.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong StatusInformation {
            get {
                if ((curObj["StatusInformation"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["StatusInformation"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsSubclassNull {
            get {
                if ((curObj["Subclass"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The resource sub class.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint Subclass {
            get {
                if ((curObj["Subclass"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["Subclass"]));
            }
            set {
                curObj["Subclass"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Type property specifies the name for the resource\'s type.")]
        public string Type {
            get {
                return ((string)(curObj["Type"]));
            }
            set {
                curObj["Type"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
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
        
        private bool ShouldSerializeCharacteristics() {
            if ((this.IsCharacteristicsNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeCoreResource() {
            if ((this.IsCoreResourceNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeDeadlockTimeout() {
            if ((this.IsDeadlockTimeoutNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetDeadlockTimeout() {
            curObj["DeadlockTimeout"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeDeleteRequiresAllNodes() {
            if ((this.IsDeleteRequiresAllNodesNull == false)) {
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
        
        private bool ShouldSerializeEmbeddedFailureAction() {
            if ((this.IsEmbeddedFailureActionNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetEmbeddedFailureAction() {
            curObj["EmbeddedFailureAction"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeFlags() {
            if ((this.IsFlagsNull == false)) {
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
        
        private bool ShouldSerializeInstallDate() {
            if ((this.IsInstallDateNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeIsAlivePollInterval() {
            if ((this.IsIsAlivePollIntervalNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetIsAlivePollInterval() {
            curObj["IsAlivePollInterval"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeIsClusterSharedVolume() {
            if ((this.IsIsClusterSharedVolumeNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeLastOperationStatusCode() {
            if ((this.IsLastOperationStatusCodeNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeLocalQuorumCapable() {
            if ((this.IsLocalQuorumCapableNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeLooksAlivePollInterval() {
            if ((this.IsLooksAlivePollIntervalNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetLooksAlivePollInterval() {
            curObj["LooksAlivePollInterval"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeMonitorProcessId() {
            if ((this.IsMonitorProcessIdNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetMonitorProcessId() {
            curObj["MonitorProcessId"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializePendingTimeout() {
            if ((this.IsPendingTimeoutNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetPendingTimeout() {
            curObj["PendingTimeout"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializePersistentState() {
            if ((this.IsPersistentStateNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetPersistentState() {
            curObj["PersistentState"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private void ResetPrivateProperties() {
            curObj["PrivateProperties"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeQuorumCapable() {
            if ((this.IsQuorumCapableNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeResourceClass() {
            if ((this.IsResourceClassNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeResourceSpecificData1() {
            if ((this.IsResourceSpecificData1Null == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeResourceSpecificData2() {
            if ((this.IsResourceSpecificData2Null == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeRestartAction() {
            if ((this.IsRestartActionNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetRestartAction() {
            curObj["RestartAction"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeRestartDelay() {
            if ((this.IsRestartDelayNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeRestartPeriod() {
            if ((this.IsRestartPeriodNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetRestartPeriod() {
            curObj["RestartPeriod"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeRestartThreshold() {
            if ((this.IsRestartThresholdNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetRestartThreshold() {
            curObj["RestartThreshold"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeRetryPeriodOnFailure() {
            if ((this.IsRetryPeriodOnFailureNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetRetryPeriodOnFailure() {
            curObj["RetryPeriodOnFailure"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeSeparateMonitor() {
            if ((this.IsSeparateMonitorNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetSeparateMonitor() {
            curObj["SeparateMonitor"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeState() {
            if ((this.IsStateNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeStatusInformation() {
            if ((this.IsStatusInformationNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeSubclass() {
            if ((this.IsSubclassNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetSubclass() {
            curObj["Subclass"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private void ResetType() {
            curObj["Type"] = null;
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
        
        private static string ConstructPath(string keyName) {
            string strPath = "ROOT\\MSCluster:MSCluster_Resource";
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
        public static ResourceCollection GetInstances() {
            return GetInstances(null, null, null);
        }
        
        public static ResourceCollection GetInstances(string condition) {
            return GetInstances(null, condition, null);
        }
        
        public static ResourceCollection GetInstances(string[] selectedProperties) {
            return GetInstances(null, null, selectedProperties);
        }
        
        public static ResourceCollection GetInstances(string condition, string[] selectedProperties) {
            return GetInstances(null, condition, selectedProperties);
        }
        
        public static ResourceCollection GetInstances(System.Management.ManagementScope mgmtScope, System.Management.EnumerationOptions enumOptions) {
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
            pathObj.ClassName = "MSCluster_Resource";
            pathObj.NamespacePath = "root\\MSCluster";
            System.Management.ManagementClass clsObject = new System.Management.ManagementClass(mgmtScope, pathObj, null);
            if ((enumOptions == null)) {
                enumOptions = new System.Management.EnumerationOptions();
                enumOptions.EnsureLocatable = true;
            }
            return new ResourceCollection(clsObject.GetInstances(enumOptions));
        }
        
        public static ResourceCollection GetInstances(System.Management.ManagementScope mgmtScope, string condition) {
            return GetInstances(mgmtScope, condition, null);
        }
        
        public static ResourceCollection GetInstances(System.Management.ManagementScope mgmtScope, string[] selectedProperties) {
            return GetInstances(mgmtScope, null, selectedProperties);
        }
        
        public static ResourceCollection GetInstances(System.Management.ManagementScope mgmtScope, string condition, string[] selectedProperties) {
            if ((mgmtScope == null)) {
                if ((statMgmtScope == null)) {
                    mgmtScope = new System.Management.ManagementScope();
                    mgmtScope.Path.NamespacePath = "root\\MSCluster";
                }
                else {
                    mgmtScope = statMgmtScope;
                }
            }
            System.Management.ManagementObjectSearcher ObjectSearcher = new System.Management.ManagementObjectSearcher(mgmtScope, new SelectQuery("MSCluster_Resource", condition, selectedProperties));
            System.Management.EnumerationOptions enumOptions = new System.Management.EnumerationOptions();
            enumOptions.EnsureLocatable = true;
            ObjectSearcher.Options = enumOptions;
            return new ResourceCollection(ObjectSearcher.Get());
        }
        
        [Browsable(true)]
        public static Resource CreateInstance() {
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
            return new Resource(tmpMgmtClass.CreateInstance());
        }
        
        [Browsable(true)]
        public void Delete() {
            PrivateLateBoundObject.Delete();
        }
        
        public void AddCryptoCheckpoint(string CheckpointName) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("AddCryptoCheckpoint");
                inParams["CheckpointName"] = ((string)(CheckpointName));
                PrivateLateBoundObject.InvokeMethod("AddCryptoCheckpoint", inParams, null);
            }
        }
        
        public void AddDependency(string Resource) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("AddDependency");
                inParams["Resource"] = ((string)(Resource));
                PrivateLateBoundObject.InvokeMethod("AddDependency", inParams, null);
            }
        }
        
        public void AddPossibleOwner(string NodeName) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("AddPossibleOwner");
                inParams["NodeName"] = ((string)(NodeName));
                PrivateLateBoundObject.InvokeMethod("AddPossibleOwner", inParams, null);
            }
        }
        
        public void AddRegistryCheckpoint(string CheckpointName) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("AddRegistryCheckpoint");
                inParams["CheckpointName"] = ((string)(CheckpointName));
                PrivateLateBoundObject.InvokeMethod("AddRegistryCheckpoint", inParams, null);
            }
        }
        
        public void AttachStorageDevice(System.Management.ManagementPath StorageDevice) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("AttachStorageDevice");
                inParams["StorageDevice"] = ((System.Management.ManagementPath)(StorageDevice)).Path;
                PrivateLateBoundObject.InvokeMethod("AttachStorageDevice", inParams, null);
            }
        }
        
        public void BringOnline(uint TimeOut) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("BringOnline");
                inParams["TimeOut"] = ((uint)(TimeOut));
                PrivateLateBoundObject.InvokeMethod("BringOnline", inParams, null);
            }
        }
        
        public static void CreateResource(string Group, ref string Id, string ResourceName, string ResourceType, bool SeparateMonitor) {
            bool IsMethodStatic = true;
            if ((IsMethodStatic == true)) {
                System.Management.ManagementBaseObject inParams = null;
                System.Management.ManagementPath mgmtPath = new System.Management.ManagementPath(CreatedClassName);
                System.Management.ManagementClass classObj = new System.Management.ManagementClass(statMgmtScope, mgmtPath, null);
                inParams = classObj.GetMethodParameters("CreateResource");
                inParams["Group"] = ((string)(Group));
                inParams["Id"] = ((string)(Id));
                inParams["ResourceName"] = ((string)(ResourceName));
                inParams["ResourceType"] = ((string)(ResourceType));
                inParams["SeparateMonitor"] = ((bool)(SeparateMonitor));
                System.Management.ManagementBaseObject outParams = classObj.InvokeMethod("CreateResource", inParams, null);
                Id = ((string)(outParams.Properties["Id"].Value));
            }
        }
        
        public void DeleteResource(uint Options) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("DeleteResource");
                inParams["Options"] = ((uint)(Options));
                PrivateLateBoundObject.InvokeMethod("DeleteResource", inParams, null);
            }
        }
        
        public void ExecuteResourceControl(int ControlCode, byte[] InputBuffer, out byte[] OutputBuffer, out int OutputBufferSize) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("ExecuteResourceControl");
                inParams["ControlCode"] = ((int)(ControlCode));
                inParams["InputBuffer"] = ((byte[])(InputBuffer));
                System.Management.ManagementBaseObject outParams = PrivateLateBoundObject.InvokeMethod("ExecuteResourceControl", inParams, null);
                OutputBuffer = ((byte[])(outParams.Properties["OutputBuffer"].Value));
                OutputBufferSize = System.Convert.ToInt32(outParams.Properties["OutputBufferSize"].Value);
            }
            else {
                OutputBuffer = null;
                OutputBufferSize = System.Convert.ToInt32(0);
            }
        }
        
        public void FailResource() {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                PrivateLateBoundObject.InvokeMethod("FailResource", inParams, null);
            }
        }
        
        public void GetDependencies(bool AsResourceIds, out string Expression) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("GetDependencies");
                inParams["AsResourceIds"] = ((bool)(AsResourceIds));
                System.Management.ManagementBaseObject outParams = PrivateLateBoundObject.InvokeMethod("GetDependencies", inParams, null);
                Expression = System.Convert.ToString(outParams.Properties["Expression"].Value);
            }
            else {
                Expression = null;
            }
        }
        
        public void GetPossibleOwners(out string[] NodeNames) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                System.Management.ManagementBaseObject outParams = PrivateLateBoundObject.InvokeMethod("GetPossibleOwners", inParams, null);
                NodeNames = ((string[])(outParams.Properties["NodeNames"].Value));
            }
            else {
                NodeNames = null;
            }
        }
        
        public void MigrateVirtualMachine(string ConfigurationDestinationPath, string[] DestinationPaths, string[] ResourceDestinationPools, string SnapshotDestinationPath, string[] SourcePaths, string SwapFileDestinationPath) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("MigrateVirtualMachine");
                inParams["ConfigurationDestinationPath"] = ((string)(ConfigurationDestinationPath));
                inParams["DestinationPaths"] = ((string[])(DestinationPaths));
                inParams["ResourceDestinationPools"] = ((string[])(ResourceDestinationPools));
                inParams["SnapshotDestinationPath"] = ((string)(SnapshotDestinationPath));
                inParams["SourcePaths"] = ((string[])(SourcePaths));
                inParams["SwapFileDestinationPath"] = ((string)(SwapFileDestinationPath));
                PrivateLateBoundObject.InvokeMethod("MigrateVirtualMachine", inParams, null);
            }
        }
        
        public void MoveToNewGroup(string Group) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("MoveToNewGroup");
                inParams["Group"] = ((string)(Group));
                PrivateLateBoundObject.InvokeMethod("MoveToNewGroup", inParams, null);
            }
        }
        
        public void ReleaseAddress() {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                PrivateLateBoundObject.InvokeMethod("ReleaseAddress", inParams, null);
            }
        }
        
        public void RemoveCryptoCheckpoint(string CheckpointName) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("RemoveCryptoCheckpoint");
                inParams["CheckpointName"] = ((string)(CheckpointName));
                PrivateLateBoundObject.InvokeMethod("RemoveCryptoCheckpoint", inParams, null);
            }
        }
        
        public void RemoveDependency(string Resource) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("RemoveDependency");
                inParams["Resource"] = ((string)(Resource));
                PrivateLateBoundObject.InvokeMethod("RemoveDependency", inParams, null);
            }
        }
        
        public void RemovePossibleOwner(string NodeName) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("RemovePossibleOwner");
                inParams["NodeName"] = ((string)(NodeName));
                PrivateLateBoundObject.InvokeMethod("RemovePossibleOwner", inParams, null);
            }
        }
        
        public void RemoveRegistryCheckpoint(string CheckpointName) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("RemoveRegistryCheckpoint");
                inParams["CheckpointName"] = ((string)(CheckpointName));
                PrivateLateBoundObject.InvokeMethod("RemoveRegistryCheckpoint", inParams, null);
            }
        }
        
        public void Rename(string newName) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("Rename");
                inParams["newName"] = ((string)(newName));
                PrivateLateBoundObject.InvokeMethod("Rename", inParams, null);
            }
        }
        
        public void RenewAddress() {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                PrivateLateBoundObject.InvokeMethod("RenewAddress", inParams, null);
            }
        }
        
        public void SetDependencies(string Expression) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("SetDependencies");
                inParams["Expression"] = ((string)(Expression));
                PrivateLateBoundObject.InvokeMethod("SetDependencies", inParams, null);
            }
        }
        
        public void TakeOffline(uint Flags, System.Management.ManagementBaseObject Parameters, uint TimeOut) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("TakeOffline");
                inParams["Flags"] = ((uint)(Flags));
                inParams["Parameters"] = ((System.Management.ManagementBaseObject )(Parameters));
                inParams["TimeOut"] = ((uint)(TimeOut));
                PrivateLateBoundObject.InvokeMethod("TakeOffline", inParams, null);
            }
        }
        
        public void TakeOfflineParams(uint Flags, byte[] Parameters, uint TimeOut) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("TakeOfflineParams");
                inParams["Flags"] = ((uint)(Flags));
                inParams["Parameters"] = ((byte[])(Parameters));
                inParams["TimeOut"] = ((uint)(TimeOut));
                PrivateLateBoundObject.InvokeMethod("TakeOfflineParams", inParams, null);
            }
        }
        
        public void UpdateVirtualMachine() {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                PrivateLateBoundObject.InvokeMethod("UpdateVirtualMachine", inParams, null);
            }
        }
        
        public enum RequiredDependencyClassesValues {
            
            Unknown0 = 0,
            
            Storage = 1,
            
            Network = 2,
            
            User = 32768,
            
            NULL_ENUM_VALUE = 32769,
        }
        
        public enum ResourceClassValues {
            
            Unknown0 = 0,
            
            Storage = 1,
            
            Network = 2,
            
            User = 32768,
            
            NULL_ENUM_VALUE = 32769,
        }
        
        public enum StateValues {
            
            Unknown0 = -1,
            
            Inherited = 0,
            
            Initializing = 1,
            
            Online = 2,
            
            Offline = 3,
            
            Failed = 4,
            
            Pending = 128,
            
            Online_Pending = 129,
            
            Offline_Pending = 130,
            
            NULL_ENUM_VALUE = 131,
        }
        
        // Enumerator implementation for enumerating instances of the class.
        public class ResourceCollection : object, ICollection {
            
            private ManagementObjectCollection privColObj;
            
            public ResourceCollection(ManagementObjectCollection objCollection) {
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
                    array.SetValue(new Resource(((System.Management.ManagementObject)(array.GetValue(nCtr)))), nCtr);
                }
            }
            
            public virtual System.Collections.IEnumerator GetEnumerator() {
                return new ResourceEnumerator(privColObj.GetEnumerator());
            }
            
            public class ResourceEnumerator : object, System.Collections.IEnumerator {
                
                private ManagementObjectCollection.ManagementObjectEnumerator privObjEnum;
                
                public ResourceEnumerator(ManagementObjectCollection.ManagementObjectEnumerator objEnum) {
                    privObjEnum = objEnum;
                }
                
                public virtual object Current {
                    get {
                        return new Resource(((System.Management.ManagementObject)(privObjEnum.Current)));
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
