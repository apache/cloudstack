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
using Microsoft.VisualStudio.TestTools.UnitTesting;
using CloudStack.Plugin.WmiWrappers.ROOT.VIRTUALIZATION;
using System.Management;
using Newtonsoft.Json.Linq;
using Newtonsoft.Json;
using System.IO;
using log4net;
using HypervResource;
using CloudStack.Plugin.AgentShell;
using System.Collections.Generic;
using System.Xml;

namespace ServerResource.Tests
{
    [TestClass]
    public class HypervResourceControllerTest
    {
        protected static string testCifsUrl = AgentSettings.Default.testCifsUrl;
        protected static string testCifsPath = AgentSettings.Default.testCifsPath;
        protected static String testPrimaryDataStoreHost = HypervResourceController.config.StorageIpAddress;
        protected static String testS3TemplateName = AgentSettings.Default.testS3TemplateName;
        protected static String testCifsTemplateName = AgentSettings.Default.testS3TemplateName;
        protected static String testSystemVMTemplateName = AgentSettings.Default.testSystemVMTemplateName;
        protected static String testSystemVMTemplateNameNoExt = AgentSettings.Default.testSystemVMTemplateNameNoExt;
        protected static String testLocalStoreUUID = "5fe2bad3-d785-394e-9949-89786b8a63d2";
        protected static String testLocalStorePath = Path.Combine(AgentSettings.Default.hyperv_plugin_root, "var", "test", "storagepool");
        protected static String testSecondaryStoreLocalPath = Path.Combine(AgentSettings.Default.hyperv_plugin_root, "var", "test", "secondary");

        // TODO: differentiate between NFS and HTTP template URLs.
        protected static String testSampleTemplateUUID = "TestCopiedLocalTemplate.vhdx";
        protected static String testSampleTemplateURL = testSampleTemplateUUID;

        // test volumes are both a minimal size vhdx.  Changing the extension to .vhd makes on corrupt.
        protected static String testSampleVolumeWorkingUUID = "TestVolumeLegit.vhdx";
        protected static String testSampleVolumeCorruptUUID = "TestVolumeCorrupt.vhd";
        protected static String testSampleVolumeTempUUID = "TestVolumeTemp.vhdx";
        protected static String testSampleVolumeTempUUIDNoExt = "TestVolumeTemp";
        protected static String testSampleVolumeWorkingURIJSON;
        protected static String testSampleVolumeCorruptURIJSON;
        protected static String testSampleVolumeTempURIJSON;

        protected static String testSampleTemplateURLJSON;
        protected static String testLocalStorePathJSON;

        private static ILog s_logger = LogManager.GetLogger(typeof(HypervResourceControllerTest));

        /// <summary>
        /// Test WmiCalls to which incoming HTTP POST requests are dispatched.
        /// 
        /// TODO: revise beyond first approximation
        /// First approximation is a quick port of the existing Java tests for Hyper-V server resource.
        /// A second approximation would use the AgentShell settings files directly.
        /// A third approximation would look to invoke ServerResource methods via an HTTP request
        /// </summary>
        [TestInitializeAttribute]
        public void setUp()
        {
            AgentService.ConfigServerResource();
            HypervResourceController.config.PrivateMacAddress = AgentSettings.Default.private_mac_address;
            HypervResourceController.config.PrivateNetmask = AgentSettings.Default.private_ip_netmask;
            HypervResourceController.config.StorageIpAddress = HypervResourceController.config.PrivateIpAddress;
            HypervResourceController.config.StorageMacAddress = HypervResourceController.config.PrivateMacAddress;
            HypervResourceController.config.StorageNetmask = HypervResourceController.config.PrivateNetmask;


            // Used to create existing StoragePool in preparation for the ModifyStoragePool
            testLocalStoreUUID = AgentSettings.Default.local_storage_uuid.ToString();

            // Make sure secondary store is available.
            string fullPath = Path.GetFullPath(testSecondaryStoreLocalPath);
            s_logger.Info("Test secondary storage in " + fullPath);
            DirectoryInfo testSecondarStoreDir = new DirectoryInfo(fullPath);
            if (!testSecondarStoreDir.Exists)
            {
                try
                {
                    testSecondarStoreDir.Create();
                }
                catch (System.IO.IOException ex)
                {
                    Assert.Fail("Need to be able to create the folder " + testSecondarStoreDir.FullName + " failed due to " + ex.Message);
                }
            }

            // Convert to secondary storage string to canonical path
            testSecondaryStoreLocalPath = testSecondarStoreDir.FullName;
            AgentSettings.Default.local_secondary_storage_path = testSecondaryStoreLocalPath;

            // Make sure local primary storage is available
            DirectoryInfo testPoolDir = new DirectoryInfo(testLocalStorePath);
            Assert.IsTrue(testPoolDir.Exists, "To simulate local file system Storage Pool, you need folder at " + testPoolDir.FullName);

            // Convert to local primary storage string to canonical path
            testLocalStorePath = testPoolDir.FullName;
            AgentSettings.Default.local_storage_path = testLocalStorePath;

            // Clean up old test files in local storage folder
            FileInfo testVolWorks = new FileInfo(Path.Combine(testLocalStorePath, testSampleVolumeWorkingUUID));
            Assert.IsTrue(testVolWorks.Exists, "Create a working virtual disk at " + testVolWorks.FullName);


            // Delete all temporary files in local folder save the testVolWorks
            foreach (var file in testPoolDir.GetFiles())
            {
                if (file.FullName == testVolWorks.FullName)
                {
                    continue;
                }
                file.Delete();
                file.Refresh();
                Assert.IsFalse(file.Exists, "removed file from previous test called " + file.FullName);
            }

            // Recreate starting point files for test, and record JSON encoded paths for each ...
            testSampleVolumeTempURIJSON = CreateTestDiskImageFromExistingImage(testVolWorks, testLocalStorePath, testSampleVolumeTempUUID);
            s_logger.Info("Created " + testSampleVolumeTempURIJSON);
            testSampleVolumeCorruptURIJSON = CreateTestDiskImageFromExistingImage(testVolWorks, testLocalStorePath, testSampleVolumeCorruptUUID);
            s_logger.Info("Created " + testSampleVolumeCorruptURIJSON);
            CreateTestDiskImageFromExistingImage(testVolWorks, testLocalStorePath, testSampleTemplateUUID);
            testSampleTemplateURLJSON = JsonConvert.SerializeObject(testSampleTemplateUUID);
            s_logger.Info("Created " + testSampleTemplateURLJSON + " in local storage.");

            // ... including a secondary storage template:
            CreateTestDiskImageFromExistingImage(testVolWorks, testSecondarStoreDir.FullName, "af39aa7f-2b12-37e1-86d3-e23f2f005101.vhdx");
            s_logger.Info("Created " + "af39aa7f-2b12-37e1-86d3-e23f2f005101.vhdx" + " in secondary (NFS) storage.");


            // Capture other JSON encoded paths
            testSampleVolumeWorkingURIJSON = Newtonsoft.Json.JsonConvert.SerializeObject(testVolWorks.FullName);
            testLocalStorePathJSON = JsonConvert.SerializeObject(testLocalStorePath);

            // TODO: may need to initialise the server resource in future.
            //    s_hypervresource.initialize();

            // Verify sample template is in place storage pool
            s_logger.Info("setUp complete, sample StoragePool at " + testLocalStorePathJSON
                      + " sample template at " + testSampleTemplateURLJSON);
        }

