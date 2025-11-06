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
package com.cloud.hypervisor.kvm.storage;

import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.storage.formatinspector.Qcow2Inspector;
import org.apache.cloudstack.utils.imagestore.ImageStoreUtil;
import org.apache.cloudstack.utils.qemu.QemuImg;
import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.cloudstack.utils.qemu.QemuImgException;
import org.apache.cloudstack.utils.qemu.QemuImgFile;
import org.apache.cloudstack.utils.qemu.QemuObject;
import org.apache.cloudstack.utils.reflectiontostringbuilderutils.ReflectionToStringBuilderUtils;
import org.apache.commons.lang3.StringUtils;
import org.libvirt.LibvirtException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class KVMPhysicalDisk {
    private String path;
    private final String name;
    private final KVMStoragePool pool;
    private String dispName;
    private String vmName;
    private boolean useAsTemplate;

    public static final String RBD_DEFAULT_DATA_POOL = "rbd_default_data_pool";

    public static String RBDStringBuilder(KVMStoragePool storagePool, String image) {
        String monHost = storagePool.getSourceHost();
        int monPort = storagePool.getSourcePort();
        String authUserName = storagePool.getAuthUserName();
        String authSecret = storagePool.getAuthSecret();
        Map<String, String> details = storagePool.getDetails();
        String dataPool = (details == null) ? null : details.get(RBD_DEFAULT_DATA_POOL);

        String rbdOpts = "rbd:" + image;
        rbdOpts += ":mon_host=" + composeOptionForMonHosts(monHost, monPort);

        if (authUserName == null) {
            rbdOpts += ":auth_supported=none";
        } else {
            rbdOpts += ":auth_supported=cephx";
            rbdOpts += ":id=" + authUserName;
            rbdOpts += ":key=" + authSecret;
        }

        if (dataPool != null) {
            rbdOpts += String.format(":rbd_default_data_pool=%s", dataPool);
        }

        rbdOpts += ":rbd_default_format=2";
        rbdOpts += ":client_mount_timeout=30";

        return rbdOpts;
    }

    private static String composeOptionForMonHosts(String monHost, int monPort) {
        List<String> hosts = new ArrayList<>();
        for (String host : monHost.split(",")) {
            if (monPort > 0) {
                hosts.add(replaceHostAddress(host) + "\\:" + monPort);
            } else {
                hosts.add(replaceHostAddress(host));
            }
        }
        return StringUtils.join(hosts, "\\;");
    }

    private static String replaceHostAddress(String hostIp) {
        if (hostIp != null && hostIp.startsWith("[") && hostIp.endsWith("]")) {
            return hostIp.replaceAll("\\:", "\\\\:");
        }
        return hostIp;
    }

    public static long getVirtualSizeFromFile(String path) {
        try {
            QemuImg qemu = new QemuImg(0);
            QemuImgFile qemuFile = new QemuImgFile(path);
            Map<String, String> info = qemu.info(qemuFile);
            if (info.containsKey(QemuImg.VIRTUAL_SIZE)) {
                return Long.parseLong(info.get(QemuImg.VIRTUAL_SIZE));
            } else {
                throw new CloudRuntimeException("Unable to determine virtual size of volume at path " + path);
            }
        } catch (QemuImgException | LibvirtException ex) {
            throw new CloudRuntimeException("Error when inspecting volume at path " + path, ex);
        }
    }

    public static void checkQcow2File(String path) {
        if (ImageStoreUtil.isCorrectExtension(path, "qcow2")) {
            try {
                Qcow2Inspector.validateQcow2File(path);
            } catch (RuntimeException e) {
                throw new CloudRuntimeException("The volume file at path " + path + " is not a valid QCOW2. Error: " + e.getMessage());
            }
        }
    }

    private PhysicalDiskFormat format;
    private long size;
    private long virtualSize;
    private QemuObject.EncryptFormat qemuEncryptFormat;

    public KVMPhysicalDisk(String path, String name, KVMStoragePool pool) {
        this.path = path;
        this.name = name;
        this.pool = pool;
    }

    @Override
    public String toString() {
        return String.format("KVMPhysicalDisk %s",
                ReflectionToStringBuilderUtils.reflectOnlySelectedFields(
                        this, "path", "name", "pool", "format", "size", "virtualSize", "dispName", "vmName"));
    }

    public void setFormat(PhysicalDiskFormat format) {
        this.format = format;
    }

    public PhysicalDiskFormat getFormat() {
        return this.format;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getSize() {
        return this.size;
    }

    public void setVirtualSize(long size) {
        this.virtualSize = size;
    }

    public long getVirtualSize() {
        return this.virtualSize;
    }

    public String getName() {
        return this.name;
    }

    public String getPath() {
        return this.path;
    }

    public KVMStoragePool getPool() {
        return this.pool;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public QemuObject.EncryptFormat getQemuEncryptFormat() {
        return this.qemuEncryptFormat;
    }

    public void setQemuEncryptFormat(QemuObject.EncryptFormat format) {
        this.qemuEncryptFormat = format;
    }

    public void setUseAsTemplate() { this.useAsTemplate = true; }

    public boolean useAsTemplate() { return this.useAsTemplate; }

    public String getDispName() {
        return dispName;
    }

    public void setDispName(String dispName) {
        this.dispName = dispName;
    }

    public String getVmName() {
        return vmName;
    }

    public void setVmName(String vmName) {
        this.vmName = vmName;
    }
}
