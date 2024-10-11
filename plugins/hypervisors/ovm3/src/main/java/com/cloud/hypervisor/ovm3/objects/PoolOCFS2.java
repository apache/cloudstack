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

package com.cloud.hypervisor.ovm3.objects;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;

public class PoolOCFS2 extends OvmObject {
    private Map<String, String> poolFileSystem = new HashMap<String, String>();
    private String poolFsTarget;
    private String poolFsType;
    private String poolFsNFSBaseId;
    private String poolFsId;
    private String poolFsVersion;
    private String poolFsManagerUuid;
    private String poolPoolFsId;

    public PoolOCFS2(Connection c) {
        setClient(c);
    }

    public String getPoolFsNFSBaseId() {
        return poolFsNFSBaseId;
    }

    public String getPoolFsId() {
        return poolFsId;
    }

    public String getPoolFsUuid() {
        return poolFsId;
    }

    public String getPoolFsTarget() {
        return poolFsTarget;
    }
    public String getPoolFsManagerUuid() {
        return poolFsManagerUuid;
    }
    public String getPoolFsVersion() {
        return poolFsVersion;
    }
    public String getPoolPoolFsId() {
        return poolPoolFsId;
    }
    public String getPoolFsType() {
        return poolFsType;
    }
    public Boolean hasPoolFs(String id) throws Ovm3ResourceException {
        if (poolFsId == null) {
            discoverPoolFs();
        }
        if (hasAPoolFs() && poolFsId.equals(id)) {
            return true;
        }
        return false;
    }
    public Boolean hasAPoolFs() throws Ovm3ResourceException {
        if (poolFsId == null) {
            discoverPoolFs();
        }
        if (poolFsId == null) {
            return false;
        }
        return true;
    }

    public Boolean destroyPoolFs(String type, String target, String uuid,
            String nfsbaseuuid) throws Ovm3ResourceException {
        // should throw exception if no poolIps set
        return nullIsTrueCallWrapper("destroy_pool_filesystem", type, target, uuid,
                nfsbaseuuid);
    }

    public Boolean destroyPoolFs() throws Ovm3ResourceException {
        // should throw exception if no poolIps set
        return nullIsTrueCallWrapper("destroy_pool_filesystem", poolFsType,
                poolFsTarget, poolFsId, poolFsNFSBaseId);
    }

    public Boolean createPoolFs(String type, String target, String clustername,
            String fsid, String nfsbaseid, String managerid) throws Ovm3ResourceException {
        if (!hasAPoolFs()) {
            return nullIsTrueCallWrapper("create_pool_filesystem", type, target,
                    clustername, fsid, nfsbaseid, managerid, fsid);
        } else if (hasPoolFs(fsid)) {
            logger.debug("PoolFs already exists on this host: " + fsid);
            return true;
        } else {
            throw new Ovm3ResourceException("Unable to add pool filesystem to host, "+
                    "pool filesystem with other id found: " + poolFsId);
        }
    }

    /* Assume a single pool can be used for a host... */
    public Boolean discoverPoolFs() throws Ovm3ResourceException{
        // should throw exception if no poolIps set
        Object x = callWrapper("discover_pool_filesystem");
        if (x == null) {
            return false;
        }
        Document xmlDocument = prepParse((String) x);
        String path = "//Discover_Pool_Filesystem_Result";
        poolFileSystem = xmlToMap(path + "/Pool_Filesystem", xmlDocument);
        poolFsTarget = poolFileSystem.get("Pool_Filesystem_Target");
        poolFsType = poolFileSystem.get("Pool_Filesystem_Type");
        poolFsNFSBaseId = poolFileSystem.get("Pool_Filesystem_Nfsbase_Uuid");
        poolFsId = poolFileSystem.get("Pool_Filesystem_Uuid");
        poolPoolFsId = poolFileSystem.get("Pool_Filesystem_Pool_Uuid");
        poolFsManagerUuid = poolFileSystem.get("Pool_Filesystem_Manager_Uuid");
        poolFsVersion = poolFileSystem.get("Pool_Filesystem_Version");
        return true;
    }

    public Boolean ocfs2GetMetaData(String device, String filename) throws Ovm3ResourceException {
        Object x = callWrapper("ocfs2_get_meta_data", device, filename);
        if (x == null) {
            return true;
        }
        return false;
    }
}