        private String CreateTestDiskImageFromExistingImage(FileInfo srcFile,
        String dstPath,
        String dstFileName)
        {
            var newFullname = Path.Combine(dstPath, dstFileName);
            var newFileInfo = new FileInfo(newFullname);
            if (!newFileInfo.Exists)
            {
                newFileInfo = srcFile.CopyTo(newFullname);
            }
            newFileInfo.Refresh();
            Assert.IsTrue(newFileInfo.Exists, "Attempted to create " + newFullname + " from " + newFileInfo.FullName);

            return JsonConvert.SerializeObject(newFileInfo.FullName);
        }

        [TestMethod]
        public void TestPrimaryStorageDownloadCommandHTTP()
        {
            string downloadURI = "https://s3-eu-west-1.amazonaws.com/cshv3eu/SmallDisk.vhdx";
            corePrimaryStorageDownloadCommandTestCycle(downloadURI);
        }

        private void corePrimaryStorageDownloadCommandTestCycle(string downloadURI)
        {
            // Arrange
            HypervResourceController rsrcServer = new HypervResourceController();
            dynamic jsonPSDCmd = JsonConvert.DeserializeObject(samplePrimaryDownloadCommand());
            jsonPSDCmd.url = downloadURI;

            // Act
            dynamic jsonResult = rsrcServer.PrimaryStorageDownloadCommand(jsonPSDCmd);

            // Assert
            JObject ansAsProperty = jsonResult[0];
            dynamic ans = ansAsProperty.GetValue(CloudStackTypes.PrimaryStorageDownloadAnswer);
            Assert.IsTrue((bool)ans.result, "PrimaryStorageDownloadCommand did not succeed " + ans.details);

            // Test that URL of downloaded template works for file creation.
            dynamic jsonCreateCmd = JsonConvert.DeserializeObject(CreateCommandSample());
            jsonCreateCmd.templateUrl = ans.installPath;
            dynamic jsonAns2 = rsrcServer.CreateCommand(jsonCreateCmd);
            JObject ansAsProperty2 = jsonAns2[0];
            dynamic ans2 = ansAsProperty2.GetValue(CloudStackTypes.CreateAnswer);

            Assert.IsTrue((bool)ans2.result, (string)ans2.details);

            FileInfo newFile = new FileInfo((string)ans2.volume.path);
            Assert.IsTrue(newFile.Length > 0, "The new file should have a size greater than zero");
            newFile.Delete();
        }

        private string samplePrimaryDownloadCommand()
        {
            String cmdJson = "{\"localPath\":" + testLocalStorePathJSON +
                    ",\"poolUuid\":\"" + testLocalStoreUUID + "\",\"poolId\":201," +
                    "\"secondaryStorageUrl\":\"nfs://10.70.176.36/mnt/cshv3/secondarystorage\"," +
                    "\"primaryStorageUrl\":\"nfs://" + HypervResourceController.config.StorageIpAddress + "E:\\\\Disks\\\\Disks\"," +
                    "\"url\":\"nfs://10.70.176.36/mnt/cshv3/secondarystorage/template/tmpl//2/204//af39aa7f-2b12-37e1-86d3-e23f2f005101.vhdx\"," +
                    "\"format\":\"VHDX\",\"accountId\":2,\"name\":\"204-2-5a1db1ac-932b-3e7e-a0e8-5684c72cb862\"" +
                    ",\"contextMap\":{},\"wait\":10800}";
            return cmdJson;
        }

        public string CreateCommandSample()
        {
            String sample = "{\"volId\":17,\"pool\":{\"id\":201,\"uuid\":\"" + testLocalStoreUUID + "\",\"host\":\"" + HypervResourceController.config.StorageIpAddress + "\"" +
                            ",\"path\":" + testLocalStorePathJSON + ",\"port\":0,\"type\":\"Filesystem\"},\"diskCharacteristics\":{\"size\":0," +
                            "\"tags\":[],\"type\":\"ROOT\",\"name\":\"ROOT-15\",\"useLocalStorage\":true,\"recreatable\":true,\"diskOfferingId\":11," +
                            "\"volumeId\":17,\"hyperType\":\"Hyperv\"},\"templateUrl\":" + testSampleTemplateURLJSON + ",\"wait\":0}";
            return sample;
        }

        [TestMethod]
        public void TestDestroyCommand()
        {
            // Arrange
            String sampleVolume = getSampleVolumeObjectTO();
            String destoryCmd = //"{\"volume\":" + getSampleVolumeObjectTO() + "}";
                            "{\"volume\":{\"name\":\"" + testSampleVolumeTempUUIDNoExt
                                    + "\",\"storagePoolType\":\"Filesystem\","
                                    + "\"mountPoint\":"
                                    + testLocalStorePathJSON
                                   + ",\"path\":" + testSampleVolumeTempURIJSON
                                    + ",\"storagePoolUuid\":\"" + testLocalStoreUUID
                                    + "\","
                                    + "\"type\":\"ROOT\",\"id\":9,\"size\":0}}";

            HypervResourceController rsrcServer = new HypervResourceController();
            dynamic jsonDestoryCmd = JsonConvert.DeserializeObject(destoryCmd);

            // Act
            dynamic destoryAns = rsrcServer.DestroyCommand(jsonDestoryCmd);

            // Assert
            JObject ansAsProperty2 = destoryAns[0];
            dynamic ans = ansAsProperty2.GetValue(CloudStackTypes.Answer);
            String path = jsonDestoryCmd.volume.path;
            Assert.IsTrue((bool)ans.result, "DestroyCommand did not succeed " + ans.details);
            Assert.IsTrue(!File.Exists(path), "Failed to delete file " + path);
        }

        [TestMethod]
        public void TestCreateCommand()
        {
            // TODO: Need sample to update the test.
            // Arrange
            String createCmd = "{\"volId\":10,\"pool\":{\"id\":201,\"uuid\":\"" + testLocalStoreUUID + "\",\"host\":\"" + HypervResourceController.config.StorageIpAddress + "\"" +
                            ",\"path\":" + testLocalStorePathJSON + ",\"port\":0,\"type\":\"Filesystem\"},\"diskCharacteristics\":{\"size\":0," +
                            "\"tags\":[],\"type\":\"ROOT\",\"name\":\"ROOT-9\",\"useLocalStorage\":true,\"recreatable\":true,\"diskOfferingId\":11," +
                            "\"volumeId\":10,\"hyperType\":\"Hyperv\"},\"templateUrl\":" + testSampleTemplateURLJSON + ",\"contextMap\":{},\"wait\":0}";
            dynamic jsonCreateCmd = JsonConvert.DeserializeObject(createCmd);
            HypervResourceController rsrcServer = new HypervResourceController();

            Assert.IsTrue(Directory.Exists(testLocalStorePath));
            string filePath = Path.Combine(testLocalStorePath, (string)JsonConvert.DeserializeObject(testSampleTemplateURLJSON));
            Assert.IsTrue(File.Exists(filePath), "The template we make volumes from is missing from path " + filePath);
            int fileCount = Directory.GetFiles(testLocalStorePath).Length;
            s_logger.Debug(" test local store has " + fileCount + "files");

            // Act
            // Test requires there to be a template at the tempalteUrl, which is its location in the local file system.
            dynamic jsonResult = rsrcServer.CreateCommand(jsonCreateCmd);

            JObject ansAsProperty2 = jsonResult[0];
            dynamic ans = ansAsProperty2.GetValue(CloudStackTypes.CreateAnswer);
            Assert.IsNotNull(ans, "Should be an answer object of type CreateAnswer");
            Assert.IsTrue((bool)ans.result, "Failed to CreateCommand due to " + (string)ans.result);
            Assert.AreEqual(Directory.GetFiles(testLocalStorePath).Length, fileCount + 1);
            FileInfo newFile = new FileInfo((string)ans.volume.path);
            Assert.IsTrue(newFile.Length > 0, "The new file should have a size greater than zero");
            newFile.Delete();
        }

