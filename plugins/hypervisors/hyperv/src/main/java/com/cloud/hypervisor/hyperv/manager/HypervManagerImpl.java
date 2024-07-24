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
package com.cloud.hypervisor.hyperv.manager;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.utils.identity.ManagementServerNode;
import org.apache.log4j.Logger;

import com.cloud.configuration.Config;
import com.cloud.storage.JavaStorageLayer;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.FileUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;

public class HypervManagerImpl implements HypervManager {
    public static final Logger s_logger = Logger.getLogger(HypervManagerImpl.class);

    private String name;
    private int runLevel;
    private Map<String, Object> params;

    private int _timeout;
    Random _rand = new Random(System.currentTimeMillis());

    Map<String, String> _storageMounts = new HashMap<String, String>();
    StorageLayer _storage;

    @Inject ConfigurationDao _configDao;
    @Inject DataStoreManager _dataStoreMgr;
    @Inject VMInstanceDao _vminstanceDao;
    @Inject NicDao _nicDao;
    int _routerExtraPublicNics = 2;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if (params != null) {
            String value = (String)params.get("scripts.timeout");
            _timeout = NumbersUtil.parseInt(value, 30) * 1000;
            _storage = (StorageLayer)params.get(StorageLayer.InstanceConfigKey);
        }

