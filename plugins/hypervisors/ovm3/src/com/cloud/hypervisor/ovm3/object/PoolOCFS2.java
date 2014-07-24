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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.xmlrpc.XmlRpcException;
import org.w3c.dom.Document;

/* should ingest this into Pool */
public class PoolOCFS2 extends OvmObject {
    public Map<String, String> poolFileSystem = new HashMap<String, String>();
    public String poolFsTarget;
    public String poolFsType;
    public String poolFsNFSBaseId;
    public String poolFsId;

    public String poolFsNFSBaseId() {
        return this.poolFsNFSBaseId;
    }

    public String poolFsId() {
        return this.poolFsId;
    }

    public String poolFsUuid() {
        return this.poolFsId;
    }

    public String poolFsTarget() {
        return this.poolFsTarget;
    }

    public PoolOCFS2(Connection c) {
        client = c;
    }

    /*
     * destroy_pool_filesystem, <class 'agent.api.poolfs.ocfs2.PoolOCFS2'>
     * argument: self - default: None argument: poolfs_type - default: None
     * argument: poolfs_target - default: None argument: poolfs_uuid - default:
     * None argument: poolfs_nfsbase_uuid - default: None
     */
    public Boolean destroyPoolFs(String type, String target, String uuid,
            String nfsbaseuuid) throws ParserConfigurationException,
            IOException, Exception {
        // should throw exception if no poolIps set
        Object x = callWrapper("destroy_pool_filesystem", type, target, uuid,
                nfsbaseuuid);
        if (x == null) {
            return true;
        }
        return false;
    }

    public Boolean destroyPoolFs() throws ParserConfigurationException,
            IOException, Exception {
        // should throw exception if no poolIps set
        Object x = callWrapper("destroy_pool_filesystem", poolFsType,
                poolFsTarget, poolFsId, poolFsNFSBaseId);
        if (x == null) {
            return true;
        }
        return false;
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
            String fsid, String nfsbaseid, String managerid, String id)
            throws XmlRpcException {
        Object x = callWrapper("create_pool_filesystem", type, target,
                clustername, fsid, nfsbaseid, managerid, id);
        // System.out.println(x);
        if (x == null) {
            return true;
        }
        return false;
    }

    public Boolean createPoolFs(String type, String target, String clustername,
            String fsid, String nfsbaseid, String managerid, String id,
            int blocksize, int clustersize, int journalsize)
            throws XmlRpcException {
        Object x = callWrapper("create_pool_filesystem", type, target,
                clustername, fsid, nfsbaseid, managerid, id, blocksize,
                clustersize, journalsize);
        if (x == null) {
            return true;
        }
        return false;
    }

    /*
     * discover_pool_filesystem, <class 'agent.api.poolfs.ocfs2.PoolOCFS2'>
     * argument: self - default: None
     */
    public Boolean discoverPoolFs() throws ParserConfigurationException,
            IOException, Exception {
        // should throw exception if no poolIps set
        Object x = callWrapper("discover_pool_filesystem");
        Document xmlDocument = prepParse((String) x);
        String path = "//Discover_Pool_Filesystem_Result";
        poolFileSystem = xmlToMap(path + "/Pool_Filesystem", xmlDocument);
        poolFsTarget = poolFileSystem.get("Pool_Filesystem_Target");
        poolFsType = poolFileSystem.get("Pool_Filesystem_Type");
        poolFsNFSBaseId = poolFileSystem.get("Pool_Filesystem_Nfsbase_Uuid");
        poolFsId = poolFileSystem.get("Pool_Filesystem_Uuid");
        if (x == null) {
            return true;
        }
        return false;
    }

    /*
     * ocfs2_get_meta_data, <class 'agent.api.poolfs.ocfs2.PoolOCFS2'> argument:
     * self - default: None argument: device - default: None argument: filename
     * - default: None
     */
    public Boolean ocfs2GetMetaData(String device, String filename)
            throws XmlRpcException {
        Object x = callWrapper("ocfs2_get_meta_data", device, filename);
        // System.out.println(x);
        if (x == null) {
            return true;
        }
        return false;
    }
}