        /// <summary>
        /// Possible additional tests:  place an ISO in the drive
        /// </summary>
        [TestMethod]
        public void TestStartStopCommand()
        {
            string vmName = TestStartCommand();
            TestStopCommand(vmName);
        }

        public static String getSamplePrimaryDataStoreInfo()
        {
            String samplePrimaryDataStoreInfo =
            "{\"org.apache.cloudstack.storage.to.PrimaryDataStoreTO\":" +
                "{\"uuid\":\"" + testLocalStoreUUID + "\"," +
                "\"id\":201," +
                "\"host\":\"" + testPrimaryDataStoreHost + "\"," +
                "\"type\":\"Filesystem\"," +  // Not used in PrimaryDataStoreTO
                "\"poolType\":\"Filesystem\"," +  // Not used in PrimaryDataStoreTO
                "\"path\":" + testLocalStorePathJSON + "," +
                "\"port\":0}" +
            "}";
            return samplePrimaryDataStoreInfo;
        }

        public static String getSampleVolumeObjectTO()
        {
            String sampleVolumeObjectTO =
                    "{\"org.apache.cloudstack.storage.to.VolumeObjectTO\":" +
                        "{\"uuid\":\"19ae8e67-cb2c-4ab4-901e-e0b864272b59\"," +
                        "\"volumeType\":\"ROOT\"," +
                        "\"format\":\"VHDX\"," +
                        "\"dataStore\":" + getSamplePrimaryDataStoreInfo() + "," +
                        "\"name\":\"" + testSampleVolumeTempUUIDNoExt + "\"," +
                        "\"size\":52428800," +
                        "\"volumeId\":10," +
                //                            "\"vmName\":\"i-3-5-VM\"," +  // TODO: do we have to fill in the vmName?
                        "\"accountId\":3,\"id\":10}" +
                    "}";  // end of destTO 
            return sampleVolumeObjectTO;
        }

        public static String getSampleStartCommand()
        {
            String sample = "{\"vm\":{\"id\":17,\"name\":\"i-2-17-VM\",\"type\":\"User\",\"cpus\":1,\"speed\":500," +
                                "\"minRam\":536870912,\"maxRam\":536870912,\"arch\":\"x86_64\"," +
                                "\"os\":\"CentOS 6.0 (64-bit)\",\"bootArgs\":\"\",\"rebootOnCrash\":false," +
                                "\"enableHA\":false,\"limitCpuUse\":false,\"vncPassword\":\"31f82f29aff646eb\"," +
                                "\"params\":{},\"uuid\":\"8b030b6a-0243-440a-8cc5-45d08815ca11\"" +
                            ",\"disks\":[" +
                               "{\"data\":" + getSampleVolumeObjectTO() + ",\"diskSeq\":0,\"type\":\"ROOT\"}," +
                               "{\"diskSeq\":1,\"type\":\"ISO\"}" +
                            "]," +
                            "\"nics\":[" +
                                    "{\"deviceId\":0,\"networkRateMbps\":100,\"defaultNic\":true,\"uuid\":\"99cb4813-23af-428c-a87a-2d1899be4f4b\"," +
                                    "\"ip\":\"10.1.1.67\",\"netmask\":\"255.255.255.0\",\"gateway\":\"10.1.1.1\"," +
                                    "\"mac\":\"02:00:51:2c:00:0e\",\"dns1\":\"4.4.4.4\",\"broadcastType\":\"Vlan\",\"type\":\"Guest\"," +
                                    "\"broadcastUri\":\"vlan://261\",\"isolationUri\":\"vlan://261\",\"isSecurityGroupEnabled\":false}" +
                            "]},\"contextMap\":{},\"wait\":0}";
            return sample;
        }


        [TestMethod]
        public void TestCopyCommandFromCifs()
        {
            // Arrange
            string sampleCopyCommandForTemplateDownload =
            #region string_literal
                // org.apache.cloudstack.storage.command.CopyCommand
                "{\"srcTO\":" +
                  "{\"org.apache.cloudstack.storage.to.TemplateObjectTO\":" +
                    "{\"path\":\"" + testCifsPath + "\"," +
                     "\"origUrl\":\"http://10.147.28.7/templates/5d67394c-4efd-4b62-966b-51aa53b35277.vhd.bz2\"," +
                     "\"uuid\":\"7e4ca941-cb1b-4113-ab9e-043960d0fb10\"," +
                     "\"id\":206," +
                     "\"format\":\"VHDX\"," +
                     "\"accountId\":2," +
                     "\"checksum\":\"4b31e2846cc67fc10ea7281986519a54\"," +
                     "\"hvm\":true," +
                     "\"displayText\":\"OS031\"," +
                     "\"imageDataStore\":" +
                       "{\"com.cloud.agent.api.to.NfsTO\":" +
                         "{\"_url\":\"" + testCifsUrl + "\"," + // Unique item here
                         "\"_role\":\"Image\"}" +
                       "}," + // end of imageDataStore
                     "\"hypervisorType\":\"Hyperv\"," +
                     "\"name\":\"" + testS3TemplateName + "\"}" +
                  "}," + // end of srcTO
                 "\"destTO\":" +
                    "{\"org.apache.cloudstack.storage.to.TemplateObjectTO\":" +
                        "{" +
                        "\"origUrl\":\"http://10.147.28.7/templates/5d67394c-4efd-4b62-966b-51aa53b35277.vhd.bz2\"," +
                        "\"uuid\":\"7e4ca941-cb1b-4113-ab9e-043960d0fb10\"," +
                        "\"id\":206," +
                        "\"format\":\"VHDX\"," +
                        "\"accountId\":2," +
                        "\"checksum\":\"4b31e2846cc67fc10ea7281986519a54\"," +
                        "\"hvm\":true," +
                        "\"displayText\":\"Test of CIFS Download\"," +
                        "\"imageDataStore\":" + getSamplePrimaryDataStoreInfo() + "," + // end of imageDataStore
                        "\"name\":\"" + testS3TemplateName + "\"," +
                        "\"hypervisorType\":\"Hyperv\"}" +
                    "}," +// end of destTO
                "\"wait\":10800}"; // end of CopyCommand
            #endregion

            HypervResourceController rsrcServer;
            dynamic jsonDownloadCopyCmd;
            string dwnldDest;
            dynamic jsonCloneCopyCmd;
            string newVolName;
            CopyCommandTestSetupCifs(null, sampleCopyCommandForTemplateDownload, out rsrcServer, out jsonDownloadCopyCmd, out dwnldDest, out jsonCloneCopyCmd, out newVolName);

            // Act & Assert
            DownloadTemplateToPrimaryStorage(rsrcServer, jsonDownloadCopyCmd, dwnldDest);

            // Repeat to verify ability to detect existing file.
            DownloadTemplateToPrimaryStorage(rsrcServer, jsonDownloadCopyCmd, dwnldDest);

            File.Delete(dwnldDest);
        }

