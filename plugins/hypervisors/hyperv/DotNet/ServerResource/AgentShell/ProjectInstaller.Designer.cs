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
namespace CloudStack.Plugin.AgentShell
{
    partial class ProjectInstaller
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary> 
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Component Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.serviceProcessInstaller = new System.ServiceProcess.ServiceProcessInstaller();
            this.serviceInstaller = new System.ServiceProcess.ServiceInstaller();
            //
            // serviceProcessInstaller
            //
            string user = Program.GetUser();
            string password = Program.GetPassword();

            if (string.IsNullOrEmpty(user))
            {
                this.serviceProcessInstaller.Account = System.ServiceProcess.ServiceAccount.LocalSystem;
                this.serviceProcessInstaller.Password = null;
                this.serviceProcessInstaller.Username = null;
            }
            else
            {
                this.serviceProcessInstaller.Account = System.ServiceProcess.ServiceAccount.User;
                this.serviceProcessInstaller.Password = password;
                this.serviceProcessInstaller.Username = user;
            }

            //
            // serviceInstaller
            //
            this.serviceInstaller.Description = "CloudStack agent for managing a hyper-v host";
            this.serviceInstaller.DisplayName = Program.serviceName;
            this.serviceInstaller.ServiceName = Program.serviceName;
            this.serviceInstaller.StartType = System.ServiceProcess.ServiceStartMode.Automatic;
            //
            // ProjectInstaller
            //
            this.Installers.AddRange(new System.Configuration.Install.Installer[] {
            this.serviceProcessInstaller,
            this.serviceInstaller});

        }

        #endregion

        private System.ServiceProcess.ServiceProcessInstaller serviceProcessInstaller;
        private System.ServiceProcess.ServiceInstaller serviceInstaller;
    }
}
