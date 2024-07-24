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
using CloudStack.Plugin.WmiWrappers.ROOT.VIRTUALIZATION.V2;
using System.Management;
using Newtonsoft.Json.Linq;
using Newtonsoft.Json;
using System.IO;
using log4net;
using HypervResource;
using CloudStack.Plugin.AgentShell;
using System.Collections.Generic;
using NSubstitute;
using System.Web.Http;
using Xunit;

namespace ServerResource.Tests
{
    public class HypervResourceController1Test
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

        protected static IWmiCallsV2 wmiCallsV2;


        private static ILog s_logger = LogManager.GetLogger(typeof(HypervResourceController1Test));

        /// <summary>
        /// Test WmiCalls to which incoming HTTP POST requests are dispatched.
        /// 
        /// TODO: revise beyond first approximation
        /// First approximation is a quick port of the existing Java tests for Hyper-V server resource.
        /// A second approximation would use the AgentShell settings files directly.
        /// A third approximation would look to invoke ServerResource methods via an HTTP request
        /// </summary>

        public HypervResourceController1Test()
        {
            wmiCallsV2 = Substitute.For<IWmiCallsV2>();
            //AgentService.ConfigServerResource();
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
                    throw new NotImplementedException("Need to be able to create the folder " + testSecondarStoreDir.FullName + " failed due to " + ex.Message);
                }
            }

            // Convert to secondary storage string to canonical path
            testSecondaryStoreLocalPath = testSecondarStoreDir.FullName;
            AgentSettings.Default.local_secondary_storage_path = testSecondaryStoreLocalPath;

            // Make sure local primary storage is available
            DirectoryInfo testPoolDir = new DirectoryInfo(testLocalStorePath);
            //Assert.True(testPoolDir.Exists, "To simulate local file system Storage Pool, you need folder at " + testPoolDir.FullName);

            // Convert to local primary storage string to canonical path
            testLocalStorePath = testPoolDir.FullName;
            AgentSettings.Default.local_storage_path = testLocalStorePath;

            // Clean up old test files in local storage folder
            FileInfo testVolWorks = new FileInfo(Path.Combine(testLocalStorePath, testSampleVolumeWorkingUUID));
            // Assert.True(testVolWorks.Exists, "Create a working virtual disk at " + testVolWorks.FullName);           
            
            testSampleTemplateURLJSON = JsonConvert.SerializeObject(testSampleTemplateUUID);
            s_logger.Info("Created " + testSampleTemplateURLJSON + " in local storage.");           


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
            Assert.True(newFileInfo.Exists, "Attempted to create " + newFullname + " from " + newFileInfo.FullName);

            return JsonConvert.SerializeObject(newFileInfo.FullName);
        }
        
        [Fact]
        public void TestCreateCommand()
        {
            DirectoryInfo localStorePath = new DirectoryInfo(testLocalStorePath);
            if (!localStorePath.Exists)
            {
                try
                {
                    localStorePath.Create();
                }
                catch (System.IO.IOException ex)
                {
                    throw new NotImplementedException("Need to be able to create the folder " + localStorePath.FullName + " failed due to " + ex.Message);
                }
            }

            FileInfo sampleTemplateFile = new FileInfo(Path.Combine(testLocalStorePath, testSampleTemplateUUID));
            if (!sampleTemplateFile.Exists)
            {
                //Create a file to write to.
                using (StreamWriter sw = sampleTemplateFile.CreateText())
                {
                    sw.WriteLine("This is fake template file for test");
                }
            }
            var counter = 0;
            wmiCallsV2.When(x => x.CreateDynamicVirtualHardDisk(Arg.Any<ulong>(), Arg.Any<String>())).Do(x => counter++);
            // TODO: Need sample to update the test.
            // Arrange
            String createCmd = "{\"volId\":10,\"pool\":{\"id\":201,\"uuid\":\"" + testLocalStoreUUID + "\",\"host\":\"" + HypervResourceController.config.StorageIpAddress + "\"" +
                            ",\"path\":" + testLocalStorePathJSON + ",\"port\":0,\"type\":\"Filesystem\"},\"diskCharacteristics\":{\"size\":0," +
                            "\"tags\":[],\"type\":\"ROOT\",\"name\":\"ROOT-9\",\"useLocalStorage\":true,\"recreatable\":true,\"diskOfferingId\":11," +
                            "\"volumeId\":10,\"hyperType\":\"Hyperv\"},\"templateUrl\":" + testSampleTemplateURLJSON + ",\"contextMap\":{},\"wait\":0}";
            dynamic jsonCreateCmd = JsonConvert.DeserializeObject(createCmd);
            HypervResourceController rsrcServer = new HypervResourceController();
            HypervResourceController.wmiCallsV2 = wmiCallsV2;

            Assert.True(Directory.Exists(testLocalStorePath), testLocalStorePath + " does not exist ");
            string filePath = Path.Combine(testLocalStorePath, (string)JsonConvert.DeserializeObject(testSampleTemplateURLJSON));
            Assert.True(File.Exists(filePath), "The template we make volumes from is missing from path " + filePath);
            int fileCount = Directory.GetFiles(testLocalStorePath).Length;
            s_logger.Debug(" test local store has " + fileCount + "files");

            // Act
            // Test requires there to be a template at the tempalteUrl, which is its location in the local file system.
            dynamic jsonResult = rsrcServer.CreateCommand(jsonCreateCmd);
            s_logger.Debug("CreateDynamicVirtualHardDisk method is called " + counter + " times");

            //Assert.Equal(counter, 1);

            JObject ansAsProperty2 = jsonResult[0];
            dynamic ans = ansAsProperty2.GetValue(CloudStackTypes.CreateAnswer);
            Assert.NotNull(ans);
            Assert.True((bool)ans.result, "Failed to CreateCommand due to " + (string)ans.result);
            Assert.Equal(Directory.GetFiles(testLocalStorePath).Length, fileCount + 1);
            FileInfo newFile = new FileInfo((string)ans.volume.path);
            Assert.True(newFile.Length > 0, "The new file should have a size greater than zero");
            newFile.Delete();
            sampleTemplateFile.Delete();
        }

        [Fact]
        public void TestDestroyCommand()
        {
            testSampleVolumeTempURIJSON = "\"storagepool\"";
            // Arrange
            String destroyCmd = //"{\"volume\":" + getSampleVolumeObjectTO() + "}";
                            "{\"volume\":{\"name\":\"" + testSampleVolumeTempUUIDNoExt
                                    + "\",\"storagePoolType\":\"Filesystem\","
                                    + "\"mountPoint\":"
                                    + testLocalStorePathJSON
                                   + ",\"path\":" + testSampleVolumeTempURIJSON
                                    + ",\"storagePoolUuid\":\"" + testLocalStoreUUID
                                    + "\","
                                    + "\"type\":\"ROOT\",\"id\":9,\"size\":0}}";

            ImageManagementService imgmgr = new ImageManagementService();
            wmiCallsV2.GetImageManagementService().Returns(imgmgr);

            HypervResourceController rsrcServer = new HypervResourceController();
            HypervResourceController.wmiCallsV2 = wmiCallsV2;

            dynamic jsonDestroyCmd = JsonConvert.DeserializeObject(destroyCmd);

            // Act
            dynamic destroyAns = rsrcServer.DestroyCommand(jsonDestroyCmd);

            // Assert
            JObject ansAsProperty2 = destroyAns[0];
            dynamic ans = ansAsProperty2.GetValue(CloudStackTypes.Answer);
            String path = jsonDestroyCmd.volume.path;
            Assert.True((bool)ans.result, "DestroyCommand did not succeed " + ans.details);
            Assert.True(!File.Exists(path), "Failed to delete file " + path);
        }

        [Fact]
        public void TestStartCommand()
        {
            ComputerSystem system = new ComputerSystem();
            wmiCallsV2.DeployVirtualMachine(Arg.Any<Object>(), Arg.Any<string>()).Returns(system);

            // Arrange
            HypervResourceController rsrcServer = new HypervResourceController();
            HypervResourceController.wmiCallsV2 = wmiCallsV2;
            String sample = getSampleStartCommand();


            dynamic jsonStartCmd = JsonConvert.DeserializeObject(sample);

            // Act
            dynamic startAns = rsrcServer.StartCommand(jsonStartCmd);

            // Assert
            Assert.NotNull(startAns[0][CloudStackTypes.StartAnswer]);
            Assert.True((bool)startAns[0][CloudStackTypes.StartAnswer].result, "StartCommand did not succeed " + startAns[0][CloudStackTypes.StartAnswer].details);

            Assert.Null((string)startAns[0][CloudStackTypes.StartAnswer].details);            
        }

        [Fact]
        public void TestStopCommand()
        {
            //string vmName = "Test VM";
            var counter = 0;
            wmiCallsV2.When(x => x.DestroyVm(Arg.Any<Object>())).Do(x => counter++);

            // Arrange
            HypervResourceController rsrcServer = new HypervResourceController();
            HypervResourceController.wmiCallsV2 = wmiCallsV2;

            String sampleStop = "{\"isProxy\":false,\"vmName\":\"i-2-17-VM\",\"contextMap\":{},\"checkBeforeCleanup\":false,\"wait\":0}";
            dynamic jsonStopCmd = JsonConvert.DeserializeObject(sampleStop);

            // Act
            dynamic stopAns = rsrcServer.StopCommand(jsonStopCmd);

            // Assert VM is gone!
            Assert.NotNull(stopAns[0][CloudStackTypes.StopAnswer]);
            Assert.True((bool)stopAns[0][CloudStackTypes.StopAnswer].result, "StopCommand did not succeed " + stopAns[0][CloudStackTypes.StopAnswer].details);

            Assert.Null((string)stopAns[0][CloudStackTypes.StopAnswer].details);
            Assert.Equal<int>(counter, 1);
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
    }
}
