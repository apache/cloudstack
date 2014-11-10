/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.cloud.hypervisor.ovm3.object;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

public class PoolOCFS2 extends OvmObject {
    private static final Logger LOGGER = Logger
            .getLogger(PoolOCFS2.class);
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
        return this.poolFsNFSBaseId;
    }

    public String getPoolFsId() {
        return this.poolFsId;
    }

    public String getPoolFsUuid() {
        return this.poolFsId;
    }

    public String getPoolFsTarget() {
        return this.poolFsTarget;
    }
    public String getPoolFsManagerUuid() {
        return this.poolFsManagerUuid;
    }
    public String getPoolFsVersion() {
        return this.poolFsVersion;
    }
    public String getPoolPoolFsId() {
        return this.poolPoolFsId;
    }
    public String getPoolFsType() {
        return this.poolFsType;
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

    /*
     * destroy_pool_filesystem, <class 'agent.api.poolfs.ocfs2.PoolOCFS2'>
     * argument: self - default: None argument: poolfs_type - default: None
     * argument: poolfs_target - default: None argument: poolfs_uuid - default:
     * None argument: poolfs_nfsbase_uuid - default: None
     */
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

    /*
     * create_pool_filesystem, <class 'agent.api.poolfs.ocfs2.PoolOCFS2'>
     * argument: self - default: None argument: poolfs_type - default: None
     * argument: poolfs_target - default: None argument: cluster_name - default:
     * None argument: poolfs_uuid - default: None argument: poolfs_nfsbase_uuid
     * - default: None argument: manager_uuid - default: None argument:
     * pool_uuid - default: None argument: block_size - default: None argument:
     * cluster_size - default: None argument: journal_size - default: None
     */
    /*
     * create_pool_filesystem('nfs', 'cs-mgmt:/volumes/cs-data/secondary',
     * 'ba9aaf00ae5e2d73', '0004fb0000050000e70fbddeb802208f',
     * 'b8ca41cb-3469-4f74-a086-dddffe37dc2d',
     * '0004fb00000100000af70d20dcce7d65', '0004fb0000020000ba9aaf00ae5e2d73')
     */
    public Boolean createPoolFs(String type, String target, String clustername,
            String fsid, String nfsbaseid, String managerid) throws Ovm3ResourceException {
        if (!this.hasAPoolFs()) {
            return nullIsTrueCallWrapper("create_pool_filesystem", type, target,
                    clustername, fsid, nfsbaseid, managerid, fsid);
        } else if (this.hasPoolFs(fsid)) {
            LOGGER.debug("PoolFs already exists on this host: " + fsid);
            return true;
        } else {
            throw new Ovm3ResourceException("Unable to add pool filesystem to host, "+
                    "pool filesystem with other id found: " + this.poolFsId);
        }
    }
    /* iSCSI/FC ?
    public Boolean createPoolFs(String type, String target, String clustername,
            String fsid, String nfsbaseid, String managerid, String id,
            int blocksize, int clustersize, int journalsize) throws Ovm3ResourceException {
        if (!this.hasAPoolFs()) {
            Object x = callWrapper("create_pool_filesystem", type, target,
                    clustername, fsid, nfsbaseid, managerid, id, blocksize,
                    clustersize, journalsize);
            if (x == null) {
                return true;
            }
            return false;
        } else if (this.hasPoolFs(fsid)) {
            LOGGER.debug("PoolFs already exists: " + fsid);
            return true;
        } else {
            throw new Ovm3ResourceException("Unable to add pool filesystem to host, "+
                    "pool filesystem with other id found: " + this.poolFsId);
        }
    }
    */
    /*
     * discover_pool_filesystem, <class 'agent.api.poolfs.ocfs2.PoolOCFS2'>
     * argument: self - default: None
     */
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

    /*
     * ocfs2_get_meta_data, <class 'agent.api.poolfs.ocfs2.PoolOCFS2'> argument:
     * self - default: None argument: device - default: None argument: filename
     * - default: None
     */
    public Boolean ocfs2GetMetaData(String device, String filename) throws Ovm3ResourceException {
        Object x = callWrapper("ocfs2_get_meta_data", device, filename);
        if (x == null) {
            return true;
        }
        return false;
    }
}
