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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.TemplateInfo;
import org.apache.cloudstack.storage.LocalHostEndpoint;
import org.apache.cloudstack.storage.RemoteHostEndPoint;

import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.QueryBuilder;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class DefaultEndPointSelector implements EndPointSelector {
    private static final Logger s_logger = Logger.getLogger(DefaultEndPointSelector.class);
    @Inject
    HostDao hostDao;
    private final String findOneHostOnPrimaryStorage =
        "select h.id from host h, storage_pool_host_ref s  where h.status = 'Up' and h.type = 'Routing' and h.resource_state = 'Enabled' and"
            + " h.id = s.host_id and s.pool_id = ? ";

    protected boolean moveBetweenPrimaryImage(DataStore srcStore, DataStore destStore) {
        DataStoreRole srcRole = srcStore.getRole();
        DataStoreRole destRole = destStore.getRole();
        if ((srcRole == DataStoreRole.Primary && destRole.isImageStore()) || (srcRole.isImageStore() && destRole == DataStoreRole.Primary)) {
            return true;
        } else {
            return false;
        }
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

        if (scope.getScopeType() == ScopeType.HOST) {
            sbuilder.append(" and h.id = ");
            sbuilder.append(scope.getScopeId());
        } else if (scope.getScopeType() == ScopeType.CLUSTER) {
            sbuilder.append(" and h.cluster_id = ");
            sbuilder.append(scope.getScopeId());
        } else if (scope.getScopeType() == ScopeType.ZONE) {
            sbuilder.append(" and h.data_center_id = ");
            sbuilder.append(scope.getScopeId());
        }
        // TODO: order by rand() is slow if there are lot of hosts
        sbuilder.append(" ORDER by rand() limit 1");
        String sql = sbuilder.toString();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        HostVO host = null;
        TransactionLegacy txn = TransactionLegacy.currentTxn();

        try {
            pstmt = txn.prepareStatement(sql);
            pstmt.setLong(1, poolId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                host = hostDao.findById(id);
            }
        } catch (SQLException e) {
            s_logger.warn("can't find endpoint", e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }

        if (host == null) {
            return null;
        }

        return RemoteHostEndPoint.getHypervisorHostEndPoint(host.getId(), host.getPrivateIpAddress(), host.getPublicIpAddress());
    }

    protected EndPoint findEndPointForImageMove(DataStore srcStore, DataStore destStore) {
        // find any xen/kvm host in the scope
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
            selectedScope = srcScope;
            if (srcStore.getRole() == DataStoreRole.Primary) {
                poolId = srcStore.getId();
            } else if (destStore.getRole() == DataStoreRole.Primary) {
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
        return RemoteHostEndPoint.getHypervisorHostEndPoint(host.getId(), host.getPrivateIpAddress(), host.getPublicIpAddress());
    }

    private List<HostVO> listUpAndConnectingSecondaryStorageVmHost(Long dcId) {
        QueryBuilder<HostVO> sc = QueryBuilder.create(HostVO.class);
        if (dcId != null) {
            sc.and(sc.entity().getDataCenterId(), Op.EQ, dcId);
        }
        sc.and(sc.entity().getStatus(), Op.IN, Status.Up, Status.Connecting);
        sc.and(sc.entity().getType(), Op.EQ, Host.Type.SecondaryStorageVM);
        return sc.list();
    }

    @Override
    public EndPoint select(DataObject object) {
        DataStore store = object.getDataStore();
        EndPoint ep = select(store);
        if (ep != null)
            return ep;
        if (object instanceof TemplateInfo) {
            TemplateInfo tmplInfo = (TemplateInfo)object;
            if (store.getScope().getScopeType() == ScopeType.ZONE && store.getScope().getScopeId() == null && tmplInfo.getTemplateType() == TemplateType.SYSTEM) {
                return LocalHostEndpoint.getEndpoint(); // for bootstrap system vm template downloading to region image store
            }
        }
        return null;
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

    @Override
    public EndPoint select(Scope scope, Long storeId) {
        return findEndPointInScope(scope, findOneHostOnPrimaryStorage, storeId);
    }

    @Override
    public List<EndPoint> selectAll(DataStore store) {
        List<EndPoint> endPoints = new ArrayList<EndPoint>();
        if (store.getScope().getScopeType() == ScopeType.HOST) {
            HostVO host = hostDao.findById(store.getScope().getScopeId());
            endPoints.add(RemoteHostEndPoint.getHypervisorHostEndPoint(host.getId(), host.getPrivateIpAddress(), host.getPublicIpAddress()));
        } else if (store.getScope().getScopeType() == ScopeType.CLUSTER) {
            QueryBuilder<HostVO> sc = QueryBuilder.create(HostVO.class);
            sc.and(sc.entity().getClusterId(), Op.EQ, store.getScope().getScopeId());
            sc.and(sc.entity().getStatus(), Op.EQ, Status.Up);
            List<HostVO> hosts = sc.list();
            for (HostVO host : hosts) {
                endPoints.add(RemoteHostEndPoint.getHypervisorHostEndPoint(host.getId(), host.getPrivateIpAddress(), host.getPublicIpAddress()));
            }

        } else {
            throw new CloudRuntimeException("shouldn't use it for other scope");
        }
        return endPoints;
    }
}
