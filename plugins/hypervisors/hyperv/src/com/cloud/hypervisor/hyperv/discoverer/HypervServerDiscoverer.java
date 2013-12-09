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
package com.cloud.hypervisor.hyperv.discoverer;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.ejb.Local;
import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreManager;
import org.apache.cloudstack.utils.identity.ManagementServerNode;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.SetupAnswer;
import com.cloud.agent.api.SetupCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.Config;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.ConnectionException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.host.Host;
import com.cloud.host.HostEnvironment;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.hyperv.resource.HypervDirectConnectResource;
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ResourceStateAdapter;
import com.cloud.resource.ServerResource;
import com.cloud.resource.UnableDeleteHostException;
import com.cloud.storage.JavaStorageLayer;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.FileUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.GlobalLock;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

/**
 * Methods to discover and managem a Hyper-V agent. Prepares a
 * HypervDirectConnectResource corresponding to the agent on a Hyper-V
 * hypervisor and manages its lifecycle.
 */
@Local(value = Discoverer.class)
public class HypervServerDiscoverer extends DiscovererBase implements Discoverer, Listener, ResourceStateAdapter {
    private static final Logger s_logger = Logger.getLogger(HypervServerDiscoverer.class);

    private String _instance;
    private String _mountParent;
    private int _timeout;
    Random _rand = new Random(System.currentTimeMillis());

    Map<String, String> _storageMounts = new HashMap<String, String>();
    StorageLayer _storage;

    @Inject
    private HostDao _hostDao = null;
    @Inject
    private ClusterDao _clusterDao;
    @Inject
    private ClusterDetailsDao _clusterDetailsDao;
    @Inject
    private ResourceManager _resourceMgr;
    @Inject
    private HostPodDao _podDao;
    @Inject
    private DataCenterDao _dcDao;
    @Inject
    DataStoreManager _dataStoreMgr;

    // TODO: AgentManager and AlertManager not being used to transmit info,
    // may want to reconsider.
    @Inject
    private AgentManager _agentMgr;
    @Inject
    private AlertManager _alertMgr;

    // Listener interface methods

    @Override
    public final boolean processAnswers(final long agentId, final long seq, final Answer[] answers) {
        return false;
    }

    @Override
    public final boolean processCommands(final long agentId, final long seq, final Command[] commands) {
        return false;
    }

    @Override
    public final AgentControlAnswer processControlCommand(final long agentId, final AgentControlCommand cmd) {
        return null;
    }

    @Override
    public final void processConnect(final Host agent, final StartupCommand cmd, final boolean forRebalance) throws ConnectionException {
        // Limit the commands we can process
        if (!(cmd instanceof StartupRoutingCommand)) {
            return;
        }

        StartupRoutingCommand startup = (StartupRoutingCommand)cmd;

        // assert
        if (startup.getHypervisorType() != HypervisorType.Hyperv) {
            s_logger.debug("Not Hyper-V hypervisor, so moving on.");
            return;
        }

        long agentId = agent.getId();
        HostVO host = _hostDao.findById(agentId);

        // Our Hyper-V machines are not participating in pools, and the pool id
        // we provide them is not persisted.
        // This means the pool id can vary.
        ClusterVO cluster = _clusterDao.findById(host.getClusterId());
        if (cluster.getGuid() == null) {
            cluster.setGuid(startup.getPool());
            _clusterDao.update(cluster.getId(), cluster);
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Setting up host " + agentId);
        }

        String secondaryStorageUri = getSecondaryStorageStoreUrl(cluster.getDataCenterId());
        if (secondaryStorageUri == null) {
            s_logger.debug("Secondary storage uri for dc " + cluster.getDataCenterId() + " couldn't be obtained");
        } else {
            prepareSecondaryStorageStore(secondaryStorageUri);
        }

        HostEnvironment env = new HostEnvironment();
        SetupCommand setup = new SetupCommand(env);
        setup.setSecondaryStorage(secondaryStorageUri);
        setup.setSystemVmIso("systemvm/" + getSystemVMIsoFileNameOnDatastore());
        if (!host.isSetup()) {
            setup.setNeedSetup(true);
        }

        try {
            SetupAnswer answer = (SetupAnswer)_agentMgr.send(agentId, setup);
            if (answer != null && answer.getResult()) {
                host.setSetup(true);
                // TODO: clean up magic numbers below
                host.setLastPinged((System.currentTimeMillis() >> 10) - 5 * 60);
                _hostDao.update(host.getId(), host);
                if (answer.needReconnect()) {
                    throw new ConnectionException(false, "Reinitialize agent after setup.");
                }
                return;
            } else {
                String reason = answer.getDetails();
                if (reason == null) {
                    reason = " details were null";
                }
                s_logger.warn("Unable to setup agent " + agentId + " due to " + reason);
            }
            // Error handling borrowed from XcpServerDiscoverer, may need to be
            // updated.
        } catch (AgentUnavailableException e) {
            s_logger.warn("Unable to setup agent " + agentId + " because it became unavailable.", e);
        } catch (OperationTimedoutException e) {
            s_logger.warn("Unable to setup agent " + agentId + " because it timed out", e);
        }
        throw new ConnectionException(true, "Reinitialize agent after setup.");
    }

