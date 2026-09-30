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
package com.cloud.network.ovs.dao;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.stereotype.Component;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
public class VpcDistributedRouterSeqNoDaoImpl extends GenericDaoBase<VpcDistributedRouterSeqNoVO, Long> implements VpcDistributedRouterSeqNoDao {
    private SearchBuilder<VpcDistributedRouterSeqNoVO> VpcIdSearch;

    private static final String INCR_TOPOLOGY_SEQ_SQL =
            "UPDATE `cloud`.`op_vpc_distributed_router_sequence_no` " +
            "SET topology_update_sequence_no = LAST_INSERT_ID(topology_update_sequence_no + 1) WHERE id = ?";

    private static final String INCR_POLICY_SEQ_SQL =
            "UPDATE `cloud`.`op_vpc_distributed_router_sequence_no` " +
            "SET routing_policy__update_sequence_no = LAST_INSERT_ID(routing_policy__update_sequence_no + 1) WHERE id = ?";

    private static final String SELECT_LAST_INSERT_ID_SQL = "SELECT LAST_INSERT_ID()";

    protected VpcDistributedRouterSeqNoDaoImpl() {
        VpcIdSearch = createSearchBuilder();
        VpcIdSearch.and("vmId", VpcIdSearch.entity().getVpcId(), SearchCriteria.Op.EQ);
        VpcIdSearch.done();
    }

    @Override
    public VpcDistributedRouterSeqNoVO findByVpcId(long vpcId) {
        SearchCriteria<VpcDistributedRouterSeqNoVO> sc = VpcIdSearch.create();
        sc.setParameters("vmId", vpcId);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public long incrementAndGetTopologySeqNo(long id) {
        return incrementAndGet(id, INCR_TOPOLOGY_SEQ_SQL, "topology");
    }

    @Override
    public long incrementAndGetPolicySeqNo(long id) {
        return incrementAndGet(id, INCR_POLICY_SEQ_SQL, "policy");
    }

    private long incrementAndGet(long id, String updateSql, String seqType) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            PreparedStatement pstmt = txn.prepareAutoCloseStatement(updateSql);
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
            pstmt = txn.prepareAutoCloseStatement(SELECT_LAST_INSERT_ID_SQL);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            throw new CloudRuntimeException("Failed to retrieve LAST_INSERT_ID after " + seqType + " seq increment for id: " + id);
        } catch (SQLException e) {
            throw new CloudRuntimeException("Error incrementing " + seqType + " sequence for id: " + id, e);
        }
    }

}
