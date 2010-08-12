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

package com.cloud.storage.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value={VMTemplateHostDao.class})
public class VMTemplateHostDaoImpl extends GenericDaoBase<VMTemplateHostVO, Long> implements VMTemplateHostDao {
	public static final Logger s_logger = Logger.getLogger(VMTemplateHostDaoImpl.class.getName());
	
	protected final SearchBuilder<VMTemplateHostVO> HostSearch;
	protected final SearchBuilder<VMTemplateHostVO> TemplateSearch;
	protected final SearchBuilder<VMTemplateHostVO> HostTemplateSearch;
	protected final SearchBuilder<VMTemplateHostVO> HostDestroyedSearch;
	protected final SearchBuilder<VMTemplateHostVO> PoolTemplateSearch;
	protected final SearchBuilder<VMTemplateHostVO> HostTemplatePoolSearch;
	protected final SearchBuilder<VMTemplateHostVO> TemplateStatusSearch;
	protected final SearchBuilder<VMTemplateHostVO> TemplateStatesSearch;
	
	protected static final String UPDATE_TEMPLATE_HOST_REF =
		"UPDATE template_host_ref SET download_state = ?, download_pct= ?, last_updated = ? "
	+   ", error_str = ?, local_path = ?, job_id = ? "
	+   "WHERE host_id = ? and template_id = ?";
	
	protected static final String DOWNLOADS_STATE_DC=
		"SELECT * FROM template_host_ref t, host h where t.host_id = h.id and h.data_center_id=? "
	+	" and t.template_id=? and t.download_state = ?" ;
	
	protected static final String DOWNLOADS_STATE_DC_POD=
		"SELECT * FROM template_host_ref t, host h where t.host_id = h.id and h.data_center_id=? and h.pod_id=? "
	+	" and t.template_id=? and t.download_state=?" ;
	
	protected static final String DOWNLOADS_STATE=
		"SELECT * FROM template_host_ref t "
	+	" where t.template_id=? and t.download_state=?";
	
