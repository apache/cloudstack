package com.cloud.network.dao;

import com.cloud.network.IpReservationVO;
import com.cloud.utils.db.GenericDao;

import java.util.List;

public interface IpReservationDao extends GenericDao<IpReservationVO, Long> {
    List<IpReservationVO> getIpReservationsForNetwork(long networkId);

    List<FullIpReservation> getAllIpReservations();

    class FullIpReservation {
        public String id;
        public String startip;
        public String endip;
        public String networkid;

        public FullIpReservation(String id, String startip, String endip, String networkid) {
            this.id = id;
            this.startip = startip;
            this.endip = endip;
            this.networkid = networkid;
        }
    }
}
