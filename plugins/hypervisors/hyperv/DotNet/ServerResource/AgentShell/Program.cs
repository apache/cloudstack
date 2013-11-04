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
using log4net;
using System;
using System.Collections.Generic;
using System.Linq;
using System.ServiceProcess;
using System.Text;
using System.Threading.Tasks;

namespace CloudStack.Plugin.AgentShell
{
    static class Program
    {
        private static ILog logger = LogManager.GetLogger(typeof(Program));

        /// <summary>
        /// Application entry point allows service to run in console application or as a Windows service.
        /// Add '--console' to the commandline for the former, the latter is the default.
        /// </summary>
        static void Main(params string[] args)
        {
            string arg1 = string.Empty;

            if (args.Length > 0)
            {
                arg1 = args[0];
                logger.DebugFormat("CloudStack ServerResource arg is ", arg1);
            }

            if (string.Compare(arg1, "--console", true) == 0)
            {
                logger.InfoFormat("CloudStack ServerResource running as console app");
                new AgentService().RunConsole(args);
            }
            else
            {
                logger.InfoFormat("CloudStack ServerResource running as Windows Service");
                ServiceBase[] ServicesToRun = new ServiceBase[] { new AgentService() };
                ServiceBase.Run(ServicesToRun);
            }
        }
    }
}
