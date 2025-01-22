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

import com.cloud.utils.crypt.DBEncryptionUtil;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.ConfigKey.Scope;
import org.apache.cloudstack.framework.config.ScopedConfigStorage;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.framework.config.impl.ConfigurationVO;
import org.apache.cloudstack.resourcedetail.ResourceDetailsDaoBase;

import com.cloud.dc.DataCenterDetailVO;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

import javax.inject.Inject;

public class DataCenterDetailsDaoImpl extends ResourceDetailsDaoBase<DataCenterDetailVO> implements DataCenterDetailsDao, ScopedConfigStorage {

    @Inject
    private ConfigurationDao _configDao;

    private final SearchBuilder<DataCenterDetailVO> DetailSearch;

    DataCenterDetailsDaoImpl() {
        DetailSearch = createSearchBuilder();
        DetailSearch.and("zoneId", DetailSearch.entity().getResourceId(), SearchCriteria.Op.EQ);
        DetailSearch.and("name", DetailSearch.entity().getName(), SearchCriteria.Op.EQ);
        DetailSearch.done();
    }

    @Override
    public Scope getScope() {
        return ConfigKey.Scope.Zone;
    }

    @Override
    public String getConfigValue(long id, ConfigKey<?> key) {
        DataCenterDetailVO vo = findDetail(id, key.key());
        return vo == null ? null : getActualValue(vo);
    }

    @Override
    public void addDetail(long resourceId, String key, String value, boolean display) {
        super.addDetail(new DataCenterDetailVO(resourceId, key, value, display));
    }

    @Override
    public void persist(long zoneId, String name, String value) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        SearchCriteria<DataCenterDetailVO> sc = DetailSearch.create();
        sc.setParameters("zoneId", zoneId);
        sc.setParameters("name", name);
        expunge(sc);

        DataCenterDetailVO vo = new DataCenterDetailVO(zoneId, name, value, true);
        persist(vo);
        txn.commit();
    }

    @Override
    public String getActualValue(DataCenterDetailVO dataCenterDetailVO) {
        ConfigurationVO configurationVO = _configDao.findByName(dataCenterDetailVO.getName());
        if (configurationVO != null && configurationVO.isEncrypted()) {
            return DBEncryptionUtil.decrypt(dataCenterDetailVO.getValue());
        }
        return dataCenterDetailVO.getValue();
    }
}
