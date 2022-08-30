package org.apache.cloudstack.reservation.dao;

import com.cloud.configuration.Resource;
import org.apache.cloudstack.reservation.ReservationVO;
import com.cloud.utils.db.GenericDao;

public interface ReservationDao extends GenericDao<ReservationVO, Long> {
    long getAccountReservation(Long account, Resource.ResourceType resourceType);
    long getDomainReservation(Long domain, Resource.ResourceType resourceType);
}