    @Override
    public final boolean processDisconnect(final long agentId, final Status state) {
        return false;
    }

    @Override
    public final boolean isRecurring() {
        return false;
    }

    @Override
    public final int getTimeout() {
        return 0;
    }

    @Override
    public final boolean processTimeout(final long agentId, final long seq) {
        return false;
    }

    // End Listener implementation

    // Returns server component used by server manager to operate the plugin.
    // Server component is a ServerResource. If a connected agent is used, the
    // ServerResource is
    // ignored in favour of another created in response to
    @Override
    public final Map<? extends ServerResource, Map<String, String>> find(final long dcId, final Long podId, final Long clusterId, final URI uri, final String username,
        final String password, final List<String> hostTags) throws DiscoveryException {

        if (s_logger.isInfoEnabled()) {
            s_logger.info("Discover host. dc(zone): " + dcId + ", pod: " + podId + ", cluster: " + clusterId + ", uri host: " + uri.getHost());
        }

        // Assertions
        if (podId == null) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("No pod is assigned, skipping the discovery in" + " Hyperv discoverer");
            }
            return null;
        }
        ClusterVO cluster = _clusterDao.findById(clusterId); // ClusterVO exists
        // in the
        // database
        if (cluster == null) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("No cluster in database for cluster id " + clusterId);
            }
            return null;
        }
        if (cluster.getHypervisorType() != HypervisorType.Hyperv) {
            if (s_logger.isInfoEnabled()) {
                s_logger.info("Cluster " + clusterId + "is not for Hyperv hypervisors");
            }
            return null;
        }
        if (!uri.getScheme().equals("http")) {
            String msg = "urlString is not http so we're not taking care of" + " the discovery for this: " + uri;
            s_logger.debug(msg);
            return null;
        }

        try {
            String hostname = uri.getHost();
            InetAddress ia = InetAddress.getByName(hostname);
            String agentIp = ia.getHostAddress();
            String uuidSeed = agentIp;
            String guidWithTail = calcServerResourceGuid(uuidSeed) + "-HypervResource";

            if (_resourceMgr.findHostByGuid(guidWithTail) != null) {
                s_logger.debug("Skipping " + agentIp + " because " + guidWithTail + " is already in the database.");
                return null;
            }

            s_logger.info("Creating" + HypervDirectConnectResource.class.getName() + " HypervDummyResourceBase for zone/pod/cluster " + dcId + "/" + podId + "/" +
                clusterId);

            // Some Hypervisors organise themselves in pools.
            // The startup command tells us what pool they are using.
            // In the meantime, we have to place a GUID corresponding to the
            // pool in the database
            // This GUID may change.
            if (cluster.getGuid() == null) {
                cluster.setGuid(UUID.nameUUIDFromBytes(String.valueOf(clusterId).getBytes()).toString());
                _clusterDao.update(clusterId, cluster);
            }

            // Settings required by all server resources managing a hypervisor
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("zone", Long.toString(dcId));
            params.put("pod", Long.toString(podId));
            params.put("cluster", Long.toString(clusterId));
            params.put("guid", guidWithTail);
            params.put("ipaddress", agentIp);
            params.put("sec.storage.url", getSecondaryStorageStoreUrl(dcId));

            // Hyper-V specific settings
            Map<String, String> details = new HashMap<String, String>();
            details.put("url", uri.getHost());
            details.put("username", username);
            details.put("password", password);
            details.put("cluster.guid", cluster.getGuid());

            params.putAll(details);

            HypervDirectConnectResource resource = new HypervDirectConnectResource();
            resource.configure(agentIp, params);

            // Assert
            // TODO: test by using bogus URL and bogus virtual path in URL
            ReadyCommand ping = new ReadyCommand();
            Answer pingAns = resource.executeRequest(ping);
            if (pingAns == null || !pingAns.getResult()) {
                String errMsg = "Agent not running, or no route to agent on at " + uri;
                s_logger.debug(errMsg);
                throw new DiscoveryException(errMsg);
            }

            Map<HypervDirectConnectResource, Map<String, String>> resources = new HashMap<HypervDirectConnectResource, Map<String, String>>();
            resources.put(resource, details);

            // TODO: does the resource have to create a connection?
            return resources;
        } catch (ConfigurationException e) {
            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, dcId, podId, "Unable to add " + uri.getHost(), "Error is " + e.getMessage());
            s_logger.warn("Unable to instantiate " + uri.getHost(), e);
        } catch (UnknownHostException e) {
            _alertMgr.sendAlert(AlertManager.AlertType.ALERT_TYPE_HOST, dcId, podId, "Unable to add " + uri.getHost(), "Error is " + e.getMessage());

            s_logger.warn("Unable to instantiate " + uri.getHost(), e);
        } catch (Exception e) {
            String msg = " can't setup agent, due to " + e.toString() + " - " + e.getMessage();
            s_logger.warn(msg);
        }
        return null;
    }

    private void prepareSecondaryStorageStore(String storageUrl) {
        String mountPoint = getMountPoint(storageUrl);

        GlobalLock lock = GlobalLock.getInternLock("prepare.systemvm");
        try {
            if (lock.lock(3600)) {
                try {
                    File patchFolder = new File(mountPoint + "/systemvm");
                    if (!patchFolder.exists()) {
                        if (!patchFolder.mkdirs()) {
                            String msg = "Unable to create systemvm folder on secondary storage. location: " + patchFolder.toString();
                            s_logger.error(msg);
                            throw new CloudRuntimeException(msg);
                        }
                    }

                    File srcIso = getSystemVMPatchIsoFile();
                    File destIso = new File(mountPoint + "/systemvm/" + getSystemVMIsoFileNameOnDatastore());
                    if (!destIso.exists()) {
                        s_logger.info("Copy System VM patch ISO file to secondary storage. source ISO: " + srcIso.getAbsolutePath() + ", destination: " +
                            destIso.getAbsolutePath());
                        try {
                            FileUtil.copyfile(srcIso, destIso);
                        } catch (IOException e) {
                            s_logger.error("Unexpected exception ", e);

                            String msg = "Unable to copy systemvm ISO on secondary storage. src location: " + srcIso.toString() + ", dest location: " + destIso;
                            s_logger.error(msg);
                            throw new CloudRuntimeException(msg);
                        }
                    } else {
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("SystemVM ISO file " + destIso.getPath() + " already exists");
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        } finally {
            lock.releaseRef();
        }
    }

    private String getMountPoint(String storageUrl) {
        String mountPoint = null;
        synchronized (_storageMounts) {
            mountPoint = _storageMounts.get(storageUrl);
            if (mountPoint != null) {
                return mountPoint;
            }

            URI uri;
            try {
                uri = new URI(storageUrl);
            } catch (URISyntaxException e) {
                s_logger.error("Invalid storage URL format ", e);
                throw new CloudRuntimeException("Unable to create mount point due to invalid storage URL format " + storageUrl);
            }

            mountPoint = mount(File.separator + File.separator + uri.getHost() + uri.getPath(), _mountParent, uri.getScheme(), uri.getQuery());
            if (mountPoint == null) {
                s_logger.error("Unable to create mount point for " + storageUrl);
                return "/mnt/sec";
            }

            _storageMounts.put(storageUrl, mountPoint);
            return mountPoint;
        }
    }

    protected String mount(String path, String parent, String scheme, String query) {
        String mountPoint = setupMountPoint(parent);
        if (mountPoint == null) {
            s_logger.warn("Unable to create a mount point");
            return null;
        }

        Script script = null;
        String result = null;
        if (scheme.equals("cifs")) {
            Script command = new Script(true, "mount", _timeout, s_logger);
            command.add("-t", "cifs");
            command.add(path);
            command.add(mountPoint);

            if (query != null) {
                query = query.replace('&', ',');
                command.add("-o", query);
            }
            result = command.execute();
        }

        if (result != null) {
            s_logger.warn("Unable to mount " + path + " due to " + result);
            File file = new File(mountPoint);
            if (file.exists()) {
                file.delete();
            }
            return null;
        }

        // Change permissions for the mountpoint
        script = new Script(true, "chmod", _timeout, s_logger);
        script.add("-R", "777", mountPoint);
        result = script.execute();
        if (result != null) {
            s_logger.warn("Unable to set permissions for " + mountPoint + " due to " + result);
        }
        return mountPoint;
    }

    private String setupMountPoint(String parent) {
        String mountPoint = null;
        long mshostId = ManagementServerNode.getManagementServerId();
        for (int i = 0; i < 10; i++) {
            String mntPt = parent + File.separator + String.valueOf(mshostId) + "." + Integer.toHexString(_rand.nextInt(Integer.MAX_VALUE));
            File file = new File(mntPt);
            if (!file.exists()) {
                if (_storage.mkdir(mntPt)) {
                    mountPoint = mntPt;
                    break;
                }
            }
            s_logger.error("Unable to create mount: " + mntPt);
        }

        return mountPoint;
    }

    private String getSystemVMIsoFileNameOnDatastore() {
        String version = this.getClass().getPackage().getImplementationVersion();
        String fileName = "systemvm-" + version + ".iso";
        return fileName.replace(':', '-');
    }

    private File getSystemVMPatchIsoFile() {
        // locate systemvm.iso
        URL url = this.getClass().getClassLoader().getResource("vms/systemvm.iso");
        File isoFile = null;
        if (url != null) {
            isoFile = new File(url.getPath());
        }

        if (isoFile == null || !isoFile.exists()) {
            isoFile = new File("/usr/share/cloudstack-common/vms/systemvm.iso");
        }

        assert (isoFile != null);
        if (!isoFile.exists()) {
            s_logger.error("Unable to locate systemvm.iso in your setup at " + isoFile.toString());
        }
        return isoFile;
    }

    private String getSecondaryStorageStoreUrl(long zoneId) {
        String secUrl = null;
        DataStore secStore = _dataStoreMgr.getImageStore(zoneId);
        if (secStore != null) {
            secUrl = secStore.getUri();
        }

        if (secUrl == null) {
            s_logger.warn("Secondary storage uri couldn't be retrieved");
        }

        return secUrl;
    }

    /**
     * Encapsulate GUID calculation in public method to allow access to test
     * programs. Works by converting a string to a GUID using
     * UUID.nameUUIDFromBytes
     *
     * @param uuidSeed
     *            string to use to generate GUID
     *
     * @return GUID in form of a string.
     */
    public static String calcServerResourceGuid(final String uuidSeed) {
        String guid = UUID.nameUUIDFromBytes(uuidSeed.getBytes()).toString();
        return guid;
    }

    // Adapter implementation: (facilitates plug in loading)
    // Required because Discoverer extends Adapter
    // Overrides Adapter.configure to always return true
    // Inherit Adapter.getName
    // Inherit Adapter.stop
    // Inherit Adapter.start
    @Override
    public final boolean configure(final String name, final Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        _mountParent = (String)params.get(Config.MountParent.key());
        if (_mountParent == null) {
            _mountParent = File.separator + "mnt";
        }

        if (_instance != null) {
            _mountParent = _mountParent + File.separator + _instance;
        }

        String value = (String)params.get("scripts.timeout");
        _timeout = NumbersUtil.parseInt(value, 30) * 1000;

        _storage = (StorageLayer)params.get(StorageLayer.InstanceConfigKey);
        if (_storage == null) {
            _storage = new JavaStorageLayer();
            _storage.configure("StorageLayer", params);
        }

        // TODO: allow timeout on we HTTPRequests to be configured
        _agentMgr.registerForHostEvents(this, true, false, true);
        _resourceMgr.registerResourceStateAdapter(this.getClass().getSimpleName(), this);
        return true;
    }

    // end of Adapter

    @Override
    public void postDiscovery(final List<HostVO> hosts, final long msId) throws DiscoveryException {
    }

    @Override
    public final Hypervisor.HypervisorType getHypervisorType() {
        return Hypervisor.HypervisorType.Hyperv;
    }

    // TODO: verify that it is okay to return true on null hypervisor
    @Override
    public final boolean matchHypervisor(final String hypervisor) {
        if (hypervisor == null) {
            return true;
        }
        return Hypervisor.HypervisorType.Hyperv.toString().equalsIgnoreCase(hypervisor);
    }

    // end of Discoverer

    // ResourceStateAdapter
    @Override
    public final HostVO createHostVOForConnectedAgent(final HostVO host, final StartupCommand[] cmd) {
        return null;
    }

    // TODO: add test for method
    @Override
    public final HostVO createHostVOForDirectConnectAgent(final HostVO host, final StartupCommand[] startup, final ServerResource resource,
        final Map<String, String> details, final List<String> hostTags) {
        StartupCommand firstCmd = startup[0];
        if (!(firstCmd instanceof StartupRoutingCommand)) {
            return null;
        }

        StartupRoutingCommand ssCmd = ((StartupRoutingCommand)firstCmd);
        if (ssCmd.getHypervisorType() != HypervisorType.Hyperv) {
            return null;
        }

        s_logger.info("Host: " + host.getName() + " connected with hypervisor type: " + HypervisorType.Hyperv + ". Checking CIDR...");

        HostPodVO pod = _podDao.findById(host.getPodId());
        DataCenterVO dc = _dcDao.findById(host.getDataCenterId());

        _resourceMgr.checkCIDR(pod, dc, ssCmd.getPrivateIpAddress(), ssCmd.getPrivateNetmask());

        return _resourceMgr.fillRoutingHostVO(host, ssCmd, HypervisorType.Hyperv, details, hostTags);
    }

    // TODO: add test for method
    @Override
    public final DeleteHostAnswer deleteHost(final HostVO host, final boolean isForced, final boolean isForceDeleteStorage) throws UnableDeleteHostException {
        // assert
        if (host.getType() != Host.Type.Routing || host.getHypervisorType() != HypervisorType.Hyperv) {
            return null;
        }
        _resourceMgr.deleteRoutingHost(host, isForced, isForceDeleteStorage);
        return new DeleteHostAnswer(true);
    }

    @Override
    public final boolean stop() {
        _resourceMgr.unregisterResourceStateAdapter(this.getClass().getSimpleName());
        return super.stop();
    }
    // end of ResourceStateAdapter

}
