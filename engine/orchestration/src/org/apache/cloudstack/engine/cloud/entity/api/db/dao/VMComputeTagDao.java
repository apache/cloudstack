package org.apache.cloudstack.engine.cloud.entity.api.db.dao;

import java.util.List;

import org.apache.cloudstack.engine.cloud.entity.api.db.VMComputeTagVO;

import com.cloud.utils.db.GenericDao;

public interface VMComputeTagDao extends GenericDao<VMComputeTagVO, Long>{

    void persist(long vmId, List<String> computeTags);
    
    List<String> getComputeTags(long vmId);

}
