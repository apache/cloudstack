package org.apache.cloudstack.ontapsvm;

import com.cloud.utils.db.GenericDao;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "ontap_svms")
public class OntapSvmVO implements OntapSvm {

    protected OntapSvmVO() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "uuid")
    String uuid = UUID.randomUUID().toString();

    @Column(name = "name")
    String name;

    @Column(name = "ip4_address")
    String iPv4Address;

    @Column(name = "vlan")
    int vlan;

    @Column(name = "network_id")
    long networkId;

    @Column(name = GenericDao.CREATED_COLUMN)
    Date created;

    @Column(name = GenericDao.REMOVED_COLUMN)
    Date removed;

    public OntapSvmVO(String name, String iPv4Address, int vlan, int networkId) {
        this.name = name;
        this.iPv4Address = iPv4Address;
        this.vlan = vlan;
        this.networkId = networkId;
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
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getiPv4Address() {
        return iPv4Address;
    }

    @Override
    public void setiPv4Address(String iPv4Address) {
        this.iPv4Address = iPv4Address;
    }

    @Override
    public int getVlan() {
        return vlan;
    }

    @Override
    public void setVlan(int vlan) {
        this.vlan = vlan;
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
                name +
                "-" +
                iPv4Address +
                "-" +
                networkId +
                "-" +
                vlan;
    }
}
