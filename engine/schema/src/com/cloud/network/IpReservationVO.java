package com.cloud.network;

import com.cloud.utils.db.GenericDao;
import com.cloud.utils.net.NetUtils;
import org.apache.cloudstack.network.IpReservation;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ip_reservation")
public class IpReservationVO implements IpReservation {

    protected IpReservationVO() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "start_ip")
    private String startIp;

    @Column(name = "end_ip")
    private String endIp;

    @Column(name = "network_id")
    private long networkId;

    @Column(name = GenericDao.CREATED_COLUMN)
    private Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    private Date removed;

    public IpReservationVO(String startIp, String endIp, long networkId) {
        this.startIp = startIp;
        this.endIp = endIp;
        this.networkId = networkId;
    }

    @Override
    public List<String> getIpList() {
        List<String> ret = new ArrayList<>();
        long current = NetUtils.ip2Long(getStartIp());
        long end = NetUtils.ip2Long(getEndIp());
        while (current <= end) {
            ret.add(NetUtils.long2Ip(current));
            current++;
        }

        return ret;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getStartIp() {
        return startIp;
    }

    @Override
    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }

    @Override
    public String getEndIp() {
        return endIp;
    }

    @Override
    public void setEndIp(String endIp) {
        this.endIp = endIp;
    }

    @Override
    public long getNetworkId() {
        return networkId;
    }

    @Override
    public void setNetworkId(long networkId) {
        this.networkId = networkId;
    }

    @Override
    public Date getCreated() {
        return created;
    }

    @Override
    public void setCreated(Date created) {
        this.created = created;
    }

    @Override
    public Date getRemoved() {
        return removed;
    }

    @Override
    public void setRemoved(Date removed) {
        this.removed = removed;
    }

    public String toString() {
        return "OntapSvm[" + id +
                "-" +
                startIp +
                "-" +
                endIp +
                "-" +
                networkId +
                "]";
    }
}