        if (_storage == null) {
            _storage = new JavaStorageLayer();
            _storage.configure("StorageLayer", params);
        }
        _routerExtraPublicNics = NumbersUtil.parseInt(_configDao.getValue(Config.RouterExtraPublicNics.key()), 2);
        return true;
    }

    @Override
    public boolean start() {
        startupCleanup(getMountParent());
        return true;
    }

    @Override
    public boolean stop() {
        shutdownCleanup();
        return true;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public Map<String, Object> getConfigParams() {
        return params;
    }

    @Override
    public int getRunLevel() {
        return runLevel;
    }

    @Override
    public void setRunLevel(int level) {
        runLevel = level;
    }

    @Override
    public String prepareSecondaryStorageStore(long zoneId) {
        String secondaryStorageUri = getSecondaryStorageStoreUrl(zoneId);
        if (secondaryStorageUri == null) {
            s_logger.debug("Secondary storage uri for dc " + zoneId + " couldn't be obtained");
        } else {
            prepareSecondaryStorageStore(secondaryStorageUri);
        }

        return secondaryStorageUri;
    }

    private String getSecondaryStorageStoreUrl(long zoneId) {
        String secUrl = null;
        DataStore secStore = _dataStoreMgr.getImageStoreWithFreeCapacity(zoneId);
        if (secStore != null) {
            secUrl = secStore.getUri();
        }

        if (secUrl == null) {
            s_logger.warn("Secondary storage uri couldn't be retrieved");
        }

        return secUrl;
    }

    private void prepareSecondaryStorageStore(String storageUrl) {
        String mountPoint = getMountPoint(storageUrl);

        GlobalLock lock = GlobalLock.getInternLock("prepare.systemvm");
        try {
            if (lock.lock(3600)) {
                try {
                    File patchFolder = new File(mountPoint + "/systemvm");
                    if (!patchFolder.exists()) {
                        if (!patchFolder.mkdirs()) {
                            String msg = "Unable to create systemvm folder on secondary storage. location: " + patchFolder.toString();
                            s_logger.error(msg);
                            throw new CloudRuntimeException(msg);
                        }
                    }

                    File srcIso = getSystemVMPatchIsoFile();
                    File destIso = new File(mountPoint + "/systemvm/" + getSystemVMIsoFileNameOnDatastore());
                    if (!destIso.exists()) {
                        s_logger.info("Copy System VM patch ISO file to secondary storage. source ISO: " +
                            srcIso.getAbsolutePath() + ", destination: " + destIso.getAbsolutePath());
                        try {
                            FileUtil.copyfile(srcIso, destIso);
                        } catch (IOException e) {
                            s_logger.error("Unexpected exception ", e);

                            String msg = "Unable to copy systemvm ISO on secondary storage. src location: " + srcIso.toString() + ", dest location: " + destIso;
                            s_logger.error(msg);
                            throw new CloudRuntimeException(msg);
                        }
                    } else {
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("SystemVM ISO file " + destIso.getPath() + " already exists");
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        } finally {
            lock.releaseRef();
        }
    }

    private String getMountPoint(String storageUrl) {
        String mountPoint = null;
        synchronized (_storageMounts) {
            mountPoint = _storageMounts.get(storageUrl);
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

            mountPoint = mount(File.separator + File.separator + uri.getHost() + uri.getPath(), getMountParent(),
                uri.getScheme(), uri.getQuery());
            if (mountPoint == null) {
                s_logger.error("Unable to create mount point for " + storageUrl);
                return "/mnt/sec";
            }

            _storageMounts.put(storageUrl, mountPoint);
            return mountPoint;
        }
    }

    protected String mount(String path, String parent, String scheme, String query) {
        String mountPoint = setupMountPoint(parent);
        if (mountPoint == null) {
            s_logger.warn("Unable to create a mount point");
            return null;
        }

        Script script = null;
        String result = null;
        if (scheme.equals("cifs")) {
            String user = System.getProperty("user.name");
            Script command = new Script(true, "mount", _timeout, s_logger);
            command.add("-t", "cifs");
            command.add(path);
            command.add(mountPoint);

            if (user != null) {
                command.add("-o", "uid=" + user + ",gid=" + user);
            }

            if (query != null) {
                query = query.replace('&', ',');
                command.add("-o", query);
            }

            result = command.execute();
        }

        if (result != null) {
            s_logger.warn("Unable to mount " + path + " due to " + result);
            File file = new File(mountPoint);
            if (file.exists()) {
                file.delete();
            }
            return null;
        }

        // Change permissions for the mountpoint
        script = new Script(true, "chmod", _timeout, s_logger);
        script.add("-R", "777", mountPoint);
        result = script.execute();
        if (result != null) {
            s_logger.warn("Unable to set permissions for " + mountPoint + " due to " + result);
        }
        return mountPoint;
    }

    private String setupMountPoint(String parent) {
        String mountPoint = null;
        long mshostId = ManagementServerNode.getManagementServerId();
        for (int i = 0; i < 10; i++) {
            String mntPt = parent + File.separator + String.valueOf(mshostId) + "." + Integer.toHexString(_rand.nextInt(Integer.MAX_VALUE));
            File file = new File(mntPt);
            if (!file.exists()) {
                if (_storage.mkdir(mntPt)) {
                    mountPoint = mntPt;
                    break;
                }
            }
            s_logger.error("Unable to create mount: " + mntPt);
        }

        return mountPoint;
    }

    private String getSystemVMIsoFileNameOnDatastore() {
        String version = this.getClass().getPackage().getImplementationVersion();
        String fileName = "systemvm-" + version + ".iso";
        return fileName.replace(':', '-');
    }

    private File getSystemVMPatchIsoFile() {
        // locate systemvm.iso
        URL url = this.getClass().getClassLoader().getResource("vms/systemvm.iso");
        File isoFile = null;
        if (url != null) {
            isoFile = new File(url.getPath());
        }

        if (isoFile == null || !isoFile.exists()) {
            isoFile = new File("/usr/share/cloudstack-common/vms/systemvm.iso");
        }

        assert (isoFile != null);
        if (!isoFile.exists()) {
            s_logger.error("Unable to locate systemvm.iso in your setup at " + isoFile.toString());
        }
        return isoFile;
    }

    private String getMountParent() {
        String mountParent = _configDao.getValue(Config.MountParent.key());
        if (mountParent == null) {
            mountParent = File.separator + "mnt";
        }

        String instance = _configDao.getValue(Config.InstanceName.key());
        if (instance == null) {
            instance = "DEFAULT";
        }

        if (instance != null) {
            mountParent = mountParent + File.separator + instance;
        }

        return mountParent;
    }

    private void startupCleanup(String parent) {
        s_logger.info("Cleanup mounted mount points used in previous session");

        long mshostId = ManagementServerNode.getManagementServerId();

        // cleanup left-over NFS mounts from previous session
        String[] mounts = _storage.listFiles(parent + File.separator + String.valueOf(mshostId) + ".*");
        if (mounts != null && mounts.length > 0) {
            for (String mountPoint : mounts) {
                s_logger.info("umount NFS mount from previous session: " + mountPoint);

                String result = null;
                Script command = new Script(true, "umount", _timeout, s_logger);
                command.add(mountPoint);
                result = command.execute();
                if (result != null) {
                    s_logger.warn("Unable to umount " + mountPoint + " due to " + result);
                }
                File file = new File(mountPoint);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
    }

    private void shutdownCleanup() {
        s_logger.info("Cleanup mounted mount points used in current session");
        synchronized (_storageMounts) {
             for (String mountPoint : _storageMounts.values()) {
                s_logger.info("umount NFS mount: " + mountPoint);

                String result = null;
                Script command = new Script(true, "umount", _timeout, s_logger);
                command.add(mountPoint);
                result = command.execute();
                if (result != null) {
                    s_logger.warn("Unable to umount " + mountPoint + " due to " + result);
                }
                File file = new File(mountPoint);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
    }

    @Override
    public int getRouterExtraPublicNics() {
        return _routerExtraPublicNics;
    }
}
