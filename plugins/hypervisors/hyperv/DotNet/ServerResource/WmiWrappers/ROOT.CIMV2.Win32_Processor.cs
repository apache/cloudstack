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

namespace CloudStack.Plugin.WmiWrappers.ROOT.CIMV2
{
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
    // An Early Bound class generated for the WMI class.Win32_Processor
    public class Processor : System.ComponentModel.Component {

        // Private property to hold the WMI namespace in which the class resides.
        private static string CreatedWmiNamespace = "ROOT\\CIMV2";

        // Private property to hold the name of WMI class which created this class.
        private static string CreatedClassName = "Win32_Processor";

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
        public Processor() {
            this.InitializeObject(null, null, null);
        }

        public Processor(string keyDeviceID) {
            this.InitializeObject(null, new System.Management.ManagementPath(Processor.ConstructPath(keyDeviceID)), null);
        }

        public Processor(System.Management.ManagementScope mgmtScope, string keyDeviceID) {
            this.InitializeObject(((System.Management.ManagementScope)(mgmtScope)), new System.Management.ManagementPath(Processor.ConstructPath(keyDeviceID)), null);
        }

        public Processor(System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(null, path, getOptions);
        }

        public Processor(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path) {
            this.InitializeObject(mgmtScope, path, null);
        }

        public Processor(System.Management.ManagementPath path) {
            this.InitializeObject(null, path, null);
        }

        public Processor(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(mgmtScope, path, getOptions);
        }

