/*
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.bridge.model;

import java.io.Serializable;

public class UserCredentials implements Serializable {
	private static final long serialVersionUID = 7459503272337054299L;
	
	private Long id;
	private String accessKey;
	private String secretKey;
	private String certUniqueId;

	public UserCredentials() {
	}
	
	public Long getId() {
		return id;
	}
	
	private void setId(Long id) {
		this.id = id;
	}
	
	public String getAccessKey() {
		return accessKey;
	}
	
	public void setAccessKey(String accessKey) {
		this.accessKey = accessKey;
	}
	
	public String getSecretKey() {
		return secretKey;
	}
	
	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}
	
	public String getCertUniqueId() {
		return certUniqueId;
	}
	
	public void setCertUniqueId(String certUniqueId) {
		this.certUniqueId = certUniqueId;
	}
	
	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		
		if (!(other instanceof UserCredentials)) return false;
		
		// -> the cert id can be null in both or either, since it is only used for the SOAP API
		if ( getAccessKey().equals(((UserCredentials)other).getAccessKey()) && 
		     getSecretKey().equals(((UserCredentials)other).getSecretKey()))
		{
			String thisCertId  = getCertUniqueId();
			String otherCertId = ((UserCredentials)other).getCertUniqueId();
			
			if (null == thisCertId && null == otherCertId) return true;
			
			if (null != thisCertId && null != otherCertId) return thisCertId.equals( otherCertId );
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		int hashCode = 0;
		String thisCertId = getCertUniqueId();
		
		// -> the cert id can be null, since it is only used for the SOAP API
		hashCode = hashCode*17 + getAccessKey().hashCode();
		hashCode = hashCode*17 + getSecretKey().hashCode();
		if (null != thisCertId) hashCode = hashCode*17 + thisCertId.hashCode();
		return hashCode;
	}
}
