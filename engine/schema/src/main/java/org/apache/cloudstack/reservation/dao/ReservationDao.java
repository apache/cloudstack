package org.apache.cloudstack.reservation.dao;

import com.cloud.configuration.Resource;
import org.apache.cloudstack.reservation.ReservationVO;
import com.cloud.utils.db.GenericDao;

public interface ReservationDao extends GenericDao<ReservationVO, Long> {
    long getReservation(Long account, Resource.ResourceType type);
}
