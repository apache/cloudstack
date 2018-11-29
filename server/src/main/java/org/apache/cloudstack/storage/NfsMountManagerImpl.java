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
package org.apache.cloudstack.storage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PreDestroy;

import com.cloud.storage.StorageLayer;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class NfsMountManagerImpl implements NfsMountManager {
    private static final Logger s_logger = Logger.getLogger(NfsMountManager.class);

    private StorageLayer storage;
    private int timeout;
    private final Random rand = new Random(System.currentTimeMillis());
    private final ConcurrentMap<String, String> storageMounts = new ConcurrentHashMap<>();

    public static final ConfigKey<String> MOUNT_PARENT = new ConfigKey<>("Advanced", String.class,
            "mount.parent", "/var/cloudstack/mnt",
            "The mount point on the Management Server for Secondary Storage.",
            true, ConfigKey.Scope.Global);

    public NfsMountManagerImpl(StorageLayer storage, int timeout) {
        this.storage = storage;
        this.timeout = timeout;
    }

    public String getMountPoint(String storageUrl, Integer nfsVersion) {
        String mountPoint = storageMounts.get(storageUrl);
        if (mountPoint != null) {
            return mountPoint;
        }

        URI uri;
        try {
            uri = new URI(storageUrl);
        } catch (URISyntaxException e) {
            s_logger.error("Invalid storage URL format ", e);
            throw new CloudRuntimeException("Unable to create mount point due to invalid storage URL format " + storageUrl);
        }

        mountPoint = mount(uri.getHost() + ":" + uri.getPath(), MOUNT_PARENT.value(), nfsVersion);
        if (mountPoint == null) {
            s_logger.error("Unable to create mount point for " + storageUrl);
            throw new CloudRuntimeException("Unable to create mount point for " + storageUrl);
        }

        storageMounts.putIfAbsent(storageUrl, mountPoint);
        return mountPoint;
    }

    private String mount(String path, String parent, Integer nfsVersion) {
        String mountPoint = setupMountPoint(parent);
        if (mountPoint == null) {
            s_logger.warn("Unable to create a mount point");
            return null;
        }

        Script command = new Script(true, "mount", timeout, s_logger);
        command.add("-t", "nfs");
        if (nfsVersion != null){
            command.add("-o", "vers=" + nfsVersion);
        }
        // command.add("-o", "soft,timeo=133,retrans=2147483647,tcp,acdirmax=0,acdirmin=0");
        if ("Mac OS X".equalsIgnoreCase(System.getProperty("os.name"))) {
            command.add("-o", "resvport");
        }
        command.add(path);
        command.add(mountPoint);
        String result = command.execute();
        if (result != null) {
            s_logger.warn("Unable to mount " + path + " due to " + result);
            deleteMountPath(mountPoint);
            return null;
        }

        // Change permissions for the mountpoint
        Script script = new Script(true, "chmod", timeout, s_logger);
        script.add("1777", mountPoint);
        result = script.execute();
        if (result != null) {
            s_logger.warn("Unable to set permissions for " + mountPoint + " due to " + result);
        }
        return mountPoint;
    }

    private String setupMountPoint(String parent) {
        String mountPoint = null;
        for (int i = 0; i < 10; i++) {
            String mntPt = parent + File.separator + String.valueOf(ManagementServerNode.getManagementServerId()) + "." + Integer.toHexString(rand.nextInt(Integer.MAX_VALUE));
            File file = new File(mntPt);
            if (!file.exists()) {
                if (storage.mkdir(mntPt)) {
                    mountPoint = mntPt;
                    break;
                }
            }
            s_logger.error("Unable to create mount: " + mntPt);
        }

        return mountPoint;
    }

    private void umount(String localRootPath) {
        if (!mountExists(localRootPath)) {
            return;
        }
        Script command = new Script(true, "umount", timeout, s_logger);
        command.add(localRootPath);
        String result = command.execute();
        if (result != null) {
            // Fedora Core 12 errors out with any -o option executed from java
            String errMsg = "Unable to umount " + localRootPath + " due to " + result;
            s_logger.error(errMsg);
            throw new CloudRuntimeException(errMsg);
        }
        deleteMountPath(localRootPath);
        s_logger.debug("Successfully umounted " + localRootPath);
    }

    private void deleteMountPath(String localRootPath) {
        try {
            Files.deleteIfExists(Paths.get(localRootPath));
        } catch (IOException e) {
            s_logger.warn(String.format("unable to delete mount directory %s:%s.%n", localRootPath, e.getMessage()));
        }
    }

    private boolean mountExists(String localRootPath) {
        Script script = new Script(true, "mount", timeout, s_logger);
        ZfsPathParser parser = new ZfsPathParser(localRootPath);
        script.execute(parser);
        return parser.getPaths().stream().filter(s -> s.contains(localRootPath)).findAny().map(s -> true).orElse(false);
    }

    public static class ZfsPathParser extends OutputInterpreter {
        String _parent;
        List<String> paths = new ArrayList<>();

        public ZfsPathParser(String parent) {
            _parent = parent;
        }

        @Override
        public String interpret(BufferedReader reader) throws IOException {
            String line;
            while ((line = reader.readLine()) != null) {
                paths.add(line);
            }
            return null;
        }

        public List<String> getPaths() {
            return paths;
        }

        @Override
        public boolean drain() {
            return true;
        }
    }

    @PreDestroy
    public void destroy() {
        s_logger.info("Clean up mounted NFS mount points used in current session.");
        storageMounts.values().stream().forEach(this::umount);
    }
}