        [TestMethod]
        public void TestCopyCommand()
        {
            // Arrange
            string sampleCopyCommandToCreateVolumeFromTemplate =
            #region string_literal
                // org.apache.cloudstack.storage.command.CopyCommand
                "{\"srcTO\":" +
                    "{\"org.apache.cloudstack.storage.to.TemplateObjectTO\":" +
                        "{" +
                        "\"origUrl\":\"http://people.apache.org/~bhaisaab/vms/ttylinux_pv.vhd\"," +
                        "\"uuid\":\"9873f1c0-bdcc-11e2-8baa-ea85dab5fcd0\"," +
                        "\"id\":5," +
                        "\"format\":\"VHDX\"," +
                        "\"accountId\":1," +
                        "\"checksum\":\"4b31e2846cc67fc10ea7281986519a54\"," +
                        "\"hvm\":false," +
                        "\"displayText\":\"tiny Linux\"," +
                        "\"imageDataStore\":" + getSamplePrimaryDataStoreInfo() + "," +
                        "\"name\":\"" + testS3TemplateName + "\"}" +
                    "}," +  // end of srcTO
                "\"destTO\":" +
                    "{\"org.apache.cloudstack.storage.to.VolumeObjectTO\":" +
                        "{\"uuid\":\"19ae8e67-cb2c-4ab4-901e-e0b864272b59\"," +
                        "\"volumeType\":\"ROOT\"," +
                        "\"dataStore\":" + getSamplePrimaryDataStoreInfo() + "," +
                        "\"name\":\"ROOT-5\"," +
                        "\"size\":52428800," +
                        "\"volumeId\":10," +
                        "\"vmName\":\"i-3-5-VM\"," +
                        "\"accountId\":3," +
                        "\"id\":10 }" +
                    "}," +  // end of destTO 
                "\"wait\":0}"; // end of Copy Command
            #endregion
            //"name":"ROOT-8","size":140616708,"volumeId":8,"vmName":"s-8-VM","accountId":1,"id":8}},"contextMap":{},"wait":0}

            string sampleCopyCommandForTemplateDownload =
            #region string_literal
                // org.apache.cloudstack.storage.command.CopyCommand
                "{\"srcTO\":" +
                    "{\"org.apache.cloudstack.storage.to.TemplateObjectTO\":" +
                        "{\"path\":\"" + testS3TemplateName + ".vhdx" + "\"," +
                        "\"origUrl\":\"http://10.147.28.7/templates/5d67394c-4efd-4b62-966b-51aa53b35277.vhd.bz2\"," +
                        "\"uuid\":\"7e4ca941-cb1b-4113-ab9e-043960d0fb10\"," +
                        "\"id\":206," +
                        "\"format\":\"VHDX\"," +
                        "\"accountId\":2," +
                        "\"checksum\":\"4b31e2846cc67fc10ea7281986519a54\"," +
                        "\"hvm\":true," +
                        "\"displayText\":\"OS031\"," +
                        "\"imageDataStore\":" +
                            "{\"com.cloud.agent.api.to.S3TO\":" +
                                "{\"id\":1," +
                                "\"uuid\":\"95a64c8f-2128-4502-b5b4-0d7aa77406d2\"," +
                                "\"accessKey\":\"" + AgentSettings.Default.testS3AccessKey + "\"," +
                                "\"secretKey\":\"" + AgentSettings.Default.testS3SecretKey + "\"," +
                                "\"endPoint\":\"" + AgentSettings.Default.testS3Endpoint + "\"," +
                                "\"bucketName\":\"" + AgentSettings.Default.testS3Bucket + "\"," +
                                "\"httpsFlag\":false," +
                                "\"created\":\"May 19, 2013 4:17:25 PM\"}" +
                                "}," + // end of imageDataStore
                        "\"name\":\"" + testS3TemplateName + "\"}" +
                     "}," + // end of srcTO
                 "\"destTO\":" +
                    "{\"org.apache.cloudstack.storage.to.TemplateObjectTO\":" +
                        "{" +
                        "\"origUrl\":\"http://10.147.28.7/templates/5d67394c-4efd-4b62-966b-51aa53b35277.vhd.bz2\"," +
                        "\"uuid\":\"7e4ca941-cb1b-4113-ab9e-043960d0fb10\"," +
                        "\"id\":206," +
                        "\"format\":\"VHDX\"," +
                        "\"accountId\":2," +
                        "\"checksum\":\"4b31e2846cc67fc10ea7281986519a54\"," +
                        "\"hvm\":true," +
                        "\"displayText\":\"OS031\"," +
                        "\"imageDataStore\":" + getSamplePrimaryDataStoreInfo() + "," + // end of imageDataStore
                        "\"name\":\"" + testS3TemplateName + "\"}" +
                    "}," +// end of destTO
                "\"wait\":10800}"; // end of CopyCommand
            #endregion

            HypervResourceController rsrcServer;
            dynamic jsonDownloadCopyCmd;
            string dwnldDest;
            dynamic jsonCloneCopyCmd;
            string newVolName;
            CopyCommandTestSetup(sampleCopyCommandToCreateVolumeFromTemplate, sampleCopyCommandForTemplateDownload, out rsrcServer, out jsonDownloadCopyCmd, out dwnldDest, out jsonCloneCopyCmd, out newVolName);

            // Act & Assert
            DownloadTemplateToPrimaryStorage(rsrcServer, jsonDownloadCopyCmd, dwnldDest);
            CreateVolumeFromTemplate(rsrcServer, jsonCloneCopyCmd, newVolName);

            // Repeat to verify ability to detect existing file.
            DownloadTemplateToPrimaryStorage(rsrcServer, jsonDownloadCopyCmd, dwnldDest);

            File.Delete(dwnldDest);
            File.Delete(newVolName);
        }

        private static void CreateVolumeFromTemplate(HypervResourceController rsrcServer, dynamic jsonCloneCopyCmd, string newVolName)
        {
            dynamic copyResult = rsrcServer.CopyCommand(jsonCloneCopyCmd);

            // Assert
            Assert.IsNotNull(copyResult[0][CloudStackTypes.CopyCmdAnswer], "CopyCommand should return a StartAnswer in all cases");
            Assert.IsTrue((bool)copyResult[0][CloudStackTypes.CopyCmdAnswer].result, "CopyCommand did not succeed " + copyResult[0][CloudStackTypes.CopyCmdAnswer].details);
            Assert.IsTrue(File.Exists(newVolName), "CopyCommand failed to generate " + newVolName);
        }

