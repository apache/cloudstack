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
package org.apache.cloudstack.utils.qemu;

import com.cloud.agent.api.to.FilesystemInfoTO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.storage.command.browser.ListDataStoreObjectsAnswer;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class was built to use guestfish commands on qcow2 volumes.
 * It should always be instanced with try-with-resources
 * Otherwise, be sure to call close().
 * */

public class GuestfishClient implements AutoCloseable {
    private Logger logger = LogManager.getLogger(getClass());

    private static final int MODE_MASK = 0_170_000;
    private static final int SYMLINK = 0_120_000;
    private static final int DIRECTORY = 0_040_000;
    private static final int FILE = 0_100_000;

    private static final String GUESTFISH = "guestfish";
    private static final String GUESTFISH_PID_STRING = "GUESTFISH_PID=";
    private static final String MODE = "st_mode";
    private static final String SIZE = "st_size";
    private static final String ATIME_SEC = "st_atime_sec";

    // Guestfish commands
    protected static final String MOUNT_RO = "mount-ro";
    protected static final String UMOUNT_ALL = "umount-all";
    protected static final String TAR_OUT = "tar-out";
    protected static final String COMPRESS_OUT = "compress-out";
    protected static final String LIST_FILESYSTEMS = "list-filesystems";
    protected static final String LSTATNS = "lstatns";
    protected static final String LSTATNSLIST = "lstatnslist";
    protected static final String IS_SYMLINK = "is-symlink";
    protected static final String READLINK = "readlink";
    protected static final String BLOCKDEV_GETSIZE_64 = "blockdev-getsize64";
    protected static final String LS = "ls";
    protected static final String EXISTS = "exists";

    private static final int MILI = 1000;

    private final String guestfishPid;
    private final long volumeId;

    /**
     * No-arg constructor for test purposes
     */
    protected GuestfishClient() {
        guestfishPid = "0";
        volumeId = 0;
    }

    public GuestfishClient(String qcow2Path, long volumeId) {
        this.volumeId = volumeId;

        Script script = new Script(GUESTFISH);
        script.add("--listen");
        script.add("--ro");

        OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
        String scriptResult = script.execute(parser);
        if (scriptResult != null) {
            throw new CloudRuntimeException("Could not start guestfish. Is it installed?");
        }

        String output = parser.getLine();

        this.guestfishPid = parseGuestfishPid(output);

        if (guestfishPid == null) {
            throw new CloudRuntimeException("Could not parse guestfish PID.");
        }

        runGuestfishRemoteCommand("add-drive-ro", qcow2Path);
        runGuestfishRemoteCommand("run");
    }

    /**
     * List filesystems. Will not return swap or unknown filesystems.
     */
    public List<FilesystemInfoTO> listFilesystems() {
        String output = runGuestfishRemoteCommand(LIST_FILESYSTEMS);
        List<FilesystemInfoTO> result = new ArrayList<>();

        logger.debug("Got the following output from list-filesystems: [{}]", output);
        output.lines().forEach( line ->{
            line = line.trim();
            if (line.isEmpty()) {
                return;
            }

            String[] parts = line.split(":");
            if (parts.length != 2) {
                return;
            }

            String device = parts[0].trim();
            String type = parts[1].trim();
            if (type.equalsIgnoreCase("swap") || type.equalsIgnoreCase("unknown")) {
                logger.debug("Ignoring filesystem [{}] with [{}] type.", device, type);
                return;
            }

            String size = runGuestfishRemoteCommand(BLOCKDEV_GETSIZE_64, device);

            result.add(new FilesystemInfoTO(device, type, parseLong(size), volumeId));
        });

        return result;
    }

