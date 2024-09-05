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
import static com.cloud.network.NetworkService.DEFAULT_MTU;
import static org.apache.cloudstack.storage.configdrive.ConfigDriveUtils.mergeJsonArraysAndUpdateObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cloud.network.Network;
import com.cloud.vm.NicProfile;
import com.googlecode.ipv6.IPv6Network;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.joda.time.Duration;

import com.cloud.network.NetworkModel;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ConfigDriveBuilder {

    protected static Logger LOGGER = LogManager.getLogger(ConfigDriveBuilder.class);

    /**
     * This is for mocking the File class. We cannot mock the File class directly because Mockito uses it internally.
     * @param filepath
     * @return
     */
    static File getFile(String filepath) {
        return new File(filepath);
    }

    static File getFile(String dirName, String filename) {
        return new File(dirName, filename);
    }

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
     *  We expect the content of the file to be encoded using {@link StandardCharsets#US_ASCII}
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
            LOGGER.warn("Exception hit while trying to recreate directory: " + destPath.getParent().toString());
        }
        return Files.write(destPath, decoded).toFile();
    }

    /**
     *  This method will build the metadata files required by OpenStack driver. Then, an ISO is going to be generated and returned as a String in base 64.
     *  If vmData is null, we throw a {@link CloudRuntimeException}. Moreover, {@link IOException} are captured and re-thrown as {@link CloudRuntimeException}.
     */
    public static String buildConfigDrive(List<NicProfile> nics, List<String[]> vmData, String isoFileName, String driveLabel, Map<String, String> customUserdataParams, Map<Long, List<Network.Service>> supportedServices) {
        if (vmData == null && nics == null) {
            throw new CloudRuntimeException("No VM metadata and nic profile provided");
        }

        Path tempDir = null;
        String tempDirName = null;
        try {
            tempDir = Files.createTempDirectory(ConfigDrive.CONFIGDRIVEDIR);
            tempDirName = tempDir.toString();

            File openStackFolder = new File(tempDirName + ConfigDrive.openStackConfigDriveName);

            writeVendorEmptyJsonFile(openStackFolder);
            writeNetworkData(nics, supportedServices, openStackFolder);
            for (NicProfile nic: nics) {
                if (supportedServices.get(nic.getId()).contains(Network.Service.UserData)) {
                    if (vmData == null) {
                        throw new CloudRuntimeException("No VM metadata provided");
                    }
                    writeVmMetadata(vmData, tempDirName, openStackFolder, customUserdataParams);

                    linkUserData(tempDirName);
                    break;
                }
            }

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
            LOGGER.warn("Failed to delete ConfigDrive temporary directory: " + tempDir.toString(), ioe);
        }
    }

    /**
     *  Generates the ISO file that has the tempDir content.
     *
     *  Max allowed file size of config drive is 64MB [1]. Therefore, if the ISO is bigger than that, we throw a {@link CloudRuntimeException}.
     *  [1] https://docs.openstack.org/project-install-guide/baremetal/draft/configdrive.html
     */
    static String generateAndRetrieveIsoAsBase64Iso(String isoFileName, String driveLabel, String tempDirName) throws IOException {
        File tmpIsoStore = getFile(tempDirName, isoFileName);
        Script command = new Script(getProgramToGenerateIso(), Duration.standardSeconds(300), LOGGER);
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
        LOGGER.debug("Executing config drive creation command: " + command.toString());
        String result = command.execute();
        if (StringUtils.isNotBlank(result)) {
            String errMsg = "Unable to create iso file: " + isoFileName + " due to ge" + result;
            LOGGER.warn(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
        File tmpIsoFile = getFile(tmpIsoStore.getAbsolutePath());
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
        File isoCreator = getFile("/usr/bin/genisoimage");
        if (!isoCreator.exists()) {
            isoCreator = getFile("/usr/bin/mkisofs");
            if (!isoCreator.exists()) {
                isoCreator = getFile("/usr/local/bin/mkisofs");
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
     * First we generate a JSON object using {@link #getNetworkDataJsonObjectForNic(NicProfile, List)}, then we write it to a file called "network_data.json".
     */
    static void writeNetworkData(List<NicProfile> nics, Map<Long, List<Network.Service>> supportedServices, File openStackFolder) {
        JsonObject finalNetworkData = new JsonObject();
        if (needForGeneratingNetworkData(supportedServices)) {
            for (NicProfile nic : nics) {
                List<Network.Service> supportedService = supportedServices.get(nic.getId());
                JsonObject networkData = getNetworkDataJsonObjectForNic(nic, supportedService);

                mergeJsonArraysAndUpdateObject(finalNetworkData, networkData, "links", "id", "type");
                mergeJsonArraysAndUpdateObject(finalNetworkData, networkData, "networks", "id", "type");
                mergeJsonArraysAndUpdateObject(finalNetworkData, networkData, "services", "address", "type");
            }
        }

        writeFile(openStackFolder, "network_data.json", finalNetworkData.toString());
    }

    static boolean needForGeneratingNetworkData(Map<Long, List<Network.Service>> supportedServices) {
        return supportedServices.values().stream().anyMatch(services -> services.contains(Network.Service.Dhcp) || services.contains(Network.Service.Dns));
    }

    /**
     *  Writes an empty JSON file named vendor_data.json in openStackFolder
     *
     *  If the folder does not exist, and we cannot create it, we throw a {@link CloudRuntimeException}.
     */
    static void writeVendorEmptyJsonFile(File openStackFolder) {
        if (openStackFolder.exists() || openStackFolder.mkdirs()) {
            writeFile(openStackFolder, "vendor_data.json", "{}");
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
            LOGGER.debug(String.format("[createConfigDriveIsoForVM] dataType=%s, filename=%s, content=%s", dataType, fileName, (PASSWORD_FILE.equals(fileName) ? "********" : content)));

            createFileInTempDirAnAppendOpenStackMetadataToJsonObject(tempDirName, metaData, dataType, fileName, content, customUserdataParams);
        }
        return metaData;
    }

    /**
     * Creates the {@link JsonObject} using @param nic's metadata. We expect the JSONObject to have the following entries:
     * <ul>
     *     <li> links </li>
     *     <li> networks </li>
     *     <li> services </li>
     * </ul>
     */
    static JsonObject getNetworkDataJsonObjectForNic(NicProfile nic, List<Network.Service> supportedServices) {
        JsonObject networkData = new JsonObject();

        JsonArray links = getLinksJsonArrayForNic(nic);
        JsonArray networks = getNetworksJsonArrayForNic(nic);
        if (links.size() > 0) {
            networkData.add("links", links);
        }
        if (networks.size() > 0) {
            networkData.add("networks", networks);
        }

        JsonArray services = getServicesJsonArrayForNic(nic);
        if (services.size() > 0) {
            networkData.add("services", services);
        }

        return networkData;
    }

    static JsonArray getLinksJsonArrayForNic(NicProfile nic) {
        JsonArray links = new JsonArray();
        if (StringUtils.isNotBlank(nic.getMacAddress())) {
            JsonObject link = new JsonObject();
            link.addProperty("ethernet_mac_address", nic.getMacAddress());
            link.addProperty("id", String.format("eth%d", nic.getDeviceId()));
            link.addProperty("mtu", nic.getMtu() != null ? nic.getMtu() : DEFAULT_MTU);
            link.addProperty("type", "phy");
            links.add(link);
        }
        return links;
    }

    static JsonArray getNetworksJsonArrayForNic(NicProfile nic) {
        JsonArray networks = new JsonArray();
        if (StringUtils.isNotBlank(nic.getIPv4Address())) {
            JsonObject ipv4Network = new JsonObject();
            ipv4Network.addProperty("id", String.format("eth%d", nic.getDeviceId()));
            ipv4Network.addProperty("ip_address", nic.getIPv4Address());
            ipv4Network.addProperty("link", String.format("eth%d", nic.getDeviceId()));
            ipv4Network.addProperty("netmask", nic.getIPv4Netmask());
            ipv4Network.addProperty("network_id", nic.getUuid());
            ipv4Network.addProperty("type", "ipv4");

            JsonArray ipv4RouteArray = new JsonArray();
            JsonObject ipv4Route = new JsonObject();
            ipv4Route.addProperty("gateway", nic.getIPv4Gateway());
            ipv4Route.addProperty("netmask", "0.0.0.0");
            ipv4Route.addProperty("network", "0.0.0.0");
            ipv4RouteArray.add(ipv4Route);

            ipv4Network.add("routes", ipv4RouteArray);

            networks.add(ipv4Network);
        }

        if (StringUtils.isNotBlank(nic.getIPv6Address())) {
            JsonObject ipv6Network = new JsonObject();
            ipv6Network.addProperty("id", String.format("eth%d", nic.getDeviceId()));
            ipv6Network.addProperty("ip_address", nic.getIPv6Address());
            ipv6Network.addProperty("link", String.format("eth%d", nic.getDeviceId()));
            ipv6Network.addProperty("netmask", IPv6Network.fromString(nic.getIPv6Cidr()).getNetmask().toString());
            ipv6Network.addProperty("network_id", nic.getUuid());
            ipv6Network.addProperty("type", "ipv6");

            JsonArray ipv6RouteArray = new JsonArray();
            JsonObject ipv6Route = new JsonObject();
            ipv6Route.addProperty("gateway", nic.getIPv6Gateway());
            ipv6Route.addProperty("netmask", "0");
            ipv6Route.addProperty("network", "::");
            ipv6RouteArray.add(ipv6Route);

            ipv6Network.add("routes", ipv6RouteArray);

            networks.add(ipv6Network);
        }
        return networks;
    }

    static JsonArray getServicesJsonArrayForNic(NicProfile nic) {
        JsonArray services = new JsonArray();
        if (StringUtils.isNotBlank(nic.getIPv4Dns1())) {
            services.add(getDnsServiceObject(nic.getIPv4Dns1()));
        }

        if (StringUtils.isNotBlank(nic.getIPv4Dns2())) {
            services.add(getDnsServiceObject(nic.getIPv4Dns2()));
        }

        if (StringUtils.isNotBlank(nic.getIPv6Dns1())) {
            services.add(getDnsServiceObject(nic.getIPv6Dns1()));
        }

        if (StringUtils.isNotBlank(nic.getIPv6Dns2())) {
            services.add(getDnsServiceObject(nic.getIPv6Dns2()));
        }
        return services;
    }

    private static JsonObject getDnsServiceObject(String dnsAddress) {
        JsonObject dnsService = new JsonObject();
        dnsService.addProperty("address", dnsAddress);
        dnsService.addProperty("type", "dns");
        return dnsService;
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
        File file = getFile(userDataFilePath);
        if (file.exists()) {
            Script hardLink = new Script("ln", Duration.standardSeconds(300), LOGGER);
            hardLink.add(userDataFilePath);
            hardLink.add(tempDirName + ConfigDrive.openStackConfigDriveName + "user_data");
            LOGGER.debug("execute command: " + hardLink.toString());

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
