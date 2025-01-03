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

namespace CloudStack.Plugin.AgentShell {


    [global::System.Runtime.CompilerServices.CompilerGeneratedAttribute()]
    [global::System.CodeDom.Compiler.GeneratedCodeAttribute("Microsoft.VisualStudio.Editors.SettingsDesigner.SettingsSingleFileGenerator", "11.0.0.0")]
    public sealed partial class AgentSettings : global::System.Configuration.ApplicationSettingsBase {

        private static AgentSettings defaultInstance = ((AgentSettings)(global::System.Configuration.ApplicationSettingsBase.Synchronized(new AgentSettings())));

        public static AgentSettings Default {
            get {
                return defaultInstance;
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("8")]
        public int cpus {
            get {
                return ((int)(this["cpus"]));
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("Routing")]
        public string type {
            get {
                return ((string)(this["type"]));
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("5fe2bad3-d785-394e-9949-89786b8a63d2")]
        public global::System.Guid local_storage_uuid {
            get {
                return ((global::System.Guid)(this["local_storage_uuid"]));
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("hvm")]
        public string capabilities {
            get {
                return ((string)(this["capabilities"]));
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("2130")]
        public int Settingcpuspeed {
            get {
                return ((int)(this["Settingcpuspeed"]));
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("8250")]
        public int port {
            get {
                return ((int)(this["port"]));
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("34359738368")]
        public long memory {
            get {
                return ((long)(this["memory"]));
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("camldonall01.citrite.net")]
        public string host {
            get {
                return ((string)(this["host"]));
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("1")]
        public int pod {
            get {
                return ((int)(this["pod"]));
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("10.1.1.1")]
        public string gateway_ip_address {
            get {
                return ((string)(this["gateway_ip_address"]));
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        public global::System.Guid guid {
            get {
                return ((global::System.Guid)(this["guid"]));
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("2")]
        public int cluster {
            get {
                return ((int)(this["cluster"]));
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("1")]
        public int zone {
            get {
                return ((int)(this["zone"]));
            }
        }

        [global::System.Configuration.UserScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("c:\\Secondary")]
        public string local_secondary_storage_path {
            get {
                return ((string)(this["local_secondary_storage_path"]));
            }
            set {
                this["local_secondary_storage_path"] = value;
            }
        }

        [global::System.Configuration.UserScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("E:\\Disks\\Disks")]
        public string local_storage_path {
            get {
                return ((string)(this["local_storage_path"]));
            }
            set {
                this["local_storage_path"] = value;
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("5")]
        public int workers {
            get {
                return ((int)(this["workers"]));
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("255.255.240.0")]
        public string private_ip_netmask {
            get {
                return ((string)(this["private_ip_netmask"]));
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("4294967296")]
        public long RootDeviceReservedSpaceBytes {
            get {
                return ((long)(this["RootDeviceReservedSpaceBytes"]));
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("../../../../../")]
        public string hyperv_plugin_root {
            get {
                return ((string)(this["hyperv_plugin_root"]));
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("e:\\")]
        public string RootDeviceName {
            get {
                return ((string)(this["RootDeviceName"]));
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("101F742C6B88")]
        public string private_mac_address {
            get {
                return ((string)(this["private_mac_address"]));
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("systemvm.iso")]
        public string system_vm_iso {
            get {
                return ((string)(this["system_vm_iso"]));
            }
            set {
                this["system_vm_iso"] = value;
            }
        }

        [global::System.Configuration.UserScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute(".\\var\\test\\storagepool")]
        public string testLocalStorePath {
            get {
                return ((string)(this["testLocalStorePath"]));
            }
            set {
                this["testLocalStorePath"] = value;
            }
        }

        [global::System.Configuration.UserScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("testS3Bucket")]
        public string testS3Bucket {
            get {
                return ((string)(this["testS3Bucket"]));
            }
            set {
                this["testS3Bucket"] = value;
            }
        }

        [global::System.Configuration.UserScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("testS3Endpoint")]
        public string testS3Endpoint {
            get {
                return ((string)(this["testS3Endpoint"]));
            }
            set {
                this["testS3Endpoint"] = value;
            }
        }

        [global::System.Configuration.UserScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("testS3AccessKey")]
        public string testS3AccessKey {
            get {
                return ((string)(this["testS3AccessKey"]));
            }
            set {
                this["testS3AccessKey"] = value;
            }
        }

        [global::System.Configuration.UserScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("testS3SecretKey")]
        public string testS3SecretKey {
            get {
                return ((string)(this["testS3SecretKey"]));
            }
            set {
                this["testS3SecretKey"] = value;
            }
        }

        [global::System.Configuration.UserScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("206-2-73592258-559a-3b38-8f66-b667aab143eb")]
        public string testS3TemplateName {
            get {
                return ((string)(this["testS3TemplateName"]));
            }
            set {
                this["testS3TemplateName"] = value;
            }
        }

        [global::System.Configuration.UserScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("5fe2bad3-d785-394e-9949-89786b8a63d2")]
        public string testLocalStoreUUID {
            get {
                return ((string)(this["testLocalStoreUUID"]));
            }
            set {
                this["testLocalStoreUUID"] = value;
            }
        }

        [global::System.Configuration.UserScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("acton-systemvm-02062012.vhd.bz2")]
        public string testSystemVMTemplateName {
            get {
                return ((string)(this["testSystemVMTemplateName"]));
            }
            set {
                this["testSystemVMTemplateName"] = value;
            }
        }

        [global::System.Configuration.UserScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("acton-systemvm-02062012")]
        public string testSystemVMTemplateNameNoExt {
            get {
                return ((string)(this["testSystemVMTemplateNameNoExt"]));
            }
            set {
                this["testSystemVMTemplateNameNoExt"] = value;
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("2048")]
        public ulong dom0MinMemory {
            get {
                return ((ulong)(this["dom0MinMemory"]));
            }
        }

        [global::System.Configuration.ApplicationScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("10.1.1.1")]
        public string private_ip_address {
            get {
                return ((string)(this["private_ip_address"]));
            }
        }

        [global::System.Configuration.UserScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("cifs://10.1.1.1/secondary?user\\u003dadministrator\\u0026password\\u003d1pass%40w" +
            "ord1")]
        public string testCifsUrl {
            get {
                return ((string)(this["testCifsUrl"]));
            }
            set {
                this["testCifsUrl"] = value;
            }
        }

        [global::System.Configuration.UserScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("template/tmpl/2/201/61d5316a-3cc4-30cf-a557-78691ff5c143.vhd")]
        public string testCifsPath {
            get {
                return ((string)(this["testCifsPath"]));
            }
            set {
                this["testCifsPath"] = value;
            }
        }

        [global::System.Configuration.UserScopedSettingAttribute()]
        [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
        [global::System.Configuration.DefaultSettingValueAttribute("CentOS64")]
        public string testKvpVmName {
            get {
                return ((string)(this["testKvpVmName"]));
            }
            set {
                this["testKvpVmName"] = value;
            }
        }
    }
}
