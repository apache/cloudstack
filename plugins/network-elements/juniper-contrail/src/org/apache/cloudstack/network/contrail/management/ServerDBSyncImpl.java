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

package org.apache.cloudstack.network.contrail.management;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.lang.reflect.Method;

import net.juniper.contrail.api.types.FloatingIp;
import net.juniper.contrail.api.types.FloatingIpPool;
import net.juniper.contrail.api.types.ServiceInstance;
import net.juniper.contrail.api.types.VirtualNetwork;
import net.juniper.contrail.api.types.VirtualMachine;
import net.juniper.contrail.api.types.VirtualMachineInterface;
import net.juniper.contrail.api.types.InstanceIp;
import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.ApiObjectBase;
import net.juniper.contrail.api.ApiPropertyBase;
import net.juniper.contrail.api.ObjectReference;

import org.apache.cloudstack.network.contrail.model.FloatingIpModel;
import org.apache.cloudstack.network.contrail.model.FloatingIpPoolModel;
import org.apache.cloudstack.network.contrail.model.ServiceInstanceModel;
import org.apache.cloudstack.network.contrail.model.VMInterfaceModel;
import org.apache.cloudstack.network.contrail.model.VirtualMachineModel;
import org.apache.cloudstack.network.contrail.model.VirtualNetworkModel;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.dc.dao.VlanDao;
import com.cloud.domain.DomainVO;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.InternalErrorException;
import com.cloud.projects.ProjectVO;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.network.IpAddress;
import com.cloud.network.Networks.TrafficType;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.cloud.vm.VMInstanceVO;
import com.cloud.vm.NicVO;
import com.cloud.network.addr.PublicIp;
import com.cloud.network.dao.IPAddressVO;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkServiceProviderDao;

import javax.inject.Inject;

@Component
public class ServerDBSyncImpl implements ServerDBSync {

    @Inject DomainDao _domainDao;
    @Inject ProjectDao _projectDao;
    @Inject NetworkDao _networksDao;
    @Inject VMInstanceDao _vmInstanceDao;
    @Inject NicDao _nicDao;
    @Inject VlanDao _vlanDao;
    @Inject PhysicalNetworkDao _physicalNetworkDao;
    @Inject PhysicalNetworkServiceProviderDao _physProviderDao;
    @Inject ContrailManager _manager;
    DBSyncGeneric _dbSync;
    Class<?>[] _vncClasses;
    // Read-Write (true) or Read-Only mode.
    boolean _rw_mode;
    private final ReentrantLock _lockSyncMode = new ReentrantLock(); 

    ServerDBSyncImpl() {
        _vncClasses = new Class[] {
                net.juniper.contrail.api.types.Domain.class,
                net.juniper.contrail.api.types.Project.class,
                VirtualNetwork.class,
                VirtualMachine.class,
                ServiceInstance.class,
                FloatingIp.class
        };
        _dbSync = new DBSyncGeneric(this);
    }

    private static final Logger s_logger = Logger.getLogger(ServerDBSync.class);

    /*
     * API for syncing all classes of vnc objects with cloudstack
     *
     * Sync cloudstack and vnc objects.
     * Order has to be maintained 
     */
    @Override
    public short syncAll(short syncMode) {
        short syncState = SYNC_STATE_IN_SYNC;
        
        /* vnc classes need to be synchronized with cloudstack */
        s_logger.debug("syncing cloudstack db with vnc");
        try {
            for(Class<?> cls : _vncClasses) {
                
                /* lock the sync mode*/
                _lockSyncMode.lock();
                _rw_mode = syncMode == DBSyncGeneric.SYNC_MODE_UPDATE;
                _dbSync.setSyncMode(syncMode);
                
                if (_dbSync.getSyncMode() == DBSyncGeneric.SYNC_MODE_CHECK) {
                    s_logger.debug("sync check start: " + DBSyncGeneric.getClassName(cls));
                } else {
                    s_logger.debug("sync start: " + DBSyncGeneric.getClassName(cls));
                }

                if (_dbSync.sync(cls) == false) {
                    if (_dbSync.getSyncMode() == DBSyncGeneric.SYNC_MODE_CHECK) {
                        s_logger.info("out of sync detected: " + DBSyncGeneric.getClassName(cls));
                    } else {
                        s_logger.info("out of sync detected and re-synced: " + DBSyncGeneric.getClassName(cls));
                    }
                    syncState = SYNC_STATE_OUT_OF_SYNC;
                }
                if (_dbSync.getSyncMode() == DBSyncGeneric.SYNC_MODE_CHECK) {
                    s_logger.debug("sync check finish: " + DBSyncGeneric.getClassName(cls));
                } else {
                    s_logger.debug("sync finish: " + DBSyncGeneric.getClassName(cls));
                }
                /* unlock the sync mode */
                _lockSyncMode.unlock();
            } 
        } catch(Exception ex) {
            s_logger.warn("DB Synchronization", ex);
            syncState = SYNC_STATE_UNKNOWN;
            if (_lockSyncMode.isLocked()) {
                _lockSyncMode.unlock();
            }
        }

        return syncState;
    } 
    