        private static void DownloadTemplateToPrimaryStorage(HypervResourceController rsrcServer, dynamic jsonDownloadCopyCmd, string dwnldDest)
        {
            dynamic dwnldResult = rsrcServer.CopyCommand(jsonDownloadCopyCmd);

            // Assert
            Assert.IsNotNull(dwnldResult[0][CloudStackTypes.CopyCmdAnswer], "CopyCommand should return a StartAnswer in all cases");
            Assert.IsTrue((bool)dwnldResult[0][CloudStackTypes.CopyCmdAnswer].result, "CopyCommand did not succeed " + dwnldResult[0][CloudStackTypes.CopyCmdAnswer].details);
            Assert.IsTrue(File.Exists(dwnldDest), "CopyCommand failed to generate " + dwnldDest);
        }

        [TestMethod]
        public void TestCopyCommandBz2Img()
        {
            // Arrange
            string sampleCopyCommandToCreateVolumeFromTemplate =
            #region string_literal
                // org.apache.cloudstack.storage.command.CopyCommand
                "{\"srcTO\":" +
                    "{\"org.apache.cloudstack.storage.to.TemplateObjectTO\":" +
                        "{" +
                        "\"origUrl\":\"http://people.apache.org/~bhaisaab/vms/ttylinux_pv.vhd\"," +
                        "\"uuid\":\"9873f1c0-bdcc-11e2-8baa-ea85dab5fcd0\"," +
                        "\"id\":5," +
                        "\"format\":\"VHD\"," +
                        "\"accountId\":1," +
                        "\"checksum\":\"f613f38c96bf039f2e5cbf92fa8ad4f8\"," +
                        "\"hvm\":false," +
                        "\"displayText\":\"tiny Linux\"," +
                        "\"imageDataStore\":" + getSamplePrimaryDataStoreInfo() + "," +
                        "\"name\":\"" + testSystemVMTemplateNameNoExt + "\"}" +
                    "}," +  // end of srcTO
                "\"destTO\":" +
                    "{\"org.apache.cloudstack.storage.to.VolumeObjectTO\":" +
                        "{\"uuid\":\"19ae8e67-cb2c-4ab4-901e-e0b864272b59\"," +
                        "\"volumeType\":\"ROOT\"," +
                        "\"dataStore\":" + getSamplePrimaryDataStoreInfo() + "," +
                        "\"name\":\"ROOT-5\"," +
                        "\"size\":52428800," +
                        "\"volumeId\":10," +
                        "\"vmName\":\"i-3-5-VM\"," +
                        "\"accountId\":1," +
                        "\"id\":10}" +
                    "}," +  // end of destTO 
                "\"wait\":0}"; // end of Copy Command
            #endregion

            string sampleCopyCommandForTemplateDownload =
            #region string_literal
                // org.apache.cloudstack.storage.command.CopyCommand
                "{\"srcTO\":" +
                    "{\"org.apache.cloudstack.storage.to.TemplateObjectTO\":" +
                        "{\"path\":\"" + testSystemVMTemplateName + "\"," +
                        "\"origUrl\":\"http://10.147.28.7/templates/5d67394c-4efd-4b62-966b-51aa53b35277.vhd.bz2\"," +
                        "\"uuid\":\"7e4ca941-cb1b-4113-ab9e-043960d0fb10\"," +
                        "\"id\":206," +
                        "\"format\":\"VHD\"," +
                        "\"accountId\":1," +
                        "\"checksum\": \"f613f38c96bf039f2e5cbf92fa8ad4f8\"," +
                        "\"hvm\":true," +
                        "\"displayText\":\"OS031\"," +
                        "\"imageDataStore\":" +
                            "{\"com.cloud.agent.api.to.S3TO\":" +
                                "{\"id\":1," +
                                "\"uuid\":\"95a64c8f-2128-4502-b5b4-0d7aa77406d2\"," +
                                "\"accessKey\":\"" + AgentSettings.Default.testS3AccessKey + "\"," +
                                "\"secretKey\":\"" + AgentSettings.Default.testS3SecretKey + "\"," +
                                "\"endPoint\":\"" + AgentSettings.Default.testS3Endpoint + "\"," +
                                "\"bucketName\":\"" + AgentSettings.Default.testS3Bucket + "\"," +
                                "\"httpsFlag\":false," +
                                "\"created\":\"May 19, 2013 4:17:25 PM\"}" +
                                "}," + // end of imageDataStore
                        "\"name\":\"" + testSystemVMTemplateNameNoExt + "\"}" +
                     "}," + // end of srcTO
                 "\"destTO\":" +
                    "{\"org.apache.cloudstack.storage.to.TemplateObjectTO\":" +
                        "{" +
                        "\"origUrl\":\"http://10.147.28.7/templates/5d67394c-4efd-4b62-966b-51aa53b35277.vhd.bz2\"," +
                        "\"uuid\":\"7e4ca941-cb1b-4113-ab9e-043960d0fb10\"," +
                        "\"id\":206," +
                        "\"format\":\"VHD\"," +
                        "\"accountId\":1," +
                        "\"checksum\": \"f613f38c96bf039f2e5cbf92fa8ad4f8\"," +
                        "\"hvm\":true," +
                        "\"displayText\":\"OS031\"," +
                        "\"imageDataStore\":" + getSamplePrimaryDataStoreInfo() + "," + // end of imageDataStore
                        "\"name\":\"" + testSystemVMTemplateNameNoExt + "\"}" +
                    "}," +// end of destTO
                "\"wait\":10800}"; // end of CopyCommand
            #endregion

            HypervResourceController rsrcServer;
            dynamic jsonDownloadCopyCmd;
            string dwnldDest;
            dynamic jsonCloneCopyCmd;
            string newVolName;
            CopyCommandTestSetup(sampleCopyCommandToCreateVolumeFromTemplate, sampleCopyCommandForTemplateDownload, out rsrcServer, out jsonDownloadCopyCmd, out dwnldDest, out jsonCloneCopyCmd, out newVolName);

            // Act & Assert
            DownloadTemplateToPrimaryStorage(rsrcServer, jsonDownloadCopyCmd, dwnldDest);
            CreateVolumeFromTemplate(rsrcServer, jsonCloneCopyCmd, newVolName);

            File.Delete(dwnldDest);
            File.Delete(newVolName);
        }

        private static void CopyCommandTestSetup(string sampleCopyCommandToCreateVolumeFromTemplate, string sampleCopyCommandForTemplateDownload, out HypervResourceController rsrcServer, out dynamic jsonDownloadCopyCmd, out string dwnldDest, out dynamic jsonCloneCopyCmd, out string newVolName)
        {
            rsrcServer = new HypervResourceController();
            jsonDownloadCopyCmd = JsonConvert.DeserializeObject(sampleCopyCommandForTemplateDownload);
            TemplateObjectTO dwnldTemplate = TemplateObjectTO.ParseJson(jsonDownloadCopyCmd.destTO);
            dwnldDest = dwnldTemplate.FullFileName;

            jsonCloneCopyCmd = JsonConvert.DeserializeObject(sampleCopyCommandToCreateVolumeFromTemplate);
            VolumeObjectTO newVol = VolumeObjectTO.ParseJson(jsonCloneCopyCmd.destTO);
            newVol.format = dwnldTemplate.format;
            newVolName = dwnldTemplate.FullFileName;

            if (File.Exists(dwnldDest))
            {
                File.Delete(dwnldDest);
            }
            if (File.Exists(newVolName))
            {
                File.Delete(newVolName);
            }
        }

