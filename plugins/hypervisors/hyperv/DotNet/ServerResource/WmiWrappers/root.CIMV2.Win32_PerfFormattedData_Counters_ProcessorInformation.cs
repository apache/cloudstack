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
    // An Early Bound class generated for the WMI class.Win32_PerfFormattedData_Counters_ProcessorInformation
    public class PerfFormattedData_Counters_ProcessorInformation : System.ComponentModel.Component {

        // Private property to hold the WMI namespace in which the class resides.
        private static string CreatedWmiNamespace = "root\\CIMV2";

        // Private property to hold the name of WMI class which created this class.
        private static string CreatedClassName = "Win32_PerfFormattedData_Counters_ProcessorInformation";

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
        public PerfFormattedData_Counters_ProcessorInformation() {
            this.InitializeObject(null, null, null);
        }

        public PerfFormattedData_Counters_ProcessorInformation(string keyName) {
            this.InitializeObject(null, new System.Management.ManagementPath(PerfFormattedData_Counters_ProcessorInformation.ConstructPath(keyName)), null);
        }

        public PerfFormattedData_Counters_ProcessorInformation(System.Management.ManagementScope mgmtScope, string keyName) {
            this.InitializeObject(((System.Management.ManagementScope)(mgmtScope)), new System.Management.ManagementPath(PerfFormattedData_Counters_ProcessorInformation.ConstructPath(keyName)), null);
        }

        public PerfFormattedData_Counters_ProcessorInformation(System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(null, path, getOptions);
        }

        public PerfFormattedData_Counters_ProcessorInformation(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path) {
            this.InitializeObject(mgmtScope, path, null);
        }

        public PerfFormattedData_Counters_ProcessorInformation(System.Management.ManagementPath path) {
            this.InitializeObject(null, path, null);
        }

        public PerfFormattedData_Counters_ProcessorInformation(System.Management.ManagementScope mgmtScope, System.Management.ManagementPath path, System.Management.ObjectGetOptions getOptions) {
            this.InitializeObject(mgmtScope, path, getOptions);
        }

        public PerfFormattedData_Counters_ProcessorInformation(System.Management.ManagementObject theObject) {
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

        public PerfFormattedData_Counters_ProcessorInformation(System.Management.ManagementBaseObject theObject) {
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
                return "root\\CIMV2";
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
        public bool IsAverageIdleTimeNull {
            get {
                if ((curObj["AverageIdleTime"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Average Idle Time is the average idle duration in 100ns units observed between th" +
            "e last two samples.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong AverageIdleTime {
            get {
                if ((curObj["AverageIdleTime"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["AverageIdleTime"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsC1TransitionsPersecNull {
            get {
                if ((curObj["C1TransitionsPersec"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"C1 Transitions/sec is the rate that the CPU enters the C1 low-power idle state. The CPU enters the C1 state when it is sufficiently idle and exits this state on any interrupt. This counter displays the difference between the values observed in the last two samples, divided by the duration of the sample interval.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong C1TransitionsPersec {
            get {
                if ((curObj["C1TransitionsPersec"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["C1TransitionsPersec"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsC2TransitionsPersecNull {
            get {
                if ((curObj["C2TransitionsPersec"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"C2 Transitions/sec is the rate that the CPU enters the C2 low-power idle state. The CPU enters the C2 state when it is sufficiently idle and exits this state on any interrupt. This counter displays the difference between the values observed in the last two samples, divided by the duration of the sample interval.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong C2TransitionsPersec {
            get {
                if ((curObj["C2TransitionsPersec"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["C2TransitionsPersec"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsC3TransitionsPersecNull {
            get {
                if ((curObj["C3TransitionsPersec"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"C3 Transitions/sec is the rate that the CPU enters the C3 low-power idle state. The CPU enters the C3 state when it is sufficiently idle and exits this state on any interrupt. This counter displays the difference between the values observed in the last two samples, divided by the duration of the sample interval.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong C3TransitionsPersec {
            get {
                if ((curObj["C3TransitionsPersec"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["C3TransitionsPersec"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("A short textual description (one-line string) for the statistic or metric.")]
        public string Caption {
            get {
                return ((string)(curObj["Caption"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsClockInterruptsPersecNull {
            get {
                if ((curObj["ClockInterruptsPersec"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"Clock Interrupts/sec is the average rate, in incidents per second, at which the processor received and serviced clock tick interrupts. This counter displays the difference between the values observed in the last two samples, divided by the duration of the sample interval.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint ClockInterruptsPersec {
            get {
                if ((curObj["ClockInterruptsPersec"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["ClockInterruptsPersec"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("A textual description of the statistic or metric.")]
        public string Description {
            get {
                return ((string)(curObj["Description"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsDPCRateNull {
            get {
                if ((curObj["DPCRate"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"DPC Rate is the rate at which deferred procedure calls (DPCs) were added to the processors DPC queues between the timer ticks of the processor clock. DPCs are interrupts that run at alower priority than standard interrupts.  Each processor has its own DPC queue. This counter measures the rate that DPCs were added to the queue, not the number of DPCs in the queue. This counter displays the last observed value only; it is not an average.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint DPCRate {
            get {
                if ((curObj["DPCRate"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["DPCRate"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsDPCsQueuedPersecNull {
            get {
                if ((curObj["DPCsQueuedPersec"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"DPCs Queued/sec is the average rate, in incidents per second, at which deferred procedure calls (DPCs) were added to the processor's DPC queue. DPCs are interrupts that run at a lower priority than standard interrupts.  Each processor has its own DPC queue. This counter measures the rate that DPCs are added to the queue, not the number of DPCs in the queue.  This counter displays the difference between the values observed in the last two samples, divided by the duration of the sample interval.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint DPCsQueuedPersec {
            get {
                if ((curObj["DPCsQueuedPersec"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["DPCsQueuedPersec"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsFrequency_ObjectNull {
            get {
                if ((curObj["Frequency_Object"] == null)) {
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
        public ulong Frequency_Object {
            get {
                if ((curObj["Frequency_Object"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["Frequency_Object"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsFrequency_PerfTimeNull {
            get {
                if ((curObj["Frequency_PerfTime"] == null)) {
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
        public ulong Frequency_PerfTime {
            get {
                if ((curObj["Frequency_PerfTime"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["Frequency_PerfTime"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsFrequency_Sys100NSNull {
            get {
                if ((curObj["Frequency_Sys100NS"] == null)) {
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
        public ulong Frequency_Sys100NS {
            get {
                if ((curObj["Frequency_Sys100NS"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["Frequency_Sys100NS"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsIdleBreakEventsPersecNull {
            get {
                if ((curObj["IdleBreakEventsPersec"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Idle Break Events/sec is the average rate, in incidents per second, at which the " +
            "processor wakes from idle.  This counter displays the difference between the val" +
            "ues observed in the last two samples, divided by the duration of the sample inte" +
            "rval.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong IdleBreakEventsPersec {
            get {
                if ((curObj["IdleBreakEventsPersec"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["IdleBreakEventsPersec"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsInterruptsPersecNull {
            get {
                if ((curObj["InterruptsPersec"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"Interrupts/sec is the average rate, in incidents per second, at which the processor received and serviced hardware interrupts. It does not include deferred procedure calls (DPCs), which are counted separately. This value is an indirect indicator of the activity of devices that generate interrupts, such as the system clock, the mouse, disk drivers, data communication lines, network interface cards, and other peripheral devices. These devices normally interrupt the processor when they have completed a task or require attention. Normal thread execution is suspended. The system clock typically interrupts the processor every 10 milliseconds, creating a background of interrupt activity. This counter displays the difference between the values observed in the last two samples, divided by the duration of the sample interval.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint InterruptsPersec {
            get {
                if ((curObj["InterruptsPersec"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["InterruptsPersec"]));
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("The Name property defines the label by which the statistic or metric is known. Wh" +
            "en subclassed, the property can be overridden to be a Key property. ")]
        public string Name {
            get {
                return ((string)(curObj["Name"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsParkingStatusNull {
            get {
                if ((curObj["ParkingStatus"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Parking Status represents whether a processor is parked or not.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint ParkingStatus {
            get {
                if ((curObj["ParkingStatus"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["ParkingStatus"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPercentC1TimeNull {
            get {
                if ((curObj["PercentC1Time"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"% C1 Time is the percentage of time the processor spends in the C1 low-power idle state. % C1 Time is a subset of the total processor idle time. C1 low-power idle state enables the processor to maintain its entire context and quickly return to the running state. Not all systems support the % C1 state.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong PercentC1Time {
            get {
                if ((curObj["PercentC1Time"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["PercentC1Time"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPercentC2TimeNull {
            get {
                if ((curObj["PercentC2Time"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"% C2 Time is the percentage of time the processor spends in the C2 low-power idle state. % C2 Time is a subset of the total processor idle time. C2 low-power idle state enables the processor to maintain the context of the system caches. The C2 power state is a lower power and higher exit latency state than C1. Not all systems support the C2 state.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong PercentC2Time {
            get {
                if ((curObj["PercentC2Time"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["PercentC2Time"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPercentC3TimeNull {
            get {
                if ((curObj["PercentC3Time"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"% C3 Time is the percentage of time the processor spends in the C3 low-power idle state. % C3 Time is a subset of the total processor idle time. When the processor is in the C3 low-power idle state it is unable to maintain the coherency of its caches. The C3 power state is a lower power and higher exit latency state than C2. Not all systems support the C3 state.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong PercentC3Time {
            get {
                if ((curObj["PercentC3Time"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["PercentC3Time"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPercentDPCTimeNull {
            get {
                if ((curObj["PercentDPCTime"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"% DPC Time is the percentage of time that the processor spent receiving and servicing deferred procedure calls (DPCs) during the sample interval. DPCs are interrupts that run at a lower priority than standard interrupts. % DPC Time is a component of % Privileged Time because DPCs are executed in privileged mode. They are counted separately and are not a component of the interrupt counters. This counter displays the average busy time as a percentage of the sample time.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong PercentDPCTime {
            get {
                if ((curObj["PercentDPCTime"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["PercentDPCTime"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPercentIdleTimeNull {
            get {
                if ((curObj["PercentIdleTime"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("% Idle Time is the percentage of time the processor is idle during the sample int" +
            "erval")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong PercentIdleTime {
            get {
                if ((curObj["PercentIdleTime"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["PercentIdleTime"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPercentInterruptTimeNull {
            get {
                if ((curObj["PercentInterruptTime"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"% Interrupt Time is the time the processor spends receiving and servicing hardware interrupts during sample intervals. This value is an indirect indicator of the activity of devices that generate interrupts, such as the system clock, the mouse, disk drivers, data communication lines, network interface cards and other peripheral devices. These devices normally interrupt the processor when they have completed a task or require attention. Normal thread execution is suspended during interrupts. Most system clocks interrupt the processor every 10 milliseconds, creating a background of interrupt activity. suspends normal thread execution during interrupts. This counter displays the average busy time as a percentage of the sample time.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong PercentInterruptTime {
            get {
                if ((curObj["PercentInterruptTime"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["PercentInterruptTime"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPercentofMaximumFrequencyNull {
            get {
                if ((curObj["PercentofMaximumFrequency"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("% of Maximum Frequency is the percentage of the current processor\'s maximum frequ" +
            "ency.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint PercentofMaximumFrequency {
            get {
                if ((curObj["PercentofMaximumFrequency"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["PercentofMaximumFrequency"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPercentPerformanceLimitNull {
            get {
                if ((curObj["PercentPerformanceLimit"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"% Performance Limit is the performance the processor guarantees it can provide, as a percentage of the nominal performance of the processor. Performance can be limited by Windows power policy, or by the platform as a result of a power budget, overheating, or other hardware issues.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint PercentPerformanceLimit {
            get {
                if ((curObj["PercentPerformanceLimit"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["PercentPerformanceLimit"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPercentPriorityTimeNull {
            get {
                if ((curObj["PercentPriorityTime"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"% Priority Time is the percentage of elapsed time that the processor spends executing threads that are not low priority. It is calculated by measuring the percentage of time that the processor spends executing low priority threads or the idle thread and then subtracting that value from 100%. (Each processor has an idle thread to which time is accumulated when no other threads are ready to run). This counter displays the average percentage of busy time observed during the sample interval excluding low priority background work. It should be noted that the accounting calculation of whether the processor is idle is performed at an internal sampling interval of the system clock tick. % Priority Time can therefore underestimate the processor utilization as the processor may be spending a lot of time servicing threads between the system clock sampling interval. Workload based timer applications are one example  of applications  which are more likely to be measured inaccurately as timers are signaled just after the sample is taken.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong PercentPriorityTime {
            get {
                if ((curObj["PercentPriorityTime"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["PercentPriorityTime"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPercentPrivilegedTimeNull {
            get {
                if ((curObj["PercentPrivilegedTime"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"% Privileged Time is the percentage of elapsed time that the process threads spent executing code in privileged mode.  When a Windows system service in called, the service will often run in privileged mode to gain access to system-private data. Such data is protected from access by threads executing in user mode. Calls to the system can be explicit or implicit, such as page faults or interrupts. Unlike some early operating systems, Windows uses process boundaries for subsystem protection in addition to the traditional protection of user and privileged modes. Some work done by Windows on behalf of the application might appear in other subsystem processes in addition to the privileged time in the process.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong PercentPrivilegedTime {
            get {
                if ((curObj["PercentPrivilegedTime"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["PercentPrivilegedTime"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPercentPrivilegedUtilityNull {
            get {
                if ((curObj["PercentPrivilegedUtility"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"Privileged Utility is the amount of work a processor is completing while executing in privileged mode, as a percentage of the amount of work the processor could complete if it were running at its nominal performance and never idle. On some processors, Privileged Utility may exceed 100%.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong PercentPrivilegedUtility {
            get {
                if ((curObj["PercentPrivilegedUtility"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["PercentPrivilegedUtility"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPercentProcessorPerformanceNull {
            get {
                if ((curObj["PercentProcessorPerformance"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Processor Performance is the average performance of the processor while it is exe" +
            "cuting instructions, as a percentage of the nominal performance of the processor" +
            ". On some processors, Processor Performance may exceed 100%.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong PercentProcessorPerformance {
            get {
                if ((curObj["PercentProcessorPerformance"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["PercentProcessorPerformance"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPercentProcessorTimeNull {
            get {
                if ((curObj["PercentProcessorTime"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"% Processor Time is the percentage of elapsed time that the processor spends to execute a non-Idle thread. It is calculated by measuring the percentage of time that the processor spends executing the idle thread and then subtracting that value from 100%. (Each processor has an idle thread to which time is accumulated when no other threads are ready to run). This counter is the primary indicator of processor activity, and displays the average percentage of busy time observed during the sample interval. It should be noted that the accounting calculation of whether the processor is idle is performed at an internal sampling interval of the system clock tick. On todays fast processors, % Processor Time can therefore underestimate the processor utilization as the processor may be spending a lot of time servicing threads between the system clock sampling interval. Workload based timer applications are one example  of applications  which are more likely to be measured inaccurately as timers are signaled just after the sample is taken.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong PercentProcessorTime {
            get {
                if ((curObj["PercentProcessorTime"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["PercentProcessorTime"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPercentProcessorUtilityNull {
            get {
                if ((curObj["PercentProcessorUtility"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Processor Utility is the amount of work a processor is completing, as a percentag" +
            "e of the amount of work the processor could complete if it were running at its n" +
            "ominal performance and never idle. On some processors, Processor Utility may exc" +
            "eed 100%.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong PercentProcessorUtility {
            get {
                if ((curObj["PercentProcessorUtility"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["PercentProcessorUtility"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPercentUserTimeNull {
            get {
                if ((curObj["PercentUserTime"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description(@"% User Time is the percentage of elapsed time the processor spends in the user mode. User mode is a restricted processing mode designed for applications, environment subsystems, and integral subsystems.  The alternative, privileged mode, is designed for operating system components and allows direct access to hardware and all memory.  The operating system switches application threads to privileged mode to access operating system services. This counter displays the average busy time as a percentage of the sample time.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public ulong PercentUserTime {
            get {
                if ((curObj["PercentUserTime"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["PercentUserTime"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsPerformanceLimitFlagsNull {
            get {
                if ((curObj["PerformanceLimitFlags"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Performance Limit Flags indicate reasons why the processor performance was limite" +
            "d.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint PerformanceLimitFlags {
            get {
                if ((curObj["PerformanceLimitFlags"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["PerformanceLimitFlags"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsProcessorFrequencyNull {
            get {
                if ((curObj["ProcessorFrequency"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Processor Frequency is the frequency of the current processor in megahertz.")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint ProcessorFrequency {
            get {
                if ((curObj["ProcessorFrequency"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["ProcessorFrequency"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsProcessorStateFlagsNull {
            get {
                if ((curObj["ProcessorStateFlags"] == null)) {
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        [Browsable(true)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        [Description("Processor State Flags")]
        [TypeConverter(typeof(WMIValueTypeConverter))]
        public uint ProcessorStateFlags {
            get {
                if ((curObj["ProcessorStateFlags"] == null)) {
                    return System.Convert.ToUInt32(0);
                }
                return ((uint)(curObj["ProcessorStateFlags"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsTimestamp_ObjectNull {
            get {
                if ((curObj["Timestamp_Object"] == null)) {
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
        public ulong Timestamp_Object {
            get {
                if ((curObj["Timestamp_Object"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["Timestamp_Object"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsTimestamp_PerfTimeNull {
            get {
                if ((curObj["Timestamp_PerfTime"] == null)) {
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
        public ulong Timestamp_PerfTime {
            get {
                if ((curObj["Timestamp_PerfTime"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["Timestamp_PerfTime"]));
            }
        }

        [Browsable(false)]
        [DesignerSerializationVisibility(DesignerSerializationVisibility.Hidden)]
        public bool IsTimestamp_Sys100NSNull {
            get {
                if ((curObj["Timestamp_Sys100NS"] == null)) {
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
        public ulong Timestamp_Sys100NS {
            get {
                if ((curObj["Timestamp_Sys100NS"] == null)) {
                    return System.Convert.ToUInt64(0);
                }
                return ((ulong)(curObj["Timestamp_Sys100NS"]));
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

        private bool ShouldSerializeAverageIdleTime() {
            if ((this.IsAverageIdleTimeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeC1TransitionsPersec() {
            if ((this.IsC1TransitionsPersecNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeC2TransitionsPersec() {
            if ((this.IsC2TransitionsPersecNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeC3TransitionsPersec() {
            if ((this.IsC3TransitionsPersecNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeClockInterruptsPersec() {
            if ((this.IsClockInterruptsPersecNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeDPCRate() {
            if ((this.IsDPCRateNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeDPCsQueuedPersec() {
            if ((this.IsDPCsQueuedPersecNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeFrequency_Object() {
            if ((this.IsFrequency_ObjectNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeFrequency_PerfTime() {
            if ((this.IsFrequency_PerfTimeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeFrequency_Sys100NS() {
            if ((this.IsFrequency_Sys100NSNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeIdleBreakEventsPersec() {
            if ((this.IsIdleBreakEventsPersecNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeInterruptsPersec() {
            if ((this.IsInterruptsPersecNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeParkingStatus() {
            if ((this.IsParkingStatusNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePercentC1Time() {
            if ((this.IsPercentC1TimeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePercentC2Time() {
            if ((this.IsPercentC2TimeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePercentC3Time() {
            if ((this.IsPercentC3TimeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePercentDPCTime() {
            if ((this.IsPercentDPCTimeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePercentIdleTime() {
            if ((this.IsPercentIdleTimeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePercentInterruptTime() {
            if ((this.IsPercentInterruptTimeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePercentofMaximumFrequency() {
            if ((this.IsPercentofMaximumFrequencyNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePercentPerformanceLimit() {
            if ((this.IsPercentPerformanceLimitNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePercentPriorityTime() {
            if ((this.IsPercentPriorityTimeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePercentPrivilegedTime() {
            if ((this.IsPercentPrivilegedTimeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePercentPrivilegedUtility() {
            if ((this.IsPercentPrivilegedUtilityNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePercentProcessorPerformance() {
            if ((this.IsPercentProcessorPerformanceNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePercentProcessorTime() {
            if ((this.IsPercentProcessorTimeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePercentProcessorUtility() {
            if ((this.IsPercentProcessorUtilityNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePercentUserTime() {
            if ((this.IsPercentUserTimeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializePerformanceLimitFlags() {
            if ((this.IsPerformanceLimitFlagsNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeProcessorFrequency() {
            if ((this.IsProcessorFrequencyNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeProcessorStateFlags() {
            if ((this.IsProcessorStateFlagsNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeTimestamp_Object() {
            if ((this.IsTimestamp_ObjectNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeTimestamp_PerfTime() {
            if ((this.IsTimestamp_PerfTimeNull == false)) {
                return true;
            }
            return false;
        }

        private bool ShouldSerializeTimestamp_Sys100NS() {
            if ((this.IsTimestamp_Sys100NSNull == false)) {
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
            string strPath = "root\\CIMV2:Win32_PerfFormattedData_Counters_ProcessorInformation";
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
        public static PerfFormattedData_Counters_ProcessorInformationCollection GetInstances() {
            return GetInstances(null, null, null);
        }

        public static PerfFormattedData_Counters_ProcessorInformationCollection GetInstances(string condition) {
            return GetInstances(null, condition, null);
        }

        public static PerfFormattedData_Counters_ProcessorInformationCollection GetInstances(string[] selectedProperties) {
            return GetInstances(null, null, selectedProperties);
        }

        public static PerfFormattedData_Counters_ProcessorInformationCollection GetInstances(string condition, string[] selectedProperties) {
            return GetInstances(null, condition, selectedProperties);
        }

        public static PerfFormattedData_Counters_ProcessorInformationCollection GetInstances(System.Management.ManagementScope mgmtScope, System.Management.EnumerationOptions enumOptions) {
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
            pathObj.ClassName = "Win32_PerfFormattedData_Counters_ProcessorInformation";
            pathObj.NamespacePath = "root\\CIMV2";
            System.Management.ManagementClass clsObject = new System.Management.ManagementClass(mgmtScope, pathObj, null);
            if ((enumOptions == null)) {
                enumOptions = new System.Management.EnumerationOptions();
                enumOptions.EnsureLocatable = true;
            }
            return new PerfFormattedData_Counters_ProcessorInformationCollection(clsObject.GetInstances(enumOptions));
        }

        public static PerfFormattedData_Counters_ProcessorInformationCollection GetInstances(System.Management.ManagementScope mgmtScope, string condition) {
            return GetInstances(mgmtScope, condition, null);
        }

        public static PerfFormattedData_Counters_ProcessorInformationCollection GetInstances(System.Management.ManagementScope mgmtScope, string[] selectedProperties) {
            return GetInstances(mgmtScope, null, selectedProperties);
        }

        public static PerfFormattedData_Counters_ProcessorInformationCollection GetInstances(System.Management.ManagementScope mgmtScope, string condition, string[] selectedProperties) {
            if ((mgmtScope == null)) {
                if ((statMgmtScope == null)) {
                    mgmtScope = new System.Management.ManagementScope();
                    mgmtScope.Path.NamespacePath = "root\\CIMV2";
                }
                else {
                    mgmtScope = statMgmtScope;
                }
            }
            System.Management.ManagementObjectSearcher ObjectSearcher = new System.Management.ManagementObjectSearcher(mgmtScope, new SelectQuery("Win32_PerfFormattedData_Counters_ProcessorInformation", condition, selectedProperties));
            System.Management.EnumerationOptions enumOptions = new System.Management.EnumerationOptions();
            enumOptions.EnsureLocatable = true;
            ObjectSearcher.Options = enumOptions;
            return new PerfFormattedData_Counters_ProcessorInformationCollection(ObjectSearcher.Get());
        }

        [Browsable(true)]
        public static PerfFormattedData_Counters_ProcessorInformation CreateInstance() {
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
            return new PerfFormattedData_Counters_ProcessorInformation(tmpMgmtClass.CreateInstance());
        }

        [Browsable(true)]
        public void Delete() {
            PrivateLateBoundObject.Delete();
        }

        // Enumerator implementation for enumerating instances of the class.
        public class PerfFormattedData_Counters_ProcessorInformationCollection : object, ICollection {

            private ManagementObjectCollection privColObj;

            public PerfFormattedData_Counters_ProcessorInformationCollection(ManagementObjectCollection objCollection) {
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
                    array.SetValue(new PerfFormattedData_Counters_ProcessorInformation(((System.Management.ManagementObject)(array.GetValue(nCtr)))), nCtr);
                }
            }

            public virtual System.Collections.IEnumerator GetEnumerator() {
                return new PerfFormattedData_Counters_ProcessorInformationEnumerator(privColObj.GetEnumerator());
            }

            public class PerfFormattedData_Counters_ProcessorInformationEnumerator : object, System.Collections.IEnumerator {

                private ManagementObjectCollection.ManagementObjectEnumerator privObjEnum;

                public PerfFormattedData_Counters_ProcessorInformationEnumerator(ManagementObjectCollection.ManagementObjectEnumerator objEnum) {
                    privObjEnum = objEnum;
                }

                public virtual object Current {
                    get {
                        return new PerfFormattedData_Counters_ProcessorInformation(((System.Management.ManagementObject)(privObjEnum.Current)));
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