    @Override
    public void syncClass(Class<?> cls) {

        s_logger.debug("syncClass: " + cls.getName());
        try {
            s_logger.debug("sync start: " + DBSyncGeneric.getClassName(cls));
            _lockSyncMode.lock();
            _dbSync.setSyncMode(DBSyncGeneric.SYNC_MODE_UPDATE);
            _dbSync.sync(cls);
            _lockSyncMode.unlock();
            s_logger.debug("sync finish: " + DBSyncGeneric.getClassName(cls));
        } catch(Exception ex) {
            s_logger.warn("Sync error: " + cls.getName(), ex);
            if (_lockSyncMode.isLocked()) {
                _lockSyncMode.unlock();
            }            
        }        
    }

    public <T extends ApiPropertyBase> void deleteChildren(List<ObjectReference<T>> childs,  Class<?> childCls, StringBuffer syncLogMesg) throws Exception {
        final ApiConnector api = _manager.getApiConnector();
        if (childs == null) {
            syncLogMesg.append("no children of type: " + childCls.getName() + "\n");
            return;
        }
        
        syncLogMesg.append("delete children of type : " + DBSyncGeneric.getClassName(childCls) + "\n");
        String deleteChildMethod = "delete" + DBSyncGeneric.getClassName(childCls);
        Method method = null;
        Method methods[] = this.getClass().getMethods();
        for (int i = 0; i < methods.length; i++) {
            if(methods[i].getName().equalsIgnoreCase(deleteChildMethod)) {
                method = methods[i];
                break;
            }
        }
        int count = 0;
        for (ObjectReference<T> childRef: childs) {
            @SuppressWarnings("unchecked")
            ApiObjectBase child = (ApiObjectBase) api.findById((Class<? extends ApiObjectBase>)childCls, childRef.getUuid());
            if (method != null) { 
               method.invoke(this, child, syncLogMesg);
            } else {
                deleteDefault(child, childCls, syncLogMesg);
            }
            count ++;
        }
        syncLogMesg.append("deleted children count : " + count + "\n"); 
    }

    public void deleteDefault(ApiObjectBase vnc, Class<?> cls, StringBuffer syncLogMesg) throws IOException {
        final ApiConnector api = _manager.getApiConnector();
        api.delete(vnc);
        syncLogMesg.append(cls.getCanonicalName() + "# VNC: " + vnc.getName() + " deleted\n");
    }

    /*
     *  Domain Synchronization methods
     */
    public boolean syncDomain() throws Exception {
        final ApiConnector api = _manager.getApiConnector();
        try {
            List<?> dbList = _domainDao.listAll();
            @SuppressWarnings("unchecked")
            List<?> vncList = (List<net.juniper.contrail.api.types.Domain>) api.list(net.juniper.contrail.api.types.Domain.class, null);
            return _dbSync.syncGeneric(net.juniper.contrail.api.types.Domain.class, dbList, vncList);
        } catch (Exception ex) {
            s_logger.warn("syncDomain", ex);
            throw ex;
        }
    }

    @Override
    public void createDomain(DomainVO db, StringBuffer syncLogMesg) throws IOException {
        final ApiConnector api = _manager.getApiConnector();
        net.juniper.contrail.api.types.Domain vnc = new net.juniper.contrail.api.types.Domain();
        vnc.setName(db.getName());
        vnc.setUuid(db.getUuid());
        if (!api.create(vnc)) {
            s_logger.error("Unable to create domain " + vnc.getName());
            syncLogMesg.append("Error: Virtual domain# VNC : Unable to create domain: " + 
                    vnc.getName() + "\n");
            return;
        }
        syncLogMesg.append("Domain# VNC: " + vnc.getName() + " created \n");
    }

    public void deleteDomain(net.juniper.contrail.api.types.Domain vnc, StringBuffer syncLogMesg) throws IOException {
        final ApiConnector api = _manager.getApiConnector();
        api.read(vnc);
        syncLogMesg.append("Domain# DB: none; VNC: " + vnc.getName() + "(" + 
                vnc.getUuid() + "); action: delete\n");

        /* delete all projects under this domain */
        try {
            deleteChildren(vnc.getProjects(), net.juniper.contrail.api.types.Project.class, syncLogMesg);
        } catch (Exception ex) {
            s_logger.warn("deleteDomain", ex);
        }

        api.delete(vnc);
        syncLogMesg.append("Domain# VNC: " + vnc.getName() + " deleted\n");
    }

    public Integer compareDomain(DomainVO db, net.juniper.contrail.api.types.Domain vnc, StringBuffer syncLogMesg) {
        if (_manager.isSystemRootDomain(db) && _manager.isSystemRootDomain(vnc)) {
            return _manager.getDomainCanonicalName(db).compareTo(vnc.getName());
        } else if (_manager.isSystemRootDomain(db)) {
            return -1;
        } else if (_manager.isSystemRootDomain(vnc)) {
            return 1;
        }
        return db.getUuid().compareTo(vnc.getUuid());
    }