    /**
     * List files in the given directory. If the directory is a symlink, will try to resolve it, if we are unable to resolve it in one hop, we will return an error.
     * */
    public ListDataStoreObjectsAnswer listFiles(String filesystem, String directory, Boolean isSymlink) {
        try {
            mount(filesystem);

            if (isSymlink == null) {
                String isSymlinkString = runGuestfishRemoteCommand(IS_SYMLINK, directory);
                isSymlink = Boolean.parseBoolean(isSymlinkString);
            }

            if (isSymlink) {
                logger.debug("Directory [{}] is a symlink, will try resolve it.", directory);
                directory = (directory.charAt(directory.length() - 1) == '/' ? directory.substring(0, directory.length() - 1) : directory);
                String canonicalPath = getCanonicalPath(directory);
                if (canonicalPath == null) {
                    return new ListDataStoreObjectsAnswer();
                }

                String isSymlinkString = runGuestfishRemoteCommand(IS_SYMLINK, canonicalPath);
                isSymlink = Boolean.parseBoolean(isSymlinkString);
                if (isSymlink) {
                    logger.warn("Directory [{}] is a symlink chain. Unable to list its files. Please try to list the real directory.", directory);
                    return new ListDataStoreObjectsAnswer();
                }
                logger.debug("Directory [{}] was resolved to [{}].", directory, canonicalPath);
                directory = canonicalPath;
            }

            String lsOutput = runGuestfishRemoteCommand(LS, directory);

            List<String> fileList = lsOutput.lines().filter(StringUtils::isNotBlank).collect(Collectors.toList());

            List<String> names = new ArrayList<>();
            List<String> paths = new ArrayList<>();
            List<String> canonicalPaths = new ArrayList<>();
            List<Boolean> isDirs = new ArrayList<>();
            List<Boolean> isSymlinks = new ArrayList<>();
            List<Long> sizes = new ArrayList<>();
            List<Long> modifiedList = new ArrayList<>();

            if (fileList.isEmpty()) {
                return new ListDataStoreObjectsAnswer(true, 0, names, paths, canonicalPaths, isDirs, sizes, modifiedList);
            }

            String details = getDetails(directory, fileList);
            logger.trace("Got the following details for these files [{}]: [{}]", fileList, details);

            addFilesToLists(directory, details, fileList, names, paths, canonicalPaths, isDirs, isSymlinks, sizes, modifiedList);

            return new ListDataStoreObjectsAnswer(true, names.size(), names, paths, canonicalPaths, isDirs, isSymlinks, sizes, modifiedList);
        } finally {
            runGuestfishRemoteCommand(UMOUNT_ALL);
        }
    }

    /**
     * Extracts a file from the given filesystem and path to the destination. If followSymlink is true will try to resolve it, if we cannot do it in one hop we throw an error.
     * */
    public boolean extractFile(String filesystem, String filePath, String destination, boolean followSymlink) {
        mount(filesystem);

        String details = runGuestfishRemoteCommand(LSTATNS, filePath);
        Map<String, String> stat = parseKeyValueOutput(details);
        long mode = parseLong(stat.get(MODE));
        long fileType = mode & MODE_MASK;
        boolean isSymlink = fileType == SYMLINK;

        if (isSymlink && followSymlink) {
            logger.debug("File is a symlink, will try to resolve it in order to download the actual file.");
            String canonicalPath = getCanonicalPath(filePath);
            if (canonicalPath == null) {
                runGuestfishRemoteCommand(UMOUNT_ALL);
                throw new CloudRuntimeException(String.format("File [%s] is a symlink to a non-existent file. Unable to download.", fileType));
            }

            return extractFile(filesystem, canonicalPath, destination, false);
        } else if (isSymlink) {
            runGuestfishRemoteCommand(UMOUNT_ALL);
            throw new CloudRuntimeException(String.format("Unable to extract file at [%s]. It seems like a symlink chain. Try to download the actual file at the end of the chain.",
                    filePath));
        }

        boolean isDirectory = fileType == DIRECTORY;
        boolean isFile = fileType == FILE;
        try {
            if (isDirectory) {
                logger.debug("Extracting directory at [{}] to [{}].", filePath, destination);
                runGuestfishRemoteCommand(TAR_OUT, filePath, destination, "compress:gzip");
            } else if (isFile) {
                logger.debug("Extracting file at [{}] to [{}].", filePath, destination);
                runGuestfishRemoteCommand(COMPRESS_OUT, "gzip", filePath, destination);
            } else {
                throw new CloudRuntimeException(String.format("Unable to extract file at [%s]. It is neither a file nor a directory.", filePath));
            }
        } catch (Exception e) {
            logger.error("Caught exception while extracting file, will try to delete any leftovers and rethrow the exception.", e);
            try {
                Files.deleteIfExists(Path.of(destination));
            } catch (IOException ignored) {
            }
            throw e;
        } finally {
            runGuestfishRemoteCommand(UMOUNT_ALL);
        }
        return isDirectory;
    }

    protected String getDetails(String directory, List<String> fileList) {
        String fileListString = formatFileListForLstatnslist(fileList);

        return runGuestfishRemoteCommand(LSTATNSLIST, directory, fileListString);
    }


    protected String formatFileListForLstatnslist(List<String> fileList) {
        StringBuilder fileListStringBuilder = new StringBuilder();
        for (String string : fileList) {
            fileListStringBuilder.append("'");
            fileListStringBuilder.append(string);
            fileListStringBuilder.append("'");
            fileListStringBuilder.append(" ");
        }
        fileListStringBuilder.deleteCharAt(fileListStringBuilder.length() - 1);
        return fileListStringBuilder.toString();
    }

