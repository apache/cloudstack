package com.cloud.api.response;

import java.util.Date;

import com.cloud.api.ResponseObject;
import com.cloud.serializer.Param;

public class ServiceOfferingResponse implements ResponseObject {
    @Param(name="id")
    private Long id;

    @Param(name="name")
    private String name;

    @Param(name="displaytext")
    private String displayText;

    @Param(name="cpunumber")
    private int cpuNumber;

    @Param(name="cpuspeed")
    private int cpuSpeed;

    @Param(name="memory")
    private int memory;

    @Param(name="created")
    private Date created;

    @Param(name="storagetype")
    private String storageType;

    @Param(name="offerha")
    private Boolean offerHa;

    @Param(name="usevirtualnetwork")
    private Boolean useVirtualNetwork;

    @Param(name="tags")
    private String tags;

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

    public int getCpuNumber() {
        return cpuNumber;
    }

    public void setCpuNumber(int cpuNumber) {
        this.cpuNumber = cpuNumber;
    }

    public int getCpuSpeed() {
        return cpuSpeed;
    }

    public void setCpuSpeed(int cpuSpeed) {
        this.cpuSpeed = cpuSpeed;
    }

    public int getMemory() {
        return memory;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public Boolean getOfferHa() {
        return offerHa;
    }

    public void setOfferHa(Boolean offerHa) {
        this.offerHa = offerHa;
    }

    public Boolean getUseVirtualNetwork() {
        return useVirtualNetwork;
    }

    public void setUseVirtualNetwork(Boolean useVirtualNetwork) {
        this.useVirtualNetwork = useVirtualNetwork;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}
