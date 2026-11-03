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
    // An Early Bound class generated for the WMI class.Msvm_EthernetPortAllocationSettingData
    public class EthernetPortAllocationSettingData : System.ComponentModel.Component {

        // Private property to hold the WMI namespace in which the class resides.
        private static string CreatedWmiNamespace = "ROOT\\virtualization\\v2";

        // Private property to hold the name of WMI class which created this class.
        public static string CreatedClassName = "Msvm_EthernetPortAllocationSettingData";

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
        public EthernetPortAllocationSettingData() {
            this.InitializeObject(null, null, null);
        }

        public EthernetPortAllocationSettingData(string keyInstanceID) {
            this.InitializeObject(null, new System.Management.ManagementPath(EthernetPortAllocationSettingData.ConstructPath(keyInstanceID)), null);
        }

        public EthernetPortAllocationSettingData(System.Management.ManagementScope mgmtScope, string keyInstanceID) {
            this.InitializeObject(((System.Management.ManagementScope)(mgmtScope)), new System.Management.ManagementPath(EthernetPortAllocationSettingData.ConstructPath(keyInstanceID)), null);
        }

        public EthernetPortAllocationSettingData(System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(null, path, getOptions);
        }

        public EthernetPortAllocationSettingData(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path) {
            this.InitializeObject(mgmtScope, path, null);
        }

        public EthernetPortAllocationSettingData(System.Management.ManagementPath path) {
            this.InitializeObject(null, path, null);
        }

        public EthernetPortAllocationSettingData(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(mgmtScope, path, getOptions);
        }

        public EthernetPortAllocationSettingData(System.Management.ManagementObject theObject) {
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

        public EthernetPortAllocationSettingData(System.Management.ManagementBaseObject theObject) {
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
        public string Address {
            get {
                return ((string)(curObj["Address"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string AddressOnParent {
            get {
                return ((string)(curObj["AddressOnParent"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string AllocationUnits {
            get {
                return ((string)(curObj["AllocationUnits"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsAutomaticAllocationNull {
            get {
                if ((curObj["AutomaticAllocation"] == null)) {
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
        public bool AutomaticAllocation {
            get {
                if ((curObj["AutomaticAllocation"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["AutomaticAllocation"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsAutomaticDeallocationNull {
            get {
                if ((curObj["AutomaticDeallocation"] == null)) {
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
        public bool AutomaticDeallocation {
            get {
                if ((curObj["AutomaticDeallocation"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["AutomaticDeallocation"]));
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
        public string[] Connection {
            get {
                return ((string[])(curObj["Connection"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsConsumerVisibilityNull {
            get {
                if ((curObj["ConsumerVisibility"] == null)) {
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
        public ushort ConsumerVisibility {
            get {
                if ((curObj["ConsumerVisibility"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["ConsumerVisibility"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string Description {
            get {
                return ((string)(curObj["Description"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsDesiredVLANEndpointModeNull {
            get {
                if ((curObj["DesiredVLANEndpointMode"] == null)) {
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
        public ushort DesiredVLANEndpointMode {
            get {
                if ((curObj["DesiredVLANEndpointMode"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["DesiredVLANEndpointMode"]));
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
        [Description(@"EnabledState is an integer enumeration that indicates whether the allocation request is enabled or disabled. When an allocation request is marked as Disabled (3), then the allocation is not processed. The EnabledState for an active configuration is always marked as Enabled (2).")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public EnabledStateValues EnabledState {
            get {
                if ((curObj["EnabledState"] == null)) {
                    return ((EnabledStateValues)(System.Convert.ToInt32(0)));
                }
                return ((EnabledStateValues)(System.Convert.ToInt32(curObj["EnabledState"])));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string[] HostResource {
            get {
                return ((string[])(curObj["HostResource"]));
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
        public bool IsLimitNull {
            get {
                if ((curObj["Limit"] == null)) {
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
        public ulong Limit {
            get {
                if ((curObj["Limit"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["Limit"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsMappingBehaviorNull {
            get {
                if ((curObj["MappingBehavior"] == null)) {
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
        public ushort MappingBehavior {
            get {
                if ((curObj["MappingBehavior"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["MappingBehavior"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string OtherEndpointMode {
            get {
                return ((string)(curObj["OtherEndpointMode"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string OtherResourceType {
            get {
                return ((string)(curObj["OtherResourceType"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string Parent {
            get {
                return ((string)(curObj["Parent"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string PoolID {
            get {
                return ((string)(curObj["PoolID"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The list of friendly names corresponding to each entry in the RequiredFeatures.")]
        public string[] RequiredFeatureHints {
            get {
                return ((string[])(curObj["RequiredFeatureHints"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The list of feature identifers representing all the features that are required fo" +
            "r a port.")]
        public string[] RequiredFeatures {
            get {
                return ((string[])(curObj["RequiredFeatures"]));
            }
            set {
                curObj["RequiredFeatures"] = value;
                if (((isEmbedded == false)
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsReservationNull {
            get {
                if ((curObj["Reservation"] == null)) {
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
        public ulong Reservation {
            get {
                if ((curObj["Reservation"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["Reservation"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string ResourceSubType {
            get {
                return ((string)(curObj["ResourceSubType"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsResourceTypeNull {
            get {
                if ((curObj["ResourceType"] == null)) {
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
        public ushort ResourceType {
            get {
                if ((curObj["ResourceType"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["ResourceType"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("This property specifies the network resource pool from which a connection will be" +
            " allocated to test replica system when it is created.")]
        public string TestReplicaPoolID {
            get {
                return ((string)(curObj["TestReplicaPoolID"]));
            }
            set {
                curObj["TestReplicaPoolID"] = value;
                if (((isEmbedded == false)
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("This property specifies the friendly name of the virtual network switch to which " +
            "a connection will be allocated for the test replica system when it is created.")]
        public string TestReplicaSwitchName {
            get {
                return ((string)(curObj["TestReplicaSwitchName"]));
            }
            set {
                curObj["TestReplicaSwitchName"] = value;
                if (((isEmbedded == false)
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsVirtualQuantityNull {
            get {
                if ((curObj["VirtualQuantity"] == null)) {
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
        public ulong VirtualQuantity {
            get {
                if ((curObj["VirtualQuantity"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["VirtualQuantity"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string VirtualQuantityUnits {
            get {
                return ((string)(curObj["VirtualQuantityUnits"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsWeightNull {
            get {
                if ((curObj["Weight"] == null)) {
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
        public uint Weight {
            get {
                if ((curObj["Weight"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["Weight"]));
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

        private bool ShouldSerializeAutomaticAllocation() {
            if ((this.IsAutomaticAllocationNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeAutomaticDeallocation() {
            if ((this.IsAutomaticDeallocationNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeConsumerVisibility() {
            if ((this.IsConsumerVisibilityNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeDesiredVLANEndpointMode() {
            if ((this.IsDesiredVLANEndpointModeNull == false)) {
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

        private bool ShouldSerializeLimit() {
            if ((this.IsLimitNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeMappingBehavior() {
            if ((this.IsMappingBehaviorNull == false)) {
                return true;
            }
            return false;
        }

        private void ResetRequiredFeatures() {
            curObj["RequiredFeatures"] = null;
            if (((isEmbedded == false)
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }

        private bool ShouldSerializeReservation() {
            if ((this.IsReservationNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeResourceType() {
            if ((this.IsResourceTypeNull == false)) {
                return true;
            }
            return false;
        }

        private void ResetTestReplicaPoolID() {
            curObj["TestReplicaPoolID"] = null;
            if (((isEmbedded == false)
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }

        private void ResetTestReplicaSwitchName() {
            curObj["TestReplicaSwitchName"] = null;
            if (((isEmbedded == false)
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }

        private bool ShouldSerializeVirtualQuantity() {
            if ((this.IsVirtualQuantityNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeWeight() {
            if ((this.IsWeightNull == false)) {
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

        private static string ConstructPath(string keyInstanceID) {
            string strPath = "ROOT\\virtualization\\v2:Msvm_EthernetPortAllocationSettingData";
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
        public static EthernetPortAllocationSettingDataCollection GetInstances() {
            return GetInstances(null, null, null);
        }

        public static EthernetPortAllocationSettingDataCollection GetInstances(string condition) {
            return GetInstances(null, condition, null);
        }

        public static EthernetPortAllocationSettingDataCollection GetInstances(string[] selectedProperties) {
            return GetInstances(null, null, selectedProperties);
        }

        public static EthernetPortAllocationSettingDataCollection GetInstances(string condition, string[] selectedProperties) {
            return GetInstances(null, condition, selectedProperties);
        }

        public static EthernetPortAllocationSettingDataCollection GetInstances(System.Management.ManagementScope mgmtScope, System.Management.EnumerationOptions enumOptions) {
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
            pathObj.ClassName = "Msvm_EthernetPortAllocationSettingData";
            pathObj.NamespacePath = "root\\virtualization\\v2";
            System.Management.ManagementClass clsObject = new System.Management.ManagementClass(mgmtScope, pathObj, null);
            if ((enumOptions == null)) {
                enumOptions = new System.Management.EnumerationOptions();
                enumOptions.EnsureLocatable = true;
            }
            return new EthernetPortAllocationSettingDataCollection(clsObject.GetInstances(enumOptions));
        }

        public static EthernetPortAllocationSettingDataCollection GetInstances(System.Management.ManagementScope mgmtScope, string condition) {
            return GetInstances(mgmtScope, condition, null);
        }

        public static EthernetPortAllocationSettingDataCollection GetInstances(System.Management.ManagementScope mgmtScope, string[] selectedProperties) {
            return GetInstances(mgmtScope, null, selectedProperties);
        }

        public static EthernetPortAllocationSettingDataCollection GetInstances(System.Management.ManagementScope mgmtScope, string condition, string[] selectedProperties) {
            if ((mgmtScope == null)) {
                if ((statMgmtScope == null)) {
                    mgmtScope = new System.Management.ManagementScope();
                    mgmtScope.Path.NamespacePath = "root\\virtualization\\v2";
                }
                else {
                    mgmtScope = statMgmtScope;
                }
            }
            System.Management.ManagementObjectSearcher ObjectSearcher = new System.Management.ManagementObjectSearcher(mgmtScope, new SelectQuery("Msvm_EthernetPortAllocationSettingData", condition, selectedProperties));
            System.Management.EnumerationOptions enumOptions = new System.Management.EnumerationOptions();
            enumOptions.EnsureLocatable = true;
            ObjectSearcher.Options = enumOptions;
            return new EthernetPortAllocationSettingDataCollection(ObjectSearcher.Get());
        }

        [Browsable(true)]
        public static EthernetPortAllocationSettingData CreateInstance() {
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
            return new EthernetPortAllocationSettingData(tmpMgmtClass.CreateInstance());
        }

        [Browsable(true)]
        public void Delete() {
            PrivateLateBoundObject.Delete();
        }

        public enum EnabledStateValues {

            Enabled = 2,

            Disabled = 3,

            NULL_ENUM_VALUE = 0,
        }

        // Enumerator implementation for enumerating instances of the class.
        public class EthernetPortAllocationSettingDataCollection : object, ICollection {

            private ManagementObjectCollection privColObj;

            public EthernetPortAllocationSettingDataCollection(ManagementObjectCollection objCollection) {
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
                    array.SetValue(new EthernetPortAllocationSettingData(((System.Management.ManagementObject)(array.GetValue(nCtr)))), nCtr);
                }
            }

            public virtual System.Collections.IEnumerator GetEnumerator() {
                return new EthernetPortAllocationSettingDataEnumerator(privColObj.GetEnumerator());
            }

            public class EthernetPortAllocationSettingDataEnumerator : object, System.Collections.IEnumerator {

                private ManagementObjectCollection.ManagementObjectEnumerator privObjEnum;

                public EthernetPortAllocationSettingDataEnumerator(ManagementObjectCollection.ManagementObjectEnumerator objEnum) {
                    privObjEnum = objEnum;
                }

                public virtual object Current {
                    get {
                        return new EthernetPortAllocationSettingData(((System.Management.ManagementObject)(privObjEnum.Current)));
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
