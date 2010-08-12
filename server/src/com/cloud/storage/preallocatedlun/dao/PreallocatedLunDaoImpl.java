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

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.storage.preallocatedlun.PreallocatedLunDetailVO;
import com.cloud.storage.preallocatedlun.PreallocatedLunVO;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value=PreallocatedLunDao.class) @DB(txn=false)
public class PreallocatedLunDaoImpl extends GenericDaoBase<PreallocatedLunVO, Long> implements PreallocatedLunDao {
    private static final Logger s_logger = Logger.getLogger(PreallocatedLunDaoImpl.class);
    
    final PreallocatedLunDetailsDao _detailsDao = ComponentLocator.inject(PreallocatedLunDetailsDaoImpl.class);
    
    private final SearchBuilder<PreallocatedLunVO> TakeSearch;
    private final SearchBuilder<PreallocatedLunVO> ReleaseSearch;
    private final SearchBuilder<PreallocatedLunDetailVO> DetailsSearch;
    private final SearchBuilder<PreallocatedLunVO> TotalSizeSearch;
    private final SearchBuilder<PreallocatedLunVO> UsedSizeSearch;
    private final SearchBuilder<PreallocatedLunVO> DeleteSearch;
    
    private final String TakeSqlPrefix = "SELECT ext_lun_alloc.* FROM ext_lun_alloc LEFT JOIN ext_lun_details ON ext_lun_details.ext_lun_id = ext_lun_alloc.id WHERE (ext_lun_alloc.target_iqn=?) AND (ext_lun_alloc.size>=?) AND (ext_lun_alloc.size<=?) AND ";
    private final String TakeSqlSuffix = " ext_lun_alloc.taken IS NULL GROUP BY ext_lun_details.ext_lun_id HAVING COUNT(ext_lun_details.tag) >= ? LIMIT 1 FOR UPDATE";
    
    protected PreallocatedLunDaoImpl() {
        TakeSearch = createSearchBuilder();
        TakeSearch.and("taken", TakeSearch.entity().getTaken(), SearchCriteria.Op.NULL);
        TakeSearch.done();
        
        ReleaseSearch = createSearchBuilder();
        ReleaseSearch.and("lun", ReleaseSearch.entity().getLun(), SearchCriteria.Op.EQ);
        ReleaseSearch.and("target", ReleaseSearch.entity().getTargetIqn(), SearchCriteria.Op.EQ);
        ReleaseSearch.and("taken", ReleaseSearch.entity().getTaken(), SearchCriteria.Op.NNULL);
        ReleaseSearch.and("instanceId", ReleaseSearch.entity().getVolumeId(), SearchCriteria.Op.EQ);
        ReleaseSearch.done();
        
        DetailsSearch = _detailsDao.createSearchBuilder();
        SearchBuilder<PreallocatedLunVO> targetSearch = createSearchBuilder();
        targetSearch.and("targetiqn", targetSearch.entity().getTargetIqn(), SearchCriteria.Op.EQ);
        DetailsSearch.join("target", targetSearch, targetSearch.entity().getId(), DetailsSearch.entity().getLunId());
        DetailsSearch.select(Func.DISTINCT, DetailsSearch.entity().getTag());
        DetailsSearch.done();
        targetSearch.done();
        
        TotalSizeSearch = createSearchBuilder();
        TotalSizeSearch.and("target", TotalSizeSearch.entity().getTargetIqn(), SearchCriteria.Op.EQ);
        TotalSizeSearch.select(Func.SUM, TotalSizeSearch.entity().getSize());
        TotalSizeSearch.done();
        
        UsedSizeSearch = createSearchBuilder();
        UsedSizeSearch.and("target", UsedSizeSearch.entity().getTargetIqn(), SearchCriteria.Op.EQ);
        UsedSizeSearch.and("taken", UsedSizeSearch.entity().getTaken(), SearchCriteria.Op.NNULL);
        UsedSizeSearch.select(Func.SUM, UsedSizeSearch.entity().getSize());
        UsedSizeSearch.done();
        
        DeleteSearch = createSearchBuilder();
        DeleteSearch.and("id", DeleteSearch.entity().getId(), SearchCriteria.Op.EQ);
        DeleteSearch.and("taken", DeleteSearch.entity().getTaken(), SearchCriteria.Op.NULL);
        DeleteSearch.done();
    }    
    
