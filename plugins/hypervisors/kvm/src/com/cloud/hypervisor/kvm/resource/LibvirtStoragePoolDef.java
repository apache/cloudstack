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
package com.cloud.hypervisor.kvm.resource;

public class LibvirtStoragePoolDef {
    public enum poolType {
        ISCSI("iscsi"), NETFS("netfs"), LOGICAL("logical"), DIR("dir"), RBD("rbd");
        String _poolType;

        poolType(String poolType) {
            _poolType = poolType;
        }

        @Override
        public String toString() {
            return _poolType;
        }
    }

    public enum authType {
        CHAP("chap"), CEPH("ceph");
        String _authType;

        authType(String authType) {
            _authType = authType;
        }

        @Override
        public String toString() {
            return _authType;
        }
    }

    private poolType _poolType;
    private String _poolName;
    private String _uuid;
    private String _sourceHost;
    private int _sourcePort;
    private String _sourceDir;
    private String _targetPath;
    private String _authUsername;
    private authType _authType;
    private String _secretUuid;

    public LibvirtStoragePoolDef(poolType type, String poolName, String uuid,
            String host, int port, String dir, String targetPath) {
        _poolType = type;
        _poolName = poolName;
        _uuid = uuid;
        _sourceHost = host;
        _sourcePort = port;
        _sourceDir = dir;
        _targetPath = targetPath;
    }

    public LibvirtStoragePoolDef(poolType type, String poolName, String uuid,
            String host, String dir, String targetPath) {
        _poolType = type;
        _poolName = poolName;
        _uuid = uuid;
        _sourceHost = host;
        _sourceDir = dir;
        _targetPath = targetPath;
    }

    public LibvirtStoragePoolDef(poolType type, String poolName, String uuid,
            String sourceHost, int sourcePort, String dir, String authUsername,
            authType authType, String secretUuid) {
        _poolType = type;
        _poolName = poolName;
        _uuid = uuid;
        _sourceHost = sourceHost;
        _sourcePort = sourcePort;
        _sourceDir = dir;
        _authUsername = authUsername;
        _authType = authType;
        _secretUuid = secretUuid;
    }

    public String getPoolName() {
        return _poolName;
    }

    public poolType getPoolType() {
        return _poolType;
    }

    public String getSourceHost() {
        return _sourceHost;
    }

    public int getSourcePort() {
        return _sourcePort;
    }

    public String getSourceDir() {
        return _sourceDir;
    }

    public String getTargetPath() {
        return _targetPath;
    }

    public String getAuthUserName() {
        return _authUsername;
    }

    public String getSecretUUID() {
        return _secretUuid;
    }

    public authType getAuthType() {
        return _authType;
    }

    @Override
    public String toString() {
        StringBuilder storagePoolBuilder = new StringBuilder();
        storagePoolBuilder.append("<pool type='" + _poolType + "'>\n");
        storagePoolBuilder.append("<name>" + _poolName + "</name>\n");
        if (_uuid != null)
            storagePoolBuilder.append("<uuid>" + _uuid + "</uuid>\n");
        if (_poolType == poolType.NETFS) {
            storagePoolBuilder.append("<source>\n");
            storagePoolBuilder.append("<host name='" + _sourceHost + "'/>\n");
            storagePoolBuilder.append("<dir path='" + _sourceDir + "'/>\n");
            storagePoolBuilder.append("</source>\n");
        }
        if (_poolType == poolType.RBD) {
            storagePoolBuilder.append("<source>\n");
            storagePoolBuilder.append("<host name='" + _sourceHost + "' port='" + _sourcePort + "'/>\n");
            storagePoolBuilder.append("<name>" + _sourceDir + "</name>\n");
            if (_authUsername != null) {
                storagePoolBuilder.append("<auth username='" + _authUsername + "' type='" + _authType + "'>\n");
                storagePoolBuilder.append("<secret uuid='" + _secretUuid + "'/>\n");
                storagePoolBuilder.append("</auth>\n");
            }
            storagePoolBuilder.append("</source>\n");
        }
        if (_poolType != poolType.RBD) {
            storagePoolBuilder.append("<target>\n");
            storagePoolBuilder.append("<path>" + _targetPath + "</path>\n");
            storagePoolBuilder.append("</target>\n");
        }
        storagePoolBuilder.append("</pool>\n");
        return storagePoolBuilder.toString();
    }
}
