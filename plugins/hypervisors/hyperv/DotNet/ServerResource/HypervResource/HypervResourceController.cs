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

using Amazon;
using Amazon.S3;
using Amazon.S3.Model;
using log4net;
using Microsoft.CSharp.RuntimeBinder;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using System;
using System.Collections;
using System.Collections.Specialized;
using System.Collections.Generic;
using System.Configuration;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Http;
using System.Text;
using System.Security.Cryptography;
using System.Security.Principal;
using System.Web.Http;
using CloudStack.Plugin.WmiWrappers.ROOT.VIRTUALIZATION.V2;

namespace HypervResource
{

    public struct HypervResourceControllerConfig
    {
        private string privateIpAddress;
        private static ILog logger = LogManager.GetLogger(typeof(HypervResourceControllerConfig));

        public string PrivateIpAddress
        {
            get
            {
                return privateIpAddress;
            }
            set
            {
                ValidateIpAddress(value);
                privateIpAddress = value;
                System.Net.NetworkInformation.NetworkInterface nic = HypervResourceController.GetNicInfoFromIpAddress(privateIpAddress, out PrivateNetmask);
                PrivateMacAddress = nic.GetPhysicalAddress().ToString();
            }
        }

        private static void ValidateIpAddress(string value)
        {
            // Convert to IP address
            IPAddress ipAddress;
            if (!IPAddress.TryParse(value, out ipAddress))
            {
                String errMsg = "Invalid PrivateIpAddress: " + value;
                logger.Error(errMsg);
                throw new ArgumentException(errMsg);
            }
        }
        public string GatewayIpAddress;
        public string PrivateMacAddress;
        public string PrivateNetmask;
        public string StorageNetmask;
        public string StorageMacAddress;
        public string StorageIpAddress;
        public long RootDeviceReservedSpaceBytes;
        public string RootDeviceName;
        public ulong ParentPartitionMinMemoryMb;
        public string LocalSecondaryStoragePath;

        private string getPrimaryKey(string id)
        {
            return "primary_storage_" + id;
        }

        public string getPrimaryStorage(string id)
        {
            NameValueCollection settings = ConfigurationManager.AppSettings;
            return settings.Get(getPrimaryKey(id));
        }

        public void setPrimaryStorage(string id, string path)
        {
            Configuration config = ConfigurationManager.OpenExeConfiguration(ConfigurationUserLevel.None);
            KeyValueConfigurationCollection settings = config.AppSettings.Settings;
            string key = getPrimaryKey(id);
            if (settings[key] != null)
            {
                settings.Remove(key);
            }
            settings.Add(key, path);
            config.Save(ConfigurationSaveMode.Modified);
            ConfigurationManager.RefreshSection("appSettings");
        }

        public List<string> getAllPrimaryStorages()
        {
            List<string> poolPaths = new List<string>();
            Configuration config = ConfigurationManager.OpenExeConfiguration(ConfigurationUserLevel.None);
            KeyValueConfigurationCollection settings = config.AppSettings.Settings;
            foreach (string key in settings.AllKeys)
            {
                if (key.Contains("primary_storage_"))
                {
                    poolPaths.Add(settings[key].Value);
                }
            }
            return poolPaths;
        }
    }

    /// <summary>
    /// Supports one HTTP GET and multiple HTTP POST URIs
    /// </summary>
    /// <remarks>
    /// <para>
    /// POST takes dynamic to allow it to receive JSON without concern for what is the underlying object.
    /// E.g. http://stackoverflow.com/questions/14071715/passing-dynamic-json-object-to-web-api-newtonsoft-example 
    /// and http://stackoverflow.com/questions/3142495/deserialize-json-into-c-sharp-dynamic-object
    /// Use ActionName attribute to allow multiple POST URLs, one for each supported command
    /// E.g. http://stackoverflow.com/a/12703423/939250
    /// Strictly speaking, this goes against the purpose of an ApiController, which is to provide one GET/POST/PUT/DELETE, etc.
    /// However, it reduces the amount of code by removing the need for a switch according to the incoming command type.
    /// http://weblogs.asp.net/fredriknormen/archive/2012/06/11/asp-net-web-api-exception-handling.aspx
    /// </para>
    /// <para>
    /// Exceptions handled on command by command basis rather than globally to allow details of the command
    /// to be reflected in the response.  Default error handling is in the catch for Exception, but
    /// other exception types may be caught where the feedback would be different.
    /// NB: global alternatives discussed at 
    /// http://weblogs.asp.net/fredriknormen/archive/2012/06/11/asp-net-web-api-exception-handling.aspx
    /// </para>
    /// </remarks>
    public class HypervResourceController : ApiController
    {
        public static void Configure(HypervResourceControllerConfig config)
        {
            HypervResourceController.config = config;
            wmiCallsV2 = new WmiCallsV2();
        }

        public static HypervResourceControllerConfig config = new HypervResourceControllerConfig();

        private static ILog logger = LogManager.GetLogger(typeof(HypervResourceController));
        Dictionary<String, String> contextMap = new Dictionary<String, String>();

        public static void Initialize()
        {
        }

        public static IWmiCallsV2 wmiCallsV2 { get; set;}

