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
package org.apache.cloudstack.engine.datacenter.entity.api.db.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity;
import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State;
import org.apache.cloudstack.engine.datacenter.entity.api.DataCenterResourceEntity.State.Event;
import org.apache.cloudstack.engine.datacenter.entity.api.db.EngineDataCenterVO;

import com.cloud.org.Grouping;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.db.UpdateBuilder;

/**
 * @config
 * {@table
 *    || Param Name | Description | Values | Default ||
 *    || mac.address.prefix | prefix to attach to all public and private mac addresses | number | 06 ||
 *  }
 **/
@Component(value = "EngineDataCenterDao")
public class EngineDataCenterDaoImpl extends GenericDaoBase<EngineDataCenterVO, Long> implements EngineDataCenterDao {
    private static final Logger s_logger = Logger.getLogger(EngineDataCenterDaoImpl.class);

    protected SearchBuilder<EngineDataCenterVO> NameSearch;
    protected SearchBuilder<EngineDataCenterVO> ListZonesByDomainIdSearch;
    protected SearchBuilder<EngineDataCenterVO> PublicZonesSearch;
    protected SearchBuilder<EngineDataCenterVO> ChildZonesSearch;
    protected SearchBuilder<EngineDataCenterVO> DisabledZonesSearch;
    protected SearchBuilder<EngineDataCenterVO> TokenSearch;
    protected SearchBuilder<EngineDataCenterVO> StateChangeSearch;
    protected SearchBuilder<EngineDataCenterVO> UUIDSearch;

    protected long _prefix;
    protected Random _rand = new Random(System.currentTimeMillis());

    @Inject
    protected DcDetailsDao _detailsDao;

    @Override
    public EngineDataCenterVO findByName(String name) {
        SearchCriteria<EngineDataCenterVO> sc = NameSearch.create();
        sc.setParameters("name", name);
        return findOneBy(sc);
    }

    @Override
    public EngineDataCenterVO findByToken(String zoneToken) {
        SearchCriteria<EngineDataCenterVO> sc = TokenSearch.create();
        sc.setParameters("zoneToken", zoneToken);
        return findOneBy(sc);
    }

    @Override
    public List<EngineDataCenterVO> findZonesByDomainId(Long domainId) {
        SearchCriteria<EngineDataCenterVO> sc = ListZonesByDomainIdSearch.create();
        sc.setParameters("domainId", domainId);
        return listBy(sc);
    }

    @Override
    public List<EngineDataCenterVO> findZonesByDomainId(Long domainId, String keyword) {
        SearchCriteria<EngineDataCenterVO> sc = ListZonesByDomainIdSearch.create();
        sc.setParameters("domainId", domainId);
        if (keyword != null) {
            SearchCriteria<EngineDataCenterVO> ssc = createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }
        return listBy(sc);
    }

    @Override
    public List<EngineDataCenterVO> findChildZones(Object[] ids, String keyword) {
        SearchCriteria<EngineDataCenterVO> sc = ChildZonesSearch.create();
        sc.setParameters("domainid", ids);
        if (keyword != null) {
            SearchCriteria<EngineDataCenterVO> ssc = createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }
        return listBy(sc);
    }

    @Override
    public List<EngineDataCenterVO> listPublicZones(String keyword) {
        SearchCriteria<EngineDataCenterVO> sc = PublicZonesSearch.create();
        if (keyword != null) {
            SearchCriteria<EngineDataCenterVO> ssc = createSearchCriteria();
            ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
            sc.addAnd("name", SearchCriteria.Op.SC, ssc);
        }
        //sc.setParameters("domainId", domainId);
        return listBy(sc);
    }

    @Override
    public List<EngineDataCenterVO> findByKeyword(String keyword) {
        SearchCriteria<EngineDataCenterVO> ssc = createSearchCriteria();
        ssc.addOr("name", SearchCriteria.Op.LIKE, "%" + keyword + "%");
        ssc.addOr("description", SearchCriteria.Op.LIKE, "%" + keyword + "%");
        return listBy(ssc);
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        if (!super.configure(name, params)) {
            return false;
        }

        String value = (String)params.get("mac.address.prefix");
        _prefix = (long)NumbersUtil.parseInt(value, 06) << 40;

        return true;
    }

