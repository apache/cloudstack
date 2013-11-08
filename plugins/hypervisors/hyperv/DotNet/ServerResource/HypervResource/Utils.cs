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
        /// serialize dictonary to map json type
        /// </summary>
        /// <param name="objValue">Object's data, can be an anonymous object, e.g. </param>
        /// <returns></returns>
        public static JToken CreateCloudStackMapObject(object objValue)
        {
            JToken objContent = JToken.FromObject(objValue);
            return objContent;
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

        public static void ConnectToRemote(string remoteUNC, string domain, string username, string password)
        {
            NETRESOURCE nr = new NETRESOURCE();
            nr.dwType = RESOURCETYPE_DISK;
            nr.lpRemoteName = remoteUNC.Replace('/', Path.DirectorySeparatorChar);
            if (domain != null)
            {
                username = domain + @"\" + username;
            }

            int ret = WNetUseConnection(IntPtr.Zero, nr, password, username, 0, null, null, null);
            if (ret != NO_ERROR)
            {
                throw new ArgumentException("net use of share " + remoteUNC + "failed with "+ getErrorForNumber(ret));
            }
        }

        public static void DisconnectRemote(string remoteUNC)
        {
            int ret = WNetCancelConnection2(remoteUNC, CONNECT_UPDATE_PROFILE, false);
            if (ret != NO_ERROR)
            {
                throw new ArgumentException("net disconnect of share " + remoteUNC + "failed with " + getErrorForNumber(ret));
            }
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

        // from http://stackoverflow.com/a/2541569/939250
        #region imports
        [DllImport("advapi32.dll", SetLastError = true)]
        private static extern bool LogonUser(string lpszUsername, string lpszDomain, string lpszPassword, int dwLogonType, int dwLogonProvider, ref IntPtr phToken);

        [DllImport("kernel32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        private static extern bool CloseHandle(IntPtr handle);

        [DllImport("advapi32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        public extern static bool DuplicateToken(IntPtr existingTokenHandle, int SECURITY_IMPERSONATION_LEVEL, ref IntPtr duplicateTokenHandle);

        [DllImport("Mpr.dll")]
        private static extern int WNetUseConnection(IntPtr hwndOwner, NETRESOURCE lpNetResource, string lpPassword, string lpUserID, int dwFlags,
            string lpAccessName, string lpBufferSize, string lpResult);

        [DllImport("Mpr.dll")]
        private static extern int WNetCancelConnection2(string lpName, int dwFlags, bool fForce);

        [DllImport("kernel32.dll", SetLastError = true, CharSet = CharSet.Auto)]
        private static extern bool GetDiskFreeSpaceEx(string lpDirectoryName,
           out ulong lpFreeBytesAvailable,
           out ulong lpTotalNumberOfBytes,
           out ulong lpTotalNumberOfFreeBytes);
        #endregion

        #region consts
        // logon types 
        const int LOGON32_LOGON_INTERACTIVE = 2;
        const int LOGON32_LOGON_NETWORK = 3;
        const int LOGON32_LOGON_NEW_CREDENTIALS = 9;

        // logon providers 
        const int LOGON32_PROVIDER_DEFAULT = 0;
        const int LOGON32_PROVIDER_WINNT50 = 3;
        const int LOGON32_PROVIDER_WINNT40 = 2;
        const int LOGON32_PROVIDER_WINNT35 = 1;

        const int RESOURCE_CONNECTED = 0x00000001;
        const int RESOURCE_GLOBALNET = 0x00000002;
        const int RESOURCE_REMEMBERED = 0x00000003;

        const int RESOURCETYPE_ANY = 0x00000000;
        const int RESOURCETYPE_DISK = 0x00000001;
        const int RESOURCETYPE_PRINT = 0x00000002;

        const int RESOURCEDISPLAYTYPE_GENERIC = 0x00000000;
        const int RESOURCEDISPLAYTYPE_DOMAIN = 0x00000001;
        const int RESOURCEDISPLAYTYPE_SERVER = 0x00000002;
        const int RESOURCEDISPLAYTYPE_SHARE = 0x00000003;
        const int RESOURCEDISPLAYTYPE_FILE = 0x00000004;
        const int RESOURCEDISPLAYTYPE_GROUP = 0x00000005;

        const int RESOURCEUSAGE_CONNECTABLE = 0x00000001;
        const int RESOURCEUSAGE_CONTAINER = 0x00000002;


        const int CONNECT_INTERACTIVE = 0x00000008;
        const int CONNECT_PROMPT = 0x00000010;
        const int CONNECT_REDIRECT = 0x00000080;
        const int CONNECT_UPDATE_PROFILE = 0x00000001;
        const int CONNECT_COMMANDLINE = 0x00000800;
        const int CONNECT_CMD_SAVECRED = 0x00001000;

        const int CONNECT_LOCALDRIVE = 0x00000100;
        #endregion

        #region Errors
        const int NO_ERROR = 0;

        const int ERROR_ACCESS_DENIED = 5;
        const int ERROR_ALREADY_ASSIGNED = 85;
        const int ERROR_BAD_DEVICE = 1200;
        const int ERROR_BAD_NET_NAME = 67;
        const int ERROR_BAD_PROVIDER = 1204;
        const int ERROR_CANCELLED = 1223;
        const int ERROR_EXTENDED_ERROR = 1208;
        const int ERROR_INVALID_ADDRESS = 487;
        const int ERROR_INVALID_PARAMETER = 87;
        const int ERROR_INVALID_PASSWORD = 1216;
        const int ERROR_MORE_DATA = 234;
        const int ERROR_NO_MORE_ITEMS = 259;
        const int ERROR_NO_NET_OR_BAD_PATH = 1203;
        const int ERROR_NO_NETWORK = 1222;

        const int ERROR_BAD_PROFILE = 1206;
        const int ERROR_CANNOT_OPEN_PROFILE = 1205;
        const int ERROR_DEVICE_IN_USE = 2404;
        const int ERROR_NOT_CONNECTED = 2250;
        const int ERROR_OPEN_FILES = 2401;

        private struct ErrorClass
        {
            public int num;
            public string message;
            public ErrorClass(int num, string message)
            {
                this.num = num;
                this.message = message;
            }
        }

        private static ErrorClass[] ERROR_LIST = new ErrorClass[] {
            new ErrorClass(ERROR_ACCESS_DENIED, "Error: Access Denied"),
            new ErrorClass(ERROR_ALREADY_ASSIGNED, "Error: Already Assigned"),
            new ErrorClass(ERROR_BAD_DEVICE, "Error: Bad Device"),
            new ErrorClass(ERROR_BAD_NET_NAME, "Error: Bad Net Name"),
            new ErrorClass(ERROR_BAD_PROVIDER, "Error: Bad Provider"),
            new ErrorClass(ERROR_CANCELLED, "Error: Cancelled"),
            new ErrorClass(ERROR_EXTENDED_ERROR, "Error: Extended Error"),
            new ErrorClass(ERROR_INVALID_ADDRESS, "Error: Invalid Address"),
            new ErrorClass(ERROR_INVALID_PARAMETER, "Error: Invalid Parameter"),
            new ErrorClass(ERROR_INVALID_PASSWORD, "Error: Invalid Password"),
            new ErrorClass(ERROR_MORE_DATA, "Error: More Data"),
            new ErrorClass(ERROR_NO_MORE_ITEMS, "Error: No More Items"),
            new ErrorClass(ERROR_NO_NET_OR_BAD_PATH, "Error: No Net Or Bad Path"),
            new ErrorClass(ERROR_NO_NETWORK, "Error: No Network"),
            new ErrorClass(ERROR_BAD_PROFILE, "Error: Bad Profile"),
            new ErrorClass(ERROR_CANNOT_OPEN_PROFILE, "Error: Cannot Open Profile"),
            new ErrorClass(ERROR_DEVICE_IN_USE, "Error: Device In Use"),
            new ErrorClass(ERROR_EXTENDED_ERROR, "Error: Extended Error"),
            new ErrorClass(ERROR_NOT_CONNECTED, "Error: Not Connected"),
            new ErrorClass(ERROR_OPEN_FILES, "Error: Open Files"),
        };

        private static string getErrorForNumber(int errNum)
        {
            foreach (ErrorClass er in ERROR_LIST)
            {
                if (er.num == errNum) return er.message;
            }
            return "Error: Unknown, " + errNum;
        }
        #endregion

        [StructLayout(LayoutKind.Sequential)]
        private class NETRESOURCE
        {
            public int dwScope = 0;
            public int dwType = 0;
            public int dwDisplayType = 0;
            public int dwUsage = 0;
            public string lpLocalName = "";
            public string lpRemoteName = "";
            public string lpComment = "";
            public string lpProvider = "";
        }
    }
}