        public Processor(System.Management.ManagementObject theObject) {
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

        public Processor(System.Management.ManagementBaseObject theObject) {
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
                return "ROOT\\CIMV2";
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
        public bool IsAddressWidthNull {
            get {
                if ((curObj["AddressWidth"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Processor address width in bits.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort AddressWidth {
            get {
                if ((curObj["AddressWidth"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["AddressWidth"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsArchitectureNull {
            get {
                if ((curObj["Architecture"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Architecture property specifies the processor architecture used by this platf" +
            "orm. It returns one of the following        integer values:\n0 - x86 \n1 - MIPS \n2" +
            " - Alpha \n3 - PowerPC \n6 - ia64 \n9 - x64 \n")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ArchitectureValues Architecture {
            get {
                if ((curObj["Architecture"] == null)) {
                    return ((ArchitectureValues)(System.Convert.ToInt32(10)));
                }
                return ((ArchitectureValues)(System.Convert.ToInt32(curObj["Architecture"])));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsAvailabilityNull {
            get {
                if ((curObj["Availability"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The availability and status of the device.  For example, the Availability property indicates that the device is running and has full power (value=3), or is in a warning (4), test (5), degraded (10) or power save state (values 13-15 and 17). Regarding the power saving states, these are defined as follows: Value 13 (""Power Save - Unknown"") indicates that the device is known to be in a power save mode, but its exact status in this mode is unknown; 14 (""Power Save - Low Power Mode"") indicates that the device is in a power save state but still functioning, and may exhibit degraded performance; 15 (""Power Save - Standby"") describes that the device is not functioning but could be brought to full power 'quickly'; and value 17 (""Power Save - Warning"") indicates that the device is in a warning state, though also in a power save mode.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public AvailabilityValues Availability {
            get {
                if ((curObj["Availability"] == null)) {
                    return ((AvailabilityValues)(System.Convert.ToInt32(0)));
                }
                return ((AvailabilityValues)(System.Convert.ToInt32(curObj["Availability"])));
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
        public bool IsConfigManagerErrorCodeNull {
            get {
                if ((curObj["ConfigManagerErrorCode"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Indicates the Win32 Configuration Manager error code.  The following values may b" +
            "e returned: \n0      This device is working properly. \n1      This device is not " +
            "configured correctly. \n2      Windows cannot load the driver for this device. \n3" +
            "      The driver for this device might be corrupted, or your system may be runni" +
            "ng low on memory or other resources. \n4      This device is not working properly" +
            ". One of its drivers or your registry might be corrupted. \n5      The driver for" +
            " this device needs a resource that Windows cannot manage. \n6      The boot confi" +
            "guration for this device conflicts with other devices. \n7      Cannot filter. \n8" +
            "      The driver loader for the device is missing. \n9      This device is not wo" +
            "rking properly because the controlling firmware is reporting the resources for t" +
            "he device incorrectly. \n10     This device cannot start. \n11     This device fai" +
            "led. \n12     This device cannot find enough free resources that it can use. \n13 " +
            "    Windows cannot verify this device\'s resources. \n14     This device cannot wo" +
            "rk properly until you restart your computer. \n15     This device is not working " +
            "properly because there is probably a re-enumeration problem. \n16     Windows can" +
            "not identify all the resources this device uses. \n17     This device is asking f" +
            "or an unknown resource type. \n18     Reinstall the drivers for this device. \n19 " +
            "    Your registry might be corrupted. \n20     Failure using the VxD loader. \n21 " +
            "    System failure: Try changing the driver for this device. If that does not wo" +
            "rk, see your hardware documentation. Windows is removing this device. \n22     Th" +
            "is device is disabled. \n23     System failure: Try changing the driver for this " +
            "device. If that doesn\'t work, see your hardware documentation. \n24     This devi" +
            "ce is not present, is not working properly, or does not have all its drivers ins" +
            "talled. \n25     Windows is still setting up this device. \n26     Windows is stil" +
            "l setting up this device. \n27     This device does not have valid log configurat" +
            "ion. \n28     The drivers for this device are not installed. \n29     This device " +
            "is disabled because the firmware of the device did not give it the required reso" +
            "urces. \n30     This device is using an Interrupt Request (IRQ) resource that ano" +
            "ther device is using. \n31     This device is not working properly because Window" +
            "s cannot load the drivers required for this device.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ConfigManagerErrorCodeValues ConfigManagerErrorCode {
            get {
                if ((curObj["ConfigManagerErrorCode"] == null)) {
                    return ((ConfigManagerErrorCodeValues)(System.Convert.ToInt32(32)));
                }
                return ((ConfigManagerErrorCodeValues)(System.Convert.ToInt32(curObj["ConfigManagerErrorCode"])));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsConfigManagerUserConfigNull {
            get {
                if ((curObj["ConfigManagerUserConfig"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Indicates whether the device is using a user-defined configuration.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool ConfigManagerUserConfig {
            get {
                if ((curObj["ConfigManagerUserConfig"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["ConfigManagerUserConfig"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsCpuStatusNull {
            get {
                if ((curObj["CpuStatus"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The CpuStatus property specifies the current status of the processor. Changes in " +
            "status arise from processor usage, not the physical condition of the processor.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public CpuStatusValues CpuStatus {
            get {
                if ((curObj["CpuStatus"] == null)) {
                    return ((CpuStatusValues)(System.Convert.ToInt32(8)));
                }
                return ((CpuStatusValues)(System.Convert.ToInt32(curObj["CpuStatus"])));
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
        public bool IsCurrentClockSpeedNull {
            get {
                if ((curObj["CurrentClockSpeed"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The current speed (in MHz) of this processor.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint CurrentClockSpeed {
            get {
                if ((curObj["CurrentClockSpeed"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["CurrentClockSpeed"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsCurrentVoltageNull {
            get {
                if ((curObj["CurrentVoltage"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The CurrentVoltage specifies the voltage of the processor. bits 0-6 of the field contain the processor's current voltage times 10. This value is only set when SMBIOS designates a voltage value. For specific values, see VoltageCaps.
Example: field value for a processor voltage of 1.8 volts would be 92h = 80h + (1.8 x 10) = 80h + 18 = 80h + 12h.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort CurrentVoltage {
            get {
                if ((curObj["CurrentVoltage"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["CurrentVoltage"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsDataWidthNull {
            get {
                if ((curObj["DataWidth"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Processor data width in bits.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort DataWidth {
            get {
                if ((curObj["DataWidth"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["DataWidth"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Description property provides a textual description of the object. ")]
        public string Description {
            get {
                return ((string)(curObj["Description"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The DeviceID property contains a string uniquely identifying the processor with o" +
            "ther devices on the system.")]
        public string DeviceID {
            get {
                return ((string)(curObj["DeviceID"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsErrorClearedNull {
            get {
                if ((curObj["ErrorCleared"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("ErrorCleared is a boolean property indicating that the error reported in LastErro" +
            "rCode property is now cleared.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool ErrorCleared {
            get {
                if ((curObj["ErrorCleared"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["ErrorCleared"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("ErrorDescription is a free-form string supplying more information about the error" +
            " recorded in LastErrorCode property, and information on any corrective actions t" +
            "hat may be taken.")]
        public string ErrorDescription {
            get {
                return ((string)(curObj["ErrorDescription"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsExtClockNull {
            get {
                if ((curObj["ExtClock"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The ExtClock property specifies the external clock frequency. If the frequency is" +
            " unknown this property is set to null.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint ExtClock {
            get {
                if ((curObj["ExtClock"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["ExtClock"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsFamilyNull {
            get {
                if ((curObj["Family"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The processor family type. For example, values include \"Pentium(R) processor with" +
            " MMX(TM) technology\" (14) and \"68040\" (96).")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public FamilyValues Family {
            get {
                if ((curObj["Family"] == null)) {
                    return ((FamilyValues)(System.Convert.ToInt32(0)));
                }
                return ((FamilyValues)(System.Convert.ToInt32(curObj["Family"])));
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
        [Description("The InstallDate property is datetime value indicating when the object was install" +
            "ed. A lack of a value does not indicate that the object is not installed.")]
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
        public bool IsL2CacheSizeNull {
            get {
                if ((curObj["L2CacheSize"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The L2CacheSize property specifies the size of the processor\'s Level 2 cache. A L" +
            "evel 2 cache is an external memory area that has a faster access times than the " +
            "main RAM memory.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint L2CacheSize {
            get {
                if ((curObj["L2CacheSize"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["L2CacheSize"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsL2CacheSpeedNull {
            get {
                if ((curObj["L2CacheSpeed"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The L2CacheSpeed property specifies the clockspeed of the processor\'s Level 2 cac" +
            "he. A Level 2 cache is an external memory area that has a faster access times th" +
            "an the main RAM memory.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint L2CacheSpeed {
            get {
                if ((curObj["L2CacheSpeed"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["L2CacheSpeed"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsL3CacheSizeNull {
            get {
                if ((curObj["L3CacheSize"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The L3CacheSize property specifies the size of the processor\'s Level 3 cache. A L" +
            "evel 3 cache is an external memory area that has a faster access times than the " +
            "main RAM memory.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint L3CacheSize {
            get {
                if ((curObj["L3CacheSize"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["L3CacheSize"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsL3CacheSpeedNull {
            get {
                if ((curObj["L3CacheSpeed"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The L3CacheSpeed property specifies the clockspeed of the processor\'s Level 3 cac" +
            "he. A Level 3 cache is an external memory area that has a faster access times th" +
            "an the main RAM memory.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint L3CacheSpeed {
            get {
                if ((curObj["L3CacheSpeed"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["L3CacheSpeed"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsLastErrorCodeNull {
            get {
                if ((curObj["LastErrorCode"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("LastErrorCode captures the last error code reported by the logical device.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint LastErrorCode {
            get {
                if ((curObj["LastErrorCode"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["LastErrorCode"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsLevelNull {
            get {
                if ((curObj["Level"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Level property further defines the processor type. The value  depends on the " +
            "architecture of the processor.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort Level {
            get {
                if ((curObj["Level"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["Level"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsLoadPercentageNull {
            get {
                if ((curObj["LoadPercentage"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The LoadPercentage property specifies each processor\'s load capacity averaged ove" +
            "r the last second. The term \'processor loading\' refers to the total computing bu" +
            "rden each processor carries at one time.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort LoadPercentage {
            get {
                if ((curObj["LoadPercentage"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["LoadPercentage"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Manufacturer property specifies the name of the processor\'s manufacturer.\nExa" +
            "mple: GenuineSilicon")]
        public string Manufacturer {
            get {
                return ((string)(curObj["Manufacturer"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsMaxClockSpeedNull {
            get {
                if ((curObj["MaxClockSpeed"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The maximum speed (in MHz) of this processor.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint MaxClockSpeed {
            get {
                if ((curObj["MaxClockSpeed"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["MaxClockSpeed"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Name property defines the label by which the object is known. When subclassed" +
            ", the Name property can be overridden to be a Key property.")]
        public string Name {
            get {
                return ((string)(curObj["Name"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsNumberOfCoresNull {
            get {
                if ((curObj["NumberOfCores"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The NumberOfCores property contains a Processor\'s total number of cores. e.g dual" +
            " core machine will have NumberOfCores = 2.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint NumberOfCores {
            get {
                if ((curObj["NumberOfCores"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["NumberOfCores"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsNumberOfLogicalProcessorsNull {
            get {
                if ((curObj["NumberOfLogicalProcessors"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The NumberOfLogicalProcessors property specifies the total number of logical proc" +
            "essors.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint NumberOfLogicalProcessors {
            get {
                if ((curObj["NumberOfLogicalProcessors"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["NumberOfLogicalProcessors"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("A string describing the processor family type - used when the family property is " +
            "set to 1 (\"Other\"). This string should be set to NULL when the family property i" +
            "s any value other than 1.")]
        public string OtherFamilyDescription {
            get {
                return ((string)(curObj["OtherFamilyDescription"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Indicates the Win32 Plug and Play device ID of the logical device.  Example: *PNP" +
            "030b")]
        public string PNPDeviceID {
            get {
                return ((string)(curObj["PNPDeviceID"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"Indicates the specific power-related capabilities of the logical device. The array values, 0=""Unknown"", 1=""Not Supported"" and 2=""Disabled"" are self-explanatory. The value, 3=""Enabled"" indicates that the power management features are currently enabled but the exact feature set is unknown or the information is unavailable. ""Power Saving Modes Entered Automatically"" (4) describes that a device can change its power state based on usage or other criteria. ""Power State Settable"" (5) indicates that the SetPowerState method is supported. ""Power Cycling Supported"" (6) indicates that the SetPowerState method can be invoked with the PowerState input variable set to 5 (""Power Cycle""). ""Timed Power On Supported"" (7) indicates that the SetPowerState method can be invoked with the PowerState input variable set to 5 (""Power Cycle"") and the Time parameter set to a specific date and time, or interval, for power-on.")]
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
        [Description(@"Boolean indicating that the Device can be power managed - ie, put into a power save state. This boolean does not indicate that power management features are currently enabled, or if enabled, what features are supported. Refer to the PowerManagementCapabilities array for this information. If this boolean is false, the integer value 1, for the string, ""Not Supported"", should be the only entry in the PowerManagementCapabilities array.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool PowerManagementSupported {
            get {
                if ((curObj["PowerManagementSupported"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["PowerManagementSupported"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The ProcessorId property contains processor-specific information that describes the processor's features. For x86 class CPUs, the field's format depends on the processor's support of the CPUID instruction. If the instruction is supported, the ProcessorId property contains two DWORD-formatted values. The first (offsets 08h-0Bh) is the EAX value returned by a CPUID instruction with input EAX set to 1. The second (offsets 0Ch-0Fh) is the EDX value returned by that instruction. Only the first two bytes of the ProcessorID property are significant (all others are set to 0) and contain (in WORD-format) the contents of the DX register at CPU reset.")]
        public string ProcessorId {
            get {
                return ((string)(curObj["ProcessorId"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsProcessorTypeNull {
            get {
                if ((curObj["ProcessorType"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The ProcessorType property specifies the processor\'s primary function.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ProcessorTypeValues ProcessorType {
            get {
                if ((curObj["ProcessorType"] == null)) {
                    return ((ProcessorTypeValues)(System.Convert.ToInt32(0)));
                }
                return ((ProcessorTypeValues)(System.Convert.ToInt32(curObj["ProcessorType"])));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsRevisionNull {
            get {
                if ((curObj["Revision"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Revision property specifies the system\'s architecture-dependent revision leve" +
            "l. The meaning of this value depends on the architecture of the processor. It co" +
            "ntains the same values as the \"Version\" member, but in a numerical format.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort Revision {
            get {
                if ((curObj["Revision"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["Revision"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("A free form string describing the role of the processor - for example, \"Central P" +
            "rocessor\"\' or \"Math Processor\"")]
        public string Role {
            get {
                return ((string)(curObj["Role"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsSecondLevelAddressTranslationExtensionsNull {
            get {
                if ((curObj["SecondLevelAddressTranslationExtensions"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The SecondLevelAddressTranslationExtensions property determines whether the proce" +
            "ssor supports address translation extensions used for virtualization.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool SecondLevelAddressTranslationExtensions {
            get {
                if ((curObj["SecondLevelAddressTranslationExtensions"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["SecondLevelAddressTranslationExtensions"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The SocketDesignation property contains the type of chip socket used on the circu" +
            "it.\nExample: J202")]
        public string SocketDesignation {
            get {
                return ((string)(curObj["SocketDesignation"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The Status property is a string indicating the current status of the object. Various operational and non-operational statuses can be defined. Operational statuses are ""OK"", ""Degraded"" and ""Pred Fail"". ""Pred Fail"" indicates that an element may be functioning properly but predicting a failure in the near future. An example is a SMART-enabled hard drive. Non-operational statuses can also be specified. These are ""Error"", ""Starting"", ""Stopping"" and ""Service"". The latter, ""Service"", could apply during mirror-resilvering of a disk, reload of a user permissions list, or other administrative work. Not all such work is on-line, yet the managed element is neither ""OK"" nor in one of the other states.")]
        public string Status {
            get {
                return ((string)(curObj["Status"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsStatusInfoNull {
            get {
                if ((curObj["StatusInfo"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("StatusInfo is a string indicating whether the logical device is in an enabled (va" +
            "lue = 3), disabled (value = 4) or some other (1) or unknown (2) state. If this p" +
            "roperty does not apply to the logical device, the value, 5 (\"Not Applicable\"), s" +
            "hould be used.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public StatusInfoValues StatusInfo {
            get {
                if ((curObj["StatusInfo"] == null)) {
                    return ((StatusInfoValues)(System.Convert.ToInt32(0)));
                }
                return ((StatusInfoValues)(System.Convert.ToInt32(curObj["StatusInfo"])));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Stepping is a free-form string indicating the revision level of the processor wit" +
            "hin the processor family.")]
        public string Stepping {
            get {
                return ((string)(curObj["Stepping"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The scoping System\'s CreationClassName.")]
        public string SystemCreationClassName {
            get {
                return ((string)(curObj["SystemCreationClassName"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The scoping System\'s Name.")]
        public string SystemName {
            get {
                return ((string)(curObj["SystemName"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("A globally unique identifier for the processor.  This identifier may only be uniq" +
            "ue within a processor family.")]
        public string UniqueId {
            get {
                return ((string)(curObj["UniqueId"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsUpgradeMethodNull {
            get {
                if ((curObj["UpgradeMethod"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("CPU socket information including data on how this Processor can be upgraded (if u" +
            "pgrades are supported). This property is an integer enumeration.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public UpgradeMethodValues UpgradeMethod {
            get {
                if ((curObj["UpgradeMethod"] == null)) {
                    return ((UpgradeMethodValues)(System.Convert.ToInt32(0)));
                }
                return ((UpgradeMethodValues)(System.Convert.ToInt32(curObj["UpgradeMethod"])));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Version property specifies an architecture-dependent processor revision numbe" +
            "r. Note: This member is not used in Windows 95.\nExample: Model 2, Stepping 12.")]
        public string Version {
            get {
                return ((string)(curObj["Version"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsVirtualizationFirmwareEnabledNull {
            get {
                if ((curObj["VirtualizationFirmwareEnabled"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The VirtualizationFirmwareEnabled property determines whether the Firmware has en" +
            "abled virtualization extensions.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool VirtualizationFirmwareEnabled {
            get {
                if ((curObj["VirtualizationFirmwareEnabled"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["VirtualizationFirmwareEnabled"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsVMMonitorModeExtensionsNull {
            get {
                if ((curObj["VMMonitorModeExtensions"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The VMMonitorModeExtensions property determines whether the processor supports In" +
            "tel or AMD Virtual Machine Monitor extensions.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool VMMonitorModeExtensions {
            get {
                if ((curObj["VMMonitorModeExtensions"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["VMMonitorModeExtensions"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsVoltageCapsNull {
            get {
                if ((curObj["VoltageCaps"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The VoltageCaps property specifies the voltage capabilities of the processor. Bits 0-3 of the field represent specific voltages that the processor socket can accept. All other bits should be set to zero. The socket is configurable if multiple bits are being set. For a range of voltages see CurrentVoltage. If the property is NULL, then the voltage capabilities are unknown.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public VoltageCapsValues VoltageCaps {
            get {
                if ((curObj["VoltageCaps"] == null)) {
                    return ((VoltageCapsValues)(System.Convert.ToInt32(8)));
                }
                return ((VoltageCapsValues)(System.Convert.ToInt32(curObj["VoltageCaps"])));
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

        private bool ShouldSerializeAddressWidth() {
            if ((this.IsAddressWidthNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeArchitecture() {
            if ((this.IsArchitectureNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeAvailability() {
            if ((this.IsAvailabilityNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeConfigManagerErrorCode() {
            if ((this.IsConfigManagerErrorCodeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeConfigManagerUserConfig() {
            if ((this.IsConfigManagerUserConfigNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeCpuStatus() {
            if ((this.IsCpuStatusNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeCurrentClockSpeed() {
            if ((this.IsCurrentClockSpeedNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeCurrentVoltage() {
            if ((this.IsCurrentVoltageNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeDataWidth() {
            if ((this.IsDataWidthNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeErrorCleared() {
            if ((this.IsErrorClearedNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeExtClock() {
            if ((this.IsExtClockNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeFamily() {
            if ((this.IsFamilyNull == false)) {
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

        private bool ShouldSerializeL2CacheSize() {
            if ((this.IsL2CacheSizeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeL2CacheSpeed() {
            if ((this.IsL2CacheSpeedNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeL3CacheSize() {
            if ((this.IsL3CacheSizeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeL3CacheSpeed() {
            if ((this.IsL3CacheSpeedNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeLastErrorCode() {
            if ((this.IsLastErrorCodeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeLevel() {
            if ((this.IsLevelNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeLoadPercentage() {
            if ((this.IsLoadPercentageNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeMaxClockSpeed() {
            if ((this.IsMaxClockSpeedNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeNumberOfCores() {
            if ((this.IsNumberOfCoresNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeNumberOfLogicalProcessors() {
            if ((this.IsNumberOfLogicalProcessorsNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePowerManagementSupported() {
            if ((this.IsPowerManagementSupportedNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeProcessorType() {
            if ((this.IsProcessorTypeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeRevision() {
            if ((this.IsRevisionNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeSecondLevelAddressTranslationExtensions() {
            if ((this.IsSecondLevelAddressTranslationExtensionsNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeStatusInfo() {
            if ((this.IsStatusInfoNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeUpgradeMethod() {
            if ((this.IsUpgradeMethodNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeVirtualizationFirmwareEnabled() {
            if ((this.IsVirtualizationFirmwareEnabledNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeVMMonitorModeExtensions() {
            if ((this.IsVMMonitorModeExtensionsNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeVoltageCaps() {
            if ((this.IsVoltageCapsNull == false)) {
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

        private static string ConstructPath(string keyDeviceID) {
            string strPath = "ROOT\\CIMV2:Win32_Processor";
            strPath = string.Concat(strPath, string.Concat(".DeviceID=", string.Concat("\"", string.Concat(keyDeviceID, "\""))));
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
        public static ProcessorCollection GetInstances() {
            return GetInstances(null, null, null);
        }

        public static ProcessorCollection GetInstances(string condition) {
            return GetInstances(null, condition, null);
        }

        public static ProcessorCollection GetInstances(string[] selectedProperties) {
            return GetInstances(null, null, selectedProperties);
        }

        public static ProcessorCollection GetInstances(string condition, string[] selectedProperties) {
            return GetInstances(null, condition, selectedProperties);
        }

        public static ProcessorCollection GetInstances(System.Management.ManagementScope mgmtScope, System.Management.EnumerationOptions enumOptions) {
            if ((mgmtScope == null)) {
                if ((statMgmtScope == null)) {
                    mgmtScope = new System.Management.ManagementScope();
                    mgmtScope.Path.NamespacePath = "root\\CIMV2";
                }
                else {
                    mgmtScope = statMgmtScope;
                }
            }
            System.Management.ManagementPath pathObj = new System.Management.ManagementPath();
            pathObj.ClassName = "Win32_Processor";
            pathObj.NamespacePath = "root\\CIMV2";
            System.Management.ManagementClass clsObject = new System.Management.ManagementClass(mgmtScope, pathObj, null);
            if ((enumOptions == null)) {
                enumOptions = new System.Management.EnumerationOptions();
                enumOptions.EnsureLocatable = true;
            }
            return new ProcessorCollection(clsObject.GetInstances(enumOptions));
        }

        public static ProcessorCollection GetInstances(System.Management.ManagementScope mgmtScope, string condition) {
            return GetInstances(mgmtScope, condition, null);
        }

        public static ProcessorCollection GetInstances(System.Management.ManagementScope mgmtScope, string[] selectedProperties) {
            return GetInstances(mgmtScope, null, selectedProperties);
        }

        public static ProcessorCollection GetInstances(System.Management.ManagementScope mgmtScope, string condition, string[] selectedProperties) {
            if ((mgmtScope == null)) {
                if ((statMgmtScope == null)) {
                    mgmtScope = new System.Management.ManagementScope();
                    mgmtScope.Path.NamespacePath = "root\\CIMV2";
                }
                else {
                    mgmtScope = statMgmtScope;
                }
            }
            System.Management.ManagementObjectSearcher ObjectSearcher = new System.Management.ManagementObjectSearcher(mgmtScope, new SelectQuery("Win32_Processor", condition, selectedProperties));
            System.Management.EnumerationOptions enumOptions = new System.Management.EnumerationOptions();
            enumOptions.EnsureLocatable = true;
            ObjectSearcher.Options = enumOptions;
            return new ProcessorCollection(ObjectSearcher.Get());
        }

        [Browsable(true)]
        public static Processor CreateInstance() {
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
            return new Processor(tmpMgmtClass.CreateInstance());
        }

        [Browsable(true)]
        public void Delete() {
            PrivateLateBoundObject.Delete();
        }

        public uint Reset() {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                System.Management.ManagementBaseObject outParams = PrivateLateBoundObject.InvokeMethod("Reset", inParams, null);
                return System.Convert.ToUInt32(outParams.Properties["ReturnValue"].Value);
            }
            else {
                return System.Convert.ToUInt32(0);
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

        public enum ArchitectureValues {

            X86 = 0,

            MIPS = 1,

            Alpha = 2,

            PowerPC = 3,

            Ia64 = 6,

            X64 = 9,

            NULL_ENUM_VALUE = 10,
        }

        public enum AvailabilityValues {

            Other0 = 1,

            Unknown0 = 2,

            Running_Full_Power = 3,

            Warning = 4,

            In_Test = 5,

            Not_Applicable = 6,

            Power_Off = 7,

            Off_Line = 8,

            Off_Duty = 9,

            Degraded = 10,

            Not_Installed = 11,

            Install_Error = 12,

            Power_Save_Unknown = 13,

            Power_Save_Low_Power_Mode = 14,

            Power_Save_Standby = 15,

            Power_Cycle = 16,

            Power_Save_Warning = 17,

            Paused = 18,

            Not_Ready = 19,

            Not_Configured = 20,

            Quiesced = 21,

            NULL_ENUM_VALUE = 0,
        }

        public enum ConfigManagerErrorCodeValues {

            This_device_is_working_properly_ = 0,

            This_device_is_not_configured_correctly_ = 1,

            Windows_cannot_load_the_driver_for_this_device_ = 2,

            The_driver_for_this_device_might_be_corrupted_or_your_system_may_be_running_low_on_memory_or_other_resources_ = 3,

            This_device_is_not_working_properly_One_of_its_drivers_or_your_registry_might_be_corrupted_ = 4,

            The_driver_for_this_device_needs_a_resource_that_Windows_cannot_manage_ = 5,

            The_boot_configuration_for_this_device_conflicts_with_other_devices_ = 6,

            Cannot_filter_ = 7,

            The_driver_loader_for_the_device_is_missing_ = 8,

            This_device_is_not_working_properly_because_the_controlling_firmware_is_reporting_the_resources_for_the_device_incorrectly_ = 9,

            This_device_cannot_start_ = 10,

            This_device_failed_ = 11,

            This_device_cannot_find_enough_free_resources_that_it_can_use_ = 12,

            Windows_cannot_verify_this_device_s_resources_ = 13,

            This_device_cannot_work_properly_until_you_restart_your_computer_ = 14,

            This_device_is_not_working_properly_because_there_is_probably_a_re_enumeration_problem_ = 15,

            Windows_cannot_identify_all_the_resources_this_device_uses_ = 16,

            This_device_is_asking_for_an_unknown_resource_type_ = 17,

            Reinstall_the_drivers_for_this_device_ = 18,

            Failure_using_the_VxD_loader_ = 19,

            Your_registry_might_be_corrupted_ = 20,

            System_failure_Try_changing_the_driver_for_this_device_If_that_does_not_work_see_your_hardware_documentation_Windows_is_removing_this_device_ = 21,

            This_device_is_disabled_ = 22,

            System_failure_Try_changing_the_driver_for_this_device_If_that_doesn_t_work_see_your_hardware_documentation_ = 23,

            This_device_is_not_present_is_not_working_properly_or_does_not_have_all_its_drivers_installed_ = 24,

            Windows_is_still_setting_up_this_device_ = 25,

            Windows_is_still_setting_up_this_device_0 = 26,

            This_device_does_not_have_valid_log_configuration_ = 27,

            The_drivers_for_this_device_are_not_installed_ = 28,

            This_device_is_disabled_because_the_firmware_of_the_device_did_not_give_it_the_required_resources_ = 29,

            This_device_is_using_an_Interrupt_Request_IRQ_resource_that_another_device_is_using_ = 30,

            This_device_is_not_working_properly_because_Windows_cannot_load_the_drivers_required_for_this_device_ = 31,

            NULL_ENUM_VALUE = 32,
        }

        public enum CpuStatusValues {

            Unknown0 = 0,

            CPU_Enabled = 1,

            CPU_Disabled_by_User_via_BIOS_Setup = 2,

            CPU_Disabled_By_BIOS_POST_Error_ = 3,

            CPU_is_Idle = 4,

            Reserved = 5,

            Reserved0 = 6,

            Other0 = 7,

            NULL_ENUM_VALUE = 8,
        }

        public enum FamilyValues {

            Other0 = 1,

            Unknown0 = 2,

            Val_8086 = 3,

            Val_80286 = 4,

            Val_80386 = 5,

            Val_80486 = 6,

            Val_8087 = 7,

            Val_80287 = 8,

            Val_80387 = 9,

            Val_80487 = 10,

            Pentium_R_brand = 11,

            Pentium_R_Pro = 12,

            Pentium_R_II = 13,

            Pentium_R_processor_with_MMX_TM_technology = 14,

            Celeron_TM_ = 15,

            Pentium_R_II_Xeon_TM_ = 16,

            Pentium_R_III = 17,

            M1_Family = 18,

            M2_Family = 19,

            K5_Family = 24,

            K6_Family = 25,

            K6_2 = 26,

            K6_3 = 27,

            AMD_Athlon_TM_Processor_Family = 28,

            AMD_R_Duron_TM_Processor = 29,

            AMD29000_Family = 30,

            K6_2_ = 31,

            Power_PC_Family = 32,

            Power_PC_601 = 33,

            Power_PC_603 = 34,

            Power_PC_603_ = 35,

            Power_PC_604 = 36,

            Power_PC_620 = 37,

            Power_PC_X704 = 38,

            Power_PC_750 = 39,

            Alpha_Family = 48,

            Alpha_21064 = 49,

            Alpha_21066 = 50,

            Alpha_21164 = 51,

            Alpha_21164PC = 52,

            Alpha_21164a = 53,

            Alpha_21264 = 54,

            Alpha_21364 = 55,

            MIPS_Family = 64,

            MIPS_R4000 = 65,

            MIPS_R4200 = 66,

            MIPS_R4400 = 67,

            MIPS_R4600 = 68,

            MIPS_R10000 = 69,

            SPARC_Family = 80,

            SuperSPARC = 81,

            MicroSPARC_II = 82,

            MicroSPARC_IIep = 83,

            UltraSPARC = 84,

            UltraSPARC_II = 85,

            UltraSPARC_IIi = 86,

            UltraSPARC_III0 = 87,

            UltraSPARC_IIIi = 88,

            Val_68040 = 96,

            Val_68xxx_Family = 97,

            Val_68000 = 98,

            Val_68010 = 99,

            Val_68020 = 100,

            Val_68030 = 101,

            Hobbit_Family = 112,

            Crusoe_TM_TM5000_Family = 120,

            Crusoe_TM_TM3000_Family = 121,

            Efficeon_TM_TM8000_Family = 122,

            Weitek = 128,

            Itanium_TM_Processor = 130,

            AMD_Athlon_TM_64_Processor_Family = 131,

            AMD_Opteron_TM_Family = 132,

            PA_RISC_Family = 144,

            PA_RISC_8500 = 145,

            PA_RISC_8000 = 146,

            PA_RISC_7300LC = 147,

            PA_RISC_7200 = 148,

            PA_RISC_7100LC = 149,

            PA_RISC_7100 = 150,

            V30_Family = 160,

            Pentium_R_III_Xeon_TM_ = 176,

            Pentium_R_III_Processor_with_Intel_R_SpeedStep_TM_Technology = 177,

            Pentium_R_4 = 178,

            Intel_R_Xeon_TM_ = 179,

            AS400_Family = 180,

            Intel_R_Xeon_TM_processor_MP = 181,

            AMD_AthlonXP_TM_Family = 182,

            AMD_AthlonMP_TM_Family = 183,

            Intel_R_Itanium_R_2 = 184,

            Intel_Pentium_M_Processor = 185,

            K7 = 190,

            IBM390_Family = 200,

            G4 = 201,

            G5 = 202,

            G6 = 203,

            Z_Architecture_base = 204,

            I860 = 250,

            I960 = 251,

            SH_3 = 260,

            SH_4 = 261,

            ARM = 280,

            StrongARM = 281,

            Val_6x86 = 300,

            MediaGX = 301,

            MII = 302,

            WinChip = 320,

            DSP = 350,

            Video_Processor = 500,

            NULL_ENUM_VALUE = 0,
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

        public enum ProcessorTypeValues {

            Other0 = 1,

            Unknown0 = 2,

            Central_Processor = 3,

            Math_Processor = 4,

            DSP_Processor = 5,

            Video_Processor = 6,

            NULL_ENUM_VALUE = 0,
        }

        public enum StatusInfoValues {

            Other0 = 1,

            Unknown0 = 2,

            Enabled = 3,

            Disabled = 4,

            Not_Applicable = 5,

            NULL_ENUM_VALUE = 0,
        }

        public enum UpgradeMethodValues {

            Other0 = 1,

            Unknown0 = 2,

            Daughter_Board = 3,

            ZIF_Socket = 4,

            Replacement_Piggy_Back = 5,

            None = 6,

            LIF_Socket = 7,

            Slot_1 = 8,

            Slot_2 = 9,

            Val_370_Pin_Socket = 10,

            Slot_A = 11,

            Slot_M = 12,

            Socket_423 = 13,

            Socket_A_Socket_462_ = 14,

            Socket_478 = 15,

            Socket_754 = 16,

            Socket_940 = 17,

            Socket_939 = 18,

            NULL_ENUM_VALUE = 0,
        }

        public enum VoltageCapsValues {

            Val_5 = 1,

            Val_3_3 = 2,

            Val_2_9 = 4,

            NULL_ENUM_VALUE = 8,
        }

        // Enumerator implementation for enumerating instances of the class.
        public class ProcessorCollection : object, ICollection {

            private ManagementObjectCollection privColObj;

            public ProcessorCollection(ManagementObjectCollection objCollection) {
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
                    array.SetValue(new Processor(((System.Management.ManagementObject)(array.GetValue(nCtr)))), nCtr);
                }
            }

            public virtual System.Collections.IEnumerator GetEnumerator() {
                return new ProcessorEnumerator(privColObj.GetEnumerator());
            }

            public class ProcessorEnumerator : object, System.Collections.IEnumerator {

                private ManagementObjectCollection.ManagementObjectEnumerator privObjEnum;

                public ProcessorEnumerator(ManagementObjectCollection.ManagementObjectEnumerator objEnum) {
                    privObjEnum = objEnum;
                }

                public virtual object Current {
                    get {
                        return new Processor(((System.Management.ManagementObject)(privObjEnum.Current)));
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
