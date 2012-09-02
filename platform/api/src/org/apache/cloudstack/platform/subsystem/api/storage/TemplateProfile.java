/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.platform.subsystem.api.storage;

import com.cloud.storage.Storage;

public class TemplateProfile {
	private String _uri;
	private String _imageStorageUri;
	private String _localPath;
	private long _id;
	private long _templatePoolRefId;
	public String getURI() {
		return _uri;
	}
	
	public long getId() {
		return _id;
	}
	
	public String getLocalPath() {
		return _localPath;
	}
	
	public void setLocalPath(String path) {
		_localPath = path;
	}
	
	public void setTemplatePoolRefId(long id) {
		this._templatePoolRefId = id;
	}
	
	public long getTemplatePoolRefId() {
		return this._templatePoolRefId;
	}
	
	public String getImageStorageUri() {
		return _imageStorageUri;
	}
	
	public String getUniqueName() {
		return null;
	}
	
	public Storage.ImageFormat getFormat() {
		return null;
	}
	
	public String getInstallPath() {
		return null;
	}
	
	public long getTemplateSize() {
		return 0;
	}
}
