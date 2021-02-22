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
package com.cloud.hypervisor.xenserver.discoverer;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.inject.Inject;
import javax.naming.ConfigurationException;
import javax.persistence.EntityExistsException;

import org.apache.cloudstack.hypervisor.xenserver.XenserverConfigs;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.SetupAnswer;
import com.cloud.agent.api.SetupCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.Config;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.DiscoveredWithErrorException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.HostEnvironment;
import com.cloud.host.HostInfo;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.xenserver.resource.CitrixHelper;
import com.cloud.hypervisor.xenserver.resource.CitrixResourceBase;
import com.cloud.hypervisor.xenserver.resource.XcpOssResource;
import com.cloud.hypervisor.xenserver.resource.XcpServerResource;
import com.cloud.hypervisor.xenserver.resource.XenServer56FP1Resource;
import com.cloud.hypervisor.xenserver.resource.XenServer56Resource;
import com.cloud.hypervisor.xenserver.resource.XenServer56SP2Resource;
import com.cloud.hypervisor.xenserver.resource.XenServer600Resource;
import com.cloud.hypervisor.xenserver.resource.XenServer610Resource;
import com.cloud.hypervisor.xenserver.resource.XenServer620Resource;
import com.cloud.hypervisor.xenserver.resource.XenServer620SP1Resource;
import com.cloud.hypervisor.xenserver.resource.XenServer650Resource;
import com.cloud.hypervisor.xenserver.resource.XenServerConnectionPool;
import com.cloud.hypervisor.xenserver.resource.Xenserver625Resource;
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.exception.HypervisorVersionChangedException;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.HostPatch;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.PoolPatch;
import com.xensource.xenapi.Session;
import com.xensource.xenapi.Types.SessionAuthenticationFailed;
import com.xensource.xenapi.Types.UuidInvalid;
import com.xensource.xenapi.Types.XenAPIException;


public class XcpServerDiscoverer extends DiscovererBase implements Discoverer, Listener, ResourceStateAdapter {
    private static final Logger s_logger = Logger.getLogger(XcpServerDiscoverer.class);
    private int _wait;
    private XenServerConnectionPool _connPool;
    private boolean _checkHvm;
    private boolean _setupMultipath;
    private String _instance;

    @Inject
    private AlertManager _alertMgr;
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private VMTemplateDao _tmpltDao;
    @Inject
    private HostPodDao _podDao;

    private String xenServerIsoName = "xs-tools.iso";
    private String xenServerIsoDisplayText = "XenServer Tools Installer ISO (xen-pv-drv-iso)";

    protected XcpServerDiscoverer() {
    }

    void setClusterGuid(ClusterVO cluster, String guid) {
        cluster.setGuid(guid);
        try {
            _clusterDao.update(cluster.getId(), cluster);
        } catch (EntityExistsException e) {
            QueryBuilder<ClusterVO> sc = QueryBuilder.create(ClusterVO.class);
            sc.and(sc.entity().getGuid(), Op.EQ, guid);
            List<ClusterVO> clusters = sc.list();
            ClusterVO clu = clusters.get(0);
            List<HostVO> clusterHosts = _resourceMgr.listAllHostsInCluster(clu.getId());
            if (clusterHosts == null || clusterHosts.size() == 0) {
                clu.setGuid(null);
                _clusterDao.update(clu.getId(), clu);
                _clusterDao.update(cluster.getId(), cluster);
                return;
            }
            throw e;
        }
    }

    protected boolean poolHasHotFix(Connection conn, String hostIp, String hotFixUuid) {
        try {
            Map<Host, Host.Record> hosts = Host.getAllRecords(conn);
            for (Map.Entry<Host, Host.Record> entry : hosts.entrySet()) {

                Host.Record re = entry.getValue();
                if (!re.address.equalsIgnoreCase(hostIp)){
                    continue;
                }
                Set<HostPatch> patches = re.patches;
                PoolPatch poolPatch = PoolPatch.getByUuid(conn, hotFixUuid);
                for(HostPatch patch : patches) {
                    PoolPatch pp = patch.getPoolPatch(conn);
                    if (pp != null && pp.equals(poolPatch) && patch.getApplied(conn)) {
                        s_logger.debug("host " + hostIp + " does have " + hotFixUuid +" Hotfix.");
                        return true;
                    }
                }
            }
            return false;
        } catch (UuidInvalid e) {
            s_logger.debug("host " + hostIp + " doesn't have " + hotFixUuid + " Hotfix");
        } catch (Exception e) {
            s_logger.debug("can't get patches information, consider it doesn't have " + hotFixUuid + " Hotfix");
        }
        return false;
    }



