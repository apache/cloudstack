package com.cloud.dc.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.dc.DataCenterIpAddressVO;
import com.cloud.dc.StorageNetworkIpAddressVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria2;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;

@Local(value={StorageNetworkIpAddressDao.class})
@DB(txn=false)
public class StorageNetworkIpAddressDaoImpl extends GenericDaoBase<StorageNetworkIpAddressVO, Long> implements StorageNetworkIpAddressDao {
	protected final GenericSearchBuilder<StorageNetworkIpAddressVO, Long> countInUserIp;
	protected final GenericSearchBuilder<StorageNetworkIpAddressVO, String> listInUseIp;
	protected final SearchBuilder<StorageNetworkIpAddressVO> untakenIp;
	protected final SearchBuilder<StorageNetworkIpAddressVO> ipSearch;
	

	protected StorageNetworkIpAddressDaoImpl() {
		countInUserIp = createSearchBuilder(Long.class);
		countInUserIp.select(null, Func.COUNT, null);
		countInUserIp.and("rangeId", countInUserIp.entity().getRangeId(), Op.EQ);
		countInUserIp.and("taken", countInUserIp.entity().getTakenAt(), Op.NNULL);
		countInUserIp.done();
		
		listInUseIp = createSearchBuilder(String.class);
		listInUseIp.selectField(listInUseIp.entity().getIpAddress());
		listInUseIp.and("rangeId", listInUseIp.entity().getRangeId(), Op.EQ);
		listInUseIp.and("taken", listInUseIp.entity().getTakenAt(), Op.NNULL);
		listInUseIp.done();
		
		untakenIp = createSearchBuilder();
		untakenIp.and("rangeId", untakenIp.entity().getRangeId(), Op.EQ);
		untakenIp.and("taken", untakenIp.entity().getTakenAt(), Op.NULL);
		untakenIp.done();
		
		ipSearch = createSearchBuilder();
		ipSearch.and("ipAddress", ipSearch.entity().getIpAddress(), Op.EQ);
		ipSearch.done();
	}
	
	@Override
	public long countInUseIpByRangeId(long rangeId) {
		SearchCriteria<Long> sc = countInUserIp.create();
		sc.setParameters("rangeId", rangeId);
		return customSearch(sc, null).get(0);
	}

	@Override
	public List<String> listInUseIpByRangeId(long rangeId) {
		SearchCriteria<String> sc = listInUseIp.create();
		sc.setParameters("rangeId", rangeId);
		return customSearch(sc, null);
	}
	
	@Override
	@DB
    public StorageNetworkIpAddressVO takeIpAddress(long rangeId) {
		SearchCriteria<StorageNetworkIpAddressVO> sc = untakenIp.create();
		sc.setParameters("rangeId", rangeId);
        Transaction txn = Transaction.currentTxn();
        txn.start();
        StorageNetworkIpAddressVO ip = lockOneRandomRow(sc, true);
        if (ip == null) {
        	txn.rollback();
        	return null;
        }
        ip.setTakenAt(new Date());
        update(ip.getId(), ip);
        txn.commit();
        return ip;
	}

	@Override
    public void releaseIpAddress(String ip) {
		SearchCriteria<StorageNetworkIpAddressVO> sc = ipSearch.create();
	    sc.setParameters("ipAddress", ip);
	    StorageNetworkIpAddressVO vo = createForUpdate();
	    vo.setTakenAt(null);
	    update(vo, sc);
    }
}
