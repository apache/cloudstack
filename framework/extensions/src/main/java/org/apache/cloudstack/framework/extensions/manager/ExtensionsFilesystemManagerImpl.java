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

package org.apache.cloudstack.framework.extensions.manager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import javax.naming.ConfigurationException;

import org.apache.cloudstack.extension.Extension;
import org.apache.cloudstack.utils.security.DigestHelper;
import org.apache.cloudstack.utils.server.ServerPropertiesUtil;
import org.apache.commons.collections.CollectionUtils;

import com.cloud.serializer.GsonHelper;
import com.cloud.utils.FileUtil;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class ExtensionsFilesystemManagerImpl extends ManagerBase implements ExtensionsFilesystemManager {

    public static final Map<Extension.Type, String> BASE_EXTERNAL_SCRIPTS =
            Map.of(Extension.Type.Orchestrator, "scripts/vm/hypervisor/external/provisioner/provisioner.sh");

    private static final String EXTENSIONS = "extensions";
    private static final String EXTENSIONS_DEPLOYMENT_MODE_NAME = "extensions.deployment.mode";
    private static final String EXTENSIONS_DIRECTORY_PROD = "/usr/share/cloudstack-management/extensions";
    private static final String EXTENSIONS_DATA_DIRECTORY_PROD = System.getProperty("user.home") + File.separator + EXTENSIONS;
    private static final String EXTENSIONS_DIRECTORY_DEV = EXTENSIONS;
    private static final String EXTENSIONS_DATA_DIRECTORY_DEV = "client/target/extensions-data";

    private String extensionsDirectory;
    private String extensionsDataDirectory;
    private ExecutorService payloadCleanupExecutor;
    private ScheduledExecutorService payloadCleanupScheduler;

    private void initializeExtensionDirectories() {
        String deploymentMode = ServerPropertiesUtil.getProperty(EXTENSIONS_DEPLOYMENT_MODE_NAME);
        if ("developer".equals(deploymentMode)) {
            extensionsDirectory = EXTENSIONS_DIRECTORY_DEV;
            extensionsDataDirectory = EXTENSIONS_DATA_DIRECTORY_DEV;
        } else {
            extensionsDirectory = EXTENSIONS_DIRECTORY_PROD;
            extensionsDataDirectory = EXTENSIONS_DATA_DIRECTORY_PROD;
        }
    }

    protected boolean checkExtensionsDirectory() {
        File dir = new File(extensionsDirectory);
        if (!dir.exists() || !dir.isDirectory() || !dir.canWrite()) {
            logger.error("Extension directory [{}] is not properly set up. It must exist, be a directory, and be writeable",
                    dir.getAbsolutePath());
            return false;
        }
        if (!extensionsDirectory.equals(dir.getAbsolutePath())) {
            extensionsDirectory = dir.getAbsolutePath();
        }
        logger.info("Extensions directory path: {}", extensionsDirectory);
        return true;
    }

    protected void createOrCheckExtensionsDataDirectory() throws ConfigurationException {
        File dir = new File(extensionsDataDirectory);
        if (!dir.exists()) {
            try {
                Files.createDirectories(dir.toPath());
            } catch (IOException e) {
                logger.error("Unable to create extensions data directory [{}]", dir.getAbsolutePath(), e);
                throw new ConfigurationException("Unable to create extensions data directory path");
            }
        }
        if (!dir.isDirectory() || !dir.canWrite()) {
            logger.error("Extensions data directory [{}] is not properly set up. It must exist, be a directory, and be writeable",
                    dir.getAbsolutePath());
            throw new ConfigurationException("Extensions data directory path is not accessible");
        }
        extensionsDataDirectory = dir.getAbsolutePath();
        logger.info("Extensions data directory path: {}", extensionsDataDirectory);
    }

    protected void scheduleExtensionPayloadDirectoryCleanup(String extensionName) {
        try {
            Future<?> future = payloadCleanupExecutor.submit(() -> {
                try {
                    cleanupExtensionData(extensionName, 1, false);
                    logger.trace("Cleaned up payload directory for extension: {}", extensionName);
                } catch (Exception e) {
                    logger.warn("Exception during payload cleanup for extension: {} due to {}", extensionName,
                            e.getMessage());
                    logger.trace(e);
                }
            });
            payloadCleanupScheduler.schedule(() -> {
                try {
                    if (!future.isDone()) {
                        future.cancel(true);
                        logger.trace("Cancelled cleaning up payload directory for extension: {} as it " +
                                "running for more than 3 seconds", extensionName);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to cancel payload cleanup task for extension: {} due to {}",
                            extensionName, e.getMessage());
                    logger.trace(e);
                }
            }, 3, TimeUnit.SECONDS);
        } catch (RejectedExecutionException e) {
            logger.warn("Payload cleanup task for extension: {} was rejected due to: {}", extensionName,
                    e.getMessage());
            logger.trace(e);
        }
    }

    protected static String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        return (lastDot == -1) ? "" : name.substring(lastDot + 1);
    }

    protected Path getExtensionRootPath(String extensionName) {
        final String normalizedName = Extension.getDirectoryName(extensionName);
        final String extensionDir = extensionsDirectory + File.separator + normalizedName;
        return Path.of(extensionDir);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        initializeExtensionDirectories();
        checkExtensionsDirectory();
        createOrCheckExtensionsDataDirectory();
        return true;
    }

    @Override
    public boolean start() {
        payloadCleanupExecutor = Executors.newSingleThreadExecutor();
        payloadCleanupScheduler = Executors.newSingleThreadScheduledExecutor();
        return true;
    }

    @Override
    public boolean stop() {
        payloadCleanupExecutor.shutdown();
        payloadCleanupScheduler.shutdown();
        return true;
    }

    @Override
    public String getExtensionsPath() {
        return extensionsDirectory;
    }

    @Override
    public Path getExtensionRootPath(Extension extension) {
        return getExtensionRootPath(extension.getName());
    }

    @Override
    public String getExtensionPath(String relativePath) {
        return String.format("%s%s%s", extensionsDirectory, File.separator, relativePath);
    }

    @Override
    public String getExtensionCheckedPath(String extensionName, String extensionRelativePath) {
        String path = getExtensionPath(extensionRelativePath);
        File file = new File(path);
        String errorSuffix = String.format("Entry point [%s] for extension: %s", path, extensionName);
        if (!file.exists()) {
            logger.error("{} does not exist", errorSuffix);
            return null;
        }
        if (!file.isFile()) {
            logger.error("{} is not a file", errorSuffix);
            return null;
        }
        if (!file.canRead()) {
            logger.error("{} is not readable", errorSuffix);
            return null;
        }
        if (!file.canExecute()) {
            logger.error("{} is not executable", errorSuffix);
            return null;
        }
        return path;
    }

    @Override
    public Map<String, String> getChecksumMapForExtension(String extensionName, String relativePath) {
        String path = getExtensionCheckedPath(extensionName, relativePath);
        if (StringUtils.isBlank(path)) {
            return null;
        }
        try {
            Path rootPath = getExtensionRootPath(extensionName);
            Map<String, String> fileChecksums = new TreeMap<>();
            java.util.List<Path> files = new java.util.ArrayList<>();
            try (Stream<Path> stream = Files.walk(rootPath)) {
                stream.filter(Files::isRegularFile).forEach(files::add);
            }
            files.sort(Comparator.naturalOrder());
            for (Path filePath : files) {
                String relative = rootPath.relativize(filePath).toString().replace(File.separatorChar, '/');
                String fileChecksum = DigestHelper.calculateChecksum(filePath.toFile());
                fileChecksums.put(relative, fileChecksum);
            }
            if (logger.isTraceEnabled()) {
                String json = GsonHelper.getGson().toJson(fileChecksums);
                logger.trace("Calculated individual file checksums for extension: {}: {}", extensionName, json);
            }
            return fileChecksums;
        } catch (IOException | CloudRuntimeException e) {
            return null;
        }
    }

    @Override
    public void prepareExtensionPath(String extensionName, boolean userDefined, Extension.Type type, String extensionRelativePath) {
        logger.debug("Preparing entry point for Extension [name: {}, user-defined: {}]", extensionName, userDefined);
        if (!userDefined) {
            logger.debug("Skipping preparing entry point for inbuilt extension: {}", extensionName);
            return;
        }
        CloudRuntimeException exception =
                new CloudRuntimeException(String.format("Failed to prepare scripts for extension: %s", extensionName));
        String sourceScriptPath = Script.findScript("", BASE_EXTERNAL_SCRIPTS.get(type));
        if(sourceScriptPath == null) {
            logger.debug("Base script is not available for preparing extension: {} of type: {}",
                    extensionName, type);
            return;
        }
        String destinationPath = getExtensionPath(extensionRelativePath);
        File destinationFile = new File(destinationPath);
        File sourceFile = new File(sourceScriptPath);
        if (!getFileExtension(sourceFile).equalsIgnoreCase(getFileExtension(destinationFile))) {
            logger.error("Extension file type do not match with base file for extension: {} of type: {}",
                    extensionName, type);
           return;
        }
        if (destinationFile.exists()) {
            logger.info("File already exists at {} for extension: {}, skipping copy.", destinationPath,
                    extensionName);
            return;
        }
        if (!checkExtensionsDirectory()) {
            throw exception;
        }
        Path destinationPathObj = Paths.get(destinationPath);
        Path destinationDirPath = destinationPathObj.getParent();
        if (destinationDirPath == null) {
            logger.error("Failed to find parent directory for extension: {} script path {}",
                    extensionName, destinationPath);
            throw exception;
        }
        try {
            Files.createDirectories(destinationDirPath);
        } catch (IOException e) {
            logger.error("Failed to create directory: {} for extension: {}", destinationDirPath,
                    extensionName, e);
            throw exception;
        }
        try {
            Path sourcePath = Paths.get(sourceScriptPath);
            Files.copy(sourcePath, destinationPathObj, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            logger.error("Failed to copy entry point file to [{}] for extension: {}",
                    destinationPath, extensionName, e);
            throw exception;
        }
        logger.debug("Successfully prepared entry point [{}] for extension: {}", destinationPath,
                extensionName);
    }

    @Override
    public void cleanupExtensionPath(String extensionName, String extensionRelativePath) {
        String normalizedPath = extensionRelativePath;
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        try {
            Path rootPath = Paths.get(extensionsDirectory).toAbsolutePath().normalize();
            String extensionDirName = Extension.getDirectoryName(extensionName);
            Path filePath = rootPath
                    .resolve(normalizedPath.startsWith(extensionDirName) ? extensionDirName : normalizedPath)
                    .normalize();
            if (!Files.exists(filePath)) {
                return;
            }
            if (!Files.isDirectory(filePath) && !Files.isRegularFile(filePath)) {
                throw new CloudRuntimeException(
                        String.format("Failed to cleanup path: %s for extension: %s as it either " +
                                        "does not exist or is not a regular file/directory",
                                extensionName, extensionRelativePath));
            }
            if (!FileUtil.deleteRecursively(filePath)) {
                throw new CloudRuntimeException(
                        String.format("Failed to delete path: %s for extension: %s",
                                extensionName, filePath));
            }
        } catch (IOException e) {
            throw new CloudRuntimeException(
                    String.format("Failed to cleanup path: %s for extension: %s due to: %s",
                            extensionName, normalizedPath, e.getMessage()), e);
        }
    }

    @Override
    public void cleanupExtensionData(String extensionName, int olderThanDays, boolean cleanupDirectory) {
        String extensionPayloadDirPath = extensionsDataDirectory + File.separator + extensionName;
        Path dirPath = Paths.get(extensionPayloadDirPath);
        if (!Files.exists(dirPath)) {
            return;
        }
        try {
            if (cleanupDirectory) {
                try (Stream<Path> paths = Files.walk(dirPath)) {
                    paths.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
                return;
            }
            long cutoffMillis = System.currentTimeMillis() - (olderThanDays * 24L * 60 * 60 * 1000);
            long lastModified = Files.getLastModifiedTime(dirPath).toMillis();
            if (lastModified < cutoffMillis) {
                return;
            }
            try (Stream<Path> paths = Files.walk(dirPath)) {
                paths.filter(path -> !path.equals(dirPath))
                        .filter(path -> {
                            try {
                                return Files.getLastModifiedTime(path).toMillis() < cutoffMillis;
                            } catch (IOException e) {
                                return false;
                            }
                        })
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException e) {
            logger.warn("Failed to clean up extension payloads for {}: {}", extensionName, e.getMessage());
        }
    }

    @Override
    public Path getExtensionsStagingPath() throws IOException {
        Path extensionsPath = Paths.get(extensionsDirectory).toAbsolutePath().normalize();
        Path stagingPath = extensionsPath.resolve(".staging");
        Files.createDirectories(stagingPath);
        return stagingPath;
    }

    @Override
    public String prepareExternalPayload(String extensionName, Map<String, Object> details) throws IOException {
        String json = GsonHelper.getGson().toJson(details);
        String fileName = UUID.randomUUID() + ".json";
        String extensionPayloadDir = extensionsDataDirectory + File.separator + extensionName;
        Path payloadDirPath = Paths.get(extensionPayloadDir);
        if (!Files.exists(payloadDirPath)) {
            Files.createDirectories(payloadDirPath);
        } else {
            scheduleExtensionPayloadDirectoryCleanup(extensionName);
        }
        Path payloadFile = payloadDirPath.resolve(fileName);
        Files.writeString(payloadFile, json, StandardOpenOption.CREATE_NEW);
        return payloadFile.toAbsolutePath().toString();
    }

    @Override
    public void deleteExtensionPayload(String extensionName, String payloadFileName) {
        logger.trace("Deleting payload file: {} for extension: {}", payloadFileName, extensionName);
        FileUtil.deletePath(payloadFileName);
    }

    @Override
    public void validateExtensionFiles(Extension extension, List<String> files) {
        if (CollectionUtils.isEmpty(files)) {
            return;
        }
        Path rootPath = getExtensionRootPath(extension);
        File rootDir = rootPath.toFile();
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            throw new CloudRuntimeException("Extension directory does not exist: " + rootPath);
        }
        for (String filePath : files) {
            File file = new File(filePath);
            if (!file.isAbsolute()) {
                file = new File(rootDir, filePath);
            }
            if (!file.exists()) {
                throw new CloudRuntimeException("File does not exist: " + filePath);
            }
        }
    }
}
