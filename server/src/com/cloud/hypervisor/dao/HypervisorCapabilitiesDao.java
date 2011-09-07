package com.cloud.hypervisor.dao;

import java.util.List;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.hypervisor.HypervisorCapabilitiesVO;
import com.cloud.utils.db.GenericDao;

public interface HypervisorCapabilitiesDao extends GenericDao<HypervisorCapabilitiesVO, Long> {
    
    List<HypervisorCapabilitiesVO> listAllByHypervisorType(HypervisorType hypervisorType);
    
    HypervisorCapabilitiesVO findByHypervisorTypeAndVersion(HypervisorType hypervisorType, String hypervisorVersion);
    
    Long getMaxGuestsLimit(HypervisorType hypervisorType, String hypervisorVersion); 
    
}
