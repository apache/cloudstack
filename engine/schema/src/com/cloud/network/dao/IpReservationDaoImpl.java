package com.cloud.network.dao;

import com.cloud.network.IpReservationVO;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.GenericSearchBuilder;
import com.cloud.utils.db.JoinBuilder;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class IpReservationDaoImpl extends GenericDaoBase<IpReservationVO, Long> implements IpReservationDao {

    @Inject
    NetworkDao networkDao;

    private SearchBuilder<IpReservationVO> NetworkSearch;
    private GenericSearchBuilder<IpReservationVO, FullIpReservation> AllSearch;

    public IpReservationDaoImpl() {
    }

    @PostConstruct
    protected void init() {
        NetworkSearch = createSearchBuilder();
        NetworkSearch.and("network", NetworkSearch.entity().getNetworkId(), Op.EQ);
        NetworkSearch.done();

        AllSearch = createSearchBuilder(FullIpReservation.class);
        AllSearch.select("id", SearchCriteria.Func.NATIVE, AllSearch.entity().getUuid());
        AllSearch.select("startip", SearchCriteria.Func.NATIVE, AllSearch.entity().getStartIp());
        AllSearch.select("endip", SearchCriteria.Func.NATIVE, AllSearch.entity().getEndIp());
        SearchBuilder<NetworkVO> networkSearch = networkDao.createSearchBuilder();
        AllSearch.join("network", networkSearch, networkSearch.entity().getId(), AllSearch.entity().getNetworkId(), JoinBuilder.JoinType.INNER);
        AllSearch.select("networkid", SearchCriteria.Func.NATIVE, networkSearch.entity().getUuid());
        AllSearch.done();
    }

    @Override
    public List<IpReservationVO> getIpReservationsForNetwork(long networkId) {
        SearchCriteria<IpReservationVO> sc = NetworkSearch.create();
        sc.setParameters("network", networkId);
        return listBy(sc);
    }

    @Override
    public List<FullIpReservation> getAllIpReservations() {
        try (TransactionLegacy txn = TransactionLegacy.currentTxn()) {
            PreparedStatement pstmt = txn.prepareAutoCloseStatement("SELECT r.uuid, r.start_ip, r.end_ip, n.uuid from ip_reservation r INNER JOIN networks n on n.id = r.network_id where r.removed is null");
            ResultSet rs = pstmt.executeQuery();
            List<FullIpReservation> ret = new ArrayList<>();
            while (rs.next()) {
                ret.add(new FullIpReservation(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
            }
            return ret;
        } catch (SQLException e) {
            throw new CloudRuntimeException(e);
        }
    }

}
