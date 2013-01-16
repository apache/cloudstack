package org.apache.cloudstack.engine.cloud.entity.api.db.dao;

import java.util.List;

import org.apache.cloudstack.engine.cloud.entity.api.db.VMNetworkMapVO;

import com.cloud.utils.db.GenericDao;

public interface VMNetworkMapDao extends GenericDao<VMNetworkMapVO, Long>{

    void persist(long vmId, List<Long> networks);
    
    List<Long> getNetworks(long vmId);

}
