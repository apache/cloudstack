/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.resource;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.AgentManager;
import com.cloud.agent.manager.AgentAttache;
import com.cloud.api.commands.AddClusterCmd;
import com.cloud.api.commands.AddHostCmd;
import com.cloud.api.commands.AddSecondaryStorageCmd;
import com.cloud.api.commands.CancelMaintenanceCmd;
import com.cloud.api.commands.DeleteClusterCmd;
import com.cloud.api.commands.PrepareForMaintenanceCmd;
import com.cloud.api.commands.ReconnectHostCmd;
import com.cloud.api.commands.UpdateHostCmd;
import com.cloud.api.commands.UpdateHostPasswordCmd;
import com.cloud.cluster.ManagementServerNode;
import com.cloud.dc.ClusterDetailsDao;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.HostPodVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.exception.AgentUnavailableException;
import com.cloud.exception.DiscoveryException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.host.Host;
import com.cloud.host.Host.HostAllocationState;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.host.dao.HostDetailsDao;
import com.cloud.host.dao.HostTagsDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.kvm.resource.KvmDummyResourceBase;
import com.cloud.org.Cluster;
import com.cloud.org.Grouping;
import com.cloud.org.Managed;
import com.cloud.storage.GuestOSCategoryVO;
import com.cloud.storage.StorageManager;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.dao.GuestOSCategoryDao;
import com.cloud.storage.dao.StoragePoolDao;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.user.UserContext;
import com.cloud.utils.UriUtils;
import com.cloud.utils.component.Adapters;
import com.cloud.utils.component.Inject;
import com.cloud.utils.component.Manager;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

@Local({ ResourceManager.class, ResourceService.class })
public class ResourceManagerImpl implements ResourceManager, ResourceService, Manager {
    private static final Logger s_logger = Logger.getLogger(ResourceManagerImpl.class);

    private String                           _name;

    @Inject
    AccountManager                           _accountMgr;
    @Inject
    AgentManager                             _agentMgr;
    @Inject
    StorageManager                           _storageMgr;
    @Inject
    protected SecondaryStorageVmManager      _secondaryStorageMgr;

    @Inject
    protected DataCenterDao                  _dcDao;
    @Inject
    protected HostPodDao                     _podDao;
    @Inject
    protected ClusterDetailsDao              _clusterDetailsDao;
    @Inject
    protected ClusterDao                     _clusterDao;
    @Inject
    protected HostDao                        _hostDao;
    @Inject
    protected HostDetailsDao                 _hostDetailsDao;
    @Inject
    protected HostTagsDao                    _hostTagsDao;    
    @Inject
    protected GuestOSCategoryDao             _guestOSCategoryDao;
    @Inject 
    protected StoragePoolDao                _storagePoolDao;

    @Inject(adapter = Discoverer.class)
    protected Adapters<? extends Discoverer> _discoverers;

    protected long                           _nodeId  = ManagementServerNode.getManagementServerId();

    protected HashMap<Integer, List<ResourceListener>> _lifeCycleListeners = new HashMap<Integer, List<ResourceListener>>();

    private void insertListener(Integer event, ResourceListener listener) {
        List<ResourceListener> lst = _lifeCycleListeners.get(event);
        if (lst == null) {
            lst = new ArrayList<ResourceListener>();
            _lifeCycleListeners.put(event, lst);
        }
        
        if (lst.contains(listener)) {
            throw new CloudRuntimeException("Duplicate resource lisener:" + listener.getClass().getSimpleName());
        }
        
        lst.add(listener);
    }
    
    @Override
    public void registerResourceEvent(Integer event, ResourceListener listener) {
        synchronized (_lifeCycleListeners) {
            if ((event & ResourceListener.EVENT_DISCOVER_BEFORE) != 0) {
                insertListener(ResourceListener.EVENT_DISCOVER_BEFORE, listener);
            }
            if ((event & ResourceListener.EVENT_DISCOVER_AFTER) != 0) {
                insertListener(ResourceListener.EVENT_DISCOVER_AFTER, listener);
            }
            if ((event & ResourceListener.EVENT_DELETE_HOST_BEFORE) != 0) {
                insertListener(ResourceListener.EVENT_DELETE_HOST_BEFORE, listener);
            }
            if ((event & ResourceListener.EVENT_DELETE_HOST_AFTER) != 0) {
                insertListener(ResourceListener.EVENT_DELETE_HOST_AFTER, listener);
            }
            if ((event & ResourceListener.EVENT_CANCEL_MAINTENANCE_BEFORE) != 0) {
                insertListener(ResourceListener.EVENT_CANCEL_MAINTENANCE_BEFORE, listener);
            }
            if ((event & ResourceListener.EVENT_CANCEL_MAINTENANCE_AFTER) != 0) {
                insertListener(ResourceListener.EVENT_CANCEL_MAINTENANCE_AFTER, listener);
            }
            if ((event & ResourceListener.EVENT_PREPARE_MAINTENANCE_BEFORE) != 0) {
                insertListener(ResourceListener.EVENT_PREPARE_MAINTENANCE_BEFORE, listener);
            }
            if ((event & ResourceListener.EVENT_PREPARE_MAINTENANCE_AFTER) != 0) {
                insertListener(ResourceListener.EVENT_PREPARE_MAINTENANCE_AFTER, listener);
            }
        }
    }
    
