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
    // An Early Bound class generated for the WMI class.MSCluster_Cluster
    public class Cluster : System.ComponentModel.Component {
        
        // Private property to hold the WMI namespace in which the class resides.
        private static string CreatedWmiNamespace = "ROOT\\MSCluster";
        
        // Private property to hold the name of WMI class which created this class.
        private static string CreatedClassName = "MSCluster_Cluster";
        
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
        public Cluster() {
            this.InitializeObject(null, null, null);
        }
        
        public Cluster(string keyName) {
            this.InitializeObject(null, new System.Management.ManagementPath(Cluster.ConstructPath(keyName)), null);
        }
        
        public Cluster(System.Management.ManagementScope mgmtScope, string keyName) {
            this.InitializeObject(((System.Management.ManagementScope)(mgmtScope)), new System.Management.ManagementPath(Cluster.ConstructPath(keyName)), null);
        }
        
        public Cluster(System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(null, path, getOptions);
        }
        
        public Cluster(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path) {
            this.InitializeObject(mgmtScope, path, null);
        }
        
        public Cluster(System.Management.ManagementPath path) {
            this.InitializeObject(null, path, null);
        }
        
        public Cluster(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(mgmtScope, path, getOptions);
        }
        
        public Cluster(System.Management.ManagementObject theObject) {
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
        
        public Cluster(System.Management.ManagementBaseObject theObject) {
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
        public bool IsAddEvictDelayNull {
            get {
                if ((curObj["AddEvictDelay"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Delay addition of a node by a specified number of seconds after an evict of anoth" +
            "er node.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint AddEvictDelay {
            get {
                if ((curObj["AddEvictDelay"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["AddEvictDelay"]));
            }
            set {
                curObj["AddEvictDelay"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsAdminAccessPointNull {
            get {
                if ((curObj["AdminAccessPoint"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Specifies the type of the cluster administrative access point.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint AdminAccessPoint {
            get {
                if ((curObj["AdminAccessPoint"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["AdminAccessPoint"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("This property is obsolete.")]
        public string[] AdminExtensions {
            get {
                return ((string[])(curObj["AdminExtensions"]));
            }
            set {
                curObj["AdminExtensions"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsBackupInProgressNull {
            get {
                if ((curObj["BackupInProgress"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Indicates whether or not a backup is in progress.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint BackupInProgress {
            get {
                if ((curObj["BackupInProgress"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["BackupInProgress"]));
            }
            set {
                curObj["BackupInProgress"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsBlockCacheSizeNull {
            get {
                if ((curObj["BlockCacheSize"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Enables/disables hard enforcement of group anti-affinity classes.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public BlockCacheSizeValues BlockCacheSize {
            get {
                if ((curObj["BlockCacheSize"] == null)) {
                    return ((BlockCacheSizeValues)(System.Convert.ToInt32(2)));
                }
                return ((BlockCacheSizeValues)(System.Convert.ToInt32(curObj["BlockCacheSize"])));
            }
            set {
                if ((BlockCacheSizeValues.NULL_ENUM_VALUE == value)) {
                    curObj["BlockCacheSize"] = null;
                }
                else {
                    curObj["BlockCacheSize"] = value;
                }
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
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
        public bool IsClusSvcHangTimeoutNull {
            get {
                if ((curObj["ClusSvcHangTimeout"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Controls how long the cluster network driver waits between Cluster Service heartb" +
            "eats before it determines that Cluster Service has stopped responding.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint ClusSvcHangTimeout {
            get {
                if ((curObj["ClusSvcHangTimeout"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["ClusSvcHangTimeout"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsClusSvcRegroupOpeningTimeoutNull {
            get {
                if ((curObj["ClusSvcRegroupOpeningTimeout"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Controls how long a node will wait on other nodes in the opening stage before dec" +
            "iding that they failed.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint ClusSvcRegroupOpeningTimeout {
            get {
                if ((curObj["ClusSvcRegroupOpeningTimeout"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["ClusSvcRegroupOpeningTimeout"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsClusSvcRegroupPruningTimeoutNull {
            get {
                if ((curObj["ClusSvcRegroupPruningTimeout"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Controls how long membership leader will wait to reach full connectivity between " +
            "cluster nodes.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint ClusSvcRegroupPruningTimeout {
            get {
                if ((curObj["ClusSvcRegroupPruningTimeout"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["ClusSvcRegroupPruningTimeout"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsClusSvcRegroupStageTimeoutNull {
            get {
                if ((curObj["ClusSvcRegroupStageTimeout"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Controls how long a node will wait on other nodes in a membership stage before de" +
            "ciding that they failed.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint ClusSvcRegroupStageTimeout {
            get {
                if ((curObj["ClusSvcRegroupStageTimeout"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["ClusSvcRegroupStageTimeout"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsClusSvcRegroupTickInMillisecondsNull {
            get {
                if ((curObj["ClusSvcRegroupTickInMilliseconds"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Controls how frequently membership algorithm is sending periodic membership messa" +
            "ges.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint ClusSvcRegroupTickInMilliseconds {
            get {
                if ((curObj["ClusSvcRegroupTickInMilliseconds"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["ClusSvcRegroupTickInMilliseconds"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsClusterEnforcedAntiAffinityNull {
            get {
                if ((curObj["ClusterEnforcedAntiAffinity"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("CSV BlockCache Size in MB.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint ClusterEnforcedAntiAffinity {
            get {
                if ((curObj["ClusterEnforcedAntiAffinity"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["ClusterEnforcedAntiAffinity"]));
            }
            set {
                curObj["ClusterEnforcedAntiAffinity"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsClusterGroupWaitDelayNull {
            get {
                if ((curObj["ClusterGroupWaitDelay"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Maximum time in seconds that a group waits for its preferred node to come online " +
            "during cluster startup before coming online on a different node.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint ClusterGroupWaitDelay {
            get {
                if ((curObj["ClusterGroupWaitDelay"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["ClusterGroupWaitDelay"]));
            }
            set {
                curObj["ClusterGroupWaitDelay"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsClusterLogLevelNull {
            get {
                if ((curObj["ClusterLogLevel"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Controls the level of cluster logging.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint ClusterLogLevel {
            get {
                if ((curObj["ClusterLogLevel"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["ClusterLogLevel"]));
            }
            set {
                curObj["ClusterLogLevel"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsClusterLogSizeNull {
            get {
                if ((curObj["ClusterLogSize"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Controls the maximum size of the cluster log files on each of the nodes.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint ClusterLogSize {
            get {
                if ((curObj["ClusterLogSize"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["ClusterLogSize"]));
            }
            set {
                curObj["ClusterLogSize"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
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
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsCrossSubnetDelayNull {
            get {
                if ((curObj["CrossSubnetDelay"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Controls how long the cluster network driver waits in milliseconds between sendin" +
            "g Cluster Service heartbeats across subnets.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint CrossSubnetDelay {
            get {
                if ((curObj["CrossSubnetDelay"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["CrossSubnetDelay"]));
            }
            set {
                curObj["CrossSubnetDelay"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsCrossSubnetThresholdNull {
            get {
                if ((curObj["CrossSubnetThreshold"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Controls how many Cluster Service heartbeats can be missed across subnets before " +
            "it determines that Cluster Service has stopped responding.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint CrossSubnetThreshold {
            get {
                if ((curObj["CrossSubnetThreshold"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["CrossSubnetThreshold"]));
            }
            set {
                curObj["CrossSubnetThreshold"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsCsvBalancerNull {
            get {
                if ((curObj["CsvBalancer"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Whether or not automatic balancing for CSV is enabled.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint CsvBalancer {
            get {
                if ((curObj["CsvBalancer"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["CsvBalancer"]));
            }
            set {
                curObj["CsvBalancer"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsDatabaseReadWriteModeNull {
            get {
                if ((curObj["DatabaseReadWriteMode"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Set database read and write mode.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint DatabaseReadWriteMode {
            get {
                if ((curObj["DatabaseReadWriteMode"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["DatabaseReadWriteMode"]));
            }
            set {
                curObj["DatabaseReadWriteMode"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
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
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsDefaultNetworkRoleNull {
            get {
                if ((curObj["DefaultNetworkRole"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Specifies the role that the cluster automatically assigns to any newly discovered" +
            " or created network.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint DefaultNetworkRole {
            get {
                if ((curObj["DefaultNetworkRole"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["DefaultNetworkRole"]));
            }
            set {
                curObj["DefaultNetworkRole"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Description property stores administrative comments about the cluster.")]
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
        public bool IsDisableGroupPreferredOwnerRandomizationNull {
            get {
                if ((curObj["DisableGroupPreferredOwnerRandomization"] == null)) {
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
        public uint DisableGroupPreferredOwnerRandomization {
            get {
                if ((curObj["DisableGroupPreferredOwnerRandomization"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["DisableGroupPreferredOwnerRandomization"]));
            }
            set {
                curObj["DisableGroupPreferredOwnerRandomization"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsDrainOnShutdownNull {
            get {
                if ((curObj["DrainOnShutdown"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Drain the node when cluster service is being stopped.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint DrainOnShutdown {
            get {
                if ((curObj["DrainOnShutdown"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["DrainOnShutdown"]));
            }
            set {
                curObj["DrainOnShutdown"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsDynamicQuorumEnabledNull {
            get {
                if ((curObj["DynamicQuorumEnabled"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Allows cluster service to adjust node weights as needed to increase availability." +
            "")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint DynamicQuorumEnabled {
            get {
                if ((curObj["DynamicQuorumEnabled"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["DynamicQuorumEnabled"]));
            }
            set {
                curObj["DynamicQuorumEnabled"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsEnableSharedVolumesNull {
            get {
                if ((curObj["EnableSharedVolumes"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Enables/disables the cluster shared volumes feature of this cluster.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public EnableSharedVolumesValues EnableSharedVolumes {
            get {
                if ((curObj["EnableSharedVolumes"] == null)) {
                    return ((EnableSharedVolumesValues)(System.Convert.ToInt32(2)));
                }
                return ((EnableSharedVolumesValues)(System.Convert.ToInt32(curObj["EnableSharedVolumes"])));
            }
            set {
                if ((EnableSharedVolumesValues.NULL_ENUM_VALUE == value)) {
                    curObj["EnableSharedVolumes"] = null;
                }
                else {
                    curObj["EnableSharedVolumes"] = value;
                }
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsFixQuorumNull {
            get {
                if ((curObj["FixQuorum"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Specifies if the cluster is in a fix quorum state.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint FixQuorum {
            get {
                if ((curObj["FixQuorum"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["FixQuorum"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The FQDN of the cluster.")]
        public string Fqdn {
            get {
                return ((string)(curObj["Fqdn"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("This property is obsolete.")]
        public string[] GroupAdminExtensions {
            get {
                return ((string[])(curObj["GroupAdminExtensions"]));
            }
            set {
                curObj["GroupAdminExtensions"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsHangRecoveryActionNull {
            get {
                if ((curObj["HangRecoveryAction"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Controls the action to take if the user-mode processes have stopped responding.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint HangRecoveryAction {
            get {
                if ((curObj["HangRecoveryAction"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["HangRecoveryAction"]));
            }
            set {
                curObj["HangRecoveryAction"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
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
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsIgnorePersistentStateOnStartupNull {
            get {
                if ((curObj["IgnorePersistentStateOnStartup"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Specifies if the cluster will ignore group persistent state on startup.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint IgnorePersistentStateOnStartup {
            get {
                if ((curObj["IgnorePersistentStateOnStartup"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["IgnorePersistentStateOnStartup"]));
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
        public bool IsLogResourceControlsNull {
            get {
                if ((curObj["LogResourceControls"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Controls the logging of resource controls.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint LogResourceControls {
            get {
                if ((curObj["LogResourceControls"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["LogResourceControls"]));
            }
            set {
                curObj["LogResourceControls"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsLowerQuorumPriorityNodeIdNull {
            get {
                if ((curObj["LowerQuorumPriorityNodeId"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("NodeId for the node which has lower quorum priority.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint LowerQuorumPriorityNodeId {
            get {
                if ((curObj["LowerQuorumPriorityNodeId"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["LowerQuorumPriorityNodeId"]));
            }
            set {
                curObj["LowerQuorumPriorityNodeId"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("This property is obsolete.")]
        public string MaintenanceFile {
            get {
                return ((string)(curObj["MaintenanceFile"]));
            }
            set {
                curObj["MaintenanceFile"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsMaxNumberOfNodesNull {
            get {
                if ((curObj["MaxNumberOfNodes"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Indicates the maximum number of nodes that may participate in the Cluster.  If un" +
            "limited, enter 0.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint MaxNumberOfNodes {
            get {
                if ((curObj["MaxNumberOfNodes"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["MaxNumberOfNodes"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsMessageBufferLengthNull {
            get {
                if ((curObj["MessageBufferLength"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Determines the maximum unacknowledged message count for GEM.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint MessageBufferLength {
            get {
                if ((curObj["MessageBufferLength"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["MessageBufferLength"]));
            }
            set {
                curObj["MessageBufferLength"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsMinimumNeverPreemptPriorityNull {
            get {
                if ((curObj["MinimumNeverPreemptPriority"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Groups having priority below this one can be preempted.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint MinimumNeverPreemptPriority {
            get {
                if ((curObj["MinimumNeverPreemptPriority"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["MinimumNeverPreemptPriority"]));
            }
            set {
                curObj["MinimumNeverPreemptPriority"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsMinimumPreemptorPriorityNull {
            get {
                if ((curObj["MinimumPreemptorPriority"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Minimum priority a cluster group must have to be able to preempt another group.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint MinimumPreemptorPriority {
            get {
                if ((curObj["MinimumPreemptorPriority"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["MinimumPreemptorPriority"]));
            }
            set {
                curObj["MinimumPreemptorPriority"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Name property specifies the name of the cluster.")]
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
        public bool IsNetftIPSecEnabledNull {
            get {
                if ((curObj["NetftIPSecEnabled"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("IPSec enabled for cluster internal traffic.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint NetftIPSecEnabled {
            get {
                if ((curObj["NetftIPSecEnabled"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["NetftIPSecEnabled"]));
            }
            set {
                curObj["NetftIPSecEnabled"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("This property is obsolete.")]
        public string[] NetworkAdminExtensions {
            get {
                return ((string[])(curObj["NetworkAdminExtensions"]));
            }
            set {
                curObj["NetworkAdminExtensions"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("This property is obsolete.")]
        public string[] NetworkInterfaceAdminExtensions {
            get {
                return ((string[])(curObj["NetworkInterfaceAdminExtensions"]));
            }
            set {
                curObj["NetworkInterfaceAdminExtensions"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("This property is obsolete.")]
        public string[] NetworkPriorities {
            get {
                return ((string[])(curObj["NetworkPriorities"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("This property is obsolete.")]
        public string[] NodeAdminExtensions {
            get {
                return ((string[])(curObj["NodeAdminExtensions"]));
            }
            set {
                curObj["NodeAdminExtensions"] = value;
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
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPlumbAllCrossSubnetRoutesNull {
            get {
                if ((curObj["PlumbAllCrossSubnetRoutes"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Plumb all possible cross subnet routes to all nodes.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint PlumbAllCrossSubnetRoutes {
            get {
                if ((curObj["PlumbAllCrossSubnetRoutes"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["PlumbAllCrossSubnetRoutes"]));
            }
            set {
                curObj["PlumbAllCrossSubnetRoutes"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPreventQuorumNull {
            get {
                if ((curObj["PreventQuorum"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Specifies if the cluster is in a prevent quorum state.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint PreventQuorum {
            get {
                if ((curObj["PreventQuorum"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["PreventQuorum"]));
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
        [Description("Private properties of the Cluster.")]
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
        public bool IsQuorumArbitrationTimeMaxNull {
            get {
                if ((curObj["QuorumArbitrationTimeMax"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Maximum time necessary to decide the Quorum owner node.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint QuorumArbitrationTimeMax {
            get {
                if ((curObj["QuorumArbitrationTimeMax"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["QuorumArbitrationTimeMax"]));
            }
            set {
                curObj["QuorumArbitrationTimeMax"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsQuorumArbitrationTimeMinNull {
            get {
                if ((curObj["QuorumArbitrationTimeMin"] == null)) {
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
        public uint QuorumArbitrationTimeMin {
            get {
                if ((curObj["QuorumArbitrationTimeMin"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["QuorumArbitrationTimeMin"]));
            }
            set {
                curObj["QuorumArbitrationTimeMin"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsQuorumLogFileSizeNull {
            get {
                if ((curObj["QuorumLogFileSize"] == null)) {
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
        public uint QuorumLogFileSize {
            get {
                if ((curObj["QuorumLogFileSize"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["QuorumLogFileSize"]));
            }
            set {
                curObj["QuorumLogFileSize"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Path that is used to maintain quorum files.")]
        public string QuorumPath {
            get {
                return ((string)(curObj["QuorumPath"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Get the current quorum type - Majority or No Majority")]
        public string QuorumType {
            get {
                return ((string)(curObj["QuorumType"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsQuorumTypeValueNull {
            get {
                if ((curObj["QuorumTypeValue"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Get the current quorum type value.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public QuorumTypeValueValues QuorumTypeValue {
            get {
                if ((curObj["QuorumTypeValue"] == null)) {
                    return ((QuorumTypeValueValues)(System.Convert.ToInt32(0)));
                }
                return ((QuorumTypeValueValues)(System.Convert.ToInt32(curObj["QuorumTypeValue"])));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Allows cluster to reset the last events displayed to clients.")]
        public string RecentEventsResetTime {
            get {
                return ((string)(curObj["RecentEventsResetTime"]));
            }
            set {
                curObj["RecentEventsResetTime"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsRequestReplyTimeoutNull {
            get {
                if ((curObj["RequestReplyTimeout"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Controls the request reply timeout period.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint RequestReplyTimeout {
            get {
                if ((curObj["RequestReplyTimeout"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["RequestReplyTimeout"]));
            }
            set {
                curObj["RequestReplyTimeout"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("This property is obsolete.")]
        public string[] ResourceAdminExtensions {
            get {
                return ((string[])(curObj["ResourceAdminExtensions"]));
            }
            set {
                curObj["ResourceAdminExtensions"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsResourceDllDeadlockPeriodNull {
            get {
                if ((curObj["ResourceDllDeadlockPeriod"] == null)) {
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
        public uint ResourceDllDeadlockPeriod {
            get {
                if ((curObj["ResourceDllDeadlockPeriod"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["ResourceDllDeadlockPeriod"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("This property is obsolete.")]
        public string[] ResourceTypeAdminExtensions {
            get {
                return ((string[])(curObj["ResourceTypeAdminExtensions"]));
            }
            set {
                curObj["ResourceTypeAdminExtensions"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
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
        public bool IsRootMemoryReservedNull {
            get {
                if ((curObj["RootMemoryReserved"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Controls the amount of memory reserved for the parent partition on all cluster no" +
            "des.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint RootMemoryReserved {
            get {
                if ((curObj["RootMemoryReserved"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["RootMemoryReserved"]));
            }
            set {
                curObj["RootMemoryReserved"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsRouteHistoryLengthNull {
            get {
                if ((curObj["RouteHistoryLength"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Specifies history length for routes to help finding network issues.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint RouteHistoryLength {
            get {
                if ((curObj["RouteHistoryLength"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["RouteHistoryLength"]));
            }
            set {
                curObj["RouteHistoryLength"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsSameSubnetDelayNull {
            get {
                if ((curObj["SameSubnetDelay"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Controls how long the cluster network driver waits in milliseconds between sendin" +
            "g Cluster Service heartbeats on the same subnet.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint SameSubnetDelay {
            get {
                if ((curObj["SameSubnetDelay"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["SameSubnetDelay"]));
            }
            set {
                curObj["SameSubnetDelay"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsSameSubnetThresholdNull {
            get {
                if ((curObj["SameSubnetThreshold"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Controls how many Cluster Service heartbeats can be missed on the same subnet bef" +
            "ore it determines that Cluster Service has stopped responding.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint SameSubnetThreshold {
            get {
                if ((curObj["SameSubnetThreshold"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["SameSubnetThreshold"]));
            }
            set {
                curObj["SameSubnetThreshold"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("This property is obsolete.")]
        public byte[] Security {
            get {
                return ((byte[])(curObj["Security"]));
            }
            set {
                curObj["Security"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsSecurityLevelNull {
            get {
                if ((curObj["SecurityLevel"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The level of security that should apply to intra cluster messages.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public SecurityLevelValues SecurityLevel {
            get {
                if ((curObj["SecurityLevel"] == null)) {
                    return ((SecurityLevelValues)(System.Convert.ToInt32(3)));
                }
                return ((SecurityLevelValues)(System.Convert.ToInt32(curObj["SecurityLevel"])));
            }
            set {
                if ((SecurityLevelValues.NULL_ENUM_VALUE == value)) {
                    curObj["SecurityLevel"] = null;
                }
                else {
                    curObj["SecurityLevel"] = value;
                }
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Stores the Windows 2000 security descriptor of a cluster.")]
        public byte[] Security_Descriptor {
            get {
                return ((byte[])(curObj["Security_Descriptor"]));
            }
            set {
                curObj["Security_Descriptor"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Certain classes of filter drivers may be flagged as incompatible with direct I/O " +
            "mode of shared volume.Use this property to override cluster service determinatio" +
            "n of filters as incompatible. Add one or more filter driver name without .sys ex" +
            "tension.")]
        public string[] SharedVolumeCompatibleFilters {
            get {
                return ((string[])(curObj["SharedVolumeCompatibleFilters"]));
            }
            set {
                curObj["SharedVolumeCompatibleFilters"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Certain classes of filter drivers may be incompatible with direct I/O mode of sha" +
            "red volume.Use this property to add filters to be deemed as incompatible for dir" +
            "ect I/O. Add one or more filter driver name without .sys extension.")]
        public string[] SharedVolumeIncompatibleFilters {
            get {
                return ((string[])(curObj["SharedVolumeIncompatibleFilters"]));
            }
            set {
                curObj["SharedVolumeIncompatibleFilters"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Controls access to the CSV Meta Data Server volume for the user mode applications" +
            ".")]
        public byte[] SharedVolumeSecurityDescriptor {
            get {
                return ((byte[])(curObj["SharedVolumeSecurityDescriptor"]));
            }
            set {
                curObj["SharedVolumeSecurityDescriptor"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Root directory from which the cluster shared volumes are linked.")]
        public string SharedVolumesRoot {
            get {
                return ((string)(curObj["SharedVolumesRoot"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsSharedVolumeVssWriterOperationTimeoutNull {
            get {
                if ((curObj["SharedVolumeVssWriterOperationTimeout"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("CSV VSS Writer operation timeout in seconds.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint SharedVolumeVssWriterOperationTimeout {
            get {
                if ((curObj["SharedVolumeVssWriterOperationTimeout"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["SharedVolumeVssWriterOperationTimeout"]));
            }
            set {
                curObj["SharedVolumeVssWriterOperationTimeout"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsShutdownTimeoutInMinutesNull {
            get {
                if ((curObj["ShutdownTimeoutInMinutes"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Maximum time in minutes allowed for cluster groups to come offline during cluster" +
            " service shutdown.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint ShutdownTimeoutInMinutes {
            get {
                if ((curObj["ShutdownTimeoutInMinutes"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["ShutdownTimeoutInMinutes"]));
            }
            set {
                curObj["ShutdownTimeoutInMinutes"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
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
        public bool IsUseClientAccessNetworksForSharedVolumesNull {
            get {
                if ((curObj["UseClientAccessNetworksForSharedVolumes"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Enables/disables the use of client access networks for cluster shared volumes fea" +
            "ture of this cluster.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public UseClientAccessNetworksForSharedVolumesValues UseClientAccessNetworksForSharedVolumes {
            get {
                if ((curObj["UseClientAccessNetworksForSharedVolumes"] == null)) {
                    return ((UseClientAccessNetworksForSharedVolumesValues)(System.Convert.ToInt32(2)));
                }
                return ((UseClientAccessNetworksForSharedVolumesValues)(System.Convert.ToInt32(curObj["UseClientAccessNetworksForSharedVolumes"])));
            }
            set {
                if ((UseClientAccessNetworksForSharedVolumesValues.NULL_ENUM_VALUE == value)) {
                    curObj["UseClientAccessNetworksForSharedVolumes"] = null;
                }
                else {
                    curObj["UseClientAccessNetworksForSharedVolumes"] = value;
                }
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsWitnessDatabaseWriteTimeoutNull {
            get {
                if ((curObj["WitnessDatabaseWriteTimeout"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The maximum time in seconds that a cluster database write to a witness can take b" +
            "efore the write is abandoned.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint WitnessDatabaseWriteTimeout {
            get {
                if ((curObj["WitnessDatabaseWriteTimeout"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["WitnessDatabaseWriteTimeout"]));
            }
            set {
                curObj["WitnessDatabaseWriteTimeout"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsWitnessDynamicWeightNull {
            get {
                if ((curObj["WitnessDynamicWeight"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Specifies weight of configured witness.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint WitnessDynamicWeight {
            get {
                if ((curObj["WitnessDynamicWeight"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["WitnessDynamicWeight"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsWitnessRestartIntervalNull {
            get {
                if ((curObj["WitnessRestartInterval"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Controls the witness restart interval.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint WitnessRestartInterval {
            get {
                if ((curObj["WitnessRestartInterval"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["WitnessRestartInterval"]));
            }
            set {
                curObj["WitnessRestartInterval"] = value;
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
        
        private bool ShouldSerializeAddEvictDelay() {
            if ((this.IsAddEvictDelayNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetAddEvictDelay() {
            curObj["AddEvictDelay"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeAdminAccessPoint() {
            if ((this.IsAdminAccessPointNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetAdminExtensions() {
            curObj["AdminExtensions"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeBackupInProgress() {
            if ((this.IsBackupInProgressNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetBackupInProgress() {
            curObj["BackupInProgress"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeBlockCacheSize() {
            if ((this.IsBlockCacheSizeNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetBlockCacheSize() {
            curObj["BlockCacheSize"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeClusSvcHangTimeout() {
            if ((this.IsClusSvcHangTimeoutNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeClusSvcRegroupOpeningTimeout() {
            if ((this.IsClusSvcRegroupOpeningTimeoutNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeClusSvcRegroupPruningTimeout() {
            if ((this.IsClusSvcRegroupPruningTimeoutNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeClusSvcRegroupStageTimeout() {
            if ((this.IsClusSvcRegroupStageTimeoutNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeClusSvcRegroupTickInMilliseconds() {
            if ((this.IsClusSvcRegroupTickInMillisecondsNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeClusterEnforcedAntiAffinity() {
            if ((this.IsClusterEnforcedAntiAffinityNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetClusterEnforcedAntiAffinity() {
            curObj["ClusterEnforcedAntiAffinity"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeClusterGroupWaitDelay() {
            if ((this.IsClusterGroupWaitDelayNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetClusterGroupWaitDelay() {
            curObj["ClusterGroupWaitDelay"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeClusterLogLevel() {
            if ((this.IsClusterLogLevelNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetClusterLogLevel() {
            curObj["ClusterLogLevel"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeClusterLogSize() {
            if ((this.IsClusterLogSizeNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetClusterLogSize() {
            curObj["ClusterLogSize"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeCrossSubnetDelay() {
            if ((this.IsCrossSubnetDelayNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetCrossSubnetDelay() {
            curObj["CrossSubnetDelay"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeCrossSubnetThreshold() {
            if ((this.IsCrossSubnetThresholdNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetCrossSubnetThreshold() {
            curObj["CrossSubnetThreshold"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeCsvBalancer() {
            if ((this.IsCsvBalancerNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetCsvBalancer() {
            curObj["CsvBalancer"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeDatabaseReadWriteMode() {
            if ((this.IsDatabaseReadWriteModeNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetDatabaseReadWriteMode() {
            curObj["DatabaseReadWriteMode"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeDefaultNetworkRole() {
            if ((this.IsDefaultNetworkRoleNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetDefaultNetworkRole() {
            curObj["DefaultNetworkRole"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private void ResetDescription() {
            curObj["Description"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeDisableGroupPreferredOwnerRandomization() {
            if ((this.IsDisableGroupPreferredOwnerRandomizationNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetDisableGroupPreferredOwnerRandomization() {
            curObj["DisableGroupPreferredOwnerRandomization"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeDrainOnShutdown() {
            if ((this.IsDrainOnShutdownNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetDrainOnShutdown() {
            curObj["DrainOnShutdown"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeDynamicQuorumEnabled() {
            if ((this.IsDynamicQuorumEnabledNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetDynamicQuorumEnabled() {
            curObj["DynamicQuorumEnabled"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeEnableSharedVolumes() {
            if ((this.IsEnableSharedVolumesNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetEnableSharedVolumes() {
            curObj["EnableSharedVolumes"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeFixQuorum() {
            if ((this.IsFixQuorumNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetGroupAdminExtensions() {
            curObj["GroupAdminExtensions"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeHangRecoveryAction() {
            if ((this.IsHangRecoveryActionNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetHangRecoveryAction() {
            curObj["HangRecoveryAction"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeIgnorePersistentStateOnStartup() {
            if ((this.IsIgnorePersistentStateOnStartupNull == false)) {
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
        
        private bool ShouldSerializeLogResourceControls() {
            if ((this.IsLogResourceControlsNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetLogResourceControls() {
            curObj["LogResourceControls"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeLowerQuorumPriorityNodeId() {
            if ((this.IsLowerQuorumPriorityNodeIdNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetLowerQuorumPriorityNodeId() {
            curObj["LowerQuorumPriorityNodeId"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private void ResetMaintenanceFile() {
            curObj["MaintenanceFile"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeMaxNumberOfNodes() {
            if ((this.IsMaxNumberOfNodesNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeMessageBufferLength() {
            if ((this.IsMessageBufferLengthNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetMessageBufferLength() {
            curObj["MessageBufferLength"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeMinimumNeverPreemptPriority() {
            if ((this.IsMinimumNeverPreemptPriorityNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetMinimumNeverPreemptPriority() {
            curObj["MinimumNeverPreemptPriority"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeMinimumPreemptorPriority() {
            if ((this.IsMinimumPreemptorPriorityNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetMinimumPreemptorPriority() {
            curObj["MinimumPreemptorPriority"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeNetftIPSecEnabled() {
            if ((this.IsNetftIPSecEnabledNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetNetftIPSecEnabled() {
            curObj["NetftIPSecEnabled"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private void ResetNetworkAdminExtensions() {
            curObj["NetworkAdminExtensions"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private void ResetNetworkInterfaceAdminExtensions() {
            curObj["NetworkInterfaceAdminExtensions"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private void ResetNodeAdminExtensions() {
            curObj["NodeAdminExtensions"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializePlumbAllCrossSubnetRoutes() {
            if ((this.IsPlumbAllCrossSubnetRoutesNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetPlumbAllCrossSubnetRoutes() {
            curObj["PlumbAllCrossSubnetRoutes"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializePreventQuorum() {
            if ((this.IsPreventQuorumNull == false)) {
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
        
        private bool ShouldSerializeQuorumArbitrationTimeMax() {
            if ((this.IsQuorumArbitrationTimeMaxNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetQuorumArbitrationTimeMax() {
            curObj["QuorumArbitrationTimeMax"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeQuorumArbitrationTimeMin() {
            if ((this.IsQuorumArbitrationTimeMinNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetQuorumArbitrationTimeMin() {
            curObj["QuorumArbitrationTimeMin"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeQuorumLogFileSize() {
            if ((this.IsQuorumLogFileSizeNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetQuorumLogFileSize() {
            curObj["QuorumLogFileSize"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeQuorumTypeValue() {
            if ((this.IsQuorumTypeValueNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetRecentEventsResetTime() {
            curObj["RecentEventsResetTime"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeRequestReplyTimeout() {
            if ((this.IsRequestReplyTimeoutNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetRequestReplyTimeout() {
            curObj["RequestReplyTimeout"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private void ResetResourceAdminExtensions() {
            curObj["ResourceAdminExtensions"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeResourceDllDeadlockPeriod() {
            if ((this.IsResourceDllDeadlockPeriodNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetResourceTypeAdminExtensions() {
            curObj["ResourceTypeAdminExtensions"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeRootMemoryReserved() {
            if ((this.IsRootMemoryReservedNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetRootMemoryReserved() {
            curObj["RootMemoryReserved"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeRouteHistoryLength() {
            if ((this.IsRouteHistoryLengthNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetRouteHistoryLength() {
            curObj["RouteHistoryLength"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeSameSubnetDelay() {
            if ((this.IsSameSubnetDelayNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetSameSubnetDelay() {
            curObj["SameSubnetDelay"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeSameSubnetThreshold() {
            if ((this.IsSameSubnetThresholdNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetSameSubnetThreshold() {
            curObj["SameSubnetThreshold"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private void ResetSecurity() {
            curObj["Security"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeSecurityLevel() {
            if ((this.IsSecurityLevelNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetSecurityLevel() {
            curObj["SecurityLevel"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private void ResetSecurity_Descriptor() {
            curObj["Security_Descriptor"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private void ResetSharedVolumeCompatibleFilters() {
            curObj["SharedVolumeCompatibleFilters"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private void ResetSharedVolumeIncompatibleFilters() {
            curObj["SharedVolumeIncompatibleFilters"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private void ResetSharedVolumeSecurityDescriptor() {
            curObj["SharedVolumeSecurityDescriptor"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeSharedVolumeVssWriterOperationTimeout() {
            if ((this.IsSharedVolumeVssWriterOperationTimeoutNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetSharedVolumeVssWriterOperationTimeout() {
            curObj["SharedVolumeVssWriterOperationTimeout"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeShutdownTimeoutInMinutes() {
            if ((this.IsShutdownTimeoutInMinutesNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetShutdownTimeoutInMinutes() {
            curObj["ShutdownTimeoutInMinutes"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeUseClientAccessNetworksForSharedVolumes() {
            if ((this.IsUseClientAccessNetworksForSharedVolumesNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetUseClientAccessNetworksForSharedVolumes() {
            curObj["UseClientAccessNetworksForSharedVolumes"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeWitnessDatabaseWriteTimeout() {
            if ((this.IsWitnessDatabaseWriteTimeoutNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetWitnessDatabaseWriteTimeout() {
            curObj["WitnessDatabaseWriteTimeout"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeWitnessDynamicWeight() {
            if ((this.IsWitnessDynamicWeightNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeWitnessRestartInterval() {
            if ((this.IsWitnessRestartIntervalNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetWitnessRestartInterval() {
            curObj["WitnessRestartInterval"] = null;
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
            string strPath = "ROOT\\MSCluster:MSCluster_Cluster";
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
        public static ClusterCollection GetInstances() {
            return GetInstances(null, null, null);
        }
        
        public static ClusterCollection GetInstances(string condition) {
            return GetInstances(null, condition, null);
        }
        
        public static ClusterCollection GetInstances(string[] selectedProperties) {
            return GetInstances(null, null, selectedProperties);
        }
        
        public static ClusterCollection GetInstances(string condition, string[] selectedProperties) {
            return GetInstances(null, condition, selectedProperties);
        }
        
        public static ClusterCollection GetInstances(System.Management.ManagementScope mgmtScope, System.Management.EnumerationOptions enumOptions) {
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
            pathObj.ClassName = "MSCluster_Cluster";
            pathObj.NamespacePath = "root\\MSCluster";
            System.Management.ManagementClass clsObject = new System.Management.ManagementClass(mgmtScope, pathObj, null);
            if ((enumOptions == null)) {
                enumOptions = new System.Management.EnumerationOptions();
                enumOptions.EnsureLocatable = true;
            }
            return new ClusterCollection(clsObject.GetInstances(enumOptions));
        }
        
        public static ClusterCollection GetInstances(System.Management.ManagementScope mgmtScope, string condition) {
            return GetInstances(mgmtScope, condition, null);
        }
        
        public static ClusterCollection GetInstances(System.Management.ManagementScope mgmtScope, string[] selectedProperties) {
            return GetInstances(mgmtScope, null, selectedProperties);
        }
        
        public static ClusterCollection GetInstances(System.Management.ManagementScope mgmtScope, string condition, string[] selectedProperties) {
            if ((mgmtScope == null)) {
                if ((statMgmtScope == null)) {
                    mgmtScope = new System.Management.ManagementScope();
                    mgmtScope.Path.NamespacePath = "root\\MSCluster";
                }
                else {
                    mgmtScope = statMgmtScope;
                }
            }
            System.Management.ManagementObjectSearcher ObjectSearcher = new System.Management.ManagementObjectSearcher(mgmtScope, new SelectQuery("MSCluster_Cluster", condition, selectedProperties));
            System.Management.EnumerationOptions enumOptions = new System.Management.EnumerationOptions();
            enumOptions.EnsureLocatable = true;
            ObjectSearcher.Options = enumOptions;
            return new ClusterCollection(ObjectSearcher.Get());
        }
        
        [Browsable(true)]
        public static Cluster CreateInstance() {
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
            return new Cluster(tmpMgmtClass.CreateInstance());
        }
        
        [Browsable(true)]
        public void Delete() {
            PrivateLateBoundObject.Delete();
        }
        
        public void AddNode(string NodeName) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("AddNode");
                inParams["NodeName"] = ((string)(NodeName));
                PrivateLateBoundObject.InvokeMethod("AddNode", inParams, null);
            }
        }
        
        public void AddResourceToClusterSharedVolumes(string Resource) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("AddResourceToClusterSharedVolumes");
                inParams["Resource"] = ((string)(Resource));
                PrivateLateBoundObject.InvokeMethod("AddResourceToClusterSharedVolumes", inParams, null);
            }
        }
        
        public void AddVirtualMachine(string VirtualMachine) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("AddVirtualMachine");
                inParams["VirtualMachine"] = ((string)(VirtualMachine));
                PrivateLateBoundObject.InvokeMethod("AddVirtualMachine", inParams, null);
            }
        }
        
        public static void CreateCluster(string ClusterName, string[] IPAddresses, string[] NodeNames, string[] SubnetMasks) {
            bool IsMethodStatic = true;
            if ((IsMethodStatic == true)) {
                System.Management.ManagementBaseObject inParams = null;
                System.Management.ManagementPath mgmtPath = new System.Management.ManagementPath(CreatedClassName);
                System.Management.ManagementClass classObj = new System.Management.ManagementClass(statMgmtScope, mgmtPath, null);
                inParams = classObj.GetMethodParameters("CreateCluster");
                inParams["ClusterName"] = ((string)(ClusterName));
                inParams["IPAddresses"] = ((string[])(IPAddresses));
                inParams["NodeNames"] = ((string[])(NodeNames));
                inParams["SubnetMasks"] = ((string[])(SubnetMasks));
                classObj.InvokeMethod("CreateCluster", inParams, null);
            }
        }
        
        public void DestroyCluster(bool CleanupAD) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("DestroyCluster");
                inParams["CleanupAD"] = ((bool)(CleanupAD));
                PrivateLateBoundObject.InvokeMethod("DestroyCluster", inParams, null);
            }
        }
        
        public void EvictNode(string NodeName) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("EvictNode");
                inParams["NodeName"] = ((string)(NodeName));
                PrivateLateBoundObject.InvokeMethod("EvictNode", inParams, null);
            }
        }
        
        public void ExecuteClusterControl(int ControlCode, byte[] InputBuffer, out byte[] OutputBuffer, out int OutputBufferSize) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("ExecuteClusterControl");
                inParams["ControlCode"] = ((int)(ControlCode));
                inParams["InputBuffer"] = ((byte[])(InputBuffer));
                System.Management.ManagementBaseObject outParams = PrivateLateBoundObject.InvokeMethod("ExecuteClusterControl", inParams, null);
                OutputBuffer = ((byte[])(outParams.Properties["OutputBuffer"].Value));
                OutputBufferSize = System.Convert.ToInt32(outParams.Properties["OutputBufferSize"].Value);
            }
            else {
                OutputBuffer = null;
                OutputBufferSize = System.Convert.ToInt32(0);
            }
        }
        
        public static void ForceCleanup(string NodeName, uint Timeout) {
            bool IsMethodStatic = true;
            if ((IsMethodStatic == true)) {
                System.Management.ManagementBaseObject inParams = null;
                System.Management.ManagementPath mgmtPath = new System.Management.ManagementPath(CreatedClassName);
                System.Management.ManagementClass classObj = new System.Management.ManagementClass(statMgmtScope, mgmtPath, null);
                inParams = classObj.GetMethodParameters("ForceCleanup");
                inParams["NodeName"] = ((string)(NodeName));
                inParams["Timeout"] = ((uint)(Timeout));
                classObj.InvokeMethod("ForceCleanup", inParams, null);
            }
        }
        
        public void GenerateValidationStatus(out System.Management.ManagementPath Status) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                System.Management.ManagementBaseObject outParams = PrivateLateBoundObject.InvokeMethod("GenerateValidationStatus", inParams, null);
                Status = null;
                if ((outParams.Properties["Status"] != null)) {
                    Status = new System.Management.ManagementPath(outParams.Properties["Status"].ToString());
                }
            }
            else {
                Status = null;
            }
        }
        
        public static void GetNodeClusterState(out int ClusterState) {
            bool IsMethodStatic = true;
            if ((IsMethodStatic == true)) {
                System.Management.ManagementBaseObject inParams = null;
                System.Management.ManagementPath mgmtPath = new System.Management.ManagementPath(CreatedClassName);
                System.Management.ManagementClass classObj = new System.Management.ManagementClass(statMgmtScope, mgmtPath, null);
                System.Management.ManagementBaseObject outParams = classObj.InvokeMethod("GetNodeClusterState", inParams, null);
                ClusterState = System.Convert.ToInt32(outParams.Properties["ClusterState"].Value);
            }
            else {
                ClusterState = System.Convert.ToInt32(0);
            }
        }
        
        public void RemoveResourceFromClusterSharedVolumes(string Resource) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("RemoveResourceFromClusterSharedVolumes");
                inParams["Resource"] = ((string)(Resource));
                PrivateLateBoundObject.InvokeMethod("RemoveResourceFromClusterSharedVolumes", inParams, null);
            }
        }
        
        public void Rename(string NewName) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("Rename");
                inParams["NewName"] = ((string)(NewName));
                PrivateLateBoundObject.InvokeMethod("Rename", inParams, null);
            }
        }
        
        public void SetDiskQuorum(string QuorumPath, string Resource) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("SetDiskQuorum");
                inParams["QuorumPath"] = ((string)(QuorumPath));
                inParams["Resource"] = ((string)(Resource));
                PrivateLateBoundObject.InvokeMethod("SetDiskQuorum", inParams, null);
            }
        }
        
        public void SetMajorityQuorum(string QuorumPath, string Resource) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("SetMajorityQuorum");
                inParams["QuorumPath"] = ((string)(QuorumPath));
                inParams["Resource"] = ((string)(Resource));
                PrivateLateBoundObject.InvokeMethod("SetMajorityQuorum", inParams, null);
            }
        }
        
        public void SetNodeMajorityQuorum() {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                PrivateLateBoundObject.InvokeMethod("SetNodeMajorityQuorum", inParams, null);
            }
        }
        
        public void VerifyPath(string Group, string Path, out uint result) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("VerifyPath");
                inParams["Group"] = ((string)(Group));
                inParams["Path"] = ((string)(Path));
                System.Management.ManagementBaseObject outParams = PrivateLateBoundObject.InvokeMethod("VerifyPath", inParams, null);
                result = System.Convert.ToUInt32(outParams.Properties["result"].Value);
            }
            else {
                result = System.Convert.ToUInt32(0);
            }
        }
        
        public enum BlockCacheSizeValues {
            
            Disabled = 0,
            
            Enabled = 1,
            
            NULL_ENUM_VALUE = 2,
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
        
        public enum EnableSharedVolumesValues {
            
            Disabled = 0,
            
            Enabled = 1,
            
            NULL_ENUM_VALUE = 2,
        }
        
        public enum QuorumTypeValueValues {
            
            Unknown0 = -1,
            
            Node = 1,
            
            FileShareWitness = 2,
            
            Storage = 3,
            
            None = 4,
            
            NULL_ENUM_VALUE = 0,
        }
        
        public enum SecurityLevelValues {
            
            Clear_Text = 0,
            
            Sign = 1,
            
            Encrypt = 2,
            
            NULL_ENUM_VALUE = 3,
        }
        
        public enum UseClientAccessNetworksForSharedVolumesValues {
            
            Disabled = 0,
            
            Enabled = 1,
            
            NULL_ENUM_VALUE = 2,
        }
        
        // Enumerator implementation for enumerating instances of the class.
        public class ClusterCollection : object, ICollection {
            
            private ManagementObjectCollection privColObj;
            
            public ClusterCollection(ManagementObjectCollection objCollection) {
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
                    array.SetValue(new Cluster(((System.Management.ManagementObject)(array.GetValue(nCtr)))), nCtr);
                }
            }
            
            public virtual System.Collections.IEnumerator GetEnumerator() {
                return new ClusterEnumerator(privColObj.GetEnumerator());
            }
            
            public class ClusterEnumerator : object, System.Collections.IEnumerator {
                
                private ManagementObjectCollection.ManagementObjectEnumerator privObjEnum;
                
                public ClusterEnumerator(ManagementObjectCollection.ManagementObjectEnumerator objEnum) {
                    privObjEnum = objEnum;
                }
                
                public virtual object Current {
                    get {
                        return new Cluster(((System.Management.ManagementObject)(privObjEnum.Current)));
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
