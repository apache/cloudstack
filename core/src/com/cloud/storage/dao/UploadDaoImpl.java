package com.cloud.storage.dao;

import java.sql.PreparedStatement;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.storage.UploadVO;
import com.cloud.utils.DateUtil;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;

@Local(value={UploadDao.class})
public class UploadDaoImpl extends GenericDaoBase<UploadVO, Long> implements UploadDao {
	public static final Logger s_logger = Logger.getLogger(UploadDaoImpl.class.getName());
	protected final SearchBuilder<UploadVO> typeUploadStatusSearch;
	
	protected static final String UPDATE_UPLOAD_INFO =
		"UPDATE upload SET upload_state = ?, upload_pct= ?, last_updated = ? "
	+   ", upload_error_str = ?, upload_job_id = ? "
	+   "WHERE host_id = ? and type_id = ? and type = ?";
	
	protected static final String UPLOADS_STATE_DC=
		"SELECT * FROM upload t, host h where t.host_id = h.id and h.data_center_id=? "
	+	" and t.type_id=? and t.upload_state = ?" ;
	
	
	public UploadDaoImpl() {
		typeUploadStatusSearch = createSearchBuilder();
		typeUploadStatusSearch.and("type_id", typeUploadStatusSearch.entity().getTypeId(), SearchCriteria.Op.EQ);
		typeUploadStatusSearch.and("upload_state", typeUploadStatusSearch.entity().getUploadState(), SearchCriteria.Op.EQ);
		typeUploadStatusSearch.and("type", typeUploadStatusSearch.entity().getType(), SearchCriteria.Op.EQ);
		typeUploadStatusSearch.done();
	}
	
	@Override
	public List<UploadVO> listByTypeUploadStatus(long typeId, UploadVO.Type type, UploadVO.Status uploadState) {
		SearchCriteria<UploadVO> sc = typeUploadStatusSearch.create();
		sc.setParameters("type_id", typeId);
		sc.setParameters("type", type);
		sc.setParameters("upload_state", uploadState.toString());
		return listBy(sc);
	}
	/*
	public void updateUploadStatus(long hostId, long typeId, int uploadPercent, UploadVO.Status uploadState,
			String uploadJobId, String uploadUrl ) {
        Transaction txn = Transaction.currentTxn();
		PreparedStatement pstmt = null;
		try {
			Date now = new Date();
			String sql = UPDATE_UPLOAD_INFO;
			pstmt = txn.prepareAutoCloseStatement(sql);
			pstmt.setString(1, uploadState.toString());
			pstmt.setInt(2, uploadPercent);
			pstmt.setString(3, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), now));
			pstmt.setString(4, uploadJobId);
			pstmt.setLong(5, hostId);
			pstmt.setLong(6, typeId);
			
			pstmt.setString(7, uploadUrl);
			pstmt.executeUpdate();
		} catch (Exception e) {
			s_logger.warn("Exception: ", e);
		}
	}*/
}