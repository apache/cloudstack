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
package org.apache.cloudstack.storage;


import com.cloud.agent.api.Answer;
import com.cloud.agent.api.HandleConfigDriveIsoCommand;
import com.cloud.agent.api.to.DataStoreTO;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.network.NetworkModel;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.naming.ConfigurationException;

import static com.cloud.network.NetworkModel.CONFIGDATA_CONTENT;
import static com.cloud.network.NetworkModel.CONFIGDATA_DIR;
import static com.cloud.network.NetworkModel.CONFIGDATA_FILE;

import static com.cloud.network.NetworkModel.METATDATA_DIR;
import static com.cloud.network.NetworkModel.PASSWORD_DIR;
import static com.cloud.network.NetworkModel.PASSWORD_FILE;
import static com.cloud.network.NetworkModel.PUBLIC_KEYS_FILE;
import static com.cloud.network.NetworkModel.USERDATA_DIR;
import static com.cloud.network.NetworkModel.USERDATA_FILE;

public class ConfigDriveFactory {
    public static final Logger s_logger = Logger.getLogger(ConfigDriveFactory.class);

    private static final String CLOUD_STACK_CONFIG_DRIVE_NAME = "/cloudstack/";
    private static final String OPEN_STACK_CONFIG_DRIVE_NAME = "/openstack/latest/";
    public static final String FAILED_TO_CREATE_FOLDER = "Failed to create folder ";
    public static final String FAILED_TO_CREATE_FILE = "Failed to create file ";
    public static final String EMPTY_SET = "{}";
    private final Integer nfsVersion;
    protected boolean inSystemVM = false;

    private StorageAttacher attacher;

    private static final Map<String, String> updatableConfigData = Maps.newHashMap();
    static {

        updatableConfigData.put(PUBLIC_KEYS_FILE, METATDATA_DIR);
        updatableConfigData.put(USERDATA_FILE, USERDATA_DIR);
        updatableConfigData.put(PASSWORD_FILE, PASSWORD_DIR);
    }


    private int timeout;