        private static void CopyCommandTestSetupCifs(string sampleCopyCommandToCreateVolumeFromTemplate, string sampleCopyCommandForTemplateDownload, out HypervResourceController rsrcServer, out dynamic jsonDownloadCopyCmd, out string dwnldDest, out dynamic jsonCloneCopyCmd, out string newVolName)
        {
            rsrcServer = new HypervResourceController();
            jsonDownloadCopyCmd = JsonConvert.DeserializeObject(sampleCopyCommandForTemplateDownload);
            TemplateObjectTO dwnldTemplate = TemplateObjectTO.ParseJson(jsonDownloadCopyCmd.destTO);
            dwnldDest = dwnldTemplate.FullFileName;

            if (File.Exists(dwnldDest))
            {
                File.Delete(dwnldDest);
            }
            newVolName = null;
            jsonCloneCopyCmd = null;
        }

        [TestMethod]
        public void TestModifyStoragePoolCommand()
        {
            // Create dummy folder
            String folderName = Path.Combine(".", "Dummy");
            if (!Directory.Exists(folderName))
            {
                Directory.CreateDirectory(folderName);
            }

            var pool = new
            {  // From java class StorageFilerTO
                type = Enum.GetName(typeof(StoragePoolType), StoragePoolType.Filesystem),
                host = "127.0.0.1",
                port = -1,
                path = folderName,
                uuid = Guid.NewGuid().ToString(),
                userInfo = string.Empty // Used in future to hold credential
            };

            var cmd = new
            {
                add = true,
                pool = pool,
                localPath = folderName
            };
            JToken tok = JToken.FromObject(cmd);
            HypervResourceController controller = new HypervResourceController();

            // Act
            dynamic jsonResult = controller.ModifyStoragePoolCommand(tok);

            // Assert
            dynamic ans = jsonResult[0][CloudStackTypes.ModifyStoragePoolAnswer];
            Assert.IsTrue((bool)ans.result, (string)ans.details);  // always succeeds

            // Clean up
            var cmd2 = new
            {
                pool = pool,
                localPath = folderName
            };
            JToken tok2 = JToken.FromObject(cmd);

            // Act
            dynamic jsonResult2 = controller.DeleteStoragePoolCommand(tok2);

            // Assert
            dynamic ans2 = jsonResult2[0][CloudStackTypes.Answer];
            Assert.IsTrue((bool)ans2.result, (string)ans2.details);  // always succeeds
        }

        [TestMethod]
        public void CreateStoragePoolCommand()
        {
            var cmd = new { localPath = "NULL" };
            JToken tok = JToken.FromObject(cmd);
            HypervResourceController controller = new HypervResourceController();

            // Act
            dynamic jsonResult = controller.CreateStoragePoolCommand(tok);

            // Assert
            dynamic ans = jsonResult[0][CloudStackTypes.Answer];
            Assert.IsTrue((bool)ans.result, (string)ans.details);  // always succeeds
        }

        [TestMethod]
        public void MaintainCommand()
        {
            // Omit HostEnvironment object, as this is a series of settings currently not used.
            var cmd = new { };
            JToken tok = JToken.FromObject(cmd);
            HypervResourceController controller = new HypervResourceController();

            // Act
            dynamic jsonResult = controller.MaintainCommand(tok);

            // Assert
            dynamic ans = jsonResult[0][CloudStackTypes.MaintainAnswer];
            Assert.IsTrue((bool)ans.result, (string)ans.details);  // always succeeds
        }

        [TestMethod]
        public void SetupCommand()
        {
            // Omit HostEnvironment object, as this is a series of settings currently not used.
            var cmd = new { multipath = false, needSetup = true };
            JToken tok = JToken.FromObject(cmd);
            HypervResourceController controller = new HypervResourceController();

            // Act
            dynamic jsonResult = controller.SetupCommand(tok);

            // Assert
            dynamic ans = jsonResult[0][CloudStackTypes.SetupAnswer];
            Assert.IsTrue((bool)ans.result, (string)ans.details);  // always succeeds
        }

        [TestMethod]
        public void TestPassingUserdataToVm()
        {
            // Sample data
            String key = "cloudstack-vm-userdata";
            String value = "username=root;password=1pass@word1";

            // Find the VM
            List<String> vmNames = WmiCallsV2.GetVmElementNames();

            // Get associated WMI object
            var vm = WmiCallsV2.GetComputerSystem(AgentSettings.Default.testKvpVmName);

            // Get existing KVP
            var vmSettings = WmiCallsV2.GetVmSettings(vm);
            var kvpInfo = WmiCallsV2.GetKvpSettings(vmSettings);

            // HostExchangesItems are embedded objects in the sense that the object value is stored and not a reference to the object.
            string[] kvpProps = kvpInfo.HostExchangeItems;

            // If the value already exists, delete it
            foreach (var item in kvpProps)
            {
                String wmiObjectXml = item;
                String existingKey;
                String existingValue;
                ParseKVP(wmiObjectXml, out existingKey, out existingValue);

                if (existingKey == key)
                {
                    WmiCallsV2.DeleteHostKvpItem(vm, existingKey);
                    break;
                }
            }

            // Add new user data
            WmiCallsV2.AddUserData(vm, value);

            // Verify key added to subsystem
            kvpInfo = WmiCallsV2.GetKvpSettings(vmSettings);

            // HostExchangesItems are embedded objects in the sense that the object value is stored and not a reference to the object.
            kvpProps = kvpInfo.HostExchangeItems;

            // If the value already exists, delete it
            bool userDataInPlace = false;
            foreach (var item in kvpProps)
            {
                String wmiObjectXml = item;
                String existingKey;
                String existingValue;
                ParseKVP(wmiObjectXml, out existingKey, out existingValue);

                if (existingKey == key && existingValue == value)
                {
//                    WmiCallsV2.DeleteHostKvpItem(vm, existingKey);
                    userDataInPlace = true;
                    break;
                }
            }

            Assert.IsTrue(userDataInPlace, "User data key / value did no save properly");
        }

        private static void ParseKVP(String wmiObjectXml, out String existingKey, out String existingValue)
        {
            // Reference:  http://blogs.msdn.com/b/virtual_pc_guy/archive/2008/12/05/enumerating-parent-kvp-data.aspx

            // Create XML parser
            var xmlDoc = new XmlDocument();

            // Load WMI object
            xmlDoc.LoadXml(wmiObjectXml);

            // Use xpath to get name and value
            var namePropXpath = "/INSTANCE/PROPERTY[@NAME='Name']/VALUE";
            var nameNode = xmlDoc.SelectSingleNode(namePropXpath);
            existingKey = nameNode.InnerText;
            var dataPropXpath = "/INSTANCE/PROPERTY[@NAME='Data']/VALUE";
            var dataNode = xmlDoc.SelectSingleNode(dataPropXpath);
            existingValue = dataNode.InnerText;
        }

