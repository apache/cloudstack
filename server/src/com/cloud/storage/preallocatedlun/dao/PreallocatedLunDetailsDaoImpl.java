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
package com.cloud.storage.preallocatedlun.dao;

import java.util.List;

import javax.ejb.Local;

import com.cloud.storage.preallocatedlun.PreallocatedLunDetailVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchCriteria;

@Local(value=PreallocatedLunDetailsDao.class) @DB(txn=false)
public class PreallocatedLunDetailsDaoImpl extends GenericDaoBase<PreallocatedLunDetailVO, Long> implements PreallocatedLunDetailsDao {
    private static final String SearchTagsSql = "SELECT ext_lun_details.ext_lun_id from ext_lun_details where ";
    protected PreallocatedLunDetailsDaoImpl() {
    }
    
    @Override
    public List<Object[]> searchAll(SearchCriteria sc) {
        return searchAll(sc, null);
    }
    
    public List<Long> findLunIdsWithTags(int limit, String... tags) {
        return null;
    }
    /*
    private static final String DetailsSql =
        "SELECT storage_pool_details.pool_id, count(*) from cloud.storage_pool_details where storage_pool_details.pool_id in (";

protected StoragePoolDetailsDaoImpl() {
    super();
}

public List<Long> findPoolIdsByDetails(Map<String, String> details, long... poolIds) {
    StringBuilder sql = new StringBuilder(DetailsSql);
    if (poolIds.length > 0) {
        sql.append("storage_pool_details.pool_id in (");
        for (long poolId : poolIds) {
            sql.append(poolId).append(",");
        }
        sql.delete(sql.length()-1, sql.length());
        sql.append(") AND (");
    }
    
    for (Map.Entry<String, String> detail : details.entrySet()) {
        sql.append("(").append("name=").append(detail.getKey()).append(" AND ");
        sql.append("value=").append(detail.getValue()).append(") OR ");
    }
    sql.delete(sql.length() - 3, sql.length());
    if (poolIds.length > 0) {
        sql.append(")");
    }
    sql.append("GROUP BY storage_pool_details.pool_id");
    Transaction txn = Transaction.currentTxn();
    
    int count = details.size();
    
    PreparedStatement pstmt;
    try {
        pstmt = txn.prepareAutoCloseStatement(sql.toString());
        ResultSet rs = pstmt.executeQuery();
        List<Long> results = new ArrayList<Long>();
        
        while (rs.next()) {
            if (rs.getInt(2) >= count) {
                results.add(rs.getLong(1));
            }
        }
        return results;
    } catch (SQLException e) {
        throw new CloudRuntimeException("Unable to execute " + sql.toString(), e);
    }
}
*/
    
}
