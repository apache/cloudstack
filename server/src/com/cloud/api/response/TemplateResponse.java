package com.cloud.api.response;

import java.util.Date;

import com.cloud.api.ResponseObject;
import com.cloud.serializer.Param;

public class TemplateResponse implements ResponseObject {
    @Param(name="id")
    private long id;

    @Param(name="name")
    private String name;

    @Param(name="displaytext")
    private String displayText;

    @Param(name="ispublic", propName="public")
    private boolean isPublic;

    @Param(name="created")
    private Date created;

    @Param(name="isready", propName="ready")
    private boolean isReady;

    @Param(name="passwordenabled")
    private boolean passwordEnabled;

    @Param(name="crossZones")
    private boolean crossZones;

    @Param(name="ostypeid")
    private Long osTypeId;

    @Param(name="ostypename")
    private String osTypeName;

    @Param(name="accountid")
    private Long accountId;

    @Param(name="account")
    private String account;

    @Param(name="zoneid")
    private Long zoneId;

    @Param(name="zonename")
    private String zoneName;

    @Param(name="domain")
    private String domainName;  

    @Param(name="domainid")
    private long domainId;

    public Long getZoneId() {
        return zoneId;
    }

    public void setZoneId(Long zoneId) {
        this.zoneId = zoneId;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public Long getOsTypeId() {
        return osTypeId;
    }

    public void setOsTypeId(Long osTypeId) {
        this.osTypeId = osTypeId;
    }

    public String getOsTypeName() {
        return osTypeName;
    }

    public void setOsTypeName(String osTypeName) {
        this.osTypeName = osTypeName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

    public boolean isPasswordEnabled() {
        return passwordEnabled;
    }

    public void setPasswordEnabled(boolean passwordEnabled) {
        this.passwordEnabled = passwordEnabled;
    }

    public boolean isCrossZones() {
        return crossZones;
    }

    public void setCrossZones(boolean crossZones) {
        this.crossZones = crossZones;
    }

    public long getDomainId() {
        return domainId;
    }

    public String getDomainName(){
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setDomainId(long domainId) {
        this.domainId = domainId;
    }
}
