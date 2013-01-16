package org.apache.cloudstack.engine.cloud.entity.api.db.dao;


import java.util.Map;

import org.apache.cloudstack.engine.cloud.entity.api.db.VMReservationVO;

import com.cloud.utils.db.GenericDao;

public interface VMReservationDao extends GenericDao<VMReservationVO, Long>{
    
    VMReservationVO findByVmId(long vmId);

    void loadVolumeReservation(VMReservationVO reservation);
    
    VMReservationVO findByReservationId(String reservationId);

}
