package com.cloud.bridge.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;


@Entity
@Table(name="offering_bundle")
public class OfferingBundleVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="ID")
    private long id;
    
    @Column(name="AmazonEC2Offering")
    private String amazonOffering;

    @Column(name="CloudStackOffering")
    private String cloudstackOffering;
    
    public long getID() {
        return id;
    }
    
    public String getAmazonOffering() {
        return amazonOffering;
    }

    public void setAmazonOffering(String amazonOffering) {
        this.amazonOffering = amazonOffering;
    }

    public String getCloudstackOffering() {
        return cloudstackOffering;
    }

    public void setCloudstackOffering(String cloudstackOffering) {
        this.cloudstackOffering = cloudstackOffering;
    }
    

}
