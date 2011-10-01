package com.cloud.simulator;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="mockconfiguration")
public class MockConfigurationVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;
    
    @Column(name="data_center_id", nullable=false)
    private Long dataCenterId;
    
    @Column(name="pod_id")
    private Long podId;
    
    @Column(name="cluster_id")
    private Long clusterId;
    
    @Column(name="host_id")
    private Long hostId;
    
    @Column(name="name")
    private String name;
    
    @Column(name="values")
    private String values;
    
    public long getId() {
        return this.id;
    }
    
    public Long getDataCenterId() {
        return this.dataCenterId;
    }
    
    public void setDataCenterId(Long dcId) {
        this.dataCenterId = dcId;
    }
    
    public Long getPodId() {
        return this.podId;
    }
    
    public void setPodId(Long podId) {
        this.podId = podId;
    }
    
    public Long getClusterId() {
        return this.clusterId;
    }
    
    public void setClusterId(Long clusterId) {
        this.clusterId = clusterId;
    }
    
    public Long getHostId() {
        return this.hostId;
    }
    
    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getValues() {
        return this.values;
    }
    
    public Map<String, String> getParameters() {
        Map<String, String> maps = new HashMap<String, String>();
        if (this.values == null) {
            return maps;
        }
        
        String[] vals = this.values.split("\\|");
        for (String val : vals) {
            String[] paras = val.split(":");
            maps.put(paras[0], paras[1]);
        }
        return maps;
    }
    
    public void setValues(String values) {
        this.values = values;
    }
}