    public Boolean filterDomain(net.juniper.contrail.api.types.Domain vnc, StringBuffer syncLogMesg)  {
        if (_manager.isSystemRootDomain(vnc)) {
            return true;
        }
        return false;
    }

    public Boolean equalDomain(DomainVO db, net.juniper.contrail.api.types.Domain vnc, StringBuffer syncLogMesg) {
        syncLogMesg.append("Domain# DB: " + db.getName() + "; VNC: " + 
                vnc.getName() + "; action: equal, no action\n");
        return true;
    }

    public Comparator<?> dbComparatorDomain() {
        Comparator<?> comparator = new Comparator<DomainVO>() {
            public int compare(DomainVO u1, DomainVO u2) {
                if (_manager.isSystemRootDomain(u1)) {
                    return -1;
                }
                if (_manager.isSystemRootDomain(u2)) {
                    return 1;
                }
                return u1.getUuid().compareTo(u2.getUuid());
            }
        };
        return comparator;
    }

    public Comparator<?> vncComparatorDomain() {
        Comparator<?> comparator = new Comparator<net.juniper.contrail.api.types.Domain> () {
            public int compare(net.juniper.contrail.api.types.Domain u1, net.juniper.contrail.api.types.Domain u2) {
                if (_manager.isSystemRootDomain(u1)) {
                    return -1;
                }
                if (_manager.isSystemRootDomain(u2)) {
                    return 1;
                }
                return u1.getUuid().compareTo(u2.getUuid());
            }
        };
        return comparator;
    }

    /*
     *  Project Synchronization methods
     */
    @SuppressWarnings("unchecked")
    public boolean syncProject() throws Exception {
        final ApiConnector api = _manager.getApiConnector();
        try {
            List<?> dbList = _projectDao.listAll();
            List<?> vncList = (List<net.juniper.contrail.api.types.Project>) api.list(net.juniper.contrail.api.types.Project.class, null);
            return _dbSync.syncGeneric(net.juniper.contrail.api.types.Project.class, dbList, vncList);
        } catch (Exception ex) {
            s_logger.warn("syncProject", ex);
            throw ex;
        }
    }

    @Override
    public void createProject(ProjectVO db, StringBuffer syncLogMesg) throws IOException {
        final ApiConnector api = _manager.getApiConnector();
        net.juniper.contrail.api.types.Project vnc = new net.juniper.contrail.api.types.Project();
        vnc.setName(db.getName());
        vnc.setUuid(db.getUuid());
        if (!api.create(vnc)) {
            s_logger.error("Unable to create project: " + vnc.getName());
            syncLogMesg.append("Error: Virtual project# VNC : Unable to create project: " + 
                    vnc.getName() + "\n");
            return;
        }
        syncLogMesg.append("Project# VNC: " + vnc.getName() + " created \n");
    }

    public void deleteProject(net.juniper.contrail.api.types.Project vnc, StringBuffer syncLogMesg) throws IOException {
        final ApiConnector api = _manager.getApiConnector();
        api.read(vnc);
        syncLogMesg.append("Project# DB: none; VNC: " + vnc.getName() + "(" + 
                vnc.getUuid() + "); action: delete\n");

        try {
            deleteChildren(vnc.getVirtualNetworks(), VirtualNetwork.class, syncLogMesg);
            deleteChildren(vnc.getSecurityGroups(), net.juniper.contrail.api.types.SecurityGroup.class, syncLogMesg);
            deleteChildren(vnc.getNetworkIpams(), net.juniper.contrail.api.types.NetworkIpam.class, syncLogMesg);
            deleteChildren(vnc.getNetworkPolicys(), net.juniper.contrail.api.types.NetworkPolicy.class, syncLogMesg);
        } catch (Exception ex) {
            s_logger.warn("deleteProject", ex);
        }

        api.delete(vnc);
        syncLogMesg.append("Project# VNC: " + vnc.getName() + " deleted\n");
    }

    public Integer compareProject(ProjectVO db, net.juniper.contrail.api.types.Project vnc, StringBuffer syncLogMesg) {
        if (_manager.isSystemDefaultProject(db) && _manager.isSystemDefaultProject(vnc)) {
            return _manager.getProjectCanonicalName(db).compareTo(vnc.getName());
        } else if (_manager.isSystemDefaultProject(db)) {
            return -1;
        } else if (_manager.isSystemDefaultProject(vnc)) {
            return 1;
        }
        return db.getUuid().compareTo(vnc.getUuid());
    }

    public Boolean filterProject(net.juniper.contrail.api.types.Project vnc, StringBuffer syncLogMesg)  {
        if (_manager.isSystemDefaultProject(vnc)) {
            syncLogMesg.append("VNC: " + vnc.getName() + " filtered; action: don't delete\n");
            return true;
        }
        return false;
    }

