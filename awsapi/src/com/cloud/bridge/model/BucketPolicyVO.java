package com.cloud.bridge.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="bucket_policies")
public class BucketPolicyVO {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="ID")
    private long id;
    
    @Column(name="BucketName")
    private String bucketName;
    
    @Column(name="OwnerCanonicalID")
    private String ownerCanonicalID;
    
    @Column(name="Policy")
    private String policy;
    
    public BucketPolicyVO() { }
    public BucketPolicyVO(String bucketName, String client, String policy) {
	this.bucketName = bucketName;
	this.ownerCanonicalID = client;
	this.policy = policy;
    }
    
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getBucketName() {
        return bucketName;
    }
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }
    public String getOwnerCanonicalID() {
        return ownerCanonicalID;
    }
    public void setOwnerCanonicalID(String ownerCanonicalID) {
        this.ownerCanonicalID = ownerCanonicalID;
    }
    public String getPolicy() {
        return policy;
    }
    public void setPolicy(String policy) {
        this.policy = policy;
    }
}
