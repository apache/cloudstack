namespace CloudStack.Plugin.WmiWrappers.ROOT.VIRTUALIZATION {
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
    // Time interval functions  ToTimeSpan and ToDmtfTimeInterval are added to the class to convert DMTF Time Interval to  System.TimeSpan and vice-versa.
    // An Early Bound class generated for the WMI class.Msvm_VirtualSwitch
    public class VirtualSwitch : System.ComponentModel.Component {
        
        // Private property to hold the WMI namespace in which the class resides.
        private static string CreatedWmiNamespace = "ROOT\\virtualization";
        
        // Private property to hold the name of WMI class which created this class.
        public static string CreatedClassName = "Msvm_VirtualSwitch";
        
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
        public VirtualSwitch() {
            this.InitializeObject(null, null, null);
        }
        
        public VirtualSwitch(string keyCreationClassName, string keyName) {
            this.InitializeObject(null, new System.Management.ManagementPath(VirtualSwitch.ConstructPath(keyCreationClassName, keyName)), null);
        }
        
        public VirtualSwitch(System.Management.ManagementScope mgmtScope, string keyCreationClassName, string keyName) {
            this.InitializeObject(((System.Management.ManagementScope)(mgmtScope)), new System.Management.ManagementPath(VirtualSwitch.ConstructPath(keyCreationClassName, keyName)), null);
        }
        
        public VirtualSwitch(System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(null, path, getOptions);
        }
        
        public VirtualSwitch(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path) {
            this.InitializeObject(mgmtScope, path, null);
        }
        
        public VirtualSwitch(System.Management.ManagementPath path) {
            this.InitializeObject(null, path, null);
        }
        
        public VirtualSwitch(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(mgmtScope, path, getOptions);
        }
        
        public VirtualSwitch(System.Management.ManagementObject theObject) {
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
        
        public VirtualSwitch(System.Management.ManagementBaseObject theObject) {
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
                return "ROOT\\virtualization";
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
        public string Caption {
            get {
                return ((string)(curObj["Caption"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string CreationClassName {
            get {
                return ((string)(curObj["CreationClassName"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public ushort[] Dedicated {
            get {
                return ((ushort[])(curObj["Dedicated"]));
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
        public bool IsEnabledDefaultNull {
            get {
                if ((curObj["EnabledDefault"] == null)) {
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
        public ushort EnabledDefault {
            get {
                if ((curObj["EnabledDefault"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["EnabledDefault"]));
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
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort EnabledState {
            get {
                if ((curObj["EnabledState"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["EnabledState"]));
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
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort HealthState {
            get {
                if ((curObj["HealthState"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["HealthState"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string[] IdentifyingDescriptions {
            get {
                return ((string[])(curObj["IdentifyingDescriptions"]));
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
        public bool IsMaxChimneyOffloadsNull {
            get {
                if ((curObj["MaxChimneyOffloads"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The maximum number of Chimney offloads allowed for a port on this switch.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint MaxChimneyOffloads {
            get {
                if ((curObj["MaxChimneyOffloads"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["MaxChimneyOffloads"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsMaxVMQOffloadsNull {
            get {
                if ((curObj["MaxVMQOffloads"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The maximum number of VM queue (VMQ) offloads allowed for a port on this switch.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint MaxVMQOffloads {
            get {
                if ((curObj["MaxVMQOffloads"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["MaxVMQOffloads"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string Name {
            get {
                return ((string)(curObj["Name"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string NameFormat {
            get {
                return ((string)(curObj["NameFormat"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsNumLearnableAddressesNull {
            get {
                if ((curObj["NumLearnableAddresses"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The number of learnable addresses for this switch.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint NumLearnableAddresses {
            get {
                if ((curObj["NumLearnableAddresses"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["NumLearnableAddresses"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public ushort[] OperationalStatus {
            get {
                return ((ushort[])(curObj["OperationalStatus"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string[] OtherDedicatedDescriptions {
            get {
                return ((string[])(curObj["OtherDedicatedDescriptions"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string OtherEnabledState {
            get {
                return ((string)(curObj["OtherEnabledState"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string[] OtherIdentifyingInfo {
            get {
                return ((string[])(curObj["OtherIdentifyingInfo"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public ushort[] PowerManagementCapabilities {
            get {
                return ((ushort[])(curObj["PowerManagementCapabilities"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string PrimaryOwnerContact {
            get {
                return ((string)(curObj["PrimaryOwnerContact"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string PrimaryOwnerName {
            get {
                return ((string)(curObj["PrimaryOwnerName"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsRequestedStateNull {
            get {
                if ((curObj["RequestedState"] == null)) {
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
        public ushort RequestedState {
            get {
                if ((curObj["RequestedState"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["RequestedState"]));
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
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ushort ResetCapability {
            get {
                if ((curObj["ResetCapability"] == null)) {
                    return System.Convert.ToUInt16(0);
                }
                return ((ushort)(curObj["ResetCapability"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string[] Roles {
            get {
                return ((string[])(curObj["Roles"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The authorization manager scope for the switch service.")]
        public string ScopeOfResidence {
            get {
                return ((string)(curObj["ScopeOfResidence"]));
            }
            set {
                curObj["ScopeOfResidence"] = value;
                if (((isEmbedded == false) 
                            && (AutoCommitProp == true))) {
                    PrivateLateBoundObject.Put();
                }
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string Status {
            get {
                return ((string)(curObj["Status"]));
            }
        }
        
        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public string[] StatusDescriptions {
            get {
                return ((string[])(curObj["StatusDescriptions"]));
            }
        }
        
        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsTimeOfLastStateChangeNull {
            get {
                if ((curObj["TimeOfLastStateChange"] == null)) {
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
        public System.DateTime TimeOfLastStateChange {
            get {
                if ((curObj["TimeOfLastStateChange"] != null)) {
                    return ToDateTime(((string)(curObj["TimeOfLastStateChange"])));
                }
                else {
                    return System.DateTime.MinValue;
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
        
        private bool ShouldSerializeEnabledDefault() {
            if ((this.IsEnabledDefaultNull == false)) {
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
        
        private bool ShouldSerializeMaxChimneyOffloads() {
            if ((this.IsMaxChimneyOffloadsNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeMaxVMQOffloads() {
            if ((this.IsMaxVMQOffloadsNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeNumLearnableAddresses() {
            if ((this.IsNumLearnableAddressesNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeRequestedState() {
            if ((this.IsRequestedStateNull == false)) {
                return true;
            }
            return false;
        }
        
        private bool ShouldSerializeResetCapability() {
            if ((this.IsResetCapabilityNull == false)) {
                return true;
            }
            return false;
        }
        
        private void ResetScopeOfResidence() {
            curObj["ScopeOfResidence"] = null;
            if (((isEmbedded == false) 
                        && (AutoCommitProp == true))) {
                PrivateLateBoundObject.Put();
            }
        }
        
        private bool ShouldSerializeTimeOfLastStateChange() {
            if ((this.IsTimeOfLastStateChangeNull == false)) {
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
        
        private static string ConstructPath(string keyCreationClassName, string keyName) {
            string strPath = "ROOT\\virtualization:Msvm_VirtualSwitch";
            strPath = string.Concat(strPath, string.Concat(".CreationClassName=", string.Concat("\"", string.Concat(keyCreationClassName, "\""))));
            strPath = string.Concat(strPath, string.Concat(",Name=", string.Concat("\"", string.Concat(keyName, "\""))));
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
        public static VirtualSwitchCollection GetInstances() {
            return GetInstances(null, null, null);
        }
        
        public static VirtualSwitchCollection GetInstances(string condition) {
            return GetInstances(null, condition, null);
        }
        
        public static VirtualSwitchCollection GetInstances(string[] selectedProperties) {
            return GetInstances(null, null, selectedProperties);
        }
        
        public static VirtualSwitchCollection GetInstances(string condition, string[] selectedProperties) {
            return GetInstances(null, condition, selectedProperties);
        }
        
        public static VirtualSwitchCollection GetInstances(System.Management.ManagementScope mgmtScope, System.Management.EnumerationOptions enumOptions) {
            if ((mgmtScope == null)) {
                if ((statMgmtScope == null)) {
                    mgmtScope = new System.Management.ManagementScope();
                    mgmtScope.Path.NamespacePath = "root\\virtualization";
                }
                else {
                    mgmtScope = statMgmtScope;
                }
            }
            System.Management.ManagementPath pathObj = new System.Management.ManagementPath();
            pathObj.ClassName = "Msvm_VirtualSwitch";
            pathObj.NamespacePath = "root\\virtualization";
            System.Management.ManagementClass clsObject = new System.Management.ManagementClass(mgmtScope, pathObj, null);
            if ((enumOptions == null)) {
                enumOptions = new System.Management.EnumerationOptions();
                enumOptions.EnsureLocatable = true;
            }
            return new VirtualSwitchCollection(clsObject.GetInstances(enumOptions));
        }
        
        public static VirtualSwitchCollection GetInstances(System.Management.ManagementScope mgmtScope, string condition) {
            return GetInstances(mgmtScope, condition, null);
        }
        
        public static VirtualSwitchCollection GetInstances(System.Management.ManagementScope mgmtScope, string[] selectedProperties) {
            return GetInstances(mgmtScope, null, selectedProperties);
        }
        
        public static VirtualSwitchCollection GetInstances(System.Management.ManagementScope mgmtScope, string condition, string[] selectedProperties) {
            if ((mgmtScope == null)) {
                if ((statMgmtScope == null)) {
                    mgmtScope = new System.Management.ManagementScope();
                    mgmtScope.Path.NamespacePath = "root\\virtualization";
                }
                else {
                    mgmtScope = statMgmtScope;
                }
            }
            System.Management.ManagementObjectSearcher ObjectSearcher = new System.Management.ManagementObjectSearcher(mgmtScope, new SelectQuery("Msvm_VirtualSwitch", condition, selectedProperties));
            System.Management.EnumerationOptions enumOptions = new System.Management.EnumerationOptions();
            enumOptions.EnsureLocatable = true;
            ObjectSearcher.Options = enumOptions;
            return new VirtualSwitchCollection(ObjectSearcher.Get());
        }
        
        [Browsable(true)]
        public static VirtualSwitch CreateInstance() {
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
            return new VirtualSwitch(tmpMgmtClass.CreateInstance());
        }
        
        [Browsable(true)]
        public void Delete() {
            PrivateLateBoundObject.Delete();
        }
        
        // Converts a given time interval in DMTF format to System.TimeSpan object.
        static System.TimeSpan ToTimeSpan(string dmtfTimespan) {
            int days = 0;
            int hours = 0;
            int minutes = 0;
            int seconds = 0;
            long ticks = 0;
            if ((dmtfTimespan == null)) {
                throw new System.ArgumentOutOfRangeException();
            }
            if ((dmtfTimespan.Length == 0)) {
                throw new System.ArgumentOutOfRangeException();
            }
            if ((dmtfTimespan.Length != 25)) {
                throw new System.ArgumentOutOfRangeException();
            }
            if ((dmtfTimespan.Substring(21, 4) != ":000")) {
                throw new System.ArgumentOutOfRangeException();
            }
            try {
                string tempString = string.Empty;
                tempString = dmtfTimespan.Substring(0, 8);
                days = int.Parse(tempString);
                tempString = dmtfTimespan.Substring(8, 2);
                hours = int.Parse(tempString);
                tempString = dmtfTimespan.Substring(10, 2);
                minutes = int.Parse(tempString);
                tempString = dmtfTimespan.Substring(12, 2);
                seconds = int.Parse(tempString);
                tempString = dmtfTimespan.Substring(15, 6);
                ticks = (long.Parse(tempString) * ((long)((System.TimeSpan.TicksPerMillisecond / 1000))));
            }
            catch (System.Exception e) {
                throw new System.ArgumentOutOfRangeException(null, e.Message);
            }
            System.TimeSpan timespan = new System.TimeSpan(days, hours, minutes, seconds, 0);
            System.TimeSpan tsTemp = System.TimeSpan.FromTicks(ticks);
            timespan = timespan.Add(tsTemp);
            return timespan;
        }
        
        // Converts a given System.TimeSpan object to DMTF Time interval format.
        static string ToDmtfTimeInterval(System.TimeSpan timespan) {
            string dmtftimespan = ((int)(timespan.Days)).ToString().PadLeft(8, '0');
            System.TimeSpan maxTimeSpan = System.TimeSpan.MaxValue;
            if ((timespan.Days > maxTimeSpan.Days)) {
                throw new System.ArgumentOutOfRangeException();
            }
            System.TimeSpan minTimeSpan = System.TimeSpan.MinValue;
            if ((timespan.Days < minTimeSpan.Days)) {
                throw new System.ArgumentOutOfRangeException();
            }
            dmtftimespan = string.Concat(dmtftimespan, ((int)(timespan.Hours)).ToString().PadLeft(2, '0'));
            dmtftimespan = string.Concat(dmtftimespan, ((int)(timespan.Minutes)).ToString().PadLeft(2, '0'));
            dmtftimespan = string.Concat(dmtftimespan, ((int)(timespan.Seconds)).ToString().PadLeft(2, '0'));
            dmtftimespan = string.Concat(dmtftimespan, ".");
            System.TimeSpan tsTemp = new System.TimeSpan(timespan.Days, timespan.Hours, timespan.Minutes, timespan.Seconds, 0);
            long microsec = ((long)((((timespan.Ticks - tsTemp.Ticks) 
                        * 1000) 
                        / System.TimeSpan.TicksPerMillisecond)));
            string strMicroSec = ((long)(microsec)).ToString();
            if ((strMicroSec.Length > 6)) {
                strMicroSec = strMicroSec.Substring(0, 6);
            }
            dmtftimespan = string.Concat(dmtftimespan, strMicroSec.PadLeft(6, '0'));
            dmtftimespan = string.Concat(dmtftimespan, ":000");
            return dmtftimespan;
        }
        
        public uint RequestStateChange(ushort RequestedState, System.TimeSpan TimeoutPeriod, out System.Management.ManagementPath Job) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("RequestStateChange");
                inParams["RequestedState"] = ((ushort)(RequestedState));
                inParams["TimeoutPeriod"] = ToDmtfTimeInterval(((System.TimeSpan)(TimeoutPeriod)));
                System.Management.ManagementBaseObject outParams = PrivateLateBoundObject.InvokeMethod("RequestStateChange", inParams, null);
                Job = null;
                if ((outParams.Properties["Job"] != null)) {
                    Job = new System.Management.ManagementPath(outParams.Properties["Job"].ToString());
                }
                return System.Convert.ToUInt32(outParams.Properties["ReturnValue"].Value);
            }
            else {
                Job = null;
                return System.Convert.ToUInt32(0);
            }
        }
        
        public uint SetPowerState(uint PowerState, System.DateTime Time) {
            if ((isEmbedded == false)) {
                System.Management.ManagementBaseObject inParams = null;
                inParams = PrivateLateBoundObject.GetMethodParameters("SetPowerState");
                inParams["PowerState"] = ((uint)(PowerState));
                inParams["Time"] = ToDmtfDateTime(((System.DateTime)(Time)));
                System.Management.ManagementBaseObject outParams = PrivateLateBoundObject.InvokeMethod("SetPowerState", inParams, null);
                return System.Convert.ToUInt32(outParams.Properties["ReturnValue"].Value);
            }
            else {
                return System.Convert.ToUInt32(0);
            }
        }
        
        // Enumerator implementation for enumerating instances of the class.
        public class VirtualSwitchCollection : object, ICollection {
            
            private ManagementObjectCollection privColObj;
            
            public VirtualSwitchCollection(ManagementObjectCollection objCollection) {
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
                    array.SetValue(new VirtualSwitch(((System.Management.ManagementObject)(array.GetValue(nCtr)))), nCtr);
                }
            }
            
            public virtual System.Collections.IEnumerator GetEnumerator() {
                return new VirtualSwitchEnumerator(privColObj.GetEnumerator());
            }
            
            public class VirtualSwitchEnumerator : object, System.Collections.IEnumerator {
                
                private ManagementObjectCollection.ManagementObjectEnumerator privObjEnum;
                
                public VirtualSwitchEnumerator(ManagementObjectCollection.ManagementObjectEnumerator objEnum) {
                    privObjEnum = objEnum;
                }
                
                public virtual object Current {
                    get {
                        return new VirtualSwitch(((System.Management.ManagementObject)(privObjEnum.Current)));
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
