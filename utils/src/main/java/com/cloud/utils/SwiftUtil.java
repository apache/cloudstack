//
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
//

package com.cloud.utils;

import java.io.File;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

public class SwiftUtil {
    private static Logger logger = Logger.getLogger(SwiftUtil.class);
    protected static final long SWIFT_MAX_SIZE = 5L * 1024L * 1024L * 1024L;
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final String CD_SRC = "cd %s;";
    private static final String SWIFT_CMD= "/usr/bin/python %s -A %s -U %s:%s -K %s %s";
    private static final String WITH_STORAGE_POLICY = " --storage-policy \"%s\"";
    private static final String WITH_SEGMENTS = " -S "+SWIFT_MAX_SIZE;
    private static final String[] OPERATIONS_WITH_STORAGE_POLICIES = {"post","upload"};

    public interface SwiftClientCfg {
        String getAccount();

        String getUserName();

        String getKey();

        String getEndPoint();

        String getStoragePolicy();
    }

    private static String getSwiftCLIPath() {
        String swiftCLI = Script.findScript("scripts/storage/secondary", "swift");
        if (swiftCLI == null) {
            logger.debug("Can't find swift cli at scripts/storage/secondary/swift");
            throw new CloudRuntimeException("Can't find swift cli at scripts/storage/secondary/swift");
        }
        return swiftCLI;
    }

    public static boolean postMeta(SwiftClientCfg cfg, String container, String object, Map<String, String> metas) {
        Script command = new Script("/bin/bash", logger);
        command.add("-c");
        command.add(getSwiftObjectCmd(cfg, getSwiftCLIPath(),"post", container, object) + getMeta(metas));

        OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        String result = command.execute(parser);
        if (result != null) {
            throw new CloudRuntimeException("Failed to post meta" + result);
        }
        return true;
    }

    public static String putObject(SwiftClientCfg cfg, File srcFile, String container, String fileName) {
        if (fileName == null) {
            fileName = srcFile.getName();
        }

        Script command = new Script("/bin/bash", logger);
        command.add("-c");
        command.add(String.format(CD_SRC, srcFile.getParent())+getUploadObjectCommand(cfg, getSwiftCLIPath(), container,fileName, srcFile.length()));

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = command.execute(parser);
        if (result != null) {
            throw new CloudRuntimeException("Failed to upload file: " + result);
        }

        if (parser.getLines() != null) {
            String[] lines = parser.getLines().split("\\n");
            for (String line : lines) {
                if (line.contains("Errno") || line.contains("failed") || line.contains("not found")) {
                    throw new CloudRuntimeException("Failed to upload file: " + Arrays.toString(lines));
                }
            }
        }

        return container + File.separator + srcFile.getName();
    }

    public static String[] list(SwiftClientCfg swift, String container, String rFilename) {
        StringBuilder swiftCmdBuilder = new StringBuilder();
        swiftCmdBuilder.append(getSwiftContainerCmd(swift, getSwiftCLIPath(), "list", container));

        if (rFilename != null) {
            swiftCmdBuilder.append(" -p ");
            swiftCmdBuilder.append(rFilename);
        }

        Script command = new Script("/bin/bash", logger);
        command.add("-c");
        command.add(swiftCmdBuilder.toString());

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = command.execute(parser);
        if (result == null && parser.getLines() != null && !parser.getLines().equalsIgnoreCase("")) {
            return parser.getLines().split("\\n");
        } else {
            if (result != null) {
                String errMsg = "swiftList failed , err=" + result;
                logger.debug("Failed to list " + errMsg);
            } else {
                String errMsg = "swiftList failed, no lines returns";
                logger.debug("Failed to list " + errMsg);
            }
        }
        return new String[0];
    }

    public static File getObject(SwiftClientCfg cfg, File destDirectory, String swiftPath) {
        int firstIndexOfSeparator = swiftPath.indexOf(File.separator);
        String container = swiftPath.substring(0, firstIndexOfSeparator);
        String srcPath = swiftPath.substring(firstIndexOfSeparator + 1);
        String destFilePath;
        if (destDirectory.isDirectory()) {
            destFilePath = destDirectory.getAbsolutePath() + File.separator + srcPath;
        } else {
            destFilePath = destDirectory.getAbsolutePath();
        }

        Script command = new Script("/bin/bash", logger);
        command.add("-c");
        command.add(getSwiftObjectCmd(cfg, getSwiftCLIPath(), "download", container, srcPath)+" -o " + destFilePath);

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String result = command.execute(parser);
        if (result != null) {
            String errMsg = "swiftDownload failed  err=" + result;
            logger.debug(errMsg);
            throw new CloudRuntimeException("failed to get object: " + swiftPath);
        }
        if (parser.getLines() != null) {
            String[] lines = parser.getLines().split("\\n");
            for (String line : lines) {
                if (line.contains("Errno") || line.contains("failed")) {
                    String errMsg = "swiftDownload failed , err=" + Arrays.toString(lines);
                    logger.debug(errMsg);
                    throw new CloudRuntimeException("Failed to get object: " + swiftPath);
                }
            }
        }
        return new File(destFilePath);
    }


