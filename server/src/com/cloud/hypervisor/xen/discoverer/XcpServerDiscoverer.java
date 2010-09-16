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
package com.cloud.hypervisor.xen.discoverer;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.agent.AgentManager;
import com.cloud.agent.Listener;
import com.cloud.agent.api.AgentControlAnswer;
import com.cloud.agent.api.AgentControlCommand;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.alert.AlertManager;
import com.cloud.configuration.Config;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDao;
import com.cloud.exception.DiscoveryException;
import com.cloud.host.HostInfo;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.xen.resource.CitrixResourceBase;
import com.cloud.hypervisor.xen.resource.XcpServerResource;
import com.cloud.hypervisor.xen.resource.XenServerConnectionPool;
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.resource.ServerResource;
import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.Storage.FileSystem;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateHostDao;
import com.cloud.storage.template.TemplateInfo;
import com.cloud.user.Account;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.Inject;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.Session;
import com.xensource.xenapi.Types.SessionAuthenticationFailed;
import com.xensource.xenapi.Types.XenAPIException;

@Local(value=Discoverer.class)
public class XcpServerDiscoverer extends DiscovererBase implements Discoverer, Listener {
    private static final Logger s_logger = Logger.getLogger(XcpServerDiscoverer.class);
    protected String _publicNic;
    protected String _privateNic;
    protected String _storageNic1;
    protected String _storageNic2;
    protected int _wait;
    protected XenServerConnectionPool _connPool;
    protected String _increase;
    protected boolean _checkHvm;
    protected String _guestNic;

    @Inject protected AlertManager _alertMgr;
    @Inject protected AgentManager _agentMgr;
    @Inject protected HostDao _hostDao;
    @Inject VMTemplateDao _tmpltDao;
    @Inject VMTemplateHostDao _vmTemplateHostDao;
    @Inject ClusterDao _clusterDao;
    
    protected XcpServerDiscoverer() {
    }
    
