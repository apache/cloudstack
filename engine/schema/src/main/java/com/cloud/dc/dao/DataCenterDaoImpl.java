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
package com.cloud.dc.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.cloud.dc.DataCenter;
import com.cloud.dc.DataCenterDetailVO;
import com.cloud.dc.DataCenterIpAddressVO;
import com.cloud.dc.DataCenterLinkLocalIpAddressVO;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.DataCenterVnetVO;
import com.cloud.dc.PodVlanVO;
import com.cloud.network.dao.AccountGuestVlanMapDao;
import com.cloud.network.dao.AccountGuestVlanMapVO;
import com.cloud.org.Grouping;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

/**
 * @config
 * {@table
 *    || Param Name | Description | Values | Default ||
 *    || mac.address.prefix | prefix to attach to all public and private mac addresses | number | 06 ||
 *  }
 **/
@Component
public class DataCenterDaoImpl extends GenericDaoBase<DataCenterVO, Long> implements DataCenterDao {

    protected SearchBuilder<DataCenterVO> NameSearch;
    protected SearchBuilder<DataCenterVO> ListZonesByDomainIdSearch;
    protected SearchBuilder<DataCenterVO> PublicZonesSearch;
    protected SearchBuilder<DataCenterVO> ChildZonesSearch;
    protected SearchBuilder<DataCenterVO> DisabledZonesSearch;
    protected SearchBuilder<DataCenterVO> TokenSearch;
    protected SearchBuilder<DataCenterVO> ZoneAllocationAndNotTypeSearch;

    @Inject
    protected DataCenterIpAddressDao _ipAllocDao = null;
    @Inject
    protected DataCenterLinkLocalIpAddressDao _linkLocalIpAllocDao = null;
    @Inject
    protected DataCenterVnetDao _vnetAllocDao = null;
    @Inject
    protected PodVlanDao _podVlanAllocDao = null;
    @Inject
    protected DataCenterDetailsDao _detailsDao = null;
    @Inject
    protected AccountGuestVlanMapDao _accountGuestVlanMapDao = null;

    protected long _prefix;
    protected Random _rand = new Random(System.currentTimeMillis());


    @Override
    public DataCenterVO findByName(String name) {
        SearchCriteria<DataCenterVO> sc = NameSearch.create();
        sc.setParameters("name", name);
        return findOneBy(sc);
    }

    @Override
    public DataCenterVO findByToken(String zoneToken) {
        SearchCriteria<DataCenterVO> sc = TokenSearch.create();
        sc.setParameters("zoneToken", zoneToken);
        return findOneBy(sc);
    }

    @Override
    public List<DataCenterVO> findZonesByDomainId(Long domainId) {
        SearchCriteria<DataCenterVO> sc = ListZonesByDomainIdSearch.create();
        sc.setParameters("domainId", domainId);
        return listBy(sc);
    }

    @Override
    public List<DataCenterVO> findZonesByDomainId(Long domainId, String keyword) {
        SearchCriteria<DataCenterVO> sc = ListZonesByDomainIdSearch.create();
        sc.setParameters("domainId", domainId);
        if (keyword != null) {
            SearchCriteria<DataCenterVO> ssc = createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }
        return listBy(sc);
    }

    @Override
    public List<DataCenterVO> findChildZones(Object[] ids, String keyword) {
        SearchCriteria<DataCenterVO> sc = ChildZonesSearch.create();
        sc.setParameters("domainid", ids);
        if (keyword != null) {
            SearchCriteria<DataCenterVO> ssc = createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }
        return listBy(sc);
    }

    @Override
    public List<DataCenterVO> listPublicZones(String keyword) {
        SearchCriteria<DataCenterVO> sc = PublicZonesSearch.create();
        if (keyword != null) {
            SearchCriteria<DataCenterVO> ssc = createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }
        //sc.setParameters("domainId", domainId);
        return listBy(sc);
    }

    @Override
    public List<DataCenterVO> findByKeyword(String keyword) {
        SearchCriteria<DataCenterVO> ssc = createSearchCriteria();
        ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
        ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
        return listBy(ssc);
    }

    @Override
    public void releaseVnet(String vnet, long dcId, long physicalNetworkId, long accountId, String reservationId) {
        _vnetAllocDao.release(vnet, physicalNetworkId, accountId, reservationId);
    }

    @Override
    public List<DataCenterVnetVO> findVnet(long dcId, long physicalNetworkId, String vnet) {
        return _vnetAllocDao.findVnet(dcId, physicalNetworkId, vnet);
    }

    @Override
    public int countZoneVlans(long dcId, boolean onlyCountAllocated) {
        return _vnetAllocDao.countZoneVlans(dcId, onlyCountAllocated);
    }

    @Override
    public void releasePrivateIpAddress(String ipAddress, long dcId, Long nicId) {
        _ipAllocDao.releaseIpAddress(ipAddress, dcId, nicId);
    }

    @Override
    public void releasePrivateIpAddress(long nicId, String reservationId) {
        _ipAllocDao.releaseIpAddress(nicId, reservationId);
    }

