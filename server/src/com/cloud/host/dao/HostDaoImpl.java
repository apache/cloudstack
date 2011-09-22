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
package com.cloud.host.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.ejb.Local;
import javax.persistence.TableGenerator;

import org.apache.log4j.Logger;

import com.cloud.cluster.agentlb.HostTransferMapVO;
import com.cloud.cluster.agentlb.dao.HostTransferMapDaoImpl;
import com.cloud.dc.ClusterVO;
import com.cloud.dc.dao.ClusterDaoImpl;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.host.HostTagVO;
import com.cloud.host.HostVO;
import com.cloud.host.Status;
import com.cloud.host.Status.Event;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.info.RunningHostCountInfo;
import com.cloud.org.Managed;
import com.cloud.utils.DateUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.Attribute;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.JoinBuilder.JoinType;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.UpdateBuilder;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value = { HostDao.class })
@DB(txn = false)
@TableGenerator(name = "host_req_sq", table = "op_host", pkColumnName = "id", valueColumnName = "sequence", allocationSize = 1)
public class HostDaoImpl extends GenericDaoBase<HostVO, Long> implements HostDao {
    private static final Logger s_logger = Logger.getLogger(HostDaoImpl.class);

    protected final SearchBuilder<HostVO> TypePodDcStatusSearch;

    protected final SearchBuilder<HostVO> IdStatusSearch;
    protected final SearchBuilder<HostVO> TypeDcSearch;
    protected final SearchBuilder<HostVO> TypeDcStatusSearch;
    protected final SearchBuilder<HostVO> MsStatusSearch;
    protected final SearchBuilder<HostVO> DcPrivateIpAddressSearch;
    protected final SearchBuilder<HostVO> DcStorageIpAddressSearch;

    protected final SearchBuilder<HostVO> GuidSearch;
    protected final SearchBuilder<HostVO> DcSearch;
    protected final SearchBuilder<HostVO> PodSearch;
    protected final SearchBuilder<HostVO> TypeSearch;
    protected final SearchBuilder<HostVO> StatusSearch;
    protected final SearchBuilder<HostVO> NameLikeSearch;
    protected final SearchBuilder<HostVO> NameSearch;
    protected final SearchBuilder<HostVO> SequenceSearch;
    protected final SearchBuilder<HostVO> DirectlyConnectedSearch;
    protected final SearchBuilder<HostVO> UnmanagedDirectConnectSearch;
    protected final SearchBuilder<HostVO> UnmanagedApplianceSearch;
    protected final SearchBuilder<HostVO> MaintenanceCountSearch;
    protected final SearchBuilder<HostVO> ClusterStatusSearch;
    protected final SearchBuilder<HostVO> ConsoleProxyHostSearch;
    protected final SearchBuilder<HostVO> AvailHypevisorInZone;

    protected final SearchBuilder<HostVO> DirectConnectSearch;
    protected final SearchBuilder<HostVO> ManagedDirectConnectSearch;
    protected final SearchBuilder<HostVO> ManagedRoutingServersSearch;
    protected final SearchBuilder<HostVO> SecondaryStorageVMSearch;
    

    protected final GenericSearchBuilder<HostVO, Long> HostsInStatusSearch;
    protected final GenericSearchBuilder<HostVO, Long> CountRoutingByDc;
    protected final SearchBuilder<HostTransferMapVO> HostTransferSearch;
    protected SearchBuilder<ClusterVO> ClusterManagedSearch;
    protected final SearchBuilder<HostVO> RoutingSearch;


    protected final Attribute _statusAttr;
    protected final Attribute _msIdAttr;
    protected final Attribute _pingTimeAttr;

    protected final HostDetailsDaoImpl _detailsDao = ComponentLocator.inject(HostDetailsDaoImpl.class);
    protected final HostTagsDaoImpl _hostTagsDao = ComponentLocator.inject(HostTagsDaoImpl.class);
    protected final HostTransferMapDaoImpl _hostTransferDao = ComponentLocator.inject(HostTransferMapDaoImpl.class);
    protected final ClusterDaoImpl _clusterDao = ComponentLocator.inject(ClusterDaoImpl.class);
    

