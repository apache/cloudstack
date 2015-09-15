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
package com.cloud.ovm.hypervisor;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.configuration.Config;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.DiscoveryException;
import com.cloud.host.HostInfo;
import com.cloud.host.HostVO;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.ovm.object.Connection;
import com.cloud.ovm.object.OvmHost;
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SSHCmdHelper;

@Local(value = Discoverer.class)
public class OvmDiscoverer extends DiscovererBase implements Discoverer, ResourceStateAdapter {
    protected String _publicNetworkDevice;
    protected String _privateNetworkDevice;
    protected String _guestNetworkDevice;

    @Inject
    ClusterDao _clusterDao;
    @Inject
    ResourceManager _resourceMgr;

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        _publicNetworkDevice = _params.get(Config.OvmPublicNetwork.key());
        _privateNetworkDevice = _params.get(Config.OvmPrivateNetwork.key());
        _guestNetworkDevice = _params.get(Config.OvmGuestNetwork.key());
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    protected OvmDiscoverer() {
    }

    @Override
    public boolean stop() {
        _resourceMgr.unregisterResourceStateAdapter(this.getClass().getSimpleName());
        return super.stop();
    }

    private boolean checkIfExisted(String guid) {
        QueryBuilder<HostVO> sc = QueryBuilder.create(HostVO.class);
        sc.and(sc.entity().getGuid(), SearchCriteria.Op.EQ, guid);
        sc.and(sc.entity().getHypervisorType(), SearchCriteria.Op.EQ, HypervisorType.Ovm);
        List<HostVO> hosts = sc.list();
        return !hosts.isEmpty();
    }

