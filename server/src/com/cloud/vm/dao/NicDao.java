/**
 * 
 */
package com.cloud.vm.dao;

import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.cloud.vm.NicVO;

public interface NicDao extends GenericDao<NicVO, Long> {
    List<NicVO> listBy(long instanceId);
    
    List<String> listIpAddressInNetwork(long networkConfigId);
    List<NicVO> listIncludingRemovedBy(long instanceId);
    
    List<NicVO> listByNetworkId(long networkId);
    
    NicVO findByInstanceIdAndNetworkId(long networkId, long instanceId);
    
    NicVO findByInstanceIdAndNetworkIdIncludingRemoved(long networkId, long instanceId);

    void removeNicsForInstance(long instanceId);
}