    @Override
    public boolean delete(long id) {
    	SearchCriteria sc = DeleteSearch.create();
    	sc.setParameters("id", id);
    	
    	return delete(sc) > 0;
    }
    
    @Override
    public boolean release(String targetIqn, int lunId, long instanceId) {
        SearchCriteria sc = ReleaseSearch.create();
        sc.setParameters("lun", lunId);
        sc.setParameters("target", targetIqn);
        sc.setParameters("instanceId", instanceId);
        
        PreallocatedLunVO vo = createForUpdate();
        vo.setTaken(null);
        vo.setVolumeId(null);
        
        return update(vo, sc) > 0;
    }

    @Override @DB
    public PreallocatedLunVO take(long volumeId, String targetIqn, long size1, long size2, String... tags) {
        StringBuilder sql = new StringBuilder(TakeSqlPrefix);
        
        if (tags.length > 0) {
            sql.append("(");
            for (String tag : tags) {
                sql.append("ext_lun_details.tag=?").append(" OR ");
            }
            sql.delete(sql.length() - 4, sql.length());
            sql.append(") AND ");
        }
        sql.append(TakeSqlSuffix);
        
        try {
            Transaction txn = Transaction.currentTxn();
            txn.start();
            PreparedStatement pstmt = txn.prepareAutoCloseStatement(sql.toString());
            int i = 1;
            pstmt.setString(i++, targetIqn);
            pstmt.setLong(i++, size1);
            pstmt.setLong(i++, size2);
            for (String tag : tags) {
                pstmt.setString(i++, tag);
            }
            pstmt.setInt(i++, tags.length);
            ResultSet rs = pstmt.executeQuery();
            s_logger.debug("Statement is " + pstmt.toString());
            if (!rs.next()) {
                return null;
            }
            PreallocatedLunVO lun = toEntityBean(rs, false);
            lun.setTaken(new Date());
            lun.setVolumeId(volumeId);
            update(lun.getId(), lun);
            txn.commit();
            return lun;
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute " + sql.toString(), e);
        }
    }
    
    @Override @DB
    public List<PreallocatedLunVO> listDistinctTargets(long dataCenterId) {
        String DistinctTargetSearchSql = "SELECT * FROM ext_lun_alloc where data_center_id = ? GROUP BY target_iqn";
        
        Transaction txn = Transaction.currentTxn();
        try {
            PreparedStatement ps = txn.prepareAutoCloseStatement(DistinctTargetSearchSql);
            ps.setLong(1, dataCenterId);
            ResultSet rs = ps.executeQuery();
            List<PreallocatedLunVO> lst = new ArrayList<PreallocatedLunVO>();
            while (rs.next()) {
                lst.add(toEntityBean(rs, false));
            }
            
            return lst;
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute " + DistinctTargetSearchSql, e);
        }
    }
    
    @Override
    public long getTotalSize(String targetIqn) {
        SearchCriteria sc = TotalSizeSearch.create();
        sc.setParameters("target", targetIqn);
        
        List<Object[]> results = searchAll(sc, null);
        if (results.size() == 0 || results.get(0)[0] == null) {
            return 0;
        }
        
        return ((BigDecimal)(results.get(0)[0])).longValue();
    }
    
    @Override
    public long getUsedSize(String targetIqn) {
        SearchCriteria sc = UsedSizeSearch.create();
        sc.setParameters("target", targetIqn);
        
        List<Object[]> results = searchAll(sc, null);
        if (results.size() == 0 || results.get(0)[0] == null) {
            return 0;
        }
        
        return ((BigDecimal)(results.get(0)[0])).longValue();
    }
    
    @Override
    public List<String> findDistinctTagsForTarget(String targetIqn) {
        SearchCriteria sc = DetailsSearch.create();
        sc.setJoinParameters("target", "targetiqn", targetIqn);
        List<Object[]> results = _detailsDao.searchAll(sc);
        List<String> tags = new ArrayList<String>(results.size());
        for (Object[] result : results) {
            tags.add((String)result[0]);
        }
        
        return tags;
    }

    @Override @DB
    public PreallocatedLunVO persist(PreallocatedLunVO lun, String[] tags) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        lun = persist(lun);
        for (String tag : tags) {
            PreallocatedLunDetailVO detail = new PreallocatedLunDetailVO(lun.getId(), tag);
            _detailsDao.persist(detail);
        }
        txn.commit();
        return lun;
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);
        
        _detailsDao.configure(name, params);
        
        return true;
    }
    
}
