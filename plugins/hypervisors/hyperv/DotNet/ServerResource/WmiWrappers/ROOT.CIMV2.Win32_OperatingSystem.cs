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

namespace CloudStack.Plugin.WmiWrappers.ROOT.CIMV2 {
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
    // An Early Bound class generated for the WMI class.Win32_OperatingSystem
    public class OperatingSystem0 : System.ComponentModel.Component {

        // Private property to hold the WMI namespace in which the class resides.
        private static string CreatedWmiNamespace = "ROOT\\CIMV2";

        // Private property to hold the name of WMI class which created this class.
        private static string CreatedClassName = "Win32_OperatingSystem";

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
        public OperatingSystem0() {
            this.InitializeObject(null, new System.Management.ManagementPath(OperatingSystem0.ConstructPath()), null);
        }

        public OperatingSystem0(System.Management.ManagementScope mgmtScope) {
            this.InitializeObject(mgmtScope, new System.Management.ManagementPath(OperatingSystem0.ConstructPath()), null);
        }

        public OperatingSystem0(System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(null, new System.Management.ManagementPath(OperatingSystem0.ConstructPath()), getOptions);
        }

        public OperatingSystem0(System.Management.ManagementScope mgmtScope, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(mgmtScope, new System.Management.ManagementPath(OperatingSystem0.ConstructPath()), getOptions);
        }

        public OperatingSystem0(System.Management.ManagementPath path) {
            this.InitializeObject(null, path, null);
        }

        public OperatingSystem0(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(mgmtScope, path, getOptions);
        }