    @Override
    public Map<? extends ServerResource, Map<String, String>> find(long dcId, Long podId, Long clusterId, URI url, String username, String password) throws DiscoveryException {
        Map<CitrixResourceBase, Map<String, String>> resources = new HashMap<CitrixResourceBase, Map<String, String>>();
        Connection conn = null;
        Connection slaveConn = null;
        if (!url.getScheme().equals("http")) {
            String msg = "urlString is not http so we're not taking care of the discovery for this: " + url;
            s_logger.debug(msg);
            return null;
        }
        try {

            String hostname = url.getHost();
            InetAddress ia = InetAddress.getByName(hostname);
            String addr = ia.getHostAddress();
            
            conn = _connPool.masterConnect(addr, username, password);
            
            if (conn == null) {
                String msg = "Unable to get a connection to " + url;
                s_logger.debug(msg);
                throw new RuntimeException(msg);
            }
            
            String pod;
            if (podId == null) {
                Map<Pool, Pool.Record> pools = Pool.getAllRecords(conn);
                assert pools.size() == 1 : "Pools are not one....where on earth have i been? " + pools.size();
                
                pod = pools.values().iterator().next().uuid;
            } else {
                pod = Long.toString(podId);
            }
            String cluster = null;
            if (clusterId != null) {
                cluster = Long.toString(clusterId);
            }
            Set<Pool> pools = Pool.getAll(conn);
            Pool pool = pools.iterator().next();
            Pool.Record pr = pool.getRecord(conn);
            String poolUuid = pr.uuid;
            Map<Host, Host.Record> hosts = Host.getAllRecords(conn);
            Host master = pr.master;
           
            if (_checkHvm) {
                for (Map.Entry<Host, Host.Record> entry : hosts.entrySet()) {
                    Host.Record record = entry.getValue();
                    boolean support_hvm = false;
                    for ( String capability : record.capabilities ) {
                        if(capability.contains("hvm")) {
                           support_hvm = true;
                           break;
                        }
                    } 
                    if( !support_hvm ) {
                        String msg = "Unable to add host " + record.address + " because it doesn't support hvm";
                        _alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, dcId, podId, msg, msg);
                        s_logger.debug(msg);
                        throw new RuntimeException(msg);
                    }
                }
            }

            for (Map.Entry<Host, Host.Record> entry : hosts.entrySet()) {
                Host.Record record = entry.getValue();
                String hostAddr = record.address;
                
                String prodVersion = record.softwareVersion.get("product_version");
                String xenVersion = record.softwareVersion.get("xen");
                String hostOS = record.softwareVersion.get("product_brand");
                String hostOSVer = prodVersion;
                String hostKernelVer = record.softwareVersion.get("linux");

                if (_hostDao.findByGuid(record.uuid) != null) {
                    s_logger.debug("Skipping " + record.address + " because " + record.uuid + " is already in the database.");
                    continue;
                }                

                CitrixResourceBase resource = createServerResource(dcId, podId, record);
                s_logger.info("Found host " + record.hostname + " ip=" + record.address + " product version=" + prodVersion);
                            
                Map<String, String> details = new HashMap<String, String>();
                Map<String, Object> params = new HashMap<String, Object>();
                details.put("url", hostAddr);
                params.put("url", hostAddr);
                details.put("pool", poolUuid);
                params.put("pool", poolUuid);
                details.put("username", username);
                params.put("username", username);
                details.put("password", password);
                params.put("password", password);
                params.put("zone", Long.toString(dcId));
                params.put("guid", record.uuid);
                params.put("pod", pod);
                params.put("cluster", cluster);
                if (_increase != null) {
                    params.put(Config.XenPreallocatedLunSizeRange.name(), _increase);
                }
                details.put(HostInfo.HOST_OS, hostOS);
                details.put(HostInfo.HOST_OS_VERSION, hostOSVer);
                details.put(HostInfo.HOST_OS_KERNEL_VERSION, hostKernelVer);
                details.put(HostInfo.HYPERVISOR_VERSION, xenVersion);

                if (!params.containsKey("public.network.device") && _publicNic != null) {
                    params.put("public.network.device", _publicNic);
                    details.put("public.network.device", _publicNic);
                }
                
                if (!params.containsKey("guest.network.device") && _guestNic != null) {
                    params.put("guest.network.device", _guestNic);
                    details.put("guest.network.device", _guestNic);
                }
                
                if (!params.containsKey("private.network.device") && _privateNic != null) {
                    params.put("private.network.device", _privateNic);
                    details.put("private.network.device", _privateNic);
                }
                
                if (!params.containsKey("storage.network.device1") && _storageNic1 != null) {
                    params.put("storage.network.device1", _storageNic1);
                    details.put("storage.network.device1", _storageNic1);
                }
                
                if (!params.containsKey("storage.network.device2") && _storageNic2 != null) {
                    params.put("storage.network.device2", _storageNic2);
                    details.put("storage.network.device2", _storageNic2);
                }
                
                
                params.put(Config.Wait.toString().toLowerCase(), Integer.toString(_wait));
                details.put(Config.Wait.toString().toLowerCase(), Integer.toString(_wait));
                try {
                    resource.configure("Xen Server", params);
                } catch (ConfigurationException e) {
                    _alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, dcId, podId, "Unable to add " + record.address, "Error is " + e.getMessage());
                    s_logger.warn("Unable to instantiate " + record.address, e);
                    continue;
                }

                resource.start();
                resources.put(resource, details);
            }
            