	public VMTemplateHostDaoImpl () {
		HostSearch = createSearchBuilder();
		HostSearch.and("host_id", HostSearch.entity().getHostId(), SearchCriteria.Op.EQ);
		HostSearch.done();
		
		TemplateSearch = createSearchBuilder();
		TemplateSearch.and("template_id", TemplateSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
		TemplateSearch.and("destroyed", TemplateSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
		TemplateSearch.done();
		
		HostTemplateSearch = createSearchBuilder();
		HostTemplateSearch.and("host_id", HostTemplateSearch.entity().getHostId(), SearchCriteria.Op.EQ);
		HostTemplateSearch.and("template_id", HostTemplateSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
		HostTemplateSearch.done();
		
		HostDestroyedSearch = createSearchBuilder();
		HostDestroyedSearch.and("host_id", HostDestroyedSearch.entity().getHostId(), SearchCriteria.Op.EQ);
		HostDestroyedSearch.and("destroyed", HostDestroyedSearch.entity().getDestroyed(), SearchCriteria.Op.EQ);
		HostDestroyedSearch.done();
		
		HostTemplatePoolSearch = createSearchBuilder();
		HostTemplatePoolSearch.and("host_id", HostTemplatePoolSearch.entity().getHostId(), SearchCriteria.Op.EQ);
		HostTemplatePoolSearch.and("template_id", HostTemplatePoolSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
		HostTemplatePoolSearch.and("pool_id", HostTemplatePoolSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
		HostTemplatePoolSearch.done();
		
		TemplateStatusSearch = createSearchBuilder();
		TemplateStatusSearch.and("template_id", TemplateStatusSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
		TemplateStatusSearch.and("download_state", TemplateStatusSearch.entity().getDownloadState(), SearchCriteria.Op.EQ);
		TemplateStatusSearch.done();
		
		TemplateStatesSearch = createSearchBuilder();
		TemplateStatesSearch.and("template_id", TemplateStatesSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
		TemplateStatesSearch.and("states", TemplateStatesSearch.entity().getDownloadState(), SearchCriteria.Op.IN);
		TemplateStatesSearch.done();
		
		PoolTemplateSearch = createSearchBuilder();
		PoolTemplateSearch.and("pool_id", PoolTemplateSearch.entity().getPoolId(), SearchCriteria.Op.EQ);
		PoolTemplateSearch.and("template_id", PoolTemplateSearch.entity().getTemplateId(), SearchCriteria.Op.EQ);
		PoolTemplateSearch.done();
	}
	
	public void update(VMTemplateHostVO instance) {
        Transaction txn = Transaction.currentTxn();
		PreparedStatement pstmt = null;
		try {
			Date now = new Date();
			String sql = UPDATE_TEMPLATE_HOST_REF;
			pstmt = txn.prepareAutoCloseStatement(sql);
			pstmt.setString(1, instance.getDownloadState().toString());
			pstmt.setInt(2, instance.getDownloadPercent());
			pstmt.setString(3, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), now));
			pstmt.setString(4, instance.getErrorString());
			pstmt.setString(5, instance.getLocalDownloadPath());
			pstmt.setString(6, instance.getJobId());
			pstmt.setLong(7, instance.getHostId());
			pstmt.setLong(8, instance.getTemplateId());
			pstmt.executeUpdate();
		} catch (Exception e) {
			s_logger.warn("Exception: ", e);
		}
	}

	@Override
	public List<VMTemplateHostVO> listByHostId(long id) {
	    SearchCriteria sc = HostSearch.create();
	    sc.setParameters("host_id", id);
	    return listBy(sc);
	}

	@Override
	public List<VMTemplateHostVO> listByTemplateId(long templateId) {
	    SearchCriteria sc = TemplateSearch.create();
	    sc.setParameters("template_id", templateId);
	    sc.setParameters("destroyed", false);
	    return listBy(sc);
	}

	@Override
	public VMTemplateHostVO findByHostTemplate(long hostId, long templateId) {
		SearchCriteria sc = HostTemplateSearch.create();
	    sc.setParameters("host_id", hostId);
	    sc.setParameters("template_id", templateId);
	    return findOneBy(sc);
	}
	
	@Override
	public List<VMTemplateHostVO> listByTemplateStatus(long templateId, VMTemplateHostVO.Status downloadState) {
		SearchCriteria sc = TemplateStatusSearch.create();
		sc.setParameters("template_id", templateId);
		sc.setParameters("download_state", downloadState.toString());
		return listBy(sc);
	}
	
	@Override
	public List<VMTemplateHostVO> listByTemplateStatus(long templateId, long datacenterId, VMTemplateHostVO.Status downloadState) {
        Transaction txn = Transaction.currentTxn();
		PreparedStatement pstmt = null;
		List<VMTemplateHostVO> result = new ArrayList<VMTemplateHostVO>();
		try {
			String sql = DOWNLOADS_STATE_DC;
			pstmt = txn.prepareAutoCloseStatement(sql);
			pstmt.setLong(1, datacenterId);
			pstmt.setLong(2, templateId);
			pstmt.setString(3, downloadState.toString());
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
                result.add(toEntityBean(rs, false));
            }
		} catch (Exception e) {
			s_logger.warn("Exception: ", e);
		}
		return result;

	}
	
	@Override
	public List<VMTemplateHostVO> listByTemplateStatus(long templateId, long datacenterId, long podId, VMTemplateHostVO.Status downloadState) {
        Transaction txn = Transaction.currentTxn();
		PreparedStatement pstmt = null;
		List<VMTemplateHostVO> result = new ArrayList<VMTemplateHostVO>();
		ResultSet rs = null;
		try {
			String sql = DOWNLOADS_STATE_DC_POD;
			pstmt = txn.prepareStatement(sql);
			
			pstmt.setLong(1, datacenterId);
			pstmt.setLong(2, podId);
			pstmt.setLong(3, templateId);
			pstmt.setString(4, downloadState.toString());
			rs = pstmt.executeQuery();
			while (rs.next()) {
                // result.add(toEntityBean(rs, false)); TODO: this is buggy in GenericDaoBase for hand constructed queries
				long id = rs.getLong(1); //ID column
				result.add(findById(id));
            }
		} catch (Exception e) {
			s_logger.warn("Exception: ", e);
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (pstmt != null) {
					pstmt.close();
				}
			} catch (SQLException e) {
			}
		}
		return result;

	}

	@Override
	public boolean templateAvailable(long templateId, long hostId) {
		VMTemplateHostVO tmpltHost = findByHostTemplate(hostId, templateId);
		if (tmpltHost == null)
		  return false;
		
		return tmpltHost.getDownloadState()==Status.DOWNLOADED;
	}

	@Override
	public List<VMTemplateHostVO> listByTemplateStates(long templateId, VMTemplateHostVO.Status... states) {
    	SearchCriteria sc = TemplateStatesSearch.create();
    	sc.setParameters("states", (Object[])states);
		sc.setParameters("template_id", templateId);

	  	return search(sc, null);
	}

	@Override
	public VMTemplateHostVO findByHostTemplatePool(long hostId, long templateId, long poolId) {
		SearchCriteria sc = HostTemplatePoolSearch.create();
	    sc.setParameters("host_id", hostId);
	    sc.setParameters("template_id", templateId);
	    sc.setParameters("pool_id", poolId);
	    return findOneBy(sc);
	}

	@Override
	public List<VMTemplateHostVO> listByHostTemplate(long hostId, long templateId) {
		SearchCriteria sc = HostTemplateSearch.create();
	    sc.setParameters("host_id", hostId);
	    sc.setParameters("template_id", templateId);
	    return listBy(sc);
	}

	@Override
	public List<VMTemplateHostVO> listByTemplatePool(long templateId, long poolId) {
		SearchCriteria sc = PoolTemplateSearch.create();
	    sc.setParameters("pool_id", poolId);
	    sc.setParameters("template_id", templateId);
	    return listBy(sc);
	}
	
	@Override
	public List<VMTemplateHostVO> listDestroyed(long hostId) {
		SearchCriteria sc = HostDestroyedSearch.create();
		sc.setParameters("host_id", hostId);
		sc.setParameters("destroyed", true);
		return listBy(sc);
	}

	@Override
	public VMTemplateHostVO findByHostTemplate(long hostId, long templateId, boolean lock) {
		SearchCriteria sc = HostTemplateSearch.create();
	    sc.setParameters("host_id", hostId);
	    sc.setParameters("template_id", templateId);
	    if (!lock)
	    	return findOneBy(sc);
	    else
	    	return lock(sc, true);
	}

}
