/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.endpoint;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import com.cloud.dc.DedicatedResourceVO;
import com.cloud.dc.dao.DedicatedResourceDao;
import com.cloud.user.Account;
import com.cloud.utils.Pair;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.SnapshotInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.StorageAction;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.engine.subsystem.api.storage.VolumeInfo;
import org.apache.cloudstack.storage.LocalHostEndpoint;
import org.apache.cloudstack.storage.RemoteHostEndPoint;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.capacity.CapacityManager;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachine;

@Component
public class DefaultEndPointSelector implements EndPointSelector {
    private static final Logger s_logger = Logger.getLogger(DefaultEndPointSelector.class);
    @Inject
    private HostDao hostDao;
    @Inject
    private DedicatedResourceDao dedicatedResourceDao;
    private final String findOneHostOnPrimaryStorage = "select t.id from "
                            + "(select h.id, cd.value "
                            + "from host h join storage_pool_host_ref s on h.id = s.host_id  "
                            + "join cluster c on c.id=h.cluster_id "
                            + "left join cluster_details cd on c.id=cd.cluster_id and cd.name='" + CapacityManager.StorageOperationsExcludeCluster.key() + "' "
                            + "where h.status = 'Up' and h.type = 'Routing' and h.resource_state = 'Enabled' and s.pool_id = ? ";

    private String findOneHypervisorHostInScopeByType = "select h.id from host h where h.status = 'Up' and h.hypervisor_type = ? ";
    private String findOneHypervisorHostInScope = "select h.id from host h where h.status = 'Up' and h.hypervisor_type is not null ";

