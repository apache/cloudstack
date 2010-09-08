package com.cloud.agent.api.storage;

import com.cloud.storage.VMTemplateHostVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.agent.api.storage.AbstractUploadCommand;
import com.cloud.agent.api.storage.DownloadCommand.PasswordAuth;


public class UploadCommand extends AbstractUploadCommand {

	private VMTemplateVO template;
	private String url;
	private String installPath;	
	private boolean hvm;
	private String description;
	private String checksum;
	private PasswordAuth auth;
	private long templateSizeInBytes;
	private long id;

	public UploadCommand(VMTemplateVO template, String url, VMTemplateHostVO vmTemplateHost) {
		
		this.template = template;
		this.url = url;
		this.installPath = vmTemplateHost.getInstallPath();
		this.checksum = template.getChecksum();
		this.id = template.getId();
		this.templateSizeInBytes = vmTemplateHost.getSize();
		
	}
	
	protected UploadCommand() {
	}
	
	public UploadCommand(UploadCommand that) {
		this.template = that.template;
		this.url = that.url;
		this.installPath = that.installPath;
		this.checksum = that.getChecksum();
		this.id = that.id;
	}

	public String getDescription() {
		return description;
	}


	public VMTemplateVO getTemplate() {
		return template;
	}

	public void setTemplate(VMTemplateVO template) {
		this.template = template;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public boolean isHvm() {
		return hvm;
	}

	public void setHvm(boolean hvm) {
		this.hvm = hvm;
	}

	public PasswordAuth getAuth() {
		return auth;
	}

	public void setAuth(PasswordAuth auth) {
		this.auth = auth;
	}

	public Long getTemplateSizeInBytes() {
		return templateSizeInBytes;
	}

	public void setTemplateSizeInBytes(Long templateSizeInBytes) {
		this.templateSizeInBytes = templateSizeInBytes;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setInstallPath(String installPath) {
		this.installPath = installPath;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public String getInstallPath() {
		return installPath;
	}

	public String getChecksum() {
		return checksum;
	}
}
