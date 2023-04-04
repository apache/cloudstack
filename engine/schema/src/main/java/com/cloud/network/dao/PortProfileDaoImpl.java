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
package com.cloud.network.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
@DB()
public class PortProfileDaoImpl extends GenericDaoBase<PortProfileVO, Long> implements PortProfileDao {
    protected static final Logger s_logger = Logger.getLogger(PortProfileDaoImpl.class);

    final SearchBuilder<PortProfileVO> nameSearch;
    final SearchBuilder<PortProfileVO> accessVlanSearch;

    public PortProfileDaoImpl() {
        super();

        nameSearch = createSearchBuilder();
        nameSearch.and("portProfileName", nameSearch.entity().getPortProfileName(), Op.EQ);
        nameSearch.done();

        accessVlanSearch = createSearchBuilder();
        accessVlanSearch.and("accessVlanId", accessVlanSearch.entity().getAccessVlanId(), Op.EQ);
        accessVlanSearch.done();
    }

    @Override
    public PortProfileVO findByName(String portProfileName) {
        SearchCriteria<PortProfileVO> sc = nameSearch.create();
        sc.setParameters("portProfileName", portProfileName);
        return findOneBy(sc);
    }

    @Override
    @DB
    public boolean doesVlanRangeClash(int lowVlanId, int highVlanId) {
        String dbName = "cloud";
        String tableName = "port_profile";
        String condition =
            "(trunk_low_vlan_id BETWEEN " + lowVlanId + " AND " + highVlanId + ")" + " OR (trunk_high_vlan_id BETWEEN " + lowVlanId + " AND " + highVlanId + ")";
        String selectSql = "SELECT * FROM `" + dbName + "`.`" + tableName + "` WHERE " + condition;

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(selectSql);
            ResultSet rs = stmt.executeQuery();
            if (rs != null && rs.next()) {
                // There are records that contain vlans in this range, so return true
                return true;
            }
        } catch (SQLException ex) {
            throw new CloudRuntimeException("Failed to execute SQL query to check for vlan range clash");
        }
        return false;
    }

    @Override
    public List<PortProfileVO> listByVlanId(int vlanId) {
        SearchCriteria<PortProfileVO> sc = accessVlanSearch.create();
        sc.setParameters("accessVlanId", vlanId);
        return search(sc, null);
    }

}
