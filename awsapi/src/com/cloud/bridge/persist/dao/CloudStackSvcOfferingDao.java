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

import java.util.List;
import org.apache.log4j.Logger;

import com.cloud.bridge.persist.EntityDao;
import com.cloud.stack.models.CloudStackServiceOffering;


public class CloudStackSvcOfferingDao extends EntityDao<CloudStackServiceOffering> {
	public static final Logger logger = Logger.getLogger(CloudStackSvcOfferingDao.class);

	public CloudStackSvcOfferingDao() {
	    super(CloudStackServiceOffering.class, true);
	}

    public List<CloudStackServiceOffering> getSvcOfferingByName( String name ){
        return queryEntities("from CloudStackServiceOffering where name=?", new Object[] {name});
    }

    public CloudStackServiceOffering getSvcOfferingById( String id ){
        return queryEntity("from CloudStackServiceOffering where id=?", new Object[] {id});
    }

}
