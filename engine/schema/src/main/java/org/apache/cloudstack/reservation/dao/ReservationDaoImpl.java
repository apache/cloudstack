package org.apache.cloudstack.reservation.dao;

import com.cloud.configuration.Resource;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.reservation.ReservationVO;

import java.util.List;

public class ReservationDaoImpl extends GenericDaoBase<ReservationVO, Long> implements ReservationDao {

    private final SearchBuilder<ReservationVO> listByRegionIDAccountAndIdSearch;
    public ReservationDaoImpl() {
        listByRegionIDAccountAndIdSearch = createSearchBuilder();
        listByRegionIDAccountAndIdSearch.and("accountId", listByRegionIDAccountAndIdSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        listByRegionIDAccountAndIdSearch.and("type", listByRegionIDAccountAndIdSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        listByRegionIDAccountAndIdSearch.done();

    }

    @Override
    public long getReservation(Long accountId, Resource.ResourceType type) {
        long total = 0;
        SearchCriteria<ReservationVO> sc = listByRegionIDAccountAndIdSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("type", type);
        List<ReservationVO> reservations = listBy(sc);
        for (ReservationVO reservation : reservations) {
            total += reservation.getReservedAmount();
        }
        return total;
    }
}
