package org.apache.cloudstack.engine.cloud.entity.api.db.dao;


import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.engine.cloud.entity.api.db.VMReservationVO;
import org.apache.cloudstack.engine.cloud.entity.api.db.VolumeReservationVO;
import org.springframework.stereotype.Component;

import com.cloud.host.dao.HostTagsDaoImpl;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Component
@Local(value = { VolumeReservationDao.class })
public class VolumeReservationDaoImpl extends GenericDaoBase<VolumeReservationVO, Long> implements VolumeReservationDao {

    protected SearchBuilder<VolumeReservationVO> VmIdSearch;
    protected SearchBuilder<VolumeReservationVO> VmReservationIdSearch;
    
    public VolumeReservationDaoImpl() {
    }
    
    @PostConstruct
    public void init() {
        VmIdSearch = createSearchBuilder();
        VmIdSearch.and("vmId", VmIdSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        VmIdSearch.done();
        
        VmReservationIdSearch = createSearchBuilder();
        VmReservationIdSearch.and("vmReservationId", VmReservationIdSearch.entity().geVmReservationId(), SearchCriteria.Op.EQ);
        VmReservationIdSearch.done();
    }
    
    @Override
    public VolumeReservationVO findByVmId(long vmId) {
        SearchCriteria<VolumeReservationVO> sc = VmIdSearch.create("vmId", vmId);
        return findOneBy(sc);
    }

    @Override
    public List<VolumeReservationVO> listVolumeReservation(long vmReservationId) {
        SearchCriteria<VolumeReservationVO> sc = VmReservationIdSearch.create("vmReservationId", vmReservationId);
        return listBy(sc);
    }
    
}
