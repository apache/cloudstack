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

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;


@Entity
@Table(name="sobject")
public class SObjectVO {
	//private static final long serialVersionUID = 8566744941395660486L;

	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="ID")
	private Long id;
	
	@Column(name="SBucketID")
	private long bucketID;

	@Column(name="NameKey")
	private String nameKey;
	
	@Column(name="OwnerCanonicalID")
	private String ownerCanonicalId;
	
	@Column(name="NextSequence")
	private int nextSequence;
	
	@Column(name="DeletionMark")
	private String deletionMark;	// This must also a unique ID to give to the REST client
	
	@Column(name="CreateTime")
	@Temporal(value=TemporalType.TIMESTAMP)
	private Date createTime;
	
	@Transient
	private SBucket bucket;
	
	@Transient
	private Set<SObjectItemVO> items = new HashSet<SObjectItemVO>();
	
	public SObjectVO() {
		deletionMark = null;
	}
	
	public Long getId() {
		return id;
	}

	private void setId(Long id) {
		this.id = id;
	}
	
	public long getBucketID() {
	    return bucketID;
	}

	public void setBucketID(long bucketID) {
	    this.bucketID = bucketID;
	}

	public String getNameKey() {
		return nameKey;
	}

	public void setNameKey(String nameKey) {
		this.nameKey = nameKey;
	}

	public String getOwnerCanonicalId() {
		return ownerCanonicalId;
	}

	public void setOwnerCanonicalId(String ownerCanonicalId) {
		this.ownerCanonicalId = ownerCanonicalId;
	}

	public int getNextSequence() {
		return nextSequence;
	}

	public void setNextSequence(int nextSequence) {
		this.nextSequence = nextSequence;
	}

	public String getDeletionMark() {
		return deletionMark;
	}

	public void setDeletionMark(String deletionMark) {
		this.deletionMark = deletionMark;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	
	public SBucket getBucket() {
		return bucket;
	}

	public void setBucket(SBucket bucket) {
		this.bucket = bucket;
	}
	
	public Set<SObjectItemVO> getItems() {
		return items;
	}

	public void setItems(Set<SObjectItemVO> items) {
		this.items = items;
	}
	
	public void deleteItem( long id ) {
		Iterator<SObjectItemVO> it = getItems().iterator();
	
		while( it.hasNext()) 
		{
			SObjectItemVO oneItem = it.next();
			if (id == oneItem.getId()) {
				boolean bRemoved = items.remove( oneItem );
				System.out.println( "deleteItem from sobject: " + bRemoved );
				return;
			}
		}
	}
	
	public SObjectItemVO getLatestVersion( boolean versioningOff ) {
		Iterator<SObjectItemVO> it = getItems().iterator();
		int maxVersion = 0;
		int curVersion = 0;
		SObjectItemVO latestItem = null;
		
		while( it.hasNext()) 
		{
			SObjectItemVO item = it.next();
			
		    //    If versioning is off then return the item with the null version string (if exists)
			//    For example, the bucket could have allowed versioning and then it was suspended
			//    If an application wants a specific version it will need to explicitly ask for it
			try {
                String version = item.getVersion();
                if (versioningOff && null == version) {
                	return item;
                }
				curVersion = Integer.parseInt( version );
				
			} catch (NumberFormatException e) {
				curVersion = 0;
			}
			
			if(curVersion >= maxVersion) {
				maxVersion = curVersion;
				latestItem = item;
			}
		}
		return latestItem;
	}
	
	/**
	 * S3 versioning allows the client to request the return of a specific version,
	 * not just the last version.
	 * 
	 * @param wantVersion
	 * @return
	 */
	public SObjectItemVO getVersion( String wantVersion ) 
	{
		Iterator<SObjectItemVO> it = getItems().iterator();	
		while( it.hasNext()) 
		{
			SObjectItemVO item = it.next();
			String curVersion = item.getVersion();
			if (null != curVersion && wantVersion.equalsIgnoreCase( curVersion )) return item;				
		}
		return null;
	}

	@Override
	public boolean equals(Object other) {
		if(this == other)
			return true;
		
		if(!(other instanceof SObjectVO))
			return false;
		
		if(!getNameKey().equals(((SObjectVO)other).getNameKey()))
			return false;
		
		if(getBucket() != null) {
			if(!getBucket().equals(((SObjectVO)other).getBucket()))
				return false;
		} else {
			if(((SObjectVO)other).getBucket() != null)
				return false;
		}
		
		return true;
	}
	
	@Override
	public int hashCode() {
		int hashCode = 0;
		hashCode = hashCode*17 + getNameKey().hashCode();
		
		if(getBucket() != null)
			hashCode = hashCode*17 + getBucket().hashCode(); 
		return hashCode;
	}
}
