package com.cloud.network.element;

import com.cloud.network.NsxProvider;
import com.cloud.utils.db.Encrypt;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "nsx_providers")
public class NsxProviderVO implements NsxProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    long id;

    @Column(name = "zone_id")
    private long zoneId;

    @Column(name = "uuid")
    private String uuid;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "hostname")
    private String hostname;

    @Column(name = "username")
    private String username;

    @Encrypt
    @Column(name = "password")
    private String password;

    @Column(name = "tier0_gateway")
    private String tier0Gateway;

    @Column(name = "edge_cluster")
    private String edgeCluster;


    public NsxProviderVO( long zoneId,String providerName, String hostname, String username, String password, String tier0Gateway, String edgeCluster) {
        this.zoneId = zoneId;
        this.uuid = UUID.randomUUID().toString();
        this.providerName = providerName;
        this.hostname = hostname;
        this.username = username;
        this.password = password;
        this.tier0Gateway = tier0Gateway;
        this.edgeCluster = edgeCluster;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public long getZoneId() {
        return zoneId;
    }

    public void setZoneId(long zoneId) {
        this.zoneId = zoneId;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    @Override
    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTier0Gateway() {
        return tier0Gateway;
    }

    public void setTier0Gateway(String tier0Gateway) {
        this.tier0Gateway = tier0Gateway;
    }

    public String getEdgeCluster() {
        return edgeCluster;
    }

    public void setEdgeCluster(String edgeCluster) {
        this.edgeCluster = edgeCluster;
    }
}
