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
package com.cloud.upgrade.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.upgrade.dao.VersionVO.Step;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value=VersionDao.class)
public class VersionDaoImpl extends GenericDaoBase<VersionVO, String> implements VersionDao {
    final GenericSearchBuilder<VersionVO, String> CurrentVersionSearch;
    protected VersionDaoImpl() {
        super();
        
        CurrentVersionSearch = createSearchBuilder(String.class);
        CurrentVersionSearch.select(null, Func.FIRST, CurrentVersionSearch.entity().getVersion());
        CurrentVersionSearch.and("step", CurrentVersionSearch.entity().getStep(), Op.EQ);
        
    }
    
    @DB
    protected String getCurrentVersion() {
        Transaction txn = Transaction.currentTxn();
        try {
            Connection conn = txn.getConnection();
            
            PreparedStatement pstmt = conn.prepareStatement("SHOW TABLES LIKE 'VERSION'");
            ResultSet rs = pstmt.executeQuery();
            if (rs.getRow() == 0) {
                return "2.1.7";
            }
            pstmt.close();
            rs.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to get the current version", e);
        } 
            SearchCriteria<String> sc = CurrentVersionSearch.create();
            
            sc.setParameters("step", Step.Complete);
            Filter filter = new Filter(VersionVO.class, "updated", true, 0l, 1l);
            
            List<String> vers = customSearch(sc, filter);
            return vers.get(0);
    }
    
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        
        
        return true;
    }
}
