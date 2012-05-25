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
package com.cloud.bridge.persist.dao;

import java.util.ArrayList;
import java.util.List;

import com.cloud.bridge.model.SBucket;
import com.cloud.bridge.model.SObject;
import com.cloud.bridge.persist.EntityDao;
import com.cloud.bridge.util.EntityParam;

/**
 * @author Kelven Yang
 */
public class SObjectDao extends EntityDao<SObject> {
	public SObjectDao() {
		super(SObject.class);
	}

	public SObject getByNameKey(SBucket bucket, String nameKey) {
		return queryEntity("from SObject where bucket=? and nameKey=?", 
				new Object[] { new EntityParam(bucket), nameKey });
	}
	
	public List<SObject> listBucketObjects(SBucket bucket, String prefix, String marker, int maxKeys) {
		StringBuffer sb = new StringBuffer();
		List<Object> params = new ArrayList<Object>();

		sb.append("from SObject o left join fetch o.items where deletionMark is null and o.bucket=?");
		params.add(new EntityParam(bucket));
		
		if(prefix != null && !prefix.isEmpty()) {
			sb.append(" and o.nameKey like ?");
			params.add(new String(prefix + "%"));
		}
		
		if(marker != null && !marker.isEmpty()) {
			sb.append(" and o.nameKey > ?");
			params.add(marker);
		}
		
		return queryEntities(sb.toString(), 0, maxKeys, params.toArray());
	}
	
	public List<SObject> listAllBucketObjects(SBucket bucket, String prefix, String marker, int maxKeys) {
		StringBuffer sb = new StringBuffer();
		List<Object> params = new ArrayList<Object>();

		sb.append("from SObject o left join fetch o.items where o.bucket=?");
		params.add(new EntityParam(bucket));
		
		if(prefix != null && !prefix.isEmpty()) {
			sb.append(" and o.nameKey like ?");
			params.add(new String(prefix + "%"));
		}
		
		if(marker != null && !marker.isEmpty()) {
			sb.append(" and o.nameKey > ?");
			params.add(marker);
		}
		
		return queryEntities(sb.toString(), 0, maxKeys, params.toArray());
	}
}