    public Boolean equalProject(ProjectVO db, net.juniper.contrail.api.types.Project vnc, StringBuffer syncLogMesg) {
        syncLogMesg.append("Project# DB: " + db.getName() + "; VNC: " + 
                vnc.getName() + "; action: equal, no action\n");
        return true;
    }

    public Comparator<?> dbComparatorProject() {
        Comparator<?> comparator = new Comparator<ProjectVO>() {
            public int compare(ProjectVO u1, ProjectVO u2) {
                if (_manager.isSystemDefaultProject(u1)) {
                    return -1;
                } 
                if (_manager.isSystemDefaultProject(u2)) {
                    return 1;
                }
                return u1.getUuid().compareTo(u2.getUuid());
            }
        };
        return comparator;
    }

    public Comparator<?> vncComparatorProject() {
        Comparator<?> comparator = new Comparator<net.juniper.contrail.api.types.Project> () {
            public int compare(net.juniper.contrail.api.types.Project u1, net.juniper.contrail.api.types.Project u2) {
                if (_manager.isSystemDefaultProject(u1)) {
                    return -1;
                }
                if (_manager.isSystemDefaultProject(u2)) {
                    return 1;
                }
                return u1.getUuid().compareTo(u2.getUuid());
            }
        };
        return comparator;
    }

    /*
     * Security Groups
     */
    
    public void deleteSecurityGroup(net.juniper.contrail.api.types.SecurityGroup vnc, StringBuffer syncLogMesg) throws IOException {
        final ApiConnector api = _manager.getApiConnector();
        api.delete(vnc);
        syncLogMesg.append("SecurityGroup# VNC: " + vnc.getName() + " deleted\n");
    }
    
    /*
     *  Virtual Network Synchronization methods
     */
    @SuppressWarnings({ "unchecked" })
    public boolean syncVirtualNetwork() throws Exception {
        final ApiConnector api = _manager.getApiConnector();
        try {

            List<TrafficType> types = new ArrayList<TrafficType>();
            types.add(TrafficType.Public);
            types.add(TrafficType.Guest);            
            List<NetworkVO> dbNets = _manager.findJuniperManagedNetworks(types);

            List<VirtualNetwork> vList = (List<VirtualNetwork>) api.list(VirtualNetwork.class, null);
            List<VirtualNetwork> vncList = new ArrayList<VirtualNetwork>(); 
            for (VirtualNetwork vn:vList) {
                if (!_manager.isSystemDefaultNetwork(vn)) {
                    vncList.add(vn);
                }
            }
            s_logger.debug("sync VN - DB size: " + dbNets.size() + " VNC Size: " + vncList.size());
            return _dbSync.syncGeneric(VirtualNetwork.class, dbNets, vncList);
        } catch (Exception ex) {
            s_logger.warn("sync virtual-networks", ex);
            throw ex;
        }
    }

    public Comparator<NetworkVO> dbComparatorVirtualNetwork() {
        Comparator<NetworkVO> comparator = new Comparator<NetworkVO>() {
            public int compare(NetworkVO u1, NetworkVO u2) {
                if (_manager.isSystemDefaultNetwork(u1) && _manager.isSystemDefaultNetwork(u2)) {
                    return _manager.getCanonicalName(u1).compareTo(_manager.getCanonicalName(u2));
                } else if (_manager.isSystemDefaultNetwork(u1)) {
                    return -1;
                } else if (_manager.isSystemDefaultNetwork(u2)) {
                    return 1;
                }
                return u1.getUuid().compareTo(u2.getUuid());
            }
        };
        return comparator;
    }

    public Comparator<?> vncComparatorVirtualNetwork() {
        Comparator<?> comparator = new Comparator<VirtualNetwork>() {
            public int compare(VirtualNetwork u1, VirtualNetwork u2) {
                if (_manager.isSystemDefaultNetwork(u1) && _manager.isSystemDefaultNetwork(u2)) {
                    return u1.getName().compareTo(u2.getName());
                } else if (_manager.isSystemDefaultNetwork(u1)) {
                    return -1;
                } else if (_manager.isSystemDefaultNetwork(u2)) {
                    return 1;
                }
                return u1.getUuid().compareTo(u2.getUuid());
            }
        };
        return comparator;
    }

