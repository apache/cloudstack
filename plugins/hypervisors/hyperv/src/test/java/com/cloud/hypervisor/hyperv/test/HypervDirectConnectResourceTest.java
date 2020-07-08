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
package com.cloud.hypervisor.hyperv.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.Files;
import com.google.gson.Gson;

import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.CreateStoragePoolCommand;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.GetHostStatsAnswer;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetStorageStatsAnswer;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.GetVmStatsAnswer;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.hyperv.resource.HypervDirectConnectResource;
import com.cloud.network.Networks.RouterPrivateIpStrategy;
import com.cloud.serializer.GsonHelper;
import com.cloud.storage.Storage;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * Functional test suit for Hyper-V plugin.
 *
 * mvn clean build -DskipTests=false
 **/
public class HypervDirectConnectResourceTest {

    private static final Logger s_logger = Logger.getLogger(HypervDirectConnectResourceTest.class.getName());

    // TODO: make this a config parameter
    private static final String sampleLegitDiskImageURL = "http://s3-eu-west-1.amazonaws.com/cshv3eu/SmallDisk.vhdx";
    private static final Gson s_gson = GsonHelper.getGson();
    private static final HypervDirectConnectResource s_hypervresource = new HypervDirectConnectResource();

    private static String s_testLocalStoreUUID = "5fe2bad3-d785-394e-9949-89786b8a63d2";
    private static String s_testLocalStorePath = "." + File.separator + "var" + File.separator + "test" + File.separator + "storagepool";
    private static String s_testSecondaryStoreLocalPath = "." + File.separator + "var" + File.separator + "test" + File.separator + "secondary";

    // TODO: differentiate between NFS and HTTP template URLs.
    private static String s_testSampleTemplateUUID = "TestCopiedLocalTemplate.vhdx";
    private static String s_testSampleTemplateURL = s_testSampleTemplateUUID;

    // test volumes are both a minimal size vhdx. Changing the extension to .vhd
    // makes on corrupt.
    private static String s_testSampleVolumeWorkingUUID = "TestVolumeLegit.vhdx";
    private static String s_testSampleVolumeWorkingUUIDNoExt = "TestVolumeLegit";
    private static String s_testSampleVolumeCorruptUUID = "TestVolumeCorrupt.vhd";
    private static String s_testSampleVolumeTempUUID = "TestVolumeTemp.vhdx";
    private static String s_testSampleVolumeWorkingURIJSON;
    private static String s_testSampleVolumeCorruptURIJSON;
    private static String s_testSampleVolumeTempURIJSON;

    private static String s_testSampleTemplateURLJSON;
    private static String s_testLocalStorePathJSON;
    private static String s_agentExecutable;
    private static Process s_agentProc;
    private static String s_testPrimaryDataStoreHost;

    public HypervDirectConnectResourceTest() {
    }