    public HostDaoImpl() {

        MaintenanceCountSearch = createSearchBuilder();
        MaintenanceCountSearch.and("cluster", MaintenanceCountSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
        MaintenanceCountSearch.and("status", MaintenanceCountSearch.entity().getStatus(), SearchCriteria.Op.IN);
        MaintenanceCountSearch.done();

        TypePodDcStatusSearch = createSearchBuilder();
        HostVO entity = TypePodDcStatusSearch.entity();
        TypePodDcStatusSearch.and("type", entity.getType(), SearchCriteria.Op.EQ);
        TypePodDcStatusSearch.and("pod", entity.getPodId(), SearchCriteria.Op.EQ);
        TypePodDcStatusSearch.and("dc", entity.getDataCenterId(), SearchCriteria.Op.EQ);
        TypePodDcStatusSearch.and("cluster", entity.getClusterId(), SearchCriteria.Op.EQ);
        TypePodDcStatusSearch.and("status", entity.getStatus(), SearchCriteria.Op.EQ);
        TypePodDcStatusSearch.done();

        MsStatusSearch = createSearchBuilder();
        MsStatusSearch.and("ms", MsStatusSearch.entity().getManagementServerId(), SearchCriteria.Op.EQ);
        MsStatusSearch.and("type", MsStatusSearch.entity().getType(), SearchCriteria.Op.EQ);
        MsStatusSearch.and("statuses", MsStatusSearch.entity().getStatus(), SearchCriteria.Op.NIN);
        MsStatusSearch.done();

        TypeDcSearch = createSearchBuilder();
        TypeDcSearch.and("type", TypeDcSearch.entity().getType(), SearchCriteria.Op.EQ);
        TypeDcSearch.and("dc", TypeDcSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        TypeDcSearch.done();

        SecondaryStorageVMSearch = createSearchBuilder();
        SecondaryStorageVMSearch.and("type", SecondaryStorageVMSearch.entity().getType(), SearchCriteria.Op.EQ);
        SecondaryStorageVMSearch.and("dc", SecondaryStorageVMSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        SecondaryStorageVMSearch.and("status", SecondaryStorageVMSearch.entity().getStatus(), SearchCriteria.Op.IN);
        SecondaryStorageVMSearch.done();

        TypeDcStatusSearch = createSearchBuilder();
        TypeDcStatusSearch.and("type", TypeDcStatusSearch.entity().getType(), SearchCriteria.Op.EQ);
        TypeDcStatusSearch.and("dc", TypeDcStatusSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        TypeDcStatusSearch.and("status", TypeDcStatusSearch.entity().getStatus(), SearchCriteria.Op.EQ);
        TypeDcStatusSearch.done();

        IdStatusSearch = createSearchBuilder();
        IdStatusSearch.and("id", IdStatusSearch.entity().getId(), SearchCriteria.Op.EQ);
        IdStatusSearch.and("states", IdStatusSearch.entity().getStatus(), SearchCriteria.Op.IN);
        IdStatusSearch.done();

        DcPrivateIpAddressSearch = createSearchBuilder();
        DcPrivateIpAddressSearch.and("privateIpAddress", DcPrivateIpAddressSearch.entity().getPrivateIpAddress(), SearchCriteria.Op.EQ);
        DcPrivateIpAddressSearch.and("dc", DcPrivateIpAddressSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DcPrivateIpAddressSearch.done();

        DcStorageIpAddressSearch = createSearchBuilder();
        DcStorageIpAddressSearch.and("storageIpAddress", DcStorageIpAddressSearch.entity().getStorageIpAddress(), SearchCriteria.Op.EQ);
        DcStorageIpAddressSearch.and("dc", DcStorageIpAddressSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DcStorageIpAddressSearch.done();

        GuidSearch = createSearchBuilder();
        GuidSearch.and("guid", GuidSearch.entity().getGuid(), SearchCriteria.Op.EQ);
        GuidSearch.done();

        DcSearch = createSearchBuilder();
        DcSearch.and("dc", DcSearch.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        DcSearch.done();

        ClusterStatusSearch = createSearchBuilder();
        ClusterStatusSearch.and("cluster", ClusterStatusSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
        ClusterStatusSearch.and("status", ClusterStatusSearch.entity().getStatus(), SearchCriteria.Op.EQ);
        ClusterStatusSearch.done();

        ConsoleProxyHostSearch = createSearchBuilder();
        ConsoleProxyHostSearch.and("name", ConsoleProxyHostSearch.entity().getName(), SearchCriteria.Op.EQ);
        ConsoleProxyHostSearch.and("type", ConsoleProxyHostSearch.entity().getType(), SearchCriteria.Op.EQ);
        ConsoleProxyHostSearch.done();

        PodSearch = createSearchBuilder();
        PodSearch.and("pod", PodSearch.entity().getPodId(), SearchCriteria.Op.EQ);
        PodSearch.done();

        TypeSearch = createSearchBuilder();
        TypeSearch.and("type", TypeSearch.entity().getType(), SearchCriteria.Op.EQ);
        TypeSearch.done();

        StatusSearch = createSearchBuilder();
        StatusSearch.and("status", StatusSearch.entity().getStatus(), SearchCriteria.Op.IN);
        StatusSearch.done();

        NameLikeSearch = createSearchBuilder();
        NameLikeSearch.and("name", NameLikeSearch.entity().getName(), SearchCriteria.Op.LIKE);
        NameLikeSearch.done();

        NameSearch = createSearchBuilder();
        NameSearch.and("name", NameSearch.entity().getName(), SearchCriteria.Op.EQ);
        NameSearch.done();

        SequenceSearch = createSearchBuilder();
        SequenceSearch.and("id", SequenceSearch.entity().getId(), SearchCriteria.Op.EQ);
        // SequenceSearch.addRetrieve("sequence", SequenceSearch.entity().getSequence());
        SequenceSearch.done();

        DirectlyConnectedSearch = createSearchBuilder();
        DirectlyConnectedSearch.and("resource", DirectlyConnectedSearch.entity().getResource(), SearchCriteria.Op.NNULL);
        DirectlyConnectedSearch.and("ms", DirectlyConnectedSearch.entity().getManagementServerId(), SearchCriteria.Op.EQ);
        DirectlyConnectedSearch.and("statuses", DirectlyConnectedSearch.entity().getStatus(), SearchCriteria.Op.EQ);
        DirectlyConnectedSearch.done();

        UnmanagedDirectConnectSearch = createSearchBuilder();
        UnmanagedDirectConnectSearch.and("resource", UnmanagedDirectConnectSearch.entity().getResource(), SearchCriteria.Op.NNULL);
        UnmanagedDirectConnectSearch.and("server", UnmanagedDirectConnectSearch.entity().getManagementServerId(), SearchCriteria.Op.NULL);
        UnmanagedDirectConnectSearch.and("lastPinged", UnmanagedDirectConnectSearch.entity().getLastPinged(), SearchCriteria.Op.LTEQ);
        UnmanagedDirectConnectSearch.and("statuses", UnmanagedDirectConnectSearch.entity().getStatus(), SearchCriteria.Op.NIN);
        /*
         * UnmanagedDirectConnectSearch.op(SearchCriteria.Op.OR, "managementServerId",
         * UnmanagedDirectConnectSearch.entity().getManagementServerId(), SearchCriteria.Op.EQ);
         * UnmanagedDirectConnectSearch.and("lastPinged", UnmanagedDirectConnectSearch.entity().getLastPinged(),
         * SearchCriteria.Op.LTEQ); UnmanagedDirectConnectSearch.cp(); UnmanagedDirectConnectSearch.cp();
         */
        HostTransferSearch = _hostTransferDao.createSearchBuilder();
        HostTransferSearch.and("id", HostTransferSearch.entity().getId(), SearchCriteria.Op.NULL);
        UnmanagedDirectConnectSearch.join("hostTransferSearch", HostTransferSearch, HostTransferSearch.entity().getId(), UnmanagedDirectConnectSearch.entity().getId(), JoinType.LEFTOUTER);
        ClusterManagedSearch = _clusterDao.createSearchBuilder();
        ClusterManagedSearch.and("managed", ClusterManagedSearch.entity().getManagedState(), SearchCriteria.Op.EQ);
        UnmanagedDirectConnectSearch.join("ClusterManagedSearch", ClusterManagedSearch, ClusterManagedSearch.entity().getId(), UnmanagedDirectConnectSearch.entity().getClusterId(), JoinType.INNER);
        UnmanagedDirectConnectSearch.done();


        DirectConnectSearch = createSearchBuilder();
        DirectConnectSearch.and("resource", DirectConnectSearch.entity().getResource(), SearchCriteria.Op.NNULL);
        DirectConnectSearch.and("id", DirectConnectSearch.entity().getId(), SearchCriteria.Op.EQ);
        DirectConnectSearch.and().op("nullserver", DirectConnectSearch.entity().getManagementServerId(), SearchCriteria.Op.NULL);
        DirectConnectSearch.or("server", DirectConnectSearch.entity().getManagementServerId(), SearchCriteria.Op.EQ);
        DirectConnectSearch.cp();
        DirectConnectSearch.done();

        UnmanagedApplianceSearch = createSearchBuilder();
        UnmanagedApplianceSearch.and("resource", UnmanagedApplianceSearch.entity().getResource(), SearchCriteria.Op.NNULL);
        UnmanagedApplianceSearch.and("server", UnmanagedApplianceSearch.entity().getManagementServerId(), SearchCriteria.Op.NULL);
        UnmanagedApplianceSearch.and("types", UnmanagedApplianceSearch.entity().getType(), SearchCriteria.Op.IN);
        UnmanagedApplianceSearch.and("lastPinged", UnmanagedApplianceSearch.entity().getLastPinged(), SearchCriteria.Op.LTEQ);
        UnmanagedApplianceSearch.done();

        AvailHypevisorInZone = createSearchBuilder();
        AvailHypevisorInZone.and("zoneId", AvailHypevisorInZone.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        AvailHypevisorInZone.and("hostId", AvailHypevisorInZone.entity().getId(), SearchCriteria.Op.NEQ);
        AvailHypevisorInZone.and("type", AvailHypevisorInZone.entity().getType(), SearchCriteria.Op.EQ);
        AvailHypevisorInZone.groupBy(AvailHypevisorInZone.entity().getHypervisorType());
        AvailHypevisorInZone.done();

        HostsInStatusSearch = createSearchBuilder(Long.class);
        HostsInStatusSearch.selectField(HostsInStatusSearch.entity().getId());
        HostsInStatusSearch.and("dc", HostsInStatusSearch.entity().getDataCenterId(), Op.EQ);
        HostsInStatusSearch.and("pod", HostsInStatusSearch.entity().getPodId(), Op.EQ);
        HostsInStatusSearch.and("cluster", HostsInStatusSearch.entity().getClusterId(), Op.EQ);
        HostsInStatusSearch.and("type", HostsInStatusSearch.entity().getType(), Op.EQ);
        HostsInStatusSearch.and("statuses", HostsInStatusSearch.entity().getStatus(), Op.IN);
        HostsInStatusSearch.done();

        CountRoutingByDc = createSearchBuilder(Long.class);
        CountRoutingByDc.select(null, Func.COUNT, null);
        CountRoutingByDc.and("dc", CountRoutingByDc.entity().getDataCenterId(), SearchCriteria.Op.EQ);
        CountRoutingByDc.and("type", CountRoutingByDc.entity().getType(), SearchCriteria.Op.EQ);
        CountRoutingByDc.and("status", CountRoutingByDc.entity().getStatus(), SearchCriteria.Op.EQ);

        CountRoutingByDc.done();

        ManagedDirectConnectSearch = createSearchBuilder();
        ManagedDirectConnectSearch.and("resource", ManagedDirectConnectSearch.entity().getResource(), SearchCriteria.Op.NNULL);
        ManagedDirectConnectSearch.and("server", ManagedDirectConnectSearch.entity().getManagementServerId(), SearchCriteria.Op.NULL);
        ManagedDirectConnectSearch.done();

        ManagedRoutingServersSearch = createSearchBuilder();
        ManagedRoutingServersSearch.and("server", ManagedRoutingServersSearch.entity().getManagementServerId(), SearchCriteria.Op.NNULL);
        ManagedRoutingServersSearch.and("type", ManagedRoutingServersSearch.entity().getType(), SearchCriteria.Op.EQ);
        ManagedRoutingServersSearch.done();
        
        RoutingSearch = createSearchBuilder();
        RoutingSearch.and("type", RoutingSearch.entity().getType(), SearchCriteria.Op.EQ);
        RoutingSearch.done();

        _statusAttr = _allAttributes.get("status");
        _msIdAttr = _allAttributes.get("managementServerId");
        _pingTimeAttr = _allAttributes.get("lastPinged");

        assert (_statusAttr != null && _msIdAttr != null && _pingTimeAttr != null) : "Couldn't find one of these attributes";
    }

    @Override
    public long countBy(long clusterId, Status... statuses) {
        SearchCriteria<HostVO> sc = MaintenanceCountSearch.create();

        sc.setParameters("status", (Object[]) statuses);
        sc.setParameters("cluster", clusterId);

        List<HostVO> hosts = listBy(sc);
        return hosts.size();
    }

    @Override
    public HostVO findSecondaryStorageHost(long dcId) {
        SearchCriteria<HostVO> sc = TypeDcSearch.create();
        sc.setParameters("type", Host.Type.SecondaryStorage);
        sc.setParameters("dc", dcId);
        List<HostVO> storageHosts = listBy(sc);
        if (storageHosts == null || storageHosts.size() < 1) {
            return null;
        } else {
            Collections.shuffle(storageHosts);
            return storageHosts.get(0);
        }
    }

    @Override
    public List<HostVO> listSecondaryStorageHosts() {
        SearchCriteria<HostVO> sc = createSearchCriteria();
        sc.addAnd("type", SearchCriteria.Op.EQ, Host.Type.SecondaryStorage);
        return search(sc, null);
    }

    @Override
    public List<HostVO> listSecondaryStorageHosts(long dataCenterId) {
        SearchCriteria<HostVO> sc = createSearchCriteria();
        sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, dataCenterId);
        sc.addAnd("type", SearchCriteria.Op.EQ, Host.Type.SecondaryStorage);
        return search(sc, null);

    }

    @Override
    public List<HostVO> listLocalSecondaryStorageHosts() {
        SearchCriteria<HostVO> sc = createSearchCriteria();
        sc.addAnd("type", SearchCriteria.Op.EQ, Host.Type.LocalSecondaryStorage);
        return search(sc, null);
    }

    @Override
    public List<HostVO> listLocalSecondaryStorageHosts(long dataCenterId) {
        SearchCriteria<HostVO> sc = createSearchCriteria();
        sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, dataCenterId);
        sc.addAnd("type", SearchCriteria.Op.EQ, Host.Type.LocalSecondaryStorage);
        return search(sc, null);

    }    

    @Override
    public List<HostVO> listAllSecondaryStorageHosts(long dataCenterId) {
        SearchCriteria<HostVO> sc = createSearchCriteria();                            
        sc.addAnd("dataCenterId", SearchCriteria.Op.EQ, dataCenterId);
        sc.addAnd("type", SearchCriteria.Op.IN, new Object[]{Host.Type.LocalSecondaryStorage, Host.Type.SecondaryStorage});
        return search(sc, null);
    }

    @Override
    public List<HostVO> findDirectlyConnectedHosts() {
        SearchCriteria<HostVO> sc = DirectlyConnectedSearch.create();
        return search(sc, null);
    }

    @Override @DB
    public List<HostVO> findAndUpdateDirectAgentToLoad(long lastPingSecondsAfter, Long limit, long managementServerId) {
        Transaction txn = Transaction.currentTxn();
        txn.start();       
    	SearchCriteria<HostVO> sc = UnmanagedDirectConnectSearch.create();
    	sc.setParameters("lastPinged", lastPingSecondsAfter);
        sc.setParameters("statuses", Status.ErrorInMaintenance, Status.Maintenance, Status.PrepareForMaintenance);
        sc.setJoinParameters("ClusterManagedSearch", "managed", Managed.ManagedState.Managed);
        List<HostVO> hosts = lockRows(sc, new Filter(HostVO.class, "clusterId", true, 0L, limit), true);
        
        for (HostVO host : hosts) {
            host.setManagementServerId(managementServerId);
            update(host.getId(), host);
        }
        
        txn.commit();
        
        return hosts;
    }
    
    @Override @DB
    public List<HostVO> findAndUpdateApplianceToLoad(long lastPingSecondsAfter, long managementServerId) {
    	Transaction txn = Transaction.currentTxn();
    	
    	txn.start();
    	SearchCriteria<HostVO> sc = UnmanagedApplianceSearch.create();
    	sc.setParameters("lastPinged", lastPingSecondsAfter);
    	sc.setParameters("types", Type.ExternalDhcp, Type.ExternalFirewall, Type.ExternalLoadBalancer, Type.PxeServer, Type.TrafficMonitor);
    	List<HostVO> hosts = lockRows(sc, null, true);
    	
    	for (HostVO host : hosts) {
    		host.setManagementServerId(managementServerId);
    		update(host.getId(), host);
    	}
    	
    	txn.commit();
    	
    	return hosts;
    }

    @Override
    public void markHostsAsDisconnected(long msId, long lastPing) {
        SearchCriteria<HostVO> sc = MsStatusSearch.create();
        sc.setParameters("ms", msId);
        sc.setParameters("statuses", Status.ErrorInMaintenance, Status.Maintenance, Status.PrepareForMaintenance);

        HostVO host = createForUpdate();
        host.setLastPinged(lastPing);
        host.setDisconnectedOn(new Date());
        UpdateBuilder ub = getUpdateBuilder(host);
        ub.set(host, "status", Status.Disconnected);

        update(ub, sc, null);

        sc = MsStatusSearch.create();
        sc.setParameters("ms", msId);

        host = createForUpdate();
        host.setManagementServerId(null);
        host.setLastPinged((System.currentTimeMillis() >> 10) - (10 * 60));
        host.setDisconnectedOn(new Date());
        ub = getUpdateBuilder(host);
        update(ub, sc, null);
    }

    @Override
    public List<HostVO> listBy(Host.Type type, Long clusterId, Long podId, long dcId) {
        SearchCriteria<HostVO> sc = TypePodDcStatusSearch.create();
        if (type != null) {
            sc.setParameters("type", type.toString());
        }
        if (clusterId != null) {
            sc.setParameters("cluster", clusterId);
        }
        if (podId != null) {
            sc.setParameters("pod", podId);
        }
        sc.setParameters("dc", dcId);
        sc.setParameters("status", Status.Up.toString());

        return listBy(sc);
    }

    @Override
    public List<HostVO> listByInAllStatus(Host.Type type, Long clusterId, Long podId, long dcId) {
        SearchCriteria<HostVO> sc = TypePodDcStatusSearch.create();
        if ( type != null ) {
            sc.setParameters("type", type.toString());
        }
        if (clusterId != null) {
            sc.setParameters("cluster", clusterId);
        }
        if (podId != null ) {
            sc.setParameters("pod", podId);
        }
        sc.setParameters("dc", dcId);

        return listBy(sc);
    }
    
    @Override
    public List<HostVO> listBy(Long clusterId, Long podId, long dcId) {
        SearchCriteria<HostVO> sc = TypePodDcStatusSearch.create();
        if (podId != null) {
            sc.setParameters("pod", podId);
        }
        if (clusterId != null) {
            sc.setParameters("cluster", clusterId);
        }
        sc.setParameters("dc", dcId);
        return listBy(sc);
    }

    @Override
    public List<HostVO> listByHostTag(Host.Type type, Long clusterId, Long podId, long dcId, String hostTag) {

        SearchBuilder<HostTagVO> hostTagSearch = _hostTagsDao.createSearchBuilder();
        HostTagVO tagEntity = hostTagSearch.entity();
        hostTagSearch.and("tag", tagEntity.getTag(), SearchCriteria.Op.EQ);

        SearchBuilder<HostVO> hostSearch = createSearchBuilder();
        HostVO entity = hostSearch.entity();
        hostSearch.and("type", entity.getType(), SearchCriteria.Op.EQ);
        hostSearch.and("pod", entity.getPodId(), SearchCriteria.Op.EQ);
        hostSearch.and("dc", entity.getDataCenterId(), SearchCriteria.Op.EQ);
        hostSearch.and("cluster", entity.getClusterId(), SearchCriteria.Op.EQ);
        hostSearch.and("status", entity.getStatus(), SearchCriteria.Op.EQ);
        hostSearch.join("hostTagSearch", hostTagSearch, entity.getId(), tagEntity.getHostId(), JoinBuilder.JoinType.INNER);

        SearchCriteria<HostVO> sc = hostSearch.create();
        sc.setJoinParameters("hostTagSearch", "tag", hostTag);
        sc.setParameters("type", type.toString());
        if (podId != null) {
            sc.setParameters("pod", podId);
        }
        if (clusterId != null) {
            sc.setParameters("cluster", clusterId);
        }
        sc.setParameters("dc", dcId);
        sc.setParameters("status", Status.Up.toString());

        return listBy(sc);
    }

    @Override
    public List<HostVO> listByCluster(long clusterId) {
        SearchCriteria<HostVO> sc = ClusterStatusSearch.create();

        sc.setParameters("cluster", clusterId);

        return listBy(sc);
    }
    

    @Override
    public List<HostVO> listByClusterStatus(long clusterId, Status status) {
        SearchCriteria<HostVO> sc = ClusterStatusSearch.create();

        sc.setParameters("cluster", clusterId);
        sc.setParameters("status", status.toString());

        return listBy(sc);
    }


    @Override
    public List<HostVO> listBy(Host.Type type, long dcId) {
        SearchCriteria<HostVO> sc = TypeDcStatusSearch.create();
        sc.setParameters("type", type.toString());
        sc.setParameters("dc", dcId);
        sc.setParameters("status", Status.Up.toString());

        return listBy(sc);
    }

    @Override
    public List<HostVO> listAllBy(Host.Type type, long dcId) {
        SearchCriteria<HostVO> sc = TypeDcSearch.create();
        sc.setParameters("type", type.toString());
        sc.setParameters("dc", dcId);

        return listBy(sc);
    }

    @Override
    public HostVO findByPrivateIpAddressInDataCenter(long dcId, String privateIpAddress) {
        SearchCriteria<HostVO> sc = DcPrivateIpAddressSearch.create();
        sc.setParameters("dc", dcId);
        sc.setParameters("privateIpAddress", privateIpAddress);

        return findOneBy(sc);
    }

    @Override
    public HostVO findByStorageIpAddressInDataCenter(long dcId, String privateIpAddress) {
        SearchCriteria<HostVO> sc = DcStorageIpAddressSearch.create();
        sc.setParameters("dc", dcId);
        sc.setParameters("storageIpAddress", privateIpAddress);

        return findOneBy(sc);
    }

    @Override
    public void loadDetails(HostVO host) {
        Map<String, String> details = _detailsDao.findDetails(host.getId());
        host.setDetails(details);
    }

    @Override
    public void loadHostTags(HostVO host) {
        List<String> hostTags = _hostTagsDao.gethostTags(host.getId());
        host.setHostTags(hostTags);
    }

    @Override
    public boolean directConnect(HostVO host, long msId) {
        SearchCriteria<HostVO> sc = DirectConnectSearch.create();
        sc.setParameters("id", host.getId());
        sc.setParameters("server", msId);

        host.setManagementServerId(msId);
        host.setLastPinged(System.currentTimeMillis() >> 10);
        UpdateBuilder ub = getUpdateBuilder(host);
        ub.set(host, _statusAttr, Status.Connecting);

        return update(host, sc) > 0;
    }

    @Override
    public boolean updateStatus(HostVO host, Event event, long msId) {
        if (host == null) {
            return false;
        }

        Status oldStatus = host.getStatus();
        long oldPingTime = host.getLastPinged();
        Status newStatus = oldStatus.getNextStatus(event);

        if (newStatus == null) {
            return false;
        }

        SearchBuilder<HostVO> sb = createSearchBuilder();
        sb.and("status", sb.entity().getStatus(), SearchCriteria.Op.EQ);
        sb.and("id", sb.entity().getId(), SearchCriteria.Op.EQ);
        if (newStatus.checkManagementServer()) {
            sb.and("ping", sb.entity().getLastPinged(), SearchCriteria.Op.EQ);
            sb.and().op("nullmsid", sb.entity().getManagementServerId(), SearchCriteria.Op.NULL);
            sb.or("msid", sb.entity().getManagementServerId(), SearchCriteria.Op.EQ);
            sb.closeParen();
        }
        sb.done();

        SearchCriteria<HostVO> sc = sb.create();

        sc.setParameters("status", oldStatus);
        sc.setParameters("id", host.getId());
        if (newStatus.checkManagementServer()) {
            sc.setParameters("ping", oldPingTime);
            sc.setParameters("msid", msId);
        }

        UpdateBuilder ub = getUpdateBuilder(host);
        ub.set(host, _statusAttr, newStatus);
        if (newStatus.updateManagementServer()) {
            if (newStatus.lostConnection()) {
                ub.set(host, _msIdAttr, null);
            } else {
                ub.set(host, _msIdAttr, msId);
            }
            if (event.equals(Event.Ping) || event.equals(Event.AgentConnected)) {
                ub.set(host, _pingTimeAttr, System.currentTimeMillis() >> 10);
            }
        }
        if (event.equals(Event.ManagementServerDown)) {
            ub.set(host, _pingTimeAttr, ((System.currentTimeMillis() >> 10) - (10 * 60)));
        }
        int result = update(ub, sc, null);
        
        assert result <= 1 : "How can this update " + result + " rows? ";
        if (result < 1) {
            s_logger.warn("Unable to update db record for host id=" + host.getId() + "; it's possible that the host is removed");
        }

        if (s_logger.isDebugEnabled() && result == 0) {
            HostVO vo = findById(host.getId());
      
            if (vo != null) {
                StringBuilder str = new StringBuilder("Unable to update host for event:").append(event.toString());
                str.append(". New=[status=").append(newStatus.toString()).append(":msid=").append(newStatus.lostConnection() ? "null" : msId).append(":lastpinged=").append(host.getLastPinged())
                .append("]");
                str.append("; Old=[status=").append(oldStatus.toString()).append(":msid=").append(msId).append(":lastpinged=").append(oldPingTime).append("]");
                str.append("; DB=[status=").append(vo.getStatus().toString()).append(":msid=").append(vo.getManagementServerId()).append(":lastpinged=").append(vo.getLastPinged()).append("]");
                s_logger.debug(str.toString());
            } else {
                s_logger.warn("Can't find host db record by id=" + host.getId() + "; host might be already marked as removed");
            }

        }
        return result > 0;
    }

    @Override
    public boolean disconnect(HostVO host, Event event, long msId) {
        host.setDisconnectedOn(new Date());
        if (event != null && event.equals(Event.Remove)) {
            host.setGuid(null);
            host.setClusterId(null);
        }
        return updateStatus(host, event, msId);
    }

    @Override
    @DB
    public boolean connect(HostVO host, long msId) {
        Transaction txn = Transaction.currentTxn();
        long id = host.getId();
        txn.start();

        if (!updateStatus(host, Event.AgentConnected, msId)) {
            return false;
        }

        txn.commit();
        return true;
    }

    @Override
    public HostVO findByGuid(String guid) {
        SearchCriteria<HostVO> sc = GuidSearch.create("guid", guid);
        return findOneBy(sc);
    }

    @Override
    public HostVO findByName(String name) {
        SearchCriteria<HostVO> sc = NameSearch.create("name", name);
        return findOneBy(sc);
    }

    @DB
    @Override
    public List<HostVO> findLostHosts(long timeout) {
        Transaction txn = Transaction.currentTxn();
        PreparedStatement pstmt = null;
        List<HostVO> result = new ArrayList<HostVO>();
        ResultSet rs = null;
        try {
            String sql = "select h.id from host h left join  cluster c on h.cluster_id=c.id where h.mgmt_server_id is not null and h.last_ping < ? and h.status in ('Up', 'Updating', 'Disconnected', 'Connecting') and h.type not in ('ExternalFirewall', 'ExternalLoadBalancer', 'TrafficMonitor', 'SecondaryStorage', 'LocalSecondaryStorage') and (h.cluster_id is null or c.managed_state = 'Managed') ;" ;
            pstmt = txn.prepareStatement(sql);
            pstmt.setLong(1, timeout);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1); //ID column
                result.add(findById(id));
            }
        } catch (Exception e) {
            s_logger.warn("Exception: ", e);
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
        return result;
    }

    @Override
    public List<HostVO> findHostsLike(String hostName) {
        SearchCriteria<HostVO> sc = NameLikeSearch.create();
        sc.setParameters("name", "%" + hostName + "%");
        return listBy(sc);
    }



    @Override
    public List<HostVO> listByDataCenter(long dcId) {
        SearchCriteria<HostVO> sc = DcSearch.create("dc", dcId);
        return listBy(sc);
    }

    @Override
    public HostVO findConsoleProxyHost(String name, Type type) {
        SearchCriteria<HostVO> sc = ConsoleProxyHostSearch.create();
        sc.setParameters("name", name);
        sc.setParameters("type", type);
        List<HostVO> hostList = listBy(sc);

        if (hostList == null || hostList.size() == 0) {
            return null;
        } else {
            return hostList.get(0);
        }
    }

    @Override
    public List<HostVO> listByHostPod(long podId) {
        SearchCriteria<HostVO> sc = PodSearch.create("pod", podId);
        return listBy(sc);
    }

    @Override
    public List<HostVO> listByStatus(Status... status) {
        SearchCriteria<HostVO> sc = StatusSearch.create();
        sc.setParameters("status", (Object[]) status);
        return listBy(sc);
    }

    @Override
    public List<HostVO> listByTypeDataCenter(Type type, long dcId) {
        SearchCriteria<HostVO> sc = TypeDcSearch.create();
        sc.setParameters("type", type.toString());
        sc.setParameters("dc", dcId);

        return listBy(sc);
    }

    @Override
    public List<HostVO> listSecondaryStorageVM(long dcId) {
        SearchCriteria<HostVO> sc = SecondaryStorageVMSearch.create();
        sc.setParameters("type", Type.SecondaryStorageVM);
        sc.setParameters("status", Status.Up);
        sc.setParameters("dc", dcId);

        return listBy(sc);
    }
    
    @Override
    public List<HostVO> listSecondaryStorageVMInUpAndConnecting() {
        SearchCriteria<HostVO> sc = SecondaryStorageVMSearch.create();
        sc.setParameters("type", Type.SecondaryStorageVM);
        sc.setParameters("status", Status.Up, Status.Connecting);

        return listBy(sc);
    }
    
    @Override
    public List<HostVO> listByType(Type type) {
        SearchCriteria<HostVO> sc = TypeSearch.create();
        sc.setParameters("type", type.toString());
        return listBy(sc);
    }

    @Override
    public void saveDetails(HostVO host) {
        Map<String, String> details = host.getDetails();
        if (details == null) {
            return;
        }
        _detailsDao.persist(host.getId(), details);
    }

    protected void saveHostTags(HostVO host) {
        List<String> hostTags = host.getHostTags();
        if (hostTags == null || (hostTags != null && hostTags.isEmpty())) {
            return;
        }
        _hostTagsDao.persist(host.getId(), hostTags);
    }

    @Override
    @DB
    public HostVO persist(HostVO host) {
        final String InsertSequenceSql = "INSERT INTO op_host(id) VALUES(?)";

        Transaction txn = Transaction.currentTxn();
        txn.start();

        HostVO dbHost = super.persist(host);

        try {
            PreparedStatement pstmt = txn.prepareAutoCloseStatement(InsertSequenceSql);
            pstmt.setLong(1, dbHost.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to persist the sequence number for this host");
        }

        saveDetails(host);
        loadDetails(dbHost);
        saveHostTags(host);
        loadHostTags(dbHost);

        txn.commit();

        return dbHost;
    }

    @Override
    @DB
    public boolean update(Long hostId, HostVO host) {
        Transaction txn = Transaction.currentTxn();
        txn.start();

        boolean persisted = super.update(hostId, host);
        if (!persisted) {
            return persisted;
        }

        saveDetails(host);
        saveHostTags(host);

        txn.commit();

        return persisted;
    }

    @Override
    @DB
    public List<RunningHostCountInfo> getRunningHostCounts(Date cutTime) {
        String sql = "select * from (" + "select h.data_center_id, h.type, count(*) as count from host as h INNER JOIN mshost as m ON h.mgmt_server_id=m.msid "
                + "where h.status='Up' and h.type='SecondaryStorage' and m.last_update > ? " + "group by h.data_center_id, h.type " + "UNION ALL "
                + "select h.data_center_id, h.type, count(*) as count from host as h INNER JOIN mshost as m ON h.mgmt_server_id=m.msid "
                + "where h.status='Up' and h.type='Routing' and m.last_update > ? " + "group by h.data_center_id, h.type) as t " + "ORDER by t.data_center_id, t.type";

        ArrayList<RunningHostCountInfo> l = new ArrayList<RunningHostCountInfo>();

        Transaction txn = Transaction.currentTxn();
        ;
        PreparedStatement pstmt = null;
        try {
            pstmt = txn.prepareAutoCloseStatement(sql);
            String gmtCutTime = DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), cutTime);
            pstmt.setString(1, gmtCutTime);
            pstmt.setString(2, gmtCutTime);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                RunningHostCountInfo info = new RunningHostCountInfo();
                info.setDcId(rs.getLong(1));
                info.setHostType(rs.getString(2));
                info.setCount(rs.getInt(3));

                l.add(info);
            }
        } catch (SQLException e) {
        } catch (Throwable e) {
        }
        return l;
    }

    @Override
    public long getNextSequence(long hostId) {
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("getNextSequence(), hostId: " + hostId);
        }

        TableGenerator tg = _tgs.get("host_req_sq");
        assert tg != null : "how can this be wrong!";

        return s_seqFetcher.getNextSequence(Long.class, tg, hostId);
    }

    @Override
    public List<HypervisorType> getAvailHypervisorInZone(Long hostId, Long zoneId) {
        SearchCriteria<HostVO> sc = AvailHypevisorInZone.create();
        if ( zoneId != null ) {
            sc.setParameters("zoneId", zoneId);
        }
        if ( hostId != null ) {
            sc.setParameters("hostId", hostId);
        }
        sc.setParameters("type", Host.Type.Routing);
        List<HostVO> hosts = listBy(sc);
        List<HypervisorType> hypers = new ArrayList<HypervisorType>(4);
        for (HostVO host : hosts) {
            hypers.add(host.getHypervisorType());
        }
        return hypers;
    }

    @Override
    public List<Long> listBy(Long dataCenterId, Long podId, Long clusterId, Type hostType, Status... statuses) {
        SearchCriteria<Long> sc = HostsInStatusSearch.create();
        if (dataCenterId != null) {
            sc.setParameters("dc", dataCenterId);
        }

        if (podId != null) {
            sc.setParameters("pod", podId);
        }

        if (clusterId != null) {
            sc.setParameters("cluster", clusterId);
        }

        if (hostType != null) {
            sc.setParameters("type", hostType);
        }

        sc.setParameters("statuses", (Object[]) statuses);

        return customSearch(sc, null);
    }

    @Override
    public long countRoutingHostsByDataCenter(long dcId) {
        SearchCriteria<Long> sc = CountRoutingByDc.create();
        sc.setParameters("dc", dcId);
        sc.setParameters("type", Host.Type.Routing);
        sc.setParameters("status", Status.Up.toString());
        return customSearch(sc, null).get(0);
    }

    @Override
    public HostVO findTrafficMonitorHost() {
        SearchCriteria<HostVO> sc = TypeSearch.create();
        sc.setParameters("type", Host.Type.TrafficMonitor);
        List<HostVO> trafficHosts = listBy(sc);

        if (trafficHosts == null || trafficHosts.size() < 1) {
            return null;
        } else {
            return trafficHosts.get(0);
        }
    }

    @Override
    public List<HostVO> listDirectHostsBy(long msId, Status status) {
        SearchCriteria<HostVO> sc = DirectlyConnectedSearch.create();
        sc.setParameters("ms", msId);
        if (status != null) {
            sc.setParameters("statuses", Status.Up);
        }

        return listBy(sc);
    }

    @Override
    public List<HostVO> listManagedDirectAgents() {
        SearchCriteria<HostVO> sc = ManagedDirectConnectSearch.create();
        return listBy(sc);
    }

    @Override
    public List<HostVO> listManagedRoutingAgents() {
        SearchCriteria<HostVO> sc = ManagedRoutingServersSearch.create();
        sc.setParameters("type", Type.Routing);
        return listBy(sc);
    }

    @Override
    public List<HostVO> listRoutingHostsByManagementServer(long msId) {
        SearchCriteria<HostVO> sc = MsStatusSearch.create();
        sc.setParameters("ms", msId);
        sc.setParameters("type", Type.Routing);

        return listBy(sc);
    }

    @Override
    public List<HostVO> listAllRoutingAgents() {
        SearchCriteria<HostVO> sc = RoutingSearch.create();
        sc.setParameters("type", Type.Routing);
        return listBy(sc);
    }
}
