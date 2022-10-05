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

import org.apache.cloudstack.utils.qemu.QemuImg.PhysicalDiskFormat;
import org.apache.cloudstack.utils.qemu.QemuObject;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class KVMPhysicalDisk {
    private String path;
    private String name;
    private KVMStoragePool pool;
    private boolean useAsTemplate;

    public static String RBDStringBuilder(String monHost, int monPort, String authUserName, String authSecret, String image) {
        String rbdOpts;

        rbdOpts = "rbd:" + image;
        rbdOpts += ":mon_host=" + composeOptionForMonHosts(monHost, monPort);

        if (authUserName == null) {
            rbdOpts += ":auth_supported=none";
        } else {
            rbdOpts += ":auth_supported=cephx";
            rbdOpts += ":id=" + authUserName;
            rbdOpts += ":key=" + authSecret;
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
        return "KVMPhysicalDisk [path=" + path + ", name=" + name + ", pool=" + pool + ", format=" + format + ", size=" + size + ", virtualSize=" + virtualSize + "]";
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
}
