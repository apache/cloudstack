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
using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Diagnostics;
using System.Linq;
using System.ServiceProcess;
using System.Text;
using System.Threading.Tasks;
using System.Web.Http.SelfHost;
using System.Web.Http;
using log4net;
using HypervResource;

namespace CloudStack.Plugin.AgentShell
{
    public partial class AgentService : ServiceBase
    {
        [System.Runtime.InteropServices.DllImport("kernel32.dll", SetLastError = true)]
        static extern bool AllocConsole();

        [System.Runtime.InteropServices.DllImport("kernel32.dll", SetLastError = true)]
        static extern bool FreeConsole();

        HttpSelfHostServer server;

        private static ILog logger = LogManager.GetLogger(typeof(AgentService));

        public AgentService()
        {
            logger.Info("Starting CloudStack agent");
            InitializeComponent();

            UriBuilder baseUri = new UriBuilder("https", AgentSettings.Default.private_ip_address, AgentSettings.Default.port);

            var config = new HttpSelfHostConfiguration(baseUri.Uri);

           // Allow ActionName to be applied to methods in ApiController, which allows it to serve multiple POST URLs
            config.Routes.MapHttpRoute(
                  "API Default", "api/{controller}/{action}",
                  new { action = RouteParameter.Optional }
                    );

            // Load controller assemblies that we want to config to route to.
            ConfigServerResource();
            AssertControllerAssemblyAvailable(config, typeof(HypervResourceController), "Cannot load Controller of type" + typeof(HypervResourceController));

            server = new HttpSelfHostServer(config);
        }

        public static void ConfigServerResource()
        {
            // For simplicity, ServerResource config and settings file are tightly coupled.
            // An alternative is to pass a dictionary to the server resource and let it find 
            // required settings.  In contrast, the approach below is strongly typed and makes
            // use of VisualStudio settings designer.  The designer allows us to avoid
            // accessing config using their key strings.
            HypervResourceControllerConfig rsrcCnf = new HypervResourceControllerConfig();
            rsrcCnf.RootDeviceReservedSpaceBytes = AgentSettings.Default.RootDeviceReservedSpaceBytes;
            rsrcCnf.RootDeviceName = AgentSettings.Default.RootDeviceName;
            rsrcCnf.ParentPartitionMinMemoryMb = AgentSettings.Default.dom0MinMemory;
            rsrcCnf.LocalSecondaryStoragePath = AgentSettings.Default.local_secondary_storage_path;

            // Side effect:  loads the assembly containing HypervResourceController, which
            // allows HttpSelfHostServer to route requests to the controller.
            HypervResourceController.Configure(rsrcCnf);

        }

        // TODO:  update to examine not the assembly resolver, but the list of available controllers themselves!
        private static bool AssertControllerAssemblyAvailable(HttpSelfHostConfiguration config, Type controllerType, string errorMessage)
        {
            var assemblies = config.Services.GetAssembliesResolver().GetAssemblies();
            foreach (var assembly in assemblies)
            {
                string name = assembly.GetName().Name;
                if (controllerType.Assembly.GetName().Name.Equals(name))
                {
                    logger.DebugFormat("Controller {0} is available", controllerType.Name);
                    return true;
                }
            }

            logger.Error(errorMessage);
            throw new AgentShellException(errorMessage);
        }

        protected override void OnStart(string[] args)
        {
            server.OpenAsync().Wait();
        }

        protected override void OnStop()
        {
            server.CloseAsync().Wait();
        }

        internal void RunConsole(string[] args)
        {
            OnStart(args);

            AllocConsole();

            Console.WriteLine("Service running ... press <ENTER> to stop");

            Console.ReadLine();

            FreeConsole();

            OnStop();
        }
    }
}