    public static boolean deleteObject(SwiftClientCfg cfg, String path) {
        Script command = new Script("/bin/bash", logger);
        command.add("-c");

        String[] paths = splitSwiftPath(path);
        if (paths == null) {
            return false;
        }
        String container = paths[0];
        String objectName = paths[1];

        command.add(getSwiftObjectCmd(cfg, getSwiftCLIPath(), "delete", container, objectName));

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        command.execute(parser);
        return true;
    }

    public static boolean setTempKey(SwiftClientCfg cfg, String tempKey){

        Map<String, String> tempKeyMap = new HashMap<>();
        tempKeyMap.put("Temp-URL-Key", tempKey);
        return postMeta(cfg, "", "", tempKeyMap);

    }

    public static URL generateTempUrl(SwiftClientCfg cfg, String container, String object, String tempKey, int urlExpirationInterval) {

        int currentTime = (int) (System.currentTimeMillis() / 1000L);
        int expirationSeconds = currentTime + urlExpirationInterval;

        try {

            URL endpoint = new URL(cfg.getEndPoint());
            String method = "GET";
            String path = String.format("/v1/AUTH_%s/%s/%s", cfg.getAccount(), container, object);

            //sign the request
            String hmacBody = String.format("%s\n%d\n%s", method, expirationSeconds, path);
            String signature = calculateRFC2104HMAC(hmacBody, tempKey);
            path += String.format("?temp_url_sig=%s&temp_url_expires=%d", signature, expirationSeconds);

            //generate the temp url
            URL tempUrl = new URL(endpoint.getProtocol(), endpoint.getHost(), endpoint.getPort(), path);

            return tempUrl;

        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new CloudRuntimeException(e.getMessage());
        }

    }

    static String calculateRFC2104HMAC(String data, String key)
            throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {

        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
        mac.init(signingKey);
        return toHexString(mac.doFinal(data.getBytes()));

    }

    static String toHexString(byte[] bytes) {
        return Hex.encodeHexString(bytes);
    }

    /////////////// SWIFT CMD STRING HELPERS ///////////////
    protected static String getSwiftCmd(SwiftClientCfg cfg, String swiftCli, String operation){
        return String.format(SWIFT_CMD, swiftCli,cfg.getEndPoint(),cfg.getAccount(),cfg.getUserName(),cfg.getKey(),operation);
    }

    protected static String getSwiftObjectCmd(SwiftClientCfg cfg, String swiftCliPath, String operation,String container, String objectName) {
        String cmd = getSwiftCmd(cfg,swiftCliPath, operation)  +" "+ container+" "+objectName;
        if(org.apache.commons.lang3.StringUtils.isNotBlank(cfg.getStoragePolicy()) && supportsStoragePolicies(operation)){
            return cmd + String.format(WITH_STORAGE_POLICY, cfg.getStoragePolicy());
        }
        return cmd;
    }

    private static boolean supportsStoragePolicies(String operation) {
        for(String supportedOp: OPERATIONS_WITH_STORAGE_POLICIES){
            if(supportedOp.equals(operation)){
                return true;
            }
        }
        return false;
    }

    protected static String getSwiftContainerCmd(SwiftClientCfg cfg, String swiftCliPath, String operation, String container) {
        return getSwiftCmd(cfg,swiftCliPath, operation) +" "+ container;
    }

    protected static String getUploadObjectCommand(SwiftClientCfg cfg, String swiftCliPath, String container, String objectName, long size) {
        String cmd = getSwiftObjectCmd(cfg, swiftCliPath, "upload", container, objectName);
        if(size > SWIFT_MAX_SIZE){
            return cmd + WITH_SEGMENTS;
        }
        return cmd;
    }

    public static String getContainerName(String type, Long id) {
        if (type.startsWith("T")) {
            return "T-" + id;
        } else if (type.startsWith("S")) {
            return "S-" + id;
        } else if (type.startsWith("V")) {
            return "V-" + id;
        }
        return null;
    }

    public static String[] splitSwiftPath(String path) {
        int index = path.indexOf(File.separator);
        if (index == -1) {
            return null;
        }
        String[] paths = new String[2];
        paths[0] = path.substring(0, index);
        paths[1] = path.substring(index + 1);
        return paths;
    }

    private static String getMeta(Map<String, String> metas) {
        StringBuilder cms = new StringBuilder();
        for (Map.Entry<String, String> entry : metas.entrySet()) {
            cms.append(" -m ");
            cms.append(entry.getKey());
            cms.append(":");
            cms.append(entry.getValue());
            cms.append(" ");
        }
        return cms.toString();
    }
}
