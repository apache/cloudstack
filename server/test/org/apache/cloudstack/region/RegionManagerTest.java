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

package org.apache.cloudstack.region;


import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.cloudstack.api.command.admin.domain.DeleteDomainCmd;
import org.apache.cloudstack.region.dao.RegionDao;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.user.Account;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;



public class RegionManagerTest extends TestCase {
    private static final Logger s_logger = Logger.getLogger(RegionManagerTest.class);

    @Before
    @Override
    protected void setUp() {

    }
    
    @Test
    public void testUniqueName() {
    	RegionManagerImpl regionMgr = new RegionManagerImpl();
    	RegionDao regionDao = Mockito.mock(RegionDao.class);
    	RegionVO region = new RegionVO(2, "APAC", "");
    	Mockito.when(regionDao.findByName(Mockito.anyString())).thenReturn(region);    	
    	regionMgr._regionDao = regionDao;
    	try {
    		regionMgr.addRegion(2, "APAC", "");
    	} catch (InvalidParameterValueException e){
    		Assert.assertEquals("Region with name: APAC already exists", e.getMessage());
    	}
    }

}