    /**
     * Prep test environment.
     **/
    @Before
    public final void setUp() throws ConfigurationException {
        // Obtain script locations from agent.properties
        final Map<String, Object> params = PropertiesUtil.toMap(loadProperties());
        // Used to create existing StoragePool in preparation for the
        // ModifyStoragePool
        params.put("local.storage.uuid", s_testLocalStoreUUID);

        // Make sure secondary store is available.
        File testSecondarStoreDir = new File(s_testSecondaryStoreLocalPath);
        if (!testSecondarStoreDir.exists()) {
            testSecondarStoreDir.mkdirs();
        }
        Assert.assertTrue("Need to be able to create the folder " + s_testSecondaryStoreLocalPath, testSecondarStoreDir.exists());
        try {
            params.put("local.secondary.storage.path", testSecondarStoreDir.getCanonicalPath());
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            Assert.fail("No canonical path for " + testSecondarStoreDir.getAbsolutePath());
        }

        // Clean up old test files in local storage folder:
        File testPoolDir = new File(s_testLocalStorePath);
        if (!testPoolDir.exists()) {
            testPoolDir.mkdirs();
        }
        Assert.assertTrue("To simulate local file system Storage Pool, " + " you need folder at " + testPoolDir.getPath(),
            testPoolDir.exists() && testPoolDir.isDirectory());
        try {
            s_testLocalStorePath = testPoolDir.getCanonicalPath();
        } catch (IOException e) {
            Assert.fail("No canonical path for " + testPoolDir.getAbsolutePath());
        }
        params.put("local.storage.path", s_testLocalStorePath);

        File testVolWorks = new File(s_testLocalStorePath + File.separator + s_testSampleVolumeWorkingUUID);
        if (!testVolWorks.exists()) {
            ReadableByteChannel rbc;
            try {
                URL sampleLegitDiskImageURL = new URL("http://s3-eu-west-1.amazonaws.com/cshv3eu/SmallDisk.vhdx");
                rbc = Channels.newChannel(sampleLegitDiskImageURL.openStream());
                FileOutputStream fos = new FileOutputStream(testVolWorks);
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        Assert.assertTrue("Need to put a sample working virtual disk at " + testVolWorks.getPath(), testVolWorks.exists());
        try {
            s_testSampleVolumeWorkingURIJSON = s_gson.toJson(testVolWorks.getCanonicalPath());
        } catch (IOException e) {
            Assert.fail("No canonical path for " + testPoolDir.getAbsolutePath());
        }

        FilenameFilter vhdsFilt = new FilenameFilter() {
            @Override
            public boolean accept(final File directory, final String fileName) {
                return fileName.endsWith(".vhdx") || fileName.endsWith(".vhd");
            }
        };
        for (File file : testPoolDir.listFiles(vhdsFilt)) {
            if (file.getName().equals(testVolWorks.getName())) {
                continue;
            }
            Assert.assertTrue("Should have deleted file " + file.getPath(), file.delete());
            s_logger.info("Cleaned up by delete file " + file.getPath());
        }

        s_testSampleVolumeTempURIJSON = createTestDiskImageFromExistingImage(testVolWorks, s_testLocalStorePath, s_testSampleVolumeTempUUID);
        s_logger.info("Created " + s_testSampleVolumeTempURIJSON);
        s_testSampleVolumeCorruptURIJSON = createTestDiskImageFromExistingImage(testVolWorks, s_testLocalStorePath, s_testSampleVolumeCorruptUUID);
        s_logger.info("Created " + s_testSampleVolumeCorruptURIJSON);
        createTestDiskImageFromExistingImage(testVolWorks, s_testLocalStorePath, s_testSampleTemplateUUID);
        s_testSampleTemplateURLJSON = s_testSampleTemplateUUID;
        s_logger.info("Created " + s_testSampleTemplateURLJSON + " in local storage.");

        // Create secondary storage template:
        createTestDiskImageFromExistingImage(testVolWorks, testSecondarStoreDir.getAbsolutePath(), "af39aa7f-2b12-37e1-86d3-e23f2f005101.vhdx");
        s_logger.info("Created " + "af39aa7f-2b12-37e1-86d3-e23f2f005101.vhdx" + " in secondary (NFS) storage.");

        s_testLocalStorePathJSON = s_gson.toJson(s_testLocalStorePath);

        String agentIp = (String)params.get("ipaddress");
        s_logger.info("Test using agent IP address " + agentIp);
        params.put("agentIp", agentIp);
        setTestJsonResult(params);
        s_hypervresource.configure("hypervresource", params);
        // Verify sample template is in place storage pool
        s_logger.info("setUp complete, sample StoragePool at " + s_testLocalStorePathJSON + " sample template at " + s_testSampleTemplateURLJSON);

        s_agentExecutable = (String)params.get("agent.executable");
        s_testPrimaryDataStoreHost = (String)params.get("ipaddress");
        agentCreation();
    }

    private String createTestDiskImageFromExistingImage(final File srcFile, final String dstPath, final String dstFileName) {
        String newFileURIJSON = null;
        File testVolTemp = new File(dstPath + File.separator + dstFileName);
        try {
            Files.copy(srcFile, testVolTemp);
        } catch (IOException e) {
            ; // NOP
        }
        Assert.assertTrue("Should be a temporary file created from the valid volume) at " + testVolTemp.getPath(), testVolTemp.exists());
        try {
            newFileURIJSON = s_gson.toJson(testVolTemp.getCanonicalPath());
        } catch (IOException e) {
            Assert.fail("No file at " + testVolTemp.getAbsolutePath());
        }
        return newFileURIJSON;
    }

    @Test
    public final void testGetVmStatsCommand() {
        // Sample GetVmStatsCommand
        List<String> vmNames = new ArrayList<String>();
        vmNames.add("i-2-11-VM");
        GetVmStatsCommand cmd = new GetVmStatsCommand(vmNames, "1", "localhost");

        s_hypervresource.executeRequest(cmd);
        GetVmStatsAnswer ans = (GetVmStatsAnswer)s_hypervresource.executeRequest(cmd);
        Assert.assertTrue(ans.getDetails(), ans.getResult());
    }

    public final void testStartupCommand() {
        StartupRoutingCommand defaultStartRoutCmd =
            new StartupRoutingCommand(0, 0, 0, 0, null, Hypervisor.HypervisorType.Hyperv, RouterPrivateIpStrategy.HostLocal);

        // Identity within the data centre is decided by CloudStack kernel,
        // and passed via ServerResource.configure()
        defaultStartRoutCmd.setDataCenter("1");
        defaultStartRoutCmd.setPod("1");
        defaultStartRoutCmd.setCluster("1");
        defaultStartRoutCmd.setGuid("1");
        defaultStartRoutCmd.setName("1");
        defaultStartRoutCmd.setPrivateIpAddress("1");
        defaultStartRoutCmd.setStorageIpAddress("1");
        defaultStartRoutCmd.setCpus(12);

        // TODO: does version need to be hard coded.
        defaultStartRoutCmd.setVersion("4.2.0");

        StartupCommand scmd = defaultStartRoutCmd;

        Command[] cmds = {scmd};
        String cmdsStr = s_gson.toJson(cmds);
        s_logger.debug("Commands[] toJson is " + cmdsStr);

        Command[] result = s_gson.fromJson(cmdsStr, Command[].class);
        s_logger.debug("Commands[] fromJson is " + s_gson.toJson(result));
        s_logger.debug("Commands[] first element has type" + result[0].toString());
    }

    // @Test
    public final void testJson() {
        StartupStorageCommand sscmd = null;
        com.cloud.agent.api.StoragePoolInfo pi = new com.cloud.agent.api.StoragePoolInfo("test123", "192.168.0.1", "c:\\", "c:\\", StoragePoolType.Filesystem, 100L, 50L);

        sscmd = new StartupStorageCommand();
        sscmd.setPoolInfo(pi);
        sscmd.setGuid(pi.getUuid());
        sscmd.setDataCenter("foo");
        sscmd.setResourceType(Storage.StorageResourceType.STORAGE_POOL);
        s_logger.debug("StartupStorageCommand fromJson is " + s_gson.toJson(sscmd));
    }

    @Test
    public final void testBadGetVmStatsCommand() {
        // Sample GetVmStatsCommand
        List<String> vmNames = new ArrayList<String>();
        vmNames.add("FakeVM");
        GetVmStatsCommand vmStatsCmd = new GetVmStatsCommand(vmNames, "1", "localhost");
        GetVmStatsAnswer ans = (GetVmStatsAnswer)s_hypervresource.executeRequest(vmStatsCmd);
        Assert.assertTrue(ans.getDetails(), ans.getResult());
    }

    @Test
    public final void testCreateStoragePoolCommand() {
        String folderName = "." + File.separator + "Dummy";
        StoragePoolVO pool = createTestStoragePoolVO(folderName);

        CreateStoragePoolCommand cmd = new CreateStoragePoolCommand(true, pool);
        s_logger.debug("TestCreateStoragePoolCommand sending " + s_gson.toJson(cmd));

        Answer ans = s_hypervresource.executeRequest(cmd);
        Assert.assertTrue(ans.getResult());
    }

    @Test
    public final void testModifyStoragePoolCommand() {
        // Create dummy folder
        String folderName = "." + File.separator + "Dummy";
        StoragePoolVO pool = createTestStoragePoolVO(folderName);

        ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(true, pool, folderName);
        Answer ans = s_hypervresource.executeRequest(cmd);
        Assert.assertTrue(ans.getResult());

        DeleteStoragePoolCommand delCmd = new DeleteStoragePoolCommand(pool, folderName);
        Answer ans2 = s_hypervresource.executeRequest(delCmd);
        Assert.assertTrue(ans2.getResult());
    }

    // Check
    @Test
    public final void testModifyStoragePoolCommand2() {
        // Should return existing pool
        // Create dummy folder
        String folderName = "." + File.separator + "Dummy";
        File folder = new File(folderName);
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                Assert.assertTrue(false);
            }
        }

        // Use same spec for pool
        s_logger.info("Createing pool at : " + folderName);

        StoragePoolVO pool = new StoragePoolVO(StoragePoolType.Filesystem, "127.0.0.1", -1, folderName);
        pool.setUuid(s_testLocalStoreUUID);

        ModifyStoragePoolCommand cmd = new ModifyStoragePoolCommand(true, pool, folderName);
        Answer ans = s_hypervresource.executeRequest(cmd);
        Assert.assertTrue(ans.getResult());

        DeleteStoragePoolCommand delCmd = new DeleteStoragePoolCommand(pool, folderName);
        Answer ans2 = s_hypervresource.executeRequest(delCmd);
        Assert.assertTrue(ans2.getResult());
    }