        public OperatingSystem0(System.Management.ManagementObject theObject) {
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

        public OperatingSystem0(System.Management.ManagementBaseObject theObject) {
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

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The BootDevice property indicates the name of the disk drive from which the Win32" +
            " operating system boots. \nExample: \\\\Device\\Harddisk0.")]
        public string BootDevice {
            get {
                return ((string)(curObj["BootDevice"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The BuildNumber property indicates the build number of the operating system.  It " +
            "can be used for more precise versioning information than product release version" +
            " numbers\nExample: 1381")]
        public string BuildNumber {
            get {
                return ((string)(curObj["BuildNumber"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The BuildType property indicates the type of build used for the operating system." +
            " Examples are retail build and checked build.")]
        public string BuildType {
            get {
                return ((string)(curObj["BuildType"]));
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

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The CodeSet property indicates the code page value used by the operating system. A code page contains a character table used by the operating system to translate strings for different languages. The American National Standards Institute (ANSI) lists values that represent defined code pages. If the operating system does not use an ANSI code page, this member will be set to 0. The CodeSet string can use up to six characters to define the code page value.
Example: 1255.")]
        public string CodeSet {
            get {
                return ((string)(curObj["CodeSet"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The CountryCode property indicates the code for the country/regionused by the operating system. Values are based on international phone dialing prefixes (also referred to as IBM country/region codes). The CountryCode string can use up to six characters to define the country/region code value.
Example: 1 for the United States)")]
        public string CountryCode {
            get {
                return ((string)(curObj["CountryCode"]));
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
        [Description("CSCreationClassName contains the scoping computer system\'s creation class name.")]
        public string CSCreationClassName {
            get {
                return ((string)(curObj["CSCreationClassName"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The CSDVersion property contains a null-terminated string, that indicates the latest Service Pack installed on the computer system. If no Service Pack is installed, the string is NULL. For computer systems running Windows 95, this property contains a null-terminated string that provides arbitrary additional information about the operating system.
Example: Service Pack 3.")]
        public string CSDVersion {
            get {
                return ((string)(curObj["CSDVersion"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("CSName contains the scoping computer system\'s name.")]
        public string CSName {
            get {
                return ((string)(curObj["CSName"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsCurrentTimeZoneNull {
            get {
                if ((curObj["CurrentTimeZone"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("CurrentTimeZone indicates the number of minutes the operating system is offset fr" +
            "om Greenwich Mean Time. Either the number is positive, negative or zero.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public short CurrentTimeZone {
            get {
                if ((curObj["CurrentTimeZone"] == null)) {
                    return System.Convert.ToInt16(0);
                }
                return ((short)(curObj["CurrentTimeZone"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsDataExecutionPrevention_32BitApplicationsNull {
            get {
                if ((curObj["DataExecutionPrevention_32BitApplications"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("If true, indicates that 32-bit applications are running with Data Execution Preve" +
            "ntion (DEP) applied. (false if DataExecutionPrevention_Available = false)")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool DataExecutionPrevention_32BitApplications {
            get {
                if ((curObj["DataExecutionPrevention_32BitApplications"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["DataExecutionPrevention_32BitApplications"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsDataExecutionPrevention_AvailableNull {
            get {
                if ((curObj["DataExecutionPrevention_Available"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"If true, indicates that the hardware supports Windows Data Execution Prevention (DEP) technology. DEP ensures that all memory locations are marked with a non-executable attribute unless the memory location explicitly contains executable code.  This can help mitigate certain types of buffer overrun security exploits.  If DEP is available, 64-bit applications are automatically protected.  To determine if DEP has been enabled for 32-bit applications and drivers, use the DataExecutionPrevention_ properties ")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool DataExecutionPrevention_Available {
            get {
                if ((curObj["DataExecutionPrevention_Available"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["DataExecutionPrevention_Available"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsDataExecutionPrevention_DriversNull {
            get {
                if ((curObj["DataExecutionPrevention_Drivers"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("If true, indicates that drivers are running with Data Execution Prevention (DEP) " +
            "applied. (false if DataExecutionPrevention_Available = false)")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool DataExecutionPrevention_Drivers {
            get {
                if ((curObj["DataExecutionPrevention_Drivers"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["DataExecutionPrevention_Drivers"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsDataExecutionPrevention_SupportPolicyNull {
            get {
                if ((curObj["DataExecutionPrevention_SupportPolicy"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The DataExecutionPrevention_SupportPolicy indicates which one of four Data Execution Prevention (DEP) settings is applied. Each setting varies by the extent to which DEP is applied to 32-bit applications.  Note that DEP is always applied to the Windows kernel. Always On (not available in the user interface) indicates that DEP is enabled for all 32-bit applications on the machine with no exceptions. OptOut indicates DEP is on by default for all 32-bit applications and that a user or administrator must explicitly remove support for a 32-bit application by adding to an exceptions list. OptIn indicates DEP is on for a limited number of binaries, the kernel, and all Windows services but it is off by default for all 32-bit applications; a user or administrator must explicitly choose the AlwaysOn (not available in the user interface) or OptOut setting before DEP can be applied to 32-bit applications.  AlwaysOff (not available in the user interface) indicates DEP is turned off for all 32-bit applications on the machine. ")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public DataExecutionPrevention_SupportPolicyValues DataExecutionPrevention_SupportPolicy {
            get {
                if ((curObj["DataExecutionPrevention_SupportPolicy"] == null)) {
                    return ((DataExecutionPrevention_SupportPolicyValues)(System.Convert.ToInt32(4)));
                }
                return ((DataExecutionPrevention_SupportPolicyValues)(System.Convert.ToInt32(curObj["DataExecutionPrevention_SupportPolicy"])));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsDebugNull {
            get {
                if ((curObj["Debug"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The Debug property indicates whether the operating system is a checked (debug) build. Checked builds provide error checking, argument verification, and system debugging code. Additional code in a checked binary generates a kernel debugger error message and breaks into the debugger. This helps  immediately determine the cause and location of the error. Performance suffers in the checked build due to the additional code that is executed.
Values: TRUE or FALSE, A value of TRUE indicates the debugging version of User.exe is installed.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool Debug {
            get {
                if ((curObj["Debug"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["Debug"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Description property provides a description of the Windows operating system. " +
            "Some user interfaces (those that allow editing of this description) limit its le" +
            "ngth to 48 characters.")]
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
        public bool IsDistributedNull {
            get {
                if ((curObj["Distributed"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Boolean indicating whether the operating system is distributed across several com" +
            "puter system nodes. If so, these nodes should be grouped as a cluster.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool Distributed {
            get {
                if ((curObj["Distributed"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["Distributed"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsEncryptionLevelNull {
            get {
                if ((curObj["EncryptionLevel"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The EncryptionLevel property specifies if the encryption level for secure transac" +
            "tions is 40-bit, 128-bit, or n-bit encryption.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public EncryptionLevelValues EncryptionLevel {
            get {
                if ((curObj["EncryptionLevel"] == null)) {
                    return ((EncryptionLevelValues)(System.Convert.ToInt32(3)));
                }
                return ((EncryptionLevelValues)(System.Convert.ToInt32(curObj["EncryptionLevel"])));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsForegroundApplicationBoostNull {
            get {
                if ((curObj["ForegroundApplicationBoost"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The ForegroundApplicationBoost property indicates the increase in priority given to the foreground application. On computer systems running Windows NT 4.0 and Windows 2000, application boost is implemented by giving an application more execution time slices (quantum lengths). A ForegroundApplicationBoost value of 0 indicates the system boosts the quantum length by 6; if 1, then 12; and if 2 then 18. On Windows NT 3.51 and earlier, application boost is implemented by increasing the scheduling priority. For these systems, the scheduling priority is increased by the value of this property. The default value is 2.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ForegroundApplicationBoostValues ForegroundApplicationBoost {
            get {
                if ((curObj["ForegroundApplicationBoost"] == null)) {
                    return ((ForegroundApplicationBoostValues)(System.Convert.ToInt32(3)));
                }
                return ((ForegroundApplicationBoostValues)(System.Convert.ToInt32(curObj["ForegroundApplicationBoost"])));
            }
            set {
                if ((ForegroundApplicationBoostValues.NULL_ENUM_VALUE == value)) {
                    curObj["ForegroundApplicationBoost"] = null;
                }
                else {
                    curObj["ForegroundApplicationBoost"] = value;
                }
                if (((isEmbedded == false)
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsFreePhysicalMemoryNull {
            get {
                if ((curObj["FreePhysicalMemory"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Number of kilobytes of physical memory currently unused and available")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong FreePhysicalMemory {
            get {
                if ((curObj["FreePhysicalMemory"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["FreePhysicalMemory"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsFreeSpaceInPagingFilesNull {
            get {
                if ((curObj["FreeSpaceInPagingFiles"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The total number of KBytes that can be mapped into the OperatingSystem\'s paging f" +
            "iles without causing any other pages to be swapped out. 0 indicates that there a" +
            "re no paging files.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong FreeSpaceInPagingFiles {
            get {
                if ((curObj["FreeSpaceInPagingFiles"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["FreeSpaceInPagingFiles"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsFreeVirtualMemoryNull {
            get {
                if ((curObj["FreeVirtualMemory"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Number of kilobytes of virtual memory currently unused and available. For example" +
            ", this may be calculated by adding the amount of free RAM to the amount of free " +
            "paging space (i.e., adding the properties, FreePhysicalMemory and FreeSpaceInPag" +
            "ingFiles).")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong FreeVirtualMemory {
            get {
                if ((curObj["FreeVirtualMemory"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["FreeVirtualMemory"]));
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
        public bool IsLargeSystemCacheNull {
            get {
                if ((curObj["LargeSystemCache"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The LargeSystemCache property indicates whether to optimize memory for applicatio" +
            "ns (value=0) or for system performance (value=1).")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public LargeSystemCacheValues LargeSystemCache {
            get {
                if ((curObj["LargeSystemCache"] == null)) {
                    return ((LargeSystemCacheValues)(System.Convert.ToInt32(2)));
                }
                return ((LargeSystemCacheValues)(System.Convert.ToInt32(curObj["LargeSystemCache"])));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsLastBootUpTimeNull {
            get {
                if ((curObj["LastBootUpTime"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Time when the operating system was last booted")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public System.DateTime LastBootUpTime {
            get {
                if ((curObj["LastBootUpTime"] != null)) {
                    return ToDateTime(((string)(curObj["LastBootUpTime"])));
                }
                else {
                    return System.DateTime.MinValue;
                }
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsLocalDateTimeNull {
            get {
                if ((curObj["LocalDateTime"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Operating system\'s notion of the local date and time of day.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public System.DateTime LocalDateTime {
            get {
                if ((curObj["LocalDateTime"] != null)) {
                    return ToDateTime(((string)(curObj["LocalDateTime"])));
                }
                else {
                    return System.DateTime.MinValue;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The Locale property indicates the language identifier used by the operating system. A language identifier is a standard international numeric abbreviation for a country or region. Each language has a unique language identifier (LANGID), a 16-bit value that consists of a primary language identifier and a secondary language identifier.")]
        public string Locale {
            get {
                return ((string)(curObj["Locale"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Manufacturer property indicates the name of the operating system manufacturer" +
            ".  For Win32 systems this value will be Microsoft Corporation.")]
        public string Manufacturer {
            get {
                return ((string)(curObj["Manufacturer"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsMaxNumberOfProcessesNull {
            get {
                if ((curObj["MaxNumberOfProcesses"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"Maximum number of process contexts the operating system can support. If there is no fixed maximum, the value should be 0. On systems that have a fixed maximum, this object can help diagnose failures that occur when the maximum is reached. If unknown, enter -1.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint MaxNumberOfProcesses {
            get {
                if ((curObj["MaxNumberOfProcesses"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["MaxNumberOfProcesses"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsMaxProcessMemorySizeNull {
            get {
                if ((curObj["MaxProcessMemorySize"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"Maximum number of kilobytes of memory that can be allocated to a process. For operating systems with no virtual memory, this value is typically equal to the total amount of physical memory minus memory used by the BIOS and OS. For some operating systems, this value may be infinity - in which case, 0 should be entered. In other cases, this value could be a constant - for example, 2G or 4G.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong MaxProcessMemorySize {
            get {
                if ((curObj["MaxProcessMemorySize"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["MaxProcessMemorySize"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The MUILanguages property indicates the MUI Languages installed in the system. \n " +
            "Example: en-us.")]
        public string[] MUILanguages {
            get {
                return ((string[])(curObj["MUILanguages"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Name of the operating system instance within a computer system.")]
        public string Name {
            get {
                return ((string)(curObj["Name"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsNumberOfLicensedUsersNull {
            get {
                if ((curObj["NumberOfLicensedUsers"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Number of user licenses for the operating system. If unlimited, enter 0. If unkno" +
            "wn, enter -1.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint NumberOfLicensedUsers {
            get {
                if ((curObj["NumberOfLicensedUsers"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["NumberOfLicensedUsers"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsNumberOfProcessesNull {
            get {
                if ((curObj["NumberOfProcesses"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Number of process contexts currently loaded or running on the operating system.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint NumberOfProcesses {
            get {
                if ((curObj["NumberOfProcesses"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["NumberOfProcesses"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsNumberOfUsersNull {
            get {
                if ((curObj["NumberOfUsers"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Number of user sessions for which the operating system is currently storing state" +
            " information")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint NumberOfUsers {
            get {
                if ((curObj["NumberOfUsers"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["NumberOfUsers"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsOperatingSystemSKUNull {
            get {
                if ((curObj["OperatingSystemSKU"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The OperatingSystemSKU property identifies the SKU of the operating system.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint OperatingSystemSKU {
            get {
                if ((curObj["OperatingSystemSKU"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["OperatingSystemSKU"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Organization property indicates the registered user\'s (of the operating syste" +
            "m) company name.\nExample: Microsoft Corporation.")]
        public string Organization {
            get {
                return ((string)(curObj["Organization"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The OSArchitecture property indicates the Architecture of the operating system.Ex" +
            "ample: 32-bit, 64-bit Intel, 64-bit AMD ")]
        public string OSArchitecture {
            get {
                return ((string)(curObj["OSArchitecture"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsOSLanguageNull {
            get {
                if ((curObj["OSLanguage"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The OSLanguage property indicates which language version of the operating system " +
            "is installed.\nExample: 0x0807 (German, Switzerland)")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint OSLanguage {
            get {
                if ((curObj["OSLanguage"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["OSLanguage"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsOSProductSuiteNull {
            get {
                if ((curObj["OSProductSuite"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The OSProductSuite property identifies installed and licensed system product addi" +
            "tions to the operating system.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public OSProductSuiteValues OSProductSuite {
            get {
                if ((curObj["OSProductSuite"] == null)) {
                    return ((OSProductSuiteValues)(System.Convert.ToInt32(256)));
                }
                return ((OSProductSuiteValues)(System.Convert.ToInt32(curObj["OSProductSuite"])));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsOSTypeNull {
            get {
                if ((curObj["OSType"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("A integer indicating the type of operating system.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public OSTypeValues OSType {
            get {
                if ((curObj["OSType"] == null)) {
                    return ((OSTypeValues)(System.Convert.ToInt32(63)));
                }
                return ((OSTypeValues)(System.Convert.ToInt32(curObj["OSType"])));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"A string describing the manufacturer and operating system type - used when the operating system property, OSType, is set to 1 (""Other""). The format of the string inserted in OtherTypeDescription should be similar in format to the Values strings defined for OSType.  OtherTypeDescription should be set to NULL when OSType is any value other than 1.")]
        public string OtherTypeDescription {
            get {
                return ((string)(curObj["OtherTypeDescription"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPAEEnabledNull {
            get {
                if ((curObj["PAEEnabled"] == null)) {
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
        public bool PAEEnabled {
            get {
                if ((curObj["PAEEnabled"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["PAEEnabled"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The PlusProductID property contains the product identification number for the Win" +
            "dows Plus! operating system enhancement software (if installed).")]
        public string PlusProductID {
            get {
                return ((string)(curObj["PlusProductID"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The PlusVersionNumber property contains the version number of the Windows Plus! o" +
            "perating system enhancement software (if installed).")]
        public string PlusVersionNumber {
            get {
                return ((string)(curObj["PlusVersionNumber"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPortableOperatingSystemNull {
            get {
                if ((curObj["PortableOperatingSystem"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The PortableOperatingSystem property indicates if theOperating System is booting from a supported locally connected storagedevice.
Values: TRUE or FALSE, A value of TRUE indicates the OperatingSystem was booted from a supported locally connected storage device.
")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool PortableOperatingSystem {
            get {
                if ((curObj["PortableOperatingSystem"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["PortableOperatingSystem"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPrimaryNull {
            get {
                if ((curObj["Primary"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Primary property determines whether this is the primary operating system.\nVal" +
            "ues: TRUE or FALSE. A value of TRUE indicates this is the primary operating syst" +
            "em.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public bool Primary {
            get {
                if ((curObj["Primary"] == null)) {
                    return System.Convert.ToBoolean(0);
                }
                return ((bool)(curObj["Primary"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsProductTypeNull {
            get {
                if ((curObj["ProductType"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The ProductType property indicates additional information about the system. This " +
            "member can be one of the following values: \n1 - Work Station \n2 - Domain Control" +
            "ler \n3 - Server")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ProductTypeValues ProductType {
            get {
                if ((curObj["ProductType"] == null)) {
                    return ((ProductTypeValues)(System.Convert.ToInt32(0)));
                }
                return ((ProductTypeValues)(System.Convert.ToInt32(curObj["ProductType"])));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The RegisteredUser property indicates the name of the registered user of the oper" +
            "ating system.\nExample: Jane Doe")]
        public string RegisteredUser {
            get {
                return ((string)(curObj["RegisteredUser"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The SerialNumber property indicates the operating system product serial identific" +
            "ation number.\nExample:10497-OEM-0031416-71674.")]
        public string SerialNumber {
            get {
                return ((string)(curObj["SerialNumber"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsServicePackMajorVersionNull {
            get {
                if ((curObj["ServicePackMajorVersion"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The ServicePackMajorVersion property indicates the major version number of the service pack installed on the computer system. If no service pack has been installed, the value is zero. ServicePackMajorVersion is valid for computers running Windows 2000 and later (NULL otherwise).")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort ServicePackMajorVersion {
            get {
                if ((curObj["ServicePackMajorVersion"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["ServicePackMajorVersion"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsServicePackMinorVersionNull {
            get {
                if ((curObj["ServicePackMinorVersion"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The ServicePackMinorVersion property indicates the minor version number of the service pack installed on the computer system. If no service pack has been installed, the value is zero. ServicePackMinorVersion is valid for computers running Windows 2000 and later (NULL otherwise).")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort ServicePackMinorVersion {
            get {
                if ((curObj["ServicePackMinorVersion"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["ServicePackMinorVersion"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsSizeStoredInPagingFilesNull {
            get {
                if ((curObj["SizeStoredInPagingFiles"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The total number of kilobytes that can be stored in the operating system\'s paging" +
            " files. Note that this number does not represent the actual physical size of the" +
            " paging file on disk.  0 indicates that there are no paging files.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong SizeStoredInPagingFiles {
            get {
                if ((curObj["SizeStoredInPagingFiles"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["SizeStoredInPagingFiles"]));
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
        public bool IsSuiteMaskNull {
            get {
                if ((curObj["SuiteMask"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"The SuiteMask property indicates a set of bit flags that identify the product suites available on the system. This member can be a combination of the following values:
0 - Windows Server 2003, Small Business Edition
1 - Windows Server 2003, Enterprise Edition
2 - Windows Server 2003, Backoffice Edition
3 - Windows Server 2003, Communications Edition
4 - Microsoft Terminal Services
5 - Windows Server 2003, Small Business Edition Restricted
6 - Windows XP Embedded
7 - Windows Server 2003, Datacenter Edition
8 - Single User
9 - Windows XP Home Edition
10 - Windows Server 2003, Web Edition")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public SuiteMaskValues SuiteMask {
            get {
                if ((curObj["SuiteMask"] == null)) {
                    return ((SuiteMaskValues)(System.Convert.ToInt32(20)));
                }
                return ((SuiteMaskValues)(System.Convert.ToInt32(curObj["SuiteMask"])));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The SystemDevice property indicates the physical disk partition the operating sys" +
            "tem is installed on.")]
        public string SystemDevice {
            get {
                return ((string)(curObj["SystemDevice"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The SystemDirectory property indicates the system directory of the operating syst" +
            "em.\nExample: C:\\WINDOWS\\SYSTEM32")]
        public string SystemDirectory {
            get {
                return ((string)(curObj["SystemDirectory"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The SystemDrive property contains the letter of the disk drive that the operating" +
            " system resides on.\nExample: C:")]
        public string SystemDrive {
            get {
                return ((string)(curObj["SystemDrive"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsTotalSwapSpaceSizeNull {
            get {
                if ((curObj["TotalSwapSpaceSize"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"Total swap space in kilobytes. This value may be NULL (unspecified) if swap space is not distinguished from page files.  However, some operating systems distinguish these concepts.  For example, in UNIX, whole processes can be 'swapped out' when the free page list falls and remains below a specified amount.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong TotalSwapSpaceSize {
            get {
                if ((curObj["TotalSwapSpaceSize"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["TotalSwapSpaceSize"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsTotalVirtualMemorySizeNull {
            get {
                if ((curObj["TotalVirtualMemorySize"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Number of kilobytes of virtual memory. For example, this may be calculated by add" +
            "ing the amount of total RAM to the amount of paging space (i.e., adding the amou" +
            "nt of memory in/aggregated by the computer system to the property, SizeStoredInP" +
            "agingFiles.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong TotalVirtualMemorySize {
            get {
                if ((curObj["TotalVirtualMemorySize"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["TotalVirtualMemorySize"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsTotalVisibleMemorySizeNull {
            get {
                if ((curObj["TotalVisibleMemorySize"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The total amount of physical memory (in Kbytes) available to the OperatingSystem." +
            " This value does not necessarily indicate the true amount of physical memory, bu" +
            "t what is reported to the OperatingSystem as available to it.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong TotalVisibleMemorySize {
            get {
                if ((curObj["TotalVisibleMemorySize"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["TotalVisibleMemorySize"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Version property indicates the version number of the operating system.\nExampl" +
            "e: 4.0")]
        public string Version {
            get {
                return ((string)(curObj["Version"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The WindowsDirectory property indicates the Windows directory of the operating sy" +
            "stem.\nExample: C:\\WINDOWS")]
        public string WindowsDirectory {
            get {
                return ((string)(curObj["WindowsDirectory"]));
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

        private bool ShouldSerializeCurrentTimeZone() {
            if ((this.IsCurrentTimeZoneNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeDataExecutionPrevention_32BitApplications() {
            if ((this.IsDataExecutionPrevention_32BitApplicationsNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeDataExecutionPrevention_Available() {
            if ((this.IsDataExecutionPrevention_AvailableNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeDataExecutionPrevention_Drivers() {
            if ((this.IsDataExecutionPrevention_DriversNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeDataExecutionPrevention_SupportPolicy() {
            if ((this.IsDataExecutionPrevention_SupportPolicyNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeDebug() {
            if ((this.IsDebugNull == false)) {
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

        private bool ShouldSerializeDistributed() {
            if ((this.IsDistributedNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeEncryptionLevel() {
            if ((this.IsEncryptionLevelNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeForegroundApplicationBoost() {
            if ((this.IsForegroundApplicationBoostNull == false)) {
                return true;
            }
            return false;
        }

        private void ResetForegroundApplicationBoost() {
            curObj["ForegroundApplicationBoost"] = null;
            if (((isEmbedded == false)
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }

        private bool ShouldSerializeFreePhysicalMemory() {
            if ((this.IsFreePhysicalMemoryNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeFreeSpaceInPagingFiles() {
            if ((this.IsFreeSpaceInPagingFilesNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeFreeVirtualMemory() {
            if ((this.IsFreeVirtualMemoryNull == false)) {
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

        private bool ShouldSerializeLargeSystemCache() {
            if ((this.IsLargeSystemCacheNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeLastBootUpTime() {
            if ((this.IsLastBootUpTimeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeLocalDateTime() {
            if ((this.IsLocalDateTimeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeMaxNumberOfProcesses() {
            if ((this.IsMaxNumberOfProcessesNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeMaxProcessMemorySize() {
            if ((this.IsMaxProcessMemorySizeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeNumberOfLicensedUsers() {
            if ((this.IsNumberOfLicensedUsersNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeNumberOfProcesses() {
            if ((this.IsNumberOfProcessesNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeNumberOfUsers() {
            if ((this.IsNumberOfUsersNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeOperatingSystemSKU() {
            if ((this.IsOperatingSystemSKUNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeOSLanguage() {
            if ((this.IsOSLanguageNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeOSProductSuite() {
            if ((this.IsOSProductSuiteNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeOSType() {
            if ((this.IsOSTypeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePAEEnabled() {
            if ((this.IsPAEEnabledNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePortableOperatingSystem() {
            if ((this.IsPortableOperatingSystemNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePrimary() {
            if ((this.IsPrimaryNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeProductType() {
            if ((this.IsProductTypeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeServicePackMajorVersion() {
            if ((this.IsServicePackMajorVersionNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeServicePackMinorVersion() {
            if ((this.IsServicePackMinorVersionNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeSizeStoredInPagingFiles() {
            if ((this.IsSizeStoredInPagingFilesNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeSuiteMask() {
            if ((this.IsSuiteMaskNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeTotalSwapSpaceSize() {
            if ((this.IsTotalSwapSpaceSizeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeTotalVirtualMemorySize() {
            if ((this.IsTotalVirtualMemorySizeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeTotalVisibleMemorySize() {
            if ((this.IsTotalVisibleMemorySizeNull == false)) {
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
            return "ROOT\\CIMV2:Win32_OperatingSystem=@";
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

        [Browsable(true)]
        public static OperatingSystem0 CreateInstance() {
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
            return new OperatingSystem0(tmpMgmtClass.CreateInstance());
        }

        [Browsable(true)]
        public void Delete() {
            PrivateLateBoundObject.Delete();
        }

        public uint Reboot() {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                bool EnablePrivileges = PrivateLateBoundObject.Scope.Options.EnablePrivileges;
                PrivateLateBoundObject.Scope.Options.EnablePrivileges = true;
                System.Management.ManagementBaseObject outParams = PrivateLateBoundObject.InvokeMethod("Reboot", inParams, null);
                PrivateLateBoundObject.Scope.Options.EnablePrivileges = EnablePrivileges;
                return System.Convert.ToUInt32(outParams.Properties["ReturnValue"].Value);
            }
            else {
                return System.Convert.ToUInt32(0);
            }
        }

        public uint SetDateTime(System.DateTime LocalDateTime) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                bool EnablePrivileges = PrivateLateBoundObject.Scope.Options.EnablePrivileges;
                PrivateLateBoundObject.Scope.Options.EnablePrivileges = true;
                inParams = PrivateLateBoundObject.GetMethodParameters("SetDateTime");
                inParams["LocalDateTime"] = ToDmtfDateTime(((System.DateTime)(LocalDateTime)));
                System.Management.ManagementBaseObject outParams = PrivateLateBoundObject.InvokeMethod("SetDateTime", inParams, null);
                PrivateLateBoundObject.Scope.Options.EnablePrivileges = EnablePrivileges;
                return System.Convert.ToUInt32(outParams.Properties["ReturnValue"].Value);
            }
            else {
                return System.Convert.ToUInt32(0);
            }
        }

        public uint Shutdown() {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                bool EnablePrivileges = PrivateLateBoundObject.Scope.Options.EnablePrivileges;
                PrivateLateBoundObject.Scope.Options.EnablePrivileges = true;
                System.Management.ManagementBaseObject outParams = PrivateLateBoundObject.InvokeMethod("Shutdown", inParams, null);
                PrivateLateBoundObject.Scope.Options.EnablePrivileges = EnablePrivileges;
                return System.Convert.ToUInt32(outParams.Properties["ReturnValue"].Value);
            }
            else {
                return System.Convert.ToUInt32(0);
            }
        }

        public uint Win32Shutdown(int Flags, int Reserved) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                bool EnablePrivileges = PrivateLateBoundObject.Scope.Options.EnablePrivileges;
                PrivateLateBoundObject.Scope.Options.EnablePrivileges = true;
                inParams = PrivateLateBoundObject.GetMethodParameters("Win32Shutdown");
                inParams["Flags"] = ((int)(Flags));
                inParams["Reserved"] = ((int)(Reserved));
                System.Management.ManagementBaseObject outParams = PrivateLateBoundObject.InvokeMethod("Win32Shutdown", inParams, null);
                PrivateLateBoundObject.Scope.Options.EnablePrivileges = EnablePrivileges;
                return System.Convert.ToUInt32(outParams.Properties["ReturnValue"].Value);
            }
            else {
                return System.Convert.ToUInt32(0);
            }
        }

        public uint Win32ShutdownTracker(string Comment, int Flags, uint ReasonCode, uint Timeout) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                bool EnablePrivileges = PrivateLateBoundObject.Scope.Options.EnablePrivileges;
                PrivateLateBoundObject.Scope.Options.EnablePrivileges = true;
                inParams = PrivateLateBoundObject.GetMethodParameters("Win32ShutdownTracker");
                inParams["Comment"] = ((string)(Comment));
                inParams["Flags"] = ((int)(Flags));
                inParams["ReasonCode"] = ((uint)(ReasonCode));
                inParams["Timeout"] = ((uint)(Timeout));
                System.Management.ManagementBaseObject outParams = PrivateLateBoundObject.InvokeMethod("Win32ShutdownTracker", inParams, null);
                PrivateLateBoundObject.Scope.Options.EnablePrivileges = EnablePrivileges;
                return System.Convert.ToUInt32(outParams.Properties["ReturnValue"].Value);
            }
            else {
                return System.Convert.ToUInt32(0);
            }
        }

        public enum DataExecutionPrevention_SupportPolicyValues {

            Always_Off = 0,

            Always_On = 1,

            Opt_In = 2,

            Opt_Out = 3,

            NULL_ENUM_VALUE = 4,
        }

        public enum EncryptionLevelValues {

            Val_40_bit = 0,

            Val_128_bit = 1,

            N_bit = 2,

            NULL_ENUM_VALUE = 3,
        }

        public enum ForegroundApplicationBoostValues {

            None = 0,

            Minimum = 1,

            Maximum = 2,

            NULL_ENUM_VALUE = 3,
        }

        public enum LargeSystemCacheValues {

            Optimize_for_Applications = 0,

            Optimize_for_System_Performance = 1,

            NULL_ENUM_VALUE = 2,
        }

        public enum OSProductSuiteValues {

            Small_Business = 1,

            Enterprise = 2,

            BackOffice = 4,

            Communication_Server = 8,

            Terminal_Server = 16,

            Small_Business_Restricted_ = 32,

            Embedded_NT = 64,

            Data_Center = 128,

            NULL_ENUM_VALUE = 256,
        }

        public enum OSTypeValues {

            Unknown0 = 0,

            Other0 = 1,

            MACOS = 2,

            ATTUNIX = 3,

            DGUX = 4,

            DECNT = 5,

            Digital_Unix = 6,

            OpenVMS = 7,

            HPUX = 8,

            AIX = 9,

            MVS = 10,

            OS400 = 11,

            OS_2 = 12,

            JavaVM = 13,

            MSDOS = 14,

            WIN3x = 15,

            WIN95 = 16,

            WIN98 = 17,

            WINNT = 18,

            WINCE = 19,

            NCR3000 = 20,

            NetWare = 21,

            OSF = 22,

            DC_OS = 23,

            Reliant_UNIX = 24,

            SCO_UnixWare = 25,

            SCO_OpenServer = 26,

            Sequent = 27,

            IRIX = 28,

            Solaris = 29,

            SunOS = 30,

            U6000 = 31,

            ASERIES = 32,

            TandemNSK = 33,

            TandemNT = 34,

            BS2000 = 35,

            LINUX = 36,

            Lynx = 37,

            XENIX = 38,

            VM_ESA = 39,

            Interactive_UNIX = 40,

            BSDUNIX = 41,

            FreeBSD = 42,

            NetBSD = 43,

            GNU_Hurd = 44,

            OS9 = 45,

            MACH_Kernel = 46,

            Inferno = 47,

            QNX = 48,

            EPOC = 49,

            IxWorks = 50,

            VxWorks = 51,

            MiNT = 52,

            BeOS = 53,

            HP_MPE = 54,

            NextStep = 55,

            PalmPilot = 56,

            Rhapsody = 57,

            Windows_2000 = 58,

            Dedicated = 59,

            OS_390 = 60,

            VSE = 61,

            TPF = 62,

            NULL_ENUM_VALUE = 63,
        }

        public enum ProductTypeValues {

            Work_Station = 1,

            Domain_Controller = 2,

            Server = 3,

            NULL_ENUM_VALUE = 0,
        }

        public enum SuiteMaskValues {

            Windows_Server_2003_Small_Business_Edition = 0,

            Windows_Server_2003_Enterprise_Edition = 1,

            Windows_Server_2003_Backoffice_Edition = 2,

            Windows_Server_2003_Communications_Edition = 3,

            Microsoft_Terminal_Services = 4,

            Windows_Server_2003_Small_Business_Edition_Restricted = 5,

            Windows_XP_Embedded = 6,

            Windows_Server_2003_Datacenter_Edition = 7,

            Single_User = 8,

            Windows_XP_Home_Edition = 9,

            Windows_Server_2003_Web_Edition = 10,

            NULL_ENUM_VALUE = 20,
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
