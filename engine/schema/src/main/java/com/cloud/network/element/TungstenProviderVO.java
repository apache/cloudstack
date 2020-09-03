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

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "port")
    private String port;

    @Column(name = "hostname")
    private String hostname;

    public TungstenProviderVO() {
        this.uuid = UUID.randomUUID().toString();
    }

    public TungstenProviderVO(long nspId, String providerName, String port, String hostname) {
        this.nspId = nspId;
        this.uuid = UUID.randomUUID().toString();
        this.providerName = providerName;
        this.port = port;
        this.hostname = hostname;
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
}