        [TestMethod]
        public void GetVmStatsCommandFail()
        {
            // Use WMI to find existing VMs
            List<String> vmNames = new List<String>();
            vmNames.Add("FakeVM");

            var cmd = new
            {
                hostGuid = "FAKEguid",
                hostName = AgentSettings.Default.host,
                vmNames = vmNames
            };
            JToken tok = JToken.FromObject(cmd);
            HypervResourceController controller = new HypervResourceController();

            // Act
            dynamic jsonResult = controller.GetVmStatsCommand(tok);

            // Assert
            dynamic ans = jsonResult[0][CloudStackTypes.GetVmStatsAnswer];
            Assert.IsTrue((bool)ans.result, (string)ans.details);  // always succeeds, fake VM means no answer for the named VM
        }

        [TestMethod]
        public void GetVmStatsCommand()
        {
            // Use WMI to find existing VMs
            List<String> vmNames = WmiCalls.GetVmElementNames();

            var cmd = new
            {
                hostGuid = "FAKEguid",
                hostName = AgentSettings.Default.host,
                vmNames = vmNames
            };
            JToken tok = JToken.FromObject(cmd);
            HypervResourceController controller = new HypervResourceController();

            // Act
            dynamic jsonResult = controller.GetVmStatsCommand(tok);

            // Assert
            dynamic ans = jsonResult[0][CloudStackTypes.GetVmStatsAnswer];
            Assert.IsTrue((bool)ans.result, (string)ans.details);
        }

        [TestMethod]
        public void GetStorageStatsCommand()
        {
            // TODO:  Update sample data to unsure it is using correct info.
            String sample = String.Format(
            #region string_literal
"{{\"" +
                "id\":{0}," +
                "\"localPath\":{1}," +
                "\"pooltype\":\"Filesystem\"," +
                "\"contextMap\":{{}}," +
                "\"wait\":0}}",
                JsonConvert.SerializeObject(AgentSettings.Default.testLocalStoreUUID),
                JsonConvert.SerializeObject(AgentSettings.Default.testLocalStorePath)
                );
            #endregion
            var cmd = JsonConvert.DeserializeObject(sample);
            JToken tok = JToken.FromObject(cmd);
            HypervResourceController controller = new HypervResourceController();

            // Act
            dynamic jsonResult = controller.GetStorageStatsCommand(tok);

            // Assert
            dynamic ans = jsonResult[0][CloudStackTypes.GetStorageStatsAnswer];
            Assert.IsTrue((bool)ans.result, (string)ans.details);
            Assert.IsTrue((long)ans.used <= (long)ans.capacity);  // TODO: verify that capacity is indeed capacity and not used.
        }

        // TODO: can we speed up this command?  The logic takes over a second.
        [TestMethod]
        public void GetHostStatsCommand()
        {
            // Arrange
            long hostIdVal = 5;
            HypervResourceController controller = new HypervResourceController();
            string sample = string.Format(
            #region string_literal
"{{" +
                    "\"hostGuid\":\"B4AE5970-FCBF-4780-9F8A-2D2E04FECC34-HypervResource\"," +
                    "\"hostName\":\"CC-SVR11\"," +
                    "\"hostId\":{0}," +
                    "\"contextMap\":{{}}," +
                    "\"wait\":0}}",
                    JsonConvert.SerializeObject(hostIdVal));
            #endregion
            var cmd = JsonConvert.DeserializeObject(sample);
            JToken tok = JToken.FromObject(cmd);

            // Act
            dynamic jsonResult = controller.GetHostStatsCommand(tok);

            // Assert
            dynamic ans = jsonResult[0][CloudStackTypes.GetHostStatsAnswer];
            Assert.IsTrue((bool)ans.result);
            Assert.IsTrue(hostIdVal == (long)ans.hostStats.hostId);
            Assert.IsTrue(0.0 < (double)ans.hostStats.totalMemoryKBs);
            Assert.IsTrue(0.0 < (double)ans.hostStats.freeMemoryKBs);
            Assert.IsTrue(0.0 <= (double)ans.hostStats.networkReadKBs);
            Assert.IsTrue(0.0 <= (double)ans.hostStats.networkWriteKBs);
            Assert.IsTrue(0.0 <= (double)ans.hostStats.cpuUtilization);
            Assert.IsTrue(100.0 >= (double)ans.hostStats.cpuUtilization);
            Assert.IsTrue("host".Equals((string)ans.hostStats.entityType));
            Assert.IsTrue(String.IsNullOrEmpty((string)ans.details));
        }

        [TestMethod]
        public void GetHostStatsCommandFail()
        {
            // Arrange
            HypervResourceController controller = new HypervResourceController();
            var cmd = new { GetHostStatsCommand = new { hostId = "badvalueType" } };
            JToken tokFail = JToken.FromObject(cmd);

            // Act
            dynamic jsonResult = controller.GetHostStatsCommand(tokFail);

            // Assert
            dynamic ans = jsonResult[0][CloudStackTypes.GetHostStatsAnswer];
            Assert.IsFalse((bool)ans.result);
            Assert.IsNull((string)ans.hostStats);
            Assert.IsNotNull(ans.details);
        }

