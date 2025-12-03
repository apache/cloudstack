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
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

// C# versions of certain CloudStack types to simplify JSON serialisation.
// Limit to the number of types, because they are written and maintained manually.
// JsonProperty used to identify property name when serialised, which allows
// later adoption of C# naming conventions if requried. 
namespace HypervResource
{
    public class PrimaryDataStoreTO
    {
        private string path;
        public string host;
        private string poolType;
        public Uri uri;
        public string _role;

        public string Path
        {
            get
            {
                if (this.isLocal)
                {
                    return path;
                }
                else
                {
                    return this.UncPath;
                }
            }
            set
            {
                this.path = value;
            }
        }

        public string UncPath
        {
            get
            {
                string uncPath = null;
                if (uri != null && (uri.Scheme.Equals("cifs") || uri.Scheme.Equals("networkfilesystem") || uri.Scheme.Equals("smb")))
                {
                    uncPath = @"\\" + uri.Host + uri.LocalPath;
                }
                return uncPath;
            }
        }

        public Boolean isLocal
        {
            get
            {
                if (poolType.Equals("Filesystem"))
                {
                    return true;
                }
                else
                {
                    return false;
                }
            }
        }

        public static PrimaryDataStoreTO ParseJson(dynamic json)
        {
            PrimaryDataStoreTO result = null;
            if (json == null)
            {
                return result;
            }

            dynamic primaryDataStoreTOJson = json[CloudStackTypes.PrimaryDataStoreTO];
            if (primaryDataStoreTOJson != null)
            {
                result = new PrimaryDataStoreTO()
                {
                    path = (string)primaryDataStoreTOJson.path,
                    host = (string)primaryDataStoreTOJson.host,
                    poolType = (string)primaryDataStoreTOJson.poolType
                };

                if (!result.isLocal)
                {
                    // Delete security credentials in original command.  Prevents logger from spilling the beans, as it were.
                    String uriStr = @"cifs://" + result.host + result.path;
                    result.uri = new Uri(uriStr);
                }
            }
            return result;
        }
    }

    public class VolumeObjectTO
    {
        private static ILog logger = LogManager.GetLogger(typeof(VolumeObjectTO));