    @Override
    public Map<? extends ServerResource, Map<String, String>>
    find(long dcId, Long podId, Long clusterId, URI url, String username, String password, List<String> hostTags) throws DiscoveryException {
        Map<CitrixResourceBase, Map<String, String>> resources = new HashMap<CitrixResourceBase, Map<String, String>>();
        Connection conn = null;
        if (!url.getScheme().equals("http")) {
            String msg = "urlString is not http so we're not taking care of the discovery for this: " + url;
            s_logger.debug(msg);
            return null;
        }
        if (clusterId == null) {
            String msg = "must specify cluster Id when add host";
            s_logger.debug(msg);
            throw new RuntimeException(msg);
        }

        if (podId == null) {
            String msg = "must specify pod Id when add host";
            s_logger.debug(msg);
            throw new RuntimeException(msg);
        }

        ClusterVO cluster = _clusterDao.findById(clusterId);
        if (cluster == null || cluster.getHypervisorType() != HypervisorType.XenServer) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("invalid cluster id or cluster is not for XenServer hypervisors");
            }
            return null;
        }

        try {
            String hostname = url.getHost();
            InetAddress ia = InetAddress.getByName(hostname);
            String hostIp = ia.getHostAddress();
            Queue<String> pass = new LinkedList<String>();
            pass.add(password);
            conn = _connPool.getConnect(hostIp, username, pass);
            if (conn == null) {
                String msg = "Unable to get a connection to " + url;
                s_logger.debug(msg);
                throw new DiscoveryException(msg);
            }

            Set<Pool> pools = Pool.getAll(conn);
            Pool pool = pools.iterator().next();
            Pool.Record pr = pool.getRecord(conn);
            String poolUuid = pr.uuid;
            Map<Host, Host.Record> hosts = Host.getAllRecords(conn);
            String latestHotFix = "";
            if (poolHasHotFix(conn, hostIp, XenserverConfigs.XSHotFix62ESP1004)) {
                latestHotFix = XenserverConfigs.XSHotFix62ESP1004;
            } else if (poolHasHotFix(conn, hostIp, XenserverConfigs.XSHotFix62ESP1)) {
                latestHotFix = XenserverConfigs.XSHotFix62ESP1;
            }

            /*set cluster hypervisor type to xenserver*/
            ClusterVO clu = _clusterDao.findById(clusterId);
            if (clu.getGuid() == null) {
                setClusterGuid(clu, poolUuid);
            } else {
                List<HostVO> clusterHosts = _resourceMgr.listAllHostsInCluster(clusterId);
                if (clusterHosts != null && clusterHosts.size() > 0) {
                    if (!clu.getGuid().equals(poolUuid)) {
                        String msg = "Please join the host " +  hostIp + " to XS pool  "
                                + clu.getGuid() + " through XC/XS before adding it through CS UI";
                        s_logger.warn(msg);
                        throw new DiscoveryException(msg);
                    }
                } else {
                    setClusterGuid(clu, poolUuid);
                }
            }
            // can not use this conn after this point, because this host may join a pool, this conn is retired
            if (conn != null) {
                try {
                    Session.logout(conn);
                } catch (Exception e) {
                    s_logger.debug("Caught exception during logout", e);
                }
                conn.dispose();
                conn = null;
            }

            poolUuid = clu.getGuid();
            _clusterDao.update(clusterId, clu);

            if (_checkHvm) {
                for (Map.Entry<Host, Host.Record> entry : hosts.entrySet()) {
                    Host.Record record = entry.getValue();

                    boolean support_hvm = false;
                    for (String capability : record.capabilities) {
                        if (capability.contains("hvm")) {
                            support_hvm = true;
                            break;
                        }
                    }
                    if (!support_hvm) {
                        String msg = "Unable to add host " + record.address + " because it doesn't support hvm";
                        _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, dcId, podId, msg, msg);
                        s_logger.debug(msg);
                        throw new RuntimeException(msg);
                    }
                }
            }

            for (Map.Entry<Host, Host.Record> entry : hosts.entrySet()) {
                Host.Record record = entry.getValue();
                String hostAddr = record.address;

                String prodVersion = CitrixHelper.getProductVersion(record);
                String xenVersion = record.softwareVersion.get("xen");
                String hostOS = record.softwareVersion.get("product_brand");
                if (hostOS == null) {
                    hostOS = record.softwareVersion.get("platform_name");
                }

                String hostOSVer = prodVersion;
                String hostKernelVer = record.softwareVersion.get("linux");

                if (_resourceMgr.findHostByGuid(record.uuid) != null) {
                    s_logger.debug("Skipping " + record.address + " because " + record.uuid + " is already in the database.");
                    continue;
                }

                CitrixResourceBase resource = createServerResource(dcId, podId, record, latestHotFix);
                s_logger.info("Found host " + record.hostname + " ip=" + record.address + " product version=" + prodVersion);

                Map<String, String> details = new HashMap<String, String>();
                Map<String, Object> params = new HashMap<String, Object>();
                details.put("url", hostAddr);
                details.put("username", username);
                params.put("username", username);
                details.put("password", password);
                params.put("password", password);
                params.put("zone", Long.toString(dcId));
                params.put("guid", record.uuid);
                params.put("pod", podId.toString());
                params.put("cluster", clusterId.toString());
                params.put("pool", poolUuid);
                params.put("ipaddress", record.address);

                details.put(HostInfo.HOST_OS, hostOS);
                details.put(HostInfo.HOST_OS_VERSION, hostOSVer);
                details.put(HostInfo.HOST_OS_KERNEL_VERSION, hostKernelVer);
                details.put(HostInfo.HYPERVISOR_VERSION, xenVersion);

                String privateNetworkLabel = _networkMgr.getDefaultManagementTrafficLabel(dcId, HypervisorType.XenServer);
                String storageNetworkLabel = _networkMgr.getDefaultStorageTrafficLabel(dcId, HypervisorType.XenServer);

                if (!params.containsKey("private.network.device") && privateNetworkLabel != null) {
                    params.put("private.network.device", privateNetworkLabel);
                    details.put("private.network.device", privateNetworkLabel);
                }

                if (!params.containsKey("storage.network.device1") && storageNetworkLabel != null) {
                    params.put("storage.network.device1", storageNetworkLabel);
                    details.put("storage.network.device1", storageNetworkLabel);
                }

                DataCenterVO zone = _dcDao.findById(dcId);
                boolean securityGroupEnabled = zone.isSecurityGroupEnabled();
                params.put("securitygroupenabled", Boolean.toString(securityGroupEnabled));

                params.put("router.aggregation.command.each.timeout", _configDao.getValue(Config.RouterAggregationCommandEachTimeout.toString()));
                params.put("wait", Integer.toString(_wait));
                details.put("wait", Integer.toString(_wait));
                params.put("migratewait", _configDao.getValue(Config.MigrateWait.toString()));
                params.put(Config.XenServerMaxNics.toString().toLowerCase(), _configDao.getValue(Config.XenServerMaxNics.toString()));
                params.put(Config.XenServerHeartBeatTimeout.toString().toLowerCase(), _configDao.getValue(Config.XenServerHeartBeatTimeout.toString()));
                params.put(Config.XenServerHeartBeatInterval.toString().toLowerCase(), _configDao.getValue(Config.XenServerHeartBeatInterval.toString()));
                params.put(Config.InstanceName.toString().toLowerCase(), _instance);
                details.put(Config.InstanceName.toString().toLowerCase(), _instance);
                try {
                    resource.configure("XenServer", params);
                } catch (ConfigurationException e) {
                    _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, dcId, podId, "Unable to add " + record.address, "Error is " + e.getMessage());
                    s_logger.warn("Unable to instantiate " + record.address, e);
                    continue;
                }
                resource.start();
                resources.put(resource, details);
            }
        } catch (SessionAuthenticationFailed e) {
            throw new DiscoveredWithErrorException("Authentication error");
        } catch (XenAPIException e) {
            s_logger.warn("XenAPI exception", e);
            return null;
        } catch (XmlRpcException e) {
            s_logger.warn("Xml Rpc Exception", e);
            return null;
        } catch (UnknownHostException e) {
            s_logger.warn("Unable to resolve the host name", e);
            return null;
        } catch (Exception e) {
            s_logger.warn("other exceptions: " + e.toString(), e);
            return null;
        }
        return resources;
    }

    String getPoolUuid(Connection conn) throws XenAPIException, XmlRpcException {
        Map<Pool, Pool.Record> pools = Pool.getAllRecords(conn);
        assert pools.size() == 1 : "Pools size is " + pools.size();
        return pools.values().iterator().next().uuid;
    }

    protected void addSamePool(Connection conn, Map<CitrixResourceBase, Map<String, String>> resources) throws XenAPIException, XmlRpcException {
        Map<Pool, Pool.Record> hps = Pool.getAllRecords(conn);
        assert (hps.size() == 1) : "How can it be more than one but it's actually " + hps.size();

        // This is the pool.
        String poolUuid = hps.values().iterator().next().uuid;

        for (Map<String, String> details : resources.values()) {
            details.put("pool", poolUuid);
        }
    }

    protected CitrixResourceBase createServerResource(String prodBrand, String prodVersion, String prodVersionTextShort, String hotfix) {
        // Xen Cloud Platform group of hypervisors
        if (prodBrand.equals("XCP") && (prodVersion.equals("1.0.0") || prodVersion.equals("1.1.0")
                || prodVersion.equals("5.6.100") || prodVersion.startsWith("1.4") || prodVersion.startsWith("1.6"))) {
            return new XcpServerResource();
        } // Citrix Xenserver group of hypervisors
        else if (prodBrand.equals("XenServer") && prodVersion.equals("5.6.0")) {
            return new XenServer56Resource();
        } else if (prodBrand.equals("XenServer") && prodVersion.equals("6.0.0")) {
            return new XenServer600Resource();
        } else if (prodBrand.equals("XenServer") && prodVersion.equals("6.0.2")) {
            return new XenServer600Resource();
        } else if (prodBrand.equals("XenServer") && prodVersion.equals("6.1.0")) {
            return new XenServer610Resource();
        } else if (prodBrand.equals("XenServer") && prodVersion.equals("6.2.0")) {
            if (hotfix != null && hotfix.equals(XenserverConfigs.XSHotFix62ESP1004)) {
                return new Xenserver625Resource();
            } else if (hotfix != null && hotfix.equals(XenserverConfigs.XSHotFix62ESP1)) {
                return new XenServer620SP1Resource();
            } else {
                return new XenServer620Resource();
            }
        } else if (prodBrand.equals("XenServer") && prodVersion.equals("5.6.100")) {
            if ("5.6 SP2".equals(prodVersionTextShort.trim())) {
                return new XenServer56SP2Resource();
            } else if ("5.6 FP1".equals(prodVersionTextShort.trim())) {
                return new XenServer56FP1Resource();
            }
        } else if (prodBrand.equals("XCP_Kronos")) {
            return new XcpOssResource();
        } else if (prodBrand.equals("XenServer") || prodBrand.equals("XCP-ng") || prodBrand.equals("Citrix Hypervisor")) {
            final String[] items = prodVersion.split("\\.");
            if ((Integer.parseInt(items[0]) > 6) ||
                    (Integer.parseInt(items[0]) == 6 && Integer.parseInt(items[1]) >= 4)) {
                s_logger.warn("defaulting to xenserver650 resource for product brand: " + prodBrand + " with product " +
                        "version: " + prodVersion);
                //default to xenserver650 resource.
                return new XenServer650Resource();
            }
        }
        String msg =
                "Only support XCP 1.0.0, 1.1.0, 1.4.x, 1.5 beta, 1.6.x; XenServer 5.6,  XenServer 5.6 FP1, XenServer 5.6 SP2, Xenserver 6.0, 6.0.2, 6.1.0, 6.2.0, >6.4.0, Citrix Hypervisor > 8.0.0 but this one is " +
                        prodBrand + " " + prodVersion;
        s_logger.warn(msg);
        throw new RuntimeException(msg);
    }



    protected CitrixResourceBase createServerResource(long dcId, Long podId, Host.Record record, String hotfix) {
        String prodBrand = record.softwareVersion.get("product_brand");
        if (prodBrand == null) {
            prodBrand = record.softwareVersion.get("platform_name").trim();
        } else {
            prodBrand = prodBrand.trim();
        }
        String prodVersion = CitrixHelper.getProductVersion(record);

        String prodVersionTextShort = record.softwareVersion.get("product_version_text_short");
        return createServerResource(prodBrand, prodVersion, prodVersionTextShort, hotfix);
    }

    protected void serverConfig() {
        String value = _params.get(Config.XenServerSetupMultipath.key());
        _setupMultipath = Boolean.parseBoolean(value);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        serverConfig();

        String value = _params.get(Config.XapiWait.toString());
        _wait = NumbersUtil.parseInt(value, Integer.parseInt(Config.XapiWait.getDefaultValue()));

        _instance = _params.get(Config.InstanceName.key());

        value = _params.get("xenserver.check.hvm");
        _checkHvm = Boolean.parseBoolean(value);
        _connPool = XenServerConnectionPool.getInstance();

        _agentMgr.registerForHostEvents(this, true, false, true);

        createXenServerToolsIsoEntryInDatabase();
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    @Override
    public boolean matchHypervisor(String hypervisor) {
        if (hypervisor == null) {
            return true;
        }
        return Hypervisor.HypervisorType.XenServer.toString().equalsIgnoreCase(hypervisor);
    }

    @Override
    public Hypervisor.HypervisorType getHypervisorType() {
        return Hypervisor.HypervisorType.XenServer;
    }

    @Override
    public void postDiscovery(List<HostVO> hosts, long msId) throws DiscoveryException {
        //do nothing
    }

    @Override
    public int getTimeout() {
        return 0;
    }

    @Override
    public boolean isRecurring() {
        return false;
    }

    @Override
    public boolean processAnswers(long agentId, long seq, Answer[] answers) {
        return false;
    }

    @Override
    public boolean processCommands(long agentId, long seq, Command[] commands) {
        return false;
    }

    /**
     * Create the XenServer tools ISO entry in the database.
     * If there is already an entry with 'isoName' equals to {@value #xenServerIsoName} , we update its 'displayText' to {@value #xenServerIsoDisplayText}.
     * Otherwise, we create a new entry.
     */
    protected void createXenServerToolsIsoEntryInDatabase() {
        VMTemplateVO tmplt = _tmpltDao.findByTemplateName(xenServerIsoName);
        if (tmplt == null) {
            long id = _tmpltDao.getNextInSequence(Long.class, "id");
            VMTemplateVO template = VMTemplateVO.createPreHostIso(id, xenServerIsoName, xenServerIsoName, ImageFormat.ISO, true, true, TemplateType.PERHOST, null, null, true, 64,
                    Account.ACCOUNT_ID_SYSTEM, null, xenServerIsoDisplayText, false, 1, false, HypervisorType.XenServer);
            _tmpltDao.persist(template);
        } else {
            long id = tmplt.getId();
            tmplt.setTemplateType(TemplateType.PERHOST);
            tmplt.setUrl(null);
            tmplt.setDisplayText(xenServerIsoDisplayText);
            _tmpltDao.update(id, tmplt);
        }
    }

    @Override
    public void processHostAdded(long hostId) {
    }

    @Override
    public void processConnect(com.cloud.host.Host agent, StartupCommand cmd, boolean forRebalance) throws ConnectionException {
        if (!(cmd instanceof StartupRoutingCommand)) {
            return;
        }
        long agentId = agent.getId();

        StartupRoutingCommand startup = (StartupRoutingCommand)cmd;
        if (startup.getHypervisorType() != HypervisorType.XenServer) {
            s_logger.debug("Not XenServer so moving on.");
            return;
        }

        HostVO host = _hostDao.findById(agentId);

        ClusterVO cluster = _clusterDao.findById(host.getClusterId());
        if (cluster.getGuid() == null) {
            cluster.setGuid(startup.getPool());
            _clusterDao.update(cluster.getId(), cluster);
        } else if (!cluster.getGuid().equals(startup.getPool())) {
            String msg = "pool uuid for cluster " + cluster.getId() + " changed from " + cluster.getGuid() + " to " + startup.getPool();
            s_logger.warn(msg);
            throw new CloudRuntimeException(msg);
        }

        Map<String, String> details = startup.getHostDetails();
        String prodBrand = details.get("product_brand").trim();
        String prodVersion = details.get("product_version").trim();
        String hotfix = details.get(XenserverConfigs.XS620HotFix);
        String prodVersionTextShort = details.get("product_version_text_short");

        String resource = createServerResource(prodBrand, prodVersion, prodVersionTextShort, hotfix).getClass().getName();

        if (!resource.equals(host.getResource())) {
            String msg = "host " + host.getPrivateIpAddress() + " changed from " + host.getResource() + " to " + resource;
            s_logger.debug(msg);
            host.setResource(resource);
            host.setSetup(false);
            _hostDao.update(agentId, host);
            throw new HypervisorVersionChangedException(msg);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Setting up host " + agentId);
        }
        HostEnvironment env = new HostEnvironment();

        SetupCommand setup = new SetupCommand(env);
        if (_setupMultipath) {
            setup.setMultipathOn();
        }
        if (!host.isSetup()) {
            setup.setNeedSetup(true);
        }

        try {
            Answer answer = _agentMgr.send(agentId, setup);
            if (answer != null && answer.getResult() && answer instanceof SetupAnswer) {
                host.setSetup(true);
                host.setLastPinged((System.currentTimeMillis() >> 10) - 5 * 60);
                host.setHypervisorVersion(prodVersion);
                _hostDao.update(host.getId(), host);
                if (((SetupAnswer)answer).needReconnect()) {
                    throw new ConnectionException(false, "Reinitialize agent after setup.");
                }
                return;
            } else {
                s_logger.warn("Unable to setup agent " + agentId + " due to " + ((answer != null) ? answer.getDetails() : "return null"));
            }
        } catch (AgentUnavailableException e) {
            s_logger.warn("Unable to setup agent " + agentId + " because it became unavailable.", e);
        } catch (OperationTimedoutException e) {
            s_logger.warn("Unable to setup agent " + agentId + " because it timed out", e);
        }
        throw new ConnectionException(true, "Reinitialize agent after setup.");
    }

    @Override
    public AgentControlAnswer processControlCommand(long agentId, AgentControlCommand cmd) {
        return null;
    }

    @Override
    public boolean processDisconnect(long agentId, Status state) {
        return false;
    }

    @Override
    public void processHostAboutToBeRemoved(long hostId) {
    }

    @Override
    public void processHostRemoved(long hostId, long clusterId) {
    }

    @Override
    public boolean processTimeout(long agentId, long seq) {
        return false;
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
        if (ssCmd.getHypervisorType() != HypervisorType.XenServer) {
            return null;
        }

        HostPodVO pod = _podDao.findById(host.getPodId());
        DataCenterVO dc = _dcDao.findById(host.getDataCenterId());
        s_logger.info("Host: " + host.getName() + " connected with hypervisor type: " + HypervisorType.XenServer + ". Checking CIDR...");
        _resourceMgr.checkCIDR(pod, dc, ssCmd.getPrivateIpAddress(), ssCmd.getPrivateNetmask());
        return _resourceMgr.fillRoutingHostVO(host, ssCmd, HypervisorType.XenServer, details, hostTags);
    }

    @Override
    public DeleteHostAnswer deleteHost(HostVO host, boolean isForced, boolean isForceDeleteStorage) throws UnableDeleteHostException {
        if (host.getType() != com.cloud.host.Host.Type.Routing || host.getHypervisorType() != HypervisorType.XenServer) {
            return null;
        }

        _resourceMgr.deleteRoutingHost(host, isForced, isForceDeleteStorage);
        return new DeleteHostAnswer(true);
    }

    @Override
    protected HashMap<String, Object> buildConfigParams(HostVO host) {
        HashMap<String, Object> params = super.buildConfigParams(host);
        DataCenterVO zone = _dcDao.findById(host.getDataCenterId());
        if (zone != null) {
            boolean securityGroupEnabled = zone.isSecurityGroupEnabled();
            params.put("securitygroupenabled", Boolean.toString(securityGroupEnabled));
        }
        return params;
    }

    @Override
    public boolean stop() {
        _resourceMgr.unregisterResourceStateAdapter(this.getClass().getSimpleName());
        return super.stop();
    }
}