    protected EngineDataCenterDaoImpl() {
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

        TokenSearch = createSearchBuilder();
        TokenSearch.and("zoneToken", TokenSearch.entity().getZoneToken(), SearchCriteria.Op.EQ);
        TokenSearch.done();

        StateChangeSearch = createSearchBuilder();
        StateChangeSearch.and("id", StateChangeSearch.entity().getId(), SearchCriteria.Op.EQ);
        StateChangeSearch.and("state", StateChangeSearch.entity().getState(), SearchCriteria.Op.EQ);
        StateChangeSearch.done();

        UUIDSearch = createSearchBuilder();
        UUIDSearch.and("uuid", UUIDSearch.entity().getUuid(), SearchCriteria.Op.EQ);
        UUIDSearch.done();
    }

    @Override
    @DB
    public boolean update(Long zoneId, EngineDataCenterVO zone) {
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
    public void loadDetails(EngineDataCenterVO zone) {
        Map<String, String> details = _detailsDao.findDetails(zone.getId());
        zone.setDetails(details);
    }

    @Override
    public void saveDetails(EngineDataCenterVO zone) {
        Map<String, String> details = zone.getDetails();
        if (details == null) {
            return;
        }
        _detailsDao.persist(zone.getId(), details);
    }

    @Override
    public List<EngineDataCenterVO> listDisabledZones() {
        SearchCriteria<EngineDataCenterVO> sc = DisabledZonesSearch.create();
        sc.setParameters("allocationState", Grouping.AllocationState.Disabled);

        List<EngineDataCenterVO> dcs = listBy(sc);

        return dcs;
    }

    @Override
    public List<EngineDataCenterVO> listEnabledZones() {
        SearchCriteria<EngineDataCenterVO> sc = DisabledZonesSearch.create();
        sc.setParameters("allocationState", Grouping.AllocationState.Enabled);

        List<EngineDataCenterVO> dcs = listBy(sc);

        return dcs;
    }

    @Override
    public EngineDataCenterVO findByTokenOrIdOrName(String tokenOrIdOrName) {
        EngineDataCenterVO result = findByToken(tokenOrIdOrName);
        if (result == null) {
            result = findByName(tokenOrIdOrName);
            if (result == null) {
                try {
                    Long dcId = Long.parseLong(tokenOrIdOrName);
                    return findById(dcId);
                } catch (NumberFormatException nfe) {
                    s_logger.debug("Cannot parse " + tokenOrIdOrName + " into long. " + nfe);
                }
            }
        }
        return result;
    }

    @Override
    public boolean remove(Long id) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        EngineDataCenterVO zone = createForUpdate();
        zone.setName(null);

        update(id, zone);

        boolean result = super.remove(id);
        txn.commit();
        return result;
    }

    @Override
    public boolean updateState(State currentState, Event event, State nextState, DataCenterResourceEntity zoneEntity, Object data) {

        EngineDataCenterVO vo = findById(zoneEntity.getId());

        Date oldUpdatedTime = vo.getLastUpdated();

        SearchCriteria<EngineDataCenterVO> sc = StateChangeSearch.create();
        sc.setParameters("id", vo.getId());
        sc.setParameters("state", currentState);

        UpdateBuilder builder = getUpdateBuilder(vo);
        builder.set(vo, "state", nextState);
        builder.set(vo, "lastUpdated", new Date());

        int rows = update(vo, sc);

        if (rows == 0 && s_logger.isDebugEnabled()) {
            EngineDataCenterVO dbDC = findByIdIncludingRemoved(vo.getId());
            if (dbDC != null) {
                StringBuilder str = new StringBuilder("Unable to update ").append(vo.toString());
                str.append(": DB Data={id=").append(dbDC.getId()).append("; state=").append(dbDC.getState()).append(";updatedTime=").append(dbDC.getLastUpdated());
                str.append(": New Data={id=")
                    .append(vo.getId())
                    .append("; state=")
                    .append(nextState)
                    .append("; event=")
                    .append(event)
                    .append("; updatedTime=")
                    .append(vo.getLastUpdated());
                str.append(": stale Data={id=")
                    .append(vo.getId())
                    .append("; state=")
                    .append(currentState)
                    .append("; event=")
                    .append(event)
                    .append("; updatedTime=")
                    .append(oldUpdatedTime);
            } else {
                s_logger.debug("Unable to update dataCenter: id=" + vo.getId() + ", as there is no such dataCenter exists in the database anymore");
            }
        }
        return rows > 0;

    }

}
