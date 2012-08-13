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
package com.cloud.bridge.persist.dao;

import java.util.List;

import com.cloud.bridge.model.SMeta;
import com.cloud.bridge.persist.EntityDao;
import com.cloud.bridge.persist.PersistContext;
import com.cloud.bridge.service.core.s3.S3MetaDataEntry;

public class SMetaDao extends EntityDao<SMeta> {
	public SMetaDao() {
		super(SMeta.class);
	}
	
	public List<SMeta> getByTarget(String target, long targetId) {
		return queryEntities("from SMeta where target=? and targetId=?", new Object[] {target, targetId});
	}

	public SMeta save(String target, long targetId, S3MetaDataEntry entry) {
		SMeta meta = new SMeta();
		meta.setTarget(target);
		meta.setTargetId(targetId);
		meta.setName(entry.getName());
		meta.setValue(entry.getValue());
		
		PersistContext.getSession().save(meta);
		return meta;
	}
	
	public void save(String target, long targetId, S3MetaDataEntry[] entries) {
		// To redefine the target's metadaa
		executeUpdate("delete from SMeta where target=? and targetId=?", new Object[] { target, new Long(targetId)});

		if(entries != null) {
			for(S3MetaDataEntry entry : entries)
				save(target, targetId, entry);
		}
	}
}
