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

package com.cloud.network.security.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.network.security.VmRulesetLogVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value={VmRulesetLogDao.class})
public class VmRulesetLogDaoImpl extends GenericDaoBase<VmRulesetLogVO, Long> implements VmRulesetLogDao {
    protected static Logger s_logger = Logger.getLogger(VmRulesetLogDaoImpl.class);
    private SearchBuilder<VmRulesetLogVO> VmIdSearch;
    private String INSERT_OR_UPDATE = "INSERT INTO op_vm_ruleset_log (instance_id, created, logsequence) " +
    		" VALUES(?, now(), 1) ON DUPLICATE KEY UPDATE logsequence=logsequence+1";
    private static HashMap<Integer, String> cachedPrepStmtStrings = new  HashMap<Integer, String>();
    final static private int cacheStringSizes [] = {512, 256, 128, 64, 32, 16, 8, 4, 2, 1};

    static {
        //prepare the cache.
        for (int size: cacheStringSizes) {
            cachedPrepStmtStrings.put(size, createPrepStatementString(size));
        }
    }

    
    private static String createPrepStatementString(int numItems) {
        StringBuilder builder = new StringBuilder("INSERT INTO op_vm_ruleset_log (instance_id, created, logsequence) VALUES ");
        for (int i=0; i < numItems-1; i++) {
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
    
    protected int createOrUpdateUsingMultiInsert(Set<Long> workItems) {
        Transaction txn = Transaction.currentTxn();
        PreparedStatement stmtInsert = null;

        int size = workItems.size();
        int count = 0;
        Iterator<Long> workIter = workItems.iterator();
        int remaining = size;
        try {
            for (int stmtSize : cacheStringSizes) {
                int numStmts = remaining / stmtSize;
                if (numStmts > 0) {
                    String pstmt = cachedPrepStmtStrings.get(stmtSize);
                    stmtInsert = txn.prepareAutoCloseStatement(pstmt);
                    for (int i=0; i < numStmts; i++) {
                        for (int argIndex=1; argIndex <= stmtSize; argIndex++) {
                            Long vmId = workIter.next();
                            stmtInsert.setLong(argIndex, vmId);
                        }
                        int numUpdated = stmtInsert.executeUpdate();
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("Inserted or updated " + numUpdated + " rows");
                        }
                        //Thread.yield();
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
        Transaction txn = Transaction.currentTxn();
        PreparedStatement stmtInsert = null;
        int [] queryResult = null;
        int count=0;
        boolean success = true;
        try {
            stmtInsert = txn.prepareAutoCloseStatement(INSERT_OR_UPDATE);
            
            txn.start();
            for (Long vmId: workItems) {
                stmtInsert.setLong(1, vmId);
                stmtInsert.addBatch();
                count++;
                if (count % 16 ==0) {
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
            Long [] arrayItems = new Long[workItems.size()];
            workItems.toArray(arrayItems);
            for (int i=0; i < queryResult.length; i++) {
                if (queryResult[i] < 0 ) {
                    s_logger.debug("Batch query update failed for vm " + arrayItems[i]);
                }
            }
        } 
        return count;
    }

    
}