        // GET api/HypervResource
        public string Get()
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                return "HypervResource controller running, use POST to send JSON encoded RPCs"; ;
            }
        }

        /// <summary>
        /// NOP - placeholder for future setup, e.g. delete existing VMs or Network ports 
        /// POST api/HypervResource/SetupCommand
        /// </summary>
        /// <param name="cmd"></param>
        /// <returns></returns>
        /// TODO: produce test
        [HttpPost]
        [ActionName(CloudStackTypes.SetupCommand)]
        public JContainer SetupCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.SetupCommand + Utils.CleanString(cmd.ToString()));

                string details = null;
                bool result = false;

                try
                {
                    result = true;
                }
                catch (Exception sysEx)
                {
                    details = CloudStackTypes.SetupCommand + " failed due to " + sysEx.Message;
                    logger.Error(details, sysEx);
                }

                object ansContent = new
                {
                    result = result,
                    details = "success - NOP",
                    _reconnect = false,
                    contextMap = contextMap
                };

                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.SetupAnswer);
            }
        }

        // POST api/HypervResource/AttachCommand
        [HttpPost]
        [ActionName(CloudStackTypes.AttachCommand)]
        public JContainer AttachCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.AttachCommand + Utils.CleanString(cmd.ToString()));

                string details = null;
                bool result = false;

                try
                {
                    string vmName = (string)cmd.vmName;
                    DiskTO disk = DiskTO.ParseJson(cmd.disk);

                    if (disk.type.Equals("ISO"))
                    {
                        TemplateObjectTO dataStore = disk.templateObjectTO;
                        NFSTO share = dataStore.nfsDataStoreTO;
                        string diskPath = Utils.NormalizePath(Path.Combine(share.UncPath, dataStore.path));
                        wmiCallsV2.AttachIso(vmName, diskPath);
                        result = true;
                    }
                    else if (disk.type.Equals("DATADISK"))
                    {
                        VolumeObjectTO volume = disk.volumeObjectTO;
                        string diskPath = Utils.NormalizePath(volume.FullFileName);
                        wmiCallsV2.AttachDisk(vmName, diskPath, disk.diskSequence);
                        result = true;
                    }
                    else
                    {
                        details = "Invalid disk type to be attached to vm " + vmName;
                    }
                }
                catch (Exception sysEx)
                {
                    details = CloudStackTypes.AttachCommand + " failed due to " + sysEx.Message;
                    logger.Error(details, sysEx);
                }

                object ansContent = new
                {
                    result = result,
                    details = details,
                    disk = cmd.disk,
                    contextMap = contextMap
                };

                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.AttachAnswer);
            }
        }

        // POST api/HypervResource/DetachCommand
        [HttpPost]
        [ActionName(CloudStackTypes.DettachCommand)]
        public JContainer DetachCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.DettachCommand + Utils.CleanString(cmd.ToString()));

                string details = null;
                bool result = false;

                try
                {
                    string vmName = (string)cmd.vmName;
                    DiskTO disk = DiskTO.ParseJson(cmd.disk);

                    if (disk.type.Equals("ISO"))
                    {
                        TemplateObjectTO dataStore = disk.templateObjectTO;
                        NFSTO share = dataStore.nfsDataStoreTO;
                        string diskPath = Utils.NormalizePath(Path.Combine(share.UncPath, dataStore.path));
                        wmiCallsV2.DetachDisk(vmName, diskPath);
                        result = true;
                    }
                    else if (disk.type.Equals("DATADISK"))
                    {
                        VolumeObjectTO volume = disk.volumeObjectTO;
                        string diskPath = Utils.NormalizePath(volume.FullFileName);
                        wmiCallsV2.DetachDisk(vmName, diskPath);
                        result = true;
                    }
                    else
                    {
                        details = "Invalid disk type to be dettached from vm " + vmName;
                    }
                }
                catch (Exception sysEx)
                {
                    details = CloudStackTypes.DettachCommand + " failed due to " + sysEx.Message;
                    logger.Error(details, sysEx);
                }

                object ansContent = new
                {
                    result = result,
                    details = details,
                    contextMap = contextMap
                };

                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.DettachAnswer);
            }
        }

        // POST api/HypervResource/RebootCommand
        [HttpPost]
        [ActionName(CloudStackTypes.RebootCommand)]
        public JContainer RebootCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.RebootCommand + Utils.CleanString(cmd.ToString()));

                string details = null;
                bool result = false;

                try
                {
                    string vmName = (string)cmd.vmName;
                    var sys = wmiCallsV2.GetComputerSystem(vmName);
                    if (sys == null)
                    {
                        details = CloudStackTypes.RebootCommand + " requested unknown VM " + vmName;
                        logger.Error(details);
                    }
                    else
                    {
                        wmiCallsV2.SetState(sys, RequiredState.Reset);
                        result = true;
                    }
                }
                catch (Exception sysEx)
                {
                    details = CloudStackTypes.RebootCommand + " failed due to " + sysEx.Message;
                    logger.Error(details, sysEx);
                }

                object ansContent = new
                {
                    result = result,
                    details = details,
                    contextMap = contextMap
                };

                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.RebootAnswer);
            }
        }

        // POST api/HypervResource/DestroyCommand
        [HttpPost]
        [ActionName(CloudStackTypes.DestroyCommand)]
        public JContainer DestroyCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.DestroyCommand + Utils.CleanString(cmd.ToString()));

                string details = null;
                bool result = false;

                try
                {
                    // Assert
                    String errMsg = "No 'volume' details in " + CloudStackTypes.DestroyCommand + " " + Utils.CleanString(cmd.ToString());
                    if (cmd.volume == null)
                    {
                        logger.Error(errMsg);
                        throw new ArgumentException(errMsg);
                    }

                    // Assert
                    errMsg = "No valid path in DestroyCommand in " + CloudStackTypes.DestroyCommand + " " + (String)cmd.ToString();
                    if (cmd.volume.path == null)
                    {
                        logger.Error(errMsg);
                        throw new ArgumentException(errMsg);
                    }

                    String path = (string)cmd.volume.path;
                    if (!File.Exists(path))
                    {
                        logger.Info(CloudStackTypes.DestroyCommand + ", but volume at pass already deleted " + path);
                    }

                    string vmName = (string)cmd.vmName;
                    if (!string.IsNullOrEmpty(vmName) && File.Exists(path))
                    {
                        // Make sure that this resource is removed from the VM
                        wmiCallsV2.DetachDisk(vmName, path);
                    }

                    File.Delete(path);
                    result = true;
                }
                catch (Exception sysEx)
                {
                    details = CloudStackTypes.DestroyCommand + " failed due to " + sysEx.Message;
                    logger.Error(details, sysEx);
                }

                object ansContent = new
                    {
                        result = result,
                        details = details,
                        contextMap = contextMap
                    };

                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.Answer);
            }
        }

        // POST api/HypervResource/DeleteCommand
        [HttpPost]
        [ActionName(CloudStackTypes.DeleteCommand)]
        public JContainer DeleteCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.DestroyCommand + Utils.CleanString(cmd.ToString()));

                string details = null;
                bool result = false;

                try
                {
                    // Assert
                    String errMsg = "No 'volume' details in " + CloudStackTypes.DestroyCommand + " " + Utils.CleanString(cmd.ToString());
                    VolumeObjectTO destVolumeObjectTO = VolumeObjectTO.ParseJson(cmd.data);

                    if (destVolumeObjectTO.name == null)
                    {
                        logger.Error(errMsg);
                        throw new ArgumentException(errMsg);
                    }

                    String path = destVolumeObjectTO.FullFileName;
                    if (!File.Exists(path))
                    {
                        logger.Info(CloudStackTypes.DestroyCommand + ", but volume at pass already deleted " + path);
                    }

                    string vmName = (string)cmd.vmName;
                    if (!string.IsNullOrEmpty(vmName) && File.Exists(path))
                    {
                        // Make sure that this resource is removed from the VM
                        wmiCallsV2.DetachDisk(vmName, path);
                    }

                    File.Delete(path);
                    result = true;
                }
                catch (Exception sysEx)
                {
                    details = CloudStackTypes.DestroyCommand + " failed due to " + sysEx.Message;
                    logger.Error(details, sysEx);
                }

                object ansContent = new
                {
                    result = result,
                    details = details,
                    contextMap = contextMap
                };

                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.Answer);
            }
        }

        
        private static JArray ReturnCloudStackTypedJArray(object ansContent, string ansType)
        {
            JObject ansObj = Utils.CreateCloudStackObject(ansType, ansContent);
            JArray answer = new JArray(ansObj);
            logger.Info(Utils.CleanString(ansObj.ToString()));
            return answer;
        }

        // POST api/HypervResource/CreateCommand
        [HttpPost]
        [ActionName(CloudStackTypes.CreateCommand)]
        public JContainer CreateCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.CreateCommand + Utils.CleanString(cmd.ToString()));

                string details = null;
                bool result = false;
                VolumeInfo volume = new VolumeInfo();

                try
                {
                    string diskType = cmd.diskCharacteristics.type;
                    ulong disksize = cmd.diskCharacteristics.size;
                    string templateUri = cmd.templateUrl;

                    // assert: valid storagepool?
                    string poolTypeStr = cmd.pool.type;
                    string poolLocalPath = cmd.pool.path;
                    string poolUuid = cmd.pool.uuid;
                    string newVolPath = null;
                    long volId = cmd.volId;
                    string newVolName = null;

                    if (ValidStoragePool(poolTypeStr, poolLocalPath, poolUuid, ref details))
                    {
                        // No template URI?  Its a blank disk.
                        if (string.IsNullOrEmpty(templateUri))
                        {
                            // assert
                            VolumeType volType;
                            if (!Enum.TryParse<VolumeType>(diskType, out volType) && volType != VolumeType.DATADISK)
                            {
                                details = "Cannot create volumes of type " + (string.IsNullOrEmpty(diskType) ? "NULL" : diskType);
                            }
                            else
                            {
                                newVolName = cmd.diskCharacteristics.name;
                                newVolPath = Path.Combine(poolLocalPath, newVolName, diskType.ToLower());
                                // TODO: make volume format and block size configurable
                                wmiCallsV2.CreateDynamicVirtualHardDisk(disksize, newVolPath);
                                if (File.Exists(newVolPath))
                                {
                                    result = true;
                                }
                                else
                                {
                                    details = "Failed to create DATADISK with name " + newVolName;
                                }
                            }
                        }
                        else
                        {
                            // TODO:  Does this always work, or do I need to download template at times?
                            if (templateUri.Contains("/") || templateUri.Contains("\\"))
                            {
                                details = "Problem with templateURL " + templateUri +
                                                " the URL should be volume UUID in primary storage created by previous PrimaryStorageDownloadCommand";
                                logger.Error(details);
                            }
                            else
                            {
                                logger.Debug("Template's name in primary store should be " + templateUri);
                                //            HypervPhysicalDisk BaseVol = primaryPool.getPhysicalDisk(tmplturl);
                                FileInfo srcFileInfo = new FileInfo(templateUri);
                                newVolName = Guid.NewGuid() + srcFileInfo.Extension;
                                newVolPath = Path.Combine(poolLocalPath, newVolName);
                                logger.Debug("New volume will be at " + newVolPath);
                                string oldVolPath = Path.Combine(poolLocalPath, templateUri);
                                File.Copy(oldVolPath, newVolPath);
                                if (File.Exists(newVolPath))
                                {
                                    result = true;
                                }
                                else
                                {
                                    details = "Failed to create DATADISK with name " + newVolName;
                                }
                            }
                            volume = new VolumeInfo(
                                      volId, diskType,
                                    poolTypeStr, poolUuid, newVolName,
                                    newVolPath, newVolPath, (long)disksize, null);
                        }
                    }
                }
                catch (Exception sysEx)
                {
                    // TODO: consider this as model for error processing in all commands
                    details = CloudStackTypes.CreateCommand + " failed due to " + sysEx.Message;
                    logger.Error(details, sysEx);
                }

                object ansContent = new
                {
                    result = result,
                    details = details,
                    volume = volume,
                    contextMap = contextMap
                };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.CreateAnswer);
            }
        }

        // POST api/HypervResource/PrimaryStorageDownloadCommand
        [HttpPost]
        [ActionName(CloudStackTypes.PrimaryStorageDownloadCommand)]
        public JContainer PrimaryStorageDownloadCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.PrimaryStorageDownloadCommand + Utils.CleanString(cmd.ToString()));
                string details = null;
                bool result = false;
                long size = 0;
                string newCopyFileName = null;

                string poolLocalPath = cmd.localPath;
                if (!Directory.Exists(poolLocalPath))
                {
                    details = "None existent local path " + poolLocalPath;
                }
                else
                {
                    // Compose name for downloaded file.
                    string sourceUrl = cmd.url;
                    if (sourceUrl.ToLower().EndsWith(".vhd"))
                    {
                        newCopyFileName = Guid.NewGuid() + ".vhd";
                    }
                    if (sourceUrl.ToLower().EndsWith(".vhdx"))
                    {
                        newCopyFileName = Guid.NewGuid() + ".vhdx";
                    }

                    // assert
                    if (newCopyFileName == null)
                    {
                        details = CloudStackTypes.PrimaryStorageDownloadCommand + " Invalid file extension for hypervisor type in source URL " + sourceUrl;
                        logger.Error(details);
                    }
                    else
                    {
                        try
                        {
                            FileInfo newFile;
                            if (CopyURI(sourceUrl, newCopyFileName, poolLocalPath, out newFile, ref details))
                            {
                                size = newFile.Length;
                                result = true;
                            }
                        }
                        catch (System.Exception ex)
                        {
                            details = CloudStackTypes.PrimaryStorageDownloadCommand + " Cannot download source URL " + sourceUrl + " due to " + ex.Message;
                            logger.Error(details, ex);
                        }
                    }
                }

                object ansContent = new
                {
                    result = result,
                    details = details,
                    templateSize = size,
                    installPath = newCopyFileName,
                    contextMap = contextMap
                };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.PrimaryStorageDownloadAnswer);
            }
        }

        private static bool ValidStoragePool(string poolTypeStr, string poolLocalPath, string poolUuid, ref string details)
        {
            StoragePoolType poolType;
            if (!Enum.TryParse<StoragePoolType>(poolTypeStr, out poolType) || poolType != StoragePoolType.Filesystem)
            {
                details = "Primary storage pool " + poolUuid + " type " + poolType + " local path " + poolLocalPath + " has invalid StoragePoolType";
                logger.Error(details);
                return false;
            }
            else if (!Directory.Exists(poolLocalPath))
            {
                details = "Primary storage pool " + poolUuid + " type " + poolType + " local path " + poolLocalPath + " has invalid local path";
                logger.Error(details);
                return false;
            }
            return true;
        }

        /// <summary>
        /// Exceptions to watch out for:
        /// Exceptions related to URI creation
        /// System.SystemException
        /// +-System.ArgumentNullException
        /// +-System.FormatException
        ///   +-System.UriFormatException
        ///
        /// Exceptions related to NFS URIs
        /// System.SystemException
        /// +-System.NotSupportedException
        /// +-System.ArgumentException
        /// +-System.ArgumentNullException
        ///   +-System.Security.SecurityException;
        /// +-System.UnauthorizedAccessException
        /// +-System.IO.IOException
        ///   +-System.IO.PathTooLongException
        ///   
        /// Exceptions related to HTTP URIs
        /// System.SystemException
        /// +-System.InvalidOperationException
        ///    +-System.Net.WebException
        /// +-System.NotSupportedException
        /// +-System.ArgumentNullException
        /// </summary>
        /// <param name="sourceUri"></param>
        /// <param name="newCopyFileName"></param>
        /// <param name="poolLocalPath"></param>
        /// <returns></returns>
        private bool CopyURI(string sourceUri, string newCopyFileName, string poolLocalPath, out FileInfo newFile, ref string details)
        {
            Uri source = new Uri(sourceUri);
            String destFilePath = Path.Combine(poolLocalPath, newCopyFileName);
            string[] pathSegments = source.Segments;
            String templateUUIDandExtension = pathSegments[pathSegments.Length - 1];
            newFile = new FileInfo(destFilePath);

            // NFS URI assumed to already be mounted locally.  Mount location given by settings.
            if (source.Scheme.ToLower().Equals("nfs"))
            {
                String srcDiskPath = Path.Combine(HypervResourceController.config.LocalSecondaryStoragePath, templateUUIDandExtension);
                String taskMsg = "Copy NFS url in " + sourceUri + " at " + srcDiskPath + " to pool " + poolLocalPath;
                logger.Debug(taskMsg);
                File.Copy(srcDiskPath, destFilePath);
            }
            else if (source.Scheme.ToLower().Equals("http") || source.Scheme.ToLower().Equals("https"))
            {
                System.Net.WebClient webclient = new WebClient();
                webclient.DownloadFile(source, destFilePath);
            }
            else
            {
                details = "Unsupported URI scheme " + source.Scheme.ToLower() + " in source URI " + sourceUri;
                logger.Error(details);
                return false;
            }

            if (!File.Exists(destFilePath))
            {
                details = "Filed to copy " + sourceUri + " to primary pool destination " + destFilePath;
                logger.Error(details);
                return false;
            }
            return true;
        }

        // POST api/HypervResource/CheckHealthCommand
        [HttpPost]
        [ActionName(CloudStackTypes.CheckHealthCommand)]
        public JContainer CheckHealthCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.CheckHealthCommand + Utils.CleanString(cmd.ToString()));
                object ansContent = new
                {
                    result = true,
                    details = "resource is alive",
                    contextMap = contextMap
                };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.CheckHealthAnswer);
            }
        }

        // POST api/HypervResource/CheckOnHostCommand
        [HttpPost]
        [ActionName(CloudStackTypes.CheckOnHostCommand)]
        public JContainer CheckOnHostCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.CheckOnHostCommand + Utils.CleanString(cmd.ToString()));
                string details = "host is not alive";
                bool result = true;
                try
                {
                    foreach (string poolPath in config.getAllPrimaryStorages())
                    {
                        if (IsHostAlive(poolPath, (string)cmd.host.privateNetwork.ip))
                        {
                            result = false;
                            details = "host is alive";
                            break;
                        }
                    }
                }
                catch (Exception e)
                {
                    logger.Error("Error Occurred in " + CloudStackTypes.CheckOnHostCommand + " : " + e.Message);
                }

                object ansContent = new
                {
                    result = result,
                    details = details,
                    contextMap = contextMap
                };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.CheckOnHostAnswer);
            }
        }

        private bool IsHostAlive(string poolPath, string privateIp)
        {
            bool hostAlive = false;
            try
            {
                string hbFile = Path.Combine(poolPath, "hb-" + privateIp);
                FileInfo file = new FileInfo(hbFile);
                using (StreamReader sr = file.OpenText())
                {
                    string epoch = sr.ReadLine();
                    string[] dateTime = epoch.Split('@');
                    string[] date = dateTime[0].Split('-');
                    string[] time = dateTime[1].Split(':');
                    DateTime epochTime = new DateTime(Convert.ToInt32(date[0]), Convert.ToInt32(date[1]), Convert.ToInt32(date[2]), Convert.ToInt32(time[0]),
                        Convert.ToInt32(time[1]), Convert.ToInt32(time[2]), DateTimeKind.Utc);
                    DateTime currentTime = DateTime.UtcNow;
                    DateTime ThreeMinuteLaterEpoch = epochTime.AddMinutes(3);
                    if (currentTime.CompareTo(ThreeMinuteLaterEpoch) < 0)
                    {
                        hostAlive = true;
                    }
                    sr.Close();
                }
            }
            catch (Exception e)
            {
                logger.Info("Exception occurred in verifying host " + e.Message);
            }

            return hostAlive;
        }

        // POST api/HypervResource/CheckSshCommand
        // TODO: create test
        [HttpPost]
        [ActionName(CloudStackTypes.CheckSshCommand)]
        public JContainer CheckSshCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.CheckSshCommand + Utils.CleanString(cmd.ToString()));
                object ansContent = new
                {
                    result = true,
                    details = "NOP, TODO: implement properly",
                    contextMap = contextMap
                };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.CheckSshAnswer);
            }
        }

        // POST api/HypervResource/CheckVirtualMachineCommand
        [HttpPost]
        [ActionName(CloudStackTypes.CheckVirtualMachineCommand)]
        public JContainer CheckVirtualMachineCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.CheckVirtualMachineCommand + Utils.CleanString(cmd.ToString()));
                string details = null;
                bool result = false;
                string vmName = cmd.vmName;
                string powerState = null;

                // TODO: Look up the VM, convert Hyper-V state to CloudStack version.
                var sys = wmiCallsV2.GetComputerSystem(vmName);
                if (sys == null)
                {
                    details = CloudStackTypes.CheckVirtualMachineCommand + " requested unknown VM " + vmName;
                    logger.Error(details);
                }
                else
                {
                    powerState = EnabledState.ToCloudStackPowerState(sys.EnabledState);
                    result = true;
                }

                object ansContent = new
                {
                    result = result,
                    details = details,
                    powerstate = powerState,
                    contextMap = contextMap
                };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.CheckVirtualMachineAnswer);
            }
        }

        // POST api/HypervResource/DeleteStoragePoolCommand
        [HttpPost]
        [ActionName(CloudStackTypes.DeleteStoragePoolCommand)]
        public JContainer DeleteStoragePoolCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.DeleteStoragePoolCommand + Utils.CleanString(cmd.ToString()));
                object ansContent = new
                {
                    result = true,
                    details = "Current implementation does not delete local path corresponding to storage pool!",
                    contextMap = contextMap
                };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.Answer);
            }
        }

        /// <summary>
        /// NOP - legacy command -
        /// POST api/HypervResource/CreateStoragePoolCommand
        /// </summary>
        /// <param name="cmd"></param>
        /// <returns></returns>
        [HttpPost]
        [ActionName(CloudStackTypes.CreateStoragePoolCommand)]
        public JContainer CreateStoragePoolCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.CreateStoragePoolCommand + Utils.CleanString(cmd.ToString()));
                object ansContent = new
                {
                    result = true,
                    details = "success - NOP",
                    contextMap = contextMap
                };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.Answer);
            }
        }

        // POST api/HypervResource/ModifyStoragePoolCommand
        [HttpPost]
        [ActionName(CloudStackTypes.ModifyStoragePoolCommand)]
        public JContainer ModifyStoragePoolCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.ModifyStoragePoolCommand + Utils.CleanString(cmd.ToString()));
                string details = null;
                string localPath;
                StoragePoolType poolType;
                long capacityBytes = 0;
                long availableBytes = 0;
                string hostPath = null;
                bool result = false;
                var tInfo = new Dictionary<string, string>();
                object ansContent;

                try
                {
                    result = ValidateStoragePoolCommand(cmd, out localPath, out poolType, ref details);
                    if (!result)
                    {
                        ansContent = new
                        {
                            result = result,
                            details = details,
                            contextMap = contextMap
                        };
                        return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.Answer);
                    }

                    if (poolType == StoragePoolType.Filesystem)
                    {
                        hostPath = localPath;
                        GetCapacityForPath(hostPath, out capacityBytes, out availableBytes);
                    }
                    else if (poolType == StoragePoolType.NetworkFilesystem ||
                        poolType == StoragePoolType.SMB)
                    {
                        NFSTO share = new NFSTO();
                        String uriStr = "cifs://" + (string)cmd.pool.host + (string)cmd.pool.path;
                        share.uri = new Uri(uriStr);
                        hostPath = Utils.NormalizePath(share.UncPath);

                        // Check access to share.
                        Utils.GetShareDetails(hostPath, out capacityBytes, out availableBytes);
                        config.setPrimaryStorage((string)cmd.pool.uuid, hostPath);
                    }
                    else
                    {
                        result = false;
                    }
                }
                catch
                {
                    result = false;
                    details = String.Format("Failed to add storage pool {0}, please verify your pool details", (string)cmd.pool.uuid);
                    logger.Error(details);
                }

                String uuid = null;
                var poolInfo = new
                {
                    uuid = uuid,
                    host = cmd.pool.host,
                    hostPath = cmd.pool.path,
                    localPath = hostPath,
                    poolType = cmd.pool.type,
                    capacityBytes = capacityBytes,
                    availableBytes = availableBytes
                };


                ansContent = new
                {
                    result = result,
                    details = details,
                    localPath = hostPath,
                    templateInfo = tInfo,
                    poolInfo = poolInfo,
                    contextMap = contextMap
                };

                if (result)
                {
                    try
                    {
                        if ((bool)cmd.add)
                        {
                            logger.Info("Adding HeartBeat Task to task scheduler for pool " + (string)cmd.pool.uuid);
                            Utils.AddHeartBeatTask((string)cmd.pool.uuid, hostPath, config.PrivateIpAddress);
                        }
                        else
                        {
                            logger.Info("Deleting HeartBeat Task from task scheduler for pool " + (string)cmd.pool.uuid);
                            Utils.RemoveHeartBeatTask(cmd.pool.uuid);
                        }
                    }
                    catch (Exception e)
                    {
                        logger.Error("Error occurred in adding/delete HeartBeat Task to/from Task Scheduler : " + e.Message);
                    }
                }

                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.ModifyStoragePoolAnswer);
            }
        }

        private bool ValidateStoragePoolCommand(dynamic cmd, out string localPath, out StoragePoolType poolType, ref string details)
        {
            dynamic pool = cmd.pool;
            string poolTypeStr = pool.type;
            localPath = cmd.localPath;
            if (!Enum.TryParse<StoragePoolType>(poolTypeStr, out poolType))
            {
                details = "Request to create / modify unsupported pool type: " + (poolTypeStr == null ? "NULL" : poolTypeStr) + "in cmd " + JsonConvert.SerializeObject(cmd);
                logger.Error(details);
                return false;
            }

            if (poolType != StoragePoolType.Filesystem &&
                poolType != StoragePoolType.NetworkFilesystem &&
                poolType != StoragePoolType.SMB)
            {
                details = "Request to create / modify unsupported pool type: " + (poolTypeStr == null ? "NULL" : poolTypeStr) + "in cmd " + JsonConvert.SerializeObject(cmd);
                logger.Error(details);
                return false;
            }

            return true;
        }

        // POST api/HypervResource/PlugNicCommand
        [HttpPost]
        [ActionName(CloudStackTypes.PlugNicCommand)]
        public JContainer PlugNicCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.PlugNicCommand + Utils.CleanString(cmd.ToString()));
                object ansContent = new
                {
                    result = true,
                    details = "Hot Nic plug not supported, change any empty virtual network adapter network settings",
                    contextMap = contextMap
                };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.PlugNicAnswer);
            }
        }


        // POST api/HypervResource/CleanupNetworkRulesCmd
        [HttpPost]
        [ActionName(CloudStackTypes.CleanupNetworkRulesCmd)]
        public JContainer CleanupNetworkRulesCmd([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.CleanupNetworkRulesCmd + Utils.CleanString(cmd.ToString()));
                object ansContent = new
                 {
                     result = false,
                     details = "nothing to cleanup in our current implementation",
                     contextMap = contextMap
                 };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.Answer);
            }
        }

        // POST api/HypervResource/CheckNetworkCommand
        [HttpPost]
        [ActionName(CloudStackTypes.CheckNetworkCommand)]
        public JContainer CheckNetworkCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.CheckNetworkCommand + Utils.CleanString(cmd.ToString()));
                object ansContent = new
                {
                    result = true,
                    details = (string)null,
                    contextMap = contextMap
                };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.CheckNetworkAnswer);
            }
        }

        // POST api/HypervResource/ReadyCommand
        [HttpPost]
        [ActionName(CloudStackTypes.ReadyCommand)]
        public JContainer ReadyCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.ReadyCommand + Utils.CleanString(cmd.ToString()));
                object ansContent = new
                {
                    result = true,
                    details = (string)null,
                    contextMap = contextMap
                };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.ReadyAnswer);
            }

        }

        // POST api/HypervResource/StartCommand
        [HttpPost]
        [ActionName(CloudStackTypes.StartCommand)]
        public JContainer StartCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.StartCommand + Utils.CleanString(cmd.ToString()));
                string details = null;
                bool result = false;

                try
                {
                    string systemVmIsoPath = null;
                    String uriStr = (String)cmd.secondaryStorage;
                    if (!String.IsNullOrEmpty(uriStr))
                    {
                        NFSTO share = new NFSTO();
                        share.uri = new Uri(uriStr);
                        string defaultDataPath = wmiCallsV2.GetDefaultDataRoot();
                        string secondaryPath = Utils.NormalizePath(Path.Combine(share.UncPath, "systemvm"));
                        string[] choices = Directory.GetFiles(secondaryPath, "systemvm*.iso");
                        if (choices.Length != 1)
                        {
                            String errMsg = "Couldn't locate the systemvm iso on " + secondaryPath;
                            logger.Error(errMsg);
                        }
                        else
                        {
                            systemVmIsoPath = choices[0];
                        }
                    }
                    wmiCallsV2.DeployVirtualMachine(cmd, systemVmIsoPath);
                    result = true;
                }
                catch (Exception wmiEx)
                {
                    details = CloudStackTypes.StartCommand + " fail on exception" + wmiEx.Message;
                    logger.Error(details, wmiEx);
                }

                object ansContent = new
                {
                    result = result,
                    details = details,
                    vm = cmd.vm,
                    contextMap = contextMap
                };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.StartAnswer);
            }
        }

        // POST api/HypervResource/StopCommand
        [HttpPost]
        [ActionName(CloudStackTypes.StopCommand)]
        public JContainer StopCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.StopCommand + Utils.CleanString(cmd.ToString()));
                string details = null;
                bool result = false;
                bool checkBeforeCleanup = cmd.checkBeforeCleanup;
                String vmName = cmd.vmName;

                if (checkBeforeCleanup == true)
                {
                    ComputerSystem vm = wmiCallsV2.GetComputerSystem(vmName);
                    if (vm == null || vm.EnabledState == 2)
                    {
                        // VM is not available or vm is not in running state
                        return ReturnCloudStackTypedJArray(new { result = false, details = "VM is not available or vm is not running on host, bailing out", vm = vmName, contextMap = contextMap }, CloudStackTypes.StopAnswer);
                    }
                }
                try
                {
                    wmiCallsV2.DestroyVm(cmd);
                    result = true;
                }
                catch (Exception wmiEx)
                {
                    details = CloudStackTypes.StopCommand + " fail on exception" + wmiEx.Message;
                    logger.Error(details, wmiEx);
                }

                object ansContent = new
                {
                    result = result,
                    details = details,
                    vm = cmd.vm,
                    contextMap = contextMap
                };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.StopAnswer);
            }
        }

        // POST api/HypervResource/CreateObjectCommand
        [HttpPost]
        [ActionName(CloudStackTypes.CreateObjectCommand)]
        public JContainer CreateObjectCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.CreateObjectCommand + Utils.CleanString(cmd.ToString()));

                bool result = false;
                string details = null;
                object newData = null;
                try
                {
                    VolumeObjectTO volume = VolumeObjectTO.ParseJson(cmd.data);
                    PrimaryDataStoreTO primary = volume.primaryDataStore;
                    ulong volumeSize = volume.size;
                    string volumeName = volume.uuid + ".vhdx";
                    string volumePath = null;

                    if (primary.isLocal)
                    {
                        volumePath = Path.Combine(primary.Path, volumeName);
                    }
                    else
                    {
                        volumePath = @"\\" + primary.uri.Host + primary.uri.LocalPath + @"\" + volumeName;
                        volumePath = Utils.NormalizePath(volumePath);
                    }
                    volume.path = volume.uuid;
                    wmiCallsV2.CreateDynamicVirtualHardDisk(volumeSize, volumePath);
                    if (File.Exists(volumePath))
                    {
                        result = true;
                        JObject ansObj = Utils.CreateCloudStackObject(CloudStackTypes.VolumeObjectTO, volume);
                        newData = ansObj;
                    }
                    else
                    {
                        details = "Failed to create disk with name " + volumePath;
                    }
                }
                catch (Exception ex)
                {
                    // Test by providing wrong key
                    details = CloudStackTypes.CreateObjectCommand + " failed on exception, " + ex.Message;
                    logger.Error(details, ex);
                }

                object ansContent = new
                {
                    result = result,
                    details = details,
                    data = newData,
                    contextMap = contextMap
                };

                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.CreateObjectAnswer);
            }
        }

        // POST api/HypervResource/MaintainCommand
        // TODO: should this be a NOP?
        [HttpPost]
        [ActionName(CloudStackTypes.MaintainCommand)]
        public JContainer MaintainCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.MaintainCommand + Utils.CleanString(cmd.ToString()));

                object ansContent = new
                {
                    result = true,
                    willMigrate = true,
                    details = "success - NOP for MaintainCommand",
                    _reconnect = false,
                    contextMap = contextMap
                };

                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.MaintainAnswer);
            }
        }

        // POST api/HypervResource/PingRoutingCommand
        // TODO: should this be a NOP?
        [HttpPost]
        [ActionName(CloudStackTypes.PingRoutingCommand)]
        public JContainer PingRoutingCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.PingRoutingCommand + Utils.CleanString(cmd.ToString()));

                object ansContent = new
                {
                    result = true,
                    details = "success - NOP for PingRoutingCommand",
                    _reconnect = false,
                    contextMap = contextMap
                };

                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.Answer);
            }
        }

        // POST api/HypervResource/PingCommand
        // TODO: should this be a NOP?
        [HttpPost]
        [ActionName(CloudStackTypes.PingCommand)]
        public JContainer PingCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.PingCommand + Utils.CleanString(cmd.ToString()));

                object ansContent = new
                {
                    result = true,
                    details = "success - NOP for PingCommand",
                    _reconnect = false,
                    contextMap = contextMap
                };

                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.Answer);
            }
        }

        // POST api/HypervResource/ModifyVmVnicVlanCommand
        [HttpPost]
        [ActionName(CloudStackTypes.ModifyVmNicConfigCommand)]
        public JContainer ModifyVmNicConfigCommand([FromBody]dynamic cmd)
        {

            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.ModifyVmNicConfigCommand + Utils.CleanString(cmd.ToString()));
                bool result = false;
                String vmName = cmd.vmName;
                String vlan = cmd.vlan;
                string macAddress = cmd.macAddress;
                uint pos = cmd.index;
                bool enable = cmd.enable;
                string switchLableName = cmd.switchLableName;
                if (macAddress != null)
                {
                    wmiCallsV2.ModifyVmVLan(vmName, vlan, macAddress);
                    result = true;
                }
                else if (pos >= 1)
                {
                    wmiCallsV2.ModifyVmVLan(vmName, vlan, pos, enable, switchLableName);
                    result = true;
                }

                object ansContent = new
                {
                    vmName = vmName,
                    result = result,
                    contextMap = contextMap
                };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.ModifyVmNicConfigAnswer);
            }

        }

        // POST api/HypervResource/GetVmConfigCommand
        [HttpPost]
        [ActionName(CloudStackTypes.GetVmConfigCommand)]
        public JContainer GetVmConfigCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.GetVmConfigCommand + Utils.CleanString(cmd.ToString()));
                bool result = false;
                String vmName = cmd.vmName;
                ComputerSystem vm = wmiCallsV2.GetComputerSystem(vmName);
                List<NicDetails> nicDetails = new List<NicDetails>();
                var nicSettingsViaVm = wmiCallsV2.GetEthernetPortSettings(vm);
                NicDetails nic = null;
                int index = 0;
                int[] nicStates = new int[8];
                int[] nicVlan = new int[8];
                int vlanid = 1;

                var ethernetConnections = wmiCallsV2.GetEthernetConnections(vm);
                foreach (EthernetPortAllocationSettingData item in ethernetConnections)
                {
                    EthernetSwitchPortVlanSettingData vlanSettings = wmiCallsV2.GetVlanSettings(item);
                    if (vlanSettings == null)
                    {
                        vlanid = -1;
                    }
                    else
                    {
                        vlanid = vlanSettings.AccessVlanId;
                    }
                    nicStates[index] = (Int32)(item.EnabledState);
                    nicVlan[index] = vlanid;
                    index++;
                }

                index = 0;
                foreach (SyntheticEthernetPortSettingData item in nicSettingsViaVm)
                {
                    nic = new NicDetails(item.Address, nicVlan[index], nicStates[index]);
                    index++;
                    nicDetails.Add(nic);
                }


                result = true;

                object ansContent = new
                {
                    vmName = vmName,
                    nics = nicDetails,
                    result = result,
                    contextMap = contextMap
                };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.GetVmConfigAnswer);
            }
        }



        // POST api/HypervResource/GetVmStatsCommand
        [HttpPost]
        [ActionName(CloudStackTypes.GetVmStatsCommand)]
        public JContainer GetVmStatsCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.GetVmStatsCommand + Utils.CleanString(cmd.ToString()));
                bool result = false;
                JArray vmNamesJson = cmd.vmNames;
                string[] vmNames = vmNamesJson.ToObject<string[]>();
                Dictionary<string, VmStatsEntry> vmProcessorInfo = new Dictionary<string, VmStatsEntry>(vmNames.Length);

                var vmsToInspect = new List<System.Management.ManagementPath>();
                foreach (var vmName in vmNames)
                {
                    var sys = wmiCallsV2.GetComputerSystem(vmName);
                    if (sys == null)
                    {
                        logger.InfoFormat("GetVmStatsCommand requested unknown VM {0}", vmNames);
                        continue;
                    }
                    var sysInfo = wmiCallsV2.GetVmSettings(sys);
                    vmsToInspect.Add(sysInfo.Path);
                }

                wmiCallsV2.GetSummaryInfo(vmProcessorInfo, vmsToInspect);

                // TODO: Network usage comes from Performance Counter API; however it is only available in kb/s, and not in total terms.
                // Curious about these?  Use perfmon to inspect them, e.g. http://msdn.microsoft.com/en-us/library/xhcx5a20%28v=vs.100%29.aspx
                // Recent post on these counter at http://blogs.technet.com/b/cedward/archive/2011/07/19/hyper-v-networking-optimizations-part-6-of-6-monitoring-hyper-v-network-consumption.aspx
                result = true;

                object ansContent = new
                {
                    vmStatsMap = vmProcessorInfo,
                    result = result,
                    contextMap = contextMap
                };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.GetVmStatsAnswer);
            }
        }

        // POST api/HypervResource/CopyCommand
        [HttpPost]
        [ActionName(CloudStackTypes.CopyCommand)]
        public JContainer CopyCommand(dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                // Log command *after* we've removed security details from the command.
                logger.Info(CloudStackTypes.CopyCommand + Utils.CleanString(cmd.ToString()));

                bool result = false;
                string details = null;
                object newData = null;
                TemplateObjectTO destTemplateObjectTO = null;
                VolumeObjectTO destVolumeObjectTO = null;
                VolumeObjectTO srcVolumeObjectTO = null;
                TemplateObjectTO srcTemplateObjectTO = null;

                try
                {
                    srcTemplateObjectTO = TemplateObjectTO.ParseJson(cmd.srcTO);
                    destTemplateObjectTO = TemplateObjectTO.ParseJson(cmd.destTO);
                    srcVolumeObjectTO = VolumeObjectTO.ParseJson(cmd.srcTO);
                    destVolumeObjectTO = VolumeObjectTO.ParseJson(cmd.destTO);

                    string destFile = null;
                    if (destTemplateObjectTO != null)
                    {
                        if (destTemplateObjectTO.path == null)
                        {
                            destTemplateObjectTO.path = System.Guid.NewGuid().ToString();
                        }
                        if (destTemplateObjectTO.primaryDataStore != null)
                        {
                            destFile = destTemplateObjectTO.FullFileName;
                        }
                        else if (destTemplateObjectTO.nfsDataStoreTO != null)
                        {
                            destFile = destTemplateObjectTO.FullFileName;
                        }
                    }

                    // Create local copy of a template?
                    if (srcTemplateObjectTO != null && destTemplateObjectTO != null)
                    {
                        // S3 download to primary storage?
                        // NFS provider download to primary storage?
                        if ((srcTemplateObjectTO.s3DataStoreTO != null || srcTemplateObjectTO.nfsDataStoreTO != null) && destTemplateObjectTO.primaryDataStore != null)
                        {
                            if (File.Exists(destFile))
                            {
                                logger.Info("Deleting existing file " + destFile);
                                File.Delete(destFile);
                            }

                            if (srcTemplateObjectTO.s3DataStoreTO != null)
                            {
                                // Download from S3 to destination data storage
                                DownloadS3ObjectToFile(srcTemplateObjectTO.path, srcTemplateObjectTO.s3DataStoreTO, destFile);
                            }
                            else if (srcTemplateObjectTO.nfsDataStoreTO != null)
                            {
                                // Download from S3 to destination data storage
                                Utils.DownloadCifsFileToLocalFile(srcTemplateObjectTO.path, srcTemplateObjectTO.nfsDataStoreTO, destFile);
                            }

                            // Uncompress, as required
                            if (srcTemplateObjectTO.path.EndsWith(".bz2"))
                            {
                                String uncompressedFile = destFile + ".tmp";
                                String compressedFile = destFile;
                                using (var uncompressedOutStrm = new FileStream(uncompressedFile, FileMode.CreateNew, FileAccess.Write))
                                {
                                    using (var compressedInStrm = new FileStream(destFile, FileMode.Open, FileAccess.Read))
                                    {
                                        using (var bz2UncompressorStrm = new Ionic.BZip2.BZip2InputStream(compressedInStrm, true) /* outer 'using' statement will close FileStream*/ )
                                        {
                                            int count = 0;
                                            int bufsize = 1024 * 1024;
                                            byte[] buf = new byte[bufsize];

                                            // EOF returns -1, see http://dotnetzip.codeplex.com/workitem/16069
                                            while (0 < (count = bz2UncompressorStrm.Read(buf, 0, bufsize)))
                                            {
                                                uncompressedOutStrm.Write(buf, 0, count);
                                            }
                                        }
                                    }
                                }
                                File.Delete(compressedFile);
                                File.Move(uncompressedFile, compressedFile);
                                if (File.Exists(uncompressedFile))
                                {
                                    String errMsg = "Extra file left around called " + uncompressedFile + " when creating " + destFile;
                                    logger.Error(errMsg);
                                    throw new IOException(errMsg);
                                }
                            }

                            // assert
                            if (!File.Exists(destFile))
                            {
                                String errMsg = "Failed to create " + destFile + " , because the file is missing";
                                logger.Error(errMsg);
                                throw new IOException(errMsg);
                            }

                            FileInfo destFileInfo = new FileInfo(destFile);
                            destTemplateObjectTO.size = destFileInfo.Length.ToString();
                            JObject ansObj = Utils.CreateCloudStackObject(CloudStackTypes.TemplateObjectTO, destTemplateObjectTO);
                            newData = ansObj;
                            result = true;
                        }
                        else
                        {
                            details = "Data store combination not supported";
                        }
                    }
                    // Create volume from a template?
                    else if (srcTemplateObjectTO != null && destVolumeObjectTO != null)
                    {
                        // VolumeObjectTO guesses file extension based on existing files
                        // this can be wrong if the previous file had a different file type
                        var guessedDestFile = destVolumeObjectTO.FullFileName;
                        if (File.Exists(guessedDestFile))
                        {
                            logger.Info("Deleting existing file " + guessedDestFile);
                            File.Delete(guessedDestFile);
                        }

                        destVolumeObjectTO.format = srcTemplateObjectTO.format;
                        destFile = destVolumeObjectTO.FullFileName;
                        if (File.Exists(destFile))
                        {
                            logger.Info("Deleting existing file " + destFile);
                            File.Delete(destFile);
                        }

                        string srcFile = srcTemplateObjectTO.FullFileName;
                        if (!File.Exists(srcFile))
                        {
                            details = "Local template file missing from " + srcFile;
                        }
                        else
                        {
                            // TODO: thin provision instead of copying the full file.
                            File.Copy(srcFile, destFile);
                            destVolumeObjectTO.path = destVolumeObjectTO.uuid;
                            JObject ansObj = Utils.CreateCloudStackObject(CloudStackTypes.VolumeObjectTO, destVolumeObjectTO);
                            newData = ansObj;
                            result = true;
                        }
                    }
                    else if (srcVolumeObjectTO != null && destVolumeObjectTO != null)
                    {
                        var guessedDestFile = destVolumeObjectTO.FullFileName;
                        if (File.Exists(guessedDestFile))
                        {
                            logger.Info("Deleting existing file " + guessedDestFile);
                            File.Delete(guessedDestFile);
                        }

                        destVolumeObjectTO.format = srcVolumeObjectTO.format;
                        destFile = destVolumeObjectTO.FullFileName;
                        if (File.Exists(destFile))
                        {
                            logger.Info("Deleting existing file " + destFile);
                            File.Delete(destFile);
                        }

                        string srcFile = srcVolumeObjectTO.FullFileName;
                        if (!File.Exists(srcFile))
                        {
                            details = "Local template file missing from " + srcFile;
                        }
                        else
                        {
                            // Create the directory before copying the files. CreateDirectory
                            // doesn't do anything if the directory is already present.
                            Directory.CreateDirectory(Path.GetDirectoryName(destFile));
                            File.Copy(srcFile, destFile);

                            if (srcVolumeObjectTO.nfsDataStore != null && srcVolumeObjectTO.primaryDataStore == null)
                            {
                                logger.Info("Copied volume from secondary data store to primary. Path: " + destVolumeObjectTO.path);
                            }
                            else if (srcVolumeObjectTO.primaryDataStore != null && srcVolumeObjectTO.nfsDataStore == null)
                            {
                                destVolumeObjectTO.path = destVolumeObjectTO.path + "/" + destVolumeObjectTO.uuid;
                                if (destVolumeObjectTO.format != null)
                                {
                                    destVolumeObjectTO.path += "." + destVolumeObjectTO.format.ToLower();
                                }
                            }
                            else
                            {
                                logger.Error("Destination volume path wasn't set. Unsupported source volume data store.");
                            }

                            // Create volumeto object deserialize and send it
                            JObject ansObj = Utils.CreateCloudStackObject(CloudStackTypes.VolumeObjectTO, destVolumeObjectTO);
                            newData = ansObj;
                            result = true;
                        }
                    }
                    else if (srcVolumeObjectTO != null && destTemplateObjectTO != null)
                    {
                        var guessedDestFile = destTemplateObjectTO.FullFileName;
                        if (File.Exists(guessedDestFile))
                        {
                            logger.Info("Deleting existing file " + guessedDestFile);
                            File.Delete(guessedDestFile);
                        }

                        destTemplateObjectTO.format = srcVolumeObjectTO.format;
                        destFile = destTemplateObjectTO.FullFileName;
                        if (File.Exists(destFile))
                        {
                            logger.Info("Deleting existing file " + destFile);
                            File.Delete(destFile);
                        }

                        string srcFile = srcVolumeObjectTO.FullFileName;
                        if (!File.Exists(srcFile))
                        {
                            details = "Local template file missing from " + srcFile;
                        }
                        else
                        {
                            // Create the directory before copying the files. CreateDirectory
                            // doesn't do anything if the directory is already present.
                            Directory.CreateDirectory(Path.GetDirectoryName(destFile));
                            File.Copy(srcFile, destFile);

                            FileInfo destFileInfo = new FileInfo(destFile);
                            // Write the template.properties file
                            PostCreateTemplate(Path.GetDirectoryName(destFile), destTemplateObjectTO.id, destTemplateObjectTO.name,
                                destFileInfo.Length.ToString(), srcVolumeObjectTO.size.ToString(), destTemplateObjectTO.format);

                            TemplateObjectTO destTemplateObject = new TemplateObjectTO();
                            destTemplateObject.size = srcVolumeObjectTO.size.ToString();
                            destTemplateObject.format = srcVolumeObjectTO.format;
                            destTemplateObject.path = destTemplateObjectTO.path + "/" + destTemplateObjectTO.uuid;
                            if (destTemplateObject.format != null)
                            {
                                destTemplateObject.path += "." + destTemplateObject.format.ToLower();
                            }
                            destTemplateObject.nfsDataStoreTO = destTemplateObjectTO.nfsDataStoreTO;
                            destTemplateObject.checksum = destTemplateObjectTO.checksum;
                            JObject ansObj = Utils.CreateCloudStackObject(CloudStackTypes.TemplateObjectTO, destTemplateObject);
                            newData = ansObj;
                            result = true;
                        }
                    }
                    else
                    {
                        details = "Data store combination not supported";
                    }
                }
                catch (Exception ex)
                {
                    // Test by providing wrong key
                    details = CloudStackTypes.CopyCommand + " failed on exception, " + ex.Message;
                    logger.Error(details, ex);
                }

                object ansContent = new
                {
                    result = result,
                    details = details,
                    newData = newData,
                    contextMap = contextMap
                };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.CopyCmdAnswer);
            }
        }

        private static void PostCreateTemplate(string path, string templateId, string templateUuid, string physicalSize, string virtualSize, string format)
        {
            string templatePropFile = Path.Combine(path, "template.properties");
            using (StreamWriter sw = new StreamWriter(File.Open(templatePropFile, FileMode.Create), Encoding.GetEncoding("iso-8859-1")))
            {
                if (format != null)
                {
                    format = format.ToLower();
                }

                sw.NewLine = "\n";
                sw.WriteLine("id=" + templateId);
                sw.WriteLine("filename=" + templateUuid + "." + format);
                sw.WriteLine(format + ".filename=" + templateUuid + "." + format);
                sw.WriteLine("uniquename=" + templateUuid);
                sw.WriteLine(format + "=true");
                sw.WriteLine("virtualsize=" + virtualSize);
                sw.WriteLine(format + ".virtualsize=" + virtualSize);
                sw.WriteLine("size=" + physicalSize);
                sw.WriteLine(format + ".size=" + physicalSize);
                sw.WriteLine("public=false");
            }
        }

        private static bool VerifyChecksum(string destFile, string checksum)
        {
            string localChecksum = BitConverter.ToString(CalcFileChecksum(destFile)).Replace("-", "").ToLower();
            logger.Debug("Checksum of " + destFile + " is " + checksum);
            if (checksum.Equals(localChecksum))
            {
                return true;
            }
            return false;
        }

        /// <summary>
        /// Match implmentation of DownloadManagerImpl.computeCheckSum
        /// </summary>
        /// <param name="destFile"></param>
        /// <returns></returns>
        private static byte[] CalcFileChecksum(string destFile)
        {
            // TODO:  Add unit test to verify that checksum algorithm has not changed.
            using (MD5 md5 = MD5.Create())
            {
                using (FileStream stream = File.OpenRead(destFile))
                {
                    return md5.ComputeHash(stream);
                }
            }
        }

        private static void DownloadS3ObjectToFile(string srcObjectKey, S3TO srcS3TO, string destFile)
        {
            AmazonS3Config S3Config = new AmazonS3Config
            {
                ServiceURL = srcS3TO.endpoint,
                CommunicationProtocol = Amazon.S3.Model.Protocol.HTTP
            };

            if (srcS3TO.httpsFlag)
            {
                S3Config.CommunicationProtocol = Protocol.HTTPS;
            }

            try
            {
                using (AmazonS3 client = Amazon.AWSClientFactory.CreateAmazonS3Client(srcS3TO.accessKey, srcS3TO.secretKey, S3Config))
                {
                    GetObjectRequest getObjectRequest = new GetObjectRequest().WithBucketName(srcS3TO.bucketName).WithKey(srcObjectKey);

                    using (S3Response getObjectResponse = client.GetObject(getObjectRequest))
                    {
                        using (Stream s = getObjectResponse.ResponseStream)
                        {
                            using (FileStream fs = new FileStream(destFile, FileMode.Create, FileAccess.Write))
                            {
                                byte[] data = new byte[524288];
                                int bytesRead = 0;
                                do
                                {
                                    bytesRead = s.Read(data, 0, data.Length);
                                    fs.Write(data, 0, bytesRead);
                                }
                                while (bytesRead > 0);
                                fs.Flush();
                            }
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                string errMsg = "Download from S3 url" + srcS3TO.endpoint + " said: " + ex.Message;
                logger.Error(errMsg, ex);
                throw new Exception(errMsg, ex);
            }
        }

        // POST api/HypervResource/GetStorageStatsCommand
        [HttpPost]
        [ActionName(CloudStackTypes.GetStorageStatsCommand)]
        public JContainer GetStorageStatsCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.GetStorageStatsCommand + Utils.CleanString(cmd.ToString()));
                bool result = false;
                string details = null;
                long capacity = 0;
                long available = 0;
                long used = 0;
                try
                {
                    StoragePoolType poolType;
                    string hostPath = null;
                    if (!Enum.TryParse<StoragePoolType>((string)cmd.pooltype, out poolType))
                    {
                        details = "Request to get unsupported pool type: " + ((string)cmd.pooltype == null ? "NULL" : (string)cmd.pooltype) + "in cmd " +
                            JsonConvert.SerializeObject(cmd);
                        logger.Error(details);
                    }
                    else if (poolType == StoragePoolType.Filesystem)
                    {
                        hostPath = (string)cmd.localPath;
                        GetCapacityForPath(hostPath, out capacity, out available);
                        used = capacity - available;
                        result = true;
                    }
                    else if (poolType == StoragePoolType.NetworkFilesystem || poolType == StoragePoolType.SMB)
                    {
                        string sharePath = config.getPrimaryStorage((string)cmd.id);
                        if (sharePath != null)
                        {
                            hostPath = sharePath;
                            Utils.GetShareDetails(sharePath, out capacity, out available);
                            used = capacity - available;
                            result = true;
                        }
                    }
                    else
                    {
                        result = false;
                    }

                    if (result)
                    {
                        logger.Debug(CloudStackTypes.GetStorageStatsCommand + " set used bytes for " + hostPath + " to " + used);
                    }
                }
                catch (Exception ex)
                {
                    details = CloudStackTypes.GetStorageStatsCommand + " failed on exception" + ex.Message;
                    logger.Error(details, ex);
                }

                object ansContent = new
                {
                    result = result,
                    details = details,
                    capacity = capacity,
                    used = used,
                    contextMap = contextMap
                };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.GetStorageStatsAnswer);
            }
        }

        // POST api/HypervResource/GetHostStatsCommand
        [HttpPost]
        [ActionName(CloudStackTypes.GetHostStatsCommand)]
        public JContainer GetHostStatsCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.GetHostStatsCommand + Utils.CleanString(cmd.ToString()));
                bool result = false;
                string details = null;
                object hostStats = null;

                var entityType = "host";
                ulong totalMemoryKBs;
                ulong freeMemoryKBs;
                double networkReadKBs;
                double networkWriteKBs;
                double cpuUtilization;

                try
                {
                    long hostId = (long)cmd.hostId;
                    wmiCallsV2.GetMemoryResources(out totalMemoryKBs, out freeMemoryKBs);
                    wmiCallsV2.GetProcessorUsageInfo(out cpuUtilization);

                    // TODO: can we assume that the host has only one adaptor?
                    string tmp;
                    var privateNic = GetNicInfoFromIpAddress(config.PrivateIpAddress, out tmp);
                    var nicStats = privateNic.GetIPv4Statistics();  //TODO: add IPV6 support, currentl
                    networkReadKBs = nicStats.BytesReceived;
                    networkWriteKBs = nicStats.BytesSent;

                    // Generate GetHostStatsAnswer
                    hostStats = new
                    {
                        hostId = hostId,
                        entityType = entityType,
                        cpuUtilization = cpuUtilization,
                        networkReadKBs = networkReadKBs,
                        networkWriteKBs = networkWriteKBs,
                        totalMemoryKBs = (double)totalMemoryKBs,
                        freeMemoryKBs = (double)freeMemoryKBs
                    };
                    result = true;
                }
                catch (Exception ex)
                {
                    details = CloudStackTypes.GetHostStatsCommand + " failed on exception" + ex.Message;
                    logger.Error(details, ex);
                }

                object ansContent = new
                {
                    result = result,
                    hostStats = hostStats,
                    details = details,
                    contextMap = contextMap
                };
                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.GetHostStatsAnswer);
            }
        }

        // POST api/HypervResource/PrepareForMigrationCommand
        [HttpPost]
        [ActionName(CloudStackTypes.PrepareForMigrationCommand)]
        public JContainer PrepareForMigrationCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.PrepareForMigrationCommand + Utils.CleanString(cmd.ToString()));

                string details = null;
                bool result = true;

                try
                {
                    details = "NOP - success";
                }
                catch (Exception sysEx)
                {
                    result = false;
                    details = CloudStackTypes.PrepareForMigrationCommand + " failed due to " + sysEx.Message;
                    logger.Error(details, sysEx);
                }

                object ansContent = new
                {
                    result = result,
                    details = details,
                    contextMap = contextMap
                };

                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.PrepareForMigrationAnswer);
            }
        }

        // POST api/HypervResource/MigrateCommand
        [HttpPost]
        [ActionName(CloudStackTypes.MigrateCommand)]
        public JContainer MigrateCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.MigrateCommand + Utils.CleanString(cmd.ToString()));

                string details = null;
                bool result = false;

                try
                {
                    string vm = (string)cmd.vmName;
                    string destination = (string)cmd.destIp;
                    wmiCallsV2.MigrateVm(vm, destination);
                    result = true;
                }
                catch (Exception sysEx)
                {
                    details = CloudStackTypes.MigrateCommand + " failed due to " + sysEx.Message;
                    logger.Error(details, sysEx);
                }

                object ansContent = new
                {
                    result = result,
                    details = details,
                    contextMap = contextMap
                };

                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.MigrateAnswer);
            }
        }

        // POST api/HypervResource/MigrateVolumeCommand
        [HttpPost]
        [ActionName(CloudStackTypes.MigrateVolumeCommand)]
        public JContainer MigrateVolumeCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.MigrateVolumeCommand + Utils.CleanString(cmd.ToString()));

                string details = null;
                bool result = false;

                try
                {
                    string vm = (string)cmd.attachedVmName;
                    string volume = (string)cmd.volumePath;
                    wmiCallsV2.MigrateVolume(vm, volume, GetStoragePoolPath(cmd.pool));
                    result = true;
                }
                catch (Exception sysEx)
                {
                    details = CloudStackTypes.MigrateVolumeCommand + " failed due to " + sysEx.Message;
                    logger.Error(details, sysEx);
                }

                object ansContent = new
                {
                    result = result,
                    volumePath = (string)cmd.volumePath,
                    details = details,
                    contextMap = contextMap
                };

                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.MigrateVolumeAnswer);
            }
        }

        // POST api/HypervResource/MigrateWithStorageCommand
        [HttpPost]
        [ActionName(CloudStackTypes.MigrateWithStorageCommand)]
        public JContainer MigrateWithStorageCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.MigrateWithStorageCommand + Utils.CleanString(cmd.ToString()));

                string details = null;
                bool result = false;
                List<dynamic> volumeTos = new List<dynamic>();

                try
                {
                    string vm = (string)cmd.vm.name;
                    string destination = (string)cmd.tgtHost;
                    var volumeToPoolList = cmd.volumeToFilerAsList;
                    var volumeToPool = new Dictionary<string, string>();
                    foreach (var item in volumeToPoolList)
                    {
                        volumeTos.Add(item.t);
                        string poolPath = GetStoragePoolPath(item.u);
                        volumeToPool.Add((string)item.t.path, poolPath);
                    }

                    wmiCallsV2.MigrateVmWithVolume(vm, destination, volumeToPool);
                    result = true;
                }
                catch (Exception sysEx)
                {
                    details = CloudStackTypes.MigrateWithStorageCommand + " failed due to " + sysEx.Message;
                    logger.Error(details, sysEx);
                }

                object ansContent = new
                {
                    result = result,
                    volumeTos = JArray.FromObject(volumeTos),
                    details = details,
                    contextMap = contextMap
                };

                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.MigrateWithStorageAnswer);
            }
        }

        private string GetStoragePoolPath(dynamic pool)
        {
            string poolTypeStr = pool.type;
            StoragePoolType poolType;
            if (!Enum.TryParse<StoragePoolType>(poolTypeStr, out poolType))
            {
                throw new ArgumentException("Invalid pool type " + poolTypeStr);
            }
            else if (poolType == StoragePoolType.SMB)
            {
                NFSTO share = new NFSTO();
                String uriStr = "cifs://" + (string)pool.host + (string)pool.path;
                share.uri = new Uri(uriStr);
                return Utils.NormalizePath(share.UncPath);
            }
            else if (poolType == StoragePoolType.Filesystem)
            {
                return pool.path;
            }

            throw new ArgumentException("Couldn't parse path for pool type " + poolTypeStr);
        }

        // POST api/HypervResource/StartupCommand
        [HttpPost]
        [ActionName(CloudStackTypes.StartupCommand)]
        public JContainer StartupCommand([FromBody]dynamic cmdArray)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(cmdArray.ToString());
                // Log agent configuration
                logger.Info("Agent StartupRoutingCommand received " + cmdArray.ToString());
                dynamic strtRouteCmd = cmdArray[0][CloudStackTypes.StartupRoutingCommand];

                // Insert networking details
                string privateIpAddress = strtRouteCmd.privateIpAddress;
                config.PrivateIpAddress = privateIpAddress;
                string subnet;
                System.Net.NetworkInformation.NetworkInterface privateNic = GetNicInfoFromIpAddress(privateIpAddress, out subnet);
                strtRouteCmd.privateIpAddress = privateIpAddress;
                strtRouteCmd.privateNetmask = subnet;
                strtRouteCmd.privateMacAddress = privateNic.GetPhysicalAddress().ToString();
                string storageip = strtRouteCmd.storageIpAddress;
                System.Net.NetworkInformation.NetworkInterface storageNic = GetNicInfoFromIpAddress(storageip, out subnet);

                strtRouteCmd.storageIpAddress = storageip;
                strtRouteCmd.storageNetmask = subnet;
                strtRouteCmd.storageMacAddress = storageNic.GetPhysicalAddress().ToString();
                strtRouteCmd.gatewayIpAddress = storageNic.GetPhysicalAddress().ToString();
                strtRouteCmd.hypervisorVersion = System.Environment.OSVersion.Version.Major.ToString() + "." +
                        System.Environment.OSVersion.Version.Minor.ToString();
                strtRouteCmd.caps = "hvm";

                dynamic details = strtRouteCmd.hostDetails;
                if (details != null)
                {
                    string productVersion = System.Environment.OSVersion.Version.Major.ToString() + "." +
                        System.Environment.OSVersion.Version.Minor.ToString();
                    details.Add("product_version", productVersion);
                    details.Add("rdp.server.port", 2179);
                }

                // Detect CPUs, speed, memory
                uint cores;
                uint mhz;
                uint sockets;
                wmiCallsV2.GetProcessorResources(out sockets, out cores, out mhz);
                strtRouteCmd.cpus = cores;
                strtRouteCmd.speed = mhz;
                strtRouteCmd.cpuSockets = sockets;
                ulong memoryKBs;
                ulong freeMemoryKBs;
                wmiCallsV2.GetMemoryResources(out memoryKBs, out freeMemoryKBs);
                strtRouteCmd.memory = memoryKBs * 1024;   // Convert to bytes

                // Need 2 Gig for DOM0, see http://technet.microsoft.com/en-us/magazine/hh750394.aspx
                strtRouteCmd.dom0MinMemory = config.ParentPartitionMinMemoryMb * 1024 * 1024;  // Convert to bytes

                // Insert storage pool details.
                //
                // Read the localStoragePath for virtual disks from the Hyper-V configuration
                // See http://blogs.msdn.com/b/virtual_pc_guy/archive/2010/05/06/managing-the-default-virtual-machine-location-with-hyper-v.aspx
                // for discussion of Hyper-V file locations paths.
                string virtualDiskFolderPath = wmiCallsV2.GetDefaultVirtualDiskFolder();
                if (virtualDiskFolderPath != null)
                {
                    // GUID arbitrary.  Host agents deals with storage pool in terms of localStoragePath.
                    // We use HOST guid.
                    string poolGuid = strtRouteCmd.guid;

                    if (poolGuid == null)
                    {
                        poolGuid = Guid.NewGuid().ToString();
                        logger.InfoFormat("Setting Startup StoragePool GUID to " + poolGuid);
                    }
                    else
                    {
                        logger.InfoFormat("Setting Startup StoragePool GUID same as HOST, i.e. " + poolGuid);
                    }

                    long capacity;
                    long available;
                    GetCapacityForPath(virtualDiskFolderPath, out capacity, out available);
                    logger.Debug(CloudStackTypes.StartupStorageCommand + " set available bytes to " + available);

                    string ipAddr = strtRouteCmd.privateIpAddress;
                    var vmStates = wmiCallsV2.GetVmSync(config.PrivateIpAddress);
                    strtRouteCmd.vms = Utils.CreateCloudStackMapObject(vmStates);

                    StoragePoolInfo pi = new StoragePoolInfo(
                        poolGuid.ToString(),
                        ipAddr,
                        virtualDiskFolderPath,
                        virtualDiskFolderPath,
                        StoragePoolType.Filesystem.ToString(),
                        capacity,
                        available);

                    // Build StartupStorageCommand using an anonymous type
                    // See http://stackoverflow.com/a/6029228/939250
                    object ansContent = new
                    {
                        poolInfo = pi,
                        guid = pi.uuid,
                        dataCenter = strtRouteCmd.dataCenter,
                        resourceType = StorageResourceType.STORAGE_POOL.ToString(),  // TODO: check encoding
                        contextMap = contextMap
                    };
                    JObject ansObj = Utils.CreateCloudStackObject(CloudStackTypes.StartupStorageCommand, ansContent);
                    cmdArray.Add(ansObj);
                }

                // Convert result to array for type correctness?
                logger.Info(CloudStackTypes.StartupCommand + " result is " + cmdArray.ToString());
                return cmdArray;
            }
        }

        // POST api/HypervResource/GetVncPortCommand
        [HttpPost]
        [ActionName(CloudStackTypes.GetVncPortCommand)]
        public JContainer GetVncPortCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.GetVncPortCommand + Utils.CleanString(cmd.ToString()));

                string details = null;
                bool result = false;
                string address = null;
                int port = -9;

                try
                {
                    string vmName = (string)cmd.name;
                    var sys = wmiCallsV2.GetComputerSystem(vmName);
                    address = "instanceId=" + sys.Name ;
                    result = true;
                }
                catch (Exception sysEx)
                {
                    details = CloudStackTypes.GetVncPortAnswer + " failed due to " + sysEx.Message;
                    logger.Error(details, sysEx);
                }

                object ansContent = new
                {
                    result = result,
                    details = details,
                    address = address,
                    port = port
                };

                return ReturnCloudStackTypedJArray(ansContent, CloudStackTypes.GetVncPortAnswer);
            }
        }

        // POST api/HypervResource/HostVmStateReportCommand
        [HttpPost]
        [ActionName(CloudStackTypes.HostVmStateReportCommand)]
        public JContainer HostVmStateReportCommand([FromBody]dynamic cmd)
        {
            using (log4net.NDC.Push(Guid.NewGuid().ToString()))
            {
                logger.Info(CloudStackTypes.HostVmStateReportCommand + Utils.CleanString(cmd.ToString()));

                string details = null;
                List<Dictionary<string, string>> hostVmStateReport = new List<Dictionary<string, string>>();

                try
                {
                    var vmCollection = wmiCallsV2.GetComputerSystemCollection();
                    foreach (ComputerSystem vm in vmCollection)
                    {
                        if (EnabledState.ToCloudStackPowerState(vm.EnabledState).Equals("PowerOn"))
                        {
                            var dict = new Dictionary<string, string>();
                            dict.Add(vm.ElementName, EnabledState.ToCloudStackPowerState(vm.EnabledState));
                            hostVmStateReport.Add(dict);
                        }
                        if (EnabledState.ToCloudStackPowerState(vm.EnabledState).Equals("PowerOff"))
                        {
                            string note = wmiCallsV2.GetVmNote((wmiCallsV2.GetVmSettings(vm)).Path);
                            if (note != null && note.Contains("CloudStack"))
                            {
                                if (!note.Contains("creating"))
                                {
                                    try
                                    {
                                        wmiCallsV2.DestroyVm(vm.ElementName);
                                    }
                                    catch (Exception ex)
                                    {
                                        logger.Error("Failed to delete stopped VMs due to " + ex.Message);
                                    }
                                }
                                else
                                {
                                    var dict = new Dictionary<string, string>();
                                    dict.Add(vm.ElementName, "PowerOn");
                                    hostVmStateReport.Add(dict);
                                }
                            }
                        }
                    }
                }
                catch (Exception sysEx)
                {
                    details = CloudStackTypes.HostVmStateReportCommand + " failed due to " + sysEx.Message;
                    logger.Error(details, sysEx);
                }

                var answer = JArray.FromObject(hostVmStateReport);
                logger.Info(String.Format("{0}: {1}",CloudStackTypes.HostVmStateReportCommand, answer.ToString()));

                return answer;
            }
        }

        public static System.Net.NetworkInformation.NetworkInterface GetNicInfoFromIpAddress(string ipAddress, out string subnet)
        {
            System.Net.NetworkInformation.NetworkInterface[] nics = System.Net.NetworkInformation.NetworkInterface.GetAllNetworkInterfaces();
            System.Net.NetworkInformation.NetworkInterface defaultnic = null;
            foreach (var nic in nics)
            {
                subnet = null;
                defaultnic = nic;
                // TODO: use to remove NETMASK and MAC from the config file, and to validate the IPAddress.
                var nicProps = nic.GetIPProperties();
                bool found = false;
                foreach (var addr in nicProps.UnicastAddresses)
                {
                    if (addr.Address.Equals(IPAddress.Parse(ipAddress)))
                    {
                        subnet = addr.IPv4Mask.ToString();
                        found = true;
                    }
                }
                if (!found)
                {
                    continue;
                }
                return nic;
            }
            var defaultSubnet = defaultnic.GetIPProperties().UnicastAddresses[0];
            subnet = defaultSubnet.IPv4Mask.ToString();
            return defaultnic;
        }

        public static void GetCapacityForLocalPath(string localStoragePath, out long capacityBytes, out long availableBytes)
        {
            // NB: DriveInfo does not work for remote folders (http://stackoverflow.com/q/1799984/939250)
            // DriveInfo requires a driver letter...
            string fullPath = Path.GetFullPath(localStoragePath);
            System.IO.DriveInfo poolInfo = new System.IO.DriveInfo(fullPath);
            capacityBytes = poolInfo.TotalSize;
            availableBytes = poolInfo.AvailableFreeSpace;

            // Don't allow all of the Root Device to be used for virtual disks
            if (poolInfo.RootDirectory.Name.ToLower().Equals(config.RootDeviceName))
            {
                availableBytes -= config.RootDeviceReservedSpaceBytes;
                availableBytes = availableBytes > 0 ? availableBytes : 0;
                capacityBytes -= config.RootDeviceReservedSpaceBytes;
                capacityBytes = capacityBytes > 0 ? capacityBytes : 0;
            }
        }

        public static void GetCapacityForPath(String hostPath, out long capacityBytes, out long availableBytes)
        {
            if (hostPath.Substring(0, 2) == "\\\\")
            {
                Utils.GetShareDetails(hostPath, out capacityBytes, out availableBytes);
            }
            else
            {
                GetCapacityForLocalPath(hostPath, out capacityBytes, out availableBytes);
            }
        }
    }
}