    public void createVirtualNetwork(NetworkVO dbNet, StringBuffer syncLogMesg) throws IOException {
        syncLogMesg.append("VN# DB: " + _manager.getCanonicalName(dbNet) +
                "(" + dbNet.getUuid() + "); VNC: none;  action: create\n");
        
        if (_manager.getDatabase().lookupVirtualNetwork(dbNet.getUuid(), 
                _manager.getCanonicalName(dbNet), dbNet.getTrafficType()) != null) {
             s_logger.warn("VN model object is already present in DB: " + 
                                   dbNet.getUuid() + ", name: " + dbNet.getName());      
        }
        
        VirtualNetworkModel vnModel = new VirtualNetworkModel(dbNet, 
                dbNet.getUuid(), _manager.getCanonicalName(dbNet), dbNet.getTrafficType());
        vnModel.build(_manager.getModelController(), dbNet);

        if (_rw_mode) {
            try {
                if (!vnModel.verify(_manager.getModelController())) {
                    vnModel.update(_manager.getModelController());
                }
            } catch (InternalErrorException ex) {
                s_logger.warn("create virtual-network", ex);
                syncLogMesg.append("Error: VN# VNC : Unable to create network " + 
                    dbNet.getName() + "\n");
                return;
            }
            s_logger.debug("add model " + vnModel.getName());
            _manager.getDatabase().getVirtualNetworks().add(vnModel);
            syncLogMesg.append("VN# VNC: " + dbNet.getUuid() + ", " + vnModel.getName() + " created\n");
        } else {
            syncLogMesg.append("VN# VNC: " + vnModel.getName() + " created \n");
        }
    }


    public void deleteVirtualNetwork(VirtualNetwork vnet, StringBuffer syncLogMesg) throws IOException {
        final ApiConnector api = _manager.getApiConnector();
        if (_manager.isSystemDefaultNetwork(vnet)) {
            syncLogMesg.append("VN# System default virtual Network# VNC: " + vnet.getName() + " can not be deleted\n");
            return;
        }
        api.read(vnet);

        deleteInstanceIps(vnet.getInstanceIpBackRefs(), syncLogMesg);
        
        List<ObjectReference<ApiPropertyBase>> fipPools = vnet.getFloatingIpPools();
        if (fipPools != null && !fipPools.isEmpty()) {
            FloatingIpPool floatingIpPool = (FloatingIpPool) api.findById(FloatingIpPool.class, fipPools.get(0).getUuid());
            if (floatingIpPool != null ) {
                deleteFloatingIps(floatingIpPool.getFloatingIps(), syncLogMesg);
            } 
        }
        
        deleteVirtualMachineInterfaces(vnet.getVirtualMachineInterfaceBackRefs(), syncLogMesg);

        syncLogMesg.append("VN# DB: none; VNC: " + vnet.getName() + "(" + vnet.getUuid() + "); action: delete\n");
        api.delete(vnet);
        syncLogMesg.append("VN# VNC: " + vnet.getName() + " deleted\n");
    }

    public Integer compareVirtualNetwork(NetworkVO dbn, VirtualNetwork vnet, StringBuffer syncLogMesg) {
        if (_manager.isSystemDefaultNetwork(dbn) && _manager.isSystemDefaultNetwork(vnet)) {
            return _manager.getCanonicalName(dbn).compareTo(vnet.getName());
        } else if (_manager.isSystemDefaultNetwork(dbn)) {
            return -1;
        } else if (_manager.isSystemDefaultNetwork(vnet)) {
            return 1;
        }
        return dbn.getUuid().compareTo(vnet.getUuid());
    }

    public Boolean filterVirtualNetwork(VirtualNetwork vnet, StringBuffer syncLogMesg)  {
        if (_manager.isSystemDefaultNetwork(vnet)) {
            syncLogMesg.append("VN# VNC: " + vnet.getName() + " filtered; action: don't delete\n");
            return true;
        }
        return false;
    }

    public Boolean equalVirtualNetwork(NetworkVO dbn, VirtualNetwork vnet, StringBuffer syncLogMesg) {
        syncLogMesg.append("VN# DB: " + _manager.getCanonicalName(dbn) + 
                "; VNC: " + vnet.getName() + "; action: equal\n");
        
        VirtualNetworkModel current = _manager.getDatabase().lookupVirtualNetwork(vnet.getUuid(),
                _manager.getCanonicalName(dbn), dbn.getTrafficType());

        VirtualNetworkModel vnModel = new VirtualNetworkModel(dbn, vnet.getUuid(), 
                _manager.getCanonicalName(dbn), dbn.getTrafficType());
        vnModel.build(_manager.getModelController(), dbn);

        if (_rw_mode) {
            if (current != null) {
                FloatingIpPoolModel fipPoolModel = current.getFipPoolModel();
                if (fipPoolModel != null) {
                   vnModel.setFipPoolModel(fipPoolModel);  
                   fipPoolModel.addToVirtualNetwork(vnModel);
                }
                _manager.getDatabase().getVirtualNetworks().remove(current);
            }
            s_logger.debug("add model " + vnModel.getName());
            _manager.getDatabase().getVirtualNetworks().add(vnModel);   
            try {
                if (!vnModel.verify(_manager.getModelController())) {
                    vnModel.update(_manager.getModelController());
                }
            } catch (Exception ex) {
                s_logger.warn("update virtual-network", ex);
            }
        } else {
            //compare 
            if (current != null && current.compare(_manager.getModelController(), vnModel) == false) {
                syncLogMesg.append("VN# DB: " + _manager.getCanonicalName(dbn) + 
                        "; VNC: " + vnet.getName() + "; attributes differ\n");
                return false;
            }
        }
        return true;
    }

    /*
     *  Virtual Machine Synchronization methods
     */

