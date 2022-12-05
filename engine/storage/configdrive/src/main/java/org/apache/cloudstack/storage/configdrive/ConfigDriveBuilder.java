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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.Duration;

import com.cloud.network.NetworkModel;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ConfigDriveBuilder {

    public static final Logger LOG = Logger.getLogger(ConfigDriveBuilder.class);

    /**
     * Writes a content {@link String} to a file that is going to be created in a folder. We will not append to the file if it already exists. Therefore, its content will be overwritten.
     * Moreover, the charset used is {@link com.cloud.utils.StringUtils#getPreferredCharset()}.
     *
     * We expect the folder object and the file not to be null/empty.
     */
    static void writeFile(File folder, String file, String content) {
        try {
            FileUtils.write(new File(folder, file), content, com.cloud.utils.StringUtils.getPreferredCharset(), false);
        } catch (IOException ex) {
            throw new CloudRuntimeException("Failed to create config drive file " + file, ex);
        }
    }

    /**
     *  Read the content of a {@link File} and convert it to a String in base 64.
     *  We expect the content of the file to be encoded using {@link StandardCharsets#US_ASC}
     */
    public static String fileToBase64String(File isoFile) throws IOException {
        byte[] encoded = Base64.encodeBase64(FileUtils.readFileToByteArray(isoFile));
        return new String(encoded, StandardCharsets.US_ASCII);
    }

    /**
     * Writes a String encoded in base 64 to a file in the given folder.
     * The content will be decoded and then written to the file. Be aware that we will overwrite the content of the file if it already exists.
     * Moreover, the content will must be encoded in {@link  StandardCharsets#US_ASCII} before it is encoded in base 64.
     */
    public static File base64StringToFile(String encodedIsoData, String folder, String fileName) throws IOException {
        byte[] decoded = Base64.decodeBase64(encodedIsoData.getBytes(StandardCharsets.US_ASCII));
        Path destPath = Paths.get(folder, fileName);
        try {
            Files.createDirectories(destPath.getParent());
        } catch (final IOException e) {
            LOG.warn("Exception hit while trying to recreate directory: " + destPath.getParent().toString());
        }
        return Files.write(destPath, decoded).toFile();
    }

    /**
     *  This method will build the metadata files required by OpenStack driver. Then, an ISO is going to be generated and returned as a String in base 64.
     *  If vmData is null, we throw a {@link CloudRuntimeException}. Moreover, {@link IOException} are captured and re-thrown as {@link CloudRuntimeException}.
     */
    public static String buildConfigDrive(List<String[]> vmData, String isoFileName, String driveLabel, Map<String, String> customUserdataParams) {
        if (vmData == null) {
            throw new CloudRuntimeException("No VM metadata provided");
        }

        Path tempDir = null;
        String tempDirName = null;
        try {
            tempDir = Files.createTempDirectory(ConfigDrive.CONFIGDRIVEDIR);
            tempDirName = tempDir.toString();

            File openStackFolder = new File(tempDirName + ConfigDrive.openStackConfigDriveName);

            writeVendorAndNetworkEmptyJsonFile(openStackFolder);
            writeVmMetadata(vmData, tempDirName, openStackFolder, customUserdataParams);

            linkUserData(tempDirName);

            return generateAndRetrieveIsoAsBase64Iso(isoFileName, driveLabel, tempDirName);
        } catch (IOException e) {
            throw new CloudRuntimeException("Failed due to", e);
        } finally {
            deleteTempDir(tempDir);
        }
    }

    private static void deleteTempDir(Path tempDir) {
        try {
            if (tempDir != null) {
                FileUtils.deleteDirectory(tempDir.toFile());
            }
        } catch (IOException ioe) {
            LOG.warn("Failed to delete ConfigDrive temporary directory: " + tempDir.toString(), ioe);
        }
    }

    /**
     *  Generates the ISO file that has the tempDir content.
     *
     *  Max allowed file size of config drive is 64MB [1]. Therefore, if the ISO is bigger than that, we throw a {@link CloudRuntimeException}.
     *  [1] https://docs.openstack.org/project-install-guide/baremetal/draft/configdrive.html
     */
    static String generateAndRetrieveIsoAsBase64Iso(String isoFileName, String driveLabel, String tempDirName) throws IOException {
        File tmpIsoStore = new File(tempDirName, isoFileName);
        Script command = new Script(getProgramToGenerateIso(), Duration.standardSeconds(300), LOG);
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
        if (StringUtils.isNotBlank(result)) {
            String errMsg = "Unable to create iso file: " + isoFileName + " due to ge" + result;
            LOG.warn(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
        File tmpIsoFile = new File(tmpIsoStore.getAbsolutePath());
        if (tmpIsoFile.length() > (64L * 1024L * 1024L)) {
            throw new CloudRuntimeException("Config drive file exceeds maximum allowed size of 64MB");
        }
        return fileToBase64String(tmpIsoFile);
    }

    /**
     *  Checks if the 'genisoimage' or 'mkisofs' is available and return the full qualified path for the program.
     *  The path checked are the following:
     *  <ul>
     *  <li> /usr/bin/genisoimage
     *  <li> /usr/bin/mkisofs
     * </ul> /usr/local/bin/mkisofs
     */
    static String getProgramToGenerateIso() throws IOException {
        File isoCreator = new File("/usr/bin/genisoimage");
        if (!isoCreator.exists()) {
            isoCreator = new File("/usr/bin/mkisofs");
            if (!isoCreator.exists()) {
                isoCreator = new File("/usr/local/bin/mkisofs");
            }
        }
        if (!isoCreator.exists()) {
            throw new CloudRuntimeException("Cannot create iso for config drive using any know tool. Known paths [/usr/bin/genisoimage, /usr/bin/mkisofs, /usr/local/bin/mkisofs]");
        }
        if (!isoCreator.canExecute()) {
            throw new CloudRuntimeException("Cannot create iso for config drive using: " + isoCreator.getCanonicalPath());
        }
        return isoCreator.getCanonicalPath();
    }

    /**
     * First we generate a JSON object using {@link #createJsonObjectWithVmData(List, String, Map)}, then we write it to a file called "meta_data.json".
     */
    static void writeVmMetadata(List<String[]> vmData, String tempDirName, File openStackFolder, Map<String, String> customUserdataParams) {
        JsonObject metaData = createJsonObjectWithVmData(vmData, tempDirName, customUserdataParams);
        writeFile(openStackFolder, "meta_data.json", metaData.toString());
    }

    /**
     *  Writes the following empty JSON files:
     *  <ul>
     *      <li> vendor_data.json
     *      <li> network_data.json
     *  </ul>
     *
     *  If the folder does not exist and we cannot create it, we throw a {@link CloudRuntimeException}.
     */
    static void writeVendorAndNetworkEmptyJsonFile(File openStackFolder) {
        if (openStackFolder.exists() || openStackFolder.mkdirs()) {
            writeFile(openStackFolder, "vendor_data.json", "{}");
            writeFile(openStackFolder, "network_data.json", "{}");
        } else {
            throw new CloudRuntimeException("Failed to create folder " + openStackFolder);
        }
    }

    /**
     * Creates the {@link JsonObject} with VM's metadata. The vmData is a list of arrays; we expect this list to have the following entries:
     * <ul>
     *  <li> [0]: config data type
     *  <li> [1]: config data file name
     *  <li> [2]: config data file content
     * </ul>
     */
    static JsonObject createJsonObjectWithVmData(List<String[]> vmData, String tempDirName, Map<String, String> customUserdataParams) {
        JsonObject metaData = new JsonObject();
        for (String[] item : vmData) {
            String dataType = item[CONFIGDATA_DIR];
            String fileName = item[CONFIGDATA_FILE];
            String content = item[CONFIGDATA_CONTENT];
            LOG.debug(String.format("[createConfigDriveIsoForVM] dataType=%s, filename=%s, content=%s", dataType, fileName, (PASSWORD_FILE.equals(fileName) ? "********" : content)));

            createFileInTempDirAnAppendOpenStackMetadataToJsonObject(tempDirName, metaData, dataType, fileName, content, customUserdataParams);
        }
        return metaData;
    }

    static void createFileInTempDirAnAppendOpenStackMetadataToJsonObject(String tempDirName, JsonObject metaData, String dataType, String fileName, String content, Map<String, String> customUserdataParams) {
        if (StringUtils.isBlank(dataType)) {
            return;
        }
        //create folder
        File typeFolder = new File(tempDirName + ConfigDrive.cloudStackConfigDriveName + dataType);
        if (!typeFolder.exists() && !typeFolder.mkdirs()) {
            throw new CloudRuntimeException("Failed to create folder: " + typeFolder);
        }
        if (StringUtils.isNotBlank(content)) {
            File file = new File(typeFolder, fileName + ".txt");
            try {
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

        //now write the file to the OpenStack directory
        buildOpenStackMetaData(metaData, dataType, fileName, content);
        buildCustomUserdataParamsMetaData(metaData, dataType, fileName, content, customUserdataParams);
    }

    protected static void buildCustomUserdataParamsMetaData(JsonObject metaData, String dataType, String fileName, String content, Map<String, String> customUserdataParams) {
        if (!NetworkModel.METATDATA_DIR.equals(dataType)) {
            return;
        }
        if (StringUtils.isEmpty(content)) {
            return;
        }
        if (MapUtils.isNotEmpty(customUserdataParams)) {
            Set<String> userdataVariableFileNames = customUserdataParams.keySet();
            if (userdataVariableFileNames.contains(fileName)) {
                metaData.addProperty(fileName, content);
            }
        }
    }

    /**
     * Hard link the user_data.txt file with the user_data file in the OpenStack directory.
     */
    static void linkUserData(String tempDirName) {
        String userDataFilePath = tempDirName + ConfigDrive.cloudStackConfigDriveName + "userdata/user_data.txt";
        File file = new File(userDataFilePath);
        if (file.exists()) {
            Script hardLink = new Script("ln", Duration.standardSeconds(300), LOG);
            hardLink.add(userDataFilePath);
            hardLink.add(tempDirName + ConfigDrive.openStackConfigDriveName + "user_data");
            LOG.debug("execute command: " + hardLink.toString());

            String executionResult = hardLink.execute();
            if (StringUtils.isNotBlank(executionResult)) {
                throw new CloudRuntimeException("Unable to create user_data link due to " + executionResult);
            }
        }
    }

    private static JsonArray arrayOf(JsonElement... elements) {
        JsonArray array = new JsonArray();
        for (JsonElement element : elements) {
            array.add(element);
        }
        return array;
    }

    private static void buildOpenStackMetaData(JsonObject metaData, String dataType, String fileName, String content) {
        if (!NetworkModel.METATDATA_DIR.equals(dataType)) {
            return;
        }
        if (StringUtils.isEmpty(content)) {
            return;
        }
        //keys are a special case in OpenStack format
        if (NetworkModel.PUBLIC_KEYS_FILE.equals(fileName)) {
            String[] keyArray = content.replace("\\n", "").split(" ");
            String keyName = "key";
            if (keyArray.length > 3 && StringUtils.isNotEmpty(keyArray[2])) {
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

}
