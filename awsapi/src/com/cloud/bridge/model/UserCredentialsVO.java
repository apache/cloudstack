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
package com.cloud.bridge.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name="usercredentials")
public class UserCredentialsVO{
	private static final long serialVersionUID = 7459503272337054299L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="ID")
	private Long id;
	
	@Column(name="AccessKey")
	private String accessKey;
	
	@Column(name="SecretKey")
	private String secretKey;
	
	@Column(name="CertUniqueId")
	private String certUniqueId;

	public UserCredentialsVO() { }
	
	public UserCredentialsVO(String accessKey, String secretKey) {
	    this.accessKey = accessKey;
	    this.secretKey = secretKey;
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
		
		if (!(other instanceof UserCredentialsVO)) return false;
		
		// The cert id can be null.  The cert is unused in the REST API.
		if ( getAccessKey().equals(((UserCredentialsVO)other).getAccessKey()) && 
		     getSecretKey().equals(((UserCredentialsVO)other).getSecretKey()))
		{
			String thisCertId  = getCertUniqueId();
			String otherCertId = ((UserCredentialsVO)other).getCertUniqueId();
			
			if (null == thisCertId && null == otherCertId) return true;
			
			if (null != thisCertId && null != otherCertId) return thisCertId.equals( otherCertId );
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		int hashCode = 0;
		String thisCertId = getCertUniqueId();
		
		// The cert id can be null.  The cert is unused in the REST API.
		hashCode = hashCode*17 + getAccessKey().hashCode();
		hashCode = hashCode*17 + getSecretKey().hashCode();
		if (null != thisCertId) hashCode = hashCode*17 + thisCertId.hashCode();
		return hashCode;
	}
}