    @Override
    public Map<? extends ServerResource, Map<String, String>>
        find(long dcId, Long podId, Long clusterId, URI url, String username, String password, List<String> hostTags) throws DiscoveryException {
        Connection conn = null;

        if (!url.getScheme().equals("http")) {
            String msg = "urlString is not http so we're not taking care of the discovery for this: " + url;
            logger.debug(msg);
            return null;
        }
        if (clusterId == null) {
            String msg = "must specify cluster Id when add host";
            logger.debug(msg);
            throw new CloudRuntimeException(msg);
        }

        if (podId == null) {
            String msg = "must specify pod Id when add host";
            logger.debug(msg);
            throw new CloudRuntimeException(msg);
        }

        ClusterVO cluster = _clusterDao.findById(clusterId);
        if (cluster == null || (cluster.getHypervisorType() != HypervisorType.Ovm)) {
            if (logger.isInfoEnabled())
                logger.info("invalid cluster id or cluster is not for Ovm hypervisors");
            return null;
        }

        String agentUsername = _params.get("agentusername");
        if (agentUsername == null) {
            throw new CloudRuntimeException("Agent user name must be specified");
        }

        String agentPassword = _params.get("agentpassword");
        if (agentPassword == null) {
            throw new CloudRuntimeException("Agent password must be specified");
        }

        try {
            String hostname = url.getHost();
            InetAddress ia = InetAddress.getByName(hostname);
            String hostIp = ia.getHostAddress();
            String guid = UUID.nameUUIDFromBytes(hostIp.getBytes()).toString();

            if (checkIfExisted(guid)) {
                throw new CloudRuntimeException("The host " + hostIp + " has been added before");
            }

            logger.debug("Ovm discover is going to disover host having guid " + guid);

            ClusterVO clu = _clusterDao.findById(clusterId);
            if (clu.getGuid() == null) {
                clu.setGuid(UUID.randomUUID().toString());
                _clusterDao.update(clusterId, clu);
            }

            com.trilead.ssh2.Connection sshConnection = new com.trilead.ssh2.Connection(hostIp, 22);
            sshConnection.connect(null, 60000, 60000);
            sshConnection = SSHCmdHelper.acquireAuthorizedConnection(hostIp, username, password);
            if (sshConnection == null) {
                throw new DiscoveryException(String.format("Cannot connect to ovm host(IP=%1$s, username=%2$s, password=%3$s, discover failed", hostIp, username,
                    password));
            }

            if (!SSHCmdHelper.sshExecuteCmd(sshConnection, "[ -f '/etc/ovs-agent/agent.ini' ]")) {
                throw new DiscoveryException("Can not find /etc/ovs-agent/agent.ini " + hostIp);
            }

            Map<String, String> details = new HashMap<String, String>();
            OvmResourceBase ovmResource = new OvmResourceBase();
            details.put("ip", hostIp);
            details.put("username", username);
            details.put("password", password);
            details.put("zone", Long.toString(dcId));
            details.put("guid", guid);
            details.put("pod", Long.toString(podId));
            details.put("cluster", Long.toString(clusterId));
            details.put("agentusername", agentUsername);
            details.put("agentpassword", agentPassword);
            if (_publicNetworkDevice != null) {
                details.put("public.network.device", _publicNetworkDevice);
            }
            if (_privateNetworkDevice != null) {
                details.put("private.network.device", _privateNetworkDevice);
            }
            if (_guestNetworkDevice != null) {
                details.put("guest.network.device", _guestNetworkDevice);
            }

            Map<String, Object> params = new HashMap<String, Object>();
            params.putAll(details);
            ovmResource.configure("Ovm Server", params);
            ovmResource.start();

            conn = new Connection(hostIp, "oracle", agentPassword);
            /* After resource start, we are able to execute our agent api */
            OvmHost.Details d = OvmHost.getDetails(conn);
            details.put("agentVersion", d.agentVersion);
            details.put(HostInfo.HOST_OS_KERNEL_VERSION, d.dom0KernelVersion);
            details.put(HostInfo.HYPERVISOR_VERSION, d.hypervisorVersion);

            Map<OvmResourceBase, Map<String, String>> resources = new HashMap<OvmResourceBase, Map<String, String>>();
            resources.put(ovmResource, details);
            return resources;
        } catch (XmlRpcException e) {
            logger.debug("XmlRpc exception, Unable to discover OVM: " + url, e);
            return null;
        } catch (UnknownHostException e) {
            logger.debug("Host name resolve failed exception, Unable to discover OVM: " + url, e);
            return null;
        } catch (ConfigurationException e) {
            logger.debug("Configure resource failed, Unable to discover OVM: " + url, e);
            return null;
        } catch (Exception e) {
            logger.debug("Unable to discover OVM: " + url, e);
            return null;
        }
    }

    @Override
    public void postDiscovery(List<HostVO> hosts, long msId) throws DiscoveryException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean matchHypervisor(String hypervisor) {
        return HypervisorType.Ovm.toString().equalsIgnoreCase(hypervisor);
    }

    @Override
    public HypervisorType getHypervisorType() {
        return HypervisorType.Ovm;
    }

    @Override
    public HostVO createHostVOForConnectedAgent(HostVO host, StartupCommand[] cmd) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HostVO createHostVOForDirectConnectAgent(HostVO host, StartupCommand[] startup, ServerResource resource, Map<String, String> details, List<String> hostTags) {
        StartupCommand firstCmd = startup[0];
        if (!(firstCmd instanceof StartupRoutingCommand)) {
            return null;
        }

        StartupRoutingCommand ssCmd = ((StartupRoutingCommand)firstCmd);
        if (ssCmd.getHypervisorType() != HypervisorType.Ovm) {
            return null;
        }

        return _resourceMgr.fillRoutingHostVO(host, ssCmd, HypervisorType.Ovm, details, hostTags);
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        if (host.getType() != com.cloud.host.Host.Type.Routing || host.getHypervisorType() != HypervisorType.Ovm) {
            return null;
        }

        _resourceMgr.deleteRoutingHost(host, isForced, isForceDeleteStorage);
        return new DeleteHostAnswer(true);
    }

}
