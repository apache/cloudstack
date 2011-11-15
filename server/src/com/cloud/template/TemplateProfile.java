package com.cloud.template;

import java.util.Map;

import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.VMTemplateVO;

public class TemplateProfile {	
	Long userId;
	String name;
	String displayText;
	Integer bits;
	Boolean passwordEnabled;
	Boolean requiresHvm;
	String url;
	Boolean isPublic;
	Boolean featured;
	Boolean isExtractable;
	ImageFormat format;
	Long guestOsId;
	Long zoneId;
	HypervisorType hypervisorType;
	String accountName;
	Long domainId;
	Long accountId;
	String chksum;
	Boolean bootable;
	Long templateId;
	VMTemplateVO template;
	String templateTag;
	Map details;
	
	public TemplateProfile(Long templateId, Long userId, String name, String displayText, Integer bits, Boolean passwordEnabled, Boolean requiresHvm,
			String url, Boolean isPublic, Boolean featured, Boolean isExtractable, ImageFormat format, Long guestOsId, Long zoneId,
			HypervisorType hypervisorType, String accountName, Long domainId, Long accountId, String chksum, Boolean bootable, Map details) {
		this.templateId = templateId;
		this.userId = userId;
		this.name = name;
		this.displayText = displayText;
		this.bits = bits;
		this.passwordEnabled = passwordEnabled;
		this.requiresHvm = requiresHvm;
		this.url = url;
		this.isPublic = isPublic;
		this.featured = featured;
		this.isExtractable = isExtractable;
		this.format = format;
		this.guestOsId = guestOsId;
		this.zoneId = zoneId;
		this.hypervisorType = hypervisorType;
		this.accountName = accountName;
		this.domainId = domainId;
		this.accountId = accountId;
		this.chksum = chksum;
		this.bootable = bootable;
		this.details = details;
	}
	
	public TemplateProfile(Long userId, VMTemplateVO template, Long zoneId) {
		this.userId = userId;
		this.template = template;
		this.zoneId = zoneId;
	}
	
    public TemplateProfile(Long templateId, Long userId, String name, String displayText, Integer bits, Boolean passwordEnabled, Boolean requiresHvm,
            String url, Boolean isPublic, Boolean featured, Boolean isExtractable, ImageFormat format, Long guestOsId, Long zoneId,
            HypervisorType hypervisorType, String accountName, Long domainId, Long accountId, String chksum, Boolean bootable, String templateTag, Map details) {
        this(templateId, userId, name, displayText, bits, passwordEnabled, requiresHvm, url, isPublic, featured, isExtractable, format, guestOsId, zoneId,
                hypervisorType, accountName, domainId, accountId, chksum, bootable, details);
        this.templateTag = templateTag;
    }	
	
	public Long getTemplateId() {
		return templateId;
	}
	public void setTemplateId(Long id) {
		this.templateId = id;
	}
	
	public Long getUserId() {
		return userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
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
	public void setDisplayText(String text) {
		this.displayText = text;
	}
	
	public Integer getBits() {
		return bits;
	}
	public void setBits(Integer bits) {
		this.bits = bits;
	}
	
	public Boolean getPasswordEnabled() {
		return passwordEnabled;
	}
	public void setPasswordEnabled(Boolean enabled) {
		this.passwordEnabled = enabled;
	}
	
	public Boolean getRequiresHVM() {
		return requiresHvm;
	}
	public void setRequiresHVM(Boolean hvm) {
		this.requiresHvm = hvm;
	}
	
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
	public Boolean getIsPublic() {
		return isPublic;
	}
	public void setIsPublic(Boolean is) {
		this.isPublic = is;
	}
	
	public Boolean getFeatured() {
		return featured;
	}
	public void setFeatured(Boolean featured) {
		this.featured = featured;
	}
	
	public Boolean getIsExtractable() {
		return isExtractable;
	}
	public void setIsExtractable(Boolean is) {
		this.isExtractable = is;
	}
	
	public ImageFormat getFormat() {
		return format;
	}
	public void setFormat(ImageFormat format) {
		this.format = format;
	}
	
	public Long getGuestOsId() {
		return guestOsId;
	}
	public void setGuestOsId(Long id) {
		this.guestOsId = id;
	}
	
	public Long getZoneId() {
		return zoneId;
	}
	public void setZoneId(Long id) {
		this.zoneId = id;
	}
	
	public HypervisorType getHypervisorType() {
		return hypervisorType;
	}
	public void setHypervisorType(HypervisorType type) {
		this.hypervisorType = type;
	}
	
	public Long getDomainId() {
		return domainId;
	}
	public void setDomainId(Long id) {
		this.domainId = id;
	}
	
	public Long getAccountId() {
		return accountId;
	}
	public void setAccountId(Long id) {
		this.accountId = id;
	}
	
	public String getCheckSum() {
		return chksum;
	}
	public void setCheckSum(String chksum) {
		this.chksum = chksum;
	}
	
	public Boolean getBootable() {
		return this.bootable;
	}
	public void setBootable(Boolean bootable) {
		this.bootable = bootable;
	}
	
	public VMTemplateVO getTemplate() {
		return template;
	}
	public void setTemplate(VMTemplateVO template) {
		this.template = template;
	}
	
    public String getTemplateTag() {
        return templateTag;
    }    

    public void setTemplateTag(String templateTag) {
        this.templateTag = templateTag;
    }  	
	
    public Map getDetails() {
    	return this.details;
    }
    
    public void setDetails(Map details) {
    	this.details = details;
    }
}
