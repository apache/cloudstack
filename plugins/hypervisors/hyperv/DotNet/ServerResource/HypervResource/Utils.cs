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
using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using System.Security.Principal;
using System.Text;
using System.Threading.Tasks;

namespace HypervResource
{
    public class Utils
    {
        private static ILog s_logger = LogManager.GetLogger(typeof(Utils));

        /// <summary>
        /// Associate CloudStack object's content with a fully qualified type name.
        /// </summary>
        /// <param name="objType">Fully qualified type name, e.g. "org.apache.cloudstack.storage.to.TemplateObjectTO"</param>
        /// <param name="objValue">Object's data, can be an anonymous object, e.g. </param>
        /// <returns></returns>
        public static JObject CreateCloudStackObject(string objType, object objValue)
        {
            JToken objContent = JToken.FromObject(objValue);
            JProperty objTypeValuePairing = new JProperty(objType, objContent);

            return new JObject(objTypeValuePairing);
        }


        /// <summary>
        /// Copy file on network share to local volume.
        /// </summary>
        /// <remarks>
        /// Access to the network share is acheived by logging into the domain corresponding to the user credentials provided.
        /// Windows impersonation does not suffice, because impersonation is limited to domains with an established trust relationship.
        /// We have had to import Win32 API calls to allow login.  There are a number of examples online.  We follow the
        /// one at http://stackoverflow.com/a/2541569/939250 </remarks>
        /// <param name="filePathRelativeToShare"></param>
        /// <param name="cifsShareDetails"></param>
        /// <param name="destFile"></param>
        public static void DownloadCifsFileToLocalFile(string filePathRelativeToShare, NFSTO cifsShareDetails, string destFile)
        {
            try
            {
                IntPtr token = IntPtr.Zero;

                bool isSuccess = LogonUser(cifsShareDetails.User, cifsShareDetails.Domain, cifsShareDetails.Password, LOGON32_LOGON_NEW_CREDENTIALS, LOGON32_PROVIDER_DEFAULT, ref token);
                using (WindowsImpersonationContext remoteIdentity = new WindowsIdentity(token).Impersonate())
                {
                 String dest = "";
                 if (filePathRelativeToShare.EndsWith(".iso") || filePathRelativeToShare.EndsWith(".vhd") || filePathRelativeToShare.EndsWith(".vhdx"))
                 {
                     dest = Path.Combine(cifsShareDetails.UncPath, filePathRelativeToShare);
                     dest = dest.Replace('/', Path.DirectorySeparatorChar);
                 }
                 // if the filePathRelativeToShare string don't have filename and only a dir point then find the vhd files in that folder and use
                 // In the clean setup, first copy command wont be having the filename it contains onlyu dir path.
                 // we need to scan the folder point and then copy the file to destination.
                 else if (!filePathRelativeToShare.EndsWith(".vhd") || !filePathRelativeToShare.EndsWith(".vhdx"))
                 {
                     // scan the folder and get the vhd filename.
                     String uncPath = Path.Combine(cifsShareDetails.UncPath, Path.Combine(filePathRelativeToShare.Split('/')));
                     //uncPath = uncPath.Replace("/", "\\");
                     DirectoryInfo dir = new DirectoryInfo(uncPath);
                     FileInfo[] vhdFiles = dir.GetFiles("*.vhd*");
                     if (vhdFiles.Length > 0)
                     {
                         FileInfo file = vhdFiles[0];
                         dest = file.FullName;
                     }
                 }
                    s_logger.Info(CloudStackTypes.CopyCommand + ": copy " + Path.Combine(cifsShareDetails.UncPath, filePathRelativeToShare) + " to " + destFile);

                    File.Copy(dest, destFile, true);
                    remoteIdentity.Undo();
                }
            }
            catch (UnauthorizedAccessException ex)
            {
                string errMsg = "Invalid user or password for the share " + cifsShareDetails.UncPath;
                s_logger.Error(errMsg);

                throw new ArgumentException(errMsg, ex);
            }
        }

        // from http://stackoverflow.com/a/2541569/939250
        #region imports
        [DllImport("advapi32.dll", SetLastError = true)]
        private static extern bool LogonUser(string lpszUsername, string lpszDomain, string lpszPassword, int dwLogonType, int dwLogonProvider, ref IntPtr phToken);

        [DllImport("kernel32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        private static extern bool CloseHandle(IntPtr handle);

        [DllImport("advapi32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        public extern static bool DuplicateToken(IntPtr existingTokenHandle, int SECURITY_IMPERSONATION_LEVEL, ref IntPtr duplicateTokenHandle);
        #endregion

        #region logon consts
        // logon types 
        const int LOGON32_LOGON_INTERACTIVE = 2;
        const int LOGON32_LOGON_NETWORK = 3;
        const int LOGON32_LOGON_NEW_CREDENTIALS = 9;

        // logon providers 
        const int LOGON32_PROVIDER_DEFAULT = 0;
        const int LOGON32_PROVIDER_WINNT50 = 3;
        const int LOGON32_PROVIDER_WINNT40 = 2;
        const int LOGON32_PROVIDER_WINNT35 = 1;
        #endregion
    }
}
