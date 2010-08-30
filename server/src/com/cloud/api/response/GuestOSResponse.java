package com.cloud.api.response;

import com.cloud.api.ResponseObject;
import com.cloud.serializer.Param;

public class GuestOSResponse implements ResponseObject {
    @Param(name="id")
    private Long id;

    @Param(name="oscategoryid")
    private Long osCategoryId;

    @Param(name="description")
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOsCategoryId() {
        return osCategoryId;
    }

    public void setOsCategoryId(Long osCategoryId) {
        this.osCategoryId = osCategoryId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
