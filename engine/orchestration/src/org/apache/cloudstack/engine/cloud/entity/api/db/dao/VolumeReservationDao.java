package org.apache.cloudstack.engine.cloud.entity.api.db.dao;

import java.util.List;

import org.apache.cloudstack.engine.cloud.entity.api.db.VolumeReservationVO;

import com.cloud.utils.db.GenericDao;

public interface VolumeReservationDao extends GenericDao<VolumeReservationVO, Long>{

   VolumeReservationVO findByVmId(long vmId);
   
   List<VolumeReservationVO> listVolumeReservation(long vmReservationId);

}
