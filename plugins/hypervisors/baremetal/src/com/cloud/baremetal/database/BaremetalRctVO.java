package com.cloud.baremetal.database;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.UUID;

/**
 * Created by frank on 5/8/14.
 */
@Entity
@Table(name = "baremetal_rct")
public class BaremetalRctVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "uuid")
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "url")
    private String url;

    @Column(name = "rct")
    private String rct;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRct() {
        return rct;
    }

    public void setRct(String rct) {
        this.rct = rct;
    }
}
