package com.cloud.network.element;

import com.cloud.network.TungstenProvider;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = ("tungsten_providers"))
public class TungstenProviderVO implements TungstenProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "nsp_id")
    private long nspId;

    @Column(name = "physical_network_id")
    private long physicalNetworkId;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "host_id")
    private long hostId;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "port")
    private String port;

    @Column(name = "hostname")
    private String hostname;

    @Column(name = "vrouter")
    private String vrouter;

    @Column(name = "vrouter_port")
    private String vrouterPort;

    public TungstenProviderVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public TungstenProviderVO(long nspId, long physicalNetworkId, String providerName, long hostId, String port, String hostname, String vrouter, String vrouterPort) {
        this.nspId = nspId;
        this.physicalNetworkId = physicalNetworkId;
        this.uuid = UUID.randomUUID().toString();
        this.providerName = providerName;
        this.port = port;
        this.hostname = hostname;
        this.vrouter = vrouter;
        this.vrouterPort = vrouterPort;
        this.hostId = hostId;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    @Override
    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public long getNspId() {
        return nspId;
    }

    public void setNspId(long nspId) {
        this.nspId = nspId;
    }

    public long getPhysicalNetworkId() {
        return physicalNetworkId;
    }

    public void setPhysicalNetworkId(long physicalNetworkId) {
        this.physicalNetworkId = physicalNetworkId;
    }

    public String getVrouter() {
        return vrouter;
    }

    public void setVrouter(final String vrouter) {
        this.vrouter = vrouter;
    }

    public String getVrouterPort() {
        return vrouterPort;
    }

    public void setVrouterPort(final String vrouterPort) {
        this.vrouterPort = vrouterPort;
    }

    public long getHostId() {
        return hostId;
    }

    public void setHostId(long hostId) {
        this.hostId = hostId;
    }
}