            if (!addHostsToPool(url, conn, dcId, podId, clusterId, resources)) {
                return null;
            }
        } catch (SessionAuthenticationFailed e) {
            s_logger.warn("Authentication error", e);
            return null;
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
        	s_logger.debug("other exceptions: " + e.toString());
        	return null;
        }
        finally {
            if (conn != null) {
                try{
                    Session.logout(conn);
                } catch (Exception e ) {
                }
                conn.dispose();
            }
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
    
    protected boolean addHostsToPool(URI url, Connection conn, long dcId, Long podId, Long clusterId, Map<? extends CitrixResourceBase, Map<String, String>> resources) throws XenAPIException, XmlRpcException, DiscoveryException {
        if ( resources.size() == 0 ) {
            return false;
        }
        if (clusterId == null ) {
            if (resources.size() > 1) {
                s_logger.warn("There's no cluster specified but we found a pool of xenservers " + resources.size());
                throw new DiscoveryException("There's no cluster specified but we found a pool of xenservers " + resources.size());
            } else if (resources.size() == 1) {
                s_logger.debug("No cluster specified and we found only one host so no pool");
                return true;
            } 
        }
        
        List<HostVO> hosts;
        String poolLabel;
        String poolDescription;
        if (clusterId != null) {
            hosts = _hostDao.listByCluster(clusterId);
            ClusterVO cluster = _clusterDao.findById(clusterId);
            poolLabel = "cluster-" + clusterId;
            poolDescription = cluster.getName();
        } else if (podId != null) {
            hosts = _hostDao.listByHostPod(podId);
            poolLabel = "pod-" + podId;
            poolDescription = "Auto-Created Pool from Pod";
        } else {
            hosts= new ArrayList<HostVO>();
            poolLabel = "cluster-self-created";
            poolDescription = "Auto-Created Pool";
        }
        
        if (hosts.size() == 0) {
            Set<Pool> pools = Pool.getAll(conn);
            Pool pool = pools.iterator().next();
            pool.setNameLabel(conn, poolLabel);
            pool.setNameDescription(conn, poolDescription);
            return true;
        }
/*
        if (hosts.size() + resources.size() > 16) {
            s_logger.debug("A pool can only have 16 hosts");
            throw new DiscoveryException("A XenServer cluster can only have 16 hosts maximum");
        }
  */
        String poolUuid1 = null;
        String poolMaster = null;
        String username = null;
        String password = null;
        String address = null;
        for (HostVO host : hosts) {
            _hostDao.loadDetails(host);
            username = host.getDetail("username");
            password = host.getDetail("password");
            address = host.getDetail("url");
            Connection hostConn = _connPool.slaveConnect(address, username, password);
            if (hostConn == null) {
                continue;
            }
            try {
                Set<Pool> pools = Pool.getAll(hostConn);
                Pool pool = pools.iterator().next();
                poolUuid1 = pool.getUuid(hostConn);
                poolMaster = pool.getMaster(hostConn).getAddress(hostConn);
                break;

            } catch (Exception e ) {
                s_logger.warn("Can not get master ip address from host " + address);
            }
            finally {
                try{
                    Session.localLogout(hostConn);
                } catch (Exception e ) {
                }
                hostConn.dispose();
                hostConn = null;
                poolMaster = null;
                poolUuid1 = null;
            }
        }
        
        if (poolMaster == null) {
            s_logger.warn("Unable to reach the pool master of the existing cluster");
            throw new DiscoveryException("Unable to reach the pool master of the existing cluster");
        }
        
        Set<Pool> pools = Pool.getAll(conn);
        Pool pool = pools.iterator().next();
        String poolUuid2 = pool.getUuid(conn);
        if (resources.size() > 1 && !poolUuid1.equals(poolUuid2)) {
            s_logger.debug("Can't add a pool of servers into an existing pool");
            throw new DiscoveryException("Can't add a pool of servers into an existing pool");
        }
        
        if (poolUuid1.equals(poolUuid2)) {
            s_logger.debug("The hosts that are discovered are already in the same pool as existing hosts");
            return true;
        }
       
        CitrixResourceBase resource = resources.keySet().iterator().next();
        if (!resource.joinPool(poolMaster, username, password)) {
            s_logger.warn("Unable to join the pool");
            throw new DiscoveryException("Unable to join the pool");
        }
        
        return true;
    }
    
    protected CitrixResourceBase createServerResource(long dcId, Long podId, Host.Record record) {
        String prodBrand = record.softwareVersion.get("product_brand").trim();
        String prodVersion = record.softwareVersion.get("product_version").trim();
        
        if(prodBrand.equals("XenCloudPlatform") && prodVersion.equals("0.1.1")) 
        	return new XcpServerResource();
        
        String msg = "Only support XCP 0.1.1 and Xerver 5.6.0, but this one is " + prodBrand + " " + prodVersion;
        _alertMgr.sendAlert(AlertManager.ALERT_TYPE_HOST, dcId, podId, msg, msg);
        s_logger.debug(msg);
        throw new RuntimeException(msg);
    }
    
    protected void serverConfig() {
        
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        serverConfig();
        
        _publicNic = _params.get(Config.XenPublicNetwork.key());
        _privateNic = _params.get(Config.XenPrivateNetwork.key());
        
        _storageNic1 = _params.get(Config.XenStorageNetwork1.key());
        _storageNic2 = _params.get(Config.XenStorageNetwork2.key());
        
        _guestNic = _params.get(Config.XenGuestNetwork.key());
        
        _increase = _params.get(Config.XenPreallocatedLunSizeRange.key());
        
        String value = _params.get(Config.Wait.toString());
        _wait = NumbersUtil.parseInt(value, Integer.parseInt(Config.Wait.getDefaultValue()));
        
        value = _params.get(Config.XenSetupMultipath.key());
        Boolean.parseBoolean(value);

        value = _params.get("xen.check.hvm");
        _checkHvm = value == null ? true : Boolean.parseBoolean(value);
        
        _connPool = XenServerConnectionPool.getInstance();
        
        _agentMgr.registerForHostEvents(this, true, false, true);
        
        return true;
    }

    @Override
    public void postDiscovery(List<HostVO> hosts, long msId) {
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
    public boolean processAnswer(long agentId, long seq, Answer[] answers) {
        return false;
    }

    @Override
    public boolean processCommand(long agentId, long seq, Command[] commands) {
        return false;
    }
    
    private void createPVTemplate(long Hostid, StartupStorageCommand cmd) {
        Map<String, TemplateInfo> tmplts = cmd.getTemplateInfo();
        if ((tmplts != null) && !tmplts.isEmpty()) {
            TemplateInfo xenPVISO = tmplts.get("xs-tools");
            if (xenPVISO != null) {
                VMTemplateVO tmplt = _tmpltDao.findByName(xenPVISO.getTemplateName());
                Long id;
                if (tmplt == null) {
                    id = _tmpltDao.getNextInSequence(Long.class, "id");
                    VMTemplateVO template = new VMTemplateVO(id, xenPVISO.getTemplateName(), xenPVISO.getTemplateName(), ImageFormat.ISO , true, true, FileSystem.cdfs, "/opt/xensource/packages/iso/xs-tools-5.5.0.iso", null, true, 64, Account.ACCOUNT_ID_SYSTEM, null, "xen-pv-drv-iso", false, 1, false);
                    _tmpltDao.persist(template);
                } else {
                    id = _tmpltDao.findByName(xenPVISO.getTemplateName()).getId();
                }

                VMTemplateHostVO tmpltHost = _vmTemplateHostDao.findByHostTemplate(Hostid, id);

                if (tmpltHost == null) {
                    VMTemplateHostVO vmTemplateHost = new VMTemplateHostVO(Hostid, id, new Date(), 100, VMTemplateStorageResourceAssoc.Status.DOWNLOADED, null, null, null, "iso/users/2/xs-tools", null);
                    vmTemplateHost.setSize(xenPVISO.getSize());
                    _vmTemplateHostDao.persist(vmTemplateHost);
                }
            }
        }
    }

    @Override
    public boolean processConnect(HostVO agent, StartupCommand cmd) {
        if (cmd instanceof StartupStorageCommand) {
            createPVTemplate(agent.getId(), (StartupStorageCommand)cmd);
            return true;
        }
        return true;
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
    public boolean processTimeout(long agentId, long seq) {
        return false;
    }
}
