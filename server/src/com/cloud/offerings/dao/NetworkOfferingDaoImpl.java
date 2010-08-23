/**
 * 
 */
package com.cloud.offerings.dao;

import javax.ejb.Local;
import javax.persistence.EntityExistsException;

import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value=NetworkOfferingDao.class)
public class NetworkOfferingDaoImpl extends GenericDaoBase<NetworkOfferingVO, Long> {
    SearchBuilder<NetworkOfferingVO> NameSearch; 
    
    protected NetworkOfferingDaoImpl() {
        super();
        
        NameSearch = createSearchBuilder();
        NameSearch.and("name", NameSearch.entity().getName(), SearchCriteria.Op.EQ);
        NameSearch.done();
    }
    
    public NetworkOfferingVO findByName(String name) {
        SearchCriteria<NetworkOfferingVO> sc = NameSearch.create();
        
        sc.setParameters("name", name);
        
        return findOneActiveBy(sc);
        
    }
    
    public NetworkOfferingVO persistSystemNetworkOffering(NetworkOfferingVO offering) {
        assert offering.getName() != null : "how are you going to find this later if you don't set it?";
        NetworkOfferingVO vo = findByName(offering.getName());
        if (vo != null) {
            return vo;
        }
        try {
            vo = persist(offering);
            return vo;
        } catch (EntityExistsException e) {
            // Assume it's conflict on unique name from two different management servers.
            return findByName(offering.getName());
        }
    }
}
