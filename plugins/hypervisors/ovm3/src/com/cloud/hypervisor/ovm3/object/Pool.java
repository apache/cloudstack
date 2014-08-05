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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/*
 * synonym to the pool python lib in the ovs-agent
 */
public class Pool extends OvmObject {
    private static final Logger LOGGER = Logger
            .getLogger(Pool.class);

    private List<String> validRoles = new ArrayList<String>() {
        {
            add("utility");
            add("xen");
        }
    };
    private List<String> poolHosts = new ArrayList<String>();
    private List<String> poolRoles = new ArrayList<String>();
    private List<String> poolMembers = new ArrayList<String>();
    private String poolMasterVip;
    private String poolAlias;
    private String poolId;

    public Pool(Connection c) {
        setClient(c);
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
        this.poolHosts = new ArrayList<String>();
        this.poolHosts.addAll(ips);
    }

    public Boolean isInPool(String id) throws Ovm3ResourceException {
        if (poolId == null) {
            discoverServerPool();
        }
        if (isInAPool() && poolId.equals(id)) {
            return true;
        }
        return false;
    }

    public Boolean isInAPool() throws Ovm3ResourceException {
        if (poolId == null) {
            discoverServerPool();
        }
        if (poolId == null) {
            return false;
        }
        return true;
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
            int num, String name, String host, List<String> roles) throws Ovm3ResourceException{
        String role = roles.toString();
        role = "xen,utility";
        if (!this.isInAPool()) {
            Object x = callWrapper("create_server_pool", alias, id, vip, num, name,
                    host, role);
            if (x == null) {
                return true;
            }
            return false;
        } else if (this.isInPool(id)) {
            return true;
        } else {
            throw new Ovm3ResourceException("Unable to add host is already in  a pool with id : " + this.poolId);
        }
    }

    public Boolean createServerPool(String alias, String id, String vip,
            int num, String name, String ip) throws Ovm3ResourceException {
        return createServerPool(alias, id, vip, num, name, ip,
                getValidRoles());
    }

    public Boolean createServerPool(int num, String name, String ip) throws Ovm3ResourceException{
        return createServerPool(poolAlias, poolId, poolMasterVip, num, name,
                ip, poolRoles);
    }

