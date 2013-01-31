package com.cloud.network.dao;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.network.Network;
import com.cloud.network.UserIpv6AddressVO;
import com.cloud.utils.Pair;
import com.cloud.utils.db.Filter;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Func;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.SearchCriteria2;

@Local(value=UserIpv6AddressDao.class)
public class UserIpv6AddressDaoImpl extends GenericDaoBase<UserIpv6AddressVO, Long> implements UserIpv6AddressDao {
    private static final Logger s_logger = Logger.getLogger(IPAddressDaoImpl.class);

    protected final SearchBuilder<UserIpv6AddressVO> AllFieldsSearch;
    protected GenericSearchBuilder<UserIpv6AddressVO, Long> CountFreePublicIps;
    
    public UserIpv6AddressDaoImpl() {
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("id", AllFieldsSearch.entity().getId(), Op.EQ);
        AllFieldsSearch.and("dataCenterId", AllFieldsSearch.entity().getDataCenterId(), Op.EQ);
        AllFieldsSearch.and("ipAddress", AllFieldsSearch.entity().getAddress(), Op.EQ);
        AllFieldsSearch.and("vlan", AllFieldsSearch.entity().getVlanId(), Op.EQ);
        AllFieldsSearch.and("accountId", AllFieldsSearch.entity().getAccountId(), Op.EQ);
        AllFieldsSearch.and("network", AllFieldsSearch.entity().getNetworkId(), Op.EQ);
        AllFieldsSearch.and("physicalNetworkId", AllFieldsSearch.entity().getPhysicalNetworkId(), Op.EQ);
        AllFieldsSearch.done();
        
        CountFreePublicIps = createSearchBuilder(Long.class);
        CountFreePublicIps.select(null, Func.COUNT, null);
        CountFreePublicIps.and("networkId", CountFreePublicIps.entity().getSourceNetworkId(), SearchCriteria.Op.EQ);
        CountFreePublicIps.done();
    }

	@Override
	public List<UserIpv6AddressVO> listByAccount(long accountId) {
        SearchCriteria<UserIpv6AddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("accountId", accountId);
        return listBy(sc);
	}

	@Override
	public List<UserIpv6AddressVO> listByVlanId(long vlanId) {
        SearchCriteria<UserIpv6AddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("vlan", vlanId);
        return listBy(sc);
	}

	@Override
	public List<UserIpv6AddressVO> listByDcId(long dcId) {
        SearchCriteria<UserIpv6AddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("dataCenterId", dcId);
        return listBy(sc);
	}

	@Override
	public List<UserIpv6AddressVO> listByNetwork(long networkId) {
        SearchCriteria<UserIpv6AddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("network", networkId);
        return listBy(sc);
	}

	@Override
	public UserIpv6AddressVO findByNetworkIdAndIp(long networkId, String ipAddress) {
        SearchCriteria<UserIpv6AddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("networkId", networkId);
        sc.setParameters("ipAddress", ipAddress);
        return findOneBy(sc);
	}

	@Override
	public List<UserIpv6AddressVO> listByPhysicalNetworkId(long physicalNetworkId) {
        SearchCriteria<UserIpv6AddressVO> sc = AllFieldsSearch.create();
        sc.setParameters("physicalNetworkId", physicalNetworkId);
        return listBy(sc);
	}

	@Override
	public long countExistedIpsInNetwork(long networkId) {
        SearchCriteria<Long> sc = CountFreePublicIps.create();
        sc.setParameters("networkId", networkId);
        return customSearch(sc, null).get(0);       
	}
}
