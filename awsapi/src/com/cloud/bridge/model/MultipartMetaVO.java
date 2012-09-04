package com.cloud.bridge.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="multipart_meta")

public class MultipartMetaVO {
    
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="ID")
    private Long id;
    
    @Column(name="UploadID")
    private long uploadID;
    
    @Column(name="Name")
    private String name;
    
    @Column(name="Value")
    private String value;

    public long getID() {
        return id;
    }
    
    public long getUploadID() {
        return uploadID;
    }

    public void setUploadID(long uploadID) {
        this.uploadID = uploadID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    

}
