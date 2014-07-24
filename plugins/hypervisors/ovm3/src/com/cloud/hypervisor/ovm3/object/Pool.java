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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

/*
 * synonym to the pool python lib in the ovs-agent
 */
public class Pool extends OvmObject {
    private static final Logger LOGGER = Logger
            .getLogger(Pool.class);
    /*
     * {Pool_Filesystem_Target=cs-mgmt:/volumes/cs-data/secondary,
     * Pool_Filesystem_Type=nfs,
     * Pool_Filesystem_Nfsbase_Uuid=b8ca41cb-3469-4f74-a086-dddffe37dc2d,
     * Pool_Filesystem_Uuid=0004fb0000050000e70fbddeb802208f}
     */
    private List<String> validRoles = new ArrayList<String>() {
        {
            add("utility");
            add("xen");
        }
    };
    private Boolean _poolDisc = false;
    public Map<String, String> poolFileSystem = new HashMap<String, String>();
    public List<String> poolIps = new ArrayList<String>();
    private List<String> poolRoles = new ArrayList<String>();
    public List<String> poolMembers = new ArrayList<String>();
    public String poolMasterVip;
    public String poolAlias;
    public String poolId = "";

    public Pool(Connection c) throws ParserConfigurationException, IOException,
            Exception {
        client = c;
        discoverServerPool();
    }

    public String getPoolMasterVip() {
        return this.poolMasterVip;
    }

    public String getPoolAlias() {
        return this.poolAlias;
    }

    public String getPoolId() {
        return this.poolId;
    }

    /* TODO: check the toString for the list x,x */
    public List<String> getValidRoles() {
        return this.validRoles;
    }

    public void setPoolIps(List<String> ips) {
        this.poolIps = new ArrayList<String>();
        this.poolIps.addAll(ips);
    }

    /*
     * create_server_pool, <class 'agent.api.serverpool.ServerPool'> argument:
     * self - default: None argument: pool_alias - default: None argument:
     * pool_uuid - default: None argument: pool_virtual_ip - default: None
     * argument: node_number - default: None argument: server_hostname -
     * default: None argument: server_ip - default: None argument: roles -
     * default: None
     */
    public Boolean createServerPool(String alias, String id, String vip,
            int num, String name, String ip, List<String> roles)
            throws XmlRpcException {
        String role = roles.toString();
        role = "xen,utility";
        Object x = callWrapper("create_server_pool", alias, id, vip, num, name,
                ip, role);
        if (x == null) {
            return true;
        }
        return false;
    }

    public Boolean createServerPool(String alias, String id, String vip,
            int num, String name, String ip) throws XmlRpcException {
        return this.createServerPool(alias, id, vip, num, name, ip,
                getValidRoles());
    }

    public Boolean createServerPool(int num, String name, String ip)
            throws XmlRpcException {
        return createServerPool(poolAlias, poolId, poolMasterVip, num, name,
                ip, poolRoles);
    }

    /*
     * update_pool_virtual_ip, <class 'agent.api.serverpool.ServerPool'>
     * argument: self - default: None argument: new_pool_vip - default: None
     */
    public Boolean updatePoolVirtualIp(String ip) throws XmlRpcException {
        Object x = callWrapper("update_pool_virtual_ip", ip);
        if (x == null) {
            this.poolMasterVip = ip;
            return true;
        }
        return false;
    }

    /*
     * leave_server_pool, <class 'agent.api.serverpool.ServerPool'> argument:
     * self - default: None argument: pool_uuid - default: None
     */
    public Boolean leaveServerPool(String uuid) throws XmlRpcException {
        Object x = callWrapper("leave_server_pool", uuid);
        if (x == null) {
            return true;
        }
        return false;
    }

    /*
     * set_server_pool_alias, <class 'agent.api.serverpool.ServerPool'>
     * argument: self - default: None argument: pool_alias - default: None
     */
    public Boolean setServerPoolAlias(String alias) throws XmlRpcException {
        Object x = callWrapper("set_server_pool_alias", alias);
        if (x == null) {
            this.poolAlias = alias;
            return true;
        }
        return false;
    }

    /*
     * take_ownership, <class 'agent.api.serverpool.ServerPool'> argument: self
     * - default: None argument: manager_uuid - default: None argument:
     * manager_core_api_url - default: None
     * take_ownership('0004fb00000100000af70d20dcce7d65',
     * 'https://2f55e3b9efa6f067ad54a7b144bb6f2e:
     * ******@0.0.0.0:7002/ovm/core/OVMManagerCoreServlet')
     */
    public Boolean takeOwnership(String uuid, String apiurl)
            throws XmlRpcException {
        Object x = callWrapper("take_ownership", uuid, apiurl);
        if (x == null) {
            return true;
        }
        return false;
    }

    /*
     * destroy_server_pool, <class 'agent.api.serverpool.ServerPool'> argument:
     * self - default: None argument: pool_uuid - default: None
     */
    public Boolean destroyServerPool(String uuid) throws XmlRpcException {
        Object x = callWrapper("destroy_server_pool", uuid);
        if (x == null) {
            return true;
        }
        return false;
    }

    /*
     * release_ownership, <class 'agent.api.serverpool.ServerPool'> argument:
     * self - default: None argument: manager_uuid - default: None
     */
    public Boolean releaseOwnership(String uuid) throws XmlRpcException {
        Object x = callWrapper("release_ownership", uuid);
        if (x == null) {
            return true;
        }
        return false;
    }

