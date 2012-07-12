package com.cloud.network.dao;

import com.cloud.network.NiciraNvpNicMappingVO;
import com.cloud.utils.db.GenericDao;

public interface NiciraNvpNicMappingDao extends GenericDao<NiciraNvpNicMappingVO, Long> {

    /** find the mapping for a nic 
     * @param nicUuid the Uuid of a nic attached to a logical switch port
     * @return NiciraNvpNicMapping for this nic uuid or null if it does not exist
     */
    public NiciraNvpNicMappingVO findByNicUuid(String nicUuid);
}
