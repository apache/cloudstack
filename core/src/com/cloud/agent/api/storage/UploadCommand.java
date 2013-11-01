// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.agent.api.storage;

import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.agent.api.to.TemplateTO;
import com.cloud.storage.Upload.Type;
import com.cloud.template.VirtualMachineTemplate;


public class UploadCommand extends AbstractUploadCommand implements InternalIdentity {

	private TemplateTO template;
	private String url;
	private String installPath;
	private boolean hvm;
	private String description;
	private String checksum;
	private PasswordAuth auth;
	private long templateSizeInBytes;
	private long id;
	private Type type;

	public UploadCommand(VirtualMachineTemplate template, String url, String installPath, long sizeInBytes) {

		this.template = new TemplateTO(template);
		this.url = url;
		this.installPath = installPath;
		checksum = template.getChecksum();
		id = template.getId();
		templateSizeInBytes = sizeInBytes;

	}

	public UploadCommand(String url, long id, long sizeInBytes, String installPath, Type type){
		template = null;
		this.url = url;
		this.installPath = installPath;
		this.id = id;
		this.type = type;
		templateSizeInBytes = sizeInBytes;
	}

	protected UploadCommand() {
	}

	public UploadCommand(UploadCommand that) {
		template = that.template;
		url = that.url;
		installPath = that.installPath;
		checksum = that.getChecksum();
		id = that.id;
	}

	public String getDescription() {
		return description;
	}


	public TemplateTO getTemplate() {
		return template;
	}

	public void setTemplate(TemplateTO template) {
		this.template = template;
	}

	@Override
    public String getUrl() {
		return url;
	}

	@Override
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

	@Override
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