    @Override
    public void releaseLinkLocalIpAddress(long nicId, String reservationId) {
        _linkLocalIpAllocDao.releaseIpAddress(nicId, reservationId);
    }

    @Override
    public void releaseLinkLocalIpAddress(String ipAddress, long dcId, Long nicId) {
        _linkLocalIpAllocDao.releaseIpAddress(ipAddress, dcId, nicId);
    }

    @Override
    public boolean deletePrivateIpAddressByPod(long podId) {
        return _ipAllocDao.deleteIpAddressByPod(podId);
    }

    @Override
    public boolean deleteLinkLocalIpAddressByPod(long podId) {
        return _linkLocalIpAllocDao.deleteIpAddressByPod(podId);
    }

    @Override
    public String allocateVnet(long dataCenterId, long physicalNetworkId, long accountId, String reservationId, boolean canUseSystemGuestVlans) {
        ArrayList<Long> dedicatedVlanDbIds = new ArrayList<Long>();
        boolean useDedicatedGuestVlans = false;
        List<AccountGuestVlanMapVO> maps = _accountGuestVlanMapDao.listAccountGuestVlanMapsByAccount(accountId);
        for (AccountGuestVlanMapVO map : maps) {
            dedicatedVlanDbIds.add(map.getId());
        }
        if (dedicatedVlanDbIds != null && !dedicatedVlanDbIds.isEmpty()) {
            useDedicatedGuestVlans = true;
            DataCenterVnetVO vo = _vnetAllocDao.take(physicalNetworkId, accountId, reservationId, dedicatedVlanDbIds);
            if (vo != null)
                return vo.getVnet();
        }
        if (!useDedicatedGuestVlans || (useDedicatedGuestVlans && canUseSystemGuestVlans)) {
            DataCenterVnetVO vo = _vnetAllocDao.take(physicalNetworkId, accountId, reservationId, null);
            if (vo != null) {
                return vo.getVnet();
            }
        }
        return null;
    }

    @Override
    public String allocatePodVlan(long podId, long accountId) {
        PodVlanVO vo = _podVlanAllocDao.take(podId, accountId);
        if (vo == null) {
            return null;
        }
        return vo.getVlan();
    }

    @Override
    public PrivateAllocationData allocatePrivateIpAddress(long dcId, long podId, long nicId, String reservationId, boolean forSystemVms) {
        _ipAllocDao.releaseIpAddress(nicId);
        DataCenterIpAddressVO vo = _ipAllocDao.takeIpAddress(dcId, podId, nicId, reservationId, forSystemVms);
        if (vo == null) {
            return null;
        }
        return new PrivateAllocationData(vo.getIpAddress(), vo.getMacAddress(), vo.getVlan());
    }

    @Override
    public DataCenterIpAddressVO allocatePrivateIpAddress(long dcId, String reservationId) {
        DataCenterIpAddressVO vo = _ipAllocDao.takeDataCenterIpAddress(dcId, reservationId);
        return vo;
    }

    @Override
    public String allocateLinkLocalIpAddress(long dcId, long podId, long nicId, String reservationId) {
        DataCenterLinkLocalIpAddressVO vo = _linkLocalIpAllocDao.takeIpAddress(dcId, podId, nicId, reservationId);
        if (vo == null) {
            return null;
        }
        return vo.getIpAddress();
    }

    @Override
    public void addVnet(long dcId, long physicalNetworkId, List<String> vnets) {
        _vnetAllocDao.add(dcId, physicalNetworkId, vnets);
    }

    @Override
    public void deleteVnet(long physicalNetworkId) {
        _vnetAllocDao.delete(physicalNetworkId);
    }

    @Override
    public List<DataCenterVnetVO> listAllocatedVnets(long physicalNetworkId) {
        return _vnetAllocDao.listAllocatedVnets(physicalNetworkId);
    }

    @Override
    public void addPrivateIpAddress(long dcId, long podId, String start, String end, boolean forSystemVms, Integer vlan) {
        _ipAllocDao.addIpRange(dcId, podId, start, end, forSystemVms, vlan);
    }

    @Override
    public void addLinkLocalIpAddress(long dcId, long podId, String start, String end) {
        _linkLocalIpAllocDao.addIpRange(dcId, podId, start, end);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if (!super.configure(name, params)) {
            return false;
        }

        String value = (String)params.get("mac.address.prefix");
        _prefix = (long)NumbersUtil.parseInt(value, 06) << 40;

        if (!_ipAllocDao.configure("Ip Alloc", params)) {
            return false;
        }

        if (!_vnetAllocDao.configure("vnet Alloc", params)) {
            return false;
        }
        return true;
    }