        public string FullFileName
        {
            get
            {
                string fileName = null;
                if (this.primaryDataStore != null)
                {
                    PrimaryDataStoreTO store = this.primaryDataStore;
                    if (store.isLocal)
                    {
                        String volume = this.path;
                        if (String.IsNullOrEmpty(volume))
                        {
                            volume = this.uuid;
                        }
                        fileName = Path.Combine(store.Path, volume);
                    }
                    else
                    {
                        String volume = this.path;
                        if (String.IsNullOrEmpty(volume))
                        {
                            volume = this.uuid;
                        }
                        fileName = @"\\" + store.uri.Host + store.uri.LocalPath + @"\" + volume;
                        fileName = Utils.NormalizePath(fileName);
                    }
                }
                else if (this.nfsDataStore != null)
                {
                    fileName = this.nfsDataStore.UncPath;
                    if (this.path != null)
                    {
                        fileName = Utils.NormalizePath(fileName + @"\" + this.path);
                    }

                    if (fileName != null && !File.Exists(fileName))
                    {
                        fileName = Utils.NormalizePath(fileName + @"\" + this.uuid);
                    }
                }
                else
                {
                    String errMsg = "Invalid dataStore in VolumeObjectTO spec";
                    logger.Error(errMsg);
                    throw new InvalidDataException(errMsg);
                }

                if (fileName != null && !Path.HasExtension(fileName) && this.format != null)
                {
                    fileName = fileName + "." + this.format.ToLowerInvariant();
                }

                return fileName;
            }
        }

        public dynamic dataStore;
        public string format;
        public string name;
        public string path;
        public string uuid;
        public ulong size;
        public PrimaryDataStoreTO primaryDataStore;
        public NFSTO nfsDataStore;

        public static VolumeObjectTO ParseJson(dynamic json)
        {
            VolumeObjectTO result = null;

            if (json == null)
            {
                return result;
            }

            dynamic volumeObjectTOJson = json[CloudStackTypes.VolumeObjectTO];
            if (volumeObjectTOJson != null)
            {
                result = new VolumeObjectTO()
                {
                    dataStore = volumeObjectTOJson.dataStore,
                    format = ((string)volumeObjectTOJson.format),
                    name = (string)volumeObjectTOJson.name,
                    path = volumeObjectTOJson.path,
                    uuid = (string)volumeObjectTOJson.uuid,
                    size = (ulong)volumeObjectTOJson.size
                };
                result.primaryDataStore = PrimaryDataStoreTO.ParseJson(volumeObjectTOJson.dataStore);
                result.nfsDataStore = NFSTO.ParseJson(volumeObjectTOJson.dataStore);

                // Assert
                if (result.dataStore == null || (result.primaryDataStore == null && result.nfsDataStore == null))
                {
                    String errMsg = "VolumeObjectTO missing dataStore in spec " + Utils.CleanString(volumeObjectTOJson.ToString());
                    logger.Error(errMsg);
                    throw new ArgumentNullException(errMsg);
                }

                GuessFileExtension(result);
            }
            return result;
        }

        private static void GuessFileExtension(VolumeObjectTO volInfo)
        {
            if (String.IsNullOrEmpty(volInfo.format))
            {
                logger.Info("No image format in VolumeObjectTO, going to use format from first file that matches " + volInfo.FullFileName);

                string path = null;
                if (volInfo.primaryDataStore != null)
                {
                    if (volInfo.primaryDataStore.isLocal)
                    {
                        path = volInfo.primaryDataStore.Path;
                    }
                    else
                    {
                        path = volInfo.primaryDataStore.UncPath;
                    }
                }
                else if (volInfo.nfsDataStore != null)
                {
                    path = volInfo.nfsDataStore.UncPath;
                    if (volInfo.path != null)
                    {
                        path += @"\" + volInfo.path;
                    }
                }
                else
                {
                    String errMsg = "VolumeObjectTO missing dataStore in spec " + Utils.CleanString(volInfo.ToString());
                    logger.Error(errMsg);
                    throw new ArgumentNullException(errMsg);
                }

                path = Utils.NormalizePath(path);
                if (Directory.Exists(path))
                {
                    string[] choices = Directory.GetFiles(path, volInfo.uuid + ".vhd*");
                    if (choices.Length != 1)
                    {
                        String errMsg = "Tried to guess file extension, but cannot find file corresponding to " +
                            Path.Combine(volInfo.primaryDataStore.Path, volInfo.uuid);
                        logger.Debug(errMsg);
                    }
                    else
                    {
                        string[] splitFileName = choices[0].Split(new char[] { '.' });
                        volInfo.format = splitFileName[splitFileName.Length - 1];
                    }
                }
                logger.Debug("Going to use file " + volInfo.FullFileName);
            }
        }
    }

    public class TemplateObjectTO
    {
        /// <summary>
        /// Full file name varies depending on whether the TemplateObjectTO has a path or not.
        /// If it has a path, we use that.  Otherwise, we build it from the name, extension and data store path.
        /// </summary>
        public string FullFileName
        {
            get
            {
                string fileName = null;
                if (this.primaryDataStore != null)
                {
                    PrimaryDataStoreTO store = this.primaryDataStore;
                    if (store.isLocal)
                    {
                        fileName = Path.Combine(store.Path, this.path);
                    }
                    else
                    {
                        fileName = @"\\" + store.uri.Host + store.uri.LocalPath + @"\" + this.path;
                    }
                    fileName = fileName + '.' + this.format.ToLowerInvariant();
                }
                else if (this.nfsDataStoreTO != null)
                {
                    fileName = this.nfsDataStoreTO.UncPath;
                    if (this.path != null)
                    {
                        fileName = Utils.NormalizePath(fileName + @"\" + this.path);
                    }

                    if (fileName != null && !File.Exists(fileName))
                    {
                        fileName = Utils.NormalizePath(fileName + @"\" + this.uuid);
                    }

                    if (!this.format.Equals("RAW"))
                    {
                        fileName = fileName + '.' + this.format.ToLowerInvariant();
                    }
                }
                else
                {
                    fileName = this.path;
                }
                return Utils.NormalizePath(fileName);
            }
        }

        public dynamic imageDataStore;
        public string format;
        public string name;
        public string uuid;
        public S3TO s3DataStoreTO = null;
        public NFSTO nfsDataStoreTO = null;
        public PrimaryDataStoreTO primaryDataStore = null;
        public string path;
        public string checksum;
        public string size;
        public string id;

        public static TemplateObjectTO ParseJson(dynamic json)
        {
            TemplateObjectTO result = null;
            dynamic templateObjectTOJson = json[CloudStackTypes.TemplateObjectTO];
            if (templateObjectTOJson != null)
            {
                result = new TemplateObjectTO()
                {
                    imageDataStore = templateObjectTOJson.imageDataStore,
                    format = (string)templateObjectTOJson.format,
                    name = (string)templateObjectTOJson.name,
                    uuid = (string)templateObjectTOJson.uuid,
                    path = (string)templateObjectTOJson.path,
                    checksum = (string)templateObjectTOJson.checksum,
                    size = (string)templateObjectTOJson.size,
                    id = (string)templateObjectTOJson.id
                };
                result.s3DataStoreTO = S3TO.ParseJson(templateObjectTOJson.imageDataStore);
                result.nfsDataStoreTO = NFSTO.ParseJson(templateObjectTOJson.imageDataStore);
                result.primaryDataStore = PrimaryDataStoreTO.ParseJson(templateObjectTOJson.imageDataStore);
            }

            return result;
        }
    }

    public class S3TO
    {
        public string bucketName;
        public string secretKey;
        public string accessKey;
        public string endpoint;
        public bool httpsFlag;

        public static S3TO ParseJson(dynamic json)
        {
            S3TO result = null;
            if (json != null)
            {
                dynamic s3TOJson = json[CloudStackTypes.S3TO];
                if (s3TOJson != null)
                {
                    result = new S3TO()
                    {
                        bucketName = (string)s3TOJson.bucketName,
                        secretKey = (string)s3TOJson.secretKey,
                        accessKey = (string)s3TOJson.accessKey,
                        endpoint = (string)s3TOJson.endPoint,
                        httpsFlag = (bool)s3TOJson.httpsFlag
                    };
                    // Delete security credentials in original command. Prevents logger from spilling the beans, as it were.
                    s3TOJson.secretKey = string.Empty;
                    s3TOJson.accessKey = string.Empty;
                }
            }
            return result;
        }
    }

    public class NFSTO
    {
        public Uri uri;
        public string _role;
        public string UncPath
        {
            get
            {
                string uncPath = null;
                if (uri.Scheme.Equals("cifs"))
                {
                    uncPath = @"\\" + uri.Host + uri.LocalPath;
                }
                return uncPath;
            }
        }

        public static NFSTO ParseJson(dynamic json)
        {
            NFSTO result = null;
            if (json != null)
            {
                dynamic nfsTOJson = json[CloudStackTypes.NFSTO];
                if (nfsTOJson != null)
                {
                    result = new NFSTO()
                    {
                        _role = (string)nfsTOJson._role,
                    };
                    // Delete security credentials in original command.  Prevents logger from spilling the beans, as it were.
                    String uriStr = (String)nfsTOJson._url;
                    result.uri = new Uri(uriStr);
                }
            }
            return result;
        }
    }

    public class DiskTO
    {
        public string type;
        public string diskSequence = null;
        public TemplateObjectTO templateObjectTO = null;
        public VolumeObjectTO volumeObjectTO = null;

        public static DiskTO ParseJson(dynamic json)
        {
            DiskTO result = null;
            if (json != null)
            {
                result = new DiskTO()
                {
                    templateObjectTO = TemplateObjectTO.ParseJson(json.data),
                    volumeObjectTO = VolumeObjectTO.ParseJson(json.data),
                    type = (string)json.type,
                    diskSequence = json.diskSeq
                };
            }

            return result;
        }
    }

    enum VolumeType
    {
        UNKNOWN,
        ROOT,
        SWAP,
        DATADISK,
        ISO
    };

    public enum StoragePoolType
    {
        /// <summary>
        /// local directory
        /// </summary>
        Filesystem,
        /// <summary>
        /// NFS or CIFS 
        /// </summary>
        NetworkFilesystem,
        /// <summary>
        /// shared LUN, with a clusterfs overlay 
        /// </summary>
        IscsiLUN,
        /// <summary>
        /// for e.g., ZFS Comstar 
        /// </summary>
        Iscsi,
        /// <summary>
        /// for iso image
        /// </summary>
        ISO,
        /// <summary>
        /// XenServer local LVM SR
        /// </summary>
        LVM,
        /// <summary>
        /// 
        /// </summary>
        CLVM,
        /// <summary>
        /// 
        /// </summary>
        RBD,
        /// <summary>
        /// 
        /// </summary>
        SharedMountPoint,
        /// <summary>
        /// VMware VMFS storage 
        /// </summary>
        VMFS,
        /// <summary>
        /// for XenServer, Storage Pool is set up by customers. 
        /// </summary>
        PreSetup,
        /// <summary>
        /// XenServer local EXT SR 
        /// </summary>
        EXT,
        /// <summary>
        /// 
        /// </summary>
        OCFS2,
        /// <summary>
        /// for hyper-v
        /// </summary>
        SMB
    }

    public enum StorageResourceType
    {
        STORAGE_POOL, STORAGE_HOST, SECONDARY_STORAGE, LOCAL_SECONDARY_STORAGE
    }

    public struct VolumeInfo
    {
#pragma warning disable 0414
        public long id;
        public string type;
        public string storagePoolType;
        public string storagePoolUuid;
        public string name;
        public string mountPoint;
        public string path;
        long size;
        string chainInfo;
#pragma warning restore 0414

        public VolumeInfo(long id, string type, string poolType, String poolUuid, String name, String mountPoint, String path, long size, String chainInfo)
        {
            this.id = id;
            this.name = name;
            this.path = path;
            this.size = size;
            this.type = type;
            this.storagePoolType = poolType;
            this.storagePoolUuid = poolUuid;
            this.mountPoint = mountPoint;
            this.chainInfo = chainInfo;
        }
    }

    public class VmState
    {
#pragma warning disable 0414
        [JsonProperty("state")]
        public String state;
        [JsonProperty("host")]
        String host;
#pragma warning restore 0414
        public VmState() { }
        public VmState(String vmState, String host)
        {
            this.state = vmState;
            this.host = host;
        }
    }

    public struct StoragePoolInfo
    {
#pragma warning disable 0414
        [JsonProperty("uuid")]
        public String uuid;
        [JsonProperty("host")]
        String host;
        [JsonProperty("localPath")]
        String localPath;
        [JsonProperty("hostPath")]
        String hostPath;
        [JsonProperty("poolType")]
        string poolType;
        [JsonProperty("capacityBytes")]
        long capacityBytes;

        // Management server copies this field to the 'used_byptes' in the database table 'storage_pool'.
        [JsonProperty("availableBytes")]
        long availableBytes;
        [JsonProperty("details")]
        Dictionary<String, String> details;
#pragma warning restore 0414

        public StoragePoolInfo(String uuid, String host, String hostPath,
                String localPath, string poolType, long capacityBytes,
                long availableBytes)
        {
            this.uuid = uuid;
            this.host = host;
            this.localPath = localPath;
            this.hostPath = hostPath;
            this.poolType = poolType;
            this.capacityBytes = capacityBytes;
            this.availableBytes = availableBytes;
            details = null;
        }

        public StoragePoolInfo(String uuid, String host, String hostPath,
                String localPath, string poolType, long capacityBytes,
                long availableBytes, Dictionary<String, String> details)
            : this(uuid, host, hostPath, localPath, poolType, capacityBytes, availableBytes)
        {
            this.details = details;
        }
    }

    public class VmStatsEntry
    {
        [JsonProperty("cpuUtilization")]
        public double cpuUtilization;
        [JsonProperty("networkReadKBs")]
        public double networkReadKBs;
        [JsonProperty("networkWriteKBs")]
        public double networkWriteKBs;
        [JsonProperty("numCPUs")]
        public int numCPUs;
        [JsonProperty("entityType")]
        public String entityType;
    }

    public class NicDetails
    {
        [JsonProperty("macAddress")]
        public string macaddress;
        [JsonProperty("vlanid")]
        public int vlanid;
        [JsonProperty("state")]
        public bool state;
        public NicDetails() { }
        public NicDetails(String macaddress, int vlanid, int enabledState)
        {
            this.macaddress = macaddress;
            this.vlanid = vlanid;
            if (enabledState == 2)
            {
                this.state = true;
            }
            else
            {
                this.state = false;
            }

        }
    }

    /// <summary>
    /// Fully qualified named for a number of types used in CloudStack.  Used to specify the intended type for JSON serialised objects. 
    /// </summary>
    public class CloudStackTypes
    {
        public const string Answer = "com.cloud.agent.api.Answer";
        public const string AttachIsoCommand = "com.cloud.agent.api.AttachIsoCommand";
        public const string AnsBackupSnapshotAnswerwer = "com.cloud.agent.api.BackupSnapshotAnswer";
        public const string BackupSnapshotCommand = "com.cloud.agent.api.BackupSnapshotCommand";
        public const string BumpUpPriorityCommand = "com.cloud.agent.api.BumpUpPriorityCommand";
        public const string CheckHealthAnswer = "com.cloud.agent.api.CheckHealthAnswer";
        public const string CheckHealthCommand = "com.cloud.agent.api.CheckHealthCommand";
        public const string CheckNetworkAnswer = "com.cloud.agent.api.CheckNetworkAnswer";
        public const string CheckNetworkCommand = "com.cloud.agent.api.CheckNetworkCommand";
        public const string CheckOnHostAnswer = "com.cloud.agent.api.CheckOnHostAnswer";
        public const string CheckOnHostCommand = "com.cloud.agent.api.CheckOnHostCommand";
        public const string CheckRouterAnswer = "com.cloud.agent.api.CheckRouterAnswer";
        public const string CheckRouterCommand = "com.cloud.agent.api.CheckRouterCommand";
        public const string CheckS2SVpnConnectionsAnswer = "com.cloud.agent.api.CheckS2SVpnConnectionsAnswer";
        public const string CheckS2SVpnConnectionsCommand = "com.cloud.agent.api.CheckS2SVpnConnectionsCommand";
        public const string CheckVirtualMachineAnswer = "com.cloud.agent.api.CheckVirtualMachineAnswer";
        public const string CheckVirtualMachineCommand = "com.cloud.agent.api.CheckVirtualMachineCommand";
        public const string CleanupNetworkRulesCmd = "com.cloud.agent.api.CleanupNetworkRulesCmd";
        public const string ClusterSyncAnswer = "com.cloud.agent.api.ClusterSyncAnswer";
        public const string ClusterSyncCommand = "com.cloud.agent.api.ClusterSyncCommand";
        public const string Command = "com.cloud.agent.api.Command";
        public const string CreatePrivateTemplateFromSnapshotCommand = "com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand";
        public const string CreatePrivateTemplateFromVolumeCommand = "com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand";
        public const string CreateStoragePoolCommand = "com.cloud.agent.api.CreateStoragePoolCommand";
        public const string CreateVMSnapshotAnswer = "com.cloud.agent.api.CreateVMSnapshotAnswer";
        public const string CreateVMSnapshotCommand = "com.cloud.agent.api.CreateVMSnapshotCommand";
        public const string CreateVolumeFromSnapshotAnswer = "com.cloud.agent.api.CreateVolumeFromSnapshotAnswer";
        public const string CreateVolumeFromSnapshotCommand = "com.cloud.agent.api.CreateVolumeFromSnapshotCommand";
        public const string DeleteStoragePoolCommand = "com.cloud.agent.api.DeleteStoragePoolCommand";
        public const string DeleteVMSnapshotAnswer = "com.cloud.agent.api.DeleteVMSnapshotAnswer";
        public const string DeleteVMSnapshotCommand = "com.cloud.agent.api.DeleteVMSnapshotCommand";
        public const string GetDomRVersionAnswer = "com.cloud.agent.api.GetDomRVersionAnswer";
        public const string GetDomRVersionCmd = "com.cloud.agent.api.GetDomRVersionCmd";
        public const string GetHostStatsAnswer = "com.cloud.agent.api.GetHostStatsAnswer";
        public const string GetHostStatsCommand = "com.cloud.agent.api.GetHostStatsCommand";
        public const string GetStorageStatsAnswer = "com.cloud.agent.api.GetStorageStatsAnswer";
        public const string GetStorageStatsCommand = "com.cloud.agent.api.GetStorageStatsCommand";
        public const string GetVmDiskStatsAnswer = "com.cloud.agent.api.GetVmDiskStatsAnswer";
        public const string GetVmDiskStatsCommand = "com.cloud.agent.api.GetVmDiskStatsCommand";
        public const string GetVmStatsAnswer = "com.cloud.agent.api.GetVmStatsAnswer";
        public const string GetVmStatsCommand = "com.cloud.agent.api.GetVmStatsCommand";
        public const string GetVmConfigCommand = "com.cloud.agent.api.GetVmConfigCommand";
        public const string GetVmConfigAnswer = "com.cloud.agent.api.GetVmConfigAnswer";
        public const string ModifyVmNicConfigCommand = "com.cloud.agent.api.ModifyVmNicConfigCommand";
        public const string ModifyVmNicConfigAnswer = "com.cloud.agent.api.ModifyVmNicConfigAnswer";
        public const string GetVncPortAnswer = "com.cloud.agent.api.GetVncPortAnswer";
        public const string GetVncPortCommand = "com.cloud.agent.api.GetVncPortCommand";
        public const string HostStatsEntry = "com.cloud.agent.api.HostStatsEntry";
        public const string MaintainAnswer = "com.cloud.agent.api.MaintainAnswer";
        public const string MaintainCommand = "com.cloud.agent.api.MaintainCommand";
        public const string ManageSnapshotAnswer = "com.cloud.agent.api.ManageSnapshotAnswer";
        public const string ManageSnapshotCommand = "com.cloud.agent.api.ManageSnapshotCommand";
        public const string MigrateAnswer = "com.cloud.agent.api.MigrateAnswer";
        public const string MigrateCommand = "com.cloud.agent.api.MigrateCommand";
        public const string MigrateWithStorageAnswer = "com.cloud.agent.api.MigrateWithStorageAnswer";
        public const string MigrateWithStorageCommand = "com.cloud.agent.api.MigrateWithStorageCommand";
        public const string ModifySshKeysCommand = "com.cloud.agent.api.ModifySshKeysCommand";
        public const string ModifyStoragePoolAnswer = "com.cloud.agent.api.ModifyStoragePoolAnswer";
        public const string ModifyStoragePoolCommand = "com.cloud.agent.api.ModifyStoragePoolCommand";
        public const string NetworkRulesSystemVmCommand = "com.cloud.agent.api.NetworkRulesSystemVmCommand";
        public const string NetworkRulesVmSecondaryIpCommand = "com.cloud.agent.api.NetworkRulesVmSecondaryIpCommand";
        public const string PingCommand = "com.cloud.agent.api.PingCommand";
        public const string PingRoutingCommand = "com.cloud.agent.api.PingRoutingCommand";
        public const string PingRoutingWithNwGroupsCommand = "com.cloud.agent.api.PingRoutingWithNwGroupsCommand";
        public const string PingRoutingWithOvsCommand = "com.cloud.agent.api.PingRoutingWithOvsCommand";
        public const string PingTestCommand = "com.cloud.agent.api.PingTestCommand";
        public const string PlugNicAnswer = "com.cloud.agent.api.PlugNicAnswer";
        public const string PlugNicCommand = "com.cloud.agent.api.PlugNicCommand";
        public const string PoolEjectCommand = "com.cloud.agent.api.PoolEjectCommand";
        public const string PrepareForMigrationAnswer = "com.cloud.agent.api.PrepareForMigrationAnswer";
        public const string PrepareForMigrationCommand = "com.cloud.agent.api.PrepareForMigrationCommand";
        public const string PvlanSetupCommand = "com.cloud.agent.api.PvlanSetupCommand";
        public const string ReadyAnswer = "com.cloud.agent.api.ReadyAnswer";
        public const string ReadyCommand = "com.cloud.agent.api.ReadyCommand";
        public const string RebootAnswer = "com.cloud.agent.api.RebootAnswer";
        public const string RebootCommand = "com.cloud.agent.api.RebootCommand";
        public const string RebootRouterCommand = "com.cloud.agent.api.RebootRouterCommand";
        public const string RevertToVMSnapshotAnswer = "com.cloud.agent.api.RevertToVMSnapshotAnswer";
        public const string RevertToVMSnapshotCommand = "com.cloud.agent.api.RevertToVMSnapshotCommand";
        public const string ScaleVmAnswer = "com.cloud.agent.api.ScaleVmAnswer";
        public const string ScaleVmCommand = "com.cloud.agent.api.ScaleVmCommand";
        public const string SecurityGroupRuleAnswer = "com.cloud.agent.api.SecurityGroupRuleAnswer";
        public const string SecurityGroupRulesCmd = "com.cloud.agent.api.SecurityGroupRulesCmd";
        public const string SetupAnswer = "com.cloud.agent.api.SetupAnswer";
        public const string SetupCommand = "com.cloud.agent.api.SetupCommand";
        public const string SetupGuestNetworkAnswer = "com.cloud.agent.api.SetupGuestNetworkAnswer";
        public const string SetupGuestNetworkCommand = "com.cloud.agent.api.SetupGuestNetworkCommand";
        public const string StartAnswer = "com.cloud.agent.api.StartAnswer";
        public const string StartCommand = "com.cloud.agent.api.StartCommand";
        public const string StartupCommand = "com.cloud.agent.api.StartupCommand";
        public const string StartupRoutingCommand = "com.cloud.agent.api.StartupRoutingCommand";
        public const string StartupStorageCommand = "com.cloud.agent.api.StartupStorageCommand";
        public const string StopAnswer = "com.cloud.agent.api.StopAnswer";
        public const string StopCommand = "com.cloud.agent.api.StopCommand";
        public const string StoragePoolInfo = "com.cloud.agent.api.StoragePoolInfo";
        public const string UnPlugNicAnswer = "com.cloud.agent.api.UnPlugNicAnswer";
        public const string UnPlugNicCommand = "com.cloud.agent.api.UnPlugNicCommand";
        public const string UpdateHostPasswordCommand = "com.cloud.agent.api.UpdateHostPasswordCommand";
        public const string UpgradeSnapshotCommand = "com.cloud.agent.api.UpgradeSnapshotCommand";
        public const string VmDiskStatsEntry = "com.cloud.agent.api.VmDiskStatsEntry";
        public const string VmStatsEntry = "com.cloud.agent.api.VmStatsEntry";
        public const string CheckSshAnswer = "com.cloud.agent.api.check.CheckSshAnswer";
        public const string CheckSshCommand = "com.cloud.agent.api.check.CheckSshCommand";
        public const string CheckConsoleProxyLoadCommand = "com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand";
        public const string ConsoleProxyLoadAnswer = "com.cloud.agent.api.proxy.ConsoleProxyLoadAnswer";
        public const string WatchConsoleProxyLoadCommand = "com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand";
        public const string CreateIpAliasCommand = "com.cloud.agent.api.routing.CreateIpAliasCommand";
        public const string DeleteIpAliasCommand = "com.cloud.agent.api.routing.DeleteIpAliasCommand";
        public const string DhcpEntryCommand = "com.cloud.agent.api.routing.DhcpEntryCommand";
        public const string DnsMasqConfigCommand = "com.cloud.agent.api.routing.DnsMasqConfigCommand";
        public const string IpAliasTO = "com.cloud.agent.api.routing.IpAliasTO";
        public const string IpAssocAnswer = "com.cloud.agent.api.routing.IpAssocAnswer";
        public const string IpAssocCommand = "com.cloud.agent.api.routing.IpAssocCommand";
        public const string IpAssocVpcCommand = "com.cloud.agent.api.routing.IpAssocVpcCommand";
        public const string LoadBalancerConfigCommand = "com.cloud.agent.api.routing.LoadBalancerConfigCommand";
        public const string NetworkElementCommand = "com.cloud.agent.api.routing.NetworkElementCommand";
        public const string RemoteAccessVpnCfgCommand = "com.cloud.agent.api.routing.RemoteAccessVpnCfgCommand";
        public const string SavePasswordCommand = "com.cloud.agent.api.routing.SavePasswordCommand";
        public const string SetFirewallRulesAnswer = "com.cloud.agent.api.routing.SetFirewallRulesAnswer";
        public const string SetFirewallRulesCommand = "com.cloud.agent.api.routing.SetFirewallRulesCommand";
        public const string SetNetworkACLAnswer = "com.cloud.agent.api.routing.SetNetworkACLAnswer";
        public const string SetNetworkACLCommand = "com.cloud.agent.api.routing.SetNetworkACLCommand";
        public const string SetPortForwardingRulesAnswer = "com.cloud.agent.api.routing.SetPortForwardingRulesAnswer";
        public const string SetPortForwardingRulesCommand = "com.cloud.agent.api.routing.SetPortForwardingRulesCommand";
        public const string SetPortForwardingRulesVpcCommand = "com.cloud.agent.api.routing.SetPortForwardingRulesVpcCommand";
        public const string SetSourceNatAnswer = "com.cloud.agent.api.routing.SetSourceNatAnswer";
        public const string SetSourceNatCommand = "com.cloud.agent.api.routing.SetSourceNatCommand";
        public const string SetStaticNatRulesAnswer = "com.cloud.agent.api.routing.SetStaticNatRulesAnswer";
        public const string SetStaticNatRulesCommand = "com.cloud.agent.api.routing.SetStaticNatRulesCommand";
        public const string SetStaticRouteAnswer = "com.cloud.agent.api.routing.SetStaticRouteAnswer";
        public const string SetStaticRouteCommand = "com.cloud.agent.api.routing.SetStaticRouteCommand";
        public const string Site2SiteVpnCfgCommand = "com.cloud.agent.api.routing.Site2SiteVpnCfgCommand";
        public const string VmDataCommand = "com.cloud.agent.api.routing.VmDataCommand";
        public const string VpnUsersCfgCommand = "com.cloud.agent.api.routing.VpnUsersCfgCommand";
        public const string CopyVolumeAnswer = "com.cloud.agent.api.storage.CopyVolumeAnswer";
        public const string CopyVolumeCommand = "com.cloud.agent.api.storage.CopyVolumeCommand";
        public const string CreateAnswer = "com.cloud.agent.api.storage.CreateAnswer";
        public const string CreateCommand = "com.cloud.agent.api.storage.CreateCommand";
        public const string CreatePrivateTemplateAnswer = "com.cloud.agent.api.storage.CreatePrivateTemplateAnswer";
        public const string DestroyCommand = "com.cloud.agent.api.storage.DestroyCommand";
        public const string MigrateVolumeAnswer = "com.cloud.agent.api.storage.MigrateVolumeAnswer";
        public const string MigrateVolumeCommand = "com.cloud.agent.api.storage.MigrateVolumeCommand";
        public const string PrimaryStorageDownloadAnswer = "com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer";
        public const string PrimaryStorageDownloadCommand = "com.cloud.agent.api.storage.PrimaryStorageDownloadCommand";
        public const string ResizeVolumeAnswer = "com.cloud.agent.api.storage.ResizeVolumeAnswer";
        public const string ResizeVolumeCommand = "com.cloud.agent.api.storage.ResizeVolumeCommand";
        public const string FirewallRuleTO = "com.cloud.agent.api.to.FirewallRuleTO";
        public const string IpAddressTO = "com.cloud.agent.api.to.IpAddressTO";
        public const string NicTO = "com.cloud.agent.api.to.NicTO";
        public const string PortForwardingRuleTO = "com.cloud.agent.api.to.PortForwardingRuleTO";
        public const string S3TO = "com.cloud.agent.api.to.S3TO";
        public const string NFSTO = "com.cloud.agent.api.to.NfsTO";
        public const string StaticNatRuleTO = "com.cloud.agent.api.to.StaticNatRuleTO";
        public const string StorageFilerTO = "com.cloud.agent.api.to.StorageFilerTO";
        public const string SwiftTO = "com.cloud.agent.api.to.SwiftTO";
        public const string VirtualMachineTO = "com.cloud.agent.api.to.VirtualMachineTO";
        public const string VolumeTO = "com.cloud.agent.api.to.VolumeTO";
        public const string DiskTO = "com.cloud.agent.api.to.DiskTO";
        public const string InternalErrorException = "com.cloud.exception.InternalErrorException";
        public const string HostType = "com.cloud.host.Host.Type";
        public const string HypervisorType = "com.cloud.hypervisor.Hypervisor.HypervisorType";
        public const string DnsMasqConfigurator = "com.cloud.network.DnsMasqConfigurator";
        public const string HAProxyConfigurator = "com.cloud.network.HAProxyConfigurator";
        public const string LoadBalancerConfigurator = "com.cloud.network.LoadBalancerConfigurator";
        public const string Networks = "com.cloud.network.Networks";
        public const string BroadcastDomainType = "com.cloud.network.Networks.BroadcastDomainType";
        public const string IsolationType = "com.cloud.network.Networks.IsolationType";
        public const string TrafficType = "com.cloud.network.Networks.TrafficType";
        public const string PhysicalNetworkSetupInfo = "com.cloud.network.PhysicalNetworkSetupInfo";
        public const string OvsCreateGreTunnelAnswer = "com.cloud.network.ovs.OvsCreateGreTunnelAnswer";
        public const string OvsCreateGreTunnelCommand = "com.cloud.network.ovs.OvsCreateGreTunnelCommand";
        public const string OvsCreateTunnelAnswer = "com.cloud.network.ovs.OvsCreateTunnelAnswer";
        public const string OvsCreateTunnelCommand = "com.cloud.network.ovs.OvsCreateTunnelCommand";
        public const string OvsDeleteFlowCommand = "com.cloud.network.ovs.OvsDeleteFlowCommand";
        public const string OvsDestroyBridgeCommand = "com.cloud.network.ovs.OvsDestroyBridgeCommand";
        public const string OvsDestroyTunnelCommand = "com.cloud.network.ovs.OvsDestroyTunnelCommand";
        public const string OvsFetchInterfaceAnswer = "com.cloud.network.ovs.OvsFetchInterfaceAnswer";
        public const string OvsFetchInterfaceCommand = "com.cloud.network.ovs.OvsFetchInterfaceCommand";
        public const string OvsSetTagAndFlowAnswer = "com.cloud.network.ovs.OvsSetTagAndFlowAnswer";
        public const string OvsSetTagAndFlowCommand = "com.cloud.network.ovs.OvsSetTagAndFlowCommand";
        public const string OvsSetupBridgeCommand = "com.cloud.network.ovs.OvsSetupBridgeCommand";
        public const string FirewallRule = "com.cloud.network.rules.FirewallRule";
        public const string ServerResource = "com.cloud.resource.ServerResource";
        public const string HypervisorResource = "com.cloud.resource.hypervisor.HypervisorResource";
        public const string Storage = "com.cloud.storage.Storage";
        public const string ImageFormat = "com.cloud.storage.Storage.ImageFormat";
        public const string StoragePoolType = "com.cloud.storage.Storage.StoragePoolType";
        public const string Volume = "com.cloud.storage.Volume";
        public const string VolumeVO = "com.cloud.storage.VolumeVO";
        public const string StorageSubsystemCommandHandler = "com.cloud.storage.resource.StorageSubsystemCommandHandler";
        public const string StorageSubsystemCommandHandlerBase = "com.cloud.storage.resource.StorageSubsystemCommandHandlerBase";
        public const string TemplateProp = "com.cloud.storage.template.TemplateProp";
        public const string BootloaderType = "com.cloud.template.VirtualMachineTemplate.BootloaderType";
        public const string VolumeObjectTO = "org.apache.cloudstack.storage.to.VolumeObjectTO";
        public const string TemplateObjectTO = "org.apache.cloudstack.storage.to.TemplateObjectTO";
        public const string PrimaryDataStoreTO = "org.apache.cloudstack.storage.to.PrimaryDataStoreTO";
        public const string AttachAnswer = "org.apache.cloudstack.storage.command.AttachAnswer";
        public const string AttachCommand = "org.apache.cloudstack.storage.command.AttachCommand";
        public const string AttachPrimaryDataStoreAnswer = "org.apache.cloudstack.storage.command.AttachPrimaryDataStoreAnswer";
        public const string AttachPrimaryDataStoreCmd = "org.apache.cloudstack.storage.command.AttachPrimaryDataStoreCmd";
        public const string CopyCmdAnswer = "org.apache.cloudstack.storage.command.CopyCmdAnswer";
        public const string CopyCommand = "org.apache.cloudstack.storage.command.CopyCommand";
        public const string CreateObjectAnswer = "org.apache.cloudstack.storage.command.CreateObjectAnswer";
        public const string CreateObjectCommand = "org.apache.cloudstack.storage.command.CreateObjectCommand";
        public const string DeleteCommand = "org.apache.cloudstack.storage.command.DeleteCommand";
        public const string DettachAnswer = "org.apache.cloudstack.storage.command.DettachAnswer";
        public const string DettachCommand = "org.apache.cloudstack.storage.command.DettachCommand";
        public const string HostVmStateReportCommand = "org.apache.cloudstack.HostVmStateReportCommand";
    }
}
