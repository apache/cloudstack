/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.datastore.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.StringTokenizer;

import com.cloud.storage.Storage;

public class NexentaUtil {
    public static final String PROVIDER_NAME = "Nexenta";

    public static final String NMS_URL = "nmsUrl";
    public static final String VOLUME = "volume";

    public static final String STORAGE_HOST = "storageHost";
    public static final String STORAGE_PORT = "storagePort";
    public static final String STORAGE_TYPE = "storageType";
    public static final String STORAGE_PATH = "storagePath";

    public static final String DEFAULT_NMS_USER = "admin";
    public static final String DEFAULT_NMS_PASSWORD = "nexenta";

    public static final String SPARSE_VOLUMES = "sparseVolumes";
    public static final String VOLUME_BLOCK_SIZE = "volumeBlockSize";

    public static final int DEFAULT_NMS_PORT = 2000;
    public static final int DEFAULT_ISCSI_TARGET_PORTAL_PORT = 3260;
    public static final int DEFAULT_NFS_PORT = 2049;

    public static final String ISCSI_TARGET_NAME_PREFIX = "iqn.1986-03.com.sun:02:cloudstack-";
    public static final String ISCSI_TARGET_GROUP_PREFIX = "cloudstack/";

    /**
     * Parse NMS url into normalized parts like scheme, user, host and others.
     *
     * Example NMS URL:
     *    auto://admin:nexenta@192.168.1.1:2000/
     *
     * NMS URL parts:
     *    auto                true if url starts with auto://, protocol will be automatically switched to https if http not supported;
     *    scheme (auto)       connection protocol (http or https);
     *    user (admin)        NMS user;
     *    password (nexenta)  NMS password;
     *    host (192.168.1.1)  NMS host;
     *    port (2000)         NMS port.
     *
     * @param nmsUrl url string to parse
     * @return instance of NexentaConnection class
     */
    public static NexentaNmsUrl parseNmsUrl(String nmsUrl) {
        URI uri;

        try {
            uri = new URI(nmsUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URI: " + nmsUrl);
        }

        boolean isAuto = false;
        String schema = uri.getScheme();
        if (schema == null || schema.isEmpty() || "auto".equalsIgnoreCase(schema)) {
            schema = "http";
            isAuto = true;
        }

        String username, password, userInfo = uri.getUserInfo();
        if (userInfo == null) {
            username = DEFAULT_NMS_USER;
            password = DEFAULT_NMS_PASSWORD;
        } else {
            if (userInfo.indexOf(':') < 0) {
                username = userInfo;
                password = DEFAULT_NMS_PASSWORD;
            } else {
                String[] parts = userInfo.split(":", 2);
                username = parts[0];
                password = parts[1];
            }
        }

        String host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException(String.format("NMS host required: %s.", nmsUrl));
        }

        int port = uri.getPort();
        if (port == -1) {
            port = DEFAULT_NMS_PORT;
        }

        return new NexentaNmsUrl(isAuto, schema, username, password, host, port);
    }

    public static Storage.StoragePoolType getStorageType(String v) {
        if ("iSCSI".equalsIgnoreCase(v)) {
            return Storage.StoragePoolType.Iscsi;
        } else if ("NFS".equalsIgnoreCase(v)) {
            return Storage.StoragePoolType.NetworkFilesystem;
        }
        return Storage.StoragePoolType.Iscsi;
    }

    public static class NexentaPluginParameters {
        protected NexentaNmsUrl nmsUrl;
        protected String volume;
        protected Storage.StoragePoolType storageType = Storage.StoragePoolType.Iscsi;
        protected String storageHost;
        protected Integer storagePort;
        protected String storagePath;
        protected Boolean sparseVolumes = false;
        protected String volumeBlockSize = "8K";

        public void setNmsUrl(String url) {
            this.nmsUrl = NexentaUtil.parseNmsUrl(url);
        }

        public NexentaNmsUrl getNmsUrl() {
            return nmsUrl;
        }

        public void setVolume(String volume) {
            if (volume.endsWith("/")) {
                this.volume = volume.substring(0, volume.length() - 1);
            } else {
                this.volume = volume;
            }
        }

        public String getVolume() {
            return volume;
        }

        public void setStorageType(String storageType) {
            this.storageType = NexentaUtil.getStorageType(storageType);
        }

        public Storage.StoragePoolType getStorageType() {
            return storageType;
        }

        public void setStorageHost(String host) {
            this.storageHost = host;
        }

        public String getStorageHost() {
            if (storageHost == null && nmsUrl != null) {
                return nmsUrl.getHost();
            }
            return storageHost;
        }

        public void setStoragePort(String port) {
            this.storagePort = Integer.parseInt(port);
        }

        public Integer getStoragePort() {
            if (storagePort == null && storageType != null) {
                if (storageType == Storage.StoragePoolType.Iscsi) {
                    return DEFAULT_ISCSI_TARGET_PORTAL_PORT;
                } else {
                    return DEFAULT_NFS_PORT;
                }
            }
            return storagePort;
        }

        public void setStoragePath(String path) {
            this.storagePath = path;
        }

        public String getStoragePath() {
            return storagePath;
        }

        public void setSparseVolumes(String sparseVolumes) {
            this.sparseVolumes = Boolean.TRUE.toString().equalsIgnoreCase(sparseVolumes);
        }

        public Boolean getSparseVolumes() {
            return sparseVolumes;
        }

        public void setVolumeBlockSize(String volumeBlockSize) {
            this.volumeBlockSize = volumeBlockSize;
        }

        public String getVolumeBlockSize() {
            return volumeBlockSize;
        }
    }

    public static NexentaPluginParameters parseNexentaPluginUrl(String url) {
        final String delimiter1 = ";";
        final String delimiter2 = "=";
        StringTokenizer st = new StringTokenizer(url, delimiter1);
        NexentaPluginParameters params = new NexentaPluginParameters();
        while (st.hasMoreElements()) {
            String token = st.nextElement().toString();
            int idx = token.indexOf(delimiter2);
            if (idx == -1) {
                throw new RuntimeException("Invalid URL format");
            }
            String[] urlKeyAndValue = token.split(delimiter2, 2);
            if (NMS_URL.equalsIgnoreCase(urlKeyAndValue[0])) {
                params.setNmsUrl(urlKeyAndValue[1]);
            } else if (VOLUME.equalsIgnoreCase(urlKeyAndValue[0])) {
                params.setVolume(urlKeyAndValue[1]);
            } else if (STORAGE_TYPE.equalsIgnoreCase(urlKeyAndValue[0])) {
                params.setStorageType(urlKeyAndValue[1]);
            } else if (STORAGE_HOST.equalsIgnoreCase(urlKeyAndValue[0])) {
                params.setStorageHost(urlKeyAndValue[1]);
            } else if (STORAGE_PORT.equalsIgnoreCase(urlKeyAndValue[0])) {
                params.setStoragePort(urlKeyAndValue[1]);
            } else if (STORAGE_PATH.equalsIgnoreCase(urlKeyAndValue[0])) {
                params.setStoragePath(urlKeyAndValue[1]);
            } else if (SPARSE_VOLUMES.equalsIgnoreCase(urlKeyAndValue[0])) {
                params.setSparseVolumes(urlKeyAndValue[1]);
            } else if (VOLUME_BLOCK_SIZE.equalsIgnoreCase(urlKeyAndValue[0])) {
                params.setVolumeBlockSize(urlKeyAndValue[1]);
            }
        }
        return params;
    }
}