    public boolean syncVirtualMachine() {
        final ApiConnector api = _manager.getApiConnector();
        try {
            List<VMInstanceVO> vmDbList = _vmInstanceDao.listAll(); 
            @SuppressWarnings("unchecked")
            List<VirtualMachine> vncVmList = (List<VirtualMachine>) api.list(VirtualMachine.class, null);
            s_logger.debug("sync VM:  CS size: " + vmDbList.size() + " VNC size: " + vncVmList.size());
            return _dbSync.syncGeneric(VirtualMachine.class, vmDbList, vncVmList);
        } catch (Exception ex) {
            s_logger.warn("sync virtual-machines", ex);
        }
        return false;
    }

    public Comparator<?> dbComparatorVirtualMachine() {
        Comparator<?> comparator = new Comparator<VMInstanceVO>() {
            public int compare(VMInstanceVO u1, VMInstanceVO u2) {
                return u1.getUuid().compareTo(u2.getUuid());
            }
        };
        return comparator;
    }

    public Comparator<?> vncComparatorVirtualMachine() {
        Comparator<?> comparator = new Comparator<VirtualMachine> () {
            public int compare(VirtualMachine u1, VirtualMachine u2) {
                return u1.getUuid().compareTo(u2.getUuid());
            }
        };
        return comparator;
    }

    public void createVirtualMachine(VMInstanceVO dbVm, StringBuffer syncLogMesg) throws IOException {
        syncLogMesg.append("VM# DB: " + dbVm.getInstanceName() + "/" + dbVm.getUuid() + "; VNC: none; action: create\n");
        VirtualMachineModel vmModel = new VirtualMachineModel(dbVm, dbVm.getUuid());
        vmModel.build(_manager.getModelController(), dbVm);
        buildNicResources(vmModel, dbVm, syncLogMesg);

        if (_rw_mode) {
            try {
                vmModel.update(_manager.getModelController());
            } catch (InternalErrorException ex) {
                s_logger.warn("create virtual-machine", ex);
                return;
            }
            _manager.getDatabase().getVirtualMachines().add(vmModel);
            syncLogMesg.append("VM# VNC: " + dbVm.getUuid() + " created\n");
        }
    }

    private void deleteVirtualMachineInterfaces(List<ObjectReference<ApiPropertyBase>> list, StringBuffer syncLogMesg) throws IOException {
        if (list == null) {
            return;
        }
        final ApiConnector api = _manager.getApiConnector();
        for (ObjectReference<ApiPropertyBase> vmiRef: list) {
            VirtualMachineInterface vmi = (VirtualMachineInterface) api.findById(VirtualMachineInterface.class, vmiRef.getUuid());
            deleteInstanceIps(vmi.getInstanceIpBackRefs(), syncLogMesg);
            deleteFloatingIps(vmi.getFloatingIpBackRefs(), syncLogMesg);
            api.delete(VirtualMachineInterface.class, vmiRef.getUuid());
            syncLogMesg.append("VNC vmi: " + vmi.getUuid() + " deleted\n");
        }        
    }

    private void deleteInstanceIps(List<ObjectReference<ApiPropertyBase>> list, StringBuffer syncLogMesg) throws IOException {
        if (list == null) {
            return;
        }
        final ApiConnector api = _manager.getApiConnector();
        for (ObjectReference<ApiPropertyBase> instIp: list) {
            api.delete(InstanceIp.class, instIp.getUuid());
            syncLogMesg.append("VNC instance ip: " + instIp.getUuid() + " deleted\n");
        }
        
    }
    
    private void deleteFloatingIps(List<ObjectReference<ApiPropertyBase>> list, StringBuffer syncLogMesg) throws IOException {
        if (list == null) {
            return;
        }
        final ApiConnector api = _manager.getApiConnector();
        for (ObjectReference<?> floatingIp: list) {
            api.delete(FloatingIp.class, floatingIp.getUuid());
            syncLogMesg.append("VNC instance ip: " + floatingIp.getUuid() + " deleted\n");
        }
    }

    public void deleteVirtualMachine(VirtualMachine vncVm, StringBuffer syncLogMesg) {
        final ApiConnector api = _manager.getApiConnector();
        syncLogMesg.append("VM# DB:none; VNC: " + vncVm.getName() + "/" + vncVm.getUuid() + "; action: delete\n");
        if (!_rw_mode) {
            return;
        }
        try {
            if (!api.read(vncVm)) {
                 return;
            }
            deleteVirtualMachineInterfaces(vncVm.getVirtualMachineInterfaces(), syncLogMesg);
            api.delete(VirtualMachine.class, vncVm.getUuid());
        } catch (IOException ex) {
            s_logger.warn("delete virtual-machine", ex);
            return;
        }
        syncLogMesg.append("VM# VNC: " + vncVm.getName() + " deleted\n");
    }

    public Integer compareVirtualMachine(VMInstanceVO dbVm, VirtualMachine vncVm, StringBuffer syncLogMesg) {
        String dbVmId = dbVm.getUuid();
        String vncVmId = vncVm.getUuid();
        return dbVmId.compareTo(vncVmId);
    }    

