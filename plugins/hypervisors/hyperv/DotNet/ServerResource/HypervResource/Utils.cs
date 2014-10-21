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
using System.Diagnostics;
using System.Reflection;

namespace HypervResource
{
    public class Utils
    {
        private static ILog s_logger = LogManager.GetLogger(typeof(Utils));

        private const string TASK_PREFIX = "cloudstack-heartbeat-";
        private const string BATCH_FILE = "heartbeat.bat";

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
        /// serialize dictonary to map json type
        /// </summary>
        /// <param name="objValue">Object's data, can be an anonymous object, e.g. </param>
        /// <returns></returns>
        public static JToken CreateCloudStackMapObject(object objValue)
        {
            JToken objContent = JToken.FromObject(objValue);
            return objContent;
        }

        public static string NormalizePath(string path)
        {
            if (!String.IsNullOrEmpty(path))
            {
                path = path.Replace('/', Path.DirectorySeparatorChar);
            }

            return path;
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
            String dest = "";
            if (filePathRelativeToShare.EndsWith(".iso") || filePathRelativeToShare.EndsWith(".vhd") || filePathRelativeToShare.EndsWith(".vhdx"))
            {
                dest = Path.Combine(cifsShareDetails.UncPath, filePathRelativeToShare);
                dest = Utils.NormalizePath(dest);
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
        }

        public static void GetShareDetails(string remoteUNC, out long capacity, out long available)
        {
            ulong freeBytesAvailable;
            ulong totalNumberOfBytes;
            ulong totalNumberOfFreeBytes;

            if (!GetDiskFreeSpaceEx(remoteUNC, out freeBytesAvailable, out totalNumberOfBytes, out totalNumberOfFreeBytes))
            {
                throw new ArgumentException("Not able to retrieve the capcity details of the share " + remoteUNC);
            }

            available = freeBytesAvailable > 0 ? (long)freeBytesAvailable : 0;
            capacity = totalNumberOfBytes > 0 ? (long)totalNumberOfBytes : 0;
        }

        public static string CleanString(string stringToClean)
        {
            string cleanString = null;
            string regexQueryString = "(&|%26)?(password|accesskey|secretkey|Password)(=|%3D).*?(?=(%26|[&'\"]))";
            string regexJson = "\"(password|accesskey|secretkey|Password)\":\\s?\".*?\",?";
            cleanString = System.Text.RegularExpressions.Regex.Replace(stringToClean, regexQueryString, "");
            cleanString = System.Text.RegularExpressions.Regex.Replace(cleanString, regexJson, "");
            return cleanString;
        }

        public static void AddHeartBeatTask(string poolGuid, string poolPath, string hostPrivateIp)
        {
            string taskName = TASK_PREFIX + poolGuid;
            UriBuilder uri = new UriBuilder(Assembly.GetExecutingAssembly().CodeBase);
            string alocation = Uri.UnescapeDataString(uri.Path);
            string batchFileLocation = Path.Combine(Path.GetDirectoryName(alocation), BATCH_FILE);
            string hbFile = Path.Combine(poolPath, "hb-" + hostPrivateIp);
            ExecuteTask("schtasks.exe", "/Create /RU \"SYSTEM\" /SC MINUTE /MO 1 /TN " + taskName + " /F /TR \"" + batchFileLocation + " " + hbFile + "\"");
        }

        public static void RemoveHeartBeatTask(string poolGuid)
        {
            string taskName = TASK_PREFIX + poolGuid;
            ExecuteTask("schtasks.exe", "/Delete /TN " + taskName + " /F");
        }

        public static void ExecuteTask(string command, string args)
        {
            ProcessStartInfo startInfo = new ProcessStartInfo();
            startInfo.CreateNoWindow = false;
            startInfo.UseShellExecute = true;
            startInfo.FileName = command;
            startInfo.WindowStyle = ProcessWindowStyle.Hidden;
            startInfo.Arguments = args;

            try
            {
                using (Process exeProcess = Process.Start(startInfo))
                {
                    exeProcess.WaitForExit();
                }
            }
            catch (Exception e)
            {
                s_logger.Error("Error occurred in deleting or adding a scheduled task " + e.Message);
            }
        }

        // from http://stackoverflow.com/a/2541569/939250
        #region imports
        [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Auto)]
        private static extern bool GetDiskFreeSpaceEx(string lpDirectoryName,
           out ulong lpFreeBytesAvailable,
           out ulong lpTotalNumberOfBytes,
           out ulong lpTotalNumberOfFreeBytes);
        #endregion
    }
}
