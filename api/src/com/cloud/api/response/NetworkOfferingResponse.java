package com.cloud.api.response;

import java.util.Date;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class NetworkOfferingResponse extends BaseResponse{
    @SerializedName("id") @Param(description="the id of the network offering")
    private Long id;

    @SerializedName("name") @Param(description="the name of the network offering")
    private String name;
    
    @SerializedName("displaytext") @Param(description="an alternate display text of the network offering.")
    private String displayText;
    
    @SerializedName("tags") @Param(description="the tags for the network offering")
    private String tags;
    
    @SerializedName("created") @Param(description="the date this network offering was created")
    private Date created;
    
    @SerializedName("maxconnections") @Param(description="the max number of concurrent connection the network offering supports")
    private Integer maxConnections;
    
    @SerializedName("type") @Param(description="type of the network. Supported types are Virtualized, DirectSingle, DirectDual")
    private String type;
    
    @SerializedName("traffictype") @Param(description="the traffic type for the network offering, supported types are Public, Management, Control, Guest, Vlan or Storage.")
    private String trafficType;
    
    @SerializedName("isdefault") @Param(description="true if network offering is default, false otherwise")
    private Boolean isDefault;
    
    @SerializedName("isshared") @Param(description="true if network offering is shared, false otherwise")
    private Boolean isShared;
    
    @SerializedName("specifyvlan") @Param(description="true if network offering supports vlans, false otherwise")
    private Boolean specifyVlan;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public Integer getMaxconnections() {
        return maxConnections;
    }

    public void setMaxconnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTrafficType() {
        return trafficType;
    }

    public void setTrafficType(String trafficType) {
        this.trafficType = trafficType;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

    public Boolean getSpecifyVlan() {
        return specifyVlan;
    }

    public void setSpecifyVlan(Boolean specifyVlan) {
        this.specifyVlan = specifyVlan;
    }

    public Boolean getIsShared() {
        return isShared;
    }

    public void setIsShared(Boolean isShared) {
        this.isShared = isShared;
    }
}
