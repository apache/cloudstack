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
package com.cloud.host.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.cloud.host.DetailVO;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class HostDetailsDaoImpl extends GenericDaoBase<DetailVO, Long> implements HostDetailsDao {
    protected final SearchBuilder<DetailVO> HostSearch;
    protected final SearchBuilder<DetailVO> DetailSearch;
    protected final SearchBuilder<DetailVO> DetailNameSearch;

    public HostDetailsDaoImpl() {
        HostSearch = createSearchBuilder();
        HostSearch.and("hostId", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        HostSearch.done();

        DetailSearch = createSearchBuilder();
        DetailSearch.and("hostId", DetailSearch.entity().getHostId(), SearchCriteria.Op.EQ);
        DetailSearch.and("name", DetailSearch.entity().getName(), SearchCriteria.Op.EQ);
        DetailSearch.done();

        DetailNameSearch = createSearchBuilder();
        DetailNameSearch.and("name", DetailNameSearch.entity().getName(), SearchCriteria.Op.EQ);
        DetailNameSearch.done();
    }

    @Override
    public DetailVO findDetail(long hostId, String name) {
        SearchCriteria<DetailVO> sc = DetailSearch.create();
        sc.setParameters("hostId", hostId);
        sc.setParameters("name", name);

        DetailVO detail = findOneIncludingRemovedBy(sc);
        if ("password".equals(name) && detail != null) {
            detail.setValue(DBEncryptionUtil.decrypt(detail.getValue()));
        }
        return detail;
    }

    @Override
    public Map<String, String> findDetails(long hostId) {
        SearchCriteria<DetailVO> sc = HostSearch.create();

        sc.setParameters("hostId", hostId);

        List<DetailVO> results = search(sc, null);

        Map<String, String> details = new HashMap<String, String>(results.size());

        for (DetailVO result : results) {
            if ("password".equals(result.getName())) {
                details.put(result.getName(), DBEncryptionUtil.decrypt(result.getValue()));
            } else {
                details.put(result.getName(), result.getValue());
            }
        }

        return details;
    }

    @Override
    public void deleteDetails(long hostId) {
        SearchCriteria sc = HostSearch.create();
        sc.setParameters("hostId", hostId);

        List<DetailVO> results = search(sc, null);
        for (DetailVO result : results) {
            remove(result.getId());
        }
    }

    @Override
    public void persist(long hostId, Map<String, String> details) {
        final String InsertOrUpdateSql = "INSERT INTO `cloud`.`host_details` (host_id, name, value) VALUES (?,?,?) ON DUPLICATE KEY UPDATE value=?";

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        txn.start();

        for (Map.Entry<String, String> detail : details.entrySet()) {
            String value = detail.getValue();
            if ("password".equals(detail.getKey())) {
                value = DBEncryptionUtil.encrypt(value);
            }
            try {
                PreparedStatement pstmt = txn.prepareAutoCloseStatement(InsertOrUpdateSql);
                pstmt.setLong(1, hostId);
                pstmt.setString(2, detail.getKey());
                pstmt.setString(3, value);
                pstmt.setString(4, value);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                throw new CloudRuntimeException("Unable to persist the host_details key: " + detail.getKey() + " for host id: " + hostId, e);
            }
        }
        txn.commit();
    }

    @Override
    public List<DetailVO> findByName(String name) {
        SearchCriteria<DetailVO> sc = DetailNameSearch.create();
        sc.setParameters("name", name);
        return listBy(sc);
    }
}
