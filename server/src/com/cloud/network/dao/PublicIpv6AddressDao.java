package com.cloud.network.dao;

import java.util.List;

import com.cloud.network.Network;
import com.cloud.network.PublicIpv6AddressVO;
import com.cloud.utils.db.GenericDao;

public interface PublicIpv6AddressDao extends GenericDao<PublicIpv6AddressVO, Long> {
	List<PublicIpv6AddressVO> listByAccount(long accountId);
	
	List<PublicIpv6AddressVO> listByVlanId(long vlanId);
	
	List<PublicIpv6AddressVO> listByDcId(long dcId); 
	
	List<PublicIpv6AddressVO> listByNetwork(long networkId);
	
	public PublicIpv6AddressVO findByNetworkIdAndIp(long networkId, String ipAddress);

	List<PublicIpv6AddressVO> listByPhysicalNetworkId(long physicalNetworkId);

	long countExistedIpsInNetwork(long networkId);
}