    /**
     * Given the directory, file list and details of those files, will populate the names, paths, canonicalPaths, isDirs, isSymlinks, sizes and modifiedList lists.
     * If a file is a symlink, will try to resolve it in a single hop, if unable to, we will not add it to the list; furthermore, if the symlink points to an inexistent file, we
     * will not add it to the list either.
     */
    protected void addFilesToLists(String directory, String details, List<String> fileList, List<String> names, List<String> paths, List<String> canonicalPaths,
            List<Boolean> isDirs, List<Boolean> isSymlinks, List<Long> sizes, List<Long> modifiedList) {
        String[] tokens = details.split("=");
        for (int i = 1; i < tokens.length; i++) {
            Map<String, String> stat = parseKeyValueOutput(tokens[i]);

            long mode = parseLong(stat.get(MODE));
            long fileType = mode & MODE_MASK;
            boolean isSymlink = fileType == SYMLINK;

            String path = (directory.charAt(directory.length() - 1) == '/' ? directory : directory + '/') + fileList.get(i - 1);
            String canonicalPath = path;
            if (isSymlink) {
                logger.debug("File [{}] is a symlink, will try to resolve it and add it to the list of files of the backup.", path);
                canonicalPath = getCanonicalPath(path);
                if (canonicalPath == null) {
                    continue;
                }

                String fileDetails = runGuestfishRemoteCommand(LSTATNS, canonicalPath);
                stat = parseKeyValueOutput(fileDetails);
                mode = parseLong(stat.get(MODE));

                fileType = mode & MODE_MASK;
                if (fileType == SYMLINK) {
                    logger.warn("File [{}] is a symlink chain, will not return it on the list of files of the backup.", path);
                    continue;
                } else {
                    logger.debug("Symlink at [{}] was resolved to [{}] in the list of files of the backup. Setting it as its canonical path", path, canonicalPath);
                }
            }
            names.add(fileList.get(i - 1));
            paths.add(path);
            canonicalPaths.add(canonicalPath);

            boolean isDirectory = fileType == DIRECTORY;
            isDirs.add(isDirectory);
            isSymlinks.add(isSymlink);

            long size = parseLong(stat.get(SIZE));
            sizes.add(size);
            long mtime = parseLong(stat.get(ATIME_SEC));
            modifiedList.add(mtime * MILI);
        }
    }

    /**
     * @param filePath path of the symlink to try and get the canonical path from. If it points to an unexisting file, we return null. This method expects the filePath to be a
     *                symlink and will throw an error if it is not.
     * @return The canonical path, if the file exists; null otherwise.
     */
    protected String getCanonicalPath(String filePath) {
        String canonicalPath = runGuestfishRemoteCommand(READLINK, filePath);
        canonicalPath = canonicalPath.replace("/sysroot", "");
        if (canonicalPath.charAt(0) != '/') {
            String basepath = filePath.substring(0, filePath.lastIndexOf('/'));
            canonicalPath = Paths.get(basepath + "/" + canonicalPath).normalize().toString();
        }

        if (!Boolean.parseBoolean(runGuestfishRemoteCommand(EXISTS, canonicalPath))) {
            logger.warn("Symlink [{}] points to a file that does not exist.", filePath);
            return null;
        }

        return canonicalPath;
    }

    protected void mount(String filesystem) {
        runGuestfishRemoteCommand(UMOUNT_ALL);
        runGuestfishRemoteCommand(MOUNT_RO, filesystem, "/");
    }

    /**
     * Execute remote guestfish command
     */
    protected String runGuestfishRemoteCommand(String... commandParts) {
        Script script = new Script(GUESTFISH);
        script.add("--remote=" + guestfishPid);
        script.add("--");
        script.add(commandParts);

        OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
        String scriptResult = script.execute(parser);
        if (scriptResult != null) {
            throw new CloudRuntimeException(String.format("Got unexpected output when trying to run guestfish command [%s].", script));
        }

        String result = parser.getLines();
        logger.trace("Result from guestfish command [{}] is [{}].", script.toString(), result);
        return result.trim();
    }

    protected String parseGuestfishPid(String output) {
        for (String token : output.split(";")) {
            token = token.trim();

            if (token.startsWith(GUESTFISH_PID_STRING)) {
                return token.substring(GUESTFISH_PID_STRING.length());
            }
        }
        return null;
    }

    protected Map<String, String> parseKeyValueOutput(String output) {
        Map<String, String> map = new HashMap<>();

        output.lines().forEach(line -> {
            line = line.trim();
            if (line.isEmpty()) {
                return;
            }

            int index = line.indexOf(':');
            if (index < 0) {
                return;
            }

            String key = line.substring(0, index).trim();
            String value = line.substring(index + 1).trim();
            map.put(key, value);
        });

        return map;
    }

    protected long parseLong(String value) {
        if (StringUtils.isEmpty(value)) {
            return 0;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            logger.warn("Unable to parse long [{}]. Returning 0.", value);
            return 0;
        }
    }


    @Override
    public void close() {
        runGuestfishRemoteCommand("exit");
    }
}
