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

package org.apache.cloudstack.storage.configdrive;

import static com.cloud.network.NetworkModel.CONFIGDATA_CONTENT;
import static com.cloud.network.NetworkModel.CONFIGDATA_DIR;
import static com.cloud.network.NetworkModel.CONFIGDATA_FILE;
import static com.cloud.network.NetworkModel.PASSWORD_FILE;
import static com.cloud.network.NetworkModel.USERDATA_FILE;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.Duration;

import com.cloud.network.NetworkModel;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ConfigDriveBuilder {

    public static final Logger LOG = Logger.getLogger(ConfigDriveBuilder.class);

    private static void writeFile(final File folder, final String file, final String content) {
        if (folder == null || Strings.isNullOrEmpty(file)) {
            return;
        }
        final File vendorDataFile = new File(folder, file);
        try (final FileWriter fw = new FileWriter(vendorDataFile); final BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(content);
        } catch (IOException ex) {
            throw new CloudRuntimeException("Failed to create config drive file " + file, ex);
        }
    }

    public static String fileToBase64String(final File isoFile) throws IOException {
        byte[] encoded = Base64.encodeBase64(FileUtils.readFileToByteArray(isoFile));
        return new String(encoded, StandardCharsets.US_ASCII);
    }

    public static File base64StringToFile(final String encodedIsoData, final String folder, final String fileName) throws IOException {
        byte[] decoded = Base64.decodeBase64(encodedIsoData.getBytes(StandardCharsets.US_ASCII));
        Path destPath = Paths.get(folder, fileName);
        return Files.write(destPath, decoded).toFile();
    }

    public static String buildConfigDrive(final List<String[]> vmData, final String isoFileName, final String driveLabel) {
        if (vmData == null) {
            throw new CloudRuntimeException("No VM metadata provided");
        }

        Path tempDir = null;
        String tempDirName = null;
        try {
            tempDir = Files.createTempDirectory(ConfigDrive.CONFIGDRIVEDIR);
            tempDirName = tempDir.toString();

            File openStackFolder = new File(tempDirName + ConfigDrive.openStackConfigDriveName);
            if (openStackFolder.exists() || openStackFolder.mkdirs()) {
                writeFile(openStackFolder, "vendor_data.json", "{}");
                writeFile(openStackFolder, "network_data.json", "{}");
            } else {
                throw new CloudRuntimeException("Failed to create folder " + openStackFolder);
            }

            JsonObject metaData = new JsonObject();
            for (String[] item : vmData) {
                String dataType = item[CONFIGDATA_DIR];
                String fileName = item[CONFIGDATA_FILE];
                String content = item[CONFIGDATA_CONTENT];
                LOG.debug(String.format("[createConfigDriveIsoForVM] dataType=%s, filename=%s, content=%s",
                        dataType, fileName, (fileName.equals(PASSWORD_FILE)?"********":content)));

                // create file with content in folder
                if (dataType != null && !dataType.isEmpty()) {
                    //create folder
                    File typeFolder = new File(tempDirName + ConfigDrive.cloudStackConfigDriveName + dataType);
                    if (typeFolder.exists() || typeFolder.mkdirs()) {
                        if (StringUtils.isNotEmpty(content)) {
                            File file = new File(typeFolder, fileName + ".txt");
                            try  {
                                if (fileName.equals(USERDATA_FILE)) {
                                    // User Data is passed as a base64 encoded string
                                    FileUtils.writeByteArrayToFile(file, Base64.decodeBase64(content));
                                } else {
                                    FileUtils.write(file, content, com.cloud.utils.StringUtils.getPreferredCharset());
                                }
                            } catch (IOException ex) {
                                throw new CloudRuntimeException("Failed to create file ", ex);
                            }
                        }
                    } else {
                        throw new CloudRuntimeException("Failed to create folder: " + typeFolder);
                    }

                    //now write the file to the OpenStack directory
                    metaData = buildOpenStackMetaData(metaData, dataType, fileName, content);
                }
            }
            writeFile(openStackFolder, "meta_data.json", metaData.toString());

            String linkResult = linkUserData(tempDirName);
            if (linkResult != null) {
                String errMsg = "Unable to create user_data link due to " + linkResult;
                throw new CloudRuntimeException(errMsg);
            }

            File tmpIsoStore = new File(tempDirName, new File(isoFileName).getName());
            Script command = new Script("/usr/bin/genisoimage", Duration.standardSeconds(300), LOG);
            command.add("-o", tmpIsoStore.getAbsolutePath());
            command.add("-ldots");
            command.add("-allow-lowercase");
            command.add("-allow-multidot");
            command.add("-cache-inodes"); // Enable caching inode and device numbers to find hard links to files.
            command.add("-l");
            command.add("-quiet");
            command.add("-J");
            command.add("-r");
            command.add("-V", driveLabel);
            command.add(tempDirName);
            LOG.debug("Executing config drive creation command: " + command.toString());
            String result = command.execute();
            if (result != null) {
                String errMsg = "Unable to create iso file: " + isoFileName + " due to " + result;
                LOG.warn(errMsg);
                throw new CloudRuntimeException(errMsg);
            }
            File tmpIsoFile = new File(tmpIsoStore.getAbsolutePath());
            // Max allowed file size of config drive is 64MB: https://docs.openstack.org/project-install-guide/baremetal/draft/configdrive.html
            if (tmpIsoFile.length() > (64L * 1024L * 1024L)) {
                throw new CloudRuntimeException("Config drive file exceeds maximum allowed size of 64MB");
            }
            return fileToBase64String(tmpIsoFile);
        } catch (IOException e) {
            throw new CloudRuntimeException("Failed due to", e);
        } finally {
            try {
                FileUtils.deleteDirectory(tempDir.toFile());
            } catch (IOException ioe) {
                LOG.warn("Failed to delete ConfigDrive temporary directory: " + tempDirName, ioe);
            }
        }
    }

    private static String linkUserData(String tempDirName) {
        //Hard link the user_data.txt file with the user_data file in the OpenStack directory.
        String userDataFilePath = tempDirName + ConfigDrive.cloudStackConfigDriveName + "userdata/user_data.txt";
        if ((new File(userDataFilePath).exists())) {
            Script hardLink = new Script("ln", Duration.standardSeconds(300), LOG);
            hardLink.add(userDataFilePath);
            hardLink.add(tempDirName + ConfigDrive.openStackConfigDriveName + "user_data");
            LOG.debug("execute command: " + hardLink.toString());
            return hardLink.execute();
        }
        return null;
    }

    private static JsonArray arrayOf(JsonElement... elements) {
        JsonArray array = new JsonArray();
        for (JsonElement element : elements) {
            array.add(element);
        }
        return array;
    }

    private static JsonObject buildOpenStackMetaData(JsonObject metaData, String dataType, String fileName, String content) {
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

}