    public ConfigDriveFactory(Integer pNfsVersion, StorageAttacher pAttacher) {
        nfsVersion = pNfsVersion;
        attacher = pAttacher;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    private String linkUserData(String tempDirName) {
        //Hard link the user_data.txt file with the user_data file in the OpenStack directory.
        String userDataFilePath = tempDirName + CLOUD_STACK_CONFIG_DRIVE_NAME + "userdata/user_data.txt";
        if ((new File(userDataFilePath).exists())) {
            Script hardLink = new Script(!inSystemVM, "ln", timeout, s_logger);
            hardLink.add(userDataFilePath);
            hardLink.add(tempDirName + OPEN_STACK_CONFIG_DRIVE_NAME + "user_data");
            s_logger.debug("execute command: " + hardLink.toString());
            return hardLink.execute();
        }
        return null;
    }

    public Answer executeRequest(HandleConfigDriveIsoCommand cmd) {

        if(s_logger.isDebugEnabled()) {
            s_logger.debug(String.format("VMdata %s, attach = %s", cmd.getVmData(), cmd.isCreate()));
        }
        if (cmd.isCreate()) {
            if(cmd.getVmData() == null) return new Answer(cmd, false, "No Vmdata available");
            String nfsMountPoint = attacher.getRootDir(cmd.getDestStore().getUrl(), nfsVersion);
            File isoFile = new File(nfsMountPoint, cmd.getIsoFile());
            if(isoFile.exists()) {
                if (!cmd.isUpdate()) {
                    return new Answer(cmd, true, "ISO already available");
                } else {
                    // Find out if we have to recover the password/ssh-key from the already available ISO.
                    try {
                        List<String[]> recoveredVmData = recoverVmData(isoFile);
                        for (String[] vmDataEntry : cmd.getVmData()) {
                            if (updatableConfigData.containsKey(vmDataEntry[CONFIGDATA_FILE])
                                    && updatableConfigData.get(vmDataEntry[CONFIGDATA_FILE]).equals(vmDataEntry[CONFIGDATA_DIR])) {
                                updateVmData(recoveredVmData, vmDataEntry);
                            }
                        }
                        cmd.setVmData(recoveredVmData);
                    } catch (IOException e) {
                        return new Answer(cmd, e);
                    }
                }
            }
            return createConfigDriveIsoForVM(cmd);
        } else {
            DataStoreTO dstore = cmd.getDestStore();
            if (dstore instanceof NfsTO) {
                NfsTO nfs = (NfsTO) dstore;
                String relativeTemplatePath = new File(cmd.getIsoFile()).getParent();
                String nfsMountPoint = attacher.getRootDir(nfs.getUrl(), nfsVersion);
                File tmpltPath = new File(nfsMountPoint, relativeTemplatePath);
                try {
                    FileUtils.deleteDirectory(tmpltPath);
                } catch (IOException e) {
                    return new Answer(cmd, e);
                }
                return new Answer(cmd);
            } else {
                return new Answer(cmd, false, "Not implemented yet");
            }
        }
    }

    public Answer createConfigDriveIsoForVM(HandleConfigDriveIsoCommand cmd) {
        //create folder for the VM
        if (cmd.getVmData() != null) {

            Path tempDir = null;
            String tempDirName = null;
            try {
                tempDir = java.nio.file.Files.createTempDirectory("ConfigDrive");
                tempDirName = tempDir.toString();

                //create OpenStack files
                //create folder with empty files
                File openStackFolder = new File(tempDirName + OPEN_STACK_CONFIG_DRIVE_NAME);
                if (openStackFolder.exists() || openStackFolder.mkdirs()) {
                    File vendorDataFile = new File(openStackFolder,"vendor_data.json");
                    WriteFileProcedure writeFileProcedure = new WriteFileProcedure(cmd, vendorDataFile, EMPTY_SET);
                    if (writeFileProcedure.invoke())
                        return new Answer(cmd, writeFileProcedure.getEx());
                    File networkDataFile = new File(openStackFolder, "network_data.json");
                    writeFileProcedure = new WriteFileProcedure(cmd, networkDataFile, EMPTY_SET);
                    if (writeFileProcedure.invoke())
                        return new Answer(cmd, writeFileProcedure.getEx());
                } else {
                    s_logger.error(FAILED_TO_CREATE_FOLDER + openStackFolder);
                    return new Answer(cmd, false, FAILED_TO_CREATE_FOLDER + openStackFolder);
                }

                JsonObject metaData = new JsonObject();
                for (String[] item : cmd.getVmData()) {
                    String dataType = item[CONFIGDATA_DIR];
                    String fileName = item[CONFIGDATA_FILE];
                    String content = item[CONFIGDATA_CONTENT];
                    s_logger.debug(String.format("[createConfigDriveIsoForVM] dataType=%s, filename=%s, content=%s",
                            dataType, fileName, (fileName.equals(PASSWORD_FILE)?"********":content)));

                    // create file with content in folder
                    if (dataType != null && !dataType.isEmpty()) {
                        //create folder
                        File typeFolder = new File(tempDirName + CLOUD_STACK_CONFIG_DRIVE_NAME + dataType);
                        if (typeFolder.exists() || typeFolder.mkdirs()) {
                            if (StringUtils.isNotEmpty(content)) {
                                File file = new File(typeFolder, fileName + ".txt");
                                WriteFileProcedure writeFileProcedure = new WriteFileProcedure(cmd, file, content);
                                if (writeFileProcedure.invoke())
                                    return new Answer(cmd, writeFileProcedure.getEx());
                            }
                        } else {
                            s_logger.error(FAILED_TO_CREATE_FOLDER + typeFolder);
                            return new Answer(cmd, false, FAILED_TO_CREATE_FOLDER + typeFolder);
                        }

                        //now write the file to the OpenStack directory
                        metaData = constructOpenStackMetaData(metaData, dataType, fileName, content);
                    }
                }

                File metaDataFile = new File(openStackFolder, "meta_data.json");
                WriteFileProcedure writeFileProcedure = new WriteFileProcedure(cmd, metaDataFile, metaData.toString());
                if(writeFileProcedure.invoke())
                    return new Answer(cmd,writeFileProcedure.getEx());

                String linkResult = linkUserData(tempDirName);
                if (linkResult != null) {
                    String errMsg = "Unable to create user_data link due to " + linkResult;
                    s_logger.warn(errMsg);
                    return new Answer(cmd, false, errMsg);
                }

                File tmpIsoStore = new File(tempDirName, new File(cmd.getIsoFile()).getName());
                Script command = new Script(!inSystemVM, "/usr/bin/genisoimage", timeout, s_logger);
                command.add("-o", tmpIsoStore.getAbsolutePath());
                command.add("-ldots");
                command.add("-allow-lowercase");
                command.add("-allow-multidot");
                command.add("-cache-inodes"); // Enable caching inode and device numbers to find hard links to files.
                command.add("-l");
                command.add("-quiet");
                command.add("-J");
                command.add("-r");
                command.add("-V", cmd.getConfigDriveLabel());
                command.add(tempDirName);
                s_logger.debug("execute command: " + command.toString());
                String result = command.execute();
                if (result != null) {
                    String errMsg = "Unable to create iso file: " + cmd.getIsoFile() + " due to " + result;
                    s_logger.warn(errMsg);
                    return new Answer(cmd, false, errMsg);
                }
                copyLocalToNfs(tmpIsoStore, new File(cmd.getIsoFile()), cmd.getDestStore());

            } catch (IOException e) {
                return new Answer(cmd, e);
            } catch (ConfigurationException e) {
                s_logger.warn("SecondStorageException ", e);
                return new Answer(cmd, e);
            } finally {
                try {
                    FileUtils.deleteDirectory(tempDir.toFile());
                } catch (IOException ioe) {
                    s_logger.warn("Failed to delete ConfigDrive temporary directory: " + tempDirName, ioe);
                }
            }
        }
        return new Answer(cmd);
    }

    private List<String[]> recoverVmData(File isoFile) throws IOException {
        String tempDirName = null;
        List<String[]> recoveredVmData = Lists.newArrayList();
        boolean mounted = false;
        try {
            Path tempDir = java.nio.file.Files.createTempDirectory("ConfigDrive");
            tempDirName = tempDir.toString();

            // Unpack the current config drive file
            Script command = new Script(!inSystemVM, "mount", timeout, s_logger);
            command.add("-o", "loop");
            command.add(isoFile.getAbsolutePath());
            command.add(tempDirName);
            String result = command.execute();

            if (result != null) {
                String errMsg = "Unable to mount " + isoFile.getAbsolutePath() + " at " + tempDirName + " due to " + result;
                s_logger.error(errMsg);
                throw new IOException(errMsg);
            }
            mounted = true;


            // Scan directory structure
            for (File configDirectory: (new File(tempDirName, "cloudstack")).listFiles()){
                for (File configFile: configDirectory.listFiles()) {
                    recoveredVmData.add(new String[]{configDirectory.getName(),
                            Files.getNameWithoutExtension(configFile.getName()),
                            Files.readFirstLine(configFile, Charset.defaultCharset())});
                }
            }

        } finally {
            if (mounted) {
                Script command = new Script(!inSystemVM, "umount", timeout, s_logger);
                command.add(tempDirName);
                String result = command.execute();
                if (result != null) {
                    s_logger.warn("Unable to umount " + tempDirName + " due to " + result);
                }
            }
            try {
                FileUtils.deleteDirectory(new File(tempDirName));
            } catch (IOException ioe) {
                s_logger.warn("Failed to delete ConfigDrive temporary directory: " + tempDirName, ioe);
            }
        }
        return  recoveredVmData;
    }
    JsonObject constructOpenStackMetaData(JsonObject metaData, String dataType, String fileName, String content) {
        if (dataType.equals(NetworkModel.METATDATA_DIR) &&  StringUtils.isNotEmpty(content)) {
            //keys are a special case in OpenStack format
            if (NetworkModel.PUBLIC_KEYS_FILE.equals(fileName)) {
                String[] keyArray = content.replace("\\n", "").split(" ");
                String keyName = "key";
                if (keyArray.length > 3 && StringUtils.isNotEmpty(keyArray[2])){
                    keyName = keyArray[2];
                }

                JsonObject keyLegacy = new JsonObject();
                keyLegacy.addProperty("type", "ssh");
                keyLegacy.addProperty("data", content.replace("\\n", ""));
                keyLegacy.addProperty("name", keyName);
                metaData.add("keys", arrayOf(keyLegacy));

                JsonObject key = new JsonObject();
                key.addProperty(keyName, content);
                metaData.add("public_keys", key);
            } else if (NetworkModel.openStackFileMapping.get(fileName) != null) {
                metaData.addProperty(NetworkModel.openStackFileMapping.get(fileName), content);
            }
        }
        return metaData;
    }
    private static JsonArray arrayOf(JsonElement... elements) {
        JsonArray array = new JsonArray();
        for (JsonElement element : elements) {
            array.add(element);
        }
        return array;
    }

    private void updateVmData(List<String[]> recoveredVmData, String[] vmDataEntry) {
        for (String[] recoveredEntry : recoveredVmData) {
            if (recoveredEntry[CONFIGDATA_DIR].equals(vmDataEntry[CONFIGDATA_DIR])
                    && recoveredEntry[CONFIGDATA_FILE].equals(vmDataEntry[CONFIGDATA_FILE])) {
                recoveredEntry[CONFIGDATA_CONTENT] = vmDataEntry[CONFIGDATA_CONTENT];
                return;
            }
        }
        recoveredVmData.add(vmDataEntry);
    }

    protected void copyLocalToNfs(File localFile, File isoFile, DataStoreTO destData) throws ConfigurationException, IOException {
        String scriptsDir = "scripts/storage/secondary";
        String createVolScr = Script.findScript(scriptsDir, "createvolume.sh");
        if (createVolScr == null) {
            throw new ConfigurationException("Unable to find createvolume.sh");
        }
        s_logger.info("createvolume.sh found in " + createVolScr);

        int installTimeoutPerGig = 180 * 60 * 1000;
        int imgSizeGigs = (int) Math.ceil(localFile.length() * 1.0d / (1024 * 1024 * 1024));
        imgSizeGigs++; // add one just in case
        long timeout = imgSizeGigs * installTimeoutPerGig;

        Script scr = new Script(createVolScr, timeout, s_logger);
        scr.add("-s", Integer.toString(imgSizeGigs));
        scr.add("-n", isoFile.getName());
        scr.add("-t", attacher.getRootDir(destData.getUrl(), nfsVersion) + "/" + isoFile.getParent());
        scr.add("-f", localFile.getAbsolutePath());
        scr.add("-d", "configDrive");
        String result;
        result = scr.execute();

        if (result != null) {
            // script execution failure
            throw new CloudRuntimeException("Failed to run script " + createVolScr);
        }
    }

    private class WriteFileProcedure {
        private HandleConfigDriveIsoCommand cmd;
        private File vendorDataFile;
        private String content;
        private IOException ex;

        public WriteFileProcedure(HandleConfigDriveIsoCommand cmd, File vendorDataFile, String content) {
            this.cmd = cmd;
            this.vendorDataFile = vendorDataFile;
            this.content = content;
        }

        public IOException getEx() {
            return ex;
        }

        public boolean invoke() {
            try (FileWriter fw = new FileWriter(vendorDataFile); BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(content);
            } catch (IOException ex) {ConfigDriveFactory.WriteFileProcedure.this.ex = ex;
                s_logger.error(FAILED_TO_CREATE_FILE, ex);
                return true;
            }
            return false;
        }
    }
}