    /* server.discover_pool_filesystem */
    /*
     * discover_server_pool, <class 'agent.api.serverpool.ServerPool'> argument:
     * self - default: None
     */
    public Boolean discoverServerPool() throws ParserConfigurationException,
            IOException, Exception {
        /* forcefull rediscover for now */
        if (this._poolDisc) {
            return true;
        }
        Object x = callWrapper("discover_server_pool");
        if (x == null) {
            return false;
        }

        try {
            Document xmlDocument = prepParse((String) x);
            String path = "//Discover_Server_Pool_Result/Server_Pool";
            this.poolId = xmlToString(path + "/Unique_Id", xmlDocument);
            this.poolAlias = xmlToString(path + "/Pool_Alias", xmlDocument);
            this.poolMasterVip = xmlToString(path + "/Master_Virtual_Ip",
                    xmlDocument);
            this.poolMembers = xmlToList(path + "/Member_List", xmlDocument);
            this.poolIps
                    .addAll(xmlToList(path + "//Registered_IP", xmlDocument));
            this._poolDisc = true;
        } catch (Exception e) {
            if (e.getMessage() == null) {
                LOGGER.debug("No pool to discover: " + e.getMessage());
            } else {
                LOGGER.debug("Error in pooldiscovery: " + e.getMessage());
            }
        }

        return true;
    }

    /*
     * update_server_roles, <class 'agent.api.serverpool.ServerPool'> argument:
     * self - default: None argument: roles - default: None ?> list or sring
     */
    private Boolean validPoolRole(String role) throws Exception {
        for (String r : this.validRoles) {
            if (r.contentEquals(role)) {
                return true;
            }
        }
        throw new Exception("Illegal role: " + role);
    }

    private Boolean validPoolRole(List<String> roles) throws Exception {
        for (String r : roles) {
            return validPoolRole(r);
        }
        return false;
    }

    public Boolean setServerRoles() throws XmlRpcException, Exception {
        validPoolRole(this.poolRoles);
        String roles = StringUtils.join(this.poolRoles.toArray(), ",");
        Object x = callWrapper("update_server_roles", roles);
        if (x == null) {
            return true;
        }
        return false;
    }

    /* do some sanity check on the valid poolroles */
    public Boolean setServerRoles(List<String> roles) throws Exception {
        this.poolRoles.addAll(roles);
        return setServerRoles();
    }

    public void addServerRole(String role) throws Exception {
        validPoolRole(role);
        this.poolRoles.add(role);
    }

    public void removeServerRole(String role) {
        this.poolRoles.remove(role);
    }

    public boolean serverHasRole(String role) {
        for (String r : this.poolRoles) {
            if (r.contentEquals(role)) {
                return true;
            }
        }
        return false;
    }

    /*
     * join_server_pool, <class 'agent.api.serverpool.ServerPool'> argument:
     * self - default: None argument: pool_alias - default: None argument:
     * pool_uuid - default: None argument: pool_virtual_ip - default: None
     * argument: node_number - default: None argument: server_hostname -
     * default: None argument: server_ip - default: None argument: roles -
     * default: None
     */
    /* allow these to be set before ? */
    public Boolean joinServerPool(String alias, String id, String vip, int num,
            String name, String ip, List<String> roles) throws XmlRpcException {
        String role = StringUtils.join(roles.toArray(), ",");
        Object x = callWrapper("join_server_pool", alias, id, vip, num, name,
                ip, role);
        if (x == null) {
            return true;
        }
        return false;
    }

    public Boolean joinServerPool(String alias, String id, String vip, int num,
            String name, String ip) throws XmlRpcException {
        return joinServerPool(alias, id, vip, num, name, ip, getValidRoles());
    }

    public Boolean joinServerPool(int num, String name, String ip)
            throws XmlRpcException {
        return joinServerPool(poolAlias, poolId, poolMasterVip, num, name, ip,
                poolRoles);
    }

    /*
     * set_pool_member_ip_list, <class 'agent.api.serverpool.ServerPool'>
     * argument: self - default: None argument: ip_list - default: None
     */
    public Boolean setPoolMemberIpList() throws XmlRpcException {
        // should throw exception if no poolIps set
        Object x = callWrapper("set_pool_member_ip_list", this.poolIps);
        if (x == null) {
            return true;
        }
        return false;
    }

    public List getPoolMemberIpList() throws XmlRpcException, Exception {
        if (poolIps.size() == 0) {
            this.discoverServerPool();
        }
        return poolIps;
    }

    /* TODO: need to change the logic here */
    public Boolean setPoolMemberIpList(String ip) throws XmlRpcException {
        this.poolIps = new ArrayList<String>();
        this.poolIps.add(ip);
        return setPoolMemberIpList();
    }

    public Boolean addPoolMemberIp(String ip) throws XmlRpcException, Exception {
        this.getPoolMemberIpList();
        this.poolIps.add(ip);
        return setPoolMemberIpList();
    }

    /* meh */
    public Boolean removePoolMemberIp(String ip) throws XmlRpcException,
            Exception {
        this.getPoolMemberIpList();
        this.poolIps.remove(ip);
        return setPoolMemberIpList();
    }
}
