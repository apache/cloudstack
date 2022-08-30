package org.apache.cloudstack.reservation.dao;

import com.cloud.configuration.Resource;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import org.apache.cloudstack.reservation.ReservationVO;

import java.util.List;

public class ReservationDaoImpl extends GenericDaoBase<ReservationVO, Long> implements ReservationDao {

    private final SearchBuilder<ReservationVO> listAccountAndTypeSearch;

    private final SearchBuilder<ReservationVO> listDomainAndTypeSearch;

    public ReservationDaoImpl() {
        listAccountAndTypeSearch = createSearchBuilder();
        listAccountAndTypeSearch.and("accountId", listAccountAndTypeSearch.entity().getAccountId(), SearchCriteria.Op.EQ);
        listAccountAndTypeSearch.and("resourceType", listAccountAndTypeSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        listAccountAndTypeSearch.done();

        listDomainAndTypeSearch = createSearchBuilder();
        listDomainAndTypeSearch.and("domainId", listDomainAndTypeSearch.entity().getDomainId(), SearchCriteria.Op.EQ);
        listDomainAndTypeSearch.and("resourceType", listDomainAndTypeSearch.entity().getResourceType(), SearchCriteria.Op.EQ);
        listDomainAndTypeSearch.done();
    }

    @Override
    public long getAccountReservation(Long accountId, Resource.ResourceType resourceType) {
        long total = 0;
        SearchCriteria<ReservationVO> sc = listAccountAndTypeSearch.create();
        sc.setParameters("accountId", accountId);
        sc.setParameters("resourceType", resourceType);
        List<ReservationVO> reservations = listBy(sc);
        for (ReservationVO reservation : reservations) {
            total += reservation.getReservedAmount();
        }
        return total;
    }

    @Override
    public long getDomainReservation(Long domainId, Resource.ResourceType resourceType) {
        long total = 0;
        SearchCriteria<ReservationVO> sc = listAccountAndTypeSearch.create();
        sc.setParameters("domainId", domainId);
        sc.setParameters("resourceType", resourceType);
        List<ReservationVO> reservations = listBy(sc);
        for (ReservationVO reservation : reservations) {
            total += reservation.getReservedAmount();
        }
        return total;
    }
}