    /*
     * update_pool_virtual_ip, <class 'agent.api.serverpool.ServerPool'>
     * argument: self - default: None argument: new_pool_vip - default: None
     */
    public Boolean updatePoolVirtualIp(String ip) throws Ovm3ResourceException {
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
    public Boolean leaveServerPool(String uuid) throws Ovm3ResourceException{
        return nullIsTrueCallWrapper("leave_server_pool", uuid);
    }

    /*
     * set_server_pool_alias, <class 'agent.api.serverpool.ServerPool'>
     * argument: self - default: None argument: pool_alias - default: None
     */
    public Boolean setServerPoolAlias(String alias) throws Ovm3ResourceException{
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
    public Boolean takeOwnership(String uuid, String apiurl) throws Ovm3ResourceException {
        return nullIsTrueCallWrapper("take_ownership", uuid, apiurl);
    }

    /*
     * destroy_server_pool, <class 'agent.api.serverpool.ServerPool'> argument:
     * self - default: None argument: pool_uuid - default: None
     */
    public Boolean destroyServerPool(String uuid) throws Ovm3ResourceException{
        return nullIsTrueCallWrapper("destroy_server_pool", uuid);
    }

    /*
     * release_ownership, <class 'agent.api.serverpool.ServerPool'> argument:
     * self - default: None argument: manager_uuid - default: None
     */
    public Boolean releaseOwnership(String uuid) throws Ovm3ResourceException{
        return nullIsTrueCallWrapper("release_ownership", uuid);
    }

    /* server.discover_pool_filesystem */
    /*
     * discover_server_pool, <class 'agent.api.serverpool.ServerPool'> argument:
     * self - default: None
     */
    public Boolean discoverServerPool() throws Ovm3ResourceException {
        Object x = callWrapper("discover_server_pool");
        if (x == null) {
            return false;
        }

        Document xmlDocument = prepParse((String) x);
        String path = "//Discover_Server_Pool_Result/Server_Pool";
        this.poolId = xmlToString(path + "/Unique_Id", xmlDocument);
        this.poolAlias = xmlToString(path + "/Pool_Alias", xmlDocument);
        this.poolMasterVip = xmlToString(path + "/Master_Virtual_Ip",
                xmlDocument);
        this.setPoolMembers(xmlToList(path + "/Member_List", xmlDocument));
        this.poolHosts.addAll(xmlToList(path + "//Registered_IP", xmlDocument));

        return true;
    }

    /*
     * update_server_roles, <class 'agent.api.serverpool.ServerPool'> argument:
     * self - default: None argument: roles - default: None ?> list or sring
     */
    private Boolean validPoolRole(String role)  {
        for (String r : this.validRoles) {
            if (r.contentEquals(role)) {
                return true;
            }
        }
        LOGGER.info("Illegal role: " + role);
        return false;
    }

    private Boolean validPoolRole(List<String> roles) {
        for (String r : roles) {
            return validPoolRole(r);
        }
        return false;
    }

    public Boolean setServerRoles() throws Ovm3ResourceException{
        validPoolRole(this.poolRoles);
        String roles = StringUtils.join(this.poolRoles.toArray(), ",");
        return nullIsTrueCallWrapper("update_server_roles", roles);
    }

    /* do some sanity check on the valid poolroles */
    public Boolean setServerRoles(List<String> roles) throws Ovm3ResourceException {
        this.poolRoles.addAll(roles);
        return setServerRoles();
    }

    public void addServerRole(String role) {
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
            String name, String host, List<String> roles) throws Ovm3ResourceException{
        String role = StringUtils.join(roles.toArray(), ",");
        if (!this.isInAPool()) {
            Object x = callWrapper("join_server_pool", alias, id, vip, num, name,
                    host, role);
            if (x == null) {
                return true;
            }
            return false;
        } else if (this.isInPool(id)) {
            return true;
        } else {
            throw new Ovm3ResourceException("Unable to add host is already in  a pool with id : " + this.poolId);
        }
    }

    public Boolean joinServerPool(String alias, String id, String vip, int num,
            String name, String host) throws Ovm3ResourceException {
        return joinServerPool(alias, id, vip, num, name, host, getValidRoles());
    }

    public Boolean joinServerPool(int num, String name, String host) throws Ovm3ResourceException{
        return joinServerPool(poolAlias, poolId, poolMasterVip, num, name, host,
                poolRoles);
    }

    /*
     * set_pool_member_ip_list, <class 'agent.api.serverpool.ServerPool'>
     * argument: self - default: None argument: ip_list - default: None
     */
    public Boolean setPoolMemberList() throws Ovm3ResourceException {
        // should throw exception if no poolHosts set
        return nullIsTrueCallWrapper("set_pool_member_ip_list", this.poolHosts);
    }

    public List<String> getPoolMemberList() throws Ovm3ResourceException {
        if (poolId == null) {
            discoverServerPool();
        }
        return poolHosts;
    }

    /* TODO: need to change the logic here */
    public Boolean setPoolMemberList(String host) throws Ovm3ResourceException {
        this.poolHosts = new ArrayList<String>();
        this.poolHosts.add(host);
        return setPoolMemberList();
    }

    public Boolean setPoolMemberList(List<String> hosts) throws Ovm3ResourceException {
        this.poolHosts = new ArrayList<String>();
        this.poolHosts.addAll(hosts);
        return setPoolMemberList();
    }

    public Boolean addPoolMemberIp(String host) throws Ovm3ResourceException{
        this.getPoolMemberList();
        this.poolHosts.add(host);
        return setPoolMemberList();
    }

    /* meh */
    public Boolean removePoolMember(String host) throws Ovm3ResourceException {
        this.getPoolMemberList();
        this.poolHosts.remove(host);
        return setPoolMemberList();
    }

    public List<String> getPoolMembers() {
        return poolMembers;
    }

    public void setPoolMembers(List<String> poolMembers) {
        this.poolMembers = poolMembers;
    }
}
