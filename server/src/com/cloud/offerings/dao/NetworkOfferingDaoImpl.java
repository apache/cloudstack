/**
 * 
 */
package com.cloud.offerings.dao;


import javax.ejb.Local;
import javax.persistence.EntityExistsException;

import org.apache.log4j.Logger;

import com.cloud.offerings.NetworkOfferingVO;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value=NetworkOfferingDao.class) @DB(txn=false)
public class NetworkOfferingDaoImpl extends GenericDaoBase<NetworkOfferingVO, Long> implements NetworkOfferingDao {
    
    private final static Logger s_logger = Logger.getLogger(NetworkOfferingDaoImpl.class);
    
    SearchBuilder<NetworkOfferingVO> NameSearch;
    SearchBuilder<NetworkOfferingVO> ServiceOfferingSearch;
    
    protected NetworkOfferingDaoImpl() {
        super();
        
        NameSearch = createSearchBuilder();
        NameSearch.and("name", NameSearch.entity().getName(), SearchCriteria.Op.EQ);
        NameSearch.done();
        
        ServiceOfferingSearch = createSearchBuilder();
        ServiceOfferingSearch.and("serviceoffering", ServiceOfferingSearch.entity().getServiceOfferingId(), SearchCriteria.Op.EQ);
        ServiceOfferingSearch.done();
    }
    
    @Override
    public NetworkOfferingVO findByName(String name) {
        SearchCriteria<NetworkOfferingVO> sc = NameSearch.create();
        
        sc.setParameters("name", name);
        
        return findOneBy(sc);
        
    }
    
    @Override
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
    
    @Override
    public NetworkOfferingVO findByServiceOffering(ServiceOfferingVO offering) {
        SearchCriteria<NetworkOfferingVO> sc = ServiceOfferingSearch.create();
        sc.setParameters("serviceoffering", offering.getId());

        NetworkOfferingVO vo = findOneBy(sc);
        if (vo != null) {
            return vo;
        }
        
        vo = new NetworkOfferingVO(offering);
        try {
            return persist(vo);
        } catch (Exception e) {
            s_logger.debug("Got a persistence exception.  Assuming it's because service offering id is duplicate");
            vo = findOneBy(sc);
            if (vo != null) {
                return vo;
            }
            
            throw new CloudRuntimeException("Unable to persist network offering", e);
        }
    }
}
