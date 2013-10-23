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
package com.cloud.dc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.ConfigKey.Scope;
import org.apache.cloudstack.framework.config.ScopedConfigStorage;

import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Local(value=ClusterDetailsDao.class)
public class ClusterDetailsDaoImpl extends GenericDaoBase<ClusterDetailsVO, Long> implements ClusterDetailsDao, ScopedConfigStorage {
    protected final SearchBuilder<ClusterDetailsVO> ClusterSearch;
    protected final SearchBuilder<ClusterDetailsVO> DetailSearch;

    protected ClusterDetailsDaoImpl() {
        ClusterSearch = createSearchBuilder();
        ClusterSearch.and("clusterId", ClusterSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
        ClusterSearch.done();

        DetailSearch = createSearchBuilder();
        DetailSearch.and("clusterId", DetailSearch.entity().getClusterId(), SearchCriteria.Op.EQ);
        DetailSearch.and("name", DetailSearch.entity().getName(), SearchCriteria.Op.EQ);
        DetailSearch.done();
    }

    @Override
    public ClusterDetailsVO findDetail(long clusterId, String name) {
        SearchCriteria<ClusterDetailsVO> sc = DetailSearch.create();
        // This is temporary fix to support list/update configuration api for cpu and memory overprovisioning ratios
        if(name.equalsIgnoreCase("cpu.overprovisioning.factor")) {
            name = "cpuOvercommitRatio";
        }
        if (name.equalsIgnoreCase("mem.overprovisioning.factor")) {
            name = "memoryOvercommitRatio";
        }
        sc.setParameters("clusterId", clusterId);
        sc.setParameters("name", name);


        ClusterDetailsVO detail = findOneIncludingRemovedBy(sc);
        if("password".equals(name) && detail != null){
            detail.setValue(DBEncryptionUtil.decrypt(detail.getValue()));
        }
        return detail;
    }


    @Override
    public Map<String, String> findDetails(long clusterId) {
        SearchCriteria<ClusterDetailsVO> sc = ClusterSearch.create();
        sc.setParameters("clusterId", clusterId);

        List<ClusterDetailsVO> results = search(sc, null);
        Map<String, String> details = new HashMap<String, String>(results.size());
        for (ClusterDetailsVO result : results) {
            if("password".equals(result.getName())){
                details.put(result.getName(), DBEncryptionUtil.decrypt(result.getValue()));
            } else {
                details.put(result.getName(), result.getValue());
            }
        }
        return details;
    }

    @Override
    public void deleteDetails(long clusterId) {
        SearchCriteria<ClusterDetailsVO> sc = ClusterSearch.create();
        sc.setParameters("clusterId", clusterId);

        List<ClusterDetailsVO> results = search(sc, null);
        for (ClusterDetailsVO result : results) {
            remove(result.getId());
        }
    }

    @Override
    public void persist(long clusterId, Map<String, String> details) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        SearchCriteria<ClusterDetailsVO> sc = ClusterSearch.create();
        sc.setParameters("clusterId", clusterId);
        expunge(sc);

        for (Map.Entry<String, String> detail : details.entrySet()) {
            String value = detail.getValue();
            if("password".equals(detail.getKey())){
                value = DBEncryptionUtil.encrypt(value);
            }
            ClusterDetailsVO vo = new ClusterDetailsVO(clusterId, detail.getKey(), value);
            persist(vo);
        }
        txn.commit();
    }

    @Override
    public void persist(long clusterId, String name, String value) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();
        SearchCriteria<ClusterDetailsVO> sc = DetailSearch.create();
        sc.setParameters("clusterId", clusterId);
        sc.setParameters("name", name);
        expunge(sc);

        ClusterDetailsVO vo = new ClusterDetailsVO(clusterId, name, value);
        persist(vo);
        txn.commit();
    }

    @Override
    public Scope getScope() {
        return ConfigKey.Scope.Cluster;
    }

    @Override
    public String getConfigValue(long id, ConfigKey<?> key) {
        ClusterDetailsVO vo = findDetail(id, key.key());
        return vo == null ? null : vo.getValue();
    }
}
