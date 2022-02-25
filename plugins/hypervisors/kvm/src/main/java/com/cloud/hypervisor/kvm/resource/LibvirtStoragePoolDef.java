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
    public enum PoolType {
        ISCSI("iscsi"), NETFS("netfs"), LOGICAL("logical"), DIR("dir"), RBD("rbd"), GLUSTERFS("glusterfs"), POWERFLEX("powerflex");
        String _poolType;

        PoolType(String poolType) {
            _poolType = poolType;
        }

        @Override
        public String toString() {
            return _poolType;
        }
    }

    public enum AuthenticationType {
        CHAP("chap"), CEPH("ceph");
        String _authType;

        AuthenticationType(String authType) {
            _authType = authType;
        }

        @Override
        public String toString() {
            return _authType;
        }
    }

    private PoolType _poolType;
    private String _poolName;
    private String _uuid;
    private String _sourceHost;
    private int _sourcePort;
    private String _sourceDir;
    private String _targetPath;
    private String _authUsername;
    private AuthenticationType _authType;
    private String _secretUuid;

    public LibvirtStoragePoolDef(PoolType type, String poolName, String uuid, String host, int port, String dir, String targetPath) {
        _poolType = type;
        _poolName = poolName;
        _uuid = uuid;
        _sourceHost = host;
        _sourcePort = port;
        _sourceDir = dir;
        _targetPath = targetPath;
    }

    public LibvirtStoragePoolDef(PoolType type, String poolName, String uuid, String host, String dir, String targetPath) {
        _poolType = type;
        _poolName = poolName;
        _uuid = uuid;
        _sourceHost = host;
        _sourceDir = dir;
        _targetPath = targetPath;
    }

    public LibvirtStoragePoolDef(PoolType type, String poolName, String uuid, String sourceHost, int sourcePort, String dir, String authUsername, AuthenticationType authType,
            String secretUuid) {
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

    public PoolType getPoolType() {
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

    public AuthenticationType getAuthType() {
        return _authType;
    }

    @Override
    public String toString() {
        StringBuilder storagePoolBuilder = new StringBuilder();
        if (_poolType == PoolType.GLUSTERFS) {
            /* libvirt mounts a Gluster volume, similar to NFS */
            storagePoolBuilder.append("<pool type='netfs'>\n");
        } else {
            storagePoolBuilder.append("<pool type='");
            storagePoolBuilder.append(_poolType);
            storagePoolBuilder.append("'>\n");
        }

        storagePoolBuilder.append("<name>" + _poolName + "</name>\n");
        if (_uuid != null)
            storagePoolBuilder.append("<uuid>" + _uuid + "</uuid>\n");
        if (_poolType == PoolType.NETFS) {
            storagePoolBuilder.append("<source>\n");
            storagePoolBuilder.append("<host name='" + _sourceHost + "'/>\n");
            storagePoolBuilder.append("<dir path='" + _sourceDir + "'/>\n");
            storagePoolBuilder.append("</source>\n");
        }
        if (_poolType == PoolType.RBD) {
            storagePoolBuilder.append("<source>\n");
            if (_sourcePort > 0) {
                storagePoolBuilder.append("<host name='" + _sourceHost + "' port='" + _sourcePort + "'/>\n");
            } else {
                storagePoolBuilder.append("<host name='" + _sourceHost + "'/>\n");
            }

            storagePoolBuilder.append("<name>" + _sourceDir + "</name>\n");
            if (_authUsername != null) {
                storagePoolBuilder.append("<auth username='" + _authUsername + "' type='" + _authType + "'>\n");
                storagePoolBuilder.append("<secret uuid='" + _secretUuid + "'/>\n");
                storagePoolBuilder.append("</auth>\n");
            }
            storagePoolBuilder.append("</source>\n");
        }
        if (_poolType == PoolType.GLUSTERFS) {
            storagePoolBuilder.append("<source>\n");
            storagePoolBuilder.append("<host name='");
            storagePoolBuilder.append(_sourceHost);
            if (_sourcePort != 0) {
                storagePoolBuilder.append("' port='");
                storagePoolBuilder.append(_sourcePort);
            }
            storagePoolBuilder.append("'/>\n");
            storagePoolBuilder.append("<dir path='");
            storagePoolBuilder.append(_sourceDir);
            storagePoolBuilder.append("'/>\n");
            storagePoolBuilder.append("<format type='");
            storagePoolBuilder.append(_poolType);
            storagePoolBuilder.append("'/>\n");
            storagePoolBuilder.append("</source>\n");
        }
        if (_poolType != PoolType.RBD && _poolType != PoolType.POWERFLEX) {
            storagePoolBuilder.append("<target>\n");
            storagePoolBuilder.append("<path>" + _targetPath + "</path>\n");
            storagePoolBuilder.append("</target>\n");
        }
        storagePoolBuilder.append("</pool>\n");
        return storagePoolBuilder.toString();
    }
}