    public boolean filterVirtualMachine(VirtualMachine vncVm, StringBuffer syncLogMesg) {
        return false;
    }

    private void buildNicResources(VirtualMachineModel vmModel, VMInstanceVO dbVm, StringBuffer syncLogMsg)
            throws IOException {
        List<NicVO> nics = _nicDao.listByVmId(dbVm.getId());
        for (NicVO nic : nics) {
            VMInterfaceModel vmiModel = vmModel.getVMInterface(nic.getUuid());
            if (vmiModel == null) {
                vmiModel = new VMInterfaceModel(nic.getUuid());
                NetworkVO network = _networksDao.findById(nic.getNetworkId());
                VirtualNetworkModel vnModel = _manager.getDatabase().lookupVirtualNetwork(
                        network.getUuid(), _manager.getCanonicalName(network), network.getTrafficType());
                if (vnModel == null)  {
                    s_logger.warn("Unable to locate virtual-network for network id " + network.getId());
                    continue;
                }
                vmiModel.addToVirtualMachine(vmModel);
                vmiModel.addToVirtualNetwork(vnModel);
            }
            vmiModel.build(_manager.getModelController(), dbVm, nic);            
        }        
    }

    public Boolean equalVirtualMachine(VMInstanceVO dbVm, VirtualMachine vncVm, StringBuffer syncLogMsg) {

        syncLogMsg.append("VM# DB: " + dbVm.getInstanceName() + "/" + dbVm.getUuid() + 
                "; VNC: " + vncVm.getUuid() + "; action: equal; DB VM State: " + dbVm.getState() + "\n");

        VirtualMachineModel vmModel = new VirtualMachineModel(dbVm, dbVm.getUuid());
        vmModel.build(_manager.getModelController(), dbVm);

        if (vmModel.isActive()) {
            try {
                buildNicResources(vmModel, dbVm, syncLogMsg);
            } catch (IOException ex) {
                s_logger.warn("build nic information for " + dbVm.getInstanceName(), ex);
            }
        }

        VirtualMachineModel current = _manager.getDatabase().lookupVirtualMachine(vncVm.getUuid());
        if (_rw_mode) {
            if (current != null) {
                _manager.getDatabase().getVirtualMachines().remove(current);
            }
            _manager.getDatabase().getVirtualMachines().add(vmModel);   
            try {
                vmModel.update(_manager.getModelController());
            } catch (Exception ex) {
                s_logger.warn("update virtual-machine", ex);
            }
        } else {
            //compare 
            if (current != null && current.compare(_manager.getModelController(), vmModel) == false) {
                syncLogMsg.append("VM # DB: " + dbVm.getInstanceName() + 
                        "; VNC: " + vncVm.getName() + "; attributes differ\n");
                return false;
            }
        }
        return true;
    }
    
     
    public boolean syncFloatingIp() throws Exception {
        
        List<IPAddressVO> ipList = _manager.findJuniperManagedPublicIps();
        List<FloatingIp> vncList = _manager.getFloatingIps();  
        if (ipList == null) {
            ipList = new ArrayList<IPAddressVO>();
        }        
        if (vncList == null) {
            vncList = new ArrayList<FloatingIp>();
        }
        
        boolean status = false;
        try {
            status =  _dbSync.syncGeneric(FloatingIp.class, ipList, vncList);
        } catch (Exception ex) {
            s_logger.warn("sync floating-ips", ex);
            throw ex;
        }        
        return status;
    }
    
    public Comparator<?> dbComparatorFloatingIp() {
        Comparator<?> comparator = new Comparator<IpAddress>() {
            public int compare(IpAddress u1, IpAddress u2) {
                return u1.getUuid().compareTo(u2.getUuid());
            }
        };
        return comparator;
    }

    public Comparator<?> vncComparatorFloatingIp() {
        Comparator<?> comparator = new Comparator<FloatingIp> () {
            public int compare(FloatingIp u1, FloatingIp u2) {
                return u1.getUuid().compareTo(u2.getUuid());
            }
        };
        return comparator;
    }
    
    public Integer compareFloatingIp(IpAddress db, FloatingIp vnc, StringBuffer syncLogMesg) {
        String dbId = db.getUuid();
        String vncId = vnc.getUuid();
        return dbId.compareTo(vncId);
    } 
    
    public void createFloatingIp(IPAddressVO dbIp, StringBuffer syncLogMesg) throws Exception {

        if (dbIp.getState() == IpAddress.State.Releasing) {
            /* Don't need to push releasing ip */
            syncLogMesg.append("fip# DB: " + dbIp.getUuid() + ", state releasing, don't create in vnc\n");
            return;
        }
        syncLogMesg.append("fip# DB: " + dbIp.getAddress().addr() + "; VNC: none; action: create\n");
        if (!_manager.createFloatingIp(PublicIp.createFromAddrAndVlan(dbIp, _vlanDao.findById(dbIp.getVlanId())))) {
            syncLogMesg.append("fip# VNC: " + dbIp.getAddress().addr() + " unable to create\n");
            return ;
        }
        syncLogMesg.append("fip# VNC: " + dbIp.getUuid() + " created\n");
    }
    
