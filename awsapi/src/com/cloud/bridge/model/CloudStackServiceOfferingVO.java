package com.cloud.bridge.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="disk_offering")
public class CloudStackServiceOfferingVO {

    @Id
    @Column(name="id")
    private String id;
    
    @Column(name="name")
    private String name;
    
    @Column(name="domain_id")
    private String domainId;
    
    
    public String getId() {
        return id;
    }


    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public String getDomainId() {
        return domainId;
    }


    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }
    


}
