package org.apache.cloudstack.engine.cloud.entity.api.db.dao;

import java.util.List;

import org.apache.cloudstack.engine.cloud.entity.api.db.VMRootDiskTagVO;

import com.cloud.utils.db.GenericDao;

public interface VMRootDiskTagDao extends GenericDao<VMRootDiskTagVO, Long>{

    void persist(long vmId, List<String> diskTags);
    
    List<String> getRootDiskTags(long vmId);

}
