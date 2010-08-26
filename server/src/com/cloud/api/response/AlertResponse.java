package com.cloud.api.response;

import java.util.Date;

import com.cloud.api.ResponseObject;
import com.cloud.serializer.Param;

public class AlertResponse implements ResponseObject {
    @Param(name="type")
    private Short alertType;

    @Param(name="description")
    private String description;

    @Param(name="sent")
    private Date lastSent;

    public Short getAlertType() {
        return alertType;
    }

    public void setAlertType(Short alertType) {
        this.alertType = alertType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getLastSent() {
        return lastSent;
    }

    public void setLastSent(Date lastSent) {
        this.lastSent = lastSent;
    }
}
