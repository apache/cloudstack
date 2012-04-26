package com.cloud.stack.models;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;

public class CloudStackConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    @SerializedName(ApiConstants.CATEGORY) 
    private String category;

    @SerializedName(ApiConstants.NAME)
    private String name;

    @SerializedName(ApiConstants.VALUE)
    private String value;

    @SerializedName(ApiConstants.DESCRIPTION)
    private String description;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    

}