    public void deleteFloatingIp(FloatingIp vnc, StringBuffer syncLogMesg) throws IOException {
        final ApiConnector api = _manager.getApiConnector();
        syncLogMesg.append("fip# DB: none; VNC: " + vnc.getAddress() + "(" + 
                vnc.getUuid() + "); action: delete\n");
        api.delete(vnc);
        syncLogMesg.append("fip# VNC: " + vnc.getUuid() + " deleted\n");
    }
    
    public Boolean equalFloatingIp(IPAddressVO db, FloatingIp vnc, StringBuffer syncLogMsg) 
            throws IOException {

        syncLogMsg.append("fip# DB: " + db.getAddress().addr() + 
                "; VNC: " + vnc.getAddress() + "; action: equal" + "\n");

        VirtualNetworkModel vnModel = _manager.lookupPublicNetworkModel();
        assert vnModel != null : "public network vn model is null";

        FloatingIpPoolModel fipPoolModel = vnModel.getFipPoolModel();
        if (fipPoolModel == null) {
            fipPoolModel = new FloatingIpPoolModel();
            fipPoolModel.addToVirtualNetwork(vnModel);
            fipPoolModel.build(_manager.getModelController());
            try {
                fipPoolModel.update(_manager.getModelController());
                vnModel.setFipPoolModel(fipPoolModel);
            } catch (Exception ex) {
                s_logger.warn("floating-ip-pool create: ", ex);
                return false;
            }
        }

        FloatingIpModel current = fipPoolModel.getFloatingIpModel(db.getUuid());
        if (current == null) {
            s_logger.debug("add model " + db.getAddress().addr());
            FloatingIpModel fipModel = new FloatingIpModel(db.getUuid());
            fipModel.addToFloatingIpPool(fipPoolModel);
            fipModel.build(_manager.getModelController(),  
                    PublicIp.createFromAddrAndVlan(db, _vlanDao.findById(db.getVlanId())));
            try {
                fipModel.update(_manager.getModelController());
            } catch (Exception ex) {
                s_logger.warn("floating-ip create: ", ex);
                return false;
            }
        }
        return true;
    }
    
    public Integer compareServiceInstance(ServiceInstanceModel siModel, ServiceInstance siObj, StringBuffer logMsg) {
        String fqn = StringUtils.join(siObj.getQualifiedName(), ':');
        return siModel.getQualifiedName().compareTo(fqn);
    }
    
    /**
     * createServiceInstance
     * 
     * This method should never be invoked since the model objects have been installed already when sync is called.
     * @param siModel
     * @param logMsg
     */
    public void createServiceInstance(ServiceInstanceModel siModel, StringBuffer logMsg) {
        assert false;
    }
    
    public void deleteServiceInstance(ServiceInstance siObj, StringBuffer logMsg) {
        final ApiConnector api = _manager.getApiConnector();
        s_logger.debug("delete " + siObj.getQualifiedName());
        if (!_rw_mode) {
            return;
        }
        try {
            api.delete(siObj);
        } catch (IOException ex) {
            s_logger.warn("service-instance delete", ex);
        }
    }

    /**
     * equalServiceInstance
     * 
     * @param siModel
     * @param siObj
     * @param logMsg
     */
    public void equalServiceInstance(ServiceInstanceModel siModel, ServiceInstance siObj, StringBuffer logMsg) {
        s_logger.debug("equal " + siModel.getQualifiedName());
    }
    
    static class ServiceInstanceComparator implements Comparator<ServiceInstance> {
        @Override
        public int compare(ServiceInstance obj1, ServiceInstance obj2) {
            String name1 = StringUtils.join(obj1.getQualifiedName(), ':');
            String name2 = StringUtils.join(obj2.getQualifiedName(), ':');
            return name1.compareTo(name2);
        }
        
    }
    /**
     * The service-instance model list is build as a result of synchronizing virtual-machines.
     * @return
     */
    public boolean syncServiceInstance() {
        final ApiConnector api = _manager.getApiConnector();
        boolean inSync;
        try {
            @SuppressWarnings("unchecked")
            List<ServiceInstance> siList = (List<ServiceInstance>) api.list(ServiceInstance.class, null);
            java.util.Collections.sort(siList, new ServiceInstanceComparator());
            DBSyncGeneric.SyncStats stats = new DBSyncGeneric.SyncStats();
            _dbSync.syncCollections(ServiceInstance.class, _manager.getDatabase().getServiceInstances(), siList,
                    _rw_mode, stats);
            inSync = stats.create == 0 && stats.delete == 0;
        } catch (Exception ex) {
            s_logger.warn("synchronize service-instances", ex);
            return false;
        }
        return inSync;
    }
}