    protected boolean moveBetweenPrimaryImage(DataStore srcStore, DataStore destStore) {
        DataStoreRole srcRole = srcStore.getRole();
        DataStoreRole destRole = destStore.getRole();
        if ((srcRole == DataStoreRole.Primary && destRole.isImageStore()) || (srcRole.isImageStore() && destRole == DataStoreRole.Primary)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean moveBetweenPrimaryDirectDownload(DataStore srcStore, DataStore destStore) {
        DataStoreRole srcRole = srcStore.getRole();
        DataStoreRole destRole = destStore.getRole();
        return srcRole == DataStoreRole.Primary && destRole == DataStoreRole.Primary;
    }

    protected boolean moveBetweenCacheAndImage(DataStore srcStore, DataStore destStore) {
        DataStoreRole srcRole = srcStore.getRole();
        DataStoreRole destRole = destStore.getRole();
        if (srcRole == DataStoreRole.Image && destRole == DataStoreRole.ImageCache || srcRole == DataStoreRole.ImageCache && destRole == DataStoreRole.Image) {
            return true;
        } else {
            return false;
        }
    }

    protected boolean moveBetweenImages(DataStore srcStore, DataStore destStore) {
        DataStoreRole srcRole = srcStore.getRole();
        DataStoreRole destRole = destStore.getRole();
        if (srcRole == DataStoreRole.Image && destRole == DataStoreRole.Image) {
            return true;
        } else {
            return false;
        }
    }

    @DB
    protected EndPoint findEndPointInScope(Scope scope, String sqlBase, Long poolId) {
        StringBuilder sbuilder = new StringBuilder();
        sbuilder.append(sqlBase);

        List<Long> dedicatedHosts = new ArrayList<Long>();

        if (scope != null) {
            if (scope.getScopeType() == ScopeType.HOST) {
                sbuilder.append(" and h.id = ");
                sbuilder.append(scope.getScopeId());
            } else if (scope.getScopeType() == ScopeType.CLUSTER) {
                sbuilder.append(" and h.cluster_id = ");
                sbuilder.append(scope.getScopeId());
                dedicatedHosts = dedicatedResourceDao.findHostsByCluster(scope.getScopeId());
            } else if (scope.getScopeType() == ScopeType.ZONE) {
                sbuilder.append(" and h.data_center_id = ");
                sbuilder.append(scope.getScopeId());
                dedicatedHosts = dedicatedResourceDao.findHostsByZone(scope.getScopeId());
            }
        } else {
            dedicatedHosts = dedicatedResourceDao.listAllHosts();
        }

        // TODO: order by rand() is slow if there are lot of hosts
        sbuilder.append(") t where t.value<>'true' or t.value is null");    //Added for exclude cluster's subquery
        sbuilder.append(" ORDER by ");
        if (dedicatedHosts.size() > 0) {
            moveDedicatedHostsToLowerPriority(sbuilder, dedicatedHosts);
        }
        sbuilder.append(" rand() limit 1");
        String sql = sbuilder.toString();
        HostVO host = null;
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try (PreparedStatement pstmt = txn.prepareStatement(sql)) {
            pstmt.setLong(1, poolId);
            try(ResultSet rs = pstmt.executeQuery();) {
                while (rs.next()) {
                    long id = rs.getLong(1);
                    host = hostDao.findById(id);
                }
            } catch (SQLException e) {
                s_logger.warn("can't find endpoint", e);
            }
        } catch (SQLException e) {
            s_logger.warn("can't find endpoint", e);
        }
        if (host == null) {
            return null;
        }

        return RemoteHostEndPoint.getHypervisorHostEndPoint(host);
    }

    private void moveDedicatedHostsToLowerPriority(StringBuilder sbuilder, List<Long> dedicatedHosts) {

        // Check if we have a call context
        final CallContext context = CallContext.current();
        if (context != null) {
            Account account = context.getCallingAccount();
            if (account != null) {
                // Remove hosts for this account. Only leave hosts dedicated to other accounts in the lower priority list.
                Pair<List<DedicatedResourceVO>, Integer> hostIds = dedicatedResourceDao.searchDedicatedHosts(null, null, account.getId(), null, null);
                List<DedicatedResourceVO> accountDedicatedHosts = hostIds.first();
                for (DedicatedResourceVO accountDedicatedResource: accountDedicatedHosts){
                    Iterator<Long> dedicatedHostsIterator = dedicatedHosts.iterator();
                    while (dedicatedHostsIterator.hasNext()) {
                        if (dedicatedHostsIterator.next() == accountDedicatedResource.getHostId()) {
                            dedicatedHostsIterator.remove();
                        }
                    }
                }
            }
        }

        if (dedicatedHosts.size() > 0) {
            Collections.shuffle(dedicatedHosts); // Randomize dedicated hosts as well.
            sbuilder.append("field(t.id, ");
            int hostIndex = 0;
            for (Long hostId: dedicatedHosts) { // put dedicated hosts at the end of the result set
                sbuilder.append("'" + hostId + "'");
                hostIndex++;
                if (hostIndex < dedicatedHosts.size()){
                    sbuilder.append(",");
                }
            }
            sbuilder.append(")," );
        }
    }

    protected EndPoint findEndPointForImageMove(DataStore srcStore, DataStore destStore) {
        // find any xenserver/kvm host in the scope
        Scope srcScope = srcStore.getScope();
        Scope destScope = destStore.getScope();
        Scope selectedScope = null;
        Long poolId = null;

        // assumption, at least one of scope should be zone, find the least
        // scope
        if (srcScope.getScopeType() != ScopeType.ZONE) {
            selectedScope = srcScope;
            poolId = srcStore.getId();
        } else if (destScope.getScopeType() != ScopeType.ZONE) {
            selectedScope = destScope;
            poolId = destStore.getId();
        } else {
            // if both are zone scope
            if (srcStore.getRole() == DataStoreRole.Primary) {
                selectedScope = srcScope;
                poolId = srcStore.getId();
            } else if (destStore.getRole() == DataStoreRole.Primary) {
                selectedScope = destScope;
                poolId = destStore.getId();
            }
        }
        return findEndPointInScope(selectedScope, findOneHostOnPrimaryStorage, poolId);
    }

    @Override
    public EndPoint select(DataObject srcData, DataObject destData) {
        DataStore srcStore = srcData.getDataStore();
        DataStore destStore = destData.getDataStore();
        if (moveBetweenPrimaryImage(srcStore, destStore)) {
            return findEndPointForImageMove(srcStore, destStore);
        } else if (moveBetweenPrimaryDirectDownload(srcStore, destStore)) {
            return findEndPointForImageMove(srcStore, destStore);
        } else if (moveBetweenCacheAndImage(srcStore, destStore)) {
            // pick ssvm based on image cache dc
            DataStore selectedStore = null;
            if (srcStore.getRole() == DataStoreRole.ImageCache) {
                selectedStore = srcStore;
            } else {
                selectedStore = destStore;
            }
            EndPoint ep = findEndpointForImageStorage(selectedStore);
            if (ep != null) {
                return ep;
            }
            // handle special case where it is used in deploying ssvm for S3
            if (srcData instanceof TemplateInfo) {
                TemplateInfo tmpl = (TemplateInfo)srcData;
                if (tmpl.getTemplateType() == TemplateType.SYSTEM) {
                    ep = LocalHostEndpoint.getEndpoint();
                }
            }
            return ep;
        } else if (moveBetweenImages(srcStore, destStore)) {
            EndPoint ep = findEndpointForImageStorage(destStore);
            return ep;
        }
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EndPoint select(DataObject srcData, DataObject destData, StorageAction action) {
        s_logger.error("IR24 select BACKUPSNAPSHOT from primary to secondary " + srcData.getId() + " dest=" + destData.getId());
        if (action == StorageAction.BACKUPSNAPSHOT && srcData.getDataStore().getRole() == DataStoreRole.Primary) {
            SnapshotInfo srcSnapshot = (SnapshotInfo)srcData;
            VolumeInfo volumeInfo = srcSnapshot.getBaseVolume();
            VirtualMachine vm = volumeInfo.getAttachedVM();
            if (srcSnapshot.getHypervisorType() == Hypervisor.HypervisorType.KVM) {
                if (vm != null && vm.getState() == VirtualMachine.State.Running) {
                    return getEndPointFromHostId(vm.getHostId());
                }
            }
            if (srcSnapshot.getHypervisorType() == Hypervisor.HypervisorType.VMware) {
                if (vm != null) {
                    Long hostId = vm.getHostId() != null ? vm.getHostId() : vm.getLastHostId();
                    if (hostId != null) {
                        return getEndPointFromHostId(hostId);
                    }
                }
            }
        }
        return select(srcData, destData);
    }

    protected EndPoint findEndpointForPrimaryStorage(DataStore store) {
        return findEndPointInScope(store.getScope(), findOneHostOnPrimaryStorage, store.getId());
    }

    protected EndPoint findEndpointForImageStorage(DataStore store) {
        Long dcId = null;
        Scope storeScope = store.getScope();
        if (storeScope.getScopeType() == ScopeType.ZONE) {
            dcId = storeScope.getScopeId();
        }
        // find ssvm that can be used to download data to store. For zone-wide
        // image store, use SSVM for that zone. For region-wide store,
        // we can arbitrarily pick one ssvm to do that task
        List<HostVO> ssAHosts = listUpAndConnectingSecondaryStorageVmHost(dcId);
        if (ssAHosts == null || ssAHosts.isEmpty()) {
            return null;
        }
        Collections.shuffle(ssAHosts);
        HostVO host = ssAHosts.get(0);
        return RemoteHostEndPoint.getHypervisorHostEndPoint(host);
    }

    @Override
    public List<EndPoint> findAllEndpointsForScope(DataStore store) {
        Long dcId = null;
        Scope storeScope = store.getScope();
        if (storeScope.getScopeType() == ScopeType.ZONE) {
            dcId = storeScope.getScopeId();
        }
        // find ssvm that can be used to download data to store. For zone-wide
        // image store, use SSVM for that zone. For region-wide store,
        // we can arbitrarily pick one ssvm to do that task
        List<HostVO> ssAHosts = listUpAndConnectingSecondaryStorageVmHost(dcId);
        if (ssAHosts == null || ssAHosts.isEmpty()) {
            return null;
        }
        List<EndPoint> endPoints = new ArrayList<EndPoint>();
        for (HostVO host: ssAHosts) {
            endPoints.add(RemoteHostEndPoint.getHypervisorHostEndPoint(host));
        }
        return endPoints;
    }

    private List<HostVO> listUpAndConnectingSecondaryStorageVmHost(Long dcId) {
        QueryBuilder<HostVO> sc = QueryBuilder.create(HostVO.class);
        if (dcId != null) {
            sc.and(sc.entity().getDataCenterId(), Op.EQ, dcId);
        }
        sc.and(sc.entity().getStatus(), Op.IN, Status.Up, Status.Connecting);
        sc.and(sc.entity().getType(), Op.EQ, Host.Type.SecondaryStorageVM);
        sc.and(sc.entity().getRemoved(), Op.NULL);
        return sc.list();
    }

    @Override
    public EndPoint select(DataObject object) {
        DataStore store = object.getDataStore();
        EndPoint ep = select(store);
        if (ep != null) {
            return ep;
        }
        if (object instanceof TemplateInfo) {
            TemplateInfo tmplInfo = (TemplateInfo)object;
            if (store.getScope().getScopeType() == ScopeType.ZONE && store.getScope().getScopeId() == null && tmplInfo.getTemplateType() == TemplateType.SYSTEM) {
                return LocalHostEndpoint.getEndpoint(); // for bootstrap system vm template downloading to region image store
            }
        }
        return null;
    }

    @Override
    public EndPoint select(DataStore store, String downloadUrl){

        HostVO host = null;
        try {
            URI uri = new URI(downloadUrl);
            String scheme = uri.getScheme();
            String publicIp = uri.getHost();
            // If its https then public ip will be of the form xxx-xxx-xxx-xxx.mydomain.com
            if(scheme.equalsIgnoreCase("https")){
                publicIp = publicIp.split("\\.")[0]; // We want xxx-xxx-xxx-xxx
                publicIp = publicIp.replace("-","."); // We not want the IP -  xxx.xxx.xxx.xxx
            }
            host = hostDao.findByPublicIp(publicIp);
            if(host != null){
                return RemoteHostEndPoint.getHypervisorHostEndPoint(host);
            }

        } catch (URISyntaxException e) {
            s_logger.debug("Received URISyntaxException for url" +downloadUrl);
        }

        // If ssvm doesnt exist then find any ssvm in the zone.
        s_logger.debug("Coudn't find ssvm for url" +downloadUrl);
        return findEndpointForImageStorage(store);
    }

    @Override
    public EndPoint select(DataStore store) {
        if (store.getRole() == DataStoreRole.Primary) {
            return findEndpointForPrimaryStorage(store);
        } else if (store.getRole() == DataStoreRole.Image || store.getRole() == DataStoreRole.ImageCache) {
            // in case there is no ssvm, directly send down command hypervisor
            // host
            // otherwise, send to localhost for bootstrap system vm template
            // download
            return findEndpointForImageStorage(store);
        } else {
            throw new CloudRuntimeException("not implemented yet");
        }
    }

    public EndPoint getEndPointFromHostId(Long hostId) {
        HostVO host = hostDao.findById(hostId);
        return RemoteHostEndPoint.getHypervisorHostEndPoint(host);
    }

    @Override
    public EndPoint select(DataObject object, StorageAction action) {
        if (action == StorageAction.TAKESNAPSHOT) {
            SnapshotInfo snapshotInfo = (SnapshotInfo)object;
            if (snapshotInfo.getHypervisorType() == Hypervisor.HypervisorType.KVM) {
                VolumeInfo volumeInfo = snapshotInfo.getBaseVolume();
                VirtualMachine vm = volumeInfo.getAttachedVM();
                if ((vm != null) && (vm.getState() == VirtualMachine.State.Running)) {
                    Long hostId = vm.getHostId();
                    return getEndPointFromHostId(hostId);
                }
            }
        } else if (action == StorageAction.MIGRATEVOLUME) {
            VolumeInfo volume = (VolumeInfo)object;
            if (volume.getHypervisorType() == Hypervisor.HypervisorType.Hyperv || volume.getHypervisorType() == Hypervisor.HypervisorType.VMware) {
                VirtualMachine vm = volume.getAttachedVM();
                if ((vm != null) && (vm.getState() == VirtualMachine.State.Running)) {
                    Long hostId = vm.getHostId();
                    return getEndPointFromHostId(hostId);
                }
            }
        } else if (action == StorageAction.DELETEVOLUME) {
            VolumeInfo volume = (VolumeInfo)object;
            if (volume.getHypervisorType() == Hypervisor.HypervisorType.VMware) {
                VirtualMachine vm = volume.getAttachedVM();
                if (vm != null) {
                    Long hostId = vm.getHostId() != null ? vm.getHostId() : vm.getLastHostId();
                    if (hostId != null) {
                        return getEndPointFromHostId(hostId);
                    }
                }
            }
        }
        return select(object);
    }

    @Override
    public EndPoint select(Scope scope, Long storeId) {
        return findEndPointInScope(scope, findOneHostOnPrimaryStorage, storeId);
    }

    @Override
    public List<EndPoint> selectAll(DataStore store) {
        List<EndPoint> endPoints = new ArrayList<EndPoint>();
        if (store.getScope().getScopeType() == ScopeType.HOST) {
            HostVO host = hostDao.findById(store.getScope().getScopeId());

            endPoints.add(RemoteHostEndPoint.getHypervisorHostEndPoint(host));
        } else if (store.getScope().getScopeType() == ScopeType.CLUSTER) {
            QueryBuilder<HostVO> sc = QueryBuilder.create(HostVO.class);
            sc.and(sc.entity().getClusterId(), Op.EQ, store.getScope().getScopeId());
            sc.and(sc.entity().getStatus(), Op.EQ, Status.Up);
            List<HostVO> hosts = sc.list();
            for (HostVO host : hosts) {
                endPoints.add(RemoteHostEndPoint.getHypervisorHostEndPoint(host));
            }

        } else {
            throw new CloudRuntimeException("shouldn't use it for other scope");
        }
        return endPoints;
    }
}
