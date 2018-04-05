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
package com.cloud.network.security.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLTransactionRollbackException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.cloud.network.security.VmRulesetLogVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.TransactionLegacy;

@Component
public class VmRulesetLogDaoImpl extends GenericDaoBase<VmRulesetLogVO, Long> implements VmRulesetLogDao {
    protected static final Logger s_logger = Logger.getLogger(VmRulesetLogDaoImpl.class);
    private SearchBuilder<VmRulesetLogVO> VmIdSearch;
    private String InsertOrUpdateSQl = "INSERT INTO op_vm_ruleset_log (instance_id, created, logsequence) "
        + " VALUES(?, now(), 1) ON DUPLICATE KEY UPDATE logsequence=logsequence+1";
    private static HashMap<Integer, String> cachedPrepStmtStrings = new HashMap<Integer, String>();
    final static private int cacheStringSizes[] = {512, 256, 128, 64, 32, 16, 8, 4, 2, 1};

    static {
        //prepare the cache.
        for (int size : cacheStringSizes) {
            cachedPrepStmtStrings.put(size, createPrepStatementString(size));
        }
    }

    private static String createPrepStatementString(int numItems) {
        StringBuilder builder = new StringBuilder("INSERT INTO op_vm_ruleset_log (instance_id, created, logsequence) VALUES ");
        for (int i = 0; i < numItems - 1; i++) {
            builder.append("(?, now(), 1), ");
        }
        builder.append("(?, now(), 1) ");
        builder.append(" ON DUPLICATE KEY UPDATE logsequence=logsequence+1");
        return builder.toString();
    }

    protected VmRulesetLogDaoImpl() {
        VmIdSearch = createSearchBuilder();
        VmIdSearch.and("vmId", VmIdSearch.entity().getInstanceId(), SearchCriteria.Op.EQ);

        VmIdSearch.done();

    }

    @Override
    public VmRulesetLogVO findByVmId(long vmId) {
        SearchCriteria<VmRulesetLogVO> sc = VmIdSearch.create();
        sc.setParameters("vmId", vmId);
        return findOneIncludingRemovedBy(sc);
    }

    @Override
    public int createOrUpdate(Set<Long> workItems) {
        //return createOrUpdateUsingBatch(workItems);
        return createOrUpdateUsingMultiInsert(workItems);
    }

    private int executeWithRetryOnDeadlock(TransactionLegacy txn, String pstmt, List<Long> vmIds) throws SQLException {

        int numUpdated = 0;
        final int maxTries = 3;
        for (int i = 0; i < maxTries; i++) {
            try {
                PreparedStatement stmtInsert = txn.prepareAutoCloseStatement(pstmt);
                int argIndex = 1;
                for (Long vmId : vmIds) {
                    stmtInsert.setLong(argIndex++, vmId);
                }
                numUpdated = stmtInsert.executeUpdate();
                i = maxTries;
            } catch (SQLTransactionRollbackException e1) {
                if (i < maxTries - 1) {
                    int delayMs = (i + 1) * 1000;
                    s_logger.debug("Caught a deadlock exception while inserting security group rule log, retrying in " + delayMs);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        s_logger.debug("[ignored] interupted while inserting security group rule log.");
                    }
                } else
                    s_logger.warn("Caught another deadlock exception while retrying inserting security group rule log, giving up");

            }
        }
        if (s_logger.isTraceEnabled()) {
            s_logger.trace("Inserted or updated " + numUpdated + " rows");
        }
        return numUpdated;
    }

    protected int createOrUpdateUsingMultiInsert(Set<Long> workItems) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();

        int size = workItems.size();
        int count = 0;
        Iterator<Long> workIter = workItems.iterator();
        int remaining = size;
        try {
            for (int stmtSize : cacheStringSizes) {
                int numStmts = remaining / stmtSize;
                if (numStmts > 0) {
                    String pstmt = cachedPrepStmtStrings.get(stmtSize);
                    for (int i = 0; i < numStmts; i++) {
                        List<Long> vmIds = new ArrayList<Long>();
                        for (int argIndex = 1; argIndex <= stmtSize; argIndex++) {
                            Long vmId = workIter.next();
                            vmIds.add(vmId);
                        }
                        int numUpdated = executeWithRetryOnDeadlock(txn, pstmt, vmIds);
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("Inserted or updated " + numUpdated + " rows");
                        }
                        if (numUpdated > 0)
                            count += stmtSize;
                    }
                    remaining = remaining - numStmts * stmtSize;
                }

            }
        } catch (SQLException sqe) {
            s_logger.warn("Failed to execute multi insert ", sqe);
        }

        return count;
    }

    protected int createOrUpdateUsingBatch(Set<Long> workItems) {
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        PreparedStatement stmtInsert = null;
        int[] queryResult = null;
        int count = 0;
        boolean success = true;
        try {
            stmtInsert = txn.prepareAutoCloseStatement(InsertOrUpdateSQl);

            txn.start();
            for (Long vmId : workItems) {
                stmtInsert.setLong(1, vmId);
                stmtInsert.addBatch();
                count++;
                if (count % 16 == 0) {
                    queryResult = stmtInsert.executeBatch();
                    stmtInsert.clearBatch();
                }
            }
            queryResult = stmtInsert.executeBatch();

            txn.commit();
            if (s_logger.isTraceEnabled())
                s_logger.trace("Updated or inserted " + workItems.size() + " log items");
        } catch (SQLException e) {
            s_logger.warn("Failed to execute batch update statement for ruleset log: ", e);
            txn.rollback();
            success = false;
        }
        if (!success && queryResult != null) {
            Long[] arrayItems = new Long[workItems.size()];
            workItems.toArray(arrayItems);
            for (int i = 0; i < queryResult.length; i++) {
                if (queryResult[i] < 0) {
                    s_logger.debug("Batch query update failed for vm " + arrayItems[i]);
                }
            }
        }
        return count;
    }

}