    @Override
    public void unregisterResourceEvent(ResourceListener listener) {
        synchronized (_lifeCycleListeners) {
            Iterator it = _lifeCycleListeners.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, List<ResourceListener>> items = (Map.Entry<Integer, List<ResourceListener>>)it.next();
                List<ResourceListener> lst = items.getValue();
                lst.remove(listener);
            }
        }
    }
    
    protected void processResourceEvent(Integer event, Object...params) {
        List<ResourceListener> lst = _lifeCycleListeners.get(event);
        if (lst == null || lst.size() == 0) {
            return;
        }

        String eventName;
        for (ResourceListener l : lst) {
            if (event == ResourceListener.EVENT_DISCOVER_BEFORE) {
                l.processDiscoverEventBefore((Long) params[0], (Long) params[1], (Long) params[2], (URI) params[3], (String) params[4], (String) params[5],
                        (List<String>) params[6]);
                eventName = "EVENT_DISCOVER_BEFORE";
            } else if (event == ResourceListener.EVENT_DISCOVER_AFTER) {
                l.processDiscoverEventAfter((Map<? extends ServerResource, Map<String, String>>) params[0]);
                eventName = "EVENT_DISCOVER_AFTER";
            } else if (event == ResourceListener.EVENT_DELETE_HOST_BEFORE) {
                l.processDeleteHostEventBefore((HostVO) params[0]);
                eventName = "EVENT_DELETE_HOST_BEFORE";
            } else if (event == ResourceListener.EVENT_DELETE_HOST_AFTER) {
                l.processDeletHostEventAfter((HostVO) params[0]);
                eventName = "EVENT_DELETE_HOST_AFTER";
            } else if (event == ResourceListener.EVENT_CANCEL_MAINTENANCE_BEFORE) {
                l.processCancelMaintenaceEventBefore((Long) params[0]);
                eventName = "EVENT_CANCEL_MAINTENANCE_BEFORE";
            } else if (event == ResourceListener.EVENT_CANCEL_MAINTENANCE_AFTER) {
                l.processCancelMaintenaceEventAfter((Long) params[0]);
                eventName = "EVENT_CANCEL_MAINTENANCE_AFTER";
            } else if (event == ResourceListener.EVENT_PREPARE_MAINTENANCE_BEFORE) {
                l.processPrepareMaintenaceEventBefore((Long) params[0]);
                eventName = "EVENT_PREPARE_MAINTENANCE_BEFORE";
            } else if (event == ResourceListener.EVENT_PREPARE_MAINTENANCE_AFTER) {
                l.processPrepareMaintenaceEventAfter((Long) params[0]);
                eventName = "EVENT_PREPARE_MAINTENANCE_AFTER";
            } else {
                throw new CloudRuntimeException("Unknown resource event:" + event);
            }
            s_logger.debug("Sent resource event " + eventName + " to listener " + l.getClass().getSimpleName());
        }
        
    }

    @Override
    public boolean updateHostPassword(UpdateHostPasswordCmd cmd) {
        return _agentMgr.updateHostPassword(cmd);
    }

    @Override
    public List<? extends Cluster> discoverCluster(AddClusterCmd cmd) throws IllegalArgumentException, DiscoveryException {
        Long dcId = cmd.getZoneId();
        Long podId = cmd.getPodId();
        String clusterName = cmd.getClusterName();
        String url = cmd.getUrl();
        String username = cmd.getUsername();
        String password = cmd.getPassword();

        if (url != null) {
            url = URLDecoder.decode(url);
        }

        URI uri = null;

        // Check if the zone exists in the system
        DataCenterVO zone = _dcDao.findById(dcId);
        if (zone == null) {
            throw new InvalidParameterValueException("Can't find zone by id " + dcId);
        }

        Account account = UserContext.current().getCaller();
        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(account.getType())) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + dcId);
        }

        // Check if the pod exists in the system
        if (podId != null) {
            if (_podDao.findById(podId) == null) {
                throw new InvalidParameterValueException("Can't find pod by id " + podId);
            }
            // check if pod belongs to the zone
            HostPodVO pod = _podDao.findById(podId);
            if (!Long.valueOf(pod.getDataCenterId()).equals(dcId)) {
                throw new InvalidParameterValueException("Pod " + podId + " doesn't belong to the zone " + dcId);
            }
        }

        // Verify cluster information and create a new cluster if needed
        if (clusterName == null || clusterName.isEmpty()) {
            throw new InvalidParameterValueException("Please specify cluster name");
        }

        if (cmd.getHypervisor() == null || cmd.getHypervisor().isEmpty()) {
            throw new InvalidParameterValueException("Please specify a hypervisor");
        }

        Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.getType(cmd.getHypervisor());
        if (hypervisorType == null) {
            s_logger.error("Unable to resolve " + cmd.getHypervisor() + " to a valid supported hypervisor type");
            throw new InvalidParameterValueException("Unable to resolve " + cmd.getHypervisor() + " to a supported ");
        }

        Cluster.ClusterType clusterType = null;
        if (cmd.getClusterType() != null && !cmd.getClusterType().isEmpty()) {
            clusterType = Cluster.ClusterType.valueOf(cmd.getClusterType());
        }
        if (clusterType == null) {
            clusterType = Cluster.ClusterType.CloudManaged;
        }

        Grouping.AllocationState allocationState = null;
        if (cmd.getAllocationState() != null && !cmd.getAllocationState().isEmpty()) {
            try {
                allocationState = Grouping.AllocationState.valueOf(cmd.getAllocationState());
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("Unable to resolve Allocation State '" + cmd.getAllocationState() + "' to a supported state");
            }
        }
        if (allocationState == null) {
            allocationState = Grouping.AllocationState.Enabled;
        }

        Discoverer discoverer = getMatchingDiscover(hypervisorType);
        if (discoverer == null) {

            throw new InvalidParameterValueException("Could not find corresponding resource manager for " + cmd.getHypervisor());
        }

        List<ClusterVO> result = new ArrayList<ClusterVO>();

        long clusterId = 0;
        ClusterVO cluster = new ClusterVO(dcId, podId, clusterName);
        cluster.setHypervisorType(cmd.getHypervisor());

        cluster.setClusterType(clusterType);
        cluster.setAllocationState(allocationState);
        try {
            cluster = _clusterDao.persist(cluster);
        } catch (Exception e) {
            // no longer tolerate exception during the cluster creation phase
            throw new CloudRuntimeException("Unable to create cluster " + clusterName + " in pod " + podId + " and data center " + dcId, e);
        }
        clusterId = cluster.getId();
        result.add(cluster);

        if (clusterType == Cluster.ClusterType.CloudManaged) {
            return result;
        }

        // save cluster details for later cluster/host cross-checking
        Map<String, String> details = new HashMap<String, String>();
        details.put("url", url);
        details.put("username", username);
        details.put("password", password);
        _clusterDetailsDao.persist(cluster.getId(), details);

        boolean success = false;
        try {
            try {
                uri = new URI(UriUtils.encodeURIComponent(url));
                if (uri.getScheme() == null) {
                    throw new InvalidParameterValueException("uri.scheme is null " + url + ", add http:// as a prefix");
                } else if (uri.getScheme().equalsIgnoreCase("http")) {
                    if (uri.getHost() == null || uri.getHost().equalsIgnoreCase("") || uri.getPath() == null || uri.getPath().equalsIgnoreCase("")) {
                        throw new InvalidParameterValueException("Your host and/or path is wrong.  Make sure it's of the format http://hostname/path");
                    }
                }
            } catch (URISyntaxException e) {
                throw new InvalidParameterValueException(url + " is not a valid uri");
            }

            List<HostVO> hosts = new ArrayList<HostVO>();
            Map<? extends ServerResource, Map<String, String>> resources = null;
            resources = discoverer.find(dcId, podId, clusterId, uri, username, password, null);
            
            if (resources != null) {
                for (Map.Entry<? extends ServerResource, Map<String, String>> entry : resources.entrySet()) {
                    ServerResource resource = entry.getKey();

                    // For Hyper-V, we are here means agent have already started and connected to management server
                    if (hypervisorType == Hypervisor.HypervisorType.Hyperv) {
                        break;
                    }

                    AgentAttache attache = _agentMgr.simulateStart(null, resource, entry.getValue(), true, null, null, false);
                    if (attache != null) {
                        hosts.add(_hostDao.findById(attache.getId()));
                    }
                    discoverer.postDiscovery(hosts, _nodeId);
                }
                s_logger.info("External cluster has been successfully discovered by " + discoverer.getName());
                success = true;
                return result;
            }

            s_logger.warn("Unable to find the server resources at " + url);
            throw new DiscoveryException("Unable to add the external cluster");
        } finally {
            if (!success) {
                _clusterDetailsDao.deleteDetails(clusterId);
                _clusterDao.remove(clusterId);
            }
        }
    }

    private Discoverer getMatchingDiscover(Hypervisor.HypervisorType hypervisorType) {
        Enumeration<? extends Discoverer> en = _discoverers.enumeration();
        while (en.hasMoreElements()) {
            Discoverer discoverer = en.nextElement();
            if (discoverer.getHypervisorType() == hypervisorType) {
                return discoverer;
            }
        }
        return null;
    }

    @Override
    public List<? extends Host> discoverHosts(AddHostCmd cmd) throws IllegalArgumentException, DiscoveryException, InvalidParameterValueException {
        Long dcId = cmd.getZoneId();
        Long podId = cmd.getPodId();
        Long clusterId = cmd.getClusterId();
        String clusterName = cmd.getClusterName();
        String url = cmd.getUrl();
        String username = cmd.getUsername();
        String password = cmd.getPassword();
        List<String> hostTags = cmd.getHostTags();

        dcId = _accountMgr.checkAccessAndSpecifyAuthority(UserContext.current().getCaller(), dcId);

        // this is for standalone option
        if (clusterName == null && clusterId == null) {
            clusterName = "Standalone-" + url;
        }

        if (clusterId != null) {
            ClusterVO cluster = _clusterDao.findById(clusterId);
            if (cluster == null) {
                throw new InvalidParameterValueException("can not fine cluster for clusterId " + clusterId);
            } else {
                if (cluster.getGuid() == null) {
                    List<HostVO> hosts = _hostDao.listByCluster(clusterId);
                    if (!hosts.isEmpty()) {
                        throw new CloudRuntimeException("Guid is not updated for cluster " + clusterId + " need to wait hosts in this cluster up");
                    }
                }
            }
        }

        String allocationState = cmd.getAllocationState();
        if (allocationState == null) {
            allocationState = Host.HostAllocationState.Enabled.toString();
        }

        return discoverHostsFull(dcId, podId, clusterId, clusterName, url, username, password, cmd.getHypervisor(), hostTags, cmd.getFullUrlParams(), allocationState);
    }

    @Override
    public List<? extends Host> discoverHosts(AddSecondaryStorageCmd cmd) throws IllegalArgumentException, DiscoveryException, InvalidParameterValueException {
        Long dcId = cmd.getZoneId();
        String url = cmd.getUrl();
        return discoverHostsFull(dcId, null, null, null, url, null, null, "SecondaryStorage", null, null, null);
    }
    
    private List<HostVO> discoverHostsFull(Long dcId, Long podId, Long clusterId, String clusterName, String url, String username, String password, String hypervisorType, List<String> hostTags,
            Map<String, String> params, String allocationState) throws IllegalArgumentException, DiscoveryException, InvalidParameterValueException {
        URI uri = null;

        // Check if the zone exists in the system
        DataCenterVO zone = _dcDao.findById(dcId);
        if (zone == null) {
            throw new InvalidParameterValueException("Can't find zone by id " + dcId);
        }

        Account account = UserContext.current().getCaller();
        if (Grouping.AllocationState.Disabled == zone.getAllocationState() && !_accountMgr.isRootAdmin(account.getType())) {
            throw new PermissionDeniedException("Cannot perform this operation, Zone is currently disabled: " + dcId);
        }

        // Check if the pod exists in the system
        if (podId != null) {
            if (_podDao.findById(podId) == null) {
                throw new InvalidParameterValueException("Can't find pod by id " + podId);
            }
            // check if pod belongs to the zone
            HostPodVO pod = _podDao.findById(podId);
            if (!Long.valueOf(pod.getDataCenterId()).equals(dcId)) {
                throw new InvalidParameterValueException("Pod " + podId + " doesn't belong to the zone " + dcId);
            }
        }

        // Verify cluster information and create a new cluster if needed
        if (clusterName != null && clusterId != null) {
            throw new InvalidParameterValueException("Can't specify cluster by both id and name");
        }

        if (hypervisorType == null || hypervisorType.isEmpty()) {
            throw new InvalidParameterValueException("Need to specify Hypervisor Type");
        }

        if ((clusterName != null || clusterId != null) && podId == null) {
            throw new InvalidParameterValueException("Can't specify cluster without specifying the pod");
        }

        if (clusterId != null) {
            if (_clusterDao.findById(clusterId) == null) {
                throw new InvalidParameterValueException("Can't find cluster by id " + clusterId);
            }
            
            if(hypervisorType.equalsIgnoreCase(HypervisorType.VMware.toString())) {
	            // VMware only allows adding host to an existing cluster, as we already have a lot of information
	            // in cluster object, to simplify user input, we will construct neccessary information here
	            Map<String, String> clusterDetails = this._clusterDetailsDao.findDetails(clusterId);
	            username = clusterDetails.get("username");
	            assert(username != null);
	            
	            password = clusterDetails.get("password");
	            assert(password != null);
	            
                try {
                    uri = new URI(UriUtils.encodeURIComponent(url));
                
                    url = clusterDetails.get("url") + "/" + uri.getHost();
                } catch (URISyntaxException e) {
                    throw new InvalidParameterValueException(url + " is not a valid uri");
                }
            }
        }

        if (clusterName != null) {
            ClusterVO cluster = new ClusterVO(dcId, podId, clusterName);
            cluster.setHypervisorType(hypervisorType);
            try {
                cluster = _clusterDao.persist(cluster);
            } catch (Exception e) {
                cluster = _clusterDao.findBy(clusterName, podId);
                if (cluster == null) {
                    throw new CloudRuntimeException("Unable to create cluster " + clusterName + " in pod " + podId + " and data center " + dcId, e);
                }
            }
            clusterId = cluster.getId();
        }

        try {
            uri = new URI(UriUtils.encodeURIComponent(url));
            if (uri.getScheme() == null) {
                throw new InvalidParameterValueException("uri.scheme is null " + url + ", add nfs:// as a prefix");
            } else if (uri.getScheme().equalsIgnoreCase("nfs")) {
                if (uri.getHost() == null || uri.getHost().equalsIgnoreCase("") || uri.getPath() == null || uri.getPath().equalsIgnoreCase("")) {
                    throw new InvalidParameterValueException("Your host and/or path is wrong.  Make sure it's of the format nfs://hostname/path");
                }
            }
        } catch (URISyntaxException e) {
            throw new InvalidParameterValueException(url + " is not a valid uri");
        }

        List<HostVO> hosts = new ArrayList<HostVO>();
        s_logger.info("Trying to add a new host at " + url + " in data center " + dcId);
        Enumeration<? extends Discoverer> en = _discoverers.enumeration();
        boolean isHypervisorTypeSupported = false;
        while (en.hasMoreElements()) {
            Discoverer discoverer = en.nextElement();
            if (params != null) {
                discoverer.putParam(params);
            }

            if (!discoverer.matchHypervisor(hypervisorType)) {
                continue;
            }
            isHypervisorTypeSupported = true;
            Map<? extends ServerResource, Map<String, String>> resources = null;

            processResourceEvent(ResourceListener.EVENT_DISCOVER_BEFORE, dcId, podId, clusterId, uri, username, password, hostTags);
            try {
                resources = discoverer.find(dcId, podId, clusterId, uri, username, password, hostTags);
            } catch(DiscoveryException e) {
            	throw e;
            } catch (Exception e) {
                s_logger.info("Exception in host discovery process with discoverer: " + discoverer.getName() + ", skip to another discoverer if there is any");
            }
            processResourceEvent(ResourceListener.EVENT_DISCOVER_AFTER, resources);
            
            if (resources != null) {
                for (Map.Entry<? extends ServerResource, Map<String, String>> entry : resources.entrySet()) {
                    ServerResource resource = entry.getKey();
                    /*
                     * For KVM, if we go to here, that means kvm agent is already connected to mgt svr.
                     */
                    if (resource instanceof KvmDummyResourceBase) {
                        Map<String, String> details = entry.getValue();
                        String guid = details.get("guid");
                        List<HostVO> kvmHosts = _hostDao.listBy(Host.Type.Routing, clusterId, podId, dcId);
                        for (HostVO host : kvmHosts) {
                            if (host.getGuid().equalsIgnoreCase(guid)) {
                                if(hostTags != null){
                                    if(s_logger.isTraceEnabled()){
                                        s_logger.trace("Adding Host Tags for KVM host, tags:  :"+hostTags);
                                    }
                                    _hostTagsDao.persist(host.getId(), hostTags);
                                }
                                hosts.add(host);
                                return hosts;
                            }
                        }
                        return null;
                    }
                    AgentAttache attache = _agentMgr.simulateStart(null, resource, entry.getValue(), true, hostTags, allocationState, false);
                    if (attache != null) {
                        hosts.add(_hostDao.findById(attache.getId()));
                    }
                    discoverer.postDiscovery(hosts, _nodeId);

                }
                s_logger.info("server resources successfully discovered by " + discoverer.getName());
                return hosts;
            }
        }
        if (!isHypervisorTypeSupported) {
            String msg = "Do not support HypervisorType " + hypervisorType + " for " + url;
            s_logger.warn(msg);
            throw new DiscoveryException(msg);
        }
        s_logger.warn("Unable to find the server resources at " + url);
        throw new DiscoveryException("Unable to add the host");
    }

    @Override
    public Host getHost(long hostId) {
        return _hostDao.findById(hostId);
    }

    @Override
    public boolean deleteHost(long hostId, boolean isForced, boolean forceDestroy) {
        User caller = _accountMgr.getActiveUser(UserContext.current().getCallerUserId());
        // Verify that host exists
        HostVO host = _hostDao.findById(hostId);
        if (host == null) {
            throw new InvalidParameterValueException("Host with id " + hostId + " doesn't exist");
        }
        _accountMgr.checkAccessAndSpecifyAuthority(UserContext.current().getCaller(), host.getDataCenterId());
        
        processResourceEvent(ResourceListener.EVENT_DELETE_HOST_BEFORE, host);
        boolean res = false;
        if (Host.Type.SecondaryStorage.equals(host.getType())) {
            _secondaryStorageMgr.deleteHost(hostId);
            res = true;
        } else {
            res = _agentMgr.deleteHost(hostId, isForced, forceDestroy, caller);
        }
        processResourceEvent(ResourceListener.EVENT_DELETE_HOST_AFTER, host);
        return res;
    }

    @Override
    @DB
    public boolean deleteCluster(DeleteClusterCmd cmd) {
        Transaction txn = Transaction.currentTxn();
        try {
            txn.start();
            ClusterVO cluster = _clusterDao.lockRow(cmd.getId(), true);
            if (cluster == null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Cluster: " + cmd.getId() + " does not even exist.  Delete call is ignored.");
                }
                txn.rollback();
                return true;
            }

            //don't allow to remove the cluster if it has non-removed hosts
            List<HostVO> hosts = _hostDao.listByCluster(cmd.getId());
            if (hosts.size() > 0) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Cluster: " + cmd.getId() + " still has hosts, can't remove");
                }
                txn.rollback();
                return false;
            }
            
            //don't allow to remove the cluster if it has non-removed storage pools
            List<StoragePoolVO> storagePools = _storagePoolDao.listPoolsByCluster(cmd.getId());
            if (storagePools.size() > 0) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Cluster: " + cmd.getId() + " still has storage pools, can't remove");
                }
                txn.rollback();
                return false;
            }

            _clusterDao.remove(cmd.getId());

            txn.commit();
            return true;
        } catch (Throwable t) {
            s_logger.error("Unable to delete cluster: " + cmd.getId(), t);
            txn.rollback();
            return false;
        }
    }

    @Override
    @DB
    public Cluster updateCluster(Cluster clusterToUpdate, String clusterType, String hypervisor, String allocationState, String managedstate) {

        ClusterVO cluster = (ClusterVO) clusterToUpdate;
        // Verify cluster information and update the cluster if needed
        boolean doUpdate = false;

        if (hypervisor != null && !hypervisor.isEmpty()) {
            Hypervisor.HypervisorType hypervisorType = Hypervisor.HypervisorType.getType(hypervisor);
            if (hypervisorType == null) {
                s_logger.error("Unable to resolve " + hypervisor + " to a valid supported hypervisor type");
                throw new InvalidParameterValueException("Unable to resolve " + hypervisor + " to a supported type");
            } else {
                cluster.setHypervisorType(hypervisor);
                doUpdate = true;
            }
        }

        Cluster.ClusterType newClusterType = null;
        if (clusterType != null && !clusterType.isEmpty()) {
            try {
                newClusterType = Cluster.ClusterType.valueOf(clusterType);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("Unable to resolve " + clusterType + " to a supported type");
            }
            if (newClusterType == null) {
                s_logger.error("Unable to resolve " + clusterType + " to a valid supported cluster type");
                throw new InvalidParameterValueException("Unable to resolve " + clusterType + " to a supported type");
            } else {
                cluster.setClusterType(newClusterType);
                doUpdate = true;
            }
        }

        Grouping.AllocationState newAllocationState = null;
        if (allocationState != null && !allocationState.isEmpty()) {
            try {
                newAllocationState = Grouping.AllocationState.valueOf(allocationState);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("Unable to resolve Allocation State '" + allocationState + "' to a supported state");
            }
            if (newAllocationState == null) {
                s_logger.error("Unable to resolve " + allocationState + " to a valid supported allocation State");
                throw new InvalidParameterValueException("Unable to resolve " + allocationState + " to a supported state");
            } else {
                cluster.setAllocationState(newAllocationState);
                doUpdate = true;
            }
        }
        
        Managed.ManagedState newManagedState = null;
        Managed.ManagedState oldManagedState = cluster.getManagedState();
        if (managedstate != null && !managedstate.isEmpty()) {
            try {
                newManagedState = Managed.ManagedState.valueOf(managedstate);
            } catch (IllegalArgumentException ex) {
                throw new InvalidParameterValueException("Unable to resolve Managed State '" + managedstate + "' to a supported state");
            }
            if (newManagedState == null) {
                s_logger.error("Unable to resolve Managed State '" + managedstate + "' to a supported state");
                throw new InvalidParameterValueException("Unable to resolve Managed State '" + managedstate + "' to a supported state");
            } else {
                doUpdate = true;
            }
        }
        
        if (doUpdate) {
            Transaction txn = Transaction.currentTxn();
            try {
                txn.start();
                _clusterDao.update(cluster.getId(), cluster);
                txn.commit();               
            } catch (Exception e) {
                s_logger.error("Unable to update cluster due to " + e.getMessage(), e);
                throw new CloudRuntimeException("Failed to update cluster. Please contact Cloud Support.");
            }
        }
        
        if( newManagedState != null && !newManagedState.equals(oldManagedState)) {
            Transaction txn = Transaction.currentTxn();
            if( newManagedState.equals(Managed.ManagedState.Unmanaged) ) {
                boolean success = false;
                try {
                    txn.start();
                    cluster.setManagedState(Managed.ManagedState.PrepareUnmanaged);
                    _clusterDao.update(cluster.getId(), cluster);
                    txn.commit();
                    List<HostVO>  hosts = _hostDao.listBy(Host.Type.Routing, cluster.getId(), cluster.getPodId(), cluster.getDataCenterId());                  
                    for( HostVO host : hosts ) {
                        if(host.getType().equals(Host.Type.Routing) && !host.getStatus().equals(Status.Down) &&  !host.getStatus().equals(Status.Disconnected) 
                                && !host.getStatus().equals(Status.Up) && !host.getStatus().equals(Status.Alert) ) {
                            String msg = "host " + host.getPrivateIpAddress() + " should not be in " + host.getStatus().toString() + " status";
                            throw new CloudRuntimeException("PrepareUnmanaged Failed due to " + msg);                                   
                         }
                    }
                    
                    for( HostVO host : hosts ) {
                        if ( host.getStatus().equals(Status.Up )) {
                            _agentMgr.disconnect(host.getId());
                        }
                    }
                    int retry = 10;
                    boolean lsuccess = true;
                    for ( int i = 0; i < retry; i++) {
                        lsuccess = true;
                        try {
                            Thread.sleep(20 * 1000);
                        } catch (Exception e) {
                        }
                        hosts = _hostDao.listBy(Host.Type.Routing, cluster.getId(), cluster.getPodId(), cluster.getDataCenterId()); 
                        for( HostVO host : hosts ) {
                            if ( !host.getStatus().equals(Status.Down) && !host.getStatus().equals(Status.Disconnected) 
                                    && !host.getStatus().equals(Status.Alert)) {
                                lsuccess = false;
                                break;
                            }
                        }
                        if( lsuccess == true ) {
                            success = true;
                            break;
                        }
                    }
                    if ( success == false ) {
                        throw new CloudRuntimeException("PrepareUnmanaged Failed due to some hosts are still in UP status after 5 Minutes, please try later "); 
                    }
                } finally {
                    txn.start();
                    cluster.setManagedState(success? Managed.ManagedState.Unmanaged : Managed.ManagedState.PrepareUnmanagedError);
                    _clusterDao.update(cluster.getId(), cluster);
                    txn.commit();
                }
            } else if( newManagedState.equals(Managed.ManagedState.Managed)) {               
                txn.start();
                cluster.setManagedState(Managed.ManagedState.Managed);
                _clusterDao.update(cluster.getId(), cluster);
                txn.commit();
            }
            
        }
        
        return cluster;
    }

    @Override
    public Host cancelMaintenance(CancelMaintenanceCmd cmd) {
        Long hostId = cmd.getId();

        // verify input parameters
        HostVO host = _hostDao.findById(hostId);
        if (host == null || host.getRemoved() != null) {
            throw new InvalidParameterValueException("Host with id " + hostId.toString() + " doesn't exist");
        }

        processResourceEvent(ResourceListener.EVENT_CANCEL_MAINTENANCE_BEFORE, hostId);
        boolean success = _agentMgr.cancelMaintenance(hostId);
        processResourceEvent(ResourceListener.EVENT_CANCEL_MAINTENANCE_AFTER, hostId);
        if (!success) {
            throw new CloudRuntimeException("Internal error cancelling maintenance.");
        }
        return host;
    }

    @Override
    public Host reconnectHost(ReconnectHostCmd cmd) throws AgentUnavailableException {
        Long hostId = cmd.getId();

        HostVO host = _hostDao.findById(hostId);
        if (host == null) {
            throw new InvalidParameterValueException("Host with id " + hostId.toString() + " doesn't exist");
        }

        boolean result = _agentMgr.reconnect(hostId);
        if (result) {
            return host;
        }
        throw new CloudRuntimeException("Failed to reconnect host with id " + hostId.toString() + ", internal error.");
    }

    @Override
    public Host maintain(PrepareForMaintenanceCmd cmd) {
        Long hostId = cmd.getId();
        HostVO host = _hostDao.findById(hostId);

        if (host == null) {
            s_logger.debug("Unable to find host " + hostId);
            throw new InvalidParameterValueException("Unable to find host with ID: " + hostId + ". Please specify a valid host ID.");
        }

        if (_hostDao.countBy(host.getClusterId(), Status.PrepareForMaintenance, Status.ErrorInMaintenance) > 0) {
            throw new InvalidParameterValueException("There are other servers in PrepareForMaintenance OR ErrorInMaintenance STATUS in cluster " + host.getClusterId());
        }

        if (_storageMgr.isLocalStorageActiveOnHost(host)) {
            throw new InvalidParameterValueException("There are active VMs using the host's local storage pool. Please stop all VMs on this host that use local storage.");
        }

        try {
            processResourceEvent(ResourceListener.EVENT_PREPARE_MAINTENANCE_BEFORE, hostId);
            if (_agentMgr.maintain(hostId)) {
                processResourceEvent(ResourceListener.EVENT_CANCEL_MAINTENANCE_AFTER, hostId);
                return _hostDao.findById(hostId);
            } else {
                throw new CloudRuntimeException("Unable to prepare for maintenance host " + hostId);
            }
        } catch (AgentUnavailableException e) {
            throw new CloudRuntimeException("Unable to prepare for maintenance host " + hostId);
        }
    }

    @Override
    public Host updateHost(UpdateHostCmd cmd) {
        Long hostId = cmd.getId();
        Long guestOSCategoryId = cmd.getOsCategoryId();

        if (guestOSCategoryId != null) {

            // Verify that the host exists
            HostVO host = _hostDao.findById(hostId);
            if (host == null) {
                throw new InvalidParameterValueException("Host with id " + hostId + " doesn't exist");
            }

            // Verify that the guest OS Category exists
            if (guestOSCategoryId > 0) {
                if (_guestOSCategoryDao.findById(guestOSCategoryId) == null) {
                    throw new InvalidParameterValueException("Please specify a valid guest OS category.");
                }
            }

            GuestOSCategoryVO guestOSCategory = _guestOSCategoryDao.findById(guestOSCategoryId);
            Map<String, String> hostDetails = _hostDetailsDao.findDetails(hostId);

            if (guestOSCategory != null) {
                // Save a new entry for guest.os.category.id
                hostDetails.put("guest.os.category.id", String.valueOf(guestOSCategory.getId()));
            } else {
                // Delete any existing entry for guest.os.category.id
                hostDetails.remove("guest.os.category.id");
            }
            _hostDetailsDao.persist(hostId, hostDetails);
        }

        String allocationState = cmd.getAllocationState();
        if (allocationState != null) {
            // Verify that the host exists
            HostVO host = _hostDao.findById(hostId);
            if (host == null) {
                throw new InvalidParameterValueException("Host with id " + hostId + " doesn't exist");
            }

            try {
                HostAllocationState newAllocationState = Host.HostAllocationState.valueOf(allocationState);
                if (newAllocationState == null) {
                    s_logger.error("Unable to resolve " + allocationState + " to a valid supported allocation State");
                    throw new InvalidParameterValueException("Unable to resolve " + allocationState + " to a supported state");
                } else {
                    host.setHostAllocationState(newAllocationState);
                }
            } catch (IllegalArgumentException ex) {
                s_logger.error("Unable to resolve " + allocationState + " to a valid supported allocation State");
                throw new InvalidParameterValueException("Unable to resolve " + allocationState + " to a supported state");
            }

            _hostDao.update(hostId, host);
        }
        
        List<String> hostTags = cmd.getHostTags();
        if (hostTags != null) {
            // Verify that the host exists
            HostVO host = _hostDao.findById(hostId);
            if (host == null) {
                throw new InvalidParameterValueException("Host with id " + hostId + " doesn't exist");
            }
            if(s_logger.isDebugEnabled()){
                s_logger.debug("Updating Host Tags to :"+hostTags);
            }
            _hostTagsDao.persist(hostId, hostTags);
        }
        
        String url = cmd.getUrl();
        if (url != null) {
        	_storageMgr.updateSecondaryStorage(cmd.getId(), cmd.getUrl());
        }

        HostVO updatedHost = _hostDao.findById(hostId);
        return updatedHost;
    }

    @Override
    public Cluster getCluster(Long clusterId) {
        return _clusterDao.findById(clusterId);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

}
