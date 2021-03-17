package com.cloud.network.dao;

import com.cloud.network.TungstenGuestNetworkIpAddressVO;
import com.cloud.utils.db.GenericDao;

import java.util.List;

public interface TungstenGuestNetworkIpAddressDao extends GenericDao<TungstenGuestNetworkIpAddressVO, Long> {
    List<String> listByNetworkId(long networkId);

    TungstenGuestNetworkIpAddressVO findByNetworkIdAndPublicIp(long networkId, String publicIp);
}