    public final StoragePoolVO createTestStoragePoolVO(final String folderName) {
        File folder = new File(folderName);
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                Assert.assertTrue(false);
            }
        }

        // Use same spec for pool
        s_logger.info("Createing pool at : " + folderName);

        StoragePoolVO pool = new StoragePoolVO(StoragePoolType.Filesystem, "127.0.0.1", -1, folderName);
        return pool;
    }

    @Test
    public final void testInitialize() {
        StartupCommand[] startCmds = s_hypervresource.initialize();
        Command[] cmds = new Command[] {startCmds[0], startCmds[1]};
        String result = s_gson.toJson(cmds);
        if (result == null) {
            result = "NULL";
        }
        s_logger.debug("TestInitialize returned " + result);
        s_logger.debug("TestInitialize expected " + _setTestJsonResultStr);
        Assert.assertTrue("StartupCommand[] not what we expected", _setTestJsonResultStr.equals(result));
        return;
    }

    @Test
    public final void testPrimaryStorageDownloadCommandHTTP() {
        PrimaryStorageDownloadCommand cmd = samplePrimaryDownloadCommand();
        cmd.setUrl("http://s3-eu-west-1.amazonaws.com/cshv3eu/SmallDisk.vhdx");
        corePrimaryStorageDownloadCommandTestCycle(cmd);
    }

    private void corePrimaryStorageDownloadCommandTestCycle(final PrimaryStorageDownloadCommand cmd) {
        PrimaryStorageDownloadAnswer ans = (PrimaryStorageDownloadAnswer)s_hypervresource.executeRequest(cmd);
        if (!ans.getResult()) {
            s_logger.error(ans.getDetails());
        } else {
            s_logger.debug(ans.getDetails());
        }

        Assert.assertTrue(ans.getDetails(), ans.getResult());
        // Test that returned URL works.
        // CreateCommand createCmd = CreateCommandSample();
        // CreateCommand testCreateCmd = new
        // CreateCommand(createCmd.getDiskCharacteristics(),
        // ans.getInstallPath(), createCmd.getPool());
        // CreateAnswer ans2 =
        // (CreateAnswer)s_hypervresource.executeRequest(testCreateCmd);
        // Assert.assertTrue(ans2.getDetails(), ans2.getResult());
    }

    private PrimaryStorageDownloadCommand samplePrimaryDownloadCommand() {
        String cmdJson =
            "{\"localPath\":" + s_testLocalStorePathJSON + ",\"poolUuid\":\"" + s_testLocalStoreUUID + "\",\"poolId\":201," + "\"secondaryStorageUrl\":" +
                "\"nfs://10.70.176.36/mnt/cshv3/secondarystorage\"," + "\"primaryStorageUrl\":" + "\"nfs://10.70.176.29E:\\\\Disks\\\\Disks\"," + "\"url\":" +
                "\"nfs://10.70.176.36/mnt/cshv3/secondarystorage/template/tmpl//2/204//af39aa7f-2b12-37e1-86d3-e23f2f005101.vhdx\"," +
                "\"format\":\"VHDX\",\"accountId\":2," + "\"name\":" + "\"204-2-5a1db1ac-932b-3e7e-a0e8-5684c72cb862\"" + ",\"contextMap\":{},\"wait\":10800}";
        PrimaryStorageDownloadCommand cmd = s_gson.fromJson(cmdJson, PrimaryStorageDownloadCommand.class);
        return cmd;
    }

    public final CreateCommand createCommandSample() {
        String sample =
            "{\"volId\":17,\"pool\":{\"id\":201,\"uuid\":\"" + s_testLocalStoreUUID + "\",\"host\":\"10.70.176.29\"" + ",\"path\":" + s_testLocalStorePathJSON +
                ",\"port\":0,\"type\":\"Filesystem\"}," + "\"diskCharacteristics\":{\"size\":0," + "\"tags\":[],\"type\":\"ROOT\",\"name\":\"ROOT-15\"," +
                "\"useLocalStorage\":true,\"recreatable\":true," + "\"diskOfferingId\":11," + "\"volumeId\":17,\"hyperType\":\"Hyperv\"}," + "\"templateUrl\":" +
                s_testSampleTemplateURLJSON + ",\"wait\":0}";
        CreateCommand cmd = s_gson.fromJson(sample, CreateCommand.class);
        return cmd;
    }

    @Test
    public final void testCreateCommand() {
        String sample =
            "{\"volId\":10,\"pool\":{\"id\":201,\"uuid\":\"" + s_testLocalStoreUUID + "\",\"host\":\"10.70.176.29\"" + ",\"path\":" + s_testLocalStorePathJSON +
                ",\"port\":0,\"type\":\"Filesystem\"}," + "\"diskCharacteristics\":{\"size\":0," + "\"tags\":[],\"type\":\"ROOT\",\"name\":\"ROOT-9\"," +
                "\"useLocalStorage\":true,\"recreatable\":true," + "\"diskOfferingId\":11," + "\"volumeId\":10,\"hyperType\":\"Hyperv\"}," + "\"templateUrl\":" +
                s_testSampleTemplateURLJSON + ",\"contextMap\":{},\"wait\":0}";

        File destDir = new File(s_testLocalStorePath);
        Assert.assertTrue(destDir.isDirectory());
        File testSampleTemplateURLFile = new File(s_testLocalStorePath + File.separator + s_gson.fromJson(s_testSampleTemplateURLJSON, String.class));
        Assert.assertTrue("The template that create should make" + " volumes from is missing from path " + testSampleTemplateURLFile.getPath(),
            testSampleTemplateURLFile.exists());

        int fileCount = destDir.listFiles().length;
        s_logger.debug(" test local store has " + fileCount + "files");
        // Test requires there to be a template at the tempalteUrl, which is its
        // location in the local file system.
        CreateCommand cmd = s_gson.fromJson(sample, CreateCommand.class);
        CreateAnswer ans = (CreateAnswer)s_hypervresource.executeRequest(cmd);
        Assert.assertTrue(ans.getDetails(), ans.getResult());
        Assert.assertTrue("CreateCommand should add a file to the folder", fileCount + 1 == destDir.listFiles().length);
        File newFile = new File(ans.getVolume().getPath());
        Assert.assertTrue("The new file should have a size greater than zero", newFile.length() > 0);
        newFile.delete();
    }

    @Test
    public final void testStartCommandCorruptDiskImage() {
        String sampleStart =
            "{\"vm\":{\"id\":16,\"name\":\"i-3-17-VM\"," + "\"type\":\"User\",\"cpus\":1,\"speed\":500," + "\"minRam\":536870912,\"maxRam\":536870912,"
                + "\"arch\":\"x86_64\"," + "\"os\":\"CentOS 6.0 (64-bit)\"," + "\"bootArgs\":\"\",\"rebootOnCrash\":false," + "\"enableHA\":false,"
                + "\"limitCpuUse\":false," + "\"vncPassword\":\"31f82f29aff646eb\"," + "\"params\":{}," + "\"uuid\":\"8b030b6a-0243-440a-8cc5-45d08815ca11\""
                + ",\"disks\":[" + "{\"id\":18,\"name\":\"" +
                s_testSampleVolumeCorruptUUID +
                "\"," +
                "\"mountPoint\":" +
                s_testSampleVolumeCorruptURIJSON +
                "," +
                "\"path\":" +
                s_testSampleVolumeCorruptURIJSON +
                ",\"size\":0," +
                "\"type\":\"ROOT\"," +
                "\"storagePoolType\":\"Filesystem\"," +
                "\"storagePoolUuid\":\"" +
                s_testLocalStoreUUID +
                "\"" +
                ",\"deviceId\":0}," +
                "{\"id\":16,\"name\":\"Hyper-V Sample2\",\"size\":0," +
                "\"type\":\"ISO\",\"storagePoolType\":\"ISO\"," +
                "\"deviceId\":3}]," +
                "\"nics\":[" +
                "{\"deviceId\":0,\"networkRateMbps\":100," +
                "\"defaultNic\":true," +
                "\"uuid\":\"99cb4813-23af-428c-a87a-2d1899be4f4b\"," +
                "\"ip\":\"10.1.1.67\",\"netmask\":\"255.255.255.0\"," +
                "\"gateway\":\"10.1.1.1\"," +
                "\"mac\":\"02:00:51:2c:00:0e\",\"dns1\":\"4.4.4.4\"," +
                "\"broadcastType\":\"Vlan\",\"type\":\"Guest\"," +
                "\"broadcastUri\":\"vlan://261\"," +
                "\"isolationUri\":\"vlan://261\"," +
                "\"isSecurityGroupEnabled\":false}" + "]},\"contextMap\":{},\"wait\":0}";

        StartCommand cmd = s_gson.fromJson(sampleStart, StartCommand.class);
        StartAnswer ans = (StartAnswer)s_hypervresource.executeRequest(cmd);
        Assert.assertFalse(ans.getDetails(), ans.getResult());
    }

    @Test
    public final void testStartStopCommand() {
        String sample = getSampleStartCommand();
        StartAnswer sans = simpleVmStart(sample);
        Assert.assertTrue(sans.getDetails(), sans.getResult());
        StopAnswer stopAns = simpleVmStop();
        Assert.assertTrue(stopAns.getDetails(), stopAns.getResult());
    }

    @Test
    public final void testStartStartCommand() {
        String sample = getSampleStartCommand();

        StartAnswer sans = simpleVmStart(sample);
        Assert.assertTrue(sans.getDetails(), sans.getResult());
        simpleVmStart(sample);

        StopAnswer ans = simpleVmStop();
        Assert.assertTrue(ans.getDetails(), ans.getResult());
    }

    private StopAnswer simpleVmStop() {
        String sampleStop = "{\"isProxy\":false,\"vmName\":\"i-2-17-VM\"," + "\"contextMap\":{},\"wait\":0}";
        StopCommand cmd = s_gson.fromJson(sampleStop, StopCommand.class);
        StopAnswer ans = (StopAnswer)s_hypervresource.executeRequest(cmd);
        return ans;
    }

    private StartAnswer simpleVmStart(final String sample) {
        StartCommand cmd = s_gson.fromJson(sample, StartCommand.class);
        s_logger.info("StartCommand sample " + s_gson.toJson(cmd));
        StartAnswer ans = (StartAnswer)s_hypervresource.executeRequest(cmd);
        return ans;
    }

    @Test
    public final void testDestroyCommand() {
        // TODO: how does the command vary when we are only deleting a vm versus
        // deleting a volume?

//        String sample2 = "{\"volume\":" + getSampleVolumeObjectTO() + "}";

        String sample2 =
            "{\"volume\":{\"name\":\"" + s_testSampleVolumeWorkingUUIDNoExt + "\",\"storagePoolType\":\"Filesystem\"," + "\"mountPoint\":" + s_testLocalStorePathJSON +
                ",\"path\":" + s_testSampleVolumeTempURIJSON + ",\"storagePoolUuid\":\"" + s_testLocalStoreUUID + "\"," + "\"type\":\"ROOT\",\"id\":9,\"size\":0}}";
        DestroyCommand cmd = s_gson.fromJson(sample2, DestroyCommand.class);
        Answer ans = s_hypervresource.executeRequest(cmd);
        Assert.assertTrue(ans.getDetails(), ans.getResult());
    }

    @Test
    public final void testGetStorageStatsCommand() {
        // TODO: Update sample data to unsure it is using correct info.
        String sample =
            "{\"id\":\"" + s_testLocalStoreUUID + "\",\"localPath\":" + s_testLocalStorePathJSON + "," + "\"pooltype\":\"Filesystem\"," + "\"contextMap\":{},\"wait\":0}";

        s_logger.info("Sample JSON: " + sample);

        GetStorageStatsCommand cmd = s_gson.fromJson(sample, GetStorageStatsCommand.class);
        s_hypervresource.executeRequest(cmd);
        GetStorageStatsAnswer ans = (GetStorageStatsAnswer)s_hypervresource.executeRequest(cmd);
        Assert.assertTrue(ans.getDetails(), ans.getResult());
        Assert.assertTrue(ans.getByteUsed() != ans.getCapacityBytes());
    }

    @After
    public final void agentTerminate() {
        // Write carriage return line feed to script's stdin
        OutputStream scriptInput = s_agentProc.getOutputStream();
        OutputStreamWriter siw = new OutputStreamWriter(scriptInput);
        try {
            BufferedWriter writer = new BufferedWriter(siw);
            writer.write("\r\n");
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            s_logger.debug("Error closing agent at " + s_agentExecutable + " message " + ex.getMessage());
        }
    }

    private void agentCreation() {
        // Launch script
        try {
            List<String> exeArgs = new ArrayList<String>();

            exeArgs.add(s_agentExecutable);
            exeArgs.add("--console");

            // when we launch from the shell, we need to use Cygwin's path to
            // the exe
            ProcessBuilder builder = new ProcessBuilder(exeArgs);
            builder.redirectErrorStream(true);
            s_agentProc = builder.start();
            Thread.sleep(4000);
        } catch (Exception ex) {
            s_logger.debug("Error calling starting aget at " + s_agentExecutable + " message " + ex.getMessage());
        }
    }

    @Test
    public final void testGetHostStatsCommand() {
        // Arrange
        long hostIdVal = 123;
        GetHostStatsCommand cmd = new GetHostStatsCommand("1", "testHost", hostIdVal);

        // Act
        GetHostStatsAnswer ans = (GetHostStatsAnswer)s_hypervresource.executeRequest(cmd);

        // Assert
        Assert.assertTrue(ans.getResult());
        Assert.assertTrue(0.0 < ans.getTotalMemoryKBs());
        Assert.assertTrue(0.0 < ans.getFreeMemoryKBs());
        Assert.assertTrue(0.0 <= ans.getNetworkReadKBs());
        Assert.assertTrue(0.0 <= ans.getNetworkWriteKBs());
        Assert.assertTrue(0.0 <= ans.getCpuUtilization());
        Assert.assertTrue(100 >= ans.getCpuUtilization());
        Assert.assertTrue(ans.getEntityType().equals("host"));
        Assert.assertTrue(ans.getDetails() == null);
    }

    public static Properties loadProperties() throws ConfigurationException {
        Properties properties = new Properties();
        final File file = PropertiesUtil.findConfigFile("agent.properties");
        if (file == null) {
            throw new ConfigurationException("Unable to find agent.properties.");
        }

        s_logger.info("agent.properties found at " + file.getAbsolutePath());

        try {
            properties.load(new FileInputStream(file));
        } catch (final FileNotFoundException ex) {
            throw new CloudRuntimeException("Cannot find the file: " + file.getAbsolutePath(), ex);
        } catch (final IOException ex) {
            throw new CloudRuntimeException("IOException in reading " + file.getAbsolutePath(), ex);
        }
        return properties;
    }

    private String _setTestJsonResultStr = null;

    public final void setTestJsonResult(final Map<String, Object> params) {
        File dir = new File((String)params.get("DefaultVirtualDiskFolder"));
        long usableCapacity = dir.getUsableSpace() - 4294967296L;
        long totalSpace = dir.getTotalSpace() - 4294967296L;

        // TODO: add test to verify capacity statistics change
        _setTestJsonResultStr =
            String.format("[{\"com.cloud.agent.api.StartupRoutingCommand\":{" + "\"cpus\":%s," + "\"speed\":%s," + "\"memory\":%s," + "\"dom0MinMemory\":%s,"
                + "\"poolSync\":false," + "\"vms\":{}," + "\"caps\":\"hvm\"," + "\"hypervisorType\":\"Hyperv\"," + "\"hostDetails\":{"
                + "\"com.cloud.network.Networks.RouterPrivateIpStrategy\":" + "\"HostLocal\"" + "}," + "\"type\":\"Routing\"," + "\"dataCenter\":%s," + "\"pod\":%s,"
                + "\"cluster\":%s," + "\"guid\":\"16f85622-4508-415e-b13a-49a39bb14e4d\"," + "\"name\":\"hypervresource\"," + "\"version\":\"4.2.0\","
                + "\"privateIpAddress\":%s," + "\"privateMacAddress\":%s," + "\"privateNetmask\":%s," + "\"storageIpAddress\":%s," + "\"storageNetmask\":%s,"
                + "\"storageMacAddress\":%s," + "\"gatewayIpAddress\":%s," + "\"contextMap\":{}," + "\"wait\":0" + "}},"
                + "{\"com.cloud.agent.api.StartupStorageCommand\":{" + "\"totalSize\":0," + "\"poolInfo\":{" + "\"uuid\":\"16f85622-4508-415e-b13a-49a39bb14e4d\","
                + "\"host\":%s," + "\"localPath\":%s," + "\"hostPath\":%s," + "\"poolType\":\"Filesystem\"," + "\"capacityBytes\":%s," + "\"availableBytes\":%s" + "},"
                + "\"resourceType\":\"STORAGE_POOL\"," + "\"hostDetails\":{}," + "\"type\":\"Storage\"," + "\"dataCenter\":\"1\"," + "\"guid\":"
                + "\"16f85622-4508-415e-b13a-49a39bb14e4d\"," + "\"contextMap\":{}," + "\"wait\":0" + "}}]", params.get("TestCoreCount"), params.get("TestCoreMhz"),
                params.get("TestMemoryMb"), params.get("TestDom0MinMemoryMb"), s_gson.toJson(params.get("zone")), s_gson.toJson(params.get("pod")),
                s_gson.toJson(params.get("cluster")), s_gson.toJson(params.get("ipaddress")), s_gson.toJson(params.get("private.mac.address")),
                s_gson.toJson(params.get("private.ip.netmask")), s_gson.toJson(params.get("ipaddress")), s_gson.toJson(params.get("private.ip.netmask")),
                s_gson.toJson(params.get("private.mac.address")), s_gson.toJson(params.get("gateway.ip.address")), s_gson.toJson(params.get("ipaddress")),
                s_gson.toJson(params.get("DefaultVirtualDiskFolder")), s_gson.toJson(params.get("DefaultVirtualDiskFolder")), s_gson.toJson(totalSpace),
                s_gson.toJson(usableCapacity));
    }

    public static String getSamplePrimaryDataStoreInfo() {
        String samplePrimaryDataStoreInfo =
            "{\"org.apache.cloudstack.storage.to.PrimaryDataStoreTO\":" + "{\"uuid\":\"" + s_testLocalStoreUUID + "\"," + "\"id\":201," + "\"host\":\"" +
                s_testPrimaryDataStoreHost + "\"," + "\"type\":\"Filesystem\"," // Not used in
                                                                                // PrimaryDataStoreTO
                + "\"poolType\":\"Filesystem\"," // Not used in
                // PrimaryDataStoreTO
                + "\"path\":" + s_testLocalStorePathJSON + "," + "\"port\":0}" + "}";
        return samplePrimaryDataStoreInfo;
    }

    public static String getSampleVolumeObjectTO() {
        String sampleVolumeObjectTO =
            "{\"org.apache.cloudstack.storage.to.VolumeObjectTO\":" + "{\"uuid\":\"19ae8e67-cb2c-4ab4-901e-e0b864272b59\"," + "\"volumeType\":\"ROOT\"," +
                "\"format\":\"VHDX\"," + "\"dataStore\":" + getSamplePrimaryDataStoreInfo() + "," + "\"name\":\"" + s_testSampleVolumeWorkingUUIDNoExt + "\"," +
                "\"size\":52428800," + "\"volumeId\":10,"
                // "\"vmName\":\"i-3-5-VM\"," + // TODO: required?
                + "\"accountId\":3,\"id\":10}" + "}"; // end of destTO
        return sampleVolumeObjectTO;
    }

    public static String getSampleStartCommand() {
        String sample =
            "{\"vm\":{\"id\":17,\"name\":\"i-2-17-VM\"," + "\"type\":\"User\",\"cpus\":1,\"speed\":500," + "\"minRam\":536870912,\"maxRam\":536870912," +
                "\"arch\":\"x86_64\"," + "\"os\":\"CentOS 6.0 (64-bit)\"," + "\"bootArgs\":\"\",\"rebootOnCrash\":false," + "\"enableHA\":false,\"limitCpuUse\":false," +
                "\"vncPassword\":\"31f82f29aff646eb\"," + "\"params\":{}," + "\"uuid\":\"8b030b6a-0243-440a-8cc5-45d08815ca11\"" + ",\"disks\":[" + "{\"data\":" +
                getSampleVolumeObjectTO() + ",\"diskSeq\":0,\"type\":\"ROOT\"}," + "{\"diskSeq\":1,\"type\":\"ISO\"}" + "]," + "\"nics\":[" +
                "{\"deviceId\":0,\"networkRateMbps\":100," + "\"defaultNic\":true," + "\"uuid\":\"99cb4813-23af-428c-a87a-2d1899be4f4b\"," + "\"ip\":\"10.1.1.67\"," +
                "\"netmask\":\"255.255.255.0\"," + "\"gateway\":\"10.1.1.1\"," + "\"mac\":\"02:00:51:2c:00:0e\",\"dns1\":\"4.4.4.4\"," +
                "\"broadcastType\":\"Vlan\",\"type\":\"Guest\"," + "\"broadcastUri\":\"vlan://261\"," + "\"isolationUri\":\"vlan://261\"," +
                "\"isSecurityGroupEnabled\":false}" + "]},\"contextMap\":{},\"wait\":0}";
        return sample;
    }
}
