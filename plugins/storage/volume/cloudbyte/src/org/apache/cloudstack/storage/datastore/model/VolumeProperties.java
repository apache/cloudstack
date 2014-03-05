package org.apache.cloudstack.storage.datastore.model;


import com.google.gson.annotations.SerializedName;

public class VolumeProperties {

    @SerializedName("id")
    private String id;
    
    @SerializedName("groupid")
    private String groupid;
    
    @SerializedName("iops")
    private String iops;
    
    @SerializedName("name")
    private String name;
    
    public String getid() {
        return id;
    }
    
    public String getQosgroupid() {
        return groupid;
    }
    
    public String getName() {
        return name;
    }
    
    public String getIops() {
        return iops;
    }
}
