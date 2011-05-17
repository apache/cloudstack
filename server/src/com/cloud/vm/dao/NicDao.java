/**
 * 
 */
package com.cloud.vm.dao;

import java.util.List;

import com.cloud.utils.db.GenericDao;
import com.cloud.vm.NicVO;
import com.cloud.vm.VirtualMachine;

public interface NicDao extends GenericDao<NicVO, Long> {
    List<NicVO> listByVmId(long instanceId);
    
    List<String> listIpAddressInNetwork(long networkConfigId);
    List<NicVO> listByVmIdIncludingRemoved(long instanceId);
    
    List<NicVO> listByNetworkId(long networkId);
    
    NicVO findByInstanceIdAndNetworkId(long networkId, long instanceId);
    
    NicVO findByInstanceIdAndNetworkIdIncludingRemoved(long networkId, long instanceId);

    void removeNicsForInstance(long instanceId);
    
    NicVO findByNetworkIdAndType(long networkId, VirtualMachine.Type vmType);
}