    public DataCenterDaoImpl() {
        super();
        NameSearch = createSearchBuilder();
        NameSearch.and("name", NameSearch.entity().getName(), SearchCriteria.Op.EQ);
        NameSearch.done();

        ListZonesByDomainIdSearch = createSearchBuilder();
        ListZonesByDomainIdSearch.and("domainId", ListZonesByDomainIdSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        ListZonesByDomainIdSearch.done();

        PublicZonesSearch = createSearchBuilder();
        PublicZonesSearch.and("domainId", PublicZonesSearch.entity().getDomainId(), SearchCriteria.Op.NULL);
        PublicZonesSearch.done();

        ChildZonesSearch = createSearchBuilder();
        ChildZonesSearch.and("domainid", ChildZonesSearch.entity().getDomainId(), SearchCriteria.Op.IN);
        ChildZonesSearch.done();

        DisabledZonesSearch = createSearchBuilder();
        DisabledZonesSearch.and("allocationState", DisabledZonesSearch.entity().getAllocationState(), SearchCriteria.Op.EQ);
        DisabledZonesSearch.done();

        ZoneAllocationAndNotTypeSearch = createSearchBuilder();
        ZoneAllocationAndNotTypeSearch.and("allocationState", ZoneAllocationAndNotTypeSearch.entity().getAllocationState(), SearchCriteria.Op.EQ);
        ZoneAllocationAndNotTypeSearch.and("type", ZoneAllocationAndNotTypeSearch.entity().getType(), SearchCriteria.Op.NLIKE);
        ZoneAllocationAndNotTypeSearch.done();

        TokenSearch = createSearchBuilder();
        TokenSearch.and("zoneToken", TokenSearch.entity().getZoneToken(), SearchCriteria.Op.EQ);
        TokenSearch.done();
    }

    @Override
    @DB
    public boolean update(Long zoneId, DataCenterVO zone) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        boolean persisted = super.update(zoneId, zone);
        if (!persisted) {
            return persisted;
        }
        saveDetails(zone);
        txn.commit();
        return persisted;
    }

    @Override
    public void loadDetails(DataCenterVO zone) {
        Map<String, String> details = _detailsDao.listDetailsKeyPairs(zone.getId());
        zone.setDetails(details);
    }

    @Override
    public void saveDetails(DataCenterVO zone) {
        Map<String, String> details = zone.getDetails();
        if (details == null) {
            return;
        }

        List<DataCenterDetailVO> resourceDetails = new ArrayList<DataCenterDetailVO>();
        for (String key : details.keySet()) {
            resourceDetails.add(new DataCenterDetailVO(zone.getId(), key, details.get(key), true));
        }

        _detailsDao.saveDetails(resourceDetails);
    }

    @Override
    public List<DataCenterVO> listDisabledZones() {
        SearchCriteria<DataCenterVO> sc = DisabledZonesSearch.create();
        sc.setParameters("allocationState", Grouping.AllocationState.Disabled);

        List<DataCenterVO> dcs = listBy(sc);

        return dcs;
    }

    @Override
    public List<DataCenterVO> listEnabledZones() {
        SearchCriteria<DataCenterVO> sc = DisabledZonesSearch.create();
        sc.setParameters("allocationState", Grouping.AllocationState.Enabled);

        List<DataCenterVO> dcs = listBy(sc);

        return dcs;
    }

    @Override
    public List<Long> listEnabledNonEdgeZoneIds() {
        SearchCriteria<DataCenterVO> sc = ZoneAllocationAndNotTypeSearch.create();
        sc.setParameters("allocationState", Grouping.AllocationState.Enabled);
        sc.setParameters("type", DataCenter.Type.Edge);
        List<DataCenterVO> zones = listBy(sc);
        if (CollectionUtils.isEmpty(zones)) {
            return new ArrayList<>();
        }
        return zones.stream().map(DataCenterVO::getId).collect(Collectors.toList());
    }

    @Override
    public DataCenterVO findByTokenOrIdOrName(String tokenOrIdOrName) {
        DataCenterVO result = findByToken(tokenOrIdOrName);
        if (result == null) {
            result = findByName(tokenOrIdOrName);
            if (result == null) {
                try {
                    Long dcId = Long.parseLong(tokenOrIdOrName);
                    return findById(dcId);
                } catch (NumberFormatException nfe) {
                    logger.debug("Cannot parse " + tokenOrIdOrName + " into long. " + nfe);
                }
            }
        }
        return result;
    }

    @Override
    public boolean remove(Long id) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        DataCenterVO zone = createForUpdate();
        zone.setName(null);

        update(id, zone);

        boolean result = super.remove(id);
        txn.commit();
        return result;
    }

    @Override
    public List<DataCenterVO> listAllZones() {
        SearchCriteria<DataCenterVO> sc = NameSearch.create();
        List<DataCenterVO> dcs = listBy(sc);

        return dcs;
    }

    @Override
    public List<DataCenterVO> listByIds(List<Long> ids) {
        SearchBuilder<DataCenterVO> idsSearch = createSearchBuilder();
        idsSearch.and("ids", idsSearch.entity().getId(), SearchCriteria.Op.IN);
        idsSearch.done();
        SearchCriteria<DataCenterVO> sc = idsSearch.create();
        sc.setParameters("ids", ids.toArray());
        return listBy(sc);
    }
}