        [TestMethod]
        public void TestStartupCommand()
        {
            // Arrange
            HypervResourceController controller = new HypervResourceController();
            String sampleStartupRoutingCommand =
            #region string_literal
 "[{\"" + CloudStackTypes.StartupRoutingCommand + "\":{" +
                    "\"cpus\":0," +
                    "\"speed\":0," +
                    "\"memory\":0," +
                    "\"dom0MinMemory\":0," +
                    "\"poolSync\":false," +
                    "\"vms\":{}," +
                    "\"hypervisorType\":\"Hyperv\"," +
                    "\"hostDetails\":{" +
                    "\"com.cloud.network.Networks.RouterPrivateIpStrategy\":\"HostLocal\"" +
                    "}," +
                    "\"type\":\"Routing\"," +
                    "\"dataCenter\":\"1\"," +
                    "\"pod\":\"1\"," +
                    "\"cluster\":\"1\"," +
                    "\"guid\":\"16f85622-4508-415e-b13a-49a39bb14e4d\"," +
                    "\"name\":\"localhost\"," +
                    "\"version\":\"4.2.0\"," +
                    "\"privateIpAddress\":\"1\"," +
                    "\"storageIpAddress\":\"1\"," +
                    "\"contextMap\":{}," +
                    "\"wait\":0}}]";
            #endregion

            uint cores;
            uint mhz;
            WmiCalls.GetProcessorResources(out cores, out mhz);
            ulong memory_mb;
            ulong freememory;
            WmiCalls.GetMemoryResources(out memory_mb, out freememory);
            memory_mb *= 1024;
            long capacityBytes;
            long availableBytes;
            HypervResourceController.GetCapacityForLocalPath(WmiCalls.GetDefaultVirtualDiskFolder(),
                    out capacityBytes, out availableBytes);
            var DefaultVirtualDiskFolder = JsonConvert.SerializeObject(WmiCalls.GetDefaultVirtualDiskFolder());
            string expected =
            #region string_literal
                    "[{\"" + CloudStackTypes.StartupRoutingCommand + "\":{" +
                        "\"cpus\":" + cores + "," +
                        "\"speed\":" + mhz + "," +
                        "\"memory\":" + memory_mb + "," +
                        "\"dom0MinMemory\":" + (AgentSettings.Default.dom0MinMemory * 1024 * 1024) + "," +
                        "\"poolSync\":false," +
                        "\"vms\":{}," +
                        "\"hypervisorType\":\"Hyperv\"," +
                        "\"hostDetails\":{" +
                        "\"com.cloud.network.Networks.RouterPrivateIpStrategy\":\"HostLocal\"" +
                        "}," +
                        "\"type\":\"Routing\"," +
                        "\"dataCenter\":\"1\"," +
                        "\"pod\":\"1\"," +
                        "\"cluster\":\"1\"," +
                        "\"guid\":\"16f85622-4508-415e-b13a-49a39bb14e4d\"," +
                        "\"name\":\"localhost\"," +
                        "\"version\":\"4.2.0\"," +
                        "\"privateIpAddress\":\"" + AgentSettings.Default.private_ip_address + "\"," +
                        "\"storageIpAddress\":\"" + AgentSettings.Default.private_ip_address + "\"," +
                        "\"contextMap\":{}," +
                        "\"wait\":0," +
                        "\"privateNetmask\":\"" + AgentSettings.Default.private_ip_netmask + "\"," +
                        "\"privateMacAddress\":\"" + AgentSettings.Default.private_mac_address + "\"," +
                        "\"storageNetmask\":\"" + AgentSettings.Default.private_ip_netmask + "\"," +
                        "\"storageMacAddress\":\"" + AgentSettings.Default.private_mac_address + "\"," +
                        "\"gatewayIpAddress\":\"" + AgentSettings.Default.gateway_ip_address + "\"" +
                        ",\"caps\":\"hvm\"" +
                        "}}," +
                        "{\"com.cloud.agent.api.StartupStorageCommand\":{" +
                        "\"poolInfo\":{" +
                            "\"uuid\":\"16f85622-4508-415e-b13a-49a39bb14e4d\"," +
                            "\"host\":\"" + AgentSettings.Default.private_ip_address + "\"," +
                            "\"localPath\":" + DefaultVirtualDiskFolder + "," +
                            "\"hostPath\":" + DefaultVirtualDiskFolder + "," +
                            "\"poolType\":\"Filesystem\"," +
                            "\"capacityBytes\":" + capacityBytes + "," +
                            "\"availableBytes\":" + availableBytes + "," +
                            "\"details\":null" +
                        "}," +
                        "\"guid\":\"16f85622-4508-415e-b13a-49a39bb14e4d\"," +
                        "\"dataCenter\":\"1\"," +
                        "\"resourceType\":\"STORAGE_POOL\"" +
                        "}}]";
            #endregion

            dynamic jsonArray = JsonConvert.DeserializeObject(sampleStartupRoutingCommand);

            // Act
            dynamic jsonResult = controller.StartupCommand(jsonArray);

            // Assert
            string actual = JsonConvert.SerializeObject(jsonResult);
            Assert.AreEqual(expected, actual, "StartupRoutingCommand not populated properly");
        }


        private static string TestStartCommand()
        {
            // Arrange
            HypervResourceController rsrcServer = new HypervResourceController();
            String sample = getSampleStartCommand();


            dynamic jsonStartCmd = JsonConvert.DeserializeObject(sample);

            // Act
            dynamic startAns = rsrcServer.StartCommand(jsonStartCmd);

            // Assert
            Assert.IsNotNull(startAns[0][CloudStackTypes.StartAnswer], "StartCommand should return a StartAnswer in all cases");
            Assert.IsTrue((bool)startAns[0][CloudStackTypes.StartAnswer].result, "StartCommand did not succeed " + startAns[0][CloudStackTypes.StartAnswer].details);
            string vmCmdName = jsonStartCmd.vm.name.Value;
            var vm = WmiCalls.GetComputerSystem(vmCmdName);
            VirtualSystemSettingData vmSettings = WmiCalls.GetVmSettings(vm);
            MemorySettingData memSettings = WmiCalls.GetMemSettings(vmSettings);
            ProcessorSettingData procSettings = WmiCalls.GetProcSettings(vmSettings);
            dynamic jsonObj = JsonConvert.DeserializeObject(sample);
            var vmInfo = jsonObj.vm;
            string vmName = vmInfo.name;
            var nicInfo = vmInfo.nics;
            int vcpus = vmInfo.cpus;
            int memSize = vmInfo.maxRam / 1048576;
            Assert.IsTrue((long)memSettings.VirtualQuantity == memSize);
            Assert.IsTrue((long)memSettings.Reservation == memSize);
            Assert.IsTrue((long)memSettings.Limit == memSize);
            Assert.IsTrue((int)procSettings.VirtualQuantity == vcpus);
            Assert.IsTrue((int)procSettings.Reservation == vcpus);
            Assert.IsTrue((int)procSettings.Limit == 100000);

            // examine NIC
            SyntheticEthernetPortSettingData[] nicSettingsViaVm = WmiCalls.GetEthernetPorts(vm);
            Assert.IsTrue(nicSettingsViaVm.Length > 0, "Should be at least one ethernet port on VM");
            string expectedMac = (string)jsonStartCmd.vm.nics[0].mac;
            string strippedExpectedMac = expectedMac.Replace(":", string.Empty);
            Assert.AreEqual(nicSettingsViaVm[0].Address.ToLower(), strippedExpectedMac.ToLower());

            // Assert switchport has correct VLAN 
            SwitchPort[] switchPorts = WmiCalls.GetSwitchPorts(vm);
            VirtualSwitchManagementService vmNetMgmtSvc = WmiCalls.GetVirtualSwitchManagementService();
            VLANEndpointSettingData vlanSettings = WmiCalls.GetVlanEndpointSettings(vmNetMgmtSvc, switchPorts[0].Path);
            string isolationUri = (string)jsonStartCmd.vm.nics[0].isolationUri;
            string vlan = isolationUri.Replace("vlan://", string.Empty);
            Assert.AreEqual(vlanSettings.AccessVLAN.ToString(), vlan);

            return vmName;
        }

        private static void TestStopCommand(string vmName)
        {
            // Arrange
            HypervResourceController rsrcServer = new HypervResourceController();
            String sampleStop = "{\"isProxy\":false,\"vmName\":\"i-2-17-VM\",\"contextMap\":{},\"wait\":0}";
            dynamic jsonStopCmd = JsonConvert.DeserializeObject(sampleStop);

            // Act
            dynamic stopAns = rsrcServer.StopCommand(jsonStopCmd);

            // Assert VM is gone!
            Assert.IsNotNull(stopAns[0][CloudStackTypes.StopAnswer], "StopCommand should return a StopAnswer in all cases");
            Assert.IsTrue((bool)stopAns[0][CloudStackTypes.StopAnswer].result, "StopCommand did not succeed " + stopAns[0][CloudStackTypes.StopAnswer].details);
            var finalVm = WmiCalls.GetComputerSystem(vmName);
            Assert.IsTrue(WmiCalls.GetComputerSystem(vmName) == null);
        }
    }
}
