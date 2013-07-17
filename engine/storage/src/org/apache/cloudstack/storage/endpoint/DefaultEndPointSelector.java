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

import org.apache.cloudstack.engine.subsystem.api.storage.DataObject;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPoint;
import org.apache.cloudstack.engine.subsystem.api.storage.EndPointSelector;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.storage.RemoteHostEndPoint;
import org.apache.cloudstack.storage.LocalHostEndpoint;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.dao.HostDao;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.ScopeType;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.SearchCriteriaService;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class DefaultEndPointSelector implements EndPointSelector {
    private static final Logger s_logger = Logger.getLogger(DefaultEndPointSelector.class);
    @Inject
    HostDao hostDao;
    private String findOneHostOnPrimaryStorage = "select id from host where " + "status = 'Up' and type = 'Routing' ";

    protected boolean moveBetweenPrimaryImage(DataStore srcStore, DataStore destStore) {
        DataStoreRole srcRole = srcStore.getRole();
        DataStoreRole destRole = destStore.getRole();
        if ((srcRole == DataStoreRole.Primary && destRole.isImageStore())
                || (srcRole.isImageStore() && destRole == DataStoreRole.Primary)) {
            return true;
        } else {
            return false;
        }
    }

    protected boolean moveBetweenCacheAndImage(DataStore srcStore, DataStore destStore) {
        DataStoreRole srcRole = srcStore.getRole();
        DataStoreRole destRole = destStore.getRole();
        if (srcRole == DataStoreRole.Image && destRole == DataStoreRole.ImageCache
                || srcRole == DataStoreRole.ImageCache && destRole == DataStoreRole.Image) {
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
    protected EndPoint findEndPointInScope(Scope scope, String sqlBase) {
        StringBuilder sbuilder = new StringBuilder();
        sbuilder.append(sqlBase);

        if (scope.getScopeType() == ScopeType.HOST) {
            sbuilder.append(" and id = ");
            sbuilder.append(scope.getScopeId());
        } else if (scope.getScopeType() == ScopeType.CLUSTER) {
            sbuilder.append(" and cluster_id = ");
            sbuilder.append(scope.getScopeId());
        } else if (scope.getScopeType() == ScopeType.ZONE) {
            sbuilder.append(" and data_center_id = ");
            sbuilder.append(scope.getScopeId());
        }
        // TODO: order by rand() is slow if there are lot of hosts
        sbuilder.append(" ORDER by rand() limit 1");
        String sql = sbuilder.toString();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        HostVO host = null;
        Transaction txn = Transaction.currentTxn();

        try {
            pstmt = txn.prepareStatement(sql);
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

        return RemoteHostEndPoint.getHypervisorHostEndPoint(host.getId(), host.getPrivateIpAddress(),
                host.getPublicIpAddress());
    }

    protected EndPoint findEndPointForImageMove(DataStore srcStore, DataStore destStore) {
        // find any xen/kvm host in the scope
        Scope srcScope = srcStore.getScope();
        Scope destScope = destStore.getScope();
        Scope selectedScope = null;
        // assumption, at least one of scope should be zone, find the least
        // scope
        if (srcScope.getScopeType() != ScopeType.ZONE) {
            selectedScope = srcScope;
        } else if (destScope.getScopeType() != ScopeType.ZONE) {
            selectedScope = destScope;
        } else {
            // if both are zone scope
            selectedScope = srcScope;
        }
        return findEndPointInScope(selectedScope, findOneHostOnPrimaryStorage);
    }

    @Override
    public EndPoint select(DataObject srcData, DataObject destData) {
        DataStore srcStore = srcData.getDataStore();
        DataStore destStore = destData.getDataStore();
        if (moveBetweenPrimaryImage(srcStore, destStore)) {
            return findEndPointForImageMove(srcStore, destStore);
        } else if (moveBetweenCacheAndImage(srcStore, destStore)) {
            EndPoint ep = findEndpointForImageStorage(destStore);
            return ep;
        } else if (moveBetweenImages(srcStore, destStore)) {
            EndPoint ep = findEndpointForImageStorage(destStore);
            return ep;
        }
        // TODO Auto-generated method stub
        return null;
    }

    protected EndPoint findEndpointForPrimaryStorage(DataStore store) {
        return findEndPointInScope(store.getScope(), findOneHostOnPrimaryStorage);
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
            return LocalHostEndpoint.getEndpoint(); // use local host as endpoint in
                                            // case of no ssvm existing
        }
        Collections.shuffle(ssAHosts);
        HostVO host = ssAHosts.get(0);
        return RemoteHostEndPoint.getHypervisorHostEndPoint(host.getId(), host.getPrivateIpAddress(),
                host.getPublicIpAddress());
    }

    private List<HostVO> listUpAndConnectingSecondaryStorageVmHost(Long dcId) {
        SearchCriteriaService<HostVO, HostVO> sc = SearchCriteria2.create(HostVO.class);
        if (dcId != null) {
            sc.addAnd(sc.getEntity().getDataCenterId(), Op.EQ, dcId);
        }
        sc.addAnd(sc.getEntity().getStatus(), Op.IN, com.cloud.host.Status.Up, com.cloud.host.Status.Connecting);
        sc.addAnd(sc.getEntity().getType(), Op.EQ, Host.Type.SecondaryStorageVM);
        return sc.list();
    }

    @Override
    public EndPoint select(DataObject object) {
        DataStore store = object.getDataStore();
        return select(store);
    }

    @Override
    public EndPoint select(DataStore store) {
        if (store.getRole() == DataStoreRole.Primary) {
            return findEndpointForPrimaryStorage(store);
        } else if (store.getRole() == DataStoreRole.Image) {
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
    public List<EndPoint> selectAll(DataStore store) {
        List<EndPoint> endPoints = new ArrayList<EndPoint>();
        if (store.getScope().getScopeType() == ScopeType.HOST) {
            HostVO host = hostDao.findById(store.getScope().getScopeId());
            endPoints.add(RemoteHostEndPoint.getHypervisorHostEndPoint(host.getId(), host.getPrivateIpAddress(),
                    host.getPublicIpAddress()));
        } else if (store.getScope().getScopeType() == ScopeType.CLUSTER) {
            SearchCriteriaService<HostVO, HostVO> sc = SearchCriteria2.create(HostVO.class);
            sc.addAnd(sc.getEntity().getClusterId(), Op.EQ, store.getScope().getScopeId());
            sc.addAnd(sc.getEntity().getStatus(), Op.EQ, Status.Up);
            List<HostVO> hosts = sc.find();
            for (HostVO host : hosts) {
                endPoints.add(RemoteHostEndPoint.getHypervisorHostEndPoint(host.getId(), host.getPrivateIpAddress(),
                        host.getPublicIpAddress()));
            }

        } else {
            throw new CloudRuntimeException("shouldn't use it for other scope");
        }
        return endPoints;
    }
}
